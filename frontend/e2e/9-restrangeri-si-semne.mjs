// Proba 9: rubrica ce se cere doar cui i se aplică, grila care spune că a salvat, panoul care
// numără ce se poate număra, termenele cu zilele rămase și documentul lor.
//
// Ce au în comun: niciuna nu repară ceva rupt, deci `tsc` era verde și înainte. Ce se poate strica
// aici e o **afirmație**: o rubrică cerută cuiva care n-o depune niciodată, o cifră care adună
// kilograme peste coduri diferite, un „salvat" pe care nu-l vede nimeni, un termen care nu duce la
// documentul care îl stinge.
import { launch, newPage, login, shot, visible, clickAt, companies, switchCompany, BASE } from "./lib.mjs";

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

// Deschide formularul de partener și spune ce vede în el.
async function formularPartener() {
  await page.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
  await page.waitForTimeout(700);
  await page.evaluate(() => {
    [...document.querySelectorAll("button")].find((b) => b.textContent.includes("Adaugă partener"))?.click();
  });
  await page.waitForTimeout(500);
  const stare = await page.evaluate(() => {
    const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
    if (!d) return null;
    return {
      arataProvenienta: Boolean(d.querySelector("#p-pkg-origin")),
      areTip: Boolean(d.querySelector("#p-type")),
      sectiuni: [...d.querySelectorAll("nav[aria-label] li")].map((li) => li.textContent.trim()),
    };
  });
  await page.keyboard.press("Escape");
  await page.waitForTimeout(300);
  return stare;
}

// ---------------------------------------------- 1. PROVENIENȚA SE CERE CUI I SE APLICĂ
// Rubrica alimentează Anexa 3 Ambalaje (Ordinul 794/2012 art. 4), pe care o depun colectorii,
// comercianții, reciclatorii și valorificatorii. Un generator pur n-o depune niciodată — deci
// întrebarea „ce e partenerul ăsta față de ambalajele pe care ți le aduce" n-are pentru el niciun
// răspuns. Se probează pe amândouă tipurile de cont: o restrângere care ascunde tuturor e un defect
// la fel de mare ca una care nu ascunde nimănui.
await login(page, "platform");

// Lista de firme vine dintr-o interogare proprie: `companies` așteaptă să apară rândurile, altfel
// verificările de mai jos ar trece **fiindcă nu s-a ales nicio firmă** — exact felul de trecere din
// motivul greşit pe care proba 8 l-a prins la paletă.
const firme = await companies(page);
check("administratorul de platformă vede firmele", firme.length >= 2, `${firme.length} firme`);

async function comutaLa(potrivire) {
  const nume = await switchCompany(page, potrivire);
  return nume ? { nume } : null;
}

const generatorPur = await comutaLa(/Proba Automata/);
check("există un cont de generator pur pe care să se probeze", generatorPur !== null,
  generatorPur?.nume ?? "niciunul");
const laGenerator = await formularPartener();
check("formularul de partener se deschide și pe contul de generator", laGenerator !== null);
check("tipul partenerului se cere în continuare", laGenerator?.areTip === true);
check("provenienţa ambalajelor NU se cere unui generator pur", laGenerator?.arataProvenienta === false);

const colector = await comutaLa(/Demo Reciclare/);
check("există contul care chiar depune Anexa 3 Ambalaje", colector !== null, colector?.nume ?? "niciunul");
const laColector = await formularPartener();
check("provenienţa se cere pe un cont care preia de la terţi", laColector?.arataProvenienta === true);
check("şi restul formularului e neatins: patru pași", (laColector?.sectiuni ?? []).length === 4,
  (laColector?.sectiuni ?? []).join(" · "));

await shot(page, "9-partener-colector");

// ------------------------------------------- 2. GRILA DE SUPRASCRIERE SPUNE CE A SALVAT
// Şaizeci şi şase de celule care se salvează la ieşirea din câmp, şi până acum se vedeau **numai
// erorile**: o salvare reuşită nu spunea nimic. Proba merge până la capăt — scrie o cifră, o
// verifică salvată după reîncărcare, apoi **goleşte celula**, fiindcă un rând golit şterge
// suprascrierea (backendul o spune explicit) şi baza de dev rămâne cum a fost găsită.
await login(page, "admin");
await page.goto(BASE + "/generare?tab=ambalaje&luna=2026", { waitUntil: "networkidle" });
await page.waitForTimeout(800);

