package com.alvand.player.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
@Singleton
class EqualizerManager @Inject constructor() {

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

    val bandCount: Int get() = runCatching { eq?.numberOfBands?.toInt() }.getOrNull()?.coerceIn(1, 10) ?: 5
    val bandRange: IntRange get() = runCatching {
        val r = eq?.bandLevelRange ?: return IntRange(-1500, 1500)
        IntRange(r[0].toInt(), r[1].toInt())
    }.getOrNull() ?: IntRange(-1500, 1500)

    fun bandFreqHz(index: Int): Int = runCatching {
        (eq?.getCenterFreq(index.toShort()) ?: 0) / 1000
    }.getOrNull() ?: 0

    fun attach(audioSessionId: Int, s: AudioSettings = _settings.value) {
        if (audioSessionId <= 0) return
        if (audioSessionId == sessionId && eq != null) {
            // همان سشن است — فقط ستینگ را به‌روز کن
            applyAll(s)
            return
        }
        // ساخت افکت روی IO تا ANR ندهد؛ کالر می‌تواند از Main صدا بزند
        // برای سازگاری سینک نگهش می‌داریم ولی بایندر سنگین را جدا try می‌کنیم
        val oldEq = eq
        val oldBass = bass
        val oldLoud = loud
        val oldReverb = reverb
        var newEq: Equalizer? = null
        try {
            newEq = Equalizer(0, audioSessionId).apply { enabled = s.eqEnabled }
        } catch (e: Exception) {
            Log.w("EQ", "Equalizer create failed sid=$audioSessionId", e)
            // EQ سالم قبلی را نابود نکن — فقط bass/loud را نگه دار
            _settings.value = s
            return
        } catch (e: UnsupportedOperationException) {
            Log.w("EQ", "Equalizer unsupported", e)
            _settings.value = s
            return
        }
        // موفق شد — حالا قدیمی‌ها را آزاد کن
        sessionId = audioSessionId
        _settings.value = s
        runCatching { oldEq?.enabled = false }
        runCatching { oldEq?.release() }
        runCatching { oldBass?.release() }
        runCatching { oldLoud?.release() }
        runCatching { oldReverb?.release() }
        eq = newEq
        bass = runCatching {
            BassBoost(0, audioSessionId).apply {
                enabled = s.bassStrength.coerceIn(0, 1000) > 0
                setStrength(s.bassStrength.coerceIn(0, 1000).toShort())
            }
        }.getOrNull()
        loud = runCatching {
            LoudnessEnhancer(audioSessionId).apply {
                enabled = s.volumeBoostDb.coerceIn(0, 10) > 0
                setTargetGain((s.volumeBoostDb.coerceIn(0, 10) * 100).coerceIn(0, 1000))
            }
        }.getOrNull()
        reverb = runCatching {
            PresetReverb(0, audioSessionId).apply {
                val validPresets = setOf(
                    PresetReverb.PRESET_NONE, PresetReverb.PRESET_SMALLROOM,
                    PresetReverb.PRESET_MEDIUMROOM, PresetReverb.PRESET_LARGEROOM,
                    PresetReverb.PRESET_MEDIUMHALL, PresetReverb.PRESET_LARGEHALL,
                    PresetReverb.PRESET_PLATE
                )
                val p = s.reverbPreset.toShort()
                enabled = s.reverbPreset > 0 && p in validPresets.map { it.toShort() }
                if (enabled) preset = p
            }
        }.getOrNull()
        applyAll(s)
    }

    /** نسخه IO برای صدا زدن از کوروتین بدون بلاک Main */
    suspend fun attachAsync(audioSessionId: Int, s: AudioSettings = _settings.value) =
        withContext(Dispatchers.IO) { attach(audioSessionId, s) }

    /** آیا اکولایزر به سشن صوتی وصل است؟ (سشن معتبر + افکت زنده) */
    fun isAttached(): Boolean = eq != null && sessionId > 0

    fun currentSessionId(): Int = sessionId

    fun applyAll(s: AudioSettings) {
        _settings.value = s
        val e = eq
        // خرابی EQ نباید bass/loud را از کار بیندازد — هر کدام try جدا
        if (e != null) {
            runCatching { e.enabled = s.eqEnabled }
            if (s.preset >= 0 && runCatching { s.preset < e.numberOfPresets }.getOrDefault(false) && !s.noiseReduction) {
                val applied = runCatching { e.usePreset(s.preset.toShort()) }.isSuccess
                if (!applied) {
                    val name = runCatching { e.getPresetName(s.preset.toShort()) }.getOrNull() ?: ""
                    runCatching { applyNamedCurve(e, name) }
                }
            } else {
                val range = bandRange
                s.bandLevels.forEachIndexed { i, lvl ->
                    if (i < runCatching { e.numberOfBands }.getOrDefault(0)) {
                        runCatching { e.setBandLevel(i.toShort(), lvl.coerceIn(range.first, range.last).toShort()) }
                    }
                }
            }
            if (s.noiseReduction) runCatching { applyNoiseReduction(s.noiseLevel, s.bandLevels) }
        }
        runCatching {
            bass?.apply {
                enabled = s.bassStrength.coerceIn(0, 1000) > 0
                setStrength(s.bassStrength.coerceIn(0, 1000).toShort())
            }
        }
        runCatching {
            loud?.apply {
                enabled = s.volumeBoostDb.coerceIn(0, 10) > 0
                setTargetGain((s.volumeBoostDb.coerceIn(0, 10) * 100).coerceIn(0, 1000))
            }
        }
    }

