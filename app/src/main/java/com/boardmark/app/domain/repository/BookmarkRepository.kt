package com.boardmark.app.domain.repository

import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.domain.model.FolderWithPreview
import kotlinx.coroutines.flow.Flow

data class TopLevelListing(
    val folders: List<FolderWithPreview>,
    val ungroupedBookmarks: List<Bookmark>,
)

interface BookmarkRepository {

    /** トップレベル(フォルダ+未分類ブックマークの混在)を検索クエリ付きで観測する。 */
    fun observeTopLevel(query: String): Flow<TopLevelListing>

    /** 指定フォルダ内のブックマークを検索クエリ付きで観測する。 */
    fun observeFolderContents(folderId: Long, query: String): Flow<List<Bookmark>>

    /** 移動先ダイアログ用の全フォルダ一覧(検索絞り込みなし)。 */
    fun observeFolders(): Flow<List<Folder>>

    /**
     * Room に PENDING 行を即時 INSERT し、OGP取得のバックグラウンドWorkerを enqueue する。
     * 同一URLが既に保存済みの場合は新規作成せず、追加日時のみ更新して返り値でfalseを返す。
     */
    suspend fun addBookmark(url: String): Boolean

    suspend fun updateFetchResult(
        bookmarkId: Long,
        status: FetchStatus,
        finalUrl: String?,
        title: String?,
        ogImageUrl: String?,
        faviconUrl: String?,
        description: String? = null,
        siteName: String? = null,
    )

    suspend fun deleteByIds(ids: Set<Long>)

    /** ページ内の画像候補一覧を取得する(ユーザーがサムネイルを手動選択する際に使用)。 */
    suspend fun fetchCandidateImages(bookmark: Bookmark): List<String>

    /** サムネイルをユーザーが選択した画像で上書きする。 */
    suspend fun setManualThumbnail(bookmark: Bookmark, imageUrl: String)

    /** 新規フォルダを作成し、作成したフォルダのIDを返す。 */
    suspend fun createFolder(name: String): Long

    /** フォルダを削除する。所属していたブックマークはトップレベル(未分類)に戻す。 */
    suspend fun deleteFolder(folderId: Long)

    /** フォルダの名前を変更する。 */
    suspend fun renameFolder(folderId: Long, name: String)

    /** 選択したブックマークを指定フォルダへ移動する。folderId=null でトップレベルに戻す。 */
    suspend fun moveBookmarksToFolder(bookmarkIds: Set<Long>, folderId: Long?)

    /** エクスポート用に、検索絞り込みなしの全ブックマークと全フォルダを一括取得する。 */
    suspend fun getAllForExport(): Pair<List<Bookmark>, List<Folder>>
}
