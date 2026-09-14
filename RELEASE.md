# OmniDial – Release & Versioning Management

## 1. Versioning Scheme
OmniDial uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
- `versionCode` (Integer in `app/build.gradle.kts`): Increments monotonically with each build (e.g., `1`, `2`, `3`).
- `versionName` (String in `app/build.gradle.kts`): Semantic representation (e.g., `1.0.0`).

| Version | Version Code | Release Date | Summary |
|---------|--------------|--------------|---------|
| `1.1.1` | `3` | Sep 2026 | T9 Nickname search, default Fav & T9 layout, automatic international country code resolution for WhatsApp/SMS, Key 1 VM deduplication, and non-jumping quick action buttons. |
| `1.1.0` | `2` | Sep 2026 | Keypad 2x2 action buttons, adaptive channel highlights, WhatsApp dark-mode icon contrast, Bluetooth/car call redirection, Favorites per-number selection, and Nickname sync. |
| `1.0.0` | `1` | Sep 2026 | Initial release with Cellular + WhatsApp integration, Caller Rules, T9 search, and Flip-to-Shhh. |

---

## Release 1.1.1 Change Log

### What's New:
- **T9 Search for Nicknames**: Dial pad T9 search now indexes and searches contact nicknames alongside full names and numbers.
- **Fav & T9 as Default Layout**: Speed Dial Keypad display setting defaults to "Fav & T9", displaying speed dial contact names above digits and standard T9 letters below.
- **Fixed-Position Quick Action Buttons**: Kept secondary action buttons (SMS, WhatsApp Chat, Secondary Call) permanently in place below the keypad with no vertical jumping.
- **Automatic WhatsApp/SMS Country Code Resolution**: Automatically queries full contact numbers or appends device country calling codes (+91, +1, etc.) before handing off to WhatsApp/SMS to prevent missing country code errors.
- **Key 1 Clean Labeling**: Removed redundant stacked "VM" and "Voicemail" text on Key 1.

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