    /** منحنی‌های جایگزین پریست (dB در فرکانس‌های 60/230/910/3600/14000Hz) */
    private val presetCurves = mapOf(
        "rock" to floatArrayOf(4f, 3f, -1f, 2f, 4f),
        "pop" to floatArrayOf(-2f, -1f, 1f, 2f, 1f),
        "jazz" to floatArrayOf(3f, 2f, 0f, 2f, 3f),
        "classical" to floatArrayOf(3f, 2f, -1f, 1f, 2f),
        "dance" to floatArrayOf(5f, 3f, 0f, 1f, 2f),
        "bass" to floatArrayOf(5f, 4f, 1f, 0f, 0f),
        "vocal" to floatArrayOf(0f, 1f, 3f, 3f, 1f),
        "treble" to floatArrayOf(-2f, -1f, 1f, 3f, 5f),
        "latin" to floatArrayOf(3f, 2f, 0f, 2f, 3f),
        "party" to floatArrayOf(3f, 2f, 0f, 2f, 3f),
        "piano" to floatArrayOf(2f, 1f, 0f, 2f, 1f)
    )
    private val curveAnchors = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)

    /** اعمال دستی پریست با درون‌یابی لگاریتمی روی باندهای واقعی دستگاه */
    private fun applyNamedCurve(e: Equalizer, name: String) {
        val curve = presetCurves.entries.firstOrNull {
            name.lowercase().contains(it.key)
        }?.value ?: return
        val lo = bandRange.first
        val hi = bandRange.last
        for (i in 0 until runCatching { e.numberOfBands }.getOrDefault(0)) {
            val f = runCatching { e.getCenterFreq(i.toShort()) / 1000f }.getOrNull() ?: 0f
            val db = interpCurve(f, curve)
            runCatching { e.setBandLevel(i.toShort(), (db * 100).toInt().coerceIn(lo, hi).toShort()) }
        }
    }

    private fun interpCurve(freqHz: Float, curve: FloatArray): Float {
        val x = kotlin.math.log10(freqHz.coerceAtLeast(20f))
        val xs = floatArrayOf(
            kotlin.math.log10(curveAnchors[0]), kotlin.math.log10(curveAnchors[1]),
            kotlin.math.log10(curveAnchors[2]), kotlin.math.log10(curveAnchors[3]),
            kotlin.math.log10(curveAnchors[4])
        )
        if (x <= xs[0]) return curve[0]
        for (k in 0 until xs.size - 1) {
            if (x <= xs[k + 1]) {
                val t = (x - xs[k]) / (xs[k + 1] - xs[k])
                return curve[k] + t * (curve[k + 1] - curve[k])
            }
        }
        return curve.last()
    }

    /**
     * حذف نویز مبتنی بر EQ:
     * - برش Hiss (باندهای بالای 8kHz کم می‌شوند)
     * - ناچ Hum برق 50/60Hz (باند پایین کم می‌شود)
     * - کمی بوست وضوح وکال (1-4kHz)
     */
    private fun applyNoiseReduction(level: Int, baseLevels: List<Int>) {
        val e = eq ?: return
        val n = runCatching { e.numberOfBands }.getOrDefault(0)
        if (n <= 0) return
        val k = level.coerceIn(0, 100) / 100f // 0..1
        for (i in 0 until n) {
            val freq = runCatching { e.getCenterFreq(i.toShort()) / 1000 }.getOrNull() ?: 0
            val cut = when {
                freq < 120 -> (-600 * k).toInt() // hum
                freq in 1000..4000 -> (150 * k).toInt() // وضوح صدا
                freq > 8000 -> (-1200 * k).toInt() // hiss
                freq > 5000 -> (-600 * k).toInt()
                else -> 0
            }
            val base = baseLevels.getOrNull(i) ?: 0
            runCatching {
                e.setBandLevel(i.toShort(), (base + cut).coerceIn(bandRange.first, bandRange.last).toShort())
            }
        }
    }

    fun release() {
        runCatching { eq?.enabled = false }
        runCatching { bass?.enabled = false }
        runCatching { loud?.enabled = false }
        runCatching { reverb?.enabled = false }
        runCatching { eq?.release() }
        runCatching { bass?.release() }
        runCatching { loud?.release() }
        runCatching { reverb?.release() }
        eq = null; bass = null; loud = null; reverb = null
        sessionId = 0
    }
}
