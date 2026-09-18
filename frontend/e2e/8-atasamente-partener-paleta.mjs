// Proba 8: atașamentele citite din tabel, formularul de partener pe secțiuni, cuprinsul fișei de
// firmă și paleta care găsește documente și pornește acțiuni.
//
// ⚠️ Cuprinsul se proba pe „Ambalaje" până pe 18.09.2026, când ecranul a devenit tab în „Generare"
// și cuprinsul lui a plecat cu totul. Primitiva a rămas pe fișa firmei (`/clienti/:id`), deci
// acolo s-a mutat și proba — aceleași trei defecte, altă pagină.
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

// Deschiderea chiar apăsată (todo-lansare, „Interfață”): până aici proba doar număra butoanele. Clicul pe
// fișier trebuie să deschidă un tab cu conținutul adus prin sesiune — adresa `blob:`, nu una de Cloudinary —
// iar poza din el să se fi încărcat. Atașamentele demo trimit spre imagini publice din cloud-ul `demo`, deci
// proba cere rețea; un backend fără ea cade aici cu mesajul de eroare din toast.
const [tab] = await Promise.all([
  page.context().waitForEvent("page", { timeout: 20000 }).catch(() => null),
  clickAt(page, 'div[role="dialog"][aria-modal="true"] li button'),
]);
check("clicul pe fișier deschide un tab", tab !== null);
if (tab) {
  await tab.waitForFunction(() => location.href.startsWith("blob:"), null, { timeout: 20000 }).catch(() => {});
  const deschis = await tab.evaluate(() => ({
    adresa: location.href,
    poza: [...document.images].some((i) => i.complete && i.naturalWidth > 0),
  })).catch(() => ({ adresa: tab.url(), poza: false }));
  check("tabul are conținutul adus prin sesiune (blob:), nu adresa din cloud",
    deschis.adresa.startsWith("blob:"), deschis.adresa.slice(0, 40));
  check("și fișierul chiar s-a deschis (poza încărcată)", deschis.poza);
  await tab.close();
}

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
// Era o coloană de cincisprezece blocuri într-un dialog de 512px, deși are secțiuni evidente. Din
// 17.09.2026 e pe trei pași, ca cererea de cont: cine e · ce face · autorizația.
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
  // `#p-type` e cârligul rubricii tipului, acum carduri: intră în ordine ca o rubrică.
  const rubrici = [...form.querySelectorAll("input, select, textarea, #p-type")].map((el) => el.id).filter(Boolean);
  return {
    latime: Math.round(d.getBoundingClientRect().width),
    pasi: [...d.querySelectorAll("nav[aria-label] li")].map((li) => li.textContent.trim()),
    rubrici,
  };
});
check("formularul are patru pași în cuprins", (partener?.pasi ?? []).length === 4,
  (partener?.pasi ?? []).join(" · "));
check("dialogul s-a lărgit, ca cel de mișcare și cel de firmă", (partener?.latime ?? 0) > 600,
  `${partener?.latime}px`);
// CUI-ul stătea între bifa de transportator și autorizație: o identificare ruptă în două de o
// întrebare despre camioane.
check("CUI-ul e prima rubrică, denumirea imediat după",
  (partener?.rubrici ?? []).indexOf("p-cui") === 0 && (partener?.rubrici ?? []).indexOf("p-name") === 1,
  (partener?.rubrici ?? []).slice(0, 4).join(" · "));
check("autorizația vine după rol și transport",
  (partener?.rubrici ?? []).indexOf("p-auth-number") > (partener?.rubrici ?? []).indexOf("p-type"));

await shot(page, "partener_sectiuni");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// ---------------------------------------------------------- CUPRINSUL, PE FIȘA FIRMEI
// Era pe „Ambalaje", cea mai lungă pagină din aplicație — până pe 18.09.2026, când tabelele ei au
// intrat ca tab în „Generare" și cuprinsul a plecat (un cuprins înăuntrul unui tab e tab în tab).
// Primitiva `SectionNav` a rămas, pe fișa firmei, cu șapte secțiuni; acolo se probează mai departe,
// fiindcă cele trei defecte de mai jos sunt ale ei, nu ale ecranului pe care stătea.
const paginaFirmei = await newPage(browser, { width: 1440, height: 900 });
await login(paginaFirmei, "platform");
const firmaId = await paginaFirmei.evaluate(async () => {
  const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
  const res = await fetch("/api/v1/companies", { headers: auth });
  const list = res.ok ? await res.json() : [];
  return list[0]?.id ?? null;
});
check("s-a găsit o firmă pe care să se deschidă fișa", Boolean(firmaId), firmaId ?? "niciuna");
await paginaFirmei.goto(BASE + "/clienti/" + firmaId, { waitUntil: "networkidle" });
await paginaFirmei.waitForTimeout(1200);

const cuprins = await paginaFirmei.$$eval("nav[aria-label] a[href^='#']", (as) =>
  as.map((a) => ({ href: a.getAttribute("href"), text: a.textContent.trim() }))
);
check("fișa are cuprins", cuprins.length >= 4, cuprins.map((c) => c.text).join(" · "));
check(
  "fiecare intrare are ținta ei",
  cuprins.length > 0 &&
    (await paginaFirmei.evaluate((hs) => hs.every((h) => !!document.querySelector(h)), cuprins.map((c) => c.href))),
  cuprins.map((c) => c.href).join(" ")
);

