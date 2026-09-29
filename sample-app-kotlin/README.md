# CometChat Sample App (Kotlin - XML Views)

A sample Android application demonstrating the CometChat UI Kit with XML Views and ViewBinding. This app showcases core chat features including conversations, messaging, user management, and group functionality.

## Features

- **User Authentication**: Login with sample users or custom UID
- **Conversations**: View and manage chat conversations
- **One-on-One Messaging**: Send text, images, videos, audio, and files
- **Group Messaging**: Create and participate in group chats
- **User Management**: Browse users, view profiles, block/unblock
- **Group Management**: View group details, manage members (for admins)
- **Real-time Updates**: Live message delivery and read receipts
- **Theme Customization**: Customizable colors and styling

## Prerequisites

- Android Studio Arctic Fox (2020.3.1) or later
- Android SDK 28 (Android 9.0) or higher
- Kotlin 1.9+
- CometChat Account with App ID, Region, and Auth Key

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd <repository-name>
```

### 2. Configure CometChat Credentials

Open `sample-app-kotlin/src/main/java/com/cometchat/sampleapp/kotlin/utils/AppConstants.kt` and update:

```kotlin
object AppConstants {
    const val APP_ID = "YOUR_APP_ID"
    const val REGION = "YOUR_REGION"  // e.g., "us", "eu"
    const val AUTH_KEY = "YOUR_AUTH_KEY"
}
```

> **Note**: You can obtain these credentials from the [CometChat Dashboard](https://app.cometchat.com/).

### 3. Build and Run

```bash
# Build the project
./gradlew :sample-app-kotlin:assembleDebug

# Install on connected device
./gradlew :sample-app-kotlin:installDebug
```

Or open the project in Android Studio and run the `sample-app-kotlin` configuration.

## Project Structure

```
sample-app-kotlin/
├── src/main/java/com/cometchat/sampleapp/kotlin/
│   ├── app/
│   │   └── SampleApplication.kt      # Application class with SDK initialization
│   ├── ui/
│   │   ├── login/
│   │   │   ├── LoginActivity.kt      # Login screen
│   │   │   └── LoginViewModel.kt     # Login state management
│   │   ├── home/
│   │   │   └── HomeActivity.kt       # Main screen with bottom navigation
│   │   ├── conversations/
│   │   │   └── ConversationsFragment.kt  # Chats tab
│   │   ├── users/
│   │   │   ├── UsersFragment.kt      # Users tab
│   │   │   └── UserDetailsActivity.kt # User profile screen
│   │   ├── groups/
│   │   │   ├── GroupsFragment.kt     # Groups tab
│   │   │   └── GroupDetailsActivity.kt # Group details screen
│   │   └── messages/
│   │       ├── MessagesActivity.kt   # Chat screen
│   │       └── ThreadMessagesActivity.kt # Thread replies
│   └── utils/
│       ├── AppConstants.kt           # App configuration
│       └── AppPreferences.kt         # SharedPreferences wrapper
├── src/main/res/
│   ├── layout/                       # XML layouts
│   ├── values/
│   │   ├── colors.xml               # Color definitions
│   │   ├── themes.xml               # Theme configuration
│   │   └── strings.xml              # String resources
│   └── values-night/
│       └── colors.xml               # Dark mode colors
└── build.gradle.kts                  # Module build configuration
```

## Architecture

This sample app follows the **MVVM (Model-View-ViewModel)** architecture pattern:

- **View**: Activities and Fragments using XML layouts with ViewBinding
- **ViewModel**: Manages UI state and business logic
- **Model**: CometChat SDK models (User, Group, Message, etc.)

### Key Components

| Component | Description |
|-----------|-------------|
| `SampleApplication` | Initializes CometChat SDK on app startup |
| `LoginActivity` | Handles user authentication |
| `HomeActivity` | Main navigation with bottom tabs |
| `MessagesActivity` | Chat interface with message list and composer |
| `UserDetailsActivity` | User profile with block/unblock functionality |
| `GroupDetailsActivity` | Group info with member management |

## CometChat UI Kit Components Used

| Screen | Component |
|--------|-----------|
| Conversations | `CometChatConversations` |
| Users | `CometChatUsers` |
| Groups | `CometChatGroups` |
| Messages | `CometChatMessageHeader`, `CometChatMessageList`, `CometChatMessageComposer` |
| Group Members | `CometChatGroupMembers` |

## Theme Customization

### Changing Colors

Edit `src/main/res/values/colors.xml`:

```xml
<!-- Primary brand color -->
<color name="primary">#6851D6</color>
<color name="primary_variant">#5A45C0</color>
```

### Dark Mode Support

Dark mode colors are defined in `src/main/res/values-night/colors.xml`. The app automatically switches themes based on system settings.

### CometChat Theme

For programmatic theme customization, see the comments in `themes.xml` for examples of using `CometChatTheme`.

## Permissions

The app requests the following permissions:

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Network communication |
| `CAMERA` | Taking photos for messages |
| `RECORD_AUDIO` | Voice messages |
| `READ_MEDIA_*` | Accessing media files |
| `POST_NOTIFICATIONS` | Push notifications (Android 13+) |

## Dependencies

- CometChat Chat SDK
- CometChat UI Kit (chatuikit-kotlin, chatuikit-core)
- AndroidX (AppCompat, ConstraintLayout, Navigation, Lifecycle)
- Material Design Components
- Kotlin Coroutines

## Troubleshooting

### Build Errors

1. Ensure you have the correct Android SDK version installed
2. Run `./gradlew clean` and rebuild
3. Check that all dependencies are resolved

### Login Issues

1. Verify your CometChat credentials are correct
2. Check network connectivity
3. Ensure the user UID exists in your CometChat dashboard

### UI Kit Issues

1. Make sure CometChat SDK is initialized before using UI components
2. Check that the user is logged in before accessing chat features

## License

This sample app is provided for demonstration purposes under the same licence as
the rest of this repository — see [LICENSE](../LICENSE).

## Support

For CometChat SDK support, visit:
- [CometChat Documentation](https://www.cometchat.com/docs)
- [CometChat Dashboard](https://app.cometchat.com/)
