import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            kotlin.exclude("**/*Test.kt")
            kotlin.exclude("**/OptimizedRealtimeService.kt")
            // shared/kotlin source root excludes (package-style paths)
            kotlin.exclude("org/crimsoncode2026/auth/**")
            kotlin.exclude("org/crimsoncode2026/contacts/**")
            kotlin.exclude("org/crimsoncode2026/data/**")
            kotlin.exclude("org/crimsoncode2026/di/**")
            kotlin.exclude("org/crimsoncode2026/domain/**")
            kotlin.exclude("org/crimsoncode2026/location/**")
            kotlin.exclude("org/crimsoncode2026/network/**")
            kotlin.exclude("org/crimsoncode2026/notifications/**")
            kotlin.exclude("org/crimsoncode2026/screens/**")
            kotlin.exclude("org/crimsoncode2026/storage/**")
            kotlin.exclude("org/crimsoncode2026/compose/**")
            kotlin.exclude("org/crimsoncode2026/utils/**")
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
