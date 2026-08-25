package com.hikmah.hadits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hikmah.hadits.data.DownloadAllWorker
import com.hikmah.hadits.data.HadithRepository
import com.hikmah.hadits.model.Book
import com.hikmah.hadits.model.DownloadItem
import com.hikmah.hadits.model.Hadith
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
    val hasSearched: Boolean = false,
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
    private var currentBookId: String? = null

    init {
        viewModelScope.launch {
            repository.seedFallbackBooks()
            repository.seedFallbackHadiths()
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
        _searchState.value = SearchUiState(query = cleanQuery, isLoading = true, hasSearched = true)
        viewModelScope.launch {
            val cached = repository.searchCached(cleanQuery)
            if (cached.isNotEmpty()) {
                _searchState.value = _searchState.value.copy(results = cached, isLoading = false)
                return@launch
            }
            val result = repository.searchRemote(cleanQuery)
            val refreshed = repository.searchCached(cleanQuery)
            _searchState.value = _searchState.value.copy(
                results = refreshed,
                isLoading = false,
                error = if (result.isFailure && refreshed.isEmpty()) {
                    "Pencarian memerlukan koneksi internet."
                } else {
                    null
                },
            )
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
        viewModelScope.launch {
            repository.setFavorite(hadith, !hadith.isFavorite)
        }
    }

    fun startDownloadAll(resume: Boolean = false) {
        viewModelScope.launch {
            if (!resume) repository.prepareDownloadQueue()
            val request = OneTimeWorkRequestBuilder<DownloadAllWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            workManager.enqueueUniqueWork(
                DownloadAllWorker.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }

    fun cancelDownloadAll() {
        workManager.cancelUniqueWork(DownloadAllWorker.UNIQUE_WORK_NAME)
        viewModelScope.launch { repository.cancelDownloads() }
    }

    fun clearSearch() {
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
