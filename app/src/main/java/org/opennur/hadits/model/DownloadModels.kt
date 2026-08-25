package org.opennur.hadits.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class DownloadItem(
    val bookId: String,
    val bookName: String,
    val downloaded: Int,
    val total: Int,
    val status: DownloadStatus,
    val error: String? = null,
) {
    val progress: Float
        get() = if (total == 0) 0f else (downloaded.toFloat() / total).coerceIn(0f, 1f)
}
