package com.cometchat.uikit.kotlin.presentation.shared.permission.listener

/**
 * Listener interface for permission request results.
 */
public interface PermissionResultListener {
    /**
     * Called when permission request completes.
     * @param grantedPermissions List of permissions that were granted
     * @param deniedPermissions List of permissions that were denied
     */
    public fun permissionResult(grantedPermissions: List<String>, deniedPermissions: List<String>)
}

/**
 * Base implementation of PermissionResultListener that does nothing.
 * Used as a default/empty listener.
 */
open public class BasePermissionResultListener : PermissionResultListener {
    override fun permissionResult(grantedPermissions: List<String>, deniedPermissions: List<String>) {
        // Default empty implementation
    }
}
