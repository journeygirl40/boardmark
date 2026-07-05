package com.picsearch.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.picsearch.app.data.remote.OgpFetcher
import com.picsearch.app.data.remote.OgpResult
import com.picsearch.app.domain.model.FetchStatus
import com.picsearch.app.domain.repository.BookmarkRepository
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
        const val MAX_ATTEMPTS = 3
    }
}
