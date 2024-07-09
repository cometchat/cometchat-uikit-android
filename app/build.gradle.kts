plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.cometchat.kotlinsampleapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cometchat.kotlinsampleapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 4
        versionName = "4.3.13"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        applicationVariants.all {
            this.mergedFlavor.manifestPlaceholders["file_provider"] = "com.cometchat.kotlinsampleapp"
        }
    }

    buildTypes {
        debug {
            isDebuggable = false
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // CometChat
    implementation (libs.chat.uikit.android)
    implementation (libs.calls.sdk.android)


}