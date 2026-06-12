# Breezy 📱

A social photo-sharing Android app with real-time chat, built in Kotlin with Jetpack Compose and Supabase.

## Demo 🎬

[Watch the demo on YouTube](https://youtu.be/sy_FtfOybgk)

## Overview 📝

Breezy is a full-featured social media app: users sign up, build a profile, share
photos taken in-app or picked from their gallery, follow other users, like and
comment on posts, search for people and content, track activity, and chat in
real time. It is backed by Supabase for authentication, a Postgres database,
file storage, and live updates over WebSockets.

## Features ✨

* **Authentication & profiles** — email sign-up and login, profile setup, editing, and follower/following lists.
* **Photo sharing** — capture a photo in-app with the camera or select one from device media, then post it with a caption.
* **Feed & interactions** — a home feed of posts with likes and comments.
* **Real-time chat** — direct message channels that update live using a publisher/subscriber pattern over Supabase Realtime.
* **Search** — find users and posts.
* **Activity** — track follows, likes, and comments.

## Tech Stack 🛠️

* **Kotlin** — primary language (JDK 11, min SDK 26, target SDK 35).
* **Jetpack Compose** — declarative UI, including `HorizontalPager` for swipeable views and Material 3 theming with light/dark support.
* **Navigation Compose** — screen navigation, back stack, and bottom-bar visibility logic.
* **MVVM** — `ViewModel` + Kotlin coroutines and `StateFlow` for lifecycle-aware, reactive UI state.
* **Supabase** (via [`supabase-kotlin`](https://github.com/supabase-community/supabase-kt)) — Auth, Postgres, Realtime, and Storage.
* **Ktor** (`ktor-client-cio`) — HTTP/WebSocket client powering Supabase Realtime.
* **kotlinx.serialization** — JSON serialization for API models.
* **CameraX** (`camera-camera2`, `camera-lifecycle`, `camera-view`) — in-app photo capture.
* **Coil 3** (`coil-compose`, `coil-network-okhttp`) — image loading with placeholders, error states, and loading indicators.

## Architecture 🧩

The app follows an MVVM structure:

* `ui/screens` — Compose screens for each route (home, search, camera, media, profile, channels, messages, etc.).
* `components` — reusable Compose components (avatars, post rows, comment sheets, message items, etc.).
* `viewmodels` — `ViewModel`s exposing UI state as `StateFlow`.
* `api` — `BreezyAPI` Supabase client plus the data models (`Post`, `User`, `Comment`, `Message`, `Channel`).

Navigation is centered on a bottom navigation bar across the primary screens —
Home 🏠, Search 🔍, Camera 📸, Activity 👣, and Profile 👤.

## Running Locally 🚀

1. Clone the repo and open it in Android Studio (a recent version that supports
   AGP 8.9 / Kotlin 2.0+).
2. Create a `local.properties` file in the project root pointing to your Android
   SDK (Android Studio creates this automatically on first sync):

   ```properties
   sdk.dir=/path/to/your/Android/sdk
   ```

3. Configure Supabase credentials. The app reads two string resources —
   `supabase_url` and `supabase_api_key` — from
   `app/src/main/res/values/supabase_api.xml`:

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <resources>
       <string name="supabase_api_key" translatable="false">YOUR_SUPABASE_ANON_KEY</string>
       <string name="supabase_url" translatable="false">https://YOUR_PROJECT.supabase.co</string>
   </resources>
   ```

   To run against your own backend you'll need a Supabase project with the
   matching schema (users, posts, comments, channels, messages, follows) and
   Storage buckets, with Row Level Security policies configured.
4. Build and run on an emulator or device (min SDK 26 / Android 8.0+).

## Screenshots 🖼️

Screenshots are stored in Google Drive: [Breezy Screenshots](https://drive.google.com/drive/folders/15K-SGrtwg_Y9wsdivy9ir7dZCvTi9Bpm?usp=sharing)

## Provenance

Built as a final project for the Mobile Computing course at UT Austin. It was a
collaborative project built with a partner; work was shared across the codebase
rather than split by feature, so contributions are spread throughout the app.
</content>
</invoke>
