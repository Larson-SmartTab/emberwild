/* Emberwild client: joins the table, listens for live updates, renders lobby / story / battle / ending. */
(function () {
  const h = EW.h, $ = id => document.getElementById(id);
  let S = null;                  // latest server state
  EW.state = { players: [], heroes: [] };
  let showLobby = false, lastSceneKey = '', lastLogTs = 0, lastPhase = '', es = null, sceneTs = 0, talkKind = 'say';

  // ================================================================ join & connection
  async function join(name) {
    const r = await EW.api('/api/join', { id: EW.me.id || null, name });
    if (!r.ok) return;
    EW.me.id = r.id; EW.me.name = r.name; EW.me.host = r.host;
    localStorage.setItem('ew.pid', r.id); localStorage.setItem('ew.name', r.name);
    EW.api('/api/info', undefined, { quiet: true }).then(i => { if (i && i.urls) EW.voice.setServer(i); });
    EW.api('/api/saves', undefined, { quiet: true }).then(r => { EW._hasAutosave = r && (r.saves || []).includes('autosave'); if (S) renderLobby(); });
    $('joinBox').classList.add('hidden');
    $('lobbyMain').classList.remove('hidden');
    connect();
  }

  function connect() {
    if (es) es.close();
    es = new EventSource('/api/events?pid=' + encodeURIComponent(EW.me.id));
    es.onmessage = ev => {
      let m; try { m = JSON.parse(ev.data); } catch { return; }
      if (m.state) applyState(m.state);
      for (const f of m.fx || []) handleFx(f);
    };
    es.onerror = () => { setTimeout(() => { if (es.readyState === 2) connect(); }, 2000); };
  }

  $('joinBtn').onclick = () => { const n = $('nameInput').value.trim(); if (!n) return EW.toast('Tell the table your name first.'); join(n); };
  $('nameInput').addEventListener('keydown', e => { if (e.key === 'Enter') $('joinBtn').click(); });
  if (EW.me.id && EW.me.name) join(EW.me.name); else { $('nameInput').value = EW.me.name || ''; $('nameInput').focus(); }

  // ================================================================ state
  function applyState(s) {
    S = s; EW.state = s;
    const meP = s.players.find(p => p.id === EW.me.id);
    if (meP) EW.me.host = meP.host;
    renderTop();
    const inGame = s.phase !== 'lobby' && s.campaign;
    if (!inGame || showLobby) { renderLobby(); show('lobby'); return; }
    show('table');
    renderParty();
    renderLog();
    EW.tale.combat(s.phase === 'combat' && !!s.combat);
    if (s.phase === 'combat' && s.combat) {
      $('story').classList.add('hidden'); $('ending').classList.add('hidden'); $('battle').classList.remove('hidden');
      EW.scene.dim(.55);
      EW.audio.setMood(s.combat.music || 'battle');
      EW.battle.update(s.combat);
    } else if (s.phase === 'end') {
      EW.battle.reset();
      $('story').classList.add('hidden'); $('battle').classList.add('hidden'); $('ending').classList.remove('hidden');
      EW.scene.dim(.3);
      renderEnding();
    } else {
      if (lastPhase === 'combat') EW.battle.reset();
      $('battle').classList.add('hidden'); $('ending').classList.add('hidden'); $('story').classList.remove('hidden');
      EW.scene.dim(0);
      renderStory();
      EW.tale.update(s);
    }
    lastPhase = s.phase;
  }

  function show(which) {
    $('lobby').classList.toggle('hidden', which !== 'lobby');
    $('table').classList.toggle('hidden', which !== 'table');
    if (which === 'table') requestAnimationFrame(() => EW.battle.layout());
  }

  function renderTop() {
    $('campaignTitle').textContent = S.campaign ? S.campaign.title : 'Emberwild';
    $('chapterLine').textContent = S.scene ? [S.scene.chapter, S.scene.title].filter(Boolean).join(' — ') : '';
    const pl = $('playersOnline'); pl.innerHTML = '';
    for (const p of S.players) pl.append(h('div.pchip' + (p.online ? '' : '.off') + (p.host ? '.host' : ''), { style: { background: p.color }, title: p.name + (p.host ? ' (host)' : '') + (p.online ? '' : ' — away') }, p.name.slice(0, 2)));
  }

  // ================================================================ lobby
  function renderLobby() {
    const party = $('lobbyParty'); party.innerHTML = '';
    if (!S.heroes.length) party.append(h('p.hint', 'No heroes yet. Create one, import one, or take a ready-made hero below.'));
    for (const hero of S.heroes) party.append(heroCard(hero, true));
    // pregens
    const pg = $('pregenList'); pg.innerHTML = '';
    for (const c of S.campaigns) for (const p of c.pregenList || []) {
      if (S.heroes.some(x => x.name === p.name)) continue;
      pg.append(h('button.pregen', { onclick: () => claim(c.id, p.id) }, h('span.em', p.emblem), h('span', h('b', p.name), h('small', p.summary), h('div.motto', p.motto))));
    }
    // campaigns
    const cl = $('campaignList'); cl.innerHTML = '';
    const inGame = S.phase !== 'lobby' && S.campaign;
    if (!inGame && EW.me.host && EW._hasAutosave) cl.append(h('div.panel', { style: { marginBottom: '6px' } }, h('p', 'Your last adventure was saved automatically.'),
      h('button.btn.primary', { onclick: async () => { const r = await EW.api('/api/load', { name: 'autosave' }); if (r.ok) { showLobby = false; lastSceneKey = ''; } } }, 'Continue your last adventure')));
    if (inGame) cl.append(h('div.panel', { style: { marginBottom: '6px' } }, h('p', `A tale is under way: `, h('b', S.campaign.title), '.'), h('button.btn.primary', { onclick: () => { showLobby = false; applyState(S); } }, 'Return to the table')));
    for (const c of S.campaigns) {
      const cover = h('canvas');
      const card = h('div.camp' + (c.core ? '.core' : ''), cover, h('div.body',
        h('h3', c.title), h('div.by', `by ${c.author}`), h('p', c.synopsis),
        h('div.facts', `${c.scenes} scenes · ${c.battles} battles · ${c.checks} skill checks`),
        c.hooks && c.hooks.length ? h('div.hookline', 'Also weaves into other tales at: ' + c.hooks.map(x => x.anchor).join(', ')) : null,
        h('div.row', { style: { marginTop: '8px' } }, h('button.btn' + (c.core ? '.primary' : ''), { onclick: () => begin(c) }, inGame ? 'Start this tale instead' : 'Begin this tale'))));
      cl.append(card);
      requestAnimationFrame(() => EW.scene.paintCover(cover, c.cover));
    }
    const mine = S.heroes.filter(x => x.player === EW.me.id);
    $('startHint').textContent = mine.length ? `${S.heroes.filter(x => !x.npc).length} hero(es) ready. Companions join automatically if your party is small.` : 'Bring a hero to the table, then choose a tale.';
  }

  async function claim(cid, pid) { const r = await EW.api('/api/hero/pregen', { campaign: cid, id: pid }); if (r.ok) EW.toast('Hero claimed!', true); }
  async function begin(c) {
    if (S.phase !== 'lobby' && S.campaign && !confirm(`Abandon “${S.campaign.title}” and start “${c.title}”? (Use the menu to save first.)`)) return;
    const r = await EW.api('/api/start', { campaign: c.id });
    if (r.ok) { showLobby = false; lastSceneKey = ''; }
  }

  $('btnBuild').onclick = () => EW.builder.open();
  $('importHero').onchange = async e => {
    const f = e.target.files[0]; if (!f) return;
    try {
      const j = JSON.parse(await f.text());
      const r = j.spec ? await EW.api('/api/hero/create', { spec: j.spec }) : await EW.api('/api/hero/import', { hero: j.hero || j });
      if (r.ok) EW.toast('Hero imported!', true);
    } catch (err) { EW.toast('That file is not a hero file: ' + err.message); }
    e.target.value = '';
  };

  // ================================================================ party
  function heroCard(hero, lobby) {
    const cur = S.combat && EW.battle.current();
    const isTurn = cur && cur.heroId === hero.id;
    const pc = h('canvas.portrait');
    EW.portrait(pc, hero, 46);
    const hpPct = Math.max(0, hero.hp) / hero.maxHp * 100;
    const owner = S.players.find(p => p.id === hero.player);
    const card = h('div.hero-card' + (hero.player === EW.me.id ? '.mine' : '') + (isTurn ? '.turn' : '') + (hero.hp <= 0 ? '.down' : ''), { onclick: () => openSheet(hero.id), title: 'Open character sheet' },
      h('div.hc-top', pc, h('div', h('div.hc-name', hero.name), h('div.hc-sub', `L${hero.level} ${hero.speciesName} ${hero.className}${owner && !hero.npc ? ' · ' + owner.name : ''}`, hero.npc ? h('span.npc-tag', 'companion') : null))),
      h('div.bar', h('i', { style: { width: hpPct + '%' } }), hero.tempHp ? h('i.temp', { style: { width: Math.min(100, hero.tempHp / hero.maxHp * 100) + '%', left: 'auto', right: 0 } }) : null),
      h('div.hc-stats', h('span', `${Math.max(0, hero.hp)}/${hero.maxHp} HP${hero.tempHp ? ' +' + hero.tempHp : ''}`), h('span', `AC ${hero.ac}`), h('span', `${hero.gold} gp`)));
    if (hero.slotsMax && hero.slotsMax.length) {
      const sl = h('div.slots', { title: 'Spell slots' });
      hero.slotsMax.forEach((m, i) => { for (let k = 0; k < m; k++) sl.append(h('span.pip' + (k < hero.slots[i] ? '.full' : ''), { title: `Level ${i + 1} slot` })); if (i < hero.slotsMax.length - 1) sl.append(h('span', { style: { width: '6px' } })); });
      card.append(sl);
    }
    const unit = S.combat && S.combat.units.find(u => u.heroId === hero.id);
    const conds = unit ? unit.conds : [];
    const extra = [];
    if (hero.inspiration) extra.push(h('span.cond.good', '✨ inspired'));
    if (hero.mageArmor) extra.push(h('span.cond.good', 'mage armor'));
    if (hero.dead) extra.push(h('span.cond.bad', 'dead'));
    if (conds.length || extra.length) card.append(h('div.conds', extra, conds.filter(c => !['ending', 'autoplaying', 'savage_used', 'cleaved', 'barked_bloodied'].includes(c)).map(c => h('span.cond.' + (EW.condClass(c) || 'x'), EW.condLabel(c)))));
    if (lobby && (hero.player === EW.me.id || EW.me.host) && !hero.npc) card.append(h('button.btn.small.ghost', { style: { marginTop: '6px' }, onclick: async e => { e.stopPropagation(); if (confirm(`Remove ${hero.name} from the party?`)) await EW.api('/api/hero/remove', { hero: hero.id }); } }, 'Remove'));
    return card;
  }

  function renderParty() {
    const p = $('partyPanel'); p.innerHTML = '';
    for (const hero of S.heroes) p.append(heroCard(hero, false));
    if (!S.heroes.some(x => x.player === EW.me.id)) p.append(h('p.hint', 'You are watching. Open the lobby (menu) to bring a hero into the tale, or open an unclaimed hero\'s sheet and choose “Play this hero”.'));
  }

  // ================================================================ story
  function renderStory() {
    const sc = S.scene;
    if (!sc) return;
    const key = sc.campaign + ':' + sc.id;
    const fresh = key !== lastSceneKey;
    if (fresh) {
      lastSceneKey = key;
      EW.scene.show(sc.backdrop, sc.time, sc.weather);
      EW.audio.setMood(sc.music);
      EW.audio.sfx('page');
      $('story').scrollTop = 0;
      // the lore-teller turns the page
      $('page').animate([{ opacity: 0, transform: 'perspective(1600px) rotateY(-28deg) translateX(-40px)', transformOrigin: 'left center' }, { opacity: 1, transform: 'perspective(1600px) rotateY(0) translateX(0)', transformOrigin: 'left center' }], { duration: 650, easing: 'cubic-bezier(.2,.7,.3,1)' });
      sceneTs = Date.now() - 500;
      EW.voice.speakScene(sc);
      EW.tale.scene(sc, key);
    }
    $('sceneTitle').textContent = sc.title;
    const html = EW.md(sc.text);
    if (fresh || $('sceneText')._html !== html) {
      $('sceneText').innerHTML = html; $('sceneText')._html = html;
      if (fresh) [...$('sceneText').children].forEach((el, i) => el.animate([{ opacity: 0, transform: 'translateY(6px)', filter: 'blur(2px)' }, { opacity: 1, transform: 'none', filter: 'none' }], { duration: 700, delay: 250 + i * 380, fill: 'backwards', easing: 'ease-out' }));
    }
    const npc = $('npcCard');
    if (sc.npc && sc.npc.name) {
      const nk = sc.npc.name + '|' + (sc.npc.line || '');
      if (npc._k !== nk || fresh) {
        npc._k = nk;
        npc.classList.remove('hidden'); npc.innerHTML = '';
        const cv = h('canvas'); EW.medallion(cv, sc.npc.color, sc.npc.glyph, 76);
        npc.append(cv, h('div', h('div.who', sc.npc.name), h('div.role', sc.npc.title || ''), sc.npc.line ? h('div.line', '“' + sc.npc.line + '”') : null));
        npc.dataset.voice = 'npc:' + sc.npc.name;
        if (fresh) npc.animate([{ opacity: 0, transform: 'translateX(-24px) scale(.96)' }, { opacity: 1, transform: 'none' }], { duration: 600, delay: 200, fill: 'backwards', easing: 'cubic-bezier(.2,.8,.3,1.2)' });
      }
    } else { npc.classList.add('hidden'); npc._k = ''; npc.dataset.voice = ''; }
    const oc = $('outcome');
    if (S.outcome && S.outcome.text) { oc.classList.remove('hidden'); oc.className = 'outcome' + (S.outcome.success ? '' : ' fail'); oc.innerHTML = EW.md(S.outcome.text); }
    else oc.classList.add('hidden');
    renderRolls();
    renderChoices();
    renderShop();
    renderDialogue();
    renderTalk();
  }

  function renderRolls() {
    const rp = $('rollPrompt');
    if (!S.pending.length) { rp.classList.add('hidden'); return; }
    rp.classList.remove('hidden'); rp.innerHTML = '';
    for (const p of S.pending) {
      const hero = S.heroes.find(x => x.id === p.hero) || {};
      const mine = p.player === EW.me.id;
      const row = h('div.roll-row', h('div', h('div.who', p.label), h('div.hint', `${EW.sign(p.bonus)} bonus${p.adv ? ' · Advantage' : ''}${hero.inspiration ? ' · Heroic Inspiration will reroll a failure' : ''} · ${p.playerName}`)));
      if (p.done) row.append(h('span.roll-done', '🎲 ' + p.total));
      else if (mine || EW.me.host) row.append(h('button.btn.primary.roll-btn', { onclick: () => EW.api('/api/roll', { hero: p.hero }) }, mine ? '🎲 Roll d20' : `Roll for ${p.playerName}`));
      else row.append(h('span.hint', `waiting for ${p.playerName}… (${p.secondsLeft}s)`));
      rp.append(row);
    }
  }

  function renderChoices() {
    const ol = $('choices'); ol.innerHTML = '';
    const busy = S.pending.length > 0;
    const myHero = S.heroes.some(x => x.player === EW.me.id && !x.npc);
    S.scene.choices.forEach((c, i) => {
      const voted = c.votes.includes(EW.me.id);
      const tags = h('span.tags');
      if (c.checkLabel) tags.append(h('span.tag.t-check', `${c.checkLabel} DC ${c.dc}${c.mode === 'group' ? ' · everyone' : c.mode === 'each' ? ' · each of you' : ''}`));
      if (c.spellName) tags.append(h('span.tag.t-spell', `✦ ${c.spellName}${c.caster ? ' (' + c.caster.split(' ')[0] + ')' : ' — nobody can cast'}`));
      if (c.battle) tags.append(h('span.tag.t-battle', '⚔ battle'));
      if (c.hook) tags.append(h('span.tag.t-hook', 'another tale'));
      const vs = h('span.voters');
      for (const v of c.votes) { const p = S.players.find(x => x.id === v); if (p) vs.append(h('i', { style: { background: p.color }, title: p.name })); }
      tags.append(vs);
      const btn = h('button.choice' + (voted ? '.voted' : '') + (c.battle ? '.battle-choice' : ''), { disabled: busy || !!c.disabled || (!myHero && !EW.me.host) || (c.spell && !c.caster), title: c.disabled || '', onclick: () => EW.api('/api/vote', { choice: c.id }),
        onmouseenter: c.battle ? () => EW.tale.lean(true) : null, onfocus: c.battle ? () => EW.tale.lean(true) : null,
        onmouseleave: c.battle ? () => { if (!S.scene.choices.some(x => x.battle && x.votes.length)) EW.tale.lean(false); } : null },
        h('span.ic', c.icon || '•'), h('span.lbl', c.label, c.hint ? h('span.sub', c.hint) : c.disabled ? h('span.sub', c.disabled) : null), tags);
      ol.append(h('li', btn));
    });
    const vi = $('voteInfo'); vi.innerHTML = '';
    const votes = S.scene.choices.reduce((n, c) => n + c.votes.length, 0);
    if (votes && S.voteSeconds) {
      vi.append(`The table is deciding… ${S.voteSeconds}s `);
      if (EW.me.host) vi.append(h('button.btn.small', { onclick: () => EW.api('/api/force', {}) }, 'Decide now'));
    } else if (!myHero) vi.append('Create a hero in the lobby to take part in decisions.');
  }

  function renderShop() {
    const sh = $('shop');
    if (!S.scene.shop || !S.stock.length) { sh.classList.add('hidden'); return; }
    sh.classList.remove('hidden'); sh.innerHTML = '';
    const mine = S.heroes.filter(x => x.player === EW.me.id && !x.npc);
    if (!mine.length) return;
    const who = mine[0];
    const grid = h('div.shop-grid');
    for (const it of S.stock) grid.append(h('div.ware', h('b', it.name), h('span.price', it.value + ' gp'), h('p', it.desc),
      h('button.btn.small', { disabled: who.gold < it.value, onclick: () => EW.api('/api/shop', { hero: who.id, item: it.id }) }, `Buy for ${who.name.split(' ')[0]}`)));
    sh.append(h('h3', `Tobin's wares — ${who.name.split(' ')[0]} has ${who.gold} gp`), grid);
  }

  // ================================================================ in-character dialogue
  function renderDialogue() {
    const box = $('dialogue');
    const lines = S.log.filter(e => ['speech', 'deed', 'npcspeech'].includes(e.style) && e.ts >= sceneTs).slice(-6);
    if (!lines.length) { box.classList.add('hidden'); return; }
    box.classList.remove('hidden'); box.innerHTML = '';
    for (const e of lines) {
      const said = e.text.includes('“') ? e.text.slice(e.text.indexOf('“') + 1).replace(/”$/, '') : e.text;
      box.append(h('div.dl' + (e.style === 'npcspeech' ? '.dl-npc' : e.style === 'deed' ? '.dl-deed' : ''),
        h('span.who', { style: { background: e.color || '#888' } }, (e.who || '?').slice(0, 2)),
        h('span.said', h('b', e.who || ''), said)));
    }
  }

  function myHeroes() { return S.heroes.filter(x => x.player === EW.me.id && !x.npc); }

  function renderTalk() {
    const mine = myHeroes();
    const t = $('talk');
    t.classList.toggle('hidden', !mine.length);
    const sel = $('talkHero');
    const cur = sel.value;
    if (sel.options.length !== mine.length || [...sel.options].some((o, i) => o.value !== mine[i].id)) {
      sel.innerHTML = '';
      for (const x of mine) sel.append(h('option', { value: x.id }, x.name.split(' ')[0]));
      if (cur) sel.value = cur;
    }
    sel.classList.toggle('hidden', mine.length < 2);
    $('dmLabel').textContent = S.dm ? 'DM: ' + S.dm.label : '';
    $('dmThinking').classList.toggle('hidden', !S.dmBusy);
  }

  for (const b of document.querySelectorAll('#talk .seg button')) b.onclick = () => {
    talkKind = b.dataset.kind;
    document.querySelectorAll('#talk .seg button').forEach(x => x.classList.toggle('on', x === b));
    $('talkInput').placeholder = talkKind === 'say' ? '“Mira, love — what\'s the strangest thing you\'ve heard tonight?”' : 'leans over the bar and slides a gold coin toward Mira…';
  };
  async function sendTalk(text, kind) {
    if (!text.trim()) return;
    const r = await EW.api('/api/say', { hero: $('talkHero').value, text, kind });
    if (r.ok) $('talkInput').value = '';
  }
  $('talk').onsubmit = e => { e.preventDefault(); sendTalk($('talkInput').value, talkKind); };

  // microphone: speak your line, it's typed for you and sent
  const Rec = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!Rec) $('talkMic').classList.add('hidden');
  else {
    let rec = null;
    $('talkMic').onclick = () => {
      if (rec) { rec.stop(); return; }
      rec = new Rec(); rec.lang = 'en-US'; rec.interimResults = true; rec.continuous = false;
      $('talkMic').classList.add('listening');
      rec.onresult = ev => { let t = ''; for (const r of ev.results) t += r[0].transcript; $('talkInput').value = t; if (ev.results[ev.results.length - 1].isFinal) sendTalk(t, talkKind); };
      rec.onend = () => { $('talkMic').classList.remove('listening'); rec = null; };
      rec.onerror = () => { EW.toast('The microphone is not available (allow access in your browser).'); };
      rec.start();
    };
  }

  // ================================================================ ending
  function renderEnding() {
    const e = $('ending'); e.innerHTML = '';
    EW.scene.show('tavern', 'night', 'none');
    EW.audio.setMood('triumph');
    const box = h('div.panel', h('h1.title-art', 'The tale is told'),
      h('p.tagline', S.campaign ? S.campaign.title : ''),
      h('div.statline', h('div', h('b', S.stats.minutes + ''), 'minutes'), h('div', h('b', S.stats.battlesWon + ''), 'battles won'), h('div', h('b', S.heroes.length + ''), 'heroes')),
      h('div.row.wrap.gap', { style: { justifyContent: 'center' } }, S.heroes.map(hr => { const c = h('canvas'); EW.portrait(c, hr, 84); return h('div', { style: { textAlign: 'center' } }, c, h('div', hr.name), h('small.hint', `L${hr.level} ${hr.className}`)); })),
      h('p.hint', { style: { marginTop: '16px' } }, 'Want more? Build the next chapter in the Campaign Forge and weave it into this one.'),
      h('div.row.gap', { style: { justifyContent: 'center' } }, h('button.btn.primary', { onclick: () => { showLobby = true; applyState(S); } }, 'Choose another tale'), h('button.btn', { onclick: () => EW.forge.open('build') }, 'Open the Campaign Forge')));
    e.append(box);
  }

  // ================================================================ log & chat
  function renderLog() {
    const el = $('log');
    const last = S.log.length ? S.log[S.log.length - 1].ts : 0;
    if (last === lastLogTs && el.childElementCount) return;
    lastLogTs = last;
    const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60;
    el.innerHTML = '';
    for (const e of S.log) {
      if (e.style === 'chat') el.append(h('div.le.l-chat', h('b', { style: { color: e.color } }, e.who + ':'), e.text));
      else el.append(h('div.le.l-' + (e.style || 'info'), { html: EW.md(e.text).replace(/^<p>|<\/p>$/g, '') }));
    }
    if (atBottom || true) el.scrollTop = el.scrollHeight;
  }
  $('chatForm').onsubmit = e => {
    e.preventDefault();
    const t = $('chatInput').value.trim();
    if (!t) return;
    if ($('chatMode').value === 'say' && !t.startsWith('/')) { const mine = myHeroes(); EW.api('/api/say', { hero: mine.length ? mine[0].id : '', text: t, kind: 'say' }); }
    else EW.api('/api/chat', { text: t });
    $('chatInput').value = '';
  };

  // ================================================================ fx
  function handleFx(f) {
    if (S && S.phase === 'combat') EW.battle.fx(f);
    EW.tale.fx(f);
    const story = !S || S.phase !== 'combat';
    switch (f.t) {
      case 'gold': if (f.amount > 0) { EW.storyfx.play('coins', { amount: f.amount }); EW.audio.sfx('coin'); } break;
      case 'loot': EW.storyfx.play('loot', { glyph: ({ potion: '🧪', ring: '💍', cloak: '🧥', amulet: '📿', scroll: '📜', wand: '🪄', weapon: '⚔️', armor: '🛡️', key: '🗝️', gear: '🎒', trinket: '🔮' })[(f.item && (f.item.kind || f.item.type)) || ''] || '✦', label: f.item && f.item.name }); EW.audio.sfx('loot'); break;
      case 'rest': EW.storyfx.play('heal'); break;
      case 'levelUp': EW.storyfx.play('level'); break;
      case 'storySpell': EW.storyfx.play(['fire', 'cold', 'lightning'].includes(f.dtype) ? f.dtype : (f.dtype || 'arcane')); break;
      case 'storyHurt': EW.storyfx.play('hurt', { amount: f.amount }); break;
      case 'checkResult': if (story) EW.storyfx.play(f.success ? 'success' : 'fail'); break;
    }
    switch (f.t) {
      case 'roll': {
        if (f.quiet) break;
        const mine = f.player && f.player === EW.me.name;
        const inCombat = S && S.phase === 'combat';
        const playerColor = (S.players.find(p => p.name === f.player) || {}).color;
        if (!inCombat || mine || f.roll.natural === 20 || f.roll.natural === 1 || /Death Saving/.test(f.label)) EW.dice.show(f.roll, { label: f.label, player: f.player, fast: inCombat, color: mine && playerColor ? EW.shade(playerColor, -60) : undefined });
        break;
      }
      case 'battle': banner(f.title, 'Roll for initiative!'); EW.audio.sfx('crit'); break;
      case 'battleEnd': banner(f.result === 'victory' ? 'Victory!' : 'Defeat', f.title, f.result !== 'victory'); EW.audio.sfx(f.result === 'victory' ? 'win' : 'lose'); break;
      case 'levelUp': banner(`Level ${f.level}!`, `${f.name} grows stronger`); EW.audio.sfx('level'); EW.toast(`${f.name}: ${f.gains.join(', ')}`, true); break;
      case 'loot': EW.toast(`${f.hero} found ${f.item.name}`, true); break;
      case 'storySpell': flash(f.dtype); EW.audio.sfx(f.dtype === 'cold' ? 'cold' : f.dtype === 'lightning' ? 'lightning' : f.dtype === 'fire' ? 'fire' : 'heal'); break;
      case 'storyHurt': $('page').animate([{ transform: 'translateX(-6px)' }, { transform: 'translateX(6px)' }, { transform: 'none' }], { duration: 250 }); EW.audio.sfx('hit'); break;
      case 'rest': EW.audio.sfx('heal'); break;
      case 'phase': banner('', f.text, true); break;
      case 'heroJoined': EW.toast(`${f.name} joins the party`, true); break;
      case 'speech': EW.voice.speech(f); break;
      case 'narrate': EW.voice.narrate(f.text); break;
      case 'turn': break;
    }
  }

  // the NPC card glows while that NPC is speaking
  EW.voice.listen((kind, item) => {
    const npc = $('npcCard');
    if (!npc || !item.role || item.role.key !== npc.dataset.voice) return;
    npc.classList.toggle('talking', kind !== 'end');
  });

  function banner(big, small, lose) {
    const b = $('banner');
    b.innerHTML = '';
    b.append(h('div', big ? h('div.big' + (lose ? '.lose' : ''), big) : null, small ? h('div.small', small) : null));
    b.classList.remove('hidden');
    clearTimeout(banner._t); banner._t = setTimeout(() => b.classList.add('hidden'), 2700);
  }

  function flash(dtype) {
    const col = { fire: '255,120,40', cold: '140,210,255', lightning: '230,240,255', psychic: '255,140,220', radiant: '255,230,140' }[dtype] || '200,160,255';
    const d = h('div', { style: { position: 'fixed', inset: 0, zIndex: 35, pointerEvents: 'none', background: `radial-gradient(circle at 50% 40%, rgba(${col},.45), rgba(${col},0) 70%)` } });
    document.body.append(d);
    d.animate([{ opacity: 0 }, { opacity: 1 }, { opacity: 0 }], { duration: 900 }).onfinish = () => d.remove();
  }

  function speak(text) { EW.voice.narrate(text); }

  // ================================================================ character sheet
  function openSheet(id) {
    const hero = S.heroes.find(x => x.id === (id || '')) || S.heroes.find(x => x.player === EW.me.id) || S.heroes[0];
    if (!hero) return EW.toast('No heroes yet.');
    const mine = hero.player === EW.me.id || EW.me.host;
    const pc = h('canvas'); EW.portrait(pc, hero, 200);
    const tabs = h('div.tabs'), body = h('div');
    const views = {
      'Overview': () => [
        h('div.abil', ['str', 'dex', 'con', 'int', 'wis', 'cha'].map(a => h('div', h('small', a.toUpperCase()), h('b', hero.scores[a]), h('small', EW.sign(hero.mods[a]) + ' · save ' + EW.sign(hero.saves[a]))))),
        h('div.kv', [['HP', `${Math.max(0, hero.hp)}/${hero.maxHp}`], ['AC', hero.ac + (hero.mageArmor ? ' (Mage Armor)' : '')], ['Speed', hero.speed + ' ft'], ['Initiative', EW.sign(hero.initiative)], ['Proficiency', '+' + hero.prof], ['Passive Perception', hero.passivePerception], ['XP', `${hero.xp}${hero.nextXp ? ' / ' + hero.nextXp : ''}`], ['Gold', hero.gold + ' gp'],
          ...(hero.spellDC ? [['Spell save DC', hero.spellDC], ['Spell attack', EW.sign(hero.spellAttack)]] : []), ['Weapon', `${hero.weapon.name} ${EW.sign(hero.weaponAttack)} · ${hero.weaponDamage}`]].map(([k, v]) => h('div', h('small', k), String(v)))),
        h('h3', 'Skills'), h('div.skills', Object.values(hero.skills).map(s => h('div' + (s.prof ? '.prof' : ''), `${EW.sign(s.bonus)} ${s.label}${s.expert ? ' ★' : ''}`))),
      ],
      'Features': () => [...hero.features.map(f => h('div.feat', h('b', f.name + ': '), f.desc)), h('h3', 'Species traits'), ...hero.traits.map(t => h('div.feat', h('b', t.name + ': '), t.text))],
      'Spells': () => spellsView(hero, mine),
      'Inventory': () => inventoryView(hero, mine),
      'Story': () => [hero.motto ? h('p', h('i', '“' + hero.motto + '”')) : null, h('h3', 'Appearance'), h('p', hero.appearance || '—'), h('h3', 'Personality'), h('p', hero.personality || '—'), h('h3', 'Backstory'), h('p', hero.backstory || '—')],
    };
    let cur = 'Overview';
    const draw = () => { tabs.innerHTML = ''; for (const k of Object.keys(views)) { if (k === 'Spells' && !hero.spells.length) continue; tabs.append(h('button' + (k === cur ? '.on' : ''), { onclick: () => { cur = k; draw(); } }, k)); } body.innerHTML = ''; body.append(...views[cur]().filter(Boolean)); };
    draw();
    const switcher = h('div.row.wrap', { style: { marginTop: '10px' } }, S.heroes.filter(x => x.id !== hero.id).map(x => h('button.btn.small', { onclick: () => openSheet(x.id) }, x.name.split(' ')[0])));
    EW.modal(h('div.sheet', h('div', pc, h('h2', { style: { marginTop: '10px' } }, hero.name), h('div.hint', `Level ${hero.level} ${hero.speciesName}${hero.lineageName && hero.lineageName !== hero.speciesName ? ' (' + hero.lineageName + ')' : ''} ${hero.className}${hero.subclass ? ' — ' + hero.subclass : ''}`), h('div.hint', `${hero.backgroundName} · ${hero.pronouns}`),
      h('div.row.wrap', { style: { marginTop: '10px' } }, h('a.btn.small', { href: `/api/hero/export?id=${hero.id}&pid=${EW.me.id}`, download: '' }, 'Export hero file'),
        !hero.npc && hero.player !== EW.me.id && !(S.players.find(p => p.id === hero.player) || {}).online ? h('button.btn.small.primary', { onclick: async () => { const r = await EW.api('/api/hero/claim', { hero: hero.id }); if (r.ok) { EW.toast(`You now play ${hero.name}`, true); EW.closeModal(); } } }, 'Play this hero') : null), switcher),
      h('div', tabs, body)));
  }

  function spellsView(hero, mine) {
    const out = [];
    const slots = (hero.slotsMax || []).map((m, i) => `Level ${i + 1}: ${hero.slots[i]}/${m}`).join(' · ');
    if (slots) out.push(h('p.hint', 'Spell slots — ' + slots));
    for (const s of hero.spells) out.push(h('div.feat', h('b', `${s.name}${s.level ? ' (L' + s.level + ')' : ' (cantrip)'}: `), s.desc));
    const cls = hero.class;
    if (mine && ['wizard', 'cleric', 'bard', 'paladin', 'ranger'].includes(cls) && S.phase !== 'combat') {
      out.push(h('h3', 'Prepare spells'));
      out.push(h('p.hint', cls === 'wizard' ? 'Choose from your spellbook.' : 'Choose from your class list.'));
      const chosen = new Set(hero.prepared);
      const pick = h('div.spell-pick');
      EW.api('/api/rules', undefined, { quiet: true }).then(R => {
        const maxLv = (hero.slotsMax || []).length;
        const pool = R.spells.filter(s => s.level >= 1 && s.level <= maxLv && s.lists.includes(cls) && (cls !== 'wizard' || hero.spellbook.includes(s.id)) && s.id !== 'divine_smite');
        for (const s of pool) pick.append(h('label', h('input', { type: 'checkbox', checked: chosen.has(s.id), onchange: e => { e.target.checked ? chosen.add(s.id) : chosen.delete(s.id); } }), h('span', h('b', s.name + ` (L${s.level})`), h('small', s.desc))));
      });
      out.push(pick, h('button.btn.primary', { style: { marginTop: '8px' }, onclick: async () => { const r = await EW.api('/api/hero/prepare', { hero: hero.id, spells: [...chosen] }); if (r.ok) EW.toast('Spells prepared', true); } }, 'Prepare these spells'));
    }
    return out;
  }

  function inventoryView(hero, mine) {
    const out = [];
    const counts = {};
    for (const it of hero.inventory) counts[it.id] = counts[it.id] ? { ...counts[it.id], n: counts[it.id].n + 1 } : { it, n: 1 };
    for (const { it, n } of Object.values(counts)) {
      const row = h('div.inv-item', h('div', h('b', `${it.name}${n > 1 ? ' ×' + n : ''}`), h('div.hint', it.desc)));
      if (mine) {
        const acts = h('div.row');
        if (it.attunable) acts.append(h('button.btn.small' + (hero.attuned.includes(it.id) ? '.primary' : ''), { onclick: async () => { await EW.api('/api/hero/attune', { hero: hero.id, item: it.id }); setTimeout(() => openSheet(hero.id), 150); } }, hero.attuned.includes(it.id) ? 'Attuned ✓' : 'Attune'));
        const others = S.heroes.filter(x => x.id !== hero.id);
        if (others.length) { const sel = h('select', { onchange: async e => { if (e.target.value) { await EW.api('/api/hero/give', { from: hero.id, to: e.target.value, item: it.id }); setTimeout(() => openSheet(hero.id), 150); } } }, h('option', { value: '' }, 'Give to…'), others.map(o => h('option', { value: o.id }, o.name))); acts.append(sel); }
        row.append(acts);
      }
      out.push(row);
    }
    if (!out.length) out.push(h('p.hint', 'Empty pockets.'));
    return out;
  }

  // ================================================================ menu, dice tray, keys
  $('btnSheet').onclick = () => openSheet();
  $('btnForge').onclick = () => EW.forge.open();
  $('btnDice').onclick = () => $('diceTray').classList.toggle('hidden');
  $('modalClose').onclick = () => EW.closeModal();
  $('modal').addEventListener('click', e => { if (e.target.id === 'modal') EW.closeModal(); });
  const db = $('diceButtons');
  for (const d of ['d4', 'd6', 'd8', 'd10', 'd12', 'd20', 'd100', '2d20kh1']) db.append(h('button.btn', { onclick: () => EW.api('/api/dice', { expr: d.startsWith('d') ? '1' + d : d }) }, d === '2d20kh1' ? 'Adv.' : d));
  $('diceForm').onsubmit = e => { e.preventDefault(); EW.api('/api/dice', { expr: $('diceExpr').value }); };

  $('btnMenu').onclick = async () => {
    const info = await EW.api('/api/info');
    const saves = (await EW.api('/api/saves')).saves || [];
    const meP = S && S.players.find(p => p.id === EW.me.id);
    const box = h('div', h('h2', 'Table menu'),
      h('h3', 'Invite friends'), h('p.hint', 'Anyone on your network can join from a browser — Windows, Mac, phone or tablet:'),
      ...info.urls.slice(1).map(u => h('div.invite', u)), info.urls.length < 2 ? h('p.hint', 'No network address found — friends can join over the internet with a tunnel (see README).') : null,
      h('h3', 'Music'),
      h('div.menu-list',
        h('label.row', h('input', { type: 'checkbox', checked: EW.audio.on, onchange: e => EW.audio.toggle(e.target.checked) }), 'Music, sound effects & living soundscapes'),
        h('div.two', h('label.field', h('span', 'Music'), h('input', { type: 'range', min: '0', max: '0.6', step: '0.02', value: EW.audio.vol.music, oninput: e => EW.audio.setVolume('music', parseFloat(e.target.value)) })),
          h('label.field', h('span', 'Ambience (crowds, fire, rain…)'), h('input', { type: 'range', min: '0', max: '1.4', step: '0.05', value: EW.audio.vol.amb, oninput: e => EW.audio.setVolume('amb', parseFloat(e.target.value)) }))),
        h('label.row', h('input', { type: 'checkbox', checked: meP && meP.autoplay, onchange: e => EW.api('/api/autoplay', { on: e.target.checked }) }), 'Auto-play my hero in battle (for when you step away)')),
      h('h3', 'Voices'), EW.voice.panel(),
      h('h3', 'The tale'),
      h('div.row.wrap.gap', h('button.btn', { onclick: () => { showLobby = true; EW.closeModal(); applyState(S); } }, 'Open the lobby (heroes & tales)')));
    if (EW.me.host) {
      const ai = await EW.api('/api/ai', undefined, { quiet: true });
      const prov = h('select', ['rules', 'ollama', 'openai', 'anthropic'].map(p => h('option', { value: p, selected: ai.provider === p }, { rules: 'Built-in improv rules (no setup)', ollama: 'Local AI with Ollama', openai: 'OpenAI-compatible server (LM Studio, llama.cpp…)', anthropic: 'Claude (Anthropic API key)' }[p])));
      const url = h('input', { value: ai.url || '', placeholder: 'default: http://localhost:11434 for Ollama' });
      const model = h('input', { value: ai.model || '', placeholder: 'e.g. gemma3, llama3.2 (blank = first installed)' });
      const key = h('input', { type: 'password', value: ai.hasKey ? '••••••••' : '', placeholder: 'API key (only for Claude / hosted servers)' });
      const out = h('p.hint');
      box.append(h('h3', 'AI Dungeon Master (host)'), h('p.hint', 'Players can speak in character; the DM answers in the NPC\'s voice and may call for checks. It never changes the plot.'),
        h('div.menu-list', h('label.field', h('span', 'Who plays the DM'), prov), h('div.two', h('label.field', h('span', 'Server URL'), url), h('label.field', h('span', 'Model'), model)), h('label.field', h('span', 'API key'), key),
          h('div.row.gap', h('button.btn.small.primary', { onclick: async () => { const r = await EW.api('/api/ai/config', { provider: prov.value, url: url.value, model: model.value, apiKey: key.value }); if (r.ok) { out.textContent = 'Saved — ' + r.dm.label; } } }, 'Save'),
            h('button.btn.small', { onclick: async () => { out.textContent = 'Testing…'; const r = await EW.api('/api/ai/test', {}); out.textContent = r.message || r.error; } }, 'Test connection')), out));
      const name = h('input', { placeholder: 'save name', value: 'my-game' });
      box.append(h('h3', 'Saves (host)'), h('div.row.wrap.gap', name, h('button.btn', { onclick: async () => { const r = await EW.api('/api/save', { name: name.value }); if (r.ok) EW.toast('Saved', true); } }, 'Save game')),
        h('div.row.wrap.gap', { style: { marginTop: '8px' } }, saves.map(s => h('button.btn.small', { onclick: async () => { if (confirm(`Load “${s}”? Unsaved progress will be lost.`)) { const r = await EW.api('/api/load', { name: s }); if (r.ok) { showLobby = false; lastSceneKey = ''; EW.closeModal(); } } } }, 'Load ' + s))));
    }
    box.append(h('p.hint', { style: { marginTop: '16px' } }, `Emberwild v${info.version} · Keys: D dice tray · C character sheet · 1–9 pick a choice · Esc cancel targeting`));
    EW.modal(box);
  };

  window.addEventListener('keydown', e => {
    if (e.target.matches('input, textarea, select')) return;
    if (e.key === 'd' || e.key === 'D') $('btnDice').click();
    else if (e.key === 'c' || e.key === 'C') openSheet();
    else if (/^[1-9]$/.test(e.key) && S && S.phase === 'story') { const b = $('choices').querySelectorAll('.choice')[parseInt(e.key) - 1]; if (b && !b.disabled) b.click(); }
    else if (e.key === 'Escape' && !$('modal').classList.contains('hidden')) EW.closeModal();
  });

  // first click anywhere enables audio if the player had it on last time (browsers need a gesture)
  window.addEventListener('pointerdown', function once() { if (EW.audio.on) EW.audio.toggle(true); window.removeEventListener('pointerdown', once); });
})();
