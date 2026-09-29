package com.cometchat.uikit.kotlin.presentation.threadheader

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.UIKitSettings
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatThreadHeaderViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messagelist.MessageListTestSdkHelper
import com.cometchat.uikit.kotlin.presentation.threadheader.ui.CometChatThreadHeader
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever

private val SUB_HEADER_TEST_ID = View.generateViewId()

/**
 * Instrumented tests for the thread-subscription bell on [CometChatThreadHeader] (chatuikit-kotlin).
 *
 * Covers the rendering contract for P3 (ENG-37569):
 * - the bell renders only when the `enableThreadSubscription` feature gate is on, a parent message
 *   is present, and the per-control visibility is not turned off;
 * - `setThreadSubscriptionVisibility(GONE)` hides it even when the gate is on.
 *
 * The gate is set by initializing [CometChatUIKit] with the desired flag (its `init` assigns the
 * settings synchronously); it is default-ON, so the gate-off cases below opt out explicitly and
 * `@After` restores the default. State reads resolve to UNKNOWN for an unseeded thread, so the
 * control renders as un-followed/enabled — which is all these visibility checks depend on.
 *
 * Run with:
 *   ./gradlew :chatuikit-kotlin:connectedDebugAndroidTest --tests "*CometChatThreadHeaderSubscriptionInstrumentedTest"
 */
@RunWith(AndroidJUnit4::class)
class CometChatThreadHeaderSubscriptionInstrumentedTest {

    @Before
    fun setup() {
        MessageListTestSdkHelper.ensureInitialized()
        SubscriptionHeaderHostFragment.reset()
    }

    @After
    fun restoreThreadSubscriptionGate() {
        // The gate lives on CometChatUIKit's process-wide settings and is default-ON, so a gate-off
        // case must not be left behind for whatever runs next.
        SubscriptionHeaderHostFragment.applyGate(ApplicationProvider.getApplicationContext(), true)
    }

    /**
     * The default fixture roots the thread in a group. [oneToOneParentMessage] covers the 1-1 case,
     * where the bell is offered too (ENG-38903) — the control follows threading's own scope.
     */
    private fun rootParentMessage(id: Long = 200L): BaseMessage {
        val message = MockFactory.createTextMessage(
            id = id,
            senderUid = "user-1",
            text = "Parent message",
            receiverId = "group-1",
            receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
        )
        whenever(message.replyCount).thenReturn(3)
        return message
    }

    private fun oneToOneParentMessage(id: Long = 200L): BaseMessage {
        val message = MockFactory.createTextMessage(
            id = id,
            senderUid = "user-1",
            text = "Parent message"
        )
        whenever(message.replyCount).thenReturn(3)
        return message
    }

