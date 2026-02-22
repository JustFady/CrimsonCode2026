# Supabase OTP to Twilio Integration - Issues & Fixes

## Root Cause Found

When testing the Supabase OTP endpoint, we received:

```json
{
  "code": 422,
  "error_code": "sms_send_failed",
  "msg": "Error sending confirmation OTP to provider: Authentication Error - invalid username More information: https://www.twilio.com/docs/errors/20003"
}
```

**Twilio Error 20003** means: "Authentication Error - invalid username" - i.e., Twilio Account SID is not configured or is invalid in Supabase.

---

## Required Fixes

### Fix #1: Configure Twilio in Supabase Dashboard (Required)

1. **Get Twilio Credentials** (if you don't have them):
   - Sign up at https://www.twilio.com
   - Get your Account SID, Auth Token, and a Twilio phone number
   - **Note:** You may need to upgrade from trial or verify your phone number

2. **Configure in Supabase**:
   - Go to: https://supabase.com/dashboard/project/xgkgsekeqrqcwzapxfcj/auth/providers
   - Click on **Phone** provider
   - Toggle **Enable Phone provider** to ON
   - Fill in:
     - **Twilio Account SID**: from Twilio console
     - **Twilio Auth Token**: from Twilio console
     - **Twilio Phone Number**: your Twilio sender number in E.164 format (e.g., +1234567890)
   - Click **Save**

3. **Verify SMS Templates** (optional but recommended):
   - In the Phone provider settings, you can customize the SMS template
   - Default: `Your code is {{ .Code }}`

---

### Fix #2: Ensure Environment Variables Reach the App (Required for Development)

Currently, `AppConfig.kt` uses `System.getenv()`, but the `.env` file values aren't loaded into the app.

#### Option A: Quick Test - Export Environment Variables

Run the app with environment variables set:

```bash
export SUPABASE_URL="https://xgkgsekeqrqcwzapxfcj.supabase.co/"
export SUPABASE_ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhna2dzZWtlcXJxY3d6YXB4ZmNqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE3MDg1MDksImV4cCI6MjA4NzI4NDUwOX0.-0I5AgOUh0AatvuOKFFG5e98wfreLnbz3l3_Ns_Mv-U"

# Then run your app (Android Studio or command line)
./gradlew :androidApp:installDebug
```

#### Option B: Proper Fix - Add .env Loading to Gradle

1. Create `apps/mobile/androidApp/local.properties` (or add to existing):
```properties
supabase.url=https://xgkgsekeqrqcwzapxfcj.supabase.co/
supabase.anon.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

2. Update `apps/mobile/androidApp/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        // ... existing code ...

        // Read from local.properties or environment
        val supabaseUrl = System.getenv("SUPABASE_URL") ?: (project.findProperty("supabase.url") as String? ?: "")
        val supabaseAnonKey = System.getenv("SUPABASE_ANON_KEY") ?: (project.findProperty("supabase.anon.key") as String? ?: "")

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures {
        buildConfig = true  // Enable BuildConfig
    }
}
```

3. Update `AppConfig.kt`:
```kotlin
object AppConfig {
    val supabaseUrl: String
        get() = try {
            BuildConfig.SUPABASE_URL
        } catch (e: Exception) {
            System.getenv("SUPABASE_URL") ?: "https://your-project-ref.supabase.co"
        }

    val supabaseAnonKey: String
        get() = try {
            BuildConfig.SUPABASE_ANON_KEY
        } catch (e: Exception) {
            System.getenv("SUPABASE_ANON_KEY") ?: "your-anon-key-here"
        }
}
```

---

## Test After Fixes

### Test 1: Verify Twilio Configuration

```bash
export SUPABASE_URL="https://xgkgsekeqrqcwzapxfcj.supabase.co/"
export SUPABASE_ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhna2dzZWtlcXJxY3d6YXB4ZmNqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE3MDg1MDksImV4cCI6MjA4NzI4NDUwOX0.-0I5AgOUh0AatvuOKFFG5e98wfreLnbz3l3_Ns_Mv-U"

curl -X POST "${SUPABASE_URL}/auth/v1/otp" \
  -H "apikey: ${SUPABASE_ANON_KEY}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${SUPABASE_ANON_KEY}" \
  -d '{
    "phone": "+15551234567",
    "channel": "sms"
  }'
```

**Expected success response:**
```json
{
  "message": "SMS sent to +15551234567",
  "phone": "+15551234567"
}
```

### Test 2: In-App Test

1. Open the app
2. Enter a phone number in E.164 format (+1XXXXXXXXXX)
3. Click "Send Verification Code"
4. Check if SMS is received
5. Enter the OTP code to verify

---

## Common Twilio Issues

| Issue | Solution |
|-------|----------|
| Error 20003 (Invalid username) | Wrong Account SID in Supabase |
| Error 20003 (Invalid password) | Wrong Auth Token in Supabase |
| Error 21614 (Unreachable number) | Phone number format incorrect or not in E.164 |
| No SMS received | Trial account can only send to verified numbers |
| Rate limit errors | Too many OTP requests - wait and retry |

---

## Summary

**Primary Issue:** Twilio credentials not configured in Supabase dashboard
**Secondary Issue:** Environment variables from `.env` not reaching the Android app

**Order of Fixes:**
1. Configure Twilio in Supabase dashboard (Fix #1)
2. Set environment variables before running the app (Fix #2 - Option A for quick test)
3. Implement proper .env loading in Gradle (Fix #2 - Option B for permanent solution)
