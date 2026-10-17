<div align="center">

# 📖 My Daily Diary

**A beautiful, private diary for Android — pages you can decorate, memories you can keep, and sync that stays in your own Google Drive.**

[![Download APK](https://img.shields.io/badge/⬇_Download-APK-E8806A?style=for-the-badge&logo=android&logoColor=white)](https://github.com/amoldalwai/MyDailyDiary/releases/latest/download/MyDailyDiary.apk)
[![Latest release](https://img.shields.io/github/v/release/amoldalwai/MyDailyDiary?style=for-the-badge&color=C9566B&label=version)](https://github.com/amoldalwai/MyDailyDiary/releases/latest)
![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)

</div>

---

## ✨ Why you'll like it

| | |
|---|---|
| 🗒️ **Real diary feel** | Ruled, dotted or grid pages, margin line, binder holes, taped polaroid photos, page-turn animation. |
| 🎨 **Make every page yours** | 12 page colours and gradients, 6 fonts, text size, line style and line colour — saved per entry. |
| 📷 **Memories with photos** | Add photos, draw by hand, drop in stickers, dictate with your voice. |
| ✍️ **Rich writing** | Bold and italic, moods, tags, and a date picker for back-dated entries. |
| 🔎 **Find anything** | Search text, tags and moods. Month calendar with mood markers. |
| 📊 **Insights** | Streaks, words written, mood breakdown, weekly activity. |
| 🔔 **Gentle habits** | Daily reminder that skips days you've already written. "On this day" memories. |
| 🔒 **Private by default** | Optional PIN and fingerprint lock. Everything is stored on your phone. |
| ☁️ **Your Drive, your data** | Optional Google Drive sync into a private app folder only this app can open. |
| 📄 **Export** | Share any entry, a month, or the whole diary as a PDF. |

## ⬇️ Install

1. Tap **Download APK** above on your Android phone.
2. Open the downloaded file. If Android asks, allow **Install unknown apps** for your browser.
3. Open **Diary** and start writing.

> Requires **Android 12 or newer**.

## ☁️ Google Drive sync (optional)

Open **Settings → Google Drive sync → Connect Google account**. Entries and photos are copied to a hidden folder in *your own* Google Drive (the `drive.appdata` scope) — nobody else, including the developer, can read them. Sign in on a second phone with the same account and your diary appears there too.

## 🔐 Privacy

- No ads, no analytics, no tracking.
- Entries and photos live in the app's private storage on your phone.
- Sync, if you turn it on, goes straight between your phone and your own Google Drive.

## 🛠️ Build from source

```powershell
git clone https://github.com/amoldalwai/MyDailyDiary.git
cd MyDailyDiary
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. You need JDK 17 and the Android SDK (platform 35).

Google sign-in only works for builds signed with a key registered in your own Google Cloud project, so create an Android OAuth client with the package name `com.amoldalwai.diary` and your signing key's SHA-1 if you want sync in your own build.

## 🧱 Built with

Kotlin · Jetpack Compose · Material 3 · SQLite · Coil · Google Identity Authorization · Drive REST API

---

<div align="center">Made with ❤️ for the days worth remembering.</div>
