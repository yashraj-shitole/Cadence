# Cadence

Habits, tasks and notes that keep a steady beat. A native Android app (Kotlin, Jetpack Compose) built to the design in `cadence-handoff/`.

## Stack
- Kotlin + Jetpack Compose, no Material components: every control is built from the design system.
- Room (SQLite) for habits, habit logs, tasks and notes; DataStore for settings. No account and no tracking; the only request it makes is a check for a newer release.
- minSdk 31 (real-time `RenderEffect` blur for floating glass), targetSdk 37.

## Notes editor
Notes are stored as plain Markdown and edited as blocks (text, headings, bulleted, numbered and to-do lists, quote, code, divider) with inline bold, italic, strikethrough, code and links. Type `/` for the formatting menu, or use Markdown shortcuts (`# `, `- `, `1. `, `[] `, `> `, ``` ``` ```, `---`). The More menu shows the raw Markdown source, copies it, or exports a `.md` file.

## Backup and restore
Settings > Backup can save everything to a folder you pick (for example Download/Cadence) as `cadence-backup.json`. It is rewritten a short while after any change, whenever you leave the app, and at least once a day. The folder is chosen with the system folder picker, so the app needs no storage permission, and the file survives an uninstall. A backup holds habits, logs, tasks, notes and the look-and-feel settings; the folder choice itself stays on the device. An empty database never overwrites a backup.

On a fresh install the app offers **Restore from a backup?** before the walkthrough: choose the file and everything comes back, settings included. Settings > Data > Import data takes the same file and asks whether to **Add** it to what is here or **Replace** everything with it. Files from 1.0.x (no settings) still import.

## Design tokens
`app/src/main/java/com/yashraj/cadence/ui/theme/GeneratedTokens.kt` is generated. After changing anything in
`cadence-handoff/design-system/`, run:

    node tools/generate-tokens.mjs

## Build and test
    ./gradlew :app:assembleDebug
    ./gradlew :app:testDebugUnitTest

Debug builds seed the sample day from the mockups on first launch; release builds start empty.
