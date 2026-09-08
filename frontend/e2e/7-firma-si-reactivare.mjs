// Proba 7: datele firmei în Setări, dezactivarea care se poate lua înapoi, și garda de la
// închiderea formularului de mișcare.
//
// Toate trei sunt din aceeași familie: lucruri pe care `tsc` le vede compilate și corecte, dar
// care ori nu există pe ecran, ori nu se pot apăsa. Ultima e și cea mai scumpă dacă se strică —
// un formular cu treizeci de rubrici închis din greșeală.
import { launch, newPage, login, shot, visible, BASE } from "./lib.mjs";

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

// ------------------------------------------------------- DATELE FIRMEI ÎN SETĂRI
// Un ADMIN de firmă nu-și vedea nicăieri CAEN-ul, autorizația sau persoana desemnată: toate se
// editau doar din „Clienți", care e ecran de PLATFORM_ADMIN. Întrebarea n-avea unde primi răspuns.
await page.goto(BASE + "/setari", { waitUntil: "networkidle" });
await page.waitForTimeout(800);

check("secțiunea „Datele firmei” există", await visible(page, "#datele-firmei"));

const firma = await page.evaluate(() => {
  const sec = document.querySelector("#datele-firmei");
  if (!sec) return null;
  const titluri = [...sec.querySelectorAll("h3")].map((h) => h.textContent.trim());
  const rubrici = [...sec.querySelectorAll("dt")].map((d) => d.textContent.trim());
  return { titluri, rubrici, text: sec.textContent };
});
check("are cele cinci grupe", firma?.titluri.length === 5, (firma?.titluri ?? []).join(" · "));
check(
  "arată rubricile care se tipăresc pe documente",
  ["CUI", "Cod CAEN", "Nr. autorizație de mediu"].every((r) =>
    (firma?.rubrici ?? []).some((x) => x.includes(r.split(" ")[0]))
  ),
  (firma?.rubrici ?? []).slice(0, 6).join(" · ")
);
check("golurile se spun, nu se ascund", (firma?.text ?? "").includes("Necompletat"));

// Cuprinsul: patru secțiuni, dintre care trei tabele cu paginare.
const cuprins = await page.$$eval("nav[aria-label] a[href^='#']", (as) =>
  as.map((a) => ({ href: a.getAttribute("href"), text: a.textContent.trim() }))
);
check("pagina are cuprins", cuprins.length === 4, cuprins.map((c) => c.text).join(" · "));
check(
  "fiecare intrare din cuprins are ținta ei",
  await page.evaluate((hrefs) => hrefs.every((h) => !!document.querySelector(h)),
    cuprins.map((c) => c.href)),
  cuprins.map((c) => c.href).join(" ")
);

// ------------------------------------------------------------------ REACTIVAREA
// Prima greșeală era definitivă: un șofer scos din listă rămânea acolo, cu badge „Inactiv", și nu
// se mai putea face nimic cu el. Proba face drumul întreg, ca să lase datele cum le-a găsit.
const NUME = "Probă Reactivare " + Date.now();
await page.click("#soferi button");           // „Adaugă șofer"
await page.waitForTimeout(500);
await page.fill("#d-name", NUME);
await page.click('button[type="submit"][form="own-driver-form"]');
await page.waitForTimeout(900);

const randSofer = async () =>
  page.evaluate((nume) => {
    const tr = [...document.querySelectorAll("#soferi tbody tr")].find((r) =>
      r.textContent.includes(nume)
    );
    if (!tr) return null;
    return {
      text: tr.textContent,
      butoane: [...tr.querySelectorAll("button")].map((b) => b.textContent.trim()),
    };
  }, NUME);

check("șoferul de probă s-a creat", !!(await randSofer()), (await randSofer())?.text?.slice(0, 40));

// Dezactivare (trece prin confirmare)
await page.evaluate((nume) => {
  const tr = [...document.querySelectorAll("#soferi tbody tr")].find((r) => r.textContent.includes(nume));
  [...tr.querySelectorAll("button")].find((b) => b.textContent.includes("Dezactivează")).click();
}, NUME);
await page.waitForTimeout(400);
await page.evaluate(() => {
  const dlg = document.querySelector('div[role="dialog"]');
  [...dlg.querySelectorAll("button")].find((b) => b.textContent.includes("Dezactivează")).click();
});
await page.waitForTimeout(900);

