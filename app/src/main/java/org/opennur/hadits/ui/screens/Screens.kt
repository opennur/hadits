package org.opennur.hadits.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.opennur.hadits.model.Book
import org.opennur.hadits.model.Hadith
import org.opennur.hadits.ui.BookUiState
import org.opennur.hadits.ui.SearchUiState
import org.opennur.hadits.ui.components.BookCard
import org.opennur.hadits.ui.components.EmptyState
import org.opennur.hadits.ui.components.HadithCard
import org.opennur.hadits.ui.components.LoadingCard
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    books: List<Book>,
    favoritesCount: Int,
    isLoading: Boolean,
    error: String?,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onDownload: () -> Unit,
    onBookClick: (Book) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Assalamu'alaikum",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("Temukan hikmah hari ini", style = MaterialTheme.typography.headlineSmall)
                }
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (isDarkTheme) "Aktifkan mode terang" else "Aktifkan mode gelap",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchLauncher(onClick = onSearch)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Simpan untuk offline",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Unduh semua kitab dan kelola progresnya",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Baca dengan tenang",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            "Satu hadits kecil bisa mengubah arah hari ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                        )
                    }
                    Icon(
                        Icons.Outlined.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Koleksi hadits", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pilih kitab untuk mulai membaca",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (favoritesCount > 0) {
                    Text(
                        "$favoritesCount tersimpan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        if (isLoading && books.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { LoadingCard() }
        } else if (books.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = "Koleksi belum tersedia",
                    message = error ?: "Coba muat ulang untuk mengambil data kitab.",
                )
            }
        } else {
            gridItems(books, key = { it.id }) { book ->
                BookCard(book = book, onClick = { onBookClick(book) })
            }
            if (error != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TextButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                        Text("Coba muat ulang")
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun SearchLauncher(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                "Cari kata, tema, atau makna hadits",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    book: Book?,
    state: BookUiState,
    onBack: () -> Unit,
    onOpenHadith: (Hadith) -> Unit,
    onFavorite: (Hadith) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onJumpToNumber: (Int) -> Unit,
    onJumpHandled: () -> Unit,
) {
    var numberText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showBackToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    LaunchedEffect(state.jumpTarget, state.hadiths) {
        val target = state.jumpTarget ?: return@LaunchedEffect
        val index = state.hadiths.indexOfFirst { it.number == target }
        if (index >= 0) {
            listState.scrollToItem(index + 2)
            onJumpHandled()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (showBackToTop) {
                FloatingActionButton(
                    onClick = { coroutineScope.launch { listState.scrollToItem(0) } },
                ) {
                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Kembali ke atas")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(book?.name ?: "Koleksi hadits", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (book != null) {
                            Text(
                                "${book.available} hadits tersedia",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Baca perlahan, simpan yang menguatkan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = numberText,
                        onValueChange = { value ->
                            numberText = value.filter(Char::isDigit).take(6)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Nomor hadits") },
                        placeholder = { Text("Contoh: 125") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { numberText.toIntOrNull()?.let(onJumpToNumber) },
                        ),
                    )
                    Button(
                        onClick = { numberText.toIntOrNull()?.let(onJumpToNumber) },
                        enabled = numberText.isNotBlank() && !state.isJumping,
                        modifier = Modifier.height(56.dp),
                    ) {
                        if (state.isJumping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Lompat")
                        }
                    }
                }
            }
            if (state.jumpError != null) {
                item {
                    Text(
                        state.jumpError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (state.isLoading && state.hadiths.isEmpty()) {
                item { LoadingCard() }
            } else if (state.hadiths.isEmpty()) {
                item {
                    EmptyState(
                        title = "Hadits belum dimuat",
                        message = state.error ?: "Belum ada hadits yang tersimpan di perangkat.",
                    )
                }
                item {
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Coba lagi")
                    }
                }
            } else {
                lazyItems(state.hadiths, key = { "${it.bookId}-${it.number}" }) { hadith ->
                    HadithCard(
                        hadith = hadith,
                        onClick = { onOpenHadith(hadith) },
                        onFavorite = { onFavorite(hadith) },
                    )
                }
                item {
                    if (state.isLoadingMore) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                        }
                    } else {
                        TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                            Text("Muat 20 hadits berikutnya")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onOpenHadith: (Hadith) -> Unit,
    onFavorite: (Hadith) -> Unit,
    onClear: () -> Unit,
) {
    var query by remember { mutableStateOf(state.query) }
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(state.hasMore, state.isLoadingMore, state.results.size) {
        derivedStateOf {
            state.hasMore &&
                !state.isLoadingMore &&
                state.results.isNotEmpty() &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.results.lastIndex - 5
        }
    }

    LaunchedEffect(state.query) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cari hadits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                placeholder = { Text("Contoh: sabar, sedekah, niat") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; onClear() }) {
                            Text("×", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            )
            Spacer(Modifier.height(16.dp))
            if (!state.hasSearched) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cari berdasarkan tema", style = MaterialTheme.typography.titleMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 20.dp),
                    ) {
                        lazyItems(
                            listOf(
                                "Sabar",
                                "Sedekah",
                                "Niat",
                                "Shalat",
                                "Puasa",
                                "Doa",
                                "Akhlak",
                                "Ilmu",
                                "Rezeki",
                                "Taubat",
                                "Orang tua",
                                "Silaturahmi",
                            ),
                        ) { suggestion ->
                            AssistChip(
                                onClick = { query = suggestion; onSearch(suggestion) },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                    EmptyState(
                        title = "Apa yang sedang kamu cari?",
                        message = "Ketik kata kunci dalam bahasa Indonesia untuk menemukan hadits yang relevan.",
                    )
                }
            } else if (state.isLoading && state.results.isEmpty()) {
                LoadingCard()
            } else if (state.results.isEmpty()) {
                EmptyState(
                    title = "Belum menemukan hasil",
                    message = state.error ?: "Coba gunakan kata kunci yang lebih umum.",
                )
            } else {
                Text(
                    "${state.results.size} dari ${state.totalResults} hadits",
                    modifier = Modifier.padding(bottom = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    lazyItems(state.results, key = { "${it.bookId}-${it.number}" }) { hadith ->
                        HadithCard(
                            hadith = hadith,
                            onClick = { onOpenHadith(hadith) },
                            onFavorite = { onFavorite(hadith) },
                        )
                    }
                    item {
                        if (state.isLoadingMore) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            }
                        } else if (state.hasMore) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<Hadith>,
    onOpenHadith: (Hadith) -> Unit,
    onFavorite: (Hadith) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Tersimpan",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("Hadits pilihanmu", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Kembali kapan saja untuk membacanya lagi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (favorites.isEmpty()) {
            item {
                EmptyState(
                    title = "Belum ada favorit",
                    message = "Ketuk ikon hati pada hadits yang ingin kamu simpan.",
                )
            }
        } else {
            lazyItems(favorites, key = { "${it.bookId}-${it.number}" }) { hadith ->
                HadithCard(
                    hadith = hadith,
                    onClick = { onOpenHadith(hadith) },
                    onFavorite = { onFavorite(hadith) },
                )
            }
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    hadith: Hadith?,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFavorite: (Hadith) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(hadith?.bookName ?: "Detail hadits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    hadith?.let { item ->
                        IconButton(onClick = { onFavorite(item) }) {
                            Icon(
                                if (item.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorit",
                                tint = if (item.isFavorite) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        when {
            loading && hadith == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            hadith == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(error ?: "Detail belum tersedia", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Coba lagi") }
            }
            else -> DetailContent(
                hadith = hadith,
                padding = padding,
                onCopy = {
                    clipboard.setText(
                        AnnotatedString(
                            buildString {
                                if (hadith.arabic.isNotBlank()) {
                                    append(hadith.arabic)
                                    append("\n\n")
                                }
                                append(hadith.translation)
                                append("\n\nHadits riwayat ${hadith.bookName} nomor ${hadith.number}.")
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun DetailContent(
    hadith: Hadith,
    padding: PaddingValues,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 10.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    "Hadits No. ${hadith.number}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin hadits")
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (hadith.arabic.isNotBlank()) {
                    Text(
                        hadith.arabic,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 26.sp,
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                        ),
                    )
                }
                if (hadith.translation.isNotBlank()) {
                    Text(
                        hadith.translation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Text(
            "Sumber: ${hadith.bookName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
