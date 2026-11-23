# Changelog

All notable changes to the CometChat UI Kit for Android are documented here.
Each entry is generated from that release's pull-request description.

## v6.3.0 (2026-11-23)

# v6.3.0

## New
- Added offline support to both UI Kits. Turn it on with `UIKitSettings.UIKitSettingsBuilder().setOfflineConfig(...)`. The UI Kit then reads from the Chat SDK's on-device cache, so conversations, users, groups, group members and saved message history, including threads, open without a connection. Offline support is off unless your app turns it on.
- With offline enabled, text messages, voice notes and messages sent with `CometChatUIKit.sendTextMessage`, `sendMediaMessage` and `sendCustomMessage` wait in a queue while the device is offline and go out when the connection returns. A queued message shows "Not sent yet". A message that could not be sent shows "Not sent. Tap to retry", and its menu offers Retry, Delete and, for text messages, Copy.
- With offline enabled, lists keep the rows saved on the device while offline. A list with nothing saved shows a "Connect to load" state, search shows "Connect to search", and the message list shows "Connect to load more" and "Jump to latest" at the edge of the saved history. Each loads once when the connection returns.
- With offline enabled, the media viewer, Download, Share and the image, video, audio and file bubbles use the copy saved on the device. Media with no saved copy shows "Media will be available when back online".
- With offline enabled, the composer restores attachments that were still uploading when the app closed, and follows uploads that resume.
- Added `CometChatEvents.offlineEvents`, with `CometChatOfflineEvent.MessageSendStateChanged` and `CometChatOfflineEvent.OfflineCacheReset`.
- Added `setHideOfflineNotices` (Kotlin) and `hideOfflineNotices` (Jetpack Compose) to `CometChatMessageList` and `CometChatConversations`, to hide the notices these lists show while offline.
- Added `CometChatOfflineRowStyle`, set with `setOfflineRowStyle` (Kotlin) or `offlineRowStyle` (Jetpack Compose) on `CometChatMessageList`, to style the "Not sent yet" and "Not sent. Tap to retry" lines.
- Added string resources for the offline states and notices, translated for every built-in language. Override them in your app's `res/values/strings.xml`.

## Enhancements
- Pinning, unpinning, saving or unsaving a message, and pinning or unpinning a conversation, now update the UI as soon as the action succeeds.

## Fixes
- Fixed the message list jumping to the bottom when older messages load.
- Fixed delivered and read ticks on a conversation's last message not updating when a receipt arrived for that message or a later one, in one-to-one and group chats.
- Fixed the conversation row time showing when the conversation was last updated instead of when its last message was sent.
- Fixed a message blocked by moderation showing as a normal last message in the conversation list after its send completed.
- Fixed a long moderation notice in the Jetpack Compose UI Kit stretching the text bubble. The text now wraps.
- Fixed the Jetpack Compose thread header showing an avatar on every parent message instead of following the message list's avatar rule.
- Fixed the Kotlin reaction list showing an empty sheet when it fails to load. It now shows its error text, or your custom error view, below the tabs.
- With offline enabled, the UI Kit now opens, downloads and shares only media files saved by the Chat SDK. A message link that names another file on the device is ignored.

## Breaking Changes
- The UI Kit now brings in Chat SDK 5.1.2. Chat SDK 5.1.2 no longer fires `onMessagePinned`, `onMessageUnpinned`, `onMessageSaved`, `onMessageUnsaved`, `onConversationPinned` or `onConversationUnpinned` on the device that made the change. The UI Kit already updates from the action's `onSuccess`. **If your app updates its own UI from these callbacks for the current device's actions, move that update into `onSuccess`.**
- If your unit tests stub `fetchNext(any())` or `fetchPrevious(any())` on Chat SDK request classes such as `ConversationsRequest`, give the matcher a type. Chat SDK 5.1.2 adds a second overload of these methods.

## v6.2.0 (2026-11-11)

# v6.2.0

