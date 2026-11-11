The frosted tile every Cadence surface is made from, in two levels.

**You provide:** the content and a radius token. Add `cd-glass` for rows, cards and fields; add `cd-glass--raised` for anything that floats above the scroll (Next up, sheet, tab bar, toast).

- A tile is five things together: translucent fill (`glass`), the `glass-sheen` light from the top-left corner, a 1px `glass-edge` rim, the three-layer inset bevel in `shadow-glass`, and backdrop blur (`blur-glass`, saturate 180%). Drop any one and it reads as a flat grey box.
- Glass needs colour behind it to read as glass. Every screen has the three blurred glows (`glow-1`, `glow-2`, `glow-3`) on the gridded `backdrop`; tiles pick up whichever glow they sit over.
- The glows are washes, never shapes: always blurred by `blur-glow`, never a visible circle or blob outline.
- Two levels only. Rows sit on the backdrop, not inside another card. The one exception is controls inside a sheet.
- Text on glass is `ink` or `ink-muted`. Contrast is measured against the strongest glow at full strength, so do not lower the fill alpha to "show more colour".
- **Reduce transparency** (system setting or Settings → Appearance): set `data-glass="solid"` on the root. Fill becomes `surface-solid` / `surface-solid-raised`, blur is removed, the rim becomes `hairline`. Layout does not change.
- The high-contrast themes are always solid, with a `control-edge`-strength border and no glow.
- Performance: at most about eight blurred tiles on screen. While a list is being flung, rows may drop to solid and restore blur when it settles.
