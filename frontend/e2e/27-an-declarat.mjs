// Proba 27: anul deja declarat (evaluarea din 17.09.2026).
//
// Cu termenul de 15 martie al anului următor bifat, anul e declarat. (1) Salvarea unei mișcări din el
// întreabă întâi „Anul X e deja declarat” — „Anulează” nu salvează nimic, „Salvează oricum” salvează.
// (2) Ștergerea spune același lucru în dialogul ei. (3) Negativa: cu termenul redeschis, salvarea trece
// direct, fără întrebare.
//
// Creează o mișcare de probă în anul trecut față de termen și o șterge; lasă termenul cum l-a găsit.
import { launch, newPage, login, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const api = (path, opts = {}) =>
  page.evaluate(
    async ([path, opts]) => {
      const r = await fetch(path, {
        ...opts,
        headers: { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" },
      });
      return r.status === 204 ? null : r.json();
    },
    [path, opts]
  );

const AN = new Date().getFullYear();
const DOC = "PROBA27-" + Date.now().toString().slice(-6);

// Resturile unei rulări întrerupte.
for (const m of (await api("/api/v1/movements?search=PROBA27-&size=50")).content ?? []) {
  if (m.documentReference?.startsWith("PROBA27-")) await api(`/api/v1/movements/${m.id}`, { method: "DELETE" });
}

const sim = (await api(`/api/v1/deadlines?year=${AN + 1}`)).find((d) => d.reportType === "SIM_ANNUAL");
check(`există termenul de 15 martie ${AN + 1}`, Boolean(sim));
const wasDone = sim?.status === "DONE";
if (sim && !wasDone) await api(`/api/v1/deadlines/${sim.id}/complete`, { method: "POST", body: "{}" });

const workPoint = (await api("/api/v1/work-points")).find((w) => w.active);
const code = (await api("/api/v1/waste-codes?search=15%2001%2001"))[0];
const created = await api("/api/v1/movements", {
  method: "POST",
  body: JSON.stringify({
    workPointId: workPoint.id, date: `${AN}-07-20`, wasteCodeId: code.id, quantity: 7.777, unit: "KG",
    operation: "RECOVERED", register: "ANEXA_1", physicalState: "SOLID", wasteDestination: "P",
    operationCode: "R3", documentReference: DOC,
  }),
});
check("mișcarea de probă s-a creat", Boolean(created?.id), JSON.stringify(created).slice(0, 120));

async function openEdit() {
  await page.goto(BASE + `/generare?luna=${AN}&miscare=${created.id}`, { waitUntil: "networkidle" });
  await page.waitForSelector('button[type="submit"][form="movement-form"]', { timeout: 10000 });
  await page.waitForTimeout(600);
}
const declaredDialog = () => page.locator('[role="dialog"]', { hasText: `Anul ${AN} e deja declarat` });

// ------------------------------------------------------------ (1) SALVAREA ÎNTREABĂ
await openEdit();
await page.click('button[type="submit"][form="movement-form"]');
await declaredDialog().waitFor({ timeout: 8000 }).catch(() => {});
check("salvarea întreabă „Anul … e deja declarat”", (await declaredDialog().count()) > 0);
check("… și spune ziua bifării", /bifat pe \d\d\.\d\d\.\d{4}/.test(await declaredDialog().innerText().catch(() => "")));
await declaredDialog().getByRole("button", { name: "Anulează" }).click({ timeout: 3000 }).catch(() => {});
await page.waitForTimeout(800);
check("„Anulează” nu salvează", !(await page.getByText("Mișcare actualizată").count()));
await page.click('button[type="submit"][form="movement-form"]', { timeout: 3000 }).catch(() => {});
await declaredDialog().getByRole("button", { name: "Salvează oricum" }).click({ timeout: 3000 }).catch(() => {});
await page.getByText("Mișcare actualizată").first().waitFor({ timeout: 8000 }).catch(() => {});
check("„Salvează oricum” salvează", (await page.getByText("Mișcare actualizată").count()) > 0);

// ------------------------------------------------------------ (2) ȘTERGEREA SPUNE LA FEL
await page.goto(BASE + `/generare?luna=${AN}`, { waitUntil: "networkidle" });
const row = page.locator("tr", { hasText: "7,777" }).or(page.locator("tr", { hasText: "7.777" })).first();
await row.getByRole("button", { name: /Mai multe acțiuni|Acțiuni/ }).click({ timeout: 5000 }).catch(() => {});
await page.getByRole("menuitem", { name: /Șterge/ }).click({ timeout: 5000 }).catch(() => {});
const deleteDialog = page.locator('[role="dialog"]', { hasText: "Ștergi mișcarea?" });
await deleteDialog.waitFor({ timeout: 8000 }).catch(() => {});
check("dialogul de ștergere spune că anul e declarat", /e deja declarat/.test(await deleteDialog.innerText().catch(() => "")));
await deleteDialog.getByRole("button", { name: "Anulează" }).click({ timeout: 3000 }).catch(() => {});

// ------------------------------------------------------------ (3) NEGATIVA: TERMENUL REDESCHIS
await api(`/api/v1/deadlines/${sim.id}/reopen`, { method: "POST" });
await openEdit();
await page.click('button[type="submit"][form="movement-form"]');
await page.waitForTimeout(1500);
check("fără anul declarat, salvarea nu întreabă", (await declaredDialog().count()) === 0);
check("… și salvează direct", (await page.getByText("Mișcare actualizată").count()) > 0);

await api(`/api/v1/movements/${created.id}`, { method: "DELETE" });
if (wasDone) await api(`/api/v1/deadlines/${sim.id}/complete`, { method: "POST", body: "{}" });
if (page.problems.length) console.log("  probleme:", page.problems);

console.log("");
if (fails === 0) console.log("✓ proba 27 trece.");
else console.log(`✗ proba 27: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
