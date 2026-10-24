/* Full-body miniatures for the battle map. Every figure stands on a painted base like a tabletop mini.
   Heroes are built from their class, species, colours and gear; monsters from their creature type and name.
   Poses: idle breathing, walking bounce, attack swing/lunge, spell casting, hit flinch, knocked down, death. */
(function () {
  const F = EW.figures = {};
  const sh = (c, a) => EW.shade(c || '#888888', a);

  // ------------------------------------------------------------------ helpers
  function limb(c, x1, y1, x2, y2, w, col) { c.strokeStyle = col; c.lineWidth = w; c.lineCap = 'round'; c.beginPath(); c.moveTo(x1, y1); c.lineTo(x2, y2); c.stroke(); }
  function ell(c, x, y, rx, ry, col, rot = 0) { c.fillStyle = col; c.beginPath(); c.ellipse(x, y, Math.abs(rx), Math.abs(ry), rot, 0, Math.PI * 2); c.fill(); }
  function poly(c, pts, col) { c.fillStyle = col; c.beginPath(); pts.forEach((p, i) => i ? c.lineTo(p[0], p[1]) : c.moveTo(p[0], p[1])); c.closePath(); c.fill(); }

  function weapon(c, kind, hx, hy, ang, s, col) {
    c.save(); c.translate(hx, hy); c.rotate(ang);
    const steel = '#d8dde3', wood = '#6b4a2c', dark = '#2a2a30';
    switch (kind) {
      case 'sword': limb(c, 0, 0, 0, -s * .55, s * .045, steel); limb(c, -s * .07, -s * .02, s * .07, -s * .02, s * .03, '#b8942e'); limb(c, 0, s * .02, 0, s * .1, s * .035, wood); break;
      case 'greatsword': limb(c, 0, 0, 0, -s * .75, s * .06, steel); limb(c, -s * .1, -s * .02, s * .1, -s * .02, s * .035, '#b8942e'); break;
      case 'dagger': limb(c, 0, 0, 0, -s * .25, s * .04, steel); limb(c, -s * .05, 0, s * .05, 0, s * .025, '#b8942e'); break;
      case 'axe': limb(c, 0, s * .1, 0, -s * .6, s * .04, wood); poly(c, [[0, -s * .58], [s * .2, -s * .66], [s * .22, -s * .42], [0, -s * .46]], steel); poly(c, [[0, -s * .58], [-s * .14, -s * .62], [-s * .14, -s * .46], [0, -s * .48]], steel); break;
      case 'mace': case 'hammer': limb(c, 0, s * .1, 0, -s * .45, s * .04, wood); ell(c, 0, -s * .5, s * .1, s * .1, kind === 'mace' ? '#a7adb5' : '#8a8f96'); break;
      case 'club': limb(c, 0, s * .1, 0, -s * .55, s * .09, '#5a3a20'); break;
      case 'spear': limb(c, 0, s * .3, 0, -s * .8, s * .035, wood); poly(c, [[0, -s * .95], [s * .05, -s * .8], [-s * .05, -s * .8]], steel); break;
      case 'staff': limb(c, 0, s * .45, 0, -s * .75, s * .045, '#5a3b22'); ell(c, 0, -s * .8, s * .07, s * .07, '#ffb35a'); break;
      case 'bow': c.strokeStyle = wood; c.lineWidth = s * .04; c.beginPath(); c.arc(0, -s * .1, s * .35, -1.2, 1.2); c.stroke(); limb(c, Math.cos(-1.2) * s * .35, -s * .1 + Math.sin(-1.2) * s * .35, Math.cos(1.2) * s * .35, -s * .1 + Math.sin(1.2) * s * .35, 1, '#ddd'); break;
      case 'lute': ell(c, 0, -s * .05, s * .12, s * .15, '#a0682e'); limb(c, 0, -s * .1, 0, -s * .45, s * .04, '#5a3b22'); break;
      case 'scimitar': c.strokeStyle = steel; c.lineWidth = s * .045; c.beginPath(); c.moveTo(0, 0); c.quadraticCurveTo(s * .12, -s * .25, 0, -s * .45); c.stroke(); break;
      case 'claw': limb(c, 0, 0, s * .08, -s * .12, s * .02, dark); limb(c, 0, 0, 0, -s * .14, s * .02, dark); limb(c, 0, 0, -s * .08, -s * .12, s * .02, dark); break;
      case 'trident': limb(c, 0, s * .3, 0, -s * .8, s * .035, '#c9a13a'); for (const dx of [-.08, 0, .08]) limb(c, s * dx, -s * .8, s * dx, -s * .95, s * .025, '#c9a13a'); limb(c, -s * .08, -s * .8, s * .08, -s * .8, s * .025, '#c9a13a'); break;
    }
    c.restore();
  }

  // ------------------------------------------------------------------ humanoids
  /** o: skin, cloth, cloth2, hair, h (height), w (width), ears, horns, beard, snout, tail, hood, hat, helm, weapon, offhand,
   *  shield, armor, robe, cape, eyes, bones, hunch, glow, pose{swing, cast, walk, hurt} */
  function humanoid(c, s, o, p) {
    const H = s * (o.h || 1), W = s * (o.w || 1);
    const bob = Math.sin(p.t * 2.2 + (o.seed || 0)) * s * .012 + (p.walk ? Math.abs(Math.sin(p.walk * 9)) * -s * .05 : 0);
    const legY = -H * .32, hipY = -H * .42 + bob, shoulderY = -H * .78 + bob + (o.hunch ? H * .06 : 0), headY = -H * .95 + bob + (o.hunch ? H * .1 : 0);
    const skin = o.skin || '#d8a47f', cloth = o.cloth || '#666', cloth2 = o.cloth2 || sh(cloth, -35);
    const stride = p.walk ? Math.sin(p.walk * 9) * W * .1 : 0;
    // tail / cape behind
    if (o.cape) poly(c, [[-W * .2, shoulderY], [W * .2, shoulderY], [W * .28 + Math.sin(p.t * 2) * W * .03, -H * .18], [-W * .28, -H * .16]], o.cape);
    if (o.tail) { c.strokeStyle = o.tailCol || skin; c.lineWidth = W * .09; c.lineCap = 'round'; c.beginPath(); c.moveTo(0, hipY + H * .05); c.quadraticCurveTo(-W * .35, -H * .1, -W * .45 + Math.sin(p.t * 3) * W * .05, -H * .25); c.stroke(); }
    // legs
    const legCol = o.bones ? '#e3dccb' : o.robe ? cloth2 : (o.legs || sh(cloth, -50));
    limb(c, -W * .08, hipY, -W * .1 + stride, legY + H * .3, W * (o.bones ? .05 : .11), legCol);
    limb(c, W * .08, hipY, W * .1 - stride, legY + H * .3, W * (o.bones ? .05 : .11), legCol);
    if (!o.bones) { ell(c, -W * .1 + stride, -s * .02, W * .08, W * .04, '#2a1d14'); ell(c, W * .1 - stride, -s * .02, W * .08, W * .04, '#2a1d14'); }
    // torso
    if (o.bones) {
      limb(c, 0, hipY, 0, shoulderY, W * .05, '#e3dccb');
      for (let i = 0; i < 3; i++) { c.strokeStyle = '#e3dccb'; c.lineWidth = W * .03; c.beginPath(); c.ellipse(0, shoulderY + H * (.06 + i * .08), W * .14, H * .03, 0, 0, Math.PI * 2); c.stroke(); }
      if (o.cloth && o.cloth !== '#666') poly(c, [[-W * .2, shoulderY], [W * .2, shoulderY], [W * .25, hipY + H * .1], [-W * .25, hipY + H * .1]], o.cloth + 'aa');
    } else if (o.robe) {
      poly(c, [[-W * .18, shoulderY], [W * .18, shoulderY], [W * .3, -s * .03], [-W * .3, -s * .03]], cloth);
      poly(c, [[-W * .05, shoulderY], [W * .05, shoulderY], [W * .08, -s * .03], [-W * .08, -s * .03]], cloth2);
    } else {
      poly(c, [[-W * .21, shoulderY], [W * .21, shoulderY], [W * .17, hipY + H * .04], [-W * .17, hipY + H * .04]], cloth);
      if (o.armor) { poly(c, [[-W * .21, shoulderY], [W * .21, shoulderY], [W * .18, shoulderY + H * .18], [-W * .18, shoulderY + H * .18]], '#9aa3ad'); ell(c, -W * .22, shoulderY + H * .02, W * .08, W * .06, '#b4bcc6'); ell(c, W * .22, shoulderY + H * .02, W * .08, W * .06, '#b4bcc6'); }
      limb(c, -W * .17, hipY, W * .17, hipY, W * .04, '#3a2414');
    }
    // arms: back arm holds offhand/shield, front arm holds weapon
    const swing = p.swing || 0, cast = p.cast || 0;
    const armCol = o.bones ? '#e3dccb' : o.armor ? '#9aa3ad' : o.sleeves || skin;
    const aW = W * (o.bones ? .045 : o.big ? .12 : .085);
    const backHand = [-W * .26, shoulderY + H * .3 - cast * H * .35];
    limb(c, -W * .2, shoulderY + H * .03, backHand[0], backHand[1], aW, armCol);
    if (o.shield) { ell(c, -W * .27, shoulderY + H * .26, W * .14, W * .17, o.shieldCol || '#7a5230'); c.strokeStyle = '#c9b06b'; c.lineWidth = W * .025; c.beginPath(); c.ellipse(-W * .27, shoulderY + H * .26, W * .14, W * .17, 0, 0, 7); c.stroke(); if (o.sigil) { c.fillStyle = '#f2d06b'; c.font = `${W * .16}px serif`; c.textAlign = 'center'; c.textBaseline = 'middle'; c.fillText(o.sigil, -W * .27, shoulderY + H * .27); } }
    if (o.offhand) weapon(c, o.offhand, backHand[0], backHand[1], -.3, s, cloth);
    // head
    const hr = W * (o.headR || .14);
    if (o.horns) { c.fillStyle = o.hornCol || '#3a2a33'; for (const d of [-1, 1]) { c.beginPath(); c.moveTo(d * hr * .5, headY - hr * .6); c.quadraticCurveTo(d * hr * 1.6, headY - hr * 1.8, d * hr * .9, headY - hr * 2.2); c.quadraticCurveTo(d * hr * 1.1, headY - hr * 1.3, d * hr * .1, headY - hr * .8); c.fill(); } }
    if (o.ears === 'elf' || o.ears === 'goblin') { const L = o.ears === 'goblin' ? 1.6 : 1.1; for (const d of [-1, 1]) poly(c, [[d * hr * .85, headY - hr * .1], [d * hr * (1 + L), headY - hr * (.6 + L * .3)], [d * hr * .8, headY + hr * .3]], o.ears === 'goblin' ? skin : sh(skin, -10)); }
    if (o.bones) { ell(c, 0, headY, hr * .95, hr, '#efe8d6'); ell(c, -hr * .35, headY, hr * .22, hr * .25, '#1a1a1a'); ell(c, hr * .35, headY, hr * .22, hr * .25, '#1a1a1a'); if (o.eyes) { ell(c, -hr * .35, headY, hr * .1, hr * .1, o.eyes); ell(c, hr * .35, headY, hr * .1, hr * .1, o.eyes); } limb(c, -hr * .3, headY + hr * .6, hr * .3, headY + hr * .6, hr * .12, '#efe8d6'); }
    else {
      if (o.snout) { ell(c, hr * .55, headY + hr * .25, hr * .7, hr * .45, skin); }
      ell(c, 0, headY, hr, hr * 1.05, skin);
      if (o.hair && !o.hood && !o.helm) { c.fillStyle = o.hair; c.beginPath(); c.ellipse(0, headY - hr * .25, hr * 1.05, hr * .85, 0, Math.PI, 0); c.fill(); if (o.longHair) poly(c, [[-hr, headY - hr * .2], [-hr * 1.15, headY + hr * 1.8], [-hr * .5, headY + hr * 1.2]], o.hair), poly(c, [[hr, headY - hr * .2], [hr * 1.15, headY + hr * 1.8], [hr * .5, headY + hr * 1.2]], o.hair); }
      const eyeCol = o.eyes || '#222';
      ell(c, -hr * .38, headY + hr * .05, hr * .14, hr * .16, '#fff'); ell(c, hr * .38, headY + hr * .05, hr * .14, hr * .16, '#fff');
      ell(c, -hr * .35 + p.face * hr * .05, headY + hr * .07, hr * .08, hr * .1, eyeCol); ell(c, hr * .41 + p.face * hr * .05, headY + hr * .07, hr * .08, hr * .1, eyeCol);
      if (o.eyeGlow) { c.save(); c.globalCompositeOperation = 'lighter'; ell(c, -hr * .35, headY + hr * .07, hr * .3, hr * .3, o.eyeGlow + '66'); ell(c, hr * .38, headY + hr * .07, hr * .3, hr * .3, o.eyeGlow + '66'); c.restore(); }
      if (o.beard) poly(c, [[-hr * .9, headY + hr * .2], [hr * .9, headY + hr * .2], [hr * .4, headY + hr * 1.9], [0, headY + hr * 2.2], [-hr * .4, headY + hr * 1.9]], o.beard);
      else if (o.teeth) { limb(c, -hr * .35, headY + hr * .55, hr * .35, headY + hr * .55, hr * .14, '#2a1010'); for (let i = -2; i <= 2; i++) poly(c, [[i * hr * .14 - hr * .05, headY + hr * .5], [i * hr * .14 + hr * .05, headY + hr * .5], [i * hr * .14, headY + hr * .68]], '#fff'); }
      else limb(c, -hr * .25, headY + hr * .55, hr * .25, headY + hr * .55, hr * .07, sh(skin, -60));
    }
    if (o.hood) { c.fillStyle = o.hood; c.beginPath(); c.moveTo(-hr * 1.25, headY + hr * .9); c.quadraticCurveTo(-hr * 1.3, headY - hr * 1.6, 0, headY - hr * 1.7); c.quadraticCurveTo(hr * 1.3, headY - hr * 1.6, hr * 1.25, headY + hr * .9); c.quadraticCurveTo(0, headY - hr * .4, -hr * 1.25, headY + hr * .9); c.fill(); }
    if (o.hat) { poly(c, [[-hr * 1.6, headY - hr * .55], [hr * 1.6, headY - hr * .55], [hr * .2, headY - hr * 3.1], [-hr * .3, headY - hr * 2.3]], o.hat); ell(c, 0, headY - hr * .55, hr * 1.6, hr * .3, sh(o.hat, -20)); }
    if (o.helm) { c.fillStyle = o.helm; c.beginPath(); c.arc(0, headY - hr * .1, hr * 1.1, Math.PI, 0); c.fill(); c.fillRect(-hr * 1.1, headY - hr * .15, hr * .3, hr * .9); c.fillRect(hr * .8, headY - hr * .15, hr * .3, hr * .9); }
    if (o.crown) poly(c, [[-hr * .8, headY - hr * .8], [-hr * .8, headY - hr * 1.5], [-hr * .4, headY - hr * 1.1], [0, headY - hr * 1.6], [hr * .4, headY - hr * 1.1], [hr * .8, headY - hr * 1.5], [hr * .8, headY - hr * .8]], o.crown);
    if (o.circlet) { c.strokeStyle = o.circlet; c.lineWidth = hr * .2; c.beginPath(); c.arc(0, headY + hr * .2, hr * 1.02, Math.PI * 1.15, Math.PI * 1.85); c.stroke(); }
    // front arm + weapon (drawn last so it reads clearly)
    const swingAng = -0.4 + swing * 2.2 - cast * 1.4;
    const hand = [W * .26 + Math.sin(swingAng) * W * .05, shoulderY + H * .3 - swing * H * .15 - cast * H * .35];
    limb(c, W * .2, shoulderY + H * .03, hand[0], hand[1], aW, armCol);
    if (o.weapon) weapon(c, o.weapon, hand[0], hand[1], o.weapon === 'bow' ? 0 : swingAng, s, cloth);
    if (cast > 0 && o.glow) { c.save(); c.globalCompositeOperation = 'lighter'; const g = c.createRadialGradient(hand[0], hand[1] - H * .05, 1, hand[0], hand[1] - H * .05, W * .35); g.addColorStop(0, o.glow + 'ff'); g.addColorStop(1, o.glow + '00'); c.fillStyle = g; c.beginPath(); c.arc(hand[0], hand[1] - H * .05, W * .35 * cast, 0, 7); c.fill(); c.restore(); }
    if (o.aura) { c.save(); c.globalCompositeOperation = 'lighter'; const g = c.createRadialGradient(0, -H * .5, 1, 0, -H * .5, W * .6); g.addColorStop(0, o.aura + '30'); g.addColorStop(1, o.aura + '00'); c.fillStyle = g; c.fillRect(-W, -H * 1.3, W * 2, H * 1.3); c.restore(); }
    return headY - hr * 2;
  }

  // ------------------------------------------------------------------ creatures
  function quadruped(c, s, o, p) {
    const W = s * (o.w || 1), H = s * (o.h || .7);
    const bob = Math.sin(p.t * 3 + (o.seed || 0)) * s * .01, gait = p.walk ? Math.sin(p.walk * 12) * W * .07 : 0;
    const body = o.col, dark = sh(body, -40);
    const bodyY = -H * .55 + bob;
    // tail
    c.strokeStyle = o.tailCol || body; c.lineWidth = W * (o.thickTail ? .12 : .06); c.lineCap = 'round';
    c.beginPath(); c.moveTo(-W * .32, bodyY); c.quadraticCurveTo(-W * .55, bodyY - H * .1, -W * (o.longTail ? .75 : .5), bodyY + Math.sin(p.t * 3) * H * .15 - (o.longTail ? 0 : H * .2)); c.stroke();
    // legs
    for (const [lx, ph] of [[-.25, 1], [-.15, -1], [.18, -1], [.28, 1]]) limb(c, W * lx, bodyY + H * .1, W * lx + gait * ph, -s * .02, W * (o.legW || .07), lx < 0 ? dark : sh(body, -20));
    // body
    ell(c, 0, bodyY, W * .38, H * .3, body);
    if (o.belly) ell(c, W * .02, bodyY + H * .12, W * .26, H * .14, o.belly);
    if (o.spines) for (let i = 0; i < 5; i++) poly(c, [[-W * .2 + i * W * .1, bodyY - H * .25], [-W * .15 + i * W * .1, bodyY - H * .48], [-W * .1 + i * W * .1, bodyY - H * .25]], o.spines);
    // wings
    if (o.wings) {
      const flap = Math.sin(p.t * (p.flying ? 6 : 1.5)) * .3 + (p.flying ? .3 : 0);
      for (const d of [-1, 1]) {
        c.save(); c.translate(0, bodyY - H * .2); c.rotate(-flap * d * .5);
        poly(c, [[0, 0], [-W * .15, -H * 1.1], [W * .25, -H * .9], [W * .5, -H * .55], [W * .35, -H * .2]], d < 0 ? sh(o.wings, -25) : o.wings);
        c.strokeStyle = sh(o.wings, -45); c.lineWidth = W * .02; for (const q of [[-W * .15, -H * 1.1], [W * .25, -H * .9], [W * .5, -H * .55]]) { c.beginPath(); c.moveTo(0, 0); c.lineTo(q[0], q[1]); c.stroke(); }
        c.restore();
      }
    }
    // neck & head (faces right)
    const lunge = (p.swing || 0) * W * .15;
    const nx = W * .3 + lunge, ny = bodyY - H * (o.longNeck ? .55 : .2);
    if (o.longNeck) { c.strokeStyle = body; c.lineWidth = W * .14; c.beginPath(); c.moveTo(W * .25, bodyY - H * .1); c.quadraticCurveTo(W * .45, bodyY - H * .4, nx + W * .05, ny); c.stroke(); }
    ell(c, nx + W * .1, ny, W * (o.headW || .16), H * (o.headH || .17), body);
    ell(c, nx + W * .24, ny + H * .05, W * .11, H * .09, o.snoutCol || sh(body, 10));
    if (o.ears) for (const d of [0, 1]) poly(c, [[nx + W * (0.02 + d * .08), ny - H * .12], [nx + W * (.04 + d * .08), ny - H * .32], [nx + W * (.1 + d * .08), ny - H * .12]], dark);
    if (o.horns) { c.fillStyle = o.horns; for (const d of [0, 1]) { const hx = nx + W * (.04 + d * .07), hy = ny - H * .12; c.beginPath(); c.moveTo(hx - W * .025, hy); c.quadraticCurveTo(hx - W * .08, hy - H * .2, hx - W * .16, hy - H * .26); c.quadraticCurveTo(hx - W * .05, hy - H * .12, hx + W * .025, hy); c.fill(); } }
    ell(c, nx + W * .14, ny - H * .03, W * .03, W * .03, o.eye || '#ffd84d');
    if (p.swing > .3 || o.teeth) for (let i = 0; i < 4; i++) poly(c, [[nx + W * (.18 + i * .04), ny + H * .08], [nx + W * (.2 + i * .04), ny + H * .15], [nx + W * (.22 + i * .04), ny + H * .08]], '#fff');
    if (o.breath && p.swing > .2) { c.save(); c.globalCompositeOperation = 'lighter'; const g = c.createRadialGradient(nx + W * .4, ny + H * .05, 1, nx + W * .4, ny + H * .05, W * .5); g.addColorStop(0, o.breath + 'cc'); g.addColorStop(1, o.breath + '00'); c.fillStyle = g; c.beginPath(); c.arc(nx + W * .4, ny, W * .5 * p.swing, 0, 7); c.fill(); c.restore(); }
    if (o.glowSeams) { c.save(); c.globalCompositeOperation = 'lighter'; c.strokeStyle = o.glowSeams; c.lineWidth = W * .015; for (let i = 0; i < 6; i++) { c.beginPath(); c.moveTo(-W * .3 + i * W * .1, bodyY - H * .2); c.lineTo(-W * .25 + i * W * .1, bodyY + H * .15); c.stroke(); } c.restore(); }
    return ny - H * .5;
  }

  function spider(c, s, o, p) {
    const W = s * (o.w || 1), bodyY = -s * .28 + Math.sin(p.t * 4) * s * .01;
    for (let i = 0; i < 4; i++) for (const d of [-1, 1]) { const a = (i - 1.5) * .35; c.strokeStyle = '#1a1a22'; c.lineWidth = W * .035; c.beginPath(); c.moveTo(0, bodyY); c.quadraticCurveTo(d * W * .35, bodyY - s * .25 + a * s * .2, d * W * (.45 + i * .03), -s * .02); c.stroke(); }
    ell(c, -W * .15, bodyY - s * .05, W * .25, s * .2, o.col);
    ell(c, W * .12, bodyY, W * .14, s * .12, sh(o.col, 15));
    for (let i = 0; i < 4; i++) ell(c, W * (.14 + (i % 2) * .04), bodyY - s * .03 + Math.floor(i / 2) * s * .04, W * .015, W * .015, '#ff3a3a');
    return bodyY - s * .3;
  }

  function crab(c, s, o, p) {
    const W = s, y = -s * .25;
    for (const d of [-1, 1]) for (let i = 0; i < 3; i++) limb(c, d * W * .15, y + s * .05, d * W * (.35 + i * .05), -s * .02, W * .04, sh(o.col, -30));
    ell(c, 0, y, W * .3, s * .18, o.col);
    for (const d of [-1, 1]) { limb(c, d * W * .25, y - s * .05, d * W * .4, y - s * .25 - (p.swing || 0) * s * .1, W * .06, o.col); ell(c, d * W * .45, y - s * .3, W * .1, W * .07, sh(o.col, 20)); }
    ell(c, -W * .08, y - s * .18, W * .03, W * .03, '#111'); ell(c, W * .08, y - s * .18, W * .03, W * .03, '#111');
    return y - s * .4;
  }

  function wisp(c, s, o, p) {
    const y = -s * .7 + Math.sin(p.t * 2 + (o.seed || 0)) * s * .1;
    c.save(); c.globalCompositeOperation = 'lighter';
    const g = c.createRadialGradient(0, y, 1, 0, y, s * .5); g.addColorStop(0, '#ffffff'); g.addColorStop(.3, o.col); g.addColorStop(1, o.col + '00');
    c.fillStyle = g; c.beginPath(); c.arc(0, y, s * .5, 0, 7); c.fill(); c.restore();
    if (o.skull) { ell(c, 0, y, s * .17, s * .19, '#efe8d6'); ell(c, -s * .06, y, s * .045, s * .05, '#2a0'); ell(c, s * .06, y, s * .045, s * .05, '#2a0'); }
    return y - s * .4;
  }

  function imp(c, s, o, p) {
    const y = -s * .55 + Math.sin(p.t * 4 + (o.seed || 0)) * s * .06;
    for (const d of [-1, 1]) { c.save(); c.translate(d * s * .1, y - s * .05); c.rotate(d * (.4 + Math.sin(p.t * 9) * .3)); poly(c, [[0, 0], [d * s * .35, -s * .2], [d * s * .3, s * .05]], sh(o.col, -30)); c.restore(); }
    ell(c, 0, y, s * .14, s * .18, o.col);
    ell(c, 0, y - s * .2, s * .11, s * .1, o.col);
    for (const d of [-1, 1]) poly(c, [[d * s * .05, y - s * .28], [d * s * .1, y - s * .4], [d * s * .1, y - s * .26]], sh(o.col, -40));
    ell(c, -s * .04, y - s * .2, s * .025, s * .025, '#fff'); ell(c, s * .04, y - s * .2, s * .025, s * .025, '#fff');
    if (o.glow) { c.save(); c.globalCompositeOperation = 'lighter'; ell(c, 0, y, s * .3, s * .3, o.glow + '44'); c.restore(); }
    return y - s * .45;
  }

  function object(c, s, o, p, u) {
    const name = (u.name || '').toLowerCase();
    if (/egg/.test(name)) { ell(c, 0, -s * .3, s * .2, s * .28, o.col || '#3a4a3a'); c.save(); c.globalCompositeOperation = 'lighter'; ell(c, 0, -s * .3, s * .3, s * .35, '#90ff9033'); c.restore(); return -s * .7; }
    if (/totem|idol|crystal|pillar|coral|rune/.test(name)) { poly(c, [[-s * .14, 0], [s * .14, 0], [s * .1, -s * .8], [0, -s * .95], [-s * .1, -s * .8]], o.col || '#7a8a9a'); c.save(); c.globalCompositeOperation = 'lighter'; ell(c, 0, -s * .6, s * .3, s * .4, (o.glow || '#9fe3ff') + '44'); c.restore(); return -s; }
    // brazier
    limb(c, -s * .15, 0, -s * .05, -s * .35, s * .05, '#1c1414'); limb(c, s * .15, 0, s * .05, -s * .35, s * .05, '#1c1414');
    ell(c, 0, -s * .4, s * .25, s * .1, '#2a2220');
    const fl = .85 + .15 * Math.sin(p.t * 11 + s);
    c.save(); c.globalCompositeOperation = 'lighter';
    for (let i = 0; i < 4; i++) { const hgt = s * (.35 + .15 * Math.sin(p.t * 9 + i * 2)) * fl; const g = c.createLinearGradient(0, -s * .4, 0, -s * .4 - hgt); g.addColorStop(0, 'rgba(255,210,100,.9)'); g.addColorStop(1, 'rgba(255,60,20,0)'); c.fillStyle = g; c.beginPath(); c.moveTo((i - 1.5) * s * .1 - s * .08, -s * .4); c.quadraticCurveTo((i - 1.5) * s * .1, -s * .4 - hgt * 1.3, (i - 1.5) * s * .1 + s * .08, -s * .4); c.fill(); }
    c.restore();
    return -s;
  }

  // ------------------------------------------------------------------ casting by class / monster
  const CLASS_GEAR = {
    wizard: { robe: true, weapon: 'staff', glow: '#ffb35a', hat: null },
    cleric: { armor: true, weapon: 'mace', shield: true, sigil: '☀', glow: '#ffe48a' },
    fighter: { armor: true, weapon: 'sword', shield: true, helm: '#9aa3ad' },
    paladin: { armor: true, weapon: 'sword', shield: true, sigil: '✦', glow: '#ffe48a', cape: '#2e5fa8' },
    barbarian: { weapon: 'axe', big: true, sleeves: null },
    rogue: { weapon: 'dagger', offhand: 'dagger', hood: '#2a2a35' },
    ranger: { weapon: 'bow', hood: '#2f4a2c', cape: '#2f4a2c' },
    bard: { weapon: 'scimitar', offhand: 'lute', glow: '#ff8ad8', cape: '#7a2a6a' },
  };

  F.heroSpec = function (hero) {
    const g = Object.assign({}, CLASS_GEAR[hero.class] || {});
    const sp = hero.species;
    const o = Object.assign(g, {
      skin: hero.skin, hair: hero.hair, eyes: hero.eyes, cloth: hero.color, cloth2: EW.shade(hero.color, -40),
      longHair: (hero.pronouns || '').startsWith('she'), seed: (hero.name || '').length,
      h: sp === 'goliath' ? 1.12 : sp === 'dwarf' ? .78 : sp === 'halfling' || sp === 'gnome' ? .68 : 1,
      w: sp === 'goliath' ? 1.1 : sp === 'dwarf' ? 1.05 : sp === 'halfling' || sp === 'gnome' ? .82 : 1,
    });
    if (sp === 'elf') o.ears = 'elf';
    if (sp === 'gnome') o.ears = 'elf';
    if (sp === 'tiefling') { o.horns = true; o.tail = true; o.tailCol = hero.skin; }
    if (sp === 'dragonborn') { o.snout = true; o.tail = true; o.horns = true; o.hornCol = EW.shade(hero.skin, -40); o.hair = null; }
    if (sp === 'dwarf' || ((hero.pronouns || '').startsWith('he') && hero.class === 'fighter')) o.beard = hero.hair;
    if (sp === 'aasimar') o.aura = '#ffe9a8';
    if (hero.class === 'wizard') o.circlet = '#8fd36a';
    if (hero.class === 'barbarian') o.sleeves = hero.skin;
    if (o.helm) o.hair = null;
    return { kind: 'humanoid', o };
  };

  F.monsterSpec = function (u) {
    const id = (u.monster || '') + ' ' + (u.name || '').toLowerCase();
    const col = u.color || '#888';
    const has = re => re.test(id);
    if (u.object) return { kind: 'object', o: { col } };
    if (has(/kobold/)) return { kind: 'humanoid', o: { skin: '#a0522d', cloth: '#6b4226', snout: true, tail: true, h: .62, w: .8, horns: true, hornCol: '#e8dcc0', weapon: has(/sling/) ? 'dagger' : has(/shaman/) ? 'staff' : 'spear', shield: has(/shaman|dragonshield/), shieldCol: '#2e3b2e', glow: '#9be35a' } };
    if (has(/thug|brawler|bandit|smuggler|hollis|pirate|sailor/)) return { kind: 'humanoid', o: { skin: '#c99a6e', cloth: col, hair: '#3a2a20', weapon: has(/brawler|hollis/) ? null : has(/pirate|sailor|smuggler/) ? 'scimitar' : 'mace', big: has(/hollis|thug/), w: has(/hollis/) ? 1.2 : 1 } };
    if (has(/dragon|vharkas|wyrm|drake|nyx/)) return { kind: 'quad', o: { col, w: 1, h: .8, longNeck: true, wings: EW.shade(col, -20), horns: '#d9cfa8', longTail: true, thickTail: true, spines: EW.shade(col, -45), belly: EW.shade(col, 35), eye: '#ffe066', breath: has(/black|nyx/) ? '#9be35a' : has(/white|frost/) ? '#bfe9ff' : '#ff7a2a', glowSeams: has(/vharkas|cinder/) ? '#ff8a3a' : null, teeth: true } };
    if (has(/wolf|ashmaw|hound/)) return { kind: 'quad', o: { col, w: .9, h: .65, ears: true, eye: has(/ash|dire/) ? '#ff7a2a' : '#ffd84d', snoutCol: EW.shade(col, -10) } };
    if (has(/boar/)) return { kind: 'quad', o: { col, w: .85, h: .6, ears: true, horns: '#f0e6d0', spines: EW.shade(col, -30) } };
    if (has(/toad|frog/)) return { kind: 'quad', o: { col, w: .9, h: .45, headW: .22, legW: .1, eye: '#ffef6a' } };
    if (has(/rat/)) return { kind: 'quad', o: { col, w: .7, h: .45, ears: true, longTail: true, tailCol: '#c99a9a', eye: '#ff3a3a' } };
    if (has(/owlbear/)) return { kind: 'humanoid', o: { skin: '#7a5c3a', cloth: '#7a5c3a', big: true, h: 1.05, w: 1.3, hair: '#5a4026', ears: 'goblin', weapon: 'claw', teeth: true, eyes: '#ffb000' } };
    if (has(/yeti/)) return { kind: 'humanoid', o: { skin: '#dfe8ee', cloth: '#e8eef2', big: true, h: 1.05, w: 1.3, hair: '#f5f9fb', weapon: 'claw', teeth: true, eyes: '#3fa0ff' } };
    if (has(/spider/)) return { kind: 'spider', o: { col: '#2b2b38' } };
    if (has(/crab/)) return { kind: 'crab', o: { col } };
    if (has(/wisp|lantern/)) return { kind: 'wisp', o: { col: '#9ad4ff' } };
    if (has(/flameskull/)) return { kind: 'wisp', o: { col: '#66ff66', skull: true } };
    if (has(/mephit|imp/)) return { kind: 'imp', o: { col, glow: has(/ice/) ? '#9fe3ff' : '#ff7a2a' } };
    if (has(/skeleton|bone/)) return { kind: 'humanoid', o: { bones: true, eyes: '#ff9a3a', weapon: has(/minotaur/) ? 'axe' : 'sword', horns: has(/minotaur/), hornCol: '#e3dccb', h: has(/minotaur/) ? 1.2 : 1, w: has(/minotaur/) ? 1.2 : 1 } };
    if (has(/zombie|ghoul|drowned|bog/)) return { kind: 'humanoid', o: { skin: has(/ghoul/) ? '#9aa88a' : '#7d8f6a', cloth: '#4a4a3a', hunch: true, eyes: '#e8f050', weapon: 'claw', teeth: has(/ghoul/), hair: '#2a3020' } };
    if (has(/shadow/)) return { kind: 'humanoid', o: { skin: '#141420', cloth: '#141420', robe: true, eyes: '#c0d0ff', eyeGlow: '#c0d0ff', hood: '#0a0a12' } };
    if (has(/armor|warden|sentinel|golem/)) return { kind: 'humanoid', o: { skin: '#3a4048', cloth: '#90a4ae', armor: true, helm: '#b0bec5', weapon: 'greatsword', shield: false, eyes: '#9fe3ff', eyeGlow: '#9fe3ff', h: 1.08 } };
    if (has(/gargoyle/)) return { kind: 'humanoid', o: { skin: '#6f6f78', cloth: '#6f6f78', horns: true, hornCol: '#55555c', ears: 'goblin', weapon: 'claw', teeth: true, cape: '#5a5a62' } };
    if (has(/snikkit/)) return { kind: 'humanoid', o: { skin: '#6a9a3a', cloth: '#c9c9e0', ears: 'goblin', h: .7, w: .85, weapon: 'scimitar', crown: '#5a8fd6', teeth: true, cape: '#e8e0f0' } };
    if (has(/goblin/)) return { kind: 'humanoid', o: { skin: '#6a9a3a', cloth: '#5a4026', ears: 'goblin', h: .7, w: .85, weapon: has(/archer|sniper/) ? 'bow' : 'scimitar', teeth: true } };
    if (has(/hobgoblin/)) return { kind: 'humanoid', o: { skin: '#c0602a', cloth: '#8b2e2e', ears: 'elf', armor: true, weapon: 'sword', shield: true, shieldCol: '#5a1a1a', helm: '#555' } };
    if (has(/bugbear/)) return { kind: 'humanoid', o: { skin: '#8d6e3f', cloth: '#4a3a24', hair: '#5a4026', ears: 'goblin', h: 1.1, w: 1.15, weapon: 'hammer', teeth: true } };
    if (has(/gnoll/)) return { kind: 'humanoid', o: { skin: '#a67c52', cloth: '#5a4026', snout: true, ears: 'goblin', hair: '#4a3020', weapon: 'spear', teeth: true, hunch: true } };
    if (has(/ogre/)) return { kind: 'humanoid', o: { skin: '#9aa860', cloth: '#5a4026', h: 1.15, w: 1.3, big: true, weapon: 'club', teeth: true, hair: '#3a3020' } };
    if (has(/sahuagin|merrow|fish/)) return { kind: 'humanoid', o: { skin: has(/merrow/) ? '#3a6b5e' : '#2e7d6b', cloth: '#1e4a40', snout: true, tail: true, weapon: has(/merrow/) ? 'trident' : 'spear', teeth: true, h: has(/merrow/) ? 1.15 : 1, w: has(/merrow/) ? 1.2 : 1, eyes: '#ffef6a' } };
    if (has(/hag|witch|queen|brine|isolde/)) return { kind: 'humanoid', o: { skin: has(/isolde|winter|queen/) ? '#cfe3f0' : '#7a9a7a', cloth: col, robe: true, hair: has(/isolde|winter/) ? '#f2f8ff' : '#2a3a2a', longHair: true, crown: '#c9e8ff', weapon: 'staff', glow: has(/isolde|winter/) ? '#9fe3ff' : '#6affc0', eyes: '#9fe3ff', eyeGlow: '#9fe3ff' } };
    if (has(/cultist|acolyte|zealot|cindrel|choir|shaman|priest|sorcer|mage/)) return { kind: 'humanoid', o: { skin: '#d8a47f', cloth: col, robe: true, hood: has(/cindrel/) ? null : EW.shade(col, -30), hair: '#5a2a20', longHair: has(/cindrel|sister/), weapon: has(/zealot|cindrel/) ? 'scimitar' : 'dagger', glow: '#ff7a2a' } };
    if (has(/beast|monstrosity/)) return { kind: 'quad', o: { col } };
    // fallback by creature type
    return { kind: 'humanoid', o: { skin: '#c99a6e', cloth: col, hair: '#2a2a2a', weapon: 'sword' } };
  };

  /**
   * Draw a unit as a miniature. (x, y) = centre of its footprint in pixels; s = footprint size in pixels.
   * pose: {t, walk, swing, cast, hurt, dead, down, face}
   */
  F.draw = function (c, u, hero, x, y, s, pose) {
    const spec = hero ? F.heroSpec(hero) : F.monsterSpec(u);
    pose.face = pose.face || 1;
    c.save();
    c.translate(x, y + s * .32);
    // base (like a painted miniature base), coloured by side
    const baseCol = u.side === 'party' ? (u.ally ? '#e6c068' : '#57c9a2') : '#d9534f';
    ell(c, 0, 0, s * .4, s * .14, 'rgba(0,0,0,.45)');
    ell(c, 0, -s * .02, s * .36, s * .12, '#232530');
    c.strokeStyle = baseCol; c.lineWidth = Math.max(1.5, s * .03); c.beginPath(); c.ellipse(0, -s * .02, s * .36, s * .12, 0, 0, Math.PI * 2); c.stroke();
    if (u.object) { const top = object(c, s, spec.o, pose, u); c.restore(); return { top: y + s * .32 + top }; }
    // death / downed: tip the figure over
    if (pose.down || pose.dead) { c.translate(-s * .05, -s * .05); c.rotate(-Math.PI / 2 * Math.min(1, pose.down || pose.dead)); c.scale(.8, .8); }
    if (pose.hurt) { c.translate(-pose.face * s * .06 * pose.hurt, 0); c.rotate(-pose.face * .15 * pose.hurt); }
    if (pose.face < 0) c.scale(-1, 1);
    const kindFn = { humanoid, quad: quadruped, spider, crab, wisp, imp, object }[spec.kind] || humanoid;
    const S = s * (u.size > 1 ? 1.0 : 1.3);
    const top = kindFn(c, S, spec.o, pose, u);
    c.restore();
    return { top: y + s * .32 + top };
  };
})();
