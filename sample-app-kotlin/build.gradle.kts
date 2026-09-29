plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.cometchat.sampleapp.kotlin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cometchat.sampleapp.kotlin"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        // ENG-38657 (B3): track the library release instead of drifting
        versionName = System.getenv("LIBRARY_VERSION") ?: "6.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // ── ENG-38671 (E1/E2): consume the PUBLISHED UIKit AAR (Cloudsmith), minified with R8 ──
        // This is the artifact customers actually ship. R8 runs using the AAR's bundled
        // consumer-rules.pro (Track-1 / R1) — the permanent canary: a missing keep rule surfaces
        // here instead of in a customer's minified release. Debuggable + debug-signed so the E2E
        // suite can install & drive it. (E3 will point testBuildType / the release gate at this
        // variant; left untouched here so existing `connectedDebugAndroidTest` keeps working.)
        create("published") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            // Test-harness keeps so the instrumented E2E suite can run against the minified app.
            proguardFiles("proguard-rules-published-e2e.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    
    // Enable JUnit 5 for Kotest property-based testing
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    // ── ENG-38671: which variant the instrumented E2E suite runs against ──
    // Default `debug` (local source, non-minified) keeps `connectedDebugAndroidTest` and existing
    // CI unchanged. `run_e2e_tests.sh` opts into the published R8-minified canary with -Pe2ePublished,
    // which makes `connectedPublishedAndroidTest` the instrumented task (Cloudsmith AAR + R8 +
    // the AAR's consumer-rules.pro). Instrumented tests attach to exactly one build type, so this
    // is a per-run switch, not both at once.
    testBuildType = if (project.hasProperty("e2ePublished")) "published" else "debug"
}

dependencies {
    // CometChat UIKit — source depends on the build variant (ENG-38671 E1):
    //  • debug / release  → local project(...) source (day-to-day dev + existing CI)
    //  • published        → the released com.cometchat:chatuikit-*-android AAR from Cloudsmith
    //                       (what customers consume; minified in the `published` build type)
    "debugImplementation"(project(":chatuikit-kotlin"))
    "debugImplementation"(libs.chatuikit.core.android)
    "releaseImplementation"(project(":chatuikit-kotlin"))
    "releaseImplementation"(libs.chatuikit.core.android)
    // The published AAR drags legacy org.jetbrains:annotations-java5, which duplicates the modern
    // org.jetbrains:annotations already on the classpath — drop the legacy one to avoid a
    // duplicate-class packaging failure on the published variant.
    "publishedImplementation"(libs.chatuikit.kotlin.android) {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    "publishedImplementation"(libs.chatuikit.core.android) {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }

    // CometChat Chat SDK
    implementation(libs.chat.sdk.android)
    
    // CometChat Calls SDK for Calls UI features (call buttons, call logs)
    implementation(libs.calls.sdk.android)
    
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Navigation components
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    
    // Gson for JSON serialization
    implementation(libs.gson)
    
    // OkHttp for network calls
    implementation(libs.okhttp)
    
    // Glide for image loading
    implementation(libs.glide)
    
    // Unit testing
    testImplementation(libs.junit)
    
    // Mockito for mocking in unit tests
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    
    // Robolectric for Android framework classes in unit tests
    testImplementation(libs.robolectric)
    
    // Kotest for property-based testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    
    // JUnit Vintage engine to run JUnit 4 tests alongside JUnit 5
    testRuntimeOnly(libs.junit.vintage.engine)
    
    // Android instrumentation tests (E2E)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.uiautomator)
}
