# CrimsonCode2026

CrimsonCode2026 is a Kotlin Multiplatform emergency response mobile app. The current runnable app is the Android Compose build with phone OTP screens, settings, contacts, map/list event browsing, and event creation when Supabase credentials are configured.

## Current Status

The Android app builds, installs, and launches on the local emulator.

Verified locally:

```bash
./gradlew :mobile:assembleDebug :mobile:compileKotlinIosSimulatorArm64
./gradlew :mobile:installDebug
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell monkey -p org.crimsoncode2026 -c android.intent.category.LAUNCHER 1
```

What works without backend keys:

- App launch
- Local shell OTP flow
- Settings screen
- Contacts shell
- Map screen
- Event list/detail UI

What needs Supabase/Firebase keys:

- Real SMS OTP
- Live public events from Supabase
- Persisted event creation
- Push notifications

## Requirements

- macOS
- JDK 17 or newer
- Android Studio or Android SDK
- Android SDK platform 36
- Android emulator named `Medium_Phone_API_36.1`
- Optional: Supabase project URL and anon key
- Optional: Firebase Android `google-services.json`

This workspace expects the Android SDK here:

```bash
/Users/fadyy/Library/Android/sdk
```

## One-Time Local Setup

Create the root Android SDK config:

```bash
cat > local.properties <<'EOF'
sdk.dir=/Users/fadyy/Library/Android/sdk
EOF
```

`local.properties` is gitignored and should stay local.

Confirm the emulator exists:

```bash
/Users/fadyy/Library/Android/sdk/emulator/emulator -list-avds
```

Expected output includes:

```text
Medium_Phone_API_36.1
```

## Run The App Visibly

From the repo root:

```bash
cd /Users/fadyy/CrimsonCode2026
```

Start the emulator UI:

```bash
/Users/fadyy/Library/Android/sdk/emulator/emulator \
  -avd Medium_Phone_API_36.1 \
  -no-snapshot \
  -gpu swiftshader_indirect
```

In another terminal, wait for Android to finish booting:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done; echo booted'
```

Install the debug app:

```bash
./gradlew :mobile:installDebug
```

Launch CrimsonCode:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell monkey \
  -p org.crimsoncode2026 \
  -c android.intent.category.LAUNCHER \
  1
```

Confirm it is running:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell pidof org.crimsoncode2026
```

If the command prints a number, the app process is alive.

Check for launch crashes:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb logcat -d -t 200 | rg -i "org.crimsoncode2026|FATAL EXCEPTION|AndroidRuntime"
```

## Build Commands

Build the Android debug APK:

```bash
./gradlew :mobile:assembleDebug
```

Build Android and compile the iOS simulator shared target:

```bash
./gradlew :mobile:assembleDebug :mobile:compileKotlinIosSimulatorArm64
```

The debug APK is created at:

```text
apps/mobile/build/outputs/apk/debug/mobile-debug.apk
```

## Backend Credentials

The app opens without credentials. Real backend behavior needs Supabase.

Create `apps/mobile/androidApp/local.properties`:

```bash
mkdir -p apps/mobile/androidApp
cat > apps/mobile/androidApp/local.properties <<'EOF'
supabase.url=https://your-project-ref.supabase.co
supabase.anon.key=your-supabase-anon-key
EOF
```

Or provide environment variables before building:

```bash
export SUPABASE_URL=https://your-project-ref.supabase.co
export SUPABASE_ANON_KEY=your-supabase-anon-key
```

Then rebuild and reinstall:

```bash
./gradlew :mobile:installDebug
```

Inside the app:

- Use shell mode for local-only OTP navigation.
- Turn on real auth in Settings when Supabase credentials are configured.
- Enter a phone number in E.164 format, such as `+15551234567`.
- Use the map screen to load public events and create events.

## Firebase Push Setup

Firebase is optional for the debug MVP build. If `google-services.json` is missing, Gradle logs:

```text
MVP build: google-services.json not found, skipping Google Services plugin.
```

To enable Firebase services:

```bash
cp apps/mobile/androidApp/google-services.json.template apps/mobile/androidApp/google-services.json
```

Then replace the template values with the Firebase Android app config from the Firebase console.

## Backend Files

Supabase schema and edge-function code live in:

```text
backend/migrations/
backend/functions/
backend/config/supabase.toml
```

More backend detail:

- [backend/README.md](backend/README.md)
- [backend/functions/README.md](backend/functions/README.md)
- [docs/dbSchema.md](docs/dbSchema.md)

## Tests And Known Caveat

Use this as the current quality gate:

```bash
./gradlew :mobile:assembleDebug :mobile:compileKotlinIosSimulatorArm64
```

Known caveat:

```bash
./gradlew :mobile:allTests
```

currently reaches app compilation, then fails while compiling legacy `commonTest` sources. Those tests reference modules excluded from the MVP source set (`auth`, `storage`, notification/realtime/performance helpers) and JVM-only APIs. Follow-up issue:

```text
beads_CrimsonCode2026-shd — Restore or prune stale commonTest suite
```

## Useful ADB Commands

List devices:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb devices
```

Re-launch the app:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell monkey -p org.crimsoncode2026 -c android.intent.category.LAUNCHER 1
```

Stop the emulator:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb emu kill
```

## Issue Tracking

This project uses `bd` (beads).

Initial setup after a fresh clone:

```bash
bd onboard
bd bootstrap
```

Useful commands:

```bash
bd ready
bd show <id>
bd update <id> --status in_progress
bd close <id>
bd export -o .beads/issues.jsonl
```

This installed `bd` version does not provide `bd sync`; use `bd export -o .beads/issues.jsonl` to update the tracked issue export after issue changes.
