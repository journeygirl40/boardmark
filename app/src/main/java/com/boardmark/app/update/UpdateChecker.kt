package com.boardmark.app.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo

/**
 * Play Coreの「アプリ内アップデート」APIを使って、Playに公開済みの新しいバージョンが
 * あるかどうかだけを確認する(ダウンロード等の更新フロー自体はここでは行わず、
 * ユーザーをGoogle Playへ誘導するだけの軽量な用途)。
 */
object UpdateChecker {

    /** 新しいバージョンが公開されていればそのversionCodeを、なければnullを返す。 */
    suspend fun checkForUpdate(context: Context): Int? = try {
        val appUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
        val info = appUpdateManager.requestAppUpdateInfo()
        if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            info.availableVersionCode()
        } else {
            null
        }
    } catch (e: Exception) {
        // ネットワーク不通やPlay Storeアプリ未インストールなど、更新確認自体が
        // できない状況は珍しくないため、失敗しても普段の利用を妨げない。
        null
    }

    /** Google PlayのアプリページをPlayストアアプリ優先で開く(なければ通常のURLで開く)。 */
    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        try {
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            )
            context.startActivity(webIntent)
        }
    }
}
