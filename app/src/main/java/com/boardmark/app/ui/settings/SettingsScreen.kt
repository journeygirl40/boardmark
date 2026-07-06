package com.boardmark.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardmark.app.R
import com.boardmark.app.ads.InterstitialAdManager
import com.boardmark.app.ui.components.BrowserPickerDialog
import com.boardmark.app.ui.components.LanguagePickerDialog
import com.boardmark.app.util.AppLanguage
import com.boardmark.app.util.BookmarkExporter
import com.boardmark.app.util.BrowserResolver
import com.boardmark.app.util.displayLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var languagePickerVisible by remember { mutableStateOf(false) }
    var browserPickerVisible by remember { mutableStateOf(false) }
    var labelManagementVisible by remember { mutableStateOf(false) }
    var defaultBrowserPackage by remember { mutableStateOf(viewModel.getDefaultBrowser()) }
    val isAdFree by viewModel.billingManager.isAdFree.collectAsState()
    val adFreeProductDetails by viewModel.billingManager.productDetails.collectAsState()

    if (labelManagementVisible) {
        LabelManagementScreen(onBack = { labelManagementVisible = false })
        return
    }

    val importHtmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val html = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (html == null) {
                Toast.makeText(context, R.string.toast_import_bookmarks_failed, Toast.LENGTH_SHORT).show()
            } else {
                val count = viewModel.importBookmarksFromHtml(html)
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_import_bookmarks_result, count),
                    Toast.LENGTH_SHORT,
                ).show()
                (context as? Activity)?.let { InterstitialAdManager.maybeShow(it, InterstitialAdManager.Trigger.IMPORT) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.remove_ads_action)) },
                supportingContent = {
                    Text(
                        if (isAdFree) {
                            stringResource(R.string.remove_ads_purchased)
                        } else {
                            adFreeProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                                ?: stringResource(R.string.remove_ads_price_fallback)
                        },
                    )
                },
                leadingContent = {
                    Icon(
                        if (isAdFree) Icons.Filled.CheckCircle else Icons.Filled.Block,
                        contentDescription = null,
                        tint = if (isAdFree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = if (isAdFree) {
                    Modifier
                } else {
                    Modifier.clickable {
                        (context as? Activity)?.let { viewModel.billingManager.launchPurchaseFlow(it) }
                    }
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.label_management_action)) },
                supportingContent = { Text(stringResource(R.string.label_management_summary)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                modifier = Modifier.clickable { labelManagementVisible = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.language_menu_action)) },
                supportingContent = { Text(AppLanguage.current().displayLabel()) },
                leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                modifier = Modifier.clickable { languagePickerVisible = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.default_browser_title)) },
                supportingContent = {
                    Text(
                        defaultBrowserPackage
                            ?.let { BrowserResolver.labelFor(context, it) }
                            ?: stringResource(R.string.default_browser_not_set),
                    )
                },
                leadingContent = { Icon(Icons.Filled.Public, contentDescription = null) },
                modifier = Modifier.clickable { browserPickerVisible = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_bookmarks_html_action)) },
                supportingContent = { Text(stringResource(R.string.import_bookmarks_html_summary)) },
                leadingContent = { Icon(Icons.Filled.Upload, contentDescription = null) },
                modifier = Modifier.clickable { importHtmlLauncher.launch(arrayOf("text/html")) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_bookmarks_html_action)) },
                supportingContent = { Text(stringResource(R.string.export_bookmarks_html_summary)) },
                leadingContent = { Icon(Icons.Filled.Download, contentDescription = null) },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        val html = viewModel.exportBookmarksHtml()
                        val uri = BookmarkExporter.saveHtmlToCacheAndGetUri(context, html)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/html"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                        (context as? Activity)?.let { InterstitialAdManager.maybeShow(it, InterstitialAdManager.Trigger.EXPORT) }
                    }
                },
            )
        }
    }

    if (languagePickerVisible) {
        LanguagePickerDialog(
            current = AppLanguage.current(),
            onSelect = { language ->
                language.apply()
                languagePickerVisible = false
            },
            onDismiss = { languagePickerVisible = false },
        )
    }

    if (browserPickerVisible) {
        BrowserPickerDialog(
            browsers = remember { BrowserResolver.installedBrowsers(context) },
            showRememberToggle = false,
            onPick = { packageName, _ ->
                viewModel.setDefaultBrowser(packageName)
                defaultBrowserPackage = packageName
                browserPickerVisible = false
            },
            onDismiss = { browserPickerVisible = false },
        )
    }
}
