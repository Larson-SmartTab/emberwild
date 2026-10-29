/* Campaign Forge: browse tales, build one visually, or upload JSON / Java. */
(function () {
  const h = EW.h;
  const BACKDROPS = ['tavern', 'town', 'road', 'forest', 'bridge', 'swamp', 'grove', 'camp', 'village', 'chapel', 'ruins', 'cave', 'dungeon', 'hall', 'lair'];
  const MUSIC = ['tavern', 'ballad', 'explore', 'camp', 'grove', 'eerie', 'mystery', 'sorrow', 'tension', 'goblin', 'ruins', 'battle', 'boss', 'hope', 'triumph'];
  const WEATHER = ['none', 'rain', 'emberrain', 'embers', 'fog', 'ash'];
  const TIMES = ['day', 'dawn', 'dusk', 'night'];
  const ANCHORS = ['tavern', 'town', 'road', 'camp', 'ruins', 'epilogue'];
  let tab = 'library', draft, selScene, monsters = [];

  EW.forge = { open };

  function blank() {
    return {
      id: 'my_tale', title: 'My Tale', author: EW.me.name || 'Anonymous', synopsis: '', start: 'start', cover: 'forest',
      hooks: [{ anchor: 'tavern', label: 'Hear a strange rumour', entry: 'start' }],
      scenes: { start: { title: 'A New Thread', chapter: 'Side Tale', backdrop: 'forest', time: 'day', weather: 'none', music: 'explore', text: 'Write what the party sees, hears and smells.', choices: [{ label: 'Head back', icon: '↩', go: '@return' }] } },
      encounters: {}, monsters: [],
    };
  }

  function load() { try { draft = JSON.parse(localStorage.getItem('ew.forge') || 'null') || blank(); } catch { draft = blank(); } selScene = Object.keys(draft.scenes)[0]; }
  function save() { try { localStorage.setItem('ew.forge', JSON.stringify(draft)); } catch {} }

  async function open(t) {
    tab = t || tab;
    if (!draft) load();
    const box = h('div');
    EW.modal(box);
    const r = await EW.api('api/rules', undefined, { quiet: true });
    monsters = (r.monsters || []).map(m => m.id);
    render(box);
  }

  function render(box) {
    box.innerHTML = '';
    box.append(h('h2', 'Campaign Forge'), h('p.hint', 'Every tale here can be played on its own or woven into the others through hooks.'));
    const tabs = h('div.tabs');
    for (const [k, l] of [['library', 'Tales in this realm'], ['build', 'Build a tale'], ['upload', 'Upload a file']]) tabs.append(h('button' + (tab === k ? '.on' : ''), { onclick: () => { tab = k; render(box); } }, l));
    box.append(tabs);
    const area = h('div');
    box.append(area);
    ({ library, build, upload })[tab](area, box);
  }

  // ---------------------------------------------------------------- library
  async function library(area, box) {
    const r = await EW.api('api/campaigns');
    for (const c of r.campaigns) {
      area.append(h('div.choice-edit',
        h('div.row', { style: { justifyContent: 'space-between' } }, h('b', c.title), h('span.hint', `by ${c.author} · ${c.source}`)),
        h('div.hint', `${c.scenes} scenes · ${c.battles} battles · ${c.checks} skill checks${c.pregens ? ' · ' + c.pregens + ' heroes' : ''}`),
        c.hooks.length ? h('div.hookline', 'Appears at: ' + c.hooks.map(x => `${x.anchor} (“${x.label}”)`).join(', ')) : null,
        h('div.row', h('a.btn.small', { href: `api/campaign?id=${c.id}&download=1&pid=${EW.me.id}`, download: c.id + '.json' }, 'Download JSON'),
          !c.core ? h('button.btn.small', { onclick: async () => { const full = await (await fetch(`api/campaign?id=${c.id}`)).json(); draft = full; draft.scenes = full.scenes; selScene = Object.keys(draft.scenes)[0]; save(); tab = 'build'; render(box); } }, 'Open in builder') : null)));
    }
    const errs = Object.entries(r.errors || {});
    if (errs.length) { area.append(h('h3', 'Files that failed to load')); for (const [f, p] of errs) area.append(h('div.problems', h('b', f), p.map(x => h('div.' + x.split(' ')[0], x)))); }
    if (EW.me.host) {
      area.append(h('h3', 'Java tales awaiting your approval'));
      if (!r.pending.length) area.append(h('p.hint', 'Nothing waiting. (Java runs as code on this machine, so it needs your OK first.)'));
      for (const f of r.pending) {
        const out = h('div');
        area.append(h('div.choice-edit', h('b', f), h('div.row',
          h('button.btn.small', { onclick: async () => { const s = await EW.api(`api/campaign/pending?file=${encodeURIComponent(f)}`); out.innerHTML = ''; out.append(h('pre.code', s.source)); } }, 'Read the code'),
          h('button.btn.small.primary', { onclick: async () => { const a = await EW.api('api/campaign/approve', { file: f }); out.innerHTML = ''; out.append(problems(a.problems || [])); if (a.ok) { EW.toast(`“${a.title}” approved`, true); } } }, 'Approve & load'),
          h('button.btn.small.danger', { onclick: async () => { await EW.api('api/campaign/reject', { file: f }); render(box); } }, 'Reject')), out));
      }
      area.append(h('button.btn', { onclick: async () => { await EW.api('api/campaign/reload', {}); EW.toast('Campaign folder reloaded', true); render(box); } }, 'Reload the campaigns folder'));
    }
  }

  function problems(list) {
    const d = h('div.problems');
    for (const p of list) d.append(h('div.' + p.split(' ')[0], p));
    return d;
  }

  // ---------------------------------------------------------------- builder
  function field(label, el) { return h('label.field', h('span', label), el); }
  function input(obj, key, ph, cb) { return h('input', { value: obj[key] ?? '', placeholder: ph || '', oninput: e => { obj[key] = e.target.value; save(); cb && cb(); } }); }
  function select(obj, key, opts, cb) { return h('select', { onchange: e => { obj[key] = e.target.value; save(); cb && cb(); } }, opts.map(o => h('option', { selected: obj[key] === o }, o))); }
  function area(obj, key, rows, ph) { return h('textarea', { rows: rows || 4, placeholder: ph || '', oninput: e => { obj[key] = e.target.value; save(); } }, obj[key] ?? ''); }

  function build(el, box) {
    const meta = h('div',
      h('div.two', field('Title', input(draft, 'title')), field('Author', input(draft, 'author'))),
      h('div.two', field('Id (letters, numbers, _ )', input(draft, 'id')), field('Cover', select(draft, 'cover', BACKDROPS))),
      field('Synopsis', area(draft, 'synopsis', 2)));
    const hook = draft.hooks[0] || (draft.hooks[0] = { anchor: 'tavern', label: '', entry: draft.start });
    meta.append(h('div.two', field('Appears in other tales at', select(hook, 'anchor', ANCHORS)), field('…as the choice', input(hook, 'label', 'Follow the strange lights'))));
    el.append(meta);
    const list = h('div.scene-list');
    const editor = h('div');
    const refresh = () => {
      list.innerHTML = '';
      for (const id of Object.keys(draft.scenes)) list.append(h('button.btn.small' + (id === selScene ? '.primary' : ''), { onclick: () => { selScene = id; refresh(); } }, (id === draft.start ? '▶ ' : '') + id));
      list.append(h('button.btn.small', { onclick: () => { const id = prompt('New scene id (e.g. cave_entrance)'); if (id && !draft.scenes[id]) { draft.scenes[id] = { title: '', backdrop: 'forest', time: 'day', weather: 'none', music: 'explore', text: '', choices: [] }; selScene = id; save(); refresh(); } } }, '+ Scene'));
      list.append(h('button.btn.small', { onclick: () => { const id = prompt('Encounter id (e.g. cave_fight)'); if (id) { draft.encounters[id] = draft.encounters[id] || { title: 'A Fight', backdrop: 'forest', map: ['##########', '#..g..g..#', '#........#', '#..PP.PP.#', '##########'], legend: { g: 'goblin' } }; save(); sceneEditor(editor, null, id); } } }, '+ Battle map'));
      for (const id of Object.keys(draft.encounters || {})) list.append(h('button.btn.small.ghost', { onclick: () => sceneEditor(editor, null, id) }, '⚔ ' + id));
      sceneEditor(editor, selScene);
    };
    el.append(h('div.forge-grid', h('div', list), editor));
    const out = h('div');
    el.append(h('div.row.wrap.gap', { style: { marginTop: '12px' } },
      h('button.btn', { onclick: async () => { draft.hooks[0].entry = draft.hooks[0].entry || draft.start; const r = await EW.api('api/campaign/validate', draft); out.innerHTML = ''; out.append(problems(r.problems || [])); } }, 'Check for problems'),
      h('button.btn.primary', { onclick: async () => { const r = await EW.api('api/campaign/submit', draft); out.innerHTML = ''; out.append(problems(r.problems || [])); if (r.saved) EW.toast(`“${r.title}” is now part of the realm`, true); } }, 'Publish to this realm'),
      h('button.btn.ghost', { onclick: () => { const blob = new Blob([JSON.stringify(draft, null, 2)], { type: 'application/json' }); const a = h('a', { href: URL.createObjectURL(blob), download: draft.id + '.json' }); document.body.append(a); a.click(); a.remove(); } }, 'Download JSON'),
      h('button.btn.ghost.danger', { onclick: () => { if (confirm('Start a blank tale? Your current draft will be lost.')) { draft = blank(); selScene = 'start'; save(); render(box); } } }, 'New blank tale')), out);
    refresh();
  }

  function sceneEditor(el, id, encId) {
    el.innerHTML = '';
    if (encId) {
      const e = draft.encounters[encId];
      const legendText = Object.entries(e.legend || {}).map(([k, v]) => `${k}=${v}`).join('\n');
      el.append(h('h3', 'Battle: ' + encId),
        h('div.two', field('Title', input(e, 'title')), field('Backdrop', select(e, 'backdrop', BACKDROPS))),
        field('Intro', area(e, 'intro', 2)),
        field('Map — # wall . floor , brush ~ water T tree B barrel L lava ^ trap P party E extra; letters are monsters', h('textarea', { rows: 10, style: { fontFamily: 'monospace' }, oninput: ev => { e.map = ev.target.value.split('\n').filter(x => x.length); save(); } }, (e.map || []).join('\n'))),
        field('Legend (one per line: letter=monster)', h('textarea', { rows: 4, style: { fontFamily: 'monospace' }, oninput: ev => { e.legend = {}; for (const line of ev.target.value.split('\n')) { const m = line.match(/^\s*(\w)\s*=\s*([\w_]+)/); if (m) e.legend[m[1]] = m[2]; } save(); } }, legendText)),
        h('p.hint', 'Monsters you can use: ' + monsters.join(', ')),
        h('button.btn.small.danger', { onclick: () => { delete draft.encounters[encId]; save(); el.innerHTML = ''; } }, 'Delete battle'));
      return;
    }
    const s = draft.scenes[id];
    if (!s) return;
    const thumb = h('canvas', { style: { width: '160px', height: '100px', borderRadius: '6px' } });
    const paint = () => EW.scene.paintCover(thumb, s.backdrop);
    el.append(h('div.row', { style: { alignItems: 'flex-start', gap: '12px' } }, thumb, h('div', { style: { flex: 1 } },
      h('div.two', field('Title', input(s, 'title')), field('Chapter', input(s, 'chapter'))),
      h('div.two', field('Backdrop', select(s, 'backdrop', BACKDROPS, paint)), field('Music', select(s, 'music', MUSIC))),
      h('div.two', field('Time', select(s, 'time', TIMES)), field('Weather', select(s, 'weather', WEATHER))))));
    paint();
    el.append(field('Narration (blank line = new paragraph, *italic*, **bold**)', area(s, 'text', 6)));
    s.npc = s.npc || null;
    const npcBox = h('div.two');
    if (s.npc) npcBox.append(field('NPC name', input(s.npc, 'name')), field('NPC title', input(s.npc, 'title')), field('Glyph (emoji)', input(s.npc, 'glyph')), field('Colour', input(s.npc, 'color', '#8e44ad')), field('What they say', input(s.npc, 'line')));
    el.append(h('div.row', h('button.btn.small', { onclick: () => { s.npc = s.npc ? null : { name: '', title: '', glyph: '🧙', color: '#8e44ad', line: '' }; save(); sceneEditor(el, id); } }, s.npc ? 'Remove speaking NPC' : 'Add a speaking NPC'),
      h('button.btn.small', { onclick: () => { draft.start = id; draft.hooks[0].entry = id; save(); EW.toast('Start scene set', true); } }, 'Make this the start'),
      h('button.btn.small.danger', { onclick: () => { if (confirm('Delete scene ' + id + '?')) { delete draft.scenes[id]; selScene = Object.keys(draft.scenes)[0]; save(); EW.forge.open('build'); } } }, 'Delete scene')), npcBox);
    el.append(h('h3', 'Choices'));
    const scenes = () => ['', '@return', '@end', ...Object.keys(draft.scenes)];
    (s.choices = s.choices || []).forEach((c, i) => {
      const type = c.battle ? 'battle' : c.check ? 'check' : c.spell ? 'spell' : 'go';
      const box = h('div.choice-edit');
      box.append(h('div.two', field('Label', input(c, 'label')), field('Type', h('select', { onchange: e => { const t = e.target.value; delete c.check; delete c.battle; delete c.spell; if (t === 'check') { c.check = 'persuasion'; c.dc = 12; c.pass = { text: '', go: '' }; c.fail = { text: '', go: '' }; } if (t === 'battle') { c.battle = Object.keys(draft.encounters)[0] || ''; c.victory = { text: '', go: '' }; c.defeat = { text: '', go: '' }; } if (t === 'spell') { c.spell = 'charm_person'; c.pass = { text: '', go: '' }; } save(); sceneEditor(el, id); } }, ['go', 'check', 'spell', 'battle'].map(t => h('option', { selected: t === type }, t))))));
      if (type === 'go') box.append(field('Goes to', select(c, 'go', scenes())));
      if (type === 'check') box.append(h('div.two', field('Skill / ability / save:dex / luck', input(c, 'check')), field('DC', h('input', { type: 'number', value: c.dc, oninput: e => { c.dc = parseInt(e.target.value) || 10; save(); } }))),
        field('Who rolls', select(c, 'mode', ['best', 'group', 'each', 'chooser'])));
      if (type === 'spell') box.append(field('Spell id (e.g. charm_person, fire_bolt, ray_of_frost)', input(c, 'spell')));
      if (type === 'battle') box.append(field('Encounter', select(c, 'battle', Object.keys(draft.encounters))));
      for (const [k, lab] of type === 'check' ? [['pass', 'On success'], ['fail', 'On failure']] : type === 'spell' ? [['pass', 'After the spell']] : type === 'battle' ? [['victory', 'Victory'], ['defeat', 'Defeat']] : []) {
        c[k] = c[k] || { text: '', go: '' };
        box.append(h('div.two', field(lab + ' — text', input(c[k], 'text')), field(lab + ' — goes to', select(c[k], 'go', scenes()))));
      }
      box.append(h('button.btn.small.danger', { onclick: () => { s.choices.splice(i, 1); save(); sceneEditor(el, id); } }, 'Remove choice'));
      el.append(box);
    });
    el.append(h('button.btn.small', { onclick: () => { s.choices.push({ label: 'New choice', go: '' }); save(); sceneEditor(el, id); } }, '+ Choice'));
  }

  // ---------------------------------------------------------------- upload
  function upload(el) {
    const out = h('div');
    el.append(h('p', 'Upload a campaign file. JSON tales load straight away; Java tales are compiled after the host approves them.'),
      h('label.btn.primary.file', 'Choose a .json or .java file', h('input', { type: 'file', accept: '.json,.java', onchange: async e => {
        const f = e.target.files[0]; if (!f) return;
        const text = await f.text();
        out.innerHTML = '';
        let r;
        if (f.name.endsWith('.java')) r = await EW.api('api/campaign/submit-java?name=' + encodeURIComponent(f.name), text, { raw: true });
        else { let obj; try { obj = JSON.parse(text); } catch (err) { out.append(h('div.err', 'That file is not valid JSON: ' + err.message)); return; } r = await EW.api('api/campaign/submit', obj); }
        out.append(problems(r.problems || []));
        if (r.saved) EW.toast(`“${r.title}” is now part of the realm`, true);
      } })), out,
      h('h3', 'Start from an example'),
      h('div.row.wrap.gap', h('a.btn', { href: 'api/template/json', download: 'the_lost_lantern.json' }, 'Example JSON tale'), h('a.btn', { href: 'api/template/java', download: 'TheSunkenBell.java' }, 'Example Java tale')),
      h('p.hint', 'Java tales implement emberwild.story.CampaignScript and use the same fluent DSL as the core campaign. Validate locally with: java -jar emberwild.jar --validate MyTale.java'));
  }
})();
