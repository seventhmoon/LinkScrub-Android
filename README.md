# LinkScrub 🧼

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="LinkScrub App Icon" width="128" height="128" />
</p>

<p align="center">
  <strong>Clean, Private, and Intelligent URL Sanitizer for Android</strong><br>
  <em>Strip tracking parameters, unshorten redirect hops, preview link metadata, and share cleanly — 100% on-device.</em>
</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin Version" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.02.00-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://m3.material.io"><img src="https://img.shields.io/badge/Material%20Design-3%20Expressive-6750A4?logo=materialdesign&logoColor=white" alt="Material Design 3" /></a>
  <a href="https://developer.android.com/about/versions/15"><img src="https://img.shields.io/badge/Android-API%2028%2B%20(Target%2037)-3DDC84?logo=android&logoColor=white" alt="Android Version" /></a>
  <a href="https://play.google.com/store/apps/details?id=com.androidfung.linkscrub"><img src="https://img.shields.io/badge/Google_Play-LinkScrub-414141?logo=google-play&logoColor=white" alt="Get it on Google Play" /></a>
  <a href="https://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
  <a href="https://developer.android.com/guide/topics/large-screens/adaptive-layouts"><img src="https://img.shields.io/badge/Adaptive-Dual--Pane%20(SupportingPaneScaffold)-00C853" alt="Adaptive Ready" /></a>
  <a href="#privacy-commitment"><img src="https://img.shields.io/badge/Privacy-100%25%20On--Device-success" alt="Privacy First" /></a>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.androidfung.linkscrub">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" width="220" />
  </a>
</p>

---

## 📖 Overview

In modern internet browsing, URLs shared across social platforms, messaging apps, and email newsletters are heavily contaminated with tracking identifiers (`utm_*`, `fbclid`, `gclid`, `gbraid`, `igsh`, TikTok telemetry, Amazon session tags, and affiliate redirections). These tokens compromise user privacy by correlating identity across websites, bloating URLs into unreadable walls of text, and masking target destinations through shortener redirects (`bit.ly`, `t.co`, etc.).

**LinkScrub** is an open-source, privacy-first Android utility inspired by projects like *Leon* and *Felis*. It intercepts, de-obfuscates, cleans, and previews URLs with full transparency:
- **No telemetry, zero cloud tracking, 100% local processing.**
- **Fine-grained parameter control** with domain-aware rule preservation (such as preserving YouTube timestamps).
- **Network unshortening** with cycle loop guards.
- **Rich Open Graph previews** without browser bloat.

---

## ✨ Key Features

### 1. 🧙 Guided 3-Step URL Sanitization Flow
A smooth, animated wizard guides users step-by-step:
1. **Input**: Paste raw text or URL via quick paste button, clipboard auto-detection, or intent interception.
2. **Review & Scrub**: Inspect individual query parameters grouped by category (Tracking tokens highlighted vs. Safe query params). Toggle parameters on/off with interactive FilterChips before committing.
3. **Preview & Share**: Inspect the clean destination URL, Open Graph card, and copy or dispatch immediately to other apps via the system share sheet.

### 2. ⚡ Quick-Clean Mode
Need fast scrubbing without extra taps?
- One-tap auto-clean action that sanitizes the URL and copies it straight to your clipboard with haptic feedback.
- Configurable auto-copy options for rapid link sharing workflows.

### 3. 🎯 Intelligent Parameter Stripping & Domain-Aware Rules
- **Comprehensive Tracking Catalog**: Built-in rules strip UTM parameters (`utm_source`, `utm_campaign`, etc.), Google Ads (`gclid`, `wbraid`, `gbraid`, `dclid`, `gad_source`), Meta/Facebook/Instagram (`fbclid`, `igsh`, `igshid`), Twitter/X (`twclid`, `s`, `t`), TikTok/ByteDance (`_t`, `_r`, `tt_from`), Hubspot, Matomo, and general affiliate click markers.
- **Domain-Aware Intelligence**:
  - **YouTube / YouTube Shorts / youtu.be**: Preserves the video playback timestamp (`t=...`) while stripping tracking markers like `si` and `feature`.
  - **Amazon**: Cleans deep path-based referral links (`/ref=...`) and session tags (`pf_rd_*`, `pd_rd_*`, `tag`) while preserving core ASIN product paths.
  - **Regex Fallbacks & Wildcards**: Matches custom tracking prefixes (`utm_*`, `itm_*`, `at_*`, `vero_*`).

### 4. 🔗 HTTP Redirect Unshortener & Cyclic Loop Protection
- Resolves shortened links (`bit.ly`, `t.co`, `tinyurl.com`, etc.) up to configurable hops.
- Efficiently queries endpoints using lightweight `HEAD` requests with automatic graceful fallback to streaming `GET`.
- **Cyclic Loop Detection**: Tracks visited redirect nodes to prevent infinite redirection loops and denial-of-service traps.

### 5. 🖼️ Rich Open Graph & Twitter Card Preview
- Extracts Open Graph (`og:title`, `og:description`, `og:image`, `og:site_name`) and Twitter Card metadata using Jsoup.
- Memory-efficient streaming with Coil Compose image rendering, shimmer placeholder animations, and error fallbacks.
- Verified destination preview so you can preview where a link leads before opening it in a browser.

### 6. 📲 Android System Integration & Intent Interception
- **Share Sheet Receiver (`ACTION_SEND`)**: Select LinkScrub from any app's share menu (WhatsApp, Slack, Telegram, Twitter, Chrome) to clean links in-place.
- **Browser Interception (`ACTION_VIEW`)**: Optionally set LinkScrub as a handler for HTTP/HTTPS links to scrub URLs automatically before opening your browser.
- **Text-in-Bulk Extraction**: Regex-based URL parser extracts and scrubs URLs embedded inside longer conversational text blocks.

