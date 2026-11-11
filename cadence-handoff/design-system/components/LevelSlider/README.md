A four-stop slider for intensity settings: Motion and Haptics.

**You provide:** four labels, the current stop (0 to 3) and an accessible name.

- Four detents, each named. Stop 0 is always Off; stop 2 is the default.
- **Motion:** Off, Subtle, Standard, Expressive map to the `motion-*` multipliers. The control animates with the new setting the moment the thumb lands, so the change previews itself.
- **Haptics:** Off, Soft, Balanced, Firm map to the `haptics-*` amplitude multipliers. Each detent plays `haptic-medium` at its own strength as the thumb crosses it, so the user feels the level they are choosing.
- If the system has Reduce Motion on, the Motion slider is locked at Off with the line "Following your system setting". Same for system haptics off.
- Thumb is 28px in a 48px-tall track; the whole track is draggable and tappable.
