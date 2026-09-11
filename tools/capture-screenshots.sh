#!/usr/bin/env bash
# Capture Play-Store screenshots from a connected device/emulator.
# Usage:  bash tools/capture-screenshots.sh [out_dir]
# Needs:  adb + a device with Alvand Player installed and music on it.
set -euo pipefail
OUT="${1:-fastlane/metadata/android/en-US/images/phoneScreenshots}"
mkdir -p "$OUT"
echo "→ screenshots will be saved to $OUT"
echo "  Arrange each screen on the device, then press ENTER to capture."

shots=(
  "01-welcome.png:صفحه خوشامد (Welcome)"
  "02-player-dynamic.png:پخش با رنگ داینامیک کاور (Player)"
  "03-playlist.png:لیست آهنگ‌ها (bottom sheet باز)"
  "04-sleep-timer.png:دیالوگ تایمر خواب"
  "05-lyrics.png:شیت لیریک همگام"
  "06-eq.png:شیت اکولایزر"
  "07-widget.png:ویجت روی هوم‌اسکرین"
)
i=1
for entry in "${shots[@]}"; do
  file="${entry%%:*}"; desc="${entry#*:}"
  read -r -p "[$i/${#shots[@]}] $desc آماده است؟ ENTER…" _
  adb exec-out screencap -p > "$OUT/$file"
  echo "  saved $OUT/$file"
  i=$((i+1))
done
echo "✓ done. Copy to fa/ too or re-capture with fa locale:"
echo "  adb shell \"cmd locale set-app-locales com.alvand.player --locales fa\""
