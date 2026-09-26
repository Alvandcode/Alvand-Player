# Play Release Checklist — Alvand Player v1.6.8

## ۱) بیلد نهایی
- [x] `./gradlew :app:testDebugUnitTest` — ۲۷ تست، بدون خطا
- [x] `./gradlew :app:lintDebug` — بدون خطا
- [x] `./gradlew :app:assembleRelease :app:bundleRelease` با کی‌استور پایدار
- [x] `apksigner verify --print-certs` روی APK Release
- [x] `versionCode=15 / versionName=1.6.8` با تگ `v1.6.8` و سکشن `## [1.6.8]` در CHANGELOG یکی است
- [ ] Secretهای `ALVAND_*` و متغیر `ALVAND_RELEASE_CERT_SHA256` در سطح Repository تنظیم شده‌اند
- [x] Release ‏`v1.6.8` از Build محلی امضاشده ساخته شد (APK/AAB/mapping/SHA256SUMS)

## ۲) فروشگاه (fastlane/metadata/android/{en-US,fa}/)
- [x] title/short/full + changelogs/5,6,7.txt (en + fa)
- [ ] اسکرین‌شات‌ها: `bash tools/capture-screenshots.sh` — حداقل: Player + Lyrics + EQ + Widget
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
- [x] `git tag v1.6.8 && git push origin v1.6.8` و Release با Artifactهای امضاشده
- [ ] پس از تنظیم Secretها: Actions → Android CI → Run workflow → tag=`v1.6.8`
- [ ] تست نصب روی اندروید ۶ (minSdk 23) و ۱۶، + Android Auto با `Desktop Head Unit`
- [ ] بستن ایشوهای مرتبط + آپدیت Roadmap به v1.7.0 (پلی‌لیست در Auto، اکولایزر per-song)
