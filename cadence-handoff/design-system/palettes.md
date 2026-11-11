# Colour palettes

Cadence ships six palettes. A palette changes only the colours that carry brand and mood; surfaces, ink, borders, danger and every shadow stay the same, so contrast never depends on which palette is chosen.

**What a palette sets:** `accent`, `on-accent`, `accent-soft`, `shadow-accent`, `glow-1`, `glow-2`, `glow-3`, `rest`, `rest-soft`. Each has a value in all four themes (light, dark, high-contrast light, high-contrast dark). The glows are removed in the high-contrast themes; the accent and rest colours are darker and lighter there.

**How it is applied:** put `data-palette="<id>"` on the same element as `data-theme`. With no attribute the default, Meadow, applies. The values are in `palettes.json` and in the CSS bundle.

| Palette | id | Accent (light / dark) | Light glows | Mood |
|---|---|---|---|---|
| Meadow | `meadow` | #1d6a3a / #9fe0a8 | #a9d98f, #eadf8c, #f6efc8 | Fresh green with a yellow wash. Default. |
| Ultramarine | `ultramarine` | #2b3fd4 / #a3afff | #8fa0f5, #a8c79a, #f3e3b8 | The original cool blue. |
| Dusk | `dusk` | #6431c2 / #cdb6ff | #c4a8f2, #f0b8d6, #f6e3cc | Soft violet with a rose glow. |
| Rose | `rose` | #b0205a / #ffb3cc | #f5a9c2, #f8d0a2, #fbeacf | Warm raspberry over peach. |
| Lagoon | `lagoon` | #0b6a74 / #86dbe2 | #8fd5da, #a6c6f2, #e6f1c9 | Clear teal with a pale blue wash. |
| Graphite | `graphite` | #2a3140 / #d6dce8 | #ccd1d9, #dfe3e8, #eee9df | Neutral and quiet. Ink-coloured accent. |

## Contrast
Measured against the strongest glow of each palette, under both glass levels, using the weakest palette for each figure:

| Pair | Light | Dark |
|---|---|---|
| `ink` on glass | 12.8:1 | 10.7:1 |
| `ink-muted` on glass | 5.7:1 | 5.7:1 |
| `accent` text on glass | 5.1:1 | 6.6:1 |
| `on-accent` on `accent` | 6.3:1 | 8.8:1 |
| `rest` on glass | 5.4:1 | 7.2:1 |
| `danger` on glass | 5.4:1 | 5.9:1 |

High-contrast themes are at least 9:1 for accent on a solid surface.

## Rules
- A new palette must pass the table above before it ships. Run the same checks.
- Rest days use a warm clay (or the sage the original blue palette used), never the accent hue, so a rest ring is not read as "done".
- Danger stays one rust red in every palette; it is always paired with a word.
- Changing the palette is instant at Motion: Off and a 240ms cross-fade otherwise. It gives a `haptic-tick`.
