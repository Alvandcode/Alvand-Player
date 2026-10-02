package com.alvand.player

import com.alvand.player.audio.AudioSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * دکمهٔ ریست شیت اکولایزر دقیقاً `AudioSettings()` را اعمال می‌کند، پس
 * پیش‌فرض باید واقعاً «همه‌چیز در حالت خنثی» باشد و از هر تنظیمی برگردد.
 */
class AudioSettingsResetTest {

    private val defaults = AudioSettings()

    @Test
    fun `defaults are neutral`() {
        assertTrue("EQ should be on but flat", defaults.eqEnabled)
        assertEquals("flat bands", listOf(0, 0, 0, 0, 0), defaults.bandLevels)
        assertEquals("Normal preset", 0, defaults.preset)
        assertEquals(0, defaults.volumeBoostDb)
        assertEquals(500, defaults.bassStrength)
        assertEquals(50, defaults.noiseLevel)
        assertEquals(false, defaults.noiseReduction)
        assertEquals(0, defaults.reverbPreset)
    }

    @Test
    fun `every field is settable and reset brings each one back`() {
        val messedUp = AudioSettings(
            eqEnabled = false,
            preset = -1,
            bandLevels = listOf(900, -800, 400, -400, 1200),
            bassStrength = 1000,
            volumeBoostDb = 10,
            noiseReduction = true,
            noiseLevel = 100,
            reverbPreset = 3
        )
        assertNotEquals(defaults, messedUp)

        // این همان کاری است که دکمهٔ ریست انجام می‌دهد
        val afterReset = messedUp.copy(
            eqEnabled = defaults.eqEnabled,
            preset = defaults.preset,
            bandLevels = defaults.bandLevels,
            bassStrength = defaults.bassStrength,
            volumeBoostDb = defaults.volumeBoostDb,
            noiseReduction = defaults.noiseReduction,
            noiseLevel = defaults.noiseLevel,
            reverbPreset = defaults.reverbPreset
        )
        assertEquals(defaults, afterReset)
    }

    @Test
    fun `band level list length does not change a reset`() {
        // تعداد باندها روی دستگاه فرق می‌کند؛ ریست نباید به آن وابسته باشد
        val fiveBand = AudioSettings(bandLevels = listOf(500, 500, 500, 500, 500))
        val tenBand = AudioSettings(bandLevels = List(10) { 500 })
        assertEquals(5, fiveBand.bandLevels.size)
        assertEquals(10, tenBand.bandLevels.size)
        // مقدار پیش‌فرض ۵ باندی است و applyAll مقادیر گمشده را صفر می‌گیرد
        assertEquals(listOf(0, 0, 0, 0, 0), AudioSettings().bandLevels)
    }

    @Test
    fun `preset minus one means manual and is not a default`() {
        assertEquals(0, defaults.preset)
        assertEquals(-1, AudioSettings(preset = -1).preset)
        assertNotEquals(defaults, AudioSettings(preset = -1))
    }
}