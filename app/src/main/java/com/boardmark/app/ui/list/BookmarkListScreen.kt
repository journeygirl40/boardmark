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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.boardmark.app.ui.components.BookmarkGridSkeleton
import com.boardmark.app.ui.components.BrowserPickerDialog
import com.boardmark.app.ui.components.DuplicateResolutionDialog
import com.boardmark.app.ui.components.EmptyBookmarksState
import com.boardmark.app.ui.components.MilestoneCelebration
import com.boardmark.app.ui.components.FolderTile
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
import com.boardmark.app.util.MilestonePreference
import com.boardmark.app.util.domainOf
import com.boardmark.app.util.rememberHaptics
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
    onOpenHelp: () -> Unit,
    viewModel: BookmarkListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val thumbnailFetchProgress by viewModel.thumbnailFetchProgress.collectAsState()
    val totalBookmarkCount by viewModel.totalBookmarkCount.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = rememberHaptics()

    var celebratingMilestone by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(totalBookmarkCount) {
        MilestonePreference.newlyReached(context, totalBookmarkCount)?.let { milestone ->
            // 表示より先に既読化しておくことで、構成変更等でこのEffectが再実行されても
            // 二重に祝わないようにする。
            MilestonePreference.markCelebrated(context, milestone)
            celebratingMilestone = milestone
            haptics.confirm()
        }
    }

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
    var sortFilterSheetVisible by remember { mutableStateOf(false) }
    var labelDialogTargetIds by remember { mutableStateOf<Set<Long>?>(null) }
    var pendingAutoThumbnailIds by remember { mutableStateOf<Set<Long>?>(null) }
    var reorderDrag by remember { mutableStateOf<ReorderDrag?>(null) }
    var pendingOpenBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var folderBrowserPickerTarget by remember { mutableStateOf<FolderTarget?>(null) }
    var searchFieldFocused by remember { mutableStateOf(false) }

    val screenScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // サムネイル更新開始の2秒後に、件数に応じた確率でインタースティシャル広告を出す。
    // itemCountが10件以上(複数選択の一括更新)の場合はほぼ確定表示になる。
    fun maybeShowThumbnailUpdateAd(itemCount: Int) {
        val activity = context as? Activity ?: return
        screenScope.launch {
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
                    // ラベル付きボタンはアイコンだけの場合よりずっと横幅を食うため、
                    // TopAppBarの title/navigationIcon/actionsという3スロット構成には
                    // 乗せない(スロットごとの割り当て幅を越えるとタイトルが消えたり
                    // ボタン同士が重なったりする)。全ボタンと件数表示を1本の横スクロール
                    // 可能なRowに並べ、幅が足りない端末でもスクロールで全ボタンに届くようにする。
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .heightIn(min = 64.dp),
                        ) {
                            val countText = stringResource(R.string.selection_count, uiState.selectedIds.size)
                            // スクロールさせず1画面に必ず収める方針のため、決め打ちサイズにせず
                            // 実際の画面幅と件数テキストの実測幅から、収まる最大のアイコン
                            // サイズをその場で計算する(端末や文字数が変わっても追従する)。
                            val sizing = rememberSelectionBarSizing(maxWidth = maxWidth, countText = countText)
                            // ×(解除)だけは見た目だけで用途が明確なので、他のボタンと違い
                            // ラベルを付けずアイコンのみにし、常に画面の左端に固定する。
                            // 残りは「件数+すべて」を左寄せ、「編集〜ゴミ箱」を右寄せにし、
                            // 間の1箇所だけ可変の間隔にする。それぞれのグループ内はボタン同士
                            // 同じ構造・同じパディングで並べ、間隔を均一にする。
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth()
                                    .padding(start = sizing.iconSize + sizing.horizontalPadding * 2),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // アイコン+見えない下段テキストで高さを揃えているボタン群と
                                    // 上下位置が合うよう、件数表示も同じ構造(見えない下段テキスト)
                                    // にする。単なるTextのままだと1行分の高さしかなく、アイコンの
                                    // 中心とずれて見えてしまうため。
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(
                                            horizontal = sizing.horizontalPadding,
                                            vertical = sizing.verticalPadding,
                                        ),
                                    ) {
                                        Text(
                                            text = countText,
                                            fontSize = sizing.fontSize,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = sizing.fontSize),
                                            maxLines = 1,
                                            modifier = Modifier.alpha(0f),
                                        )
                                    }
                                    LabeledIconButton(
                                        icon = Icons.Filled.SelectAll,
                                        label = stringResource(R.string.action_select_all),
                                        onClick = viewModel::onSelectAll,
                                        sizing = sizing,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 1件選択時にだけこのボタンを出し入れすると、後続のボタンの位置が
                                    // 選択件数によってずれてしまい押し間違いのもとになる。常に表示した
                                    // まま、名前変更が意味を持つ1件選択時だけ有効化することで位置を固定する。
                                    LabeledIconButton(
                                        icon = Icons.Filled.Edit,
                                        label = stringResource(R.string.rename_bookmark_action),
                                        onClick = {
                                            val id = uiState.selectedIds.single()
                                            renameBookmarkTarget = uiState.gridItems
                                                .filterIsInstance<BookmarkGridItem.BookmarkItem>()
                                                .firstOrNull { it.bookmark.id == id }?.bookmark
                                        },
                                        enabled = uiState.selectedIds.size == 1,
                                        sizing = sizing,
                                    )
                                    LabeledIconButton(
                                        icon = Icons.AutoMirrored.Filled.Label,
                                        label = stringResource(R.string.assign_labels_action),
                                        onClick = { labelDialogTargetIds = uiState.selectedIds },
                                        sizing = sizing,
                                    )
                                    LabeledIconButton(
                                        icon = Icons.AutoMirrored.Filled.DriveFileMove,
                                        label = stringResource(R.string.action_move),
                                        onClick = { moveDialogVisible = true },
                                        sizing = sizing,
                                    )
                                    LabeledIconButton(
                                        icon = Icons.Filled.Image,
                                        label = stringResource(R.string.choose_thumbnail_label),
                                        onClick = {
                                            val ids = uiState.selectedIds
                                            if (ids.size == 1) {
                                                val id = ids.single()
                                                thumbnailPickerBookmark = uiState.gridItems
                                                    .filterIsInstance<BookmarkGridItem.BookmarkItem>()
                                                    .firstOrNull { it.bookmark.id == id }?.bookmark
                                            } else {
                                                pendingAutoThumbnailIds = ids
                                            }
                                        },
                                        sizing = sizing,
                                    )
                                    UnlabeledIconButton(
                                        icon = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete_confirm_ok),
                                        onClick = { pendingDeleteSelection = true },
                                        sizing = sizing,
                                    )
                                }
                            }
                            UnlabeledIconButton(
                                icon = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_clear_selection),
                                onClick = viewModel::clearSelection,
                                sizing = sizing,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                        }
                    }
                } else if (selectedFolder != null) {
                    val target = selectedFolder!!
                    TopAppBar(
                        title = { Text(target.name) },
                        navigationIcon = {
                            TooltipIconButton(
                                tooltip = stringResource(R.string.action_clear_selection),
                                onClick = { selectedFolder = null },
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_selection))
                            }
                        },
                        actions = {
                            TooltipIconButton(
                                tooltip = stringResource(R.string.delete_folder_action),
                                onClick = {
                                    deleteFolderTarget = target
                                    selectedFolder = null
                                },
                            ) {
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
                                // 通常のTextFieldはMaterial3仕様上の最小幅(280dp)を内部で
                                // 強制するため、TopAppBar上の他のアクションアイコンに押されて
                                // このSurfaceの実際の幅がそれを下回ると、末尾のクリアボタンが
                                // 枠外にはみ出してSurfaceの角丸クリップで見えなくなってしまう
                                // (常に横幅が足りない一部の端末で発生)。最小幅を持たない
                                // BasicTextFieldを手組みすることでこれを避け、どんな幅でも
                                // アイコンが必ず収まるようにする。
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.search_placeholder),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicTextField(
                                        value = uiState.query,
                                        onValueChange = viewModel::onQueryChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            },
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .onFocusChanged { searchFieldFocused = it.isFocused },
                                    )
                                    if (uiState.query.isNotEmpty()) {
                                        TooltipIconButton(
                                            tooltip = stringResource(R.string.action_clear_search),
                                            onClick = { viewModel.onQueryChange("") },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = stringResource(R.string.action_clear_search),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            if (uiState.currentFolderId != null) {
                                TooltipIconButton(
                                    tooltip = stringResource(R.string.action_back),
                                    onClick = viewModel::onExitFolder,
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                                }
                            }
                        },
                        actions = {
                            Box {
                                TooltipIconButton(
                                    tooltip = stringResource(R.string.thumbnail_size_label),
                                    onClick = { sizeMenuExpanded = true },
                                ) {
                                    Icon(Icons.Filled.PhotoSizeSelectLarge, contentDescription = stringResource(R.string.thumbnail_size_label))
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
                            TooltipIconButton(
                                tooltip = stringResource(R.string.sort_section_title),
                                onClick = { sortFilterSheetVisible = true },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_section_title))
                            }
                            TooltipIconButton(
                                tooltip = stringResource(R.string.settings_action),
                                onClick = onOpenSettings,
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_action))
                            }
                        },
                        )
                        AnimatedVisibility(
                            visible = uiState.allLabels.isNotEmpty() &&
                                (searchFieldFocused || uiState.query.isNotEmpty() || uiState.activeLabelFilter.isNotEmpty()),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            // 検索語がラベル名にマッチする場合はそのラベルを先頭に寄せる。
                            // ラベルで絞り込みたいときに横スクロールせず見つけられるようにするため。
                            val orderedLabels = if (uiState.query.isBlank()) {
                                uiState.allLabels
                            } else {
                                uiState.allLabels.sortedByDescending {
                                    it.name.contains(uiState.query, ignoreCase = true)
                                }
                            }
                            LabelFilterRow(
                                labels = orderedLabels,
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            renameFolderTarget = FolderTarget(folderId, folderName)
                                        },
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.rename_folder_action),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = folderName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val gridState = rememberLazyGridState()
            val columnCount = thumbnailColumnsForLevel(uiState.thumbnailSizeLevel)

            if (uiState.isLoading) {
                BookmarkGridSkeleton(columns = columnCount, modifier = Modifier.fillMaxSize())
            } else if (uiState.gridItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().clearFocusOnTap(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyBookmarksState(
                        isFiltered = uiState.query.isNotEmpty() || uiState.activeLabelFilter.isNotEmpty(),
                        onLearnMoreClick = onOpenHelp,
                    )
                }
            } else {
                val currentItems by rememberUpdatedState(uiState.gridItems)
                val selectionModeState by rememberUpdatedState(uiState.isSelectionMode)
                val selectedIdsState by rememberUpdatedState(uiState.selectedIds)

                LazyVerticalGrid(
                    state = gridState,
                    // 列数を直接指定するFixedを使う(余白は出ず、スライダーの4段階が
                    // そのまま列数1〜4に対応するため、動かして効果のない中間値が存在しない)。
                    // StaggeredGridではなく通常のGridを使うことで、カードの高さが
                    // 揃っていない場合でも必ずZ字(行優先)の並び順になる。
                    columns = GridCells.Fixed(columnCount),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
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
                                                            haptics.selectionToggle()
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
                                                    haptics.longPress()
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
                                                    haptics.longPress()
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
                                                val previousHoverTarget = reorderDrag?.hoverTargetId
                                                reorderDrag = reorderDrag?.let { drag ->
                                                    val hoverHit = hitTestItem(gridState, currentItems, change.position)
                                                    val hoverTarget = (hoverHit as? BookmarkGridItem.BookmarkItem)
                                                        ?.bookmark?.id
                                                        ?.takeIf { it !in drag.draggedIds }
                                                    drag.copy(currentOffset = change.position, hoverTargetId = hoverTarget)
                                                }
                                                if (reorderDrag?.hoverTargetId != previousHoverTarget) {
                                                    haptics.dragOverNewTarget()
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isExtendingSelection) {
                                                viewModel.onSelectionDragEnd()
                                            } else {
                                                reorderDrag?.let { drag ->
                                                    drag.hoverTargetId?.let { target ->
                                                        haptics.reorderCommit()
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
                    items(
                        uiState.gridItems,
                        key = { it.key },
                        // フォルダとブックマークで構成が全く異なるため、種別ごとにcontentTypeを
                        // 分けることで、スクロールでアイテムが入れ替わる際のコンポジション再利用の
                        // 効率を上げる(異なる種別同士では再利用されず、同種別間でのみ再利用される)。
                        contentType = { item ->
                            when (item) {
                                is BookmarkGridItem.FolderItem -> "folder"
                                is BookmarkGridItem.BookmarkItem -> "bookmark"
                            }
                        },
                    ) { item ->
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

                // スワイプ中に一覧全体のどのあたりを見ているか分かるよう、右端に
                // 現在位置を示す細いバーを表示する。スクロール中だけ出してすぐ消すことで、
                // 常時表示による視覚的なノイズを避ける。
                GridScrollbar(
                    gridState = gridState,
                    columns = columnCount,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp, horizontal = 3.dp),
                )
            }

            // 処理中もグリッド操作やダイアログ表示を妨げないよう、モーダルにはせず
            // 画面上部(検索バーの直下)に細い進捗バーとして表示する。下部は広告バナーが
            // あり親指の操作域にも近いため避け、常に視界に入る上部に置く。
            thumbnailFetchProgress?.let { progress ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
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
            }
        }

        // ブックマーク件数の節目を一度だけ祝う。タップ・スワイプを一切奪わないため
        // (MilestoneCelebration自体はpointerInputを持たない)、表示中も一覧の操作は妨げない。
        celebratingMilestone?.let { milestone ->
            MilestoneCelebration(
                milestone = milestone,
                onDismiss = { celebratingMilestone = null },
                modifier = Modifier.fillMaxSize(),
            )
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
                        haptics.confirm()
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

// 一覧全体に対する現在のスクロール位置を示す、右端の細いバー。行数ベースで
// 位置とバーの長さを計算するため、GridCells.Fixed(N)の列数(columns)が必要。
@Composable
private fun GridScrollbar(
    gridState: LazyGridState,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = gridState.layoutInfo
    val totalItemCount = layoutInfo.totalItemsCount
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (columns <= 0 || totalItemCount == 0 || visibleItemsInfo.isEmpty()) return

    val totalRows = (totalItemCount + columns - 1) / columns
    val firstItem = visibleItemsInfo.first()
    val firstVisibleRow = firstItem.index / columns

    // 行番号(整数)だけで位置を出すと、1行分スクロールしきるまでバーが動かず
    // かくつく。行の途中までのスクロール量(端数)も加味して、指の動きに
    // なめらかに追従させる。
    val density = LocalDensity.current
    val rowStepPx = firstItem.size.height + with(density) { 16.dp.toPx() }
    val subRowFraction = (-firstItem.offset.y / rowStepPx).coerceIn(0f, 1f)
    val continuousFirstRow = firstVisibleRow + subRowFraction

    // 「同時に何行分見えているか」はビューポートの高さから連続値で求める。
    // visibleItemsInfoの件数(端で半端に見切れた行がスクロールにつれて出入りする
    // ことで前後1行ぶれる)から数えると、位置は動かなくてもバーの長さ自体が
    // スクロール中にガタついてしまうため使わない。
    val viewportHeightPx = layoutInfo.viewportSize.height -
        layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    val visibleRowCount = (viewportHeightPx / rowStepPx).coerceIn(1f, totalRows.toFloat())
    // 一覧が1画面に収まっている(スクロール不要)場合はバーを出す意味がない。
    if (visibleRowCount >= totalRows) return

    val scrollableRows = (totalRows - visibleRowCount).coerceAtLeast(1f)
    val progress = (continuousFirstRow / scrollableRows).coerceIn(0f, 1f)
    val barFraction = (visibleRowCount / totalRows).coerceIn(0.06f, 1f)

    // フリング中も含めて操作している間だけ表示し、止まったら少し待ってフェードアウトする
    // (常時表示すると一覧の見た目のノイズになるため)。
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            isVisible = true
        } else {
            delay(600)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxHeight().width(4.dp)) {
            val trackHeight = maxHeight
            val barHeight = trackHeight * barFraction
            Box(
                modifier = Modifier
                    .offset(y = (trackHeight - barHeight) * progress)
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)),
            )
        }
    }
}

