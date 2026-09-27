// Proba 52: borderoul și NIR-ul intrării de la o persoană fizică (F1, D1.17a) — dialogul operațiunii de pe „Cântar”.
//
// Ce apără: pe un depozit nou, o intrare de la o persoană fizică cu o linie de carton la 0,5 lei/kg și una la 0 lei.
// În lucru, pe o persoană fizică, sub preț scrie „0 = gratuit, se face NIR”. După finalizare dialogul are
// „Borderou nr. …” și „NIR nr. …”, iar fiecare își deschide PDF-ul într-un tab; 1440 și 375 fără lățire.
//
// ⚠️ Lasă în urmă depozitul „Proba 52 <număr>” (dezactivat), persoana „Proba 52 <număr>” (dezactivată) și intrarea,
// anulată; pe o bază fără sortimente își face „Carton Proba 52”.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now() % 100000;
const DEPOT = `Proba 52 ${RUN}`;
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
      const type = res.headers.get("content-type") ?? "";
      const text = type.includes("json") ? await res.text() : "";
      return { status: res.status, type, json: text ? JSON.parse(text) : null };
    },
    { method, url, body }
  );
const dialog = 'div[role="dialog"]';
const openOperation = async (page) => {
  await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await page.click(`tbody tr:has-text("${DEPOT}") button:has-text("Deschide")`);
  await page.waitForTimeout(900);
};
const opensATab = async (page, label) => {
  if (!(await page.$(`${dialog} button:has-text("${label}")`))) return false;
  const [tab] = await Promise.all([
    page.context().waitForEvent("page", { timeout: 8000 }).catch(() => null),
    page.click(`${dialog} button:has-text("${label}")`),
  ]);
  if (!tab) return false;
  await tab.waitForURL(/^blob:/, { timeout: 8000 }).catch(() => null);
  const ok = tab.url().startsWith("blob:");
  await tab.close();
  return ok;
};

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const depot = (await api(page, "POST", "/api/v1/work-points", { name: DEPOT, address: "Str. Probei 52" })).json;
let article = ((await api(page, "GET", "/api/v1/waste-articles")).json ?? []).find((a) => a.active && !a.metal);
if (!article) {
  const codes = (await api(page, "GET", "/api/v1/waste-codes")).json ?? [];
  const code = codes.find((c) => c.code === "15 01 01") ?? codes.find((c) => !c.hazardous);
  article = (await api(page, "POST", "/api/v1/waste-articles",
    { name: "Carton Proba 52", wasteCodeId: code.id, metal: false, forbiddenFromIndividuals: false })).json;
}
const person = (await api(page, "POST", "/api/v1/natural-persons", { name: `Proba 52 ${RUN}` })).json;
const op = (await api(page, "POST", "/api/v1/weighing-operations",
  { type: "IN", workPointId: depot.id, date: today, naturalPersonId: person.id })).json;
const line = (kg, price) =>
  ({ articleId: article.id, grossKg: null, tareKg: null, netKg: kg, finalKg: null, unitPrice: price, operationCode: null, notes: null });
await api(page, "PUT", `/api/v1/weighing-operations/${op.id}/lines`,
  { grossKg: null, tareKg: null, lines: [line(100, 0.5), line(40, 0)] });

// 1. În lucru: textul de sub preț.
await openOperation(page);
const draft = await page.$eval(dialog, (d) => d.textContent.replace(/\s+/g, " "));
check("sub preț: „0 = gratuit, se face NIR”", draft.includes("0 = gratuit, se face NIR"));
check("în lucru nu există „NIR nr.”", !draft.includes("NIR nr."));
await page.locator('[id^="wo-p-"]').last().scrollIntoViewIfNeeded();
await shot(page, "52-in-lucru");

// 2. Finalizată: amândouă documentele, fiecare în tabul lui.
const finalized = await api(page, "POST", `/api/v1/weighing-operations/${op.id}/finalize`);
check("finalizarea trece", finalized.status === 200, String(finalized.status));
check("răspunsul are ambele numere",
  finalized.json?.borderouNumber != null && finalized.json?.receptionNoteNumber != null,
  `${finalized.json?.borderouNumber} / ${finalized.json?.receptionNoteNumber}`);
await openOperation(page);
const done = await page.$eval(dialog, (d) => d.textContent.replace(/\s+/g, " "));
check("„Borderou nr. …” pe dialog", done.includes(`Borderou nr. ${finalized.json?.borderouNumber}`));
check("„NIR nr. …” pe dialog", done.includes(`NIR nr. ${finalized.json?.receptionNoteNumber}`));
await shot(page, "52-finalizata");
check("borderoul se deschide", await opensATab(page, "Borderou nr."));
check("NIR-ul se deschide", await opensATab(page, "NIR nr."));
const width = await page.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 1440px", width <= 0, `${width}px`);


// 3. Pe telefon, subsolul cu patru butoane nu lățește pagina.
const phone = await newPage(browser, { width: 375, height: 812 });
await login(phone, "admin");
await openOperation(phone);
check("„NIR nr. …” și la 375px", Boolean(await phone.$(`${dialog} button:has-text("NIR nr.")`)));
const phoneWidth = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 375px", phoneWidth <= 0, `${phoneWidth}px`);
await shot(phone, "52-finalizata-375");

await api(page, "POST", `/api/v1/weighing-operations/${op.id}/cancel`, { reason: "Proba 52" });
await api(page, "DELETE", `/api/v1/natural-persons/${person.id}`);
await api(page, "DELETE", `/api/v1/work-points/${depot.id}`);
for (const p of [page, phone]) {
  const problems = [...new Set(p.problems)];
  if (problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of problems) console.log(`         ${problem}`);
    fails += problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 52 trece." : `\n✗ Proba 52: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
