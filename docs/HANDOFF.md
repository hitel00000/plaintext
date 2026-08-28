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

The app provides a robust, polished, minimal plain-text editor with:

- **Clean "Blank Canvas" Editor**: Borderless, full-screen text canvas with comfortable typography (16sp, 24sp line height), transparent background, and edge-to-edge + IME keyboard insets.
- **Material 3 TopAppBar & Overflow Menu**: Clean app bar displaying filename/dirty state (`•`) with standardized 48dp action icon buttons (`+` New, `📂` Open, `💾` Save, and `⋮` More options).
- **Storage Access Framework (SAF) flow**:
  - **New**: Resets current document (with unsaved changes confirmation dialog).
  - **Open**: Uses `OpenDocument` to read text files and display their filename.
  - **Save**: Saves directly to existing document `Uri` or launches `CreateDocument` for new files.
  - **Save As**: Accessible via overflow menu to save the current document to a new file location.
  - **External Intent Support**: Supports opening `.txt` documents via `ACTION_VIEW` and `ACTION_EDIT` intent filters.
- **Robust File Storage (`DocumentStorage`)**:
  - Automatic UTF-8 BOM (`\uFEFF`) detection and sanitization.
  - Fallback encoding decoding for non-standard UTF-8 files.
  - Maximum safe file size protection (10MB limit) to prevent out-of-memory crashes.
- **Editor Features**:
  - **Monospace Font Toggle**: Switch between default system font and monospace font.
  - **Text Statistics**: Subtle word and character counts displayed at the bottom.
- **BackHandler Protection**: Intercepts back gestures when changes are unsaved.
- **Comprehensive Unit Testing**: JVM unit tests for BOM sanitization, word/character counting, and UTF-8 decoding.

## Next task

- Manual user testing on device/emulator.
- Verify system theme transitions (Light / Dark mode).
- Keep changes minimal and aligned with core principles.
