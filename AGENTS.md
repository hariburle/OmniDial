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
