# User Guidelines & Project Conventions

## Communication Rules
- Be extremely concise in assistant responses.
- If the user asks a question, answer it directly before taking or summarizing actions.
- Do not show code diffs, raw logs, or long summaries in assistant chat responses.
- List exact changes made in 2–3 brief bullet points.

## Release Notes & Documentation Standards
- **Published Release Notes** (`documents/RELEASE.md`, `index.html`): Must be strictly end-user friendly, written in clear, non-technical plain English focusing on practical benefits and polished UX improvements. Avoid developer jargon, variable names, class names, or internal architecture details.
- **Internal Change Log** (`documents/CHANGELOG.md`): Can contain technical details, architecture notes, method names, and developer-level specifications alongside user-facing notes.

## Release & Versioning Workflow
- **Semantic Versioning**: Increment `versionCode` (integer) and `versionName` (`MAJOR.MINOR.PATCH`) in `app/build.gradle.kts` on new releases.
- **APK Artifact Distribution**: When building releases, mirror the APK to `apks/OmniDial-v<version>.apk` and `apks/OmniDial.apk`.
- **Website Synchronization**: Always keep `index.html` in sync with the latest download links and user-friendly release cards.
- **Git Commit Standards**: Commit complete release updates with clear, descriptive commit messages.

## Document Sync on Every Release Push
When incrementing `versionCode`/`versionName` or pushing a new Git release, ALL of the following documents must be updated **before** committing:

1. **`app/build.gradle.kts`**: Increment `versionCode` and `versionName`.
2. **`README.md`**:
   - Update the version badge (`[![Version](…)](documents/CHANGELOG.md)`) to the new `vX.Y.Z (Build N)`.
   - Add the new APK to the **Download & Installation** section as the first entry (Latest — Build N).
   - Update any feature sections to reflect new capabilities added in the release.
3. **`documents/RELEASE.md`**:
   - Add a new `<details open>` release card at the top as the new "Latest" and demote the previous latest to a closed `<details>` card.
   - Add a row to the version table at the top of the file.
   - Update the **Versioned APK Download Scheme** section with the new latest APK filename and prior releases list.
4. **`documents/CHANGELOG.md`**: Add a new `## 🚀 [vX.Y.Z] — Build N` section with user-facing enhancements and technical architecture notes.
5. **`index.html`**: Add new release download card and update the "Latest" download button link to the new APK.
6. **`apks/`**: Copy release APK as `OmniDial-v<version>.apk` and overwrite `OmniDial.apk` as the latest alias.
7. **`documents/ToTest.md`**: Add test items for any new features introduced in the release.
8. **`documents/ARCHITECTURE.md`**: Update the version header, any new entities/components/subsystems introduced, unit test table, and recent fixes section.
9. **`documents/DESIGN.md`**: Update the version header, module directory listing, Room DB entity schemas, subsystem workflow descriptions, and any new architectural patterns introduced.
