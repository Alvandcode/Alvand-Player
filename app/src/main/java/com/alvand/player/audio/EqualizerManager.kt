package com.alvand.player.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** تنظیمات صوتی قابل ذخیره‌سازی */
data class AudioSettings(
    val eqEnabled: Boolean = true,
    val preset: Int = 0, // ایندکس پریست یا -1 یعنی دستی
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0), // میلی‌بل
    val bassStrength: Int = 500, // 0..1000
    val volumeBoostDb: Int = 0, // 0..10 dB تقویت صدا
    val noiseReduction: Boolean = false,
    val noiseLevel: Int = 50, // 0..100 شدت حذف نویز
    val reverbPreset: Int = 0 // 0=off
)

/**
 * مدیریت اکولایزر + تقویت صدا + حذف نویز.
 * به audioSessionId پلیر وصل می‌شود (ExoPlayer.audioSessionId).
 *
 * حذف نویز واقعی پخش (playback) در اندروید API اختصاصی ندارد؛
 * ترکیب برش Hiss/Hum با EQ پیاده شده است.
 */
class EqualizerManager {

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var loud: LoudnessEnhancer? = null
    private var reverb: PresetReverb? = null
    private var sessionId: Int = 0

    private val _settings = MutableStateFlow(AudioSettings())
    val settings: StateFlow<AudioSettings> = _settings

    val presets: List<String> get() = runCatching {
        eq?.let { e -> (0 until e.numberOfPresets).map { e.getPresetName(it.toShort()) } }
    }.getOrNull() ?: listOf("Normal", "Pop", "Rock", "Jazz", "Classical", "Bass", "Vocal", "Treble")

    val bandCount: Int get() = runCatching { eq?.numberOfBands?.toInt() }.getOrNull() ?: 5
    val bandRange: IntRange get() = runCatching {
        val r = eq?.bandLevelRange ?: return IntRange(-1500, 1500)
        IntRange(r[0].toInt(), r[1].toInt())
    }.getOrNull() ?: IntRange(-1500, 1500)

    fun bandFreqHz(index: Int): Int = runCatching {
        (eq?.getCenterFreq(index.toShort()) ?: 0) / 1000
    }.getOrNull() ?: 0

    fun attach(audioSessionId: Int, s: AudioSettings = _settings.value) {
        if (audioSessionId <= 0) return
        release()
        sessionId = audioSessionId
        _settings.value = s
        runCatching {
            eq = Equalizer(0, audioSessionId).apply { enabled = s.eqEnabled }
            bass = BassBoost(0, audioSessionId).apply {
                enabled = s.bassStrength > 0
                setStrength(s.bassStrength.toShort().coerceIn(0, 1000))
            }
            loud = LoudnessEnhancer(audioSessionId).apply {
                enabled = s.volumeBoostDb > 0
                // هر 100 واحد ≈ 1dB
                setTargetGain(s.volumeBoostDb * 100)
            }
            reverb = PresetReverb(0, audioSessionId).apply {
                enabled = s.reverbPreset > 0
                if (s.reverbPreset > 0) preset = s.reverbPreset.toShort()
            }
            applyAll(s)
        }
    }

    fun applyAll(s: AudioSettings) {
        _settings.value = s
        val e = eq ?: return
        runCatching {
            e.enabled = s.eqEnabled
            if (s.preset >= 0 && s.preset < e.numberOfPresets && !s.noiseReduction) {
                e.usePreset(s.preset.toShort())
            } else {
                s.bandLevels.forEachIndexed { i, lvl ->
                    if (i < e.numberOfBands) e.setBandLevel(i.toShort(), lvl.toShort())
                }
            }
            if (s.noiseReduction) applyNoiseReductionLocked(s.noiseLevel)
            bass?.apply {
                enabled = s.bassStrength > 0
                setStrength(s.bassStrength.toShort().coerceIn(0, 1000))
            }
            loud?.apply {
                enabled = s.volumeBoostDb > 0
                setTargetGain((s.volumeBoostDb * 100).coerceIn(0, 1000))
            }
        }
    }

    /**
     * حذف نویز مبتنی بر EQ:
     * - برش Hiss (باندهای بالای 8kHz کم می‌شوند)
     * - ناچ Hum برق 50/60Hz (باند پایین کم می‌شود)
     * - کمی بوست وضوح وکال (1-4kHz)
     */
    private fun applyNoiseReductionLocked(level: Int) {
        val e = eq ?: return
        val n = e.numberOfBands
        if (n <= 0) return
        val k = level / 100f // 0..1
        for (i in 0 until n) {
            val freq = runCatching { e.getCenterFreq(i.toShort()) / 1000 }.getOrNull() ?: 0
            val cut = when {
                freq < 120 -> (-600 * k).toInt() // hum
                freq in 1000..4000 -> (150 * k).toInt() // وضوح صدا
                freq > 8000 -> (-1200 * k).toInt() // hiss
                freq > 5000 -> (-600 * k).toInt()
                else -> 0
            }
            val base = _settings.value.bandLevels.getOrNull(i) ?: 0
            e.setBandLevel(i.toShort(), (base + cut).coerceIn(bandRange.first, bandRange.last).toShort())
        }
    }

    fun release() {
        runCatching { eq?.release() }
        runCatching { bass?.release() }
        runCatching { loud?.release() }
        runCatching { reverb?.release() }
        eq = null; bass = null; loud = null; reverb = null
    }
}
