# Motion and haptics

Motion and touch feedback exist to confirm what the user just did, in the place they did it. They never ask for attention on their own.

## Rules

- **Respond on touch-down.** Press feedback starts when the finger lands (`duration-press`), not on release.
- **Short.** Feedback is `duration-quick` or less; state changes use `duration-base`; only screen and sheet transitions reach `duration-slow` and `duration-sheet`. Nothing in the app runs longer than `duration-celebrate`.
- **Enter with `ease-out`, leave with `ease-in`, move with `ease-in-out`.** Exits run at 75% of the enter time.
- **One overshoot.** `ease-settle` (about 6%) is allowed on the check mark, a filling beat, the toggle knob and the slider thumb. No bounces or elastic curves anywhere else.
- **Animate transform and opacity.** Do not animate blur radius or layout properties; a pane's blur stays constant while it moves.
- **Interruptible.** Sheets and the detail transition follow the finger and can be reversed mid-way.
- **Sight and touch together.** A haptic fires in the same frame as the visual event it belongs to. A haptic with no visible change is a bug.
- **Only for the user's own actions.** Nothing vibrates or animates because of a timer, a reminder arriving in-app, or a screen merely appearing.

## Catalogue

| Interaction | Visual | Timing | Haptic |
| --- | --- | --- | --- |
| Press row, card, button | scale to `press-scale` | `duration-press`, `ease-out` | none (primary button: `haptic-light`) |
| Press check, icon button, add | scale to `press-scale-small` | `duration-press` | `haptic-light` |
| Complete a beat | fill to `accent`, tick draws, title strikes | `duration-quick` then `duration-base`, `ease-settle` | `haptic-medium` as the tick lands |
| Beat segment fills | scaleX 0 → 1 from the left | `duration-base`, `ease-settle`, 80ms after the tick | none (same event) |
| All beats done | one light sweep across the bar | `duration-celebrate`, once per day | `haptic-success` |
| Undo | reverse of completion | `duration-quick` | `haptic-tick` |
| Next up swaps | old lifts 8px and fades; new rises 12px | `duration-quick` out, `duration-slow` in | none |
| Chip, segmented, tab change | tint or thumb slide; content cross-fades | `duration-quick` / `duration-base` | `haptic-tick` |
| Toggle | knob slides | `duration-base`, `ease-settle` | `haptic-tick` |
| Intensity slider detent | thumb snaps to stop | `duration-base` | `haptic-medium` at the chosen level |
| Quick add opens | sheet rises, scrim fades, plus rotates 45° | `duration-sheet`, `ease-out` | `haptic-light` |
| Item added | sheet leaves; new row rises into the list | 75% of `duration-sheet`, then `duration-base` | `haptic-medium` |
| Row → detail | row expands into the screen (shared element) | `duration-slow`, `ease-out` | none |
| Toast | rises 16px and fades in | `duration-base` | none |
| Pull to reveal search | field follows the finger, settles | spring to rest | `haptic-tick` at threshold |
| Destructive confirm | dialog fades and scales from 96% | `duration-base` | `haptic-warning` |
| List first load | rows rise 8px, 30ms apart, at most 6 staggered | `duration-base` each | none |

## Intensity settings

Both are in Settings → Feel, as four-stop sliders that preview themselves.

**Motion** multiplies travel distance and overshoot through `--m`.

| Level | Token | Effect |
| --- | --- | --- |
| Off | `motion-off` | No movement or scale. Changes cross-fade in 120ms. Progress still updates. |
| Subtle | `motion-subtle` | Distances × 0.6, `ease-settle` replaced by `ease-out`, no stagger. |
| Standard | `motion-standard` | As specified above. Default. |
| Expressive | `motion-expressive` | Distances × 1.25, durations + 15%, the day-complete sweep also lifts the number 4px. |

**Haptics** multiplies amplitude.

| Level | Token | Effect |
| --- | --- | --- |
| Off | `haptics-off` | No haptics. |
| Soft | `haptics-soft` | Amplitude × 0.5; `haptic-tick` events are dropped, so only completions are felt. |
| Balanced | `haptics-balanced` | Amplitude × 0.8. Default. |
| Firm | `haptics-firm` | Full amplitude. |

A separate toggle, "Completion only", keeps haptics for finishing a beat and the day and silences every selection tick.

## Platform mapping

- **iOS:** `haptic-tick` → selection feedback; `haptic-light` / `haptic-medium` → impact with intensity = token × level; `haptic-success` / `haptic-warning` → notification feedback. Prepare the generator on touch-down so the tap is not late.
- **Android:** `haptic-tick` → `CLOCK_TICK`; `haptic-light` → `VIRTUAL_KEY`; `haptic-medium` → `CONFIRM`; `haptic-warning` → `REJECT`; `haptic-success` → a two-primitive composition. Use the action constants at Balanced; use amplitude-scaled primitives for Soft and Firm where the device supports amplitude control, and fall back to Off/On where it does not.
- If the system has Reduce Motion or system haptics off, the matching slider is locked at Off and says "Following your system setting".
