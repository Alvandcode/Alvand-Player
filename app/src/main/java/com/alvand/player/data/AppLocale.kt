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

    private val codeSet = all.map { it.code }.toSet()

    /** نرمالایز: "in" قدیمی → "id" مدرن؛ "zh-Hans-CN" → "zh" */
    fun normalize(code: String): String {
        val c = code.lowercase()
        if (c == "in" || c == "ind") return "id"
        if (c.startsWith("zh")) return "zh"
        val base = c.substringBefore("-").substringBefore("_")
        if (base == "in") return "id"
        return if (base in codeSet) base else "en"
    }

    /** تغییر زبان اپ (خودکار ذخیره و اعمال می‌شود) */
    fun apply(code: String) {
        val n = normalize(code)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(n))
    }

    /** تگ زبان فعلی اپ، مثل fa یا en — چندلوکیله و منطقه را درست هندل می‌کند */
    fun currentTag(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { return "en" }
        // اولویت اول کاربر، بعد بقیه
        for (raw in tags.split(",")) {
            val n = normalize(raw.trim())
            if (n in codeSet) return n
        }
        return normalize(tags.split(",").first())
    }
}