## New
- Moved calling onto the Calls SDK 5.1 and the Push SDK 2.0 calls module. Incoming calls now ring through `push-notifications-calls-android`, including when the app is killed or the screen is locked, and the Push SDK handles busy state when the callee is already on a call. The UI Kit no longer uses any Chat SDK calling APIs.
- Added `UIKitSettings.overrideCallsHost` to point the Calls SDK at a staging or dedicated calls deployment. The UI Kit now forwards the admin, client and calls host overrides to the Calls SDK when it initialises it.
- Added `CometChatIncomingCallTheme` so the Push SDK's incoming-call screen follows the UI Kit theme in light and dark mode. Build it with `CometChatTheme.getIncomingCallTheme(context)` in the View UI Kit or `incomingCallTheme(light, dark)` in the Compose UI Kit, and pass it to the Push SDK through `PNConfiguration.Builder.setIncomingCallTheme()`. Apps that do nothing get UI Kit default colours through the `cometchat_pn_call_*` colour resources when the UI Kit is listed before the Push SDK artifacts in their dependencies; to follow the app's own theme, pass the theme as above.
- Added `CometChatOngoingCallActivity.launchOngoingCallActivity(context, call)` in both UI Kits, with optional `SessionSettingsBuilder` and `CometChatOngoingCallStyle` parameters, so an app's answer activity can open the call screen directly with an answered call.
- Added `CallRingingUtils` with `buildIncomingCall(...)` to construct a `Call` from a push payload, `decline(sessionId)` to decline a ringing call, and `permissionsToAnswer(call)` to list the runtime permissions a call needs.
- Added `CallInitiationUtils` with `initiateUserCall(...)`, `endCall(sessionId, reason)` and `isCallInProgress(throwable)` for apps that start or end calls from their own screens.
- Added `CometChatMessageComposer.setDisableSoundForMessages(Boolean)` to the View UI Kit to mute the outgoing message sound, matching the `disableSoundForMessages` parameter the Compose composer already had.
- Added `UIKitConstants.CallErrorCodes` with `ACTIVE_CALL`, `CALL_IN_PROGRESS` and `NO_INTERNET`, and `UIKitConstants.CallType` with `AUDIO`, `VIDEO` and `AUDIO_VIDEO`, so apps can tell these outcomes apart in `onError`.

## Enhancements
- Improved the call-logs call icon to call back by default in both UI Kits. Call-log errors now go to `onError` instead of replacing the list with an error state.
- Improved the outgoing call screen: End wins over a late answer and is disabled after the first tap, a cancel is capped at 10 seconds, the ring timer counts from when the call was placed, session timeouts are handled, and end reasons are normalised so `declined` shows as rejected and `timed_out` as unanswered.
- Improved "End & accept" on an ongoing call: the call screen switches to the new call in place, and leaving a call only tears down that call's own session and state.
- Improved logout to cancel an outgoing call that is still ringing and leave a joined call first, bounded at 3 seconds, so the other side does not keep ringing or talking to a user who has gone.
- Improved permission prompts: the UI Kit asks for the microphone and camera once, when the call screen is created. Voice calls ask for the microphone only. Call buttons, call logs and the outgoing screen no longer ask for anything.
- Improved 1:1 calls to end when the other participant leaves, by preparing them as peer calls on the Calls SDK.
- Improved call buttons to start exactly one call on a double tap and to ignore taps while a call is already being started.
- Improved the call screen to appear once the call is actually joined, instead of a blank grey view for several seconds after the callee answered.
- Improved offline handling: tapping a call button without a connection now fails at once with `NO_INTERNET`, and `CometChatCallButtons` shows a "No internet connection" toast when the app sets no `onError`. The message is translated in all 19 locales.
- Improved the Back button on Android 12 and later to keep the call alive in picture-in-picture instead of ending it.
- Improved call status labels in `CometChatCallActionBubble` and translated "ringing" in all supported locales.
- Improved the View UI Kit outgoing call title to stay on one line with an ellipsis for long names, matching Compose.


## Fixes
- Fixed the Join button on meeting call bubbles (`CometChatMeetCallBubble`, both UI Kits) doing nothing because the bubble read a different `sessionId` key than the one the UI Kit writes into the message.
- Fixed the Compose UI Kit not opening the outgoing call screen for a second call started from the same open chat.
- Fixed the call screen staying on "The session has ended" after the connection closed. It now closes.
- Fixed group and direct call joins failing with "The session was cancelled before it was joined" on Android 12 and later, caused by a race while handing over between call screens.
- Fixed the call button staying stuck in its loading state after a second call start was refused because another one was already in flight.
- Fixed voice calls requesting camera permission.
- Fixed multi-attachment sends from the composer tray playing no outgoing message sound. They now play it once per send, and `disableSoundForMessages` still silences it.

