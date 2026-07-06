package com.boardmark.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri

data class BrowserApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

object BrowserResolver {

    /** 端末にインストールされているブラウザ(httpsを開けるアプリ)の一覧を返す。 */
    fun installedBrowsers(context: Context): List<BrowserApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { info ->
                BrowserApp(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** 指定パッケージ名のアプリ表示名を取得する。見つからなければパッケージ名をそのまま返す。 */
    fun labelFor(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }

    /** packageName を明示して開く。未指定または起動できない場合は通常のURL起動にフォールバックする。 */
    fun openUrl(context: Context, url: String, packageName: String?) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (packageName != null) setPackage(packageName)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