// Cele trei defecte ale cuprinsului din „Setări" s-au văzut abia pe captură, cu DOM-ul verde:
// coloana de acțiuni a tabelelor e și ea lipicioasă și vine după bară în DOM. Aici se măsoară.
const aDoua = cuprins[1]?.href ?? "";
const ultima = cuprins[cuprins.length - 1]?.href ?? "";
await paginaFirmei.evaluate((sel) => document.querySelector(sel)?.scrollIntoView({ block: "start" }), aDoua);
await paginaFirmei.waitForTimeout(500);
const bara = await paginaFirmei.evaluate(() => {
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
// Se probează pe **a doua** secțiune, nu pe una de la coadă: `scroll-mt` duce secțiunea sub bară
// numai dacă pagina mai are unde curge. Ultimele intră toate în ultimul ecran — clicul pe ele
// ajunge la fundul paginii, nu la titlul lor — și tocmai de asta `SectionNav` are ramura „la fund",
// scrisă după ce prima variantă a fost văzută greșind pe o captură.
await clickAt(paginaFirmei, `nav[aria-label] a[href='${aDoua}']`);
await paginaFirmei.waitForTimeout(900);
const laMijloc = await paginaFirmei.evaluate((sel) => ({
  activ: document.querySelector("nav[aria-label] a[aria-current='true']")?.getAttribute("href"),
  sus: Math.round(document.querySelector(sel).getBoundingClientRect().top),
}), aDoua);
check("clicul duce la secțiune", laMijloc.sus >= 0 && laMijloc.sus < 200, `${laMijloc.sus}px`);
check("și marcajul o urmează", laMijloc.activ === aDoua, laMijloc.activ ?? "niciunul");
// `scroll-mt-24` = 96px. Sub 40 ar însemna că titlul intră pe sub bara lipicioasă.
check("titlul nu intră sub bară", laMijloc.sus > 40, `${laMijloc.sus}px sub antet`);

// Ultima secțiune: marcajul trebuie să ajungă pe ea chiar dacă titlul ei nu urcă niciodată sus —
// pagina se termină înaintea lui. Prima variantă a primitivei marca aici secțiunea de dinainte.
//
// Se probează **ramura „la fund"**, prin derulare până jos, nu prin clic pe ultima intrare: pe o
// pagină destul de lungă clicul îi duce titlul sus, fără să atingă fundul, și atunci verificarea
// ar spune ceva despre lungimea paginii, nu despre cuprins.
await clickAt(paginaFirmei, `nav[aria-label] a[href='${ultima}']`);
await paginaFirmei.waitForTimeout(900);
check("clicul pe ultima intrare mută marcajul pe ea",
  (await paginaFirmei.evaluate(() => document.querySelector("nav[aria-label] a[aria-current='true']")?.getAttribute("href"))) === ultima);

const laCoada = await paginaFirmei.evaluate(async (sel) => {
  const sec = document.querySelector(sel);
  let sc = sec.parentElement;
  while (sc && !(getComputedStyle(sc).overflowY.match(/auto|scroll/) && sc.scrollHeight > sc.clientHeight)) {
    sc = sc.parentElement;
  }
  if (sc) sc.scrollTop = sc.scrollHeight;
  else window.scrollTo(0, document.body.scrollHeight);
  await new Promise((r) => setTimeout(r, 600));
  return {
    activ: document.querySelector("nav[aria-label] a[aria-current='true']")?.getAttribute("href"),
    laFund: sc ? sc.scrollTop + sc.clientHeight >= sc.scrollHeight - 4 : true,
  };
}, ultima);
check("derulat până jos, pagina chiar e la fund", laCoada.laFund);
check("și marcajul e pe ultima secțiune, deși titlul ei nu urcă sus",
  laCoada.activ === ultima, laCoada.activ ?? "niciunul");
await shot(paginaFirmei, "cuprins_fisa_firmei");
await paginaFirmei.close();

await shot(page, "ambalaje_cuprins");

// `min-w-max` pe `<ul>` a făcut o dată pagina să se deruleze lateral cu 129px la 375px.
const telefon = await newPage(browser, { width: 375, height: 812 });
await login(telefon, "admin");
await telefon.goto(BASE + "/generare?tab=ambalaje", { waitUntil: "networkidle" });
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

// Din 18.09.2026 fișa se tipărește de pe „Generare" (tabul „Totalul anului"): ecranul „Evidențe"
// a fost scos, iar cuvintele lui au venit pe intrarea asta.
const fisa = await paleta("fisa");
check("„fisa” găsește Generare", fisa.some((r) => r.includes("Generare")), fisa.join(" · "));
await inchidePaleta();

// „Anexa 1" e numele scurt a două documente diferite (decizia 12): declarația de ambalaje și fișa
// din HG 856/2002. Amândouă apar, cu eticheta lor — a alege una ar ascunde cealaltă de cine o
// caută pe nume.
const anexa1 = await paleta("anexa 1");
check("„anexa 1” găsește amândouă documentele",
  anexa1.some((r) => r.includes("Ambalaje")) && anexa1.some((r) => r.includes("Generare")),
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
check("„predare” găsește acțiunea de adăugare", predare.some((r) => r.includes("Deșeuri proprii")),
  predare.join(" · "));

await shot(page, "paleta_actiuni");

// Enter pe ea duce pe Mișcări **cu formularul deschis**, iar parametrul se consumă — ca `?miscare=`,
// altfel un refresh ar redeschide dialogul peste ce lucrezi.
await page.evaluate(() => {
  [...document.querySelectorAll('div[role="option"]')]
    .find((o) => o.textContent.includes("Deșeuri proprii"))
    .click();
});
await page.waitForTimeout(1400);
const dupaActiune = await page.evaluate(() => ({
  cale: location.pathname,
  adresa: location.search,
  dialog: !!document.querySelector('div[role="dialog"][aria-modal="true"]'),
}));
// Firma demo e „Generator și colector", deci `/miscari?nou=1` ajunge pe primul ei ecran, „Generare".
check("acțiunea duce pe Generare", dupaActiune.cale === "/generare", dupaActiune.cale);
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
