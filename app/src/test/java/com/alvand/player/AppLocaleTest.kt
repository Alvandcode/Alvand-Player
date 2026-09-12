package com.alvand.player

import com.alvand.player.data.AppLocale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class AppLocaleTest {

    @Test
    fun `normalize maps legacy in to id`() {
        assertEquals("id", AppLocale.normalize("in"))
        assertEquals("id", AppLocale.normalize("id"))
        assertEquals("id", AppLocale.normalize("IN"))
    }

    @Test
    fun `normalize strips region`() {
        assertEquals("zh", AppLocale.normalize("zh-Hans-CN"))
        assertEquals("fa", AppLocale.normalize("fa-IR"))
        assertEquals("en", AppLocale.normalize("en-US"))
    }

    @Test
    fun `normalize falls back to en`() {
        assertEquals("en", AppLocale.normalize("xx"))
        assertEquals("en", AppLocale.normalize(""))
    }
}
