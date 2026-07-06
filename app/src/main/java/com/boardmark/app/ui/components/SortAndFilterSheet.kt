package com.boardmark.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.boardmark.app.ui.list.SortCriterion
import com.boardmark.app.ui.list.SortDirection
import com.boardmark.app.ui.list.SortField

/** 並べ替え専用のシート。ラベルによる絞り込みは検索バー直下のチップ行に統合されている。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortAndFilterSheet(
    sortCriteria: List<SortCriterion>,
    isManualOrder: Boolean,
    onSortCriteriaChange: (List<SortCriterion>) -> Unit,
    onDismiss: () -> Unit,
) {
    var addFieldMenuExpanded by remember { mutableStateOf(false) }
    val usedFields = sortCriteria.map { it.field }.toSet()
    val availableFields = SortField.entries.filterNot { it in usedFields }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.sort_section_title), style = MaterialTheme.typography.titleMedium)

            if (isManualOrder) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.sort_manual_active_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onSortCriteriaChange(sortCriteria) }) {
                        Text(stringResource(R.string.sort_manual_reset))
                    }
                }
            }

            sortCriteria.forEachIndexed { index, criterion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(text = criterion.field.displayLabel(), modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val next = criterion.copy(
                                direction = if (criterion.direction == SortDirection.ASC) {
                                    SortDirection.DESC
                                } else {
                                    SortDirection.ASC
                                },
                            )
                            onSortCriteriaChange(sortCriteria.toMutableList().also { it[index] = next })
                        },
                    ) {
                        Icon(
                            if (criterion.direction == SortDirection.ASC) {
                                Icons.Filled.ArrowUpward
                            } else {
                                Icons.Filled.ArrowDownward
                            },
                            contentDescription = stringResource(
                                if (criterion.direction == SortDirection.ASC) {
                                    R.string.sort_direction_asc
                                } else {
                                    R.string.sort_direction_desc
                                },
                            ),
                        )
                    }
                    IconButton(
                        enabled = index > 0,
                        onClick = {
                            val reordered = sortCriteria.toMutableList()
                            reordered[index] = sortCriteria[index - 1]
                            reordered[index - 1] = criterion
                            onSortCriteriaChange(reordered)
                        },
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.action_move_up))
                    }
                    IconButton(
                        enabled = index < sortCriteria.lastIndex,
                        onClick = {
                            val reordered = sortCriteria.toMutableList()
                            reordered[index] = sortCriteria[index + 1]
                            reordered[index + 1] = criterion
                            onSortCriteriaChange(reordered)
                        },
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.action_move_down))
                    }
                    IconButton(onClick = { onSortCriteriaChange(sortCriteria.filterIndexed { i, _ -> i != index }) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove))
                    }
                }
            }

            if (availableFields.isNotEmpty()) {
                Box {
                    OutlinedButton(
                        onClick = { addFieldMenuExpanded = true },
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    ) {
                        Text(stringResource(R.string.sort_add_criterion))
                    }
                    DropdownMenu(
                        expanded = addFieldMenuExpanded,
                        onDismissRequest = { addFieldMenuExpanded = false },
                    ) {
                        availableFields.forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.displayLabel()) },
                                onClick = {
                                    onSortCriteriaChange(sortCriteria + SortCriterion(field, SortDirection.DESC))
                                    addFieldMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SortField.displayLabel(): String = when (this) {
    SortField.DATE -> stringResource(R.string.sort_field_date)
    SortField.TITLE -> stringResource(R.string.sort_field_title)
    SortField.VIEW_COUNT -> stringResource(R.string.sort_field_view_count)
    SortField.DOMAIN -> stringResource(R.string.sort_field_domain)
}
