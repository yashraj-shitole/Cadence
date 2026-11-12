/*
 * Cadence landing: 3D scene.
 * One fixed, transparent WebGL canvas behind the page. Every prop is anchored to a DOM "stage"
 * element ([data-stage]) so it scrolls with the page and works at any screen size.
 * Shapes follow the design language: frosted tiles, capsules, rounded squares (tasks), circles
 * (habits), clay rings (rest), small quiet dots (missed). No spheres or blobs in the backdrop.
 * The page drives it: frame(t, dt) from one shared rAF loop, so the page can measure performance
 * and downgrade or switch the scene off without the scene knowing why.
 */
import {
  WebGLRenderer, Scene, PerspectiveCamera, Group, Mesh, Color, Vector3, CatmullRomCurve3, TubeGeometry,
  CylinderGeometry, TorusGeometry, BoxGeometry, SphereGeometry, Shape, ExtrudeGeometry,
  MeshPhysicalMaterial, MeshStandardMaterial, AmbientLight, DirectionalLight, PMREMGenerator, MathUtils,
} from 'three';
import { RoundedBoxGeometry } from 'three/examples/jsm/geometries/RoundedBoxGeometry.js';
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js';

const FOV = 30;
const DIST = 10;
const VIS_H = 2 * DIST * Math.tan(MathUtils.degToRad(FOV / 2)); // world units visible at z = 0

const TIERS = {
  full: { dpr: 2, aa: true, env: true, fps: 60, lod: 3, seg: 6 },
  balanced: { dpr: 1.5, aa: true, env: true, fps: 45, lod: 2, seg: 4 },
  light: { dpr: 1, aa: false, env: false, fps: 30, lod: 1, seg: 3 },
};

/* ---------- small helpers ---------- */
const sp = (x = 0) => ({ x, v: 0 });
function step(o, target, dt, motion, k = 170, c = 14) {
  if (motion <= 0) { o.x = target; o.v = 0; return; }
  // Subtle motion drops the overshoot (critically damped), as in the design system.
  const damp = motion < 1 ? 2 * Math.sqrt(k) : c;
  o.v += ((target - o.x) * k - o.v * damp) * dt;
  o.x += o.v * dt;
}
const css = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

