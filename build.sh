#!/usr/bin/env bash
# Build a debug APK without opening Android Studio.
set -euo pipefail
cd "$(dirname "$0")"

if [[ -z "${ANDROID_HOME:-}" ]]; then
    if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
        export ANDROID_HOME="$ANDROID_SDK_ROOT"
    elif [[ ! -f local.properties && -d "$HOME/Android/Sdk" ]]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    fi
fi
if [[ -z "${ANDROID_HOME:-}" && ! -f local.properties ]]; then
    echo 'Android SDK not configured. Set ANDROID_HOME or create local.properties (see README.md).' >&2
    exit 1
fi

./bootstrap-gradle.sh
./gradlew assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/reflection.apk
printf '\nAPK ready: %s\n' 'dist/reflection.apk'
