package com.boardmark.app.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardmark.app.ui.list.BookmarkListScreen
import com.boardmark.app.ui.list.BookmarkListViewModel
import com.boardmark.app.ui.settings.HelpScreen
import com.boardmark.app.ui.settings.SettingsScreen
import com.boardmark.app.ui.theme.BoardmarkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoardmarkTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                // 一覧が空のときの案内リンクから、設定を経由せず直接開けるようにする。
                var showHelp by rememberSaveable { mutableStateOf(false) }
                // Navigation Composeを使わず画面をbooleanで切り替えているため、システムの
                // 戻るボタンは何もしなければActivity終了(=アプリが閉じる)まで素通りしてしまう。
                BackHandler(enabled = showSettings) { showSettings = false }
                BackHandler(enabled = showHelp) { showHelp = false }
                // ラベル管理画面からのラベル選択で一覧側の絞り込みを直接更新できるよう、
                // BookmarkListScreen配下ではなくここでViewModelを取得して共有する
                // (hiltViewModel()はActivityのViewModelStoreに紐づくため同一インスタンスになる)。
                val bookmarkListViewModel: BookmarkListViewModel = hiltViewModel()
                when {
                    showHelp -> HelpScreen(onBack = { showHelp = false })
                    showSettings -> SettingsScreen(
                        onBack = { showSettings = false },
                        onSelectLabel = { labelId ->
                            bookmarkListViewModel.onFilterByLabelId(labelId)
                            showSettings = false
                        },
                    )
                    else -> BookmarkListScreen(
                        onOpenSettings = { showSettings = true },
                        onOpenHelp = { showHelp = true },
                        viewModel = bookmarkListViewModel,
                    )
                }
            }
        }
    }
}
