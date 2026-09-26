// Proba 44: cântarele depozitului (D2.3) — secțiunea „Cântare” din Setări, istoricul și cântarul la finalizare.
//
// Ce apără: un cântar pus în funcțiune acum doi ani, fără verificare, apare „Verificare expirată”; o verificare
// ADMIS îl face „Verificat” cu data de pe buletin (un an dacă nu e scrisă); o reparație de azi îl face „Reparat, de
// reverificat”; la cântar, cântarul depozitului se alege dintr-o tastă cu starea lui alături; finalizarea cu un
// cântar nelegal cere motiv, iar motivul și starea de atunci se văd pe operațiunea finalizată; vizualizatorul
// vede cântarele fără butoane; nimic nu se lățește la 1440 și 375.
//
// ⚠️ Lasă în urmă cântarul „Proba 44 <număr>” (trecut „Scos din uz”: cu el s-a cântărit, deci nu se șterge) și
// două intrări anulate la final, cu motivul „Proba 44”.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const iso = (days) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
const NAME = `Proba 44 ${Date.now() % 100000}`;
const section = "section#cantare";
const rowSel = `${section} tbody tr:has-text("${NAME}")`;
const rowText = (page) =>
  page.evaluate(({ section, name }) => {
    const tr = [...document.querySelectorAll(`${section} tbody tr`)].find((r) => r.textContent.includes(name));
    return tr?.textContent.replace(/\s+/g, " ") ?? "";
  }, { section, name: NAME });

