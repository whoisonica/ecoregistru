// Plimbarea vizuală a temelor (pasul 7 din `docs/specs/2026-10-14-teme-culori-plan.md`): capturi pentru privit, nu probă.
// Fiecare ecran principal, plus dialoguri, meniuri și paleta, în tema dată, la 1440 și 375. Pe lângă capturi, măsoară
// contrastul fiecărui text cu fondul de sub el și petele deschise rămase pe Întunecat, ca să știi unde să te uiți.
// E2E_BASE=… E2E_PASSWORD=… node e2e/shots-teme-toate.mjs <dir> <luminos|padure|intunecat>
import fs from "node:fs";
import { launch, newPage, login, BASE } from "./lib.mjs";

const OUT = process.argv[2];
const THEME = process.argv[3] ?? "luminos";
fs.mkdirSync(OUT, { recursive: true });
const AN = new Date().getFullYear();

const SCREENS = [
  ["/", "acasa"],
  [`/generare?luna=${AN}`, "generare"],
  ["/generare?tab=total", "total"],
  ["/generare?tab=ambalaje", "ambalaje"],
  [`/intrari?luna=${AN}`, "intrari"],
  [`/iesiri?luna=${AN}`, "iesiri"],
  ["/cantar", "cantar"],
  ["/termene", "termene"],
  ["/termene?tab=bifate", "termene-bifate"],
  [`/energie?an=${AN - 1}`, "energie"],
  [`/raport-mediu?an=${AN - 1}`, "raport-mediu"],
  ["/raport-mediu?tab=autorizatie", "raport-autorizatie"],
  ["/dosar-control", "dosar"],
  ["/parteneri", "parteneri"],
  ["/parteneri?tab=persoane-fizice", "persoane-fizice"],
  ["/setari", "setari"],
  ["/setari/datele-firmei", "setari-firma"],
  ["/setari/puncte-de-lucru", "setari-puncte"],
  ["/setari/utilizatori", "setari-utilizatori"],
  ["/setari/jurnal-audit", "setari-jurnal"],
  ["/import", "import"],
  ["/abonament", "abonament"],
];
const PLATFORM = [
  ["/clienti", "clienti"],
  ["/facturare", "facturare"],
  ["/consultant", "consultant"],
];
const PUBLIC = [
  ["/login", "login"],
  ["/cerere-cont", "cerere-cont"],
  ["/parola-uitata", "parola-uitata"],
  ["/termeni", "termeni"],
];

/** Contrastul textelor vizibile cu fondul efectiv și, pe Întunecat, fondurile deschise mai mari de 24×24. */
async function measure(page, dark) {
  return page.evaluate((dark) => {
    const parse = (c) => {
      const m = c.match(/rgba?\(([^)]+)\)/);
      if (!m) return null;
      const [r, g, b, a = 1] = m[1].split(/[ ,/]+/).filter(Boolean).map(Number);
      return { r, g, b, a };
    };
    const lum = ({ r, g, b }) => {
      const f = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; };
      return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
    };
    const over = (top, bottom) => ({
      r: top.r * top.a + bottom.r * (1 - top.a),
      g: top.g * top.a + bottom.g * (1 - top.a),
      b: top.b * top.a + bottom.b * (1 - top.a),
      a: 1,
    });
    const bgOf = (el) => {
      const layers = [];
      for (let e = el; e; e = e.parentElement) {
        const c = parse(getComputedStyle(e).backgroundColor);
        if (c && c.a > 0) { layers.push(c); if (c.a >= 1) break; }
      }
      let bg = { r: 255, g: 255, b: 255, a: 1 };
      if (layers.length && layers[layers.length - 1].a < 1) bg = dark ? { r: 19, g: 26, b: 22, a: 1 } : bg;
      for (let i = layers.length - 1; i >= 0; i--) bg = over(layers[i], bg);
      return bg;
    };
    const faded = (el) => { for (let e = el; e; e = e.parentElement) if (Number(getComputedStyle(e).opacity) < 1) return true; return false; };
    const label = (el) => `${el.tagName.toLowerCase()}.${String(el.className).split(" ").slice(0, 4).join(".")}`;
    const low = [];
    const seen = new Set();
    for (const el of document.querySelectorAll("body *")) {
      const own = [...el.childNodes].filter((n) => n.nodeType === 3).map((n) => n.textContent.trim()).join(" ").trim();
      if (!own) continue;
      const r = el.getBoundingClientRect();
      if (r.width < 2 || r.height < 2 || r.bottom < 0 || r.top > innerHeight || r.right < 0 || r.left > innerWidth) continue;
      const s = getComputedStyle(el);
      if (s.visibility === "hidden" || s.display === "none" || faded(el)) continue;
      const fg = parse(s.color);
      if (!fg) continue;
      const bg = bgOf(el);
      const L1 = lum(over(fg, bg)), L2 = lum(bg);
      const ratio = (Math.max(L1, L2) + 0.05) / (Math.min(L1, L2) + 0.05);
      const big = parseFloat(s.fontSize) >= 24 || (parseFloat(s.fontSize) >= 18.66 && Number(s.fontWeight) >= 700);
      if (ratio < (big ? 3 : 4.5)) {
        const key = label(el) + "|" + s.color;
        if (seen.has(key)) continue;
        seen.add(key);
        low.push(`${ratio.toFixed(2)}  ${s.color} pe rgb(${bg.r | 0},${bg.g | 0},${bg.b | 0})  ${label(el)}  «${own.slice(0, 40)}»`);
      }
    }
    const light = [];
    if (dark) {
      for (const el of document.querySelectorAll("body *")) {
        const r = el.getBoundingClientRect();
        if (r.width < 24 || r.height < 24 || r.bottom < 0 || r.top > innerHeight) continue;
        const c = parse(getComputedStyle(el).backgroundColor);
        if (c && c.a > 0.5 && lum(c) > 0.5 && !el.closest('[data-keep-light]')) light.push(`${label(el)} ${getComputedStyle(el).backgroundColor} ${r.width | 0}×${r.height | 0}`);
      }
    }
    return { low, light: [...new Set(light)] };
  }, dark);
}

