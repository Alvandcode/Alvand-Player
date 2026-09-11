plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// تک‌منبع نام و ورژن — در اسم فایل خروجی هم استفاده می‌شود
val appVersionName = "1.1.0"
val appVersionCode = 3

base {
    // خروجی‌ها: AlvandPlayer-v1.0.1-debug.apk و AlvandPlayer-v1.0.1-release.aab
    archivesName.set("AlvandPlayer-v$appVersionName")
}

android {
    namespace = "com.alvand.player"
    // اندروید ۶ (API 23) تا اندروید ۱۷ (API 37): کف ۲۳ سقف Jetpack است،
    // روی ۱۷ بدون تارگت مستقیم هم نصب و اجرا می‌شود (forward compatible)
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alvand.player"
        minSdk = 23
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables { useSupportLibrary = true }
        // زبان‌های پشتیبانی‌شده (۱۷ لوکیل)
        resConfigs(
            "en", "zh", "hi", "es", "fr", "ar", "pt", "ru", "ur",
            "in", "de", "ja", "it", "tr", "ko", "vi", "fa"
        )
    }
    signingConfigs {
        // کانفیگ «alvand»: کی‌استور ثابت پروژه. اگر فایل/پسورد نباشد، استفاده نمی‌شود
        // و هر buildType به امضای پیش‌فرض خودش برمی‌گردد.
        create("alvand") {
            storeFile = file(System.getenv("ALVAND_KEYSTORE_PATH") ?: "alvand-release.keystore")
            storeType = "PKCS12"
            storePassword = System.getenv("ALVAND_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ALVAND_KEY_ALIAS") ?: "alvand"
            keyPassword = System.getenv("ALVAND_KEY_PASSWORD")
        }
    }
    // true یعنی کی‌استور ثابت در دسترس است (فایل هست + پسورد ست شده)
    // در CI سکرت‌ها ست‌اند؛ لوکال فقط وقتی فایل و env هر دو باشند.
    val stableKsFile = file(System.getenv("ALVAND_KEYSTORE_PATH") ?: "alvand-release.keystore")
    val hasStableKs = stableKsFile.exists() && !System.getenv("ALVAND_KEYSTORE_PASSWORD").isNullOrEmpty()
    buildTypes {
        release {
            // امضای پایدار: نسخه جدید همیشه روی قبلی نصب می‌شود
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
            // مهم: بیلد debug هم با همان کلید ثابت امضا می‌شود تا خروجی‌های
            // هر ران CI روی هم نصب شوند (کلید debug رانرها موقتی است و هر بار عوض می‌شود)
            if (hasStableKs) signingConfig = signingConfigs.getByName("alvand")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
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
}
