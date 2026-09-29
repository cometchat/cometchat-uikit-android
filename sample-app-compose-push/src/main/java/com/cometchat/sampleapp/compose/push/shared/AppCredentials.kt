package com.cometchat.sampleapp.compose.push.shared

/**
 * Non-secret defaults for CometChat SDK initialization, used when nothing is stored in
 * SharedPreferences yet.
 *
 * [APP_ID] and [AUTH_KEY] are deliberately empty (ENG-38650): a working App ID and Auth Key must
 * never be committed. Every caller already treats blank credentials as "not configured" and routes
 * to the in-app credentials screen, which is where they are entered and persisted — the same flow
 * the other sample apps use. Do not put real values back here; enter them in the app instead.
 *
 * [REGION] and [PROVIDER_ID] are not credentials: a datacentre code and a push-provider name. They
 * stay as sensible defaults so the credentials screen starts pre-filled.
 */
object AppCredentials {
    const val APP_ID: String = ""
    const val AUTH_KEY: String = ""
    const val REGION: String = "XXXXXXXXX"
    const val PROVIDER_ID: String = "XXXXXXXXX"
}