    @Test
    fun bellIsHiddenWhenFeatureGateOff() {
        SubscriptionHeaderHostFragment.enableThreadSubscription = false
        SubscriptionHeaderHostFragment.injectedParentMessage = rootParentMessage()

        launchFragmentInContainer<SubscriptionHeaderHostFragment>(
            themeResId = R.style.CometChatTheme_DayNight
        )

        onView(withId(R.id.iv_thread_subscription))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun bellIsVisibleWhenFeatureGateOnWithParentMessage() {
        SubscriptionHeaderHostFragment.enableThreadSubscription = true
        SubscriptionHeaderHostFragment.injectedParentMessage = rootParentMessage()

        launchFragmentInContainer<SubscriptionHeaderHostFragment>(
            themeResId = R.style.CometChatTheme_DayNight
        )

        onView(withId(R.id.iv_thread_subscription))
            .check(matches(isDisplayed()))
    }


    @Test
    fun setThreadSubscriptionVisibilityGoneHidesBellEvenWhenGateOn() {
        SubscriptionHeaderHostFragment.enableThreadSubscription = true
        SubscriptionHeaderHostFragment.injectedParentMessage = rootParentMessage()
        SubscriptionHeaderHostFragment.threadSubscriptionVisibility = View.GONE

        launchFragmentInContainer<SubscriptionHeaderHostFragment>(
            themeResId = R.style.CometChatTheme_DayNight
        )

        onView(withId(R.id.iv_thread_subscription))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun bellIsHiddenWhenNoParentMessage() {
        SubscriptionHeaderHostFragment.enableThreadSubscription = true
        SubscriptionHeaderHostFragment.injectedParentMessage = null

        launchFragmentInContainer<SubscriptionHeaderHostFragment>(
            themeResId = R.style.CometChatTheme_DayNight
        )

        onView(withId(R.id.iv_thread_subscription))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }
}

/**
 * Host fragment for the thread-subscription bell tests. Sets the feature gate via [CometChatUIKit]
 * before the header is built, then configures the header.
 */
class SubscriptionHeaderHostFragment : Fragment() {

    companion object {
        var injectedParentMessage: BaseMessage? = null

        // Mirrors the kit's own default (ON); the gate-off cases set this to false explicitly.
        var enableThreadSubscription: Boolean = true
        var threadSubscriptionVisibility: Int? = null

        fun reset() {
            injectedParentMessage = null
            enableThreadSubscription = true
            threadSubscriptionVisibility = null
        }

        /**
         * Applies the init-time gate process-wide, before the header reads it. `init` assigns
         * `authenticationSettings` synchronously, so `isThreadSubscriptionEnabled()` reflects
         * this immediately.
         *
         * **Ends up on the main thread either way.** [CometChatUIKit.init] asserts its own
         * threading contract (ENG-38658 / X5) and throws `IllegalStateException` off the main
         * thread. This helper has two callers on two different threads: `onCreateView`, already
         * on the main thread, and `@After`, which runs on the instrumentation thread — so hop
         * only when we are not already there, because `runOnMainSync` called from the main
         * thread deadlocks waiting on itself. `runOnMainSync` blocks until the block returns,
         * so the gate is assigned before the next case launches its fragment.
         *
         * Credentials come from instrumentation runner arguments (ENG-38650), never
         * hardcoded — same argument names as MessageListTestSdkHelper and the sample apps'
         * E2ETestConfig. The placeholder fallbacks are deliberate and sufficient: this test
         * never authenticates, so the app ID and auth key are inert. Do not substitute real
         * credentials — chatuikit-kotlin is mirrored to the public repo, and a 40-hex auth
         * key here fails mirror-secret-gate.sh and stops the release.
         */
        @Suppress("DEPRECATION")
        fun applyGate(context: Context, enabled: Boolean) {
            fun arg(name: String): String? =
                InstrumentationRegistry.getArguments().getString(name)?.takeIf { it.isNotBlank() }

            val settings = UIKitSettings.UIKitSettingsBuilder()
                .setAppId(arg("appId") ?: "YOUR_APP_ID")
                .setRegion(arg("region") ?: "in")
                .setAuthKey(arg("authKey") ?: "YOUR_AUTH_KEY")
                .setEnableThreadSubscription(enabled)
                .build()

            if (Looper.myLooper() == Looper.getMainLooper()) {
                CometChatUIKit.init(context, settings, null)
            } else {
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    CometChatUIKit.init(context, settings, null)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        // Set the feature gate before the header reads it.
        applyGate(requireContext(), enableThreadSubscription)

        val threadHeader = CometChatThreadHeader(requireContext())
        threadHeader.id = SUB_HEADER_TEST_ID

        return FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            addView(threadHeader)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val threadHeader = (view as FrameLayout).getChildAt(0) as CometChatThreadHeader
        threadHeader.setViewModel(CometChatThreadHeaderViewModel(enableListeners = false))
        threadSubscriptionVisibility?.let { threadHeader.setThreadSubscriptionVisibility(it) }
        injectedParentMessage?.let { threadHeader.setParentMessage(it) }
    }
}