### 7. 📱 Material Design 3 Adaptive Dual-Pane Layout
- Powered by **Material 3 Expressive** and `androidx.compose.material3.adaptive.layout.SupportingPaneScaffold`.
- **Phone (Compact)**: Focused, ergonomic single-pane layout with bottom-sheet access to history and smooth horizontal step transitions.
- **Foldables & Tablets (Expanded / Dual-Pane)**: 50:50 side-by-side split pane with live scrubber controls on the primary pane and searchable local history on the supporting pane.
- Full **Edge-to-Edge** display with predictive navigation, dynamic wallpaper color theming (Android 12+), and contrast accessibility.

### 8. 🗄️ Local History & Search
- Backed by **Room Database** (SQLite) with zero cloud sync.
- Search past cleaned links by URL, domain, or title.
- One-tap re-copy, re-clean, or clear history anytime.

---

## 🏛️ Architecture & Tech Stack

LinkScrub is engineered following modern Android development practices, MVVM architecture, and Unidirectional Data Flow (UDF).

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  Jetpack Compose + Material 3 Expressive + Adaptive Scaffold │
│          LinkScrubScreen  ◄──►  LinkScrubViewModel          │
└──────────────────────────────▲──────────────────────────────┘
                               │ UI State / User Intents
┌──────────────────────────────┴──────────────────────────────┐
│                       Domain Layer                          │
│        CleanUrlUseCase        │         UrlCleaner          │
│   (Domain-Aware Rules, Parameter Blocklists, Text Parser)   │
└──────────────────────────────▲──────────────────────────────┘
                               │
┌──────────────────────────────┴──────────────────────────────┐
│                        Data Layer                           │
│   OkHttpRedirectUnshortener   │      OpenGraphScraper       │
│      (Redirect Resolution)    │    (Jsoup Metadata Scraper) │
│               HistoryRepository & Room Database             │
└─────────────────────────────────────────────────────────────┘
```

| Component | Technology | Role |
|---|---|---|
| **Language** | [Kotlin 2.0.21](https://kotlinlang.org) | Modern concise, type-safe development |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) | Declarative UI toolkit |
| **Design System** | [Material Design 3 (M3 Expressive)](https://m3.material.io) | Modern shapes, dynamic colors, elevation, and typography |
| **Adaptive Layouts** | `androidx.compose.material3.adaptive` | Responsive dual-pane layouts for foldables & tablets |
| **Concurrency** | Kotlin Coroutines & `StateFlow` | Asynchronous, reactive state management |
| **Local Storage** | [Room](https://developer.android.com/training/data-storage/room) | Offline-first SQLite persistence for cleaned links |
| **Networking** | [OkHttp](https://square.github.io/okhttp/) | High-performance redirect unshortening & HTTP queries |
| **HTML Parsing** | [Jsoup](https://jsoup.org/) | Robust, memory-safe Open Graph & Twitter Card metadata extraction |
| **Image Loading** | [Coil Compose](https://coil-kt.github.io/coil/compose/) | Asynchronous image loading for link previews |

---

## 🔒 Privacy Commitment

LinkScrub is built on one foundational principle: **your data belongs exclusively to you.**

- **0% Analytics / 0% Telemetry**: No Firebase Analytics, No Mixpanel, No Sentry, No advertising SDKs.
- **100% On-Device Processing**: Link stripping and regular expressions run entirely on your CPU.
- **Transparent Network Requests**: The network is accessed solely when:
  1. Resolving a redirect chain (HTTP `HEAD`/`GET` to the URL target).
  2. Fetching Open Graph metadata (HTML `GET` and image thumbnail).
- **No Remote Database**: Cleaning history is saved locally in a private SQLite database via Room and never leaves your device. You can purge history at any moment with a single tap.

---

## 🚀 Getting Started & Building

### Prerequisites

- **Android Studio**: Android Studio Ladybug (2024.2.1+) or Meerkat (2024.3.1+)
- **JDK**: Java Development Kit 17 or higher
- **Android SDK**: Compile SDK 35 / 37, Min SDK 28 (Android 9.0+)

### Clone the Repository

```bash
git clone https://github.com/<your-username>/LinkScrub.git
cd LinkScrub
```

### Build & Run via Gradle

- **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```

- **Run Unit Tests:**
  ```bash
  ./gradlew testDebugUnitTest
  ```

- **Run Instrumented UI & Device Tests:**
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

---

## 🧪 Testing

The project maintains comprehensive test coverage across all layers:
- **Unit Tests**:
  - `UrlCleanerTest`: Validates stripping of UTM tags, Google Ads, TikTok, Meta parameters, and domain-specific rules (e.g., YouTube timestamp retention, Amazon clean paths).
  - `CleanUrlUseCaseTest`: Validates end-to-end clean execution.
  - `RedirectUnshortenerTest`: Validates redirection hop tracking and cyclic loop termination.
  - `OpenGraphScraperTest`: Tests robust HTML parsing of Open Graph and Twitter Card tags.
  - `LinkScrubViewModelTest`: Tests UDF state transitions across Wizard steps.
- **Instrumentation & UI Tests**:
  - `LinkScrubUiTest`: End-to-end Compose UI test verifying wizard interactions, chips, and responsive behavior.

---

## 🤝 Contributing

Contributions to LinkScrub are welcome!
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/NewTrackingRule`).
3. Commit your Changes (`git commit -m 'Add tracking rule for NewService'`).
4. Push to the Branch (`git push origin feature/NewTrackingRule`).
5. Open a Pull Request.

Please ensure all tests pass (`./gradlew testDebugUnitTest`) before submitting a PR.

---

## 📄 License

LinkScrub is distributed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for more information.

```text
Copyright 2025 LinkScrub Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
