'use client';
// ^ needed by Next.js (touches window/canvas). Harmless no-op in Vite/CRA.

import { useRef, useEffect } from 'react';
import './gyrofield.css';

/*
  <GyroField variant="hero|steel|synapse|horizon|circuit" onEnergize={fn} />

  One engine, five expressions of the living conductor field. Tracks the
  mouse on desktop and the gyroscope on mobile (with iOS tap-to-enable),
  and renders a static frame when prefers-reduced-motion is set.

  onEnergize: optional, fired by the "circuit" variant each time a pulse
  lands at the center — wire it to glow your sign-in card.
*/

const COPPER = [224, 144, 74];
const SIGNAL = [79, 209, 197];

const rand = (a, b) => a + Math.random() * (b - a);
const pick = (a) => a[(Math.random() * a.length) | 0];

/* ---------- hero helpers: routed traces + arc-length sampling ---------- */
function makeTrace(grid, W, H, M) {
  const cols = Math.ceil((W + M * 2) / grid);
  const rows = Math.ceil((H + M * 2) / grid);
  let cx = (Math.random() * cols) | 0;
  let cy = (Math.random() * rows) | 0;
  const pts = [[cx * grid - M, cy * grid - M]];
  const len = 6 + ((Math.random() * 10) | 0);
  let dir = pick([[1, 0], [-1, 0], [0, 1], [0, -1]]);
  for (let i = 0; i < len; i++) {
    if (Math.random() < 0.42)
      dir = Math.abs(dir[0]) ? pick([[0, 1], [0, -1]]) : pick([[1, 0], [-1, 0]]);
    const step = 1 + ((Math.random() * 3) | 0);
    cx = Math.max(0, Math.min(cols, cx + dir[0] * step));
    cy = Math.max(0, Math.min(rows, cy + dir[1] * step));
    pts.push([cx * grid - M, cy * grid - M]);
  }
  const cum = [0];
  let total = 0;
  for (let i = 1; i < pts.length; i++) {
    total += Math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]);
    cum.push(total);
  }
  return { pts, cum, total };
}
function sampleAt(tr, s) {
  const { pts, cum, total } = tr;
  if (total === 0) return pts[0];
  s = ((s % total) + total) % total;
  let i = 1;
  while (i < cum.length && cum[i] < s) i++;
  if (i >= cum.length) return pts[pts.length - 1];
  const segLen = cum[i] - cum[i - 1] || 1;
  const f = (s - cum[i - 1]) / segLen;
  return [
    pts[i - 1][0] + (pts[i][0] - pts[i - 1][0]) * f,
    pts[i - 1][1] + (pts[i][1] - pts[i - 1][1]) * f,
  ];
}
function pulseTrail(ctx, x, y, col, steps, gap) {
  const [r, g, b] = col;
  ctx.shadowBlur = 14;
  ctx.shadowColor = `rgba(${r},${g},${b},0.9)`;
  ctx.fillStyle = `rgb(${r},${g},${b})`;
  ctx.beginPath();
  ctx.arc(x, y, 2.4, 0, 7);
  ctx.fill();
  ctx.shadowBlur = 0;
  for (let i = 1; i <= steps; i++) {
    ctx.fillStyle = `rgba(${r},${g},${b},${0.5 * (1 - i / steps)})`;
    ctx.beginPath();
    ctx.arc(x - gap * i, y, 2.4 * (1 - (i / steps) * 0.7), 0, 7);
    ctx.fill();
  }
}