## Deprecations
- Deprecated the `CometChatIncomingCall` component and `CometChatIncomingCallStyle` in both UI Kits, together with `CometChatIncomingCallViewModel` and `CometChatIncomingCallViewModelFactory`. The Push SDK calls module now rings and draws the incoming call itself. Remove the component unless the app draws its own ring screen through `setIncomingCallActivity` and wires accept and decline to `CometChatCallNotifications`.
- Deprecated `CallManager` in `chatuikit-core`. It now delegates to `CallingState`, which has the same `getActiveCall`, `setActiveCall`, `isActiveMeeting` and `setIsActiveMeeting` API, and it will be removed in a later release. For the Calls SDK's own view of an active session use `CometChatCalls.getActiveSession()`, or `CallButtonsDataSource.hasActiveCallSession()` from a custom data source.
- Deprecated `CometChatCallActivity.launchIncomingCallScreen` in both UI Kits for the same reason. Open the call screen from the app's answer activity with `CometChatOngoingCallActivity.launchOngoingCallActivity(context, call)` instead. `CometChatCallActivity` itself and its `launchOutgoingCallScreen` / `launchConferenceCallScreen` are unchanged.

## Removals
- Removed the Chat SDK call listeners that `CallManager` registered. `CallManager` itself stays for one release as a deprecated alias that delegates to `CallingState` (see Deprecations).

## Breaking Changes
- Incoming calls no longer ring through the Chat SDK. To receive calls, add `com.cometchat:push-notifications-calls-android:2.0.0`, initialise the Push SDK with the app's push token, and register an answer activity that reads the call with `CallBundles.getCall`, builds it with `CallRingingUtils.buildIncomingCall(...)` and opens `CometChatOngoingCallActivity.launchOngoingCallActivity(context, call)`. Apps that still rely on the `CometChatIncomingCall` component alone will **not** ring.
- Upgraded the Calls SDK to 5.1.0. It stays a `compileOnly` dependency of the UI Kit, as in 6.1, so a chat-only app ships none of its permissions, foreground-service types or WebRTC libraries. An app that uses calling must declare `com.cometchat:calls-sdk-android:5.1.0` itself and replace any pin to 5.0.x. Apps using `push-notifications-android` must move from 1.0.0 to 2.0.0.
- Binary-incompatible changes that a recompile absorbs but prebuilt binaries will not: the synthetic `UIKitSettings` constructor gained the `overrideCallsHost` field (use `UIKitSettingsBuilder`), the deprecated Compose `CometChatIncomingCall` gained the `onAcceptRequested` parameter with no old overload, and `CometchatOngoingCallScreenBinding.getRoot()` in the View UI Kit now returns a `FrameLayout` instead of a `LinearLayout`.
- Remove any direct `CometChatCalls.init(...)` call from the app. The UI Kit initialises the Calls SDK with the hosts from `UIKitSettings`, and the Calls SDK keeps the hosts of its first initialisation, so a second init with different hosts is ignored.
- Changed the signature of `initiateUserCall` on `CallButtonsDataSource`, `CallButtonsRepository` and `InitiateUserCallUseCase` to take the callee `User`. Custom implementations must add the parameter.
- Chat SDK 5.1.x adds typed `*FetchListener` overloads of `fetchNext` and `fetchPrevious` on every request builder and deprecates the `CallbackListener` ones. Kotlin or Mockito code that calls or stubs `fetchNext(any())` / `fetchPrevious(any())` now fails to compile with an ambiguous overload; name the listener type (for example `any<CometChat.CallbackListener<List<BaseMessage>>>()`).

## Dependencies
- **Chat SDK → 5.1.1** — Bundled `com.cometchat:chat-sdk-android` updated from 5.0.7 to 5.1.1. Every integrator gets it transitively through `chatuikit-core`. 5.1.1 also deprecates the Chat SDK calling APIs, which the UI Kit no longer uses.

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

