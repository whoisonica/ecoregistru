// Proba 3: lucrurile pe care le-am adăugat chiar fac ce spun.
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

const rows = () => page.$$eval("tbody tr", (r) => r.length);

// ---------------------------------------------------------------- CĂUTARE
await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
const before = await rows();
await page.fill("[data-table-search]", "15 01 02");
await page.waitForTimeout(300);
const after = await rows();
check("căutare restrânge", after > 0 && after < before, `${before} → ${after} rânduri`);

const codes = await page.$$eval("tbody tr td:nth-child(2)", (tds) => tds.map((t) => t.textContent.trim()));
check("toate rezultatele se potrivesc", codes.every((c) => c.includes("15 01 02")), codes.slice(0, 3).join(" | "));

// Escape golește căutarea
await page.focus("[data-table-search]");
await page.keyboard.press("Escape");
await page.waitForTimeout(300);
check("Escape golește căutarea", (await rows()) === before, `înapoi la ${await rows()}`);

// Căutare fără rezultate → starea goală potrivită
await page.fill("[data-table-search]", "zzzznuexista");
await page.waitForTimeout(300);
const emptyText = await page.textContent("tbody");
check("gol din căutare are alt mesaj", emptyText.includes("Niciun rezultat"), JSON.stringify(emptyText.trim().slice(0, 40)));
await page.fill("[data-table-search]", "");
await page.waitForTimeout(300);

// ---------------------------------------------------------------- SORTARE
const dateHeader = await page.$('th[aria-sort] button');
const sortBefore = await page.getAttribute("th[aria-sort]", "aria-sort");
await dateHeader.click();
await page.waitForTimeout(200);
const sortAfter = await page.getAttribute("th[aria-sort]", "aria-sort");
check("sortarea schimbă aria-sort", sortAfter === "ascending", `${sortBefore} → ${sortAfter}`);

const dates = await page.$$eval("tbody tr td:nth-child(1)", (t) => t.map((x) => x.textContent.trim()));
const iso = dates.filter((d) => /^\d{2}\.\d{2}\.\d{4}$/.test(d)).map((d) => d.split(".").reverse().join("-"));
const ascending = iso.every((d, i) => i === 0 || iso[i - 1] <= d);
check("rândurile chiar sunt sortate crescător", ascending, iso.slice(0, 3).join(" , "));

// ---------------------------------------------------------------- PAGINARE
const pageInfo = await page.textContent("section");
check("paginarea arată intervalul", /\d+–\d+ din \d+/.test(pageInfo), (pageInfo.match(/\d+–\d+ din \d+/) || [""])[0]);

// ---------------------------------------------------------------- FILTRE ÎN URL
await page.selectOption("#filter-wp", { index: 1 });
await page.waitForTimeout(500);
check("filtrul intră în adresă", page.url().includes("punct="), page.url().split("?")[1] || "(fără query)");
const filtered = await rows();
await page.goto(BASE + "/panou-inexistent", { waitUntil: "domcontentloaded" }).catch(() => {});
await page.goBack({ waitUntil: "networkidle" });
await page.waitForTimeout(600);
check("filtrul supraviețuiește navigării", page.url().includes("punct="), page.url().split("?")[1] || "(fără query)");
await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(600);

// ---------------------------------------------------------------- PALETA Ctrl+K
await page.keyboard.press("Control+k");
await page.waitForTimeout(400);
const paletteOpen = await page.$('div[role="dialog"] input');
check("Ctrl+K deschide paleta", !!paletteOpen);
if (paletteOpen) {
  await page.keyboard.type("evid");
  await page.waitForTimeout(300);
  await page.keyboard.press("Enter");
  await page.waitForURL((u) => u.pathname === "/evidente", { timeout: 5000 }).catch(() => {});
  check("paleta navighează", page.url().includes("/evidente"), page.url());
}

// ---------------------------------------------------------------- „/" pe căutare
await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click("h1");
await page.keyboard.press("/");
await page.waitForTimeout(250);
const focused = await page.evaluate(() => document.activeElement?.hasAttribute("data-table-search") === true);
check("„/” duce în caseta de căutare", focused);

// Sub prag caseta nu se arată deloc — regula, nu o scăpare.
await page.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
const few = await page.evaluate(() => ({
  randuri: document.querySelectorAll("tbody tr").length,
  caseta: !!document.querySelector("[data-table-search]"),
}));
check("sub 10 rânduri nu apare căutare", few.randuri < 10 && !few.caseta, `${few.randuri} rânduri, casetă: ${few.caseta}`);

// ---------------------------------------------------------------- „N" deschide formularul
await page.click("h1");
await page.keyboard.press("n");
await page.waitForTimeout(400);
const dlg = await page.$('div[role="dialog"][aria-modal="true"]');
check("„N” deschide formularul", !!dlg);
if (dlg) {
  await page.keyboard.press("Escape");
  await page.waitForTimeout(300);
  check("Escape închide dialogul", !(await page.$('div[role="dialog"][aria-modal="true"]')));
}

await shot(page, "interactiuni_final");
await browser.close();
console.log("");
console.log(fails === 0 ? "REZULTAT: toate interacțiunile trec" : `REZULTAT: ${fails} eșecuri`);
process.exit(fails === 0 ? 0 : 1);
