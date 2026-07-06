package com.boardmark.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardmark.app.R
import com.boardmark.app.util.AppLanguage

@Composable
fun LanguagePickerDialog(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_menu_action)) },
        text = {
            Column {
                LanguageRow(
                    label = stringResource(R.string.language_system_default),
                    selected = current == AppLanguage.SYSTEM_DEFAULT,
                    onClick = { onSelect(AppLanguage.SYSTEM_DEFAULT) },
                )
                LanguageRow(
                    label = "日本語",
                    selected = current == AppLanguage.JAPANESE,
                    onClick = { onSelect(AppLanguage.JAPANESE) },
                )
                LanguageRow(
                    label = "English",
                    selected = current == AppLanguage.ENGLISH,
                    onClick = { onSelect(AppLanguage.ENGLISH) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
