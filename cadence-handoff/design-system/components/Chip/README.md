Chips filter a list; tags label an item.

**You provide:** for a chip row, the options and the selected one; for a tag, a one-word label.

- **Chip** (40px tall, 48px target, pill): single-select filter rows on Tasks and Notes. Selected = `accent` border plus `accent-soft` tint, label stays `ink`. The row scrolls horizontally; "All" is always first.
- **Tag** (24px, `radius-tag`, `track` fill, `ink-muted` label): read-only, inside rows and cards on glass only. At most two per row.
- Tags have no per-tag colours. One accent means colour never needs decoding.
- **Touch:** chip press scales to `press-scale`; selection plays `haptic-tick`.
