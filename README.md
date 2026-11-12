# Cadence

Habits, tasks and notes that keep a steady beat. A native Android app (Kotlin, Jetpack Compose) built to the design in `cadence-handoff/`.

## Stack
- Kotlin + Jetpack Compose, no Material components: every control is built from the design system.
- Room (SQLite) for habits, habit logs, tasks and notes; DataStore for settings. No account and no tracking; the only request it makes is a check for a newer release.
- minSdk 31 (real-time `RenderEffect` blur for floating glass), targetSdk 37.

## Notes editor
Notes are stored as plain Markdown and edited as blocks (text, headings, bulleted, numbered and to-do lists, quote, code, divider) with inline bold, italic, strikethrough, code and links. Type `/` for the formatting menu, or use Markdown shortcuts (`# `, `- `, `1. `, `[] `, `> `, ``` ``` ```, `---`). The More menu shows the raw Markdown source, copies it, or exports a `.md` file.

## Design tokens
`app/src/main/java/com/yashraj/cadence/ui/theme/GeneratedTokens.kt` is generated. After changing anything in
`cadence-handoff/design-system/`, run:

    node tools/generate-tokens.mjs

## Build and test
    ./gradlew :app:assembleDebug
    ./gradlew :app:testDebugUnitTest

Debug builds seed the sample day from the mockups on first launch; release builds start empty.
