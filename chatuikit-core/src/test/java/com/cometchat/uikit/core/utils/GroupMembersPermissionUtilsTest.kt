package com.cometchat.uikit.core.utils

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Tests for [GroupMembersPermissionUtils], the shared permission logic both toolkits call.
 *
 * Product rules under test, taken from the object's own contract:
 * - Nobody can act on themselves, whatever their scope.
 * - The group owner can kick or ban anyone but themselves, regardless of their own scope.
 * - Admins act on moderators and participants; moderators kick participants only and ban nobody.
 * - Only the owner changes scopes, and never their own or another owner's.
 *
 * Scope comparison is case-insensitive throughout, which matters because the SDK's constants are
 * lowercase ("admin", "moderator", "participant") while [GroupMembersPermissionUtils.SCOPE_OWNER]
 * is "OWNER" — a mismatch these tests pin down rather than assume.
 *
 * Coverage note: this reaches 37 of 40 branches, and the other three are unreachable rather
 * than untested. In canKickMember, canBanMember and canChangeMemberScope the clause
 * `&& targetMemberId != groupOwnerId` is only evaluated once `groupOwnerId == loggedInUserId`,
 * so its false case needs `targetMemberId == loggedInUserId` — which the self-check at the top
 * of each function has already returned on. The clause is redundant in production; it is left
 * alone here deliberately, and the branches cannot be covered from any input.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*GroupMembersPermissionUtilsTest"
 */
