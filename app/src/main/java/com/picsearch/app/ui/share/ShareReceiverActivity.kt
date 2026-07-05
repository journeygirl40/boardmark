package com.picsearch.app.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.picsearch.app.R
import com.picsearch.app.domain.repository.BookmarkRepository
import com.picsearch.app.util.UrlExtractor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject lateinit var repository: BookmarkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val sharedSubject = intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        val url = UrlExtractor.extract(sharedText) ?: UrlExtractor.extract(sharedSubject)

        if (url == null) {
            Toast.makeText(this, R.string.toast_url_not_recognized, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val added = repository.addBookmark(url)
            val messageRes = if (added) R.string.toast_bookmark_added else R.string.toast_bookmark_already_exists
            Toast.makeText(this@ShareReceiverActivity, messageRes, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
