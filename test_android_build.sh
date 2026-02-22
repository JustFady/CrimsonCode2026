#!/bin/bash

# Android Build and Configuration Verification Script

echo "=========================================="
echo "Android Build & Configuration Test"
echo "=========================================="
echo ""

# Check if local.properties exists
LOCAL_PROPS="apps/mobile/androidApp/local.properties"
if [ -f "$LOCAL_PROPS" ]; then
    echo "✓ $LOCAL_PROPS found"
    echo ""
    echo "Contents:"
    cat "$LOCAL_PROPS"
else
    echo "✗ $LOCAL_PROPS NOT found"
    echo ""
    echo "Creating local.properties..."
    mkdir -p "$(dirname "$LOCAL_PROPS")"
    cat > "$LOCAL_PROPS" << 'EOF'
# Supabase Configuration for Android App

supabase.url=https://xgkgsekeqrqcwzapxfcj.supabase.co/
supabase.anon.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhna2dzZWtlcXJxY3d6YXB4ZmNqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE3MDg1MDksImV4cCI6MjA4NzI4NDUwOX0.-0I5AgOUh0AatvuOKFFG5e98wfreLnbz3l3_Ns_Mv-U
EOF
    echo "✓ Created $LOCAL_PROPS"
fi

echo ""
echo "=========================================="
echo "Testing Gradle Sync"
echo "=========================================="
echo ""

# Try to run Gradle tasks
./gradlew :mobile:tasks --quiet 2>&1 | head -20

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Gradle sync successful"
else
    echo ""
    echo "⚠ Gradle sync had issues, check the output above"
fi

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "Changes made:"
echo "1. ✓ Created apps/mobile/androidApp/local.properties with Supabase credentials"
echo "2. ✓ Updated apps/mobile/build.gradle.kts to load local.properties and create BuildConfig fields"
echo "3. ✓ Updated shared/kotlin/org/crimsoncode2026/di/Config.kt to use BuildConfig"
echo "4. ✓ Added local.properties to .gitignore"
echo ""
echo "Next steps:"
echo "1. Configure Twilio in Supabase dashboard:"
echo "   https://supabase.com/dashboard/project/xgkgsekeqrqcwzapxfcj/auth/providers"
echo ""
echo "2. Build and run the app:"
echo "   ./gradlew :mobile:installDebug"
echo ""
echo "3. Test OTP flow:"
echo "   - Open the app"
echo "   - Enter phone number (E.164 format: +1XXXXXXXXXX)"
echo "   - Send and verify OTP"
echo ""
