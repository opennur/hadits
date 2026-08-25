package com.hikmah.hadits.data

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
            val completed = HadithRepository.getInstance(applicationContext).downloadAllResources()
            if (completed) Result.success() else Result.failure()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "hikmah-download-all"
    }
}
