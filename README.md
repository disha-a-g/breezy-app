# Breezy 📱 - Social Media Application

## Project Description 📝

Breezy is a full-featured social media app with a sleek user interface and a robust backend to support real-time messaging 💬, dynamic content feeds 🖼️, user and post search 🔍, activity tracking 👣, and customizable profile pages 👤.

## Project Repository 📂

[Breezy Android](https://github.com/WhirlyFan/breezy-android.git)

## Project Demo 🎬

[Breezy Demo](https://youtu.be/sy_FtfOybgk)

## Screenshots 🖼️

Screenshots are stored in Google Drive for organization: [Breezy Screenshots](https://drive.google.com/drive/folders/15K-SGrtwg_Y9wsdivy9ir7dZCvTi9Bpm?usp=sharing)

## Major Functionalities ✨

* Custom backend ⚙️
* Live Chat 💬 (using a publisher/subscriber pattern for live updates)
* Complex Layout 🧩
* Mutable shared state 🔄

## Team Members 🧑‍🤝‍🧑

* Michael Lee
* Disha A

## APIs Used 🛠️

* **Supabase:**
    * Auth 🔑
    * Postgres 🗄️
    * Realtime ⏱️
    * Storage 📦

## Android Features Used 🤖

* Bottom navigation action bar for navigation across primary screens (Home 🏠, Search 🔍, Media 🖼️, Activity 👣, and Profile 👤).
* Jetpack Compose's Navigation component for state navigation, back stack entries, and visibility logic.
* Camera integration 📸 for photo capturing.

## Third-Party Libraries Used 📚

* **Coil3:** For image loading and display, including smooth transitions, error handling with placeholders, and loading indicators. Includes `coil-network-okhttp` for loading images from network sources.
* **Jetpack Compose:** For building the app's UI with reusable composable functions. Used `HorizontalPager` for swipeable dual-page views.
* **ktor-client-cio:** For real-time messaging from Supabase using WebSockets.
* **kotlinx.serialization:** For easier data handling and communication with the backend API.

## Third-Party Services Used ☁️

* **Supabase:** A PostgreSQL cloud-hosted relational database for backend tasks. Used `Supabase-kotlin`.
