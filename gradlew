#!/usr/bin/env sh
# Minimal gradle wrapper bootstrap — if gradle-wrapper.jar is missing, uses system gradle.
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
else
  echo "gradle-wrapper.jar not found, falling back to system gradle. Run 'gradle wrapper' once to generate it."
  exec gradle "$@"
fi
