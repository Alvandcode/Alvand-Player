package com.alvand.player

import com.alvand.player.data.AppLocale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class AppLocaleTest {

    @Test
    fun `supported locales are english and persian`() {
        assertEquals(listOf("en", "fa"), AppLocale.all.map { it.code })
    }

    @Test
    fun `normalize strips region`() {
        assertEquals("fa", AppLocale.normalize("fa-IR"))
        assertEquals("en", AppLocale.normalize("en-US"))
    }

    @Test
    fun `normalize falls back to en`() {
        assertEquals("en", AppLocale.normalize("xx"))
        assertEquals("en", AppLocale.normalize(""))
        assertEquals("en", AppLocale.normalize("zh-Hans-CN"))
    }
}
