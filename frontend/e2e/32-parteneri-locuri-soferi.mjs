// Proba 32: punctele de lucru și șoferii unui partener, la vedere (17.09.2026).
//
// Stăteau doar în fișă: punctele de lucru la coada pasului 1, după Registrul Comerțului, iar șoferii
// sub cardul „Vine el și îl ia” de la pasul 2. Cine voia să adauge un șofer mai târziu nu avea niciun
// semn în tabel. Acum rândul spune câte are și are „+ Punct de lucru” / „+ Șofer”, care deschid
// fișa pe pasul 4 cu un rând gol și cursorul în el.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
let fails = 0;

function check(name, ok, detail = "") {
  if (ok) console.log(`  OK   ${name}${detail ? " — " + detail : ""}`);
  else {
    console.log(`  FAIL ${name}${detail ? " — " + detail : ""}`);
    fails++;
  }
}

const run = Date.now().toString().slice(-6);
const carrierName = `Proba 32 Transport ${run}`;
const dialog = 'div[role="dialog"][aria-modal="true"]';
const submit = `${dialog} button[type="submit"]`;

async function stepLabel() {
  return (await page.textContent(`${dialog} .eyebrow`))?.trim() ?? "";
}

/** Rândul partenerului, după căutare — tabelul e paginat. Caseta apare numai peste câteva rânduri. */
async function row(name) {
  if (await page.$('input[type="search"]')) {
    await page.fill('input[type="search"]', name);
    await page.waitForTimeout(400);
  }
  return page.locator("main tbody tr", { hasText: name }).first();
}

// ------------------------------------------------------------ (1) ADĂUGARE PE PATRU PAȘI
await page.goto(BASE + "/parteneri?nou=1", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.fill("#p-name", carrierName);
await page.click(submit); // → pasul 2
await page.waitForTimeout(300);
await page.click(`${dialog} input[name="p-type"][value="NONE"]`, { force: true });
await page.click(submit); // → pasul 3
await page.waitForTimeout(300);
await page.click(submit); // → pasul 4
await page.waitForTimeout(300);
check("adăugarea are pasul 4", (await stepLabel()) === "Pasul 4 din 4", await stepLabel());
const rail = await page.$$eval(`${dialog} nav[aria-label] li`, (lis) => lis.map((li) => li.textContent.trim()));
check("cuprinsul are patru pași", rail.length === 4, rail.join(" · "));
check(
  "pasul 4 are punctele de lucru și șoferii",
  await page.isVisible(`${dialog} button:has-text("Adaugă punct de lucru")`) &&
    await page.isVisible(`${dialog} button:has-text("Adaugă șofer")`)
);
await page.click(submit); // salvează, fără nimic la pasul 4
await page.waitForTimeout(900);
check("se salvează cu pasul 4 gol", (await page.$(dialog)) === null);

// ------------------------------------------------------------ (2) „+ ȘOFER” DIN TABEL
let r = await row(carrierName);
check("rândul transportatorului arată „+ Șofer”", (await r.locator('button:has-text("Șofer")').count()) === 1);
check("și „+ Punct de lucru”", (await r.locator('button:has-text("Punct de lucru")').count()) === 1);
await shot(page, "32_rand_fara_soferi");

await r.locator('button:has-text("Șofer")').click();
await page.waitForTimeout(500);
check("„+ Șofer” deschide fișa pe pasul 4", (await stepLabel()) === "Pasul 4 din 4", await stepLabel());
const focused = await page.evaluate(() => document.activeElement?.id);
check("cursorul e în rândul nou de șofer", focused === "p-driver-name-0", focused);
await page.keyboard.type("Ion Proba");
await page.fill("#p-driver-plate-0", "CJ 32 PRB");
await page.click(submit);
await page.waitForTimeout(900);

r = await row(carrierName);
check("rândul spune „1 șofer”", (await r.locator('button:has-text("1 șofer")').count()) === 1, await r.textContent());

// ------------------------------------------------------------ (3) „+ PUNCT DE LUCRU”
await r.locator('button:has-text("Punct de lucru")').click();
await page.waitForTimeout(500);
check("cursorul e în rândul nou de punct de lucru", (await page.evaluate(() => document.activeElement?.id)) === "p-wp-name-0");
await page.keyboard.type("Depozit Proba");
await page.fill("#p-wp-address-0", "Str. Probei 32, Cluj-Napoca");
await page.click(submit);
await page.waitForTimeout(900);
r = await row(carrierName);
const text = (await r.textContent()) ?? "";
check("rândul spune „1 punct de lucru · 1 șofer”", text.includes("1 punct de lucru") && text.includes("1 șofer"), text);

// Numărul deschide fișa pe pasul 4, fără rând gol în plus.
await r.locator('button:has-text("1 șofer")').click();
await page.waitForTimeout(500);
check("numărul deschide pasul 4", (await stepLabel()) === "Pasul 4 din 4");
check("fără rând gol adăugat", (await page.$("#p-driver-name-1")) === null);
await shot(page, "32_pasul_4");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// ------------------------------------------------------------ (4) CINE NU TRANSPORTĂ
// Un partener fără transport n-are „+ Șofer” în tabel: formularul de mișcare nu-i alege șoferii.
if (await page.$('input[type="search"]')) {
  await page.fill('input[type="search"]', "");
  await page.waitForTimeout(300);
}
const nonCarrierPlus = await page.$$eval("main tbody tr", (rows) =>
  rows.filter((tr) => {
    const t = tr.textContent ?? "";
    return !t.includes("Transportator") && [...tr.querySelectorAll("button")].some((b) => b.textContent.trim() === "Șofer");
  }).length
);
check("niciun „+ Șofer” la cine nu transportă", nonCarrierPlus === 0, String(nonCarrierPlus));

const width = await page.evaluate(() => {
  const w = document.querySelector("main table")?.parentElement;
  return w ? `${w.scrollWidth}/${w.clientWidth}` : "?";
});
check("tabelul nu derulează lateral la 1440px", width.split("/")[0] === width.split("/")[1], width);

// ------------------------------------------------------------ (5) TELEFON
const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
await phone.waitForTimeout(700);
const overflow = await phone.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
check("375px fără derulare laterală a paginii", overflow <= 0, String(overflow));
await shot(phone, "32_telefon");

console.log("");
if (fails === 0) console.log("✓ proba 32 trece.");
else console.log(`✗ proba 32: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
