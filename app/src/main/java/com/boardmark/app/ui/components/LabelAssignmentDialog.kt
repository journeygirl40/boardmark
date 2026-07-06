package com.boardmark.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.boardmark.app.R
import com.boardmark.app.domain.model.Label

private enum class LabelTriState { NONE, ALL, SOME }

/**
 * ラベルの一括編集ダイアログ。
 * targetLabelNamesは選択中の各ブックマークが個別に持つラベル名の集合(要素数=選択件数)。
 * 全員が持つラベルはALL、誰も持たなければNONE、一部だけが持つ場合はSOME(タップするまでは
 * どちらとも決めず、個々のブックマークの設定をそのまま保持する)。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelAssignmentDialog(
    allLabels: List<Label>,
    targetLabelNames: List<Set<String>>,
    onConfirm: (addNames: Set<String>, removeNames: Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val totalCount = targetLabelNames.size
    val countByName = remember(targetLabelNames) {
        targetLabelNames.flatten().groupingBy { it }.eachCount()
    }
    var overrides by remember { mutableStateOf<Map<String, LabelTriState>>(emptyMap()) }
    var newLabelText by remember { mutableStateOf("") }

    fun defaultState(name: String): LabelTriState = when (countByName[name] ?: 0) {
        0 -> LabelTriState.NONE
        totalCount -> LabelTriState.ALL
        else -> LabelTriState.SOME
    }
    fun effectiveState(name: String): LabelTriState = overrides[name] ?: defaultState(name)

    val candidateNames = remember(allLabels, countByName, overrides) {
        (allLabels.map { it.name } + countByName.keys + overrides.keys).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.assign_labels_title)) },
        text = {
            Column {
                if (totalCount > 1) {
                    Text(
                        text = stringResource(R.string.label_tristate_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    candidateNames.forEach { name ->
                        val state = effectiveState(name)
                        FilterChip(
                            selected = state == LabelTriState.ALL,
                            onClick = {
                                // 一部だけ設定(SOME)のラベルは ALL -> NONE -> 元の状態(未選択=変更なし)
                                // と一周させる。誤タップしても、そのラベルの個別設定を触らない状態に
                                // 戻せるようにするため。
                                val default = defaultState(name)
                                val cycle = listOf(default, LabelTriState.ALL, LabelTriState.NONE).distinct()
                                val currentIndex = cycle.indexOf(state)
                                val next = cycle[(currentIndex + 1) % cycle.size]
                                overrides = if (next == default) overrides - name else overrides + (name to next)
                            },
                            label = { Text(name) },
                            leadingIcon = when (state) {
                                LabelTriState.ALL -> {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                }
                                LabelTriState.SOME -> {
                                    { Icon(Icons.Filled.HorizontalRule, contentDescription = null) }
                                }
                                LabelTriState.NONE -> null
                            },
                            colors = if (state == LabelTriState.SOME) {
                                FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                )
                            } else {
                                FilterChipDefaults.filterChipColors()
                            },
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    TextField(
                        value = newLabelText,
                        onValueChange = { newLabelText = it },
                        placeholder = { Text(stringResource(R.string.new_label_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            val trimmed = newLabelText.trim()
                            if (trimmed.isNotEmpty()) {
                                overrides = overrides + (trimmed to LabelTriState.ALL)
                                newLabelText = ""
                            }
                        },
                        enabled = newLabelText.isNotBlank(),
                    ) { Text(stringResource(R.string.new_label_add)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val addNames = candidateNames.filter { effectiveState(it) == LabelTriState.ALL }.toSet()
                val removeNames = candidateNames.filter { effectiveState(it) == LabelTriState.NONE }.toSet()
                onConfirm(addNames, removeNames)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_confirm_cancel)) }
        },
    )
}
