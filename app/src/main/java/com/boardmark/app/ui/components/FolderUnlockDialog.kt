package com.boardmark.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.boardmark.app.R

@Composable
fun FolderUnlockDialog(
    folderName: String,
    isError: Boolean,
    onPasswordChange: () -> Unit,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_locked_title, folderName)) },
        text = {
            Column {
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onPasswordChange()
                    },
                    placeholder = { Text(stringResource(R.string.folder_password_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError,
                )
                if (isError) {
                    Text(
                        text = stringResource(R.string.folder_unlock_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (password.isNotBlank()) onConfirm(password) },
                enabled = password.isNotBlank(),
            ) { Text(stringResource(R.string.folder_unlock_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_confirm_cancel)) }
        },
    )
}
