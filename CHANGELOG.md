# Changelog — Alvand Player

همه تغییرات مهم این فایل ثبت می‌شود. قالب بر اساس [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.7.1] — 2026-09-30

### Added — پلی‌لیست
- 🎵 **ساخت پلی‌لیست**: دکمهٔ «پلی‌لیست جدید» در تب پلی‌لیست‌ها، با نام حداکثر ۶۰ کاراکتر و نام خالیِ ردشده.
- ➕ **افزودن آهنگ به پلی‌لیست** با یک دکمه در کنار هر آهنگ، بدون رفتن به منو. آهنگ تکراری بی‌صدا رد نمی‌شود و پیام می‌دهد.
- 📂 **پخش کل پلی‌لیست** با یک ضربه روی دکمهٔ پخش کنار هر پلی‌لیست.
- 🗂 **سه تب در شیت کتابخانه**: آهنگ‌ها / پلی‌لیست‌ها / تاریخچه — با زیرخط رنگی و حفظ انتخاب هنگام چرخش صفحه.
- 🕘 **تب تاریخچه**: پخش دوبارهٔ آهنگ‌های اخیر با شمارندهٔ تعداد دفعات، و پاک کردن تاریخچه.
- حذف و باز کردن پلی‌لیست برای دیدن و برداشتن آهنگ‌ها.

### Fixed
- پخش از پلی‌لیست و تاریخچه هم اجازهٔ اعلان را می‌گیرد، مثل بقیهٔ راه‌های پخش (روی اندروید ۱۳ به بعد بدون آن پخش درست جواب نمی‌داد).
- پلی‌لیست و تاریخچهٔ بلند دیگر از شیت بیرون نمی‌زنند؛ ارتفاعشان محدود و اسکرول‌پذیر شد.
- پلی‌لیست خالی و فایلِ پاک‌شده پیام روشن می‌دهند به‌جای بی‌صدا رد شدن.
- برچسب تب سوم از «اخیراً» به «تاریخچه» تغییر کرد تا گویاتر باشد.

## [1.7.0] — 2026-09-30

### Added — ویجت هوم‌اسکرین بازطراحی‌شده
- 🧱 **ویجت کارتی**: کاور مربعی در یک سر کارت، و در سر دیگر عنوان، خواننده، نوار پیشرفت و پنج کنترل.
  🧱 **Card widget**: square cover on one side; title, artist, seek line and five controls on the other.
- ⏱️ **نوار پیشرفت زنده**: زمان سپری‌شده و نوار پرشدن هر ثانیه به‌روز می‌شود (فقط وقتی ویجت نصب است).
- 🔁 **دکمهٔ تکرار** و **دکمهٔ علاقه‌مندی** به ویجت اضافه شد — هر دو بدون باز کردن اپ کار می‌کنند.
- 🌗 **تم روشن و تیرهٔ ویجت** که با تنظیمات سیستم هماهنگ می‌شود.
- ↔️ **چیدمان راست‌به‌چپ** برای فارسی: ترتیب کنترل‌ها، جهت نوار پیشرفت و گوشهٔ گرد کاور معکوس می‌شوند.

### Fixed — پایداری ویجت
- **باگ مهم**: هر بار که کاربر دکمه‌ای از ویجت را می‌زد، سرویس پخش بلافاصله کشته می‌شد و آهنگ قطع می‌شد. حالا سرویس فقط وقتی صف خالی است بسته می‌شود.
- کاور ویجت از روی فایل استخراج و متناسب با ارتفاع واقعی ویجت برش می‌خورد؛ گوشه‌های گرد داخل خود تصویر ساخته می‌شوند تا روی اندروید ۶ تا ۱۱ هم درست باشند.
- کش کاور ویجت بایت‌محور شد؛ قبلاً ظرفیتش ۲ بایت بود و عملاً هر ثانیه کاور از دیسک دوباره خوانده می‌شد.
- درخواست‌های همزمان به‌روزرسانی ویجت هم‌ادغام می‌شوند تا تایمر یک‌ثانیه‌ای صف انباشته نسازد.
- اگر علاقه‌مندی را از داخل اپ تغییر دهید، ویجت هم بلافاصله تازه می‌شود؛ و تغییر از خود ویجت بعد از نوشتن روی دیسک دیگر برنمی‌گردد.
- پس از ریستارت لانچر، زمان ویجت از لحظهٔ ذخیره جلو می‌رود و عقب نمی‌ماند.
- رشته‌های ویجت با زبان انتخابی خود اپ خوانده می‌شوند، نه زبان دستگاه.
- ویجت دیگر هیچ ارجاعی به تم لانچر ندارد و روی هر لانچری یکسان inflate می‌شود.

