# ─── Alvand Player — قواعد R8 سبک‌شده ───
# کتابخانه‌های زیر (media3/coil/okhttp/hilt/room/datastore/compose) هرکدام
# consumer rules خودشان را داخل AAR حمل می‌کنند؛ keep سراسری فقط APK را
# باد می‌کرد و مزیت minification را صفر می‌کرد. فقط موارد واقعاً لازم مانده.

# خوانایی stack trace در گزارش کرش درون‌اپ (CrashDialog/CrashLog)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# نقطه ورود Hilt (consumer rules Hilt بقیه را هندل می‌کند)
-keep class com.alvand.player.AlvandApp { *; }

# Room — نگهداری سبک برای DB و Entity ها
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# نویزهای شناخته‌شده وابسته به نسخه
-dontwarn okhttp3.**
-dontwarn okio.**
