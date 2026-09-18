// Proba 39: „Acasă”, varianta A (18.09.2026).
//
// Proprietarul a ales-o din cinci machete, apoi a scos din ea „Dacă vine controlul azi” și „Ultimele
// predări”. Au rămas anul pe luni, deșeurile anului și termenele pe douăsprezece luni, cu „Adaugă
// în calendar”. Ce se poate strica aici sunt **cifrele**, nu cutiile, deci proba le socotește a doua
// oară din API (evidența anului) și le compară cu ecranul — nu caută doar titlurile. Nu scrie nimic.
import fs from "node:fs";
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 1000 });
let fails = 0;
function check(name, ok, detail = "") {
  if (ok) console.log(`  OK   ${name}${detail ? " — " + detail : ""}`);
  else {
    console.log(`  FAIL ${name}${detail ? " — " + detail : ""}`);
    fails++;
  }
}
const digits = (s) => (s ?? "").replace(/\D/g, "");

await login(page, "admin");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForSelector('[data-testid="home-year"] [data-month], [data-testid="home-year"] p', { timeout: 15000 });
await page.waitForTimeout(600);

const YEAR = new Date().getFullYear();
const MONTH = new Date().getMonth() + 1;
const api = await page.evaluate(async (year) => {
  const get = async (url) => {
    const r = await fetch(url, { headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") } });
    return r.ok ? r.json() : null;
  };
  return {
    evidences: await get(`/api/v1/evidences?year=${year}`),
  };
}, YEAR);

// ------------------------------------------------------ 1. ANTETUL
const antet = await page.evaluate(() => ({
  h1: document.querySelector("h1")?.textContent.trim() ?? "",
  // Ce stă în `main` înaintea titlului: de pe 18.09.2026, nimic — ziua și data au fost scoase.
  inainte: (() => {
    const h1 = document.querySelector("main h1");
    return [...document.querySelectorAll("main p, main span")]
      .filter((el) => h1 && el.compareDocumentPosition(h1) & Node.DOCUMENT_POSITION_FOLLOWING && !el.contains(h1))
      .map((el) => el.textContent.trim())
      .filter((x) => /\d{4}/.test(x));
  })(),
}));
check("titlul e salutul zilei", /^Bună (dimineața|ziua|seara)$/.test(antet.h1), antet.h1);
check("deasupra lui nu mai stă ziua și data", antet.inainte.length === 0, antet.inainte.join(" | "));
// Tot de pe 18.09.2026: antetul n-are buton de adăugare. Adăugarea e pe ecranul de mișcări și pe N / I / E.
const adaugari = await page.$$eval("main a", (a) => a.map((x) => x.textContent.trim()).filter((x) => /^(Deșeuri proprii|Adaugă deșeuri|Intrare)$/.test(x)));
check("antetul n-are buton de adăugare", adaugari.length === 0, adaugari.join(" | "));

// ------------------------------------------------------ 2. ANUL PE LUNI, SOCOTIT A DOUA OARĂ
const kg = Array(12).fill(0);
for (const r of api.evidences ?? []) kg[r.month - 1] += r.totalGenerated;
const total = kg.reduce((a, b) => a + b, 0);
const first = kg.findIndex((v) => v > 0);
const gaps = [];
if (first >= 0) for (let i = first + 1; i < MONTH - 1; i++) if (kg[i] === 0) gaps.push(i + 1);
const an = await page.evaluate(() => {
  const card = document.querySelector('[data-testid="home-year"]');
  return {
    bare: card.querySelectorAll("[data-month]").length,
    total: card.querySelector("h2")?.parentElement?.nextElementSibling?.textContent.trim() ?? "",
    goluri: [...card.querySelectorAll("[data-gap]")].map((b) => Number(b.getAttribute("data-month"))),
  };
});
check("douăsprezece luni", an.bare === 12, String(an.bare));
check("totalul anului e suma evidenței din API", digits(an.total) === digits(String(Math.round(total))),
  `${an.total} față de ${Math.round(total)} kg`);
check("lunile goale sunt exact cele socotite din API", JSON.stringify(an.goluri) === JSON.stringify(gaps),
  `${JSON.stringify(an.goluri)} față de ${JSON.stringify(gaps)}`);

// ------------------------------------------------------ 3. DEȘEURILE ANULUI
const byCode = new Map();
for (const r of api.evidences ?? []) byCode.set(r.wasteCode, (byCode.get(r.wasteCode) ?? 0) + r.totalGenerated);
const top = [...byCode.entries()].filter(([, v]) => v > 0).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
const coduri = await page.evaluate(() =>
  [...document.querySelectorAll('[data-testid="home-codes"] li')].map((li) => li.querySelector(".font-mono")?.textContent.trim())
);
check("cel mult cinci coduri", coduri.length === Math.min(5, top.length), `${coduri.length} din ${top.length}`);
check("în ordinea cantității din evidență", coduri.join("|") === top.slice(0, 5).map(([c]) => c).join("|"),
  coduri.join(" | "));

// ------------------------------------------------------ 4. TERMENELE ȘI FIȘIERUL .ICS
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForSelector('[data-testid="stat-deadlines"]', { timeout: 15000 });
const coloane = await page.evaluate(() => document.querySelectorAll('[data-testid="home-deadlines"] ol > li').length);
check("douăsprezece coloane de luni (pe desktop)", coloane === 12, String(coloane));
const buton = page.locator('[data-testid="home-deadlines"] button:has-text("Adaugă în calendar")');
if (await buton.isEnabled()) {
  const [dl] = await Promise.all([page.waitForEvent("download", { timeout: 10000 }), buton.click()]);
  const ics = fs.readFileSync(await dl.path(), "utf8");
  const evenimente = (ics.match(/BEGIN:VEVENT/g) ?? []).length;
  const zile = (ics.match(/DTSTART;VALUE=DATE:\d{8}/g) ?? []).length;
  check("fișierul se cheamă termene-wastehouse.ics", dl.suggestedFilename() === "termene-wastehouse.ics", dl.suggestedFilename());
  check("e un calendar cu câte o zi întreagă pe termen", ics.startsWith("BEGIN:VCALENDAR") && evenimente > 0 && zile === evenimente,
    `${evenimente} evenimente`);
} else {
  check("butonul de calendar e activ când sunt termene", false, "dezactivat");
}

// ------------------------------------------------------ 5. „+ ÎNCĂ N” NU INTRĂ ÎN BANDĂ
const mai = page.locator('[data-testid="next-action"] button[aria-expanded]');
if (await mai.count()) {
  const eticheta = (await mai.textContent()).trim();
  const n = Number(/\d+/.exec(eticheta)?.[0] ?? 0);
  await mai.click();
  const dupa = await page.evaluate(() => ({
    lista: document.querySelectorAll('[data-testid="more-actions"] li').length,
    banda: document.querySelector('[data-testid="next-action"]').textContent,
  }));
  check("„+ încă N” desface exact N lucruri", dupa.lista === n, `${dupa.lista} / ${n}`);
  check("iar banda tot unul numește", dupa.banda.split("Următoarea acțiune").length === 2);
} else {
  console.log("  —    un singur lucru deschis: „+ încă N” nu apare (corect)");
}
await shot(page, "39-acasa");
// Pagina derulează în `main`, nu în document. Un element absolut fără părinte poziționat (textul
// `sr-only` de la „Adaugă în calendar”) lungea documentul: la derulare urcau antetul și panoul, iar
// dedesubt rămânea alb (proprietarul, pe localhost, 18.09.2026).
const inalt = await page.evaluate(() => document.documentElement.scrollHeight - document.documentElement.clientHeight);
const scoase = await page.$('[data-testid="home-control"], [data-testid="home-recent"]');
check("„Dacă vine controlul azi” și „Ultimele predări” nu mai sunt pe pagină", scoase === null);
check("documentul nu e mai înalt decât fereastra", inalt <= 0, inalt + "px");

// ------------------------------------------------------ 6. TELEFONUL
await page.setViewportSize({ width: 375, height: 800 });
await page.waitForTimeout(700);
const lat = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
check("nu se derulează lateral la 375px", lat <= 0, lat + "px");
await shot(page, "39-acasa-telefon");

console.log("");
if (fails === 0) console.log("✓ proba 39 trece.");
else console.log(`✗ proba 39: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
