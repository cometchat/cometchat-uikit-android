package com.cometchat.sampleapp.compose.push.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry

/**
 * True while this entry is the one the user is actually interacting with.
 *
 * A double-tap on a list item fires two clicks before the opened screen is on top, so without
 * this check both clicks navigate and the same chat is pushed twice (ENG-29984). The first
 * `navigate()` takes this entry off RESUMED, so the second click finds it settled and does
 * nothing — no timing window, and no change to how any single navigation behaves.
 *
 * Guard the navigation on the entry that *issued* it, not on `currentBackStackEntry`, which has
 * already moved on by the time the duplicate arrives.
 */
fun NavBackStackEntry.lifecycleIsResumed(): Boolean =
    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
