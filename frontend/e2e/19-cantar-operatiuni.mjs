// Proba 19: ecranul „Cântar” — operațiunile depozitului (D1.15), peste D1.4–D1.10.
//
// Nu scrie nimic în bază: deschide formularul, tastează în el ca să vadă ce calculează singur, și
// îl închide cu Escape. Ce apără: intrarea de meniu apare doar la firmele cu registrul art. 48;
// Setările își păstrează o tastă când Cântarul împinge meniul peste cele zece cifre; ecranul are
// cele două direcții și banda reținerilor; formularul calculează neto din brut − tara și arată cât
// se reține din plată; tabelul încape la 1440px, iar pagina nu se lățește la 375px.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
await page.waitForTimeout(900);

// ---------------------------------------------------------------- MENIUL
const menu = await page.evaluate(() => {
  const aside = document.getElementById("navigatie-principala");
  const links = [...aside.querySelectorAll("nav a")];
  const cantar = links.find((a) => a.getAttribute("href") === "/cantar");
  const settings = links.find((a) => a.getAttribute("href") === "/setari");
  const keyOf = (a) => a?.querySelector("kbd")?.textContent.trim() ?? null;
  return {
    hasCantar: Boolean(cantar),
    label: cantar?.textContent.trim() ?? "",
    afterIesiri: links.findIndex((a) => a.getAttribute("href") === "/cantar") ===
      links.findIndex((a) => a.getAttribute("href") === "/iesiri") + 1,
    settingsKey: keyOf(settings),
  };
});
check("firma cu depozit are „Cântar” în meniu", menu.hasCantar && /Cântar/.test(menu.label), menu.label);
check("stă imediat după Ieșiri", menu.afterIesiri);
// Cifrele ajung fix pentru zece intrări. De când „Evidențe" a fost scos (18.09.2026), meniul firmei
// cu depozit are exact zece — deci Setările sunt iar pe „0", nu pe litera de rezervă.
check("Setările au ultima cifră, nu litera de rezervă", menu.settingsKey === "0", String(menu.settingsKey));

// ---------------------------------------------------------------- ECRANUL
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const screen = await page.evaluate(() => {
  const tabs = [...document.querySelectorAll('[role="tab"]')].map((t) => t.textContent.trim());
  const cols = [...document.querySelectorAll("table thead th")].map((t) => t.textContent.trim());
  const table = document.querySelector("table");
  const strip = document.querySelector("main section dl, main section p");
  return {
    title: document.querySelector("h1")?.textContent.trim() ?? "",
    tabs,
    cols,
    overflow: table ? table.scrollWidth - table.clientWidth : 0,
    strip: strip?.textContent.trim().replace(/\s+/g, " ") ?? "",
    bodyOverflow: document.body.scrollWidth - window.innerWidth,
  };
});
check("titlul e „Cântar”", screen.title === "Cântar", screen.title);
check("cele două direcții sunt taburi", screen.tabs.join("|") === "Intrări|Ieșiri", screen.tabs.join(" · "));
check("coloanele listei", ["Nr.", "Data", "De la", "Sortimente", "Cantitate (kg)", "Valoare", "Stare"].every((c) => screen.cols.some((x) => x === c)), screen.cols.join(" | "));
check("tabelul încape în 1440px", screen.overflow <= 0, `${screen.overflow}px peste`);
check("pagina nu se lățește la 1440px", screen.bodyOverflow <= 0, `${screen.bodyOverflow}px`);
// Banda reținerilor o vede adminul, care vede și prețurile; conținutul ei depinde de lună.
check("banda reținerilor spune ceva despre luna asta", screen.strip.length > 0, screen.strip.slice(0, 80));
await shot(page, "19-cantar");

// ---------------------------------------------------------------- REGISTRUL LUNII (D1.14)
// O citire: descarcă registrul intrărilor și ieșirilor pe luna aleasă. Coloanele le apără DepotRegisterIT;
// aici se probează că butonul e pe ecran, că pleacă luna din filtru și că vine un xlsx, nu o eroare.
// Ecranul pornește pe luna curentă (`currentMonth()`).
const now = new Date();
const monthValue = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
const [registru] = await Promise.all([
  page.waitForEvent("download", { timeout: 15000 }).catch(() => null),
  page.click('button:has-text("Registrul lunii")'),
]);
const expectedName = `registru-intrari-iesiri-${monthValue}.xlsx`;
check("„Registrul lunii” descarcă luna din filtru", registru?.suggestedFilename() === expectedName,
  `${registru?.suggestedFilename()} (așteptat ${expectedName})`);
if (registru) {
  const fs = await import("node:fs/promises");
  const bytes = await fs.readFile(await registru.path());
  check("fișierul e un xlsx (zip), nu o pagină de eroare", bytes.length > 1000 && bytes.subarray(0, 2).toString() === "PK",
    `${bytes.length} octeți`);
}

// ---------------------------------------------------------------- FORMULARUL
await page.click('button:has-text("Intrare nouă")');
await page.waitForTimeout(900);
const sections = await page.$$eval('div[role="dialog"] section h3, div[role="dialog"] section h2', (h) =>
  h.map((x) => x.textContent.trim())
);
check("formularul are cele patru secțiuni", ["Cine și când", "Transport", "Cântarul", "Plata"].every((s) => sections.includes(s)), sections.join(" | "));

// Neto se calculează din brut − tara, iar câmpul lui se blochează: cifra vine din cântărire.
await page.fill('div[role="dialog"] input[id^="wo-g-"]', "1200");
await page.fill('div[role="dialog"] input[id^="wo-t-"]', "200");
await page.waitForTimeout(400);
const net = await page.$eval('div[role="dialog"] input[id^="wo-n-"]', (i) => ({ value: i.value, disabled: i.disabled }));
check("neto = brut − tara, și nu se scrie de mână", net.value === "1000" && net.disabled, JSON.stringify(net));

// Cât se reține din plată se vede înainte de finalizare, cu cotele venite de la server.
await page.fill('div[role="dialog"] input[id^="wo-p-"]', "20");
await page.waitForTimeout(400);
const money = await page.$eval('div[role="dialog"] dl', (dl) => dl.textContent.replace(/\s+/g, " "));
check("valoarea liniei intră în total", /20\.000,00 lei/.test(money), money.slice(0, 120));
check("cei 2% AFM se scad din plată", /Fondul pentru mediu · 2%.*400,00 lei/.test(money), money.slice(0, 200));
check("rămâne de plătit cât s-a promis", /19\.600,00 lei/.test(money), money.slice(-80));
await shot(page, "19-cantar-formular");
await page.keyboard.press("Escape");
await page.waitForTimeout(500);

// ---------------------------------------------------------------- TELEFON
const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await phone.waitForTimeout(800);
const narrow = await phone.evaluate(() => ({
  overflow: document.body.scrollWidth - window.innerWidth,
  title: document.querySelector("h1")?.textContent.trim() ?? "",
}));
check("ecranul nu se lățește la 375px", narrow.overflow <= 0, `${narrow.overflow}px`);
check("titlul se vede și pe telefon", narrow.title === "Cântar", narrow.title);
await shot(phone, "19-cantar-telefon");

for (const p of [page, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}

await browser.close();
console.log(fails === 0 ? "\n✓ Proba 19 trece." : `\n✗ Proba 19: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
