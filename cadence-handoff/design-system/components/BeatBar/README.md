Day progress as one number over a segmented bar, one segment per beat due today.

**You provide:** `done` and `total`. The component writes the number ("4/8"), the words "beats done", and the segments.

- This is the only large number on Today. Do not add percentages, rings or a second metric beside it.
- It sits directly on the backdrop, not in a card, so every word in it is `ink`: the number (`numeral`), "beats done" and "4 to go". `ink-muted` fails on bare backdrop and glow.
- One segment per beat up to 12; above 12, segments merge into a continuous bar with the same number above.
- Rest-day habits are removed from `total` for that day rather than shown as done.
- **Motion:** a segment fills left to right over `duration-base` with `ease-settle`. When the last one fills, the whole bar brightens once left to right over `duration-celebrate` and `haptic-success` plays. That is the entire celebration: no confetti, no modal.
- With motion Off, segments change colour without movement.
