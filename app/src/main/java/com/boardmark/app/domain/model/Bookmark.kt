package com.boardmark.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

enum class FetchStatus { PENDING, SUCCESS, FAILED }

// Listを持つデータクラスはComposeコンパイラがデフォルトで「unstable」と推論し、
// グリッドの各アイテムでスキップ最適化が効かず不要な再コンポジションの原因になる。
// 実際は生成後に変更しない値なので@Immutableで安定性を明示し、スクロールを滑らかにする。
@Immutable
data class Bookmark(
    val id: Long,
    val url: String,
    val originalUrl: String,
    val title: String?,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val fetchStatus: FetchStatus,
    val addedAt: Instant,
    val folderId: Long? = null,
    val description: String? = null,
    val siteName: String? = null,
    val manualOrder: Double = 0.0,
    val viewCount: Int = 0,
    val duplicateIgnored: Boolean = false,
    val labels: List<Label> = emptyList(),
)