/* ---------------------------- variant factories --------------------------- */
function makeVariant(variant, getEnergize) {
  switch (variant) {
    /* ---- HERO: parallax trace layers + traveling current ---- */
    case 'hero': {
      const M = 140;
      let layers = [];
      const MAXSHIFT = 30;
      return {
        pulse: SIGNAL,
        build(W, H) {
          const defs = [
            { depth: 0.35, grid: 96, count: 7, w: 1, alpha: 0.1, pulses: 2, color: '#7B8696' },
            { depth: 0.72, grid: 80, count: 9, w: 1.2, alpha: 0.16, pulses: 4, color: '#7B8696' },
            { depth: 1.18, grid: 64, count: 11, w: 1.6, alpha: 0.22, pulses: 6, color: '#9aa6b6' },
          ];
          layers = defs.map((d) => {
            const traces = [];
            for (let i = 0; i < d.count; i++) traces.push(makeTrace(d.grid, W, H, M));
            const pulses = [];
            for (let i = 0; i < d.pulses; i++) {
              const tr = pick(traces);
              pulses.push({
                tr, s: Math.random() * tr.total,
                speed: rand(28, 64) * d.depth,
                col: Math.random() < 0.5 ? COPPER : SIGNAL,
                tail: rand(28, 60),
              });
            }
            return { ...d, traces, pulses };
          });
        },
        draw(ctx, W, H, o, dt, isStatic) {
          ctx.clearRect(0, 0, W, H);
          for (const layer of layers) {
            const ox = isStatic ? -M * 0.4 * layer.depth : -o.x * MAXSHIFT * layer.depth;
            const oy = isStatic ? -M * 0.4 * layer.depth : -o.y * MAXSHIFT * layer.depth;
            ctx.save();
            ctx.translate(ox, oy);
            ctx.lineWidth = layer.w; ctx.strokeStyle = layer.color;
            ctx.lineCap = 'round'; ctx.lineJoin = 'round';
            for (const tr of layer.traces) {
              ctx.globalAlpha = layer.alpha;
              ctx.beginPath();
              ctx.moveTo(tr.pts[0][0], tr.pts[0][1]);
              for (let i = 1; i < tr.pts.length; i++) ctx.lineTo(tr.pts[i][0], tr.pts[i][1]);
              ctx.stroke();
              ctx.globalAlpha = layer.alpha * 1.6;
              ctx.fillStyle = layer.color;
              const a = tr.pts[0], b = tr.pts[tr.pts.length - 1];
              ctx.fillRect(a[0] - 1.5, a[1] - 1.5, 3, 3);
              ctx.fillRect(b[0] - 1.5, b[1] - 1.5, 3, 3);
            }
            ctx.globalAlpha = 1;
            for (const p of layer.pulses) {
              if (!isStatic) p.s += p.speed * dt;
              const [hx, hy] = sampleAt(p.tr, p.s);
              if (isStatic) {
                const [r, g, b] = p.col;
                ctx.fillStyle = `rgba(${r},${g},${b},0.85)`;
                ctx.beginPath(); ctx.arc(hx, hy, 2.2, 0, 7); ctx.fill();
              } else {
                pulseTrail(ctx, hx, hy, p.col, 7, p.tail / 7);
              }
            }
            ctx.restore();
          }
        },
      };
    }

    /* ---- STEEL: quiet brushed grid + raking copper light + bus current ---- */
    case 'steel': {
      let pulses = [];
      return {
        pulse: COPPER,
        build() {
          pulses = [
            { y: 0.3, s: Math.random(), sp: 0.05, col: COPPER },
            { y: 0.68, s: Math.random(), sp: 0.04, col: SIGNAL },
          ];
        },
        draw(ctx, W, H, o, dt, isStatic) {
          ctx.clearRect(0, 0, W, H);
          const ox = -o.x * 24, oy = -o.y * 24, g = 58;
          ctx.lineWidth = 1; ctx.strokeStyle = 'rgba(123,134,150,0.07)';
          ctx.beginPath();
          for (let x = (ox % g) - g; x < W + g; x += g) { ctx.moveTo(x, 0); ctx.lineTo(x, H); }
          for (let y = (oy % g) - g; y < H + g; y += g) { ctx.moveTo(0, y); ctx.lineTo(W, y); }
          ctx.stroke();

          const lx = W * (0.5 + o.x * 0.42), ly = H * (0.5 + o.y * 0.42), r = Math.max(W, H) * 0.6;
          const grd = ctx.createRadialGradient(lx, ly, 0, lx, ly, r);
          grd.addColorStop(0, 'rgba(224,144,74,0.14)');
          grd.addColorStop(0.4, 'rgba(224,144,74,0.05)');
          grd.addColorStop(1, 'rgba(224,144,74,0)');
          ctx.fillStyle = grd; ctx.fillRect(0, 0, W, H);

          for (const p of pulses) {
            const y = H * p.y + oy * 0.4;
            ctx.strokeStyle = 'rgba(123,134,150,0.12)'; ctx.lineWidth = 1.4;
            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
            if (!isStatic) p.s = (p.s + p.sp * dt) % 1;
            pulseTrail(ctx, p.s * W, y, p.col, 8, 9);
          }
        },
      };
    }

    /* ---- SYNAPSE: charge-nodes that connect + fire; attention follows hand ---- */
    case 'synapse': {
      const N = 46, LINK = 150;
      let nodes = [], sparks = [];
      return {
        pulse: SIGNAL,
        build() {
          nodes = [];
          for (let i = 0; i < N; i++)
            nodes.push({
              x: Math.random(), y: Math.random(),
              vx: (Math.random() - 0.5) * 0.012, vy: (Math.random() - 0.5) * 0.012,
              r: Math.random() * 1.6 + 1, copper: Math.random() < 0.35,
            });
          sparks = [];
        },
        draw(ctx, W, H, o, dtSec, isStatic) {
          const dt = dtSec * 60;
          ctx.clearRect(0, 0, W, H);
          const ox = -o.x * 30, oy = -o.y * 30;
          const ax = W * (0.5 + o.x * 0.5), ay = H * (0.5 + o.y * 0.5);
          if (!isStatic) for (const n of nodes) {
            n.x += n.vx * dt; n.y += n.vy * dt;
            if (n.x < 0 || n.x > 1) n.vx *= -1;
            if (n.y < 0 || n.y > 1) n.vy *= -1;
            n.x = Math.max(0, Math.min(1, n.x)); n.y = Math.max(0, Math.min(1, n.y));
          }
          const px = (n) => n.x * W + ox, py = (n) => n.y * H + oy;
          for (let i = 0; i < nodes.length; i++)
            for (let j = i + 1; j < nodes.length; j++) {
              const a = nodes[i], b = nodes[j];
              const d = Math.hypot(px(a) - px(b), py(a) - py(b));
              if (d < LINK) {
                const mx = (px(a) + px(b)) / 2, my = (py(a) + py(b)) / 2;
                const att = 1 - Math.min(1, Math.hypot(mx - ax, my - ay) / (Math.max(W, H) * 0.6));
                const base = 1 - d / LINK;
                ctx.strokeStyle = `rgba(79,209,197,${base * 0.16 + base * att * 0.4})`;
                ctx.lineWidth = 0.6 + att * 0.8;
                ctx.beginPath(); ctx.moveTo(px(a), py(a)); ctx.lineTo(px(b), py(b)); ctx.stroke();
                if (!isStatic && att > 0.55 && Math.random() < 0.004)
                  sparks.push({ ax: px(a), ay: py(a), bx: px(b), by: py(b), s: 0, col: a.copper ? COPPER : SIGNAL });
              }
            }
          for (const n of nodes) {
            const c = n.copper ? COPPER : SIGNAL;
            ctx.fillStyle = `rgba(${c[0]},${c[1]},${c[2]},0.7)`;
            ctx.beginPath(); ctx.arc(px(n), py(n), n.r, 0, 7); ctx.fill();
          }
          for (let k = sparks.length - 1; k >= 0; k--) {
            const s = sparks[k];
            if (!isStatic) s.s += dtSec * 96; // matches screen-3 spark speed (~1.6 per frame @60fps)
            if (s.s >= 1) { sparks.splice(k, 1); continue; }
            const x = s.ax + (s.bx - s.ax) * s.s, y = s.ay + (s.by - s.ay) * s.s;
            ctx.shadowBlur = 12; ctx.shadowColor = `rgba(${s.col[0]},${s.col[1]},${s.col[2]},.9)`;
            ctx.fillStyle = `rgb(${s.col[0]},${s.col[1]},${s.col[2]})`;
            ctx.beginPath(); ctx.arc(x, y, 2.2, 0, 7); ctx.fill(); ctx.shadowBlur = 0;
          }
        },
      };
    }

    /* ---- HORIZON: perspective wireframe floor that tips in 3D ---- */
    case 'horizon': {
      let scroll = 0;
      return {
        pulse: SIGNAL,
        build() { scroll = 0; },
        draw(ctx, W, H, o, dt, isStatic) {
          ctx.clearRect(0, 0, W, H);
          const horizon = H * (0.46 + o.y * 0.06);
          const vpx = W * (0.5 + o.x * 0.18);
          const bottom = H + 40;
          ctx.lineWidth = 1;
          const cols = 22;
          for (let i = 0; i <= cols; i++) {
            const fx = (i / cols) * 1.8 - 0.4;
            const a = 0.05 + 0.1 * (1 - Math.abs(fx - 0.5) / 1.4);
            ctx.strokeStyle = `rgba(79,209,197,${Math.max(0.02, a)})`;
            ctx.beginPath(); ctx.moveTo(vpx, horizon); ctx.lineTo(fx * W, bottom); ctx.stroke();
          }
          if (!isStatic) scroll = (scroll + dt * 0.18) % 1;
          for (let i = 0; i < 16; i++) {
            const ease = ((i + scroll) / 16) ** 2;
            const y = horizon + (bottom - horizon) * ease;
            ctx.strokeStyle = `rgba(123,134,150,${0.04 + 0.16 * ease})`;
            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
          }
          ctx.strokeStyle = 'rgba(79,209,197,0.35)'; ctx.lineWidth = 1.2;
          ctx.beginPath(); ctx.moveTo(0, horizon); ctx.lineTo(W, horizon); ctx.stroke();
          if (!isStatic) {
            const sx = ((performance.now() / 4000) % 1) * W;
            ctx.shadowBlur = 14; ctx.shadowColor = 'rgba(79,209,197,.9)';
            ctx.fillStyle = 'rgb(79,209,197)';
            ctx.beginPath(); ctx.arc(sx, horizon, 2.4, 0, 7); ctx.fill(); ctx.shadowBlur = 0;
          }
          ctx.fillStyle = 'rgba(123,134,150,0.18)';
          for (let i = 0; i < 40; i++)
            ctx.fillRect(((i * 97.31) % W) + o.x * 8, ((i * 53.7) % horizon) + o.y * 4, 1, 1);
        },
      };
    }

    /* ---- CIRCUIT: traces converge on center; pulses energize on arrival ---- */
    case 'circuit':
    default: {
      let spokes = [], pulses = [];
      return {
        pulse: COPPER,
        build() {
          spokes = [];
          const n = 20, inner = 110;
          for (let i = 0; i < n; i++)
            spokes.push({ ang: (i / n) * Math.PI * 2 + (Math.random() - 0.5) * 0.15, inner: inner + Math.random() * 30 });
          pulses = [];
        },
        draw(ctx, W, H, o, dt, isStatic) {
          ctx.clearRect(0, 0, W, H);
          const cx = W * (0.5 + o.x * 0.12), cy = H * (0.5 + o.y * 0.12);
          const outer = Math.max(W, H) * 0.85;
          for (const s of spokes) {
            const x1 = cx + Math.cos(s.ang) * s.inner, y1 = cy + Math.sin(s.ang) * s.inner;
            const x2 = cx + Math.cos(s.ang) * outer, y2 = cy + Math.sin(s.ang) * outer;
            const g = ctx.createLinearGradient(x1, y1, x2, y2);
            g.addColorStop(0, 'rgba(123,134,150,0.02)');
            g.addColorStop(0.15, 'rgba(123,134,150,0.16)');
            g.addColorStop(1, 'rgba(123,134,150,0.02)');
            ctx.strokeStyle = g; ctx.lineWidth = 1;
            ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke();
          }
          if (!isStatic && Math.random() < 0.05)
            pulses.push({ s: pick(spokes), r: outer, col: Math.random() < 0.5 ? COPPER : SIGNAL });
          for (let k = pulses.length - 1; k >= 0; k--) {
            const p = pulses[k];
            if (!isStatic) p.r -= dt * 260;
            if (p.r <= p.s.inner) {
              pulses.splice(k, 1);
              const fn = getEnergize && getEnergize();
              if (fn) fn();
              continue;
            }
            const x = cx + Math.cos(p.s.ang) * p.r, y = cy + Math.sin(p.s.ang) * p.r;
            const [r, g, b] = p.col;
            ctx.shadowBlur = 12; ctx.shadowColor = `rgba(${r},${g},${b},.9)`;
            ctx.fillStyle = `rgb(${r},${g},${b})`;
            ctx.beginPath(); ctx.arc(x, y, 2.2, 0, 7); ctx.fill(); ctx.shadowBlur = 0;
            for (let i = 1; i <= 6; i++) {
              const rr = p.r + i * 7;
              ctx.fillStyle = `rgba(${r},${g},${b},${0.4 * (1 - i / 6)})`;
              ctx.beginPath();
              ctx.arc(cx + Math.cos(p.s.ang) * rr, cy + Math.sin(p.s.ang) * rr, 2 * (1 - i / 7), 0, 7);
              ctx.fill();
            }
          }
          ctx.strokeStyle = 'rgba(224,144,74,0.12)'; ctx.lineWidth = 1;
          ctx.beginPath(); ctx.arc(cx, cy, 120, 0, 7); ctx.stroke();
        },
      };
    }
  }
}