// Filtrul de stare apare abia acum — pe un cont fără niciun rând inactiv ar fi un comutator
// între „tot" și „tot".
const filtru = await page.evaluate(() => {
  const sec = document.querySelector("#soferi");
  const sel = [...sec.querySelectorAll("select")].find((s) =>
    (s.getAttribute("aria-label") ?? "").includes("Starea")
  );
  return sel ? { valoare: sel.value, optiuni: [...sel.options].map((o) => o.textContent) } : null;
});
check("filtrul de stare apare când există rânduri inactive", !!filtru, (filtru?.optiuni ?? []).join(" · "));
check("filtrul pornește pe „Active”", filtru?.valoare === "active");
check("rândul dezactivat iese din listă", (await randSofer()) === null);

await page.selectOption("#soferi select[aria-label*='Starea']", "inactive");
await page.waitForTimeout(400);
const inactiv = await randSofer();
check("„Inactive” îl aduce înapoi la vedere", !!inactiv, inactiv?.butoane?.join(" · "));
check("și îi oferă „Reactivează”", (inactiv?.butoane ?? []).some((b) => b.includes("Reactivează")));

await page.evaluate((nume) => {
  const tr = [...document.querySelectorAll("#soferi tbody tr")].find((r) => r.textContent.includes(nume));
  [...tr.querySelectorAll("button")].find((b) => b.textContent.includes("Reactivează")).click();
}, NUME);
await page.waitForTimeout(900);
await page.selectOption("#soferi select[aria-label*='Starea']", "all").catch(() => {});
await page.waitForTimeout(400);
const dupa = await randSofer();
check("reactivarea îl face din nou activ", (dupa?.text ?? "").includes("Activ") && !(dupa?.text ?? "").includes("Inactiv"), dupa?.text?.slice(0, 60));

// Lăsăm datele cum le-am găsit: șoferul de probă se scoate din listă.
await page.evaluate((nume) => {
  const tr = [...document.querySelectorAll("#soferi tbody tr")].find((r) => r.textContent.includes(nume));
  [...tr.querySelectorAll("button")].find((b) => b.textContent.includes("Dezactivează"))?.click();
}, NUME);
await page.waitForTimeout(400);
await page.evaluate(() => {
  const dlg = document.querySelector('div[role="dialog"]');
  [...(dlg?.querySelectorAll("button") ?? [])].find((b) => b.textContent.includes("Dezactivează"))?.click();
});
await page.waitForTimeout(600);

await shot(page, "setari_datele_firmei");

// ------------------------------------------- GARDA DE LA ÎNCHIDEREA FORMULARULUI
await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.keyboard.press("n");
await page.waitForTimeout(600);
check("formularul s-a deschis", await visible(page, 'div[role="dialog"][aria-modal="true"]'));

// Neatins, Escape închide fără să întrebe: o întrebare pe un formular gol e zgomot.
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
check("formularul neatins se închide direct", !(await visible(page, 'div[role="dialog"][aria-modal="true"]')));

await page.keyboard.press("n");
await page.waitForTimeout(600);
await page.fill("#mv-doc", "AVIZ 12345");
await page.waitForTimeout(200);
await page.keyboard.press("Escape");
await page.waitForTimeout(500);
const intrebare = await page.evaluate(() => {
  const dlgs = [...document.querySelectorAll('div[role="dialog"]')];
  return {
    cate: dlgs.length,
    text: dlgs.map((d) => d.textContent).join(" | "),
  };
});
check("formularul început întreabă înainte să se închidă", intrebare.text.includes("Închizi fără să salvezi?"), `${intrebare.cate} dialoguri`);
check("formularul e încă acolo, sub întrebare", intrebare.cate === 2);

// Escape peste întrebare închide **doar** întrebarea: fără teancul de dialoguri, o singură
// apăsare le-ar fi închis pe amândouă — adică exact paguba de care întreabă.
await page.keyboard.press("Escape");
await page.waitForTimeout(500);
const dupaEscape = await page.evaluate(() => ({
  cate: document.querySelectorAll('div[role="dialog"]').length,
  document: document.querySelector("#mv-doc")?.value,
}));
check("Escape închide doar întrebarea", dupaEscape.cate === 1, `${dupaEscape.cate} dialoguri`);
check("ce s-a scris e neatins", dupaEscape.document === "AVIZ 12345", dupaEscape.document);

// Iar dacă răspunzi „închide", se închide.
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
await page.evaluate(() => {
  const dlgs = [...document.querySelectorAll('div[role="dialog"]')];
  const intrebare = dlgs[dlgs.length - 1];
  [...intrebare.querySelectorAll("button")].find((b) => b.textContent.includes("Închide")).click();
});
await page.waitForTimeout(600);
check("răspunsul „închide” chiar închide", !(await visible(page, 'div[role="dialog"][aria-modal="true"]')));

await shot(page, "garda_formular");
await browser.close();
console.log("");
console.log(fails === 0 ? "REZULTAT: firma, reactivarea și garda trec" : `REZULTAT: ${fails} eșecuri`);
process.exit(fails === 0 ? 0 : 1);
