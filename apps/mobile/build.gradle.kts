import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load local.properties for Supabase credentials
val localProperties = Properties()
val localPropertiesFile = rootProject.file("apps/mobile/androidApp/local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.googleServices)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            export(libs.kmpnotifier.get())
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("shared/kotlin")
            kotlin.srcDir("shared/domain")
            kotlin.srcDir("shared/compose")
            kotlin.exclude("**/*Test.kt")
            kotlin.exclude("**/OptimizedRealtimeService.kt")
            // shared/kotlin source root excludes (package-style paths)
            kotlin.exclude("org/crimsoncode2026/auth/**")
            kotlin.exclude("org/crimsoncode2026/contacts/**")
            kotlin.exclude("org/crimsoncode2026/di/**")
            kotlin.exclude("org/crimsoncode2026/domain/**")
            kotlin.exclude("org/crimsoncode2026/location/**")
            kotlin.exclude("org/crimsoncode2026/network/**")
            kotlin.exclude("org/crimsoncode2026/notifications/**")
            kotlin.exclude("org/crimsoncode2026/screens/auth/**")
            kotlin.exclude("org/crimsoncode2026/screens/contacts/**")
            kotlin.exclude("org/crimsoncode2026/screens/eventcreation/**")
            kotlin.exclude("org/crimsoncode2026/screens/privateevents/**")
            kotlin.exclude("org/crimsoncode2026/screens/settings/**")
            kotlin.exclude("org/crimsoncode2026/screens/main/MainMapViewModel.kt")
            kotlin.exclude("org/crimsoncode2026/storage/**")
            kotlin.exclude("org/crimsoncode2026/compose/**")
            kotlin.exclude("org/crimsoncode2026/utils/**")
            // Keep only Event/User models from data package for UI restoration.
            kotlin.exclude("org/crimsoncode2026/data/EventRecipient.kt")
            kotlin.exclude("org/crimsoncode2026/data/EventRecipientRepository.kt")
            kotlin.exclude("org/crimsoncode2026/data/EventRepository.kt")
            kotlin.exclude("org/crimsoncode2026/data/OptimizedRealtimeService.kt")
            kotlin.exclude("org/crimsoncode2026/data/PushNotificationPayload.kt")
            kotlin.exclude("org/crimsoncode2026/data/RealtimeService.kt")
            kotlin.exclude("org/crimsoncode2026/data/RetryAwareEventRecipientRepository.kt")
            kotlin.exclude("org/crimsoncode2026/data/RetryAwareEventRepository.kt")
            kotlin.exclude("org/crimsoncode2026/data/UserContact.kt")
            kotlin.exclude("org/crimsoncode2026/data/UserContactRepository.kt")
            kotlin.exclude("org/crimsoncode2026/data/UserRepository.kt")
            // shared/compose source root files to defer until map stack is stable
            kotlin.exclude("CategoryChip.kt")
            kotlin.exclude("CategoryColor.kt")
            kotlin.exclude("CategoryIcon.kt")
            kotlin.exclude("CategorySelectionGrid.kt")
            kotlin.exclude("ContactListItem.kt")
            kotlin.exclude("EventMarker.kt")
            kotlin.exclude("EventMarkers.kt")
            kotlin.exclude("FilterChipGroup.kt")
            kotlin.exclude("FilterState.kt")
            kotlin.exclude("ManualContactEntryDialog.kt")
            kotlin.exclude("MapControls.kt")
            kotlin.exclude("NetworkStatusIndicator.kt")
            kotlin.exclude("OptimizedEventListView.kt")
            kotlin.exclude("OptimizedEventMarkers.kt")
            kotlin.exclude("SeverityBadge.kt")
            kotlin.exclude("SeverityColor.kt")
            kotlin.exclude("UserLocationMarker.kt")
            // shared/domain source root excludes (root-relative paths)
            kotlin.exclude("usecases/**")
            kotlin.exclude("UserSessionManager.kt")
        }
        androidMain {
            kotlin.srcDir("shared/src/androidMain/kotlin")
            kotlin.exclude("org/crimsoncode2026/auth/**")
            kotlin.exclude("org/crimsoncode2026/network/**")
            kotlin.exclude("org/crimsoncode2026/notifications/**")
            kotlin.exclude("org/crimsoncode2026/storage/**")
        }
        iosMain {
            kotlin.srcDir("shared/src/iosMain/kotlin")
            kotlin.exclude("org/crimsoncode2026/auth/**")
            kotlin.exclude("org/crimsoncode2026/network/**")
            kotlin.exclude("org/crimsoncode2026/notifications/**")
            kotlin.exclude("org/crimsoncode2026/storage/**")
        }
        commonTest {
            kotlin.srcDir("shared/src/commonTest/kotlin")
        }
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.play.services.location)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.material.icons.core)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.maplibre.compose)

            implementation(libs.moko.geo)
            implementation(libs.moko.geo.compose)
            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.location)

            implementation(libs.moko.biometry)
            implementation(libs.moko.biometry.compose)

            implementation(libs.ksafe)
            implementation(libs.ksafe.compose)

            implementation(libs.kontacts)

            implementation(libs.supabase.auth.kt)
            implementation(libs.supabase.postgrest.kt)
            implementation(libs.supabase.realtime.kt)

            implementation(libs.kmpnotifier)
        }
    }
}

android {
    namespace = "org.crimsoncode2026"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    sourceSets["main"].apply {
        manifest.srcFile("androidApp/AndroidManifest.xml")
        java.srcDirs("androidApp/kotlin")
        res.srcDirs("androidApp/res")
    }
    sourceSets["androidTest"].apply {
        java.srcDirs("androidApp/src/androidTest/kotlin")
    }

    defaultConfig {
        applicationId = "org.crimsoncode2026"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Supabase configuration from local.properties or environment variables
        val supabaseUrl = System.getenv("SUPABASE_URL")
            ?: localProperties["supabase.url"] as String?
            ?: "https://your-project-ref.supabase.co"
        val supabaseAnonKey = System.getenv("SUPABASE_ANON_KEY")
            ?: localProperties["supabase.anon.key"] as String?
            ?: "your-anon-key-here"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)
}
