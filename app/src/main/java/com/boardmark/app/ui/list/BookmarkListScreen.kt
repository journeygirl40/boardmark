package com.boardmark.app.ui.list

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.boardmark.app.R
import com.boardmark.app.ads.BannerAd
import com.boardmark.app.ads.InterstitialAdManager
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.Label
import com.boardmark.app.ui.components.BookmarkCard
import com.boardmark.app.ui.components.BrowserPickerDialog
import com.boardmark.app.ui.components.DuplicateResolutionDialog
import com.boardmark.app.ui.components.FolderTile
import com.boardmark.app.ui.components.MoreActionsSheet
import com.boardmark.app.ui.components.MoveToFolderDialog
import com.boardmark.app.ui.components.NewFolderDialog
import com.boardmark.app.ui.components.RenameBookmarkDialog
import com.boardmark.app.ui.components.LabelAssignmentDialog
import com.boardmark.app.ui.components.RenameFolderDialog
import com.boardmark.app.ui.components.SortAndFilterSheet
import com.boardmark.app.ui.components.ThumbnailPickerDialog
import com.boardmark.app.ui.components.WebThumbnailCaptureScreen
import com.boardmark.app.util.BrowserResolver
import com.boardmark.app.util.LocalImageStore
import com.boardmark.app.util.domainOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class FolderTarget(val id: Long, val name: String)

