// Proba 49: stocul (F3, D3.1–D3.2) — tabul „Stoc” din „Cântar” și avertismentul de stoc negativ.
//
// Ce apără: pe un depozit nou, o intrare finalizată de 1.000 kg apare în „Stoc” cu 1.000; o ieșire în lucru de 300 kg
// e „Angajat” și scade „Disponibil” la 700; finalizarea unei ieșiri de 2.000 kg (peste stoc) nu e oprită, dar spune
// că stocul a ieșit negativ, iar rândul apare cu „Negativ” și la filtrul „Doar de corectat”; ieri stocul era gol;
// 1440 și 375 fără lățire. Cifrele de pe ecran se compară cu `/api/v1/stock`.
//
// ⚠️ Lasă în urmă depozitul „Proba 49 <număr>” (dezactivat), două operațiuni finalizate și una anulată.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const DEPOT = `Proba 49 ${Date.now() % 100000}`;
const today = (() => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
})();
const yesterday = (() => {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
})();
const api = (page, method, url, body) =>
  page.evaluate(
    async ({ method, url, body }) => {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem("eco_token") },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await res.text();
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    { method, url, body }
  );
const tableText = (page) => page.$eval("tbody", (b) => b.textContent.replace(/\s+/g, " "));

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const depot = (await api(page, "POST", "/api/v1/work-points", { name: DEPOT, address: "Str. Probei 49" })).json;
let articles = ((await api(page, "GET", "/api/v1/waste-articles")).json ?? []).filter((a) => a.active);
if (articles.length === 0) {
  const codes = (await api(page, "GET", "/api/v1/waste-codes")).json ?? [];
  const code = codes.find((c) => c.code === "15 01 01") ?? codes.find((c) => !c.hazardous);
  await api(page, "POST", "/api/v1/waste-articles", { name: "Carton Proba 49", wasteCodeId: code.id, metal: false, forbiddenFromIndividuals: false });
  articles = ((await api(page, "GET", "/api/v1/waste-articles")).json ?? []).filter((a) => a.active);
}
const article = articles[0];
const partners = (await api(page, "GET", "/api/v1/partners")).json ?? [];
const partnerList = Array.isArray(partners) ? partners : partners.content ?? [];
const partner = partnerList.find((p) => p.active) ?? null;
const operation = async (type, kg, code) => {
  const op = (await api(page, "POST", "/api/v1/weighing-operations",
    { type, workPointId: depot.id, date: today, partnerId: partner?.id ?? null })).json;
  await api(page, "PUT", `/api/v1/weighing-operations/${op.id}/lines`, {
    grossKg: null, tareKg: null,
    lines: [{ articleId: article.id, grossKg: null, tareKg: null, netKg: kg, finalKg: null, unitPrice: null, operationCode: code, notes: null }],
  });
  return op.id;
};
const inId = await operation("IN", 1000, null);
await api(page, "POST", `/api/v1/weighing-operations/${inId}/finalize`);
const pending = await operation("OUT", 300, "R3");

await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.click('[role="tab"]:has-text("Stoc")');
await page.waitForTimeout(600);
await page.selectOption("#st-depot", depot.id);
await page.waitForTimeout(900);
let text = await tableText(page);
const api1 = (await api(page, "GET", `/api/v1/stock?workPointId=${depot.id}&date=${today}`)).json;
check("serverul dă 1.000 în stoc și 300 angajat", api1.rows[0]?.stockKg === 1000 && api1.rows[0]?.committedKg === 300,
  JSON.stringify(api1.rows[0]));
check("ecranul arată stocul, angajatul și disponibilul", /1\.000/.test(text) && /300/.test(text) && /700/.test(text), text);
check("nu există acțiune principală pe „Stoc”", !(await page.$('button:has-text("Intrare nouă")')));
await shot(page, "49-stoc");

// Stoc negativ: o ieșire peste stoc se finalizează, dar spune.
await api(page, "POST", `/api/v1/weighing-operations/${pending}/cancel`, { reason: "Proba 49" });
const tooMuch = await operation("OUT", 2000, "R3");
const finalized = (await api(page, "POST", `/api/v1/weighing-operations/${tooMuch}/finalize`)).json;
check("ieșirea peste stoc nu e oprită", finalized.status === "FINALIZED", finalized.status);
check("dar răspunsul spune că stocul a ieșit negativ", finalized.stockWarnings?.[0]?.stockKg === -1000,
  JSON.stringify(finalized.stockWarnings));
await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.click('[role="tab"]:has-text("Stoc")');
await page.waitForTimeout(500);
await page.selectOption("#st-depot", depot.id);
await page.waitForTimeout(900);
text = await tableText(page);
check("rândul are „Negativ”", /Negativ/.test(text) && /-1\.000/.test(text), text);
check("banda spune câte rânduri sunt de corectat", Boolean(await page.$('[role="status"]:has-text("de corectat")')));
await page.click('[role="radiogroup"] >> text="Doar de corectat"');
await page.waitForTimeout(400);
check("filtrul „Doar de corectat” îl păstrează", /Negativ/.test(await tableText(page)));

await page.fill("#st-date", yesterday);
await page.waitForTimeout(900);
check("ieri stocul era gol", /Nimic în stoc/.test(await tableText(page)), await tableText(page));
const width = await page.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 1440px", width <= 0, `${width}px`);

const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await phone.waitForTimeout(600);
await phone.click('[role="tab"]:has-text("Stoc")');
await phone.waitForTimeout(700);
const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("tabul nu lățește pagina la 375px", overflow <= 0, `${overflow}px`);
await shot(phone, "49-stoc-telefon");

await api(page, "DELETE", `/api/v1/work-points/${depot.id}`);
for (const p of [page, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 49 trece." : `\n✗ Proba 49: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
