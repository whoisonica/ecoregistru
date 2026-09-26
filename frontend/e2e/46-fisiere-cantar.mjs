// Proba 46: fișierele cântarului (V70) — dovada BRML și buletinul verificării, în dialogul „Istoric”.
//
// Ce apără: dialogul arată rubrica „Dovada declarării la BRML” cu „Atașează”; buletinul are loc doar pe rândul
// unei verificări, nu pe reparație; după urcare apare numele fișierului, care se deschide într-un tab nou prin
// sesiune (nu printr-un link public); „Scoate fișierul” îl scoate. Nimic nu se lățește la 1440.
//
// ⚠️ Local nu există chei Cloudinary, deci urcarea, conținutul și scoaterea sunt interceptate (`page.route`) cu
// răspunsul pe care l-ar da serverul; regulile serverului sunt în `ScaleDocumentIT`. Lasă în urmă cântarul
// „Proba 46 <număr>” (fără cântăriri, se șterge la final).
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
const NAME = `Proba 46 ${Date.now() % 100000}`;
const section = "section#cantare";
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

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const depot = (await api(page, "GET", "/api/v1/work-points")).json.find((wp) => wp.active);
let scale = (await api(page, "POST", "/api/v1/scales", {
  workPointId: depot.id, name: NAME, commissionedOn: iso(-800), brmlDeclaredOn: iso(-795), brmlReference: "BRML 46",
})).json;
await api(page, "POST", `/api/v1/scales/${scale.id}/events`, {
  kind: "VERIFICATION", date: iso(-30), admitted: true, bulletinNumber: "B-46", laboratory: "Laborator probă",
});
scale = (await api(page, "POST", `/api/v1/scales/${scale.id}/events`, { kind: "REPAIR", date: iso(-10) })).json;

// Serverul fără Cloudinary: răspunsul pe care l-ar da, cu fișierul pus pe locul cerut.
const DOC = { id: "00000000-0000-4000-8000-000000000046", fileName: "declaratie-brml.pdf", contentType: "application/pdf" };
let current = scale;
await page.route(`**/api/v1/scales/${scale.id}/brml-proof`, (route) => {
  current = { ...current, brmlProof: DOC };
  route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(current) });
});
await page.route(`**/api/v1/scales/${scale.id}/documents/**`, (route) => {
  if (route.request().method() === "DELETE") {
    current = { ...current, brmlProof: null };
    return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(current) });
  }
  route.fulfill({ status: 200, contentType: "application/pdf", body: "%PDF-1.4 proba 46" });
});
// Lista trebuie să arate ce ar ține serverul după urcare.
await page.route("**/api/v1/scales", async (route) => {
  if (route.request().method() !== "GET") return route.continue();
  const res = await route.fetch();
  const list = (await res.json()).map((s) => (s.id === current.id ? { ...s, brmlProof: current.brmlProof } : s));
  route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(list) });
});

await page.goto(BASE + "/setari/cantare", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.click(`${section} tbody tr:has-text("${NAME}") button:has-text("Istoric")`);
await page.waitForTimeout(600);
const dialog = 'div[role="dialog"]';
const text = await page.$eval(dialog, (d) => d.textContent.replace(/\s+/g, " "));
check("dialogul are rubrica „Dovada declarării la BRML”", text.includes("Dovada declarării la BRML"));
const rows = await page.$$eval(`${dialog} tbody tr`, (trs) => trs.map((tr) => ({
  text: tr.textContent.replace(/\s+/g, " "),
  attach: [...tr.querySelectorAll("button")].some((b) => b.textContent.includes("Atașează")),
})));
const verRow = rows.find((r) => r.text.includes("B-46"));
const repRow = rows.find((r) => r.text.includes("Reparație"));
check("verificarea are „Atașează” pentru buletin", verRow?.attach === true, verRow?.text);
check("reparația n-are buletin", repRow?.attach === false, repRow?.text);

// Urcarea dovezii BRML.
const [chooser] = await Promise.all([
  page.waitForEvent("filechooser"),
  page.click(`${dialog} div:has(> span:text("Dovada declarării la BRML")) button:has-text("Atașează")`),
]);
await chooser.setFiles({ name: "declaratie-brml.pdf", mimeType: "application/pdf", buffer: Buffer.from("%PDF-1.4") });
await page.waitForTimeout(1200);
const named = await page.$(`${dialog} button:has-text("declaratie-brml.pdf")`);
check("după urcare apare numele fișierului", named !== null);
await shot(page, "46-fisiere-cantar");

const [tab] = await Promise.all([page.context().waitForEvent("page"), named?.click()]);
await tab.waitForURL(/^blob:/, { timeout: 8000 }).catch(() => {});
check("fișierul se deschide într-un tab, din blob (prin sesiune)", tab.url().startsWith("blob:"), tab.url());
await tab.close();

await page.click(`${dialog} button[aria-label="Scoate fișierul"]`);
await page.waitForTimeout(1000);
check("„Scoate fișierul” îl scoate", (await page.$(`${dialog} button:has-text("declaratie-brml.pdf")`)) === null);
const overflow = await page.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 1440px", overflow <= 0, `${overflow}px`);

// ---------------------------------------------------------------- CURĂȚENIE
await page.unrouteAll({ behavior: "ignoreErrors" });
const removed = await api(page, "DELETE", `/api/v1/scales/${scale.id}`);
check("cântarul de probă se șterge", removed.status === 204, `HTTP ${removed.status}`);

if (page.problems.length > 0) {
  console.log("  FAIL consola/rețeaua");
  for (const problem of [...new Set(page.problems)]) console.log(`         ${problem}`);
  fails += page.problems.length;
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 46 trece." : `\n✗ Proba 46: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
