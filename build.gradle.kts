// AGP 9 compiles Kotlin by itself (built-in Kotlin). Declaring the Compose
// compiler plugin here also pins the Kotlin version used by the build.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
