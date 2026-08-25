package org.opennur.hadits.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadModelTest {
    @Test
    fun progressIsCalculatedAndClamped() {
        val item = DownloadItem(
            bookId = "ahmad",
            bookName = "Musnad Ahmad",
            downloaded = 4_500,
            total = 4_305,
            status = DownloadStatus.DOWNLOADING,
        )

        assertEquals(1f, item.progress)
    }
}
