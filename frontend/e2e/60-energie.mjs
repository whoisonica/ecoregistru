// Proba 60: declarația anuală de energie (Legea 121/2014, Anexa 1 sub 1000 tep) — fișa, Termene, dosarul, Acasă.
//
// Ce se poate strica: (1) /termene/energie nu se deschide sau rubricile bifate nu apar ca rânduri; (2) „Total an” la
// curent nu e suma lunilor sau cărbunele fără tep nu arată „?”; (3) cu tep pe toate lunile, totalul păstrează „?”, sau
// rândul de pe Termene nu spune „din 12 luni completate” / nu duce la fișa anului; (4) Anexa 1 / Declarația nu
// descarcă .xlsx / .docx; (5) peste 1000 tep Anexa 1 rămâne activă sau avertismentul lipsește; (6) dosarul de control
// n-are tabul „Energie” cu anul, sau tabul implicit pierde un rând din „Ce intră în arhivă”; (7) Acasă nu amintește
// luna netrecută; (8) fișa derulează pe pagină la 1440×900 sau lateral la 375px; (1b) o rubrică bifată pe anul AN
// schimbă și fișa din AN-1 (rubricile sunt ale anului, decizia F3 din 05.10.2026).
// Date-robustă: AN vine din ceas; documentele se completează pe AN-1; Termene se verifică pe termenul viitor (anul AN).
// ⚠️ Lasă în urmă: pe firma demo, rubricile „Energie electrică” și „Cărbune” și cele douăsprezece luni ale lor pe anul
// AN-1 (curent 10, cărbune 1 cu tep 0,5), iar pe anul AN rubricile „Energie electrică”, „Cărbune” și „Gaze naturale”,
// fără luni. Se curăță singură la rulare următoare.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const AN = new Date().getFullYear();
const Y = AN - 1;
const MONTHS = ["ianuarie", "februarie", "martie", "aprilie", "mai", "iunie", "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie"];
const ELEC = "Energie electrică";
const COAL = "Cărbune";
const GAS = "Gaze naturale";
// Singurul 4xx iertat: un 401 la intrare (POST /auth/login). Consola îl repetă fără adresă, deci și linia ei se iartă;
// orice alt 4xx sau 5xx rămâne prins de linia „[HTTP …]”, care poartă adresa.
const KNOWN_LOGIN_401 = /^\[HTTP 401\] POST \S*\/api\/v1\/auth\/login(\?|$)|^\[consolă error\] Failed to load resource: the server responded with a status of 401/;
const isWrite = (r) => r.request().method() !== "GET" && r.url().includes("/api/") && /energ/i.test(r.url());

/** Scrie o cifră în celulă și așteaptă răspunsul serverului; celula neschimbată nu trimite nimic. */
async function put(p, carrier, month, value, tep = false) {
  const input = p.getByLabel(`${carrier}, ${MONTHS[month - 1]}${tep ? ", tep" : ""}`, { exact: true });
  if ((await input.inputValue()) === value) return;
  await input.fill(value);
  const saved = p.waitForResponse(isWrite, { timeout: 8000 }).catch(() => null);
  await input.blur();
  await saved;
}
const rowOf = (p, carrier) => p.locator("tbody tr", { hasText: carrier });
const lastCell = async (p, carrier, fromEnd) => {
  const cells = rowOf(p, carrier).locator("td");
  return (await cells.nth((await cells.count()) - 1 - fromEnd).innerText()).trim();
};
const dialog = () => page.locator('[role="dialog"]');

