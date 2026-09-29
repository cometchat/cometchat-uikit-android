# Consumer ProGuard rules for chatuikit-core
# Add any project-specific keep rules here that consumers of this library need.

# ENG-38653 (G3): strip gated debug logging from minified consumer builds.
# CometChatLogger.d/i/v calls (and their message-string arguments) are removed
# by R8 when the consuming app builds with minification - debug chatter never
# ships in release APKs. w/e are deliberately KEPT so warnings/errors remain
# available in production via CometChatLogger.enableLog(true).
# Covers both the @JvmStatic bridges and the Kotlin object's instance methods.
-assumenosideeffects class com.cometchat.uikit.core.utils.CometChatLogger {
    public *** d(...);
    public *** i(...);
    public *** v(...);
}

# ENG-38654 (R1): keep rules for this library's reflective surfaces.
# Precise on purpose - no blanket keeps. Each rule names the reflective
# consumer it serves; delete the rule when that code goes away.

# Annotation-driven reflection (Gson @SerializedName etc.) needs these intact.
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

# ViewModels created reflectively without a factory, e.g.
# ViewModelProvider(owner)[CometChatOutgoingCallViewModel::class.java] in
# CometChatOutgoingCall / CometChatOngoingCall: R8 must not strip the no-arg
# constructor that the default factory instantiates.
-keepclassmembers class com.cometchat.uikit.** extends androidx.lifecycle.ViewModel {
    <init>();
}

# CallsUtils.isCallsSDKAvailable() probes the OPTIONAL Calls SDK via
# Class.forName("com.cometchat.calls.core.CometChatCalls"). keepnames preserves
# the class name when the Calls SDK is on the consumer's classpath (a rename
# would silently disable calling); R8 may still remove it when truly unused.
# The Calls SDK is compileOnly here, so consumers that opt out of calling need
# the dontwarn for the unresolved references.
-keepnames class com.cometchat.calls.core.CometChatCalls
-dontwarn com.cometchat.calls.**
