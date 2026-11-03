#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK installation}"
: "${NDK_HOME:?Set NDK_HOME to Android NDK 30.0.16248370}"
mode="${1:-debug}"
case "$mode" in debug|release) ;; *) echo 'Usage: bash scripts/build-ispeed.sh [debug|release]' >&2; exit 2;; esac
# The source ZIP already contains the pinned submodule trees. Git checkouts initialize them.
if [ -d .git ]; then git submodule update --init --recursive; fi
if [ -d AndroidLibXrayLite/.git ]; then
  test "$(git -C AndroidLibXrayLite rev-parse HEAD)" = 76a6e2172210613fcb4aa32b6de55bcd9452ce43
fi
if [ -d hev-socks5-tunnel/.git ]; then
  test "$(git -C hev-socks5-tunnel rev-parse HEAD)" = c5584feca8f6df70ee53eb2879e034b849a3d15a
fi
bash compile-hevtun.sh
mkdir -p V2rayNG/app/libs
cp -R libs/. V2rayNG/app/libs/
# This release is the tag at the exact core gitlink in v2rayNG 2.3.14, not latest.
curl --fail --location --proto '=https' --tlsv1.2 \
  https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.10.21/libv2ray.aar \
  --output V2rayNG/app/libs/libv2ray.aar
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > V2rayNG/local.properties
cd V2rayNG
# ABI_FILTERS is deliberately absent: produces all four splits and the universal APK.
if [ "$mode" = debug ]; then
  bash gradlew :app:testFdroidDebugUnitTest :app:compileFdroidDebugKotlin :app:assembleFdroidDebug
else
  bash gradlew licenseFdroidReleaseReport :app:assembleFdroidRelease
fi
