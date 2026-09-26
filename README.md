# Alvand Player

[![Stars](https://img.shields.io/github/stars/Alvandcode/Alvand-Player?style=flat-square)](https://github.com/Alvandcode/Alvand-Player/stargazers) [![License](https://img.shields.io/github/license/Alvandcode/Alvand-Player?style=flat-square)](./LICENSE) [![Last commit](https://img.shields.io/github/last-commit/Alvandcode/Alvand-Player?style=flat-square)](https://github.com/Alvandcode/Alvand-Player/commits)

> Minimal black-and-white Android music player (Kotlin + Media3) — background playback, lyrics, 5-band equalizer, English and Persian.

<div dir="rtl">

## موزیک‌پلیر الوند

موزیک‌پلیر مینیمال سیاه‌وسفید اندروید با کاتلین و Media3؛ پخش در پس‌زمینه، نمایش متن ترانه، اکولایزر پنج بانده و پشتیبانی از انگلیسی و فارسی.

</div>

---

# 🎵 Alvand Player — موزیک‌پلیر مینیمال سیاه‌سفید اندروید

موزیک‌پلیر اندروید با رابط مینیمال مشکی‌سفید: تک‌صفحه پخش با پنل آرت کشیده و
حلقه پیشرفت لمسی دور نیم‌دایره، لیست آهنگ‌ها با کشیدن نوار پایین به بالا،
کارت لیریک با متن متحرک برای خط‌های بلند.

| قابلیت | وضعیت |
|---|---|
| پخش تمام فرمت‌ها (mp3/aac/ogg/opus/flac/wav/m4a/amr/midi + HLS/DASH) | ✅ ExoPlayer / Media3 |
| کنترل از نوار اعلان، لاک‌اسکرین و خروجی مدیا + پخش پس‌زمینه | ✅ MediaSession |
| لینک مستقیم (Paste URL + باز کردن از اپ‌های دیگر) | ✅ |
| لیریک: امبدد تگ + فایل ‎.lrc‎ + جستجوی اختیاری آنلاین (LRCLIB) + ست دستی | ✅ |
| اکولایزر 5 باند + BassBoost + تقویت صدا (LoudnessEnhancer) | ✅ |
| حذف نویز (Hiss/Hum مبتنی بر EQ) | ✅ |
| انگلیسی + فارسی + انتخاب زبان داخل اپ | ✅ |
| UI مینیمال سیاه‌سفید، حلقه پیشرفت لمسی، کاور واقعی آهنگ | ✅ |
| 🎨 پالت رنگی داینامیک از کاور (گرادیان + کنترل‌های هم‌رنگ) | ✅ v1.1.0 |
| 🌙 تایمر خواب با محو تدریجی صدا (۵–۹۰ دقیقه + پایان آهنگ) | ✅ v1.1.0 |
| 🏠 ویجت هوم‌اسکرین (پخش/توقف/بعدی بدون باز کردن اپ) | ✅ v1.1.0 |
| 🧩 Hilt + ناوبری Compose تمیز (پایه قابلیت‌های بعدی) | ✅ v1.1.0 |
| 🔍 اسکنر کتابخانه: لود خودکار + دکمه اسکن + دیده‌بان آهنگ‌های جدید | ✅ |
| 🌗 تم روشن/تیره/سیستم + 🫧 لیکویید گلس + 🖼 بکگراند دلخواه زیر کادر | ✅ |
| صفحه درباره ما (گیت‌هاب/سایت/تلگرام/حمایت TON) | ✅ |
| نصب روی اندروید ۶ تا ۱۷، فایل خروجی با نام+ورژن | ✅ |
| بیلد خودکار در گیت‌هاب (APK/AAB) | ✅ |

## 🌍 زبان‌ها
- English 🇬🇧
- فارسی 🇮🇷

- زبان پیش‌فرض = زبان گوشی (خودکار).
- تغییر دستی: دکمه 🌐 بالای صفحه خانه → انتخاب زبان (بدون خروج از اپ اعمال می‌شود).
- رشته‌ها در `app/src/main/res/values-*/strings.xml` و انتخابگر در
`data/AppLocale.kt` + `ui/components/LanguageDialog.kt` است.

## 📦 ساختار
```
alvand-player/
├── app/src/main/java/com/alvand/player/
│   ├── MainActivity.kt              (@AndroidEntryPoint)
│   ├── AlvandApp.kt                 (Hilt @HiltAndroidApp)
│   ├── AppViewModel.kt              (@HiltViewModel)
│   ├── data/Song.kt + SongRepository.kt + AppLocale.kt + Artwork.kt
│   ├── player/MusicPlayerManager.kt + PlaybackService.kt + SleepTimer.kt
│   ├── player/widget/PlayerWidgetProvider.kt
│   ├── audio/EqualizerManager.kt
│   ├── lyrics/LyricsManager.kt
│   ├── ui/theme/ (Theme + Color + DynamicTheme) + ui/components/ (Glass + Sheets + SleepTimerDialog + LanguageDialog) + ui/screens/ (Welcome/Player/About) + ui/navigation/ (Routes + AppNavHost)
│   ├── res/layout/widget_player.xml + res/xml/player_widget_info.xml
├── fastlane/metadata/android/{en-US,fa}/  (title/short/full/changelogs)
├── docs/screenshots-README.md + tools/capture-screenshots.sh
├── CHANGELOG.md
├── app/src/main/res/values/strings.xml + values-fa/strings.xml   (en + fa)
├── app/src/main/res/xml/locales_config.xml
├── app/src/main/res/drawable/ic_alvand.xml (لوگو/آیکون)
├── .github/workflows/android.yml
```

## 📸 اسکرین‌شات و انتشار در فروشگاه

- شات‌لیست، تنظیمات استاندارد و اسکریپت ضبط: [`docs/screenshots-README.md`](./docs/screenshots-README.md)
  + `bash tools/capture-screenshots.sh`
- متن‌های آماده گوگل‌پلی (انگلیسی + فارسی): `fastlane/metadata/android/{en-US,fa}/`
  (`title.txt`، `short_description.txt`، `full_description.txt`، `changelogs/3.txt`).
- یادداشت نسخه: [`CHANGELOG.md`](CHANGELOG.md) — با پوش تگ (`git tag v1.1.0 && git push origin v1.1.0`)
  بخش همان نسخه به‌صورت خودکار به بدنه GitHub Release می‌رود.

## 🎨 لوگو و آیکون
- فایل اصلی: `app/src/main/res/drawable-nodpi/alvand_mark.png` (لوگوی کامل، صفحه شروع)
- آیکون لانچر: `drawable-nodpi/alvand_icon.png` (فقط علامت A، بدون متن — تا ماسک
  گرد لانچر چیزی را نبرد) + `mipmap-*/ic_launcher.png` برای اندروید ۶ و ۷.
- وکتور `drawable/ic_alvand.xml` فقط به‌عنوان نسخه تک‌رنگ (monochrome) آیکون نگه داشته شده.

## 🔏 امضای دیجیتال (نصب نسخه‌ها روی هم)
اپ با یک کی‌استور ثابت امضا می‌شود؛ نسخه جدید روی قبلی نصب می‌شود. کلید Release
داخل ریپو نیست و در `%USERPROFILE%\.keystores\alvand-release.p12` نگهداری می‌شود.
از خود کلید و فایل Credentials **بکاپ آفلاین** بگیرید؛ اگر گم شوند، دیگر نمی‌توانید
روی نسخه‌های قبلی آپدیت بدهید.

در GitHub Environment با نام `release` چهار Secret و یک Variable بسازید
(Settings → Environments → release → Environment secrets):
- `ALVAND_KEYSTORE_BASE64` ← محتوای `alvand-release.p12.base64.txt`
- `ALVAND_KEYSTORE_PASSWORD` ← `Store password` در فایل Credentials
- `ALVAND_KEY_ALIAS` ← `alvand`
- `ALVAND_KEY_PASSWORD` ← `Key password` در فایل Credentials
- `ALVAND_RELEASE_CERT_SHA256` ← اثر انگشت SHA-256 گواهی Release (متغیر، نه Secret)

بدون کی‌استور یا متغیر گواهی، Build مربوط به Release عمداً متوقف می‌شود و هیچ
APK/AAB با کلید موقت یا گواهی ناشناخته منتشر نمی‌شود. Debug یک بستهٔ جدا با
application id ‎`com.alvand.player.debug` است.

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
- `alvand-player-debug-apk` → فقط برای توسعه و آزمون
- `alvand-player-release-bundle` → APK، AAB، mapping و checksumهای نسخهٔ Release

## 🏷 انتشار نسخه (Release)
نام فایل‌ها خودکار با ورژن ساخته می‌شود:
`AlvandPlayer-v1.6.8-release.apk` و `AlvandPlayer-v1.6.8-release.aab`.
پس از ثبت Secretها، از تب **Actions** → ورک‌فلو `Android CI` → **Run workflow**
تگ موجود (مثل `v1.6.8`) را وارد کنید. Job امضاشده Quality را دوباره اجرا می‌کند،
گواهی را بررسی می‌کند و Release را به‌روزرسانی می‌کند. Environment ‏`release` فقط
روی `main` و تگ‌های `v*` قابل استقرار است و Push تگ به‌تنهایی فقط Quality را
اجرا می‌کند؛ هیچ Secretی در PR در دسترس نیست.

## 📱 سازگاری
- **نصب:** اندروید ۶ تا ۱۷ (`minSdk 23` کف Jetpack است؛ روی ۱۷ بدون تارگت مستقیم هم اجرا می‌شود)
- **درباره ما:** دکمه ☰ بالای خانه → گیت‌هاب، وب‌سایت، کانال تلگرام + حمایت مالی با تون‌کوین (TON)

## 📱 نصب کاربر نهایی / Install (end users)

- FA: از بخش **Releases** فایل `AlvandPlayer-v*-release.apk` را دریافت کنید. اگر
  صفحهٔ Releases خالی است، هنوز تنظیمات انتشار کامل نشده است. خروجی Debug فقط برای
  توسعه‌دهندگان است و نباید به‌عنوان نسخهٔ نهایی نصب شود.
  نکته: Artifacts گیت‌هاب فایل ZIP است؛ پس از دانلود آن را باز و فایل `.apk` امضاشده را نصب کنید.
  روی گوشی: `Settings → Security → Install unknown apps / Unknown Sources`
  را برای مرورگر/فایل‌منجر فعال کنید. نیازمند **اندروید ۶ به بالا**
  (تست‌شده تا اندروید ۱۶؛ `minSdk 23` / `targetSdk 36`) است.
- EN: Download `AlvandPlayer-v*-release.apk` from **Releases**. If Releases is empty,
  publication is not fully configured yet. Debug artifacts are for development only.
  Unzip the artifact, enable installation from the trusted file manager, and install the signed APK on Android 6+.

## ✅ پیش‌نیازها / Prerequisites

مقادیر دقیق از `app/build.gradle.kts` و `.github/workflows/android.yml` خوانده شده
(اگر متن قدیمی‌تری دیدید که `compileSdk 35` می‌گوید، ملاک همین‌جاست):

- EN: **JDK 17** (CI: Temurin 17 via `setup-java`; `compileOptions` +
  Kotlin `jvmTarget = "17"`), **Android Studio** سازگار با AGP `8.7.3`
  (Ladybug به بعد) + **Gradle `8.9`** (طبق `gradle-wrapper.properties`).
- FA: **SDK**: ‏`compileSdk = 36`، ‏`targetSdk = 36`، ‏`minSdk = 23`
  (اندروید ۶ تا ۱۶؛ روی ۱۷ هم اجرا می‌شود) + `build-tools;36.0.0` و
  `platforms;android-36` (قدم `Install Android 36 packages` در ورک‌فلو).
- FA/EN: SDK را از Android Studio (`SDK Manager`) یا با `sdkmanager`
  نصب کنید؛ سپس بیلد لوکال (پایین) را اجرا کنید.

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
3. **آنلاین:** دریافت یک‌باره با دکمه «دریافت متن» یا دریافت خودکارِ صریح از تنظیم شیت لیریک → LRCLIB.
4. **دستی:** Paste/Set → ذخیره به‌صورت `.lrc`.

## 🎚 اکولایزر / تقویت / نویز
- اکولایزر ۵ باند + پریست (Normal/Pop/Rock/Jazz/Classical/Bass…)
- BassBoost و Reverb
- تقویت صدا تا ‎+10dB‎ با LoudnessEnhancer
- حذف نویز: برش Hiss فرکانس‌بالا + حذف Hum ‏50/60Hz‏ + بوست وضوح وکال

---

## Contributing / مشارکت

- EN: Issues and Pull Requests are welcome. Please see [CONTRIBUTING.md](./CONTRIBUTING.md).
- FA: برای گزارش مشکل یا پیشنهاد قابلیت جدید، لطفا ایشو یا پول‌ریکوئست ثبت کنید.

## License / لایسنس

MIT — see [LICENSE](./LICENSE).

## Contact / ارتباط

- Telegram: https://t.me/a_c_official
- Website: https://alvandcode.github.io
