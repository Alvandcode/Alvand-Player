package com.alvand.player

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** نقطه ورود Hilt — گراف وابستگی‌های اپ (پلیر، ریپو، ویومدل) */
@HiltAndroidApp
class AlvandApp : Application()
