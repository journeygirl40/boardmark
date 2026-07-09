package com.boardmark.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardmark.app.R
import com.boardmark.app.ui.components.RenameLabelDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelManagementScreen(
    onBack: () -> Unit,
    onSelectLabel: (Long) -> Unit,
    viewModel: LabelManagementViewModel = hiltViewModel(),
) {
    val items by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var createDialogVisible by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<LabelWithCount?>(null) }
    var deleteTarget by remember { mutableStateOf<LabelWithCount?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.label_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { createDialogVisible = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_label_action))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChange(it)
                },
                placeholder = { Text(stringResource(R.string.label_management_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            viewModel.onQueryChange("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_search))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.label_management_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.label.id }) { item ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    item.label.name,
                                    modifier = Modifier.clickable { onSelectLabel(item.label.id) },
                                )
                            },
                            supportingContent = {
                                Text(stringResource(R.string.folder_item_count, item.bookmarkCount))
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { renameTarget = item }) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.rename_label_action),
                                        )
                                    }
                                    IconButton(onClick = { deleteTarget = item }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.delete_label_action),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (createDialogVisible) {
        RenameLabelDialog(
            currentName = "",
            title = stringResource(R.string.add_label_title),
            onConfirm = { newName ->
                viewModel.onCreate(newName)
                createDialogVisible = false
            },
            onDismiss = { createDialogVisible = false },
        )
    }

    renameTarget?.let { item ->
        RenameLabelDialog(
            currentName = item.label.name,
            onConfirm = { newName ->
                viewModel.onRename(item.label.id, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_label_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_label_confirm_message,
                        item.label.name,
                        item.bookmarkCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDelete(item.label.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete_confirm_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.delete_confirm_cancel))
                }
            },
        )
    }
}