## [1.6.9] — 2026-09-28

### Added — تم‌های رنگی
- 🎨 **سه تم رنگی جدید**: سلطنتی (بنفش)، غروب (نارنجی) و اقیانوس (فیروزه‌ای) به‌علاوهٔ حالت مینیمال قبلی.
  هر تم رنگ accent خود را در حالت روشن و تیره دارد و کل پالت شیشه‌ای اپ (کارت، شیت، گلس، برق لبه) را به همان رنگ می‌آمیزد.
  🎨 **Three new accent themes**: Royal (purple), Sunset (orange), Ocean (teal) plus the existing Minimal.
  The whole glass palette is tinted, not just buttons.
- 🎛️ **انتخابگر تم رنگی** در دیالوگ تم: شبکهٔ ۲×۲ با پیش‌نمایش گرادیان واقعی و اعمال فوری.
- پس‌زمینهٔ گرادیانی صفحهٔ پلیر وقتی آهنگی پخش نمی‌شود از رنگ تم استفاده می‌کند؛ اولویت با رنگ کاور است.

### Fixed — کیفیت و پایداری
- تنظیمات خراب‌شده (DataStore corrupt) دیگر اپ را در بومد نمی‌اندازد؛ تنظیمات پیش‌فرض بازیابی می‌شود.
- آیکون‌های ویجت در تم روشن دیده می‌شوند، دکمه‌های قبلی/بعدی با RTL هماهنگ شدند و contentDescription اضافه شد.
- رنگ آیکون نوار وضعیت متناسب با تم روشن/تیره اصلاح شد.
- لینک‌های مستقیم فقط از HTTPS پذیرفته می‌شوند (هم در کد و هم در intent-filterها).
- تشخیص زبان فعلی، برچسب فرکانس اکولایزر و محاسبهٔ تایمر خواب اصلاح شد.
- فایل لیریک با UTF-8 سخت‌گیرانه خوانده و در صورت خطا به Windows-1256 برمی‌گردد.
- Android Auto: صفحه‌بندی browse و سقف Binder، اسکیپ خودکار فایل خراب با سقف تلاش، و کانال اعلان اختصاصی پخش.
- ویجت هنگام بسته‌شدن سرویس ریست می‌شود و `FileProvider` بلااستفاده حذف شد.
- `SECURITY.md` به کانال گزارش واقعی (GitHub Security Advisory) اشاره می‌کند.

## [1.6.8] — 2026-09-23

### Changed — هاله واقعی
- 🌫️ **حذف نقطه چرخان**: دنباله نورانی (که مثل نقطه دور دایره می‌گشت) برداشته شد؛ حالا دو لایه درخشش نرم دور دایره «نفس می‌کشند» و با توقف آهنگ خاموش می‌شوند.
  🌫️ **True halo**: orbiting comet removed; soft glow layers breathe while playing, off on pause.

### Fixed — کیفیت و انتشار
- تست‌های LRC و تنظیمات Android Backup اصلاح شدند.
- دریافت خودکار لیریک آنلاین اکنون پیش‌فرض خاموش و از شیت لیریک قابل‌کنترل است.
- درخواست دسترسی صوتی از CTA انجام می‌شود و برای رد دسترسی مسیر بازیابی و Settings اضافه شد.
- Lint و Unit Test در CI دیگر report-only نیستند.
- Build نسخهٔ Release بدون کی‌استور و گواهی معتبر متوقف می‌شود.
- PR و Release به Jobهای مستقل با دسترسی‌های جدا تقسیم شدند.
- Gradle Wrapper رسمی همراه checksum توزیع Gradle 8.9 به مخزن اضافه شد.
- تشخیص پسوند لینک مستقیم اصلاح شد؛ دامنه‌های نقطه‌دار دیگر باعث رد شدن فایل صوتی نمی‌شوند.
- لیست زبان‌ها، `locales_config` و منابع به زبان‌های واقعاً ترجمه‌شده (انگلیسی/فارسی) محدود شد.
- حذف خودکار اطلاعات Room غیرفعال و Schema پایگاه‌داده برای بازبینی تغییرات آینده ثبت شد.
- کانفیگ امضای Release به خروجی APK متصل شد؛ خروجی با نام `unsigned` دیگر تولید نمی‌شود.

