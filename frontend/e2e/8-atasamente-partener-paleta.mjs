// Proba 8: atașamentele citite din tabel, formularul de partener pe secțiuni, cuprinsul de pe
// Ambalaje și paleta care găsește documente și pornește acțiuni.
//
// Cele patru felii au un lucru comun: `tsc` le vede compilate în toate cele patru cazuri, iar în
// trei din patru „gata" ar fi însemnat ceva ce nu se poate apăsa. Atașamentele erau o cifră fără
// drum, secțiunile pot exista în cod fără să se randeze, cuprinsul poate fi acoperit de coloana
// lipicioasă a tabelului, iar `keywords` era declarat, citit la potrivire și niciodată completat —
// adică o funcție întreagă verde la compilare și moartă la rulare.
import { launch, newPage, login, shot, visible, clickAt, BASE } from "./lib.mjs";

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

// ------------------------------------------------------------------ ATAȘAMENTELE
// Coloana arăta „📎 2" și nimic mai mult: singurul drum către fișier trecea prin formularul de
// editare, cu treizeci de rubrici — iar un VIEWER nu-l poate deschide deloc.
//
// Rândul cu atașamente e ieșirea fără cod R/D din iunie, seedată anume (`DevDataSeeder`). Fără ea
// verificările de mai jos ar trece pe un tabel care n-are rândul, ca al treilea defect din
// primitivele reparate pe 07.09.
await page.goto(BASE + "/miscari?luna=2026-06", { waitUntil: "networkidle" });
await page.waitForTimeout(800);

const celule = await page.$$eval("td button[aria-label]", (bs) =>
  bs.filter((b) => b.getAttribute("aria-label") === "Vezi atașamentele").map((b) => b.textContent.trim())
);
check("coloana de atașamente e un buton, nu o cifră moartă", celule.length > 0, celule.join(" · "));

await clickAt(page, 'td button[aria-label="Vezi atașamentele"]');
await page.waitForTimeout(500);

const dialog = await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  if (!d) return null;
  return {
    titlu: d.querySelector("h2,h3")?.textContent.trim() ?? "",
    text: d.textContent,
    html: d.innerHTML,
    // De la 11-bis fișierele sunt butoane, nu linkuri: conținutul vine printr-o cerere cu
    // sesiune, iar un `<a href>` nu duce cu el antetul `Authorization`.
    fisiere: [...d.querySelectorAll("li button")].map((b) => b.textContent.trim()),
    linkuri: [...d.querySelectorAll("a[href]")].map((a) => a.getAttribute("href")),
  };
});
check("dialogul se deschide", dialog !== null);
check("și numește mișcarea, nu doar «atașamente»", /15 01 02|\d{2}\.\d{2}\.2026/.test(dialog?.text ?? ""),
  (dialog?.text ?? "").slice(0, 60));
check("listează toate fișierele", (dialog?.fisiere ?? []).length === 2,
  (dialog?.fisiere ?? []).join(" · "));
check("fiecare are numele lui, nu «atașament 1»",
  (dialog?.fisiere ?? []).every((n) => n.length > 3 && !/^atașament/i.test(n)));
// 11-bis, proba care contează pe ecran: adresa fișierului nu mai ajunge în pagină deloc.
// Înainte era un `href` către Cloudinary — public, fără sesiune, fără verificare de tenant — iar
// „vezi sursa" era destul ca să iasă din aplicație cu documentul unui client în mână.
check("niciun link către fișier în dialog", (dialog?.linkuri ?? []).length === 0,
  (dialog?.linkuri ?? []).join(" · "));
check("și nicio adresă de Cloudinary în tot dialogul",
  !/cloudinary|res\.cloudinary/i.test(dialog?.html ?? ""));
// Numai citire: ștergerea rămâne în formular, lângă urcare, unde e și confirmarea.
check("nu are buton de ștergere", !/Șterge/.test(dialog?.text ?? ""));

await shot(page, "atasamente_dialog");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
check("Escape îl închide", !(await visible(page, 'div[role="dialog"][aria-modal="true"]')));

// Un rând fără atașamente rămâne o liniuță, nu un buton care nu deschide nimic.
const goale = await page.$$eval("td", (tds) =>
  tds.filter((td) => td.textContent.trim() === "—" && td.classList.contains("text-center")).length
);
check("rândurile fără atașamente n-au buton", goale > 0, `${goale} liniuțe`);

// -------------------------------------------------------- FORMULARUL DE PARTENER
// Era o coloană de cincisprezece blocuri într-un dialog de 512px, deși are secțiuni evidente.
await page.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.evaluate(() => {
  [...document.querySelectorAll("button")].find((b) => b.textContent.includes("Adaugă partener"))?.click();
});
await page.waitForTimeout(500);

const partener = await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  if (!d) return null;
  const form = d.querySelector("#partner-form");
  const rubrici = [...form.querySelectorAll("input, select, textarea")].map((el) => el.id).filter(Boolean);
  return {
    latime: Math.round(d.getBoundingClientRect().width),
    titluri: [...form.querySelectorAll("h3")].map((h) => h.textContent.trim()),
    rubrici,
  };
});
check("formularul are secțiuni titrate", (partener?.titluri ?? []).length === 5,
  (partener?.titluri ?? []).join(" · "));
