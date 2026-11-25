# Reflection

Reflection is a small native Android app for turning personal insights into recurring practices and short questionnaires. The app is content independent, works offline and keeps answers on the device. Its guiding principle is **minimum friction between remembering something and actually doing it**.

Current version: **0.1.2**. See [DESIGN.md](DESIGN.md) for product principles, architecture, decisions and the roadmap. The complete user guide and JSON contract are in [guide.txt](app/src/main/assets/guide.txt) and are also available inside the app.

## Build from the terminal

Requirements:

* JDK 17 or 21 (Java source/target 17)
* Android SDK Platform 35 and Android SDK Build Tools 34.0.0 (AGP default), with SDK licences accepted
* Bash, curl and unzip; network access for the initial Gradle and dependency downloads
* Android SDK Platform Tools (`adb`) if installing from the terminal

Android Studio is optional. An existing Android Studio SDK installation can be reused. `build.sh` uses `ANDROID_HOME`, falls back to `ANDROID_SDK_ROOT`, or detects the usual Linux SDK directory if no local SDK configuration exists. For a custom path, export `ANDROID_HOME` or copy `local.properties.example` to `local.properties` and edit `sdk.dir`. Do not commit the actual `local.properties` file.

```bash
./build.sh
```

The first invocation generates the official Gradle 8.9 wrapper if missing, then runs `assembleDebug`. The APK is copied to `dist/reflection.apk`. Subsequent builds reuse the wrapper and Gradle cache. No need to open Android Studio. To build directly once the wrapper exists:

```bash
./gradlew assembleDebug
```

The Gradle APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install or update

Connect an Android device with USB debugging enabled:

```bash
adb install -r dist/reflection.apk
```

Alternatively, transfer the APK to the device and install it manually. **Update in place; do not uninstall or clear storage.** Android requires the same application ID and signing identity for updates. Debug builds normally use the machine's existing debug keystore. Preserve that keystore securely outside this repository. If Android rejects an update due to a signature mismatch, stop rather than uninstalling to bypass it: there is no backup/restore yet. A debug APK is for development/personal use, not a signed public release. Establish a persistent release signing and data-preserving migration plan before distributing releases.

## Use the app

The app starts with no categories. Copy [the example JSON](examples/breaking-the-feedback-loop.json) to the device, select **Import category JSON**, and enable notifications and exact alarms in **Reminder settings**. The example is a generic, reusable category definition, not a record of anyone's answers. Reimport an updated definition with the same category ID to replace its questions and practices without rewriting historical submissions.

Daily, weekly and monthly reports are due at 21:00 local time. Weekly reports are due on Sunday and monthly reports on the last Sunday of the month. The latest missed period is optionally available; older missed periods do not accumulate. Notification delivery depends on Android settings. Completed reports retain their original questions and answers.

There is no Internet permission, cloud sync, account or analytics. Uninstalling or clearing app data deletes local answers. Category JSON files are not answer backups.

## Tests

```bash
./tests/run-schedule-tests.sh
python3 tests/check-example.py
```

These check the calendar logic and supplied example definition without an Android SDK. They do not replace Android compilation or device testing. See [DESIGN.md](DESIGN.md) for the remaining device checks.

## Repository hygiene

The repository includes source, documentation, generic examples, build scripts and tests. Generated output, SDK paths, IDE settings and signing credentials are excluded by `.gitignore`. Keep private category definitions, personal answers, backups and exports outside the repository, or in the ignored `private/`, `backups/` and `exports/` directories. Do not put private JSON into `examples/`. Ignore rules are not a substitute for reviewing staged files before pushing.

After the first successful bootstrap, commit all four generated wrapper files: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and `gradle/wrapper/gradle-wrapper.properties`. They are portable project files, not machine configuration. The initial source archive does not bundle them because the official distribution was not accessible when it was packaged.

For a new repository:

```bash
./build.sh
git init
git add .
git status --short
# Review staged files before committing or pushing.
git commit -m "Initial Reflection Android project"
```

No public licence is selected in this package; choose one explicitly before inviting reuse or contributions.

## Changes

* **0.1.2:** command line APK build, repository hygiene, consolidated public documentation and content design principles in the in-app guide. No storage or questionnaire logic changes from 0.1.1.
* **0.1.1:** verbal labels for integer scales; seven example practices instead of thirteen; observable interference replaces retrospective duration; daily assessment shortened to five questions. Older numeric scales remain supported. Changed question meanings/ranges use new IDs.
* **0.1.0:** initial local app, JSON categories, reporting, reminders and historical snapshots.
