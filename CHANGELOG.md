# Changelog — Alvand Player

همه تغییرات مهم این فایل ثبت می‌شود. قالب بر اساس [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.1.0] — 2026-09-11

### Added — جدید
- 🎨 **Dynamic color palette**: background gradient, progress arc, controls and
  artwork glow now follow the current song cover (Palette API + smooth
  animated transitions, monochrome fallback when there is no artwork).
  🎨 **پالت رنگی داینامیک**: گرادیان پس‌زمینه، نوار پیشرفت، دکمه‌ها و هاله آرت
  هم‌رنگ کاور آهنگ می‌شوند (با انیمیشن نرم و فالبک سیاه‌سفید).
- 🌙 **Sleep timer + fade-out**: presets 5/10/15/30/45/60/90 min + end of
  current song; volume fades out linearly then playback stops; live countdown
  chip on the player + menu entry.
  🌙 **تایمر خواب با محو صدا**: ۵ تا ۹۰ دقیقه + پایان آهنگ فعلی؛ کاهش تدریجی
  صدا و توقف پخش؛ نمایش شمارش معکوس روی صفحه پخش و در منو.
- 🏠 **Home-screen widget**: title/artist + previous/play-pause/next without
  opening the app; tap opens the player; zero-battery polling (push updates
  from the playback service).
  🏠 **ویجت هوم‌اسکرین**: عنوان/خواننده + قبلی/پخش/بعدی بدون باز کردن اپ.
- 🧩 **Hilt + Compose Navigation foundation**: `@HiltAndroidApp`,
  constructor-injected player/repository/EQ, `@HiltViewModel`, typed `Routes`
  (`welcome/player/about`) + `AppNavHost` — ready for bigger features.
  🧩 **زیرساخت Hilt و ناوبری**: تزریق وابستگی و گراف ناوبری تمیز؛ پایه
  قابلیت‌های بزرگ‌تر.
- 📸 **Store-ready release flow**: `CHANGELOG.md` → GitHub Release notes
  automatically, Play Store listings (en + fa) under `fastlane/`, screenshot
  shot-list + capture script (`tools/capture-screenshots.sh`).
  📸 **انتشار حرفه‌ای**: یادداشت نسخه خودکار، متن‌های فروشگاه (انگلیسی و
  فارسی) و راهنمای اسکرین‌شات.

## [1.0.1] — tidligere
- Minimal black-white player, ExoPlayer/Media3, lyrics (embedded/.lrc/LRCLIB/manual),
  5-band EQ + BassBoost + LoudnessEnhancer + denoise, 17 languages, about page,
  stable release signing, auto CI (APK/AAB).

[1.1.0]: https://github.com/Alvandcode/Alvand-Player/releases/tag/v1.1.0
