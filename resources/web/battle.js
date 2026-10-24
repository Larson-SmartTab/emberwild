/* Battle map: painted grid terrain, tokens with portraits, movement / targeting / area templates, animated effects. */
(function () {
  const B = EW.battle = {};
  const cv = document.getElementById('map');
  const ctx = cv.getContext('2d');
  const wrap = document.getElementById('mapWrap');
  const tip = document.getElementById('tooltip');
  const bar = document.getElementById('actionBar');
  let view = null, cell = 40, ox = 0, oy = 0, dpr = 1, terrain = null, terrainKey = '';
  let pos = {}, anims = {}, floaters = [], effects = [], fxQueue = [], fxBusyUntil = 0;
  let hover = null, mode = null, portraits = {}, lastTurn = null;

  const DTYPE = { fire: '#ff7a2a', cold: '#8fd3ff', lightning: '#d9f2ff', acid: '#9be35a', poison: '#7bdc5a', thunder: '#c5b3ff', force: '#c9a2ff', radiant: '#ffe48a', necrotic: '#9a7aa8', psychic: '#ff8ad8', piercing: '#e8e0d0', slashing: '#e8e0d0', bludgeoning: '#e8e0d0', arcane: '#c9a2ff' };

  const THEMES = {
    tavern: { floor: '#5a3a22', floor2: '#4e321d', wall: '#2b1d14', grid: 'rgba(0,0,0,.25)' },
    forest: { floor: '#3c5a32', floor2: '#36522d', wall: '#2d2d2d', grid: 'rgba(0,0,0,.18)' },
    camp: { floor: '#2f4428', floor2: '#2a3d24', wall: '#2d2d2d', grid: 'rgba(0,0,0,.2)' },
    bridge: { floor: '#4a6a3a', floor2: '#43603a', wall: '#3a3a3a', grid: 'rgba(0,0,0,.18)' },
    swamp: { floor: '#3a4a36', floor2: '#34432f', wall: '#2a2a2a', grid: 'rgba(0,0,0,.2)' },
    chapel: { floor: '#4a4a52', floor2: '#43434b', wall: '#25252b', grid: 'rgba(0,0,0,.25)' },
    ruins: { floor: '#3e3a40', floor2: '#373339', wall: '#1d1a20', grid: 'rgba(0,0,0,.25)' },
    hall: { floor: '#4a5a6a', floor2: '#435362', wall: '#232c36', grid: 'rgba(0,0,0,.22)' },
    lair: { floor: '#2a2428', floor2: '#252024', wall: '#121014', grid: 'rgba(0,0,0,.3)' },
    dungeon: { floor: '#3e3a36', floor2: '#383430', wall: '#1d1b19', grid: 'rgba(0,0,0,.25)' },
  };

  function rng(seed) { let a = seed; return () => { a = (a * 1664525 + 1013904223) >>> 0; return a / 4294967296; }; }

  // ---------------------------------------------------------------- layout
  function layout() {
    if (!view) return;
    dpr = Math.min(2, window.devicePixelRatio || 1);
    const ww = wrap.clientWidth - 16, wh = wrap.clientHeight - 16;
    cell = Math.max(18, Math.min(64, Math.floor(Math.min(ww / view.w, wh / view.h))));
    cv.width = view.w * cell * dpr; cv.height = view.h * cell * dpr;
    cv.style.width = view.w * cell + 'px'; cv.style.height = view.h * cell + 'px';
    ox = 0; oy = 0;
    terrainKey = '';
  }
  window.addEventListener('resize', () => { layout(); });

  function paintTerrain() {
    const key = view.tiles.join('') + cell + view.backdrop + (view.scorched || []).join();
    if (key === terrainKey && terrain) return;
    terrainKey = key;
    terrain = document.createElement('canvas');
    terrain.width = cv.width; terrain.height = cv.height;
    const c = terrain.getContext('2d');
    c.scale(dpr, dpr);
    const th = THEMES[view.backdrop] || THEMES.forest;
    const r = rng(7);
    for (let y = 0; y < view.h; y++) for (let x = 0; x < view.w; x++) {
      const t = view.tiles[y][x] || '#';
      const px = x * cell, py = y * cell;
      c.fillStyle = (x + y) % 2 ? th.floor : th.floor2;
      c.fillRect(px, py, cell, cell);
      for (let k = 0; k < 4; k++) { c.fillStyle = `rgba(0,0,0,${r() * .08})`; c.fillRect(px + r() * cell, py + r() * cell, cell * .2, cell * .2); }
      if (view.backdrop === 'tavern' && t !== '#') { c.strokeStyle = 'rgba(0,0,0,.25)'; c.beginPath(); c.moveTo(px, py + cell / 2); c.lineTo(px + cell, py + cell / 2); c.stroke(); }
      switch (t) {
        case '#': {
          c.fillStyle = th.wall; c.fillRect(px, py, cell, cell);
          c.fillStyle = EW.shade(th.wall, 18); c.fillRect(px + 2, py + 2, cell - 4, cell * .25);
          c.strokeStyle = 'rgba(0,0,0,.4)'; c.strokeRect(px + .5, py + .5, cell - 1, cell - 1);
          break;
        }
        case ',': for (let k = 0; k < 5; k++) { c.strokeStyle = '#6f8f42'; c.lineWidth = 1.5; const gx = px + r() * cell, gy = py + cell * (.4 + r() * .5); c.beginPath(); c.moveTo(gx, gy); c.lineTo(gx - 3, gy - cell * .25); c.moveTo(gx, gy); c.lineTo(gx + 3, gy - cell * .22); c.stroke(); } break;
        case '~': { c.fillStyle = view.backdrop === 'swamp' ? '#24382f' : '#2f5a6e'; c.fillRect(px, py, cell, cell); break; }
        case '=': { c.fillStyle = '#7a6046'; c.fillRect(px, py, cell, cell); c.strokeStyle = '#4a3828'; for (let k = 1; k < 4; k++) { c.beginPath(); c.moveTo(px + k * cell / 4, py); c.lineTo(px + k * cell / 4, py + cell); c.stroke(); } break; }
        case 'T': {
          c.fillStyle = 'rgba(0,0,0,.35)'; c.beginPath(); c.ellipse(px + cell * .55, py + cell * .62, cell * .5, cell * .38, 0, 0, 7); c.fill();
          c.fillStyle = view.backdrop === 'camp' ? '#1d2e22' : '#2d4a2a'; c.beginPath(); c.arc(px + cell * .5, py + cell * .45, cell * .48, 0, 7); c.fill();
          c.fillStyle = 'rgba(255,255,255,.07)'; c.beginPath(); c.arc(px + cell * .4, py + cell * .35, cell * .22, 0, 7); c.fill();
          break;
        }
        case 'B': {
          c.fillStyle = 'rgba(0,0,0,.3)'; c.fillRect(px + cell * .18, py + cell * .22, cell * .74, cell * .74);
          c.fillStyle = view.backdrop === 'bridge' ? '#6b4a2c' : view.backdrop === 'chapel' ? '#3a2616' : '#6b4526';
          c.fillRect(px + cell * .12, py + cell * .12, cell * .74, cell * .74);
          c.strokeStyle = 'rgba(0,0,0,.4)'; c.strokeRect(px + cell * .12, py + cell * .12, cell * .74, cell * .74);
          c.beginPath(); c.moveTo(px + cell * .12, py + cell * .5); c.lineTo(px + cell * .86, py + cell * .5); c.stroke();
          break;
        }
        case '^': { c.fillStyle = '#b8b0a0'; for (let k = 0; k < 3; k++) { c.beginPath(); const sx = px + cell * (.25 + k * .25); c.moveTo(sx - cell * .08, py + cell * .7); c.lineTo(sx, py + cell * .3); c.lineTo(sx + cell * .08, py + cell * .7); c.fill(); } break; }
      }
      if (t !== '#') { c.strokeStyle = th.grid; c.lineWidth = 1; c.strokeRect(px + .5, py + .5, cell - 1, cell - 1); }
    }
    for (const [x, y] of view.scorched || []) { c.fillStyle = 'rgba(20,10,5,.45)'; c.beginPath(); c.arc(x * cell + cell / 2, y * cell + cell / 2, cell * .4, 0, 7); c.fill(); }
  }

  // ---------------------------------------------------------------- geometry (mirrors the server)
  function sphereCells(cx, cy, rad) {
    const out = [];
    for (let y = cy - rad; y <= cy + rad; y++) for (let x = cx - rad; x <= cx + rad; x++) {
      if (x < 0 || y < 0 || x >= view.w || y >= view.h || view.tiles[y][x] === '#') continue;
      if (Math.hypot(x - cx, y - cy) <= rad + .5) out.push([x, y]);
    }
    return out;
  }
  function coneCells(u, tx, ty, len) {
    const out = [], o = { x: u.x + (u.size - 1) / 2, y: u.y + (u.size - 1) / 2 };
    const ang = Math.atan2(ty - o.y, tx - o.x);
    for (let y = 0; y < view.h; y++) for (let x = 0; x < view.w; x++) {
      if (view.tiles[y][x] === '#') continue;
      if (x >= u.x && x < u.x + u.size && y >= u.y && y < u.y + u.size) continue;
      const d = Math.hypot(x - o.x, y - o.y) - (u.size - 1) / 2;
      if (d > len + .3 || d < .5) continue;
      let a = Math.atan2(y - o.y, x - o.x) - ang;
      while (a > Math.PI) a -= 2 * Math.PI; while (a < -Math.PI) a += 2 * Math.PI;
      if (Math.abs(a) <= Math.PI / 6) out.push([x, y]);
    }
    return out;
  }
  function cubeCells(u, tx, ty, len) {
    let dx = Math.sign(tx - u.x), dy = Math.sign(ty - u.y);
    if (!dx && !dy) dx = 1;
    const x0 = dx > 0 ? u.x + u.size : dx < 0 ? u.x - len : u.x - Math.floor((len - u.size) / 2);
    const y0 = dy > 0 ? u.y + u.size : dy < 0 ? u.y - len : u.y - Math.floor((len - u.size) / 2);
    const out = [];
    for (let y = y0; y < y0 + len; y++) for (let x = x0; x < x0 + len; x++) if (x >= 0 && y >= 0 && x < view.w && y < view.h && view.tiles[y][x] !== '#') out.push([x, y]);
    return out;
  }
  const gap = (a, b) => Math.max(Math.max(0, Math.max(a.x - (b.x + (b.size || 1) - 1), b.x - (a.x + (a.size || 1) - 1))), Math.max(0, Math.max(a.y - (b.y + (b.size || 1) - 1), b.y - (a.y + (a.size || 1) - 1))));
  const unitAt = (x, y) => view.units.find(u => !u.gone && !u.dead && x >= u.x && x < u.x + u.size && y >= u.y && y < u.y + u.size);
  const byId = id => view.units.find(u => u.id === id);

  // ---------------------------------------------------------------- state
  B.current = () => view && byId(view.turn);
  B.myTurn = function () {
    const u = B.current();
    if (!u || !u.hero || u.ai || view.over) return false;
    return u.owner === EW.me.id || (EW.me.host && !EW.state.players.some(p => p.id === u.owner && p.online));
  };

  B.update = function (v) {
    const first = !view || view.title !== v.title || view.w !== v.w;
    view = v;
    for (const u of v.units) {
      if (!pos[u.id] || (!anims[u.id] || !anims[u.id].length)) pos[u.id] = { x: u.x, y: u.y };
      if (u.hero && !portraits[u.heroId]) {
        const hero = EW.state.heroes.find(h => h.id === u.heroId);
        if (hero) { const pc = document.createElement('canvas'); EW.portrait(pc, hero, 128); portraits[u.heroId] = pc; }
      }
    }
    if (first) { pos = {}; for (const u of v.units) pos[u.id] = { x: u.x, y: u.y }; anims = {}; effects = []; floaters = []; layout(); mode = null; }
    if (v.turn !== lastTurn) { lastTurn = v.turn; mode = null; if (B.myTurn()) EW.audio.sfx('turn'); }
    document.getElementById('battleTitle').textContent = v.title;
    document.getElementById('battleObjective').textContent = `Round ${v.round} · ${v.objective}` + (v.nonlethal ? ' · nobody dies in a brawl' : '');
    renderInitiative();
    renderBar();
  };

  B.reset = function () { view = null; pos = {}; anims = {}; portraits = {}; mode = null; };

  function renderInitiative() {
    const el = document.getElementById('initiative');
    el.innerHTML = '';
    for (const id of view.order) {
      const u = byId(id); if (!u) continue;
      const d = EW.h('div.ini' + (u.side === 'foe' ? '.foe' : '') + (id === view.turn ? '.cur' : '') + (u.gone || u.dead || (u.down && !u.hero) ? '.gone' : ''), { title: `${u.name} (initiative ${u.init})` }, u.glyph);
      if (u.hero) d.style.background = u.color;
      el.append(d);
    }
  }

  // ---------------------------------------------------------------- action bar
  function econ(ti) {
    return EW.h('div.econ',
      EW.h('span.' + (ti.action ? 'on' : 'off'), 'Action'),
      EW.h('span.' + (ti.bonus ? 'on' : 'off'), 'Bonus'),
      EW.h('span.' + (ti.reaction ? 'on' : 'off'), 'Reaction'),
      EW.h('span.' + (ti.move > 0 ? 'on' : 'off'), `Move ${ti.move} ft`));
  }

  function act(type, extra) { return EW.api('/api/act', Object.assign({ type }, extra || {})); }

  function renderBar() {
    bar.innerHTML = '';
    const u = B.current();
    if (!u) return;
    if (view.over) { bar.append(EW.h('div.waiting', view.result === 'victory' ? 'Victory!' : 'Defeat…')); return; }
    if (!B.myTurn()) {
      const who = u.hero ? `${u.name} (${(EW.state.players.find(p => p.id === u.owner) || {}).name || 'companion'})` : u.name;
      const row = EW.h('div.ab-row', EW.h('div.waiting', u.side === 'foe' ? `${u.name} is acting…` : `Waiting for ${who}…`));
      if (u.hero && !u.ai && EW.me.host) row.append(EW.h('button.act', { onclick: () => act('auto') }, 'Let the AI play this turn'));
      bar.append(row);
      return;
    }
    const ti = view.turnInfo || {}, op = view.options || {};
    const hero = EW.state.heroes.find(h => h.id === u.heroId) || {};
    const status = EW.h('div.ab-status', EW.h('b', `${u.name}'s turn`), econ(ti));
    if (hero.spells && hero.spells.some(s => s.id === 'shield')) status.append(EW.h('button.act' + (ti.autoShield ? '.sel' : ''), { onclick: () => act('toggleShield'), title: 'Cast Shield automatically when it would turn a hit into a miss' }, `🛡 Auto-Shield ${ti.autoShield ? 'on' : 'off'}`));
    status.append(EW.h('button.act', { onclick: () => act('auto'), title: 'Let the AI take this turn for you' }, '🤖 Auto'));
    status.append(EW.h('button.act.end', { onclick: () => { mode = null; act('end'); } }, 'End turn'));
    bar.append(status);
    if (mode) {
      const r = EW.h('div.ab-row', EW.h('span.lab', 'Targeting'), EW.h('span', mode.prompt));
      if (mode.multi) r.append(EW.h('button.act.sel', { onclick: () => commit() }, `Cast (${mode.targets.length}/${mode.max})`));
      if (mode.elements) {
        const els = EW.h('span.elements');
        for (const e of ['fire', 'cold', 'lightning', 'acid', 'thunder', 'poison']) els.append(EW.h('button.act' + (mode.element === e ? '.sel' : ''), { onclick: () => { mode.element = e; renderBar(); }, style: { color: DTYPE[e] } }, e));
        r.append(els);
      }
      r.append(EW.h('button.act', { onclick: () => { mode = null; renderBar(); } }, 'Cancel (Esc)'));
      bar.append(r);
    }
    // actions
    const w = op.weapon || {};
    const a1 = EW.h('div.ab-row', EW.h('span.lab', 'Action'));
    a1.append(EW.h('button.act' + (mode && mode.kind === 'attack' ? '.sel' : ''), { disabled: !ti.action, onclick: () => setMode({ kind: 'attack', filter: 'enemy', prompt: `Click an enemy to attack with ${w.name} (${EW.sign(w.toHit)} to hit, ${w.damage}).` }), title: `${w.name}: ${EW.sign(w.toHit)} to hit, ${w.damage}${w.mastery ? ', mastery: ' + w.mastery : ''}` }, `⚔ ${w.name} ${EW.sign(w.toHit || 0)}`));
    for (const [t, l, tt] of [['dash', '🏃 Dash', 'Double your movement'], ['disengage', '↩ Disengage', 'Move without provoking opportunity attacks'], ['dodge', '🌀 Dodge', 'Attacks against you have Disadvantage'], ['hide', '🌑 Hide', 'Stealth check to become hidden']])
      a1.append(EW.h('button.act', { disabled: !ti.action, title: tt, onclick: () => act(t) }, l));
    a1.append(EW.h('button.act', { disabled: !ti.action, title: 'Give an ally Advantage against an adjacent enemy, or stabilize/wake an ally', onclick: () => setMode({ kind: 'help', filter: 'any', prompt: 'Click an enemy next to you, or a dying / sleeping ally.' }) }, '🤝 Help'));
    bar.append(a1);
    if (op.spells && op.spells.length) {
      const r = EW.h('div.ab-row', EW.h('span.lab', `Spells`));
      const slots = (op.slots || []).map((n, i) => `L${i + 1}: ${n}/${op.slotsMax[i]}`).join('  ');
      for (const s of op.spells) {
        if (s.reason === 'Not useful in combat' || s.reason === 'Reaction (automatic)') continue;
        const b = EW.h('button.act.spell' + (mode && mode.spell && mode.spell.id === s.id ? '.sel' : ''), { disabled: !s.available, title: `${s.name}${s.level ? ' (level ' + s.level + ')' : ' (cantrip)'} — ${s.desc}${s.reason ? '\n' + s.reason : ''}`, onclick: () => pickSpell(s) },
          s.name, EW.h('span.lv', s.level ? `L${s.level}${s.cast === 'bonus' ? ' · bonus' : ''}` : (s.cast === 'bonus' ? 'bonus' : '∞')));
        r.append(b);
      }
      if (slots) r.append(EW.h('span.hint', slots));
      bar.append(r);
    }
    if (op.features && op.features.length) {
      const r = EW.h('div.ab-row', EW.h('span.lab', 'Features'));
      for (const f of op.features) r.append(EW.h('button.act' + (f.id === 'smite' && ti.smiteArmed ? '.sel' : ''), { disabled: !f.available, title: f.desc + ` (${f.cost})`, onclick: () => pickFeature(f) }, f.name));
      bar.append(r);
    }
    if (op.items && op.items.length) {
      const r = EW.h('div.ab-row', EW.h('span.lab', 'Items'));
      for (const it of op.items) r.append(EW.h('button.act', { disabled: !it.available, title: it.desc, onclick: () => pickItem(it) }, `${it.kind === 'potion' ? '🧪' : '💥'} ${it.name} ×${it.count}`));
      bar.append(r);
    }
  }

  function setMode(m) { mode = m; mode.targets = []; renderBar(); }

  function pickSpell(s) {
    let level = s.level ? (s.levels[0] || s.level) : 0;
    if (s.level && s.levels.length > 1) {
      const pick = prompt(`Cast ${s.name} using which slot level? (${s.levels.join(', ')})`, String(s.levels[0]));
      if (pick === null) return;
      if (s.levels.includes(parseInt(pick))) level = parseInt(pick);
    }
    const base = { spell: s, level };
    if (s.selfOnly) { act('cast', { spell: s.id, level }); return; }
    if (s.id === 'misty_step') return setMode({ ...base, kind: 'point', shape: 'tele', range: 30, prompt: 'Click an empty square within 30 ft to teleport.' });
    if (s.area === 'cone') return setMode({ ...base, kind: 'point', shape: 'cone', size: s.areaSize / 5, prompt: `Aim the ${s.areaSize}-ft cone — careful, it hits allies too.` });
    if (s.area === 'cube') return setMode({ ...base, kind: 'point', shape: 'cube', size: s.areaSize / 5, prompt: `Choose a direction for the ${s.areaSize}-ft cube.` });
    if (s.area === 'sphere') return setMode({ ...base, kind: 'point', shape: 'sphere', size: s.areaSize / 5, range: s.range, prompt: `Pick a point within ${s.range} ft (${s.areaSize}-ft radius).` });
    const extra = Math.max(0, level - s.level) * (s.upTargets || 0);
    const max = s.targets + extra;
    const filter = s.targetsAllies ? 'ally' : (s.kind === 'buff' && s.id !== 'hunters_mark' ? 'ally' : 'enemy');
    setMode({ ...base, kind: 'unit', filter, max, multi: max > 1, elements: s.elements, element: 'cold', range: s.range,
      prompt: max > 1 ? `Choose up to ${max} targets (click the same target again for extra ${s.kind === 'auto' ? 'darts' : 'rays'}), then press Cast.` : `Choose a target for ${s.name} (${s.range === 5 ? 'touch' : s.range + ' ft'}).` });
  }

  function pickFeature(f) {
    if (f.cost === 'toggle') return act('smite');
    switch (f.target) {
      case 'none': return act('feature', { id: f.id });
      case 'ally': return setMode({ kind: 'feature', feature: f, filter: 'ally', prompt: `${f.name}: choose an ally.` });
      case 'enemy': return setMode({ kind: 'feature', feature: f, filter: 'enemy', prompt: `${f.name}: choose an enemy.` });
      case 'any': return setMode({ kind: 'feature', feature: f, filter: 'any', prompt: `${f.name}: choose an ally to heal or an enemy to sear.` });
      case 'cone15': return setMode({ kind: 'point', feature: f, shape: 'cone', size: 3, prompt: 'Aim your breath (15-ft cone).' });
      case 'point5': return setMode({ kind: 'point', feature: f, shape: 'sphere', size: 1, range: 60, prompt: 'Roll the sphere to a point.' });
    }
  }

  function pickItem(it) {
    if (it.kind === 'potion') return setMode({ kind: 'item', item: it, filter: 'ally', prompt: `Who drinks the ${it.name}? (yourself = bonus action, an adjacent ally = action)` });
    setMode({ kind: 'item', item: it, filter: 'enemy', prompt: `Throw the ${it.name} at an enemy within 20 ft.` });
  }

  function commit() {
    if (!mode) return;
    const m = mode; mode = null;
    if (m.kind === 'unit') act('cast', { spell: m.spell.id, level: m.level, targets: m.targets, element: m.element });
    renderBar();
  }

  // ---------------------------------------------------------------- input
  function cellFrom(ev) {
    const rect = cv.getBoundingClientRect();
    const x = Math.floor((ev.clientX - rect.left) / cell), y = Math.floor((ev.clientY - rect.top) / cell);
    return x >= 0 && y >= 0 && x < view.w && y < view.h ? { x, y } : null;
  }

  cv.addEventListener('mousemove', ev => {
    if (!view) return;
    hover = cellFrom(ev);
    const u = hover && unitAt(hover.x, hover.y);
    if (u) {
      tip.classList.remove('hidden');
      const hp = `${Math.max(0, u.hp)}/${u.maxHp} HP${u.temp ? ` (+${u.temp})` : ''}`;
      tip.innerHTML = `<b>${EW.esc(u.name)}</b><br>${hp} · AC ${u.ac} · speed ${u.speed} ft${u.conds.length ? '<br>' + u.conds.map(EW.condLabel).join(', ') : ''}${u.blurb ? `<br><i>${EW.esc(u.blurb)}</i>` : ''}${u.down && u.hero && !u.dead ? `<br>Death saves: ${u.deathS}✓ ${u.deathF}✗` : ''}`;
      const wr = wrap.getBoundingClientRect();
      tip.style.left = Math.min(wr.width - 270, ev.clientX - wr.left + 14) + 'px';
      tip.style.top = (ev.clientY - wr.top + 14) + 'px';
    } else tip.classList.add('hidden');
  });
  cv.addEventListener('mouseleave', () => { hover = null; tip.classList.add('hidden'); });
  cv.addEventListener('contextmenu', ev => { ev.preventDefault(); mode = null; renderBar(); });
  window.addEventListener('keydown', ev => { if (ev.key === 'Escape' && mode) { mode = null; renderBar(); } });

  cv.addEventListener('click', ev => {
    if (!view || !B.myTurn()) return;
    const c = cellFrom(ev); if (!c) return;
    const me = B.current();
    const u = unitAt(c.x, c.y);
    if (mode) {
      if (mode.kind === 'point') {
        if (mode.feature) act('feature', { id: mode.feature.id, x: c.x, y: c.y });
        else act('cast', { spell: mode.spell.id, level: mode.level, x: c.x, y: c.y });
        mode = null; renderBar(); return;
      }
      if (!u) return;
      const isAlly = u.side === me.side;
      if (mode.filter === 'enemy' && isAlly) return EW.toast('Choose an enemy.');
      if (mode.filter === 'ally' && !isAlly) return EW.toast('Choose an ally.');
      if (mode.kind === 'attack') { act('attack', { target: u.id }); mode = null; }
      else if (mode.kind === 'help') { act('help', { target: u.id }); mode = null; }
      else if (mode.kind === 'feature') { act('feature', { id: mode.feature.id, target: u.id }); mode = null; }
      else if (mode.kind === 'item') { act('item', { item: mode.item.id, target: u.id }); mode = null; }
      else if (mode.kind === 'unit') {
        mode.targets.push(u.id);
        if (!mode.multi || mode.targets.length >= mode.max) { commit(); return; }
      }
      renderBar(); return;
    }
    // default: click enemy = attack, click reachable square = move
    if (u && u.side !== me.side && view.turnInfo.action) { act('attack', { target: u.id }); return; }
    if (view.reach && view.reach[c.x + ',' + c.y] != null) act('move', { x: c.x, y: c.y });
  });

  // ---------------------------------------------------------------- fx
  B.fx = function (e) {
    if (e.t === 'move') {
      // reserve the animation now so the token doesn't jump to its new square before the move plays
      const pts = e.path.map(p => ({ x: p[0], y: p[1] }));
      e._anim = { pts, t0: Infinity, dur: Math.max(120, (pts.length - 1) * (e.push ? 70 : 110)) };
      (anims[e.id] = anims[e.id] || []).push(e._anim);
    }
    fxQueue.push(e);
  };

  function startFx(e, now) {
    switch (e.t) {
      case 'move': {
        if (e._anim && e._anim.t0 === Infinity) e._anim.t0 = now;
        return e._anim ? e._anim.dur : 0;
      }
      case 'teleport': effects.push({ k: 'mist', at: e.from, t0: now, dur: 600 }); effects.push({ k: 'mist', at: e.to, t0: now + 200, dur: 600 }); pos[e.id] = { x: e.to[0], y: e.to[1] }; anims[e.id] = []; EW.audio.sfx('cold'); return 350;
      case 'attack': {
        const a = byId(e.from), b = byId(e.to); if (!a || !b) return 0;
        const col = DTYPE[e.dtype] || (e.kind === 'melee' ? '#fff3d0' : '#e8d8b0');
        effects.push({ k: e.kind === 'melee' ? 'slash' : 'shot', from: e.from, to: e.to, col, hit: e.hit, t0: now, dur: e.kind === 'melee' ? 260 : 330 });
        EW.audio.sfx(e.hit ? 'hit' : 'miss');
        return 300;
      }
      case 'spell': {
        const col = DTYPE[e.dtype] || '#c9a2ff';
        const kind = e.spell === 'ray_of_frost' || e.spell === 'scorching_ray' ? 'beam' : e.spell === 'shocking_grasp' ? 'bolt' : e.from === e.to ? 'burst' : 'orb';
        effects.push({ k: kind, from: e.from, to: e.to, col, hit: e.hit !== false, t0: now + (e.i || 0) * 90, dur: 420 });
        EW.audio.sfx(e.dtype === 'cold' ? 'cold' : e.dtype === 'lightning' ? 'lightning' : e.dtype === 'fire' ? 'fire' : 'heal');
        return 260;
      }
      case 'area': effects.push({ k: 'area', cells: e.cells, col: DTYPE[e.dtype] || '#ff9a4a', t0: now, dur: 700, from: e.from, name: e.name }); EW.audio.sfx(e.dtype === 'cold' ? 'cold' : e.dtype === 'thunder' ? 'lightning' : 'fire'); return 450;
      case 'dmg': { const u = byId(e.id); if (u) floaters.push({ id: e.id, text: e.amount === 0 && e.note ? e.note : '-' + e.amount + (e.note === 'VULNERABLE' ? '!!' : ''), col: e.note === 'IMMUNE' ? '#bbb' : DTYPE[e.dtype] && !['piercing', 'slashing', 'bludgeoning'].includes(e.dtype) ? DTYPE[e.dtype] : '#ff6b6b', t0: now, big: e.crit || e.note === 'VULNERABLE' }); effects.push({ k: 'shake', id: e.id, t0: now, dur: 250 }); return 60; }
      case 'heal': { if (e.amount > 0) { floaters.push({ id: e.id, text: '+' + e.amount, col: '#7dffb0', t0: now }); EW.audio.sfx('heal'); } return 60; }
      case 'shield': effects.push({ k: 'shield', id: e.id, t0: now, dur: 900 }); EW.audio.sfx('cold'); return 200;
      case 'death': effects.push({ k: 'death', id: e.id, t0: now, dur: 700 }); return 150;
      case 'down': floaters.push({ id: e.id, text: 'DOWN', col: '#ff6b6b', t0: now, big: true }); return 100;
      case 'telegraph': return 100;
      case 'erupt': effects.push({ k: 'erupt', cells: e.cells, t0: now, dur: 900 }); EW.audio.sfx('fire'); return 500;
      case 'phase': return 400;
      case 'summon': effects.push({ k: 'mist', at: [byId(e.id)?.x ?? 0, byId(e.id)?.y ?? 0], t0: now, dur: 600, col: '#ff7a2a' }); return 150;
      case 'flee': effects.push({ k: 'death', id: e.id, t0: now, dur: 500 }); return 100;
      default: return 0;
    }
  }

  function centre(id) {
    const u = byId(id); if (!u) return [0, 0];
    const p = displayPos(u, performance.now());
    return [(p.x + u.size / 2) * cell, (p.y + u.size / 2) * cell];
  }

  function displayPos(u, now) {
    const q = anims[u.id];
    if (q && q.length) {
      const a = q[0];
      if (now < a.t0) return a.pts[0];
      const p = Math.min(1, (now - a.t0) / a.dur);
      if (p >= 1) { q.shift(); const last = a.pts[a.pts.length - 1]; pos[u.id] = { x: last.x, y: last.y }; if (q.length && q[0].t0 !== Infinity) q[0].t0 = Math.max(q[0].t0, now); return pos[u.id]; }
      const f = p * (a.pts.length - 1), i = Math.floor(f), k = f - i;
      const p0 = a.pts[i], p1 = a.pts[Math.min(i + 1, a.pts.length - 1)];
      return { x: p0.x + (p1.x - p0.x) * k, y: p0.y + (p1.y - p0.y) * k };
    }
    pos[u.id] = { x: u.x, y: u.y };
    return pos[u.id];
  }

  // ---------------------------------------------------------------- draw
  function drawToken(u, now) {
    const p = displayPos(u, now);
    let x = (p.x + u.size / 2) * cell, y = (p.y + u.size / 2) * cell;
    const r = cell * u.size * .42;
    const shake = effects.find(e => e.k === 'shake' && e.id === u.id && now - e.t0 < e.dur);
    if (shake) x += Math.sin(now / 18) * cell * .06;
    const dying = effects.find(e => e.k === 'death' && e.id === u.id);
    let alpha = 1;
    if (u.gone) { if (!dying || now - dying.t0 > dying.dur) return; alpha = 1 - (now - dying.t0) / dying.dur; }
    ctx.globalAlpha = alpha;
    // turn glow
    if (u.id === view.turn && !view.over) {
      const g = ctx.createRadialGradient(x, y, r * .8, x, y, r * 1.6);
      g.addColorStop(0, 'rgba(240,138,60,.65)'); g.addColorStop(1, 'rgba(240,138,60,0)');
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r * 1.6, 0, 7); ctx.fill();
    }
    // shadow
    ctx.fillStyle = 'rgba(0,0,0,.45)'; ctx.beginPath(); ctx.ellipse(x + 2, y + r * .75, r * .9, r * .35, 0, 0, 7); ctx.fill();
    if (u.flies) y -= cell * .12 + Math.sin(now / 400 + x) * cell * .04;
    // body
    if (u.object) {
      ctx.fillStyle = '#2a2220'; ctx.fillRect(x - r * .7, y - r * .3, r * 1.4, r * .9);
      const fl = .8 + .2 * Math.sin(now / 90 + x);
      const g = ctx.createRadialGradient(x, y - r * .4, 2, x, y - r * .4, r * 1.3);
      g.addColorStop(0, `rgba(255,200,90,${fl})`); g.addColorStop(1, 'rgba(255,80,20,0)');
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y - r * .4, r * 1.3, 0, 7); ctx.fill();
    } else if (u.hero && portraits[u.heroId]) {
      ctx.save(); ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.clip();
      ctx.drawImage(portraits[u.heroId], x - r, y - r, r * 2, r * 2);
      ctx.restore();
    } else {
      const g = ctx.createRadialGradient(x - r * .3, y - r * .3, 2, x, y, r);
      g.addColorStop(0, EW.shade(u.color, 50)); g.addColorStop(1, EW.shade(u.color, -50));
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fill();
      ctx.font = `${Math.round(r * 1.05)}px serif`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText(u.glyph, x, y + r * .06);
    }
    // ring: side colour
    ctx.lineWidth = Math.max(2, cell * .07);
    ctx.strokeStyle = u.side === 'party' ? (u.ally ? '#e6c068' : '#7dd3b0') : '#e05a5a';
    ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.stroke();
    // HP arc
    if (!u.object || u.hp < u.maxHp) {
      const frac = Math.max(0, u.hp) / u.maxHp;
      ctx.lineWidth = Math.max(2, cell * .08);
      ctx.strokeStyle = frac > .5 ? '#6ee07a' : frac > .25 ? '#f0c040' : '#ff5a5a';
      ctx.beginPath(); ctx.arc(x, y, r + ctx.lineWidth * 1.2, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * frac); ctx.stroke();
    }
    // states
    if (u.down && !u.gone) { ctx.fillStyle = 'rgba(20,10,10,.55)'; ctx.beginPath(); ctx.arc(x, y, r, 0, 7); ctx.fill(); ctx.font = `${Math.round(r)}px serif`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText(u.dead ? '☠' : u.stable ? '💤' : '💔', x, y); }
    if (u.conds.includes('charmed')) { ctx.font = `${Math.round(r * .6)}px serif`; ctx.fillText('💗', x + r * .8, y - r * .8); }
    if (u.conds.includes('asleep')) { ctx.font = `${Math.round(r * .6)}px serif`; ctx.fillText('💤', x + r * .8, y - r * .8); }
    if (u.conds.includes('raging')) { ctx.strokeStyle = 'rgba(255,60,40,.8)'; ctx.lineWidth = 2; ctx.beginPath(); ctx.arc(x, y, r * 1.18, 0, 7); ctx.stroke(); }
    if (u.conds.includes('blessed')) { ctx.strokeStyle = 'rgba(255,230,140,.8)'; ctx.setLineDash([3, 4]); ctx.lineWidth = 2; ctx.beginPath(); ctx.arc(x, y, r * 1.25, 0, 7); ctx.stroke(); ctx.setLineDash([]); }
    if (u.conds.includes('prone')) { ctx.font = `${Math.round(r * .5)}px sans-serif`; ctx.fillStyle = '#fff'; ctx.fillText('⤓', x - r * .85, y - r * .8); }
    if (u.conds.includes('outlined')) { ctx.strokeStyle = `rgba(200,120,255,${.5 + .3 * Math.sin(now / 200)})`; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(x, y, r * 1.1, 0, 7); ctx.stroke(); }
    if (u.conc) { ctx.fillStyle = '#9fe3ff'; ctx.beginPath(); ctx.arc(x - r * .8, y + r * .8, cell * .08, 0, 7); ctx.fill(); }
    ctx.globalAlpha = 1;
  }

  function drawEffects(now) {
    effects = effects.filter(e => now - e.t0 < e.dur + 50);
    for (const e of effects) {
      if (now < e.t0) continue;
      const p = Math.min(1, (now - e.t0) / e.dur);
      switch (e.k) {
        case 'shot': case 'orb': case 'beam': case 'bolt': {
          const [x1, y1] = centre(e.from), [x2, y2] = centre(e.to);
          let tx = x2, ty = y2;
          if (!e.hit) { tx += cell * .6; ty -= cell * .5; }
          if (e.k === 'beam' || e.k === 'bolt') {
            ctx.strokeStyle = e.col; ctx.lineWidth = cell * (e.k === 'beam' ? .14 : .08) * (1 - p) + 1; ctx.globalAlpha = 1 - p * .6;
            ctx.beginPath(); ctx.moveTo(x1, y1);
            if (e.k === 'bolt') for (let i = 1; i < 6; i++) ctx.lineTo(x1 + (tx - x1) * i / 6 + (Math.random() - .5) * cell * .3, y1 + (ty - y1) * i / 6 + (Math.random() - .5) * cell * .3);
            ctx.lineTo(tx, ty); ctx.stroke(); ctx.globalAlpha = 1;
          } else {
            const x = x1 + (tx - x1) * p, y = y1 + (ty - y1) * p;
            const rr = e.k === 'orb' ? cell * .2 : cell * .08;
            const g = ctx.createRadialGradient(x, y, 1, x, y, rr * 2.5);
            g.addColorStop(0, '#fff'); g.addColorStop(.3, e.col); g.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, rr * 2.5, 0, 7); ctx.fill();
            if (e.k === 'shot') { ctx.strokeStyle = e.col; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(x - (tx - x1) * .08, y - (ty - y1) * .08); ctx.stroke(); }
          }
          if (p > .85 && e.hit) { const [hx, hy] = centre(e.to); ctx.fillStyle = e.col; ctx.globalAlpha = (1 - p) * 4; ctx.beginPath(); ctx.arc(hx, hy, cell * .5, 0, 7); ctx.fill(); ctx.globalAlpha = 1; }
          break;
        }
        case 'slash': {
          const [x2, y2] = centre(e.to);
          ctx.strokeStyle = e.hit ? '#fff' : 'rgba(255,255,255,.4)'; ctx.lineWidth = cell * .07; ctx.globalAlpha = 1 - p;
          ctx.beginPath(); ctx.arc(x2, y2, cell * .5, -2.4 + p * 2, -1 + p * 2); ctx.stroke(); ctx.globalAlpha = 1;
          break;
        }
        case 'burst': { const [x, y] = centre(e.to); ctx.strokeStyle = e.col; ctx.globalAlpha = 1 - p; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(x, y, cell * (.4 + p), 0, 7); ctx.stroke(); ctx.globalAlpha = 1; break; }
        case 'area': {
          for (const [x, y] of e.cells) {
            const a = Math.sin(p * Math.PI) * .7;
            ctx.fillStyle = e.col; ctx.globalAlpha = a * (.6 + .4 * Math.random());
            ctx.fillRect(x * cell, y * cell, cell, cell);
          }
          ctx.globalAlpha = 1;
          break;
        }
        case 'erupt': {
          for (const [x, y] of e.cells) {
            const hgt = Math.sin(p * Math.PI) * cell * 1.6;
            const g = ctx.createLinearGradient(0, y * cell + cell, 0, y * cell + cell - hgt);
            g.addColorStop(0, 'rgba(255,220,120,.95)'); g.addColorStop(1, 'rgba(255,60,10,0)');
            ctx.fillStyle = g; ctx.fillRect(x * cell + cell * .1, y * cell + cell - hgt, cell * .8, hgt);
          }
          break;
        }
        case 'shield': { const [x, y] = centre(e.id); ctx.strokeStyle = `rgba(143,211,255,${1 - p})`; ctx.lineWidth = 4; ctx.beginPath(); for (let i = 0; i <= 6; i++) { const a = i / 6 * Math.PI * 2; const rr = cell * .65; i ? ctx.lineTo(x + Math.cos(a) * rr, y + Math.sin(a) * rr) : ctx.moveTo(x + Math.cos(a) * rr, y + Math.sin(a) * rr); } ctx.stroke(); break; }
        case 'mist': { const x = (e.at[0] + .5) * cell, y = (e.at[1] + .5) * cell; ctx.fillStyle = e.col || 'rgba(220,230,255,.7)'; ctx.globalAlpha = 1 - p; for (let i = 0; i < 6; i++) { ctx.beginPath(); ctx.arc(x + Math.cos(i) * cell * p * .6, y + Math.sin(i * 2) * cell * p * .5, cell * .25, 0, 7); ctx.fill(); } ctx.globalAlpha = 1; break; }
      }
    }
  }

  function drawFloaters(now) {
    floaters = floaters.filter(f => now - f.t0 < 1300);
    for (const f of floaters) {
      const u = byId(f.id); if (!u) continue;
      const p = (now - f.t0) / 1300;
      const [x, y] = centre(f.id);
      ctx.globalAlpha = 1 - p * p;
      ctx.font = `800 ${Math.round(cell * (f.big ? .62 : .45))}px "Alegreya Sans", sans-serif`;
      ctx.textAlign = 'center'; ctx.lineWidth = 4; ctx.strokeStyle = '#000';
      ctx.strokeText(f.text, x, y - cell * .5 - p * cell * .9);
      ctx.fillStyle = f.col; ctx.fillText(f.text, x, y - cell * .5 - p * cell * .9);
      ctx.globalAlpha = 1;
    }
  }

  function drawOverlays(now) {
    const me = B.current();
    const mine = B.myTurn();
    // telegraphed eruptions
    for (const [x, y] of view.warnings || []) {
      const a = .35 + .25 * Math.sin(now / 150);
      ctx.fillStyle = `rgba(255,90,30,${a})`; ctx.fillRect(x * cell, y * cell, cell, cell);
      ctx.strokeStyle = 'rgba(255,200,120,.9)'; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(x * cell + cell * .2, y * cell + cell * .3); ctx.lineTo(x * cell + cell * .5, y * cell + cell * .55); ctx.lineTo(x * cell + cell * .4, y * cell + cell * .8); ctx.moveTo(x * cell + cell * .5, y * cell + cell * .55); ctx.lineTo(x * cell + cell * .8, y * cell + cell * .45); ctx.stroke();
    }
    // lava/fire tiles animate
    for (let y = 0; y < view.h; y++) for (let x = 0; x < view.w; x++) {
      const t = view.tiles[y][x];
      if (t === 'L') {
        const fl = .5 + .5 * Math.sin(now / 300 + x * 1.7 + y);
        if (view.backdrop === 'camp') {
          ctx.fillStyle = '#3a2210'; ctx.fillRect(x * cell + cell * .15, y * cell + cell * .55, cell * .7, cell * .15);
          const g = ctx.createRadialGradient(x * cell + cell / 2, y * cell + cell * .55, 1, x * cell + cell / 2, y * cell + cell * .5, cell * .9);
          g.addColorStop(0, `rgba(255,220,120,${.9})`); g.addColorStop(.4, `rgba(255,120,40,${.6 + fl * .3})`); g.addColorStop(1, 'rgba(255,60,10,0)');
          ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x * cell + cell / 2, y * cell + cell * .5, cell * .9, 0, 7); ctx.fill();
        } else {
          const g = ctx.createLinearGradient(x * cell, y * cell, x * cell + cell, y * cell + cell);
          g.addColorStop(0, `rgb(255,${120 + fl * 60},40)`); g.addColorStop(1, `rgb(${200 + fl * 40},50,15)`);
          ctx.fillStyle = g; ctx.fillRect(x * cell, y * cell, cell, cell);
          ctx.fillStyle = `rgba(255,240,180,${fl * .4})`; ctx.beginPath(); ctx.arc(x * cell + cell * (.3 + .4 * Math.sin(now / 900 + x)), y * cell + cell * .5, cell * .12, 0, 7); ctx.fill();
        }
      } else if (t === '~') {
        ctx.strokeStyle = 'rgba(200,230,255,.18)'; ctx.lineWidth = 1;
        const o = (now / 40 + x * 13 + y * 7) % cell;
        ctx.beginPath(); ctx.moveTo(x * cell + o * .5, y * cell + cell * .4); ctx.lineTo(x * cell + o * .5 + cell * .3, y * cell + cell * .4); ctx.stroke();
      }
    }
    if (!mine || !me) return;
    // movement range
    if (!mode && view.reach) {
      ctx.fillStyle = 'rgba(125,211,176,.13)';
      for (const k in view.reach) { const [x, y] = k.split(',').map(Number); ctx.fillRect(x * cell + 1, y * cell + 1, cell - 2, cell - 2); }
      if (hover && view.reach[hover.x + ',' + hover.y] != null) {
        ctx.strokeStyle = '#7dd3b0'; ctx.lineWidth = 2; ctx.strokeRect(hover.x * cell + 2, hover.y * cell + 2, cell - 4, cell - 4);
        ctx.fillStyle = '#e8fff6'; ctx.font = `700 ${Math.round(cell * .28)}px sans-serif`; ctx.textAlign = 'center';
        ctx.fillText(view.reach[hover.x + ',' + hover.y] + ' ft', hover.x * cell + cell / 2, hover.y * cell + cell * .9);
      }
    }
    // targeting
    if (mode && hover) {
      let cells = null;
      if (mode.kind === 'point') {
        if (mode.shape === 'cone') cells = coneCells(me, hover.x, hover.y, mode.size);
        else if (mode.shape === 'cube') cells = cubeCells(me, hover.x, hover.y, mode.size);
        else if (mode.shape === 'sphere') cells = sphereCells(hover.x, hover.y, mode.size);
        else if (mode.shape === 'tele') cells = [[hover.x, hover.y]];
        const col = mode.spell ? (DTYPE[mode.spell.damageType] || '#c9a2ff') : '#ff9a4a';
        ctx.fillStyle = col; ctx.globalAlpha = .35;
        for (const [x, y] of cells) ctx.fillRect(x * cell, y * cell, cell, cell);
        ctx.globalAlpha = 1;
        for (const [x, y] of cells) { const u = unitAt(x, y); if (u && u.id !== me.id) { const [cx, cy] = centre(u.id); ctx.strokeStyle = u.side === me.side ? '#ffd27a' : '#ff5a5a'; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(cx, cy, cell * .55 * u.size, 0, 7); ctx.stroke(); } }
        if (mode.range) { ctx.strokeStyle = 'rgba(255,255,255,.25)'; ctx.setLineDash([6, 6]); ctx.beginPath(); const [cx, cy] = centre(me.id); ctx.arc(cx, cy, (mode.range / 5 + .5) * cell, 0, 7); ctx.stroke(); ctx.setLineDash([]); }
      } else {
        const u = unitAt(hover.x, hover.y);
        if (u) { const [cx, cy] = centre(u.id); ctx.strokeStyle = '#ffd27a'; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(cx, cy, cell * .58 * u.size, 0, 7); ctx.stroke(); }
      }
    }
    if (mode && mode.targets) for (const id of mode.targets) { const [cx, cy] = centre(id); ctx.fillStyle = '#ffd27a'; ctx.font = `800 ${Math.round(cell * .35)}px sans-serif`; ctx.textAlign = 'center'; ctx.fillText('◎ ' + mode.targets.filter(t => t === id).length, cx, cy - cell * .55); }
  }

  function frame(now) {
    requestAnimationFrame(frame);
    if (!view || document.getElementById('battle').classList.contains('hidden')) return;
    // fx timeline
    if (fxQueue.length && now >= fxBusyUntil) {
      let budget = fxQueue.length > 12 ? 0.35 : 1;
      while (fxQueue.length && now >= fxBusyUntil) {
        const e = fxQueue.shift();
        const d = startFx(e, now) * budget;
        fxBusyUntil = now + d;
      }
    }
    paintTerrain();
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, cv.width, cv.height);
    ctx.drawImage(terrain, 0, 0);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    drawOverlays(now);
    const sorted = view.units.slice().sort((a, b) => (a.down ? -1 : 0) - (b.down ? -1 : 0) || a.y - b.y);
    for (const u of sorted) drawToken(u, now);
    drawEffects(now);
    drawFloaters(now);
  }
  requestAnimationFrame(frame);
  B.layout = layout;
})();
