The floating glass tab bar: four destinations around the centre Quick add button.

**You provide:** the current destination.

- Order is fixed: Today, Habits, add, Tasks, Notes. Settings is not a tab; it opens from the icon at the top of Today.
- Every tab has an icon and a word. The current tab has `ink` label and an `accent` icon with a heavier stroke, so it is not marked by colour alone.
- The bar floats 12px above the bottom safe area with `space-4` side margins, on `glass-raised`. Lists scroll under it and get bottom padding equal to its height plus `space-8`.
- **Add button:** 56px `accent` disc, the screen's highest-contrast object after the Next up button. Tapping it opens the Quick add sheet and rotates the plus 45 degrees into a close mark.
- **Touch:** tabs scale to `press-scale-small` and play `haptic-tick` on change; the add button plays `haptic-light` on touch-down. Switching tabs cross-fades content over `duration-quick`; it does not slide, so position in each list is kept.
- The bar hides while the keyboard is up.
