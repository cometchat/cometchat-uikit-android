package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupMembersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [GroupMembersDataSourceImpl] (ENG-38677 / L — re-pointed).
 *
 * Previously exercised a stand-in. Re-pointed to the REAL Impl:
 *  - `kick`/`ban`/`changeMemberScope` → static `CometChat.*` (note the SDK takes
 *    `(uid, guid, …)` order); throw on error.
 *  - `fetchGroupMembers` builds its `GroupMembersRequest` internally, so the
 *    builder is intercepted with `Mockito.mockConstruction` to inject a mock
 *    request whose `fetchNext` callback we drive.
 *  - `hasMoreMembers` / `resetRequest` state.
 * Flat tests so the static mock registers once per leaf.
 */
class GroupMembersDataSourceImplTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var dataSource: GroupMembersDataSourceImpl

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        dataSource = GroupMembersDataSourceImpl(mock<GroupMembersRequest.GroupMembersRequestBuilder>())
    }
    afterTest {
        cometChatMock.close()
    }

    test("fetchGroupMembers returns members and reports hasMore") {
        runTest {
            val request = mock<GroupMembersRequest>()
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<GroupMember>>>(0)
                    .onSuccess(listOf(mock<GroupMember>(), mock<GroupMember>()))
                null
            }
            Mockito.mockConstruction(GroupMembersRequest.GroupMembersRequestBuilder::class.java) { builder, _ ->
                whenever(builder.setLimit(any())).thenReturn(builder)
                whenever(builder.setSearchKeyword(any())).thenReturn(builder)
                whenever(builder.build()).thenReturn(request)
            }.use {
                val members = dataSource.fetchGroupMembers("g1", 10, null)

                members.size shouldBe 2
                dataSource.hasMoreMembers() shouldBe true
            }
        }
    }

    test("fetchGroupMembers throws the SDK exception on error") {
        runTest {
            val request = mock<GroupMembersRequest>()
            val error = MockFactory.createCometChatException("ERR_MEMBERS", "boom")
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<GroupMember>>>(0).onError(error)
                null
            }
            Mockito.mockConstruction(GroupMembersRequest.GroupMembersRequestBuilder::class.java) { builder, _ ->
                whenever(builder.setLimit(any())).thenReturn(builder)
                whenever(builder.setSearchKeyword(any())).thenReturn(builder)
                whenever(builder.build()).thenReturn(request)
            }.use {
                shouldThrow<CometChatException> { dataSource.fetchGroupMembers("g1", 10, "query") } shouldBe error
            }
        }
    }

    test("kickGroupMember returns the SDK result (uid, guid order) on success") {
        runTest {
            cometChatMock.`when`<Unit> {
                CometChat.kickGroupMember(eq("u1"), eq("g1"), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<String>>(2).onSuccess("kicked")
                null
            }

            dataSource.kickGroupMember("g1", "u1") shouldBe "kicked"
        }
    }

    test("kickGroupMember throws on error") {
        runTest {
            val error = MockFactory.createCometChatException("ERR_KICK", "denied")
            cometChatMock.`when`<Unit> {
                CometChat.kickGroupMember(any(), any(), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<String>>(2).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.kickGroupMember("g1", "u1") } shouldBe error
        }
    }

    test("banGroupMember returns the SDK result on success") {
        runTest {
            cometChatMock.`when`<Unit> {
                CometChat.banGroupMember(eq("u2"), eq("g2"), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<String>>(2).onSuccess("banned")
                null
            }

            dataSource.banGroupMember("g2", "u2") shouldBe "banned"
        }
    }

    test("changeMemberScope returns the SDK result on success (uid, guid, scope)") {
        runTest {
            cometChatMock.`when`<Unit> {
                CometChat.updateGroupMemberScope(eq("u3"), eq("g3"), eq("admin"), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<String>>(3).onSuccess("scope-changed")
                null
            }

            dataSource.changeMemberScope("g3", "u3", "admin") shouldBe "scope-changed"
        }
    }

    test("hasMoreMembers is true initially and after resetRequest") {
        dataSource.hasMoreMembers() shouldBe true
        dataSource.resetRequest()
        dataSource.hasMoreMembers() shouldBe true
    }
})
