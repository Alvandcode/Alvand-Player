plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// تک‌منبع نام و ورژن — در اسم فایل خروجی هم استفاده می‌شود
val appVersionName = "1.6.6"
val appVersionCode = 13

base {
    // خروجی‌ها: AlvandPlayer-v1.3.0-debug.apk و AlvandPlayer-v1.3.0-release.aab
    archivesName.set("AlvandPlayer-v$appVersionName")
}

android {
    namespace = "com.alvand.player"
    // اندروید ۶ (API 23) تا اندروید ۱۶ (API 36).
    // compileSdk/targetSdk=36 برای الزام Play (آگوست 2026) لازم است.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alvand.player"
        minSdk = 23
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables { useSupportLibrary = true }
        // زبان‌های پشتیبانی‌شده (۱۷ لوکیل) — باید با locales_config.xml یکی باشد.
        // نکته: کد مدرن اندونزیایی "id" است نه "in" قدیمی.
        resConfigs(
            "en", "zh", "hi", "es", "fr", "ar", "pt", "ru", "ur",
            "id", "de", "ja", "it", "tr", "ko", "vi", "fa"
        )
    }
    signingConfigs {
        // کانفیگ «alvand»: کی‌استور فقط از مسیر امن خارج از ریپو یا env می‌آید.
        // هرگز فایل keystore را داخل پوشه پروژه نگه ندارید (ریسک لو رفتن با zip/backup).
        // مسیر پیش‌فرض روت ریپو است چون فایل قدیمی آنجا بود؛ برای امنیت به ../ یا %USERPROFILE% منتقل کنید.
        create("alvand") {
            val defaultPath = rootDir.resolve("alvand-release.keystore").absolutePath
            storeFile = file(System.getenv("ALVAND_KEYSTORE_PATH") ?: defaultPath)
            storeType = "PKCS12"
            storePassword = System.getenv("ALVAND_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ALVAND_KEY_ALIAS") ?: "alvand"
            keyPassword = System.getenv("ALVAND_KEY_PASSWORD")
        }
    }
    // true یعنی کی‌استور ثابت در دسترس است (فایل هست + پسورد ست شده)
    // در CI سکرت‌ها ست‌اند؛ لوکال فقط وقتی فایل و env هر دو باشند.
    val stableKsFile = file(System.getenv("ALVAND_KEYSTORE_PATH") ?: rootDir.resolve("alvand-release.keystore").absolutePath)
    val hasStableKs = stableKsFile.exists() && !System.getenv("ALVAND_KEYSTORE_PASSWORD").isNullOrEmpty()
    buildTypes {
        release {
            // امضای پایدار: نسخه جدید همیشه روی قبلی نصب می‌شود.
            // اگر کی‌استور نباشد عمداً خطا می‌دهیم تا APK با کلید موقت debug
            // به‌اشتباه با نام release منتشر نشود (با checkReleaseSigning کنترل می‌شود).
            signingConfig = if (hasStableKs) signingConfigs.getByName("alvand")
            else signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            // امنیت: debug همیشه با کلید debug امضا می‌شود تا کلید release
            // در آرتیفکت‌های پابلیک debug لو نرود و تفکیک debug/release حفظ شود.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.coroutines)
    implementation(libs.datastore.prefs)
    implementation(libs.appcompat) // انتخاب زبان داخل اپ (per-app locales)
    implementation(libs.palette.ktx) // پالت رنگی داینامیک از کاور
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    val composeBom = libs.compose.bom
    implementation(platform(composeBom))
    androidTestImplementation(platform(composeBom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    implementation(libs.coil.compose)

    // Media3 — پشتیبانی از mp3/aac/ogg/opus/flac/wav/m4a/amr/midi + HLS/DASH (لینک مستقیم و استریم)
    // برای wma/alac اضافه: media3-decoder-ffmpeg را می‌توان به‌صورت ماژول جدا اضافه کرد
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.okhttp)

    // تست واحد — پوشش منطق بحرانی (Song، SleepTimer، Palette، Locale)
    // NOTE: فقط وابستگی‌های واقعاً استفاده‌شده (turbine/mockk استفاده نمی‌شوند و حذف شدند
    // تا ریسک resolution نداشته باشیم)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.junit)
}