## [1.6.7] — 2026-09-23

### Added — هاله چرخان + نصب آپدیت
- 🌀 **هاله نور چرخان دور دایره**: نوار نورانی زیر کاور حذف شد؛ به‌جایش حلقه نور دور دایره موقع پخش می‌چرخد و با توقف می‌ایستد (حلقه کم‌رنگ ثابت همیشه هست).
  🌀 **Rotating halo**: light bar removed; glowing ring orbits the cover while playing, freezes on pause.
- 📲 **نصب آپدیت روی نسخه قبلی**: دیباگ‌کی‌استور ثابت (`gradle/debug.keystore`) کامیت شد تا همه خروجی‌های دیباگ یک امضا داشته باشند و خطای «App not installed» تمام شود. فقط **یک‌بار آخر** حذف/نصب لازم است چون نسخه فعلی گوشی با کلید موقت امضا شده.
  📲 **Updates install over old**: stable debug keystore committed; one last reinstall needed.

## [1.6.6] — 2026-09-23

### Changed — کاور دایره‌ای
- ⭕ **کاور دایره‌ای وسط‌چین** به‌جای مربع گرد؛ عنوان و خواننده زیر دایره (وسط‌چین)؛ نوار منو/تایمر خواب بالای دایره.
  ⭕ **Circular cover art** centered with title/artist below; menu/sleep row moved above.

## [1.6.5] — 2026-09-23

### Fixed — کرش موقع پخش
- 🔋 **`WAKE_LOCK` جاافتاده**: سرویس پخش با `setWakeMode(WAKE_MODE_LOCAL)` ویک‌لاک می‌خواست ولی پرمیشن در مانیفست نبود؛ با زدن Play روی thread اصلی `SecurityException` می‌داد و اپ می‌پرید. پرمیشن اضافه شد (عادی، بدون نیاز به اجازه کاربر).
  🔋 **Play crash fix**: missing `WAKE_LOCK` permission crashed playback on start. Found via the in-app crash report — thanks!

## [1.6.4] — 2026-09-23

### Changed — بازگشت ظاهر قبلی
- 🎨 **UI دقیقاً مثل v1.3.0**: تب‌ها، جستجو، چیپ‌ها و دکمه‌های ♡ از روی صفحه برداشته شدند؛ لیست ساده تکی برگشت. همه امکانات زیرین (علاقه‌مندی پایدار، کش لیریک، پلی‌لیست، تاریخچه، گزارش کرش) سر جایشان‌اند و چیزی پاک نشده.
  🎨 **UI restored**: playlist sheet back to the single plain list; all backend features kept.

## [1.6.3] — 2026-09-23

### Added — شکار کرش داخل اپ
- 🐞 **گزارش کرش درون‌برنامه‌ای**: هر کرش با مدل گوشی و نسخه اندروید در فایل ذخیره می‌شود؛ بعد از باز شدن دوباره، دیالوگ نمایش/کپی/اشتراک می‌آید + بخش «گزارش خطا» در صفحه درباره ما. هیچ‌چیز خودکار ارسال نمی‌شود.
  🐞 **In-app crash reports**: uncaught crashes saved with device info; resend dialog on next start + Bug report card in About. Nothing sent automatically.
- 🛡 **تاریخچه از مسیر پخش جدا شد**: ثبت Recently Played دیگر در نخ بحرانی پخش نیست (IO + fire-and-forget) تا خطای دیتابیس نتواند پخش را خراب کند.

## [1.6.2] — 2026-09-22

