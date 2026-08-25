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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.opennur.hadits.model.DownloadItem
import org.opennur.hadits.model.DownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    downloads: List<DownloadItem>,
    onBack: () -> Unit,
    onStart: (resume: Boolean) -> Unit,
    onCancel: () -> Unit,
    onStartBook: (bookId: String, resume: Boolean) -> Unit,
    onCancelBook: (bookId: String) -> Unit,
    onDeleteBook: (bookId: String) -> Unit,
) {
    var deleteTarget by remember { mutableStateOf<DownloadItem?>(null) }
    val active = downloads.any {
        it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
    }
    val total = downloads.sumOf { it.total }
    val downloaded = downloads.sumOf { it.downloaded.coerceAtMost(it.total) }
    val overallProgress = if (total == 0) 0f else downloaded.toFloat() / total
    val completed = downloads.count { it.status == DownloadStatus.COMPLETED }
    val hasPartialDownload = downloads.any {
        it.status != DownloadStatus.COMPLETED && it.downloaded > 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download offline") },
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Baca tanpa internet",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text(
                                    if (active) "$downloaded dari $total hadits diproses"
                                    else "$completed dari ${downloads.size} kitab tersedia offline",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                                )
                            }
                            Icon(
                                Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(46.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            )
                        }
                        if (downloads.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { overallProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }
            item {
                if (active) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text("Batalkan unduhan")
                    }
                } else {
                    Button(onClick = { onStart(hasPartialDownload) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            when {
                                hasPartialDownload -> "Lanjutkan unduhan"
                                downloads.any { it.status == DownloadStatus.COMPLETED } -> "Unduh ulang semua"
                                else -> "Unduh semua resource"
                            },
                        )
                    }
                }
            }
            if (downloads.isEmpty()) {
                item {
                    EmptyDownloadState()
                }
            } else {
                item {
                    Text("Status per kitab", style = MaterialTheme.typography.titleMedium)
                }
                items(downloads, key = { it.bookId }) { item ->
                    DownloadBookRow(
                        item = item,
                        onStart = { resume -> onStartBook(item.bookId, resume) },
                        onCancel = { onCancelBook(item.bookId) },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus unduhan?") },
            text = {
                Text("Semua hadits ${item.bookName} yang tersimpan offline akan dihapus dari perangkat.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBook(item.bookId)
                        deleteTarget = null
                    },
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun DownloadBookRow(
    item: DownloadItem,
    onStart: (resume: Boolean) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val icon = when (item.status) {
        DownloadStatus.COMPLETED -> Icons.Outlined.CheckCircle
        DownloadStatus.FAILED -> Icons.Outlined.ErrorOutline
        DownloadStatus.QUEUED, DownloadStatus.CANCELLED -> Icons.Outlined.Schedule
        DownloadStatus.DOWNLOADING -> Icons.Outlined.CloudDownload
    }
    val iconColor = when (item.status) {
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val statusText = when (item.status) {
        DownloadStatus.COMPLETED -> "Tersedia offline"
        DownloadStatus.DOWNLOADING -> "${item.downloaded} / ${item.total} hadits"
        DownloadStatus.QUEUED -> "Menunggu antrean"
        DownloadStatus.CANCELLED -> "Dibatalkan"
        DownloadStatus.FAILED -> item.error ?: "Gagal mengunduh"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(9.dp), tint = iconColor)
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        item.bookName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when (item.status) {
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Hapus unduhan ${item.bookName}")
                        }
                    }
                    DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Outlined.Close, contentDescription = "Batalkan ${item.bookName}")
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        IconButton(onClick = { onStart(item.downloaded > 0) }) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = "Unduh ${item.bookName}")
                        }
                    }
                }
            }
            if (item.status != DownloadStatus.COMPLETED) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text("Belum ada unduhan", style = MaterialTheme.typography.titleMedium)
        Text(
            "Unduh semua resource agar hadits bisa dibaca dan dicari tanpa internet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
