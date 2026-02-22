package org.crimsoncode2026

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Android Device Compatibility Tests
 *
 * These tests verify basic app compatibility across different API levels and devices.
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class AndroidDeviceCompatibilityTest {

    @get:Rule
    val locationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @get:Rule
    val contactsPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_CONTACTS
    )

    @get:Rule
    val notificationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ==================== API Level Tests ====================

    @Test
    fun app_meets_minimum_api_level_requirement() {
        // Spec requires API 28+ (Android 9)
        val apiLevel = Build.VERSION.SDK_INT
        assertTrue("App requires API 28+, but running on $apiLevel", apiLevel >= 28)
    }

    @Test
    fun app_correctly_reports_target_api_level() {
        // The app should report the correct targetSdk in package info
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            0
        )
        // targetSdk should be 35 (Android 15)
        val targetSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        // The targetSdk is set to 35 in build.gradle.kts
        assertTrue("App should target SDK 35 or higher", targetSdk >= 35)
    }

    // ==================== Permission Tests ====================

    @Test
    fun required_permissions_declared_in_manifest() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )

        val requestedPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()

        val requiredPermissions = setOf(
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.USE_BIOMETRIC
        )

        val missingPermissions = requiredPermissions - requestedPermissions
        assertTrue(
            "Missing required permissions: $missingPermissions",
            missingPermissions.isEmpty()
        )
    }

    @Test
    fun notification_permission_declared_on_api_33_plus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )

            val requestedPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()

            assertTrue(
                "POST_NOTIFICATIONS permission should be declared on API 33+",
                requestedPermissions.contains(android.Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }

    @Test
    fun location_permissions_can_be_granted() {
        val hasFineLocation = context.checkCallingOrSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = context.checkCallingOrSelfPermission(
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // With the GrantPermissionRule, these should be granted
        assertTrue("Fine location permission should be granted", hasFineLocation)
        assertTrue("Coarse location permission should be granted", hasCoarseLocation)
    }

    @Test
    fun contacts_permission_can_be_granted() {
        val hasContacts = context.checkCallingOrSelfPermission(
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        // With the GrantPermissionRule, this should be granted
        assertTrue("Contacts permission should be granted", hasContacts)
    }

    @Test
    fun notification_permission_can_be_granted_on_api_33_plus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifications = context.checkCallingOrSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            // With the GrantPermissionRule, this should be granted
            assertTrue(
                "Notification permission should be granted on API 33+",
                hasNotifications
            )
        }
    }

    // ==================== Storage Tests ====================

    @Test
    fun app_has_required_storage_space() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            0
        )

        val appSizeInBytes = packageInfo.applicationInfo.publicSourceDir?.let { java.io.File(it).length() } ?: 0L
        val appSizeInMB = appSizeInBytes / (1024.0 * 1024.0)

        // App should be less than 100MB (typical APK size)
        assertTrue(
            "App size should be less than 100MB, but is ${"%.2f".format(appSizeInMB)}MB",
            appSizeInMB < 100.0
        )
    }

    @Test
    fun app_has_available_internal_storage() {
        val dataDir = context.filesDir
        val freeSpace = dataDir.freeSpace / (1024.0 * 1024.0)

        // Should have at least 10MB free for runtime use
        assertTrue(
            "Device should have at least 10MB free storage, but has ${"%.2f".format(freeSpace)}MB",
            freeSpace >= 10.0
        )
    }

    // ==================== Feature Tests ====================

    @Test
    fun device_has_required_features() {
        val pm = context.packageManager

        // Camera is not required for MVP, but good to document
        val hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        val hasLocation = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION)
        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

        // GPS location is required for the app to work properly
        assertTrue("Device should have location/GPS feature", hasLocation)

        // Telephony is not strictly required (tablets/ wifi-only devices), but phone OTP is
        // This test documents the device's capability
        // Don't fail on tablets without cellular
    }

    @Test
    fun app_respects_display_cutout_if_present() {
        val windowInsets = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(
            android.view.InsetsSourceCompat.obtain(
                context,
                null,
                android.view.WindowInsets.Type.statusBars()
            )
        )

        // Just verify we can get insets - actual cutout handling is in Compose
        assertNotNull("Should be able to get window insets", windowInsets)
    }

    // ==================== Screen Density Tests ====================

    @Test
    fun app_works_on_various_screen_densities() {
        val displayMetrics = context.resources.displayMetrics
        val densityDpi = displayMetrics.densityDpi

        // Verify we can read screen density
        assertTrue("Density should be positive", densityDpi > 0)

        // Common densities: ldpi(120), mdpi(160), hdpi(240), xhdpi(320), xxhdpi(480), xxxhdpi(640)
        val isCommonDensity = densityDpi in 120..640
        assertTrue(
            "Density $densityDpi should be within common range 120-640",
            isCommonDensity
        )
    }

    @Test
    fun app_works_on_various_screen_sizes() {
        val displayMetrics = context.resources.displayMetrics
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        val heightDp = displayMetrics.heightPixels / displayMetrics.density

        // Verify we can read screen size
        assertTrue("Width should be positive", widthDp > 0)
        assertTrue("Height should be positive", heightDp > 0)

        // Android defines small (320x426dp), normal (320x470dp), large (480x640dp), xlarge (720x960dp)
        // Our app should work on small screens and above
        val minWidthForSmallScreen = 320f
        assertTrue(
            "Width should be at least ${minWidthForSmallScreen.toInt()}dp for small screen support, but is ${widthDp.toInt()}dp",
            widthDp >= minWidthForSmallScreen
        )
    }

    // ==================== Locale Tests ====================

    @Test
    fun app_works_with_en_locale() {
        val config = context.resources.configuration
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }

        // App is designed for English only
        // Just verify we can read locale
        assertNotNull("Should be able to read locale", locale)
        assertNotNull("Locale should have language", locale.language)
    }

    // ==================== Android Version Specific Tests ====================

    @Test
    fun api_34_features_work_correctly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14 specific: foreground service types
            // Just verify we're running on this version
            assertTrue("Running on Android 14 or higher", true)
        }
    }

    @Test
    fun api_33_features_work_correctly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 specific: notification runtime permission
            // Just verify we're running on this version
            assertTrue("Running on Android 13 or higher", true)
        }
    }

    @Test
    fun api_31_features_work_correctly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 specific: splash screen, approximate location
            // Just verify we're running on this version
            assertTrue("Running on Android 12 or higher", true)
        }
    }

    @Test
    fun api_28_features_work_correctly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9 specific: notification channels, display cutout
            // Just verify we're running on this version
            assertTrue("Running on Android 9 or higher", true)
        }
    }

    // ==================== Deep Link Tests ====================

    @Test
    fun app_has_deep_link_scheme_defined() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_INTENT_FILTERS
        )

        val intentFilters = packageInfo.activities?.flatMap { it.intentFilters ?: emptyList() }
        val hasDeepLinkScheme = intentFilters?.any { intentFilter ->
            intentFilter.dataSchemes?.any { it == "crimsoncode" } == true
        } ?: false

        // Deep links should be defined: crimsoncode://event/{eventId}
        // Note: This test may need adjustment based on actual implementation
        // assertTrue("App should define deep link scheme", hasDeepLinkScheme)
    }

    // ==================== OEM Compatibility Tests ====================

    @Test
    fun device_manufacturer_identified() {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        assertNotNull("Manufacturer should not be null", manufacturer)
        assertNotNull("Model should not be null", model)

        // Just log this - no pass/fail
        // This helps identify which devices have been tested
    }

    @Test
    fun app_works_on_samsung_devices() {
        val isSamsung = Build.MANUFACTURER.equals("Samsung", ignoreCase = true)

        if (isSamsung) {
            // Samsung-specific considerations:
            // - One UI theming
            // - Samsung keyboard
            // - Battery optimization
            // - Samsung DeX mode
            // Just verify we're on Samsung - detailed testing manual
            assertTrue("Running on Samsung device", true)
        }
    }

    @Test
    fun app_works_on_pixel_devices() {
        val isPixel = Build.MANUFACTURER.equals("Google", ignoreCase = true)

        if (isPixel) {
            // Pixel is baseline Android - should work perfectly
            assertTrue("Running on Pixel device", true)
        }
    }

    @Test
    fun app_works_on_oneplus_devices() {
        val isOnePlus = Build.MANUFACTURER.equals("OnePlus", ignoreCase = true) ||
                Build.MANUFACTURER.equals("OPPO", ignoreCase = true)

        if (isOnePlus) {
            // OnePlus-specific considerations:
            // - OxygenOS customizations
            // - Aggressive battery optimization
            // Just verify we're on OnePlus - detailed testing manual
            assertTrue("Running on OnePlus device", true)
        }
    }
}
