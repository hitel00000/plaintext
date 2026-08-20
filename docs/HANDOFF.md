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

The initial Android project has been created with Kotlin and Jetpack Compose.

The app currently provides a placeholder editor screen with:

- A text editing area occupying most of the screen.
- Placeholder Open and Save buttons.
- A current file name placeholder.

## Next task

Implement the minimal Storage Access Framework flow:

1. Create/open a text document.
2. Edit UTF-8 text.
3. Save the current document.
4. Handle basic file errors gracefully.
5. Preserve unsaved text across basic lifecycle changes.

Keep the implementation small and avoid speculative editor features.
