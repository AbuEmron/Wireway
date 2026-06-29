// Top-level build file. Plugins are declared here with `apply false` so the
// versions resolve once (from the version catalog) and each module opts in.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // On the classpath but NOT applied here; the app module applies it only when
    // google-services.json is present, so the build stays green without it.
    alias(libs.plugins.google.services) apply false
}
