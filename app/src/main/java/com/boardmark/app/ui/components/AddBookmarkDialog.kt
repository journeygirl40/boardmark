package com.boardmark.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.boardmark.app.R
import com.boardmark.app.util.UrlExtractor

/**
 * 共有(Share)を使わず、URLを直接貼り付けてブックマークを追加するためのダイアログ。
 * クリップボードにURLらしき文字列があれば、貼り付けボタンでそのまま流し込める。
 */
@Composable
fun AddBookmarkDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var showInvalidUrlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_bookmark_title)) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        showInvalidUrlError = false
                    },
                    placeholder = { Text(stringResource(R.string.add_bookmark_url_hint)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip(context)
                            if (clipText != null) {
                                text = clipText
                                showInvalidUrlError = false
                            }
                        }) {
                            Icon(
                                Icons.Filled.ContentPaste,
                                contentDescription = stringResource(R.string.add_bookmark_paste_action),
                            )
                        }
                    },
                    singleLine = true,
                    isError = showInvalidUrlError,
                )
                if (showInvalidUrlError) {
                    Text(
                        text = stringResource(R.string.toast_url_not_recognized),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val url = UrlExtractor.extract(text)
                    if (url == null) {
                        showInvalidUrlError = true
                    } else {
                        onConfirm(url)
                    }
                },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.add_bookmark_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_confirm_cancel)) }
        },
    )
}

private fun ClipboardManager.primaryClip(context: Context): String? =
    if (hasPrimaryClip()) primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() else null
