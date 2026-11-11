Four button kinds: primary, glass, quiet and icon.

**You provide:** a verb label ("Mark done", "Save", "Later") and the kind.

- **Primary** (`accent` fill, `on-accent` label): the one thing to do now. At most one per screen or sheet.
- **Glass**: the alternative next to a primary ("Later").
- **Quiet** (`accent` text): low-commitment actions inside a card or toast ("Undo", "Skip today").
- **Danger** (`danger` text): destructive only, always behind a confirm.
- **Icon** (48px glass disc): top-bar actions. Always has an accessible label.
- Height is `tap-min` (48px) for every kind. Labels are sentence case, one or two words, no trailing punctuation.
- **Touch:** scale to `press-scale` on touch-down over `duration-press`, release with `ease-out`. Haptic `haptic-light` on touch-down for primary; none for quiet.
