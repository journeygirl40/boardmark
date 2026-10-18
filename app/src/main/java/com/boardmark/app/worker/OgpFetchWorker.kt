package com.boardmark.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.boardmark.app.data.remote.OgpFetcher
import com.boardmark.app.data.remote.OgpResult
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.repository.BookmarkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OgpFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ogpFetcher: OgpFetcher,
    private val repository: BookmarkRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val bookmarkId = inputData.getLong(KEY_BOOKMARK_ID, -1L)
        val url = inputData.getString(KEY_URL)
        if (bookmarkId == -1L || url == null) return Result.failure()

        return when (val ogpResult = ogpFetcher.fetch(url)) {
            is OgpResult.Success -> {
                repository.updateFetchResult(
                    bookmarkId = bookmarkId,
                    status = FetchStatus.SUCCESS,
                    finalUrl = ogpResult.finalUrl,
                    title = ogpResult.title,
                    ogImageUrl = ogpResult.imageUrl,
                    faviconUrl = ogpResult.faviconUrl,
                    description = ogpResult.description,
                    siteName = ogpResult.siteName,
                )
                Result.success()
            }
            is OgpResult.Failure -> {
                if (runAttemptCount < MAX_ATTEMPTS - 1) {
                    Result.retry()
                } else {
                    repository.updateFetchResult(
                        bookmarkId = bookmarkId,
                        status = FetchStatus.FAILED,
                        finalUrl = null,
                        title = null,
                        ogImageUrl = null,
                        faviconUrl = null,
                    )
                    Result.success(workDataOf(KEY_BOOKMARK_ID to bookmarkId))
                }
            }
        }
    }

    companion object {
        const val KEY_BOOKMARK_ID = "bookmark_id"
        const val KEY_URL = "url"
        // 共有直後はアプリ切り替えで通信が不安定なことが多いため、間隔を広げながら
        // 数回やり直す(バックオフはBookmarkRepositoryImpl.enqueueOgpFetchで設定)。
        const val MAX_ATTEMPTS = 5
    }
}
