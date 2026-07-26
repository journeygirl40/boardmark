package com.boardmark.app.domain.model

/** フォルダ名変更ダイアログでのパスワード設定の変更内容。 */
sealed interface FolderPasswordUpdate {
    /** チェック状態・入力欄が既存の設定から変わっていない。 */
    data object Unchanged : FolderPasswordUpdate

    /** パスワード保護を解除する。 */
    data object Cleared : FolderPasswordUpdate

    /** 新しいパスワードを設定する(既存のパスワードを置き換える、または新規設定)。 */
    data class Changed(val password: String) : FolderPasswordUpdate
}
