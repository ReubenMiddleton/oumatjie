plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
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
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
