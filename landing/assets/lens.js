/* Real refraction for the glass on this page, the way the Android app does it.
   The app models a slab of glass whose rim is a convex squircle bezel: a ray meets the surface at an angle given by the slope of
   h(x) = (1 - (1 - x)^4)^(1/4), Snell's law at n = 1.5 bends it toward the normal, and the backdrop is sampled that far inward,
   so the edge magnifies like the thick rim of a lens; red and blue are sampled a little apart for a faint colour fringe.
   Here the same maths is baked into a displacement map per glass element and applied with backdrop-filter. Only Chromium can
   run an SVG filter as a backdrop filter, so Safari and Firefox keep the CSS glass. Off in the Light and Essential modes. */
(function () {
  'use strict';
  var ua = navigator.userAgent;
  var ok = /Chrome\/|Edg\//.test(ua) && !/OPR\/|iPhone|iPad|iPod/.test(ua) && window.CSS && CSS.supports && CSS.supports('backdrop-filter', 'url(#x) blur(1px)');
  if (!ok) return;

  var NS = 'http://www.w3.org/2000/svg';
  var html = document.documentElement;
  var svg = document.createElementNS(NS, 'svg');
  svg.setAttribute('width', '0'); svg.setAttribute('height', '0'); svg.setAttribute('aria-hidden', 'true');
  svg.style.cssText = 'position:absolute;width:0;height:0;overflow:hidden';
  var defs = document.createElementNS(NS, 'defs');
  svg.appendChild(defs);
  document.body.appendChild(svg);

  var targets = '.cd-glass, .cd-btn--glass, .cd-iconbtn';
  var counter = 0;
  var state = new WeakMap();      // element -> { id, w, h, r }
  var live = new Set();           // elements currently lensed
  var MAX_LIVE = 16;
  var MAP_CAP = 320;              // longest side of a displacement map, in px: the filter stretches it

  function tier() { return html.dataset.perf || 'balanced'; }
  function enabled() { var t = tier(); return t === 'full' || t === 'balanced'; }

  function sdRoundRect(px, py, hx, hy, r) {
    var qx = Math.abs(px) - (hx - r), qy = Math.abs(py) - (hy - r);
    return Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - r;
  }

  /* One element's lens: a displacement map (R and G carry the sideways shift, 128 is none) and the filter that uses it. */
  function build(el, w, h, r) {
    var sx = Math.min(1, MAP_CAP / Math.max(w, h));
    var cw = Math.max(2, Math.round(w * sx)), ch = Math.max(2, Math.round(h * sx));
    var rr = Math.min(r * sx, cw / 2, ch / 2);
    var minSide = Math.min(w, h);
    var boost = 1 + 0.6 * Math.min(1, Math.max(0, (minSide - 48) / 160));
    var bezel = Math.min(22 * (0.8 + 0.2 * boost), minSide * 0.45) * sx;      // css px, then map px
    var thick = Math.min(40 * boost, minSide * 0.8) * sx;
    var hx = cw / 2, hy = ch / 2;

    // First pass: the shift in map px at every pixel, and the largest, so the map can be normalised.
    var dxs = new Float32Array(cw * ch), dys = new Float32Array(cw * ch), maxShift = 0.0001;
    for (var y = 0; y < ch; y++) {
      for (var x = 0; x < cw; x++) {
        var px = x + 0.5 - hx, py = y + 0.5 - hy;
        var depth = -sdRoundRect(px, py, hx, hy, rr);
        if (depth >= bezel || depth < 0) continue;
        var t = Math.min(1, Math.max(0, depth / bezel)), u = 1 - t, u4 = u * u * u * u;
        var inner = Math.max(1 - u4, 1e-4);
        var height = Math.pow(inner, 0.25);
        var slope = u * u * u * Math.pow(inner, -0.75) * thick / bezel;
        var th1 = Math.atan(slope), th2 = Math.asin(Math.sin(th1) / 1.5);
        var shift = height * thick * Math.tan(th1 - th2);
        var gx = sdRoundRect(px + 1, py, hx, hy, rr) - sdRoundRect(px - 1, py, hx, hy, rr);
        var gy = sdRoundRect(px, py + 1, hx, hy, rr) - sdRoundRect(px, py - 1, hx, hy, rr);
        var len = Math.hypot(gx, gy);
        if (len < 1e-4) { gx = 0; gy = -1; len = 1; }
        // The map says where to sample from: inward, against the outward normal.
        var i = y * cw + x;
        dxs[i] = -gx / len * shift; dys[i] = -gy / len * shift;
        if (shift > maxShift) maxShift = shift;
      }
    }

    var canvas = document.createElement('canvas'); canvas.width = cw; canvas.height = ch;
    var ctx = canvas.getContext('2d'); var img = ctx.createImageData(cw, ch), d = img.data;
    for (var k = 0; k < cw * ch; k++) {
      d[k * 4] = Math.round(127.5 + 127.5 * dxs[k] / maxShift);
      d[k * 4 + 1] = Math.round(127.5 + 127.5 * dys[k] / maxShift);
      d[k * 4 + 2] = 128; d[k * 4 + 3] = 255;
    }
    ctx.putImageData(img, 0, 0);

    // The filter's scale is in css px: a map value of 255 asks for scale / 2 of shift.
    var S = 2 * (maxShift / sx);
    var id = 'lens-' + (++counter);
    var f = document.createElementNS(NS, 'filter');
    f.setAttribute('id', id); f.setAttribute('filterUnits', 'userSpaceOnUse');
    f.setAttribute('x', '0'); f.setAttribute('y', '0'); f.setAttribute('width', String(w)); f.setAttribute('height', String(h));
    f.setAttribute('color-interpolation-filters', 'sRGB');
    var im = document.createElementNS(NS, 'feImage');
    im.setAttribute('href', canvas.toDataURL('image/png')); im.setAttribute('x', '0'); im.setAttribute('y', '0');
    im.setAttribute('width', String(w)); im.setAttribute('height', String(h)); im.setAttribute('preserveAspectRatio', 'none'); im.setAttribute('result', 'map');
    f.appendChild(im);
    // Three displacements, red a little less and blue a little more, then each channel kept and screened together.
    [['0.9', '1 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 1 0', 'r'], ['1', '0 0 0 0 0  0 1 0 0 0  0 0 0 0 0  0 0 0 1 0', 'g'], ['1.1', '0 0 0 0 0  0 0 0 0 0  0 0 1 0 0  0 0 0 1 0', 'b']].forEach(function (c) {
      var disp = document.createElementNS(NS, 'feDisplacementMap');
      disp.setAttribute('in', 'SourceGraphic'); disp.setAttribute('in2', 'map'); disp.setAttribute('scale', String(S * parseFloat(c[0])));
      disp.setAttribute('xChannelSelector', 'R'); disp.setAttribute('yChannelSelector', 'G'); disp.setAttribute('result', 'd' + c[2]);
      var cm = document.createElementNS(NS, 'feColorMatrix');
      cm.setAttribute('in', 'd' + c[2]); cm.setAttribute('type', 'matrix'); cm.setAttribute('values', c[1]); cm.setAttribute('result', c[2]);
      f.appendChild(disp); f.appendChild(cm);
    });
    var b1 = document.createElementNS(NS, 'feBlend'); b1.setAttribute('in', 'r'); b1.setAttribute('in2', 'g'); b1.setAttribute('mode', 'screen'); b1.setAttribute('result', 'rg');
    var b2 = document.createElementNS(NS, 'feBlend'); b2.setAttribute('in', 'rg'); b2.setAttribute('in2', 'b'); b2.setAttribute('mode', 'screen');
    f.appendChild(b1); f.appendChild(b2);
    defs.appendChild(f);
    return id;
  }

  function remove(el) {
    var s = state.get(el);
    if (s && s.id) { var old = defs.querySelector('#' + s.id); if (old) old.remove(); }
    el.removeAttribute('data-lens'); el.style.removeProperty('--lens');
    state.delete(el); live.delete(el);
  }

  function apply(el) {
    var w = el.offsetWidth, h = el.offsetHeight;
    if (w < 36 || h < 36) return remove(el);
    var r = parseFloat(getComputedStyle(el).borderTopLeftRadius) || 0;
    r = Math.min(r, w / 2, h / 2);
    var s = state.get(el);
    if (s && s.w === w && s.h === h && Math.abs(s.r - r) < 0.5) { el.setAttribute('data-lens', ''); return; }
    var oldId = s && s.id;
    var id = build(el, w, h, r);
    if (oldId) { var old = defs.querySelector('#' + oldId); if (old) old.remove(); }
    state.set(el, { id: id, w: w, h: h, r: r });
    el.style.setProperty('--lens', 'url(#' + id + ')');
    el.setAttribute('data-lens', '');
    live.add(el);
  }

  /* Only glass near the screen is lensed, at most a handful at a time: SVG backdrop filters are not free. */
  var io = new IntersectionObserver(function (entries) {
    if (!enabled()) return;
    entries.forEach(function (en) {
      if (en.isIntersecting) { if (live.size < MAX_LIVE || live.has(en.target)) apply(en.target); }
      else if (live.has(en.target)) remove(en.target);
    });
  }, { rootMargin: '120px' });

  var ro = new ResizeObserver(function (entries) {
    if (!enabled()) return;
    entries.forEach(function (en) { if (live.has(en.target)) apply(en.target); });
  });

  function watch(root) {
    var nodes = (root.matches && root.matches(targets) ? [root] : []).concat(Array.prototype.slice.call(root.querySelectorAll ? root.querySelectorAll(targets) : []));
    nodes.forEach(function (el) { if (el.parentElement && el.parentElement.closest('.cd-glass')) return; if (!el.__lensWatched) { el.__lensWatched = true; io.observe(el); ro.observe(el); } });
  }

  function sync() {
    if (enabled()) { watch(document.body); }
    else { Array.from(live).forEach(remove); }
  }

  new MutationObserver(function (muts) {
    muts.forEach(function (m) { m.addedNodes.forEach(function (n) { if (n.nodeType === 1 && enabled()) watch(n); }); });
  }).observe(document.body, { childList: true, subtree: true });
  new MutationObserver(sync).observe(html, { attributes: true, attributeFilter: ['data-perf', 'data-glass', 'data-theme'] });

  if (document.readyState === 'complete') setTimeout(sync, 400); else window.addEventListener('load', function () { setTimeout(sync, 400); });
})();
