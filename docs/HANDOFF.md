# PlainText — Handoff

PlainText is a deliberately minimal Android plain-text editor.

## Product principles

- Plain text only.
- No advertisements.
- No analytics.
- No account.
- No cloud service.
- No unnecessary network access.
- No unnecessary permissions.
- No rich-text editing.
- Prefer Android platform facilities over third-party libraries.
- Prefer the smallest implementation that solves the actual problem.
- Do not add features speculatively.

## Current state

The Android project is built with Kotlin and Jetpack Compose following Material 3 design principles.

The app provides a complete, production-ready, minimal plain-text editor with:

- **Clean "Blank Canvas" Editor**: Borderless, full-screen text canvas with comfortable typography (16sp, 24sp line height), transparent background, and edge-to-edge + IME keyboard insets.
- **Material 3 TopAppBar & Overflow Menu**: Clean app bar displaying filename/dirty state (`•`) with standardized 48dp action icon buttons (`+` New, `📂` Open, `💾` Save, and `⋮` More options).
- **Storage Access Framework (SAF) & Intent Flow**:
  - **New**: Resets current document (with unsaved changes confirmation dialog).
  - **Open**: Uses `OpenDocument` to read text files and display their filename.
  - **Save**: Saves directly to existing document `Uri` or launches `CreateDocument` for new files.
  - **Save As**: Accessible via overflow menu to save the current document to a new file location.
  - **External Open Support**: Supports opening `.txt` documents via `ACTION_VIEW` and `ACTION_EDIT` intent filters.
  - **Share Integration (Send & Receive)**: Share documents via Android sharesheet (`ACTION_SEND`) and receive shared text from other apps to edit.
- **Robust File Storage (`DocumentStorage`)**:
  - Automatic UTF-8 BOM (`\uFEFF`) detection and sanitization.
  - Fallback encoding decoding for non-standard UTF-8 files.
  - Multi-mode stream opening (`wt`, `w`, default) for maximum ContentProvider compatibility.
  - Persistable URI permission acquisition across intents and SAF launchers.
  - Smart save fallback: Automatically routes to `CreateDocument` if an external file is read-only.
  - Maximum safe file size protection (10MB limit) to prevent out-of-memory crashes.
- **Editor Features**:
  - **Visual Scrollbar Indicator**: Custom minimalist vertical scrollbar rendered dynamically during scrolling.
  - **Scroll & Cursor Preservation**: Uses `TextFieldValue` with explicit `verticalScrollState` so scroll and cursor positions are seamlessly preserved when the soft keyboard appears or disappears.
  - **Monospace Font Toggle**: Switch between default system font and monospace font.
  - **Word Wrap Toggle**: Toggle horizontal scrolling on/off for log files and code.
  - **Text Statistics**: Subtle word and character counts displayed at the bottom.
- **Adaptive App Icons**: Custom monochrome minimalist document icon design (`ic_launcher` / `ic_launcher_round`).
- **Release Optimization & CI/CD**:
  - Configured ProGuard/R8 code and resource shrinking for minimal release APK footprint.
  - Set release APK output filename directly to `plaintext.apk`.
  - Configured GitHub Actions CI/CD workflow (`.github/workflows/build.yml`) to automatically build and upload `plaintext.apk` artifacts and attach to GitHub Releases on tag push.
- **BackHandler Protection**: Intercepts back gestures when changes are unsaved.
- **Comprehensive Unit Testing**: JVM unit tests for BOM sanitization, word/character counting, and UTF-8 decoding.

## Next task

- Manual user testing on device/emulator.
- Keep changes minimal and aligned with core principles.
