package com.boardmark.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.boardmark.app.ads.AppOpenAdManager
import com.boardmark.app.ads.ConsentManager
import com.boardmark.app.billing.BillingManager
import com.boardmark.app.ui.list.BookmarkListScreen
import com.boardmark.app.ui.list.BookmarkListViewModel
import com.boardmark.app.ui.settings.HelpScreen
import com.boardmark.app.ui.settings.SettingsScreen
import com.boardmark.app.ui.theme.BoardmarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // 参照はしないが、フィールド注入させるだけでHiltがSingletonを起動時に生成し、
    // Billing接続・商品情報取得(非同期)をこの時点で開始させるために宣言している。
    // 設定画面を開いたタイミングで初めて生成されると、フォールバック価格表示から
    // 実際の価格への差し替わりでレイアウトが一瞬ずれて見えるため、それより前倒しする。
    @Inject lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // コールドスタート時は、アプリ起動広告の読み込みを待つ間(最大数秒)起動画面を
        // 出したままにし、間に合えば起動画面から直接広告に切り替える(Google推奨の
        // 「読み込み画面の間に起動広告を出す」形)。待つ必要がなければすぐ閉じる。
        installSplashScreen().setKeepOnScreenCondition { AppOpenAdManager.shouldHoldSplash() }
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
        // enableEdgeToEdge()/setContent()がこのWindowのdecor(android.R.id.content)を
        // 完全に構築し終えた後で呼ぶこと。先に呼ぶと、GDPR同意情報取得(UMP SDK)が
        // 別スレッドから同じActivityのWindow.getDecorView()に触れることがあり、メイン
        // スレッドが行うinstallDecor()(PhoneWindowの内部状態はスレッドセーフではない)
        // と競合してdecorの構築が壊れ、起動直後に極めて発生しやすい確率でクラッシュしていた
        // (NullPointerException in ComponentActivityKt.setContent /
        // "Window couldn't find content container view")。
        ConsentManager.gatherConsentAndInitialize(this)
    }

    // リンクを開く(startActivity)・ファイル選択(ActivityResultLauncher)など、この
    // Activityから外部へ遷移する経路は最終的にすべてここを通るため、一箇所でまとめて
    // 「アプリ自身が外へ送り出した」ことを記録し、戻ってきた直後の起動広告を抑止する。
    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        AppOpenAdManager.notifyLeavingByOwnAction()
        super.startActivityForResult(intent, requestCode, options)
    }
}
