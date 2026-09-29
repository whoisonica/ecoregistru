// Proba 58: predarea unui deșeu periculos la generator nu mai cere rubricile Anexei 3 (30.09.2026).
//
// Proprietarul: „daca adaugi deseu periculos te pune sa completezi anexa 3 transport desi nu trebuie sa o
// completezi si nici nu te lasa. anexa 2 pentru deseuri periculoase o primeste de la colector”. Pe un cont de
// generator, cu un cod periculos predat unui colector, blocul de transport păstrează ce scrie avizul (șoferul,
// mașina) și spune că formularul îl aduce colectorul — fără „Unitatea tipărită”, fără datele de încărcare și
// descărcare și fără caseta „Destinat:”. Pe un cod nepericulos, în același formular, rubricile Anexei 3 rămân.
// Salvată, predarea periculoasă oferă avizul, nu Anexa 3.
//
// ⚠️ Lasă în urmă o firmă „Proba 58 Periculos <număr>” cu un punct de lucru, un colector și o predare.
import { launch, newPage, login, shot, switchCompany, validCui, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-8);
const NAME = `Proba 58 Periculos ${RUN}`;
const AN = new Date().getFullYear();

await login(page, "platform");
const created = await page.evaluate(async ([name, cui]) => {
  const headers = { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" };
  const res = await fetch("/api/v1/companies", {
    method: "POST", headers,
    body: JSON.stringify({ name, cui, type: "GENERATOR", afmObligation: false, address: "Cluj-Napoca" }),
  });
  if (!res.ok) return "companie HTTP " + res.status;
  const id = (await res.json()).id;
  const tenant = { ...headers, "X-Tenant-Id": id };
  const wp = await fetch("/api/v1/work-points", { method: "POST", headers: tenant, body: JSON.stringify({ name: "Sediu P58", address: "Cluj" }) });
  if (!wp.ok) return "punct HTTP " + wp.status;
  const partner = await fetch("/api/v1/partners", {
    method: "POST", headers: tenant,
    body: JSON.stringify({ name: "Colector P58", type: "COLLECTOR", authorizationNumber: "AM-58", client: false, supplier: true, carrier: false }),
  });
  if (!partner.ok) return "partener HTTP " + partner.status + " " + (await partner.text());
  return id;
}, [NAME, validCui()]);
check("firma de generator, punctul și colectorul există", /^[0-9a-f-]{36}$/.test(String(created)), String(created));
await page.reload({ waitUntil: "networkidle" });
check("comutat pe firma nouă", Boolean(await switchCompany(page, new RegExp(RUN))));

async function pickCode(code) {
  await page.click("#mv-code");
  await page.keyboard.press("ControlOrMeta+a");
  await page.keyboard.type(code);
  await page.waitForTimeout(900);
  await page.locator('[role="listbox"] [role="option"]', { hasText: code }).first().click();
  await page.waitForTimeout(300);
}
const present = (sel) => page.locator(sel).count().then((n) => n > 0);

await page.goto(BASE + `/generare?luna=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click('button:has-text("Adaugă deșeuri")');
await page.waitForTimeout(600);
await pickCode("13 02 05");
await page.fill("#mv-qty", "58");
await page.locator('label:has(input[name="mv-fate"][value="RECOVERED"])').click();
await page.waitForTimeout(200);
await page.selectOption("#mv-code-rd", { index: 1 });
await page.locator('label:has(input[name="mv-destination"][value="Vr"])').click();
await page.locator('label:has(input[name="mv-state"][value="LIQUID"])').click();
await page.selectOption("#mv-storage", { index: 1 });
await page.locator('label:has(input[name="mv-transport-means"][value="AN"])').click();
const partnerValue = await page.$eval("#mv-partner", (sel) =>
  [...sel.options].find((o) => o.textContent.includes("Colector P58"))?.value ?? "");
await page.selectOption("#mv-partner", partnerValue);
await page.waitForTimeout(400);

const dialog = page.locator('[role="dialog"]');
const text = await dialog.innerText();
// Doar blocul de transport: exemplele de atașamente („avizul, Anexa 3 semnată”) sunt comune tuturor predărilor.
const transport = text.slice(text.indexOf("Cine îl preia"), text.indexOf("Document"));
check("periculos: spune că Anexa 2 o întocmește colectorul", /Anexa 2\) îl întocmește colectorul/.test(text));
check("periculos: transportul nu pomenește Anexa 3", transport.length > 0 && !/Anexa 3/.test(transport),
  (transport.match(/.{0,40}Anexa 3.{0,40}/) ?? [""])[0]);
check("periculos: șoferul și mașina rămân (avizul)", (await present("#mv-driver")) && (await present("#mv-plate")));
check("periculos: fără „Unitatea tipărită”", !(await present("#mv-anexa3-unit")));
check("periculos: fără datele de încărcare/descărcare", !(await present("#mv-load")) && !(await present("#mv-unload")));
check("periculos: fără caseta „Destinat:”", !(await present('input[name="mv-destinat"]')));
await shot(page, "58-periculos-generator-1440");

// Același formular, cod nepericulos: rubricile Anexei 3 rămân.
await pickCode("15 01 01");
await page.waitForTimeout(400);
check("nepericulos: Anexa 3 cu unitatea, datele și caseta „Destinat:”",
  (await present("#mv-anexa3-unit")) && (await present("#mv-load")) && (await present('input[name="mv-destinat"]')));

// Înapoi pe periculos și salvează: se poate, iar după salvare se oferă avizul, nu Anexa 3.
await pickCode("13 02 05");
await page.waitForTimeout(400);
await page.fill("#mv-plate", "CJ 58 PER");
await page.click('button[type="submit"][form="movement-form"]');
await page.waitForTimeout(1500);
const saved = await page.locator('[role="dialog"]').first().innerText().catch(() => "");
check("periculos: salvarea trece", /în evidență/.test(saved), saved.slice(0, 120));
check("după salvare: avizul, nu Anexa 3", /Aviz/i.test(saved) && !/Anexa 3/.test(saved), (saved.replace(/\s+/g, " ").match(/.{0,80}Anexa 3.{0,60}/) ?? [""])[0]);

console.log(fails === 0 ? "\n58: toate verificările au trecut" : `\n58: ${fails} verificări au căzut`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
