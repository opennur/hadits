package com.hikmah.hadits.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hikmah.hadits.data.local.BookEntity
import com.hikmah.hadits.data.local.DownloadEntity
import com.hikmah.hadits.data.local.HadithDatabase
import com.hikmah.hadits.data.local.HadithEntity
import com.hikmah.hadits.data.remote.EditionHadithDto
import com.hikmah.hadits.data.remote.HadithApi
import com.hikmah.hadits.data.remote.HadithDetailDto
import com.hikmah.hadits.data.remote.HadithIntegrityValidator
import com.hikmah.hadits.data.remote.HadithSearchApi
import com.hikmah.hadits.data.remote.HadithItemDto
import com.hikmah.hadits.model.Book
import com.hikmah.hadits.model.DownloadItem
import com.hikmah.hadits.model.DownloadStatus
import com.hikmah.hadits.model.Hadith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HadithRepository private constructor(
    private val database: HadithDatabase,
    private val api: HadithApi,
    private val searchApi: HadithSearchApi,
) {
    private val bookDao = database.bookDao()
    private val hadithDao = database.hadithDao()
    private val downloadDao = database.downloadDao()

    fun observeBooks(): Flow<List<Book>> = bookDao.observeBooks().map { books ->
        books.map { it.toModel() }
    }

    fun observeBook(bookId: String): Flow<List<Hadith>> = hadithDao.observeByBook(bookId).map { hadiths ->
        hadiths.map { it.toModel() }
    }

    fun observeDetail(bookId: String, number: Int): Flow<Hadith?> =
        hadithDao.observeDetail(bookId, number).map { it?.toModel() }

    fun observeFavorites(): Flow<List<Hadith>> = hadithDao.observeFavorites().map { hadiths ->
        hadiths.map { it.toModel() }
    }

    fun observeDownloads(): Flow<List<DownloadItem>> = downloadDao.observeAll().map { downloads ->
        downloads.map { it.toModel() }
    }

    suspend fun refreshBooks(): Result<Unit> = runCatching { seedFallbackBooks() }

    suspend fun refreshBook(bookId: String, from: Int, to: Int): Result<Unit> = runCatching {
        val bookName = DEFAULT_BOOKS.firstOrNull { it.id == bookId }?.name ?: bookId.titleCase()
        val page = ((from - 1) / PAGE_SIZE) + 1
        val response = api.getHadithPage(bookId.apiSlug(), page = page, limit = PAGE_SIZE)
        val hadiths = HadithIntegrityValidator.validPageItems(
            page = response,
            firstNumber = (page - 1) * PAGE_SIZE + 1,
            pageSize = PAGE_SIZE,
        ).mapNotNull { dto ->
            dto.toEntity(bookId, bookName)
        }
        if (hadiths.isEmpty()) error("Data hadits tidak tersedia")
        if (response.total > 0) {
            bookDao.upsertAll(listOf(BookEntity(bookId, bookName, response.total)))
        }
        saveHadiths(hadiths)
    }

    suspend fun refreshDetail(bookId: String, number: Int): Result<Unit> = runCatching {
        val bookName = DEFAULT_BOOKS.firstOrNull { it.id == bookId }?.name ?: bookId.titleCase()
        val apiSlug = bookId.apiSlug()
        val detail = api.getHadithDetail(apiSlug, number)
        if (!HadithIntegrityValidator.isValidDetail(detail, apiSlug, number)) {
            error("Respons detail hadits tidak cocok")
        }
        val hadith = detail.toEntity(bookId, bookName)
            ?: error("Data hadits tidak tersedia")
        saveHadiths(listOf(hadith))
    }

    suspend fun searchRemote(query: String): Result<Unit> = runCatching {
        // The CDN has no search endpoint. Download the two most-used collections only
        // when a query has no local result, then search the Room cache.
        val books = listOf("bukhari", "muslim")
        val loadedBooks = supervisorScope {
            books.map { bookId ->
                async(Dispatchers.IO) {
                    runCatching {
                        val bookName = DEFAULT_BOOKS.first { it.id == bookId }.name
                        val response = searchApi.getEdition("ind-${bookId.apiEditionId()}")
                        val hadiths = response.hadiths.orEmpty().mapNotNull { dto ->
                            dto.toEntity(bookId, bookName, arabic = "")
                        }
                        if (hadiths.isNotEmpty()) saveHadiths(hadiths)
                        hadiths.isNotEmpty()
                    }.getOrDefault(false)
                }
            }.awaitAll()
        }
        if (loadedBooks.none { it }) error("Pencarian gagal")
    }

    suspend fun searchCached(query: String): List<Hadith> = withContext(Dispatchers.IO) {
        hadithDao.search(query.trim()).map { it.toModel() }
    }

    suspend fun setFavorite(hadith: Hadith, favorite: Boolean) = withContext(Dispatchers.IO) {
        hadithDao.setFavorite(hadith.bookId, hadith.number, favorite)
    }

    suspend fun prepareDownloadQueue() = withContext(Dispatchers.IO) {
        seedFallbackBooks()
        val books = bookDao.getBooks().ifEmpty { DEFAULT_BOOKS }
        downloadDao.upsertAll(
            books.map { book ->
                DownloadEntity(
                    bookId = book.id,
                    bookName = book.name,
                    downloaded = 0,
                    total = book.available,
                    status = DownloadStatus.QUEUED.name,
                )
            },
        )
    }

    suspend fun cancelDownloads() = withContext(Dispatchers.IO) {
        downloadDao.updateActiveStatus(
            status = DownloadStatus.CANCELLED.name,
            error = null,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun downloadAllResources(): Boolean = withContext(Dispatchers.IO) {
        seedFallbackBooks()
        val books = bookDao.getBooks().ifEmpty { DEFAULT_BOOKS }
        var allSucceeded = true
        books.forEach { book ->
            ensureActive()
            val existing = downloadDao.get(book.id)
            var downloaded = existing?.downloaded?.coerceIn(0, book.available) ?: 0
            if (existing?.status == DownloadStatus.COMPLETED.name && downloaded >= book.available) return@forEach

            downloadDao.updateStatus(
                bookId = book.id,
                status = DownloadStatus.DOWNLOADING.name,
                error = null,
                updatedAt = System.currentTimeMillis(),
            )

            try {
                while (downloaded < book.available) {
                    ensureActive()
                    val ranges = (downloaded until minOf(downloaded + PAGE_SIZE * DOWNLOAD_BATCH_SIZE, book.available))
                        .step(PAGE_SIZE)
                        .map { from -> from to minOf(from + PAGE_SIZE - 1, book.available) }
                    coroutineScope {
                        ranges.map { (from, to) ->
                            async { refreshBook(book.id, from, to).getOrThrow() }
                        }.awaitAll()
                    }
                    downloaded = minOf(downloaded + ranges.size * PAGE_SIZE, book.available)
                    downloadDao.updateProgress(
                        bookId = book.id,
                        downloaded = downloaded,
                        total = book.available,
                        status = DownloadStatus.DOWNLOADING.name,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                downloadDao.updateStatus(
                    bookId = book.id,
                    status = DownloadStatus.COMPLETED.name,
                    error = null,
                    updatedAt = System.currentTimeMillis(),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                allSucceeded = false
                downloadDao.updateStatus(
                    bookId = book.id,
                    status = DownloadStatus.FAILED.name,
                    error = "Gagal mengunduh ${book.name}",
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
        allSucceeded
    }

    suspend fun seedFallbackBooks() = withContext(Dispatchers.IO) {
        bookDao.upsertAll(DEFAULT_BOOKS)
    }

    suspend fun seedFallbackHadiths() = withContext(Dispatchers.IO) {
        if (hadithDao.getByBook("bukhari").isEmpty()) hadithDao.insertAll(DEFAULT_HADITHS)
    }

    private suspend fun saveHadiths(hadiths: List<HadithEntity>) {
        hadithDao.insertAll(hadiths)
        hadiths.forEach { hadith ->
            hadithDao.updateContent(
                bookId = hadith.bookId,
                number = hadith.number,
                bookName = hadith.bookName,
                arabic = hadith.arabic,
                translation = hadith.translation,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    private fun BookEntity.toModel() = Book(id, name, available)

    private fun HadithEntity.toModel() = Hadith(
        id = "$bookId-$number",
        bookId = bookId,
        bookName = bookName,
        number = number,
        arabic = arabic,
        translation = translation,
        isFavorite = isFavorite,
    )

    private fun DownloadEntity.toModel() = DownloadItem(
        bookId = bookId,
        bookName = bookName,
        downloaded = downloaded,
        total = total,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED),
        error = error,
    )

    private fun HadithItemDto.toEntity(bookId: String, bookName: String): HadithEntity? {
        val number = number ?: return null
        val arabicText = arab.orEmpty().trim()
        val translation = id.orEmpty().trim()
        if (arabicText.isBlank() && translation.isBlank()) return null
        return HadithEntity(
            bookId = bookId,
            bookName = bookName,
            number = number,
            arabic = arabicText,
            translation = translation,
        )
    }

    private fun HadithDetailDto.toEntity(bookId: String, bookName: String): HadithEntity? {
        val number = number ?: return null
        val arabicText = arab.orEmpty().trim()
        val translation = id.orEmpty().trim()
        if (arabicText.isBlank() && translation.isBlank()) return null
        return HadithEntity(
            bookId = bookId,
            bookName = bookName,
            number = number,
            arabic = arabicText,
            translation = translation,
        )
    }

    private fun EditionHadithDto.toEntity(
        bookId: String,
        bookName: String,
        arabic: String,
    ): HadithEntity? {
        val number = hadithNumber ?: return null
        val translation = text.orEmpty().trim()
        val arabicText = arabic.trim()
        if (arabicText.isBlank() && translation.isBlank()) return null
        return HadithEntity(
            bookId = bookId,
            bookName = bookName,
            number = number,
            arabic = arabicText,
            translation = translation,
        )
    }

    companion object {
        private const val BASE_URL = "https://hadis-api-id.vercel.app/"
        private const val SEARCH_BASE_URL = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/"
        private const val PAGE_SIZE = 20

        private val DEFAULT_BOOKS = listOf(
            BookEntity("bukhari", "Shahih Bukhari", 6638),
            BookEntity("muslim", "Shahih Muslim", 4930),
            BookEntity("abu-daud", "Sunan Abu Daud", 4419),
            BookEntity("tirmidzi", "Jami' At-Tirmidzi", 3625),
            BookEntity("nasai", "Sunan An-Nasa'i", 5364),
            BookEntity("ibnu-majah", "Sunan Ibnu Majah", 4285),
            BookEntity("malik", "Muwatha Malik", 1587),
            BookEntity("ahmad", "Musnad Ahmad", 4305),
            BookEntity("darimi", "Sunan Ad-Darimi", 2949),
        )

        private val DEFAULT_HADITHS = listOf(
            HadithEntity(
                bookId = "bukhari",
                bookName = "Shahih Bukhari",
                number = 1,
                arabic = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
                translation = "Sesungguhnya setiap amalan tergantung pada niatnya. Dan setiap orang akan mendapatkan sesuai dengan apa yang dia niatkan.",
            ),
        )

        @Volatile
        private var instance: HadithRepository? = null

        fun getInstance(context: Context): HadithRepository = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): HadithRepository {
            val database = Room.databaseBuilder(
                context,
                HadithDatabase::class.java,
                "hikmah.db",
            ).addMigrations(
                object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL(
                            "CREATE TABLE IF NOT EXISTS downloads (" +
                                "bookId TEXT NOT NULL, " +
                                "bookName TEXT NOT NULL, " +
                                "downloaded INTEGER NOT NULL, " +
                                "total INTEGER NOT NULL, " +
                                "status TEXT NOT NULL, " +
                                "error TEXT, " +
                                "updatedAt INTEGER NOT NULL, " +
                                "PRIMARY KEY(bookId))",
                        )
                    }
                },
            ).build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val searchRetrofit = Retrofit.Builder()
                .baseUrl(SEARCH_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return HadithRepository(
                database = database,
                api = retrofit.create(HadithApi::class.java),
                searchApi = searchRetrofit.create(HadithSearchApi::class.java),
            )
        }

        private fun String.titleCase(): String =
            replace('-', ' ').split(' ').joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }

        private fun String.apiEditionId(): String = when (this) {
            "abu-daud" -> "abudawud"
            "tirmidzi" -> "tirmidhi"
            "ibnu-majah" -> "ibnmajah"
            else -> this
        }

        private fun String.apiSlug(): String = when (this) {
            "abu-daud" -> "abu-dawud"
            else -> this
        }

        private const val DOWNLOAD_BATCH_SIZE = 4
    }
}
