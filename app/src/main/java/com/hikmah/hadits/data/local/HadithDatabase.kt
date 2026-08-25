package com.hikmah.hadits.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class BookEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val available: Int,
)

@Entity(tableName = "hadiths", primaryKeys = ["bookId", "number"])
data class HadithEntity(
    val bookId: String,
    val bookName: String,
    val number: Int,
    val arabic: String,
    val translation: String,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @androidx.room.PrimaryKey val bookId: String,
    val bookName: String,
    val downloaded: Int,
    val total: Int,
    val status: String,
    val error: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY available DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY available DESC")
    suspend fun getBooks(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<BookEntity>)
}

@Dao
interface HadithDao {
    @Query("SELECT * FROM hadiths WHERE bookId = :bookId ORDER BY number")
    suspend fun getByBook(bookId: String): List<HadithEntity>

    @Query("SELECT * FROM hadiths WHERE bookId = :bookId ORDER BY number")
    fun observeByBook(bookId: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE bookId = :bookId AND number = :number LIMIT 1")
    fun observeDetail(bookId: String, number: Int): Flow<HadithEntity?>

    @Query(
        "SELECT * FROM hadiths " +
            "WHERE arabic LIKE '%' || :query || '%' " +
            "OR translation LIKE '%' || :query || '%' " +
            "OR bookName LIKE '%' || :query || '%' " +
            "ORDER BY bookName, number LIMIT 80",
    )
    suspend fun search(query: String): List<HadithEntity>

    @Query("SELECT * FROM hadiths WHERE isFavorite = 1 ORDER BY bookName, number")
    fun observeFavorites(): Flow<List<HadithEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(hadiths: List<HadithEntity>)

    @Query(
        "UPDATE hadiths SET arabic = :arabic, translation = :translation, " +
            "bookName = :bookName, updatedAt = :updatedAt " +
            "WHERE bookId = :bookId AND number = :number",
    )
    suspend fun updateContent(
        bookId: String,
        number: Int,
        bookName: String,
        arabic: String,
        translation: String,
        updatedAt: Long,
    )

    @Query("UPDATE hadiths SET isFavorite = :favorite WHERE bookId = :bookId AND number = :number")
    suspend fun setFavorite(bookId: String, number: Int, favorite: Boolean)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY bookName")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE bookId = :bookId LIMIT 1")
    suspend fun get(bookId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(downloads: List<DownloadEntity>)

    @Query("UPDATE downloads SET status = :status, error = :error, updatedAt = :updatedAt WHERE bookId = :bookId")
    suspend fun updateStatus(bookId: String, status: String, error: String?, updatedAt: Long)

    @Query(
        "UPDATE downloads SET downloaded = :downloaded, total = :total, " +
            "status = :status, error = NULL, updatedAt = :updatedAt WHERE bookId = :bookId",
    )
    suspend fun updateProgress(
        bookId: String,
        downloaded: Int,
        total: Int,
        status: String,
        updatedAt: Long,
    )

    @Query(
        "UPDATE downloads SET status = :status, error = :error, updatedAt = :updatedAt " +
            "WHERE status IN ('QUEUED', 'DOWNLOADING')",
    )
    suspend fun updateActiveStatus(status: String, error: String?, updatedAt: Long)
}

@Database(entities = [BookEntity::class, HadithEntity::class, DownloadEntity::class], version = 2, exportSchema = false)
abstract class HadithDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun hadithDao(): HadithDao
    abstract fun downloadDao(): DownloadDao
}
