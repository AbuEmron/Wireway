import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Enable Firebase Cloud Messaging ONLY when a real google-services.json is present.
// The google-services plugin fails the build if the file is missing, so we apply
// it conditionally — the app compiles and runs fine without push configured.
if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// ── Supabase credentials ──────────────────────────────────────────────────────
// Read the SAME public values the web app uses (REACT_APP_SUPABASE_URL /
// REACT_APP_SUPABASE_ANON_KEY) from local.properties so they never live in
// source control. local.properties is git-ignored by default in Android projects.
// See local.properties.example and the README for setup.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String, default: String = ""): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: default)

android {
    namespace = "com.wirewaypro.app"
    compileSdk = 35

    defaultConfig {
        // `.native` so this dev build installs ALONGSIDE the Capacitor app
        // (com.wirewaypro.app) instead of replacing it.
        applicationId = "com.wirewaypro.app.native"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Surfaced to the app as BuildConfig.SUPABASE_URL / SUPABASE_ANON_KEY.
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${secret("REACT_APP_SUPABASE_URL")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${secret("REACT_APP_SUPABASE_ANON_KEY")}\""
        )
    }

    signingConfigs {
        // Stable, committed debug key so every debug APK (local + CI) shares one
        // signature — builds install over the top instead of conflicting. This is
        // the standard Android debug key (alias androiddebugkey, store/key pass
        // "android"); it is NOT a release/upload key and carries no secrecy value.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            // Distinct id during dev so debug + release can also coexist.
            applicationIdSuffix = ".dev"
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO(phase: release): wire upload-key signing like the Capacitor app
            // (keystore.properties) before shipping to Play. Debug-signed for now.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX foundation
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose (BOM keeps all Compose artifacts on one compatible version)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Async + serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Supabase (BOM pins auth-kt / postgrest-kt versions) + Ktor engine
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)

    // Offline queue
    implementation(libs.androidx.datastore.preferences)

    // Home-screen widget (Glance)
    implementation(libs.androidx.glance.appwidget)

    // WorkManager (local reminders + offline sync) with Hilt worker injection
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Biometric unlock
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)

    // Location (FusedLocationProvider) — AI pricing + pull-list locality
    implementation(libs.play.services.location)

    // Chrome Custom Tabs — Stripe Connect onboarding / hosted pay pages
    implementation(libs.androidx.browser)

    // Plaid Link (bank connections)
    implementation(libs.plaid.link)

    // Firebase Cloud Messaging (push). Compiles without google-services.json;
    // runtime calls are guarded so the app is safe until push is configured.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Play Billing (subscriptions)
    implementation(libs.billing.ktx)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
