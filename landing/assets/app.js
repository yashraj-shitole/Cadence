/* Cadence landing page: interaction, live phone screens, and the performance governor.
   No framework. The 3D scene (assets/scene.js) is loaded only when the device can run it. */
(function () {
  'use strict';
  const $ = (s, r) => (r || document).querySelector(s);
  const $$ = (s, r) => Array.from((r || document).querySelectorAll(s));
  const html = document.documentElement;
  const mq = (q) => (window.matchMedia ? matchMedia(q) : { matches: false, addEventListener() {} });
  const MQ = { dark: mq('(prefers-color-scheme: dark)'), motion: mq('(prefers-reduced-motion: reduce)'), contrast: mq('(prefers-contrast: more)'), hover: mq('(hover: hover) and (pointer: fine)') };
  const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

  /* ================= icons (Lucide geometry, as in the app) ================= */
  const IC = {
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/>',
    repeat: '<path d="m17 2 4 4-4 4"/><path d="M3 11v-1a4 4 0 0 1 4-4h14"/><path d="m7 22-4-4 4-4"/><path d="M21 13v1a4 4 0 0 1-4 4H3"/>',
    plus: '<path d="M5 12h14M12 5v14"/>',
    tasks: '<path d="m9 11 3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
    notes: '<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4M16 13H8M16 17H8"/>',
    'file-text': '<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4M16 13H8M16 17H8"/>',
    sliders: '<path d="M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4"/>',
    search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
    clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
    timer: '<path d="M10 2h4M12 14l3-3"/><circle cx="12" cy="14" r="8"/>',
    flag: '<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><path d="M4 22v-7"/>',
    tag: '<path d="M12.6 2.6A2 2 0 0 0 11.2 2H4a2 2 0 0 0-2 2v7.2a2 2 0 0 0 .6 1.4l8.7 8.7a2.4 2.4 0 0 0 3.4 0l6.6-6.6a2.4 2.4 0 0 0 0-3.4z"/><circle cx="7.5" cy="7.5" r=".5"/>',
    pin: '<path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z"/>',
    undo: '<path d="M3 7v6h6"/><path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13"/>',
    'chevron-left': '<path d="m15 18-6-6 6-6"/>', 'chevron-right': '<path d="m9 18 6-6-6-6"/>', 'chevron-down': '<path d="m6 9 6 6 6-6"/>',
    more: '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
    x: '<path d="M18 6 6 18M6 6l12 12"/>',
    check: '<path d="M20 6 9 17l-5-5"/>',
    phone: '<rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/>',
    'user-off': '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="m17 8 5 5M22 8l-5 5"/>',
    'wifi-off': '<path d="M12 20h.01M8.5 16.4a5 5 0 0 1 7 0M5 12.9a10 10 0 0 1 5.2-2.7M19 12.9a10 10 0 0 0-2-1.5M2 8.8a15 15 0 0 1 4.2-2.6M22 8.8a15 15 0 0 0-11.3-3.8M2 2l20 20"/>',
    pointer: '<path d="m9 9 5 12 1.8-5.2L21 14Z"/><path d="M7.2 2.2 8 5.1M5.1 8 2.2 7.2M14 4.1 12 6M6 12l-1.9 2"/>',
    'arrow-right': '<path d="M5 12h14M12 5l7 7-7 7"/>',
    database: '<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14a9 3 0 0 0 18 0V5M3 12a9 3 0 0 0 18 0"/>',
    bell: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0"/>',
    download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/>',
    eye: '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/>',
    copy: '<rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>',
    checklist: '<path d="m3 17 2 2 4-4M3 7l2 2 4-4M13 6h8M13 12h8M13 18h8"/>',
    grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
    edit: '<path d="M12 20h9"/><path d="M16.4 3.6a2 2 0 0 1 2.8 2.8L7 18.6 3 20l1.4-4z"/>',
  };
  const icon = (n, sm, cls) => '<svg class="' + (cls || 'ic') + (sm ? ' ' + (cls ? 'cd-icon--sm' : 'ic--sm') : '') + '" viewBox="0 0 24 24" aria-hidden="true">' + (IC[n] || '') + '</svg>';
  const ci = (n, sm) => icon(n, sm, 'cd-icon'); // in-phone icon, design-system class
  const TICK = '<i><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg></i>';
  $$('i[data-icon]').forEach((el) => { el.innerHTML = icon(el.dataset.icon, el.hasAttribute('data-sm')); });

  /* ================= settings store ================= */
  const KEY = 'cadence-landing';
  const S = { palette: 'meadow', theme: 'system', contrast: 'normal', glass: 'frosted', backdrop: 'glow', motion: 2, haptics: 2, completionOnly: false, perf: 'auto' };
  try { Object.assign(S, JSON.parse(localStorage.getItem(KEY) || '{}')); } catch (e) {}
  const qs = new URLSearchParams(location.search);
  if (qs.get('perf')) S.perf = qs.get('perf');
  if (qs.get('theme')) S.theme = qs.get('theme');
  if (qs.get('palette')) S.palette = qs.get('palette');
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch (e) {} };
  const MOTION = ['off', 'subtle', 'standard', 'expressive'];
  const MOTION_M = [0, 0.6, 1, 1.25];
  const PALETTES = ['meadow', 'ultramarine', 'dusk', 'rose', 'lagoon', 'graphite'];

  /* ================= haptics (real on Android browsers) ================= */
  function buzz(kind) {
    if (!navigator.vibrate || !S.haptics) return;
    const completion = kind === 'medium' || kind === 'success';
    if (kind === 'tick' && S.haptics === 1) return;
    if (S.completionOnly && !completion) return;
    const base = { tick: 6, light: 10, medium: 18, success: [14, 50, 22], warning: [20, 40, 20] }[kind] || 8;
    const k = [0, 0.6, 0.85, 1.2][S.haptics];
    try { navigator.vibrate(Array.isArray(base) ? base.map((v, i) => (i % 2 ? v : Math.round(v * k))) : Math.max(4, Math.round(base * k))); } catch (e) {}
  }

  /* ================= performance governor ================= */
  const ORDER = ['off', 'light', 'balanced', 'full'];
  const P = { webgl: false, software: false, gpu: '', cores: navigator.hardwareConcurrency || 0, mem: navigator.deviceMemory || 0, saveData: false, net: '', coarse: mq('(pointer: coarse)').matches, dpr: window.devicePixelRatio || 1, reasons: [], auto: 'balanced', downgraded: false };
  function probe() {
    try {
      const c = document.createElement('canvas');
      const gl = c.getContext('webgl2') || c.getContext('webgl');
      if (gl) {
        P.webgl = true;
        const ext = gl.getExtension('WEBGL_debug_renderer_info');
        P.gpu = String(ext ? gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER) || '');
        P.software = /swiftshader|llvmpipe|software|basic render|softpipe/i.test(P.gpu);
        const lose = gl.getExtension('WEBGL_lose_context'); if (lose) lose.loseContext();
      }
    } catch (e) {}
    const cn = navigator.connection || {};
    P.saveData = !!cn.saveData; P.net = cn.effectiveType || '';
    let t = 3; const why = [];
    const cap = (n, msg) => { if (n < t) { t = n; } why.push(msg); };
    if (!P.webgl) cap(0, 'WebGL is not available, so the 3D scene is off');
    else if (P.software) cap(0, 'graphics are software-rendered, so the 3D scene is off');
    if (P.saveData) cap(1, 'data saver is on');
    if (/(^|-)2g$/.test(P.net)) cap(1, 'the connection is slow');
    if (P.mem && P.mem <= 2) cap(1, 'memory is ' + P.mem + ' GB');
    else if (P.mem && P.mem <= 4) cap(2, 'memory is ' + P.mem + ' GB');
    if (P.cores && P.cores <= 2) cap(1, P.cores + ' processor cores');
    else if (P.cores && P.cores <= 4) cap(2, P.cores + ' processor cores');
    if (/Mali-4|Mali-T|Adreno \(TM\) [1-4]\d\d|PowerVR|HD Graphics [2-5]\d{3}\b|GMA /i.test(P.gpu)) cap(1, 'an older graphics chip');
    if (P.coarse && !(P.cores >= 8 && (!P.mem || P.mem >= 6))) cap(2, 'this is a touch device, so effects are kept moderate');
    if (!why.length) why.push((P.cores ? P.cores + ' cores' : 'a capable processor') + (P.mem ? ', ' + P.mem + ' GB memory' : '') + ' and hardware graphics');
    P.reasons = why; P.auto = ORDER[t];
    try { const s = sessionStorage.getItem(KEY + '-tier'); if (s && ORDER.indexOf(s) < ORDER.indexOf(P.auto)) { P.auto = s; P.downgraded = true; } } catch (e) {}
  }
  probe();
  let tier = 'balanced';
  function resolveTier() {
    let t = S.perf === 'auto' ? P.auto : (ORDER.indexOf(S.perf) >= 0 ? S.perf : P.auto);
    return t;
  }
  const has3d = () => tier !== 'off' && P.webgl && !sceneFailed;

  /* ================= 3D scene loader ================= */
  let scene = null, sceneLoading = false, sceneFailed = false;
  const canvas = $('#gl');
  function ensureScene() {
    if (!has3d()) { if (scene) { scene.dispose(); scene = null; } html.dataset['3d'] = 'off'; return; }
    if (scene) { scene.setTier(tier); return; }
    if (sceneLoading) return;
    sceneLoading = true;
    const go = () => {
      const s = document.createElement('script'); s.src = 'assets/scene.js'; s.async = true;
      s.onload = () => {
        sceneLoading = false;
        if (!has3d()) return;
        try {
          scene = window.CadenceScene.create({ canvas, tier });
          html.dataset['3d'] = 'on';
          pushSceneState(); scene.setMotion(effM()); startLoop();
        } catch (e) { sceneFailed = true; scene = null; apply(); }
      };
      s.onerror = () => { sceneLoading = false; sceneFailed = true; apply(); };
      document.head.appendChild(s);
    };
    (window.requestIdleCallback || ((f) => setTimeout(f, 200)))(go, { timeout: 1500 });
  }
  canvas.addEventListener('webglcontextlost', (e) => { e.preventDefault(); sceneFailed = true; scene = null; apply(); toast('The 3D scene was switched off to keep things steady.'); });
  function pushSceneState() {
    if (!scene) return;
    scene.setBeats(Math.round(doneCount() / Math.max(1, beatTotal()) * 8));
    scene.setMotif(SCREENS[tourIdx].motif);
    scene.setGrid(streakCells.map((c) => (c === 't' ? 'f' : c)));
    scene.setSliders(effMotionIdx(), S.haptics);
    scene.readColors();
  }

  /* ================= apply settings to the page ================= */
  const effMotionIdx = () => { let m = MQ.motion.matches ? 0 : S.motion; if (tier === 'off') m = Math.min(m, 1); if (tier === 'light') m = Math.min(m, 2); return m; };
  const effM = () => MOTION_M[effMotionIdx()];
  function apply() {
    tier = resolveTier();
    if (tier !== 'off' && (!P.webgl || sceneFailed) && S.perf === 'auto') tier = P.software || !P.webgl ? 'off' : tier;
    const dark = S.theme === 'dark' || (S.theme === 'system' && MQ.dark.matches);
    const hc = S.contrast === 'high' || MQ.contrast.matches;
    html.dataset.theme = (hc ? 'hc-' : '') + (dark ? 'dark' : 'light');
    html.dataset.palette = PALETTES.indexOf(S.palette) >= 0 ? S.palette : 'meadow';
    html.dataset.glass = S.glass;
    html.dataset.backdrop = S.backdrop;
    html.dataset.motion = MOTION[effMotionIdx()];
    html.dataset.perf = tier;
    const tc = $('meta[name="theme-color"]'); if (tc) tc.content = getComputedStyle(html).getPropertyValue('--backdrop').trim();
    ensureScene();
    if (scene) { scene.setMotion(effM()); scene.setSliders(effMotionIdx(), S.haptics); scene.readColors(); }
    syncControls(); perfUI(); save(); startLoop();
  }
  function set(k, v, haptic) { if (S[k] === v) return; S[k] = v; if (haptic !== false) buzz(haptic || 'tick'); apply(); }
  [MQ.dark, MQ.motion, MQ.contrast].forEach((m) => m.addEventListener && m.addEventListener('change', apply));

  /* ================= shared controls: levels, segments, palettes, toggles ================= */
  const swatches = () => PALETTES.map((p) => '<button type="button" class="cd-swatch" data-p="' + p + '" aria-pressed="false"><span class="cd-swatch__tile"><span class="cd-swatch__tick"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg></span></span><span class="cd-swatch__name">' + p[0].toUpperCase() + p.slice(1) + '</span></button>').join('');
  function syncControls(root) {
    root = root || document;
    $$('[data-palettes]', root).forEach((el) => { if (!el.children.length) el.innerHTML = swatches(); });
    $$('.cd-swatch', root).forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.p === html.dataset.palette)));
    const segVal = { theme: S.theme, perf: S.perf === 'balanced' ? 'auto' : S.perf, backdrop: S.backdrop };
    $$('[data-seg]', root).forEach((seg) => {
      const btns = $$('button', seg);
      btns.forEach((b, i) => { const on = b.dataset.v === segVal[seg.dataset.seg]; b.setAttribute('aria-pressed', String(on)); if (on) seg.dataset.i = i; });
    });
    $$('[data-level]', root).forEach((lv) => {
      const isM = lv.dataset.level === 'motion';
      const v = isM ? effMotionIdx() : S.haptics;
      lv.dataset.v = v; const inp = $('input', lv); inp.value = v;
      const names = isM ? ['Off', 'Subtle', 'Standard', 'Expressive'] : ['Off', 'Soft', 'Balanced', 'Firm'];
      inp.setAttribute('aria-valuetext', names[v]);
      $$('.cd-level__labels span', lv).forEach((s, i) => s.classList.toggle('is-on', i === v));
    });
    const tg = { glass: S.glass === 'solid', contrast: S.contrast === 'high', completion: S.completionOnly, weekend: D.weekendRest };
    $$('[data-toggle]', root).forEach((t) => t.setAttribute('aria-checked', String(!!tg[t.dataset.toggle])));
    const mn = $('#lvMotionNote');
    if (mn) mn.textContent = MQ.motion.matches ? 'Following your system setting: reduce motion is on.' : tier === 'off' ? 'Essential mode keeps motion at Subtle or lower.' : tier === 'light' && S.motion === 3 ? 'Light mode keeps motion at Standard or lower.' : 'Multiplies travel and overshoot everywhere on this page.';
    const hn = $('#lvHapticsNote');
    if (hn) hn.textContent = navigator.vibrate ? 'This device can vibrate: check something off to feel it.' : 'Haptics need a phone. On Android this page vibrates like the app does.';
  }
  document.addEventListener('click', (e) => {
    const sw = e.target.closest('.cd-swatch'); if (sw) return set('palette', sw.dataset.p);
    const sb = e.target.closest('[data-seg] button');
    if (sb) { const k = sb.parentElement.dataset.seg; if (k === 'perf') { try { sessionStorage.removeItem(KEY + '-tier'); } catch (er) {} if (sb.dataset.v === 'auto') { P.downgraded = false; probe(); } } return set(k, sb.dataset.v); }
    const tg = e.target.closest('[data-toggle]');
    if (tg) {
      const k = tg.dataset.toggle;
      if (k === 'glass') set('glass', S.glass === 'solid' ? 'frosted' : 'solid');
      else if (k === 'contrast') set('contrast', S.contrast === 'high' ? 'normal' : 'high');
      else if (k === 'completion') set('completionOnly', !S.completionOnly);
      else { D.weekendRest = !D.weekendRest; buzz('tick'); syncControls(); }
    }
  });
  document.addEventListener('input', (e) => {
    const lv = e.target.closest('[data-level]'); if (!lv) return;
    const v = +e.target.value;
    if (lv.dataset.level === 'motion') { if (MQ.motion.matches) return syncControls(); set('motion', v, 'medium'); }
    else { S.haptics = v; buzz('medium'); apply(); }
  });

  /* ================= toasts ================= */
  function toastIn(host, text, opts) {
    opts = opts || {};
    host.innerHTML = '';
    const t = document.createElement('div');
    t.className = 'cd-toast cd-glass cd-glass--raised cd-enter'; t.setAttribute('role', 'status');
    t.innerHTML = '<span>' + esc(text) + '</span>' + (opts.undo ? '<button class="cd-btn cd-btn--quiet" type="button">' + ci('undo', true) + 'Undo</button><i class="cd-toast__time"></i>' : '<span style="flex:none;width:14px"></span>');
    host.appendChild(t);
    const kill = () => { clearTimeout(timer); t.classList.add('is-leaving'); setTimeout(() => t.remove(), 200); };
    const timer = setTimeout(() => { kill(); opts.done && opts.done(); }, opts.undo ? 5000 : 3200);
    if (opts.undo) $('button', t).addEventListener('click', () => { buzz('tick'); kill(); opts.undo(); });
    return kill;
  }
  const toast = (text, opts) => toastIn($('#toasts'), text, opts);

  /* ================= demo data (the sample day from the mockups) ================= */
  const D = {
    habits: [
      { id: 'water', title: 'Glass of water', cue: 'Right after waking', min: 'one sip', streak: 46, week: 'ddddddt', group: 'Morning' },
      { id: 'stretch', title: 'Stretch', cue: 'Before the first meeting', min: '2 minutes', streak: 31, week: 'dmddrdt', group: 'Morning' },
      { id: 'read', title: 'Read 10 pages', cue: 'After chai, on the balcony', min: '1 page', streak: 12, week: 'ddmdddt', group: 'Afternoon' },
      { id: 'walk', title: 'Evening walk', cue: 'After dinner', min: '5 minutes', streak: 8, week: 'dddmddt', group: 'Evening' },
      { id: 'journal', title: 'Three lines', cue: 'In bed, before the phone', min: '1 line', streak: 19, week: 'xddddrt', group: 'Evening' },
    ],
    tasks: [
      { id: 'invoice', title: 'Send invoice to Meera', due: '5:30 pm', dur: '25 min', tag: 'Work', flag: true, when: 'Today' },
      { id: 'tickets', title: 'Book train tickets', due: 'Today', dur: '10 min', tag: 'Home', when: 'Today' },
      { id: 'gift', title: 'Order gift for Aai', due: '8:00 pm', dur: '15 min', tag: 'Home', when: 'Today' },
      { id: 'library', title: 'Renew library card', due: 'Thu', dur: '10 min', tag: 'Home', when: 'This week' },
      { id: 'outline', title: 'Draft the talk outline', due: 'Fri', dur: '45 min', tag: 'Work', flag: true, when: 'This week' },
    ],
    notes: [
      { id: 'trip', title: 'Trip packing', body: '2 of 5 done · Charger and the long cable. Check the monsoon forecast before choosing shoes.', tag: 'Home', time: 'Today', pinned: true },
      { id: 'talk', title: 'Ideas for the talk', body: 'Open with the chai story. Three points only. End on the balcony photo.', tag: 'Work', time: 'Sun', pinned: true },
      { id: 'books', title: 'Books to borrow', body: 'The one Meera mentioned about slow mornings. Ask the library about the waitlist.', tag: 'Reading', time: 'Yesterday' },
      { id: 'dal', title: 'Dal recipe from Aai', body: 'Temper the mustard seeds first. Kokum goes in last, off the heat.', tag: 'Home', time: '3 Nov' },
      { id: 'plants', title: 'Balcony plants', body: 'Tulsi wants morning sun. Water the fern every other day, less in the rains.', tag: 'Home', time: '1 Nov' },
      { id: 'quotes', title: 'Lines worth keeping', body: 'Collected from this month. Copy the good ones onto the first page of the notebook.', tag: 'Reading', time: '28 Oct' },
    ],
    lines: [['Passport', true], ['Train tickets', true], ['Charger and the long cable', false], ['Umbrella', false], ['Book for the train', false]],
    done: { water: true, stretch: true, tickets: true, walk: true },
    minToday: {},
    order: ['read', 'walk', 'journal', 'invoice', 'tickets', 'gift'],
    detail: 'read',
    weekendRest: true,
  };
  const READ_GRID = 'ddxddrr' + 'dmdxdrr' + 'dddddrr' + 'ddmddrr' + 'dtfffff';
  const TODAY_IDX = 29;
  const beatsList = () => D.habits.concat(D.tasks.filter((t) => t.when === 'Today'));
  const beatTotal = () => beatsList().length;
  const doneCount = () => beatsList().filter((b) => D.done[b.id]).length;
  const byId = (id) => D.habits.concat(D.tasks).find((x) => x.id === id);
  const listeners = new Set();
  function emit(why) {
    listeners.forEach((f) => f(why));
    if (scene) scene.setBeats(Math.round(doneCount() / Math.max(1, beatTotal()) * 8));
  }
  let celebrated = false;
  function toggleBeat(id, ctx, asMin) {
    const b = byId(id); if (!b) return;
    const was = !!D.done[id];
    if (was && !asMin) { D.done[id] = false; delete D.minToday[id]; buzz('tick'); emit(); ctx && ctx.toast(null); return; }
    D.done[id] = true; if (asMin) D.minToday[id] = true; else delete D.minToday[id];
    const all = doneCount() === beatTotal();
    buzz(all && !celebrated ? 'success' : 'medium');
    if (scene) scene.pulseTick();
    emit(all && !celebrated ? 'all' : 'done'); if (all) celebrated = true;
    ctx && ctx.toast(b.title + (asMin ? ': minimum done' : ' done'), () => { D.done[id] = false; delete D.minToday[id]; emit(); });
  }
  function streakOf(cells, todayIdx) {
    let n = 0;
    for (let i = todayIdx; i >= 0; i--) { const c = cells[i]; if (c === 'd' || c === 'm') n++; else if (c === 'x') break; }
    return n;
  }
  const activeOf = (cells) => { const k = cells.filter((c) => c === 'd' || c === 'm').length, x = cells.filter((c) => c === 'x').length; return [k, k + x]; };
  function gridFor(h) {
    let g = h.id === 'read' ? READ_GRID : 'dddddrr'.repeat(4) + 'dtfffff';
    const t = D.done[h.id] ? (D.minToday[h.id] ? 'm' : 'd') : 't';
    return (g.slice(0, TODAY_IDX) + t + g.slice(TODAY_IDX + 1)).split('');
  }
  const habitStreak = (h) => (h.id === 'read' ? streakOf(gridFor(h), TODAY_IDX) : h.streak);

  /* ================= phone screens ================= */
  const DOT = { d: 'done', m: 'min', r: 'rest', x: 'miss', t: 'today' };
  const WORD = { d: 'done', m: 'minimum', r: 'rest day', x: 'missed', t: 'today', n: 'not started' };
  const GLOWS = [
    [['1', 300, 280, 'right:-90px;top:-60px'], ['3', 240, 240, 'left:-90px;top:300px'], ['2', 340, 300, 'left:60px;bottom:-80px']],
    [['2', 300, 280, 'right:-90px;top:-40px'], ['1', 260, 260, 'left:-100px;top:330px'], ['3', 340, 280, 'right:-60px;bottom:-60px']],
    [['3', 300, 260, 'right:-80px;top:-50px'], ['2', 260, 260, 'left:-100px;top:280px'], ['1', 340, 300, 'right:-70px;bottom:-40px']],
    [['1', 280, 260, 'left:-100px;top:-40px'], ['2', 280, 260, 'right:-100px;top:260px'], ['3', 340, 300, 'left:-40px;bottom:-60px']],
  ];
  const glows = (i) => GLOWS[i % 4].map((g) => '<i class="cd-glow cd-glow--' + g[0] + '" style="width:' + g[1] + 'px;height:' + g[2] + 'px;' + g[3] + '"></i>').join('');
  const tabbar = (on) => '<div class="cd-screen__tabbar"><nav class="cd-tabbar cd-glass cd-glass--raised" aria-label="App tabs">' +
    [['today', 'sun', 'Today'], ['habits', 'repeat', 'Habits'], ['add'], ['tasks', 'tasks', 'Tasks'], ['notes', 'notes', 'Notes']].map((t) => t[0] === 'add'
      ? '<button class="cd-add" data-go="add" aria-label="Quick add">' + ci('plus') + '</button>'
      : '<button class="cd-tab" data-go="' + t[0] + '"' + (on === t[0] ? ' aria-current="page"' : '') + '>' + ci(t[1]) + '<span>' + t[2] + '</span></button>').join('') + '</nav></div>';
  const top = (sub, title, btn) => '<header class="cd-screen__top"><div><div class="cd-screen__date">' + sub + '</div><h1>' + title + '</h1></div>' + (btn || '') + '</header>';
  const iconBtn = (ic, label, go) => '<button class="cd-iconbtn cd-glass"' + (go ? ' data-go="' + go + '"' : '') + ' aria-label="' + label + '">' + ci(ic) + '</button>';
  const weekDots = (h) => h.week.split('').map((c, i) => '<i class="cd-dot' + (DOT[c] ? ' cd-dot--' + DOT[c] : '') + '"' + (i === 6 ? ' data-today' : '') + '></i>').join('');
  const habitRow = (h, withMin) => '<div class="cd-row cd-glass cd-press" data-row="' + h.id + '"><button class="cd-check" data-toggle-beat="' + h.id + '" aria-label="Log ' + esc(h.title) + '">' + TICK + '</button>' +
    '<div class="cd-row__body" data-detail="' + h.id + '" role="button" tabindex="0"><div class="cd-row__title">' + esc(h.title) + '</div><div class="cd-row__sub">' + esc(h.cue || 'No cue yet') + '</div>' + (withMin && h.min ? '<div class="cd-row__meta"><span>Minimum: ' + esc(h.min) + '</span></div>' : '') + '</div>' +
    '<div class="cd-row__side"><div class="cd-week" role="img">' + weekDots(h) + '</div><span class="cd-streak"><b data-streak></b> day streak</span></div></div>';
  const taskRow = (t) => '<div class="cd-row cd-glass cd-press" data-row="' + t.id + '" data-tag="' + t.tag + '"><button class="cd-check cd-check--task" data-toggle-beat="' + t.id + '" aria-label="Complete ' + esc(t.title) + '">' + TICK + '</button>' +
    '<div class="cd-row__body"><div class="cd-row__title">' + esc(t.title) + '</div><div class="cd-row__meta"><span>' + ci('clock', true) + t.due + '</span><span>' + ci('timer', true) + t.dur + '</span><span class="cd-tag">' + t.tag + '</span></div></div>' +
    (t.flag ? '<span class="cd-flag" role="img" aria-label="Priority">' + ci('flag', true) + '</span>' : '') + '</div>';
  function syncRows(root) {
    $$('[data-row]', root).forEach((r) => {
      const id = r.dataset.row, on = !!D.done[id];
      r.classList.toggle('is-done', on && !D.minToday[id]); r.classList.toggle('is-min', on && !!D.minToday[id]);
      const h = D.habits.find((x) => x.id === id);
      if (h) {
        const td = $('[data-today]', r); if (td) td.className = 'cd-dot cd-dot--' + (on ? (D.minToday[id] ? 'min' : 'done') : 'today');
        const st = $('[data-streak]', r); if (st) st.textContent = habitStreak(h);
        const wk = $('.cd-week', r); if (wk) wk.setAttribute('aria-label', 'Last 7 days: ' + h.week.split('').map((c, i) => (i === 6 && on ? 'done' : WORD[c])).join(', '));
      }
    });
  }

  const SCR = {
    today(root, ctx) {
      root.innerHTML = glows(0) + top('Tue 10 Nov', 'Today', iconBtn('sliders', 'Settings', 'settings')) +
        '<div style="margin-bottom:20px"><div class="cd-progress"><b><span data-count></span><small>beats done</small></b><span data-left></span></div><div class="cd-beats" role="progressbar" data-beats></div></div>' +
        '<section class="cd-nextup cd-glass cd-glass--raised" data-next></section>' +
        '<div class="cd-screen__scroll" style="margin-top:4px"><h2 class="cd-section">Habits <span data-hc></span></h2><div class="cd-list">' + D.habits.map((h) => habitRow(h)).join('') + '</div>' +
        '<h2 class="cd-section">Tasks <span data-tc></span></h2><div class="cd-list">' + D.tasks.filter((t) => t.when === 'Today').map(taskRow).join('') + '</div></div>' +
        '<div class="cd-screen__toast" data-toast></div>' + tabbar('today');
      let lastNext = null;
      return (why) => {
        const total = beatTotal(), n = doneCount();
        $('[data-count]', root).textContent = n + '/' + total;
        $('[data-left]', root).textContent = n === total ? 'All done' : (total - n) + ' to go';
        const bb = $('[data-beats]', root);
        if (bb.children.length !== total) bb.innerHTML = '<i class="cd-beat"></i>'.repeat(total);
        Array.from(bb.children).forEach((b, i) => b.classList.toggle('is-done', i < n));
        bb.setAttribute('aria-label', n + ' of ' + total + ' beats done');
        if (why === 'all') { bb.classList.remove('is-sweep'); void bb.offsetWidth; bb.classList.add('is-sweep'); }
        const ids = beatsList().map((b) => b.id);
        const nextId = D.order.concat(ids).find((id) => ids.indexOf(id) >= 0 && !D.done[id]);
        if (nextId !== lastNext) {
          lastNext = nextId; const nx = nextId && byId(nextId); const el = $('[data-next]', root);
          el.innerHTML = '<div class="nextswap"><div class="cd-eyebrow">Next up</div>' + (nx
            ? '<h3>' + esc(nx.title) + '</h3><p>' + esc(nx.group ? (nx.cue || 'Any time today') + (nx.min ? ' · Minimum: ' + nx.min : '') : nx.due + ' · ' + nx.dur) + '</p><div class="cd-nextup__actions"><button class="cd-btn cd-btn--primary cd-press" data-toggle-beat="' + nx.id + '">Mark done</button><button class="cd-btn cd-btn--glass cd-press" data-later="' + nx.id + '">Later</button></div>'
            : '<h3>That\'s today</h3><p>' + total + ' of ' + total + ' beats done. Rest well.</p>') + '</div>';
        }
        const tt = D.tasks.filter((t) => t.when === 'Today');
        $('[data-hc]', root).textContent = D.habits.filter((h) => D.done[h.id]).length + ' of ' + D.habits.length;
        $('[data-tc]', root).textContent = tt.filter((t) => D.done[t.id]).length + ' of ' + tt.length;
        syncRows(root);
      };
    },
    habits(root, ctx) {
      const groups = ['Morning', 'Afternoon', 'Evening'];
      root.innerHTML = glows(1) + top('<span data-sum></span>', 'Habits', iconBtn('plus', 'New habit', 'add')) + '<div class="cd-screen__scroll">' +
        groups.map((g) => '<h2 class="cd-section">' + g + ' <span data-g="' + g + '"></span></h2><div class="cd-list">' + D.habits.filter((h) => h.group === g).map((h) => habitRow(h, true)).join('') + '</div>').join('') +
        '<div class="cd-glass" style="margin-top:24px;padding:14px 16px"><div class="cd-legend" style="margin-top:0"><span><i class="cd-dot cd-dot--done"></i>Done</span><span><i class="cd-dot cd-dot--min"></i>Minimum</span><span><i class="cd-dot cd-dot--rest"></i>Rest</span><span><i class="cd-dot cd-dot--miss"></i>Missed</span></div></div></div>' +
        '<div class="cd-screen__toast" data-toast></div>' + tabbar('habits');
      return () => {
        $('[data-sum]', root).textContent = D.habits.filter((h) => D.done[h.id]).length + ' of ' + D.habits.length + ' logged today';
        groups.forEach((g) => { const hs = D.habits.filter((h) => h.group === g); $('[data-g="' + g + '"]', root).textContent = hs.filter((h) => D.done[h.id]).length + ' of ' + hs.length; });
        syncRows(root);
      };
    },
    detail(root, ctx) {
      const h = D.habits.find((x) => x.id === D.detail) || D.habits[2];
      root.innerHTML = glows(0) + '<header class="cd-screen__top" style="margin-bottom:12px">' + iconBtn('chevron-left', 'Back to habits', 'habits') + iconBtn('more', 'More options') + '</header>' +
        '<div class="cd-screen__scroll" style="padding-bottom:150px"><h1 style="margin:0;font:700 32px/36px var(--font-display);letter-spacing:-.02em">' + esc(h.title) + '</h1><div class="cd-screen__date" style="margin:4px 0 20px">' + esc(h.cue || 'No cue yet') + '</div>' +
        '<section class="cd-glass" style="padding:20px 16px 16px"><div style="display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:16px"><div class="cd-stat"><b data-n></b><span class="cd-row__title">day streak</span></div><div style="text-align:right"><div class="cd-row__title" data-active></div><div class="cd-row__sub">active days</div></div></div>' +
        '<div class="cd-grid" role="img" data-grid>' + 'MTWTFSS'.split('').map((d) => '<span class="cd-grid__head">' + d + '</span>').join('') + '<i class="cd-cell"></i>'.repeat(35) + '</div>' +
        '<div class="cd-legend"><span><i class="cd-dot cd-dot--done"></i>Done</span><span><i class="cd-dot cd-dot--min"></i>Minimum</span><span><i class="cd-dot cd-dot--rest"></i>Rest</span><span><i class="cd-dot cd-dot--miss"></i>Missed</span></div></section>' +
        '<h2 class="cd-section">Rhythm</h2><div class="cd-glass cd-group">' +
        '<button class="cd-setting cd-press"><div class="cd-setting__body"><div class="cd-setting__title">Minimum version</div><div class="cd-setting__sub">Counts on a low-energy day</div></div><span class="cd-setting__value">' + esc(h.min || 'Not set') + ci('chevron-right', true) + '</span></button>' +
        '<button class="cd-setting cd-press"><div class="cd-setting__body"><div class="cd-setting__title">Time of day</div></div><span class="cd-setting__value">' + h.group + ci('chevron-right', true) + '</span></button>' +
        '<button class="cd-setting cd-press"><div class="cd-setting__body"><div class="cd-setting__title">Rest days</div><div class="cd-setting__sub">Keep the streak, off the day\'s count</div></div><span class="cd-setting__value">Sat, Sun' + ci('chevron-right', true) + '</span></button></div></div>' +
        '<div class="cd-screen__toast" data-toast style="bottom:112px"></div>' +
        '<div class="cd-glass cd-glass--raised" style="position:absolute;left:16px;right:16px;bottom:24px;padding:12px;border-radius:var(--radius-sheet);display:flex;gap:8px"><button class="cd-btn cd-btn--primary cd-press" style="flex:1" data-log></button><button class="cd-btn cd-btn--glass cd-press" data-logmin>Did the minimum</button></div>';
      $('[data-log]', root).addEventListener('click', () => { if (D.done[h.id] && D.minToday[h.id]) { delete D.minToday[h.id]; buzz('medium'); emit(); } else toggleBeat(h.id, ctx); });
      $('[data-logmin]', root).addEventListener('click', () => { if (D.done[h.id] && D.minToday[h.id]) toggleBeat(h.id, ctx); else toggleBeat(h.id, ctx, true); });
      return () => {
        const g = gridFor(h), names = { d: 'done', m: 'min', r: 'rest', x: 'miss', f: 'future', t: 'future' };
        $$('.cd-cell', root).forEach((c, i) => { c.className = 'cd-cell cd-cell--' + names[g[i]]; });
        const a = activeOf(g);
        $('[data-n]', root).textContent = habitStreak(h);
        $('[data-active]', root).textContent = a[0] + ' of ' + a[1];
        $('[data-grid]', root).setAttribute('aria-label', 'Last five weeks: ' + g.filter((c) => c === 'd').length + ' done, ' + g.filter((c) => c === 'm').length + ' minimum, ' + g.filter((c) => c === 'r').length + ' rest days, ' + g.filter((c) => c === 'x').length + ' missed');
        $('[data-log]', root).textContent = D.done[h.id] && !D.minToday[h.id] ? 'Logged today' : 'Log today';
      };
    },
    tasks(root, ctx) {
      let filter = 'All';
      const groups = ['Today', 'This week'];
      root.innerHTML = glows(2) + top('<span data-sum></span>', 'Tasks', iconBtn('search', 'Search tasks')) +
        '<div class="cd-chips" style="margin:0 -20px 4px;padding:4px 20px">' + ['All', 'Work', 'Home'].map((f) => '<button class="cd-chip" data-f="' + f + '">' + f + '</button>').join('') + '</div><div class="cd-screen__scroll">' +
        groups.map((g) => '<div data-grp><h2 class="cd-section">' + g + ' <span data-g="' + g + '"></span></h2><div class="cd-list">' + D.tasks.filter((t) => t.when === g).map(taskRow).join('') + '</div></div>').join('') +
        '<h2 class="cd-section">Done <span>1</span></h2><div class="cd-list"><div class="cd-row cd-glass is-done" data-tag="Home"><span class="cd-check cd-check--task is-done" aria-hidden="true">' + TICK + '</span><div class="cd-row__body"><div class="cd-row__title">Pay the electricity bill</div><div class="cd-row__meta"><span>' + ci('clock', true) + 'Mon</span><span class="cd-tag">Home</span></div></div></div></div></div>' +
        '<div class="cd-screen__toast" data-toast></div>' + tabbar('tasks');
      const sync = () => {
        const tt = D.tasks.filter((t) => t.when === 'Today');
        $('[data-sum]', root).textContent = tt.filter((t) => !D.done[t.id]).length + ' due today';
        groups.forEach((g) => { const ts = D.tasks.filter((t) => t.when === g); $('[data-g="' + g + '"]', root).textContent = ts.filter((t) => D.done[t.id]).length + ' of ' + ts.length; });
        $$('.cd-chip', root).forEach((c) => { const on = c.dataset.f === filter; c.classList.toggle('is-on', on); c.setAttribute('aria-pressed', String(on)); });
        $$('[data-tag]', root).forEach((r) => { r.hidden = filter !== 'All' && r.dataset.tag !== filter; });
        syncRows(root);
      };
      root.addEventListener('click', (e) => { const c = e.target.closest('.cd-chip'); if (c) { filter = c.dataset.f; buzz('tick'); sync(); } });
      return sync;
    },
    notes(root, ctx) {
      let filter = 'All', q = '';
      const card = (n) => '<button class="cd-note cd-glass cd-press" data-note="' + n.id + '" data-go="editor" style="text-align:left;width:100%"><div class="cd-note__title"><span>' + esc(n.title) + '</span>' + (n.pinned ? ci('pin', true) : '') + '</div><p>' + esc(n.body) + '</p><div class="cd-note__foot">' + (n.tag ? '<span class="cd-tag">' + n.tag + '</span>' : '') + '<time>' + n.time + '</time></div></button>';
      const pinned = D.notes.filter((n) => n.pinned), rest = D.notes.filter((n) => !n.pinned);
      root.innerHTML = glows(3) + top(D.notes.length + ' notes · ' + pinned.length + ' pinned', 'Notes', iconBtn('plus', 'New note', 'add')) +
        '<label class="cd-search cd-glass">' + ci('search') + '<input placeholder="Search notes" aria-label="Search notes"></label>' +
        '<div class="cd-chips" style="margin:8px -20px 0;padding:4px 20px">' + ['All', 'Home', 'Work', 'Reading'].map((f) => '<button class="cd-chip" data-f="' + f + '">' + f + '</button>').join('') + '</div>' +
        '<div class="cd-screen__scroll"><div data-sec="p"><h2 class="cd-section">Pinned <span data-c="p"></span></h2><div class="cd-list">' + pinned.map(card).join('') + '</div></div>' +
        '<div data-sec="a"><h2 class="cd-section">All notes <span data-c="a"></span></h2><div class="cd-list">' + rest.map(card).join('') + '</div></div><p class="cd-row__sub" data-empty hidden style="margin:24px 4px;white-space:normal">No notes match. Try another word or tag.</p></div>' + tabbar('notes');
      const sync = () => {
        const show = (n) => (filter === 'All' || n.tag === filter) && (!q || (n.title + ' ' + n.body).toLowerCase().indexOf(q) >= 0);
        let any = 0;
        [['p', pinned], ['a', rest]].forEach(([k, list]) => {
          let c = 0; list.forEach((n) => { const s = show(n); if (s) c++; $('[data-note="' + n.id + '"]', root).hidden = !s; });
          $('[data-c="' + k + '"]', root).textContent = c; $('[data-sec="' + k + '"]', root).hidden = !c; any += c;
        });
        $('[data-empty]', root).hidden = !!any;
        $$('.cd-chip', root).forEach((c) => { const on = c.dataset.f === filter; c.classList.toggle('is-on', on); c.setAttribute('aria-pressed', String(on)); });
      };
      root.addEventListener('click', (e) => { const c = e.target.closest('.cd-chip'); if (c) { filter = c.dataset.f; buzz('tick'); sync(); } });
      $('input', root).addEventListener('input', (e) => { q = e.target.value.trim().toLowerCase(); sync(); });
      return sync;
    },
    editor(root, ctx) {
      root.innerHTML = glows(2) + '<header class="cd-screen__top">' + iconBtn('chevron-left', 'Back to notes', 'notes') + '<div class="cd-screen__date" role="status" data-saved>Saved just now</div><div style="display:flex;gap:8px"><button class="cd-iconbtn cd-glass" aria-label="Unpin note" aria-pressed="true" style="color:var(--accent)" data-pin>' + ci('pin') + '</button>' + iconBtn('more', 'More options') + '</div></header>' +
        '<article class="cd-editor cd-glass cd-glass--raised"><h2 contenteditable="true" spellcheck="false">Trip packing</h2><div class="cd-row__meta" style="margin:-4px 0 4px"><span class="cd-tag">Home</span><span data-prog></span></div><div>' +
        D.lines.map((l, i) => '<div class="cd-line" data-line="' + i + '"><button class="cd-check cd-check--task" aria-label="' + esc(l[0]) + '">' + TICK + '</button><span>' + esc(l[0]) + '</span></div>').join('') +
        '</div><p contenteditable="true" spellcheck="false">Check the monsoon forecast before choosing shoes. The train leaves at 6:10 am, so pack the night before and leave the bag by the door.</p><p class="cd-muted">Keep writing…</p></article>' +
        '<div class="cd-glass cd-glass--raised" style="position:absolute;left:16px;right:16px;bottom:24px;height:56px;padding:0 8px;border-radius:var(--radius-pill);display:flex;align-items:center;justify-content:space-between"><div style="display:flex"><button class="cd-iconbtn" aria-label="Add checklist line">' + ci('checklist') + '</button><button class="cd-iconbtn" aria-label="Add tag">' + ci('tag') + '</button><button class="cd-iconbtn" aria-label="Turn into a task">' + ci('tasks') + '</button></div><button class="cd-btn cd-btn--quiet" data-go="notes">Done</button></div>';
      const sync = () => {
        $$('[data-line]', root).forEach((el) => el.classList.toggle('is-done', D.lines[+el.dataset.line][1]));
        $('[data-prog]', root).textContent = D.lines.filter((l) => l[1]).length + ' of ' + D.lines.length + ' done';
      };
      root.addEventListener('click', (e) => {
        const ln = e.target.closest('[data-line]');
        if (ln && e.target.closest('.cd-check')) { const l = D.lines[+ln.dataset.line]; l[1] = !l[1]; buzz(l[1] ? 'medium' : 'tick'); sync(); }
        const pin = e.target.closest('[data-pin]');
        if (pin) { const on = pin.getAttribute('aria-pressed') !== 'true'; pin.setAttribute('aria-pressed', String(on)); pin.style.color = on ? 'var(--accent)' : ''; buzz('tick'); }
      });
      root.addEventListener('input', () => { const s = $('[data-saved]', root); s.textContent = 'Saving…'; clearTimeout(root._t); root._t = setTimeout(() => { s.textContent = 'Saved just now'; }, 600); });
      return sync;
    },
    add(root, ctx) {
      let type = 0, title = '';
      const types = ['Task', 'Habit', 'Note'];
      root.innerHTML = glows(0) + '<header class="cd-screen__top" aria-hidden="true"><div><div class="cd-screen__date">Tue 10 Nov</div><h1>Today</h1></div></header>' +
        '<div aria-hidden="true"><div class="cd-progress"><b>' + doneCount() + '/' + beatTotal() + '<small>beats done</small></b><span>' + (beatTotal() - doneCount()) + ' to go</span></div><div class="cd-beats">' + beatsList().map((b, i) => '<i class="cd-beat' + (i < doneCount() ? ' is-done' : '') + '"></i>').join('') + '</div></div>' +
        '<button class="cd-scrim" data-go="today" aria-label="Close quick add" style="border-radius:0"></button>' +
        '<div class="cd-screen__sheet"><form class="cd-sheet cd-glass cd-glass--raised" style="padding-bottom:40px" aria-label="Quick add"><i class="cd-sheet__grab"></i>' +
        '<div class="cd-seg" data-n="3" data-i="0"><i class="cd-seg__thumb"></i>' + types.map((t, i) => '<button type="button" data-t="' + i + '">' + t + '</button>').join('') + '</div>' +
        '<label class="cd-field"><input aria-label="Title" maxlength="48" autocomplete="off"></label><div data-extra></div>' +
        '<button class="cd-btn cd-btn--primary cd-btn--block cd-press" type="submit"></button></form></div>';
      const extra = [
        '<div class="cd-chips" style="margin:0 -20px;padding:4px 20px"><button type="button" class="cd-chip is-on">' + ci('clock', true) + 'Today 5:30 pm</button><button type="button" class="cd-chip">' + ci('timer', true) + '25 min</button><button type="button" class="cd-chip">' + ci('flag', true) + 'Priority</button><button type="button" class="cd-chip">' + ci('tag', true) + 'Tag</button></div>',
        '<div style="display:grid;gap:8px"><label class="cd-field" style="min-height:48px"><span class="cd-muted" style="font:500 14px/20px var(--font-text)">Cue</span><input data-cue placeholder="After chai, on the balcony" aria-label="Cue" style="font:400 16px/24px var(--font-text)"></label><label class="cd-field" style="min-height:48px"><span class="cd-muted" style="font:500 14px/20px var(--font-text)">Minimum</span><input data-min placeholder="The smallest version that counts" aria-label="Minimum version" style="font:400 16px/24px var(--font-text)"></label><div class="cd-chips" style="margin:0 -20px;padding:4px 20px"><button type="button" class="cd-chip">Morning</button><button type="button" class="cd-chip">Afternoon</button><button type="button" class="cd-chip is-on">Evening</button></div></div>',
        '<div class="cd-chips" style="margin:0 -20px;padding:4px 20px"><button type="button" class="cd-chip">' + ci('pin', true) + 'Pin</button><button type="button" class="cd-chip">' + ci('tag', true) + 'Tag</button><button type="button" class="cd-chip">' + ci('checklist', true) + 'Checklist</button></div>',
      ];
      const form = $('form', root), field = $('.cd-field input', root);
      const sync = () => {
        const seg = $('.cd-seg', root); seg.dataset.i = type;
        $$('button', seg).forEach((b, i) => b.setAttribute('aria-pressed', String(i === type)));
        field.placeholder = ['What needs doing?', 'What will you do each day?', 'Title'][type];
        $('[data-extra]', root).innerHTML = extra[type];
        $('button[type="submit"]', root).textContent = ['Add task', 'Add habit', 'Save note'][type];
      };
      root.addEventListener('click', (e) => {
        const t = e.target.closest('[data-t]'); if (t) { type = +t.dataset.t; buzz('tick'); sync(); return; }
        const c = e.target.closest('[data-extra] .cd-chip');
        if (c) { if (type === 1) $$('[data-extra] .cd-chips .cd-chip', root).forEach((x) => x.classList.remove('is-on')); c.classList.toggle('is-on'); buzz('tick'); }
      });
      form.addEventListener('submit', (e) => {
        e.preventDefault(); title = field.value.trim();
        if (!title) { field.focus(); return; }
        const id = 'n' + Date.now(); buzz('medium');
        if (type === 0) { D.tasks.push({ id, title, due: 'Today', dur: '25 min', tag: 'Home', when: 'Today' }); ctx.go('today'); ctx.toast('Task added', () => { D.tasks = D.tasks.filter((t) => t.id !== id); ctx.go('today'); emit(); }); }
        else if (type === 1) { const on = $('[data-extra] .cd-chip.is-on', root); D.habits.push({ id, title, cue: ($('[data-cue]', root) || {}).value || '', min: ($('[data-min]', root) || {}).value || '', streak: 0, week: 'nnnnnnt', group: on ? on.textContent : 'Evening' }); ctx.go('habits'); ctx.toast('Habit added', () => { D.habits = D.habits.filter((h) => h.id !== id); ctx.go('habits'); emit(); }); }
        else { D.notes.splice(2, 0, { id, title, body: 'Empty note. Open it to start writing.', tag: '', time: 'Now' }); ctx.go('notes'); ctx.toast('Note saved', () => { D.notes = D.notes.filter((n) => n.id !== id); ctx.go('notes'); }); }
        celebrated = false; emit();
      });
      if (scene) scene.setPlusOpen(true);
      return sync;
    },
    settings(root, ctx) {
      const level = (k, labels) => '<div class="cd-level" data-level="' + k + '" data-v="2"><div class="cd-level__track"><i class="cd-level__fill"></i><div class="cd-level__stops"><i></i><i></i><i></i><i></i></div><i class="cd-level__thumb"></i><input type="range" min="0" max="3" step="1" aria-label="' + k + ' level"></div><div class="cd-level__labels">' + labels.map((l) => '<span>' + l + '</span>').join('') + '</div></div>';
      const tog = (k, label) => '<button class="cd-toggle" role="switch" data-toggle="' + k + '" aria-checked="false" aria-label="' + label + '"></button>';
      const row = (t, s, right) => '<div class="cd-setting"><div class="cd-setting__body"><div class="cd-setting__title">' + t + '</div>' + (s ? '<div class="cd-setting__sub">' + s + '</div>' : '') + '</div>' + right + '</div>';
      const val = (v) => '<span class="cd-setting__value">' + v + ci('chevron-right', true) + '</span>';
      root.innerHTML = glows(1) + '<header class="cd-screen__top">' + iconBtn('chevron-left', 'Back to Today', 'today') + '<div></div></header><h1 style="margin:0 0 8px;font:700 32px/36px var(--font-display);letter-spacing:-.02em">Settings</h1>' +
        '<div class="cd-screen__scroll" style="padding-bottom:48px"><h2 class="cd-section">Feel</h2><div class="cd-glass cd-group">' +
        '<div class="cd-setting--stack"><div class="cd-setting__title">Motion</div>' + level('motion', ['Off', 'Subtle', 'Standard', 'Expressive']) + '</div>' +
        '<div class="cd-setting--stack"><div class="cd-setting__title">Haptics</div>' + level('haptics', ['Off', 'Soft', 'Balanced', 'Firm']) + '</div>' +
        row('Completion only', 'Haptics for finishing a beat, nothing else', tog('completion', 'Completion only')) + '</div>' +
        '<h2 class="cd-section">Appearance</h2><div class="cd-glass cd-group"><div class="cd-setting--stack"><div class="cd-setting__title">Colour palette</div><div class="cd-palettes" data-palettes></div></div>' +
        '<div class="cd-setting--stack"><div class="cd-setting__title">Theme</div><div class="cd-seg" data-seg="theme" data-n="3"><i class="cd-seg__thumb"></i><button type="button" data-v="light">Light</button><button type="button" data-v="dark">Dark</button><button type="button" data-v="system">System</button></div></div>' +
        '<div class="cd-setting--stack"><div class="cd-setting__title">Backdrop</div><div class="cd-seg" data-seg="backdrop" data-n="3"><i class="cd-seg__thumb"></i><button type="button" data-v="glow">Glow</button><button type="button" data-v="soft">Soft</button><button type="button" data-v="plain">Plain</button></div></div>' +
        row('Reduce transparency', 'Solid surfaces instead of glass', tog('glass', 'Reduce transparency')) + row('High contrast', 'Stronger edges, no glow', tog('contrast', 'High contrast')) + '</div>' +
        '<h2 class="cd-section">Rhythm</h2><div class="cd-glass cd-group">' + row('Daily reminder', 'One nudge, nothing else', val('8:00 am')) + row('Weekend rest days', 'Sat and Sun keep every streak', tog('weekend', 'Weekend rest days')) + row('Week starts on', '', val('Monday')) + '</div>' +
        '<h2 class="cd-section">Data</h2><div class="cd-glass cd-group"><button class="cd-setting cd-press" data-export><div class="cd-setting__body"><div class="cd-setting__title">Export data</div></div>' + ci('download') + '</button><button class="cd-setting cd-press" data-erase><div class="cd-setting__body"><div class="cd-setting__title" style="color:var(--danger)">Erase all data</div><div class="cd-setting__sub">The only action that asks first</div></div></button></div></div>' +
        '<div class="cd-screen__toast" data-toast style="bottom:24px"></div>';
      root.addEventListener('click', (e) => {
        if (e.target.closest('[data-export]')) { buzz('light'); ctx.toast('In the app, this saves a file to your phone'); }
        if (e.target.closest('[data-erase]')) { buzz('warning'); ctx.toast('This is a demo, so nothing was erased'); }
      });
      return () => syncControls(root);
    },
  };

  function mountPhone(el, opts) {
    opts = opts || {};
    el.innerHTML = '<div class="phone__clip"><div class="phone__inner"></div></div>';
    const clip = el.firstChild, inner = clip.firstChild;
    const fit = () => el.style.setProperty('--ps', (clip.clientWidth / 390).toFixed(4));
    if (window.ResizeObserver) new ResizeObserver(fit).observe(clip); else window.addEventListener('resize', fit);
    fit();
    let sync = null, current = null, killToast = null;
    const ctx = {
      go(name, silent) {
        if (!SCR[name]) return;
        if (sync) listeners.delete(sync);
        if (scene && current === 'add' && name !== 'add') scene.setPlusOpen(null);
        current = name; D.screen = name;
        const root = document.createElement('div');
        root.className = 'cd-stage cd-screen' + (silent ? '' : ' scr-enter');
        inner.innerHTML = ''; inner.appendChild(root);
        sync = SCR[name](root, ctx); listeners.add(sync); sync();
        if (name === 'add' && !silent && MQ.hover.matches) { const f = $('.cd-field input', root); f && f.focus({ preventScroll: true }); }
        opts.onScreen && opts.onScreen(name);
      },
      toast(text, undo) {
        killToast && killToast(); killToast = null;
        const host = $('[data-toast]', inner); if (!host || !text) return;
        killToast = toastIn(host, text, { undo });
      },
      get current() { return current; },
    };
    inner.addEventListener('click', (e) => {
      const tb = e.target.closest('[data-toggle-beat]'); if (tb) return toggleBeat(tb.dataset.toggleBeat, ctx);
      const lt = e.target.closest('[data-later]');
      if (lt) { D.order = D.order.filter((x) => x !== lt.dataset.later).concat([lt.dataset.later]); buzz('light'); return emit(); }
      const dt = e.target.closest('[data-detail]'); if (dt) { D.detail = dt.dataset.detail; return ctx.go('detail'); }
      const go = e.target.closest('[data-go]'); if (go) { buzz(go.dataset.go === 'add' ? 'light' : 'tick'); return ctx.go(go.dataset.go); }
    });
    inner.addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.target.matches('[data-detail]')) { D.detail = e.target.dataset.detail; ctx.go('detail'); } });
    return ctx;
  }

  /* ================= hero phone ================= */
  const hero = mountPhone($('#heroPhone'));
  hero.go('today', true);

  /* ================= screens tour ================= */
  const SCREENS = [
    { id: 'today', name: 'Today', icon: 'sun', motif: 'today', purpose: 'Know the next action and how the day is going.', body: 'One number over a segmented bar, and a single Next up card. Everything due today is a beat, and nothing else is on the screen.', flow: ['Date', 'Beat bar', 'Next up', 'Habits', 'Tasks'], action: 'Mark done', quote: 'How we spend our days is, of course, how we spend our lives.', by: 'Annie Dillard, The Writing Life' },
    { id: 'habits', name: 'Habits', icon: 'repeat', motif: 'habits', purpose: 'See every habit and its week.', body: 'Each habit carries a cue, written the way you would say it, and a minimum version for low-energy days. Seven dots show the week, grouped by morning, afternoon and evening.', flow: ['Morning', 'Afternoon', 'Evening'], action: 'Check on a row', quote: 'You do not rise to the level of your goals. You fall to the level of your systems.', by: 'James Clear, Atomic Habits' },
    { id: 'detail', name: 'Habit detail', icon: 'grid', motif: 'detail', purpose: 'Understand one habit without judgement.', body: 'A streak count, the active days behind it, and five weeks of history. The minimum version, the time of day and the rest days are all one tap away.', flow: ['Title and cue', 'Streak', 'Five-week grid', 'Rhythm'], action: 'Log today', quote: 'Rest is part of the rhythm.', by: '' },
    { id: 'tasks', name: 'Tasks', icon: 'tasks', motif: 'tasks', purpose: 'Decide what to do this week.', body: 'Each task has a due time, a duration, a priority flag and a tag. They sit in three groups: Today, This week and Done.', flow: ['Filter chips', 'Today', 'This week', 'Done'], action: 'Check on a row', quote: 'A journey of a thousand miles begins with a single step.', by: 'Lao Tzu' },
    { id: 'notes', name: 'Notes', icon: 'notes', motif: 'notes', purpose: 'Find or start a note.', body: 'Pinned notes stay on top. Search and tags narrow the rest as you type. Try searching for "chai" in the phone.', flow: ['Search', 'Tags', 'Pinned', 'All notes'], action: 'Open a note', quote: 'Keep what matters where you can find it.', by: '' },
    { id: 'editor', name: 'Note editor', icon: 'edit', motif: 'notes', purpose: 'Write without chrome.', body: 'One steady tile for reading and writing, with checklist lines that tick off in place. Notes are plain Markdown underneath and save themselves.', flow: ['Title', 'Body', 'Checklist lines', 'Tags'], action: 'Typing. It saves itself', quote: 'Write it down so your mind can rest.', by: '' },
    { id: 'add', name: 'Quick add', icon: 'plus', motif: 'add', purpose: 'Capture in one tap.', body: 'The centre button opens one sheet with the keyboard up. Switch between task, habit and note. Type a title in the phone and add it: it lands in the right list.', flow: ['Type switch', 'Title', 'Optional chips', 'Add'], action: 'Add task, Add habit or Save note', quote: 'A title alone is a complete entry.', by: '' },
    { id: 'settings', name: 'Settings', icon: 'sliders', motif: 'settings', purpose: 'Set rhythm, look and feel.', body: 'Every control applies at once, and here they apply to this whole page. Change the palette or the motion level in the phone and watch everything follow.', flow: ['Feel', 'Appearance', 'Rhythm', 'Data'], action: 'None. Every control applies at once', quote: 'Calm is a setting, and it is yours to set.', by: '' },
  ];
  let tourIdx = 0;
  const tabsEl = $('#tourTabs'), infoEl = $('#tourInfo');
  tabsEl.innerHTML = SCREENS.map((s, i) => '<button class="tourtab" role="tab" type="button" id="tab-' + s.id + '" data-i="' + i + '" aria-selected="false" tabindex="-1">' + icon(s.icon) + '<span>' + s.name + '</span><small>' + (i + 1) + '</small></button>').join('');
  const tour = mountPhone($('#tourPhone'), { onScreen(name) { const i = SCREENS.findIndex((s) => s.id === name); if (i >= 0 && i !== tourIdx) tourSelect(i, true); } });
  function tourSelect(i, fromPhone) {
    tourIdx = (i + SCREENS.length) % SCREENS.length;
    const s = SCREENS[tourIdx];
    $$('.tourtab', tabsEl).forEach((b, j) => { const on = j === tourIdx; b.setAttribute('aria-selected', String(on)); b.tabIndex = on ? 0 : -1; if (on && tabsEl.scrollWidth > tabsEl.clientWidth) tabsEl.scrollTo({ left: b.offsetLeft - tabsEl.clientWidth / 2 + b.offsetWidth / 2, behavior: effM() ? 'smooth' : 'auto' }); });
    if (!fromPhone && tour.current !== s.id) tour.go(s.id);
    infoEl.setAttribute('aria-labelledby', 'tab-' + s.id);
    infoEl.classList.remove('is-in');
    infoEl.innerHTML = '<p class="cd-eyebrow">' + (tourIdx + 1) + ' of ' + SCREENS.length + ' · ' + esc(s.purpose) + '</p><h3>' + s.name + '</h3><p>' + esc(s.body) + '</p>' +
      '<ol class="flow" aria-label="Order of content">' + s.flow.map((f) => '<li><span>' + f + '</span></li>').join('') + '</ol>' +
      '<div class="pa cd-glass">' + icon('pointer') + '<span>Primary action: <b>' + esc(s.action) + '</b></span></div>' +
      '<figure class="quote quote--inline"><blockquote>' + esc(s.quote) + '</blockquote>' + (s.by ? '<figcaption>' + esc(s.by) + '</figcaption>' : '') + '</figure>';
    void infoEl.offsetWidth; infoEl.classList.add('is-in');
    $('#tourCount').textContent = (tourIdx + 1) + ' / ' + SCREENS.length;
    if (scene) scene.setMotif(s.motif);
  }
  tabsEl.addEventListener('click', (e) => { const b = e.target.closest('.tourtab'); if (b) { buzz('tick'); tourSelect(+b.dataset.i); } });
  tabsEl.addEventListener('keydown', (e) => {
    const d = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[e.key]; if (!d) return;
    e.preventDefault(); tourSelect(tourIdx + d); $$('.tourtab', tabsEl)[tourIdx].focus();
  });
  $('#tourPrev').addEventListener('click', () => { buzz('tick'); tourSelect(tourIdx - 1); });
  $('#tourNext').addEventListener('click', () => { buzz('tick'); tourSelect(tourIdx + 1); });
  tour.go('today', true); tourSelect(0, true);

  /* ================= streak grid ================= */
  let streakCells = READ_GRID.split('');
  const sgrid = $('#sgrid');
  const CELLCLS = { d: 'done', m: 'min', r: 'rest', x: 'miss', t: 'today', f: 'future' };
  const CELLWORD = { d: 'done', m: 'minimum', r: 'rest day', x: 'missed', t: 'today, not logged yet', f: 'not yet' };
  sgrid.innerHTML = streakCells.map((c, i) => '<button class="sc" type="button" data-c="' + i + '"' + (i > TODAY_IDX ? ' disabled' : '') + '><i class="cd-cell"></i></button>').join('');
  function streakSync(changed) {
    $$('.sc', sgrid).forEach((b, i) => {
      const c = streakCells[i]; b.firstChild.className = 'cd-cell cd-cell--' + CELLCLS[c];
      const wk = Math.floor(i / 7) + 1, day = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][i % 7];
      b.setAttribute('aria-label', 'Week ' + wk + ', ' + day + ': ' + CELLWORD[c]);
    });
    const n = streakOf(streakCells, TODAY_IDX), a = activeOf(streakCells);
    $('#streakN').textContent = n; $('#streakActive').textContent = a[0] + ' of ' + a[1];
    const t = streakCells[TODAY_IDX];
    $('#streakSay').textContent = (n === 0 ? 'No streak yet. The next kept day starts one.' : n + (n === 1 ? ' day' : ' days') + ' kept in a row.') + (t === 't' ? ' Today is still open.' : t === 'm' ? ' Today counted with the minimum.' : t === 'r' ? ' Today is a rest day.' : t === 'x' ? '' : ' Today is logged.');
    if (scene) scene.setGrid(streakCells.map((c) => (c === 't' ? 'f' : c)));
  }
  sgrid.addEventListener('click', (e) => {
    const b = e.target.closest('.sc'); if (!b || b.disabled) return;
    const i = +b.dataset.c, c = streakCells[i];
    const cyc = i === TODAY_IDX ? ['t', 'd', 'm', 'r'] : ['d', 'm', 'r', 'x'];
    streakCells[i] = cyc[(cyc.indexOf(c) + 1) % cyc.length];
    buzz(streakCells[i] === 'd' || streakCells[i] === 'm' ? 'medium' : 'tick');
    streakSync(i);
  });
  $('#streakReset').addEventListener('click', () => { streakCells = READ_GRID.split(''); buzz('tick'); streakSync(); });
  streakSync();

  /* ================= feel: try rows ================= */
  const lvM = $('#lvMotion'), lvH = $('#lvHaptics'); lvM.dataset.level = 'motion'; lvH.dataset.level = 'haptics';
  const tryEl = $('#feelTry');
  tryEl.innerHTML = [['Glass of water', 'Right after waking', '', true], ['Read 10 pages', 'After chai, on the balcony', '', false], ['Book train tickets', 'Today · 10 min', ' cd-check--task', false]].map((r) => '<div class="cd-row cd-glass cd-press' + (r[3] ? ' is-done' : '') + '"><button class="cd-check' + r[2] + '" type="button" aria-label="' + r[0] + '" aria-pressed="' + r[3] + '">' + TICK + '</button><div class="cd-row__body"><div class="cd-row__title">' + r[0] + '</div><div class="cd-row__sub">' + r[1] + '</div></div></div>').join('');
  tryEl.addEventListener('click', (e) => { const c = e.target.closest('.cd-check'); if (!c) return; const on = c.parentElement.classList.toggle('is-done'); c.setAttribute('aria-pressed', String(on)); buzz(on ? 'medium' : 'tick'); if (on && scene) scene.pulseTick(); });

  /* ================= principle demos ================= */
  (function demos() {
    const items = [['Read 10 pages', 'After chai, on the balcony · Minimum: 1 page'], ['Evening walk', 'After dinner · Minimum: 5 minutes'], ['Send invoice to Meera', '5:30 pm · 25 min'], ['Three lines', 'In bed, before the phone · Minimum: 1 line']];
    let i = 0; const sw = $('#demoNext .demo__swap');
    const swap = (h) => { buzz(h); sw.classList.remove('is-in'); sw.classList.add('is-out'); setTimeout(() => { i = (i + 1) % items.length; $('#demoNextTitle').textContent = items[i][0]; $('#demoNextLine').textContent = items[i][1]; sw.classList.remove('is-out'); void sw.offsetWidth; sw.classList.add('is-in'); }, effM() ? 160 : 0); };
    $('#demoNextDone').addEventListener('click', () => { swap('medium'); if (scene) scene.pulseTick(); });
    $('#demoNextLater').addEventListener('click', () => swap('light'));

    const list = $('#demoAddList'), inp = $('#demoAddInput');
    $('#demoAdd').addEventListener('submit', (e) => {
      e.preventDefault(); const v = inp.value.trim(); if (!v) { inp.focus(); return; }
      const row = document.createElement('div'); row.className = 'cd-row cd-glass cd-enter';
      row.innerHTML = '<button class="cd-check cd-check--task" type="button" aria-label="Complete ' + esc(v) + '">' + TICK + '</button><div class="cd-row__body"><div class="cd-row__title">' + esc(v) + '</div></div>';
      list.prepend(row); while (list.children.length > 2) list.lastChild.remove();
      inp.value = ''; buzz('medium');
    });
    list.addEventListener('click', (e) => { const c = e.target.closest('.cd-check'); if (c) { const on = c.parentElement.classList.toggle('is-done'); buzz(on ? 'medium' : 'tick'); } });

    const row = $('#demoUndoRow'), host = $('#demoUndoToast');
    $('#demoUndoBtn').addEventListener('click', () => {
      buzz('light'); row.classList.add('is-gone');
      toastIn(host, 'Note deleted', { undo: () => row.classList.remove('is-gone'), done: () => setTimeout(() => row.classList.remove('is-gone'), 900) });
    });
  })();

  /* ================= performance UI ================= */
  const TIER_NAME = { full: 'full', balanced: 'balanced', light: 'light', off: 'essential' };
  function perfUI() {
    const auto = S.perf === 'auto';
    $('#perfPillText').textContent = (auto ? 'Auto · ' : '') + TIER_NAME[tier][0].toUpperCase() + TIER_NAME[tier].slice(1);
    const why = auto
      ? (P.downgraded ? 'Frames were dropping, so the page stepped down to stay smooth. ' : '') + 'Chosen automatically because ' + P.reasons.join(', ') + '.' + (MQ.motion.matches ? ' Your system asks for reduced motion, so movement is off.' : '')
      : 'You chose this mode. Pick Auto to let the page decide again.' + (S.perf !== 'off' && !has3d() ? ' The 3D scene cannot run on this device.' : '');
    $('#lookNote').textContent = why;
  }

  /* ================= frame loop + runtime monitor ================= */
  let running = false, lastT = 0, acc = 0, frames = 0, lowSecs = 0, lastChange = performance.now();
  const hist = [];
  function downgrade() {
    const i = ORDER.indexOf(tier); if (i <= 0) return;
    P.auto = ORDER[i - 1]; P.downgraded = true; lastChange = performance.now(); lowSecs = 0;
    try { sessionStorage.setItem(KEY + '-tier', P.auto); } catch (e) {}
    apply();
    toast('Switched to ' + TIER_NAME[tier] + ' mode to keep things smooth.', { undo: () => set('perf', ORDER[i] === 'balanced' ? 'full' : ORDER[i], false) });
  }
  function loop(t) {
    if (!running) return;
    const dt = lastT ? t - lastT : 16; lastT = t;
    if (dt < 250) {
      acc += dt; frames++;
      if (acc >= 1000) {
        const fps = Math.round(frames * 1000 / acc); acc = 0; frames = 0;
        hist.push(fps); if (hist.length > 40) hist.shift();
        // Step down one mode after three slow seconds in a row. Never step up on its own: no flapping.
        if (S.perf === 'auto' && t - lastChange > 4000 && document.hasFocus()) { lowSecs = fps < 40 ? lowSecs + 1 : 0; if (lowSecs >= 3) downgrade(); }
      }
    } else { acc = 0; frames = 0; lowSecs = 0; }
    if (scene) scene.frame(t, dt / 1000);
    if (needLoop()) requestAnimationFrame(loop); else running = false;
  }
  const needLoop = () => !document.hidden && !!scene;
  function startLoop() { if (running || !needLoop()) return; running = true; lastT = 0; requestAnimationFrame(loop); }
  document.addEventListener('visibilitychange', () => { lowSecs = 0; startLoop(); });
  window.addEventListener('scroll', () => { if (scene) scene.invalidate(); }, { passive: true });

  /* ================= pointer: parallax, tilt, spotlight ================= */
  let ptrQueued = false, px = 0, py = 0;
  window.addEventListener('pointermove', (e) => {
    if (e.pointerType === 'touch') return;
    px = e.clientX; py = e.clientY;
    if (ptrQueued) return; ptrQueued = true;
    requestAnimationFrame(() => {
      ptrQueued = false;
      if (scene) scene.setPointer((px / innerWidth) * 2 - 1, (py / innerHeight) * 2 - 1);
      if (tier === 'off' || !effM()) return;
      const el = document.elementFromPoint(px, py); if (!el) return;
      const sp = el.closest('[data-spot]');
      if (sp) { const r = sp.getBoundingClientRect(); sp.style.setProperty('--mx', (px - r.left) + 'px'); sp.style.setProperty('--my', (py - r.top) + 'px'); }
      $$('[data-tilt]').forEach((tl) => {
        const r = tl.getBoundingClientRect();
        if (r.bottom < 0 || r.top > innerHeight) return;
        const nx = Math.max(-1, Math.min(1, (px - (r.left + r.width / 2)) / (innerWidth / 2))), ny = Math.max(-1, Math.min(1, (py - (r.top + r.height / 2)) / (innerHeight / 2)));
        tl.style.setProperty('--ry', (nx * 7 * effM()).toFixed(2) + 'deg'); tl.style.setProperty('--rx', (-ny * 5 * effM()).toFixed(2) + 'deg');
      });
    });
  }, { passive: true });

  /* ================= reveal, nav, sheet ================= */
  const rv = $$('.rv');
  if ('IntersectionObserver' in window) {
    const io = new IntersectionObserver((es) => es.forEach((en) => { if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); } }), { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
    rv.forEach((el) => io.observe(el));
    const links = $$('.nav__links a, .mtab .cd-tab');
    const secIO = new IntersectionObserver((es) => es.forEach((en) => {
      if (!en.isIntersecting) return; const id = en.target.id;
      links.forEach((a) => a.classList.toggle('is-on', a.getAttribute('href') === '#' + id || (id === 'principles' && a.dataset.sec === 'top')));
    }), { rootMargin: '-45% 0px -50% 0px' });
    $$('main section[id]').forEach((s) => secIO.observe(s));
  } else rv.forEach((el) => el.classList.add('in'));

  const look = $('#look'), lookBtn = $('#lookBtn');
  let lookFrom = null;
  function openLook(from) { lookFrom = from || lookBtn; look.hidden = false; look.classList.remove('is-closing'); buzz('light'); syncControls(look); const f = $('.cd-swatch[aria-pressed="true"]', look); f && f.focus({ preventScroll: true }); }
  function closeLook() { if (look.hidden) return; look.classList.add('is-closing'); setTimeout(() => { look.hidden = true; look.classList.remove('is-closing'); }, effM() ? 240 : 0); lookFrom && lookFrom.focus({ preventScroll: true }); }
  lookBtn.addEventListener('click', () => (look.hidden ? openLook(lookBtn) : closeLook()));
  $('#perfPill').addEventListener('click', () => (look.hidden ? openLook($('#perfPill')) : closeLook()));
  look.addEventListener('click', (e) => { if (e.target.closest('[data-close]')) closeLook(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeLook(); });

  /* ================= get ================= */
  const storeUrl = ($('#page').dataset.storeUrl || '').trim();
  // A direct link to the installer: the browser starts the download and the page stays put.
  if (storeUrl) $('[data-get], #storeBtn').forEach((a) => { a.href = storeUrl; a.setAttribute('download', 'Cadence.apk'); });
  // Version and size of what the button will download, read once from GitHub's public release data. Silent if it fails.
  try {
    fetch('https://api.github.com/repos/yashraj-shitole/Cadence/releases/latest', { headers: { Accept: 'application/vnd.github+json' } })
      .then((r) => (r.ok ? r.json() : null))
      .then((rel) => {
        if (!rel || !rel.tag_name) return;
        const apk = (rel.assets || []).find((a) => a.name === 'Cadence.apk');
        const text = 'Version ' + String(rel.tag_name).replace(/^v/, '') + (apk ? ' · ' + (apk.size / 1048576).toFixed(1) + ' MB' : '');
        $('[data-version]').forEach((el) => { el.textContent = text; el.hidden = false; });
      })
      .catch(() => {});
  } catch (e) { /* offline or blocked: the button still works */ }

  /* ================= boot ================= */
  apply();
  window.__cadence = { S, P, get tier() { return tier; }, get scene() { return scene; }, hist };
})();
