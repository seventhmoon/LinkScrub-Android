# Google Play Store Listing & Data Safety Metadata

Use this document to copy & paste metadata directly into the **Google Play Console**.

---

## 1. Store Listing Details

### App Name (Max 30 characters)
```text
LinkScrub: Clean URL Sanitizer
```
*(Alternative: `LinkScrub - Clean & Share Links`)*

### Short Description (Max 80 characters)
```text
Strip tracking parameters, unshorten redirects, and share clean URLs safely.
```

### Full Description (Max 4000 characters)
```text
Clean, Private, and Intelligent URL Sanitizer for Android.

Have you ever shared a link from Amazon, Twitter/X, Instagram, or TikTok and noticed how ridiculously long and messy it is? Modern links are loaded with hidden tracking tokens (UTM parameters, fbclid, gclid, igsh, session tokens, and affiliate tags) that correlate your online activity, profile your identity across websites, and clutter your shared messages.

LinkScrub strips the noise and gives you clean, human-readable, tracking-free links in seconds.

🧼 KEY FEATURES

• 100% On-Device Processing
LinkScrub does not track you, does not log your browsing habits, and does not send your links to cloud servers. All parameter stripping and rule matching happen entirely locally on your device.

• Guided 3-Step Sanitization Wizard
1. Input: Paste or share a link straight from any app via the Android Share sheet.
2. Review & Scrub: View detected tracking parameters categorized with clear tags. Toggle individual parameters on or off with interactive chips.
3. Preview & Share: Verify the final clean destination, view rich Open Graph metadata cards, and copy or share directly to your favorite apps.

• Quick-Clean Mode
In a rush? Clean and copy links directly with a single tap, with built-in haptic confirmation.

• Unshorten Redirect Chains
Reveal where suspicious short links (bit.ly, t.co, tinyurl, and custom shorteners) actually lead before opening them. LinkScrub resolves redirects directly on-device with built-in loop detection.

• Domain-Aware Intelligent Preservation
Intelligently preserves necessary parameters—such as YouTube video start timestamps (t=...) and playlist IDs—while purging tracking garbage.

• Clean QR Code Scanner
Scan QR codes containing URLs quickly and securely using Google Play Services without needing to grant camera permissions to the app.

• Modern Material 3 & Adaptive Design
Features dynamic Material You color theming, edge-to-edge aesthetics, and dual-pane adaptive layouts optimized for phones, foldables, and tablets.

Say goodbye to tracking bloat and messy links. Keep your browsing private with LinkScrub!
```

---

## 2. Play Console Declarations

### Ads Declaration
- **Question:** Does your app contain advertisements?
- **Answer:** **Yes** (Contains ads via Google AdMob).

### Target Audience & Content Rating
- **Target Age:** 13+ (or 18+).

### App Access
- **Answer:** All functionality is available without special access restrictions (no demo account required).

### Advertising ID Declaration
- **Question:** Does your app use Advertising ID?
- **Answer:** **Yes** (Used by Google Mobile Ads SDK for advertising and analytics).

---

## 3. Play Store Data Safety Questionnaire Guide

Fill out the Google Play Data Safety form with the following answers:

| Data Type | Collected? | Shared? | Purpose | Ephemeral? | Required / Optional? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Location (Approximate location)** | Yes (by AdMob via IP) | Yes (with Google) | Advertising, Analytics | Yes | Required |
| **Device or other IDs (Advertising ID / AAID)** | Yes | Yes (with Google) | Advertising, Analytics, Fraud Prevention | No | Required |
| **App info and performance (Crash logs)** | Yes | No (Processed by Crashlytics) | App functionality, Analytics | No | Required |
| **App info and performance (Diagnostics / Performance)** | Yes | No (Processed by Firebase Perf) | App functionality, Analytics | No | Required |

- **Is all of the user data collected by your app encrypted in transit?** → **Yes** (All network communication uses HTTPS).
- **Do you provide a way for users to request that their data be deleted?** → **Yes** (Or select "No user accounts created in app; third-party SDK telemetry follows standard Google deletion policies").
- **Personal Data Collected by LinkScrub directly:** None. URLs and links processed are 100% on-device and never stored or shared with external servers.
