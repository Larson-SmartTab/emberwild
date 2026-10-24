/* 3D polyhedral dice rendered on a 2D canvas. The server decides the result; the die tumbles and settles
   with exactly that face toward you. */
(function () {
  const D = EW.dice = {};
  const cv = document.getElementById('diceCanvas');
  const ctx = cv.getContext('2d');
  const resEl = document.getElementById('diceResult');
  let W = 0, H = 0, dpr = 1, active = [], queue = [], busy = false;

  // ------------------------------------------------------------------ vector math
  const sub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
  const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
  const len = a => Math.hypot(a[0], a[1], a[2]);
  const norm = a => { const l = len(a) || 1; return [a[0] / l, a[1] / l, a[2] / l]; };
  const qmul = (a, b) => [a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3], a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2], a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1], a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]];
  const qnorm = q => { const l = Math.hypot(...q) || 1; return q.map(x => x / l); };
  const qaxis = (ax, ang) => { const s = Math.sin(ang / 2), n = norm(ax); return [Math.cos(ang / 2), n[0] * s, n[1] * s, n[2] * s]; };
  function qrot(q, v) { const p = [0, v[0], v[1], v[2]]; const qc = [q[0], -q[1], -q[2], -q[3]]; const r = qmul(qmul(q, p), qc); return [r[1], r[2], r[3]]; }
  function qfromTo(a, b) {
    a = norm(a); b = norm(b);
    const d = dot(a, b);
    if (d > 0.99999) return [1, 0, 0, 0];
    if (d < -0.99999) { let ax = cross([1, 0, 0], a); if (len(ax) < .01) ax = cross([0, 1, 0], a); return qaxis(ax, Math.PI); }
    const c = cross(a, b);
    return qnorm([1 + d, c[0], c[1], c[2]]);
  }
  function slerp(a, b, t) {
    let d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
    if (d < 0) { b = b.map(x => -x); d = -d; }
    if (d > .9995) return qnorm(a.map((x, i) => x + (b[i] - x) * t));
    const th = Math.acos(d), s = Math.sin(th);
    return a.map((x, i) => (x * Math.sin((1 - t) * th) + b[i] * Math.sin(t * th)) / s);
  }

  // ------------------------------------------------------------------ geometry
  function orderFace(verts, idx) {
    const c = norm(idx.reduce((s, i) => [s[0] + verts[i][0], s[1] + verts[i][1], s[2] + verts[i][2]], [0, 0, 0]));
    let u = norm(cross(c, Math.abs(c[0]) < .9 ? [1, 0, 0] : [0, 1, 0])); const v = cross(c, u);
    return idx.slice().sort((i, j) => Math.atan2(dot(verts[i], v), dot(verts[i], u)) - Math.atan2(dot(verts[j], v), dot(verts[j], u)));
  }
  function facesByEdge(verts, k) {
    let min = 1e9;
    for (let i = 0; i < verts.length; i++) for (let j = i + 1; j < verts.length; j++) min = Math.min(min, len(sub(verts[i], verts[j])));
    const near = (i, j) => Math.abs(len(sub(verts[i], verts[j])) - min) < 1e-3;
    const faces = [];
    const n = verts.length;
    for (let a = 0; a < n; a++) for (let b = a + 1; b < n; b++) if (near(a, b)) for (let c = b + 1; c < n; c++) if (near(a, c) && near(b, c)) faces.push(orderFace(verts, [a, b, c]));
    return faces;
  }
  const PHI = (1 + Math.sqrt(5)) / 2;
  const GEO = {};
  (function build() {
    // d4
    const t4 = [[1, 1, 1], [1, -1, -1], [-1, 1, -1], [-1, -1, 1]];
    GEO[4] = { v: t4, f: facesByEdge(t4, 3), scale: .75 };
    // d6
    const c6 = []; for (const x of [-1, 1]) for (const y of [-1, 1]) for (const z of [-1, 1]) c6.push([x, y, z]);
    const f6 = []; for (let ax = 0; ax < 3; ax++) for (const sgn of [-1, 1]) f6.push(orderFace(c6, c6.map((v, i) => v[ax] === sgn ? i : -1).filter(i => i >= 0)));
    GEO[6] = { v: c6, f: f6, scale: .62 };
    // d8
    const o8 = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [0, 0, 1], [0, 0, -1]];
    GEO[8] = { v: o8, f: facesByEdge(o8, 3), scale: 1 };
    // d20
    const i20 = [];
    for (const a of [-1, 1]) for (const b of [-1, 1]) { i20.push([0, a, b * PHI]); i20.push([a, b * PHI, 0]); i20.push([b * PHI, 0, a]); }
    GEO[20] = { v: i20.map(norm), f: facesByEdge(i20.map(norm), 3), scale: 1.05 };
    // d12: dual of the icosahedron
    const iv = GEO[20].v, ifc = GEO[20].f;
    const dv = ifc.map(f => norm(f.reduce((s, i) => [s[0] + iv[i][0], s[1] + iv[i][1], s[2] + iv[i][2]], [0, 0, 0])));
    const df = iv.map((_, vi) => orderFace(dv, ifc.map((f, fi) => f.includes(vi) ? fi : -1).filter(x => x >= 0)));
    GEO[12] = { v: dv, f: df, scale: 1.05 };
    // d10: pentagonal trapezohedron
    const v10 = [[0, 0, 1.15], [0, 0, -1.15]], z0 = .12;
    for (let k = 0; k < 10; k++) { const a = k * Math.PI / 5; v10.push([Math.cos(a), Math.sin(a), k % 2 ? -z0 : z0]); }
    const f10 = [];
    for (let k = 0; k < 10; k += 2) { f10.push(orderFace(v10, [0, 2 + k, 2 + (k + 1) % 10, 2 + (k + 2) % 10])); f10.push(orderFace(v10, [1, 2 + (k + 1) % 10, 2 + (k + 2) % 10, 2 + (k + 3) % 10])); }
    GEO[10] = { v: v10, f: f10, scale: .95 };
    for (const k in GEO) {
      const g = GEO[k];
      g.normals = g.f.map(f => norm(f.reduce((s, i) => [s[0] + g.v[i][0], s[1] + g.v[i][1], s[2] + g.v[i][2]], [0, 0, 0])));
    }
  })();

  const STYLE = {
    20: ['#7a1f24', '#f3d27a'], 12: ['#2a4f7a', '#e8f1ff'], 10: ['#1f5a4c', '#e9fff6'], 8: ['#5a2a7a', '#f2e6ff'], 6: ['#e9e2d0', '#2a1e14'], 4: ['#7a5a1f', '#fff1cc'],
  };

  function resize() {
    dpr = Math.min(2, window.devicePixelRatio || 1);
    W = cv.width = Math.floor(window.innerWidth * dpr);
    H = cv.height = Math.floor(window.innerHeight * dpr);
  }
  window.addEventListener('resize', resize);
  resize();

  function makeDie(sides, face, kept, i, n, dur, color) {
    const s = GEO[sides] ? sides : (sides === 100 ? 10 : 20);
    const g = GEO[s];
    const faceIdx = Math.max(0, Math.min(g.f.length - 1, ((face - 1) % g.f.length + g.f.length) % g.f.length));
    const target = qmul(qaxis([0, 0, 1], (Math.random() - .5) * .6), qfromTo(g.normals[faceIdx], [0, 0, 1]));
    const size = Math.min(W, H) * (n > 4 ? .06 : .085);
    const spread = Math.min(W * .8, n * size * 2.6);
    const endX = W / 2 - spread / 2 + (n === 1 ? spread / 2 : spread * i / (n - 1));
    return {
      sides: s, label: sides === 100 ? (face % 10) * 10 : null, g, face, kept, faceIdx, target, size, dur, color,
      q: qnorm([Math.random() - .5, Math.random() - .5, Math.random() - .5, Math.random() - .5]),
      spin: [Math.random() * 14 - 7, Math.random() * 14 - 7, Math.random() * 14 - 7],
      sx: Math.random() < .5 ? -size * 2 : W + size * 2, sy: H * (.25 + Math.random() * .3), ex: endX, ey: H * .45, t0: performance.now(), settled: false,
    };
  }

  function faceNumber(d, fi) {
    if (d.label != null && fi === d.faceIdx) return String(d.face);
    return String(fi + 1);
  }

  function drawDie(d, now) {
    const p = Math.min(1, (now - d.t0) / d.dur);
    // position: arc in, bounce
    const e = 1 - Math.pow(1 - p, 3);
    const x = d.sx + (d.ex - d.sx) * e;
    const bounce = Math.abs(Math.sin(p * Math.PI * 3)) * (1 - p) * H * .18;
    const y = d.ey - bounce;
    // orientation: tumble, then settle onto the target face
    let q;
    if (p < .62) {
      const k = (1 - p / .62);
      const ang = len(d.spin) * 0.016 * k;
      d.q = qnorm(qmul(qaxis(d.spin, ang), d.q));
      q = d.q; d.qSettle = q;
    } else {
      const k = (p - .62) / .38;
      q = slerp(d.qSettle || d.q, d.target, 1 - Math.pow(1 - k, 3));
    }
    const sc = d.size * d.g.scale;
    const pv = d.g.v.map(v => { const r = qrot(q, v); const persp = 1 / (1 - r[2] * .12); return [x + r[0] * sc * persp, y - r[1] * sc * persp, r[2]]; });
    // shadow
    ctx.fillStyle = 'rgba(0,0,0,.35)';
    ctx.beginPath(); ctx.ellipse(x, d.ey + d.size * 1.1, d.size * (1 - bounce / H), d.size * .25, 0, 0, 7); ctx.fill();
    const order = d.g.f.map((f, i) => ({ f, i, z: f.reduce((s, vi) => s + pv[vi][2], 0) / f.length, n: qrot(q, d.g.normals[i]) })).filter(o => o.n[2] > -0.05).sort((a, b) => a.z - b.z);
    const [base, ink] = STYLE[d.sides] || STYLE[20];
    const col = d.color || base;
    for (const o of order) {
      const light = Math.max(0, dot(o.n, norm([-.4, .5, .8])));
      ctx.fillStyle = EW.shade(col, Math.round(-40 + light * 70));
      ctx.strokeStyle = EW.shade(col, 50);
      ctx.lineWidth = 1.2 * dpr;
      ctx.beginPath();
      o.f.forEach((vi, k) => k ? ctx.lineTo(pv[vi][0], pv[vi][1]) : ctx.moveTo(pv[vi][0], pv[vi][1]));
      ctx.closePath(); ctx.fill(); ctx.stroke();
      if (o.n[2] > .35) {
        const cx = o.f.reduce((s, vi) => s + pv[vi][0], 0) / o.f.length, cy = o.f.reduce((s, vi) => s + pv[vi][1], 0) / o.f.length;
        const fs = d.size * (d.sides >= 12 ? .42 : .55) * (.5 + .5 * o.n[2]);
        ctx.font = `700 ${fs}px "Alegreya Sans", sans-serif`;
        ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
        ctx.fillStyle = (p >= 1 && o.i === d.faceIdx && d.kept) ? '#fff' : ink;
        const num = faceNumber(d, o.i);
        ctx.fillText(num === '6' || num === '9' ? num + '.' : num, cx, cy);
      }
    }
    if (p >= 1 && !d.kept) { ctx.fillStyle = 'rgba(10,10,20,.55)'; ctx.beginPath(); ctx.arc(x, y, d.size * 1.05, 0, 7); ctx.fill(); }
    if (p >= 1) d.settled = true;
    return p;
  }

  let raf = 0, hideAt = 0;
  function loop(now) {
    ctx.clearRect(0, 0, W, H);
    let done = true;
    for (const d of active) if (drawDie(d, now) < 1) done = false;
    if (done && active.length && !D._shown) { D._shown = true; showResult(); }
    if (active.length && now < hideAt + 1e9) raf = requestAnimationFrame(loop);
  }

  let current = null;
  function showResult() {
    const r = current;
    if (!r) return;
    const nat = r.roll.natural;
    resEl.innerHTML = '';
    resEl.append(
      EW.h('div.total' + (nat === 20 ? '.nat20' : nat === 1 ? '.nat1' : ''), String(r.roll.total)),
      EW.h('div.lab', r.label || ''),
      EW.h('div.detail', (r.player ? r.player + ' · ' : '') + r.roll.expr + (r.roll.mode !== 'normal' ? ' (' + r.roll.mode + ')' : '') + (nat === 20 ? ' — natural 20!' : nat === 1 ? ' — natural 1…' : '')));
    resEl.classList.add('show');
    EW.audio.sfx(nat === 20 ? 'crit' : 'dice');
    setTimeout(() => { resEl.classList.remove('show'); }, r.hold);
    setTimeout(() => { active = []; ctx.clearRect(0, 0, W, H); cancelAnimationFrame(raf); busy = false; next(); }, r.hold + 150);
  }

  function next() {
    if (busy || !queue.length) return;
    busy = true;
    current = queue.shift();
    D._shown = false;
    const r = current.roll;
    const dice = (r.dice || []).slice(0, 12);
    if (!dice.length) dice.push({ sides: 20, face: r.total, kept: true });
    const dur = current.fast ? 750 : 1400;
    active = dice.map((d, i) => makeDie(d.sides, d.face, d.kept, i, dice.length, dur + i * 90, current.color));
    EW.audio.sfx('dice');
    cancelAnimationFrame(raf);
    raf = requestAnimationFrame(loop);
  }

  /** Animate a server roll. opts: {label, player, fast, color} */
  D.show = function (roll, opts = {}) {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || document.hidden) return;
    if (queue.length > 2) queue.splice(0, queue.length - 2);
    queue.push({ roll, label: opts.label, player: opts.player, fast: !!opts.fast, color: opts.color, hold: opts.fast ? 900 : 1700 });
    next();
  };
})();
