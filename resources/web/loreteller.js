/* The Lore-teller: the person actually telling you this story, sitting by a candle with a big book.
   - Drawn live on a canvas: breathes, blinks, looks around, speaks (lips follow the narrator's voice), gestures.
   - Reacts to the table: hushes a rowdy tavern, flinches at thunder, grins at a good roll, goes grim when a fight is near.
   - Runs the scene's soundscape and "asides": little moments where the world makes a noise and the teller answers it.
   EW.tale is driven by app.js: scene(sc), update(state), fx(f), combat(on). */
(function () {
  const T = EW.tale = {};
  const V = EW.voice, A = EW.audio;
  const rand = a => a[Math.floor(Math.random() * a.length)];
  const hash = s => { let h = 2166136261; for (const ch of String(s)) h = Math.imul(h ^ ch.charCodeAt(0), 16777619); return h >>> 0; };
  const pick = (a, seed) => a[seed % a.length];

  // ======================================================================== lines
  const LINES = {
    hello: ['Pull up a stool, friends. Let me tell you a tale…', 'Ah — you came. Good. Sit, sit. This one starts on a rainy night…', 'Candle\'s lit, the book is open. Shall we begin?'],
    shush: ['Oi! Pipe down back there — I\'m telling a story!', 'Settle, settle! …Thank you. Now, where was I?', 'Gerrold, put the bench DOWN. …Right. Our heroes.', 'If you lot are quite done cheering, there\'s a tale to get on with!', 'Shhh! Shh. The good part\'s coming.'],
    barkeep: ['{npc} bangs a tankard on the bar: “Settle down, you lot, or you\'re all out in the rain!”', '{npc} glares across the room until the noise dies. “Thank you.”'],
    fire: ['Mind the sparks — they bite.', 'Ooh, the fire likes that. Fires always know.', 'Somebody throw another log on. …No, not THAT one!'],
    thunder: ['…Just thunder. Probably.', 'Ha! Even the sky agrees with me.', 'Close the shutters, would you? This one\'s going to get worse.'],
    owl: ['Only an owl. …Almost certainly only an owl.', 'The owls are watching tonight. They always are.'],
    gull: ['Blasted gulls. Every tale by the sea has gulls.', 'Shoo! Go on! …Where was I?'],
    rumble: ['Hear that? Something down there is listening, too.', 'The deep places grumble. Best not keep them waiting.'],
    gust: ['Brr! Who left the door open?', 'Keep that candle lit, friends. You\'ll want the light for what comes next.'],
    foreshadow: ['Steel will be drawn before this is done…', 'I can smell a fight coming. Can you?', 'Ready your dice, friends. This could turn ugly.', 'Careful now. One wrong word and blades come out.'],
    lean: ['Oh, you\'re really going to do it…', 'Brave. Or foolish. The best tales are both.', 'Here we go…'],
    battle: ['And so it begins!', 'Roll for initiative — and may the dice love you.', 'Blades out! Now we\'ll see what you\'re made of.'],
    crit: ['Oh-ho! What a blow!', 'Did you SEE that?', 'They\'ll sing about that one.'],
    down: ['No, no, no — get up!', 'Ooh. That\'s going to leave a mark.', 'Someone help them!'],
    victory: ['Ha! I knew you had it in you!', 'And THAT is how it\'s done.', 'Write that down, somebody!'],
    defeat: ['…That\'s not how I remember it ending. Let\'s try that again.', 'Hmm. Perhaps I told that part wrong.'],
    success: ['Ha! Well done.', 'Lovely.', 'Just as I said it would happen.'],
    fail: ['Ooh…', 'Ah. Hm.', 'That\'s one way to do it.'],
    level: ['Look at you now!', 'Growing up so fast…', 'Stronger. You\'ll need it.'],
    gold: ['Coin! The best kind of plot.', 'Don\'t spend it all at once.'],
    loot: ['Ooh, keep that safe.', 'Now THAT might come in handy.'],
    hurt: ['Ouch!', 'Careful!'],
    poke: ['Yes? I\'m in the middle of a story, you know.', 'Patience. The best bits take their time.', 'Another stool? Help yourself.', 'Have I told you about the time I met a dragon? …Later.'],
  };

  // ======================================================================== DOM
  const host = document.getElementById('loreteller');
  const cv = document.createElement('canvas');
  const cap = document.createElement('div'); cap.className = 'lt-caption';
  const plate = document.createElement('div'); plate.className = 'lt-plate';
  const nameEl = document.createElement('span'); nameEl.className = 'lt-name';
  const btn = document.createElement('button'); btn.className = 'lt-voice'; btn.type = 'button';
  plate.append(nameEl, btn);
  host.append(cv, plate, cap);
  const ctx = cv.getContext('2d');
  const W = 240, H = 200;
  let dpr = 1;
  function size() { dpr = Math.min(2, window.devicePixelRatio || 1); cv.width = W * dpr; cv.height = H * dpr; }
  size();
  function refreshButton() {
    btn.textContent = V.cfg.narrator ? '🔊' : '🔈';
    btn.title = V.cfg.narrator ? 'The lore-teller reads aloud (click to stop)' : 'Let the lore-teller read aloud';
  }
  btn.onclick = e => { e.stopPropagation(); V.cfg.narrator = !V.cfg.narrator; V.save(); if (!V.cfg.narrator) V.stop(); else if (EW.state.scene) V.speakScene(EW.state.scene); refreshButton(); };
  refreshButton();
  cv.onclick = () => { react('sly', 2500); say(rand(LINES.poke)); };
  cv.title = 'The lore-teller';

  // ======================================================================== the figure's state
  const st = {
    gender: 'm', expr: 'idle', exprUntil: 0, gesture: 'rest', gestureUntil: 0, look: 0, lookUntil: 0,
    speaking: false, simulated: 0, wordAt: 0, captionUntil: 0, blinkAt: 0, flash: 0, gutter: 0, tension: 0,
    // eased drawing params
    p: { browTilt: 0, browLift: 0, smile: .25, eyeOpen: 1, mouth: 0, round: 0, hx: 150, hy: 172, look: 0, nod: 0, glowR: 1 },
  };
  const EXPR = {
    idle:    { browTilt: .3, browLift: .5, smile: .3, eyeOpen: 1, round: 0 },
    grim:    { browTilt: -1, browLift: -1.5, smile: -.35, eyeOpen: .7, round: 0 },
    happy:   { browTilt: .4, browLift: 2, smile: .9, eyeOpen: .55, round: 0 },
    wince:   { browTilt: 1, browLift: 1, smile: -.5, eyeOpen: .35, round: 0 },
    awe:     { browTilt: .3, browLift: 4, smile: 0, eyeOpen: 1.35, round: 1 },
    sly:     { browTilt: -.4, browLift: 1, smile: .55, eyeOpen: .75, round: 0 },
    stern:   { browTilt: -.8, browLift: -1, smile: -.15, eyeOpen: .85, round: 0 },
  };
  const HANDS = { rest: [150, 172], raise: [172, 112], shush: [124, 108], point: [200, 118], page: [104, 168] };

  function react(expr, ms) { st.expr = expr; st.exprUntil = performance.now() + (ms || 2500); }
  function gesture(g, ms) { st.gesture = g; st.gestureUntil = performance.now() + (ms || 1800); }
  function lookAt(x, ms) { st.look = x; st.lookUntil = performance.now() + (ms || 2000); }

  let lastLine = 0;
  /** The teller says something: voiced when the voice is free, always captioned. */
  function say(text, opts) {
    opts = opts || {};
    const now = performance.now();
    if (!opts.force && now - lastLine < 6000) return false;
    lastLine = now;
    caption(text);
    const voiced = V.aside(text);
    if (!voiced) { st.simulated = now + Math.min(4200, 600 + text.length * 48); }
    return true;
  }
  T.say = say;
  let capTimer = 0;
  function caption(text, keep) {
    cap.textContent = text;
    cap.classList.add('on');
    clearTimeout(capTimer);
    if (!keep) capTimer = setTimeout(() => cap.classList.remove("on"), Math.max(5500, text.length * 85));
  }

  // follow the narrator's real voice
  V.listen((kind, item) => {
    const narr = item.role && item.role.key === 'narrator';
    if (kind === 'start') {
      if (narr) {
        st.speaking = true; caption(item.text, true);
        if (/[!?]["”']?$/.test(item.text) && Math.random() < .6) gesture('raise', 1400);
        else if (Math.random() < .15) gesture('page', 1600);
      } else { lookAt(.8, 4000); }
    } else if (kind === 'word') { if (narr) st.wordAt = performance.now(); }
    else if (kind === 'end') {
      if (narr) { st.speaking = false; clearTimeout(capTimer); capTimer = setTimeout(() => cap.classList.remove('on'), 1600); }
    }
  });

  // ======================================================================== drawing
  const ease = (o, k, target, rate) => { o[k] += (target - o[k]) * rate; };
  function roundRect(x, y, w, h, r) { ctx.beginPath(); ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + h, r); ctx.arcTo(x + w, y + h, x, y + h, r); ctx.arcTo(x, y + h, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath(); }

  function draw(ts) {
    requestAnimationFrame(draw);
    if (host.offsetParent === null) return;
    const now = performance.now(), t = ts / 1000, p = st.p;
    if (now > st.exprUntil) st.expr = st.tension ? 'stern' : 'idle';
    if (now > st.gestureUntil) st.gesture = 'rest';
    if (now > st.lookUntil) st.look = st.speaking ? 0 : Math.sin(t * .23) * .35;
    const E = EXPR[st.expr] || EXPR.idle;
    for (const k of ['browTilt', 'browLift', 'smile', 'eyeOpen', 'round']) ease(p, k, E[k], .12);
    const [hx, hy] = HANDS[st.gesture] || HANDS.rest;
    ease(p, 'hx', hx, .14); ease(p, 'hy', hy, .14); ease(p, 'look', st.look, .08);
    // the mouth: real word boundaries when we have them, a believable flap otherwise
    const talking = st.speaking || now < st.simulated;
    let mt = 0;
    if (talking) {
      const sinceWord = (now - st.wordAt) / 1000;
      mt = sinceWord < .35 ? Math.max(.25, 1 - sinceWord * 2.4) * (.7 + .3 * Math.sin(t * 31)) : Math.max(0, Math.sin(t * 13.5) * .5 + Math.sin(t * 7.1) * .4 + .15);
    }
    ease(p, 'mouth', mt, .45);
    ease(p, 'nod', talking ? Math.sin(t * 4.3) * 1.6 : 0, .1);
    if (st.flash > 0) st.flash *= .9;
    if (st.gutter > 0) st.gutter *= .97;
    if (now > st.blinkAt) st.blinkAt = now + 2500 + Math.random() * 4000;
    const blink = Math.max(0, 1 - Math.abs(now - (st.blinkAt - 120)) / 120);

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
    // --- alcove
    const bg = ctx.createLinearGradient(0, 0, 0, H); bg.addColorStop(0, '#120f17'); bg.addColorStop(1, '#1d1620');
    ctx.fillStyle = bg; ctx.fillRect(0, 0, W, H);
    ctx.strokeStyle = '#2c2433'; ctx.lineWidth = 2;
    for (let i = 0; i < 9; i++) { const a = Math.PI + i * Math.PI / 8; ctx.beginPath(); ctx.moveTo(120 + Math.cos(a) * 112, 120 + Math.sin(a) * 112); ctx.lineTo(120 + Math.cos(a) * 128, 120 + Math.sin(a) * 128); ctx.stroke(); }
    ctx.beginPath(); ctx.arc(120, 120, 112, Math.PI, 0); ctx.stroke();
    // --- candle light (flickers, gutters in the wind, goes red when a fight is near)
    const fl = .82 + .1 * Math.sin(t * 9) * Math.sin(t * 13.3) + Math.random() * .05 - st.gutter * .5;
    const cx = 206, cy = 138;
    const col = st.tension ? [255, 120, 70] : [255, 190, 110];
    const g = ctx.createRadialGradient(cx, cy, 4, cx, cy, 190 * fl);
    g.addColorStop(0, `rgba(${col},${.45 * fl})`); g.addColorStop(.5, `rgba(${col},${.12 * fl})`); g.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
    const breathe = Math.sin(t * 1.25) * 1.4;
    const lx = p.look * 3;
    // --- cloak & shoulders
    const cloak = st.gender === 'f' ? '#3f2a4a' : '#2f3d2c', cloakD = st.gender === 'f' ? '#2a1c32' : '#1f2a1d';
    ctx.fillStyle = cloak;
    ctx.beginPath(); ctx.moveTo(18, H); ctx.quadraticCurveTo(30, 140 + breathe, 70, 128 + breathe); ctx.quadraticCurveTo(112, 112 + breathe, 154, 128 + breathe); ctx.quadraticCurveTo(196, 140 + breathe, 208, H); ctx.fill();
    ctx.fillStyle = cloakD; ctx.beginPath(); ctx.moveTo(100, 125 + breathe); ctx.lineTo(112, 168); ctx.lineTo(124, 125 + breathe); ctx.fill();
    ctx.fillStyle = '#c9a24a'; ctx.beginPath(); ctx.arc(112, 136 + breathe, 3.2, 0, 7); ctx.fill(); // clasp
    // --- head group
    ctx.save(); ctx.translate(lx * .6, breathe * .6 + p.nod * .6);
    // hood back
    ctx.fillStyle = cloakD; ctx.beginPath(); ctx.ellipse(112, 82, 50, 54, 0, 0, 7); ctx.fill();
    ctx.fillStyle = cloak; ctx.beginPath(); ctx.moveTo(60, 120); ctx.quadraticCurveTo(52, 28, 112, 26); ctx.quadraticCurveTo(172, 28, 164, 120); ctx.quadraticCurveTo(140, 104, 112, 104); ctx.quadraticCurveTo(84, 104, 60, 120); ctx.fill();
    ctx.fillStyle = '#0e0a10'; ctx.beginPath(); ctx.ellipse(112, 84, 33, 40, 0, 0, 7); ctx.fill();
    // face
    const skin = '#e3bf9a', skinD = '#c99d78';
    ctx.fillStyle = skin; ctx.beginPath(); ctx.ellipse(112, 86, 25, 30, 0, 0, 7); ctx.fill();
    const sh = ctx.createLinearGradient(87, 0, 137, 0); sh.addColorStop(0, 'rgba(60,30,30,.25)'); sh.addColorStop(.7, 'rgba(0,0,0,0)'); sh.addColorStop(1, 'rgba(255,200,120,.15)');
    ctx.fillStyle = sh; ctx.beginPath(); ctx.ellipse(112, 86, 25, 30, 0, 0, 7); ctx.fill();
    if (st.gender === 'f') {
      ctx.fillStyle = '#c8c4cc'; ctx.beginPath(); ctx.moveTo(87, 84); ctx.quadraticCurveTo(88, 56, 112, 55); ctx.quadraticCurveTo(136, 56, 137, 84); ctx.quadraticCurveTo(130, 66, 112, 64); ctx.quadraticCurveTo(94, 66, 87, 84); ctx.fill();
      ctx.fillStyle = 'rgba(220,120,120,.28)'; ctx.beginPath(); ctx.arc(97, 96, 5, 0, 7); ctx.arc(127, 96, 5, 0, 7); ctx.fill();
    } else {
      ctx.fillStyle = '#d9d6d0'; ctx.beginPath(); ctx.moveTo(88, 70); ctx.quadraticCurveTo(112, 58, 136, 70); ctx.lineTo(136, 66); ctx.quadraticCurveTo(112, 52, 88, 66); ctx.fill();
    }
    // eyes
    const eo = Math.max(.08, p.eyeOpen * (1 - blink));
    for (const ex of [101, 123]) {
      ctx.fillStyle = '#f3ece0'; ctx.beginPath(); ctx.ellipse(ex, 84, 5.2, 3.6 * eo, 0, 0, 7); ctx.fill();
      ctx.fillStyle = '#3a2a1a'; ctx.beginPath(); ctx.arc(ex + p.look * 2.2, 84 + (p.round ? -.4 : 0), 2.1 * Math.min(1, eo + .2), 0, 7); ctx.fill();
      ctx.strokeStyle = skinD; ctx.lineWidth = 1; ctx.beginPath(); ctx.ellipse(ex, 84, 5.4, 3.8 * eo, 0, Math.PI, 0); ctx.stroke();
    }
    // spectacles
    ctx.strokeStyle = '#c9a24a'; ctx.lineWidth = 1.3;
    ctx.beginPath(); ctx.arc(101, 84, 7.5, 0, 7); ctx.moveTo(130.5, 84); ctx.arc(123, 84, 7.5, 0, 7); ctx.moveTo(108.5, 83); ctx.quadraticCurveTo(112, 80, 115.5, 83); ctx.stroke();
    ctx.fillStyle = `rgba(255,240,200,${.25 + .2 * fl})`; ctx.beginPath(); ctx.arc(126, 81, 1.6, 0, 7); ctx.fill();
    // brows
    const browCol = st.gender === 'f' ? '#b8b2bc' : '#e8e4dc';
    ctx.strokeStyle = browCol; ctx.lineWidth = st.gender === 'f' ? 2 : 3.2; ctx.lineCap = 'round';
    const by = 74 - p.browLift, tilt = p.browTilt * 2.6;
    ctx.beginPath(); ctx.moveTo(94, by + tilt * .2); ctx.lineTo(107, by - tilt); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(130, by + tilt * .2); ctx.lineTo(117, by - tilt); ctx.stroke();
    // nose
    ctx.strokeStyle = skinD; ctx.lineWidth = 1.6; ctx.beginPath(); ctx.moveTo(112, 86); ctx.quadraticCurveTo(116, 95, 110, 97); ctx.stroke();
    // mouth
    const my = 105, mw = 8 + (p.round ? -3 * p.round : 0), open = p.mouth * 6 + p.round * 3;
    if (st.gender === 'm') {
      // beard behind the mouth
      ctx.fillStyle = '#e4e0d8'; ctx.beginPath(); ctx.moveTo(88, 92); ctx.quadraticCurveTo(84, 126, 100, 140 + p.mouth * 3); ctx.quadraticCurveTo(112, 152 + p.mouth * 4, 124, 140 + p.mouth * 3); ctx.quadraticCurveTo(140, 126, 136, 92); ctx.quadraticCurveTo(128, 112, 112, 112); ctx.quadraticCurveTo(96, 112, 88, 92); ctx.fill();
      ctx.strokeStyle = 'rgba(160,150,140,.6)'; ctx.lineWidth = 1;
      for (let i = 0; i < 6; i++) { ctx.beginPath(); ctx.moveTo(96 + i * 6, 116); ctx.quadraticCurveTo(95 + i * 6.5, 130, 100 + i * 4.5, 142); ctx.stroke(); }
    }
    ctx.fillStyle = '#4a1e1e';
    ctx.beginPath();
    ctx.moveTo(112 - mw, my - p.smile * 2.5);
    ctx.quadraticCurveTo(112, my + p.smile * 4 + open * 1.4, 112 + mw, my - p.smile * 2.5);
    ctx.quadraticCurveTo(112, my + p.smile * 1.5 - open * .2, 112 - mw, my - p.smile * 2.5);
    ctx.fill();
    if (open > 2) { ctx.fillStyle = '#f0e8dc'; ctx.fillRect(108, my + p.smile * .5 - .5, 8, 1.6); }
    if (st.gender === 'm') { // moustache
      ctx.fillStyle = '#ece8e0'; ctx.beginPath(); ctx.moveTo(98, 103 - p.smile); ctx.quadraticCurveTo(105, 96, 112, 100); ctx.quadraticCurveTo(119, 96, 126, 103 - p.smile); ctx.quadraticCurveTo(119, 101, 112, 103); ctx.quadraticCurveTo(105, 101, 98, 103 - p.smile); ctx.fill();
    }
    ctx.restore();
    // --- table, book, candle
    ctx.fillStyle = '#3b2617'; ctx.fillRect(0, 168, W, H - 168);
    ctx.fillStyle = '#4c321e'; ctx.fillRect(0, 166, W, 5);
    ctx.save(); ctx.translate(100, 178);
    ctx.fillStyle = '#5a1f1f'; roundRect(-58, -6, 116, 26, 3); ctx.fill();
    for (const side of [-1, 1]) {
      ctx.fillStyle = '#efe2c4'; ctx.beginPath(); ctx.moveTo(0, -2); ctx.quadraticCurveTo(side * 26, -10, side * 54, -6); ctx.lineTo(side * 54, 16); ctx.quadraticCurveTo(side * 26, 12, 0, 18); ctx.closePath(); ctx.fill();
      ctx.strokeStyle = 'rgba(80,60,40,.35)'; ctx.lineWidth = .8;
      for (let i = 0; i < 5; i++) { ctx.beginPath(); ctx.moveTo(side * 8, 0 + i * 3.2); ctx.lineTo(side * 46, -3 + i * 3.4); ctx.stroke(); }
    }
    ctx.restore();
    // candle
    ctx.fillStyle = '#e8dcc0'; ctx.fillRect(cx - 6, cy + 2, 12, 30);
    ctx.fillStyle = '#7a5a2a'; ctx.fillRect(cx - 12, cy + 30, 24, 5);
    const fh = (13 + Math.sin(t * 11) * 2) * (1 - st.gutter * .6), fx = Math.sin(t * 7) * 1.2 + st.gutter * 4;
    const fg = ctx.createRadialGradient(cx + fx, cy - 4, 1, cx + fx, cy - 4, 12);
    fg.addColorStop(0, st.tension ? '#fff0d0' : '#fffbe6'); fg.addColorStop(.4, st.tension ? '#ff7a3a' : '#ffbf4a'); fg.addColorStop(1, 'rgba(255,120,40,0)');
    ctx.fillStyle = fg; ctx.beginPath(); ctx.moveTo(cx - 4, cy + 2); ctx.quadraticCurveTo(cx + fx, cy - fh * 1.6, cx + 4, cy + 2); ctx.fill();
    // --- hand
    ctx.fillStyle = '#dcb690'; ctx.strokeStyle = '#a77f5c'; ctx.lineWidth = 1;
    const hX = p.hx, hY = p.hy;
    ctx.save(); ctx.fillStyle = cloak; ctx.beginPath(); ctx.moveTo(160, 150 + breathe); ctx.quadraticCurveTo((160 + hX) / 2 + 12, (150 + hY) / 2 + 10, hX + 4, hY + 10); ctx.lineTo(hX - 8, hY + 12); ctx.quadraticCurveTo((150 + hX) / 2, (160 + hY) / 2 + 16, 146, 160); ctx.fill(); ctx.restore();
    ctx.beginPath(); ctx.ellipse(hX, hY, 8, 7, 0, 0, 7); ctx.fill(); ctx.stroke();
    if (st.gesture === 'shush') { ctx.beginPath(); roundRect(hX - 2.2, hY - 15, 4.4, 14, 2); ctx.fill(); ctx.stroke(); }
    else if (st.gesture === 'raise') { for (let i = 0; i < 4; i++) { ctx.beginPath(); roundRect(hX - 7 + i * 4, hY - 13, 3.4, 9, 1.6); ctx.fill(); } }
    else if (st.gesture === 'point') { ctx.beginPath(); roundRect(hX + 4, hY - 2, 13, 4, 2); ctx.fill(); ctx.stroke(); }
    // --- lightning flash
    if (st.flash > .02) { ctx.fillStyle = `rgba(220,230,255,${st.flash * .5})`; ctx.fillRect(0, 0, W, H); }
    // vignette
    const vg = ctx.createRadialGradient(W / 2, H / 2, 60, W / 2, H / 2, 160); vg.addColorStop(0, 'rgba(0,0,0,0)'); vg.addColorStop(1, st.tension ? 'rgba(60,0,0,.55)' : 'rgba(0,0,0,.5)');
    ctx.fillStyle = vg; ctx.fillRect(0, 0, W, H);
  }
  requestAnimationFrame(draw);

  function setGender() {
    st.gender = V.narratorGender();
    nameEl.textContent = st.gender === 'f' ? 'Old Mother Wick · the Lore-teller' : 'Old Wick · the Lore-teller';
  }
  setGender();
  if ('speechSynthesis' in window) speechSynthesis.addEventListener && speechSynthesis.addEventListener('voiceschanged', setGender);
  T.refresh = () => { setGender(); refreshButton(); };
  setInterval(T.refresh, 2000);

  // ======================================================================== the world talks back
  A.listen(kind => {
    if (kind === 'thunder') { st.flash = 1; EW.scene.flash && EW.scene.flash(); if (Math.random() < .5) react('awe', 1200); }
    if (kind === 'owl' || kind === 'gull') lookAt(-.9, 1500);
    if (kind === 'rumble') { react('grim', 2000); lookAt(.3, 1500); }
  });

  function inferAmbience(sc) {
    if (sc.ambience && sc.ambience.length) return sc.ambience;
    const b = sc.backdrop, w = sc.weather, txt = ((sc.title || '') + ' ' + (sc.text || '')).toLowerCase(), m = sc.music || '';
    const out = new Set();
    if (b === 'tavern') { out.add('crowd'); out.add('fire'); }
    if (['forest', 'grove', 'road', 'swamp', 'village', 'bridge'].includes(b)) out.add('forest');
    if (b === 'camp') { out.add('fire'); out.add('forest'); }
    if (['cave', 'dungeon', 'crypt', 'mine', 'hall', 'ruins', 'chapel', 'seacave', 'hoard', 'lair'].includes(b)) out.add('cave');
    if (['coast', 'ship', 'seacave'].includes(b)) out.add('sea');
    if (b === 'snow') out.add('snow');
    if (b === 'town' && sc.time !== 'night') out.add('crowd');
    if (['lair', 'hoard'].includes(b) || m === 'boss' || m === 'cindrel' || w === 'embers' || w === 'emberrain' || /\b(fire|flames?|burning|blaze|hearth|bonfire|campfire|smoulder|forge|lava|molten)\b/.test(txt)) out.add('fire');
    if (w === 'emberrain' || /\b(rain|downpour|drizzle)\b/.test(txt)) out.add(/\b(thunder|storm|lightning)\b/.test(txt) ? 'storm' : 'rain');
    else if (/\b(thunder|storm|lightning)\b/.test(txt)) out.add('storm');
    if (['road', 'bridge', 'ruins'].includes(b) || w === 'ash' || /\b(wind|gale|howl)/.test(txt)) out.add('wind');
    if (!out.size) out.add('wind');
    return [...out];
  }
  T.inferAmbience = inferAmbience;

  // ======================================================================== scenes, asides & tension
  let sceneKey = '', sceneAt = 0, events = [], scene = null, firstScene = true, hasBattle = false, leaning = 0, foreshadowed = false, inCombat = false;

  T.scene = function (sc, key) {
    scene = sc; sceneKey = key; sceneAt = performance.now();
    const amb = inferAmbience(sc);
    A.setAmbience(amb, { night: sc.time === 'night' || sc.time === 'dusk' });
    A.crowdRelax();
    EW.scene.setCritters && EW.scene.setCritters(sc.backdrop, sc.time, amb);
    hasBattle = (sc.choices || []).some(c => c.battle);
    foreshadowed = false; leaning = 0;
    setTension();
    st.tension = hasBattle ? 1 : 0;
    // plan this scene's little moments (the same for everyone at the table)
    const seed = hash(key);
    events = [];
    const add = (kind, extra) => events.push({ kind, at: 0, ...extra });
    (sc.asides || []).forEach(line => add('aside', { line }));
    if (amb.includes('crowd') && sc.backdrop === 'tavern' && sc.music !== 'tension') add('rowdy');
    if (amb.includes('storm')) add('thunder');
    if (amb.includes('fire') && seed % 3 === 0) add('flare');
    if (amb.includes('snow') && seed % 2 === 0) add('gust');
    if (amb.includes('cave') && seed % 3 === 1) add('rumble');
    if (amb.includes('sea') && seed % 3 === 2) add('gull');
    if (amb.includes('forest') && (sc.time === 'night' || sc.time === 'dusk') && seed % 2 === 1) add('owl');
    events = events.slice(0, 3);
    let at = 20000 + (seed % 9000);
    for (const e of events) { e.at = at; at += 38000 + (hash(key + e.kind) % 20000); }
    react('idle', 10); gesture(firstScene ? 'raise' : 'page', 1400);
    if (firstScene) { firstScene = false; setTimeout(() => { if (!V.cfg.narrator) say(rand(LINES.hello), { force: true }); }, 900); }
  };

  function setTension() {
    if (inCombat) { A.setTension(0); return; }
    const lvl = hasBattle ? (leaning ? 2 : 1) : 0;
    A.setTension(lvl);
    st.tension = lvl;
    document.getElementById('stage').classList.toggle('foreboding', lvl > 0);
    document.getElementById('stage').classList.toggle('foreboding-high', lvl > 1);
  }

  /** Votes and hovering a battle choice push the music toward the fight. */
  T.lean = function (on) {
    const v = on ? 1 : 0;
    if (v === leaning) return;
    leaning = v; setTension();
    if (on) { A.sfx('riser'); react('grim', 2500); lookAt(.9, 2000); if (Math.random() < .5) say(rand(LINES.lean)); }
  };

  T.update = function (S) {
    if (!S || S.phase !== 'story' || !S.scene) return;
    const votedBattle = (S.scene.choices || []).some(c => c.battle && c.votes && c.votes.length);
    if (votedBattle) T.lean(true);
  };

  function run(e) {
    const npc = scene && scene.npc && scene.npc.name;
    switch (e.kind) {
      case 'aside': react('sly', 3000); gesture('raise', 1500); say(e.line, { force: true }); break;
      case 'rowdy':
        A.crowdSwell(); lookAt(-1, 3000); react('wince', 1500);
        setTimeout(() => {
          if (npc && hash(sceneKey) % 2 === 0) { const line = rand(LINES.barkeep).replace('{npc}', npc.split(' ')[0]); caption(line); V.aside(line); A.sfx('knock'); A.crowdHush(); react('sly', 2500); }
          else { gesture('shush', 2200); react('stern', 3000); A.sfx('shush'); setTimeout(() => { A.crowdHush(); say(rand(LINES.shush), { force: true }); }, 650); }
        }, 2400);
        break;
      case 'thunder': A.thunder(); setTimeout(() => say(rand(LINES.thunder), { force: true }), 1800); break;
      case 'flare': A.fireFlare(); EW.scene.flare && EW.scene.flare('sparks'); react('wince', 1200); setTimeout(() => say(rand(LINES.fire), { force: true }), 900); break;
      case 'gust': st.gutter = 1; A.sfx('swoosh'); react('wince', 1500); setTimeout(() => say(rand(LINES.gust), { force: true }), 900); break;
      case 'rumble': react('grim', 2500); A.sfx('doom'); setTimeout(() => say(rand(LINES.rumble), { force: true }), 1500); break;
      case 'gull': lookAt(-1, 2000); setTimeout(() => say(rand(LINES.gull), { force: true }), 900); break;
      case 'owl': lookAt(-1, 2000); setTimeout(() => say(rand(LINES.owl), { force: true }), 1500); break;
    }
  }

  T._run = kind => run({ kind, line: 'A test aside.' }); // for testing and the curious

  // the scheduler: fires planned moments when the table is idle
  setInterval(() => {
    if (!scene || inCombat) return;
    const S = EW.state;
    if (!S || S.phase !== 'story') return;
    const now = performance.now(), age = now - sceneAt;
    // foreshadow once the narration has finished
    if (hasBattle && !foreshadowed && age > 4000 && !V.busy()) { foreshadowed = true; react('grim', 3500); gesture('point', 1800); say(rand(LINES.foreshadow), { force: true }); }
    const e = events[0];
    if (!e || age < e.at) return;
    if (V.busy() || (S.pending && S.pending.length)) { e.at = age + 3000; return; }
    events.shift();
    run(e);
  }, 1000);

  T.combat = function (on) {
    if (on === inCombat) return;
    inCombat = on;
    A.dimAmbience(on ? .35 : 1);
    if (on) { A.setTension(0); st.tension = 0; document.getElementById('stage').classList.remove('foreboding', 'foreboding-high'); A.sfx('doom'); react('awe', 2000); gesture('raise', 1500); say(rand(LINES.battle), { force: true }); }
  };

  /** React to what happens at the table. */
  T.fx = function (f) {
    switch (f.t) {
      case 'checkResult': if (f.success) { react('happy', 2200); A.sfx('success'); if (Math.random() < .3) say(rand(LINES.success)); } else { react('wince', 2200); A.sfx('fail'); if (Math.random() < .3) say(rand(LINES.fail)); } break;
      case 'battleEnd': if (f.result === 'victory') { react('happy', 4000); gesture('raise', 2000); say(rand(LINES.victory), { force: true }); } else { react('wince', 4000); say(rand(LINES.defeat), { force: true }); } break;
      case 'levelUp': react('awe', 3000); say(rand(LINES.level)); break;
      case 'gold': if (f.amount > 0) { react('happy', 1800); if (Math.random() < .3) say(rand(LINES.gold)); } break;
      case 'loot': react('awe', 1800); if (Math.random() < .35) say(rand(LINES.loot)); break;
      case 'storyHurt': react('wince', 1800); if (Math.random() < .3) say(rand(LINES.hurt)); break;
      case 'storySpell': react('awe', 1600); lookAt(.8, 1600); break;
      case 'attack': if (f.crit) { react('awe', 1500); if (Math.random() < .5) say(rand(LINES.crit)); } break;
      case 'down': react('wince', 2000); if (Math.random() < .5) say(rand(LINES.down)); break;
      case 'speech': lookAt(.8, 3500); break;
    }
  };
})();
