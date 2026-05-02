# CrimsonCode2026

CrimsonCode2026 is a Kotlin Multiplatform emergency response mobile app. The current runnable surface is the Compose-based mobile shell with phone OTP entry, settings, contacts, public event map/list viewing, and event creation against Supabase when credentials are configured.

## What You Need

- macOS with JDK 17+
- Android Studio or Android SDK installed
- Android SDK platform 36
- An Android emulator/device for the fastest full run
- Optional: Supabase project credentials for real OTP and live public events
- Optional: Firebase config for push notification services

This workspace is set up for the local Android SDK at:

```bash
/Users/fadyy/Library/Android/sdk
```

The root `local.properties` file is intentionally gitignored and should contain:

```properties
sdk.dir=/Users/fadyy/Library/Android/sdk
```

## Configure Backend Credentials

The app can open without credentials, but real OTP, live events, and event creation require Supabase.

Create `apps/mobile/androidApp/local.properties`:

```properties
supabase.url=https://your-project-ref.supabase.co
supabase.anon.key=your-supabase-anon-key
```

You can also provide these as environment variables:

```bash
export SUPABASE_URL=https://your-project-ref.supabase.co
export SUPABASE_ANON_KEY=your-supabase-anon-key
```

For Firebase push setup, copy the Android template and fill it with your project values:

```bash
cp apps/mobile/androidApp/google-services.json.template apps/mobile/androidApp/google-services.json
```

`google-services.json` is optional for the MVP debug build. If it is absent, Gradle skips the Google Services plugin.

## Build

```bash
./gradlew :mobile:assembleDebug
```

Useful multiplatform compile gate:

```bash
./gradlew :mobile:assembleDebug :mobile:compileKotlinIosSimulatorArm64
```

The APK is written to:

```bash
apps/mobile/build/outputs/apk/debug/mobile-debug.apk
```

## Run On Android

Start the included emulator:

```bash
/Users/fadyy/Library/Android/sdk/emulator/emulator \
  -avd Medium_Phone_API_36.1 \
  -no-window \
  -no-audio \
  -no-snapshot \
  -gpu swiftshader_indirect
```

In another terminal, wait for boot:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb wait-for-device shell getprop sys.boot_completed
```

Install the app:

```bash
./gradlew :mobile:installDebug
```

Launch it:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell monkey \
  -p org.crimsoncode2026 \
  -c android.intent.category.LAUNCHER \
  1
```

Check that it stayed up:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell pidof org.crimsoncode2026
/Users/fadyy/Library/Android/sdk/platform-tools/adb logcat -d -t 200 | rg -i "org.crimsoncode2026|FATAL EXCEPTION|AndroidRuntime"
```

## Using The App

- Use shell mode to move through OTP locally without a backend.
- Enable real auth and set Supabase credentials in Settings to send and verify OTP through Supabase.
- The map screen loads public events from Supabase when credentials are present.
- Tap `+` to create an event at the current device location or map center.
- Use the event list and details panel to browse, clear, and navigate to events.

## Backend

Supabase migrations and edge functions live under `backend/`.

```bash
backend/migrations/
backend/functions/
```

See [backend/README.md](backend/README.md) and [docs/dbSchema.md](docs/dbSchema.md) for schema and deployment notes.

## Verification Status

Verified locally:

```bash
./gradlew :mobile:assembleDebug :mobile:compileKotlinIosSimulatorArm64
./gradlew :mobile:installDebug
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell monkey -p org.crimsoncode2026 -c android.intent.category.LAUNCHER 1
```

The launched Android process was confirmed with:

```bash
/Users/fadyy/Library/Android/sdk/platform-tools/adb shell pidof org.crimsoncode2026
```

Known test-suite caveat: `./gradlew :mobile:allTests` currently reaches app compilation, then fails while compiling the legacy `commonTest` sources. Those tests reference modules that the MVP source set intentionally excludes (`auth`, `storage`, notification/realtime/performance helpers) and also contain JVM-only test APIs. Treat `assembleDebug` plus `compileKotlinIosSimulatorArm64` as the current app build gate until the test suite is pruned or restored with the excluded modules.

## Issue Tracking

This project uses `bd` (beads). Start with:

```bash
bd onboard
bd ready
bd show <id>
bd update <id> --status in_progress
bd close <id>
bd sync
```
