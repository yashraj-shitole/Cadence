# Cadence landing page

A static page: `index.html` plus `assets/`. No framework and no build step is needed to serve it.

    node serve.mjs        # preview at http://localhost:4173

- `assets/cadence.css` is a copy of `cadence-handoff/screens/cadence.css` (tokens, palettes, components). Copy it again after changing the design system.
- `assets/landing.css` and `assets/app.js` are the page layout, the live phone screens and the performance governor.
- `assets/scene.js` is the bundled 3D scene. Its source is `src/scene.js`; rebuild with `npm install && npm run build`.
- Every "Download" button is a direct link to `Cadence.apk` on the latest GitHub release (`data-store-url` on `#page` in `index.html`), so it starts the download instead of opening GitHub. Each release must therefore include the installer under that exact name. The page also reads the latest version and size from GitHub's public release data.

## Performance modes
On load the page reads cores, memory, the graphics chip, data saver and the system reduce-motion setting, then picks a mode: full, balanced, light or essential. While running it measures frame rate and steps down one mode after three slow seconds in a row. Visitors can override it from the sliders button in the top bar. Add `?perf=full|light|off` to the URL to force a mode while testing.
