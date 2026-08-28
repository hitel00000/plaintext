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

- Text editing area for UTF-8 plain text.
- Storage Access Framework (SAF) flow:
  - **New**: Resets the current document.
  - **Open**: Uses `OpenDocument` to read text files and display their filename.
  - **Save**: Saves directly to the existing document `Uri` or launches `CreateDocument` for new files.
- Basic error handling and feedback via `Snackbar`.
- Document text and file metadata preserved across configuration/lifecycle changes with `rememberSaveable`.

## Next task

- Refine editor UX details (e.g., keyboard handling, insets/padding refinement, dirty state indication).
- Verify edge cases in file reading/writing (e.g., empty files, non-UTF-8 encodings fallback).
- Keep changes minimal and aligned with core principles.