private data class ReorderDrag(
    val draggedIds: Set<Long>,
    val startOffset: Offset,
    val currentOffset: Offset,
    val hoverTargetId: Long? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkListScreen(
    onOpenSettings: () -> Unit,
    viewModel: BookmarkListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val thumbnailFetchProgress by viewModel.thumbnailFetchProgress.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var pendingDeleteSelection by remember { mutableStateOf(false) }
    var moveDialogVisible by remember { mutableStateOf(false) }
    var newFolderDialogVisible by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<FolderTarget?>(null) }
    var renameFolderTarget by remember { mutableStateOf<FolderTarget?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<FolderTarget?>(null) }
    var thumbnailPickerBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var renameBookmarkTarget by remember { mutableStateOf<Bookmark?>(null) }
    var webCaptureBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var sizeMenuExpanded by remember { mutableStateOf(false) }
    var moreSheetVisible by remember { mutableStateOf(false) }
    var sortFilterSheetVisible by remember { mutableStateOf(false) }
    var labelDialogTargetIds by remember { mutableStateOf<Set<Long>?>(null) }
    var pendingAutoThumbnailIds by remember { mutableStateOf<Set<Long>?>(null) }
    var reorderDrag by remember { mutableStateOf<ReorderDrag?>(null) }
    var pendingOpenBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var folderBrowserPickerTarget by remember { mutableStateOf<FolderTarget?>(null) }
    var searchFieldFocused by remember { mutableStateOf(false) }

    val adCoroutineScope = rememberCoroutineScope()
    // サムネイル更新開始の2秒後に、件数に応じた確率でインタースティシャル広告を出す。
    // itemCountが10件以上(複数選択の一括更新)の場合はほぼ確定表示になる。
    fun maybeShowThumbnailUpdateAd(itemCount: Int) {
        val activity = context as? Activity ?: return
        adCoroutineScope.launch {
            delay(2000)
            InterstitialAdManager.maybeShow(
                activity,
                InterstitialAdManager.Trigger.THUMBNAIL_UPDATE,
                itemCount = itemCount,
            )
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val bookmark = thumbnailPickerBookmark
        if (uri != null && bookmark != null) {
            val savedPath = LocalImageStore.saveToInternalStorage(context, uri)
            if (savedPath != null) {
                viewModel.onSetManualThumbnail(bookmark, savedPath)
                maybeShowThumbnailUpdateAd(1)
            }
        }
        thumbnailPickerBookmark = null
    }

    // アプリ起動時(他アプリからの復帰を含む)の広告表示はBoardmarkApplication側で
    // ProcessLifecycleOwnerを使って一元管理しているため、ここでは呼び出さない。

    BackHandler(enabled = uiState.isSelectionMode) { viewModel.clearSelection() }
    BackHandler(enabled = selectedFolder != null) { selectedFolder = null }
    BackHandler(
        enabled = !uiState.isSelectionMode && selectedFolder == null && uiState.currentFolderId != null,
    ) { viewModel.onExitFolder() }

    // アプリがバックグラウンドに回るたびに検索欄のフォーカスが残っていると、
    // 復帰時にIMEが自動的に再表示されてしまうため、ON_STOPで明示的に外す。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 検索欄+ラベル絞り込みチップは「検索」という一つの操作域として扱い、そこへのタップは
    // フォーカスを外さない。一覧(グリッド/空状態)側をタップしたときだけフォーカスを外す
    // (Modifier.clearFocusOnTapとして両方の内容分岐で共有する)。
    fun Modifier.clearFocusOnTap(): Modifier = this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            focusManager.clearFocus()
        }
    }

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
                            IconButton(onClick = viewModel::onSelectAll) {
                                Icon(
                                    Icons.Filled.SelectAll,
                                    contentDescription = stringResource(R.string.action_select_all),
                                )
                            }
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
                            IconButton(onClick = {
                                val ids = uiState.selectedIds
                                if (ids.size == 1) {
                                    val id = ids.single()
                                    thumbnailPickerBookmark = uiState.gridItems
                                        .filterIsInstance<BookmarkGridItem.BookmarkItem>()
                                        .firstOrNull { it.bookmark.id == id }?.bookmark
                                } else {
                                    pendingAutoThumbnailIds = ids
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = stringResource(R.string.choose_thumbnail),
                                )
                            }
                            if (uiState.selectedIds.size == 1) {
                                IconButton(onClick = {
                                    val id = uiState.selectedIds.single()
                                    renameBookmarkTarget = uiState.gridItems
                                        .filterIsInstance<BookmarkGridItem.BookmarkItem>()
                                        .firstOrNull { it.bookmark.id == id }?.bookmark
                                }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.rename_bookmark_action),
                                    )
                                }
                            }
                            IconButton(onClick = { labelDialogTargetIds = uiState.selectedIds }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Label,
                                    contentDescription = stringResource(R.string.assign_labels_action),
                                )
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
                    Column {
                        TopAppBar(
                        title = {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TextField(
                                    value = uiState.query,
                                    onValueChange = viewModel::onQueryChange,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Search,
                                            contentDescription = stringResource(R.string.search_placeholder),
                                        )
                                    },
                                    trailingIcon = {
                                        if (uiState.query.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = stringResource(R.string.action_clear_search),
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { searchFieldFocused = it.isFocused },
                                )
                            }
                        },
                        navigationIcon = {
                            if (uiState.currentFolderId != null) {
                                IconButton(onClick = viewModel::onExitFolder) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { sizeMenuExpanded = true }) {
                                    Icon(Icons.Filled.PhotoSizeSelectLarge, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = sizeMenuExpanded,
                                    onDismissRequest = { sizeMenuExpanded = false },
                                ) {
                                    Column(modifier = Modifier.width(260.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text(
                                            text = stringResource(R.string.thumbnail_size_label),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                        Slider(
                                            value = uiState.thumbnailSizeLevel.toFloat(),
                                            valueRange = THUMBNAIL_SIZE_LEVEL_MIN.toFloat()..THUMBNAIL_SIZE_LEVEL_MAX.toFloat(),
                                            steps = THUMBNAIL_SIZE_LEVEL_MAX - THUMBNAIL_SIZE_LEVEL_MIN - 1,
                                            onValueChange = { viewModel.onThumbnailSizeChange(it.roundToInt()) },
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { sortFilterSheetVisible = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                            }
                            IconButton(onClick = { moreSheetVisible = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = null)
                            }
                        },
                        )
                        AnimatedVisibility(
                            visible = uiState.allLabels.isNotEmpty() &&
                                (searchFieldFocused || uiState.query.isNotEmpty() || uiState.activeLabelFilter.isNotEmpty()),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            LabelFilterRow(
                                labels = uiState.allLabels,
                                activeIds = uiState.activeLabelFilter,
                                onToggle = viewModel::onToggleLabelFilter,
                            )
                        }
                        val folderId = uiState.currentFolderId
                        if (folderId != null) {
                            val folderName = uiState.currentFolderName.orEmpty()
                            val browserLabel = viewModel.resolveEffectiveBrowser()
                                ?.let { BrowserResolver.labelFor(context, it) }
                                ?: stringResource(R.string.default_browser_not_set)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            renameFolderTarget = FolderTarget(folderId, folderName)
                                        },
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        folderBrowserPickerTarget = FolderTarget(folderId, folderName)
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Public,
                                        contentDescription = stringResource(R.string.folder_default_browser_action),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = browserLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = { BannerAd() },
        ) { padding ->
            if (uiState.gridItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).clearFocusOnTap(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_state_message),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                val gridState = rememberLazyGridState()
                val currentItems by rememberUpdatedState(uiState.gridItems)
                val selectionModeState by rememberUpdatedState(uiState.isSelectionMode)
                val selectedIdsState by rememberUpdatedState(uiState.selectedIds)

                LazyVerticalGrid(
                    state = gridState,
                    // 列数を直接指定するFixedを使う(余白は出ず、スライダーの4段階が
                    // そのまま列数1〜4に対応するため、動かして効果のない中間値が存在しない)。
                    // StaggeredGridではなく通常のGridを使うことで、カードの高さが
                    // 揃っていない場合でも必ずZ字(行優先)の並び順になる。
                    columns = GridCells.Fixed(thumbnailColumnsForLevel(uiState.thumbnailSizeLevel)),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .clearFocusOnTap()
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
                                                            viewModel.onBookmarkOpened(hitItem.bookmark.id)
                                                            val resolvedBrowser = viewModel.resolveEffectiveBrowser()
                                                            if (resolvedBrowser != null) {
                                                                BrowserResolver.openUrl(
                                                                    context,
                                                                    hitItem.bookmark.url,
                                                                    resolvedBrowser,
                                                                )
                                                            } else {
                                                                pendingOpenBookmark = hitItem.bookmark
                                                            }
                                                        }
                                                    }
                                                    null -> {}
                                                }
                                            }
                                        },
                                    )
                                }

                                // 長押し→ドラッグで複数選択(ブックマーク)、またはフォルダの長押しで
                                // フォルダ操作ダイアログ(名前変更/削除)を開く。既に選択済みの
                                // ブックマーク(1件でも複数件でも)を長押しドラッグした場合は、選択拡張
                                // ではなく並び替えドラッグに切り替え、選択中の全件をまとめて動かす。
                                // タイムアウト前にタッチスロップを超えて移動する（グリッドのスクロールに
                                // 取られる）場合はonDragStartが発火しないため、スワイプが誤検出される
                                // ことがない。
                                launch {
                                    var isExtendingSelection = false
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            when (val hitItem = hitTestItem(gridState, currentItems, offset)) {
                                                is BookmarkGridItem.BookmarkItem -> {
                                                    if (selectionModeState &&
                                                        hitItem.bookmark.id in selectedIdsState
                                                    ) {
                                                        isExtendingSelection = false
                                                        reorderDrag = ReorderDrag(
                                                            draggedIds = selectedIdsState,
                                                            startOffset = offset,
                                                            currentOffset = offset,
                                                        )
                                                    } else {
                                                        isExtendingSelection = true
                                                        selectedFolder = null
                                                        viewModel.onSelectionLongPressStart(hitItem.bookmark.id)
                                                    }
                                                }
                                                is BookmarkGridItem.FolderItem -> {
                                                    isExtendingSelection = false
                                                    viewModel.clearSelection()
                                                    selectedFolder = FolderTarget(
                                                        hitItem.data.folder.id,
                                                        hitItem.data.folder.name,
                                                    )
                                                }
                                                null -> isExtendingSelection = false
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            if (isExtendingSelection) {
                                                val dragHit = hitTestItem(gridState, currentItems, change.position)
                                                if (dragHit is BookmarkGridItem.BookmarkItem) {
                                                    viewModel.onSelectionDragOver(dragHit.bookmark.id)
                                                }
                                            } else {
                                                reorderDrag = reorderDrag?.let { drag ->
                                                    val hoverHit = hitTestItem(gridState, currentItems, change.position)
                                                    val hoverTarget = (hoverHit as? BookmarkGridItem.BookmarkItem)
                                                        ?.bookmark?.id
                                                        ?.takeIf { it !in drag.draggedIds }
                                                    drag.copy(currentOffset = change.position, hoverTargetId = hoverTarget)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isExtendingSelection) {
                                                viewModel.onSelectionDragEnd()
                                            } else {
                                                reorderDrag?.let { drag ->
                                                    drag.hoverTargetId?.let { target ->
                                                        viewModel.onReorderBookmarks(drag.draggedIds, target)
                                                    }
                                                }
                                                reorderDrag = null
                                            }
                                        },
                                        onDragCancel = {
                                            if (isExtendingSelection) viewModel.onSelectionDragEnd() else reorderDrag = null
                                        },
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
                                // 検索/ラベル絞り込みの切り替わりで一覧が入れ替わる様子自体を
                                // 「フィルタされている」ことの表現として使うため、出現・消失・
                                // 並び替えをアニメーションさせる。
                                modifier = Modifier.animateItem(),
                            )
                            is BookmarkGridItem.BookmarkItem -> {
                                val drag = reorderDrag?.takeIf { item.bookmark.id in it.draggedIds }
                                val dragDelta = drag?.let { it.currentOffset - it.startOffset }
                                val isDropTarget = reorderDrag?.hoverTargetId == item.bookmark.id
                                BookmarkCard(
                                    bookmark = item.bookmark,
                                    isSelected = item.bookmark.id in uiState.selectedIds,
                                    selectionMode = uiState.isSelectionMode,
                                    isDragActive = drag != null,
                                    isDropTarget = isDropTarget,
                                    // ドラッグ中のカードだけ独自レイヤーに載せて動かす。全カードに常時
                                    // graphicsLayerを適用するとスクロール中も余分な合成コストがかかるため、
                                    // 実際に動かす必要があるとき(drag != null)だけ付与する。
                                    modifier = if (drag != null && dragDelta != null) {
                                        Modifier
                                            .animateItem()
                                            .zIndex(1f)
                                            .graphicsLayer {
                                                translationX = dragDelta.x
                                                translationY = dragDelta.y
                                                shadowElevation = 16f
                                            }
                                    } else {
                                        Modifier.animateItem()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 処理中もグリッド操作やダイアログ表示を妨げないよう、モーダルにはせず
        // 画面下部(バナー広告の直上)に細い進捗バーとして表示する。
        thumbnailFetchProgress?.let { progress ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 64.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.auto_thumbnail_progress,
                            progress.completed,
                            progress.total,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.completed / progress.total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        pendingAutoThumbnailIds?.let { ids ->
            AlertDialog(
                onDismissRequest = { pendingAutoThumbnailIds = null },
                title = { Text(stringResource(R.string.auto_thumbnail_confirm_title)) },
                text = { Text(stringResource(R.string.auto_thumbnail_confirm_message, ids.size)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onAutoSetFirstThumbnailForSelection(ids)
                        maybeShowThumbnailUpdateAd(ids.size)
                        pendingAutoThumbnailIds = null
                    }) { Text(stringResource(R.string.auto_thumbnail_confirm_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAutoThumbnailIds = null }) {
                        Text(stringResource(R.string.delete_confirm_cancel))
                    }
                },
            )
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

        renameBookmarkTarget?.let { bookmark ->
            RenameBookmarkDialog(
                currentTitle = bookmark.title ?: domainOf(bookmark.url),
                onConfirm = { title ->
                    viewModel.onRenameBookmark(bookmark, title)
                    renameBookmarkTarget = null
                },
                onDismiss = { renameBookmarkTarget = null },
            )
        }

        thumbnailPickerBookmark?.let { bookmark ->
            ThumbnailPickerDialog(
                loadCandidates = { viewModel.fetchCandidateImages(bookmark) },
                onSelectCandidate = { imageUrl ->
                    viewModel.onSetManualThumbnail(bookmark, imageUrl)
                    maybeShowThumbnailUpdateAd(1)
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
                    if (savedPath != null) {
                        viewModel.onSetManualThumbnail(bookmark, savedPath)
                        maybeShowThumbnailUpdateAd(1)
                    }
                    webCaptureBookmark = null
                },
                onDismiss = { webCaptureBookmark = null },
            )
        }

        if (moreSheetVisible) {
            MoreActionsSheet(
                onOpenSettings = {
                    moreSheetVisible = false
                    onOpenSettings()
                },
                onDismiss = { moreSheetVisible = false },
            )
        }

        pendingOpenBookmark?.let { bookmark ->
            BrowserPickerDialog(
                browsers = remember { BrowserResolver.installedBrowsers(context) },
                showRememberToggle = true,
                onPick = { packageName, remember ->
                    if (remember) viewModel.onSetGlobalDefaultBrowser(packageName)
                    BrowserResolver.openUrl(context, bookmark.url, packageName)
                    pendingOpenBookmark = null
                },
                onDismiss = { pendingOpenBookmark = null },
            )
        }

        folderBrowserPickerTarget?.let { target ->
            BrowserPickerDialog(
                browsers = remember { BrowserResolver.installedBrowsers(context) },
                showRememberToggle = false,
                onPick = { packageName, _ ->
                    viewModel.onSetFolderDefaultBrowser(target.id, packageName)
                    folderBrowserPickerTarget = null
                },
                onDismiss = { folderBrowserPickerTarget = null },
            )
        }

        if (sortFilterSheetVisible) {
            SortAndFilterSheet(
                sortCriteria = uiState.sortCriteria,
                isManualOrder = uiState.isManualOrder,
                onSortCriteriaChange = viewModel::onSortCriteriaChange,
                onDismiss = { sortFilterSheetVisible = false },
            )
        }

        labelDialogTargetIds?.let { ids ->
            val targetBookmarks = uiState.gridItems
                .filterIsInstance<BookmarkGridItem.BookmarkItem>()
                .map { it.bookmark }
                .filter { it.id in ids }
            LabelAssignmentDialog(
                allLabels = uiState.allLabels,
                targetLabelNames = targetBookmarks.map { bookmark -> bookmark.labels.map { it.name }.toSet() },
                onConfirm = { addNames, removeNames ->
                    viewModel.onSetLabelsForSelection(ids, addNames, removeNames)
                    labelDialogTargetIds = null
                },
                onDismiss = { labelDialogTargetIds = null },
            )
        }

        duplicateGroups.firstOrNull()?.let { group ->
            DuplicateResolutionDialog(
                group = group,
                onKeepAll = { viewModel.onKeepAllDuplicates(group.first().url) },
                onKeepOne = { keepId -> viewModel.onResolveDuplicateKeepOne(keepId, group) },
                onDismiss = { viewModel.onDismissDuplicateGroup(group) },
            )
        }
    }
}

/**
 * 検索バーと一体化したラベル絞り込みチップ行。フィルタは「検索」の一部として
 * 扱うため、独立したシートではなくこの位置に統合している。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelFilterRow(
    labels: List<Label>,
    activeIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ラベル数が多いときに横スワイプを強いられないよう、1行に収まらない分は折り返す。
    // それでも収まりきらない場合に備え、高さに上限を設けて縦スクロールで逃がす。
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        labels.forEach { label ->
            val selected = label.id in activeIds
            val containerColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                label = "labelChipContainer",
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                label = "labelChipContent",
            )
            Text(
                text = label.name,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(containerColor)
                    .clickable { onToggle(label.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

private fun hitTestItem(
    gridState: LazyGridState,
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