const report = [];
async function capture(page, name, width) {
  await page.waitForTimeout(700);
  await page.screenshot({ path: `${OUT}/${name}-${width}.png` });
  const m = await measure(page, THEME === "intunecat");
  if (m.low.length || m.light.length || page.problems.length) {
    report.push(`\n## ${name} @${width}`);
    for (const l of m.low) report.push(`  contrast ${l}`);
    for (const l of m.light) report.push(`  deschis  ${l}`);
    for (const p of page.problems) report.push(`  problemă ${p}`);
  }
  page.problems.length = 0;
}

const browser = await launch();
for (const width of [1440, 375]) {
  const page = await newPage(browser, { width, height: width === 375 ? 740 : 900 });
  await page.addInitScript((t) => { try { localStorage.setItem("eco360.tema", t); } catch {} }, THEME);
  for (const [route, name] of PUBLIC) {
    await page.goto(BASE + route, { waitUntil: "networkidle" });
    await capture(page, name, width);
  }
  await login(page, "admin");
  for (const [route, name] of SCREENS) {
    await page.goto(BASE + route, { waitUntil: "networkidle" });
    await capture(page, name, width);
  }
  // Dialogul de mișcare nouă, meniul de pe rând, paleta, meniul contului.
  await page.goto(BASE + `/generare?luna=${AN}&nou=1`, { waitUntil: "networkidle" });
  await capture(page, "dialog-miscare", width);
  await page.keyboard.press("Escape");
  await page.goto(BASE + `/generare?luna=${AN}`, { waitUntil: "networkidle" });
  const more = page.locator('main button[aria-label*="acțiuni" i]:visible').first();
  if (await more.count()) { await more.click(); await capture(page, "meniu-rand", width); await page.keyboard.press("Escape"); }
  if (width === 1440) {
    await page.keyboard.press("Control+k");
    await capture(page, "paleta", width);
    await page.keyboard.press("Escape");
    await page.click('button[aria-label="Meniul contului"]');
    await capture(page, "meniu-cont", width);
    await page.keyboard.press("Escape");
    await page.keyboard.press("[");
    await capture(page, "panou-strans", width);
    await page.keyboard.press("[");
  } else {
    await page.click('button[aria-label="Deschide meniul de navigație"]');
    await capture(page, "sertar", width);
  }
  await page.context().close();

  const admin = await newPage(browser, { width, height: width === 375 ? 740 : 900 });
  await admin.addInitScript((t) => { try { localStorage.setItem("eco360.tema", t); } catch {} }, THEME);
  await login(admin, "platform");
  for (const [route, name] of PLATFORM) {
    await admin.goto(BASE + route, { waitUntil: "networkidle" });
    await capture(admin, name, width);
  }
  await admin.context().close();
}
await browser.close();
fs.writeFileSync(`${OUT}/raport.txt`, report.join("\n") + "\n");
console.log(`gata: ${OUT}, ${report.filter((l) => l.startsWith("\n##")).length} ecrane cu note (raport.txt)`);
