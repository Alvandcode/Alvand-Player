package com.alvand.player

import android.app.Application
import com.alvand.player.data.CrashLog
import dagger.hilt.android.HiltAndroidApp

/** نقطه ورود Hilt — گراف وابستگی‌های اپ (پلیر، ریپو، ویومدل) */
@HiltAndroidApp
class AlvandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // شکارچی کرش باید اول از همه نصب شود تا هیچ خطایی گم نشود
        CrashLog.install(this)
    }
}
