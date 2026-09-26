package com.alvand.player

import com.alvand.player.player.SleepTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    @Test
    fun `defaultFade handles tiny totals`() = runTest {
        // رگرسیون: total<5s قبلاً fade>total می‌شد و ولوم ناگهان می‌افتاد
        assertTrue(SleepTimer.defaultFade(4000) <= 4000)
        assertTrue(SleepTimer.defaultFade(60_000) in 1..60_000)
        assertEquals(30_000L, SleepTimer.defaultFade(600_000L))
    }

    @Test
    fun `formatRemaining`() {
        assertEquals("0:05", SleepTimer.formatRemaining(5000))
        assertEquals("1:00", SleepTimer.formatRemaining(60_000))
        assertEquals("1:05:00", SleepTimer.formatRemaining(3900_000))
    }

    @Test
    fun `start rejects too-small total`() = runTest {
        val timer = SleepTimer(
            scope = this,
            setVolume = {},
            getVolume = { 0.7f },
            onExpire = { fail("should not expire") },
            elapsedRealtime = { testScheduler.currentTime }
        )
        timer.start(1000)
        testScheduler.advanceTimeBy(5000)
        assertFalse(timer.state.value.active)
    }

    @Test
    fun `expire pauses and restores base volume`() = runTest {
        var vol = 0.7f
        var expired = false
        val timer = SleepTimer(
            scope = this,
            setVolume = { vol = it },
            getVolume = { 0.7f },
            onExpire = { expired = true },
            elapsedRealtime = { testScheduler.currentTime }
        )
        timer.start(5000, fadeMs = 2000)
        testScheduler.advanceTimeBy(5500)
        testScheduler.runCurrent()
        assertTrue(expired)
        // ولوم به ولوم اولیه کاربر برمی‌گردد، نه همیشه 1f
        assertEquals(0.7f, vol, 0.01f)
    }

    @Test
    fun `cancel before expiry prevents onExpire`() = runTest {
        var expired = false
        val timer = SleepTimer(
            scope = this,
            setVolume = {},
            onExpire = { expired = true },
            elapsedRealtime = { testScheduler.currentTime }
        )
        timer.start(5000, fadeMs = 1000)
        testScheduler.advanceTimeBy(2000)
        timer.cancel()
        testScheduler.advanceTimeBy(5000)
        testScheduler.runCurrent()
        assertFalse(expired)
        assertFalse(timer.state.value.active)
    }
}