check("dialogul s-a lărgit, ca cel de mișcare și cel de firmă", (partener?.latime ?? 0) > 600,
  `${partener?.latime}px`);
// CUI-ul stătea între bifa de transportator și autorizație: o identificare ruptă în două de o
// întrebare despre camioane.
check("CUI-ul stă lângă denumire, nu după camioane",
  (partener?.rubrici ?? []).indexOf("p-cui") === 1,
  (partener?.rubrici ?? []).slice(0, 4).join(" · "));
check("autorizația vine după rol și transport",
  (partener?.rubrici ?? []).indexOf("p-auth-number") > (partener?.rubrici ?? []).indexOf("p-type"));

await shot(page, "partener_sectiuni");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// ---------------------------------------------------------- CUPRINSUL DE PE AMBALAJE
// Patru tabele mari unul sub altul plus grila de 66 de celule: cea mai lungă pagină din aplicație.
await page.goto(BASE + "/ambalaje", { waitUntil: "networkidle" });
await page.waitForTimeout(1000);

const cuprins = await page.$$eval("nav[aria-label] a[href^='#']", (as) =>
  as.map((a) => ({ href: a.getAttribute("href"), text: a.textContent.trim() }))
);
check("pagina are cuprins", cuprins.length === 4, cuprins.map((c) => c.text).join(" · "));
check(
  "fiecare intrare are ținta ei",
  await page.evaluate((hs) => hs.every((h) => !!document.querySelector(h)), cuprins.map((c) => c.href)),
  cuprins.map((c) => c.href).join(" ")
);

// Cele trei defecte ale cuprinsului din „Setări" s-au văzut abia pe captură, cu DOM-ul verde:
// coloana de acțiuni a tabelelor e și ea lipicioasă și vine după bară în DOM. Aici se măsoară.
await page.evaluate(() => document.querySelector("#tabelul-1")?.scrollIntoView({ block: "start" }));
await page.waitForTimeout(500);
const bara = await page.evaluate(() => {
  const nav = document.querySelector("nav[aria-label] a[href^='#']")?.closest("nav");
  if (!nav) return null;
  const r = nav.getBoundingClientRect();
  // Trei puncte pe bară: dacă vreunul e acoperit, altceva a câștigat la z-index.
  const puncte = [0.15, 0.5, 0.85].map((f) => {
    const el = document.elementFromPoint(r.left + r.width * f, r.top + r.height / 2);
    return nav.contains(el);
  });
  return { top: Math.round(r.top), acoperita: puncte.some((p) => !p) };
});
check("bara rămâne lipită sub antet", (bara?.top ?? -1) >= 0 && (bara?.top ?? 999) < 120, `top ${bara?.top}px`);
check("și nimic nu trece peste ea", bara && !bara.acoperita);

// Clicul chiar duce acolo, și marcajul se mută pe secțiunea curentă.
//
// Se probează pe **Tabelul 1**, nu pe una de la coadă: `scroll-mt-20` duce secțiunea sub bară
// numai dacă pagina mai are unde curge. Ultimele două intră amândouă în ultimul ecran — clicul pe
// ele ajunge la fundul paginii, nu la titlul lor — și tocmai de asta `SectionNav` are ramura „la
// fund", scrisă după ce prima variantă a fost văzută greșind pe o captură.
await clickAt(page, "nav[aria-label] a[href='#tabelul-1']");
await page.waitForTimeout(900);
const laMijloc = await page.evaluate(() => ({
  activ: document.querySelector("nav[aria-label] a[aria-current='true']")?.getAttribute("href"),
  sus: Math.round(document.querySelector("#tabelul-1").getBoundingClientRect().top),
}));
check("clicul duce la secțiune", laMijloc.sus >= 0 && laMijloc.sus < 200, `${laMijloc.sus}px`);
check("și marcajul o urmează", laMijloc.activ === "#tabelul-1", laMijloc.activ ?? "niciunul");
// `scroll-mt-20` = 80px. Sub 40 ar însemna că titlul intră pe sub bara lipicioasă.
check("titlul nu intră sub bară", laMijloc.sus > 40, `${laMijloc.sus}px sub antet`);

// Ultima secțiune: marcajul trebuie să ajungă pe ea chiar dacă titlul ei rămâne la jumătatea
// ecranului. Prima variantă a primitivei marca aici secțiunea de dinainte.
await clickAt(page, "nav[aria-label] a[href='#anexa-3']");
await page.waitForTimeout(900);
const laCoada = await page.evaluate(() => {
  const sec = document.querySelector("#anexa-3");
  let sc = sec.parentElement;
  while (sc && !(getComputedStyle(sc).overflowY.match(/auto|scroll/) && sc.scrollHeight > sc.clientHeight)) {
    sc = sc.parentElement;
  }
  return {
    activ: document.querySelector("nav[aria-label] a[aria-current='true']")?.getAttribute("href"),
    laFund: sc ? sc.scrollTop + sc.clientHeight >= sc.scrollHeight - 4 : false,
  };
});
check("ultima secțiune duce la capătul paginii", laCoada.laFund);
check("și marcajul ajunge pe ea, deși titlul nu urcă sus",
  laCoada.activ === "#anexa-3", laCoada.activ ?? "niciunul");

