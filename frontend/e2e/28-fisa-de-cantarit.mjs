// Proba 28: butonul fișei când anul are cantități „de cântărit” (evaluarea din 17.09.2026).
//
// Butonul „Evidența gestiunii deșeurilor generate” deschidea un dialog în locul fișierului, cu un titlu
// care nu numea documentul — părea că butonul nu merge. (1) Nota apare pe Evidențe înainte de clic și
// numără liniile. (2) Dialogul poartă numele documentului, iar „Descarcă oricum” chiar deschide fișa în tab.
// (3) Dialogul nu mai scrie culori de mână (amber), doar tokenii „Cântar”.
//
// Firma demo are o predare de plastic pe 22 iulie cu „se cântărește la descărcare”. Nu lasă nimic în urmă.
import { launch, newPage, login, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

const AN = new Date().getFullYear();
await page.goto(BASE + `/generare?tab=total&luna=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(1200);

// ------------------------------------------------------------ (1) NOTA DE DINAINTE DE CLIC
const nota = page.locator('[data-testid="pending-weighing-note"]');
const textNota = (await nota.count()) ? await nota.innerText() : "";
check("nota „de cântărit” apare pe totalul anului", /așteaptă cântarul destinatarului/.test(textNota), textNota);
check("… cu numărul liniilor", /\d+ lini[ei]+? așteaptă/.test(textNota), textNota);
check("… și lângă ea, drumul spre rândurile care lipsesc", /Arată mișcările/.test(textNota), textNota);

// ------------------------------------------------------------ (2) DIALOGUL NUMEȘTE DOCUMENTUL
await page.getByRole("button", { name: "Evidența gestiunii deșeurilor generate" }).click();
const dialog = page.getByRole("dialog");
await dialog.waitFor({ timeout: 5000 }).catch(() => {});
const textDialog = (await dialog.count()) ? await dialog.innerText() : "";
check("dialogul are în titlu numele fișei", /Evidența gestiunii deșeurilor generate: lipsesc cantități/.test(textDialog), textDialog.slice(0, 80));
check("… și spune de unde se completează", /Adaugă cantitatea/.test(textDialog));

// ------------------------------------------------------------ (3) FĂRĂ CULORI DE MÂNĂ
const amber = await page.evaluate(() => document.querySelector('[role="dialog"]')?.innerHTML.includes("amber-") ?? true);
check("dialogul nu folosește amber-*, doar tokenii", !amber);

// Fișa se deschide într-un tab nou, ca PDF adus prin sesiune (cererea Andreei din 15.09), nu ca descărcare.
const [tab] = await Promise.all([
  page.context().waitForEvent("page", { timeout: 20000 }).catch(() => null),
  dialog.getByRole("button", { name: "Descarcă oricum" }).click({ timeout: 3000 }).catch(() => {}),
]);
let adresa = "(niciun tab)";
if (tab) {
  await tab.waitForFunction(() => location.href.startsWith("blob:"), null, { timeout: 20000 }).catch(() => {});
  adresa = tab.url();
  await tab.close();
}
check("„Descarcă oricum” chiar deschide fișa (tab blob:)", adresa.startsWith("blob:"), adresa.slice(0, 40));

if (page.problems.length) console.log("  probleme:", page.problems);
console.log("");
if (fails === 0) console.log("✓ proba 28 trece.");
else console.log(`✗ proba 28: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
