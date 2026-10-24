/* Painted, animated scene backdrops. Static layers are painted once per scene into an offscreen canvas;
   flickering lights and weather particles animate on top. */
(function () {
  const S = EW.scene = {};
  const cv = document.getElementById('backdrop');
  const ctx = cv.getContext('2d');
  let W = 0, H = 0, dpr = 1;
  let cur = null, prev = null, fadeT = 1;
  let particles = [], key = '';
  let dim = 0, dimTarget = 0;
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function rng(seed) { let a = seed >>> 0; return () => { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
  function hash(s) { let h = 2166136261; for (const ch of s) h = Math.imul(h ^ ch.charCodeAt(0), 16777619); return h >>> 0; }

  const SKIES = {
    day: ['#6fa7d6', '#bcd8e6', '#e9e1c8'],
    dusk: ['#2a2346', '#7a3f5a', '#e2875a'],
    dawn: ['#3a4a76', '#c48a8a', '#f2c48e'],
    night: ['#0b0e1f', '#1a1f3d', '#33284a'],
  };

  function sky(c, w, h, time, r) {
    const s = SKIES[time] || SKIES.night;
    const g = c.createLinearGradient(0, 0, 0, h * .7);
    g.addColorStop(0, s[0]); g.addColorStop(.6, s[1]); g.addColorStop(1, s[2]);
    c.fillStyle = g; c.fillRect(0, 0, w, h);
    if (time === 'night') {
      for (let i = 0; i < 160; i++) { c.fillStyle = `rgba(255,255,240,${r() * .7})`; c.fillRect(r() * w, r() * h * .55, 1.2, 1.2); }
      c.fillStyle = '#f4ecd0'; c.beginPath(); c.arc(w * .78, h * .16, Math.min(w, h) * .045, 0, 7); c.fill();
      c.fillStyle = SKIES.night[0]; c.beginPath(); c.arc(w * .78 + 12, h * .16 - 6, Math.min(w, h) * .042, 0, 7); c.fill();
    } else if (time === 'day' || time === 'dawn') {
      const sg = c.createRadialGradient(w * .2, h * .2, 4, w * .2, h * .2, h * .35);
      sg.addColorStop(0, 'rgba(255,248,220,.9)'); sg.addColorStop(1, 'rgba(255,248,220,0)');
      c.fillStyle = sg; c.fillRect(0, 0, w, h);
    }
  }

  function ridge(c, w, h, y, amp, color, r, rough = 6) {
    c.fillStyle = color; c.beginPath(); c.moveTo(0, h);
    const pts = 40; let v = 0;
    for (let i = 0; i <= pts; i++) { v += (r() - .5) * rough; v *= .9; const x = i / pts * w; c.lineTo(x, y + Math.sin(i * .35 + r()) * amp * .4 + v * amp * .12); }
    c.lineTo(w, h); c.fill();
  }

  function pine(c, x, base, hgt, color) {
    c.fillStyle = color; c.beginPath(); c.moveTo(x, base - hgt);
    for (let i = 1; i <= 4; i++) { const yy = base - hgt + hgt * i / 4.4; const ww = hgt * .12 * i; c.lineTo(x + ww, yy); c.lineTo(x + ww * .45, yy); }
    c.lineTo(x + hgt * .05, base); c.lineTo(x - hgt * .05, base);
    for (let i = 4; i >= 1; i--) { const yy = base - hgt + hgt * i / 4.4; const ww = hgt * .12 * i; c.lineTo(x - ww * .45, yy); c.lineTo(x - ww, yy); }
    c.closePath(); c.fill();
  }

  function oak(c, x, base, hgt, color, r) {
    c.fillStyle = color;
    c.fillRect(x - hgt * .04, base - hgt * .5, hgt * .08, hgt * .5);
    for (let i = 0; i < 7; i++) { c.beginPath(); c.arc(x + (r() - .5) * hgt * .5, base - hgt * (.55 + r() * .35), hgt * (.16 + r() * .12), 0, 7); c.fill(); }
  }

  function forestBand(c, w, h, base, n, hgt, color, r, kind) {
    for (let i = 0; i < n; i++) {
      const x = r() * w, hh = hgt * (.7 + r() * .6);
      if (kind === 'oak' || (kind === 'mix' && r() < .4)) oak(c, x, base + r() * 10, hh, color, r); else pine(c, x, base + r() * 10, hh, color);
    }
  }

  function vignette(c, w, h, strength = .65) {
    const g = c.createRadialGradient(w / 2, h * .45, Math.min(w, h) * .25, w / 2, h / 2, Math.max(w, h) * .75);
    g.addColorStop(0, 'rgba(0,0,0,0)'); g.addColorStop(1, `rgba(5,5,12,${strength})`);
    c.fillStyle = g; c.fillRect(0, 0, w, h);
  }

  function stoneWall(c, w, h, top, bot, base, r) {
    c.fillStyle = base; c.fillRect(0, top, w, bot - top);
    for (let y = top; y < bot; y += 34) for (let x = ((y / 34) % 2) * -40; x < w; x += 80) {
      c.fillStyle = EW.shade(base, Math.round((r() - .5) * 22));
      c.fillRect(x + 2, y + 2, 76, 30);
    }
  }

  // ------------------------------------------------------------------- painters: return animated lights
  const P = {};

  P.tavern = (c, w, h, r) => {
    const g = c.createLinearGradient(0, 0, 0, h); g.addColorStop(0, '#2a170d'); g.addColorStop(.6, '#4a2914'); g.addColorStop(1, '#1a0e07');
    c.fillStyle = g; c.fillRect(0, 0, w, h);
    // plaster panels between timber
    for (let x = 0; x < w; x += w / 6) { c.fillStyle = '#5a3720'; c.fillRect(x + 18, h * .14, w / 6 - 36, h * .42); }
    c.fillStyle = '#1e1009';
    for (let x = 0; x <= w; x += w / 6) c.fillRect(x - 9, 0, 18, h * .72);
    c.fillRect(0, h * .1, w, 22); c.fillRect(0, h * .56, w, 18);
    // windows with rain-dark glass
    for (const wx of [w * .1, w * .27]) { c.fillStyle = '#121a2c'; c.fillRect(wx, h * .2, w * .09, h * .22); c.strokeStyle = '#1e1009'; c.lineWidth = 6; c.strokeRect(wx, h * .2, w * .09, h * .22); c.beginPath(); c.moveTo(wx + w * .045, h * .2); c.lineTo(wx + w * .045, h * .42); c.moveTo(wx, h * .31); c.lineTo(wx + w * .09, h * .31); c.stroke(); }
    // hearth
    const hx = w * .78, hy = h * .62;
    c.fillStyle = '#3b3530'; c.fillRect(hx - w * .12, h * .2, w * .24, h * .5);
    for (let y = h * .2; y < h * .7; y += 22) for (let x = hx - w * .12; x < hx + w * .12; x += 40) { c.fillStyle = EW.shade('#4a443d', Math.round((r() - .5) * 30)); c.fillRect(x + ((y / 22) % 2) * 20, y, 37, 19); }
    c.fillStyle = '#0d0806'; c.beginPath(); c.moveTo(hx - w * .08, h * .7); c.lineTo(hx - w * .08, h * .48); c.quadraticCurveTo(hx, h * .38, hx + w * .08, h * .48); c.lineTo(hx + w * .08, h * .7); c.fill();
    // antlers over hearth
    c.strokeStyle = '#d9c39a'; c.lineWidth = 5; c.lineCap = 'round';
    for (const s of [-1, 1]) { c.beginPath(); c.moveTo(hx, h * .24); c.quadraticCurveTo(hx + s * 60, h * .2, hx + s * 90, h * .1); c.moveTo(hx + s * 40, h * .21); c.lineTo(hx + s * 48, h * .12); c.moveTo(hx + s * 70, h * .16); c.lineTo(hx + s * 86, h * .2); c.stroke(); }
    // floor
    const fg = c.createLinearGradient(0, h * .7, 0, h); fg.addColorStop(0, '#3a2112'); fg.addColorStop(1, '#170c06');
    c.fillStyle = fg; c.fillRect(0, h * .7, w, h * .3);
    c.strokeStyle = '#1e1009'; c.lineWidth = 2;
    for (let i = -10; i < 20; i++) { c.beginPath(); c.moveTo(w / 2 + i * 40, h * .7); c.lineTo(w / 2 + i * 160, h); c.stroke(); }
    // table silhouettes
    c.fillStyle = '#120a05';
    for (const [tx, ty, tw] of [[w * .18, h * .82, w * .2], [w * .5, h * .78, w * .16], [w * .05, h * .95, w * .25]]) { c.fillRect(tx, ty, tw, 12); c.fillRect(tx + 10, ty, 10, h * .08); c.fillRect(tx + tw - 20, ty, 10, h * .08); c.beginPath(); c.ellipse(tx + tw * .3, ty - 8, 8, 10, 0, 0, 7); c.ellipse(tx + tw * .6, ty - 6, 6, 8, 0, 0, 7); c.fill(); }
    // chandelier
    c.strokeStyle = '#0d0705'; c.lineWidth = 3; c.beginPath(); c.moveTo(w * .42, 0); c.lineTo(w * .42, h * .12); c.stroke();
    c.beginPath(); c.ellipse(w * .42, h * .14, w * .08, 12, 0, 0, 7); c.stroke();
    vignette(c, w, h, .55);
    const lights = [{ x: hx, y: h * .62, r: Math.min(w, h) * .45, col: [255, 140, 50], a: .55, f: 1, fire: true }];
    for (let i = 0; i < 6; i++) lights.push({ x: w * .42 + Math.cos(i) * w * .075, y: h * .14 + Math.sin(i) * 9, r: 60, col: [255, 200, 120], a: .5, f: .6, candle: true });
    return { lights, particles: 'dust', tint: [255, 160, 80] };
  };

  P.town = (c, w, h, r, time) => {
    sky(c, w, h, time, r);
    ridge(c, w, h, h * .5, 40, '#5d7a6a', r);
    for (let i = 0; i < 9; i++) {
      const x = i * w / 8 - 30 + r() * 30, bw = w / 9 + r() * 40, bh = h * (.18 + r() * .12), base = h * .74;
      c.fillStyle = ['#c9b38e', '#b49b78', '#d6c4a0'][i % 3]; c.fillRect(x, base - bh, bw, bh);
      c.strokeStyle = '#5a3a24'; c.lineWidth = 4; c.strokeRect(x, base - bh, bw, bh); c.beginPath(); c.moveTo(x, base - bh * .5); c.lineTo(x + bw, base - bh * .5); c.stroke();
      c.fillStyle = ['#7a3226', '#5a3a5a', '#3d5a6a'][i % 3]; c.beginPath(); c.moveTo(x - 10, base - bh); c.lineTo(x + bw / 2, base - bh - bh * .55); c.lineTo(x + bw + 10, base - bh); c.fill();
      c.fillStyle = '#2b2f3f'; c.fillRect(x + bw * .2, base - bh * .8, bw * .18, bh * .2); c.fillRect(x + bw * .6, base - bh * .8, bw * .18, bh * .2);
    }
    c.fillStyle = '#8c7b62'; c.fillRect(0, h * .74, w, h * .26);
    for (let i = 0; i < 5; i++) { const x = w * (.1 + i * .2), col = ['#c0392b', '#e6c068', '#2e86c1', '#27ae60', '#8e44ad'][i]; c.fillStyle = '#4a3020'; c.fillRect(x, h * .78, 6, h * .14); c.fillRect(x + w * .12, h * .78, 6, h * .14); for (let s = 0; s < 6; s++) { c.fillStyle = s % 2 ? col : '#f1e6cf'; c.beginPath(); c.moveTo(x + s * w * .02, h * .76); c.lineTo(x + (s + 1) * w * .02 + 6, h * .76); c.lineTo(x + (s + .5) * w * .02 + 3, h * .81); c.fill(); } c.fillStyle = '#6b4a2c'; c.fillRect(x, h * .86, w * .126, 10); }
    vignette(c, w, h, .45);
    return { lights: [], particles: 'motes' };
  };

  P.road = (c, w, h, r, time) => {
    sky(c, w, h, time, r);
    ridge(c, w, h, h * .48, 60, '#3a3a5a', r);
    ridge(c, w, h, h * .56, 40, '#2f3a3a', r);
    forestBand(c, w, h, h * .6, 40, h * .14, '#1c2622', r, 'mix');
    c.fillStyle = '#3e4a2e'; c.fillRect(0, h * .62, w, h * .38);
    c.fillStyle = '#6b5a44'; c.beginPath(); c.moveTo(w * .47, h * .62); c.lineTo(w * .53, h * .62); c.lineTo(w * .85, h); c.lineTo(w * .15, h); c.fill();
    forestBand(c, w, h, h, 6, h * .5, '#0f1512', r, 'oak');
    // town lights on the horizon
    const lights = [];
    for (let i = 0; i < 6; i++) lights.push({ x: w * (.45 + r() * .1), y: h * .6 + r() * 6, r: 14, col: [255, 190, 110], a: .7, f: .4 });
    // glow of the Thornwood
    lights.push({ x: w * .7, y: h * .5, r: w * .35, col: [255, 90, 40], a: .25, f: .2 });
    vignette(c, w, h);
    return { lights, particles: 'none' };
  };

  P.forest = (c, w, h, r, time) => {
    sky(c, w, h, time, r);
    const shades = time === 'day' ? ['#4f6f5a', '#3a5a46', '#284434', '#14261c'] : ['#22304a', '#1a2638', '#121b28', '#0a0f17'];
    forestBand(c, w, h, h * .55, 50, h * .2, shades[0], r, 'pine');
    forestBand(c, w, h, h * .66, 35, h * .3, shades[1], r, 'mix');
    c.fillStyle = shades[2]; c.fillRect(0, h * .72, w, h * .28);
    forestBand(c, w, h, h * .8, 18, h * .45, shades[2], r, 'oak');
    forestBand(c, w, h, h * 1.02, 6, h * .9, shades[3], r, 'oak');
    // god rays
    if (time === 'day' || time === 'dawn') for (let i = 0; i < 4; i++) { const x = w * (.2 + r() * .6); c.fillStyle = 'rgba(255,240,190,.07)'; c.beginPath(); c.moveTo(x, 0); c.lineTo(x + 60, 0); c.lineTo(x + 260, h); c.lineTo(x + 140, h); c.fill(); }
    vignette(c, w, h);
    return { lights: [], particles: time === 'night' || time === 'dusk' ? 'fireflies' : 'motes' };
  };

  P.bridge = (c, w, h, r, time) => {
    sky(c, w, h, time, r);
    ridge(c, w, h, h * .45, 50, '#567a6a', r);
    forestBand(c, w, h, h * .55, 40, h * .16, '#2f4a3a', r, 'mix');
    const rg = c.createLinearGradient(0, h * .6, 0, h); rg.addColorStop(0, '#5a8aa0'); rg.addColorStop(1, '#22404e');
    c.fillStyle = '#3e5a36'; c.fillRect(0, h * .56, w, h * .44);
    c.fillStyle = rg; c.beginPath(); c.moveTo(0, h * .66); c.quadraticCurveTo(w * .5, h * .6, w, h * .7); c.lineTo(w, h * .9); c.quadraticCurveTo(w * .5, h * .82, 0, h * .92); c.fill();
    // bridge arch
    c.fillStyle = '#7d7468'; c.beginPath(); c.moveTo(w * .15, h * .66); c.quadraticCurveTo(w * .5, h * .5, w * .85, h * .68); c.lineTo(w * .85, h * .74); c.quadraticCurveTo(w * .5, h * .58, w * .15, h * .72); c.fill();
    c.fillStyle = '#22404e'; c.beginPath(); c.ellipse(w * .5, h * .75, w * .14, h * .1, 0, Math.PI, 0); c.fill();
    // barricade junk
    c.fillStyle = '#4a3020'; for (let i = 0; i < 6; i++) c.fillRect(w * (.55 + i * .03), h * (.56 - r() * .05), 30, 26);
    forestBand(c, w, h, h * 1.05, 5, h * .8, '#14201a', r, 'oak');
    vignette(c, w, h, .5);
    return { lights: [], particles: 'motes', water: { y: h * .76 } };
  };

  P.swamp = (c, w, h, r, time) => {
    sky(c, w, h, time === 'day' ? 'dusk' : time, r);
    c.fillStyle = '#26302a'; c.fillRect(0, h * .55, w, h * .45);
    ridge(c, w, h, h * .55, 20, '#1f2a24', r);
    const wg = c.createLinearGradient(0, h * .62, 0, h); wg.addColorStop(0, '#2c3a3a'); wg.addColorStop(1, '#0c1414');
    c.fillStyle = wg; c.fillRect(0, h * .62, w, h * .38);
    c.strokeStyle = '#111a16';
    for (let i = 0; i < 14; i++) {
      const x = r() * w, base = h * (.6 + r() * .3), hh = h * (.2 + r() * .3);
      c.lineWidth = 6 + r() * 8; c.beginPath(); c.moveTo(x, base); c.lineTo(x + (r() - .5) * 30, base - hh); c.stroke();
      c.lineWidth = 3; for (let b = 0; b < 3; b++) { const by = base - hh * (.4 + r() * .5); c.beginPath(); c.moveTo(x, by); c.lineTo(x + (r() - .5) * 120, by - r() * 50); c.stroke(); }
    }
    for (let i = 0; i < 40; i++) { c.strokeStyle = '#2e4434'; c.lineWidth = 2; const x = r() * w, y = h * (.62 + r() * .35); c.beginPath(); c.moveTo(x, y); c.lineTo(x + (r() - .5) * 8, y - 30 - r() * 30); c.stroke(); }
    vignette(c, w, h, .7);
    const lights = [];
    for (let i = 0; i < 5; i++) lights.push({ x: w * (.2 + r() * .7), y: h * (.4 + r() * .25), r: 40, col: [160, 220, 255], a: .55, f: .5, wisp: true, phase: r() * 10 });
    return { lights, particles: 'fog', water: { y: h * .7 } };
  };

  P.grove = (c, w, h, r, time) => {
    sky(c, w, h, time, r);
    forestBand(c, w, h, h * .6, 40, h * .25, '#2a4436', r, 'oak');
    c.fillStyle = '#2b4a2c'; c.fillRect(0, h * .7, w, h * .3);
    // the great oak
    const x = w * .5, base = h * .82;
    c.fillStyle = '#3a2a1c';
    c.beginPath(); c.moveTo(x - w * .07, base); c.quadraticCurveTo(x - w * .03, h * .5, x - w * .05, h * .32); c.lineTo(x + w * .05, h * .32); c.quadraticCurveTo(x + w * .03, h * .5, x + w * .07, base); c.fill();
    for (let i = 0; i < 18; i++) { c.fillStyle = ['#3f6b3a', '#5a8a45', '#2e5530'][i % 3]; c.beginPath(); c.arc(x + (r() - .5) * w * .5, h * (.12 + r() * .25), w * (.05 + r() * .06), 0, 7); c.fill(); }
    // glowing wound
    c.fillStyle = '#ffb04a'; c.beginPath(); c.ellipse(x, h * .6, 8, 26, 0, 0, 7); c.fill();
    c.strokeStyle = '#ff8a3a'; c.lineWidth = 2;
    for (let i = 0; i < 6; i++) { c.beginPath(); c.moveTo(x, base); c.quadraticCurveTo(x + (r() - .5) * w * .3, base + 10, x + (r() - .5) * w * .6, h); c.stroke(); }
    vignette(c, w, h, .55);
    return { lights: [{ x, y: h * .6, r: w * .25, col: [255, 140, 60], a: .5, f: .8, fire: true }], particles: 'embers' };
  };

  P.camp = (c, w, h, r, time) => {
    sky(c, w, h, time, r);
    forestBand(c, w, h, h * .62, 50, h * .25, '#121a24', r, 'pine');
    c.fillStyle = '#141c18'; c.fillRect(0, h * .66, w, h * .34);
    // standing stones
    for (const [sx, sh] of [[.12, .3], [.27, .22], [.73, .24], [.88, .32]]) { c.fillStyle = '#2a2e38'; c.beginPath(); c.moveTo(w * sx - 30, h * .9); c.lineTo(w * sx - 22, h * (.9 - sh)); c.lineTo(w * sx + 20, h * (.92 - sh)); c.lineTo(w * sx + 30, h * .9); c.fill(); }
    // fire
    c.fillStyle = '#2a1a10'; for (let i = 0; i < 5; i++) { c.save(); c.translate(w * .5, h * .86); c.rotate(i * 1.25); c.fillRect(-40, -5, 80, 10); c.restore(); }
    vignette(c, w, h, .6);
    return { lights: [{ x: w * .5, y: h * .82, r: Math.min(w, h) * .55, col: [255, 140, 50], a: .6, f: 1, fire: true, flame: true }], particles: 'embers' };
  };

  P.village = (c, w, h, r, time) => {
    sky(c, w, h, 'dusk', r);
    c.fillStyle = 'rgba(40,30,30,.5)'; c.fillRect(0, 0, w, h);
    ridge(c, w, h, h * .55, 30, '#2a2626', r);
    for (let i = 0; i < 7; i++) {
      const x = r() * w, bw = 80 + r() * 90, bh = 60 + r() * 60, base = h * .78;
      c.fillStyle = '#1a1414'; c.fillRect(x, base - bh, bw, bh);
      c.beginPath(); c.moveTo(x - 6, base - bh); c.lineTo(x + bw * .3, base - bh - 30); c.lineTo(x + bw * .45, base - bh - 10); c.lineTo(x + bw * .6, base - bh - 40); c.lineTo(x + bw + 6, base - bh); c.fill();
      c.fillStyle = 'rgba(30,25,25,.5)'; for (let s = 0; s < 6; s++) { c.beginPath(); c.arc(x + bw / 2 + Math.sin(s) * 20 + s * 10, base - bh - 60 - s * 50, 30 + s * 12, 0, 7); c.fill(); }
    }
    c.fillStyle = '#2a2420'; c.fillRect(0, h * .78, w, h * .22);
    vignette(c, w, h, .7);
    const lights = []; for (let i = 0; i < 5; i++) lights.push({ x: r() * w, y: h * (.72 + r() * .08), r: 30, col: [255, 110, 40], a: .4, f: .9, fire: true });
    return { lights, particles: 'ash' };
  };

  P.chapel = (c, w, h, r) => {
    const g = c.createLinearGradient(0, 0, 0, h); g.addColorStop(0, '#15161c'); g.addColorStop(1, '#2a2a30'); c.fillStyle = g; c.fillRect(0, 0, w, h);
    for (let i = 0; i < 5; i++) {
      const x = w * (i / 4); c.fillStyle = '#383842'; c.fillRect(x - 24, 0, 48, h);
      c.strokeStyle = '#383842'; c.lineWidth = 30; c.beginPath(); c.arc(x + w / 8, h * .45, w / 8, Math.PI, 0); c.stroke();
    }
    // window (gothic) at back
    c.fillStyle = '#0a0b10'; c.beginPath(); c.moveTo(w * .45, h * .55); c.lineTo(w * .45, h * .25); c.quadraticCurveTo(w * .5, h * .1, w * .55, h * .25); c.lineTo(w * .55, h * .55); c.fill();
    c.fillStyle = '#26262c'; c.fillRect(0, h * .72, w, h * .28);
    for (let row = 0; row < 3; row++) for (const side of [-1, 1]) { c.fillStyle = '#1a120c'; c.fillRect(w * .5 + side * w * .12 - (side < 0 ? w * .25 : 0), h * (.76 + row * .07), w * .25, 14); }
    vignette(c, w, h, .7);
    return { lights: [{ x: w * .5, y: h * .68, r: w * .3, col: [90, 255, 140], a: .35, f: .9, fire: true }], particles: 'ash' };
  };

  P.ruins = (c, w, h, r) => {
    sky(c, w, h, 'night', r);
    const glow = c.createRadialGradient(w * .5, h, 10, w * .5, h, h * .8); glow.addColorStop(0, 'rgba(255,90,30,.55)'); glow.addColorStop(1, 'rgba(255,90,30,0)');
    c.fillStyle = glow; c.fillRect(0, 0, w, h);
    ridge(c, w, h, h * .62, 80, '#141218', r, 10);
    // broken towers
    for (const [tx, th] of [[.25, .42], [.42, .55], [.62, .38], [.78, .5]]) {
      c.fillStyle = '#1d1a22'; const x = w * tx, top = h * (.62 - th);
      c.beginPath(); c.moveTo(x - 30, h * .66); c.lineTo(x - 26, top + 20); c.lineTo(x - 10, top); c.lineTo(x + 2, top + 30); c.lineTo(x + 14, top + 8); c.lineTo(x + 28, top + 40); c.lineTo(x + 30, h * .66); c.fill();
      c.fillStyle = '#ff7a3a'; c.fillRect(x - 6, top + 60, 8, 14);
    }
    c.fillStyle = '#0d0b10'; c.fillRect(0, h * .78, w, h * .22);
    vignette(c, w, h, .6);
    return { lights: [{ x: w * .5, y: h * 1.05, r: w * .6, col: [255, 80, 30], a: .35, f: .5 }], particles: 'embers' };
  };

  function interior(c, w, h, r, base, light) {
    c.fillStyle = base; c.fillRect(0, 0, w, h);
    stoneWall(c, w, h, 0, h * .7, base, r);
    for (let i = 0; i < 4; i++) { const x = w * (.12 + i * .25); c.fillStyle = EW.shade(base, 18); c.fillRect(x - 28, 0, 56, h * .76); c.fillStyle = EW.shade(base, -15); c.fillRect(x + 20, 0, 8, h * .76); }
    c.fillStyle = EW.shade(base, -25); c.fillRect(0, h * .7, w, h * .3);
    vignette(c, w, h, .75);
    const lights = [];
    for (let i = 0; i < 4; i++) { const x = w * (.12 + i * .25); lights.push({ x: x, y: h * .38, r: 120, col: light, a: .5, f: .8, fire: light[0] > 200 }); c.fillStyle = '#20140c'; c.fillRect(x - 5, h * .4, 10, 22); }
    return lights;
  }
  P.cave = (c, w, h, r) => ({ lights: interior(c, w, h, r, '#2a2722', [255, 160, 80]), particles: 'dust' });
  P.dungeon = (c, w, h, r) => ({ lights: interior(c, w, h, r, '#25232a', [255, 150, 70]), particles: 'dust' });
  P.hall = (c, w, h, r) => ({ lights: interior(c, w, h, r, '#2a3340', [140, 200, 255]), particles: 'snow' });

  P.lair = (c, w, h, r) => {
    c.fillStyle = '#0b0a0e'; c.fillRect(0, 0, w, h);
    // black glass shards
    for (let i = 0; i < 26; i++) { c.fillStyle = EW.shade('#17141c', Math.round(r() * 20)); const x = r() * w, y = r() * h * .6; c.beginPath(); c.moveTo(x, y); c.lineTo(x + 40 + r() * 80, y + 120 + r() * 160); c.lineTo(x - 40 - r() * 60, y + 140 + r() * 120); c.fill(); }
    // lava lake
    const lg = c.createLinearGradient(0, h * .62, 0, h); lg.addColorStop(0, '#ff9a3a'); lg.addColorStop(.4, '#e0461e'); lg.addColorStop(1, '#5a1208');
    c.fillStyle = lg; c.beginPath(); c.moveTo(0, h * .68); c.quadraticCurveTo(w * .5, h * .6, w, h * .7); c.lineTo(w, h); c.lineTo(0, h); c.fill();
    // the Tyrant's silhouette
    c.fillStyle = '#100c10';
    c.beginPath(); c.moveTo(w * .36, h * .68); c.quadraticCurveTo(w * .42, h * .36, w * .5, h * .3); c.quadraticCurveTo(w * .58, h * .36, w * .64, h * .68); c.fill();
    c.beginPath(); c.moveTo(w * .46, h * .32); c.lineTo(w * .43, h * .2); c.lineTo(w * .48, h * .3); c.moveTo(w * .54, h * .32); c.lineTo(w * .57, h * .2); c.lineTo(w * .52, h * .3); c.fill();
    for (const s of [-1, 1]) { c.beginPath(); c.moveTo(w * (.5 + s * .08), h * .45); c.quadraticCurveTo(w * (.5 + s * .3), h * .15, w * (.5 + s * .42), h * .3); c.quadraticCurveTo(w * (.5 + s * .3), h * .35, w * (.5 + s * .12), h * .55); c.fill(); }
    // braziers
    for (const bx of [.2, .8, .5]) { c.fillStyle = '#1c1414'; c.fillRect(w * bx - 14, h * (bx === .5 ? .86 : .7), 28, 40); }
    vignette(c, w, h, .5);
    return {
      lights: [{ x: w * .5, y: h * .9, r: w * .7, col: [255, 80, 20], a: .45, f: .6 }, { x: w * .47, y: h * .4, r: 16, col: [255, 220, 120], a: .9, f: .3, eye: true }, { x: w * .53, y: h * .4, r: 16, col: [255, 220, 120], a: .9, f: .3, eye: true },
        { x: w * .2, y: h * .68, r: 90, col: [255, 120, 40], a: .6, f: 1, fire: true }, { x: w * .8, y: h * .68, r: 90, col: [255, 120, 40], a: .6, f: 1, fire: true }],
      particles: 'embers'
    };
  };

  // ------------------------------------------------------------------- engine
  function paint(backdrop, time, w, h, seed) {
    const off = document.createElement('canvas');
    off.width = w; off.height = h;
    const c = off.getContext('2d');
    const r = rng(seed);
    const painter = P[backdrop] || P.road;
    const info = painter(c, w, h, r, time || 'day') || {};
    if (time === 'night' && !['tavern', 'chapel', 'lair', 'cave', 'dungeon', 'hall'].includes(backdrop)) { c.fillStyle = 'rgba(10,14,40,.35)'; c.fillRect(0, 0, w, h); }
    return { canvas: off, info };
  }

  function resize() {
    dpr = Math.min(1.5, window.devicePixelRatio || 1);
    W = Math.floor(window.innerWidth * dpr); H = Math.floor(window.innerHeight * dpr);
    cv.width = W; cv.height = H;
    if (cur) { const k = cur.args; cur = make(...k); prev = null; }
  }

  function make(backdrop, time, weather) {
    const p = paint(backdrop, time, W, H, hash(backdrop + time));
    return { ...p, args: [backdrop, time, weather], weather };
  }

  S.show = function (backdrop, time, weather) {
    const k = backdrop + '|' + time + '|' + weather;
    if (k === key) return;
    key = k;
    prev = cur;
    cur = make(backdrop || 'road', time || 'day', weather || 'none');
    fadeT = prev ? 0 : 1;
    spawnParticles();
  };

  S.dim = function (v) { dimTarget = v; };

  function spawnParticles() {
    particles = [];
    if (!cur) return;
    const kinds = [];
    const pk = cur.info.particles;
    if (pk && pk !== 'none') kinds.push(pk);
    const wt = cur.weather;
    if (wt === 'rain' || wt === 'emberrain') kinds.push('rain');
    if (wt === 'emberrain' || wt === 'embers') kinds.push('embers');
    if (wt === 'fog') kinds.push('fog');
    if (wt === 'ash') kinds.push('ash');
    for (const kind of kinds) {
      const n = { rain: 220, embers: 70, fireflies: 40, dust: 50, motes: 40, fog: 14, ash: 80, snow: 70 }[kind] || 30;
      for (let i = 0; i < n; i++) particles.push(newParticle(kind, true));
    }
  }

  function newParticle(kind, anywhere) {
    const p = { kind, x: Math.random() * W, y: anywhere ? Math.random() * H : 0, life: Math.random() };
    switch (kind) {
      case 'rain': p.vy = 14 * dpr + Math.random() * 6; p.vx = -2 * dpr; p.len = 14 * dpr; if (!anywhere) p.y = -20; break;
      case 'embers': p.vy = -(0.4 + Math.random() * 1.2) * dpr; p.vx = (Math.random() - .5) * .6; p.size = (1 + Math.random() * 2.2) * dpr; if (!anywhere) p.y = H + 5; break;
      case 'ash': p.vy = (0.3 + Math.random() * .6) * dpr; p.vx = (Math.random() - .3) * .5; p.size = (1 + Math.random() * 2) * dpr; if (!anywhere) p.y = -5; break;
      case 'snow': p.vy = (0.3 + Math.random() * .5) * dpr; p.vx = (Math.random() - .5) * .3; p.size = (1 + Math.random() * 1.6) * dpr; if (!anywhere) p.y = -5; break;
      case 'fireflies': p.y = H * (.4 + Math.random() * .55); p.phase = Math.random() * 10; break;
      case 'dust': case 'motes': p.vy = (Math.random() - .5) * .15; p.vx = (Math.random() - .5) * .2; p.size = (0.8 + Math.random() * 1.4) * dpr; break;
      case 'fog': p.y = H * (.45 + Math.random() * .5); p.vx = (0.1 + Math.random() * .3) * dpr; p.size = (150 + Math.random() * 220) * dpr; break;
    }
    return p;
  }

  function drawLights(info, t, alpha) {
    if (!info.lights) return;
    ctx.globalCompositeOperation = 'lighter';
    for (const L of info.lights) {
      let f = 1;
      if (L.fire) f = .78 + .22 * Math.sin(t * 7 + L.x) * Math.sin(t * 13.7 + L.y) + Math.random() * .08 * L.f;
      else if (L.candle) f = .85 + .15 * Math.sin(t * 9 + L.x);
      else if (L.wisp) f = .6 + .4 * Math.sin(t * 2 + L.phase);
      else f = .9 + .1 * Math.sin(t * 1.5 + L.x);
      let x = L.x, y = L.y;
      if (L.wisp) { x += Math.sin(t * .5 + L.phase) * 40 * dpr; y += Math.cos(t * .7 + L.phase) * 20 * dpr; }
      const rad = L.r * (L.fire ? (0.95 + .05 * f) : 1);
      const g = ctx.createRadialGradient(x, y, 1, x, y, rad);
      g.addColorStop(0, `rgba(${L.col[0]},${L.col[1]},${L.col[2]},${L.a * f * alpha})`);
      g.addColorStop(1, `rgba(${L.col[0]},${L.col[1]},${L.col[2]},0)`);
      ctx.fillStyle = g; ctx.fillRect(x - rad, y - rad, rad * 2, rad * 2);
      if (L.flame) drawFlame(x, y, t, alpha);
    }
    ctx.globalCompositeOperation = 'source-over';
  }

  function drawFlame(x, y, t, alpha) {
    for (let i = 0; i < 5; i++) {
      const hh = (50 + 30 * Math.sin(t * 8 + i * 2)) * dpr, ww = (14 + i * 3) * dpr;
      const ox = Math.sin(t * 5 + i) * 6 * dpr + (i - 2) * 8 * dpr;
      const g = ctx.createLinearGradient(x, y, x, y - hh);
      g.addColorStop(0, `rgba(255,200,90,${.5 * alpha})`); g.addColorStop(1, 'rgba(255,60,20,0)');
      ctx.fillStyle = g; ctx.beginPath(); ctx.moveTo(x + ox - ww, y); ctx.quadraticCurveTo(x + ox, y - hh * 1.2, x + ox + ww, y); ctx.fill();
    }
  }

  function drawParticles(t) {
    for (let i = 0; i < particles.length; i++) {
      const p = particles[i];
      switch (p.kind) {
        case 'rain':
          ctx.strokeStyle = cur.weather === 'emberrain' ? 'rgba(180,190,220,.35)' : 'rgba(180,190,220,.4)';
          ctx.lineWidth = dpr; ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(p.x + p.vx * 2, p.y + p.len); ctx.stroke();
          p.x += p.vx; p.y += p.vy; if (p.y > H) particles[i] = newParticle('rain', false);
          break;
        case 'embers': {
          const a = .5 + .5 * Math.sin(t * 3 + p.x);
          ctx.fillStyle = `rgba(255,${140 + Math.floor(a * 80)},60,${.55 + a * .4})`;
          ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, 7); ctx.fill();
          p.x += p.vx + Math.sin(t + p.y * .01) * .4; p.y += p.vy; if (p.y < -10) particles[i] = newParticle('embers', false);
          break;
        }
        case 'ash': case 'snow':
          ctx.fillStyle = p.kind === 'ash' ? 'rgba(170,160,150,.55)' : 'rgba(220,240,255,.7)';
          ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, 7); ctx.fill();
          p.x += p.vx + Math.sin(t + p.y * .02) * .3; p.y += p.vy; if (p.y > H + 5) particles[i] = newParticle(p.kind, false);
          break;
        case 'fireflies': {
          const a = Math.max(0, Math.sin(t * 1.3 + p.phase));
          ctx.fillStyle = `rgba(220,255,140,${a * .9})`;
          ctx.beginPath(); ctx.arc(p.x + Math.sin(t * .4 + p.phase) * 30 * dpr, p.y + Math.cos(t * .3 + p.phase) * 18 * dpr, 2 * dpr, 0, 7); ctx.fill();
          break;
        }
        case 'dust': case 'motes':
          ctx.fillStyle = p.kind === 'dust' ? 'rgba(255,210,150,.35)' : 'rgba(255,250,220,.4)';
          ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, 7); ctx.fill();
          p.x += p.vx + Math.sin(t * .5 + p.y) * .1; p.y += p.vy; if (p.x < 0) p.x = W; if (p.x > W) p.x = 0; if (p.y < 0) p.y = H; if (p.y > H) p.y = 0;
          break;
        case 'fog': {
          const g = ctx.createRadialGradient(p.x, p.y, 1, p.x, p.y, p.size);
          g.addColorStop(0, 'rgba(190,200,200,.10)'); g.addColorStop(1, 'rgba(190,200,200,0)');
          ctx.fillStyle = g; ctx.fillRect(p.x - p.size, p.y - p.size, p.size * 2, p.size * 2);
          p.x += p.vx; if (p.x - p.size > W) p.x = -p.size;
          break;
        }
      }
    }
  }

  function water(info, t) {
    if (!info.water) return;
    ctx.strokeStyle = 'rgba(200,230,255,.12)'; ctx.lineWidth = dpr;
    for (let i = 0; i < 18; i++) {
      const y = info.water.y + (i * 9 % 80) * dpr, x = ((i * 137 + t * 20) % (W + 200)) - 100;
      ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x + 50 * dpr, y); ctx.stroke();
    }
  }

  let last = 0;
  function frame(ts) {
    requestAnimationFrame(frame);
    if (!cur) return;
    if (reduced && ts - last < 500) return;
    last = ts;
    const t = ts / 1000;
    if (fadeT < 1) fadeT = Math.min(1, fadeT + 0.025);
    dim += (dimTarget - dim) * 0.08;
    ctx.globalAlpha = 1;
    if (prev && fadeT < 1) { ctx.drawImage(prev.canvas, 0, 0); ctx.globalAlpha = fadeT; }
    ctx.drawImage(cur.canvas, 0, 0);
    ctx.globalAlpha = 1;
    drawLights(cur.info, t, fadeT);
    water(cur.info, t);
    if (!reduced) drawParticles(t);
    if (dim > 0.01) { ctx.fillStyle = `rgba(8,9,16,${dim})`; ctx.fillRect(0, 0, W, H); }
  }

  /** Small cover painting for campaign cards. */
  S.paintCover = function (canvas, backdrop) {
    const w = 240, h = 300;
    const p = paint(backdrop || 'road', backdrop === 'tavern' ? 'night' : 'dusk', w, h, hash(backdrop + 'cover'));
    canvas.width = w; canvas.height = h;
    const c = canvas.getContext('2d');
    c.drawImage(p.canvas, 0, 0);
    if (p.info.lights) { c.globalCompositeOperation = 'lighter'; for (const L of p.info.lights) { const g = c.createRadialGradient(L.x, L.y, 1, L.x, L.y, L.r); g.addColorStop(0, `rgba(${L.col},${L.a})`); g.addColorStop(1, `rgba(${L.col},0)`); c.fillStyle = g; c.fillRect(L.x - L.r, L.y - L.r, L.r * 2, L.r * 2); } c.globalCompositeOperation = 'source-over'; }
  };

  window.addEventListener('resize', () => { clearTimeout(S._rt); S._rt = setTimeout(resize, 150); });
  resize();
  S.show('tavern', 'night', 'emberrain');
  requestAnimationFrame(frame);
})();
