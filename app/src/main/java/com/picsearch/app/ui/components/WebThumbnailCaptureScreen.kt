package com.picsearch.app.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private enum class CaptureMode { BROWSE, SELECT }

private const val MIN_SELECTION_PX = 20f

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
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    val start = dragStart
    val end = dragEnd
    val selectionRect = if (start != null && end != null) {
        Rect(
            left = minOf(start.x, end.x),
            top = minOf(start.y, end.y),
            right = maxOf(start.x, end.x),
            bottom = maxOf(start.y, end.y),
        )
    } else {
        null
    }
    val hasValidSelection = selectionRect != null &&
        selectionRect.width > MIN_SELECTION_PX &&
        selectionRect.height > MIN_SELECTION_PX

    fun exitSelectMode() {
        mode = CaptureMode.BROWSE
        dragStart = null
        dragEnd = null
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
                    Text(if (mode == CaptureMode.SELECT) "サムネにしたい範囲をドラッグして選択" else "サムネにしたい画面を表示してください")
                },
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (mode == CaptureMode.BROWSE) {
                        IconButton(onClick = { mode = CaptureMode.SELECT }) {
                            Icon(Icons.Filled.Crop, contentDescription = "範囲を選択")
                        }
                    } else {
                        IconButton(
                            enabled = hasValidSelection,
                            onClick = {
                                val wv = webView
                                val rect = selectionRect
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
                            Icon(Icons.Filled.Check, contentDescription = "この範囲を使う")
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
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStart = offset
                                    dragEnd = offset
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    dragEnd = change.position
                                },
                            )
                        },
                ) {
                    val scrim = Color.Black.copy(alpha = 0.5f)
                    val rect = selectionRect
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
