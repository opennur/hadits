package com.hikmah.hadits.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hikmah.hadits.ui.screens.CollectionScreen
import com.hikmah.hadits.ui.screens.DetailScreen
import com.hikmah.hadits.ui.screens.DownloadManagerScreen
import com.hikmah.hadits.ui.screens.FavoritesScreen
import com.hikmah.hadits.ui.screens.HomeScreen
import com.hikmah.hadits.ui.screens.SearchScreen

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination("home", "Beranda", Icons.Outlined.Home),
    BottomDestination("search", "Cari", Icons.Outlined.Search),
    BottomDestination("favorites", "Tersimpan", Icons.Outlined.FavoriteBorder),
    BottomDestination("downloads", "Offline", Icons.Outlined.CloudDownload),
)

@Composable
fun HikmahApp(
    viewModel: HadithViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomDestinations.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    val books by viewModel.books.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val bookState by viewModel.bookState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val detailLoading by viewModel.detailLoading.collectAsStateWithLifecycle()
    val detailError by viewModel.detailError.collectAsStateWithLifecycle()
    val booksLoading by viewModel.booksLoading.collectAsStateWithLifecycle()
    val booksError by viewModel.booksError.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    books = books,
                    favoritesCount = favorites.size,
                    isLoading = booksLoading,
                    error = booksError,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onRefresh = viewModel::refreshBooks,
                    onSearch = { navController.navigate("search") },
                    onDownload = { navController.navigate("downloads") },
                    onBookClick = { book -> navController.navigate("collection/${book.id}") },
                )
            }
            composable("search") {
                SearchScreen(
                    state = searchState,
                    onBack = { navController.popBackStack() },
                    onSearch = viewModel::search,
                    onOpenHadith = { hadith ->
                        navController.navigate("detail/${hadith.bookId}/${hadith.number}")
                    },
                    onFavorite = viewModel::toggleFavorite,
                    onClear = viewModel::clearSearch,
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    favorites = favorites,
                    onOpenHadith = { hadith ->
                        navController.navigate("detail/${hadith.bookId}/${hadith.number}")
                    },
                    onFavorite = viewModel::toggleFavorite,
                )
            }
            composable("downloads") {
                DownloadManagerScreen(
                    downloads = downloads,
                    onBack = { navController.popBackStack() },
                    onStart = viewModel::startDownloadAll,
                    onCancel = viewModel::cancelDownloadAll,
                )
            }
            composable("collection/{bookId}") { entry ->
                val bookId = entry.arguments?.getString("bookId").orEmpty()
                val book = books.firstOrNull { it.id == bookId }
                androidx.compose.runtime.LaunchedEffect(bookId) {
                    viewModel.loadBook(bookId)
                }
                CollectionScreen(
                    book = book,
                    state = bookState,
                    onBack = { navController.popBackStack() },
                    onOpenHadith = { hadith ->
                        navController.navigate("detail/${hadith.bookId}/${hadith.number}")
                    },
                    onFavorite = viewModel::toggleFavorite,
                    onLoadMore = viewModel::loadMore,
                    onRetry = { viewModel.loadBook(bookId) },
                    onJumpToNumber = viewModel::jumpToHadith,
                    onJumpHandled = viewModel::consumeJumpTarget,
                )
            }
            composable("detail/{bookId}/{number}") { entry ->
                val bookId = entry.arguments?.getString("bookId").orEmpty()
                val number = entry.arguments?.getString("number")?.toIntOrNull() ?: 1
                androidx.compose.runtime.LaunchedEffect(bookId, number) {
                    viewModel.loadDetail(bookId, number)
                }
                DetailScreen(
                    hadith = detail,
                    loading = detailLoading,
                    error = detailError,
                    onBack = { navController.popBackStack() },
                    onRetry = { viewModel.loadDetail(bookId, number) },
                    onFavorite = viewModel::toggleFavorite,
                )
            }
        }
    }
}
