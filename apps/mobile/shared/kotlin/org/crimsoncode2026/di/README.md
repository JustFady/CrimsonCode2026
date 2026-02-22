# Dependency Injection Configuration

This directory contains the dependency injection (DI) setup for the Emergency Response App using Koin.

## Files

| File | Purpose |
|-------|---------|
| `Config.kt` | App configuration loaded from environment variables |
| `Supabase.kt` | Supabase client factory and Koin module |
| `Koin.kt` | Main Koin initialization with all app modules |

## Environment Variables

Required environment variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `SUPABASE_URL` | Supabase project URL | `https://xyzabc123.supabase.co` |
| `SUPABASE_ANON_KEY` | Supabase anon/public key | `eyJhbGc...` |

Optional environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `SUPABASE_SERVICE_ROLE_KEY` | Service role key (admin only) | `your-service-role-key-here` |
| `FCM_SERVER_KEY` | Firebase Cloud Messaging server key | empty string |
| `NETWORK_CACHE_DURATION` | Cache duration for network requests (seconds) | 10 |
| `DATA_CACHE_TTL` | Time-to-live for cached data (seconds) | 300 |

## Setting Environment Variables

### Android

Environment variables can be set via:

**1. local.properties** (for local development):
```properties
SUPABASE_URL=https://xyzabc123.supabase.co
SUPABASE_ANON_KEY=eyJhbGc...
FCM_SERVER_KEY=your-fcm-key
```

**2. Gradle run configuration** (for Android Studio):
Add environment variables to the run configuration in Android Studio:
- Run -> Edit Configurations -> Select your configuration
- Environment variables section

**3. CI/CD pipelines**:
```bash
export SUPABASE_URL=https://xyzabc123.supabase.co
export SUPABASE_ANON_KEY=eyJhbGc...
export FCM_SERVER_KEY=your-fcm-key
```

### iOS

For iOS, add configuration to your Xcode project:

**1. Using Info.plist**:
```xml
<key>SUPABASE_URL</key>
<string>https://xyzabc123.supabase.co</string>
<key>SUPABASE_ANON_KEY</key>
<string>eyJhbGc...</string>
```

**2. Using xcconfig files** (recommended):
Create a `.xcconfig` file and add to build settings:
```
SUPABASE_URL = https://xyzabc123.supabase.co
SUPABASE_ANON_KEY = eyJhbGc...
```

## Security Notes

**CRITICAL:**
- **NEVER** commit `.env` file to version control
- **NEVER** use `SUPABASE_SERVICE_ROLE_KEY` in mobile app code
- Service role key should ONLY be used in Supabase Edge Functions (backend)

The `.env.example` file in the project root shows the required format.
Create a real `.env` file with your actual credentials.

## Configuration Access

Configuration is loaded on-demand using lazy initialization:

```kotlin
// Access configuration
val url = AppConfig.supabaseUrl
val anonKey = AppConfig.supabaseAnonKey
```

## Validation

The `AppConfig.validate()` method checks if required values are present and not placeholders:

```kotlin
if (!AppConfig.validate()) {
    // Configuration is incomplete - handle appropriately
}
```

## Supabase Client Modules

The Supabase client is initialized with three modules:

| Module | Purpose |
|---------|---------|
| `Auth` | Phone OTP authentication, session management |
| `Postgrest` | Database access via REST API |
| `Realtime` | Real-time subscriptions for live updates |

## Koin Modules

| Module | Provides |
|---------|-----------|
| `supabaseClientModule` | SupabaseClient, Auth, Postgrest, Realtime |
| `locationModule` | Location services and state management |

## Example Usage

```kotlin
// Initialize Koin in Application class
initKoin()

// Inject Supabase client
val supabaseClient: SupabaseClient by inject()

// Use Auth
supabaseClient.auth.signInWithOtp(...)

// Use Postgrest
supabaseClient.postgrest.from("events").select()

// Use Realtime
supabaseClient.realtime.connect()
```

## Troubleshooting

### Configuration not loading
- Check that environment variables are properly set
- Verify variable names match exactly (case-sensitive)
- For Android: check `local.properties` file location
- For iOS: check Info.plist or xcconfig files

### Invalid URL or Key errors
- Verify URL format: `https://[project-ref].supabase.co` (no trailing slash)
- Verify anon key starts with `eyJ` (JWT format)
- Check values in Supabase dashboard -> Settings -> API

### Service role key errors
- Service role key should only be used in Edge Functions
- Don't use it in mobile app
- Verify you're using anon key in app code

### Build errors after config changes
- Clean and rebuild: `./gradlew clean build`
- Clear IDE caches
- Check for conflicting `actual` declarations (should only have one per expect)
