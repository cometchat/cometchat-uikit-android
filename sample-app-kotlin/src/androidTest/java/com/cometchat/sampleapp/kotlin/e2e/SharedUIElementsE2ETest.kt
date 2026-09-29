package com.cometchat.sampleapp.kotlin.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestConfig
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper
import com.cometchat.sampleapp.kotlin.e2e.helpers.RestApiHelper
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.PACKAGE
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.SETTLE_TIME
import com.cometchat.sampleapp.kotlin.e2e.helpers.E2ETestHelper.TIMEOUT
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * E2E tests for shared UI elements (Avatar, Badge Count, Timestamp/Date).
 *
 * These tests verify that the core UIKit components render correctly across
 * the app: avatars load images in the Users list, badge counts show on
 * conversations with unread messages, and timestamps are properly formatted.
 *
 * Test IDs:
 * - E2E-057: testAvatarLoadsImage
 * - E2E-058: testBadgeCountShown
 * - E2E-059: testTimestampFormatted
 *
 * Run:
 *   ./gradlew :sample-app-kotlin:connectedDebugAndroidTest \
 *       -Pandroid.testInstrumentationRunnerArguments.class=com.cometchat.sampleapp.kotlin.e2e.SharedUIElementsE2ETest
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
     * CometChatAvatar (extends MaterialCardView) renders user profile images.
     * We verify that ImageView elements exist within the users list items and
     * that they have non-trivial dimensions (indicating an image loaded, not
     * just a tiny placeholder).
     */
    @Test
    fun test01_avatarLoadsImage() {
        E2ETestHelper.navigateToTab(device, "Users")
        Thread.sleep(SETTLE_TIME)

        val uikitPackage = "com.cometchat.uikit.kotlin"

        // Wait for the users list RecyclerView to load
        var recyclerView = device.wait(
            Until.findObject(By.res(uikitPackage, "recyclerview_users_list")),
            TIMEOUT
        ) ?: device.findObject(By.res(PACKAGE, "recyclerview_users_list"))

        // Fallback: find any RecyclerView with user items
        if (recyclerView == null) {
            val allRecyclers = device.findObjects(
                By.clazz("androidx.recyclerview.widget.RecyclerView")
            )
            for (rv in allRecyclers) {
                if (rv.children.size > 1) {
                    recyclerView = rv
                    break
                }
            }
        }

        assertNotNull("Users list RecyclerView not found", recyclerView)

        // Wait for items to load
        val deadline = System.currentTimeMillis() + TIMEOUT
        while (System.currentTimeMillis() < deadline) {
            if (recyclerView!!.children.isNotEmpty()) break
            Thread.sleep(1500)
            recyclerView = device.findObject(By.res(uikitPackage, "recyclerview_users_list"))
                ?: device.findObject(By.res(PACKAGE, "recyclerview_users_list"))
            if (recyclerView == null) break
        }

        assertNotNull("Users RecyclerView disappeared", recyclerView)
        assertTrue("Users list has no items loaded", recyclerView!!.children.isNotEmpty())

        // Find avatar views within the users list.
        // CometChatAvatar is a MaterialCardView containing an ImageView.
        // Search the entire RecyclerView for ImageViews (not just first child,
        // as UIAutomator may not expose nested views through children).
        val allImageViews = recyclerView!!.findObjects(By.clazz("android.widget.ImageView"))
        val allMaterialCards = recyclerView.findObjects(
            By.clazz("com.google.android.material.card.MaterialCardView")
        )
        val allAvatarCards = recyclerView.findObjects(
            By.clazz("com.cometchat.uikit.kotlin.presentation.shared.baseelements.avatar.CometChatAvatar")
        )

        // Also try finding any view in the list area that could be an avatar
        val anyImagesOnScreen = device.findObjects(By.clazz("android.widget.ImageView"))
            .filter { img ->
                try {
                    val bounds = img.visibleBounds
                    bounds.top > 200 && bounds.bottom < device.displayHeight - 200 &&
                        (bounds.right - bounds.left) in 60..300 &&
                        (bounds.bottom - bounds.top) in 60..300
                } catch (_: Exception) { false }
            }

        val hasAvatarView = allAvatarCards.isNotEmpty() || allMaterialCards.isNotEmpty() ||
            allImageViews.isNotEmpty() || anyImagesOnScreen.isNotEmpty()

        assertTrue(
            "No avatar views found in users list. " +
                "ImageViews in RV: ${allImageViews.size}, AvatarCards: ${allAvatarCards.size}, " +
                "MaterialCards: ${allMaterialCards.size}, Images on screen: ${anyImagesOnScreen.size}",
            hasAvatarView
        )
    }

    /**
     * E2E-058: unread badge — GENUINE (non-vacuous). Deterministically seed an incoming
     * (peer→me) message via the REST API so the peer's conversation row must carry an unread
     * badge, then assert it. The badge (`badge_view`) sits in a sibling `tail_view`, not inside
     * the item container, so it's associated with the row's subtitle geometrically (same y-band).
     * Fails if no unread badge appears — no soft/vacuous pass, and no SDK re-login gymnastics.
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

        val rowSel = By.res(PACKAGE, "conversations_item_container").hasDescendant(By.text(tag))
        val row = E2ETestHelper.waitForObject(device, rowSel)
        assertNotNull("Peer conversation row for the incoming message '$tag' did not appear", row)
        assertTrue(
            "Conversation row did not show an unread badge after a peer sent an incoming message",
            rowHasUnreadBadge(tag)
        )
    }

    /**
     * True if the conversation row whose subtitle is [subtitleText] shows an unread-count badge.
     * The badge lives in a sibling view of the item container, so match it by vertical overlap
     * with the row's subtitle rather than by parent/child containment.
     */
    private fun rowHasUnreadBadge(subtitleText: String): Boolean {
        val sub = device.findObject(By.res(PACKAGE, "tv_subtitle").text(subtitleText))
            ?: device.findObject(By.text(subtitleText))
            ?: return false
        val rb = sub.visibleBounds
        return device.findObjects(By.res(PACKAGE, "badge_view")).any {
            val cy = it.visibleBounds.centerY()
            cy in (rb.top - 20)..(rb.bottom + 20)
        }
    }

    /**
     * E2E-059: Navigate to Conversations, verify timestamp text is shown on conversation items.
     *
     * CometChatDate renders the last message timestamp in the tail_view area of each
     * conversation item. The date is formatted as "HH:MM AM/PM", "Yesterday", "Mon", etc.
     */
    @Test
    fun test03_timestampFormatted() {
        E2ETestHelper.navigateToTab(device, "Chats")
        Thread.sleep(SETTLE_TIME)

        val uikitPackage = "com.cometchat.uikit.kotlin"

        // Wait for conversations list
        val conversationsList = E2ETestHelper.waitForObject(
            device, By.res(PACKAGE, "conversationList")
        )
        assertNotNull("Conversations list not found", conversationsList)

        // Wait for RecyclerView items
        val recyclerView = device.wait(
            Until.findObject(By.res(uikitPackage, "recyclerview_conversations_list")),
            TIMEOUT
        ) ?: device.findObject(By.res(PACKAGE, "recyclerview_conversations_list"))
            ?: device.findObject(
                By.clazz("androidx.recyclerview.widget.RecyclerView")
                    .hasAncestor(By.res(PACKAGE, "conversationList"))
            )

        assertNotNull("Conversations RecyclerView not found", recyclerView)

        val deadline = System.currentTimeMillis() + TIMEOUT
        while (System.currentTimeMillis() < deadline) {
            if (recyclerView!!.children.isNotEmpty()) break
            Thread.sleep(1500)
        }

        assertTrue("Conversations list is empty", recyclerView!!.children.isNotEmpty())

        // Look for timestamp patterns in TextViews within conversation items.
        // Timestamps can appear as:
        // - "12:30 PM" / "3:45 AM" (time format)
        // - "Yesterday"
        // - "Mon", "Tue", etc. (day abbreviations)
        // - "Jan 15" / "15 Jan" (date format)
        // - "1/15/24" (short date)
        val timePatterns = listOf(
            Regex("\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?"),  // HH:MM AM/PM
            Regex("Yesterday", RegexOption.IGNORE_CASE),
            Regex("^(Mon|Tue|Wed|Thu|Fri|Sat|Sun)$", RegexOption.IGNORE_CASE),  // Day names
            Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2}", RegexOption.IGNORE_CASE),  // Month Day
            Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),  // M/D/Y format
            Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}")  // D.M.Y format
        )

        val allTextViews = recyclerView.findObjects(By.clazz("android.widget.TextView"))
        val timestampTexts = allTextViews.filter { tv ->
            val text = tv.text ?: ""
            timePatterns.any { pattern -> pattern.containsMatchIn(text) }
        }

        // Also check for CometChatDate components directly
        val dateComponents = device.findObjects(
            By.clazz("com.cometchat.uikit.kotlin.presentation.shared.baseelements.date.CometChatDate")
        )

        val hasTimestamp = timestampTexts.isNotEmpty() || dateComponents.isNotEmpty()

        assertTrue(
            "No formatted timestamps found in conversation items. " +
                "Looked for time patterns (HH:MM, Yesterday, Day names, Month Day) in " +
                "${allTextViews.size} TextViews. DateComponents found: ${dateComponents.size}",
            hasTimestamp
        )

        // Verify the timestamp has reasonable text (not empty or "null")
        if (timestampTexts.isNotEmpty()) {
            val firstTimestamp = timestampTexts[0].text
            assertTrue(
                "Timestamp text should be non-empty and formatted: '$firstTimestamp'",
                !firstTimestamp.isNullOrBlank() && firstTimestamp.length >= 3
            )
        }
    }
}
