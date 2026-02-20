// Pure Kotlin/JVM module — zero Android dependencies.
// Designed for easy migration to Kotlin Multiplatform (KMP):
// simply change the plugin to `kotlin("multiplatform")` and add targets.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Align JVM target with Android modules
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Coroutines — needed for Flow in repository interfaces
    implementation(libs.kotlinx.coroutines.core)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
