# PlainText

PlainText is a deliberately minimal Android plain-text editor.

It exists to make opening, editing, and saving a text file feel immediate and unobtrusive. The project intentionally avoids advertisements, analytics, accounts, cloud services, and unnecessary permissions.

## Current status

This repository contains the initial Android project skeleton:

- Kotlin
- Jetpack Compose
- A basic application entry point
- A placeholder editor screen with Open and Save actions

File open/save behavior through Android's Storage Access Framework is the next implementation step.

## Build

```sh
./gradlew assembleDebug
```


## APK artifact

A debug APK can be generated from GitHub Actions by manually running the `Build APK` workflow. The completed workflow uploads the debug APK as an artifact.
