package com.boardmark.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.boardmark.app.R
import com.boardmark.app.util.BrowserApp

@Composable
fun BrowserPickerDialog(
    browsers: List<BrowserApp>,
    showRememberToggle: Boolean,
    onPick: (packageName: String, remember: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPackage by remember { mutableStateOf(browsers.firstOrNull()?.packageName) }
    var rememberChoice by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.browser_picker_title)) },
        text = {
            Column {
                if (browsers.isEmpty()) {
                    Text(stringResource(R.string.browser_picker_none_found))
                }
                browsers.forEach { browser ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedPackage == browser.packageName,
                                onClick = { selectedPackage = browser.packageName },
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(
                            selected = selectedPackage == browser.packageName,
                            onClick = { selectedPackage = browser.packageName },
                        )
                        Image(
                            bitmap = remember(browser.packageName) { browser.icon.toBitmap().asImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).padding(start = 8.dp),
                        )
                        Text(text = browser.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (showRememberToggle && browsers.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberChoice = !rememberChoice }
                            .padding(top = 8.dp),
                    ) {
                        Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
                        Text(
                            text = stringResource(R.string.browser_picker_remember),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedPackage?.let { onPick(it, rememberChoice) } },
                enabled = selectedPackage != null,
            ) { Text(stringResource(R.string.browser_picker_open)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_confirm_cancel)) }
        },
    )
}
