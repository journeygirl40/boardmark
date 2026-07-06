package com.boardmark.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.boardmark.app.R

enum class AppLanguage(val tag: String?) {
    SYSTEM_DEFAULT(null),
    JAPANESE("ja"),
    ENGLISH("en"),
    ;

    fun apply() {
        val locales = if (tag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    companion object {
        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.firstOrNull { it.tag == tag } ?: SYSTEM_DEFAULT
        }
    }
}

@Composable
fun AppLanguage.displayLabel(): String = when (this) {
    AppLanguage.SYSTEM_DEFAULT -> stringResource(R.string.language_system_default)
    AppLanguage.JAPANESE -> "日本語"
    AppLanguage.ENGLISH -> "English"
}
