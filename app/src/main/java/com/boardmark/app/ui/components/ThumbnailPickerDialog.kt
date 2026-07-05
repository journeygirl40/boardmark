package com.boardmark.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ThumbnailPickerDialog(
    loadCandidates: suspend () -> List<String>,
    onSelectCandidate: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onPickFromWebPage: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        candidates = loadCandidates()
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("サムネイルを選択") },
        text = {
            Column {
                OutlinedButton(onClick = onPickFromWebPage, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Crop, contentDescription = null)
                    Text(text = "ページの画面から範囲を選ぶ", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text(text = "端末の写真から選ぶ", modifier = Modifier.padding(start = 8.dp))
                }

                Text(
                    text = "ページ内の画像から選ぶ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 320.dp)) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        candidates.isEmpty() -> {
                            Text(
                                text = "ページから画像が見つかりませんでした",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            )
                        }
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                            ) {
                                items(candidates) { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { onSelectCandidate(imageUrl) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
    )
}
