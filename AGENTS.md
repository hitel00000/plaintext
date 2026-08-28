## Project

PlainText is a deliberately minimal Android plain-text editor.

Read `docs/HANDOFF.md` for the current project state and immediate work.

## Principles

- Keep the application minimal.
- Prefer Android platform APIs.
- Avoid unnecessary dependencies.
- Do not add speculative features.
- Do not introduce architecture without a concrete need.
- No advertisements, analytics, accounts, cloud services, or unnecessary network access.

## Workflow

- Make small, focused changes.
- Build/test after meaningful changes.
- Prefer focused commits.
- Update `docs/HANDOFF.md` when project state or direction changes.
- **Mandatory on Release/Deploy**: Always write/update release notes in `docs/release-notes/<tag>.md` before creating a git tag or triggering a release deployment. Do not skip release notes under any circumstances.