async function deschideGrila() {
  await page.evaluate(() => {
    [...document.querySelectorAll("button")].find((b) => b.textContent.includes("Scrie cifre proprii"))?.click();
  });
  await page.waitForTimeout(500);
}
await deschideGrila();

const CELULA = 'input[aria-label="Sticlă — Ambalaje de desfacere fabricate/importate"]';
const stareRand = () =>
  page.evaluate((sel) => {
    const input = document.querySelector(sel);
    // Din 18.09.2026 (seara) câmpurile sunt chiar celulele tabelului 1, iar starea rândului stă
    // lângă numele materialului, nu într-o coloană proprie: o coloană în plus lățea tabelul.
    const rand = input?.closest("tr");
    return rand ? (rand.querySelector("[aria-live]")?.textContent.trim() ?? null) : null;
  }, CELULA);

const areCelula = await visible(page, CELULA);
check("grila de suprascriere se deschide şi are celule etichetate", areCelula);
check("… în acelaşi tabel, nu într-o a doua grilă", (await page.locator("main table").count()) === 1);
check("rândul neatins nu spune nimic", (await stareRand()) === "");

await page.fill(CELULA, "12.5");
await page.waitForTimeout(200);
check("ce s-a tastat şi n-a plecat încă se vede ca «nesalvat»", (await stareRand()) === "nesalvat");
const rezumat = await page.evaluate(() =>
  [...document.querySelectorAll("p")].some((p) => /cifre nesalvate/.test(p.textContent))
);
check("şi se numără, pentru rândul derulat afară din ochi", rezumat);
await shot(page, "9-grila-nesalvat");

// Ieşirea din celulă e chiar declanşatorul salvării.
await page.click("h1");
await page.waitForTimeout(1200);
check("după ieşirea din celulă rândul spune «salvat»", (await stareRand()) === "salvat");
const rezumatDupa = await page.evaluate(() =>
  [...document.querySelectorAll("p")].some((p) => /cifre nesalvate/.test(p.textContent))
);
check("iar numărătoarea de nesalvate dispare", !rezumatDupa);
await shot(page, "9-grila-salvat");

// Semnul nu e o promisiune: cifra chiar a ajuns pe server.
await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(800);
// Semnul „scris de tine" se citeşte cu tabelul închis: la scris, locul lui îl ia starea rândului.
const marcatScris = await page.evaluate(() =>
  [...document.querySelectorAll("td")].some(
    (td) => /Sticlă/.test(td.textContent) && /scris de tine/.test(td.textContent)
  )
);
check("iar tabelul 1 marchează rândul ca scris de mână", marcatScris);
await deschideGrila();
const dupaReincarcare = await page.$eval(CELULA, (el) => el.value);
check("şi cifra chiar a ajuns pe server", dupaReincarcare === "12.5", dupaReincarcare);

// Curăţenie: rândul golit şterge suprascrierea, deci baza rămâne cum era.
await page.fill(CELULA, "");
await page.click("h1");
await page.waitForTimeout(1200);
check("golirea rândului se salvează şi ea", (await stareRand()) === "salvat");
await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(800);
await deschideGrila();
const dupaCuratenie = await page.$eval(CELULA, (el) => el.value);
check("iar suprascrierea a dispărut, ca proba să nu lase nimic în urmă", dupaCuratenie === "");
const totMarcat = await page.evaluate(() =>
  [...document.querySelectorAll("td")].some(
    (td) => /Sticlă/.test(td.textContent) && /scris de tine/.test(td.textContent)
  )
);
check("şi rândul nu mai e marcat", !totMarcat);

// ---------------------------------------- 3. PANOUL NU MAI ARE DALĂ DE STOC
// Până pe 16.09.2026 dala „Coduri cu stoc” număra codurile cu stoc din evidența lunară. De atunci
// generarea se scrie numai ca predare (V58) — un generator n-are cântar și nu ține stoc — deci pe
// Anexa 1 stocul iese zero prin construcție, iar o dală care arată mereu „0” ar fi un zgomot.
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
check("panoul nu mai are dala de stoc", (await page.$('[data-testid="stat-stock"]')) === null);
check("dar are dala de termene", (await page.$('[data-testid="stat-deadlines"]')) !== null);
await shot(page, "9-panou-fara-stoc");

