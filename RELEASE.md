# OmniDial – Release & Versioning Management

## 1. Versioning Scheme
OmniDial uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
- `versionCode` (Integer in `app/build.gradle.kts`): Increments monotonically with each build (e.g., `1`, `2`, `3`).
- `versionName` (String in `app/build.gradle.kts`): Semantic representation (e.g., `1.0.0`).

| Version | Version Code | Release Date | Summary |
|---------|--------------|--------------|---------|
| `1.1.2` | `4` | Sep 2026 | Enhanced contact number resolution for WhatsApp and SMS, ensuring full stored international country codes (such as +91) are automatically used when dialing domestic numbers. |
| `1.1.1` | `3` | Sep 2026 | T9 Nickname search, default Fav & T9 layout, automatic international country code resolution for WhatsApp/SMS, Key 1 VM deduplication, and non-jumping quick action buttons. |
| `1.1.0` | `2` | Sep 2026 | Keypad 2x2 action buttons, adaptive channel highlights, WhatsApp dark-mode icon contrast, Bluetooth/car call redirection, Favorites per-number selection, and Nickname sync. |
| `1.0.0` | `1` | Sep 2026 | Initial release with Cellular + WhatsApp integration, Caller Rules, T9 search, and Flip-to-Shhh. |

---

## Release 1.1.2 Change Log

### What's New & Improvements:
- **Accurate WhatsApp & SMS Contact Matching**: When typing a local or 10-digit number for a contact saved with an international country code (such as +91 for India or +44 for UK), the app now automatically uses the full saved country code rather than defaulting to your phone's region.
- **Direct Dial Pad Actions Sync**: Tapping the WhatsApp Chat, WhatsApp Call, SMS, or regular Call buttons from the dial pad now resolves the full international number of the matched contact directly.
- **Reliable Messaging Links**: Prevents "number does not exist" or incorrect region errors when messaging contacts worldwide.

---

---

## Release 1.1.1 Change Log

### What's New & Improvements:
- **Search by Nickname**: You can now search for contacts on the dial pad using their nicknames in addition to their full names and phone numbers.
- **Speed Dial & Letters Together**: The dial pad now shows your favorite speed dial contact names above each number and classic letters below by default.
- **Smooth, Steady Dial Pad**: Action buttons below the dial pad (Text, WhatsApp Chat, and Phone Call) stay neatly in place without causing the screen to jump when typing.
- **Smart Country Codes for WhatsApp & SMS**: The app automatically attaches country codes (like +1, +91, +44) when opening WhatsApp or Text messages, preventing "contact not found" errors on local numbers.
- **Cleaner Voicemail Key**: Removed redundant stacked text on the '1' key for a clean, unified Voicemail button.
- **Faster Speed Dialing**: Long-pressing speed dial keys shows a large, easy-to-tap Call button, while long-pressing an empty key lets you assign a shortcut immediately.
- **Live Contact Names in Call History**: Your recent call history now updates contact names, nicknames, and photos immediately as you save or edit contacts.
- **Instant Favorite Updates**: Starring or unstarring a contact updates immediately throughout the app without needing a refresh.

---

## Release 1.1.0 Change Log

### What's New:
- **Cleaner 4-Button Dial Pad**: Quickly reach anyone with 4 organized buttons on your dial pad: Send Text, Regular Phone Call, WhatsApp Message, or WhatsApp Voice Call.
- **Smart Call Suggestions**: The app gently highlights how you usually contact each person, so you never have to remember whether to call on WhatsApp or regular mobile.
- **Car Bluetooth & Hands-Free Calling**: Making calls from your car dashboard, smartwatch, or Bluetooth headset now automatically routes through WhatsApp when that's your preferred channel for that contact.
- **Choose Default Numbers for Favorites**: When starring a contact who has multiple numbers (like home, work, and mobile), you can easily choose the exact number to dial by default.
- **Easier to See in Dark Mode**: WhatsApp icons and buttons now feature crisp, high-contrast outlines so they stand out clearly on dark backgrounds.
- **Friendlier Nicknames**: Add personal nicknames that appear front-and-center on your favorites grid, with official legal names neatly subtitled below.

### Improvements & Fixes:
- **Reliable WhatsApp Call Confirmations**: Fixed an issue where tapping WhatsApp call on favorite cards could accidentally place a regular cellular call or skip confirmation.
- **Smoother Navigation**: Pressing the Android back button when searching favorites or contacts now cleanly dismisses the search bar rather than exiting the app.

---

## 2. Versioned APK Download Scheme
OmniDial maintains both versioned and latest APK artifacts:
- Latest Version: `OmniDial-v1.1.0.apk` (and alias `OmniDial.apk`)
- Prior Release: `OmniDial-v1.0.0.apk`
Hosted directly on GitHub Pages under the `/docs` directory.

---

## 2. GitHub Pages Deployment Steps
1. Push repository changes to GitHub (`git push origin main`).
2. On GitHub, navigate to **Settings > Pages**.
3. Under **Build and deployment > Source**, select **Deploy from a branch**.
4. Set branch to `main` and folder to `/docs`, then click **Save**.
5. Your download webpage will be live at: `https://<username>.github.io/<repo-name>/`

---

## 3. Creating New Releases
1. Review the running work log in **`CHANGELOG.md`** under `[Unreleased]` for all user-facing changes and fixes made during development.
2. Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Build release APK:
   ```bash
   gradle :app:assembleRelease
   ```
4. Copy the output APK to versioned files in root and `/docs`:
   ```bash
   cp app/build/outputs/apk/release/app-release-unsigned.apk OmniDial-v<version>.apk
   cp OmniDial-v<version>.apk docs/OmniDial-v<version>.apk
   cp OmniDial-v<version>.apk OmniDial.apk
   cp OmniDial.apk docs/OmniDial.apk
   ```
5. Move `[Unreleased]` in `CHANGELOG.md` to the new version header, update `docs/index.html` & `index.html` release notes, and push to GitHub.
