Cadence is a calm phone app that keeps daily habits, to-dos and notes in one place. It exists to make the next useful action obvious and to show progress without nagging. Build every screen to pass one test: within two seconds of opening, the user knows the one thing to do now and how the day is going.

## Principles

1. **One next action.** Each screen has one `accent`-filled action. Today names a single beat in the Next up card; it is never a list.
2. **Progress without pressure.** Show what was done. A missed day is a small grey dot, never red, never an X, never a notification. Rest days and minimum versions keep a streak.
3. **Capture in one tap.** The centre add button opens one sheet with the keyboard up. A title alone is a complete entry.
4. **Undo instead of confirm.** Completing and deleting are instant and reversible for five seconds. Dialogs are for erasing all data only.
5. **Calm by default.** One accent colour, one number on Today, one daily reminder. Nothing blinks, bounces for attention, or badges.
6. **The user sets the feel.** Motion and haptics each have four levels in Settings, and the system's Reduce Motion, Reduce Transparency and contrast settings always win.

## Words

- A **beat** is anything due today: a habit to log or a task to finish. Use "beats" for the day total ("4/8 beats done") and "habit" or "task" for single items.
- Write to "you", never "we" or "I". Sentence case everywhere, including buttons and headings. No exclamation marks, no emoji.
- Buttons are verbs that name the result: "Mark done", "Add task", "Save note", "Later". Never "OK" or "Submit".
- Cues are written the way people talk: "After chai, on the balcony". Minimums start with "Minimum:" ("Minimum: 1 page").
- Describe misses neutrally or not at all. Say "2 missed" in a legend; never "You broke your streak", "Don't give up" or "You're falling behind".
- When the day is finished: "That's today" and "8 of 8 beats done. Rest well." Nothing bigger.
- Times are lowercase with a space ("5:30 pm"); durations are "25 min"; dates are "Today", "Sun", then "3 Nov".

## Colour

- The ground is `backdrop`, near-white with a faint `grid-line` grid, lit from behind by three blurred glows: `glow-1`, `glow-2` and `glow-3`. The glows are scenery with no edges; nothing interactive is ever a glow colour.
- Surfaces are frosted tiles: `glass` (rows, cards, fields) and `glass-raised` (Next up, sheet, tab bar, toast), each with the `glass-sheen` light and a `glass-edge` rim.
- Text is `ink` or `ink-muted`. On bare `backdrop` or glow use `ink` only; `ink-muted` is for text on glass or solid surfaces.
- `accent` is the only hue with meaning: done, selected, primary, link. Put `on-accent` on it. Selected chips use `accent-soft` behind `ink` text.
- The accent, the three backdrop glows and the rest-day colour belong to a **colour palette** the user picks in Settings (Meadow, Ultramarine, Dusk, Rose, Lagoon, Graphite). Everything else stays fixed. See `palettes.md`.
- `rest` marks rest days, always as a ring. `danger` is for destructive actions only, always with a word. There is no success green and no warning yellow.
- Every state has a shape as well as a colour: filled dot = done, tinted ring = minimum, clay ring = rest, small dot = missed, dashed ring = today.
- Themes: Light, Dark, and a high-contrast pair. Light and Dark hold at least 4.5:1 for all text measured against the strongest glow behind the glass. The high-contrast themes hold 7:1, remove the glow and blur, and draw every surface edge at 3:1.

## Glass

- Glass in Cadence is a frosted, softly moulded tile over colour: rounded, lit from the top-left, with a shaded lower lip. It is not a flat transparent sheet and never sits over an empty white ground.
- A tile is five things together: translucent fill, `glass-sheen` gradient, 1px `glass-edge` rim, the inset bevel in `shadow-glass` (or `shadow-float`), and backdrop blur `blur-glass` (or `blur-float`) with saturate 180%.
- Keep a glow behind every group of tiles so the frost has something to pick up. Place glows so the upper third leans `glow-1`, the middle `glow-3` and the lower third `glow-2`; blur each by `blur-glow`.
- Never draw a glow as a circle, sphere, blob outline or any other visible shape.
- Two depths only. Rows sit on the backdrop; they are not nested inside another card.
- Keep long reading text off heavy translucency: the note editor uses one full-width `glass-raised` tile so body copy has the steadiest ground.
- Reduce transparency (system or in-app) swaps `glass` → `surface-solid` and `glass-raised` → `surface-solid-raised` and removes blur. Set `data-glass="solid"` on the root.
- Backdrop style is a setting: Glow (default), Soft (glows at half strength, `data-backdrop="soft"`), Plain (`data-backdrop="plain"`, grid only).

## Type

- Headings, numbers and titles use the `display` family (Bricolage Grotesque): `numeral`, `display`, `title`, `heading`. Everything else uses the `text` family (Geist): `body`, `body-strong`, `callout`, `caption`, `label`.
- One `display` title per screen, top left, on the backdrop. One `numeral` on Today.
- `caption` (13px) is the smallest running text. `label` (12px, uppercase, tracked) is for eyebrows such as "Next up" and weekday initials only.
- Use tabular numerals for counts and times. Support system text scaling to 200%: rows grow in height, titles wrap to two lines, week dots drop below the title.

## Layout

- Screen margin `space-5`. Rows are full width, `space-2` apart, with `space-4` padding and `radius-card` corners. Groups are `space-6` apart under a `heading`.
- Every touch target is at least `tap-min` (48px) in both directions, including checks, chips and toggles whose visible mark is smaller.
- Primary actions live in the bottom half of the screen, within thumb reach: the Next up button, the tab bar, the sheet's button. The top holds only the title and one or two icon buttons.
- The floating tab bar is always the lowest layer of chrome; lists scroll beneath it.
- Radii: `radius-tag` for small marks, `radius-control` for buttons and inputs, `radius-card` for rows, `radius-sheet` for the Next up card and sheets, `radius-pill` for anything fully round.

## Icons

- Line icons on a 24px box with a 1.75px stroke, round caps and joins, no fills, drawn in `currentColor`. The set follows Lucide's geometry; take any further icons from Lucide at the same stroke.
- The priority flag is the only filled icon. Icons beside text in a row are 16px with a 2px stroke.
- An icon alone is allowed only in the top bar and must have an accessible label. Tabs always pair icon and word.

## Accessibility

- Honour system Reduce Motion, Reduce Transparency, Increase Contrast, text size and haptics settings before the in-app ones.
- Focus is a 2px `focus-ring` outline with 2px offset.
- Every chart-like element (week dots, consistency grid, beat bar) has a text alternative stating the counts.
- Nothing depends on a gesture alone: swipe actions always have a visible equivalent in detail.
