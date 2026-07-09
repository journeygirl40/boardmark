package com.boardmark.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.boardmark.app.billing.BillingManager
import com.boardmark.app.data.local.BrowserPreference
import com.boardmark.app.domain.repository.BookmarkRepository
import com.boardmark.app.util.BookmarkExporter
import com.boardmark.app.util.FullBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import java.io.OutputStream
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

    /** ラベル・並び順・サムネイル実体まで含めた完全バックアップをZIPとして書き出す。 */
    suspend fun exportFullBackup(output: OutputStream) {
        val snapshot = repository.getFullBackupSnapshot()
        FullBackup.write(snapshot, output)
    }

    /** 完全バックアップのZIPを取り込む。戻り値は実際に追加したブックマーク件数。 */
    suspend fun restoreFullBackup(context: Context, input: InputStream): Int {
        val snapshot = FullBackup.read(context, input)
        return repository.restoreFullBackup(snapshot)
    }
}
