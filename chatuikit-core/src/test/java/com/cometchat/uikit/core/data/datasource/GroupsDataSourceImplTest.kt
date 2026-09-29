package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [GroupsDataSourceImpl] (ENG-38677 / L — re-pointed).
 *
 * Previously this suite exercised a hand-written `TestableGroupsDataSource`
 * stand-in, so the real Impl scored ~0%. Re-pointed to construct the REAL
 * [GroupsDataSourceImpl] and drive its SDK seams: `fetchGroups` →
 * `request.fetchNext` (throws on error via `resumeWithException`), `joinGroup`
 * → static `CometChat.joinGroup(id, type, password, callback)` (callback index
 * 3). Flat tests so the static mock registers once per leaf.
 */
class GroupsDataSourceImplTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var dataSource: GroupsDataSourceImpl

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        dataSource = GroupsDataSourceImpl()
    }
    afterTest {
        cometChatMock.close()
    }

    test("fetchGroups returns the SDK list on success") {
        runTest {
            val request = mock<GroupsRequest>()
            val groups = listOf(mock<Group>(), mock<Group>(), mock<Group>())
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<Group>>>(0).onSuccess(groups)
                null
            }

            dataSource.fetchGroups(request) shouldBe groups
        }
    }

    test("fetchGroups throws the SDK exception on error") {
        runTest {
            val request = mock<GroupsRequest>()
            val error = MockFactory.createCometChatException("ERR_FETCH", "Network error")
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<Group>>>(0).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.fetchGroups(request) } shouldBe error
        }
    }

    test("joinGroup returns the joined group and passes null password as empty string") {
        runTest {
            val group = mock<Group>()
            cometChatMock.`when`<Unit> {
                CometChat.joinGroup(eq("join-1"), eq("public"), eq(""), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<Group>>(3).onSuccess(group)
                null
            }

            dataSource.joinGroup("join-1", "public", null) shouldBe group
        }
    }

    test("joinGroup forwards the password and throws the SDK exception on error") {
        runTest {
            val error = MockFactory.createCometChatException("ERR_JOIN", "Cannot join group")
            cometChatMock.`when`<Unit> {
                CometChat.joinGroup(eq("g2"), eq("password"), eq("secret"), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<Group>>(3).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.joinGroup("g2", "password", "secret") } shouldBe error
        }
    }

    test("PBT: fetchGroups returns exactly the SDK-provided count for any size 0..30") {
        checkAll(15, Arb.int(0, 30)) { count ->
            runTest {
                val request = mock<GroupsRequest>()
                val groups = (1..count).map { mock<Group>() }
                whenever(request.fetchNext(any())).thenAnswer { inv ->
                    inv.getArgument<CometChat.CallbackListener<List<Group>>>(0).onSuccess(groups)
                    null
                }

                dataSource.fetchGroups(request).size shouldBe count
            }
        }
    }
})
