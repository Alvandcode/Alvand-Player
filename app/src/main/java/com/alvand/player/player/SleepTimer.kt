package com.alvand.player.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** وضعیت تایمر خواب */
data class SleepTimerState(
    val active: Boolean = false,
    /** میلی‌ثانیه باقی‌مانده */
    val remainingMs: Long = 0L,
    /** کل مدت تنظیم‌شده (برای نوار پیشرفت) */
    val totalMs: Long = 0L,
    /** آیا در فاز محو صدا هستیم؟ */
    val fading: Boolean = false
) {
    val progress: Float get() =
        if (totalMs > 0) (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
}

/**
 * تایمر خواب با Fade-out خطی صدا.
 *
 * - تیک هر ۱ ثانیه؛ در [fadeMs] پایانی ولوم از ۱ به ۰ می‌رسد.
 * - با تمام شدن: [onExpire] (توقف پخش) صدا زده و ولوم به ۱ برمی‌گردد.
 * - با کنسل دستی هم ولوم ریست می‌شود تا پخش بعدی بی‌صدا نباشد.
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val setVolume: (Float) -> Unit,
    private val onExpire: () -> Unit
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state

    private var job: Job? = null

    /** شروع تایمر؛ fade پیش‌فرض: min(۳۰ثانیه، یک‌چهارم کل و حداقل ۵ثانیه) */
    fun start(totalMs: Long, fadeMs: Long = defaultFade(totalMs)) {
        cancelInternal(restoreVolume = true)
        if (totalMs <= 0) return
        val fade = fadeMs.coerceIn(0L, totalMs)
        _state.value = SleepTimerState(active = true, remainingMs = totalMs, totalMs = totalMs)
        job = scope.launchTimer(totalMs, fade)
    }

    fun cancel() {
        cancelInternal(restoreVolume = true)
    }

    private fun cancelInternal(restoreVolume: Boolean) {
        job?.cancel()
        job = null
        if (restoreVolume) runCatching { setVolume(1f) }
        _state.value = SleepTimerState()
    }

    private fun CoroutineScope.launchTimer(totalMs: Long, fadeMs: Long): Job =
        kotlinx.coroutines.launch {
            var remaining = totalMs
            while (remaining > 0) {
                delay(1000)
                remaining = (remaining - 1000).coerceAtLeast(0)
                val fading = fadeMs > 0 && remaining <= fadeMs
                if (fading) {
                    val v = (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
                    runCatching { setVolume(v) }
                }
                _state.value = SleepTimerState(
                    active = remaining > 0,
                    remainingMs = remaining,
                    totalMs = totalMs,
                    fading = fading && remaining > 0
                )
            }
            runCatching { onExpire() }
            runCatching { setVolume(1f) }
            _state.value = SleepTimerState()
        }

    companion object {
        fun defaultFade(totalMs: Long): Long =
            minOf(30_000L, (totalMs / 4).coerceAtLeast(5_000L))

        /** فرمت باقی‌مانده: ۴۵:۰۰ یا ۱:۰۵:۰۰ */
        fun formatRemaining(ms: Long): String {
            val s = (ms / 1000).coerceAtLeast(0)
            val h = s / 3600
            val m = (s % 3600) / 60
            val sec = s % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
            else "%d:%02d".format(m, sec)
        }
    }
}