// アイコンだけのボタンは見た目上は常に説明を出さず、長押し(またはマウスホバー)した
// ときだけ標準のツールチップとして説明を出す。常時テキストを添えると窮屈になる
// TopAppBarのアクション列でも、押し間違いを防ぐ手がかりを邪魔にならない形で出せる。
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = icon)
    }
}

// 選択モードのツールバー(件数表示・LabeledIconButton・UnlabeledIconButton)で共通の
// サイズ。決め打ちの数値ではなく、rememberSelectionBarSizingが実際の画面幅から
// その場で計算する。3箇所でバラバラに数値を持つと調整のたびにズレる原因になる
// ため、計算結果をこの1つにまとめて渡す。
private data class SelectionBarSizing(
    val iconSize: Dp,
    val fontSize: TextUnit,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
)

// ×・すべて・編集・ラベル・移動・サムネ・ゴミ箱=アイコンのみのボタン7個(件数表示は
// 別枠で加算)を並べたときに横スクロールなしで1画面に収まるよう、画面幅と件数
// テキストの実測幅から「収まる最大のアイコンサイズ」を二分探索で求める。決め打ち
// サイズだと画面幅の違う端末で入り切らなくなる/逆に無駄に余白ができるため、
// 常にその場の画面幅に合わせて調整する。
private const val SelectionBarFontToIconRatio = 13f / 30f
private const val SelectionBarPaddingToIconRatio = 8f / 30f
private const val SelectionBarVerticalPaddingToIconRatio = 6f / 30f
private const val SelectionBarIconOnlyButtonCount = 7
private const val SelectionBarMinGapDp = 24f
private const val SelectionBarMinIconDp = 18f
private const val SelectionBarMaxIconDp = 44f