export function create(opts) {
  const canvas = opts.canvas;
  let tier = TIERS[opts.tier] ? opts.tier : 'balanced';
  let cfg = TIERS[tier];
  let motion = 1;
  let dirty = true;
  const pointer = { x: 0, y: 0, tx: 0, ty: 0 };

  const renderer = new WebGLRenderer({ canvas, alpha: true, antialias: cfg.aa, powerPreference: 'default' });
  renderer.setClearColor(0x000000, 0);
  const scene = new Scene();
  const camera = new PerspectiveCamera(FOV, 1, 0.1, 60);
  camera.position.set(0, 0, DIST);

  const amb = new AmbientLight(0xffffff, 0.9);
  const key = new DirectionalLight(0xffffff, 1.6); key.position.set(-3, 4, 6);
  const rim = new DirectionalLight(0xffffff, 0.6); rim.position.set(4, -2, 3);
  scene.add(amb, key, rim);

  let pmrem = null;
  let envTex = null;
  function applyEnv() {
    if (cfg.env) {
      if (!envTex) {
        pmrem = new PMREMGenerator(renderer);
        envTex = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
      }
      scene.environment = envTex;
    } else scene.environment = null;
  }

  /* ---------- materials (colours follow the active palette and theme) ---------- */
  const M = {
    glass: new MeshPhysicalMaterial({ transparent: true, opacity: 0.55, roughness: 0.14, metalness: 0, clearcoat: 1, clearcoatRoughness: 0.12, envMapIntensity: 0.9 }),
    glassRaised: new MeshPhysicalMaterial({ transparent: true, opacity: 0.8, roughness: 0.12, metalness: 0, clearcoat: 1, clearcoatRoughness: 0.1, envMapIntensity: 1 }),
    accent: new MeshStandardMaterial({ roughness: 0.38, metalness: 0.05 }),
    accentSoft: new MeshStandardMaterial({ roughness: 0.5, metalness: 0, transparent: true, opacity: 0.55 }),
    onAccent: new MeshStandardMaterial({ roughness: 0.4, metalness: 0 }),
    track: new MeshStandardMaterial({ roughness: 0.6, metalness: 0, transparent: true, opacity: 0.45 }),
    rest: new MeshStandardMaterial({ roughness: 0.5, metalness: 0 }),
    restSoft: new MeshStandardMaterial({ roughness: 0.6, metalness: 0, transparent: true, opacity: 0.4 }),
    miss: new MeshStandardMaterial({ roughness: 0.6, metalness: 0 }),
    glow1: new MeshStandardMaterial({ roughness: 0.8, metalness: 0, transparent: true, opacity: 0.55 }),
    glow2: new MeshStandardMaterial({ roughness: 0.8, metalness: 0, transparent: true, opacity: 0.55 }),
  };
  function readColors() {
    const dark = new Color(css('--backdrop')).getHSL({}).l < 0.4;
    M.glass.color.set(dark ? '#9fb0cc' : '#ffffff');
    M.glass.opacity = dark ? 0.26 : 0.55;
    M.glassRaised.color.set(dark ? '#b8c6e0' : '#ffffff');
    M.glassRaised.opacity = dark ? 0.34 : 0.82;
    M.accent.color.set(css('--accent'));
    M.accentSoft.color.set(css('--accent'));
    M.onAccent.color.set(css('--on-accent'));
    M.track.color.set(css('--control-edge'));
    M.rest.color.set(css('--rest'));
    M.restSoft.color.set(css('--rest'));
    M.miss.color.set(css('--control-edge'));
    M.glow1.color.set(css('--glow-1'));
    M.glow2.color.set(css('--glow-2'));
    amb.intensity = dark ? 0.55 : 0.9;
    key.intensity = dark ? 1.1 : 1.6;
    dirty = true;
  }

  /* ---------- geometry builders (nominal width ~1 world unit) ---------- */
  const rbox = (w, h, d, r, s) => new RoundedBoxGeometry(w, h, d, s ?? cfg.seg, r);
  const mesh = (g, m) => new Mesh(g, m);
  const slab = (w, h, d, r, m = M.glass) => mesh(rbox(w, h, d, r), m);

  function buildBeats() {
    const g = new Group();
    g.add(slab(1.06, 0.3, 0.07, 0.1, M.glassRaised));
    const fills = [];
    for (let i = 0; i < 8; i++) {
      const x = -0.455 + i * 0.13;
      const t = mesh(rbox(0.11, 0.07, 0.05, 0.034), M.track); t.position.set(x, 0, 0.05); g.add(t);
      const f = mesh(rbox(0.11, 0.07, 0.06, 0.034), M.accent);
      f.position.set(x, 0, 0.065); f.geometry.translate(0.055, 0, 0); f.position.x = x - 0.055; f.scale.x = 0.001;
      g.add(f); fills.push({ mesh: f, s: sp(0) });
    }
    g.userData.fills = fills;
    return g;
  }
  function tickMesh(radius = 0.034) {
    const pts = [new Vector3(-0.15, 0.0, 0), new Vector3(-0.045, -0.115, 0), new Vector3(0.16, 0.115, 0)];
    const curve = new CatmullRomCurve3(pts, false, 'catmullrom', 0);
    const tube = mesh(new TubeGeometry(curve, 20, radius, 10, false), M.onAccent);
    const cap = new SphereGeometry(radius, 12, 10);
    const a = mesh(cap, M.onAccent); a.position.copy(pts[0]);
    const b = mesh(cap, M.onAccent); b.position.copy(pts[2]);
    const t = new Group(); t.add(tube, a, b); return t;
  }
  function buildTick() {
    const g = new Group();
    const tile = slab(0.62, 0.62, 0.14, 0.18, M.accent); g.add(tile);
    const t = tickMesh(); t.position.z = 0.09; g.add(t);
    g.userData.pop = sp(1);
    g.userData.tick = t;
    return g;
  }
  function buildHabitCheck() {
    const g = new Group();
    const disc = mesh(new CylinderGeometry(0.3, 0.3, 0.12, 40), M.accent); disc.rotation.x = Math.PI / 2; g.add(disc);
    const t = tickMesh(); t.position.z = 0.08; g.add(t);
    return g;
  }
  function dashRing(radius, n, m) {
    const g = new Group();
    for (let i = 0; i < n; i++) {
      const a = (i / n) * Math.PI * 2;
      const b = mesh(rbox(0.05, 0.022, 0.022, 0.01, 2), m);
      b.position.set(Math.cos(a) * radius, Math.sin(a) * radius, 0); b.rotation.z = a + Math.PI / 2; g.add(b);
    }
    return g;
  }
  function buildDots() {
    const g = new Group();
    g.add(slab(1.1, 0.28, 0.06, 0.1, M.glassRaised));
    const R = 0.06, gap = 0.145, x0 = -gap * 3;
    const kinds = ['d', 'd', 'm', 'r', 'x', 'd', 't'];
    kinds.forEach((k, i) => {
      const x = x0 + i * gap; const n = new Group(); n.position.set(x, 0, 0.05);
      if (k === 'd') { const c = mesh(new CylinderGeometry(R, R, 0.05, 28), M.accent); c.rotation.x = Math.PI / 2; n.add(c); }
      if (k === 'm') { const c = mesh(new CylinderGeometry(R * 0.82, R * 0.82, 0.03, 28), M.accentSoft); c.rotation.x = Math.PI / 2; n.add(c); n.add(mesh(new TorusGeometry(R * 0.85, 0.014, 8, 28), M.accent)); }
      if (k === 'r') n.add(mesh(new TorusGeometry(R * 0.85, 0.016, 8, 28), M.rest));
      if (k === 'x') { const c = mesh(new CylinderGeometry(0.022, 0.022, 0.03, 16), M.miss); c.rotation.x = Math.PI / 2; n.add(c); }
      if (k === 't') n.add(dashRing(R * 0.85, 9, M.miss));
      g.add(n);
    });
    return g;
  }
  function buildTasks() {
    const g = new Group();
    g.add(slab(1.0, 0.34, 0.07, 0.11, M.glassRaised));
    const sq = slab(0.2, 0.2, 0.06, 0.06, M.track); sq.position.set(-0.36, 0, 0.06); g.add(sq);
    const sqIn = slab(0.13, 0.13, 0.07, 0.04, M.glassRaised); sqIn.position.set(-0.36, 0, 0.075); g.add(sqIn);
    [0.34, 0.2].forEach((w, i) => { const l = slab(w, 0.04, 0.03, 0.02, M.track); l.position.set(-0.05 + (w - 0.34) / 2 * 0.0 - 0.0 + (w) / 2 - 0.17, 0.045 - i * 0.09, 0.06); g.add(l); });
    // priority flag
    const sh = new Shape(); sh.moveTo(0, 0); sh.quadraticCurveTo(0.07, 0.05, 0.14, 0.0); sh.lineTo(0.14, -0.1); sh.quadraticCurveTo(0.07, -0.05, 0, -0.1); sh.lineTo(0, 0);
    const flag = mesh(new ExtrudeGeometry(sh, { depth: 0.025, bevelEnabled: true, bevelSize: 0.008, bevelThickness: 0.008, bevelSegments: 2 }), M.accent);
    flag.position.set(0.33, 0.07, 0.06); g.add(flag);
    const pole = slab(0.022, 0.2, 0.022, 0.01, M.accent); pole.position.set(0.328, -0.0, 0.07); g.add(pole);
    return g;
  }
  function buildNotes() {
    const g = new Group();
    [[-0.12, 0.1, 0.12], [0.02, 0, -0.06], [0.14, -0.08, 0.02]].forEach(([x, y, r], i) => {
      const p = new Group();
      p.add(slab(0.56, 0.74, 0.03, 0.06, i === 2 ? M.glassRaised : M.glass));
      for (let l = 0; l < 4; l++) { const ln = slab(l === 3 ? 0.2 : 0.38, 0.03, 0.02, 0.012, M.track); ln.position.set(l === 3 ? -0.09 : 0, 0.16 - l * 0.09, 0.03); p.add(ln); }
      p.position.set(x, y, i * 0.06); p.rotation.z = r; g.add(p);
    });
    const pin = mesh(new CylinderGeometry(0.035, 0.035, 0.05, 20), M.accent); pin.rotation.x = Math.PI / 2; pin.position.set(0.2, 0.2, 0.22); g.add(pin);
    return g;
  }
  function buildPlus() {
    const g = new Group();
    const d = mesh(new CylinderGeometry(0.3, 0.3, 0.12, 40), M.accent); d.rotation.x = Math.PI / 2; g.add(d);
    const bars = new Group();
    const h = slab(0.28, 0.056, 0.05, 0.024, M.onAccent), v = slab(0.056, 0.28, 0.05, 0.024, M.onAccent);
    h.position.z = v.position.z = 0.08; bars.add(h, v); g.add(bars);
    g.userData.bars = bars; g.userData.open = sp(0);
    return g;
  }
  function buildSliders() {
    const g = new Group();
    g.add(slab(1.0, 0.56, 0.07, 0.12, M.glassRaised));
    const thumbs = [];
    [0.14, -0.14].forEach((y, row) => {
      const tr = slab(0.8, 0.045, 0.04, 0.02, M.track); tr.position.set(0, y, 0.05); g.add(tr);
      for (let i = 0; i < 4; i++) { const s = mesh(new CylinderGeometry(0.012, 0.012, 0.02, 10), M.miss); s.rotation.x = Math.PI / 2; s.position.set(-0.4 + i * (0.8 / 3), y, 0.075); g.add(s); }
      const fill = mesh(rbox(0.8, 0.045, 0.05, 0.02, 3), M.accent); fill.geometry.translate(0.4, 0, 0); fill.position.set(-0.4, y, 0.06); g.add(fill);
      const th = new Group(); th.position.set(0, y, 0.1);
      const disc = mesh(new CylinderGeometry(0.07, 0.07, 0.04, 28), M.glassRaised); disc.rotation.x = Math.PI / 2; th.add(disc);
      const ring = mesh(new TorusGeometry(0.07, 0.012, 8, 28), M.accent); th.add(ring); g.add(th);
      thumbs.push({ th, fill, s: sp(row === 0 ? 2 : 1), target: row === 0 ? 2 : 1 });
    });
    g.userData.thumbs = thumbs; g.userData.cycle = 0;
    return g;
  }
  function buildSlabs(a = 0) {
    const g = new Group();
    const big = slab(1.1, 1.5, 0.08, 0.16, M.glass); g.add(big);
    const tint = slab(0.86, 1.26, 0.04, 0.12, a ? M.glow2 : M.glow1); tint.position.z = -0.07; g.add(tint);
    return g;
  }
  function buildGrid() {
    const g = new Group();
    const cells = [];
    const pitch = 1 / 7, size = pitch * 0.84;
    for (let i = 0; i < 35; i++) {
      const c = new Group();
      const col = i % 7, row = Math.floor(i / 7);
      c.position.set(-0.5 + pitch * (col + 0.5), (5 / 14) - pitch * (row + 0.5), 0);
      const outer = mesh(rbox(size, size, 0.05, size * 0.26, 3), M.accent);
      const inner = mesh(rbox(size * 0.7, size * 0.7, 0.06, size * 0.18, 2), M.glassRaised); inner.position.z = 0.004;
      const dot = mesh(new CylinderGeometry(size * 0.09, size * 0.09, 0.04, 14), M.miss); dot.rotation.x = Math.PI / 2; dot.position.z = 0.03;
      c.add(outer, inner, dot);
      g.add(c);
      cells.push({ group: c, outer, inner, dot, state: 'f', pop: sp(1), base: c.position.clone() });
    }
    g.userData.cells = cells;
    return g;
  }
  function setCellState(cell, st) {
    cell.state = st;
    const o = cell.outer, i = cell.inner, d = cell.dot;
    i.visible = d.visible = false; o.visible = true;
    if (st === 'd') { o.material = M.accent; }
    else if (st === 'm') { o.material = M.accent; i.visible = true; i.material = M.accentSoft; i.scale.setScalar(1); }
    else if (st === 'r') { o.material = M.rest; i.visible = true; i.material = M.restSoft; }
    else if (st === 'x') { o.material = M.track; d.visible = true; }
    else { o.material = M.track; i.visible = true; i.material = M.glass; i.scale.setScalar(1.12); o.scale.setScalar(1); }
    if (st !== 'f') i.scale.setScalar(1);
  }

  /* ---------- props (anchored to stages) ---------- */
  // nx, ny: offset from the stage centre in stage widths; s: scale in stage widths; z: depth (world units)
  const props = [];
  const motifs = {}; // tour motifs
  function addProp(def) {
    const p = Object.assign({ nx: 0, ny: 0, s: 1, z: 0, lod: 1, tilt: 0.25, float: 0.02, phase: Math.random() * 6, rot: [0, 0, 0], vis: sp(0), motif: null }, def);
    p.group.visible = false; scene.add(p.group); props.push(p); return p;
  }

  function buildAll() {
    // Hero: tiles around the live phone.
    addProp({ stage: 'hero', group: buildSlabs(0), nx: 0.46, ny: 0.05, z: -1.4, s: 0.95, lod: 1, rot: [0.08, -0.28, 0.1], tilt: 0.18, float: 0.03 });
    addProp({ stage: 'hero', group: buildSlabs(1), nx: -0.5, ny: -0.12, z: -1.2, s: 0.8, lod: 2, rot: [-0.06, 0.3, -0.12], tilt: 0.2, float: 0.025 });
    const beats = addProp({ stage: 'hero', group: buildBeats(), nx: 0.8, ny: -0.5, z: 0.5, s: 0.85, lod: 1, rot: [-0.12, -0.3, 0.06], tilt: 0.6, float: 0.025 });
    const tick = addProp({ stage: 'hero', group: buildTick(), nx: 0.62, ny: 0.34, z: 0.6, s: 0.4, lod: 1, rot: [0.1, -0.35, -0.08], tilt: 0.8, float: 0.035 });
    addProp({ stage: 'hero', group: buildDots(), nx: -0.7, ny: 0.86, z: 0.4, s: 0.62, lod: 2, rot: [-0.1, 0.35, 0.05], tilt: 0.6, float: 0.03 });
    addProp({ stage: 'hero', group: buildPlus(), nx: -0.74, ny: -0.62, z: 0.7, s: 0.3, lod: 3, rot: [0.2, 0.4, 0], tilt: 0.9, float: 0.04 });
    addProp({ stage: 'hero', group: buildHabitCheck(), nx: 0.72, ny: 0.74, z: 0.9, s: 0.24, lod: 3, rot: [0.3, -0.3, 0], tilt: 0.9, float: 0.04 });
    heroBeats = beats.group; heroTick = tick.group;

    // Tour: depth tiles plus a motif that follows the selected screen.
    addProp({ stage: 'tour', group: buildSlabs(1), nx: -0.34, ny: 0.1, z: -1.5, s: 0.9, lod: 2, rot: [0.06, -0.3, 0.08], tilt: 0.2 });
    const slot = new Group();
    const defs = { today: buildBeats(), habits: buildDots(), detail: buildHabitCheck(), tasks: buildTasks(), notes: buildNotes(), add: buildPlus(), settings: buildSliders() };
    Object.keys(defs).forEach((k) => { const m = defs[k]; m.visible = false; m.scale.setScalar(0.0001); slot.add(m); motifs[k] = { group: m, vis: sp(0), target: 0 }; });
    addProp({ stage: 'tour', group: slot, nx: 0.84, ny: -0.84, z: 0.6, s: 0.5, m: { nx: 0.4, ny: -1.0, s: 0.42 }, lod: 1, rot: [-0.08, -0.32, 0.04], tilt: 0.7, float: 0.03 });

    // Streaks: the consistency grid itself.
    gridGroup = buildGrid();
    addProp({ stage: 'streak', group: gridGroup, nx: 0, ny: 0, z: 0, s: 1, lod: 1, rot: [0, 0, 0], tilt: 0.05, float: 0, scrollRot: 0 });
    addProp({ stage: 'streak', group: buildSlabs(0), nx: 0.42, ny: 0.2, z: -1.2, s: 0.9, lod: 2, rot: [0.05, -0.3, 0.1], tilt: 0.15 });

    // Feel: sliders and a plus.
    addProp({ stage: 'feel', group: buildSliders(), nx: 0, ny: 0, z: 0.4, s: 0.9, lod: 1, rot: [-0.08, 0.2, 0.02], tilt: 0.5 });
    addProp({ stage: 'feel', group: buildSlabs(1), nx: -0.3, ny: 0.1, z: -1.3, s: 0.8, lod: 2, rot: [0.06, 0.3, -0.08], tilt: 0.2 });

    // Get: a large tick.
    addProp({ stage: 'get', group: buildTick(), nx: 0, ny: 0, z: 0.4, s: 0.9, lod: 1, rot: [0.1, -0.3, -0.05], tilt: 0.9, float: 0.035 });
    addProp({ stage: 'get', group: buildBeats(), nx: 0.1, ny: 0.62, z: 0.2, s: 1.4, lod: 2, rot: [-0.1, 0.25, 0.04], tilt: 0.5 });
  }
  let heroBeats = null, heroTick = null, gridGroup = null;

  /* ---------- layout / resize ---------- */
  let vw = 1, vh = 1, k = 1;
  const stageEls = {};
  function findStages() {
    document.querySelectorAll('[data-stage]').forEach((el) => { stageEls[el.dataset.stage] = el; });
  }
  function resize() {
    vw = window.innerWidth; vh = window.innerHeight;
    const dpr = Math.min(window.devicePixelRatio || 1, cfg.dpr);
    renderer.setPixelRatio(dpr);
    renderer.setSize(vw, vh, false);
    camera.aspect = vw / vh; camera.updateProjectionMatrix();
    k = VIS_H / vh;
    dirty = true;
  }

  /* ---------- public state setters ---------- */
  let beatsDone = 4;
  function setBeats(n) { beatsDone = n; dirty = true; }
  function pulseTick() { if (heroTick) heroTick.userData.pop.v = 7; dirty = true; }
  function setMotif(name) { Object.keys(motifs).forEach((m) => { motifs[m].target = m === name ? 1 : 0; }); dirty = true; }
  function setGrid(states) {
    if (!gridGroup) return;
    gridGroup.userData.cells.forEach((c, i) => {
      if (c.state !== states[i]) { setCellState(c, states[i]); c.pop.v = 6; }
    });
    dirty = true;
  }
  function setSliders(motionIdx, hapticIdx) {
    props.forEach((p) => p.group.userData.thumbs && p.group.userData.thumbs.forEach((t, i) => { t.target = i === 0 ? motionIdx : hapticIdx; }));
    Object.values(motifs).forEach((m) => m.group.userData.thumbs && m.group.userData.thumbs.forEach((t, i) => { t.target = i === 0 ? motionIdx : hapticIdx; }));
    dirty = true;
  }
  function setPlusOpen(v) { Object.values(motifs).forEach((m) => { if (m.group.userData.open) m.group.userData.open.t = v ? 1 : 0; }); }

  function setTier(name) {
    if (!TIERS[name]) return;
    tier = name; cfg = TIERS[name];
    renderer.dispose && 0;
    applyEnv(); resize(); dirty = true;
  }
  function setMotion(m) { motion = m; dirty = true; }
  function setPointer(x, y) { pointer.tx = x; pointer.ty = y; dirty = true; }
  function invalidate() { dirty = true; }

  /* ---------- per-frame ---------- */
  const tmp = new Vector3();
  let last = 0, clock = 0, rendered = 0;
  function frame(t, dt) {
    const animate = motion > 0;
    if (!animate && !dirty) return false;
    if (cfg.fps < 60 && t - last < 1000 / cfg.fps - 2) return false;
    last = t;
    const d = Math.min(0.033, dt || 0.016);
    clock += d;

    pointer.x += (pointer.tx - pointer.x) * Math.min(1, d * 6);
    pointer.y += (pointer.ty - pointer.y) * Math.min(1, d * 6);

    let anyVisible = false;
    for (const p of props) {
      const el = stageEls[p.stage];
      if (!el || p.lod > cfg.lod) { p.group.visible = false; continue; }
      const r = el.getBoundingClientRect();
      const on = r.bottom > -vh * 0.3 && r.top < vh * 1.3 && r.width > 40;
      step(p.vis, on ? 1 : 0, d, motion, 120, 16);
      if (p.vis.x < 0.01 && !on) { p.group.visible = false; continue; }
      anyVisible = true; p.group.visible = true;

      const q = vw < 700 && p.m ? p.m : p;
      const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
      const sc = Math.max(0.0001, r.width * k * q.s * Math.max(0, p.vis.x));
      p.group.scale.setScalar(sc);
      const bob = Math.sin(clock * 0.9 + p.phase) * p.float * motion;
      p.group.position.set(
        (cx - vw / 2) * k + q.nx * r.width * k,
        -(cy - vh / 2) * k - q.ny * r.width * k + bob * sc * 4,
        p.z
      );
      const sr = p.scrollRot === 0 ? 0 : ((cy - vh / 2) / vh) * 0.5 * motion;
      const px = pointer.x * p.tilt * 0.5 * motion, py = pointer.y * p.tilt * 0.4 * motion;
      p.group.rotation.set(p.rot[0] + py + sr * 0.3, p.rot[1] + px + sr, p.rot[2], 'XYZ');
      // Phone-sized parallax: nearer props drift a little further with the pointer.
      p.group.position.x += pointer.x * p.z * 0.05 * motion * 0.0;
    }

    // hero beats fill to the live count
    if (heroBeats) heroBeats.userData.fills.forEach((f, i) => {
      step(f.s, i < beatsDone ? 1 : 0, d, motion, 200, 15);
      f.mesh.scale.x = Math.max(0.001, f.s.x);
    });
    if (heroTick) {
      const pop = heroTick.userData.pop; step(pop, 1, d, motion, 220, 9);
      heroTick.scale.setScalar(1);
      heroTick.userData.tick.scale.setScalar(Math.max(0.01, pop.x));
    }
    // tour motifs
    for (const key2 in motifs) {
      const m = motifs[key2];
      step(m.vis, m.target, d, motion, 150, 15);
      const v = Math.max(0, m.vis.x);
      m.group.visible = v > 0.01;
      m.group.scale.setScalar(Math.max(0.0001, v));
      const ud = m.group.userData;
      if (ud.fills) ud.fills.forEach((f, i) => { step(f.s, i < beatsDone ? 1 : 0, d, motion, 200, 15); f.mesh.scale.x = Math.max(0.001, f.s.x); });
      if (ud.open) { step(ud.open, ud.open.t ?? ((Math.sin(clock * 0.8) > 0.2) ? 1 : 0), d, motion, 160, 16); ud.bars.rotation.z = ud.open.x * Math.PI / 4; }
      if (ud.thumbs) ud.thumbs.forEach((th) => { step(th.s, th.target, d, motion, 170, 14); const x = -0.4 + (th.s.x / 3) * 0.8; th.th.position.x = x; th.fill.scale.x = Math.max(0.001, (th.s.x / 3) * 0.8 / 0.8 + 0.0001); });
    }
    // grid pops
    if (gridGroup) gridGroup.userData.cells.forEach((c) => {
      step(c.pop, 1, d, motion, 220, 11);
      c.group.scale.setScalar(Math.max(0.01, c.pop.x));
    });
    // standalone slider props & plus in other stages: gentle idle
    props.forEach((p) => {
      const ud = p.group.userData;
      if (ud.thumbs) ud.thumbs.forEach((th) => { step(th.s, th.target, d, motion, 170, 14); th.th.position.x = -0.4 + (th.s.x / 3) * 0.8; th.fill.scale.x = Math.max(0.001, th.s.x / 3 + 0.0001); });
      if (ud.open && p.group.visible) { step(ud.open, (Math.sin(clock * 0.8 + p.phase) > 0.2) ? 1 : 0, d, motion, 160, 16); ud.bars.rotation.z = ud.open.x * Math.PI / 4; }
      if (ud.fills && p.group !== heroBeats) { ud.fills.forEach((f, i) => { const on = ((Math.floor(clock * 0.9 + p.phase) % 9) > i) ? 1 : 0; step(f.s, on, d, motion, 200, 15); f.mesh.scale.x = Math.max(0.001, f.s.x); }); }
    });

    if (!anyVisible) { dirty = false; renderer.clear(); return false; }
    renderer.render(scene, camera);
    rendered++;
    dirty = false;
    return true;
  }

  /* ---------- boot ---------- */
  findStages();
  buildAll();
  readColors();
  applyEnv();
  resize();
  setCellState; // keep reference
  // Seed grid cells so first paint is never empty.
  gridGroup.userData.cells.forEach((c) => setCellState(c, 'f'));
  window.addEventListener('resize', resize, { passive: true });

  function dispose() {
    window.removeEventListener('resize', resize);
    props.forEach((p) => scene.remove(p.group));
    Object.values(M).forEach((m) => m.dispose());
    scene.traverse((o) => { if (o.geometry) o.geometry.dispose(); });
    if (envTex) envTex.dispose();
    if (pmrem) pmrem.dispose();
    renderer.dispose();
    renderer.forceContextLoss && renderer.forceContextLoss();
  }

  return {
    frame, resize, setTier, setMotion, setPointer, setBeats, pulseTick, setMotif, setGrid, setSliders,
    setPlusOpen, readColors, invalidate, dispose,
    info: () => ({ tier, rendered, gl: renderer.getContext().getParameter(renderer.getContext().VERSION) }),
  };
}

export const supported = () => {
  try {
    const c = document.createElement('canvas');
    const gl = c.getContext('webgl2') || c.getContext('webgl');
    return !!gl;
  } catch (e) { return false; }
};
