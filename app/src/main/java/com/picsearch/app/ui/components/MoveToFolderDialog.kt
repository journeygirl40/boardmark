package com.picsearch.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.picsearch.app.R
import com.picsearch.app.domain.model.Folder

@Composable
fun MoveToFolderDialog(
    folders: List<Folder>,
    onSelectFolder: (Long) -> Unit,
    onSelectTopLevel: () -> Unit,
    onRequestCreateFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_folder_title)) },
        text = {
            Column {
                TextButton(onClick = onSelectTopLevel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.move_to_top_level))
                }
                TextButton(onClick = onRequestCreateFolder, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.move_to_new_folder))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(folders, key = { it.id }) { folder ->
                        TextButton(
                            onClick = { onSelectFolder(folder.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(folder.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_confirm_cancel)) }
        },
    )
}
