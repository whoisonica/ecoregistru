// Proba 18: panoul „Cântar” (15.09.2026), în forma „E3” din 18.09.2026 — firma pe plăcuța ei, căutarea
// ca iconiță, taburile sub intrarea deschisă; FĂRĂ afișajul lunii, tastele mari de adăugare, rândul
// „Caută oriunde” și orice „+” pe rânduri (toate scoase de proprietar); meniul cu cifre și indicatori, strângerea cu `[`, ecranele Intrări / Ieșiri separate, totalurile
// de deasupra listei și culoarea pubelei pe codul de deșeu.
//
// Firma demo e „Generator și colector”, deci vede toate cele trei ecrane de mișcări și toate cele
// trei taste (N · I · E). Nu scrie nimic în bază: deschide formulare și le închide.
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

// ---------------------------------------------------------------- PANOUL
const panel = await page.evaluate(() => {
  const aside = document.getElementById("navigatie-principala");
  const label = aside.querySelector('[data-testid="company-label"]')?.textContent ?? "";
  const keys = [...aside.querySelectorAll("nav kbd")].map((k) => k.textContent.trim());
  // Rândurile meniului, nu orice link din `nav`: acolo stau acum și taburile.
  const items = [...aside.querySelectorAll('nav a[data-nav="row"]')].map((a) => a.getAttribute("href"));
  // Orice link din panou care deschide un formular (`?nou=1`): din 18.09.2026 nu mai e niciunul.
  const adds = [...aside.querySelectorAll('a[href*="nou=1"]')].map((a) => a.getAttribute("href"));
  const icons = aside.querySelectorAll('nav a[data-nav="row"] svg').length;
  return {
    label, keys, items, adds, icons,
    gone: {
      lcd: Boolean(aside.querySelector('[data-testid="month-display"]')),
      actions: Boolean(aside.querySelector('[data-testid="panel-actions"]')),
      searchRow: /Caută oriunde/.test(aside.textContent),
    },
    search: aside.querySelector('[data-testid="panel-search"]')?.getAttribute("aria-label") ?? "",
    navTop: Math.round(aside.querySelector("nav").getBoundingClientRect().top),
    width: Math.round(aside.getBoundingClientRect().width),
  };
});
check("firma e etichetă: nume + CUI + tip", /Demo Reciclare/.test(panel.label) && /RO\d+/.test(panel.label), panel.label);
check("au plecat afișajul lunii, tastele mari și rândul „Caută oriunde”", !panel.gone.lcd && !panel.gone.actions && !panel.gone.searchRow, JSON.stringify(panel.gone));
check("căutarea e o iconiță cu nume", /Caută oriunde/.test(panel.search), panel.search);
check("din panou nu se adaugă nimic: niciun „+”, nicio tastă de adăugare", panel.adds.length === 0, panel.adds.join(" | "));
check("rândurile n-au iconiță cât panoul e lat", panel.icons === 0, String(panel.icons));
check("meniul începe sus, sub firmă", panel.navTop < 140, panel.navTop + "px");
// Cifrele merg în ordine, câte intrări are firma — 1…9, apoi 0 pentru a zecea. Firma asta are
// nouă de pe 18.09.2026, de când „Ambalaje" e tab în „Generare" și a ieșit din meniu; a zecea
// cifră se probează unde chiar există a zecea intrare, nu cerută pe de rost aici.
check("meniul are cifrele în ordine, câte intrări are",
  panel.keys.slice(0, 10).join("") === "1234567890".slice(0, panel.items.length),
  `${panel.keys.join("")} pe ${panel.items.length} intrări`);
check("Generare, Intrări, Ieșiri sunt intrări proprii", ["/generare", "/intrari", "/iesiri"].every((h) => panel.items.includes(h)), panel.items.join(" "));
check("Import și Abonament nu sunt în meniu (sunt în paletă și în Setări)", !panel.items.includes("/import"), panel.items.join(" "));
check("panoul are 262px", panel.width === 262, panel.width + "px");

// Indicatorii citesc cifre deja calculate: „?” numai când o sursă a căzut, niciodată „0”.
const indicators = await page.$$eval("#navigatie-principala nav a span.font-mono", (s) => s.map((x) => x.textContent.trim()).filter(Boolean));
check("niciun indicator nu scrie „0”", indicators.every((i) => !/^0 /.test(i)), indicators.join(" · "));
check("niciun indicator nu e „?” cu serverul sus", indicators.every((i) => i !== "?"), indicators.join(" · "));
await shot(page, "18-panou");

