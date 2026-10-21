# YBA Helper (Android)

A small native Kotlin Android companion app for Your Bizarre Adventure (Roblox).

## Features
- Dark-themed home screen and settings
- Detects whether the Roblox Android app is installed
- Opens the official YBA experience page (place ID `2809202155`) using Roblox's Android app link handling
- Optional Usage Access-based foreground-app check. Android does not expose the exact Roblox sub-place to third-party apps, so this cannot reliably confirm that YBA itself is open.

## Build locally
1. Install Android Studio and Android SDK Platform 35 (JDK 17 recommended).
2. Open this folder as an existing project and let Gradle sync.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The debug APK will be under `app/build/outputs/apk/debug/`.

## Build using GitHub Actions
This repository includes `.github/workflows/build-apk.yml`.
1. Upload this project folder to a GitHub repository.
2. Open the repository's **Actions** tab and run **Build YBA Helper APK** (or push to `main`/`master`).
3. Download the `YBA-Helper-debug-apk` artifact from the completed workflow run.

The current environment does not have Android SDK or Gradle installed, so it cannot compile the APK directly here. This archive contains the source project and a cloud-build workflow, not a compiled APK.
