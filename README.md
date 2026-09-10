# 🎵 Alvand Player — موزیک‌پلیر اندروید (گلس‌مورفیسم + سه‌بعدی)

موزیک‌پلیر مدرن اندروید در سبک **Glassmorphism** با انیمیشن‌ها و افکت‌های **3D**،
با پشتیبانی از **۱۷ زبان** زنده دنیا.

| قابلیت | وضعیت |
|---|---|
| پخش تمام فرمت‌ها (mp3/aac/ogg/opus/flac/wav/m4a/amr/midi + HLS/DASH) | ✅ ExoPlayer / Media3 |
| لینک مستقیم (Paste URL + باز کردن از اپ‌های دیگر) | ✅ |
| لیریک: امبدد تگ + فایل ‎.lrc‎ + جستجوی آنلاین (LRCLIB) + ست دستی | ✅ |
| اکولایزر 5 باند + BassBoost + تقویت صدا (LoudnessEnhancer) | ✅ |
| حذف نویز (Hiss/Hum مبتنی بر EQ) | ✅ |
| ۱۷ زبان + انتخاب زبان داخل اپ | ✅ |
| UI شیشه‌ای، وینیل چرخان 3D، اکولایزر بار متحرک | ✅ |
| بیلد خودکار در گیت‌هاب (APK/AAB) | ✅ |

## 🌍 زبان‌ها (۱۷)
English 🇬🇧 • 中文 🇨🇳 • हिन्दी 🇮🇳 • Español 🇪🇸 • Français 🇫🇷 • العربية 🇸🇦 •
Português 🇵🇹 • Русский 🇷🇺 • اردو 🇵🇰 • Bahasa Indonesia 🇮🇩 •
Deutsch 🇩🇪 • 日本語 🇯🇵 • Italiano 🇮🇹 • Türkçe 🇹🇷 •
한국어 🇰🇷 • Tiếng Việt 🇻🇳 • فارسی 🇮🇷

- زبان پیش‌فرض = زبان گوشی (خودکار).
- تغییر دستی: دکمه 🌐 بالای صفحه خانه → انتخاب زبان (بدون خروج از اپ اعمال می‌شود).
- رشته‌ها در `app/src/main/res/values-*/strings.xml` و انتخابگر در
`data/AppLocale.kt` + `ui/components/LanguageDialog.kt` است.

## 📦 ساختار
```
alvand-player/
├── app/src/main/java/com/alvand/player/
│   ├── MainActivity.kt
│   ├── data/Song.kt + SongRepository.kt + AppLocale.kt
│   ├── player/MusicPlayerManager.kt + PlaybackService.kt
│   ├── audio/EqualizerManager.kt
│   ├── lyrics/LyricsManager.kt
│   ├── ui/theme/ + ui/components/ (+ LanguageDialog) + ui/screens/
├── app/src/main/res/values-*/strings.xml   (۱۷ زبان)
├── app/src/main/res/xml/locales_config.xml
├── app/src/main/res/drawable/ic_alvand.xml (لوگو/آیکون)
├── .github/workflows/android.yml
```

## 🎨 لوگو و آیکون
- فایل اصلی: `app/src/main/res/drawable-nodpi/alvand_mark.png` (لوگوی واقعی Alvand)
- آیکون لانچر: `mipmap-anydpi-v26/ic_launcher.xml` با فورگراند
  `drawable/ic_launcher_foreground.xml` که لوگو را با حاشیه امن وسط می‌گذارد تا
  ماسک دایره‌ای/گرد لانچر، متن «Alvand player» را نبرد؛ پس‌زمینه مشکی.
- لوگوی صفحه شروع: همان PNG در اندازه بزرگ.
- وکتور `drawable/ic_alvand.xml` فقط به‌عنوان نسخه تک‌رنگ (monochrome) آیکون نگه داشته شده.

## 🚀 انتشار روی گیت‌هاب (قدم‌به‌قدم)

```bash
# ۱) ریپوی جدید در github.com/new بسازید (مثلا alvand-player)
# ۲) محتویات همین پوشه alvand-player را در روت ریپو کپی کنید، بعد:
git init -b main
git add .
git commit -m "feat: Alvand Player initial"
git remote add origin https://github.com/<user>/alvand-player.git
git push -u origin main
```

بعد از پوش، تب **Actions** → ورک‌فلو `Android CI` اجرا می‌شود و خروجی‌ها در **Artifacts** قرار می‌گیرد:
- `alvand-player-debug-apk` → نصب مستقیم روی گوشی
- `alvand-player-release-aab` → انتشار در گوگل‌پلی

## 🛠 بیلد لوکال
```bash
./gradlew :app:assembleDebug
# خروجی: app/build/outputs/apk/debug/app-debug.apk
```

## 🎧 فرمت‌ها
ExoPlayer به‌صورت native از mp3, aac, ogg/vorbis, opus, flac, wav, m4a/alac, amr, midi, m3u8, mpd پشتیبانی می‌کند.
برای wma می‌توانید اکستنشن FFmpeg مدیا۳ را اضافه کنید (`media3-decoder-ffmpeg`).

## 📝 لیریک
1. **امبدد:** تگ USLT/SYLT داخل فایل خودکار خوانده و نمایش داده می‌شود.
2. **فایل هم‌نام:** `song.lrc` کنار آهنگ.
3. **آنلاین:** دکمه «دریافت متن» → API رایگان LRCLIB.
4. **دستی:** Paste/Set → ذخیره به‌صورت `.lrc`.

## 🎚 اکولایزر / تقویت / نویز
- اکولایزر ۵ باند + پریست (Normal/Pop/Rock/Jazz/Classical/Bass…)
- BassBoost و Reverb
- تقویت صدا تا ‎+10dB‎ با LoudnessEnhancer
- حذف نویز: برش Hiss فرکانس‌بالا + حذف Hum ‏50/60Hz‏ + بوست وضوح وکال
