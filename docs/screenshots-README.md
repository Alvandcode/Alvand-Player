# 📸 اسکرین‌شات‌های فروشگاه — راهنما

تصاویر نهایی در این پوشه‌ها می‌نشینند (قرارداد fastlane):

```
fastlane/metadata/android/en-US/images/phoneScreenshots/  (حداقل ۲، ایده‌آل ۷)
fastlane/metadata/android/fa/images/phoneScreenshots/
fastlane/metadata/android/en-US/images/featureGraphic/   (بنر ۱۰۲۴×۵۰۰)
```

## شات‌لیست پیشنهادی (۷ قاب)

| # | فایل | صحنه | چرا؟ |
|---|------|------|------|
| 1 | `01-welcome.png` | صفحه خوشامد با لوگو | اولین برداشت |
| 2 | `02-player-dynamic.png` | پخش + **رنگ داینامیک کاور** (آهنگی با کاور رنگی) | ستاره نسخه ۱٫۱٫۰ |
| 3 | `03-playlist.png` | شیت لیست باز + MiniBars | عمق محصول |
| 4 | `04-sleep-timer.png` | دیالوگ تایمر خواب | قابلیت پرتقاضا |
| 5 | `05-lyrics.png` | لیریک همگام هایلایت‌شده | تمایز |
| 6 | `06-eq.png` | اکولایزر ۵ بانده | قدرت صوتی |
| 7 | `07-widget.png` | ویجت روی هوم‌اسکرین کنار آیکون اپ | visibility |

## تنظیمات استاندارد

- دستگاه: Pixel 8 پروفایل، رزولوشن 1080×2400، تم روشن، فونت پیش‌فرض.
- هر دو لوکیل `en` و `fa` (برای `fa/` دوباره بگیرید).
- آهنگ نمایشی با کاور رنگی (قرمز/بنفش زنده) تا پالت داینامیک بدرخشد.
- نوار وضعیت تمیز: ساعت 9:41، باتری پر، با
  `adb shell settings put global sysui_demo_allowed 1` + demo mode.

## ضبط سریع

```bash
bash tools/capture-screenshots.sh
```

## چک‌لیست انتشار

- [ ] ۷ شات en + ۷ شات fa
- [ ] بنر featureGraphic (لوگو + «Your world. Your music.»)
- [ ] آیکون ۵۱۲×۵۱۲ از `alvand_icon.png`
- [ ] متن‌ها از `fastlane/metadata/...` کپی شوند
- [ ] یادداشت نسخه از `CHANGELOG.md` (خودکار در ریلیز گیت‌هاب می‌آید)
