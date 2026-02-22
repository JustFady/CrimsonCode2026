# Firebase Setup for Push Notifications

This project uses KMPNotifier with Firebase Cloud Messaging (FCM) for cross-platform push notifications.

## Setup Instructions

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Enable Cloud Messaging API

### 2. Android Configuration

1. Add Android app to Firebase project:
   - Package name: `org.crimsoncode2026`
   - Debug signing certificate SHA-1 (optional, for debug builds)

2. Download `google-services.json`

3. Place `google-services.json` in:
   ```
   apps/mobile/androidApp/google-services.json
   ```

4. The `google-services` plugin in `build.gradle.kts` will automatically process the file during build.

### 3. iOS Configuration

1. Add iOS app to Firebase project:
   - Bundle ID: Set to your iOS bundle ID
   - App Store ID: Optional for development

2. Download `GoogleService-Info.plist`

3. Place `GoogleService-Info.plist` in:
   ```
   apps/mobile/iosApp/GoogleService-Info.plist
   ```

4. Add the file to your Xcode project:
   - Open `iosApp/iosApp.xcodeproj` in Xcode
   - Drag `GoogleService-Info.plist` into the project navigator
   - Ensure it's added to all targets

5. Enable Push Notifications capability:
   - Select your app target
   - Signing & Capabilities tab
   - Click "+ Capability"
   - Add "Push Notifications"

### 4. Backend Configuration

Update `.env` with your Firebase FCM Server Key:

```
FCM_SERVER_KEY=your_fcm_server_key_here
```

Find the server key in Firebase Console:
- Project Settings → Cloud Messaging → Server Key

## Notes

- Firebase config files are excluded from git (.gitignore)
- Use `google-services.json.template` and `GoogleService-Info.plist.template` as references
- Never commit actual Firebase credentials to version control
- Separate config files for development and production environments
