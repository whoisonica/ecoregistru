// Proba 50: pragurile și limitele de stoc (F3, D3.3–D3.4) — butonul „Praguri și limite” de pe tabul „Stoc”.
//
// Ce apără: pe un depozit nou cu 1.000 kg de un sortiment, adminul pune din dialog un prag maxim de 500 kg → rândul
// are „Peste maxim”; adaugă limita „Stocat pe amplasament, 0,5 t la un moment dat” → banda limitelor spune „1 / 0.5 t”
// și „Depășit”; o limită în m³ apare „fără comparație”; coloana „Vechime” arată „0 zile”; operatorul nu are butonul;
// 1440 fără lățire.
//
// ⚠️ Lasă în urmă depozitul „Proba 50 <număr>” (dezactivat) cu pragul, limitele și intrarea lui.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const DEPOT = `Proba 50 ${Date.now() % 100000}`;
const dialog = 'div[role="dialog"]';
const today = (() => {
  const d = new Date();
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
const openStock = async (page) => {
  await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await page.click('[role="tab"]:has-text("Stoc")');
  await page.waitForTimeout(500);
  await page.selectOption("#st-depot", { label: DEPOT });
  await page.waitForTimeout(900);
};

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const depot = (await api(page, "POST", "/api/v1/work-points", { name: DEPOT, address: "Str. Probei 50" })).json;
const article = ((await api(page, "GET", "/api/v1/waste-articles")).json ?? []).find((a) => a.active);
const partnerList = (await api(page, "GET", "/api/v1/partners")).json;
const partner = (Array.isArray(partnerList) ? partnerList : partnerList?.content ?? []).find((p) => p.active) ?? null;
const op = (await api(page, "POST", "/api/v1/weighing-operations",
  { type: "IN", workPointId: depot.id, date: today, partnerId: partner?.id ?? null })).json;
await api(page, "PUT", `/api/v1/weighing-operations/${op.id}/lines`, {
  grossKg: null, tareKg: null,
  lines: [{ articleId: article.id, grossKg: null, tareKg: null, netKg: 1000, finalKg: null, unitPrice: null, operationCode: null, notes: null }],
});
await api(page, "POST", `/api/v1/weighing-operations/${op.id}/finalize`);

await openStock(page);
check("coloana „Vechime” arată 0 zile", /0 zile/.test(await page.$eval("tbody", (b) => b.textContent)));
await page.click('button:has-text("Praguri și limite")');
await page.waitForTimeout(700);
await page.selectOption("#sts-article", article.id);
await page.fill("#sts-max", "500");
await page.click(`${dialog} button:has-text("Salvează pragul")`);
await page.waitForTimeout(800);
// Limita pe amplasament: 0,5 t la un moment dat (implicit „Stocat pe amplasament”, „t”, „la un moment dat”).
await page.fill("#stl-qty", "0.5");
await page.fill("#stl-note", "Proba 50, pct. 3");
await page.click(`${dialog} button:has-text("Adaugă limita")`);
await page.waitForTimeout(800);
// O limită în metri cubi: se arată, nu se compară.
await page.fill("#stl-qty", "40");
await page.click(`${dialog} [role="radiogroup"] >> text="m³"`);
await page.click(`${dialog} button:has-text("Adaugă limita")`);
await page.waitForTimeout(800);
await shot(page, "50-praguri-limite-dialog");
await page.click(`${dialog} button:has-text("Închide")`);
await page.waitForTimeout(900);

const row = await page.$eval("tbody", (b) => b.textContent.replace(/\s+/g, " "));
check("rândul are „Peste maxim”", /Peste maxim/.test(row), row);
const band = await page.$eval('section[aria-label="Limitele din autorizație"]', (s) => s.textContent.replace(/\s+/g, " "));
check("banda limitelor spune „Depășit” la stocul pe amplasament", /Stocat pe amplasament.*1 \/ 0\.5 t.*Depășit/.test(band), band);
check("limita în m³ e „fără comparație”", /m³.*fără comparație/.test(band), band);
const width = await page.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 1440px", width <= 0, `${width}px`);
await shot(page, "50-stoc-limite");

const operator = await newPage(browser, { width: 1440, height: 900 });
await login(operator, "operator");
await openStock(operator);
check("operatorul nu are „Praguri și limite”", !(await operator.$('button:has-text("Praguri și limite")')));

await api(page, "DELETE", `/api/v1/work-points/${depot.id}`);
for (const p of [page, operator]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 50 trece." : `\n✗ Proba 50: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