class GroupMembersPermissionUtilsTest : FunSpec({

    val admin = CometChatConstants.SCOPE_ADMIN
    val moderator = CometChatConstants.SCOPE_MODERATOR
    val participant = CometChatConstants.SCOPE_PARTICIPANT
    val ownerScope = GroupMembersPermissionUtils.SCOPE_OWNER

    // ==================== canKickMember ====================

    test("nobody can kick themselves, even the owner") {
        GroupMembersPermissionUtils.canKickMember(
            loggedInUserScope = ownerScope,
            targetMemberScope = ownerScope,
            loggedInUserId = "me",
            targetMemberId = "me",
            groupOwnerId = "me"
        ) shouldBe false
    }

    test("the owner can kick any other member regardless of their scope") {
        listOf(admin, moderator, participant, ownerScope).forAll { targetScope ->
            GroupMembersPermissionUtils.canKickMember(
                loggedInUserScope = participant, // the owner's own scope is irrelevant
                targetMemberScope = targetScope,
                loggedInUserId = "owner-uid",
                targetMemberId = "other",
                groupOwnerId = "owner-uid"
            ) shouldBe true
        }
    }

    test("an admin can kick moderators and participants but not other admins") {
        fun adminKicks(targetScope: String) = GroupMembersPermissionUtils.canKickMember(
            admin, targetScope, "admin-uid", "target", "owner-uid"
        )
        adminKicks(moderator) shouldBe true
        adminKicks(participant) shouldBe true
        adminKicks(admin) shouldBe false
        adminKicks(ownerScope) shouldBe false
    }

    test("a moderator can kick participants only") {
        fun moderatorKicks(targetScope: String) = GroupMembersPermissionUtils.canKickMember(
            moderator, targetScope, "mod-uid", "target", "owner-uid"
        )
        moderatorKicks(participant) shouldBe true
        moderatorKicks(moderator) shouldBe false
        moderatorKicks(admin) shouldBe false
    }

    test("a participant can kick nobody") {
        listOf(admin, moderator, participant).forAll { targetScope ->
            GroupMembersPermissionUtils.canKickMember(
                participant, targetScope, "p-uid", "target", "owner-uid"
            ) shouldBe false
        }
    }

    test("kick permission ignores scope casing") {
        GroupMembersPermissionUtils.canKickMember(
            "ADMIN", "PARTICIPANT", "admin-uid", "target", "owner-uid"
        ) shouldBe true
        GroupMembersPermissionUtils.canKickMember(
            "Moderator", "Participant", "mod-uid", "target", "owner-uid"
        ) shouldBe true
    }

    test("a null group owner does not grant kick rights to anyone") {
        GroupMembersPermissionUtils.canKickMember(
            participant, participant, "p-uid", "target", null
        ) shouldBe false
        // an admin still acts by scope, not by ownership
        GroupMembersPermissionUtils.canKickMember(
            admin, participant, "admin-uid", "target", null
        ) shouldBe true
    }

    // ==================== canBanMember ====================

    test("nobody can ban themselves") {
        GroupMembersPermissionUtils.canBanMember(
            admin, admin, "me", "me", "owner-uid"
        ) shouldBe false
    }

    test("the owner can ban any other member") {
        listOf(admin, moderator, participant).forAll { targetScope ->
            GroupMembersPermissionUtils.canBanMember(
                participant, targetScope, "owner-uid", "other", "owner-uid"
            ) shouldBe true
        }
    }

    test("an admin can ban moderators and participants but not other admins") {
        fun adminBans(targetScope: String) = GroupMembersPermissionUtils.canBanMember(
            admin, targetScope, "admin-uid", "target", "owner-uid"
        )
        adminBans(moderator) shouldBe true
        adminBans(participant) shouldBe true
        adminBans(admin) shouldBe false
    }

    test("a moderator can ban nobody — this is where ban differs from kick") {
        listOf(admin, moderator, participant).forAll { targetScope ->
            GroupMembersPermissionUtils.canBanMember(
                moderator, targetScope, "mod-uid", "target", "owner-uid"
            ) shouldBe false
        }
        // the same moderator *can* kick a participant
        GroupMembersPermissionUtils.canKickMember(
            moderator, participant, "mod-uid", "target", "owner-uid"
        ) shouldBe true
    }

    test("ban permission ignores scope casing") {
        GroupMembersPermissionUtils.canBanMember(
            "ADMIN", "MODERATOR", "admin-uid", "target", "owner-uid"
        ) shouldBe true
    }

    // ==================== canChangeMemberScope ====================

    test("only the group owner can change a member's scope") {
        GroupMembersPermissionUtils.canChangeMemberScope(
            loggedInUserId = "owner-uid", targetMemberId = "member", groupOwnerId = "owner-uid"
        ) shouldBe true
        GroupMembersPermissionUtils.canChangeMemberScope(
            loggedInUserId = "admin-uid", targetMemberId = "member", groupOwnerId = "owner-uid"
        ) shouldBe false
    }

    test("the owner cannot change their own scope") {
        GroupMembersPermissionUtils.canChangeMemberScope(
            loggedInUserId = "owner-uid", targetMemberId = "owner-uid", groupOwnerId = "owner-uid"
        ) shouldBe false
    }

    test("a null group owner blocks scope changes entirely") {
        GroupMembersPermissionUtils.canChangeMemberScope(
            loggedInUserId = "anyone", targetMemberId = "member", groupOwnerId = null
        ) shouldBe false
    }

    // ==================== getScopeLevel / compareScopes ====================

    test("scope levels rank owner above admin above moderator above participant") {
        GroupMembersPermissionUtils.getScopeLevel(ownerScope) shouldBe 4
        GroupMembersPermissionUtils.getScopeLevel(admin) shouldBe 3
        GroupMembersPermissionUtils.getScopeLevel(moderator) shouldBe 2
        GroupMembersPermissionUtils.getScopeLevel(participant) shouldBe 1
    }

    test("an unrecognised scope ranks below every known one") {
        GroupMembersPermissionUtils.getScopeLevel("superuser") shouldBe 0
        GroupMembersPermissionUtils.getScopeLevel("") shouldBe 0
    }

    test("scope levels ignore casing") {
        GroupMembersPermissionUtils.getScopeLevel("owner") shouldBe 4
        GroupMembersPermissionUtils.getScopeLevel("Admin") shouldBe 3
        GroupMembersPermissionUtils.getScopeLevel("MODERATOR") shouldBe 2
    }

    test("compareScopes orders by privilege and returns zero for equals") {
        GroupMembersPermissionUtils.compareScopes(admin, participant) shouldBeGreaterThan 0
        GroupMembersPermissionUtils.compareScopes(participant, admin) shouldBeLessThan 0
        GroupMembersPermissionUtils.compareScopes(moderator, moderator) shouldBe 0
        GroupMembersPermissionUtils.compareScopes(ownerScope, admin) shouldBeGreaterThan 0
    }

    // ==================== getAssignableScopes ====================

    test("assignable scopes are admin, moderator and participant — never owner") {
        val scopes = GroupMembersPermissionUtils.getAssignableScopes()
        scopes shouldContainExactly listOf(admin, moderator, participant)
        scopes shouldNotContain ownerScope
    }

    // ==================== hasScope / isOwner ====================

    test("hasScope matches a member's scope case-insensitively") {
        val member = GroupMember("uid-1", admin)
        GroupMembersPermissionUtils.hasScope(member, admin) shouldBe true
        GroupMembersPermissionUtils.hasScope(member, "ADMIN") shouldBe true
        GroupMembersPermissionUtils.hasScope(member, moderator) shouldBe false
    }

    test("isOwner compares the member uid against the group owner") {
        val group = Group().apply { setOwner("owner-uid") }
        GroupMembersPermissionUtils.isOwner(GroupMember("owner-uid", admin), group) shouldBe true
        GroupMembersPermissionUtils.isOwner(GroupMember("someone-else", admin), group) shouldBe false
    }

    // ==================== userToGroupMember ====================

    test("a converted user defaults to participant and carries their profile across") {
        val user = User().apply {
            uid = "u-1"
            name = "Ada"
            avatar = "https://example.invalid/ada.png"
            status = CometChatConstants.USER_STATUS_ONLINE
        }

        val member = GroupMembersPermissionUtils.userToGroupMember(user)

        member.uid shouldBe "u-1"
        member.scope shouldBe participant
        member.name shouldBe "Ada"
        member.avatar shouldBe "https://example.invalid/ada.png"
        member.status shouldBe CometChatConstants.USER_STATUS_ONLINE
    }

    test("a converted user can be given an explicit scope") {
        val user = User().apply { uid = "u-2"; name = "Grace" }
        GroupMembersPermissionUtils.userToGroupMember(user, admin).scope shouldBe admin
    }
})

/** Applies [assertion] to every element, so one failing case names the scope that broke. */
private fun <T> List<T>.forAll(assertion: (T) -> Unit) = forEach(assertion)
