# Changelog

All notable changes to the CometChat UI Kit for Android are documented here.
Each entry is generated from that release's pull-request description.

## v6.1.1 (2026-10-12)

# v6.1.1

## Breaking Changes
- **NotificationFeed callback properties** — Direct property assignment on `CometChatNotificationFeed` (e.g. `notificationFeed.onItemClick = { … }`) is no longer supported; the properties are now private. Kotlin callers must migrate to the new setters (e.g. `setOnItemClick { … }`). Java callers using `setOnItemClick(...)` are unaffected. 
- GroupMembersPermissionUtils.canChangeMemberScope signature changed — The helper now takes five parameters (loggedInUserScope, targetMemberScope, loggedInUserId, targetMemberId, groupOwnerId); the previous three-argument overload has been removed. Callers of the old form must update.
- **Compose `CometChatMessageList` gained a `threaded` parameter** — added mid-signature; source-compatible for named/default callers, binary-incompatible for code compiled against the previous artifact. (ENG-39321)

## Enhancements
- **Documented build requirements** — Added a build-requirements table (minSdk 28, compileSdk 36, AGP 8.9.1, Gradle 8.11, `useAndroidX`/jetifier) and aligned the declared `androidx.core` version (1.18.0) with what the toolkit actually resolves, so consumers get a clear signal instead of an opaque `checkDebugAarMetadata` failure. (ENG-39320)

## Fixes
- **Theme crash under a non-CometChat host theme** — Kit views no longer crash on inflate when the host app's theme doesn't extend `CometChatTheme.DayNight`; the inflation context is now wrapped safely. (ENG-39309)
- **Duplicate-class build failure on first build** — Excluded the legacy `org.jetbrains:annotations-java5` transitive dependency (from prism4j / markwon-syntax-highlight) and carried the exclusion into the published POM, so a fresh consumer app builds without the ~40 "Duplicate class" dexing errors. (ENG-39310)
- **Message-header member count stayed stale on re-bind** — `CometChatMessageHeaderViewModel.setGroup` no longer re-seeds the member count when re-bound with the same group, so a stale re-bind can't clobber the live count; realtime events and `refreshGroup` keep it current. (ENG-39701)
- **Calling launch crash without the Calls SDK** — `enableCalling=true` without the Calls SDK on the classpath no longer crashes at launch; Calls init is gated on SDK availability and hardened against linkage failures, and calling components degrade gracefully when the SDK is absent. (ENG-39311)
- **Thread view leaked the full conversation** — A thread screen opened without a valid parent message now shows an empty state instead of rendering the entire conversation. Adds a `setThreaded(...)` option on the message-list components (View + Compose). (ENG-39321)
- **Thread replies silently dropped** — The message composer now fails fast with a clear `ERR_NO_RECEIVER` error when a send is attempted with no receiver, instead of silently dropping the message. (ENG-39322)
- Admins couldn't change member scopes — In group details, admins saw only Ban/Kick with no option to change a member's scope; scope changes were restricted to the group owner. Admins can now change the scope of moderators and participants (owners can still change anyone's except their own and the owner's). (ENG-39699)

## Dependencies
- **Chat SDK → 5.0.7** — Bundled `com.cometchat:chat-sdk-android` updated from 5.0.6 to 5.0.7.

## v6.1.0 (2026-09-29)

Release 6.1.0 (production)

