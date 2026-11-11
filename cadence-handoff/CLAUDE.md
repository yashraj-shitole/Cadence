# Cadence: build brief for Claude Code

Cadence is a calm phone app that keeps daily habits, to-dos and notes in one place. One-line pitch: "habits, tasks and notes that keep a steady beat."
This folder is the finished design. Your job is to build the app to match it.

## What is in this folder
- `design-system/` the source of truth for look and feel.
  - `README.md` principles, words, colour, glass, type, layout, icons, accessibility. Read first.
  - `tokens.json` all colour (4 themes), type, spacing, radius, shadow, blur, duration, easing, haptic and size tokens, each with a usage note.
  - `palettes.md` and `palettes.json` six colour palettes (Meadow default, Ultramarine, Dusk, Rose, Lagoon, Graphite), with contrast figures.
  - `motion-and-haptics.md` every interaction, its animation, and its haptic, plus iOS and Android mappings.
  - `screens.md` screen map and rules.
  - `components/<Name>/README.md` + `preview.html` 19 components (Glass, Button, Check, BeatBar, NextUp, HabitRow, ConsistencyGrid, TaskRow, Chip, NoteCard, SearchField, Segmented, Toggle, LevelSlider, SettingsGroup, Toast, TabBar, QuickAddSheet, PalettePicker). `bundle.css` holds all component CSS and the palette overrides.
- `screens/` 11 interactive 390x844 mockups (`*.dc.html`) plus `cadence.css`. They are HTML prototypes: open them in a browser (they need `support.js` from the design tool, so rely on the markup, CSS and logic in each file rather than running them standalone). `canvas.json` lists them.
- `reference-renders/` PNGs of Today in all palettes (light and dark) and the palette picker.

## Product rules (do not drift)
1. Everything due today is a "beat". Today shows one number ("4/8 beats done") over a segmented bar, and a single "Next up" card.
2. Habits have a cue ("After chai, on the balcony") and a minimum version ("Minimum: 1 page"). They show the last 7 days and a streak count, grouped Morning, Afternoon, Evening.
3. Streaks are forgiving: a rest day keeps the streak, a missed day is a quiet grey dot, never red. Habit detail has a five-week consistency grid.
4. Tasks have due time, duration, priority flag and tags, grouped Today, This week, Done.
5. Notes: pinned notes, tags, search, editor with checklist lines.
6. Quick add is one sheet from the centre tab button (Task / Habit / Note switch).
7. Undo instead of confirm. Minimum touch target 48px. Every state has a shape as well as a colour.
8. Settings: Feel (Motion and Haptics, 4 stops each: Off / Subtle-Soft / Standard-Balanced / Expressive-Firm; plus "Completion only" haptics), Appearance (Colour palette, Theme, Backdrop, Reduce transparency, High contrast), Rhythm (one daily reminder, weekend rest days, week start), Data (export, erase).

## Look
Frosted-glass tiles (bevelled, soft-edged) over a pale grid lit by three blurred colour glows. No circles or spheres in the backdrop. Bricolage Grotesque for headings, Geist for text. One accent colour, set by the chosen palette. Reduce transparency and high contrast switch glass to solid surfaces. Light and dark themes both pass contrast (see `palettes.md`).

## How to build
- The stack is not fixed. Pick one for a cross-platform phone app (React Native with Expo, or Flutter, are both reasonable) and say which you chose and why before starting. Keep tokens in one theme module generated from `design-system/tokens.json` and `palettes.json`, never hard-coded.
- Implement the glass recipe from `design-system/README.md`: translucent fill + 145 degree sheen + 1px rim + three-layer inset bevel + backdrop blur 24px (36px for floating layers) with saturate. Fall back to solid surfaces when transparency is reduced.
- Motion: multiply all travel and duration by the Motion setting; respect the OS reduced-motion setting. Haptics: scale amplitude by the Haptics setting; fire only on the user's own actions; strongest for completing a beat. Use the platform mappings in `motion-and-haptics.md`.
- Persist locally first (SQLite or similar); no account needed. Streak logic must treat rest days and minimum-version days as kept.
- Build in this order: theme and tokens, glass and core components, Today, Habits and Habit detail, Tasks, Notes and editor, Quick add, Settings (with live palette, motion and haptic settings), then polish and accessibility checks.
- Match the screens in `screens/` for layout and copy. Where a mockup and `design-system/` disagree, the design system wins.

## Known limits of the mockups
- Haptics cannot be felt in the browser mockups; the specification is in `motion-and-haptics.md`.
- In the mockups the palette choice applies per screen; in the app it must apply everywhere.
- Fonts load from Google Fonts in the mockups; bundle them in the app.
