package org.opennur.hadits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.opennur.hadits.data.DownloadAllWorker
import org.opennur.hadits.data.HadithRepository
import org.opennur.hadits.model.Book
import org.opennur.hadits.model.DownloadItem
import org.opennur.hadits.model.DownloadStatus
import org.opennur.hadits.model.Hadith
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookUiState(
    val bookId: String? = null,
    val hadiths: List<Hadith> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val loadedUntil: Int = 0,
    val jumpTarget: Int? = null,
    val isJumping: Boolean = false,
    val jumpError: String? = null,
)

data class SearchUiState(
    val query: String = "",
    val results: List<Hadith> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val hasMore: Boolean = false,
    val totalResults: Int = 0,
    val error: String? = null,
)

class HadithViewModel(
    private val repository: HadithRepository,
    private val workManager: WorkManager,
) : ViewModel() {
    val books: StateFlow<List<Book>> = repository.observeBooks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Hadith>> = repository.observeFavorites()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = repository.observeDownloads()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _bookState = MutableStateFlow(BookUiState())
    val bookState: StateFlow<BookUiState> = _bookState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _detail = MutableStateFlow<Hadith?>(null)
    val detail: StateFlow<Hadith?> = _detail.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    private val _booksLoading = MutableStateFlow(false)
    val booksLoading: StateFlow<Boolean> = _booksLoading.asStateFlow()

    private val _booksError = MutableStateFlow<String?>(null)
    val booksError: StateFlow<String?> = _booksError.asStateFlow()

    private var bookObserver: Job? = null
    private var detailObserver: Job? = null
    private var searchJob: Job? = null
    private var currentBookId: String? = null

    init {
        viewModelScope.launch {
            repository.seedFallbackBooks()
            repository.seedFallbackHadiths()
            repository.ensureDownloadCatalog()
            refreshBooks()
        }
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _booksLoading.value = true
            _booksError.value = null
            repository.refreshBooks().onFailure { error ->
                _booksError.value = "Belum dapat memuat koleksi terbaru. Data bawaan tetap tersedia."
            }
            _booksLoading.value = false
        }
    }

    fun loadBook(bookId: String) {
        if (currentBookId == bookId && _bookState.value.hadiths.isNotEmpty()) return
        currentBookId = bookId
        _bookState.value = BookUiState(bookId = bookId, isLoading = true)
        bookObserver?.cancel()
        bookObserver = viewModelScope.launch {
            repository.observeBook(bookId).collect { hadiths ->
                _bookState.value = _bookState.value.copy(hadiths = hadiths)
            }
        }
        viewModelScope.launch {
            val result = repository.refreshBook(bookId, 1, 20)
            _bookState.value = _bookState.value.copy(
                isLoading = false,
                loadedUntil = maxOf(_bookState.value.loadedUntil, 20),
                error = result.exceptionOrNull()?.let { "Hadits belum dapat dimuat. Periksa koneksi internet." },
            )
        }
    }

    fun loadMore() {
        val state = _bookState.value
        val bookId = state.bookId ?: return
        if (state.isLoadingMore || state.loadedUntil == 0) return
        val from = state.loadedUntil + 1
        val to = from + 19
        _bookState.value = state.copy(isLoadingMore = true, error = null)
        viewModelScope.launch {
            val result = repository.refreshBook(bookId, from, to)
            _bookState.value = _bookState.value.copy(
                isLoadingMore = false,
                loadedUntil = if (result.isSuccess) to else state.loadedUntil,
                error = result.exceptionOrNull()?.let { "Gagal memuat halaman berikutnya." },
            )
        }
    }

    fun jumpToHadith(number: Int) {
        val bookId = _bookState.value.bookId ?: return
        if (number < 1) return

        val cached = _bookState.value.hadiths.any { it.number == number }
        if (cached) {
            _bookState.value = _bookState.value.copy(jumpTarget = number, jumpError = null)
            return
        }

        _bookState.value = _bookState.value.copy(
            isJumping = true,
            jumpTarget = null,
            jumpError = null,
        )
        viewModelScope.launch {
            val result = repository.refreshBook(bookId, number, number)
            _bookState.value = _bookState.value.copy(
                isJumping = false,
                jumpTarget = if (result.isSuccess) number else null,
                jumpError = result.exceptionOrNull()?.let {
                    "Hadits nomor $number belum dapat dimuat."
                },
            )
        }
    }

    fun consumeJumpTarget() {
        _bookState.value = _bookState.value.copy(jumpTarget = null)
    }

    fun search(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return
        searchJob?.cancel()
        _searchState.value = SearchUiState(query = cleanQuery, isLoading = true, hasSearched = true)
        searchJob = viewModelScope.launch {
            var page = repository.searchCached(cleanQuery)
            var remoteResult: Result<Unit>? = null
            if (!repository.isPrimarySearchIndexReady()) {
                remoteResult = repository.searchRemote(cleanQuery)
                page = repository.searchCached(cleanQuery)
            }
            _searchState.value = _searchState.value.copy(
                results = page.results,
                isLoading = false,
                hasMore = page.hasMore,
                totalResults = page.total,
                error = if (remoteResult?.isFailure == true && page.results.isEmpty()) {
                    "Pencarian memerlukan koneksi internet."
                } else {
                    null
                },
            )
        }
    }

    fun loadMoreSearch() {
        val state = _searchState.value
        if (state.query.isBlank() || state.isLoading || state.isLoadingMore || !state.hasMore) return
        val query = state.query
        val offset = state.results.size
        _searchState.value = state.copy(isLoadingMore = true)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val page = repository.searchCached(query, offset)
            if (_searchState.value.query == query) {
                _searchState.value = _searchState.value.copy(
                    results = _searchState.value.results + page.results,
                    hasMore = page.hasMore,
                    totalResults = page.total,
                    isLoadingMore = false,
                )
            }
        }
    }

    fun loadDetail(bookId: String, number: Int) {
        detailObserver?.cancel()
        _detail.value = null
        _detailError.value = null
        detailObserver = viewModelScope.launch {
            repository.observeDetail(bookId, number).collect { hadith ->
                _detail.value = hadith
                if (hadith != null) _detailLoading.value = false
            }
        }
        _detailLoading.value = true
        viewModelScope.launch {
            val result = repository.refreshDetail(bookId, number)
            _detailLoading.value = false
            if (result.isFailure && _detail.value == null) {
                _detailError.value = "Detail hadits belum dapat dimuat."
            }
        }
    }

    fun toggleFavorite(hadith: Hadith) {
        val favorite = !hadith.isFavorite
        updateFavoriteInUi(hadith, favorite)
        viewModelScope.launch {
            runCatching {
                repository.setFavorite(hadith, favorite)
            }.onFailure {
                updateFavoriteInUi(hadith, hadith.isFavorite)
            }
        }
    }

    private fun updateFavoriteInUi(hadith: Hadith, favorite: Boolean) {
        _bookState.value = _bookState.value.copy(
            hadiths = _bookState.value.hadiths.map { item ->
                if (item.id == hadith.id) item.copy(isFavorite = favorite) else item
            },
        )
        _searchState.value = _searchState.value.copy(
            results = _searchState.value.results.map { item ->
                if (item.id == hadith.id) item.copy(isFavorite = favorite) else item
            },
        )
        if (_detail.value?.id == hadith.id) {
            _detail.value = _detail.value?.copy(isFavorite = favorite)
        }
    }

    fun startDownloadAll(resume: Boolean = false) {
        viewModelScope.launch {
            val targetIds = if (resume) {
                downloads.value
                    .filter { it.status != DownloadStatus.COMPLETED }
                    .map { it.bookId }
            } else {
                repository.prepareDownloadQueue().map { it.id }
            }
            targetIds.forEach(::enqueueBookDownload)
        }
    }

    fun cancelDownloadAll() {
        downloads.value.forEach { book ->
            workManager.cancelUniqueWork(DownloadAllWorker.workName(book.bookId))
        }
        viewModelScope.launch { repository.cancelDownloads() }
    }

    fun startDownloadBook(bookId: String, resume: Boolean) {
        viewModelScope.launch {
            if (!resume) repository.prepareBookDownload(bookId)
            enqueueBookDownload(bookId)
        }
    }

    fun cancelDownloadBook(bookId: String) {
        workManager.cancelUniqueWork(DownloadAllWorker.workName(bookId))
        viewModelScope.launch { repository.cancelDownload(bookId) }
    }

    fun deleteDownloadedBook(bookId: String) {
        workManager.cancelUniqueWork(DownloadAllWorker.workName(bookId))
        viewModelScope.launch { repository.deleteDownloadedBook(bookId) }
    }

    private fun enqueueBookDownload(bookId: String) {
        val request = OneTimeWorkRequestBuilder<DownloadAllWorker>()
            .setInputData(workDataOf(DownloadAllWorker.BOOK_ID_KEY to bookId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniqueWork(
            DownloadAllWorker.workName(bookId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchState.value = SearchUiState()
    }

    override fun onCleared() {
        bookObserver?.cancel()
        detailObserver?.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: HadithRepository,
        private val workManager: WorkManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HadithViewModel(repository, workManager) as T
    }
}
