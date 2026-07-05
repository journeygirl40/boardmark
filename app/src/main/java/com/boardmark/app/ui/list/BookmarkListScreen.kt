package com.boardmark.app.ui.list

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardmark.app.R
import com.boardmark.app.ads.BannerAd
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.ui.components.BookmarkCard
import com.boardmark.app.ui.components.FolderTile
import com.boardmark.app.ui.components.MoveToFolderDialog
import com.boardmark.app.ui.components.NewFolderDialog
import com.boardmark.app.ui.components.RenameFolderDialog
import com.boardmark.app.ui.components.ThumbnailPickerDialog
import com.boardmark.app.ui.components.WebThumbnailCaptureScreen
import com.boardmark.app.util.BookmarkExporter
import com.boardmark.app.util.FolderCollageRenderer
import com.boardmark.app.util.LocalImageStore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class FolderTarget(val id: Long, val name: String)

private const val FOLDER_COLLAGE_MAX_TILES = 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkListScreen(viewModel: BookmarkListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val screenCoroutineScope = rememberCoroutineScope()

    var pendingDeleteSelection by remember { mutableStateOf(false) }
    var moveDialogVisible by remember { mutableStateOf(false) }
    var newFolderDialogVisible by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<FolderTarget?>(null) }
    var renameFolderTarget by remember { mutableStateOf<FolderTarget?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<FolderTarget?>(null) }
    var thumbnailPickerBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var webCaptureBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sizeMenuExpanded by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val bookmark = thumbnailPickerBookmark
        if (uri != null && bookmark != null) {
            val savedPath = LocalImageStore.saveToInternalStorage(context, uri)
            if (savedPath != null) viewModel.onSetManualThumbnail(bookmark, savedPath)
        }
        thumbnailPickerBookmark = null
    }

    BackHandler(enabled = uiState.isSelectionMode) { viewModel.clearSelection() }
    BackHandler(enabled = selectedFolder != null) { selectedFolder = null }
    BackHandler(
        enabled = !uiState.isSelectionMode && selectedFolder == null && uiState.currentFolderId != null,
    ) { viewModel.onExitFolder() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (uiState.isSelectionMode) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.selection_count, uiState.selectedIds.size)) },
                        navigationIcon = {
                            IconButton(onClick = viewModel::clearSelection) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        },
                        actions = {
                            IconButton(onClick = { moveDialogVisible = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = stringResource(R.string.action_move),
                                )
                            }
                            IconButton(onClick = { pendingDeleteSelection = true }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete_confirm_ok),
                                )
                            }
                            if (uiState.selectedIds.size == 1) {
                                IconButton(onClick = {
                                    val id = uiState.selectedIds.single()
                                    thumbnailPickerBookmark = uiState.gridItems
                                        .filterIsInstance<BookmarkGridItem.BookmarkItem>()
                                        .firstOrNull { it.bookmark.id == id }?.bookmark
                                }) {
                                    Icon(
                                        Icons.Filled.Image,
                                        contentDescription = stringResource(R.string.choose_thumbnail),
                                    )
                                }
                            }
                        },
                    )
                } else if (selectedFolder != null) {
                    val target = selectedFolder!!
                    TopAppBar(
                        title = { Text(target.name) },
                        navigationIcon = {
                            IconButton(onClick = { selectedFolder = null }) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                val folderId = target.id
                                selectedFolder = null
                                screenCoroutineScope.launch {
                                    val imageUrls = viewModel.getFolderThumbnailUrls(folderId, FOLDER_COLLAGE_MAX_TILES)
                                    val uri = FolderCollageRenderer.renderAndSaveCollage(context, imageUrls)
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/jpeg"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.folder_collage_no_images,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.share_folder_as_image_action),
                                )
                            }
                            IconButton(onClick = {
                                renameFolderTarget = target
                                selectedFolder = null
                            }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.rename_folder_action),
                                )
                            }
                            IconButton(onClick = {
                                deleteFolderTarget = target
                                selectedFolder = null
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete_folder_action),
                                )
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = {
                            TextField(
                                value = uiState.query,
                                onValueChange = viewModel::onQueryChange,
                                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        navigationIcon = {
                            if (uiState.currentFolderId != null) {
                                IconButton(onClick = viewModel::onExitFolder) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                screenCoroutineScope.launch {
                                    val json = viewModel.exportBookmarksJson()
                                    val uri = BookmarkExporter.saveToCacheAndGetUri(context, json)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = stringResource(R.string.export_bookmarks_action),
                                )
                            }
                            Box {
                                IconButton(onClick = { sizeMenuExpanded = true }) {
                                    Icon(Icons.Filled.PhotoSizeSelectLarge, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = sizeMenuExpanded,
                                    onDismissRequest = { sizeMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.thumbnail_size_small)) },
                                        onClick = {
                                            viewModel.onThumbnailSizeChange(ThumbnailSize.SMALL)
                                            sizeMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.thumbnail_size_medium)) },
                                        onClick = {
                                            viewModel.onThumbnailSizeChange(ThumbnailSize.MEDIUM)
                                            sizeMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.thumbnail_size_large)) },
                                        onClick = {
                                            viewModel.onThumbnailSizeChange(ThumbnailSize.LARGE)
                                            sizeMenuExpanded = false
                                        },
                                    )
                                }
                            }
                            Box {
                                IconButton(onClick = { sortMenuExpanded = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_date_desc)) },
                                        onClick = {
                                            viewModel.onSortOrderChange(SortOrder.DATE_DESC)
                                            sortMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_name_asc)) },
                                        onClick = {
                                            viewModel.onSortOrderChange(SortOrder.NAME_ASC)
                                            sortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            },
            bottomBar = { BannerAd() },
        ) { padding ->
            if (uiState.gridItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.empty_state_message),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                val gridState = rememberLazyStaggeredGridState()
                val currentItems by rememberUpdatedState(uiState.gridItems)
                val selectionModeState by rememberUpdatedState(uiState.isSelectionMode)

                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(minSize = uiState.thumbnailSize.minWidthDp.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 16.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .pointerInput(Unit) {
                            coroutineScope {
                                // タップ: フォルダを開く / URLを開く / 選択トグル。
                                // 途中でスクロールや長押しドラッグ側にconsumeされた場合はonTapが
                                // 発火しないため、素早いスワイプでは呼ばれない。
                                launch {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            if (selectedFolder != null) {
                                                selectedFolder = null
                                            } else {
                                                when (val hitItem = hitTestItem(gridState, currentItems, offset)) {
                                                    is BookmarkGridItem.FolderItem -> {
                                                        if (!selectionModeState) {
                                                            viewModel.onEnterFolder(hitItem.data.folder.id)
                                                        }
                                                    }
                                                    is BookmarkGridItem.BookmarkItem -> {
                                                        if (selectionModeState) {
                                                            viewModel.onToggleSelection(hitItem.bookmark.id)
                                                        } else {
                                                            CustomTabsIntent.Builder().build().launchUrl(
                                                                context,
                                                                android.net.Uri.parse(hitItem.bookmark.url),
                                                            )
                                                        }
                                                    }
                                                    null -> {}
                                                }
                                            }
                                        },
                                    )
                                }

                                // 長押し→ドラッグで複数選択(ブックマーク)、またはフォルダの長押しで
                                // フォルダ操作ダイアログ(名前変更/削除)を開く。タイムアウト前に
                                // タッチスロップを超えて移動する（グリッドのスクロールに取られる）
                                // 場合はonDragStartが発火しないため、スワイプが誤検出されることがない。
                                launch {
                                    var isBookmarkDrag = false
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            when (val hitItem = hitTestItem(gridState, currentItems, offset)) {
                                                is BookmarkGridItem.BookmarkItem -> {
                                                    isBookmarkDrag = true
                                                    selectedFolder = null
                                                    viewModel.onSelectionLongPressStart(hitItem.bookmark.id)
                                                }
                                                is BookmarkGridItem.FolderItem -> {
                                                    isBookmarkDrag = false
                                                    viewModel.clearSelection()
                                                    selectedFolder = FolderTarget(
                                                        hitItem.data.folder.id,
                                                        hitItem.data.folder.name,
                                                    )
                                                }
                                                null -> isBookmarkDrag = false
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            if (isBookmarkDrag) {
                                                val dragHit = hitTestItem(gridState, currentItems, change.position)
                                                if (dragHit is BookmarkGridItem.BookmarkItem) {
                                                    viewModel.onSelectionDragOver(dragHit.bookmark.id)
                                                }
                                            }
                                        },
                                        onDragEnd = { if (isBookmarkDrag) viewModel.onSelectionDragEnd() },
                                        onDragCancel = { if (isBookmarkDrag) viewModel.onSelectionDragEnd() },
                                    )
                                }
                            }
                        },
                ) {
                    items(uiState.gridItems, key = { it.key }) { item ->
                        when (item) {
                            is BookmarkGridItem.FolderItem -> FolderTile(
                                data = item.data,
                                isSelected = item.data.folder.id == selectedFolder?.id,
                            )
                            is BookmarkGridItem.BookmarkItem -> BookmarkCard(
                                bookmark = item.bookmark,
                                isSelected = item.bookmark.id in uiState.selectedIds,
                                selectionMode = uiState.isSelectionMode,
                            )
                        }
                    }
                }
            }
        }

        if (pendingDeleteSelection) {
            AlertDialog(
                onDismissRequest = { pendingDeleteSelection = false },
                title = { Text(stringResource(R.string.delete_confirm_title)) },
                text = { Text(stringResource(R.string.delete_selection_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onDeleteSelection()
                        pendingDeleteSelection = false
                    }) { Text(stringResource(R.string.delete_confirm_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteSelection = false }) {
                        Text(stringResource(R.string.delete_confirm_cancel))
                    }
                },
            )
        }

        if (moveDialogVisible) {
            MoveToFolderDialog(
                folders = uiState.allFolders,
                onSelectFolder = { folderId ->
                    viewModel.onMoveSelectionTo(folderId)
                    moveDialogVisible = false
                },
                onSelectTopLevel = {
                    viewModel.onMoveSelectionTo(null)
                    moveDialogVisible = false
                },
                onRequestCreateFolder = {
                    moveDialogVisible = false
                    newFolderDialogVisible = true
                },
                onDismiss = { moveDialogVisible = false },
            )
        }

        if (newFolderDialogVisible) {
            NewFolderDialog(
                onConfirm = { name ->
                    viewModel.onCreateFolderAndMoveSelection(name)
                    newFolderDialogVisible = false
                },
                onDismiss = { newFolderDialogVisible = false },
            )
        }

        deleteFolderTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteFolderTarget = null },
                title = { Text(stringResource(R.string.delete_folder_confirm_title)) },
                text = { Text(stringResource(R.string.delete_folder_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onDeleteFolder(target.id)
                        deleteFolderTarget = null
                    }) { Text(stringResource(R.string.delete_confirm_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteFolderTarget = null }) {
                        Text(stringResource(R.string.delete_confirm_cancel))
                    }
                },
            )
        }

        renameFolderTarget?.let { target ->
            RenameFolderDialog(
                currentName = target.name,
                onConfirm = { name ->
                    viewModel.onRenameFolder(target.id, name)
                    renameFolderTarget = null
                },
                onDismiss = { renameFolderTarget = null },
            )
        }

        thumbnailPickerBookmark?.let { bookmark ->
            ThumbnailPickerDialog(
                loadCandidates = { viewModel.fetchCandidateImages(bookmark) },
                onSelectCandidate = { imageUrl ->
                    viewModel.onSetManualThumbnail(bookmark, imageUrl)
                    thumbnailPickerBookmark = null
                },
                onPickFromGallery = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onPickFromWebPage = {
                    webCaptureBookmark = bookmark
                    thumbnailPickerBookmark = null
                },
                onDismiss = { thumbnailPickerBookmark = null },
            )
        }

        webCaptureBookmark?.let { bookmark ->
            WebThumbnailCaptureScreen(
                url = bookmark.url,
                onCaptured = { bitmap ->
                    val savedPath = LocalImageStore.saveBitmapToInternalStorage(context, bitmap)
                    if (savedPath != null) viewModel.onSetManualThumbnail(bookmark, savedPath)
                    webCaptureBookmark = null
                },
                onDismiss = { webCaptureBookmark = null },
            )
        }
    }
}

private fun hitTestItem(
    gridState: LazyStaggeredGridState,
    items: List<BookmarkGridItem>,
    position: Offset,
): BookmarkGridItem? {
    val hit = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
        val left = info.offset.x.toFloat()
        val top = info.offset.y.toFloat()
        position.x in left..(left + info.size.width) &&
            position.y in top..(top + info.size.height)
    } ?: return null

    val key = hit.key as? String ?: return null
    return items.firstOrNull { it.key == key }
}
