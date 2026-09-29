// Proba 59: cantitatea în kilograme nu mai primește zecimale (30.09.2026).
//
// Proprietarul: „fieldul din Adauga deseuri care are cantitatea deseului te lasa sa pui cu ,000”. Rubrica avea
// `step="0.001"` oriunde. Acum, în kilograme, 12,5 e oprit sub rubrică („În kilograme, fără zecimale.”), 12 trece;
// în tone, 0,25 trece (250 kg). Proba nu salvează nimic: citește doar eroarea rubricii după „Salvează”.
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

await page.goto(BASE + `/generare?luna=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click('button:has-text("Deșeuri proprii")');
await page.waitForTimeout(600);

async function qtyError(value, unit) {
  await page.locator(`label:has(input[name="mv-unit"][value="${unit}"])`).click();
  await page.fill("#mv-qty", value);
  await page.click('button[type="submit"][form="movement-form"]');
  await page.waitForTimeout(400);
  return (await page.locator("#mv-qty-err").textContent().catch(() => null))?.trim() ?? "";
}

check("12,5 kg: oprit sub rubrică", (await qtyError("12.5", "KG")) === "În kilograme, fără zecimale.");
check("12 kg: fără eroare pe cantitate", (await qtyError("12", "KG")) === "");
check("0,25 t: fără eroare pe cantitate", (await qtyError("0.25", "TONS")) === "");
check("0,0005 t: mai fin decât un kilogram, oprit", /cel mult trei zecimale/.test(await qtyError("0.0005", "TONS")));
check("dialogul rămâne deschis (nimic salvat)", await page.locator("#mv-qty").isVisible());

console.log(fails === 0 ? "\n59: toate verificările au trecut" : `\n59: ${fails} verificări au căzut`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
