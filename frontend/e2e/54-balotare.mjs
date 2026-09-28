// Proba 54: balotarea (F5, 27.09.2026) — operatorul de cântar scrie doar câți baloți a făcut.
//
// Ce apără: în Setări → Sortimente, „Carton balotat P54” se face din „Carton vrac P54” cu 380 kg/balot și rândul spune
// „din … · 380 kg/balot”; pe /cantar tabul „Balotare” are „Balotare nouă” (tasta N); fișa arată „12 × 380 kg = 4.560 kg din
// … în …” înainte de salvare și nota TRAT; după salvare rândul e „Finalizată”, „12 baloți”, 4.560 kg, iar stocul depozitului are
// 440 kg vrac și 4.560 kg balotat; operatorul de cântar salvează și el o balotare, dar n-are „Anulează operațiunea”, iar
// „Operator”-ul de birou primește 403 (28.09.2026); adminul anulează cu
// motiv și stocul revine; 1440×900 fără derulare și 375 fără lățire.
//
// ⚠️ Lasă în urmă depozitul „Proba 54 <număr>” (dezactivat), sortimentele „Carton vrac/balotat P54 <număr>” (dezactivate),
// o intrare finalizată și două balotări (una anulată).
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const stamp = Date.now() % 100000;
const DEPOT = `Proba 54 ${stamp}`;
const LOOSE = `Carton vrac P54 ${stamp}`;
const BALED = `Carton balotat P54 ${stamp}`;
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const today = iso(new Date());
const api = (page, method, url, body) =>
  page.evaluate(
    async ({ method, url, body }) => {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem("eco_token") },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = (res.headers.get("content-type") ?? "").includes("json") ? await res.text() : "";
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    { method, url, body }
  );
const top = (page) => page.locator('div[role="dialog"]').last();
const stockOf = async (page, depotId, name) => {
  const s = (await api(page, "GET", `/api/v1/stock?workPointId=${depotId}&date=${today}`)).json;
  return s?.rows?.find((r) => r.articleName === name)?.stockKg ?? 0;
};

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const depot = (await api(page, "POST", "/api/v1/work-points", { name: DEPOT, address: "Str. Probei 54" })).json;
// Fără `q`, lista de coduri e scurtă și n-are 15 01 01: se caută.
const codes = (await api(page, "GET", "/api/v1/waste-codes?q=" + encodeURIComponent("15 01 01"))).json ?? [];
const code = codes.find((c) => c.code === "15 01 01");
const loose = (await api(page, "POST", "/api/v1/waste-articles",
  { name: LOOSE, wasteCodeId: code.id, metal: false, forbiddenFromIndividuals: false })).json;

