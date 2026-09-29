package com.cometchat.uikit.core

/**
 * Configuration settings for the CometChat UIKit.
 * 
 * Use [UIKitSettingsBuilder] to create instances of this class.
 *
 * @property appId The CometChat App ID
 * @property region The region where the app is hosted
 * @property subscriptionType The subscription type for presence updates
 * @property autoEstablishSocketConnection Whether to automatically establish socket connection
 * @property authKey The authentication key for the app
 * @property roles List of roles for presence subscription
 * @property overrideAdminHost Custom admin host URL (optional)
 * @property overrideClientHost Custom client host URL (optional)
 * @property enableCalling Whether calling features are enabled (default false)
 * @property callSettingsBuilder Custom SessionSettingsBuilder for call configuration (CometChatCalls.SessionSettingsBuilder)
 */
public class UIKitSettings private constructor(
    public val appId: String?,
    public val region: String?,
    public val subscriptionType: String,
    public val autoEstablishSocketConnection: Boolean,
    public val authKey: String?,
    public val roles: List<String>?,
    public val overrideAdminHost: String?,
    public val overrideClientHost: String?,
    public val enableCalling: Boolean,
    public val callSettingsBuilder: Any?,
    public val enableThreadSubscription: Boolean
) {
    public companion object {
        private const val TAG = "UIKitSettings"
    }

    /**
     * Builder class for constructing [UIKitSettings] instances.
     */
    public class UIKitSettingsBuilder {
        private var appId: String? = null
        private var region: String? = null
        private var subscriptionType: String = "NONE"
        private var autoEstablishSocketConnection: Boolean = true
        private var authKey: String? = null
        private var roles: List<String>? = null
        private var overrideAdminHost: String? = null
        private var overrideClientHost: String? = null
        private var enableCalling: Boolean = false
        private var callSettingsBuilder: Any? = null
        private var enableThreadSubscription: Boolean = true

        /**
         * Sets the CometChat App ID.
         *
         * @param appId The app ID
         * @return This builder instance
         */
        public fun setAppId(appId: String): UIKitSettingsBuilder {
            this.appId = appId
            return this
        }

        /**
         * Sets the region where the app is hosted.
         *
         * @param region The region (e.g., "us", "eu")
         * @return This builder instance
         */
        public fun setRegion(region: String): UIKitSettingsBuilder {
            this.region = region
            return this
        }

        /**
         * Sets the subscription type to subscribe to presence updates for all users.
         *
         * @return This builder instance
         */
        public fun subscribePresenceForAllUsers(): UIKitSettingsBuilder {
            this.subscriptionType = "ALL_USERS"
            return this
        }

        /**
         * Sets the subscription type to subscribe to presence updates for the specified roles.
         *
         * @param roles The list of roles to subscribe to
         * @return This builder instance
         */
        public fun subscribePresenceForRoles(roles: List<String>): UIKitSettingsBuilder {
            this.subscriptionType = "ROLES"
            this.roles = roles
            return this
        }

        /**
         * Sets the subscription type to subscribe to presence updates for friends.
         *
         * @return This builder instance
         */
        public fun subscribePresenceForFriends(): UIKitSettingsBuilder {
            this.subscriptionType = "FRIENDS"
            return this
        }

        /**
         * Sets the list of roles associated with the app.
         *
         * @param roles The list of roles
         * @return This builder instance
         */
        public fun setRoles(roles: List<String>): UIKitSettingsBuilder {
            this.roles = roles
            return this
        }

        /**
         * Sets whether to automatically establish a socket connection.
         *
         * @param autoEstablishSocketConnection Flag indicating whether to auto-connect
         * @return This builder instance
         */
        public fun setAutoEstablishSocketConnection(autoEstablishSocketConnection: Boolean): UIKitSettingsBuilder {
            this.autoEstablishSocketConnection = autoEstablishSocketConnection
            return this
        }

        /**
         * Sets the authentication key for the app.
         *
         * @param authKey The authentication key
         * @return This builder instance
         */
        public fun setAuthKey(authKey: String): UIKitSettingsBuilder {
            this.authKey = authKey
            return this
        }

        /**
         * Sets a custom admin host URL.
         *
         * @param adminHost The admin host URL
         * @return This builder instance
         */
        public fun overrideAdminHost(adminHost: String): UIKitSettingsBuilder {
            this.overrideAdminHost = adminHost
            return this
        }

        /**
         * Sets a custom client host URL.
         *
         * @param clientHost The client host URL
         * @return This builder instance
         */
        public fun overrideClientHost(clientHost: String): UIKitSettingsBuilder {
            this.overrideClientHost = clientHost
            return this
        }

        /**
         * Sets whether calling features are enabled.
         * When enabled, the CometChatCalls SDK will be initialized during UIKit initialization.
         *
         * @param enable True to enable calling features, false to disable (default is false)
         * @return This builder instance
         */
        public fun setEnableCalling(enable: Boolean): UIKitSettingsBuilder {
            this.enableCalling = enable
            return this
        }

        /**
         * Sets a custom SessionSettingsBuilder for call configuration.
         * This allows customization of session settings such as audio/video modes, default layouts, etc.
         *
         * @param builder The CometChatCalls.SessionSettingsBuilder instance
         * @return This builder instance
         */
        public fun setCallSettingsBuilder(builder: Any): UIKitSettingsBuilder {
            this.callSettingsBuilder = builder
            return this
        }

        /**
         * Sets whether the thread-subscription (follow/unfollow) feature is enabled.
         * **On by default**, matching the React and React Native UI Kits — integrators opt
         * OUT. When off, neither the thread-header control nor the message action-sheet
         * option renders, and no subscription request is ever made.
         *
         * @param enable True to enable thread subscription surfaces, false to disable (default is true)
         * @return This builder instance
         */
        @Deprecated(
            "Thread subscription is on by default now, so passing true is a no-op. Kept only as a " +
                "global opt-out; prefer hiding the surfaces per screen with " +
                "setThreadSubscriptionOptionVisibility / setThreadSubscriptionVisibility (View) or " +
                "hideThreadSubscriptionOption / hideThreadSubscription (Compose)."
        )
        public fun setEnableThreadSubscription(enable: Boolean): UIKitSettingsBuilder {
            this.enableThreadSubscription = enable
            return this
        }

        /**
         * Builds a new instance of [UIKitSettings] using the provided configuration.
         *
         * @return A new [UIKitSettings] instance
         */
        public fun build(): UIKitSettings {
            return UIKitSettings(
                appId = appId,
                region = region,
                subscriptionType = subscriptionType,
                autoEstablishSocketConnection = autoEstablishSocketConnection,
                authKey = authKey,
                roles = roles,
                overrideAdminHost = overrideAdminHost,
                overrideClientHost = overrideClientHost,
                enableCalling = enableCalling,
                callSettingsBuilder = callSettingsBuilder,
                enableThreadSubscription = enableThreadSubscription
            )
        }
    }
}