// ---------------------------------------------------------------- TABURILE SUB INTRAREA DESCHISĂ
const tabsOf = () => page.$$eval('#navigatie-principala nav a[data-nav="tab"]', (a) => a.map((x) => ({ text: x.textContent.trim(), on: x.getAttribute("aria-current") === "page" })));
check("pe Acasă niciun rând nu e desfăcut", (await tabsOf()).length === 0);
await page.click('#navigatie-principala nav a[data-nav="row"][href="/generare"]');
await page.waitForURL((u) => u.pathname === "/generare", { timeout: 5000 }).catch(() => {});
await page.waitForTimeout(600);
let subs = await tabsOf();
const pageTabs = await page.$$eval('[role="tablist"] [role="tab"]', (b) => b.map((x) => x.innerText.trim()));
check("Generare își arată taburile, aceleași ca pagina", subs.map((x) => x.text.replace(/\s*\d+ de cântărit$/, "")).join(" · ") === pageTabs.join(" · "), `${subs.map((x) => x.text).join(" · ")} vs ${pageTabs.join(" · ")}`);
check("primul e cel ales", subs[0]?.on && subs.slice(1).every((x) => !x.on), JSON.stringify(subs));
await page.goto(BASE + "/generare?luna=2026&tab=total", { waitUntil: "networkidle" });
await page.click('#navigatie-principala nav a[data-nav="tab"]:has-text("Ambalaje")');
await page.waitForTimeout(600);
const afterTab = new URL(page.url());
check("tabul din meniu schimbă doar `tab`, luna rămâne", afterTab.searchParams.get("tab") === "ambalaje" && afterTab.searchParams.get("luna") === "2026", afterTab.search);
subs = await tabsOf();
check("… și e cel aprins, în meniu și în pagină", subs.find((x) => x.on)?.text === "Ambalaje" && (await page.getAttribute('[role="tab"][aria-selected="true"]', "aria-selected")) === "true" && /Ambalaje/.test(await page.innerText('[role="tab"][aria-selected="true"]')), JSON.stringify(subs));
await shot(page, "18-panou-taburi");

await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(500);

// ---------------------------------------------------------------- TASTELE
await page.click("h1");
await page.keyboard.press("3");
await page.waitForURL((u) => u.pathname === "/intrari", { timeout: 5000 }).catch(() => {});
check("tasta 3 deschide Intrări", new URL(page.url()).pathname === "/intrari", page.url());
await page.waitForTimeout(600);
const inTitle = (await page.textContent("h1")) ?? "";
check("Intrări are titlul lui", /Intrări de deșeuri/.test(inTitle), inTitle.trim());
const totals = await page.$$eval('[data-testid="totals"] > div', (d) => d.map((x) => x.textContent.trim().replace(/\s+/g, " ")));
check("totalurile: Primit · De la firme · De la persoane fizice · De cântărit", totals.length === 4 && /Primit/i.test(totals[0]) && /persoane fizice/i.test(totals[2]), totals.join(" | "));

await page.keyboard.press("4");
await page.waitForURL((u) => u.pathname === "/iesiri", { timeout: 5000 }).catch(() => {});
check("tasta 4 deschide Ieșiri", new URL(page.url()).pathname === "/iesiri", page.url());
await page.waitForTimeout(600);
const outCols = await page.$$eval("table thead th", (th) => th.map((x) => x.textContent.trim()));
check("Ieșiri are coloanele Cod R/D și Către", outCols.some((c) => /Cod R\/D/i.test(c)) && outCols.some((c) => /Către/i.test(c)), outCols.join(" | "));
check("Ieșiri n-are coloana Secția", !outCols.some((c) => /Secția/i.test(c)), outCols.join(" | "));

// I și E deschid formularul potrivit, de oriunde.
await page.keyboard.press("1");
await page.waitForURL((u) => u.pathname === "/", { timeout: 5000 }).catch(() => {});
await page.waitForTimeout(400);
await page.click("h1");
await page.keyboard.press("i");
await page.waitForTimeout(1200);
const inDialog = (await page.textContent('div[role="dialog"][aria-modal="true"] h2').catch(() => "")) ?? "";
check("I deschide „Intrare de deșeuri”", /Intrare de deșeuri/.test(inDialog), inDialog.trim());
const inOperation = await page.$eval('div[role="dialog"] select#operation, div[role="dialog"] select[id*="operation"]', (s) => s.value).catch(() => null);
check("formularul de intrare pornește pe preluare", inOperation === "COLLECTED" || inOperation === null, String(inOperation));
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
await page.click("h1");
await page.keyboard.press("e");
await page.waitForTimeout(1200);
const outDialog = (await page.textContent('div[role="dialog"][aria-modal="true"] h2').catch(() => "")) ?? "";
check("E deschide „Ieșire de deșeuri”", /Ieșire de deșeuri/.test(outDialog), outDialog.trim());
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// `[` strânge panoul la o șină și ține minte.
await page.click("h1");
await page.keyboard.press("[");
await page.waitForTimeout(400);
const collapsed = await page.evaluate(() => {
  const aside = document.getElementById("navigatie-principala");
  return { w: Math.round(aside.getBoundingClientRect().width), flag: aside.dataset.collapsed, stored: (() => { try { return localStorage.getItem("wh.panel"); } catch { return null; } })() };
});
check("„[” strânge panoul la 64px și ține minte", collapsed.w === 64 && collapsed.flag === "true" && collapsed.stored === "collapsed", JSON.stringify(collapsed));
const rail = await page.evaluate(() => {
  const nav = document.querySelector("#navigatie-principala nav");
  return { icons: nav.querySelectorAll('a[data-nav="row"] svg').length, rows: nav.querySelectorAll('a[data-nav="row"]').length, extra: nav.querySelectorAll('a[data-nav="tab"]').length };
});
check("pe șină rândurile au iconiță și nimic altceva", rail.icons === rail.rows && rail.extra === 0, JSON.stringify(rail));
await shot(page, "18-panou-strans");
await page.keyboard.press("[");
await page.waitForTimeout(300);

