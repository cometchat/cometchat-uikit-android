package com.cometchat.uikit.core.models

import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.User
import com.cometchat.chat.core.CometChat
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.json.JSONObject
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for the poll extraction in PollModels.kt — [extractPollData] and [getUserVotedOption].
 *
 * A poll arrives split across two places on the same message, and the parser has to join them:
 * the question and option text live in `customData`, while vote counts and voters live in
 * `metadata.@injected.extensions.polls.results`. Everything here pins that contract, including
 * what happens when half of it is missing, which is the common case for a poll nobody has
 * voted on yet.
 *
 * Rules under test:
 * - No `customData`, no question, or no options → null. A missing poll id falls back to the
 *   message id rather than failing.
 * - Options are read by 1-based string key and stop at the first gap, because the loop indexes
 *   1..length rather than iterating the keys.
 * - `isSelected` is true only for the option the logged-in user appears in.
 * - At most three voters are carried for display, but the vote count and the selected flag
 *   reflect *all* voters.
 * - Malformed metadata degrades to a poll with zero votes instead of throwing.
 *
 * CometChatUIKit is a Kotlin object, so `getLoggedInUser()` is an instance method that
 * `mockStatic` cannot intercept. It delegates straight to `CometChat.getLoggedInUser()`, which is
 * a real static, so that is what these tests stub.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*PollModelsTest"
 */
class PollModelsTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val me = "voter-me"

    lateinit var cometChatMock: MockedStatic<CometChat>

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        val loggedInUser = mock<User>()
        whenever(loggedInUser.uid).thenReturn(me)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedInUser)
    }

    afterTest { cometChatMock.close() }

    /** customData for a poll with [question] and 1-based [options]; omit either to leave it out. */
    fun customData(
        id: String? = "poll-1",
        question: String? = "Best colour?",
        options: List<String>? = listOf("Red", "Blue")
    ): JSONObject = JSONObject().apply {
        id?.let { put("id", it) }
        question?.let { put("question", it) }
        options?.let { list ->
            put("options", JSONObject().apply {
                list.forEachIndexed { i, text -> put((i + 1).toString(), text) }
            })
        }
    }

    /** metadata carrying results for the given option key → (count, voter uids). */
    fun metadataWith(results: Map<String, Pair<Int, List<String>>>, total: Int): JSONObject {
        val optionsObj = JSONObject()
        results.forEach { (key, value) ->
            val (count, voterUids) = value
            val votersObj = JSONObject()
            voterUids.forEach { uid ->
                votersObj.put(uid, JSONObject().apply {
                    put("name", "Name-$uid")
                    put("avatar", "avatar-$uid")
                })
            }
            optionsObj.put(key, JSONObject().apply {
                put("count", count)
                put("voters", votersObj)
            })
        }
        return JSONObject().apply {
            put("@injected", JSONObject().apply {
                put("extensions", JSONObject().apply {
                    put("polls", JSONObject().apply {
                        put("results", JSONObject().apply {
                            put("total", total)
                            put("options", optionsObj)
                        })
                    })
                })
            })
        }
    }

    fun message(customData: JSONObject?, metadata: JSONObject? = null, id: Long = 4242L): CustomMessage {
        val m = mock<CustomMessage>()
        whenever(m.customData).thenReturn(customData)
        whenever(m.metadata).thenReturn(metadata)
        whenever(m.id).thenReturn(id)
        return m
    }

    // ==================== rejection cases ====================

    test("a message with no customData is not a poll") {
        extractPollData(message(customData = null)) shouldBe null
    }

    test("a poll with no question is rejected") {
        extractPollData(message(customData(question = null))) shouldBe null
    }

    test("a poll with no options is rejected") {
        extractPollData(message(customData(options = null))) shouldBe null
    }

    // ==================== the happy path ====================

    test("a poll with no votes yet reads its question and options with zero counts") {
        val poll = extractPollData(message(customData()))
        poll shouldNotBe null
        poll!!.id shouldBe "poll-1"
        poll.question shouldBe "Best colour?"
        poll.totalVotes shouldBe 0
        poll.options.size shouldBe 2
        poll.options[0].text shouldBe "Red"
        poll.options[0].voteCount shouldBe 0
        poll.options[0].voters shouldBe emptyList()
        poll.options[0].isSelected shouldBe false
    }

    test("a poll with no id falls back to the message id") {
        extractPollData(message(customData(id = null), id = 777L))!!.id shouldBe "777"
    }

    test("vote counts and totals come from the results metadata") {
        val md = metadataWith(mapOf("1" to (2 to listOf("u1", "u2")), "2" to (1 to listOf("u3"))), total = 3)
        val poll = extractPollData(message(customData(), md))!!
        poll.totalVotes shouldBe 3
        poll.options[0].voteCount shouldBe 2
        poll.options[1].voteCount shouldBe 1
    }

    test("voter names and avatars are carried across") {
        val md = metadataWith(mapOf("1" to (1 to listOf("u1"))), total = 1)
        val voter = extractPollData(message(customData(), md))!!.options[0].voters.single()
        voter.uid shouldBe "u1"
        voter.name shouldBe "Name-u1"
        voter.avatarUrl shouldBe "avatar-u1"
    }

    // ==================== the logged-in user's vote ====================

    test("the option the logged-in user voted for is marked selected") {
        val md = metadataWith(mapOf("1" to (1 to listOf("someone")), "2" to (1 to listOf(me))), total = 2)
        val poll = extractPollData(message(customData(), md))!!
        poll.options[0].isSelected shouldBe false
        poll.options[1].isSelected shouldBe true
    }

    test("getUserVotedOption returns the 1-indexed option, or zero when the user has not voted") {
        val voted = metadataWith(mapOf("1" to (0 to emptyList()), "2" to (1 to listOf(me))), total = 1)
        getUserVotedOption(message(customData(), voted)) shouldBe 2

        val notVoted = metadataWith(mapOf("1" to (1 to listOf("someone"))), total = 1)
        getUserVotedOption(message(customData(), notVoted)) shouldBe 0
    }

    test("getUserVotedOption returns zero when the message is not a poll at all") {
        getUserVotedOption(message(customData = null)) shouldBe 0
    }

    test("with no logged-in user, no option is selected") {
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(null)
        val md = metadataWith(mapOf("1" to (1 to listOf(me))), total = 1)
        extractPollData(message(customData(), md))!!.options[0].isSelected shouldBe false
    }

    // ==================== display limits and edge shapes ====================

    test("at most three voters are carried, while the count and selection still see them all") {
        val voters = listOf("u1", "u2", "u3", "u4", me)
        val md = metadataWith(mapOf("1" to (5 to voters)), total = 5)
        val option = extractPollData(message(customData(), md))!!.options[0]
        option.voters.size shouldBe 3      // display cap
        option.voteCount shouldBe 5        // from the results payload, not the voter list
        option.isSelected shouldBe true    // "me" is the 5th voter, past the display cap
    }

    test("an empty option text is skipped rather than producing a blank row") {
        val poll = extractPollData(message(customData(options = listOf("Red", "", "Green"))))!!
        // the loop runs 1..3 and drops the blank, so two options survive
        poll.options.size shouldBe 2
        poll.options.map { it.id } shouldBe listOf("1", "3")
    }

    test("metadata without the injected results block yields a poll with zero votes") {
        val bare = JSONObject().apply { put("something-else", 1) }
        val poll = extractPollData(message(customData(), bare))!!
        poll.totalVotes shouldBe 0
        poll.options[0].voteCount shouldBe 0
    }

    test("results present but missing an option's entry leaves that option at zero") {
        val md = metadataWith(mapOf("1" to (2 to listOf("u1", "u2"))), total = 2)
        val poll = extractPollData(message(customData(), md))!!
        poll.options[0].voteCount shouldBe 2
        poll.options[1].voteCount shouldBe 0
        poll.options[1].voters shouldBe emptyList()
    }

    test("a poll whose options hold a non-object value is rejected rather than throwing") {
        val malformed = JSONObject().apply {
            put("id", "p"); put("question", "q"); put("options", "not-an-object")
        }
        extractPollData(message(malformed)) shouldBe null
    }
})
