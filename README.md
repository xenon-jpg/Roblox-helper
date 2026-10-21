# YBA Helper (Android)

A small native Kotlin Android companion app for Your Bizarre Adventure (Roblox).

## Features
- Dark-themed home screen and settings
- Detects whether the Roblox Android app is installed
- Uses Roblox deep link `roblox://experiences/start?placeId=2809202155` to request joining the YBA experience (rather than opening only the website page)
- Optional Usage Access-based foreground-app check.
- Optional foreground-service auto-return mode: after Roblox was observed in the foreground, if it stays away for about 12 seconds, the app makes one best-effort attempt to request launching into the YBA experience via a Roblox deep link and exposes a notification tap as a fallback. Duplicate launches are suppressed until Roblox is observed foreground again.
- Android may block activity launches from the background and can stop services; auto-return is best-effort, not guaranteed. This cannot reliably confirm the exact Roblox sub-place.

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
