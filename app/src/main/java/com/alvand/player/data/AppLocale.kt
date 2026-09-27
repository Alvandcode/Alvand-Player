package com.alvand.player.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** زبان‌های پشتیبانی‌شده + اعمال per-app locale */
object AppLocale {

    data class Lang(val code: String, val nativeName: String)

    val all = listOf(
        Lang("en", "English"),
        Lang("fa", "فارسی")
    )

    private val codeSet = all.map { it.code }.toSet()

    /** نرمالایز تگ زبان؛ زبان‌های بدون ترجمه به انگلیسی برمی‌گردند */
    fun normalize(code: String): String {
        val c = code.lowercase()
        val base = c.substringBefore("-").substringBefore("_")
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
        // اولویت اول کاربر، بعد بقیه — فقط زبان‌های واقعاً پشتیبانی‌شده پذیرفته می‌شوند
        for (raw in tags.split(",")) {
            val base = raw.trim().lowercase().substringBefore("-").substringBefore("_")
            if (base in codeSet) return base
        }
        return "en"
    }
}
