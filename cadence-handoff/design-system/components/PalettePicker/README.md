A grid of six rounded-square tiles for choosing the colour palette.

**You provide:** the palette ids and names, and the selected one.

- Lives in Settings, Appearance, above Theme. Choosing a tile restyles the whole app at once.
- Each tile previews its own glow wash and a short bar in its accent. The selected tile has a 2px `ink` outline and a check badge, so selection never depends on colour.
- Tiles are 56px tall with the name below; the whole cell is at least 48px. Three per row.
- Tiles are rounded squares (`radius-control`), never circles.
- **Motion:** press scale 0.97; the check badge settles in over `duration-base`. **Haptic:** `haptic-tick`.
- In high-contrast themes the tile gains a 2px `control-edge` border.
