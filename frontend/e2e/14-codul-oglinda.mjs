// Proba 14: codul-oglindă declarat nepericulos, pe ecran (G-4).
//
// OUG 92/2021 art. 8 alin. (2): acelaşi deşeu se încadrează sub două coduri, după cum are sau nu
// caracteristici periculoase, iar încadrarea ca **nepericulos** se face „numai în baza unei analize
// a originii, testelor, buletinelor de analiză şi a altor documente relevante". E singurul loc din
// modul unde aplicaţia se uită la o **încadrare**, nu la o rubrică goală — şi singura ei abatere pe
// care un inspector chiar o caută, fiindcă un cod-oglindă declarat nepericulos ieftineşte
// eliminarea.
//
// Ce se probează: că badge-ul apare pe mişcarea fără atașament, că **numele perechii periculoase e
// în motivul afişat** (altfel clientul n-are ce verifica), şi că un cod obişnuit nu-l primeşte.
// Ultima verificare e cea care prinde o regulă prea largă: un avertisment pus pe toate rândurile
// ar trece o probă scrisă doar pe „apare badge-ul".
//
// ⚠️ Proba îşi scrie singură datele şi le şterge la sfârşit. Nu se sprijină pe seed: `17 05 04` nu
// e printre codurile puse de `DevDataSeeder`, iar felia n-are rost să aştepte o bază anume.
import { launch, newPage, login, shot, clickAt, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;

function check(name, ok, detail = "") {
  if (ok) console.log(`  OK   ${name}${detail ? " — " + detail : ""}`);
  else {
    console.log(`  FAIL ${name}${detail ? " — " + detail : ""}`);
    fails++;
  }
}

await login(page, "admin");

// `17 05 04` (pământ şi pietre) e oglinda lui `17 05 03*`; `20 01 01` (hârtie şi carton) n-are
// pereche. Amândouă sunt în nomenclatorul oficial, deci proba nu depinde de ce a seedat cineva.
const MIRROR = "17 05 04";
const PLAIN = "20 01 01";
const today = new Date().toISOString().slice(0, 10);

const setup = await page.evaluate(
  async ([mirror, plain, date]) => {
    const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
    const json = async (url, init) => {
      const res = await fetch(url, { ...init, headers: { ...auth, ...(init?.headers ?? {}) } });
      if (!res.ok) throw new Error(url + " -> " + res.status);
      return res.json();
    };
    const codeId = async (q) => (await json("/api/v1/waste-codes?q=" + encodeURIComponent(q)))
      .find((c) => c.code === q)?.id;

    const workPoints = await json("/api/v1/work-points");
    const workPointId = workPoints[0]?.id;
    const mirrorId = await codeId(mirror);
    const plainId = await codeId(plain);
    if (!workPointId || !mirrorId || !plainId) return null;

    const create = (wasteCodeId) =>
      json("/api/v1/movements", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          workPointId,
          date,
          wasteCodeId,
          quantity: 1.5,
          unit: "KG",
          physicalState: "SOLID",
          operation: "GENERATED",
          notes: "proba 14 — cod-oglindă",
        }),
      });

    const onMirror = await create(mirrorId);
    const onPlain = await create(plainId);
    return {
      mirrorId: onMirror.id,
      plainId: onPlain.id,
      flagged: onMirror.mirrorClassificationUnproven,
      pair: onMirror.mirrorOf,
      plainFlagged: onPlain.mirrorClassificationUnproven,
    };
  },
  [MIRROR, PLAIN, today]
);

if (!setup) {
  console.log("  ——   n-am putut pregăti datele (punct de lucru sau cod lipsă): proba n-are pe ce rula.");
  await browser.close();
  process.exit(1);
}

// Capătul de API, înainte de ecran: dacă răspunsul n-o spune, badge-ul n-are de unde s-o ia.
check("API: mişcarea pe cod-oglindă fără atașament e semnalată", setup.flagged === true);
check("API: perechea periculoasă e numită", setup.pair === "17 05 03", "mirrorOf = " + setup.pair);
check("API: codul obişnuit nu e semnalat", setup.plainFlagged === false);

await page.goto(BASE + `/miscari?luna=${today.slice(0, 7)}`, { waitUntil: "networkidle" });
await page.waitForTimeout(1200);

// Rândurile se caută după cod: pe o bază acumulată pot exista şi alte mişcări pe acelaşi cod, dar
// toate ar trebui să se comporte la fel — ce se probează e o regulă, nu un rând anume.
const mirrorRow = page.locator("tr").filter({ hasText: MIRROR }).first();
const plainRow = page.locator("tr").filter({ hasText: PLAIN }).first();

const mirrorBadges = await mirrorRow.getByText("Cod-oglindă").count();
check("ecran: rândul pe cod-oglindă poartă badge-ul", mirrorBadges > 0);

const plainBadges = await plainRow.getByText("Cod-oglindă").count();
check("ecran: rândul pe cod obişnuit nu-l poartă", plainBadges === 0);

// Badge-ul stă lângă COD, nu lângă partener — ce se pune la îndoială e încadrarea, nu predarea.
// Verificarea e pe celula a doua a rândului, unde ecranul scrie codul.
const inCodeCell = await mirrorRow.locator("td").nth(1).getByText("Cod-oglindă").count();
check("ecran: badge-ul stă în celula codului", inCodeCell > 0);

check("ecran: badge-ul apare o singură dată pe rând", mirrorBadges === 1, `badges = ${mirrorBadges}`);

// Motivul. `Tooltip` îşi randează bula într-un portal, numai cât e deschisă, deci se apasă
// declanşatorul şi se citeşte `[role="tooltip"]` — pe touch apăsarea e oricum singurul drum.
// `clickAt` fiindcă antetul lipicios al tabelului interceptează clicurile derulate de Playwright.
await clickAt(page, `tr:has-text("${MIRROR}") button:has-text("Cod-oglindă")`);
await page.waitForTimeout(300);
const hint = (await page.locator('[role="tooltip"]').first().textContent()) ?? "";
check("motiv: citează articolul", hint.includes("art. 8 alin. (2)"));
check("motiv: numeşte perechea periculoasă", hint.includes("17 05 03"), hint.slice(0, 70) + "…");
// Cele două jumătăţi ale regulii „constată, nu blochează": mişcarea rămâne, formularele se
// tipăresc. Un text care ar aluneca spre refuz ar trece o verificare scrisă doar pe „conţine
// art. 8", iar clientul ar crede că i s-a blocat înregistrarea.
check("motiv: spune că mişcarea rămâne înregistrată", hint.includes("rămâne înregistrată"));
check("motiv: cere documentul, nu refuză", hint.includes("atașează"));

await shot(page, "14-cod-oglinda");

// Curăţenia: proba îşi şterge mişcările, ca a doua rulare să nu citească badge-ul de la prima.
const cleaned = await page.evaluate(async ([a, b]) => {
  const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
  const del = async (id) => (await fetch("/api/v1/movements/" + id, { method: "DELETE", headers: auth })).ok;
  return (await del(a)) && (await del(b));
}, [setup.mirrorId, setup.plainId]);
check("curăţenie: mişcările de probă s-au şters", cleaned === true);

await browser.close();
process.exit(fails > 0 ? 1 : 0);