### Fixed — خطاهای کامپایل (اولین بیلد واقعی از ۱۲ سپتامبر)
- 🩹 **`MediaLibrarySession` تودرتو**: در Media3 1.5.1 این کلاس مستقل نیست بلکه `MediaLibraryService.MediaLibrarySession` است (همراه `Callback` و `Builder`).
  🩹 **Nested session API**: use `MediaLibraryService.MediaLibrarySession[.Callback/.Builder]` for Media3 1.5.1.
- 🩹 **`onAddMediaItems` types**: امضای override با `MutableList` نمی‌خواند (`ListenableFuture` invariant است) — به `List<MediaItem>` برگشت.
- 🩹 **`Modifier.clip` import**: در `PlayerScreen` جا افتاده بود (از v1.3.0 که هیچ‌وقت کامپایل نشده بود).
- 🩹 **`controller.audioSessionId`**: این خاصیت روی اینترفیس `Player` در 1.5.1 نیست (فقط `ExoPlayer`) — اتصال EQ حالا فقط از `PlaybackService.audioSessionId` با تلاش مجدد خودکار.
- 📢 **گزارشگر خودکار**: اگر بیلد بشکند، خطاها در ایشوی 🔴 منتشر می‌شوند.

## [1.6.1] — 2026-09-22

