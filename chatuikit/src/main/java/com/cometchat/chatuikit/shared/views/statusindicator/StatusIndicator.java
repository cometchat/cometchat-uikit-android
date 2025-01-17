package com.cometchat.chatuikit.shared.views.statusindicator;

/**
 * Enum representing various status indicators.
 */
public enum StatusIndicator {

    /**
     * Represents the online status of a user or entity.
     */
    ONLINE,

    /**
     * Represents the offline status of a user or entity.
     */
    OFFLINE,

    /**
     * Represents a public group status, indicating that the group is open to all
     * users.
     */
    PUBLIC_GROUP,

    /**
     * Represents a private group status, indicating that the group is restricted
     * and requires an invitation to join.
     */
    PRIVATE_GROUP,

    /**
     * Represents a protected group status, indicating that the group is accessible
     * with certain restrictions or permissions.
     */
    PROTECTED_GROUP,
}
