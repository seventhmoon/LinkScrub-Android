# Contributing to LinkScrub 🧼

Thank you for your interest in contributing to LinkScrub! As a privacy-focused open-source Android utility, community contributions—especially new URL tracking parameter definitions and localizations—are warmly welcomed.

---

## 🛠️ Development Setup

1. **Prerequisites**:
   - Android Studio Ladybug (or newer)
   - JDK 17 (recommended: Eclipse Temurin or Android Studio bundled JDK)
   - Android SDK 35/37

2. **Clone and Build**:
   ```bash
   git clone https://github.com/seventhmoon/LinkScrub-Android.git
   cd LinkScrub-Android
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```

---

## 🧹 Adding New Tracking Parameters or Cleaning Rules

The core URL sanitization logic lives in:
- `app/src/main/java/com/androidfung/linkscrub/domain/UrlCleaner.kt`

When adding rules:
1. Locate `TRACKING_PARAMS` (or domain-specific handlers if the domain requires special preservation, such as YouTube or Amazon).
2. Add corresponding unit test cases in:
   - `app/src/test/java/com/androidfung/linkscrub/domain/UrlCleanerTest.kt`
3. Verify all tests pass:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🌐 Adding or Updating Localizations

Translations are located under:
- `app/src/main/res/values/strings.xml` (Base - English)
- `app/src/main/res/values-b+zh+Hant/strings.xml` (Traditional Chinese)
- `app/src/main/res/values-ja/strings.xml` (Japanese)
- `app/src/main/res/values-ko/strings.xml` (Korean)

Please avoid altering string identifiers (`name="foo"`) when adding new language translations.

---

## 📋 Pull Request Guidelines

1. Fork the repo and create a branch from `main`:
   ```bash
   git checkout -b feat/add-threads-tracking-rules
   ```
2. Commit with meaningful conventional commit messages (`feat: ...`, `fix: ...`, `chore: ...`).
3. Ensure lint and unit tests pass before opening a PR:
   ```bash
   ./gradlew testDebugUnitTest lintDebug
   ```
4. Open a pull request against `main` using the provided PR template.