/** Bifează pe fișa anului `year` exact rubricile din `names` (prin card sau prin „Schimbă rubricile”) și salvează. */
async function pickCarriers(year, names) {
  await page.goto(BASE + `/termene/energie?an=${year}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  const hasSheet = (await page.getByRole("button", { name: "Schimbă rubricile" }).count()) > 0;
  if (hasSheet) {
    await page.getByRole("button", { name: "Schimbă rubricile" }).click();
    await dialog().waitFor();
  }
  const group = hasSheet ? dialog() : page;
  const boxes = group.locator('input[name="rubrici-energie"]');
  for (let i = 0; i < (await boxes.count()); i++) {
    const label = (await boxes.nth(i).locator("xpath=..").innerText()).trim();
    // „Energie electrică” nu e „Energie electrică din surse regenerabile”.
    const want = names.some((n) => label.startsWith(n) && !(n === ELEC && label.startsWith("Energie electrică din")));
    if ((await boxes.nth(i).isChecked()) !== want) await boxes.nth(i).locator("xpath=..").click();
  }
  const saved = page.waitForResponse(isWrite, { timeout: 8000 }).catch(() => null);
  await group.getByRole("button", { name: "Salvează", exact: true }).click();
  await saved;
  if (hasSheet) await dialog().waitFor({ state: "detached" });
  await page.waitForTimeout(800);
}

await login(page, "admin");

// (1) Rubricile: exact „Energie electrică” și „Cărbune”.
await page.goto(BASE + `/termene/energie?an=${Y}`, { waitUntil: "networkidle" });
await page.waitForTimeout(600);
check("fișa se deschide cu titlul anului", (await page.locator("h1").innerText()).includes(`Fișa de energie · ${Y}`));
await pickCarriers(Y, [ELEC, COAL]);
check("tabelul are 2 rânduri", (await page.locator("tbody tr").count()) === 2, String(await page.locator("tbody tr").count()));
check("rândurile sunt curentul și cărbunele", (await rowOf(page, ELEC).count()) === 1 && (await rowOf(page, COAL).count()) === 1);

// (1b) Rubricile sunt ale anului: gazele bifate pe AN nu apar pe fișa din Y.
await pickCarriers(AN, [ELEC, COAL, GAS]);
check(`pe ${AN}: 3 rânduri, cu gazele`, (await page.locator("tbody tr").count()) === 3 && (await rowOf(page, GAS).count()) === 1,
  String(await page.locator("tbody tr").count()));
await page.goto(BASE + `/termene/energie?an=${Y}`, { waitUntil: "networkidle" });
await page.waitForTimeout(600);
check(`pe ${Y}: tot 2 rânduri, fără gaze`, (await page.locator("tbody tr").count()) === 2 && (await rowOf(page, GAS).count()) === 0,
  String(await page.locator("tbody tr").count()));

// Curățenie: o rulare anterioară a lăsat cifre (golită, cantitatea șterge celula întreagă).
for (const c of [ELEC, COAL]) for (let m = 1; m <= 12; m++) await put(page, c, m, "");

// (2) Curent 12 × 10; cărbune 12 × 1, tep pe Feb–Dec, ianuarie fără tep.
for (let m = 1; m <= 12; m++) await put(page, ELEC, m, "10");
for (let m = 1; m <= 12; m++) await put(page, COAL, m, "1");
for (let m = 2; m <= 12; m++) await put(page, COAL, m, "0,5", true);
await page.waitForTimeout(600);
check("curent: „Total an” = 120,000", (await lastCell(page, ELEC, 1)) === "120,000", await lastCell(page, ELEC, 1));
check("cărbune: tep „?” cât lipsește ianuarie", (await lastCell(page, COAL, 0)) === "?", await lastCell(page, COAL, 0));
const totalLine = () => page.locator("p", { hasText: /^Total:/ }).first().innerText();
check("linia de total are „?”", (await totalLine()).includes("?"), await totalLine());

// (3) tep și în ianuarie: fără „?”; pe Termene, „din 12 luni completate” și linkul spre anul declarat.
await put(page, COAL, 1, "0,5", true);
await page.waitForTimeout(600);
check("cărbune: tep fără „?”", (await lastCell(page, COAL, 0)) === "6,000", await lastCell(page, COAL, 0));
check("linia de total fără „?”", !(await totalLine()).includes("?"), await totalLine());
await shot(page, "60-energie-fisa");

await page.goto(BASE + "/termene", { waitUntil: "networkidle" });
await page.waitForTimeout(1000);
const energyRows = page.locator('[data-testid="deadlines-todo"] tbody tr', { hasText: "Declarația de consum de energie" });
let found = null;
for (let i = 0; i < (await energyRows.count()); i++) {
  const r = energyRows.nth(i);
  if ((await r.locator(`a[href="/termene/energie?an=${AN}"]`).count()) > 0) found = r;
}
check(`rândul de energie al anului ${AN} are linkul „Fișa de energie”`, found !== null);
if (found) {
  const text = (await found.innerText()).replace(/\s+/g, " ");
  check("rândul spune „din 12 luni completate”", /\d+ din 12 luni completate/.test(text), text);
  check("linkul se cheamă „Fișa de energie”",
    (await found.locator(`a[href="/termene/energie?an=${AN}"]`).first().innerText()).includes("Fișa de energie"));
}
await shot(page, "60-energie-termene");

// (4) Documentele.
await page.goto(BASE + `/termene/energie?an=${Y}`, { waitUntil: "networkidle" });
await page.waitForTimeout(600);
const annex = page.getByRole("button", { name: "Anexa 1", exact: true });
check("Anexa 1 e activă sub prag", await annex.isEnabled());
const [dlX] = await Promise.all([page.waitForEvent("download", { timeout: 15000 }), annex.click()]);
check("Anexa 1 descarcă .xlsx", dlX.suggestedFilename().endsWith(".xlsx"), dlX.suggestedFilename());
const [dlD] = await Promise.all([
  page.waitForEvent("download", { timeout: 15000 }),
  page.getByRole("button", { name: "Declarația", exact: true }).click(),
]);
check("Declarația descarcă .docx", dlD.suggestedFilename().endsWith(".docx"), dlD.suggestedFilename());

// (5) Peste 1000 tep: Anexa 1 stinsă, avertisment; apoi înapoi sub prag.
await put(page, COAL, 1, "1500", true);
await page.waitForTimeout(800);
check("peste 1000 tep, Anexa 1 e dezactivată", await annex.isDisabled());
check("avertismentul „Peste 1000 tep” e vizibil", await page.getByText("Peste 1000 tep", { exact: false }).first().isVisible());
await shot(page, "60-energie-peste-prag");
await put(page, COAL, 1, "0,5", true);
await page.waitForTimeout(800);
check("sub prag, Anexa 1 revine activă", await annex.isEnabled());
check("avertismentul dispare", (await page.getByText("Peste 1000 tep", { exact: false }).count()) === 0);

// (6) Dosarul de control.
await page.goto(BASE + "/dosar-control?tab=energie", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const dossier = page.locator('[data-testid="audit-file-energy"] li', { hasText: String(Y) });
check(`tabul „Energie” arată rândul ${Y}`, (await dossier.count()) === 1);
await shot(page, "60-energie-dosar");
await page.goto(BASE + "/dosar-control", { waitUntil: "networkidle" });
await page.waitForSelector('[data-testid="audit-file-contents"] li', { timeout: 10000 });
const arhiva = await page.locator('[data-testid="audit-file-contents"] li').count();
check("tabul implicit: 7 rânduri în „Ce intră în arhivă”", arhiva === 7, String(arhiva));

// (7) Acasă. Luna trecută cade în anul Y numai în ianuarie, când fișa din Y e plină: atunci n-are ce amintiți.
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
if (new Date().getMonth() === 0) {
  console.log("  —    ianuarie: luna trecută e decembrie, completă pe fișa din " + Y + " — memento-ul nu apare (corect)");
} else {
  const mai = page.locator('[data-testid="next-action"] button[aria-expanded]');
  if (await mai.count()) await mai.click();
  const banda = (await page.locator('[data-testid="next-action"]').innerText()) +
    ((await page.locator('[data-testid="more-actions"]').count()) ? await page.locator('[data-testid="more-actions"]').innerText() : "");
  check("Acasă amintește „Energia pe <luna>”", banda.includes("Energia pe"), banda.replace(/\s+/g, " ").slice(0, 200));
  const items = page.locator('[data-testid="more-actions"] li');
  if ((await items.count()) > 0) {
    check("energia e ultima acțiune", (await items.last().innerText()).includes("Energia pe"));
  } else {
    check("energia e singura acțiune, în bandă", (await page.locator('[data-testid="next-action"]').innerText()).includes("Energia pe"));
  }
}
await shot(page, "60-energie-acasa");

// (8) Derularea.
await page.goto(BASE + `/termene/energie?an=${Y}`, { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const over = await page.evaluate(() => {
  const m = document.querySelector("main#continut");
  return m.scrollHeight - m.clientHeight;
});
check("1440×900: main#continut fără derulare", over <= 1, `${over}px`);
const tel = await newPage(browser, { width: 375, height: 800 });
await login(tel, "admin");
await tel.goto(BASE + `/termene/energie?an=${Y}`, { waitUntil: "networkidle" });
await tel.waitForTimeout(800);
const lat = await tel.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
check("375px fără derulare laterală", lat <= 0, `${lat}px`);
await shot(tel, "60-energie-telefon");

for (const p of [page, tel]) {
  const real = p.problems.filter((x) => !KNOWN_LOGIN_401.test(x));
  check("fără erori de consolă sau HTTP", real.length === 0, [...new Set(real)].join(" | "));
}

await browser.close();
console.log(fails ? `\n${fails} FAIL` : "\nToate OK");
process.exit(fails ? 1 : 0);
