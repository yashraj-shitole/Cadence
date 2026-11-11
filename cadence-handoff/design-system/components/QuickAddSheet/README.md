One bottom sheet that creates a task, a habit or a note.

**You provide:** the default type (the tab the user came from: Tasks → Task, Habits → Habit, Notes → Note, Today → Task).

- Opens with the title field focused and the keyboard up. Title plus the primary button is a complete entry; every other field is an optional chip.
- **Task chips:** due, duration, priority, tag. **Habit:** the sheet swaps to title, cue ("After…"), minimum, and time of day (Morning, Afternoon, Evening). **Note:** title, then the body field grows.
- Switching type keeps whatever was typed in the title.
- The primary label names the result: "Add task", "Add habit", "Save note".
- Dismiss by dragging down, tapping the scrim, or the rotated add button. If text was entered, it is kept as a draft for the next open, with no "Discard?" dialog.
- **Motion:** rises from the bottom over `duration-sheet` with `ease-out` while the `scrim` fades in; leaves at 75% of that with `ease-in`. It tracks the finger 1:1 when dragged. **Haptic:** `haptic-light` when it opens, `haptic-medium` on add.
