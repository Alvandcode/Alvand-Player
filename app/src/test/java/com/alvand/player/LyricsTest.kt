package com.alvand.player

import com.alvand.player.lyrics.LyricsManager
import org.junit.Assert.*
import org.junit.Test

class LyricsTest {

    @Test
    fun `parseLrc basic`() {
        val raw = "[00:01.00]hello\n[00:05.50]world"
        val lines = LyricsManager.parseLrc(raw)
        assertEquals(2, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals(5500L, lines[1].timeMs)
        assertEquals("hello", lines[0].text)
    }

    @Test
    fun `parseLrc ignores metadata and empty text`() {
        val raw = "[ti:title]\n[ar:artist]\n[00:10.00]"
        assertTrue(LyricsManager.parseLrc(raw).isEmpty())
    }

    @Test
    fun `online lyrics require local miss and explicit opt in`() {
        assertTrue(LyricsManager.shouldFetchOnline(hasLocalLyrics = false, onlineEnabled = true))
        assertFalse(LyricsManager.shouldFetchOnline(hasLocalLyrics = false, onlineEnabled = false))
        assertFalse(LyricsManager.shouldFetchOnline(hasLocalLyrics = true, onlineEnabled = true))
    }

    @Test
    fun `parseLrc rejects out of range seconds`() {
        val raw = "[00:60.00]bad\n[01:02.00]good"
        val lines = LyricsManager.parseLrc(raw)
        assertTrue(lines.none { it.text == "bad" })
        assertTrue(lines.any { it.text == "good" })
    }

    @Test
    fun `parseLrc sorts by time`() {
        val raw = "[00:10.00]b\n[00:02.00]a"
        val lines = LyricsManager.parseLrc(raw)
        assertEquals("a", lines[0].text)
        assertEquals("b", lines[1].text)
    }
}