const api = (page, method, url, body) =>
  page.evaluate(
    async ({ method, url, body }) => {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem("eco_token") },
        body: body ? JSON.stringify(body) : undefined,
      });
      return { status: res.status, json: res.status === 204 ? null : await res.json() };
    },
    { method, url, body }
  );

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
await page.goto(BASE + "/setari", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
check("Setările au cardul „Cântare”", Boolean(await page.$('a[href="/setari/cantare"]')));
await page.goto(BASE + "/setari/cantare", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
check("pagina are secțiunea „Cântare”", (await page.$(section)) !== null);

// ---------------------------------------------------------------- ADAUGĂ
await page.click(`${section} button:has-text("Adaugă cântar")`);
await page.waitForTimeout(500);
await page.fill("#sc-name", NAME);
const depotId = await page.$eval("#sc-depot", (s) => [...s.options].find((o) => o.value)?.value ?? "");
await page.selectOption("#sc-depot", depotId);
await page.fill("#sc-kind", "Pod basculă 60 t");
await page.fill("#sc-serial", "SN-44");
await page.click('div[role="radiogroup"] >> text="III"');
await page.fill("#sc-division", "20");
await page.fill("#sc-commissioned", iso(-800));
await page.fill("#sc-brml", iso(-795));
await page.fill("#sc-brml-ref", "BRML 44");
await shot(page, "44-cantar-formular");
await page.click('div[role="dialog"] button[type="submit"]');
await page.waitForTimeout(1200);
let text = await rowText(page);
check("cântarul apare în listă", text.includes(NAME), text.slice(0, 120));
check("fără verificare, după primul an: „Verificare expirată”", /Verificare expirată/.test(text), text);

// ---------------------------------------------------------------- ISTORICUL
await page.click(`${rowSel} button:has-text("Istoric")`);
await page.waitForTimeout(600);
await page.fill("#sc-ev-date", iso(-340));
await page.fill("#sc-ev-bulletin", "B-44");
await page.fill("#sc-ev-lab", "Laborator probă");
await page.click('div[role="dialog"] button:has-text("Adaugă în istoric")');
await page.waitForTimeout(1000);
const dialogText = () =>
  page.evaluate(() => document.querySelector('div[role="dialog"]')?.textContent.replace(/\s+/g, " ") ?? "");
let hist = await dialogText();
check("verificarea ADMIS apare în istoric, cu buletinul", /B-44/.test(hist) && /ADMIS/.test(hist), hist.slice(0, 200));
check("valabil un an de la verificare, deci „Verificat”", /Verificat · valabil până la/.test(hist), hist.slice(0, 200));
const histOverflow = await page.$eval('div[role="dialog"] table', (t) => t.scrollWidth - t.clientWidth);
check("istoricul încape în dialog", histOverflow <= 0, `${histOverflow}px peste`);
await shot(page, "44-cantar-istoric");

await page.click('div[role="dialog"] div[role="radiogroup"] >> text="Reparație"');
await page.waitForTimeout(200);
check("la reparație nu se cer buletin și rezultat", (await page.$("#sc-ev-bulletin")) === null);
await page.fill("#sc-ev-date", iso(0));
await page.fill("#sc-ev-notes", "celula 3 schimbată");
await page.click('div[role="dialog"] button:has-text("Adaugă în istoric")');
await page.waitForTimeout(1000);
hist = await dialogText();
check("după reparație: „Reparat, de reverificat”", /Reparat, de reverificat/.test(hist), hist.slice(0, 200));
await page.keyboard.press("Escape");
await page.waitForTimeout(500);
text = await rowText(page);
check("starea din listă se schimbă odată cu istoricul", /Reparat, de reverificat/.test(text), text);
const overflow = await page.$eval(`${section} table`, (t) => t.scrollWidth - t.clientWidth);
check("tabelul cântarelor încape în 1440px", overflow <= 0, `${overflow}px peste`);
await shot(page, "44-cantare");

// ---------------------------------------------------------------- LA CÂNTAR
const scaleId = (await api(page, "GET", "/api/v1/scales")).json.find((s) => s.name === NAME).id;
const partner = (await api(page, "GET", "/api/v1/partners")).json.find((p) => p.active);
let article = (await api(page, "GET", "/api/v1/waste-articles")).json.find((a) => a.active && !a.metal);
if (!article) {
  // Pe o bază nouă nu există sortimente; unul de hârtie ajunge (fără metal, deci fără borderou cu CNP).
  const code = (await api(page, "GET", "/api/v1/waste-codes?q=15 01 01")).json[0];
  article = (await api(page, "POST", "/api/v1/waste-articles", {
    name: "Proba 44 carton", wasteCodeId: code.id, metal: false, forbiddenFromIndividuals: false,
  })).json;
}
const created = await api(page, "POST", "/api/v1/weighing-operations", {
  type: "IN", workPointId: depotId, date: iso(0), partnerId: partner.id, naturalPersonId: null,
  driverName: null, vehicleRegistration: null, orderNumber: null, paymentMethod: null, receiptNumber: null,
  ownHousehold: null, notes: "Proba 44", scaleId,
});
check("operațiunea cu cântarul reparat se poate salva (în lucru)", created.status === 200, `HTTP ${created.status}`);
check("serverul spune starea cântarului înainte de finalizare", created.json?.scaleState === "REPAIRED", created.json?.scaleState);
const opId = created.json.id;
await api(page, "PUT", `/api/v1/weighing-operations/${opId}/lines`, {
  grossKg: null, tareKg: null,
  lines: [{ articleId: article.id, grossKg: null, tareKg: null, netKg: 120, finalKg: null, unitPrice: null, operationCode: null, notes: null }],
});

await page.goto(BASE + `/cantar?op=${opId}`, { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
const scaleBlock = await page.evaluate(() =>
  document.getElementById("wo-scale-label")?.parentElement.textContent.replace(/\s+/g, " ") ?? "");
check("la cântar, cântarul e ales, cu starea lui alături", scaleBlock.includes(NAME) && /Reparat/.test(scaleBlock), scaleBlock);
const pressed = await page.$$eval('input[name="wo-scale"]:checked', (inputs) =>
  inputs.map((i) => i.closest("label").textContent.trim()));
check("tasta cântarului e apăsată", pressed.some((t) => t.includes(NAME)), pressed.join(", "));
await shot(page, "44-cantar-operatiune");

await page.click('div[role="dialog"] button:has-text("Finalizează")');
await page.waitForTimeout(400);
// Confirmarea se deschide peste formular: butonul ei e ultimul „Finalizează” din pagină.
await page.locator('button:text-is("Finalizează")').last().click();
await page.waitForTimeout(1200);
const reasonDialog = await page.evaluate(() => document.body.textContent.includes("Cântarul nu era legal la cântărire"));
check("finalizarea cere motiv pentru cântarul nelegal", reasonDialog);
const confirmDisabled = await page.$eval('button:has-text("Finalizează cu motiv")', (b) => b.disabled);
check("fără motiv, butonul e stins", confirmDisabled);
await page.fill("#wo-scale-reason", "Proba 44: reverificarea e programată mâine");
await shot(page, "44-cantar-motiv");
await page.click('button:has-text("Finalizează cu motiv")');
await page.waitForTimeout(1500);
const after = (await api(page, "GET", `/api/v1/weighing-operations/${opId}`)).json;
check("operațiunea e finalizată", after.status === "FINALIZED", after.status);
check("motivul și starea de atunci rămân pe operațiune",
  after.scaleOverrideReason?.startsWith("Proba 44") && after.scaleState === "REPAIRED",
  `${after.scaleState} · ${after.scaleOverrideReason}`);

await page.goto(BASE + `/cantar?op=${opId}`, { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
const readOnly = await page.evaluate(() => document.querySelector('div[role="dialog"]')?.textContent.replace(/\s+/g, " ") ?? "");
check("pe operațiunea finalizată se citesc starea la cântărire și motivul",
  /Starea la cântărire:/.test(readOnly) && /Motivul confirmării: Proba 44/.test(readOnly), readOnly.slice(0, 300));
await shot(page, "44-cantar-finalizata");
await page.keyboard.press("Escape");

// ---------------------------------------------------------------- OPERATORUL LA „DOAR ADMINISTRATORUL”
// Decizia din 26.09.2026: operatorul plătește omul la cântar, deci vede totalul de plată, nu prețul pe kg.
const before = (await api(page, "GET", "/api/v1/companies/current")).json.priceVisibility;
const priced = (await api(page, "POST", "/api/v1/weighing-operations", {
  type: "IN", workPointId: depotId, date: iso(0), partnerId: partner.id, naturalPersonId: null,
  driverName: null, vehicleRegistration: null, orderNumber: null, paymentMethod: null, receiptNumber: null,
  ownHousehold: null, notes: "Proba 44 plata", scaleId: null,
})).json;
await api(page, "PUT", `/api/v1/weighing-operations/${priced.id}/lines`, {
  grossKg: null, tareKg: null,
  lines: [{ articleId: article.id, grossKg: null, tareKg: null, netKg: 1000, finalKg: null, unitPrice: 0.5, operationCode: null, notes: null }],
});
await api(page, "PUT", "/api/v1/companies/current/price-visibility", { priceVisibility: "ADMIN_ONLY" });
const operator = await newPage(browser, { width: 1440, height: 900 });
await login(operator, "operator");
await operator.goto(BASE + `/cantar?op=${priced.id}`, { waitUntil: "networkidle" });
await operator.waitForTimeout(1200);
const opView = await operator.evaluate(() => ({
  text: document.querySelector('div[role="dialog"]')?.textContent.replace(/\s+/g, " ") ?? "",
  priceInputs: [...document.querySelectorAll('div[role="dialog"] label')].filter((l) => /Lei\/kg/.test(l.textContent)).length,
}));
check("operatorul vede „Rămâne de plătit” cu suma de la server", /Rămâne de plătit\??\s*490,00/.test(opView.text), opView.text.match(/Total valoare.{0,80}/)?.[0]);
check("operatorul nu vede prețul pe kg", opView.priceInputs === 0, `${opView.priceInputs} rubrici Lei/kg`);
await shot(operator, "44-operator-plata");
await api(page, "PUT", "/api/v1/companies/current/price-visibility", { priceVisibility: before });
await api(page, "POST", `/api/v1/weighing-operations/${priced.id}/cancel`, { reason: "Proba 44" });

// ---------------------------------------------------------------- VIZUALIZATORUL ȘI TELEFONUL
const viewer = await newPage(browser, { width: 375, height: 800 });
await login(viewer, "viewer");
await viewer.goto(BASE + "/setari/cantare", { waitUntil: "networkidle" });
await viewer.waitForTimeout(900);
const seen = await viewer.evaluate(({ section, name }) => ({
  row: document.querySelector(section)?.textContent.includes(name) ?? false,
  write: [...document.querySelectorAll(`${section} button`)].some((b) => /Adaugă cântar|Istoric|Editează/.test(b.textContent)),
  overflow: document.body.scrollWidth - window.innerWidth,
}), { section, name: NAME });
check("vizualizatorul vede cântarele", seen.row);
check("vizualizatorul n-are butoane de scriere", !seen.write);
check("Setările nu se lățesc la 375px", seen.overflow <= 0, `${seen.overflow}px`);
await shot(viewer, "44-cantare-telefon");

// ---------------------------------------------------------------- CURĂȚENIE
await api(page, "POST", `/api/v1/weighing-operations/${opId}/cancel`, { reason: "Proba 44" });
const scale = (await api(page, "GET", "/api/v1/scales")).json.find((s) => s.id === scaleId);
const retired = await api(page, "PUT", `/api/v1/scales/${scaleId}`, { ...scale, status: "OUT_OF_USE" });
const refused = await api(page, "DELETE", `/api/v1/scales/${scaleId}`);
check("un cântar cu cântăriri nu se șterge", refused.status === 422 || refused.status === 400 || refused.status === 409, `HTTP ${refused.status}`);
check("se trece „Scos din uz”", retired.json?.status === "OUT_OF_USE");
// Refuzul de mai sus e așteptat: nu e o problemă de rețea.
page.problems = page.problems.filter(
  (p) => !p.includes(`/api/v1/scales/${scaleId}`) && !p.includes("status of 400 (Bad Request)"));

for (const p of [page, viewer, operator]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}

await browser.close();
console.log(fails === 0 ? "\n✓ Proba 44 trece." : `\n✗ Proba 44: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
