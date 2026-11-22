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
    fun `supported locales cover all shipped translations`() {
        assertEquals(
            listOf("en", "fa", "ar", "de", "es", "fr", "hi", "in", "it", "ja", "ko", "pt", "ru", "tr", "ur", "vi", "zh"),
            AppLocale.all.map { it.code }
        )
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
    }

    @Test
    fun `normalize maps supported region tags`() {
        assertEquals("zh", AppLocale.normalize("zh-Hans-CN"))
        assertEquals("pt", AppLocale.normalize("pt-BR"))
    }
}
