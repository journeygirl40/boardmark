package com.boardmark.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.boardmark.app.R
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private enum class CaptureMode { BROWSE, SELECT }

private enum class DragTarget { NEW, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

private const val MIN_SELECTION_PX = 20f
private val HANDLE_TOUCH_RADIUS = 24.dp
private val HANDLE_DRAW_RADIUS = 6.dp

private fun rectFromAnchor(anchor: Offset, point: Offset) = Rect(
    left = minOf(anchor.x, point.x),
    top = minOf(anchor.y, point.y),
    right = maxOf(anchor.x, point.x),
    bottom = maxOf(anchor.y, point.y),
)

/**
 * 対象URLの実際の描画結果をWebViewで表示し、ユーザーがドラッグで選んだ矩形を
 * ビットマップとして切り出す。PixelCopyはWebViewが属するWindowでのみ正しく動作するため、
 * 呼び出し側でDialog等の別Windowに包まず、ホストActivityと同じ画面ツリーに直接配置すること。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebThumbnailCaptureScreen(
    url: String,
    onCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(CaptureMode.BROWSE) }
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var selection by remember { mutableStateOf<Rect?>(null) }

    val hasValidSelection = selection?.let {
        it.width > MIN_SELECTION_PX && it.height > MIN_SELECTION_PX
    } ?: false

    fun exitSelectMode() {
        mode = CaptureMode.BROWSE
        selection = null
    }

    fun handleBack() {
        when {
            mode == CaptureMode.SELECT -> exitSelectMode()
            webView?.canGoBack() == true -> webView?.goBack()
            else -> onDismiss()
        }
    }

    BackHandler(onBack = ::handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (mode == CaptureMode.SELECT) {
                                R.string.capture_select_instruction
                            } else {
                                R.string.capture_browse_instruction
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (mode == CaptureMode.BROWSE) {
                        IconButton(onClick = { mode = CaptureMode.SELECT }) {
                            Icon(
                                Icons.Filled.Crop,
                                contentDescription = stringResource(R.string.capture_select_action),
                            )
                        }
                    } else {
                        IconButton(
                            enabled = hasValidSelection,
                            onClick = {
                                val wv = webView
                                val rect = selection
                                if (wv != null && rect != null) {
                                    coroutineScope.launch {
                                        val bitmap = captureWebView(context, wv)
                                        if (bitmap != null) {
                                            onCaptured(cropBitmap(bitmap, rect))
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.capture_use_selection_action),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                isLoading = false
                            }
                        }
                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (mode == CaptureMode.SELECT) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            val handleRadiusPx = HANDLE_TOUCH_RADIUS.toPx()
                            var dragTarget = DragTarget.NEW
                            var anchor = Offset.Zero
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val current = selection
                                    dragTarget = when {
                                        current == null -> DragTarget.NEW
                                        (offset - current.topLeft).getDistance() <= handleRadiusPx ->
                                            DragTarget.TOP_LEFT
                                        (offset - current.topRight).getDistance() <= handleRadiusPx ->
                                            DragTarget.TOP_RIGHT
                                        (offset - current.bottomLeft).getDistance() <= handleRadiusPx ->
                                            DragTarget.BOTTOM_LEFT
                                        (offset - current.bottomRight).getDistance() <= handleRadiusPx ->
                                            DragTarget.BOTTOM_RIGHT
                                        current.contains(offset) -> DragTarget.MOVE
                                        else -> DragTarget.NEW
                                    }
                                    anchor = when (dragTarget) {
                                        DragTarget.NEW -> offset
                                        DragTarget.TOP_LEFT -> current!!.bottomRight
                                        DragTarget.TOP_RIGHT -> current!!.bottomLeft
                                        DragTarget.BOTTOM_LEFT -> current!!.topRight
                                        DragTarget.BOTTOM_RIGHT -> current!!.topLeft
                                        DragTarget.MOVE -> Offset.Zero
                                    }
                                    if (dragTarget == DragTarget.NEW) {
                                        selection = Rect(offset, offset)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val maxWidth = size.width.toFloat()
                                    val maxHeight = size.height.toFloat()
                                    selection = if (dragTarget == DragTarget.MOVE) {
                                        selection?.let { current ->
                                            val translateX = dragAmount.x
                                                .coerceIn(-current.left, maxWidth - current.right)
                                            val translateY = dragAmount.y
                                                .coerceIn(-current.top, maxHeight - current.bottom)
                                            current.translate(translateX, translateY)
                                        }
                                    } else {
                                        val point = Offset(
                                            change.position.x.coerceIn(0f, maxWidth),
                                            change.position.y.coerceIn(0f, maxHeight),
                                        )
                                        rectFromAnchor(anchor, point)
                                    }
                                },
                            )
                        },
                ) {
                    val scrim = Color.Black.copy(alpha = 0.5f)
                    val rect = selection
                    if (rect != null) {
                        drawRect(color = scrim, topLeft = Offset.Zero, size = Size(size.width, rect.top))
                        drawRect(
                            color = scrim,
                            topLeft = Offset(0f, rect.bottom),
                            size = Size(size.width, size.height - rect.bottom),
                        )
                        drawRect(color = scrim, topLeft = Offset(0f, rect.top), size = Size(rect.left, rect.height))
                        drawRect(
                            color = scrim,
                            topLeft = Offset(rect.right, rect.top),
                            size = Size(size.width - rect.right, rect.height),
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = rect.topLeft,
                            size = rect.size,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        val handleRadiusPx = HANDLE_DRAW_RADIUS.toPx()
                        listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { corner ->
                            drawCircle(color = Color.White, radius = handleRadiusPx, center = corner)
                        }
                    } else {
                        drawRect(color = scrim)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
    }
}

private suspend fun captureWebView(context: Context, webView: WebView): Bitmap? {
    val activity = context.findActivity() ?: return null
    val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
    val location = IntArray(2)
    webView.getLocationInWindow(location)
    val srcRect = android.graphics.Rect(
        location[0],
        location[1],
        location[0] + webView.width,
        location[1] + webView.height,
    )
    return suspendCancellableCoroutine { continuation ->
        PixelCopy.request(
            activity.window,
            srcRect,
            bitmap,
            { result -> continuation.resume(if (result == PixelCopy.SUCCESS) bitmap else null) },
            Handler(Looper.getMainLooper()),
        )
    }
}

private fun cropBitmap(source: Bitmap, rect: Rect): Bitmap {
    val left = rect.left.coerceIn(0f, source.width.toFloat()).toInt()
    val top = rect.top.coerceIn(0f, source.height.toFloat()).toInt()
    val right = rect.right.coerceIn(0f, source.width.toFloat()).toInt()
    val bottom = rect.bottom.coerceIn(0f, source.height.toFloat()).toInt()
    val width = (right - left).coerceAtLeast(1)
    val height = (bottom - top).coerceAtLeast(1)
    return Bitmap.createBitmap(source, left, top, width, height)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
