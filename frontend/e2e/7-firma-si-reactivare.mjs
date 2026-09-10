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

// Cuprinsul: cinci secțiuni, dintre care patru tabele cu paginare. A cincea — „Utilizatorii
// firmei" — a venit cu P1.12 și **apare doar pentru ADMIN / PLATFORM_ADMIN**, fiindcă endpointul
// e 403 pentru ceilalți; proba rulează ca admin, deci o vede. Numărul e scris aici dinadins: un
// `length > 0` ar fi trecut și dacă jumătate din cuprins dispărea.
const cuprins = await page.$$eval("nav[aria-label] a[href^='#']", (as) =>
  as.map((a) => ({ href: a.getAttribute("href"), text: a.textContent.trim() }))
);
check("pagina are cuprins", cuprins.length === 5, cuprins.map((c) => c.text).join(" · "));
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
// Restrânge la şoferul de probă înainte de a-l căuta. Fără asta proba depindea de **câţi** şoferi
// inactivi s-au adunat în baza locală: suita lasă în urmă câte unul la fiecare rulare (nu există
// ştergere de şofer, doar dezactivare), iar la a 26-a rulare rândul căutat a trecut pe pagina a
// doua şi proba a căzut — deşi ecranul era neschimbat. Căutarea o face independentă de vechime.
await page.fill("#soferi input[type='search']", NUME);
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

// ------------------------------------------------------ UTILIZATORII FIRMEI (P1.12)
// Drumul întreg al unei invitații, **apăsând butoanele**, nu citind DOM-ul: invită → rândul apare
// „În aşteptare" cu exact cele două acţiuni care i se potrivesc → anulează → rândul dispare, deci
// datele rămân cum au fost găsite.
//
// De ce apasă, nu doar se uită: pe 09.09.2026 un atașament **se descărca** în loc să se deschidă,
// și tot ce era automat a rămas verde, fiindcă nicio probă nu apăsa butonul. Aceeași gaură ar fi
// aici, unde fiecare stare are alt set de butoane și ele nu se pot vedea din tipuri.
const EMAIL_PROBA = `proba.ui.${Date.now()}@client.ro`;
await page.click("#utilizatori button");            // „Invită utilizator"
await page.waitForTimeout(500);
await page.fill("#cu-email", EMAIL_PROBA);
await page.click('button[type="submit"][form="company-invite-form"]');
await page.waitForTimeout(1000);

const randUtilizator = async (email) =>
  page.evaluate((e) => {
    const tr = [...document.querySelectorAll("#utilizatori tbody tr")].find((r) =>
      r.textContent.includes(e)
    );
    if (!tr) return null;
    return {
      text: tr.textContent,
      butoane: [...tr.querySelectorAll("button")].map((b) => b.textContent.trim()),
    };
  }, email);

const invitat = await randUtilizator(EMAIL_PROBA);
check("invitatul apare în listă", !!invitat, invitat?.text?.slice(0, 60));
check("și e „În așteptare”", (invitat?.text ?? "").includes("În așteptare"));
// Cele două care i se potrivesc, și **nu** „Dezactivează": o invitație nu se dezactivează —
// serverul refuză (`user.still.pending`), fiindcă drumul înapoi ar fi starea stricată pe care
// `V34` o previne. Butonul nici nu trebuie oferit.
check(
  "are „Retrimite” și „Anulează”, nu „Dezactivează”",
  (invitat?.butoane ?? []).some((b) => b.includes("Retrimite")) &&
    (invitat?.butoane ?? []).some((b) => b.includes("Anulează")) &&
    !(invitat?.butoane ?? []).some((b) => b.includes("Dezactivează")),
  (invitat?.butoane ?? []).join(" · ")
);

// Rândul propriu nu-și oferă nici rolul, nici dezactivarea: serverul le refuză oricum
// (`user.cannot.manage.self`), iar un control care nu poate reuși e mai rău decât o etichetă.
const eu = await randUtilizator("admin@demo.ro");
check("rândul propriu e marcat „(tu)”", (eu?.text ?? "").includes("(tu)"), eu?.text?.slice(0, 40));
check("și n-are butoane de acțiune", (eu?.butoane ?? []).length === 0, (eu?.butoane ?? []).join(" · "));

await page.evaluate((e) => {
  const tr = [...document.querySelectorAll("#utilizatori tbody tr")].find((r) => r.textContent.includes(e));
  [...tr.querySelectorAll("button")].find((b) => b.textContent.includes("Anulează")).click();
}, EMAIL_PROBA);
await page.waitForTimeout(400);
const confirmare = await page.evaluate(() => document.querySelector('div[role="dialog"]')?.textContent ?? "");
check("anularea întreabă întâi", confirmare.includes("Anulezi invitația?"), confirmare.slice(0, 60));
await page.evaluate(() => {
  const dlg = document.querySelector('div[role="dialog"]');
  [...dlg.querySelectorAll("button")].find((b) => b.textContent.includes("Anulează invitația")).click();
});
await page.waitForTimeout(1000);
check("rândul dispare de tot", (await randUtilizator(EMAIL_PROBA)) === null);

await shot(page, "setari_utilizatori");

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
