package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.exceptions.CometChatException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * ENG-41133 (W7): the mentions-suggestion merge shared by both kits (README §3.9 "Mentions
 * suggestions"; BEH-SNAP-04). Rows are plain strings; the id is the string itself.
 */
class TwoPhaseSuggestionFetchTest : FunSpec({

    val id: (String) -> String = { it }
    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    test("no keyword: cached first, then server by id with no duplicates") {
        val fetch = TwoPhaseSuggestionFetch("")
        val afterCached = fetch.onCached(listOf("@all"), listOf("ann", "bob"), id)
        afterCached shouldBe listOf("@all", "ann", "bob")

        // Server drops bob (its cached copy is replaced) and adds cat; ann is not repeated.
        fetch.onServer(afterCached!!, listOf("ann", "cat"), id) shouldBe listOf("@all", "ann", "cat")
    }

    test("no keyword: a cached row already listed by an earlier page is kept through the server phase") {
        val fetch = TwoPhaseSuggestionFetch(null)
        val afterCached = fetch.onCached(listOf("ann"), listOf("ann", "bob"), id)!!
        fetch.onServer(afterCached, listOf("bob"), id) shouldBe listOf("ann", "bob")
    }

    test("keyword: the cached phase is ignored (server only)") {
        val fetch = TwoPhaseSuggestionFetch("an")
        fetch.acceptsCached shouldBe false
        fetch.onCached(emptyList(), listOf("ann"), id) shouldBe null
        fetch.onServer(emptyList(), listOf("ann", "dan"), id) shouldBe listOf("ann", "dan")
    }

    test("keyword offline: rows are not kept, so the list empties (no toast is a kit concern)") {
        val fetch = TwoPhaseSuggestionFetch("an")
        fetch.onCached(emptyList(), listOf("ann"), id)
        fetch.keepRowsOnError(notConnected) shouldBe false
    }

    test("no keyword offline: the cached rows are kept on the not-connected error only") {
        val fetch = TwoPhaseSuggestionFetch("")
        fetch.onCached(emptyList(), listOf("ann"), id)
        fetch.keepRowsOnError(notConnected) shouldBe true
        fetch.keepRowsOnError(CometChatException("ERR_SOMETHING", "boom")) shouldBe false
    }

    test("offline off (server phase only): ids already listed are skipped, the rest appended in order") {
        val fetch = TwoPhaseSuggestionFetch("")
        fetch.onServer(listOf("ann", "bob"), listOf("bob", "cat", "dan"), id) shouldBe listOf("ann", "bob", "cat", "dan")
        fetch.keepRowsOnError(notConnected) shouldBe false
    }
})
