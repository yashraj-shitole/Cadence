The control that completes a beat: a circle for habits, a rounded square for tasks.

**You provide:** the kind (`cd-check` or `cd-check cd-check--task`), the state, and an accessible label that names the item ("Log Read 10 pages").

- The visible mark is 28px inside a 48px target. The whole row is not the target: tapping the row opens detail, tapping the check completes. This keeps accidental completions out.
- Shape carries the kind, so habits and tasks stay distinguishable without colour.
- **Minimum version** (habits): long-press the check, or pick "Did the minimum" in detail. It fills with `accent-soft` and an `accent` tick, and it counts as a beat.
- **Sequence on completion** (Standard motion): touch-down scale to `press-scale-small` + `haptic-light`; on release the fill cross-fades to `accent` in `duration-quick`, the tick draws in over `duration-base` with `ease-settle`, `haptic-medium` fires as the tick lands, then the beat bar segment fills. Undo reverses without haptics.
- Never auto-complete on swipe. Swipe is reserved for scrolling.
