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

/**
 * Mişcările pe **tot anul**.
 *
 * <p>Ecranul porneşte pe luna curentă (07.09.2026): fără lună se aduceau toate mişcările firmei,
 * oricâte, iar paginarea taie abia după ce au venit. Aici e nevoie de mai mult de zece rânduri —
 * bara de căutare apare abia de la zece în sus — deci proba cere dinadins anul întreg, ceea ce
 * probează şi treapta nouă din filtru.
 */
const AN = new Date().getFullYear();
const MISCARI = `${BASE}/miscari?luna=${AN}`;

// ---------------------------------------------------------------- CĂUTARE
await page.goto(MISCARI, { waitUntil: "networkidle" });
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

// Diacriticele nu contează: cine tastează repede scrie „deseuri", nu „deșeuri". Până la `fold`
// (07.09.2026) răspunsul era zero rânduri — nu rezultate parțiale, zero, cu ecranul spunând
// „Niciun rezultat" pentru un cuvânt care se vede în tabel.
const searchCount = async (q) => {
  await page.fill("[data-table-search]", "");
  await page.waitForTimeout(300);
  await page.fill("[data-table-search]", q);
  await page.waitForTimeout(600);
  const cells = await page.$$eval("tbody tr", (rs) => rs.map((x) => x.textContent));
  return cells.filter((t) => !t.includes("Niciun rezultat")).length;
};
for (const [fara, cu] of [
  ["deseuri", "deșeuri"],
  ["hartie", "hârtie"],
]) {
  const a = await searchCount(fara);
  const b = await searchCount(cu);
  check(`„${fara}" găsește cât „${cu}"`, a === b && a > 0, `${a} vs ${b} rânduri`);
}
await page.fill("[data-table-search]", "");
await page.waitForTimeout(300);

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

// NEACOPERIT, şi dinadins: „De cântărit" trebuie să stea la coadă **în ambele sensuri** ale
// sortării pe cantitate (la fel autorizaţia fără dată, pe Parteneri). Regula s-a stricat şi s-a
// reparat pe 07.09.2026, dar nicio probă n-o poate prinde aici: seed-ul de demo n-are niciun rând
// fără cantitate şi niciun partener fără dată de expirare — zero din 34, respectiv zero din 5.
// Ca să existe proba, trebuie întâi ca seed-ul să conţină stările pentru care ecranul are reguli.

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
await page.goto(MISCARI, { waitUntil: "networkidle" });
await page.waitForTimeout(600);

// ------------------------------------------------- FILTRUL DE LUNĂ (Safari-proof)
// `<input type="month">` nu există în Safari şi Firefox: degenera în câmp text liber, adică pe Mac
// filtrul principal al ecranului n-avea nici selector, nici validare. Proba cere ce s-a pus în loc.
await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
const luna = await page.evaluate(() => {
  const el = document.querySelector("#filter-month");
  const anul = el?.parentElement?.parentElement?.querySelectorAll("select")[1];
  const azi = new Date();
  return {
    tag: el?.tagName,
    tip: el?.getAttribute("type"),
    optiuni: el ? el.options.length : 0,
    valoare: el?.value,
    lunaAzi: String(azi.getMonth() + 1),
    anValoare: anul?.value,
    anAzi: String(azi.getFullYear()),
  };
});
check("luna se alege dintr-un select, nu dintr-un input", luna.tag === "SELECT" && !luna.tip, `${luna.tag} ${luna.tip ?? ""}`);
check("are cele 12 luni plus „Tot anul”", luna.optiuni === 13, `${luna.optiuni} opțiuni`);
check("pornește pe luna curentă", luna.valoare === luna.lunaAzi && luna.anValoare === luna.anAzi, `${luna.valoare}.${luna.anValoare}`);
check("luna implicită nu murdărește adresa", !page.url().includes("luna="), page.url().split("?")[1] || "(fără query)");

await page.selectOption("#filter-month", "3");
await page.waitForTimeout(600);
check("alegerea unei luni intră în adresă", /luna=\d{4}-03/.test(page.url()), page.url().split("?")[1] || "(fără query)");

await page.selectOption("#filter-month", "0");
await page.waitForTimeout(600);
check("„Tot anul” cere anul, fără lună", /luna=\d{4}(&|$)/.test(page.url()), page.url().split("?")[1] || "(fără query)");

// Luna goală îşi spune numele şi dă drumul înapoi — altfel un ecran care porneşte pe luna curentă
// arată „nicio mişcare" unui client care are şapte sute.
await page.goto(BASE + "/miscari?luna=2019-02", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
const gol = await page.textContent("tbody");
check("luna fără rânduri se explică", gol.includes("Nicio mișcare în Februarie 2019"), gol.trim().slice(0, 40));
const iesire = await page.$("tbody button");
check("și oferă anul întreg ca ieșire", !!iesire && (await iesire.textContent()).includes("2019"));

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
await page.goto(MISCARI, { waitUntil: "networkidle" });
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
