/* Procedural music & sound — no audio files, all WebAudio. Off until the player turns it on. */
(function () {
  const A = EW.audio = { on: localStorage.getItem('ew.audio') === '1', mood: '', ctx: null };
  let master, musicGain, timer, nextNote = 0, step = 0, noiseBuf, crackleNode;

  const MOODS = {
    tavern:  { scale: [0, 2, 4, 7, 9], root: 57, tempo: 0.28, bass: true, pluck: .7, drum: 0, noise: 'murmur', bright: 1 },
    ballad:  { scale: [0, 2, 3, 7, 8], root: 57, tempo: 0.42, bass: true, pluck: .6, drum: 0, noise: 'murmur', bright: .8 },
    brawl:   { scale: [0, 2, 4, 7, 9], root: 57, tempo: 0.17, bass: true, pluck: .9, drum: .6, noise: 'murmur', bright: 1 },
    explore: { scale: [0, 2, 4, 7, 9], root: 55, tempo: 0.45, bass: false, pluck: .35, drum: 0, noise: 'wind', bright: .9 },
    camp:    { scale: [0, 2, 4, 7, 9], root: 52, tempo: 0.6, bass: false, pluck: .25, drum: 0, noise: 'fire', bright: .7 },
    grove:   { scale: [0, 2, 4, 6, 9, 11], root: 64, tempo: 0.35, bass: false, pluck: .45, drum: 0, noise: 'wind', bright: 1.2 },
    eerie:   { scale: [0, 1, 5, 6, 10], root: 48, tempo: 0.7, bass: true, pluck: .2, drum: 0, noise: 'wind', bright: .5 },
    mystery: { scale: [0, 2, 3, 6, 7, 10], root: 50, tempo: 0.55, bass: true, pluck: .3, drum: 0, noise: 'wind', bright: .6 },
    sorrow:  { scale: [0, 2, 3, 7, 8], root: 50, tempo: 0.65, bass: true, pluck: .3, drum: 0, noise: 'wind', bright: .5 },
    tension: { scale: [0, 1, 3, 6, 7], root: 45, tempo: 0.3, bass: true, pluck: .3, drum: .25, noise: '', bright: .6 },
    goblin:  { scale: [0, 3, 5, 6, 7, 10], root: 57, tempo: 0.18, bass: true, pluck: .7, drum: .4, noise: '', bright: 1 },
    ruins:   { scale: [0, 1, 5, 7, 8], root: 45, tempo: 0.6, bass: true, pluck: .25, drum: .1, noise: 'fire', bright: .5 },
    battle:  { scale: [0, 2, 3, 5, 7, 8], root: 45, tempo: 0.16, bass: true, pluck: .55, drum: 1, noise: '', bright: .8 },
    cindrel: { scale: [0, 2, 3, 7, 8, 11], root: 50, tempo: 0.2, bass: true, pluck: .6, drum: .7, noise: 'fire', bright: .9 },
    boss:    { scale: [0, 1, 3, 6, 7, 8], root: 40, tempo: 0.15, bass: true, pluck: .6, drum: 1.3, noise: 'fire', bright: .7 },
    hope:    { scale: [0, 2, 4, 7, 9, 11], root: 60, tempo: 0.4, bass: true, pluck: .45, drum: 0, noise: '', bright: 1.1 },
    sea:     { scale: [0, 2, 4, 5, 7, 9], root: 52, tempo: 0.5, bass: true, pluck: .4, drum: 0, noise: 'wind', bright: .8 },
    shanty:  { scale: [0, 2, 3, 5, 7, 10], root: 55, tempo: 0.2, bass: true, pluck: .8, drum: .5, noise: 'murmur', bright: 1 },
    snow:    { scale: [0, 2, 3, 7, 9], root: 62, tempo: 0.6, bass: false, pluck: .35, drum: 0, noise: 'wind', bright: 1.3 },
    dungeon: { scale: [0, 1, 3, 5, 6, 8], root: 43, tempo: 0.6, bass: true, pluck: .25, drum: .15, noise: '', bright: .5 },
    dragon:  { scale: [0, 1, 4, 5, 7, 8], root: 38, tempo: 0.14, bass: true, pluck: .7, drum: 1.4, noise: 'fire', bright: .8 },
    triumph: { scale: [0, 4, 7, 9, 12], root: 60, tempo: 0.22, bass: true, pluck: .7, drum: .5, noise: 'murmur', bright: 1.2 },
  };

  function init() {
    if (A.ctx) return;
    A.ctx = new (window.AudioContext || window.webkitAudioContext)();
    master = A.ctx.createGain(); master.gain.value = 0.5; master.connect(A.ctx.destination);
    musicGain = A.ctx.createGain(); musicGain.gain.value = 0.32; musicGain.connect(master);
    noiseBuf = A.ctx.createBuffer(1, A.ctx.sampleRate * 2, A.ctx.sampleRate);
    const d = noiseBuf.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
  }

  const hz = m => 440 * Math.pow(2, (m - 69) / 12);

  function pluck(t, midi, vol, bright) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain(), f = A.ctx.createBiquadFilter();
    o.type = 'triangle'; o.frequency.value = hz(midi);
    f.type = 'lowpass'; f.frequency.setValueAtTime(900 + 2600 * bright, t); f.frequency.exponentialRampToValueAtTime(400, t + .6);
    g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(vol, t + 0.01); g.gain.exponentialRampToValueAtTime(0.0001, t + 1.1);
    o.connect(f); f.connect(g); g.connect(musicGain); o.start(t); o.stop(t + 1.2);
  }
  function bassNote(t, midi, dur) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = 'sine'; o.frequency.value = hz(midi - 12);
    g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(0.28, t + 0.03); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(musicGain); o.start(t); o.stop(t + dur + .05);
  }
  function drum(t, low, vol) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = 'sine'; o.frequency.setValueAtTime(low ? 120 : 220, t); o.frequency.exponentialRampToValueAtTime(low ? 45 : 90, t + .18);
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.0001, t + .3);
    o.connect(g); g.connect(musicGain); o.start(t); o.stop(t + .32);
  }
  function noiseBurst(t, dur, freq, vol, dest) {
    const s = A.ctx.createBufferSource(), f = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
    s.buffer = noiseBuf; f.type = 'bandpass'; f.frequency.value = freq; f.Q.value = 1.2;
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    s.connect(f); f.connect(g); g.connect(dest || master); s.start(t, Math.random()); s.stop(t + dur + .05);
  }

  function startBed(kind) {
    if (crackleNode) { try { crackleNode.stop(); } catch {} crackleNode = null; }
    if (!kind) return;
    const s = A.ctx.createBufferSource(), f = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
    s.buffer = noiseBuf; s.loop = true;
    f.type = kind === 'wind' ? 'lowpass' : 'bandpass';
    f.frequency.value = kind === 'wind' ? 500 : kind === 'murmur' ? 350 : 1200;
    g.gain.value = kind === 'wind' ? 0.05 : kind === 'murmur' ? 0.04 : 0.015;
    s.connect(f); f.connect(g); g.connect(musicGain); s.start();
    crackleNode = s;
  }

  function tick() {
    const m = MOODS[A.mood] || MOODS.explore;
    while (nextNote < A.ctx.currentTime + 0.25) {
      const t = nextNote, beat = step % 8;
      if (Math.random() < m.pluck) {
        const deg = m.scale[Math.floor(Math.random() * m.scale.length)];
        const oct = Math.random() < .3 ? 12 : 0;
        pluck(t, m.root + 12 + deg + oct, 0.12 + Math.random() * .06, m.bright);
      }
      if (m.bass && beat % 4 === 0) bassNote(t, m.root + (beat === 4 ? m.scale[2] : 0), m.tempo * 3.5);
      if (m.drum > 0) {
        if (beat === 0 || beat === 3 || beat === 6) drum(t, true, 0.35 * m.drum);
        if (beat === 4) noiseBurst(t, .08, 2500, 0.08 * m.drum, musicGain);
      }
      if (A.mood === 'camp' || A.mood === 'ruins' || A.mood === 'boss' || A.mood === 'cindrel') if (Math.random() < .5) noiseBurst(t + Math.random() * m.tempo, .03, 3000, 0.05, musicGain);
      step++;
      nextNote += m.tempo * (0.92 + Math.random() * 0.16);
    }
  }

  A.setMood = function (mood) {
    if (!mood || mood === A.mood) return;
    A.mood = mood;
    if (!A.on || !A.ctx) return;
    startBed((MOODS[mood] || {}).noise);
  };

  A.toggle = function (on) {
    A.on = on;
    localStorage.setItem('ew.audio', on ? '1' : '0');
    if (on) {
      init();
      A.ctx.resume();
      nextNote = A.ctx.currentTime + .1;
      clearInterval(timer); timer = setInterval(tick, 100);
      startBed((MOODS[A.mood] || {}).noise);
    } else {
      clearInterval(timer);
      startBed(null);
    }
  };

  A.sfx = function (kind) {
    if (!A.on || !A.ctx) return;
    const t = A.ctx.currentTime;
    switch (kind) {
      case 'dice': for (let i = 0; i < 6; i++) noiseBurst(t + i * 0.07 + Math.random() * .03, .04, 1800 + Math.random() * 1500, 0.25 - i * .03); break;
      case 'hit': drum(t, true, .5); noiseBurst(t, .12, 900, .3); break;
      case 'miss': noiseBurst(t, .2, 3000, .12); break;
      case 'fire': noiseBurst(t, .5, 600, .35); noiseBurst(t + .05, .4, 1400, .2); break;
      case 'cold': for (let i = 0; i < 5; i++) pluck(t + i * .04, 88 + i * 2, .08, 1.5); break;
      case 'lightning': noiseBurst(t, .25, 4000, .4); noiseBurst(t + .1, .15, 2500, .3); break;
      case 'heal': [0, 4, 7, 12].forEach((d, i) => pluck(t + i * .08, 72 + d, .1, 1.3)); break;
      case 'crit': drum(t, true, .7); [0, 7, 12].forEach((d, i) => pluck(t + i * .05, 64 + d, .14, 1.4)); break;
      case 'level': [0, 4, 7, 12, 16].forEach((d, i) => pluck(t + i * .1, 67 + d, .15, 1.4)); break;
      case 'turn': pluck(t, 76, .08, 1); pluck(t + .08, 83, .08, 1); break;
      case 'win': [0, 4, 7, 12, 7, 12, 16].forEach((d, i) => pluck(t + i * .12, 60 + d, .16, 1.3)); break;
      case 'lose': [0, -1, -5, -12].forEach((d, i) => pluck(t + i * .25, 57 + d, .14, .5)); break;
    }
  };
})();
