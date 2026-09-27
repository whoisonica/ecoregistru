// Proba 55: rapoartele fixe ale depozitului (D4.7, 27.09.2026) — Cântar → Rapoarte.
//
// Ce apără: panoul are „Rapoarte” sub „Cântar” și duce la `/cantar?tab=rapoarte`; adminul vede toate cele unsprezece
// rapoarte, fiecare se descarcă (xlsx, iar fișa de stoc, 2% AFM, impozitul și borderourile și în PDF) cu numele pe slug
// și perioadă; fișa partenerului cere întâi partenerul; o perioadă de peste un an e spusă și oprește descărcarea;
// operatorul vede doar cele șapte fără bani și fără CNP; 1440×900 fără derulare, 375px fără lățire.
//
// ⚠️ Lasă în urmă o intrare finalizată „Proba 55” pe primul depozit activ (ca rapoartele să nu fie goale) și sortimentul
// „Carton P55 <număr>”, dezactivat.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
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

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");

// O intrare finalizată, cu preț, ca rapoartele să aibă ce arăta.
const depot = ((await api(page, "GET", "/api/v1/work-points")).json ?? []).find((w) => w.active);
const partner = ((await api(page, "GET", "/api/v1/partners")).json ?? []).find((p) => p.active);
// Fără `q`, lista de coduri n-are 15 01 01: se caută (capcana din proba 54).
const code = ((await api(page, "GET", "/api/v1/waste-codes?q=" + encodeURIComponent("15 01 01"))).json ?? [])
  .find((c) => c.code === "15 01 01");
const article = (await api(page, "POST", "/api/v1/waste-articles",
  { name: `Carton P55 ${Date.now() % 100000}`, wasteCodeId: code.id, metal: false, forbiddenFromIndividuals: false })).json;
const op = (await api(page, "POST", "/api/v1/weighing-operations",
  { type: "IN", workPointId: depot.id, date: today, partnerId: partner.id, notes: "Proba 55" })).json;
await api(page, "PUT", `/api/v1/weighing-operations/${op.id}/lines`, {
  grossKg: null, tareKg: null,
  lines: [{ articleId: article.id, grossKg: 1500, tareKg: 500, netKg: null, finalKg: 950, unitPrice: 0.5, operationCode: null, notes: null }],
});
const finalized = await api(page, "POST", `/api/v1/weighing-operations/${op.id}/finalize`, {});
check("intrarea de probă e finalizată", finalized.status === 200, `${finalized.status}`);

// Din panou: „Rapoarte” stă sub „Cântar”.
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.locator('nav a:has-text("Rapoarte"), aside a:has-text("Rapoarte")').first().click();
await page.waitForTimeout(700);
check("panoul duce la ?tab=rapoarte", page.url().includes("tab=rapoarte"), page.url());
const cards = await page.locator("[data-report]").count();
check("adminul vede cele unsprezece rapoarte", cards === 11, `${cards}`);
// Derulează `main#continut` (Layout.tsx), nu documentul: `documentElement` ar da mereu 0.
const height = await page.evaluate(() => {
  const main = document.querySelector("#continut");
  return main.scrollHeight - main.clientHeight;
});
check("1440×900 fără derulare", height <= 0, `${height}px`);
await shot(page, "55-rapoarte");

// Fiecare descărcare.
const expected = [
  ["registru", "xlsx"], ["jurnal-cantar", "xlsx"], ["documente", "xlsx"], ["anulate", "xlsx"],
  ["fisa-stoc", "xlsx"], ["fisa-stoc", "pdf"], ["transferuri", "xlsx"], ["afm", "xlsx"], ["afm", "pdf"],
  ["impozit", "xlsx"], ["impozit", "pdf"], ["numerar", "xlsx"], ["persoane-fizice", "xlsx"],
  ["persoane-fizice", "pdf"], ["partener", "xlsx"],
];
const partnerRow = page.locator('[data-report="partener"]');
check("fișa partenerului e oprită fără partener",
  await partnerRow.locator('button:has-text("xlsx")').isDisabled());
await partnerRow.locator("select").selectOption(partner.id);
// Chrome oprește a unsprezecea descărcare automată de pe aceeași încărcare de pagină (a căzut mereu a 11-a, oricare ar fi
// fost raportul, și cu ordinea inversată): pagina se reîncarcă la fiecare șapte.
let downloads = 0;
for (const [slug, format] of expected) {
  if (downloads > 0 && downloads % 7 === 0) {
    await page.reload({ waitUntil: "networkidle" });
    await page.waitForTimeout(500);
    await partnerRow.locator("select").selectOption(partner.id);
  }
  downloads++;
  const button = page.locator(`[data-report="${slug}"] button:has-text("${format === "pdf" ? "PDF" : "xlsx"}")`);
  const [download] = await Promise.all([page.waitForEvent("download", { timeout: 15000 }).catch(() => null), button.click()]);
  const name = download?.suggestedFilename() ?? "";
  check(`${slug}.${format} se descarcă`, name.startsWith(slug + "-") && name.endsWith("." + format), name || "nimic");
}

// Perioada de peste un an e spusă și oprește tastele.
await page.fill("#dr-from", "2024-01-01");
await page.fill("#dr-to", "2026-12-31");
await page.waitForTimeout(300);
check("peste un an: mesajul apare", (await page.locator('[role="alert"]').count()) > 0);
check("peste un an: descărcarea e oprită",
  await page.locator('[data-report="jurnal-cantar"] button:has-text("xlsx")').isDisabled());

// Operatorul: doar cele fără bani și fără CNP.
const operator = await newPage(browser, { width: 1440, height: 900 });
await login(operator, "operator");
await operator.goto(BASE + "/cantar?tab=rapoarte", { waitUntil: "networkidle" });
await operator.waitForTimeout(700);
const seen = await operator.locator("[data-report]").evaluateAll((els) => els.map((e) => e.getAttribute("data-report")));
check("operatorul vede șapte rapoarte", seen.length === 7, seen.join(", "));
check("operatorul nu vede banii și CNP-urile",
  !seen.some((s) => ["afm", "impozit", "numerar", "persoane-fizice"].includes(s)), seen.join(", "));

const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/cantar?tab=rapoarte", { waitUntil: "networkidle" });
await phone.waitForTimeout(700);
const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu lățește pagina la 375px", overflow <= 0, `${overflow}px`);
await shot(phone, "55-rapoarte-telefon");

await api(page, "DELETE", `/api/v1/waste-articles/${article.id}`);
for (const p of [page, operator, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 55 trece." : `\n✗ Proba 55: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
