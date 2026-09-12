// Top-level build file
// نسخه‌ها تک‌منبع از gradle/libs.versions.toml می‌آیند (قبلاً اینجا هاردکد و ناهماهنگ بود:
// AGP 8.5.2 در برابر 8.7.3 کاتالوگ، Hilt 2.52 در برابر 2.55 — همان خطای چندثانیه‌ای تسک‌های کامپایل)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
