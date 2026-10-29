/* Character builder following the 2024 Player's Handbook order:
   class → background (ability increases, skills, Origin feat) → species → ability scores → skills & spells → look & story. */
(function () {
  const h = EW.h;
  const STEPS = ['Class', 'Background', 'Species', 'Abilities', 'Skills & spells', 'Look & story', 'Review'];
  const ABIL = ['str', 'dex', 'con', 'int', 'wis', 'cha'];
  const ABN = { str: 'Strength', dex: 'Dexterity', con: 'Constitution', int: 'Intelligence', wis: 'Wisdom', cha: 'Charisma' };
  const COST = { 8: 0, 9: 1, 10: 2, 11: 3, 12: 4, 13: 5, 14: 7, 15: 9 };
  const STD = [15, 14, 13, 12, 10, 8];
  const EMBLEMS = ['🔥', '❄', '⚡', '🌿', '🗡', '🛡', '🏹', '🪓', '🎻', '☀', '🌙', '⭐', '💀', '🐉', '🌸', '🕯', '⚒', '🦉', '🐺', '🍄'];
  let R = null, st, step, body;

  EW.builder = { open };

  async function open(prefill) {
    if (!R) R = await EW.api('api/rules');
    st = Object.assign({
      name: '', pronouns: 'she/her', class: '', background: '', species: '', lineage: '',
      method: 'pointbuy', scores: { str: 8, dex: 8, con: 8, int: 8, wis: 8, cha: 8 }, std: {}, rolled: null,
      incMode: '21', inc: {}, skills: [], cantrips: [], spells: [],
      color: '#e8743b', hair: '#6b3e26', skin: '#d8a47f', eyes: '#3fa34d', emblem: '🔥',
      appearance: '', personality: '', backstory: '', motto: '',
    }, prefill || {});
    step = 0;
    body = h('div');
    EW.modal(h('div', h('h2', 'Create a hero'), h('div.steps#bsteps'), body));
    render();
  }

  const cls = () => R.classes.find(c => c.id === st.class);
  const bg = () => R.backgrounds.find(b => b.id === st.background);
  const sp = () => R.species.find(s => s.id === st.species);

  function final() {
    const base = st.method === 'standard' ? Object.fromEntries(ABIL.map(a => [a, st.std[a] || 8])) : st.method === 'roll' && st.rolled ? st.rolled : st.scores;
    const out = {};
    for (const a of ABIL) out[a] = Math.min(20, (base[a] || 8) + (st.inc[a] || 0));
    return { base, out };
  }
  const mod = v => Math.floor((v - 10) / 2);

  function render() {
    const s = document.getElementById('bsteps');
    s.innerHTML = '';
    STEPS.forEach((n, i) => s.append(h('span' + (i === step ? '.on' : i < step ? '.done' : ''), n)));
    body.innerHTML = '';
    [stepClass, stepBg, stepSpecies, stepAbil, stepSkills, stepLook, stepReview][step]();
    const nav = h('div.builder-nav',
      h('button.btn', { disabled: step === 0, onclick: () => { step--; render(); } }, 'Back'),
      h('div.err#berr'),
      step < STEPS.length - 1 ? h('button.btn.primary', { onclick: nextStep }, 'Next') : h('span'));
    body.append(nav);
  }

  function nextStep() {
    const err = validate();
    if (err) { document.getElementById('berr').textContent = err; return; }
    step++; render();
  }

  function validate() {
    if (step === 0 && !st.class) return 'Choose a class.';
    if (step === 1 && !st.background) return 'Choose a background.';
    if (step === 2 && !st.species) return 'Choose a species.';
    if (step === 3) {
      if (st.method === 'pointbuy') { const spent = ABIL.reduce((t, a) => t + COST[st.scores[a]], 0); if (spent > 27) return 'You have spent more than 27 points.'; }
      if (st.method === 'standard' && ABIL.some(a => !st.std[a])) return 'Assign every number of the standard array.';
      if (st.method === 'roll' && !st.rolled) return 'Roll your scores first.';
      const total = Object.values(st.inc).reduce((a, b) => a + b, 0);
      if (total !== 3) return 'Assign your background ability increases (+2/+1 or +1/+1/+1).';
    }
    if (step === 4) {
      const c = cls();
      if (st.skills.length < c.skillCount) return `Choose ${c.skillCount} class skills.`;
      if (c.spellList && st.cantrips.length < c.cantrips[0] && c.cantrips[0] > 0) return `Choose ${c.cantrips[0]} cantrips.`;
      if (c.spellList && st.spells.length < c.prepared[0]) return `Choose ${c.prepared[0]} level 1 spells.`;
    }
    if (step === 5 && !st.name.trim()) return 'Give your hero a name.';
    return '';
  }

  function optGrid(items, sel, onPick, sub) {
    const g = h('div.optgrid');
    for (const it of items) g.append(h('button.opt' + (it.id === sel ? '.sel' : ''), { onclick: () => onPick(it) }, h('b', (it.glyph ? it.glyph + ' ' : '') + it.name), h('small', sub(it))));
    return g;
  }

  function stepClass() {
    body.append(h('p.hint', 'Your class is the biggest choice: it decides how you fight, cast and survive.'));
    body.append(optGrid(R.classes, st.class, c => { st.class = c.id; st.skills = []; st.cantrips = c.defaultCantrips.slice(0, c.cantrips[0]); st.spells = c.defaultSpells.filter(id => (R.spells.find(s => s.id === id) || {}).level === 1).slice(0, c.prepared[0]); st.color = c.color; st.emblem = c.glyph; render(); }, c => c.tagline));
    const c = cls();
    if (c) body.append(h('div.detail-box',
      h('b', `${c.name} — d${c.hitDie} hit die · primary ${ABN[c.primary]} · saves ${c.saves.map(a => ABN[a]).join(' & ')}`),
      h('div', c.features.filter(f => f.level === 1).map(f => h('div.feat', h('b', f.name + ': '), f.desc))),
      h('div.hint', 'At levels 2–3 you gain: ' + c.features.filter(f => f.level > 1).map(f => f.name).join(', ') + (c.subclass ? ` (subclass: ${c.subclass})` : ''))));
  }

  function stepBg() {
    body.append(h('p.hint', 'In the 2024 rules your background sets your ability increases, two skills and an Origin feat.'));
    body.append(optGrid(R.backgrounds, st.background, b => { st.background = b.id; st.inc = {}; render(); }, b => `${b.abilities.map(a => a.toUpperCase()).join('/')} · ${b.featName}`));
    const b = bg();
    if (b) body.append(h('div.detail-box', h('div', b.desc), h('div', h('b', 'Skills: '), b.skills.map(sk => R.skills.find(x => x.id === sk).label).join(', ')), h('div.feat', h('b', b.featName + ': '), b.featDesc), h('div', h('b', 'Starting gold: '), b.gold + 10 + ' gp')));
  }

  function stepSpecies() {
    body.append(optGrid(R.species, st.species, s => { st.species = s.id; st.lineage = s.lineages[0] ? s.lineages[0].id : ''; render(); }, s => `Speed ${s.speed} ft${s.darkvision ? ' · darkvision ' + s.darkvision + ' ft' : ''}`));
    const s = sp();
    if (s) {
      const box = h('div.detail-box', h('div', s.desc), s.traits.map(t => h('div.feat', h('b', t.name + ': '), t.text)));
      if (s.lineages.length > 1) {
        box.append(h('h3', 'Lineage'));
        box.append(optGrid(s.lineages, st.lineage, l => { st.lineage = l.id; render(); }, l => l.desc));
      }
      body.append(box);
    }
  }

  function stepAbil() {
    const tabs = h('div.tabs');
    for (const [m, l] of [['pointbuy', 'Point buy (27)'], ['standard', 'Standard array'], ['roll', 'Roll 4d6']]) tabs.append(h('button' + (st.method === m ? '.on' : ''), { onclick: () => { st.method = m; render(); } }, l));
    body.append(tabs);
    const b = bg(), c = cls();
    const { base, out } = final();
    const grid = h('div.scores');
    for (const a of ABIL) {
      const box = h('div.score', h('div', ABN[a] + (c.primary === a ? ' ★' : '')));
      if (st.method === 'pointbuy') {
        box.append(h('div.row', { style: { justifyContent: 'center' } },
          h('button.btn.small', { onclick: () => { if (st.scores[a] > 8) { st.scores[a]--; render(); } } }, '−'),
          h('span.big', String(st.scores[a])),
          h('button.btn.small', { onclick: () => { if (st.scores[a] < 15) { st.scores[a]++; render(); } } }, '+')));
      } else if (st.method === 'standard') {
        const sel = h('select', { onchange: e => { const v = parseInt(e.target.value) || 0; for (const k of ABIL) if (st.std[k] === v) delete st.std[k]; if (v) st.std[a] = v; else delete st.std[a]; render(); } }, h('option', { value: '' }, '—'));
        for (const v of STD) sel.append(h('option', { value: v, selected: st.std[a] === v }, String(v)));
        box.append(sel);
      } else box.append(h('div.big', st.rolled ? String(st.rolled[a]) : '–'));
      box.append(h('div.inc', st.inc[a] ? '+' + st.inc[a] + ' background' : ''));
      box.append(h('div', h('b', String(out[a])), ' ', h('span.mod', EW.sign(mod(out[a])))));
      grid.append(box);
    }
    if (st.method === 'pointbuy') body.append(h('p.hint', `Points spent: ${ABIL.reduce((t, a) => t + COST[st.scores[a]], 0)} / 27`));
    if (st.method === 'roll') body.append(h('p', h('button.btn', { onclick: () => { const r = {}; for (const a of ABIL) { const d = [0, 0, 0, 0].map(() => 1 + crypto.getRandomValues(new Uint32Array(1))[0] % 6).sort((x, y) => y - x); r[a] = d[0] + d[1] + d[2]; } st.rolled = r; render(); } }, '🎲 Roll 4d6 (drop lowest) six times')));
    body.append(grid);
    // increases
    const inc = h('div.detail-box', h('b', `${b.name} increases: `));
    const modes = h('span.row', { style: { display: 'inline-flex' } });
    for (const [m, l] of [['21', '+2 and +1'], ['111', '+1 to all three']]) modes.append(h('button.btn.small' + (st.incMode === m ? '.primary' : ''), { onclick: () => { st.incMode = m; st.inc = {}; if (m === '111') for (const x of b.abilities) st.inc[x] = 1; render(); } }, l));
    inc.append(modes);
    if (st.incMode === '21') {
      const row = h('div.row.wrap', { style: { marginTop: '8px' } });
      for (const [val, lab] of [[2, '+2 to'], [1, '+1 to']]) {
        const sel = h('select', { onchange: e => { for (const k in st.inc) if (st.inc[k] === val) delete st.inc[k]; if (e.target.value) { delete st.inc[e.target.value]; st.inc[e.target.value] = val; } render(); } }, h('option', { value: '' }, '—'));
        for (const x of b.abilities) sel.append(h('option', { value: x, selected: st.inc[x] === val }, ABN[x]));
        row.append(h('label', lab + ' '), sel);
      }
      inc.append(row);
    }
    body.append(inc);
  }

  function stepSkills() {
    const c = cls(), b = bg(), s = sp();
    const granted = new Set([...b.skills, ...(s.skills || [])]);
    const free = b.feat === 'skilled' ? 3 : 0;
    body.append(h('p.hint', `Already trained: ${[...granted].map(k => R.skills.find(x => x.id === k).label).join(', ')}. Choose ${c.skillCount} from your class list${free ? ` plus ${free} more of anything (Skilled feat)` : ''}.`));
    const g = h('div.spell-pick');
    const allowed = new Set(c.skillChoices);
    for (const sk of R.skills) {
      if (granted.has(sk.id)) continue;
      const classPicks = st.skills.filter(x => allowed.has(x)).length;
      const inClass = allowed.has(sk.id);
      const checked = st.skills.includes(sk.id);
      const total = c.skillCount + free;
      const can = checked || (st.skills.length < total && (inClass ? true : (st.skills.length - Math.min(classPicks, c.skillCount)) < free));
      g.append(h('label', { style: { opacity: can ? 1 : .45 } }, h('input', { type: 'checkbox', checked, disabled: !can, onchange: e => { if (e.target.checked) st.skills.push(sk.id); else st.skills = st.skills.filter(x => x !== sk.id); render(); } }), h('span', sk.label, h('small', ABN[sk.ability] + (inClass ? '' : ' · feat pick')))));
    }
    body.append(g);
    if (c.spellList) {
      const lineageCantrips = (s.lineages.find(l => l.id === st.lineage) || { cantrips: [] }).cantrips;
      if (c.cantrips[0] > 0) {
        body.append(h('h3', `Cantrips (${st.cantrips.length}/${c.cantrips[0]})`));
        body.append(spellGrid(R.spells.filter(x => x.level === 0 && x.lists.includes(c.spellList) && !lineageCantrips.includes(x.id)), st.cantrips, c.cantrips[0]));
      }
      body.append(h('h3', `Level 1 spells (${st.spells.length}/${c.prepared[0]})`));
      body.append(spellGrid(R.spells.filter(x => x.level === 1 && x.lists.includes(c.spellList) && x.id !== 'divine_smite' && x.id !== 'hunters_mark'), st.spells, c.prepared[0]));
      if (lineageCantrips.length) body.append(h('p.hint', 'Your lineage also grants: ' + lineageCantrips.join(', ')));
    }
  }

  function spellGrid(list, arr, max) {
    const g = h('div.spell-pick');
    for (const s of list) {
      const checked = arr.includes(s.id);
      g.append(h('label', h('input', { type: 'checkbox', checked, disabled: !checked && arr.length >= max, onchange: e => { if (e.target.checked) arr.push(s.id); else arr.splice(arr.indexOf(s.id), 1); render(); } }),
        h('span', h('b', s.name), s.elemental ? ' ✦' : '', h('small', s.desc))));
    }
    return g;
  }

  function stepLook() {
    const pc = h('canvas');
    const preview = () => EW.portrait(pc, { ...st, class: st.class }, 140);
    const field = (lab, el) => h('label.field', h('span', lab), el);
    const inp = (k, ph, max = 60) => h('input', { value: st[k], placeholder: ph, maxlength: max, oninput: e => { st[k] = e.target.value; } });
    const ta = (k, ph, rows = 3) => h('textarea', { rows, placeholder: ph, oninput: e => { st[k] = e.target.value; } }, st[k]);
    const color = k => h('input', { type: 'color', value: st[k], oninput: e => { st[k] = e.target.value; preview(); } });
    const pron = h('select', { onchange: e => { st.pronouns = e.target.value; preview(); } }, ['she/her', 'he/him', 'they/them'].map(p => h('option', { selected: st.pronouns === p }, p)));
    const emb = h('div.row.wrap');
    for (const e of EMBLEMS) emb.append(h('button.btn.small' + (st.emblem === e ? '.primary' : ''), { onclick: () => { st.emblem = e; render(); } }, e));
    body.append(h('div.sheet', h('div', pc, h('div.colorrow', h('label', 'Skin', color('skin')), h('label', 'Hair', color('hair')), h('label', 'Eyes', color('eyes')), h('label', 'Colour', color('color')))),
      h('div', h('div.two', field('Name', inp('name', 'Sylwen Ashbloom', 40)), field('Pronouns', pron)),
        field('Emblem', emb), field('Motto', inp('motto', 'A line they live by', 140)),
        field('Appearance', ta('appearance', 'What do people notice first?')),
        field('Personality', ta('personality', 'How do they act when things go wrong?')),
        field('Backstory', ta('backstory', 'Where do they come from, and why are they on the road?', 4)))));
    preview();
  }

  function spec() {
    const { base } = final();
    return {
      name: st.name.trim(), pronouns: st.pronouns, class: st.class, background: st.background, species: st.species, lineage: st.lineage,
      method: st.method === 'roll' ? 'rolled' : st.method, scores: base, increases: st.inc, skills: st.skills, cantrips: st.cantrips, spells: st.spells,
      color: st.color, hair: st.hair, skin: st.skin, eyes: st.eyes, emblem: st.emblem, appearance: st.appearance, personality: st.personality, backstory: st.backstory, motto: st.motto,
    };
  }

  function javaSnippet(s) {
    const q = v => '"' + String(v).replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '\\n') + '"';
    const list = a => 'List.of(' + a.map(q).join(', ') + ')';
    return `// Paste into your CampaignScript to ship this hero as a pre-made character:
c.pregen(Hero.fromSpec(Json.obj(
    "name", ${q(s.name)}, "pronouns", ${q(s.pronouns)},
    "class", ${q(s.class)}, "species", ${q(s.species)}, "lineage", ${q(s.lineage)}, "background", ${q(s.background)},
    "method", ${q(s.method)}, "scores", Json.obj(${ABIL.map(a => `"${a}", ${s.scores[a]}`).join(', ')}),
    "increases", Json.obj(${Object.entries(s.increases).map(([k, v]) => `"${k}", ${v}`).join(', ')}),
    "skills", ${list(s.skills)}, "cantrips", ${list(s.cantrips)}, "spells", ${list(s.spells)},
    "color", ${q(s.color)}, "emblem", ${q(s.emblem)}, "hair", ${q(s.hair)}, "skin", ${q(s.skin)}, "eyes", ${q(s.eyes)},
    "motto", ${q(s.motto)}, "appearance", ${q(s.appearance)}, "personality", ${q(s.personality)}, "backstory", ${q(s.backstory)}
), new ArrayList<>()));`;
  }

  function stepReview() {
    const c = cls(), b = bg(), s = sp();
    const { out } = final();
    const pc = h('canvas');
    EW.portrait(pc, { ...st }, 160);
    const con = mod(out.con);
    const hp = c.hitDie + con + (s.id === 'dwarf' ? 1 : 0) + (b.feat === 'tough' ? 2 : 0);
    body.append(h('div.sheet', h('div', pc),
      h('div', h('h2', st.name || 'Nameless'), h('div.hint', `${s.name}${st.lineage && s.lineages.length > 1 ? ' (' + s.lineages.find(l => l.id === st.lineage).name + ')' : ''} ${c.name} · ${b.name} · ${st.pronouns}`),
        h('div.abil', ABIL.map(a => h('div', h('small', a.toUpperCase()), h('b', String(out[a])), h('small', EW.sign(mod(out[a])))))),
        h('div.kv', h('div', h('small', 'Hit points'), String(Math.max(1, hp))), h('div', h('small', 'Speed'), (st.lineage === 'wood' ? 35 : s.speed) + ' ft'), h('div', h('small', 'Origin feat'), b.featName)),
        st.motto ? h('p', h('i', '“' + st.motto + '”')) : null)));
    const sp2 = spec();
    body.append(h('div.row.wrap.gap', { style: { marginTop: '14px' } },
      h('button.btn.primary', { onclick: create }, 'Create hero and join the party'),
      h('button.btn', { onclick: () => download(sp2) }, 'Save hero file'),
      h('button.btn.ghost', { onclick: () => { const pre = h('pre.code', javaSnippet(sp2)); body.append(pre); navigator.clipboard && navigator.clipboard.writeText(javaSnippet(sp2)).then(() => EW.toast('Java copied to clipboard', true)); } }, 'Copy as Java')));
  }

  function download(s) {
    const blob = new Blob([JSON.stringify({ format: 'emberwild-spec-1', spec: s }, null, 2)], { type: 'application/json' });
    const a = h('a', { href: URL.createObjectURL(blob), download: (s.name || 'hero').replace(/\W+/g, '_') + '.spec.json' });
    document.body.append(a); a.click(); a.remove();
  }

  async function create() {
    const r = await EW.api('api/hero/create', { spec: spec() });
    if (r.ok) { EW.toast(`${st.name} joins the party!`, true); EW.closeModal(); if (r.warnings && r.warnings.length) EW.toast('Adjusted: ' + r.warnings.join('; ')); }
    else if (r.errors) document.getElementById('berr').textContent = r.errors.join(' ');
  }
})();
