package org.opennur.hadits.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

class DownloadAllWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val repository = HadithRepository.getInstance(applicationContext)
            val bookId = inputData.getString(BOOK_ID_KEY)
            val completed = if (bookId == null) {
                repository.downloadAllResources()
            } else {
                repository.downloadBookResources(bookId)
            }
            if (completed) Result.success() else Result.failure()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val BOOK_ID_KEY = "book_id"
        private const val WORK_NAME_PREFIX = "hikmah-download-book-"

        fun workName(bookId: String): String = "$WORK_NAME_PREFIX$bookId"
    }
}
