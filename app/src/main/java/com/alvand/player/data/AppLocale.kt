package com.alvand.player.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** زبان‌های پشتیبانی‌شده + اعمال per-app locale */
object AppLocale {

    data class Lang(val code: String, val nativeName: String)

    val all = listOf(
        Lang("en", "English"),
        Lang("zh", "中文 (简体)"),
        Lang("hi", "हिन्दी"),
        Lang("es", "Español"),
        Lang("fr", "Français"),
        Lang("ar", "العربية"),
        Lang("pt", "Português"),
        Lang("ru", "Русский"),
        Lang("ur", "اردو"),
        Lang("id", "Bahasa Indonesia"),
        Lang("de", "Deutsch"),
        Lang("ja", "日本語"),
        Lang("it", "Italiano"),
        Lang("tr", "Türkçe"),
        Lang("ko", "한국어"),
        Lang("vi", "Tiếng Việt"),
        Lang("fa", "فارسی")
    )

    /** تغییر زبان اپ (خودکار ذخیره و اعمال می‌شود) */
    fun apply(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    /** تگ زبان فعلی اپ، مثل fa یا en */
    fun currentTag(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .ifBlank { "en" }.split(",").first().substringBefore("-").lowercase()
}
