package com.cometchat.uikit.core.utils

import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.UIKitSettings
import com.cometchat.uikit.core.constants.UIKitConstants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for the thread-subscription message action-sheet option (P3, ENG-37569).
 *
 * Locks the DD-mandated behaviour:
 * - §6.3 the option sits immediately after "Reply in Thread" and appears on every message type
 *   (never gated on replyCount).
 * - reply -> parent thread: threadRootId resolves a reply to its parentMessageId, a root to its own id.
 * - the feature gate (UIKitSettings.enableThreadSubscription) is default-ON and opt-OUT, matching
 *   the React and React Native kits; §7.5's original default-off is superseded.
 * - the option is absent on the ineligible categories (interactive / action / call), which matters
 *   because an unrecognised category falls back to the custom-message option set.
 *
 * Scope (ENG-38903): the option and the header bell are offered in BOTH 1-1 and group
 * conversations — the same scope as threading itself. That rule lives in
 * [ThreadSubscriptionScopeTest]. The option's gate in [MessageOptionsUtils.createMessageOption] also
 * reads `CometChatUIKit.isThreadSubscriptionEnabled()`, and `CometChatUIKit` is a Kotlin `object`
 * without `@JvmStatic`, so Mockito's `mockStatic` cannot intercept it — asserting the feature gate
 * here would need a mocking library this module does not use. It is covered at the UI layer instead,
 * by the gate-off tests in both kits' thread-header instrumented tests.
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ThreadSubscriptionOptionTest"
 */
class ThreadSubscriptionOptionTest : FunSpec({

    // ==================== §6.3 placement / presence (pure) ====================

    test("thread subscription option follows Reply in Thread for text messages") {
        val ids = MessageOptionsUtils.getDefaultOptionIds("message", "text")
        ids shouldContain UIKitConstants.MessageOption.THREAD_SUBSCRIPTION
        val replyInThreadIdx = ids.indexOf(UIKitConstants.MessageOption.REPLY_IN_THREAD)
        val threadIdx = ids.indexOf(UIKitConstants.MessageOption.THREAD_SUBSCRIPTION)
        threadIdx shouldBe replyInThreadIdx + 1
    }

    test("thread subscription option is present across message types") {
        listOf("image", "video", "audio", "file").forEach { type ->
            MessageOptionsUtils.getDefaultOptionIds("message", type)
                .shouldContain(UIKitConstants.MessageOption.THREAD_SUBSCRIPTION)
        }
        MessageOptionsUtils.getDefaultOptionIds(UIKitConstants.MessageCategory.CARD, "any")
            .shouldContain(UIKitConstants.MessageOption.THREAD_SUBSCRIPTION)
        MessageOptionsUtils.getDefaultOptionIds("custom", "anything")
            .shouldContain(UIKitConstants.MessageOption.THREAD_SUBSCRIPTION)
    }

    test("agent (copy-only) messages never carry the thread subscription option") {
        MessageOptionsUtils.getDefaultOptionIds(UIKitConstants.MessageCategory.AGENTIC, "assistant")
            .shouldNotContain(UIKitConstants.MessageOption.THREAD_SUBSCRIPTION)
    }

    // ==================== categories ineligible for the toggle ====================

    test("interactive, action and call categories are ineligible for the toggle") {
        listOf(
            UIKitConstants.MessageCategory.INTERACTIVE,
            UIKitConstants.MessageCategory.ACTION,
            UIKitConstants.MessageCategory.CALL
        ).forEach { category ->
            MessageOptionsUtils.isThreadSubscriptionEligible(category) shouldBe false
            // Case must not matter — the option map lower-cases its lookup key.
            MessageOptionsUtils.isThreadSubscriptionEligible(category.uppercase()) shouldBe false
        }
    }

    test("eligible categories stay eligible, including an unknown one") {
        listOf(
            UIKitConstants.MessageCategory.MESSAGE,
            UIKitConstants.MessageCategory.CUSTOM,
            UIKitConstants.MessageCategory.CARD,
            "something_new"
        ).forEach { category ->
            MessageOptionsUtils.isThreadSubscriptionEligible(category) shouldBe true
        }
        // Unknown/absent category follows the permissive option-map fallback.
        MessageOptionsUtils.isThreadSubscriptionEligible(null) shouldBe true
    }

    // ==================== reply -> parent thread ====================

    test("threadRootId resolves a reply to its parent thread and a root to its own id") {
        val reply = mock<BaseMessage> {
            whenever(it.parentMessageId).thenReturn(500L)
            whenever(it.id).thenReturn(7L)
        }
        val root = mock<BaseMessage> {
            whenever(it.parentMessageId).thenReturn(0L)
            whenever(it.id).thenReturn(42L)
        }
        MessageOptionsUtils.threadRootId(reply) shouldBe 500L
        MessageOptionsUtils.threadRootId(root) shouldBe 42L
    }

    // ============ feature gate is default-ON, opt-OUT (React / RN parity) ============

    test("thread subscription is on by default and opt-OUT via the deprecated builder flag") {
        UIKitSettings.UIKitSettingsBuilder().build().enableThreadSubscription shouldBe true

        // Deprecated, but it must keep working — it is the only global opt-out.
        @Suppress("DEPRECATION")
        val optedOut = UIKitSettings.UIKitSettingsBuilder()
            .setEnableThreadSubscription(false)
            .build()
        optedOut.enableThreadSubscription shouldBe false
    }
})