export default function GyroField({ variant = 'hero', onEnergize }) {
  const canvasRef = useRef(null);
  const hintRef = useRef(null);
  const hintTextRef = useRef(null);
  const energizeRef = useRef(onEnergize);
  energizeRef.current = onEnergize;

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const v = makeVariant(variant, () => energizeRef.current);

    let W = 0, H = 0, raf = 0, last = performance.now();
    const t = { x: 0, y: 0 }, o = { x: 0, y: 0 };

    const resize = () => {
      const DPR = Math.min(window.devicePixelRatio || 1, 2);
      W = window.innerWidth; H = window.innerHeight;
      canvas.width = Math.floor(W * DPR); canvas.height = Math.floor(H * DPR);
      canvas.style.width = W + 'px'; canvas.style.height = H + 'px';
      ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
      v.build(W, H);
      if (reduce) v.draw(ctx, W, H, { x: 0, y: 0 }, 0, true);
    };

    const frame = (now) => {
      const dt = Math.min((now - last) / 1000, 0.05); last = now;
      o.x += (t.x - o.x) * 0.06; o.y += (t.y - o.y) * 0.06;
      v.draw(ctx, W, H, o, dt, false);
      raf = requestAnimationFrame(frame);
    };

    /* ---- input plumbing (shared across all variants) ---- */
    const onMouse = (e) => { t.x = (e.clientX / W) * 2 - 1; t.y = (e.clientY / H) * 2 - 1; };
    let baseB = null, live = false;
    const onTilt = (e) => {
      if (e.gamma == null) return;
      if (baseB == null) baseB = e.beta || 45;
      t.x = Math.max(-1, Math.min(1, e.gamma / 32));
      t.y = Math.max(-1, Math.min(1, ((e.beta || 45) - baseB) / 32));
      live = true;
    };

    const hint = hintRef.current;
    const isTouch = window.matchMedia('(pointer: coarse)').matches;
    const needsPerm =
      typeof DeviceOrientationEvent !== 'undefined' &&
      typeof DeviceOrientationEvent.requestPermission === 'function';
    if (isTouch && hintTextRef.current)
      hintTextRef.current.textContent = needsPerm ? 'Tap — then tilt' : 'Tilt your phone';

    const enable = async () => {
      try {
        if (needsPerm) {
          const res = await DeviceOrientationEvent.requestPermission();
          if (res !== 'granted') return;
        }
        window.addEventListener('deviceorientation', onTilt, true);
      } catch (_) {}
    };
    const goneTimers = [];
    const gone = () => hint && hint.classList.add('is-gone');
    const onHintClick = () => { enable(); goneTimers.push(setTimeout(gone, 400)); };
    const onFirstTap = () => { if (isTouch) enable(); };

    hint && hint.addEventListener('click', onHintClick);
    document.addEventListener('click', onFirstTap, { once: true });

    if (!isTouch) {
      window.addEventListener('mousemove', onMouse, { passive: true });
      goneTimers.push(setTimeout(gone, 4200));
    } else if (!needsPerm) {
      window.addEventListener('deviceorientation', onTilt, true);
      goneTimers.push(setTimeout(() => { if (live) gone(); }, 4200));
    }

    window.addEventListener('resize', resize, { passive: true });
    resize();
    if (!reduce) raf = requestAnimationFrame(frame);
    else gone();

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', resize);
      window.removeEventListener('mousemove', onMouse);
      window.removeEventListener('deviceorientation', onTilt, true);
      hint && hint.removeEventListener('click', onHintClick);
      document.removeEventListener('click', onFirstTap);
      goneTimers.forEach(clearTimeout);
    };
  }, [variant]);

  // hint pulse color matches the variant's accent
  const accent = variant === 'steel' || variant === 'circuit' ? '224,144,74' : '79,209,197';

  return (
    <>
      <canvas ref={canvasRef} className="ww-field" />
      <div ref={hintRef} className="ww-hint">
        <span
          className="ww-hint__pulse"
          style={{ background: `rgb(${accent})`, boxShadow: `0 0 8px rgb(${accent})` }}
        />
        <span ref={hintTextRef}>Move your mouse</span>
      </div>
    </>
  );
}