await shot(page, "ambalaje_cuprins");

// `min-w-max` pe `<ul>` a făcut o dată pagina să se deruleze lateral cu 129px la 375px.
const telefon = await newPage(browser, { width: 375, height: 812 });
await login(telefon, "admin");
await telefon.goto(BASE + "/ambalaje", { waitUntil: "networkidle" });
await telefon.waitForTimeout(1000);
const lateral = await telefon.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
check("pagina nu se derulează lateral la 375px", lateral <= 0, `${lateral}px`);
await shot(telefon, "ambalaje_telefon");
await telefon.close();

// ------------------------------------------------------------------- PALETA (Ctrl+K)
// `Command.keywords` era declarat și citit la potrivire, dar nu i-l dădea nimeni: tastai „fișa"
// sau „anexa 1" și paleta nu găsea nimic, deși propriul docstring promitea „unde vreau să ajung".
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(600);

async function paleta(text) {
  await page.keyboard.press("Control+k");
  await page.waitForTimeout(300);
  await page.keyboard.type(text);
  await page.waitForTimeout(400);
  const rezultate = await page.$$eval('div[role="option"]', (os) => os.map((o) => o.textContent.trim()));
  return rezultate;
}
async function inchidePaleta() {
  await page.keyboard.press("Escape");
  await page.waitForTimeout(300);
}

const fisa = await paleta("fisa");
check("„fisa” găsește Evidențe", fisa.some((r) => r.includes("Evidențe")), fisa.join(" · "));
await inchidePaleta();

// „Anexa 1" e numele scurt a două documente diferite (decizia 12): declarația de ambalaje și fișa
// din HG 856/2002. Amândouă apar, cu eticheta lor — a alege una ar ascunde cealaltă de cine o
// caută pe nume.
const anexa1 = await paleta("anexa 1");
check("„anexa 1” găsește amândouă documentele",
  anexa1.some((r) => r.includes("Ambalaje")) && anexa1.some((r) => r.includes("Evidențe")),
  anexa1.join(" · "));
// Şi **numai** pe ele. Verificarea asta e cea care a prins cheile duplicate: lista arăta şi
// „Dosar de control", care n-are „anexa" nicăieri — erau rânduri rămase din randarea dinainte.
check("și nimic altceva", anexa1.length === 2, `${anexa1.length} rezultate`);
check("niciun rând nu apare de două ori", new Set(anexa1).size === anexa1.length, anexa1.join(" · "));
await inchidePaleta();

const control = await paleta("inspector");
check("„inspector” găsește dosarul de control", control.some((r) => r.includes("Dosar")), control.join(" · "));
await inchidePaleta();

const soferi = await paleta("soferi");
check("„soferi” găsește și Setări, și Parteneri",
  soferi.some((r) => r.includes("Setări")) && soferi.some((r) => r.includes("Parteneri")),
  soferi.join(" · "));
await inchidePaleta();

// Jumătatea cealaltă: ce se **începe**, nu unde se ajunge.
const predare = await paleta("predare");
check("„predare” găsește acțiunea de adăugare", predare.some((r) => r.includes("Adaugă mișcare")),
  predare.join(" · "));

await shot(page, "paleta_actiuni");

// Enter pe ea duce pe Mișcări **cu formularul deschis**, iar parametrul se consumă — ca `?miscare=`,
// altfel un refresh ar redeschide dialogul peste ce lucrezi.
await page.evaluate(() => {
  [...document.querySelectorAll('div[role="option"]')]
    .find((o) => o.textContent.includes("Adaugă mișcare"))
    .click();
});
await page.waitForTimeout(1400);
const dupaActiune = await page.evaluate(() => ({
  cale: location.pathname,
  adresa: location.search,
  dialog: !!document.querySelector('div[role="dialog"][aria-modal="true"]'),
}));
check("acțiunea duce pe Mișcări", dupaActiune.cale === "/miscari", dupaActiune.cale);
check("cu formularul deschis", dupaActiune.dialog);
check("iar `?nou=1` se consumă", !dupaActiune.adresa.includes("nou"), dupaActiune.adresa || "(gol)");

// Nicio comandă nu scrie nimic: „regenerează" ar fi rescris tăcut liniile unui an (decizia 51).
await page.keyboard.press("Escape");
await page.waitForTimeout(500);
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
const regen = await paleta("regenereaza");
check("nicio comandă nu recalculează un dosar din paletă",
  !regen.some((r) => /Regenerea/i.test(r)), regen.join(" · ") || "(niciun rezultat)");
await inchidePaleta();

await browser.close();
console.log("");
console.log(
  fails === 0
    ? "REZULTAT: atașamentele, formularul de partener, cuprinsul și paleta trec"
    : `REZULTAT: ${fails} eșecuri`
);
process.exit(fails === 0 ? 0 : 1);
