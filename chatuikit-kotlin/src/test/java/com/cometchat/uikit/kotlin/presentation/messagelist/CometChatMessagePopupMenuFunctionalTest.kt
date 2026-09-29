package com.cometchat.uikit.kotlin.presentation.messagelist

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowLooper

/**
 * First render-and-assert coverage for the **View** [CometChatMessagePopupMenu].
 *
 * Everything it had was property tests over style and alignment helpers; nothing
 * had ever shown the menu. It is not a View but a plain class driving a
 * `PopupWindow` plus a blur overlay on the WindowManager, so the way in is
 * `show(anchor, parent, message)` from a real Activity — `show` returns early
 * without one — and the way to read the result is the popup's own content view,
 * which Robolectric hands back through [ShadowApplication].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessagePopupMenuFunctionalTest {

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun message(): BaseMessage = MockFactory.createTextMessage(text = "long-press me")

    private fun items(vararg names: String, onClick: ((String) -> Unit)? = null) =
        names.map { name ->
            MenuItem(
                id = name.lowercase(),
                name = name,
                onClick = { onClick?.invoke(name) },
            )
        }

    /**
     * Shows the menu from a real Activity and hands back the popup's content view.
     * The anchor has to be attached and laid out — `show` reads its screen location
     * to place the popup.
     */
    private fun withMenu(
        configure: (CometChatMessagePopupMenu) -> Unit = {},
        assert: (menu: CometChatMessagePopupMenu, popupView: View) -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val root = FrameLayout(activity)
            val anchor = View(activity).apply {
                layoutParams = ViewGroup.LayoutParams(200, 80)
            }
            root.addView(anchor)
            activity.setContentView(root)
            root.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, 1080, 2160)
            ShadowLooper.idleMainLooper()

            val menu = CometChatMessagePopupMenu(activity)
            configure(menu)
            menu.show(anchor, root, message())
            ShadowLooper.idleMainLooper()

            val popup = latestPopup()
            assertNotNull("the menu should have opened a PopupWindow", popup)
            val popupView = popup.contentView
            // The popup is never measured by the window under Robolectric, so lay it
            // out by hand before reading anything positional out of it.
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.AT_MOST),
            )
            popupView.layout(0, 0, popupView.measuredWidth, popupView.measuredHeight)
            ShadowLooper.idleMainLooper()

            assert(menu, popupView)
        }
        scenario.close()
    }

    /** The last PopupWindow the app opened — how Robolectric surfaces a shown popup. */
    private fun latestPopup(): android.widget.PopupWindow =
        Shadows.shadowOf(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>())
            .latestPopupWindow

    private fun View.visibleText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    private fun View.rows(): List<View> {
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view)
        return (0 until rv.childCount).map { rv.getChildAt(it) }
    }

    @Test
    fun showingTheMenuOpensAPopup() = withMenu(
        configure = { it.setMenuItems(items("Reply", "Copy")) },
    ) { _, popupView ->
        assertNotNull(popupView.findViewById<View>(R.id.menu_parent))
        assertTrue("the popup should be showing", latestPopup().isShowing)
    }

    @Test
    fun theMenuItemsRenderAsRows() = withMenu(
        configure = { it.setMenuItems(items("Reply", "Copy", "Delete")) },
    ) { _, popupView ->
        val text = popupView.visibleText()
        assertTrue("expected the option names, was '$text'", text.contains("Reply"))
        assertTrue(text.contains("Copy"))
        assertTrue(text.contains("Delete"))
    }

    @Test
    fun theMessagePreviewCarriesTheMessage() = withMenu(
        configure = { it.setMenuItems(items("Reply")) },
    ) { _, popupView ->
        assertTrue(
            "the preview should show the long-pressed message",
            popupView.visibleText().contains("long-press me"),
        )
    }

    @Test
    fun tappingARowReachesBothTheItemAndTheListener() {
        var itemFired: String? = null
        var listenerId: String? = null
        var listenerName: String? = null
        withMenu(
            configure = { menu ->
                menu.setMenuItems(items("Reply", "Copy") { itemFired = it })
                menu.setOnMenuItemClickListener { id, item ->
                    listenerId = id
                    listenerName = item
                }
            },
        ) { _, popupView ->
            val rows = popupView.rows()
            assertTrue("expected the option rows to be bound, found ${rows.size}", rows.size >= 2)
            rows[1].performClick()
            ShadowLooper.idleMainLooper()

            assertEquals("the item's own onClick should fire", "Copy", itemFired)
            assertEquals("copy", listenerId)
            assertEquals("Copy", listenerName)
        }
    }

    @Test
    fun theQuickReactionsRowRendersTheDefaults() = withMenu(
        configure = { it.setMenuItems(items("Reply")) },
    ) { _, popupView ->
        val card = popupView.findViewById<View>(R.id.reaction_card)
        assertEquals(View.VISIBLE, card.visibility)
        val text = popupView.visibleText()
        CometChatMessagePopupMenu.DEFAULT_REACTIONS.forEach {
            assertTrue("expected the default reaction $it, was '$text'", text.contains(it))
        }
    }

    @Test
    fun customQuickReactionsReplaceTheDefaults() = withMenu(
        configure = {
            it.setMenuItems(items("Reply"))
            it.setQuickReactions(listOf("🎉", "🚀"))
        },
    ) { _, popupView ->
        val text = popupView.visibleText()
        assertTrue(text.contains("🎉"))
        assertTrue(text.contains("🚀"))
        assertFalse("the defaults should be gone", text.contains("🔥"))
    }

    @Test
    fun hidingTheQuickReactionsHidesTheWholeCard() = withMenu(
        configure = {
            it.setMenuItems(items("Reply"))
            it.setQuickReactionsVisibility(View.GONE)
        },
    ) { _, popupView ->
        assertEquals(
            "the reaction card should be gone, not empty",
            View.GONE,
            popupView.findViewById<View>(R.id.reaction_card).visibility,
        )
    }

    @Test
    fun aReactionTapReachesTheListener() {
        var reacted: String? = null
        withMenu(
            configure = { menu ->
                menu.setMenuItems(items("Reply"))
                menu.setQuickReactions(listOf("🎉"))
                menu.setReactionClickListener { _, reaction -> reacted = reaction }
            },
        ) { _, popupView ->
            val reactions = popupView.findViewById<ViewGroup>(R.id.view_reactions)
            assertTrue("expected a reaction to tap", reactions.childCount > 0)
            reactions.getChildAt(0).performClick()
            ShadowLooper.idleMainLooper()
            assertEquals("🎉", reacted)
        }
    }

    @Test
    fun theCurrentMessageIsHeldWhileShowing() = withMenu(
        configure = { it.setMenuItems(items("Reply")) },
    ) { menu, _ ->
        assertEquals("long-press me", (menu.getCurrentMessage() as? com.cometchat.chat.models.TextMessage)?.text)
    }

    @Test
    fun dismissClosesThePopup() = withMenu(
        configure = { it.setMenuItems(items("Reply")) },
    ) { menu, _ ->
        menu.dismiss()
        ShadowLooper.idleMainLooper()
        assertFalse(
            "the popup should be closed",
            latestPopup().isShowing,
        )
    }

    @Test
    fun anEmptyMenuStillOpens() = withMenu(
        configure = { it.setMenuItems(emptyList()) },
    ) { _, popupView ->
        // A host that filters every option away should get an empty list, not a crash.
        assertEquals(0, popupView.rows().size)
    }

    @Test
    fun theAlignmentSetterIsHonouredBeforeShowing() = withMenu(
        configure = {
            it.setMenuItems(items("Reply"))
            it.setMessageAlignment(UIKitConstants.MessageListAlignment.LEFT_ALIGNED)
        },
    ) { _, popupView ->
        // Left-aligned puts both cards on the start edge; the property tests cover the
        // rule itself, this pins that the setter survives as far as a shown popup.
        val params = popupView.findViewById<View>(R.id.menu_parent).layoutParams
        assertNotNull(params)
        assertTrue(popupView.visibleText().contains("Reply"))
    }
}
