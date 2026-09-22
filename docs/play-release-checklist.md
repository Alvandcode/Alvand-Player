# Play Release Checklist — Alvand Player v1.6.0

## ۱) بیلد نهایی
- [ ] `./gradlew :app:testDebugUnitTest` سبز (blocking در CI)
- [ ] `./gradlew :app:assembleRelease :app:bundleRelease` با کی‌استور پایدار
- [ ] `apksigner verify --print-certs` روی release با SHA کی‌استور ثابت یکی است (قدم CI)
- [ ] `versionCode=7 / versionName=1.6.0` با تگ `v1.6.0` و سکشن `## [1.6.0]` در CHANGELOG یکی است

## ۲) فروشگاه (fastlane/metadata/android/{en-US,fa}/)
- [x] title/short/full + changelogs/5,6,7.txt (en + fa)
- [ ] اسکرین‌شات‌ها: `bash tools/capture-screenshots.sh` — حداقل: Player + Playlist tabs + Lyrics + EQ + Widget
- [ ] Feature Graphic ۱۰۲۴×۵۰۰ + آیکون ۵۱۲×۵۱۲ (از `alvand_icon.png`)
- [ ] Privacy Policy URL → `PRIVACY.md` (آپلود در سایت یا گیست، لینک در Play Console)
- [ ] Data Safety (مهم!):
  - Data collected: **None** (no analytics)
  - `READ_MEDIA_AUDIO` → App functionality / Music
  - `INTERNET` → artist+title → lrclib.net فقط برای lyrics، optional، قابل انصراف
  - No data shared with third parties for ads

## ۳) F-Droid (رایگان، بدون وابستگی proprietary)
- [x] همه وابستگی‌ها OSS هستند (Media3/Coil/OkHttp/Hilt/Room/DataStore — بدون GMS)
- [ ] درخواست در F-Droid: fork `fdroiddata` + `metadata/com.alvand.player.yml` با `Build: gradle ... gradleprops versionCode`
- [ ] آدرس سورس + ایشو + donate (TON) در متادیتا

## ۴) بعد از انتشار
- [ ] `git tag v1.6.0 && git push origin v1.6.0` → Release خودکار با یادداشت CHANGELOG
- [ ] تست نصب روی اندروید ۶ (minSdk 23) و ۱۶، + Android Auto با `Desktop Head Unit`
- [ ] بستن ایشوهای مرتبط + آپدیت Roadmap به v1.7.0 (پلی‌لیست در Auto، اکولایزر per-song)