// Sortimentul balotat, din formularul de Setări.
await page.goto(BASE + "/setari/sortimente", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.click('button:has-text("Adaugă sortiment")');
await page.waitForTimeout(400);
await page.fill("#wa-name", BALED);
await page.click("#wa-code");
await page.keyboard.type("15 01 01");
await page.waitForTimeout(900);
await page.locator('[role="option"]').filter({ hasText: "15 01 01" }).first().click();
await page.waitForTimeout(300);
await page.selectOption("#wa-bale-source", loose.id);
await page.fill("#wa-bale-weight", "380");
await top(page).locator('button:has-text("Salvează")').click();
await page.waitForTimeout(900);
// Căutarea apare abia peste un număr de rânduri; rândul se ia direct.
const row = ((await page.locator("tbody tr", { hasText: BALED }).first().textContent()) ?? "").replace(/\s+/g, " ");
check("sortimentul balotat spune din ce se face", row.includes(`din ${LOOSE} · 380 kg/balot`), row.slice(0, 160));
await shot(page, "54-sortiment-balotat");

// Stoc de pornire: o intrare de 5.000 kg vrac.
const partners = (await api(page, "GET", "/api/v1/partners")).json ?? [];
const partner = (Array.isArray(partners) ? partners : partners.content ?? []).find((p) => p.active) ?? null;
const inOp = (await api(page, "POST", "/api/v1/weighing-operations",
  { type: "IN", workPointId: depot.id, date: today, partnerId: partner?.id ?? null })).json;
await api(page, "PUT", `/api/v1/weighing-operations/${inOp.id}/lines`, {
  grossKg: null, tareKg: null,
  lines: [{ articleId: loose.id, grossKg: null, tareKg: null, netKg: 5000, finalKg: null, unitPrice: null, operationCode: null, notes: null }],
});
await api(page, "POST", `/api/v1/weighing-operations/${inOp.id}/finalize`);

// Fișa de balotare.
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.click('[role="tab"]:has-text("Balotare")');
await page.waitForTimeout(700);
check("butonul principal e „Balotare nouă”", (await page.locator('button:has-text("Balotare nouă")').count()) === 1);
await page.keyboard.press("n");
await page.waitForTimeout(600);
check("tasta N deschide fișa", /Fișă de balotare/.test((await top(page).textContent()) ?? ""));
const depotSelect = page.locator("#baling-depot");
if ((await depotSelect.count()) === 1) await depotSelect.selectOption(depot.id);
await top(page).locator(`label:has-text("${BALED}")`).click();
await page.fill("#baling-count", "12");
await page.waitForTimeout(300);
const computed = (await page.textContent('[data-testid="baling-computed"]')) ?? "";
check("fișa arată calculul înainte de salvare", computed.includes(`12 × 380 kg = 4.560 kg din ${LOOSE} în ${BALED}`), computed);
check("fișa spune de TRAT", /TRAT, cap\. 8/.test((await top(page).textContent()) ?? ""));
await shot(page, "54-fisa-balotare");
await top(page).locator('button:has-text("Salvează balotarea")').click();
await page.waitForTimeout(1000);
const list = (await page.textContent("tbody"))?.replace(/\s+/g, " ") ?? "";
check("rândul: depozitul, din → în, 12 baloți, 4.560 kg, finalizată",
  list.includes(DEPOT) && list.includes(`${LOOSE} → ${BALED}`) && list.includes("12 baloți") && list.includes("4.560")
    && list.includes("Finalizată"), list.slice(0, 200));
check("stocul vrac scade la 440 kg", (await stockOf(page, depot.id, LOOSE)) === 440);
check("stocul balotat e 4.560 kg", (await stockOf(page, depot.id, BALED)) === 4560);
const height = await page.evaluate(() => document.documentElement.scrollHeight - window.innerHeight);
check("tabul nu derulează la 1440×900", height <= 0, `${height}px`);
await shot(page, "54-balotare-lista");

// Operatorul de cântar salvează și el, dar nu anulează; cel de birou nu balotează deloc (28.09.2026).
const baledId = (await api(page, "GET", "/api/v1/waste-articles")).json.find((a) => a.name === BALED).id;
const office = await newPage(browser, { width: 1440, height: 900 });
await login(office, "operator");
const byOffice = await api(office, "POST", "/api/v1/weighing-operations/balings",
  { workPointId: depot.id, date: today, articleId: baledId, baleCount: 1, notes: null });
check("operatorul de birou primește 403 la balotare", byOffice.status === 403, `${byOffice.status}`);
office.problems = office.problems.filter((p) => !p.includes("403"));
const operator = await newPage(browser, { width: 1440, height: 900 });
await login(operator, "cantar");
const byOperator = await api(operator, "POST", "/api/v1/weighing-operations/balings",
  { workPointId: depot.id, date: today, articleId: baledId, baleCount: 1, notes: null });
check("operatorul de cântar salvează o balotare", byOperator.status === 200 && byOperator.json.status === "FINALIZED", `${byOperator.status}`);
await operator.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await operator.waitForTimeout(600);
await operator.click('[role="tab"]:has-text("Balotare")');
await operator.waitForTimeout(700);
await operator.click(`tbody tr:has-text("${DEPOT}") >> nth=0 >> button:has-text("Deschide")`);
await operator.waitForTimeout(600);
check("operatorul de cântar n-are „Anulează operațiunea”", (await top(operator).locator('button:has-text("Anulează operațiunea")').count()) === 0);

// Adminul anulează cu motiv: stocul revine.
await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.click('[role="tab"]:has-text("Balotare")');
await page.waitForTimeout(700);
await page.click(`tbody tr:has-text("12 baloți") >> button:has-text("Deschide")`);
await page.waitForTimeout(600);
await top(page).locator('button:has-text("Anulează operațiunea")').click();
await page.waitForTimeout(300);
await page.fill("#baling-cancel-reason", "au fost 11 baloți");
await top(page).locator('button:has-text("Anulează operațiunea")').click();
await page.waitForTimeout(1000);
check("după anulare vrac-ul revine la 4.620 kg (5.000 − 1 balot)", (await stockOf(page, depot.id, LOOSE)) === 4620);

const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await phone.waitForTimeout(600);
await phone.click('[role="tab"]:has-text("Balotare")');
await phone.waitForTimeout(700);
const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("tabul nu lățește pagina la 375px", overflow <= 0, `${overflow}px`);
await shot(phone, "54-balotare-telefon");

const all = (await api(page, "GET", "/api/v1/waste-articles")).json ?? [];
for (const a of all.filter((x) => x.name === BALED || x.name === LOOSE)) await api(page, "DELETE", `/api/v1/waste-articles/${a.id}`);
await api(page, "DELETE", `/api/v1/work-points/${depot.id}`);
for (const p of [page, office, operator, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 54 trece." : `\n✗ Proba 54: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