// -------------------------------- 4. TERMENELE SPUN CÂTE ZILE MAI SUNT, ŞI DUC LA DOCUMENT
// Panoul socotea zilele de mult; tabelul lăsa clientul s-o facă în cap. Iar niciun termen nu ducea
// la documentul care îl stinge — deşi aplicaţia chiar îl tipăreşte.
// Din 16.09.2026 pagina are „De făcut” (fără an) și „Bifate” (cu an); zilele și documentul stau în prima.
await page.goto(BASE + "/termene", { waitUntil: "networkidle" });
await page.waitForTimeout(1000);

const termene = await page.evaluate(() => {
  const randuri = [...document.querySelectorAll('[data-testid="deadlines-todo"] tbody tr')];
  return randuri.map((tr) => {
    const c = [...tr.querySelectorAll("td")];
    const link = c[3]?.querySelector("a");
    return {
      tip: c[0]?.textContent.trim() ?? "",
      termen: c[1]?.textContent.trim() ?? "",
      stare: c[2]?.textContent.trim() ?? "",
      document: link ? { text: link.textContent.trim(), href: link.getAttribute("href") } : null,
      documentText: c[3]?.textContent.trim() ?? "",
    };
  });
});
check("tabelul de termene are rânduri", termene.length > 0, `${termene.length} rânduri`);

const cuZile = termene.filter((r) => /zile|azi|mâine/.test(r.termen));
check("fiecare termen nefinalizat spune câte zile mai sunt",
  cuZile.length === termene.filter((r) => r.stare !== "Finalizat").length,
  cuZile[0]?.termen.replace(/\s+/g, " "));
// Seederul pune termene trecute nebifate; din 16.09.2026 nu se mai arată nicăieri.
check("niciun termen trecut nebifat în „De făcut”",
  termene.every((r) => r.stare !== "Depășit"),
  termene.find((r) => r.stare === "Depășit")?.termen.replace(/\s+/g, " "));

// „De făcut” nu are an: 15 martie de anul viitor stă aici lângă AFM-ul lunii acesteia.
const anRaportat = new Date().getFullYear();
const anexa1 = termene.find((r) => /Evidența gestiunii/.test(r.tip));
check("termenul de 15 martie duce la evidenţă", anexa1?.document?.href === `/generare?tab=total&luna=${anRaportat}`,
  anexa1?.document?.href ?? anexa1?.documentText);
check("şi duce la anul raportat, nu la anul termenului",
  new RegExp(String(anRaportat)).test(anexa1?.document?.text ?? ""), anexa1?.document?.text);

// Din 16.09.2026, sub document stă starea lui, socotită din evidența anului raportat; din 17.09.2026 în kg (app v101).
check("termenul de 15 martie arată cifrele anului raportat",
  new RegExp(`${anRaportat}: \\d+ (cod|coduri|de coduri), [\\d.,]+ kg`).test(anexa1?.documentText ?? ""),
  anexa1?.documentText);
const anexa3 = termene.find((r) => /^Anexa 3 Ambalaje/.test(r.tip));
check("un colector cu ambalaje preluate primește termenul Anexei 3 (Ordinul 794/2012 art. 4)",
  Boolean(anexa3) && /25\.02\./.test(anexa3.termen), anexa3?.termen);

// Contribuţiile AFM sunt bani declaraţi în aplicaţia AFM: n-avem ce document să oferim, deci nu
// oferim niciunul. Un link către ceva ce nu tipărim ar fi chiar promisiunea goală reparată pe 07.09.
const afm = termene.filter((r) => /^AFM/.test(r.tip));
check("contribuţiile AFM n-au link către un document pe care nu-l tipărim",
  afm.length > 0 && afm.every((r) => r.document === null), `${afm.length} rânduri AFM`);