@Composable
private fun rememberSelectionBarSizing(maxWidth: Dp, countText: String): SelectionBarSizing {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    return remember(maxWidth, countText, density) {
        fun totalWidthDp(iconDp: Float): Float {
            val fontSp = iconDp * SelectionBarFontToIconRatio
            val paddingDp = iconDp * SelectionBarPaddingToIconRatio
            val countLayout = textMeasurer.measure(
                text = countText,
                style = TextStyle(fontSize = fontSp.sp),
            )
            val countTextWidthDp = with(density) { countLayout.size.width.toDp().value }
            val countSlotWidthDp = countTextWidthDp + paddingDp * 2
            val iconSlotWidthDp = iconDp + paddingDp * 2
            return iconSlotWidthDp * SelectionBarIconOnlyButtonCount + countSlotWidthDp + SelectionBarMinGapDp
        }

        var lo = SelectionBarMinIconDp
        var hi = SelectionBarMaxIconDp
        repeat(24) {
            val mid = (lo + hi) / 2f
            if (totalWidthDp(mid) <= maxWidth.value) lo = mid else hi = mid
        }

        SelectionBarSizing(
            iconSize = lo.dp,
            fontSize = (lo * SelectionBarFontToIconRatio).sp,
            horizontalPadding = (lo * SelectionBarPaddingToIconRatio).dp,
            verticalPadding = (lo * SelectionBarVerticalPaddingToIconRatio).dp,
        )
    }
}

