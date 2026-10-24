/* Voices: a narrator plus a cast of distinct voices for every hero, NPC and monster.
   Uses the server's Piper neural voices when available (studio quality, same for everyone), otherwise the most
   natural voices the browser offers (Edge "Natural", Chrome "Google", Safari "Premium/Enhanced/Siri"), never the
   robotic legacy ones. Narration and quoted dialogue are split so NPC lines are spoken in the NPC's own voice. */
(function () {
  const V = EW.voice = {};
  const LS = (k, d) => { try { const v = localStorage.getItem('ew.voice.' + k); return v === null ? d : JSON.parse(v); } catch { return d; } };
  const SAVE = (k, v) => { try { localStorage.setItem('ew.voice.' + k, JSON.stringify(v)); } catch {} };
  V.cfg = {
    narrator: LS('narrator', localStorage.getItem('ew.narrator') === '1'), dialogue: LS('dialogue', true), barks: LS('barks', true),
    narratorVoice: LS('narratorVoice', ''), rate: LS('rate', 1), volume: LS('volume', 1), engine: LS('engine', 'auto'),
  };
  V.save = () => { for (const k in V.cfg) SAVE(k, V.cfg[k]); };
  let server = { tts: false, voices: [] };
  let browserVoices = [], queue = [], playing = null, audio = null;
  const listeners = [];
  /** Subscribe to speech events: fn(kind, {role, text, charIndex}) with kind 'start' | 'word' | 'end'. */
  V.listen = fn => listeners.push(fn);
  const emit = (k, d) => { for (const fn of listeners) try { fn(k, d); } catch (e) { console.warn(e); } };
  V.busy = () => !!playing || queue.length > 0;

  // ------------------------------------------------------------------ browser voice ranking
  const FEMALE = /(female|woman|aria|jenny|sonia|libby|natasha|samantha|karen|moira|tessa|serena|victoria|fiona|zira|ava|allison|susan|hazel|emma|clara|michelle|elizabeth|amy|joanna|salli|kimberly|ivy|kendra|nicole|olivia|isla|maisie|abbie|bella|hollie|ana\b|catherine|kate|martha|nora|zoe|siri female|kathy|vicki|veena|heera|neerja|ruth|kylie)/i;
  const MALE = /(male|man\b|guy|ryan|thomas|daniel|arthur|oliver|alex|fred|david|mark|george|christopher|eric|brian|andrew|william|liam|james|roger|steffan|tony|davis|jason|aaron|evan|rishi|lee|gordon|reed|ralph|albert|bruce|junior|ethan|noah|prabhat|ravi|matthew|joey|justin|siri male)/i;
  function score(v) {
    let s = 0; const n = v.name;
    if (/natural|neural|online/i.test(n)) s += 60;
    if (/premium|enhanced|siri/i.test(n)) s += 50;
    if (/^Google/i.test(n)) s += 35;
    if (/en-GB|en-IE|en-AU/i.test(v.lang)) s += 8; else if (/^en/i.test(v.lang)) s += 5; else s -= 100;
    if (/compact|espeak|desktop|zira|david|mark\b/i.test(n)) s -= 25;
    if (v.localService === false) s += 5;
    return s;
  }
  function loadVoices() {
    if (!('speechSynthesis' in window)) return;
    browserVoices = speechSynthesis.getVoices().filter(v => /^en/i.test(v.lang)).sort((a, b) => score(b) - score(a));
  }
  if ('speechSynthesis' in window) { loadVoices(); speechSynthesis.onvoiceschanged = loadVoices; }
  const gender = v => FEMALE.test(v.name) ? 'f' : MALE.test(v.name) ? 'm' : '?';
  const hash = s => { let h = 2166136261; for (const ch of String(s)) h = Math.imul(h ^ ch.charCodeAt(0), 16777619); return h >>> 0; };

  V.setServer = function (info) { server = { tts: !!info.tts, voices: info.ttsVoices || [] }; };
  V.useNeural = () => server.tts && V.cfg.engine !== 'browser';

  // ------------------------------------------------------------------ casting
  /** A role: {key, gender ('f'|'m'|'?'), pitch, rate, deep} → concrete voice choice. */
  function castBrowser(role) {
    const good = browserVoices.slice(0, 18);
    if (!good.length) return null;
    if (role.key === 'narrator' && V.cfg.narratorVoice) { const v = browserVoices.find(x => x.name === V.cfg.narratorVoice); if (v) return v; }
    let pool = good.filter(v => role.gender === '?' || gender(v) === role.gender || gender(v) === '?');
    if (role.key !== 'narrator') { const nv = castBrowser({ key: 'narrator', gender: '?' }); const alt = pool.filter(v => v !== nv); if (alt.length) pool = alt; }
    if (!pool.length) pool = good;
    // keep to the most natural tier when we can
    const top = score(pool[0]);
    const tier = pool.filter(v => score(v) >= top - 15);
    if (role.key === 'narrator') return tier[0];
    return tier[hash(role.key) % tier.length];
  }
  function castNeural(role) {
    const vs = server.voices; if (!vs.length) return null;
    if (role.key === 'narrator' && V.cfg.narratorVoice && vs.includes(V.cfg.narratorVoice)) return V.cfg.narratorVoice;
    const fem = vs.filter(v => FEMALE.test(v) || /alba|amy|jenny|kathleen|lessac|cori|kristin|ljspeech|semaine|southern_english_female|northern_english_female/i.test(v));
    const mal = vs.filter(v => !fem.includes(v));
    const pool = role.gender === 'f' && fem.length ? fem : role.gender === 'm' && mal.length ? mal : vs;
    if (role.key === 'narrator') return pool[0];
    return pool[hash(role.key) % pool.length];
  }

  V.roleFor = function (f) {
    const v = f.voice || '';
    if (v === 'narrator') return { key: 'narrator', gender: '?', pitch: .95, rate: .95 };
    if (v.startsWith('hero:')) {
      const g = (f.pronouns || '').startsWith('she') ? 'f' : (f.pronouns || '').startsWith('he') ? 'm' : '?';
      const sp = f.species || '';
      const pitch = { dwarf: .82, goliath: .78, orc: .8, dragonborn: .75, halfling: 1.18, gnome: 1.22, elf: 1.05, tiefling: .95 }[sp] || 1;
      return { key: v, gender: g, pitch, rate: 1 };
    }
    if (v.startsWith('npc:')) {
      const n = v.slice(4);
      const g = /\b(mother|sister|lady|queen|mira|wren|lark|eloweth|maelis|odessa|hrolla|isolde|dagna|cindrel|witch|madam|mistress)\b/i.test(n) ? 'f'
        : /\b(king|brother|lord|old fen|hollis|vesk|snikkit|tobin|hobb|captain|foreman|father|sir)\b/i.test(n) ? 'm' : (hash(n) % 2 ? 'f' : 'm');
      const pitch = /snikkit|goblin|kobold|imp|sprite|flicker/i.test(n) ? 1.45 : /vharkas|dragon|tyrant|nyx|warden|giant|ogre|mother brine/i.test(n) ? .55 : .9 + (hash(n) % 30) / 100;
      return { key: v, gender: g, pitch, rate: /old|elder|ancient/i.test(n) ? .9 : 1 };
    }
    // monsters by creature type
    const t = v || 'humanoid';
    const deep = { dragon: .45, giant: .6, undead: .6, construct: .55, monstrosity: .6, beast: .6, elemental: 1.3, fiend: .7 }[t] || .85;
    return { key: 'monster:' + t + ':' + (f.name || ''), gender: 'm', pitch: f.boss ? Math.min(deep, .55) : deep, rate: .95 };
  };

  // ------------------------------------------------------------------ playback queue
  function clean(t) { return String(t || '').replace(/\*\*?/g, '').replace(/[_#`]/g, '').replace(/\(([^)]*)\)/g, '$1').replace(/\s+/g, ' ').trim(); }

  function enqueue(text, role, priority) {
    text = clean(text);
    if (!text) return;
    if (priority >= 2) { queue = queue.filter(q => q.priority >= 2); }
    // split long text into sentences for natural pacing (and to dodge Chrome's 15-second cut-off)
    const parts = text.match(/[^.!?…]+[.!?…]+["”']?|\S[^.!?…]*$/g) || [text];
    for (const p of parts) if (p.trim()) queue.push({ text: p.trim(), role, priority });
    if (queue.length > 30) queue.splice(0, queue.length - 30);
    pump();
  }

  function pump() {
    if (playing || !queue.length) return;
    const item = queue.shift();
    playing = item;
    emit('start', item);
    let finished = false;
    const done = () => { if (finished) return; finished = true; emit('end', item); playing = null; setTimeout(pump, item.role.key === 'narrator' ? 120 : 220); };
    if (V.useNeural()) {
      const voice = castNeural(item.role);
      const url = `/api/tts?voice=${encodeURIComponent(voice || '')}&rate=${(V.cfg.rate * (item.role.rate || 1)).toFixed(2)}&text=${encodeURIComponent(item.text)}`;
      audio = new Audio(url);
      audio.volume = V.cfg.volume;
      // pitch for monsters and small folk via playback rate (Piper has no pitch control)
      const pr = item.role.pitch && Math.abs(item.role.pitch - 1) > .15 ? Math.max(.7, Math.min(1.3, item.role.pitch)) : 1;
      audio.preservesPitch = false; audio.mozPreservesPitch = false; audio.webkitPreservesPitch = false;
      audio.playbackRate = pr;
      audio.onended = done; audio.onerror = () => { server.tts = false; done(); };
      audio.play().catch(done);
      return;
    }
    if (!('speechSynthesis' in window)) { done(); return; }
    const u = new SpeechSynthesisUtterance(item.text);
    const v = castBrowser(item.role);
    if (v) { u.voice = v; u.lang = v.lang; }
    // natural voices sound best near their defaults; push pitch harder only for monsters and small folk
    const natural = v && /natural|neural|online|premium|enhanced/i.test(v.name);
    const pitch = item.role.pitch || 1;
    u.pitch = natural ? 1 + (pitch - 1) * .6 : pitch;
    u.rate = Math.max(.6, Math.min(1.6, V.cfg.rate * (item.role.rate || 1) * (item.role.key === 'narrator' ? .96 : 1.02)));
    u.volume = V.cfg.volume;
    u.onend = done; u.onerror = done;
    u.onboundary = e => emit('word', { ...item, charIndex: e.charIndex });
    speechSynthesis.speak(u);
    // safety net: some browsers never fire onend
    setTimeout(() => { if (playing === item && !speechSynthesis.speaking) done(); }, 400 + item.text.length * 90);
  }

  V.stop = function () {
    queue = [];
    if (playing) emit('end', playing);
    playing = null;
    if ('speechSynthesis' in window) speechSynthesis.cancel();
    if (audio) { audio.pause(); audio = null; }
  };

  // ------------------------------------------------------------------ public helpers
  /** Read a scene: narration in the narrator's voice, quoted lines in the NPC's voice. */
  V.speakScene = function (scene) {
    if (!V.cfg.narrator || !scene) return;
    V.stop();
    const npcRole = scene.npc && scene.npc.name ? V.roleFor({ voice: 'npc:' + scene.npc.name }) : null;
    const narr = V.roleFor({ voice: 'narrator' });
    if (scene.title) enqueue(scene.title + '.', narr, 1);
    if (npcRole && scene.npc.line) enqueue(scene.npc.line, npcRole, 1);
    const text = String(scene.text || '');
    const re = /“([^”]+)”|"([^"]+)"/g;
    let last = 0, m;
    while ((m = re.exec(text))) {
      if (m.index > last) enqueue(text.slice(last, m.index), narr, 1);
      enqueue(m[1] || m[2], npcRole || narr, 1);
      last = re.lastIndex;
    }
    if (last < text.length) enqueue(text.slice(last), narr, 1);
  };

  /** The lore-teller's own asides: spoken only if nothing else is being said. Returns true if it was voiced. */
  V.aside = function (text) {
    if (!V.cfg.narrator && !V.cfg.dialogue) return false;
    if (V.busy()) return false;
    enqueue(text, V.roleFor({ voice: 'narrator' }), 0);
    return true;
  };
  /** Which way the narrator's voice leans, so the lore-teller on screen can match it. */
  V.narratorGender = function () {
    if (V.useNeural()) { const v = castNeural({ key: 'narrator', gender: '?' }) || ''; return FEMALE.test(v) || /alba|amy|jenny|kathleen|lessac|cori|kristin|ljspeech|female/i.test(v) ? 'f' : 'm'; }
    const v = castBrowser({ key: 'narrator', gender: '?' });
    return v ? (gender(v) === 'f' ? 'f' : 'm') : 'm';
  };
  V.narrate = function (text) { if (V.cfg.narrator || V.cfg.dialogue) enqueue(text, V.roleFor({ voice: 'narrator' }), 1); };

  /** Somebody speaks in character (players or NPCs). */
  V.speech = function (f) {
    if (!V.cfg.dialogue) return;
    const role = V.roleFor(f);
    enqueue(f.kind === 'do' ? (f.name + ' ' + f.text) : f.text, f.kind === 'do' ? V.roleFor({ voice: 'narrator' }) : role, 2);
  };

  /** Battle chatter. */
  V.bark = function (f) {
    if (!V.cfg.barks || !V.cfg.dialogue || queue.length > 2) return;
    if (/^\*.*\*$/.test(f.text)) return; // growls and hisses are better read than spoken
    const role = f.side === 'party' && f.voice ? V.roleFor({ voice: 'hero:' + f.voice, pronouns: (EW.state.heroes.find(h => h.id === f.voice) || {}).pronouns, species: (EW.state.heroes.find(h => h.id === f.voice) || {}).species }) : V.roleFor({ voice: f.voice, name: f.name, boss: f.boss });
    enqueue(f.text, role, 1);
  };

  V.test = function (who) {
    V.stop();
    if (who === 'narrator') enqueue('The rain has been falling for three days, and it is not alone. Somewhere in the Thornwood, something is waking.', V.roleFor({ voice: 'narrator' }), 2);
    else enqueue('Well met, traveller! Pull up a chair — the stew is hot and the ale is honest.', V.roleFor({ voice: 'npc:Mira Holloway' }), 2);
  };

  /** Settings panel (used in the ☰ menu). */
  V.panel = function () {
    const h = EW.h;
    const box = h('div.menu-list');
    const chk = (k, label) => h('label.row', h('input', { type: 'checkbox', checked: V.cfg[k], onchange: e => { V.cfg[k] = e.target.checked; V.save(); if (!e.target.checked) V.stop(); } }), label);
    box.append(chk('narrator', 'Narrator reads each scene aloud'), chk('dialogue', 'Speak dialogue aloud (players, NPCs, the DM)'), chk('barks', 'Battle chatter'));
    const engine = h('select', { onchange: e => { V.cfg.engine = e.target.value; V.save(); } },
      h('option', { value: 'auto', selected: V.cfg.engine === 'auto' }, server.tts ? 'Neural voices from the server (best)' : 'Best voices in this browser'),
      h('option', { value: 'browser', selected: V.cfg.engine === 'browser' }, 'Browser voices only'));
    const list = V.useNeural() ? server.voices : browserVoices.slice(0, 25).map(v => v.name);
    const narr = h('select', { onchange: e => { V.cfg.narratorVoice = e.target.value; V.save(); } }, h('option', { value: '' }, 'Automatic (most natural)'), list.map(n => h('option', { value: n, selected: V.cfg.narratorVoice === n }, n)));
    const rate = h('input', { type: 'range', min: '0.7', max: '1.4', step: '0.05', value: V.cfg.rate, oninput: e => { V.cfg.rate = parseFloat(e.target.value); V.save(); } });
    const vol = h('input', { type: 'range', min: '0', max: '1', step: '0.05', value: V.cfg.volume, oninput: e => { V.cfg.volume = parseFloat(e.target.value); V.save(); } });
    box.append(h('label.field', h('span', 'Voice engine'), engine), h('label.field', h('span', 'Narrator voice'), narr),
      h('div.two', h('label.field', h('span', 'Speed'), rate), h('label.field', h('span', 'Volume'), vol)),
      h('div.row.gap', h('button.btn.small', { onclick: () => V.test('narrator') }, 'Test narrator'), h('button.btn.small', { onclick: () => V.test('npc') }, 'Test an NPC'), h('button.btn.small.ghost', { onclick: () => V.stop() }, 'Stop')));
    if (!server.tts) box.append(h('p.hint', 'Tip: Microsoft Edge has very natural “Online (Natural)” voices built in. For studio-quality voices for everyone, the host can add Piper (see README → Natural voices).'));
    else if (!V.useNeural()) box.append(h('p.hint', 'Neural voices are available from the server — choose them above.'));
    if (!V.useNeural() && browserVoices.length && !/natural|neural|online|premium|enhanced|google|siri/i.test(browserVoices[0].name)) box.append(h('p.hint', 'This browser only has basic voices. Try Edge or Chrome, or install extra voices in your system settings.'));
    return box;
  };
})();
