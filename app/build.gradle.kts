plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.alvand.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alvand.player"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
        // زبان‌های پشتیبانی‌شده (۱۷ لوکیل)
        resConfigs(
            "en", "zh", "hi", "es", "fr", "ar", "pt", "ru", "ur",
            "in", "de", "ja", "it", "tr", "ko", "vi", "fa"
        )
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
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
