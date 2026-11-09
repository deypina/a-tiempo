#!/usr/bin/env bash
# Fails if the given APK requests any network permission.
# A Tiempo must work fully offline (CLAUDE.md, firm rules).
set -euo pipefail

apk="${1:?Usage: check-no-internet.sh <path-to-apk>}"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
aapt2="$(ls -d "$sdk"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1 || true)"

if [ -z "$aapt2" ]; then
  echo "aapt2 not found under \$ANDROID_HOME/build-tools" >&2
  exit 2
fi

permissions="$("$aapt2" dump permissions "$apk")"
echo "$permissions"

if echo "$permissions" | grep -E -q "android\.permission\.(INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE)"; then
  echo "ERROR: the APK requests a network permission. A Tiempo must not use the internet." >&2
  exit 1
fi
echo "OK: no network permissions."
