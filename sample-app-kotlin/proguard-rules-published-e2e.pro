# ENG-38671 (E2/E3) — keeps required ONLY to run the instrumented E2E suite against the
# MINIFIED `published` variant of this sample app.
#
# Why this is needed: the test APK is NOT minified, but it runs against the app's MINIFIED
# classpath. Any class the tests/helpers reference by its original name must therefore survive
# R8 in the app. These are TEST-HARNESS keeps for the sample app — NOT customer requirements.
#
# Why this does NOT weaken the R1 canary: it only keeps Kotlin stdlib and this sample app's own
# classes. The UIKit itself (com.cometchat.uikit.*) is left to R8 + the AAR's shipped
# consumer-rules.pro (Track-1 / R1), so a genuine missing consumer keep still surfaces here as a
# com.cometchat.uikit.* ClassNotFound/NoSuchMethod when the suite drives that surface.

# Kotlin stdlib referenced by the E2E helpers (e.g. `by lazy { }` -> kotlin.LazyKt).
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# The sample app's own classes the E2E helpers call directly (SampleApplication.onSDKInitialized,
# AppPreferences.saveCredentials, etc.) — the test APK references these by their original names.
-keep class com.cometchat.sampleapp.kotlin.** { *; }
