package com.cometchat.sampleapp.compose.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper.SETTLE_TIME
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestHelper.TIMEOUT
import com.cometchat.sampleapp.compose.e2e.helpers.E2ETestConfig
import com.cometchat.sampleapp.compose.e2e.helpers.RestApiHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for shared UI elements (Avatar, Badge Count, Timestamp/Date)
 * in the Compose sample app.
 *
 * Ported from sample-app-kotlin SharedUIElementsE2ETest, adapted for Compose:
 * - No RecyclerView or resource IDs — lists are Compose LazyColumns (scrollable in a11y tree)
 * - Avatars render as generic Image views (android.widget.ImageView or android.view.View)
 * - Badge counts appear as Text nodes with numeric content
 * - Timestamps appear as Text nodes matching time/date patterns
 * - No CometChatAvatar/CometChatBadgeCount/CometChatDate class selectors
 *
 * Test IDs:
 * - E2E-057: testAvatarLoadsImage
 * - E2E-058: testBadgeCountShown
 * - E2E-059: testTimestampFormatted
 *
 * Run:
 *   ./gradlew :sample-app-compose:connectedDebugAndroidTest \
 *       -Pandroid.testInstrumentationRunnerArguments.class=com.cometchat.sampleapp.compose.e2e.SharedUIElementsE2ETest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SharedUIElementsE2ETest {

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        E2ETestHelper.fullSetupAndLogin(device)
    }

    /**
     * E2E-057: Navigate to Users list, verify avatars are displayed.
     *
     * In Compose, CometChatAvatar renders as a generic Image composable which appears
     * as an ImageView or View node in the accessibility tree. We verify that image-sized
     * views exist within the users list items and have non-trivial dimensions
     * (indicating an image loaded, not just a tiny placeholder).
     */
    @Test
    fun test01_avatarLoadsImage() {
        // Navigate to Users tab
        E2ETestHelper.navigateToTab(device, "Users")
        Thread.sleep(SETTLE_TIME)

        // Wait for users list to load
        device.waitForIdle()
        Thread.sleep(SETTLE_TIME)

        // In Compose, avatars render as ImageView or generic View with image content.
        // Use safeGetBounds to avoid StaleObjectException.
        val avatarImageBounds = E2ETestHelper.safeGetBounds(
            device, By.clazz("android.widget.ImageView")
        ) { bounds ->
            val width = bounds.right - bounds.left
            val height = bounds.bottom - bounds.top
            width in 60..300 && height in 60..300 &&
                bounds.top > 100 && bounds.bottom < device.displayHeight - 150
        }

        // Also check for content descriptions containing "avatar"
        val descAvatarBounds = E2ETestHelper.safeGetBounds(device, By.descContains("avatar")) +
            E2ETestHelper.safeGetBounds(device, By.descContains("Avatar"))

        // Also check for generic View nodes that are avatar-sized (Compose Image composable)
        val viewBounds = E2ETestHelper.safeGetBounds(
            device, By.clazz("android.view.View")
        ) { bounds ->
            val width = bounds.right - bounds.left
            val height = bounds.bottom - bounds.top
            width in 60..300 && height in 60..300 &&
                Math.abs(width - height) < 30 &&
                bounds.top > 100 && bounds.bottom < device.displayHeight - 150
        }

        val hasAvatarView = avatarImageBounds.isNotEmpty() ||
            descAvatarBounds.isNotEmpty() ||
            viewBounds.isNotEmpty()

        assertTrue(
            "No avatar views found in user list items. " +
                "ImageViews: ${avatarImageBounds.size}, descAvatars: ${descAvatarBounds.size}, " +
                "genericViews: ${viewBounds.size}",
            hasAvatarView
        )
    }

    /**
     * E2E-058: Navigate to Conversations, verify unread badge count is visible.
     *
     * In Compose, the badge count renders as a Text composable with numeric content
     * within the conversation list items. If any conversation has unread messages,
     * a badge with a count (e.g., "1", "5", "99+") should be visible.
     *
     * Note: This test may pass vacuously if no conversations have unread messages.
     */
    /**
     * E2E-058: unread badge — GENUINE (non-vacuous). Deterministically seed an incoming
     * (peer→me) message via REST so the peer's conversation row must carry an unread badge,
     * then assert it. The row's content-description reads e.g. "dove, Badge123, 2 unread messages".
     * Fails if no unread badge appears — no soft/vacuous pass.
     */
    @Test
    fun test02_badgeCountShown() {
        E2ETestHelper.navigateToTab(device, "Chats")

        // Unique probe (avoid the substring "unread" so it can't be confused with the badge text).
        val tag = "Badge" + (System.currentTimeMillis() % 100000)
        RestApiHelper.sendMessage(
            sender = E2ETestConfig.ONE_TO_ONE_UID,
            receiver = E2ETestConfig.LOGGED_IN_UID,
            text = tag
        )

        // The peer's row surfaces carrying an unread badge in its content-description.
        val row = E2ETestHelper.waitFor(device, By.descContains(tag))
        assertNotNull("Peer conversation row for the incoming message '$tag' did not appear", row)
        assertTrue(
            "Conversation row did not show an unread badge after a peer sent an incoming message",
            (row!!.contentDescription ?: "").contains("unread", ignoreCase = true)
        )
    }

    /**
     * E2E-059: Navigate to Conversations, verify timestamp text is shown on items.
     *
     * In Compose, CometChatDate renders timestamps as Text composables. The formatted
     * date appears as a TextView in the accessibility tree with patterns like
     * "HH:MM AM/PM", "Yesterday", "Mon", "Jan 15", etc.
     */
    @Test
    fun test03_timestampFormatted() {
        // Navigate to Chats tab
        E2ETestHelper.navigateToTab(device, "Chats")
        Thread.sleep(SETTLE_TIME)
        device.waitForIdle()

        // Wait for conversations list to load
        Thread.sleep(SETTLE_TIME)

        // Define timestamp/date patterns
        val timePatterns = listOf(
            Regex("\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?"),
            Regex("Yesterday", RegexOption.IGNORE_CASE),
            Regex("^(Mon|Tue|Wed|Thu|Fri|Sat|Sun)$", RegexOption.IGNORE_CASE),
            Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2}", RegexOption.IGNORE_CASE),
            Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),
            Regex("\\d{1,2}:\\d{2}")
        )

        // Search TextViews for timestamp text — use try-catch for StaleObjectException
        var hasTimestamp = false
        val allTextViews = device.findObjects(By.clazz("android.widget.TextView"))
        for (tv in allTextViews) {
            try {
                val text = tv.text ?: ""
                val bounds = tv.visibleBounds
                if (bounds.top > 100 && bounds.bottom < device.displayHeight - 150 &&
                    timePatterns.any { it.containsMatchIn(text) }) {
                    hasTimestamp = true
                    break
                }
            } catch (_: androidx.test.uiautomator.StaleObjectException) {
                continue
            }
        }

        // Fallback: check content descriptions
        if (!hasTimestamp) {
            for (obj in device.findObjects(By.descContains(":"))) {
                try {
                    val desc = obj.contentDescription ?: ""
                    if (timePatterns.any { it.containsMatchIn(desc) }) {
                        hasTimestamp = true
                        break
                    }
                } catch (_: androidx.test.uiautomator.StaleObjectException) {
                    continue
                }
            }
        }

        assertTrue(
            "No formatted timestamps found in conversation items",
            hasTimestamp
        )
    }
}
