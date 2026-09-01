plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

// Static analysis and formatting. See docs/TOOLING.md item 3 and docs/DECISIONS.md's
// "detekt and ktlint added" entry for why these are configured non-blocking on existing
// code (baseline) rather than failing CI on a codebase that predates them.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
}

ktlint {
    // Code style is set in .editorconfig (`ktlint_code_style`), not here, so the IDE and the
    // Gradle task can never disagree about it. See .editorconfig for why intellij_idea is used.
    //
    // Baseline: after the .editorconfig style fix, 174 pure-whitespace violations remained
    // (mostly signature/argument wrapping). They are recorded here rather than auto-formatted,
    // so CI fails on NEW issues only — same approach as detekt's baseline above. To actually
    // clean them up, run `./gradlew ktlintFormat` and delete this baseline; that is a deliberate
    // ~60-file reformat and should be its own reviewed commit, not a side effect of adding lint.
    baseline.set(file("$rootDir/config/ktlint/baseline.xml"))
    // Reporters kept to plain text + checkstyle so CI logs stay readable and tools can parse them.
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

android {
    // namespace intentionally stays com.granify.app: it's the Kotlin package used for R-class
    // generation and manifest class resolution, and changing it would require moving every
    // source file's package declaration and directory. applicationId below is the public,
    // Play-Store-facing identity and is independent of namespace — see docs/DECISIONS.md's
    // "Granify -> Oumatjie rename" entry for the full reasoning.
    namespace = "com.granify.app"
    compileSdk = 37
    compileSdkExtension = 19 // required by androidx.pdf:pdf-viewer-fragment's AAR metadata

    defaultConfig {
        applicationId = "com.oumatjie.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests {
            // JVM unit tests run against a stubbed android.jar whose methods throw
            // "Method ... not mocked" by default. util/DebugLog.kt calls android.util.Log on the
            // deliberately-swallowed failure paths, and GmailMailRepositoryTest exercises exactly
            // one of those (loadInbox_dropsAMessageThatFailsToFetchInsteadOfFailingTheWholeInbox),
            // so without this the logging would break a test that is asserting real behaviour.
            // Returning defaults is correct here: logging must never influence control flow, so a
            // no-op Log in unit tests is exactly the desired semantics.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    // Backports the Android 12+ SplashScreen API down to minSdk (28) so cold start looks
    // deliberate everywhere, not just on newer devices. See docs/DECISIONS.md, "Splash screen".
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // Fragment hosting and Material XML theme for the PDF viewer screen, which is a
    // classic View/Fragment activity rather than Compose. AppCompatActivity is required
    // by androidx.pdf's PdfViewerFragment.
    implementation("androidx.fragment:fragment-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")

    // Google's embeddable viewer supports protected PDFs (it shows its own password
    // dialog). It is still an alpha library, so Oumatjie keeps it behind a replaceable
    // document-viewer boundary (pdf/PdfViewerActivity).
    implementation("androidx.pdf:pdf-viewer-fragment:1.0.0-alpha19")

    // Gmail REST API access. OkHttp is pinned to the version retrofit:3.0.0 itself
    // depends on, rather than a newer one, so the pairing is one that's actually tested.
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Google account authorization (Gmail scope grants) without ever handling a password.
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