// ---------------------------------------------------------------- CULOAREA PUBELEI
const AN = new Date().getFullYear();
await page.goto(BASE + `/generare?luna=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const bins = await page.$$eval("table tbody tr", (rows) =>
  rows.map((r) => {
    const cell = r.querySelector("td:nth-child(2)");
    const code = cell?.querySelector(".font-mono")?.textContent.trim() ?? "";
    const bin = cell?.querySelector("[data-bin]")?.dataset.bin ?? null;
    // Periculoasele poartă insigna „Periculos”; codul de pe ecran nu are steluța din catalog.
    const hazardous = /Periculos/.test(cell?.textContent ?? "");
    return { code, bin, hazardous };
  })
);
// 15 01 01 intră de la proba 26, care salvează o predare pe hârtie și carton (o șterge, dar o rulare întreruptă o lasă).
const expected = { "20 01 01": "paper", "15 01 01": "paper", "15 01 02": "plastic", "20 03 01": "residual", "15 01 07": "glass", "20 01 40": "plastic" };
const known = bins.filter((b) => b.code in expected);
check("proba a avut rânduri cu coduri cunoscute", known.length >= 3, `${known.length} din ${bins.length}`);
check("fiecare cod cunoscut are pubela lui", known.every((b) => b.bin === expected[b.code]), known.map((b) => `${b.code}=${b.bin}`).join(" · "));
const hazard = bins.filter((b) => b.hazardous);
check("proba a avut și un cod periculos", hazard.length >= 1, `${hazard.length}`);
check("periculoasele sunt roșii, oricare ar fi codul", hazard.every((b) => b.bin === "hazard"), hazard.map((b) => `${b.code}=${b.bin}`).join(" · "));
const unknown = bins.filter((b) => !(b.code in expected) && !b.hazardous);
check("un cod necunoscut n-are pătrățel", unknown.every((b) => b.bin === null), unknown.length ? unknown.map((b) => `${b.code}=${b.bin}`).join(" · ") : "niciun cod necunoscut pe pagină (verificare pe gol)");

// Adresa veche duce la Intrări.
await page.goto(BASE + "/intrari-iesiri", { waitUntil: "networkidle" });
await page.waitForTimeout(500);
check("/intrari-iesiri duce la /intrari", new URL(page.url()).pathname === "/intrari", page.url());

// ---------------------------------------------------------------- TELEFON
const phone = await newPage(browser, { width: 375, height: 740 });
await login(phone, "admin");
await phone.waitForTimeout(900);
const bar = await phone.evaluate(() => {
  const nav = [...document.querySelectorAll("nav")].find((n) => getComputedStyle(n).position === "fixed" && n.getBoundingClientRect().bottom >= window.innerHeight - 1);
  return nav ? [...nav.querySelectorAll("a, button")].map((x) => x.textContent.trim() || x.getAttribute("aria-label")) : null;
});
check("telefonul are bara de jos: Acasă · Generare · Adaugă · Termene · Mai mult", bar && bar.length === 5 && bar[0] === "Acasă" && bar[2] === "Adaugă", bar ? bar.join(" · ") : "lipsă");
const overflow = await phone.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
check("nimic nu iese lateral pe telefon", overflow <= 1, String(overflow));
await phone.click('button[aria-label][aria-controls="navigatie-principala"]');
await phone.waitForTimeout(500);
const drawer = await phone.evaluate(() => document.getElementById("navigatie-principala").getBoundingClientRect().left === 0);
check("„Mai mult” / meniul deschide sertarul", drawer);
await shot(phone, "18-telefon-sertar");
await phone.close();

console.log("");
console.log(fails === 0 ? "✓ proba 18 trece." : `✗ proba 18: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
