# 🎵 Alvand Player — موزیک‌پلیر مینیمال سیاه‌سفید اندروید

موزیک‌پلیر اندروید با رابط مینیمال مشکی‌سفید: تک‌صفحه پخش با پنل آرت کشیده و
حلقه پیشرفت لمسی دور نیم‌دایره، لیست آهنگ‌ها با کشیدن نوار پایین به بالا،
کارت لیریک با متن متحرک برای خط‌های بلند.

| قابلیت | وضعیت |
|---|---|
| پخش تمام فرمت‌ها (mp3/aac/ogg/opus/flac/wav/m4a/amr/midi + HLS/DASH) | ✅ ExoPlayer / Media3 |
| کنترل از نوار اعلان، لاک‌اسکرین و خروجی مدیا + پخش پس‌زمینه | ✅ MediaSession |
| لینک مستقیم (Paste URL + باز کردن از اپ‌های دیگر) | ✅ |
| لیریک: امبدد تگ + فایل ‎.lrc‎ + جستجوی آنلاین (LRCLIB) + ست دستی | ✅ |
| اکولایزر 5 باند + BassBoost + تقویت صدا (LoudnessEnhancer) | ✅ |
| حذف نویز (Hiss/Hum مبتنی بر EQ) | ✅ |
| ۱۷ زبان + انتخاب زبان داخل اپ | ✅ |
| UI مینیمال سیاه‌سفید، حلقه پیشرفت لمسی، کاور واقعی آهنگ | ✅ |
| صفحه درباره ما (گیت‌هاب/سایت/تلگرام/حمایت TON) | ✅ |
| نصب روی اندروید ۶ تا ۱۷، فایل خروجی با نام+ورژن | ✅ |
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
│   ├── data/Song.kt + SongRepository.kt + AppLocale.kt + Artwork.kt
│   ├── player/MusicPlayerManager.kt + PlaybackService.kt
│   ├── audio/EqualizerManager.kt
│   ├── lyrics/LyricsManager.kt
│   ├── ui/theme/ + ui/components/ (Glass + Sheets + LanguageDialog) + ui/screens/ (Welcome/Player/About)
├── app/src/main/res/values-*/strings.xml   (۱۷ زبان)
├── app/src/main/res/xml/locales_config.xml
├── app/src/main/res/drawable/ic_alvand.xml (لوگو/آیکون)
├── .github/workflows/android.yml
```

## 🎨 لوگو و آیکون
- فایل اصلی: `app/src/main/res/drawable-nodpi/alvand_mark.png` (لوگوی کامل، صفحه شروع)
- آیکون لانچر: `drawable-nodpi/alvand_icon.png` (فقط علامت A، بدون متن — تا ماسک
  گرد لانچر چیزی را نبرد) + `mipmap-*/ic_launcher.png` برای اندروید ۶ و ۷.
- وکتور `drawable/ic_alvand.xml` فقط به‌عنوان نسخه تک‌رنگ (monochrome) آیکون نگه داشته شده.

## 🔏 امضای دیجیتال (نصب نسخه‌ها روی هم)
اپ با یک کی‌استور ثابت امضا می‌شود؛ پس نسخه جدید روی قبلی نصب می‌شود و هشدار
امضا رفع می‌شود. فایل `alvand-release.keystore` و `keystore-base64.txt` هرگز
کامیت نمی‌شوند (در `.gitignore` هستند) — از آن‌ها **بکاپ** بگیرید؛ اگر گم شوند
دیگر هیچ‌وقت نمی‌توانید آپدیت روی نسخه‌های قبلی بدهید!

۴ سکرت زیر را در ریپو بسازید (Settings → Secrets and variables → Actions → New repository secret):
- `ALVAND_KEYSTORE_BASE64` ← کل محتوای فایل `keystore-base64.txt`
- `ALVAND_KEYSTORE_PASSWORD` ← پسورد کی‌استور (موقع ساخت به شما داده شد)
- `ALVAND_KEY_ALIAS` ← معمولاً `alvand` (لاگ قدم «Show keystore aliases» در Actions آن را تأیید می‌کند)
- `ALVAND_KEY_PASSWORD` ← معمولاً همان پسورد کی‌استور

بدون این سکرت‌ها هم بیلد می‌گیرد ولی با کلید debug امضا می‌شود.

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

## 🏷 انتشار نسخه (Release)
نام فایل‌ها خودکار با ورژن ساخته می‌شود:
`AlvandPlayer-v1.0.0-debug.apk` و `AlvandPlayer-v1.0.0-release.aab`.
برای اینکه فایل‌ها در صفحه **Releases** ریپو بیایند، تگ بزنید و پوش کنید:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 📱 سازگاری
- **نصب:** اندروید ۶ تا ۱۷ (`minSdk 23` کف Jetpack است؛ روی ۱۷ بدون تارگت مستقیم هم اجرا می‌شود)
- **درباره ما:** دکمه ☰ بالای خانه → گیت‌هاب، وب‌سایت، کانال تلگرام + حمایت مالی با تون‌کوین (TON)

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
