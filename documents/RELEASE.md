# OmniDial – Release & Versioning Management

## 1. Versioning Scheme
OmniDial uses **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
- `versionCode` (Integer in `app/build.gradle.kts`): Increments monotonically with each build (e.g., `1`, `2`, `3`).
- `versionName` (String in `app/build.gradle.kts`): Semantic representation (e.g., `1.0.0`).

| Version | Version Code | Release Date | Summary |
|---------|--------------|--------------|---------|
| `1.2.1` | `9` | Sep 2026 | Full dual-contact predictive suggestions, instant app-contact recognition, and preserved contact visibility. |
| `1.2.0` | `8` | Sep 2026 | Architecture modularization, faster call history search, and tamper-proof verified backups. |
| `1.1.5` | `7` | Sep 2026 | Instant, lag-free switching to the Keypad panel and fluid tab transitions. |
| `1.1.4` | `6` | Sep 2026 | Simplified automatic backup saving and one-tap restore without confusing file prompts. |
| `1.1.3` | `5` | Sep 2026 | Added crisp vibration feedback when typing on the dial pad and smoother in-call controls. |
| `1.1.2` | `4` | Sep 2026 | Fixes an issue where messaging or calling an international contact from the dial pad could fail if typed without their country code. |
| `1.1.1` | `3` | Sep 2026 | Search contacts by nickname, combined speed dial and letters keypad, steady dial pad layout, and clean voicemail button. |
| `1.1.0` | `2` | Sep 2026 | Keypad 2x2 action buttons, adaptive channel highlights, WhatsApp dark-mode icon contrast, Bluetooth/car call redirection, Favorites per-number selection, and Nickname sync. |
| `1.0.0` | `1` | Sep 2026 | Initial release with Cellular + WhatsApp integration, Caller Rules, T9 search, and Flip-to-Shhh. |

---

<details open>
<summary><h3>🚀 Release 1.2.1 (Latest) — Predictive Suggestions & In-App Contact Sync</h3></summary>

### What's New & Improvements:
- **Full Dual-Contact Keypad Suggestions**: Dialing a full 10-digit number now accurately lists all associated contacts in the predictive suggestion drawer, including phonebook and in-app contacts sharing a number.
- **Instant In-App Contact Recognition**: Newly saved app contacts immediately appear across search suggestions and directory lists without needing an app restart or manual reload.
- **Preserved Contact Visibility**: In-app and device contacts that share identical phone numbers are now both preserved cleanly across the address book without one hiding or overwriting the other.

</details>

<br>

<details>
<summary><h3>📦 Release 1.2.0 — Core Architecture & Data Integrity</h3></summary>

### What's New & Improvements:
- **Snappier Call History & Search**: Faster indexing for recent calls and rules ensures searching through thousands of past calls and predictive contacts remains smooth and responsive.
- **Tamper-Proof Secure Backups**: Backups now include automated cryptographic integrity verification, safeguarding your rules, speed dials, and favorite contacts against corruption during device transfers.
- **Enhanced Calling Stability**: Upgraded calling subsystem with automatic fallbacks for custom Android devices ensures uninterrupted voice connection across different phone manufacturers.
- **Rock-Solid Background Tasks**: Background tasks, notification dismissals, and call actions now run on isolated, crash-resilient lifecycles for smoother daily operation.

</details>

<br>

<details>
<summary><h3>📦 Release 1.1.5 — Performance & Instant Switching</h3></summary>

### What's New & Improvements:
- **Instant Keypad Switching**: Tapping or swiping to the Keypad from Contacts, Rules, or Favorites is now immediate with zero delay or freeze, even when carrying hundreds of contacts.
- **Fluid Screen Transitions**: All five main app screens stay pre-warmed in memory, giving you smooth, buttery swipes with zero pause.

</details>

<br>

<details>
<summary><h3>📦 Release 1.1.4 — Automatic Backups & File Manager</h3></summary>

### What's New & Improvements:
- **One-Tap Automatic Backups**: Backing up your settings, caller rules, speed dials, and favorites is now as easy as tapping a single "Backup Now" button. No more navigating confusing file export screens or picking folder paths.
- **Instant Restore List**: The app automatically detects all your saved backups and displays them in a clean list with their date and file size. Restoring or removing an old backup takes only one tap.
- **Simple Cross-Device Transfer**: A "Browse Files" button allows you to effortlessly restore backup files copied from another phone or downloaded from cloud storage.

