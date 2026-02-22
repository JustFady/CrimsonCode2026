#!/bin/bash

# Test script to debug Supabase OTP issues
# This script checks common configuration issues

echo "=========================================="
echo "Supabase OTP Debug Test"
echo "=========================================="
echo ""

# Check if .env file exists
if [ -f ".env" ]; then
    echo "✓ .env file found"
    echo ""
    echo "Contents of .env (sensitive):"
    # Show just the keys, not values
    grep "=" .env | sed 's/=.*$/=***/'
else
    echo "✗ .env file NOT found"
fi

echo ""
echo "=========================================="
echo "Supabase Configuration Check"
echo "=========================================="

# Extract values from .env
SUPABASE_URL=$(grep "^SUPABASE_URL=" .env | cut -d'=' -f2)
SUPABASE_ANON_KEY=$(grep "^SUPABASE_ANON_KEY=" .env | cut -d'=' -f2)

echo "SUPABASE_URL: $SUPABASE_URL"
echo "SUPABASE_ANON_KEY: ${SUPABASE_ANON_KEY:0:20}... (truncated)"
echo ""

# Check if the values are placeholders
if [[ "$SUPABASE_URL" == *"your-project-ref"* ]]; then
    echo "✗ SUPABASE_URL is still a placeholder!"
    echo "  Please set your actual Supabase project URL"
elif [[ -z "$SUPABASE_URL" ]]; then
    echo "✗ SUPABASE_URL is empty!"
else
    echo "✓ SUPABASE_URL appears to be set"
fi

if [[ "$SUPABASE_ANON_KEY" == *"your-anon-key"* ]]; then
    echo "✗ SUPABASE_ANON_KEY is still a placeholder!"
    echo "  Please set your actual Supabase anon key"
elif [[ -z "$SUPABASE_ANON_KEY" ]]; then
    echo "✗ SUPABASE_ANON_KEY is empty!"
else
    echo "✓ SUPABASE_ANON_KEY appears to be set"
fi

echo ""
echo "=========================================="
echo "Environment Variables Check"
echo "=========================================="

# Check if variables are exported to current shell environment
if [ -z "$SUPABASE_URL" ]; then
    echo "⚠ SUPABASE_URL is NOT set in shell environment"
    echo "  This is the ROOT CAUSE - AppConfig.kt uses System.getenv()"
    echo "  which only reads from OS environment, not .env file"
else
    echo "✓ SUPABASE_URL is available in environment"
fi

echo ""
echo "=========================================="
echo "Supabase Connection Test"
echo "=========================================="

# Test connection to Supabase
if [[ -n "$SUPABASE_URL" ]]; then
    echo "Testing connection to Supabase..."
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$SUPABASE_URL/auth/v1/user")

    if [ "$RESPONSE" == "401" ] || [ "$RESPONSE" == "200" ]; then
        echo "✓ Supabase server is reachable (HTTP $RESPONSE)"
    elif [ "$RESPONSE" == "000" ]; then
        echo "✗ Cannot connect to Supabase server"
        echo "  Check your internet connection and verify the URL"
    else
        echo "⚠ Supabase responded with HTTP $RESPONSE"
    fi
else
    echo "✗ Cannot test connection - SUPABASE_URL not set"
fi

echo ""
echo "=========================================="
echo "Fix Recommendations"
echo "=========================================="
echo ""
echo "CRITICAL ISSUE:"
echo "The .env file exists but is NOT being loaded by the app."
echo ""
echo "You need to do ONE of the following:"
echo ""
echo "Option 1: Use the .env file (Recommended for development)"
echo "  1. Add this to androidApp/build.gradle.kts:"
echo ""
echo "  android {"
echo "    defaultConfig {"
echo "      // ... existing config ..."
echo "      buildConfigField(\"String\", \"SUPABASE_URL\", \"\\\"\\$SUPABASE_URL\\\"\")"
echo "      buildConfigField(\"String\", \"SUPABASE_ANON_KEY\", \"\\\"\\$SUPABASE_ANON_KEY\\\"\")"
echo "    }"
echo "  }"
echo ""
echo "  2. Modify Config.kt to use BuildConfig instead of System.getenv()"
echo ""
echo "Option 2: Set environment variables directly"
echo "  export SUPABASE_URL=$SUPABASE_URL"
echo "  export SUPABASE_ANON_KEY=$SUPABASE_ANON_KEY"
echo ""
echo "Option 3: Use a gradle plugin to load .env"
echo "  Add 'com.google.android.libraries.mapsplatform.secrets-gradle-plugin' to plugins"
echo ""
echo ""
echo "TWILIO SETUP:"
echo "1. Go to https://supabase.com/dashboard"
echo "2. Navigate to Authentication > Providers"
echo "3. Enable Phone provider"
echo "4. Configure Twilio (Account SID, Auth Token, Phone Number)"
echo "5. Ensure the phone number format matches E.164 (+1XXXXXXXXXX)"
echo ""
