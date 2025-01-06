
<p align="center">
  <img alt="CometChat" src="https://assets.cometchat.io/website/images/logos/banner.png">
</p>

# Android Sample App for Java by CometChat

This is a reference application showcasing the integration of [CometChat's Android UI Kit](https://www.cometchat.com/docs/ui-kit/android/5.0/overview) in a native Android application. It demonstrates how to implement real-time messaging and voice/video calling features with ease.

<div style="display: flex; align-items: center; justify-content: center">
   <img src="../screenshots/overview_cometchat_screens.png" />
</div>

## Prerequisites

- Ensure you have the following installed:
   - Android Studio (latest stable version)
   - Java Development Kit (JDK 8 or later)

- Sign up for a [CometChat](https://app.cometchat.com/) account to obtain your app credentials: _`App ID`_, _`Region`_, and _`Auth Key`_

## Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/cometchat/cometchat-uikit-android.git
   ```

2. Checkout v5 branch:
   ```sh
   git checkout v5
   ```

3. Open the project in Android Studio.

4. Sync Gradle to ensure all dependencies are downloaded.

5. `[Optional]` Enter your CometChat _`App ID`_, _`Region`_, and _`Auth Key`_ in the [AppCredentials.java](src/main/java/com/cometchat/sampleapp/java/AppCredentials.java) file:
   file:https://github.com/cometchat/cometchat-uikit-android/blob/b7b7c0d76eb70960728e6622ed7f70ab4e45b4af/sample-app-java/src/main/java/com/cometchat/sampleapp/java/AppCredentials.java#L3-L7

6. In the Android Studio toolbar, select the `sample-app-java` module from the module dropdown and run the project using an emulator or a physical device 


## Help and Support

For issues running the project or integrating with our UI Kits, consult our [documentation](https://www.cometchat.com/docs/ui-kit/android/5.0/getting-started) or create a [support ticket](https://help.cometchat.com/hc/en-us). You can also access real-time support via the [CometChat Dashboard](http://app.cometchat.com/).
