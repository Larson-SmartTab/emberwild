/* Procedural music, living soundscapes & effects — no audio files, all WebAudio. Off until the player turns it on.
   Three buses: music (with a "tension" filter + drone + heartbeat before battles), ambience (layered, scene-aware:
   a tavern crowd that laughs, clinks and can be hushed, fire crackle, rain & thunder, wind, surf & gulls, cave drips,
   birdsong, crickets & owls, snow wind) and effects (stingers, coins, page turns, the lore-teller's "shh"). */
(function () {
  const LSn = (k, d) => { try { const v = parseFloat(localStorage.getItem(k)); return isNaN(v) ? d : v; } catch { return d; } };
  const A = EW.audio = {
    on: localStorage.getItem('ew.audio') === '1', mood: '', ctx: null, tension: 0,
    vol: { music: LSn('ew.vol.music', .32), amb: LSn('ew.vol.amb', .7) },
  };
  let master, musicBus, musicFilter, ambBus, ambDuck, sfxBus, reverb, reverbSend, timer, nextNote = 0, step = 0, noiseBuf;
  let layers = {}, wantAmb = [], ambDim = 1, rowdy = 1, rowdyBase = 1, drone = null, nextBeat = 0, moodFade = null;
  const listeners = [];
  A.listen = fn => listeners.push(fn);
  const emit = (kind, data) => { for (const fn of listeners) try { fn(kind, data); } catch (e) { console.warn(e); } };

  const MOODS = {
    tavern:  { scale: [0, 2, 4, 7, 9], root: 57, tempo: 0.28, bass: true, pluck: .7, drum: 0, bright: 1 },
    ballad:  { scale: [0, 2, 3, 7, 8], root: 57, tempo: 0.42, bass: true, pluck: .6, drum: 0, bright: .8 },
    brawl:   { scale: [0, 2, 4, 7, 9], root: 57, tempo: 0.17, bass: true, pluck: .9, drum: .6, bright: 1 },
    explore: { scale: [0, 2, 4, 7, 9], root: 55, tempo: 0.45, bass: false, pluck: .35, drum: 0, bright: .9 },
    camp:    { scale: [0, 2, 4, 7, 9], root: 52, tempo: 0.6, bass: false, pluck: .25, drum: 0, bright: .7 },
    grove:   { scale: [0, 2, 4, 6, 9, 11], root: 64, tempo: 0.35, bass: false, pluck: .45, drum: 0, bright: 1.2 },
    eerie:   { scale: [0, 1, 5, 6, 10], root: 48, tempo: 0.7, bass: true, pluck: .2, drum: 0, bright: .5 },
    mystery: { scale: [0, 2, 3, 6, 7, 10], root: 50, tempo: 0.55, bass: true, pluck: .3, drum: 0, bright: .6 },
    sorrow:  { scale: [0, 2, 3, 7, 8], root: 50, tempo: 0.65, bass: true, pluck: .3, drum: 0, bright: .5 },
    tension: { scale: [0, 1, 3, 6, 7], root: 45, tempo: 0.3, bass: true, pluck: .3, drum: .25, bright: .6 },
    goblin:  { scale: [0, 3, 5, 6, 7, 10], root: 57, tempo: 0.18, bass: true, pluck: .7, drum: .4, bright: 1 },
    ruins:   { scale: [0, 1, 5, 7, 8], root: 45, tempo: 0.6, bass: true, pluck: .25, drum: .1, bright: .5 },
    battle:  { scale: [0, 2, 3, 5, 7, 8], root: 45, tempo: 0.16, bass: true, pluck: .55, drum: 1, bright: .8 },
    cindrel: { scale: [0, 2, 3, 7, 8, 11], root: 50, tempo: 0.2, bass: true, pluck: .6, drum: .7, bright: .9 },
    boss:    { scale: [0, 1, 3, 6, 7, 8], root: 40, tempo: 0.15, bass: true, pluck: .6, drum: 1.3, bright: .7 },
    hope:    { scale: [0, 2, 4, 7, 9, 11], root: 60, tempo: 0.4, bass: true, pluck: .45, drum: 0, bright: 1.1 },
    sea:     { scale: [0, 2, 4, 5, 7, 9], root: 52, tempo: 0.5, bass: true, pluck: .4, drum: 0, bright: .8 },
    shanty:  { scale: [0, 2, 3, 5, 7, 10], root: 55, tempo: 0.2, bass: true, pluck: .8, drum: .5, bright: 1 },
    snow:    { scale: [0, 2, 3, 7, 9], root: 62, tempo: 0.6, bass: false, pluck: .35, drum: 0, bright: 1.3 },
    dungeon: { scale: [0, 1, 3, 5, 6, 8], root: 43, tempo: 0.6, bass: true, pluck: .25, drum: .15, bright: .5 },
    dragon:  { scale: [0, 1, 4, 5, 7, 8], root: 38, tempo: 0.14, bass: true, pluck: .7, drum: 1.4, bright: .8 },
    triumph: { scale: [0, 4, 7, 9, 12], root: 60, tempo: 0.22, bass: true, pluck: .7, drum: .5, bright: 1.2 },
  };

  // ======================================================================== graph
  function init() {
    if (A.ctx) return;
    const C = A.ctx = new (window.AudioContext || window.webkitAudioContext)();
    master = C.createGain(); master.gain.value = 0.55; master.connect(C.destination);
    musicFilter = C.createBiquadFilter(); musicFilter.type = 'lowpass'; musicFilter.frequency.value = 18000; musicFilter.connect(master);
    musicBus = C.createGain(); musicBus.gain.value = A.vol.music; musicBus.connect(musicFilter);
    ambBus = C.createGain(); ambBus.gain.value = A.vol.amb; ambBus.connect(master);
    ambDuck = C.createGain(); ambDuck.gain.value = 1; ambDuck.connect(ambBus);
    sfxBus = C.createGain(); sfxBus.gain.value = 1; sfxBus.connect(master);
    noiseBuf = C.createBuffer(1, C.sampleRate * 2, C.sampleRate);
    const d = noiseBuf.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
    // a small generated room reverb, used by cave drips, the crowd and stingers
    const len = C.sampleRate * 2.4, ir = C.createBuffer(2, len, C.sampleRate);
    for (let ch = 0; ch < 2; ch++) { const b = ir.getChannelData(ch); for (let i = 0; i < len; i++) b[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3.2); }
    reverb = C.createConvolver(); reverb.buffer = ir; reverb.connect(ambDuck);
    reverbSend = C.createGain(); reverbSend.gain.value = 0.5; reverbSend.connect(reverb);
  }

  const hz = m => 440 * Math.pow(2, (m - 69) / 12);
  const rnd = (a, b) => a + Math.random() * (b - a);
  const expGap = rate => -Math.log(1 - Math.random()) / rate;

  // ======================================================================== voices of the instruments
  function pluck(t, midi, vol, bright) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain(), f = A.ctx.createBiquadFilter();
    o.type = 'triangle'; o.frequency.value = hz(midi);
    f.type = 'lowpass'; f.frequency.setValueAtTime(900 + 2600 * bright, t); f.frequency.exponentialRampToValueAtTime(400, t + .6);
    g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(vol, t + 0.01); g.gain.exponentialRampToValueAtTime(0.0001, t + 1.1);
    o.connect(f); f.connect(g); g.connect(musicBus); o.start(t); o.stop(t + 1.2);
  }
  function bassNote(t, midi, dur) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = 'sine'; o.frequency.value = hz(midi - 12);
    g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(0.28, t + 0.03); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(musicBus); o.start(t); o.stop(t + dur + .05);
  }
  function drum(t, low, vol, dest) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = 'sine'; o.frequency.setValueAtTime(low ? 120 : 220, t); o.frequency.exponentialRampToValueAtTime(low ? 45 : 90, t + .18);
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.0001, t + .3);
    o.connect(g); g.connect(dest || musicBus); o.start(t); o.stop(t + .32);
  }
  function noiseBurst(t, dur, freq, vol, dest, type, q) {
    const s = A.ctx.createBufferSource(), f = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
    s.buffer = noiseBuf; f.type = type || 'bandpass'; f.frequency.value = freq; f.Q.value = q || 1.2;
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    s.connect(f); f.connect(g); g.connect(dest || sfxBus); s.start(t, Math.random()); s.stop(t + dur + .05);
  }
  function tone(t, f0, f1, dur, vol, type, dest, attack) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = type || 'sine'; o.frequency.setValueAtTime(f0, t); if (f1 && f1 !== f0) o.frequency.exponentialRampToValueAtTime(f1, t + dur);
    g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(vol, t + (attack || .005)); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(dest || sfxBus); o.start(t); o.stop(t + dur + .05);
  }
  function panned(dest, pan) {
    if (!A.ctx.createStereoPanner) return dest;
    const p = A.ctx.createStereoPanner(); p.pan.value = pan; p.connect(dest); return p;
  }
  function loopNoise(type, freq, q, vol, dest) {
    const s = A.ctx.createBufferSource(), f = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
    s.buffer = noiseBuf; s.loop = true; s.loopStart = Math.random(); f.type = type; f.frequency.value = freq; f.Q.value = q;
    g.gain.value = vol; s.connect(f); f.connect(g); g.connect(dest); s.start(A.ctx.currentTime, Math.random());
    return { s, f, g };
  }

  // A voice in the crowd: a sawtooth throat through two vowel formants, muffled by distance.
  const VOWELS = { a: [800, 1200], e: [420, 2100], i: [300, 2500], o: [520, 880], u: [350, 700], ay: [650, 1700] };
  function syllable(t, f0, vowel, dur, vol, dest) {
    const C = A.ctx, o = C.createOscillator(), g = C.createGain();
    const [F1, F2] = VOWELS[vowel] || VOWELS.a;
    o.type = 'sawtooth'; o.frequency.setValueAtTime(f0, t); o.frequency.linearRampToValueAtTime(f0 * rnd(.85, 1.12), t + dur);
    const b1 = C.createBiquadFilter(), b2 = C.createBiquadFilter();
    b1.type = b2.type = 'bandpass'; b1.frequency.value = F1; b2.frequency.value = F2; b1.Q.value = 7; b2.Q.value = 9;
    g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(vol, t + .02); g.gain.setValueAtTime(vol, t + dur * .6); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(b1); o.connect(b2); b1.connect(g); b2.connect(g); g.connect(dest);
    o.start(t); o.stop(t + dur + .03);
  }
  function chatter(t, dest, loud) {
    const male = Math.random() < .55, f0 = male ? rnd(95, 150) : rnd(170, 260), n = 2 + Math.floor(Math.random() * 6);
    const p = panned(dest, rnd(-.8, .8)), vs = Object.keys(VOWELS);
    for (let i = 0; i < n; i++) syllable(t + i * rnd(.11, .19), f0 * rnd(.92, 1.12), vs[Math.floor(Math.random() * vs.length)], rnd(.08, .17), .012 * loud, p);
  }
  function laugh(t, dest, loud) {
    const male = Math.random() < .6, f0 = male ? rnd(140, 190) : rnd(230, 330), n = 4 + Math.floor(Math.random() * 4);
    const p = panned(dest, rnd(-.7, .7));
    for (let i = 0; i < n; i++) {
      const tt = t + i * rnd(.13, .17);
      noiseBurst(tt, .04, 1500, .01 * loud, p);
      syllable(tt + .02, f0 * (1 - i * .035), 'a', .09, .022 * loud, p);
    }
  }
  function clink(t, dest, vol) {
    const f = rnd(2400, 4200), p = panned(dest, rnd(-.6, .6));
    tone(t, f, f * .995, .35, vol, 'sine', p); tone(t, f * 2.71, f * 2.7, .18, vol * .4, 'sine', p);
  }

  // ======================================================================== ambience layers
  const LAYERS = {
    crowd: {
      start(L) { L.m1 = loopNoise('bandpass', 380, 1.4, .035, L.out); L.m2 = loopNoise('bandpass', 900, 2, .012, L.out); L.dest = A.ctx.createBiquadFilter(); L.dest.type = 'lowpass'; L.dest.frequency.value = 2600; L.dest.connect(L.out); L.dest.connect(reverbSend); },
      tick(L, now, until) {
        const r = rowdy;
        L.m1.g.gain.setTargetAtTime(.02 + .03 * r, now, .8); L.m2.g.gain.setTargetAtTime(.006 + .01 * r, now, .8);
        this.ev(L, 'talk', until, 2.6 * r + .3, t => chatter(t, L.dest, .8 + r * .4));
        this.ev(L, 'laugh', until, .09 * r * r, t => laugh(t, L.dest, .8 + r * .5));
        this.ev(L, 'clink', until, .25 * r + .05, t => clink(t, L.dest, .012 + .008 * r));
        this.ev(L, 'chair', until, .03 * r, t => noiseBurst(t, rnd(.25, .5), rnd(500, 900), .02, L.dest, 'bandpass', 6));
      },
    },
    fire: {
      start(L) { L.roar = loopNoise('lowpass', 260, .7, .05, L.out); },
      tick(L, now, until) {
        L.roar.g.gain.setTargetAtTime(.035 + Math.random() * .03 * (L.hot || 1), now, .15);
        this.ev(L, 'crackle', until, 9 * (L.hot || 1), t => noiseBurst(t, rnd(.004, .02), rnd(2500, 7000), rnd(.03, .09), L.out, 'highpass', .7));
        this.ev(L, 'pop', until, .5 * (L.hot || 1), t => { noiseBurst(t, .05, rnd(900, 1800), rnd(.12, .22), L.out); emit('pop'); });
        if (L.hot > 1) L.hot = Math.max(1, L.hot - .05);
      },
    },
    rain: {
      start(L) { L.hiss = loopNoise('highpass', 2500, .5, .05, L.out); L.low = loopNoise('lowpass', 500, .5, .025, L.out); },
      tick(L, now, until) { this.ev(L, 'drip', until, 7, t => tone(t, rnd(1400, 3200), rnd(700, 1400), .06, rnd(.006, .02), 'sine', L.out)); },
    },
    storm: {
      start(L) { LAYERS.rain.start(L); L.hiss.g.gain.value = .075; },
      tick(L, now, until) { LAYERS.rain.tick.call(this, L, now, until); this.ev(L, 'thunder', until, 1 / 26, t => thunder(t, L.out)); },
    },
    wind: {
      start(L) { L.w = loopNoise('lowpass', 450, 1.5, .05, L.out); },
      tick(L, now) { L.w.f.frequency.setTargetAtTime(rnd(250, 900), now, 1.2); L.w.g.gain.setTargetAtTime(rnd(.025, .07), now, 1.5); },
    },
    snow: {
      start(L) { L.w = loopNoise('bandpass', 600, 7, .05, L.out); L.b = loopNoise('lowpass', 300, .7, .02, L.out); },
      tick(L, now, until) {
        L.w.f.frequency.setTargetAtTime(rnd(380, 1100), now, .9); L.w.g.gain.setTargetAtTime(rnd(.02, .08), now, 1.1);
        this.ev(L, 'creak', until, .05, t => tone(t, rnd(180, 260), rnd(120, 200), .6, .012, 'sawtooth', L.out, .2));
      },
    },
    sea: {
      start(L) { L.surf = loopNoise('lowpass', 700, .6, .0001, L.out); L.nextWave = 0; },
      tick(L, now, until) {
        if (L.nextWave < until) {
          const t = Math.max(L.nextWave, now), up = rnd(2.2, 3.4), down = rnd(3, 4.5);
          L.surf.g.gain.setTargetAtTime(.09, t, up / 3); L.surf.f.frequency.setTargetAtTime(1300, t, up / 2);
          L.surf.g.gain.setTargetAtTime(.012, t + up, down / 3); L.surf.f.frequency.setTargetAtTime(500, t + up, down / 2);
          L.nextWave = t + up + down;
        }
        this.ev(L, 'gull', until, 1 / 18, t => { gull(t, L.out); emit('gull'); });
      },
    },
    cave: {
      start(L) { L.rum = loopNoise('lowpass', 90, .7, .03, L.out); },
      tick(L, now, until) {
        this.ev(L, 'drip', until, .8, t => { const f = rnd(900, 2100); tone(t, f, f * .55, .12, rnd(.03, .06), 'sine', panned(reverbSend, rnd(-.8, .8))); });
        this.ev(L, 'groan', until, 1 / 40, t => { tone(t, 48, 38, 3.5, .08, 'sawtooth', L.out, 1.2); emit('rumble'); });
      },
    },
    forest: {
      start(L) { L.leaves = loopNoise('highpass', 3000, .5, .008, L.out); },
      tick(L, now, until) {
        if (L.night) {
          this.ev(L, 'cricket', until, 1.3, t => { const p = panned(L.out, rnd(-.9, .9)); for (let i = 0; i < 3; i++) tone(t + i * .045, 4600, 4580, .03, .01, 'sine', p); });
          this.ev(L, 'owl', until, 1 / 35, t => { owl(t, L.out); emit('owl'); });
        } else {
          this.ev(L, 'bird', until, .6, t => bird(t, L.out));
        }
      },
    },
    quiet: { start() {}, tick() {} },
  };
  // shared Poisson scheduler for layers
  for (const k in LAYERS) LAYERS[k].ev = function (L, name, until, rate, fn) {
    if (rate <= 0) return;
    L.next = L.next || {};
    if (L.next[name] === undefined) L.next[name] = A.ctx.currentTime + expGap(rate);
    while (L.next[name] < until) { fn(L.next[name]); L.next[name] += expGap(rate); }
  };

  function thunder(t, dest) {
    noiseBurst(t, .35, 1800, .25, dest, 'bandpass', .5);
    noiseBurst(t + .05, 3.8, 90, .9, dest, 'lowpass', .5);
    noiseBurst(t + .3, 2.5, 160, .5, dest, 'lowpass', .5);
    const ms = Math.max(0, (t - A.ctx.currentTime) * 1000);
    setTimeout(() => emit('thunder'), ms);
  }
  function gull(t, dest) {
    const p = panned(dest, rnd(-.9, .9)), n = 2 + Math.floor(Math.random() * 3);
    for (let i = 0; i < n; i++) tone(t + i * .32, rnd(1500, 1800), rnd(900, 1100), .26, .025, 'triangle', p, .03);
  }
  function owl(t, dest) {
    const p = panned(dest, rnd(-.7, .7));
    tone(t, 400, 360, .4, .05, 'sine', p, .08); tone(t + .6, 410, 340, .7, .05, 'sine', p, .1);
  }
  function bird(t, dest) {
    const p = panned(dest, rnd(-.9, .9)), base = rnd(2600, 4800), n = 2 + Math.floor(Math.random() * 5);
    for (let i = 0; i < n; i++) tone(t + i * rnd(.06, .11), base * rnd(.9, 1.15), base * rnd(.8, 1.3), .07, .012, 'sine', p, .01);
  }

  function startLayer(kind, opts) {
    const C = A.ctx, out = C.createGain();
    out.gain.value = 0.0001; out.connect(ambDuck);
    const L = { kind, out, ...(opts || {}) };
    LAYERS[kind].start(L);
    out.gain.setTargetAtTime(L.level || 1, C.currentTime, .9);
    return L;
  }
  function stopLayer(L) {
    const C = A.ctx;
    L.out.gain.setTargetAtTime(0.0001, C.currentTime, .6);
    setTimeout(() => { for (const k in L) if (L[k] && L[k].s) try { L[k].s.stop(); } catch {} try { L.out.disconnect(); } catch {} }, 3000);
  }
  function syncLayers() {
    if (!A.ctx) return;
    const want = new Set(A.on ? wantAmb : []);
    for (const k of Object.keys(layers)) if (!want.has(k)) { stopLayer(layers[k]); delete layers[k]; }
    for (const k of want) if (!layers[k] && LAYERS[k]) layers[k] = startLayer(k, { night: A._night, hot: 1 });
    for (const k of want) if (layers[k]) layers[k].night = A._night;
  }

  /** Set the scene's background sound: a list of layer names. */
  A.setAmbience = function (list, opts) {
    wantAmb = [...new Set((list || []).filter(k => LAYERS[k]))];
    A._night = !!(opts && opts.night);
    A.ambience = wantAmb.slice();
    syncLayers();
  };
  /** Turn the room down (battles, the lore-teller hushing everyone). */
  A.dimAmbience = function (v) {
    ambDim = v;
    if (A.ctx) ambDuck.gain.setTargetAtTime(v, A.ctx.currentTime, .5);
  };
  A.has = k => wantAmb.includes(k);

  /** The tavern gets rowdy: laughter, a cheer, a smashed mug. */
  A.crowdSwell = function () {
    rowdy = 2.2;
    if (!A.on || !A.ctx || !layers.crowd) return;
    const t = A.ctx.currentTime + .05, d = layers.crowd.dest;
    for (let i = 0; i < 5; i++) laugh(t + rnd(0, 1.6), d, 1.8);
    for (let i = 0; i < 7; i++) syllable(t + rnd(.2, .9), rnd(110, 260), Math.random() < .5 ? 'ay' : 'o', rnd(.5, .9), .03, panned(d, rnd(-.8, .8)));
    noiseBurst(t + 1.1, .45, 3500, .16, d, 'highpass', .6);
    for (let i = 0; i < 5; i++) clink(t + 1.1 + i * .05 + Math.random() * .1, d, .03);
  };
  /** ...and settles down when told off. */
  A.crowdHush = function () { rowdy = .12; };
  A.crowdRelax = function () { rowdyBase = 1; };
  A.fireFlare = function () { if (layers.fire) layers.fire.hot = 3; };
  A.thunder = function () { if (A.on && A.ctx) thunder(A.ctx.currentTime + .05, ambDuck); else emit('thunder'); };

  // ======================================================================== tension (the music knows a fight is coming)
  /** 0 = none, 1 = a battle is one choice away, 2 = the table is leaning toward it. */
  let applied = 0;
  A.setTension = function (lvl) {
    A.tension = lvl || 0;
    if (A.on && A.ctx) applyTension(A.tension);
  };
  function applyTension(lvl) {
    if (lvl === applied) return;
    applied = lvl;
    const now = A.ctx.currentTime;
    musicFilter.frequency.setTargetAtTime(lvl === 0 ? 18000 : lvl === 1 ? 1500 : 900, now, .8);
    if (lvl > 0 && !drone) {
      const g = A.ctx.createGain(); g.gain.value = 0.0001; g.connect(master);
      const f = A.ctx.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 240; f.Q.value = 3; f.connect(g);
      const oscs = [55, 55 * 1.006, 58.27].map((fr, i) => { const o = A.ctx.createOscillator(); o.type = 'sawtooth'; o.frequency.value = fr; const og = A.ctx.createGain(); og.gain.value = i === 2 ? 0 : 1; o.connect(og); og.connect(f); o.start(); return { o, og }; });
      const lfo = A.ctx.createOscillator(), lg = A.ctx.createGain(); lfo.frequency.value = .13; lg.gain.value = 90; lfo.connect(lg); lg.connect(f.frequency); lfo.start();
      drone = { g, f, oscs, lfo };
      nextBeat = now + .4;
    }
    if (drone) {
      drone.g.gain.setTargetAtTime(lvl === 0 ? 0.0001 : lvl === 1 ? .05 : .085, now, 1);
      drone.oscs[2].og.gain.setTargetAtTime(lvl === 2 ? .8 : 0, now, 1.5);
      if (lvl === 0) { const d = drone; drone = null; setTimeout(() => { d.oscs.forEach(x => x.o.stop()); d.lfo.stop(); d.g.disconnect(); }, 4000); }
      else if (lvl === 2) { const t = now + .05; noiseBurst(t, 1.6, 300, .2, master, 'lowpass', 2); tone(t, 41, 41, 1.8, .2, 'sine', master, .6); }
    }
  }

  // ======================================================================== the clock
  function tick() {
    const C = A.ctx, now = C.currentTime, until = now + 0.3;
    const m = MOODS[A.mood] || MOODS.explore;
    const calm = 1 - .35 * A.tension;
    while (nextNote < until) {
      const t = nextNote, beat = step % 8;
      if (Math.random() < m.pluck * calm) {
        const deg = m.scale[Math.floor(Math.random() * m.scale.length)];
        const oct = Math.random() < .3 ? 12 : 0;
        pluck(t, m.root + 12 + deg + oct, 0.12 + Math.random() * .06, m.bright);
      }
      if (m.bass && beat % 4 === 0) bassNote(t, m.root + (beat === 4 ? m.scale[2] : 0), m.tempo * 3.5);
      if (m.drum > 0) {
        if (beat === 0 || beat === 3 || beat === 6) drum(t, true, 0.35 * m.drum);
        if (beat === 4) noiseBurst(t, .08, 2500, 0.08 * m.drum, musicBus);
      }
      step++;
      nextNote += m.tempo * (0.92 + Math.random() * 0.16);
    }
    // heartbeat under the music when a fight is near
    if (applied > 0) {
      const gap = applied === 1 ? 1.25 : .78;
      while (nextBeat < until) { drum(nextBeat, true, applied === 1 ? .35 : .5, master); drum(nextBeat + .2, true, applied === 1 ? .22 : .32, master); nextBeat += gap; }
    } else nextBeat = until;
    // the crowd drifts back to its usual noise
    rowdy += (rowdyBase - rowdy) * (rowdy > rowdyBase ? .03 : .007);
    for (const k in layers) LAYERS[k].tick(layers[k], now, until);
  }

  // ======================================================================== public controls
  A.setMood = function (mood) {
    if (!mood || mood === A.mood) return;
    if (!A.on || !A.ctx || !A.mood) { A.mood = mood; return; }
    // crossfade: dip the band, change the tune, bring it back
    const now = A.ctx.currentTime;
    musicBus.gain.cancelScheduledValues(now);
    musicBus.gain.setTargetAtTime(0.0001, now, .25);
    clearTimeout(moodFade);
    moodFade = setTimeout(() => { A.mood = mood; nextNote = A.ctx.currentTime + .05; musicBus.gain.setTargetAtTime(A.vol.music, A.ctx.currentTime, .6); }, 700);
  };

  A.setVolume = function (which, v) {
    A.vol[which] = v;
    try { localStorage.setItem('ew.vol.' + which, String(v)); } catch {}
    if (!A.ctx) return;
    if (which === 'music') musicBus.gain.setTargetAtTime(v, A.ctx.currentTime, .1);
    else ambBus.gain.setTargetAtTime(v, A.ctx.currentTime, .1);
  };

  A.toggle = function (on) {
    A.on = on;
    localStorage.setItem('ew.audio', on ? '1' : '0');
    if (on) {
      init();
      A.ctx.resume();
      nextNote = A.ctx.currentTime + .1;
      clearInterval(timer); timer = setInterval(tick, 100);
      applyTension(A.tension);
    } else {
      clearInterval(timer);
      if (A.ctx) applyTension(0);
    }
    syncLayers();
  };

  A.sfx = function (kind) {
    if (!A.on || !A.ctx) return;
    const t = A.ctx.currentTime;
    switch (kind) {
      case 'dice': for (let i = 0; i < 6; i++) noiseBurst(t + i * 0.07 + Math.random() * .03, .04, 1800 + Math.random() * 1500, 0.25 - i * .03); break;
      case 'hit': drum(t, true, .5, sfxBus); noiseBurst(t, .12, 900, .3); break;
      case 'miss': noiseBurst(t, .2, 3000, .12); break;
      case 'fire': noiseBurst(t, .5, 600, .35); noiseBurst(t + .05, .4, 1400, .2); for (let i = 0; i < 8; i++) noiseBurst(t + rnd(0, .6), .01, rnd(3000, 7000), .12, sfxBus, 'highpass'); break;
      case 'cold': for (let i = 0; i < 5; i++) pluck(t + i * .04, 88 + i * 2, .08, 1.5); break;
      case 'lightning': noiseBurst(t, .25, 4000, .4); noiseBurst(t + .1, .15, 2500, .3); noiseBurst(t + .15, 1.5, 120, .4, sfxBus, 'lowpass'); break;
      case 'heal': [0, 4, 7, 12].forEach((d, i) => pluck(t + i * .08, 72 + d, .1, 1.3)); break;
      case 'crit': drum(t, true, .7, sfxBus); [0, 7, 12].forEach((d, i) => pluck(t + i * .05, 64 + d, .14, 1.4)); break;
      case 'level': [0, 4, 7, 12, 16].forEach((d, i) => pluck(t + i * .1, 67 + d, .15, 1.4)); break;
      case 'turn': pluck(t, 76, .08, 1); pluck(t + .08, 83, .08, 1); break;
      case 'win': [0, 4, 7, 12, 7, 12, 16].forEach((d, i) => pluck(t + i * .12, 60 + d, .16, 1.3)); break;
      case 'lose': [0, -1, -5, -12].forEach((d, i) => pluck(t + i * .25, 57 + d, .14, .5)); break;
      case 'coin': for (let i = 0; i < 6; i++) { const f = rnd(3200, 5200); tone(t + i * rnd(.05, .09), f, f, .25, .05, 'sine'); tone(t + i * .07, f * 1.5, f * 1.5, .12, .02, 'sine'); } break;
      case 'loot': [0, 7, 12, 19, 24].forEach((d, i) => tone(t + i * .06, hz(79 + d), hz(79 + d), .6, .04, 'sine')); noiseBurst(t, .8, 7000, .04, sfxBus, 'highpass'); break;
      case 'page': { const s = A.ctx.createBufferSource(), f = A.ctx.createBiquadFilter(), g = A.ctx.createGain(); s.buffer = noiseBuf; f.type = 'bandpass'; f.Q.value = 1.5; f.frequency.setValueAtTime(900, t); f.frequency.exponentialRampToValueAtTime(3800, t + .28); g.gain.setValueAtTime(.0001, t); g.gain.exponentialRampToValueAtTime(.12, t + .08); g.gain.exponentialRampToValueAtTime(.0001, t + .35); s.connect(f); f.connect(g); g.connect(sfxBus); s.start(t, Math.random()); s.stop(t + .4); break; }
      case 'shush': noiseBurst(t, .7, 4200, .14, sfxBus, 'bandpass', 2.2); break;
      case 'knock': drum(t, false, .35, sfxBus); drum(t + .16, false, .35, sfxBus); break;
      case 'doom': // a battle begins
        tone(t, 55, 30, 2.5, .5, 'sine', sfxBus, .01); noiseBurst(t, 2.2, 140, .5, sfxBus, 'lowpass', .7);
        [0, 1, 6].forEach(d => tone(t + .05, hz(45 + d), hz(45 + d), 2.4, .06, 'sawtooth', reverbSend || sfxBus, .3));
        break;
      case 'riser': { // the music leans in
        const s = A.ctx.createBufferSource(), f = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
        s.buffer = noiseBuf; f.type = 'bandpass'; f.Q.value = 4; f.frequency.setValueAtTime(200, t); f.frequency.exponentialRampToValueAtTime(2400, t + 1.6);
        g.gain.setValueAtTime(.0001, t); g.gain.exponentialRampToValueAtTime(.18, t + 1.5); g.gain.exponentialRampToValueAtTime(.0001, t + 1.75);
        s.connect(f); f.connect(g); g.connect(sfxBus); s.start(t, Math.random()); s.stop(t + 1.8); break;
      }
      case 'success': [0, 4, 7].forEach((d, i) => tone(t + i * .07, hz(72 + d), hz(72 + d), .5, .05, 'triangle')); break;
      case 'fail': tone(t, hz(55), hz(50), .7, .08, 'triangle'); tone(t + .15, hz(54), hz(49), .7, .06, 'triangle'); break;
      case 'swoosh': noiseBurst(t, .35, 1200, .15, sfxBus, 'bandpass', .8); break;
    }
  };
})();
