-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
# Hilt / Dagger (KSP) — نقطه ورود و ماژول‌ها باید بمانند
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class com.alvand.player.AlvandApp { *; }
-keep class com.alvand.player.** { *; }
-keepclasseswithmembernames class * {
    @dagger.hilt.android.HiltAndroidApp *;
}
-keepclasseswithmembernames class * {
    @dagger.hilt.android.AndroidEntryPoint *;
}
# Coil / OkHttp — رفلکشن decoder و platform
-keep class coil.** { *; }
-dontwarn coil.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
# DataStore / Proto
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**
# Palette / Compose — انیمیشن و Canvas نباید strip شود
-keep class androidx.palette.graphics.** { *; }
-keep class androidx.compose.** { *; }
# مدل‌های اپ (Gson/Moshi نداریم ولی برای R8-minified crashes)
-keep class com.alvand.player.data.** { *; }
-keep class com.alvand.player.audio.** { *; }
-keep class com.alvand.player.lyrics.** { *; }
# Room — DAO و Entity نباید strip شوند
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**
