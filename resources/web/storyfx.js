/* Story animations drawn over everything: coins that fly to your purse, loot that rises glowing, fire, frost,
   lightning and arcane bursts when a spell solves a problem, red pulses when a trap bites, golden rays on a level-up. */
(function () {
  const F = EW.storyfx = {};
  const cv = document.createElement('canvas');
  cv.id = 'fxLayer';
  document.body.append(cv);
  const ctx = cv.getContext('2d');
  let W = 0, H = 0, dpr = 1, parts = [], overlays = [], running = false;
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function size() { dpr = Math.min(2, window.devicePixelRatio || 1); W = innerWidth; H = innerHeight; cv.width = W * dpr; cv.height = H * dpr; }
  size(); addEventListener('resize', size);
  const R = (a, b) => a + Math.random() * (b - a);

  /** Where things happen: the centre of the story page (or the battle map). */
  function origin() {
    const el = document.getElementById('page');
    const r = el && el.offsetParent ? el.getBoundingClientRect() : { left: W * .3, top: H * .2, width: W * .4, height: H * .5 };
    return { x: r.left + r.width / 2, y: r.top + Math.min(r.height, H) * .45, r };
  }
  function purse() {
    const el = document.querySelector('#partyPanel .hero-card');
    if (!el) return { x: 60, y: 80 };
    const r = el.getBoundingClientRect(); return { x: r.left + r.width * .8, y: r.top + 30 };
  }
  function start() { if (!running) { running = true; requestAnimationFrame(loop); } }

  F.play = function (kind, o) {
    if (reduced) return;
    o = o || {};
    const c = origin(), now = performance.now();
    switch (kind) {
      case 'coins': {
        const p = purse(), n = Math.min(24, 6 + Math.round((o.amount || 10) / 5));
        for (let i = 0; i < n; i++) parts.push({ k: 'coin', x: c.x + R(-60, 60), y: c.y + R(-20, 30), vx: R(-3, 3), vy: R(-9, -5), t0: now + i * 35, life: 1300, tx: p.x, ty: p.y, spin: R(0, 6) });
        break;
      }
      case 'loot':
        parts.push({ k: 'glyph', x: c.x, y: c.y + 40, text: o.glyph || '✦', label: o.label || '', t0: now, life: 2200 });
        for (let i = 0; i < 26; i++) parts.push({ k: 'mote', x: c.x + R(-50, 50), y: c.y + R(0, 60), vx: R(-.4, .4), vy: R(-2.2, -.8), t0: now + R(0, 500), life: R(900, 1600), col: '255,225,140', s: R(1.5, 3.2) });
        break;
      case 'heal':
        for (let i = 0; i < 40; i++) parts.push({ k: 'mote', x: c.r.left + R(0, c.r.width), y: c.r.top + Math.min(c.r.height, H) * R(.5, 1), vx: R(-.3, .3), vy: R(-1.6, -.6), t0: now + R(0, 700), life: R(1200, 2000), col: '140,255,180', s: R(1.5, 3) });
        break;
      case 'fire':
        overlays.push({ k: 'tint', col: '255,110,40', t0: now, life: 900 });
        for (let i = 0; i < 90; i++) parts.push({ k: 'flame', x: c.x + R(-c.r.width * .5, c.r.width * .5), y: c.y + R(80, 220), vx: R(-.5, .5), vy: R(-6, -2.5), t0: now + R(0, 350), life: R(600, 1100), s: R(12, 30) });
        break;
      case 'cold':
        overlays.push({ k: 'frost', t0: now, life: 1600 });
        for (let i = 0; i < 40; i++) parts.push({ k: 'shard', x: c.x + R(-c.r.width * .5, c.r.width * .5), y: c.y - R(120, 260), vx: R(-.6, .6), vy: R(2, 5), t0: now + R(0, 400), life: R(700, 1300), s: R(4, 9), rot: R(0, 6) });
        break;
      case 'lightning': {
        overlays.push({ k: 'tint', col: '220,230,255', t0: now, life: 500 });
        let x = c.x + R(-80, 80), y = 0; const pts = [[x, y]];
        while (y < c.y + 40) { y += R(25, 55); x += R(-40, 40); pts.push([x, y]); }
        parts.push({ k: 'bolt', pts, t0: now, life: 450 });
        break;
      }
      case 'arcane': case 'psychic': case 'radiant': case 'necrotic': case 'thunder': case 'force': case 'poison': case 'acid':
        parts.push({ k: 'rune', x: c.x, y: c.y, t0: now, life: 1400, col: { psychic: '255,140,220', radiant: '255,230,140', necrotic: '150,255,170', poison: '140,230,90', acid: '170,255,80' }[kind] || '190,150,255' });
        for (let i = 0; i < 24; i++) { const a = R(0, 6.3); parts.push({ k: 'mote', x: c.x + Math.cos(a) * 70, y: c.y + Math.sin(a) * 70, vx: Math.cos(a) * 1.5, vy: Math.sin(a) * 1.5, t0: now + 200, life: 900, col: '200,170,255', s: 2.2 }); }
        break;
      case 'hurt':
        overlays.push({ k: 'vignette', col: '200,20,20', t0: now, life: 700 });
        if (o.amount) parts.push({ k: 'text', x: c.x + R(-40, 40), y: c.y, text: '−' + o.amount, col: '#ff6b6b', t0: now, life: 1200 });
        break;
      case 'level':
        parts.push({ k: 'rays', x: W / 2, y: H * .42, t0: now, life: 2600 });
        break;
      case 'success':
        parts.push({ k: 'ring', x: c.x, y: c.y, t0: now, life: 900, col: '255,215,120' });
        break;
      case 'fail':
        for (let i = 0; i < 14; i++) parts.push({ k: 'smoke', x: c.x + R(-60, 60), y: c.y + R(-10, 30), vx: R(-.4, .4), vy: R(-.9, -.3), t0: now + R(0, 200), life: R(900, 1400), s: R(14, 26) });
        break;
    }
    start();
  };

  function loop(ts) {
    const now = performance.now();
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
    for (const o of overlays) {
      const k = (now - o.t0) / o.life; if (k < 0 || k > 1) continue;
      const a = Math.sin(k * Math.PI);
      if (o.k === 'tint') { ctx.fillStyle = `rgba(${o.col},${a * .22})`; ctx.fillRect(0, 0, W, H); }
      if (o.k === 'vignette' || o.k === 'frost') {
        const col = o.k === 'frost' ? '170,220,255' : o.col;
        const g = ctx.createRadialGradient(W / 2, H / 2, Math.min(W, H) * .25, W / 2, H / 2, Math.max(W, H) * .7);
        g.addColorStop(0, `rgba(${col},0)`); g.addColorStop(1, `rgba(${col},${a * (o.k === 'frost' ? .45 : .55)})`);
        ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
      }
    }
    overlays = overlays.filter(o => now - o.t0 < o.life);
    for (const p of parts) {
      const age = now - p.t0; if (age < 0) continue;
      const k = age / p.life; if (k > 1) { p.dead = true; continue; }
      switch (p.k) {
        case 'coin': {
          // fly up, then home in on the purse
          const e = Math.min(1, k * 1.25), hx = p.x + p.vx * age / 16, hy = p.y + p.vy * age / 16 + .5 * .35 * Math.pow(age / 16, 2) * .5;
          const x = hx + (p.tx - hx) * Math.pow(e, 2.2), y = hy + (p.ty - hy) * Math.pow(e, 2.2);
          const w = Math.abs(Math.cos(p.spin + age / 90)) * 6 + 1;
          ctx.fillStyle = '#f2c14e'; ctx.strokeStyle = '#a07418'; ctx.lineWidth = 1;
          ctx.globalAlpha = k > .9 ? (1 - k) * 10 : 1;
          ctx.beginPath(); ctx.ellipse(x, y, w, 6, 0, 0, 7); ctx.fill(); ctx.stroke(); ctx.globalAlpha = 1;
          break;
        }
        case 'mote': case 'flame': case 'smoke': {
          const x = p.x + p.vx * age / 16, y = p.y + p.vy * age / 16, a = Math.sin(k * Math.PI);
          if (p.k === 'mote') { ctx.fillStyle = `rgba(${p.col},${a})`; ctx.beginPath(); ctx.arc(x, y, p.s, 0, 7); ctx.fill(); }
          else if (p.k === 'flame') {
            // a licking tongue of flame: a stretched glow that narrows as it rises
            ctx.save(); ctx.globalCompositeOperation = 'lighter'; ctx.translate(x, y); ctx.scale(.55, 1.6);
            const r = p.s * (1 - k * .6), g = ctx.createRadialGradient(0, 0, 1, 0, 0, r);
            g.addColorStop(0, `rgba(255,235,150,${a})`); g.addColorStop(.45, `rgba(255,120,30,${a * .7})`); g.addColorStop(1, 'rgba(200,40,0,0)');
            ctx.fillStyle = g; ctx.beginPath(); ctx.arc(0, 0, r, 0, 7); ctx.fill(); ctx.restore();
          }
          else { ctx.fillStyle = `rgba(90,90,100,${a * .35})`; ctx.beginPath(); ctx.arc(x, y, p.s * (1 + k), 0, 7); ctx.fill(); }
          break;
        }
        case 'shard': {
          const x = p.x + p.vx * age / 16, y = p.y + p.vy * age / 16;
          ctx.save(); ctx.translate(x, y); ctx.rotate(p.rot + k * 3); ctx.fillStyle = `rgba(200,235,255,${1 - k})`; ctx.strokeStyle = `rgba(255,255,255,${1 - k})`;
          ctx.beginPath(); ctx.moveTo(0, -p.s); ctx.lineTo(p.s * .35, 0); ctx.lineTo(0, p.s); ctx.lineTo(-p.s * .35, 0); ctx.closePath(); ctx.fill(); ctx.stroke(); ctx.restore();
          break;
        }
        case 'bolt':
          ctx.strokeStyle = `rgba(235,240,255,${1 - k})`; ctx.lineWidth = 3; ctx.shadowColor = '#9cf'; ctx.shadowBlur = 18;
          ctx.beginPath(); p.pts.forEach(([x, y], i) => i ? ctx.lineTo(x, y) : ctx.moveTo(x, y)); ctx.stroke(); ctx.shadowBlur = 0;
          break;
        case 'rune': {
          const r = 40 + k * 60, a = Math.sin(k * Math.PI);
          ctx.save(); ctx.translate(p.x, p.y); ctx.rotate(k * 2.5);
          ctx.strokeStyle = `rgba(${p.col},${a})`; ctx.lineWidth = 2; ctx.shadowColor = `rgb(${p.col})`; ctx.shadowBlur = 12;
          ctx.beginPath(); ctx.arc(0, 0, r, 0, 7); ctx.stroke(); ctx.beginPath(); ctx.arc(0, 0, r * .72, 0, 7); ctx.stroke();
          for (let i = 0; i < 6; i++) { const a1 = i * Math.PI / 3, a2 = a1 + Math.PI * 2 / 3; ctx.beginPath(); ctx.moveTo(Math.cos(a1) * r * .72, Math.sin(a1) * r * .72); ctx.lineTo(Math.cos(a2) * r * .72, Math.sin(a2) * r * .72); ctx.stroke(); }
          ctx.shadowBlur = 0; ctx.restore();
          break;
        }
        case 'glyph': {
          const y = p.y - k * 90, a = k < .15 ? k / .15 : k > .75 ? (1 - k) / .25 : 1, s = 1 + Math.min(1, k * 4) * .6;
          ctx.save(); ctx.globalAlpha = a; ctx.translate(p.x, y);
          const g = ctx.createRadialGradient(0, 0, 2, 0, 0, 60); g.addColorStop(0, 'rgba(255,220,130,.6)'); g.addColorStop(1, 'rgba(255,220,130,0)'); ctx.fillStyle = g; ctx.fillRect(-60, -60, 120, 120);
          ctx.font = `${Math.round(40 * s)}px serif`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText(p.text, 0, 0);
          if (p.label) {
            ctx.font = '700 15px "Alegreya Sans", sans-serif'; const tw = ctx.measureText(p.label).width + 22;
            ctx.fillStyle = 'rgba(20,16,28,.85)'; ctx.beginPath(); ctx.roundRect ? ctx.roundRect(-tw / 2, 32, tw, 24, 12) : ctx.rect(-tw / 2, 32, tw, 24); ctx.fill();
            ctx.strokeStyle = 'rgba(255,210,120,.7)'; ctx.lineWidth = 1; ctx.stroke();
            ctx.fillStyle = '#ffe9b0'; ctx.fillText(p.label, 0, 45);
          }
          ctx.restore();
          break;
        }
        case 'text': {
          ctx.globalAlpha = 1 - k; ctx.font = '800 30px "Alegreya Sans", sans-serif'; ctx.textAlign = 'center'; ctx.fillStyle = p.col; ctx.strokeStyle = '#000'; ctx.lineWidth = 4;
          ctx.strokeText(p.text, p.x, p.y - k * 60); ctx.fillText(p.text, p.x, p.y - k * 60); ctx.globalAlpha = 1;
          break;
        }
        case 'rays': {
          const a = Math.sin(k * Math.PI);
          ctx.save(); ctx.translate(p.x, p.y); ctx.rotate(k * .8); ctx.globalCompositeOperation = 'lighter';
          for (let i = 0; i < 14; i++) { ctx.rotate(Math.PI / 7); const g = ctx.createLinearGradient(0, 0, 0, -Math.max(W, H) * .6); g.addColorStop(0, `rgba(255,220,120,${a * .35})`); g.addColorStop(1, 'rgba(255,220,120,0)'); ctx.fillStyle = g; ctx.beginPath(); ctx.moveTo(-14, 0); ctx.lineTo(0, -Math.max(W, H) * .6); ctx.lineTo(14, 0); ctx.fill(); }
          ctx.restore(); ctx.globalCompositeOperation = 'source-over';
          break;
        }
        case 'ring': {
          ctx.strokeStyle = `rgba(${p.col},${1 - k})`; ctx.lineWidth = 4 * (1 - k) + 1; ctx.beginPath(); ctx.arc(p.x, p.y, 20 + k * 180, 0, 7); ctx.stroke();
          break;
        }
      }
    }
    parts = parts.filter(p => !p.dead);
    if (parts.length || overlays.length) requestAnimationFrame(loop);
    else { running = false; ctx.clearRect(0, 0, W, H); }
  }
})();
