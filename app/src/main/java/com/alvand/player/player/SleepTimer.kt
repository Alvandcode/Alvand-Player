package com.alvand.player.player

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** وضعیت تایمر خواب */
data class SleepTimerState(
    val active: Boolean = false,
    /** میلی‌ثانیه باقی‌مانده */
    val remainingMs: Long = 0L,
    /** کل مدت تنظیم‌شده (برای نوار پیشرفت) */
    val totalMs: Long = 0L,
    /** آیا در فاز محو صدا هستیم؟ */
    val fading: Boolean = false,
    /** true یعنی همین الان با انقضا تمام شده (نه کنسل دستی) */
    val justFinished: Boolean = false
) {
    val progress: Float get() =
        if (totalMs > 0) (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
}

/**
 * تایمر خواب با Fade-out صدا.
 *
 * - تیک هر ۱ ثانیه با ساعت elapsedRealtime (بدون درفت زیر بار/Doze).
 * - ولوم اولیه کاربر ذخیره و بعد از انقضا/کنسل به همان برمی‌گردد (نه همیشه 1f).
 * - ریس cancel لحظه انقضا با generation حل شده: فقط آخرین نسل حق expire دارد.
 * - مرگ اسکوپ یا کنسل خارجی state را گیر نمی‌اندازد (finally + try/catch دور launch).
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val setVolume: (Float) -> Unit,
    private val getVolume: () -> Float = { 1f },
    private val onExpire: () -> Unit
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state

    private var job: Job? = null
    private val generation = AtomicLong(0)
    private var baseVolume: Float = 1f

    /** حداکثر تایمر: ۱۲ ساعت تا جاب چندساله ساخته نشود */
    companion object {
        const val MAX_MS = 12L * 60 * 60 * 1000
        const val MIN_MS = 5_000L
        fun defaultFade(totalMs: Long): Long {
            if (totalMs <= MIN_MS) return totalMs / 2
            return minOf(30_000L, (totalMs / 4).coerceAtLeast(5_000L)).coerceAtMost(totalMs)
        }

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

    /** شروع تایمر؛ fade پیش‌فرض: min(۳۰ثانیه، یک‌چهارم کل و حداقل ۵ثانیه) */
    @Synchronized
    fun start(totalMs: Long, fadeMs: Long = defaultFade(totalMs)) {
        if (totalMs < MIN_MS) return
        val total = totalMs.coerceAtMost(MAX_MS)
        val fade = fadeMs.coerceIn(0L, total)
        // اگر تایمر قبلی در فاز fade نبود، ولوم کاربر را نپران
        val wasFading = _state.value.fading
        cancelInternal(restoreVolume = wasFading)
        baseVolume = runCatching { getVolume() }.getOrNull()?.takeIf { it in 0f..1f } ?: 1f
        _state.value = SleepTimerState(active = true, remainingMs = total, totalMs = total, fading = false)
        val gen = generation.incrementAndGet()
        try {
            job = scope.launchTimer(total, fade, gen, baseVolume)
        } catch (_: Exception) {
            // اسکوپ مرده است — state را گیر نینداز
            _state.value = SleepTimerState()
        }
    }

    fun cancel() {
        cancelInternal(restoreVolume = true)
    }

    @Synchronized
    private fun cancelInternal(restoreVolume: Boolean) {
        generation.incrementAndGet()
        job?.cancel()
        job = null
        if (restoreVolume) runCatching { setVolume(baseVolume.coerceIn(0f, 1f)) }
        _state.value = SleepTimerState()
    }

    private fun CoroutineScope.launchTimer(totalMs: Long, fadeMs: Long, gen: Long, startVolume: Float): Job =
        launch {
            val deadline = SystemClock.elapsedRealtime() + totalMs
            try {
                // تیک اول سریع تا UI یک ثانیه دیر نکند
                _state.value = SleepTimerState(active = true, remainingMs = totalMs, totalMs = totalMs, fading = fadeMs >= totalMs)
                if (fadeMs >= totalMs) runCatching { setVolume(startVolume) }
                while (true) {
                    delay(1000)
                    if (generation.get() != gen) return@launch // کنسل شده‌ایم
                    val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                    val fading = fadeMs > 0 && remaining <= fadeMs && remaining > 0
                    if (fading) {
                        // fade ادراکی نرم‌تر: جذر نسبت به‌جای خطی
                        val linear = (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
                        val perceptual = kotlin.math.sqrt(linear)
                        runCatching { setVolume((startVolume * perceptual).coerceIn(0f, 1f)) }
                    }
                    _state.value = SleepTimerState(
                        active = remaining > 0,
                        remainingMs = remaining,
                        totalMs = totalMs,
                        fading = fading
                    )
                    if (remaining <= 0) break
                }
                // فقط اگر هنوز آخرین نسل هستیم expire کن (ضد ریس cancel)
                if (generation.get() == gen) {
                    runCatching { onExpire() }
                    _state.value = SleepTimerState(justFinished = true)
                }
            } finally {
                // اگر کنسل خارجی خوردیم و هنوز همین نسل است، state مرده نماند
                if (generation.get() == gen && _state.value.active) {
                    _state.value = SleepTimerState()
                }
                runCatching { setVolume(startVolume.coerceIn(0f, 1f)) }
            }
        }
}
