plugins {
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false

    // Kotlin static analysis (detekt) and formatting (ktlint), added 2026-08-25 per
    // docs/TOOLING.md item 3. Applied in app/build.gradle.kts, not here.
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

