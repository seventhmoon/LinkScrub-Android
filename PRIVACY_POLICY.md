# Privacy Policy for LinkScrub

**Last updated:** March 2026

**LinkScrub** ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains our data practices when you use the LinkScrub mobile application for Android.

---

## 1. 100% On-Device URL Sanitization

LinkScrub is built with a strict privacy-first architecture:
- **No Link Logging:** All URL parameter stripping, tracking token removals, rule validations, and local rule configurations are executed completely **on your device**.
- **No Browsing History Sent to LinkScrub Servers:** We do not operate proprietary servers to store, log, analyze, or profile the links you scrub, copy, share, or preview.

---

## 2. Network Operations & Metadata Previews

- **HTTP Redirect Unshortening:** If you unshorten a shortened URL (e.g., `bit.ly`, `t.co`), the app sends direct HTTP HEAD/GET requests directly from your device to the destination domain to resolve the redirection path.
- **Open Graph Metadata Previews:** When previewing a clean link, the app fetches public Open Graph metadata (title, description, and thumbnail image) directly from the target website. LinkScrub does not proxy these requests through intermediary servers.
- **QR Code Scanning:** QR codes are scanned using the Google Play Services on-device Code Scanner module. The app does not request or require direct camera permissions, and camera feed frames are processed privately on-device.

---

## 3. Third-Party Services & Analytics

LinkScrub uses select third-party SDKs provided by Google to maintain stability, deliver app updates, and support maintenance:

### A. Google Mobile Ads (AdMob)
- The app displays advertisements provided by Google AdMob.
- AdMob may collect and process information such as:
  - Device identifiers (including Google Advertising ID / AAID)
  - IP address (used for approximate geolocation)
  - Interaction with advertisements
- For more details on how Google processes ad data, visit [Google's Advertising Policies](https://policies.google.com/technologies/ads).

### B. Firebase Crashlytics & Performance Monitoring
- Used to diagnose application crashes, bugs, and performance bottlenecks.
- Collects diagnostic information such as stack traces, device state, OS version, and performance metrics.
- No personally identifiable information (PII) or URLs processed by the user are attached to crash reports.
- See [Firebase Privacy & Security](https://firebase.google.com/support/privacy).

### C. Google Play In-App Updates
- Uses the Google Play Core library to notify you when newer, more secure versions of LinkScrub are available on Google Play.

---

## 4. Permissions

LinkScrub adheres strictly to the principle of least privilege:
- `android.permission.INTERNET`: Required to resolve redirect chains, fetch Open Graph link previews, and load AdMob advertisements.
- **No Location Permission:** The app does not request or access fine or coarse GPS device locations.
- **No Camera Permission:** Uses Play Services Code Scanner to scan QR codes without accessing the camera directly.
- **No Storage Permission:** Does not read or write external shared storage.

---

## 5. Children's Privacy

LinkScrub is not directed to individuals under the age of 13. We do not knowingly collect personal information from children.

---

## 6. Changes to This Privacy Policy

We may update this Privacy Policy periodically to reflect new features or regulatory requirements. Any updates will be published in this document and updated in the application store listing.

---

## 7. Contact Us

If you have questions or concerns regarding this Privacy Policy, please contact us by opening an issue on GitHub or reaching out via email:
- **Email:** support@androidfung.com
- **GitHub Repository:** https://github.com/androidfung/LinkScrub
