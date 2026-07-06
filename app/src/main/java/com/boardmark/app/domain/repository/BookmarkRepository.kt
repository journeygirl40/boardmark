package com.boardmark.app.domain.repository

import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.domain.model.FolderWithPreview
import com.boardmark.app.domain.model.Label
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

    /** ブックマークのタイトルをユーザーが指定した文字列で上書きする。 */
    suspend fun renameBookmark(bookmark: Bookmark, title: String)

    /** 新規フォルダを作成し、作成したフォルダのIDを返す。 */
    suspend fun createFolder(name: String): Long

    /** フォルダを削除する。所属していたブックマークはトップレベル(未分類)に戻す。 */
    suspend fun deleteFolder(folderId: Long)

    /** フォルダの名前を変更する。 */
    suspend fun renameFolder(folderId: Long, name: String)

    /** ブックマークの手動並び順を更新する。 */
    suspend fun reorderBookmark(bookmarkId: Long, order: Double)

    /** フォルダの既定ブラウザを設定する(null で未設定に戻す)。 */
    suspend fun setFolderDefaultBrowser(folderId: Long, packageName: String?)

    /** 選択したブックマークを指定フォルダへ移動する。folderId=null でトップレベルに戻す。 */
    suspend fun moveBookmarksToFolder(bookmarkIds: Set<Long>, folderId: Long?)

    /** エクスポート用に、検索絞り込みなしの全ブックマークと全フォルダを一括取得する。 */
    suspend fun getAllForExport(): Pair<List<Bookmark>, List<Folder>>

    /**
     * Netscapeブックマークファイル(Chrome等のHTMLエクスポート形式)を取り込む。
     * 既存URLは重複追加せず、フォルダ名が一致する既存フォルダがあれば再利用する。
     * 戻り値は実際に追加した件数。
     */
    suspend fun importBookmarksFromHtml(html: String): Int

    /** 全ラベルを名前順で観測する。 */
    fun observeLabels(): Flow<List<Label>>

    /**
     * 指定ブックマークのラベルを名前の集合で丸ごと置き換える。存在しない名前は新規作成する。
     */
    suspend fun setBookmarkLabels(bookmarkId: Long, labelNames: Set<String>)

    /** 指定ラベルが付いているブックマーク件数(ラベル管理画面での確認表示用)。 */
    suspend fun countBookmarksForLabel(labelId: Long): Int

    /**
     * ラベル名を変更する。変更先の名前を持つ別ラベルが既に存在する場合は、そのラベルへ
     * 統合する(付け替え後に元のラベルは削除する)。
     */
    suspend fun renameLabel(labelId: Long, newName: String)

    /** ラベルを削除する。付いていた全ブックマークからも外れる。 */
    suspend fun deleteLabel(labelId: Long)

    /** ブックマークを開くたびに呼び出す閲覧回数のインクリメント。 */
    suspend fun incrementViewCount(bookmarkId: Long)

    /**
     * 同一URLで2件以上重複しているブックマークをグループ化して返す。
     * グループ内の全件が「すべて残す」で無視済みのグループは含めない。
     */
    suspend fun getUnresolvedDuplicateGroups(): List<List<Bookmark>>

    /** 指定URLのグループを「すべて残す」として無視済みにする(以後は重複検知の対象外)。 */
    suspend fun ignoreDuplicateGroup(url: String)
}