</details>

<br>

<details>
<summary><h3>📦 Release 1.1.3 — Tactile Haptics & Fluid In-Call UI</h3></summary>

### What's New & Improvements:
- **Tactile Haptic Keystrokes**: Feel subtle, satisfying vibrations when dialing numbers, long-pressing speed dials, or connecting a call.
- **Smoother In-Call Screen**: Call timer updates no longer cause visual hiccups, keeping call controls responsive and fluid.

</details>

<br>

<details>
<summary><h3>📦 Release 1.1.2 — International WhatsApp & Texting</h3></summary>

### What's New & Improvements:
- **Instant International WhatsApp & Texting**: When you type a contact's number on the dial pad, WhatsApp and text messages now open directly to that person, even if you typed their number without the country code.
- **Fixed WhatsApp "Contact Not Found" Errors**: Fixed an issue where sending a WhatsApp message to an overseas contact could fail with an "invalid number" error when dialed without a country prefix.
- **Accurate One-Tap Calling & Messaging**: Tapping WhatsApp Chat, WhatsApp Call, Text, or Phone Call from the dial pad always connects using your contact's complete, saved international number.

</details>

<br>

<details>
<summary><h3>📦 Release 1.1.1 — Nicknames & Speed Dial Keypad</h3></summary>

### What's New & Improvements:
- **Search by Nickname**: You can now search for contacts on the dial pad using their nicknames in addition to their full names and phone numbers.
- **Speed Dial & Letters Together**: The dial pad now shows your favorite speed dial contact names above each number and classic letters below by default.
- **Smooth, Steady Dial Pad**: Action buttons below the dial pad (Text, WhatsApp Chat, and Phone Call) stay neatly in place without causing the screen to jump when typing.
- **Smart Country Codes for WhatsApp & SMS**: The app automatically attaches country codes (like +1, +91, +44) when opening WhatsApp or Text messages, preventing "contact not found" errors on local numbers.
- **Cleaner Voicemail Key**: Removed redundant stacked text on the '1' key for a clean, unified Voicemail button.
- **Faster Speed Dialing**: Long-pressing speed dial keys shows a large, easy-to-tap Call button, while long-pressing an empty key lets you assign a shortcut immediately.
- **Live Contact Names in Call History**: Your recent call history now updates contact names, nicknames, and photos immediately as you save or edit contacts.
- **Instant Favorite Updates**: Starring or unstarring a contact updates immediately throughout the app without needing a refresh.

</details>

<br>

<details>
<summary><h3>📦 Release 1.1.0 — 4-Button Dial Pad & Car Redirection</h3></summary>

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

</details>

---

## 2. Versioned APK Download Scheme
OmniDial maintains both versioned and latest APK artifacts:
- Latest Version: `OmniDial-v1.1.5.apk` (and alias `OmniDial.apk`)
- Prior Releases: `OmniDial-v1.1.4.apk`, `OmniDial-v1.1.3.apk`, `OmniDial-v1.1.2.apk`, `OmniDial-v1.1.1.apk`, `OmniDial-v1.1.0.apk`
Hosted directly on GitHub Pages under the `/apks` directory.

---

## 2. GitHub Pages Deployment Steps
1. Push repository changes to GitHub (`git push origin main`).
2. On GitHub, navigate to **Settings > Pages**.
3. Under **Build and deployment > Source**, select **Deploy from a branch**.
4. Set branch to `main` and folder to `/ (root)`, then click **Save**.
5. Your download webpage will be live at: `https://<username>.github.io/<repo-name>/`

---

## 3. Creating New Releases
1. Review the running work log in **`CHANGELOG.md`** under `[Unreleased]` for all user-facing changes and fixes made during development.
2. Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Build release APK:
   ```bash
   gradle :app:assembleRelease
   ```
4. Copy the output APK to versioned files in `/apks`:
   ```bash
   cp app/build/outputs/apk/release/app-release-unsigned.apk apks/OmniDial-v<version>.apk
   cp apks/OmniDial-v<version>.apk apks/OmniDial.apk
   ```
5. Move `[Unreleased]` in `CHANGELOG.md` to the new version header, update `index.html` release notes, and push to GitHub.