// Coloana nouă a strâns tabelul, iar butonul de acţiune s-a rupt pe două rânduri — chiar defectul
// văzut pe captură pe 07.09, la inboxul de cereri. Se măsoară, nu se presupune: un rând de tabel
// are ~30px, două ~60px.
const inaltimi = await page.evaluate(() => {
  const celule = [...document.querySelectorAll("tbody tr td:last-child")];
  return {
    buton: Math.max(...celule.map((td) => td.querySelector("button")?.getBoundingClientRect().height ?? 0)),
    badge: Math.max(
      ...[...document.querySelectorAll("tbody tr td:nth-child(3) span")].map(
        (b) => b.getBoundingClientRect().height
      )
    ),
    lateral: document.documentElement.scrollWidth - document.documentElement.clientWidth,
  };
});
check("butonul de acţiune rămâne pe un rând", inaltimi.buton > 0 && inaltimi.buton < 45,
  `${Math.round(inaltimi.buton)}px`);
check("şi badge-ul de status la fel", inaltimi.badge > 0 && inaltimi.badge < 30,
  `${Math.round(inaltimi.badge)}px`);
check("iar pagina nu se derulează lateral", inaltimi.lateral === 0, `${inaltimi.lateral}px`);
await shot(page, "9-termene");

// Linkul chiar deschide ecranul, pe anul din adresă.
await clickAt(page, `[data-testid="deadlines-todo"] table a[href="/generare?tab=total&luna=${anRaportat}"]`);
await page.waitForTimeout(1200);
const dupaClic = await page.evaluate(() => ({
  adresa: location.pathname + location.search,
  an: document.querySelector("#ev-year")?.value ?? document.querySelector("select")?.value ?? "",
}));
check("iar clicul chiar deschide documentul, pe anul lui",
  dupaClic.adresa === `/generare?tab=total&luna=${anRaportat}`, dupaClic.adresa);

// ------------------------- 5. ACTUL DE IDENTITATE AL ŞOFERILOR SPUNE DE CE E ŢINUT
// Singurul dat personal al cuiva din afara firmei pe care aplicaţia îl ţine — şi singurul care se
// tipăreşte. Nota stă în amândouă locurile unde chiar se tastează: „Şoferii noştri" din Setări şi
// fişa partenerului. Un singur loc ar fi însemnat că jumătate din cei care scriu rubrica n-o văd.
await page.goto(BASE + "/setari/soferi", { waitUntil: "networkidle" });
await page.waitForTimeout(900);
const inSetari = await page.evaluate(() => {
  const sectiune = document.querySelector("#soferi");
  return {
    text: sectiune?.textContent ?? "",
    // Nota trebuie să stea sub titlu, la vedere, nu într-un dialog pe care nu-l deschide nimeni.
    inainteDeTabel:
      sectiune && sectiune.querySelector("table")
        ? [...sectiune.querySelectorAll("p")].some((p) => /Date personale/.test(p.textContent))
        : false,
  };
});
check("nota de retenţie stă în „Şoferii noştri", /Date personale/.test(inSetari.text));
check("şi spune termenul, cu temeiul lui",
  /cel puțin 3 ani/.test(inSetari.text) && /art\. 48/.test(inSetari.text));
// Din 14.09.2026 (AO, `V41`) fișa unui șofer dezactivat se poate șterge definitiv, iar datele de pe
// mișcări pleacă singure după trei ani. Nota spune asta; textul vechi („dezactivarea nu îl scoate”) a ieșit.
check("şi spune ce se întâmplă cu fişa unui şofer dezactivat",
  /Fișa unui șofer dezactivat se poate șterge definitiv/.test(inSetari.text));
check("stă la vedere, nu într-un dialog", inSetari.inainteDeTabel);
await shot(page, "9-soferi-nota");

await page.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.evaluate(() => {
  [...document.querySelectorAll("button")].find((b) => b.textContent.includes("Adaugă partener"))?.click();
});
await page.waitForTimeout(400);
// Blocul de şoferi apare doar la un partener care vine el după deșeu — acolo se tastează rubrica.
await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  // Din 17.09.2026 bifa e cardul „Vine el și îl ia”.
  d.querySelector('input[name="p-carrier"][value="yes"]')?.click();
});
await page.waitForTimeout(400);
const inFisa = await page.evaluate(
  () => document.querySelector('div[role="dialog"][aria-modal="true"]')?.textContent ?? ""
);
check("şi în fişa transportatorului, unde se scriu şoferii lui", /Date personale/.test(inFisa));
await page.keyboard.press("Escape");
await page.waitForTimeout(300);

console.log("");
if (fails === 0) console.log("✓ proba 9 trece.");
else console.log(`✗ proba 9: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
