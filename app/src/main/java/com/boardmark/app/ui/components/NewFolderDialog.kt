package com.boardmark.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.boardmark.app.R

@Composable
fun NewFolderDialog(
    onConfirm: (name: String, password: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var passwordEnabled by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var showPasswordError by remember { mutableStateOf(false) }

    fun submit() {
        if (name.isBlank()) return
        if (passwordEnabled && password.isBlank()) {
            showPasswordError = true
            return
        }
        onConfirm(name.trim(), if (passwordEnabled) password else null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_new_folder)) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.new_folder_name_hint)) },
                    singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = passwordEnabled,
                        onCheckedChange = {
                            passwordEnabled = it
                            showPasswordError = false
                        },
                    )
                    Text(stringResource(R.string.folder_password_toggle))
                }
                if (passwordEnabled) {
                    TextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showPasswordError = false
                        },
                        placeholder = { Text(stringResource(R.string.folder_password_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = showPasswordError,
                    )
                    if (showPasswordError) {
                        Text(
                            text = stringResource(R.string.folder_password_required_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submit() },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.new_folder_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_confirm_cancel)) }
        },
    )
}
