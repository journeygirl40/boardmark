package com.boardmark.app.ui.settings

import androidx.lifecycle.ViewModel
import com.boardmark.app.billing.BillingManager
import com.boardmark.app.data.local.BrowserPreference
import com.boardmark.app.domain.repository.BookmarkRepository
import com.boardmark.app.util.BookmarkExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val browserPreference: BrowserPreference,
    private val repository: BookmarkRepository,
    val billingManager: BillingManager,
) : ViewModel() {

    fun getDefaultBrowser(): String? = browserPreference.get()

    fun setDefaultBrowser(packageName: String?) {
        browserPreference.set(packageName)
    }

    suspend fun exportBookmarksHtml(): String {
        val (bookmarks, folders) = repository.getAllForExport()
        return BookmarkExporter.toNetscapeHtml(bookmarks, folders)
    }

    suspend fun importBookmarksFromHtml(html: String): Int = repository.importBookmarksFromHtml(html)
}
