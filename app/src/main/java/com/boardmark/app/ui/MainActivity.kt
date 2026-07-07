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
import com.boardmark.app.ui.list.BookmarkListScreen
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
                // Navigation Composeを使わず画面をbooleanで切り替えているため、システムの
                // 戻るボタンは何もしなければActivity終了(=アプリが閉じる)まで素通りしてしまう。
                BackHandler(enabled = showSettings) { showSettings = false }
                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    BookmarkListScreen(onOpenSettings = { showSettings = true })
                }
            }
        }
    }
}
