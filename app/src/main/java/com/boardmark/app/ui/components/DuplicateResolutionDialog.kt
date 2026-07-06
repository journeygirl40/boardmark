package com.boardmark.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardmark.app.R
import com.boardmark.app.domain.model.Bookmark
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 同一URLの重複ブックマークを検知した際に表示するダイアログ。
 * 「すべて残す」(以後この組み合わせは無視)と「残す方を選ぶ」(選んだ1件以外を削除)の2択。
 */
@Composable
fun DuplicateResolutionDialog(
    group: List<Bookmark>,
    onKeepAll: () -> Unit,
    onKeepOne: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var showPicker by remember(group) { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.duplicate_detected_title)) },
        text = {
            if (!showPicker) {
                Column {
                    Text(stringResource(R.string.duplicate_detected_message, group.size))
                    Text(
                        text = group.first().url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                Column {
                    Text(stringResource(R.string.duplicate_picker_title))
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(group, key = { it.id }) { bookmark ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            ) {
                                Text(
                                    text = bookmark.title ?: bookmark.url,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                )
                                Text(
                                    text = formatter.format(bookmark.addedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(
                                    onClick = { onKeepOne(bookmark.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(stringResource(R.string.duplicate_keep_this)) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showPicker) {
                TextButton(onClick = { showPicker = true }) {
                    Text(stringResource(R.string.duplicate_choose_one))
                }
            }
        },
        dismissButton = {
            if (!showPicker) {
                TextButton(onClick = onKeepAll) { Text(stringResource(R.string.duplicate_keep_all)) }
            } else {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.delete_confirm_cancel)) }
            }
        },
    )
}
