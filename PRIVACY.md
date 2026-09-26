# Privacy Policy — Alvand Player

**Last updated: 2026-09-25**

Alvand Player is a private, offline-first music player.

## FA — فارسی

- **فایل‌های صوتی شما از گوشی خارج نمی‌شوند.** پخش، اکولایزر، پلی‌لیست، علاقه‌مندی‌ها و تاریخچه فقط روی دستگاه شماست (DataStore + Room + فایل‌های `.lrc` در حافظه داخلی).
- **جستجوی خودکار لیریک آنلاین به‌صورت پیش‌فرض خاموش است:** اگر کاربر گزینه «دریافت خودکار متن آنلاین» را روشن کند، عنوان و خوانندهٔ آهنگ‌های فاقد لیریک محلی به `lrclib.net` فرستاده می‌شود. دکمه «دریافت متن» نیز فقط برای همان درخواست کاربر فعال است. نتیجه فقط روی دستگاه کش می‌شود.
- **لینک مستقیم:** اگر خودتان URL وارد کنید، اپ همان فایل را استریم می‌کند. ما لینک‌های شما را جمع نمی‌کنیم.
- **هیچ حساب، تبلیغ، تحلیلگر یا ردیابی نداریم.** هیچ داده‌ای به سرور ما (که اصلاً وجود ندارد) فرستاده نمی‌شود.
- **دسترسی‌ها و دلیل‌شان:**
  - `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` — خواندن آهنگ‌های گوشی
  - `POST_NOTIFICATIONS` — کنترل پخش در نوار اعلان (اندروید ۱۳+)
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — پخش در پس‌زمینه
  - `INTERNET` — فقط برای لینک مستقیم شما و جستجوی لیریک LRCLIB
- **حذف داده:** با حذف اپ، همه داده‌ها (پلی‌لیست، تاریخچه، کش لیریک) پاک می‌شود. از تنظیمات اندروید هم می‌توانید Clear Data بزنید.

تماس: https://github.com/Alvandcode/Alvand-Player/issues

## EN — English

- **Your music never leaves your device.** Playback, EQ, playlists, favorites and history stay on-device (DataStore + Room + local `.lrc` files).
- **Automatic online lyrics are off by default:** when the user enables “Automatic online lyrics”, the title and artist of tracks without local lyrics are sent to `lrclib.net`. Tapping “Fetch lyrics” also sends data for that one user-requested lookup. Results are cached on-device.
- **Direct links:** if you paste a URL, only that file is streamed. We don’t collect your links.
- **No accounts, ads, analytics or tracking.** There is no server to send data to.
- **Permissions and why:**
  - `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` — read on-device songs
  - `POST_NOTIFICATIONS` — playback controls in notification shade (Android 13+)
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — background playback
  - `INTERNET` — only for your direct links + LRCLIB lyrics lookup
- **Delete data:** uninstalling removes everything. You can also Clear Data in Android settings.

Contact: https://github.com/Alvandcode/Alvand-Player/issues