// 選択モードのツールバーは操作の種類が多く、長押しでしか出ないツールチップだと
// 気づかれないまま押し間違えられやすい。そのためここだけはアイコンの下に常時
// 短いラベルを出す。IconButtonは内部で幅を固定してしまい長いラベルがはみ出す
// ため使わず、ボタンごとにラベルの実際の幅に合わせて自然に伸縮させる。
@Composable
private fun LabeledIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    sizing: SelectionBarSizing,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick, enabled = enabled, role = Role.Button)
            .padding(horizontal = sizing.horizontalPadding, vertical = sizing.verticalPadding)
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(sizing.iconSize))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = sizing.fontSize),
            maxLines = 1,
            softWrap = false,
        )
    }
}

// LabeledIconButtonと構造(パディング・文字スタイル分の高さ)を完全に揃えるための
// ラベルなし版。見た目だけで用途が明確なアイコン(×・ゴミ箱)に使う。同じ高さの
// 透明なテキストを確保しておくことで、ラベル付きボタンと並べたときにアイコンの
// 上下位置がぴったり揃う(単純にIconButtonを混ぜるとラベル分の高さの差でずれる)。
@Composable
private fun UnlabeledIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    sizing: SelectionBarSizing,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = sizing.horizontalPadding, vertical = sizing.verticalPadding),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(sizing.iconSize))
        Text(
            text = "",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = sizing.fontSize),
            maxLines = 1,
            modifier = Modifier.alpha(0f),
        )
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
