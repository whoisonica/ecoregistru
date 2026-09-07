// Proba 4: formularul de mișcare — secțiuni, banda de efect, validarea pe rubrici, duplicarea,
// confirmarea de ștergere. Scrie în baza locală de dev, nu în producție.
import { launch, newPage, login, shot, clickAt, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(700);

// ------------------------------------------------------------ SECȚIUNI
await page.click('button:has-text("Adaugă mișcare")');
await page.waitForTimeout(600);
const sections = await page.$$eval('div[role="dialog"] section h3', (h) => h.map((x) => x.textContent.trim()));
check("formularul are secțiuni titrate", sections.length === 8, sections.join(" · "));

const dlgWidth = await page.$eval('div[role="dialog"][aria-modal="true"]', (d) => Math.round(d.getBoundingClientRect().width));
check("dialogul e mai lat de 512px", dlgWidth > 560, dlgWidth + "px");

// ------------------------------------------------------------ BANDA DE EFECT
const effectBefore = await page.textContent('div[role="dialog"]');
check("banda cere codul întâi", effectBefore.includes("Alege codul de deșeu"), "");

// ------------------------------------------------------------ VALIDARE PE RUBRICI
await page.click('button[type="submit"][form="movement-form"]');
await page.waitForTimeout(600);
const invalids = await page.$$eval('div[role="dialog"] [data-invalid="true"]', (e) =>
  e.map((x) => x.id || x.getAttribute("aria-describedby") || x.tagName)
);
check("rubricile greșite sunt marcate", invalids.length >= 2, invalids.join(", "));

const messages = await page.$$eval('div[role="dialog"] p.text-red-600', (p) => p.map((x) => x.textContent.trim()));
check("fiecare marcaj are mesajul lui", messages.length >= 2, messages.join(" | "));

const banner = await page.$eval('div[role="dialog"] p[role="alert"]', (e) => e.textContent.trim()).catch(() => null);
check("bannerul trimite la rubrici", banner === "Verifică rubricile marcate mai jos.", JSON.stringify(banner));

// Rubrica greșită e legată de mesaj prin aria-describedby, nu doar colorată.
const linked = await page.evaluate(() => {
  const el = document.querySelector('div[role="dialog"] [data-invalid="true"]');
  const id = el?.getAttribute("aria-describedby");
  return { camp: el?.id, descrisDe: id, mesajExista: !!(id && document.getElementById(id)) };
});
check("marcajul e legat de mesaj", linked.mesajExista, JSON.stringify(linked));

await shot(page, "formular_validare");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// ------------------------------------------------------------ DUPLICARE
const firstRowCode = await page.textContent("tbody tr:first-child td:nth-child(2)");
await clickAt(page, "tbody tr:first-child td:last-child button[aria-haspopup='menu']");
await page.waitForTimeout(300);
const menuItems = await page.$$eval('[role="menuitem"]', (m) => m.map((x) => x.textContent.trim()));
check("meniul de rând are acțiunile", menuItems.some((m) => m.includes("Duplică")), menuItems.join(" · "));

await page.click('[role="menuitem"]:has-text("Duplică")');
await page.waitForTimeout(700);
const dupTitle = await page.textContent('div[role="dialog"] h2');
check("duplicarea deschide un formular nou", dupTitle.includes("pornită de la alta"), dupTitle);

const dup = await page.evaluate(() => ({
  cod: document.querySelector("#mv-code")?.textContent?.trim(),
  data: document.querySelector("#mv-date")?.value,
  document: document.querySelector("#mv-doc")?.value,
}));
const azi = new Date().toISOString().slice(0, 10);
check("duplicarea aduce codul", !!dup.cod && dup.cod !== "Caută codul de deșeu", dup.cod);
check("duplicarea pune data de azi", dup.data === azi, `${dup.data} (azi: ${azi})`);
check("duplicarea golește documentul", dup.document === "", JSON.stringify(dup.document));
await shot(page, "formular_duplicare");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// ------------------------------------------------------------ CONFIRMARE ȘTERGERE
await clickAt(page, "tbody tr:first-child td:last-child button[aria-haspopup='menu']");
await page.waitForTimeout(300);
await page.click('[role="menuitem"]:has-text("Șterge")');
await page.waitForTimeout(500);
const confirmText = await page.textContent('div[role="dialog"]');
check("confirmarea spune ce rând", confirmText.includes(firstRowCode.trim().slice(0, 8)), confirmText.replace(/\s+/g, " ").slice(0, 110));
check("confirmarea spune urmarea", confirmText.includes("nu poate fi anulată"), "");
await shot(page, "confirmare_stergere");
// Anulăm: nu ștergem nimic.
await page.click('div[role="dialog"] button:has-text("Anulează")');
await page.waitForTimeout(400);
check("anularea nu șterge nimic", (await page.$$("tbody tr")).length > 0, "");

await browser.close();
console.log("");
console.log(fails === 0 ? "REZULTAT: formularul trece" : `REZULTAT: ${fails} eșecuri`);
process.exit(fails === 0 ? 0 : 1);
