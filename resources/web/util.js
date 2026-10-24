/* Shared helpers: API calls, DOM building, text formatting, portraits. */
const EW = window.EW = {};

EW.me = { id: localStorage.getItem('ew.pid') || '', name: localStorage.getItem('ew.name') || '', host: false };

EW.api = async function (path, body, opts = {}) {
  const init = { method: body === undefined ? 'GET' : 'POST', headers: { 'X-Player': EW.me.id } };
  if (body !== undefined) {
    if (opts.raw) { init.body = body; init.headers['Content-Type'] = 'text/plain'; }
    else { init.body = JSON.stringify(body); init.headers['Content-Type'] = 'application/json'; }
  }
  const res = await fetch(path, init);
  let data;
  try { data = await res.json(); } catch { data = { ok: false, error: 'Bad response from server' }; }
  if (data && data.ok === false && data.error && !opts.quiet) EW.toast(data.error);
  return data;
};

/** h('div.class#id', {attrs}, children...) */
EW.h = function (sel, attrs, ...kids) {
  const m = sel.match(/^([a-z0-9]+)?((?:[.#][\w-]+)*)$/i);
  const el = document.createElement(m[1] || 'div');
  (m[2].match(/[.#][\w-]+/g) || []).forEach(t => t[0] === '.' ? el.classList.add(t.slice(1)) : (el.id = t.slice(1)));
  if (attrs && (typeof attrs !== 'object' || attrs instanceof Node || Array.isArray(attrs))) { kids.unshift(attrs); attrs = null; }
  if (attrs) for (const [k, v] of Object.entries(attrs)) {
    if (v == null || v === false) continue;
    if (['disabled', 'checked', 'selected', 'hidden', 'readonly', 'multiple'].includes(k)) { if (v) el.setAttribute(k, ''); continue; }
    if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (k === 'html') el.innerHTML = v;
    else if (k === 'style' && typeof v === 'object') Object.assign(el.style, v);
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const k of kids.flat(Infinity)) {
    if (k == null || k === false) continue;
    el.append(k instanceof Node ? k : document.createTextNode(String(k)));
  }
  return el;
};

EW.esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/** Minimal markdown: **bold**, *italic*, paragraphs, bullet lines. */
EW.md = function (text) {
  return String(text || '').split(/\n\n+/).map(p => {
    let t = EW.esc(p).replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>').replace(/\*(.+?)\*/g, '<em>$1</em>').replace(/\n/g, '<br>');
    return `<p>${t}</p>`;
  }).join('');
};

EW.toast = function (msg, ok) {
  const t = EW.h('div.toast' + (ok ? '.ok' : ''), msg);
  document.getElementById('toasts').append(t);
  setTimeout(() => t.remove(), 3800);
};

EW.sign = n => (n >= 0 ? '+' : '') + n;

EW.modal = function (content, onClose) {
  const m = document.getElementById('modal'), b = document.getElementById('modalBody');
  b.innerHTML = '';
  b.append(content);
  m.classList.remove('hidden');
  EW._modalClose = onClose;
};
EW.closeModal = function () {
  document.getElementById('modal').classList.add('hidden');
  if (EW._modalClose) { const f = EW._modalClose; EW._modalClose = null; f(); }
};

/* ---------------------------------------------------------------- portraits */
function shade(hex, amt) {
  const n = parseInt(hex.slice(1), 16);
  let r = (n >> 16) + amt, g = ((n >> 8) & 255) + amt, b = (n & 255) + amt;
  r = Math.max(0, Math.min(255, r)); g = Math.max(0, Math.min(255, g)); b = Math.max(0, Math.min(255, b));
  return '#' + ((1 << 24) | (r << 16) | (g << 8) | b).toString(16).slice(1);
}
EW.shade = shade;

/** Paint a hero bust: species features, hair, eyes, class colour frame and emblem. */
EW.portrait = function (canvas, hero, size = 96) {
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  canvas.width = size * dpr; canvas.height = size * dpr;
  canvas.style.width = size + 'px'; canvas.style.height = size + 'px';
  const c = canvas.getContext('2d');
  c.scale(dpr, dpr);
  const s = size / 100;
  c.save(); c.scale(s, s);
  // frame & background
  const bg = c.createRadialGradient(50, 40, 10, 50, 50, 60);
  bg.addColorStop(0, shade(hero.color || '#e67e22', 30)); bg.addColorStop(1, shade(hero.color || '#e67e22', -70));
  c.beginPath(); c.arc(50, 50, 48, 0, Math.PI * 2); c.fillStyle = bg; c.fill();
  c.save(); c.clip();
  const skin = hero.skin || '#d8a47f', hair = hero.hair || '#6b3e26', eyes = hero.eyes || '#3fa34d';
  const sp = hero.species || 'human';
  // shoulders / garment
  c.fillStyle = shade(hero.color || '#7a5', -40);
  c.beginPath(); c.ellipse(50, 104, 40, 30, 0, 0, Math.PI * 2); c.fill();
  c.fillStyle = shade(hero.color || '#7a5', -10);
  c.beginPath(); c.moveTo(38, 80); c.lineTo(50, 96); c.lineTo(62, 80); c.closePath(); c.fill();
  // neck
  c.fillStyle = shade(skin, -25); c.fillRect(43, 64, 14, 16);
  // back hair
  c.fillStyle = hair;
  if (!['dwarf', 'goliath', 'dragonborn'].includes(sp)) { c.beginPath(); c.ellipse(50, 50, 25, 30, 0, 0, Math.PI * 2); c.fill(); }
  // ears
  c.fillStyle = shade(skin, -10);
  if (sp === 'elf') {
    c.beginPath(); c.moveTo(29, 46); c.lineTo(14, 30); c.lineTo(31, 56); c.fill();
    c.beginPath(); c.moveTo(71, 46); c.lineTo(86, 30); c.lineTo(69, 56); c.fill();
  } else if (sp !== 'dragonborn') {
    c.beginPath(); c.ellipse(29, 50, 4, 7, 0, 0, Math.PI * 2); c.fill();
    c.beginPath(); c.ellipse(71, 50, 4, 7, 0, 0, Math.PI * 2); c.fill();
  }
  // head
  const headW = sp === 'goliath' || sp === 'dwarf' ? 23 : sp === 'halfling' || sp === 'gnome' ? 22 : 21;
  c.fillStyle = skin;
  c.beginPath();
  if (sp === 'dragonborn') { c.ellipse(50, 47, 21, 24, 0, 0, Math.PI * 2); c.fill(); c.beginPath(); c.ellipse(50, 60, 14, 9, 0, 0, Math.PI * 2); }
  else c.ellipse(50, 48, headW, 25, 0, 0, Math.PI * 2);
  c.fill();
  // horns
  if (sp === 'tiefling' || sp === 'dragonborn') {
    c.fillStyle = sp === 'tiefling' ? '#3a2a33' : shade(skin, -45);
    c.beginPath(); c.moveTo(36, 30); c.quadraticCurveTo(22, 14, 30, 4); c.quadraticCurveTo(32, 18, 42, 26); c.fill();
    c.beginPath(); c.moveTo(64, 30); c.quadraticCurveTo(78, 14, 70, 4); c.quadraticCurveTo(68, 18, 58, 26); c.fill();
  }
  // goliath markings
  if (sp === 'goliath') { c.strokeStyle = shade(skin, -55); c.lineWidth = 2; c.beginPath(); c.moveTo(35, 40); c.lineTo(42, 48); c.moveTo(65, 40); c.lineTo(58, 48); c.moveTo(50, 26); c.lineTo(50, 36); c.stroke(); }
  // front hair / fringe
  c.fillStyle = hair;
  if (sp !== 'dragonborn' && sp !== 'goliath') {
    c.beginPath(); c.ellipse(50, 30, 23, 12, 0, Math.PI, 0); c.fill();
    c.beginPath(); c.moveTo(27, 32); c.quadraticCurveTo(40, 42, 50, 28); c.quadraticCurveTo(58, 40, 73, 32); c.lineTo(73, 26); c.lineTo(27, 26); c.fill();
    if (!['dwarf'].includes(sp) && (hero.pronouns || '').startsWith('she')) {
      c.beginPath(); c.moveTo(27, 34); c.quadraticCurveTo(20, 60, 30, 78); c.lineTo(34, 60); c.fill();
      c.beginPath(); c.moveTo(73, 34); c.quadraticCurveTo(80, 60, 70, 78); c.lineTo(66, 60); c.fill();
    }
  }
  // eyes
  for (const ex of [41, 59]) {
    c.fillStyle = '#f7f1e6'; c.beginPath(); c.ellipse(ex, 49, 4.6, 3.2, 0, 0, Math.PI * 2); c.fill();
    c.fillStyle = eyes; c.beginPath(); c.arc(ex, 49, 2.4, 0, Math.PI * 2); c.fill();
    c.fillStyle = '#111'; c.beginPath(); c.arc(ex, 49, 1.1, 0, Math.PI * 2); c.fill();
  }
  if (sp === 'elf' || hero.class === 'wizard') { c.fillStyle = eyes + '55'; c.beginPath(); c.arc(41, 49, 6, 0, 7); c.arc(59, 49, 6, 0, 7); c.fill(); }
  // brows, nose, mouth
  c.strokeStyle = shade(hair, -20); c.lineWidth = 1.6;
  c.beginPath(); c.moveTo(36, 43); c.lineTo(45, 42); c.moveTo(55, 42); c.lineTo(64, 43); c.stroke();
  c.strokeStyle = shade(skin, -45); c.lineWidth = 1.2;
  c.beginPath(); c.moveTo(50, 50); c.lineTo(48, 57); c.lineTo(51, 58); c.stroke();
  c.beginPath(); c.moveTo(44, 63); c.quadraticCurveTo(50, 67, 56, 63); c.stroke();
  // freckles for our elf wizard (and anyone with copper hair)
  if (/^#[cd]/.test(hair)) { c.fillStyle = shade(skin, -40); for (const [fx, fy] of [[40, 56], [43, 58], [57, 56], [60, 58], [38, 58], [62, 58]]) { c.beginPath(); c.arc(fx, fy, .7, 0, 7); c.fill(); } }
  // beards
  if (sp === 'dwarf' || (hero.pronouns || '').startsWith('he') && hero.class === 'fighter') {
    c.fillStyle = hair; c.beginPath(); c.moveTo(30, 52); c.quadraticCurveTo(50, 98, 70, 52); c.quadraticCurveTo(50, 72, 30, 52); c.fill();
  }
  c.restore();
  // rim
  c.lineWidth = 3; c.strokeStyle = shade(hero.color || '#e67e22', 40);
  c.beginPath(); c.arc(50, 50, 47, 0, Math.PI * 2); c.stroke();
  // emblem badge
  if (hero.emblem) {
    c.fillStyle = '#14151f'; c.beginPath(); c.arc(82, 82, 13, 0, 7); c.fill();
    c.strokeStyle = shade(hero.color || '#e67e22', 40); c.lineWidth = 2; c.stroke();
    c.font = '14px serif'; c.textAlign = 'center'; c.textBaseline = 'middle'; c.fillText(hero.emblem, 82, 83);
  }
  c.restore();
};

/** NPC medallion: coloured disc + glyph. */
EW.medallion = function (canvas, color, glyph, size = 76) {
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  canvas.width = size * dpr; canvas.height = size * dpr;
  canvas.style.width = size + 'px'; canvas.style.height = size + 'px';
  const c = canvas.getContext('2d'); c.scale(dpr, dpr);
  const r = size / 2;
  const g = c.createRadialGradient(r * .8, r * .7, 2, r, r, r);
  g.addColorStop(0, shade(color || '#888', 60)); g.addColorStop(1, shade(color || '#888', -50));
  c.fillStyle = g; c.beginPath(); c.arc(r, r, r - 2, 0, 7); c.fill();
  c.strokeStyle = '#6d5236'; c.lineWidth = 3; c.stroke();
  c.strokeStyle = '#e8d3a5'; c.lineWidth = 1; c.beginPath(); c.arc(r, r, r - 6, 0, 7); c.stroke();
  c.font = `${Math.round(size * .46)}px serif`; c.textAlign = 'center'; c.textBaseline = 'middle';
  c.fillText(glyph || '?', r, r + 2);
};

EW.condClass = c => ['prone', 'poisoned', 'frightened', 'grappled', 'burning', 'paralyzed', 'asleep', 'slowed', 'sapped', 'outlined', 'marked', 'charmed', 'unconscious'].includes(c) ? 'bad'
  : ['blessed', 'shield', 'raging', 'dodging', 'hidden', 'heroism', 'shield_of_faith', 'spiritual_weapon', 'sacred_weapon'].includes(c) ? 'good' : '';
EW.condLabel = c => c.replace(/_/g, ' ');
