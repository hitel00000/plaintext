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

The Android project is built with Kotlin and Jetpack Compose.

The app provides a functional minimal plain-text editor with:

- Text editing area for UTF-8 plain text with edge-to-edge and IME/keyboard padding.
- Storage Access Framework (SAF) flow:
  - **New**: Resets the current document (with unsaved changes confirmation dialog).
  - **Open**: Uses `OpenDocument` to read text files and display their filename.
  - **Save**: Saves directly to the existing document `Uri` or launches `CreateDocument` for new files.
  - **External Intent Support**: Supports opening `.txt` documents via `ACTION_VIEW` and `ACTION_EDIT` intent filters.
- Dirty state indication (displays `•` next to filename when modified).
- Error handling and user feedback via `Snackbar`.
- Document text, file metadata, and state preserved across configuration/lifecycle changes with `rememberSaveable`.

## Next task

- Check file reading/writing encoding robustness (e.g., non-UTF-8 fallback or empty file handling).
- Add unit or UI tests if needed.
- Keep changes minimal and aligned with core principles.
