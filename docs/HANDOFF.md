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

The app provides a polished, minimal plain-text editor with:

- **Clean "Blank Canvas" Editor**: Borderless, full-screen text canvas with comfortable typography (16sp, 24sp line height), transparent background, and edge-to-edge + IME keyboard insets.
- **Material 3 TopAppBar**: Clean app bar displaying filename/dirty state (`•`) with standardized 48dp action icon buttons (`+` New, `📂` Open, `💾` Save) using lightweight vector drawables.
- **Storage Access Framework (SAF) flow**:
  - **New**: Resets the current document (with unsaved changes confirmation dialog).
  - **Open**: Uses `OpenDocument` to read text files and display their filename.
  - **Save**: Saves directly to the existing document `Uri` or launches `CreateDocument` for new files.
  - **External Intent Support**: Supports opening `.txt` documents via `ACTION_VIEW` and `ACTION_EDIT` intent filters.
- **BackHandler Protection**: Intercepts back gestures when changes are unsaved to prevent accidental data loss.
- **Lifecycle & Error Handling**: State preserved across rotation/lifecycle with `rememberSaveable`, clear `Snackbar` error feedback.

## Next task

- Check file reading/writing encoding robustness (e.g., non-UTF-8 fallback or empty file handling).
- Add unit or UI tests for core document operations.
- Keep changes minimal and aligned with core principles.
