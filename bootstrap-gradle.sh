#!/usr/bin/env bash
# Generate the official Gradle wrapper on a machine with Internet access.
set -euo pipefail
cd "$(dirname "$0")"
version=8.9
if [[ -f gradle/wrapper/gradle-wrapper.jar && -f gradle/wrapper/gradle-wrapper.properties && -f ./gradlew.bat && -x ./gradlew ]]; then
  echo "Gradle wrapper already present."
  exit 0
fi
cache="${GRADLE_USER_HOME:-$HOME/.gradle}/reflection-bootstrap"
mkdir -p "$cache"
if [[ ! -x "$cache/gradle-$version/bin/gradle" ]]; then
  curl --fail --location --retry 2 "https://services.gradle.org/distributions/gradle-$version-bin.zip" -o "$cache/gradle-$version-bin.zip"
  unzip -q -o "$cache/gradle-$version-bin.zip" -d "$cache"
fi
# Generate the wrapper in an isolated empty Gradle project; avoids requiring an Android SDK for this step.
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
printf "rootProject.name = 'wrapper-bootstrap'\n" > "$tmp/settings.gradle"
"$cache/gradle-$version/bin/gradle" -p "$tmp" wrapper --gradle-version "$version" --distribution-type bin
mkdir -p gradle/wrapper
cp "$tmp/gradlew" "$tmp/gradlew.bat" .
cp "$tmp/gradle/wrapper/gradle-wrapper.jar" "$tmp/gradle/wrapper/gradle-wrapper.properties" gradle/wrapper/
chmod +x gradlew
echo "Wrapper ready. Run ./build.sh to build the APK, or open the project in Android Studio."