### Fixed — سبز شدن CI
- 🩹 **Setup Android SDK v3 → v4**: اکشن v3 از ۱۲ سپتامبر روی همه ران‌ها (حتی main) می‌شکست؛ v4 مشکل را حل کرد (اثبات: ران PR #48).
  🩹 **CI infra**: `android-actions/setup-android@v4`; tests back to report-only until the 4s test-setup infra issue is fixed.
- 🔇 **تست‌ها report-only**: تست واحد دوباره بلاک نمی‌کند (زیرساخت تست هنوز همان خطای ۴ ثانیه‌ای را دارد)؛ گزارش‌ها در آرتیفکت می‌مانند.
- 🤖 **Dependabot بدون majorهای breaking**: پین media3 روی 1.5.1، Room روی 2.6.1، بدون Gradle 9 / AGP 9 خودکار.

## [1.6.0] — 2026-09-22

### Added — خودرو و کیفیت انتشار
- 🚗 **Android Auto**: `PlaybackService` از `MediaSessionService` به `MediaLibraryService` رفت؛ root = صف فعلی برای browse، `automotive_app_desc.xml` + اکسپورت سرویس.
  🚗 **Android Auto**: MediaLibraryService with browsable queue root.
- ✅ **گیت نسخه/تگ**: چک `tag == appVersionName` + وجود سکشن CHANGELOG برای تگ.
  ✅ **Release gate**: tag/version/changelog consistency check.
- 🤖 **Dependabot + CODEOWNERS**: آپدیت هفتگی Gradle/Actions، اونر ریویو خودکار.
  🤖 **Repo health**: Dependabot + CODEOWNERS.

## [1.5.0] — 2026-09-22

### Added — پلی‌لیست و کتابخانه واقعی
- 📂 **پلی‌لیست با Room**: ساخت/حذف/تغییرنام، افزودن بدون تکرار، پخش پلی‌لیست، ماندگاری بین اجراها (`playlists` + `playlist_songs`).
  📂 **Room playlists**: create/rename/delete, dedup add, play, persisted.
- 🕘 **تاریخچه پخش**: Recently Played + شمارش تکرار (`play_history`)، پاک‌سازی، ثبت خودکار روی هر آهنگ.
  🕘 **Play history**: auto-record on track change, recent list with play counts.
- 💿 **تب Albums/Artists**: گروه‌بندی از متادیتای MediaStore (بدون DB)، پخش یک‌تپ آلبوم/خواننده.
  💿 **Albums/Artists tabs**: grouped from MediaStore metadata, one-tap play.

## [1.4.0] — 2026-09-22

### Added — کتابخانه کاربردی
- ♡ **علاقه‌مندی پایدار**: لایک‌ها در DataStore می‌مانند (قبلاً با بستن اپ می‌پرید)؛ دکمه ♡ در لیست + روی کاور آهنگ فعلی.
  ♡ **Persistent favorites**: likes survive restarts (DataStore); heart button in playlist rows + on cover.
- 🔍 **جستجو + سورت + فیلتر علاقه‌مندی‌ها**: سرچ زنده روی عنوان/خواننده/آلبوم، سورت جدیدترین/الفبا/خواننده/طولانی‌ترین، چیپ All/Favorites.
  🔍 **Search + sort + favorites filter**: live search, sort Recent/A–Z/Artist/Longest, empty states.
- 💾 **کش لیریک آنلاین**: نتیجه موفق LRCLIB روی دیسک ذخیره می‌شود تا دفعه بعد آفلاین بیاید؛ دستی کاربر همچنان اولویت دارد.
  💾 **Online lyrics cache**: successful LRCLIB fetch is cached to disk for offline reuse.
- 🧪 **تست LibraryFilter**: ۵ تست واحد برای فیلتر/سورت ترکیبی.

## [1.3.0] — 2026-09-12

### Changed — بازطراحی Mono+Aura
- 🎬 **Cinematic art panel**: U-shape (190dp) → modern 28dp card + soft
  shadow + bottom scrim so the title stays readable on any cover.
  🎬 **پنل سینمایی**: فرم U حذف شد؛ کارت مدرن با سایه نرم و اسکریم
  پایین برای خوانایی تایتل روی هر کاوری.
- 🖼️ **Blurred-cover background**: live blurred artwork behind the player
  + single soft aura (was double blob + double glow) — richer, faster.
  🖼️ **بکگراند بلر زنده**: کاور بلرشده پشت پلیر + تک‌هاله ملایم.
- 🎵 **Readable playlist**: artwork thumbnails + artist + selected state +
  empty state; labeled bottom handle (`Playlist • N`).
  🎵 **پلی‌لیست خوانا**: کاور کوچک + خواننده + حالت انتخاب + حالت خالی.
- 🌗 **Contrast & widget**: fixed low-contrast disabled icons, white icon on
  accent, dark-glass widget with white controls, flat mono launcher icon.
  🌗 **کنتراست و ویجت**: آیکون‌های خواناتر، ویجت شیشه تیره، آیکون فلت.

## [Unreleased]

### Added — جدید
- 🔍 **Library scanner**: songs load automatically once audio permission is
  granted, a «Scan device songs» menu button re-scans on demand (with a result
  toast), and a MediaStore observer silently adds new tracks while the app is
  open. Manual links/files are preserved.
  🔍 **اسکنر کتابخانه**: لود خودکار بعد از دادن دسترسی، دکمه اسکن دستی در منو
  و دیده‌بان خودکار آهنگ‌های جدید؛ لینک‌ها و فایل‌های دستی حفظ می‌شوند.
- 🫧 **Liquid glass + dark/light theme + custom background**: frosted-glass
  cards/sheets, system/light/dark theme picker (DataStore-persisted) and an
  optional user photo behind the player panel (persisted URI permission);
  the art panel keeps one shape with or without cover art.
  🫧 **لیکویید گلس + تم روشن/تیره + بکگراند دلخواه**: کارت‌ها و شیت‌های
  شیشه‌ای، انتخاب تم سیستم/روشن/تیره و عکس دلخواه زیر کادر پخش؛ فرم کادر
  با کاور یا بدون کاور یکی می‌ماند.

### Fixed — رفع‌شده
- 🔏 **Install over previous version**: debug APKs from CI are now signed with
  the same stable keystore as release (ephemeral runner debug keys used to
  change every run, forcing an uninstall before every update).
  قدم «Verify APK signatures match» در CI یکسان‌بودن گواهی‌ها را چک می‌کند.
  🔏 **نصب آپدیت روی نسخه قبلی**: خروجی‌های debug هم با همان کلید ثابت امضا
  می‌شوند؛ دیگر لازم نیست برای هر آپدیت اپ را حذف کنید (یک‌بار آخر حذف/نصب لازم است).

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

[1.3.0]: https://github.com/Alvandcode/Alvand-Player/releases/tag/v1.3.0
[1.1.0]: https://github.com/Alvandcode/Alvand-Player/releases/tag/v1.1.0
