package com.alvand.player

import com.alvand.player.data.AccentThemeMode
import com.alvand.player.ui.theme.AccentThemes
import com.alvand.player.ui.theme.DarkPalette
import com.alvand.player.ui.theme.LightPalette
import com.alvand.player.ui.theme.accentThemeAt
import com.alvand.player.ui.theme.tintedWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentThemeTest {

    @Test
    fun `unknown id falls back to minimal`() {
        assertEquals(AccentThemeMode.MONO, accentThemeAt(-1).id)
        assertEquals(AccentThemeMode.MONO, accentThemeAt(999).id)
    }

    @Test
    fun `every mode has exactly one theme`() {
        assertEquals(AccentThemeMode.COUNT, AccentThemes.size)
        AccentThemes.forEachIndexed { i, t -> assertEquals(i, t.id) }
    }

    @Test
    fun `colored themes are not mono and readable in both schemes`() {
        val colored = AccentThemes.filter { it.id != AccentThemeMode.MONO }
        assertEquals(3, colored.size)
        colored.forEach { t ->
            assertTrue("${t.id} should be colored", t.isColored)
            // رنگ روشن باید تیره باشد و برعکس، وگرنه در یکی از حالت‌ها ناخواناست
            assertNotEquals(t.light, t.dark)
        }
    }

    @Test
    fun `mono theme leaves palettes untouched`() {
        val mono = accentThemeAt(AccentThemeMode.MONO)
        assertSame(LightPalette, LightPalette.tintedWith(mono, isDark = false))
        assertSame(DarkPalette, DarkPalette.tintedWith(mono, isDark = true))
    }

    @Test
    fun `colored theme pulls glass closer to accent than matte surfaces`() {
        val royal = accentThemeAt(AccentThemeMode.ROYAL)
        val tinted = LightPalette.tintedWith(royal, isDark = false)
        val accent = royal.accent(isDark = false)

        assertNotEquals(LightPalette.glass, tinted.glass)
        assertNotEquals(LightPalette.glassBorder, tinted.glassBorder)

        // شیشه باید بیشتر از کارت مات به رنگ تم نزدیک شود
        val glassShift = distance(LightPalette.glass, tinted.glass)
        val cardShift = distance(LightPalette.card, tinted.card)
        assertTrue("glass=$glassShift should exceed card=$cardShift", glassShift > cardShift)

        // و هر دو باید به سمت accent حرکت کرده باشند
        assertTrue(distance(LightPalette.glass, tinted.glass) < distance(LightPalette.glass, accent))
    }

    private fun distance(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Float {
        val dr = a.red - b.red
        val dg = a.green - b.green
        val db = a.blue - b.blue
        return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
    }

    @Test
    fun `dark and light variants use different accents`() {
        val ocean = accentThemeAt(AccentThemeMode.OCEAN)
        assertNotEquals(ocean.accent(isDark = true), ocean.accent(isDark = false))
        assertNotEquals(ocean.deep(isDark = true), ocean.deep(isDark = false))
    }
}
