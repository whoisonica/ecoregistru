// Proba 10: banda „Următoarea acțiune" din capul Panoului.
//
// Ce se poate strica aici e o **afirmație**, nu un buton: banda alege un singur lucru din cinci
// surse și îl numește. Trei feluri de a greși, fiecare cu verificarea lui mai jos:
//
//   1. spune „Ești la zi" peste date care încă n-au venit — verdele fals, exact clasa de defect
//      pentru care s-a reparat citirea evidenței pe 07.09;
//   2. alege altceva decât cel mai scump lucru deschis — ordinea e chiar conținutul feliei;
//   3. duce în altă parte decât ce numește, sau duce la anul termenului în loc de anul raportat
//      (decizia 59), adică deschide un dosar gol chiar în ziua depunerii.
//
// Din 16.09.2026 termenele nebifate cu data trecută nu se mai arată nicăieri (api v105), deci
// tenantul demo nu mai are ramura „termene depășite”: sus stă linia fără cod R/D. Verificăm **și** că ce e mai jos nu s-a mutat: banda nu ține locul
// casetei de blocaje, o dublează dinadins, iar dacă una dispare felia s-a înțeles greșit.
import { launch, newPage, login, shot, companies, switchCompany, BASE } from "./lib.mjs";

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
await page.goto(BASE + "/", { waitUntil: "networkidle" });
// Banda tace până vin toate trei sursele; fără așteptarea asta am proba scheletul, nu banda.
await page.waitForFunction(
  () => {
    const b = document.querySelector('[data-testid="next-action"]');
    return b && b.textContent.includes("Următoarea acțiune");
  },
  { timeout: 15000 }
);

const banda = await page.evaluate(() => {
  const b = document.querySelector('[data-testid="next-action"]');
  const link = b.querySelector("a");
  const r = b.getBoundingClientRect();
  const stare = document.querySelector("h2, h3");
  return {
    text: b.textContent.replace(/\s+/g, " ").trim(),
    href: link?.getAttribute("href") ?? null,
    cta: link?.textContent.trim() ?? "",
    inaltime: Math.round(r.height),
    sus: Math.round(r.top),
    // Câte rânduri ocupă eticheta linkului: două înseamnă butonul rupt, defectul din 07.09 și 08.09.
    inaltimeLink: link ? Math.round(link.getBoundingClientRect().height) : 0,
    titluStare: stare?.textContent.trim() ?? "",
    rosu: b.className.includes("border-state-bad"),
  };
});

// ------------------------------------------------------ 1. BANDA EXISTĂ ȘI SPUNE UN SINGUR LUCRU
check("panoul are banda de acțiune", banda.text.includes("Următoarea acțiune"));
check("banda numește un lucru, nu cinci", banda.text.split("Următoarea acțiune").length === 2);
check("și are un drum către el", Boolean(banda.href), banda.href ?? "(niciunul)");
// Din 15.09.2026 drumul e un buton de 40px („Cântar”), nu un link de text: un rând înseamnă ≤ 44px.
check("eticheta linkului rămâne pe un rând", banda.inaltimeLink > 0 && banda.inaltimeLink <= 44,
  banda.inaltimeLink + "px");

// ------------------------------------------------------ 2. ALEGE CEL MAI SCUMP LUCRU DESCHIS
// Pe tenantul demo termenele AFM trecute au rămas în bază, nebifate. Banda nu are voie să le
// scoată la iveală pe altă cale decât pagina de Termene, care le ascunde.
const dala = await page.evaluate(
  () => document.querySelector('[data-testid="stat-deadlines"]')?.textContent.replace(/\s+/g, " ").trim() ?? ""
);
// „3 termene depășite" de la felia de numeral din 09.09 — înainte scria „3 depășite". Regexul
// prinde amândouă formele de plural, plus singularul: cu un singur termen depășit dala scrie
// „1 termen depășit", iar o probă care cere „depășite" ar trece de la sine pe zero.
const depasite = Number(/(\d+)(?: de)? termen[e]? depășit/.exec(dala)?.[1] ?? 0);
// Seederul pune termene AFM trecute și nebifate; ele nu se mai numără și nu se mai numesc.
check("termenele trecute nebifate nu mai apar ca depășite pe dală", depasite === 0, depasite + " depășite");
check("banda nu le mai numește", !/depășit/.test(banda.text), banda.text.slice(0, 90));
check("și nu vorbește despre altceva în același timp",
  !/așteaptă cântarul/.test(banda.text) && !/Ești la zi/.test(banda.text));

// ------------------------------------------------------ 3. DRUMUL DUCE UNDE SPUNE
// Un termen depășit duce fie la documentul care îl stinge (pe anul **raportat**), fie la Termene —
// niciodată la un document pe care nu-l tipărim (contribuțiile AFM, decizia 59).
const dus = banda.href;
await page.goto(BASE + dus, { waitUntil: "networkidle" });
await page.waitForTimeout(600);
const ajuns = await page.evaluate(() => ({
  url: location.pathname + location.search,
  titlu: document.querySelector("h1")?.textContent.trim() ?? "",
}));
check("clicul ajunge unde promite banda", ajuns.url === dus, ajuns.url + " ← " + dus);
check("și pe un ecran real, nu pe 404", ajuns.titlu.length > 0 && !/negăsit/i.test(ajuns.titlu),
  ajuns.titlu);
if (/an=/.test(dus)) {
  const an = Number(/an=(\d+)/.exec(dus)[1]);
  check("dacă duce la un document, îl deschide pe anul raportat", an < new Date().getFullYear(),
    String(an));
}

// ------------------------------------------------------ 4. CE ERA MAI JOS N-A DISPĂRUT
// Banda **dublează** dinadins caseta de blocaje: una spune ce se face acum, cealaltă tot ce e de
// lămurit. Dacă una a înlocuit-o pe cealaltă, felia s-a înțeles greșit.
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(900);
const panou = await page.evaluate(() => {
  const txt = document.body.textContent.replace(/\s+/g, " ");
  const dale = [...document.querySelectorAll("div")].filter(
    (d) => d.children.length === 0 && /^(Termene de făcut)$/.test(d.textContent.trim())
  ).length;
  return {
    areStarea: /Starea evidenței pe \d{4}/.test(txt),
    areDale: dale,
    areTermene: /Termene următoare/.test(txt),
    areAutorizatii: /Autorizații care expiră curând/.test(txt),
    // Numărul de mișcări s-a întors lângă kilograme, de unde ieșise pe 08.09.
    // Numeralul are trei forme: „pe 1 mișcare", „pe 3 mișcări", „pe 20 de mișcări" (`countOf`).
    numaraMiscari:
      /pe \d+ (de )?mișc(are|ări) din luna aceasta|nicio mișcare înregistrată luna aceasta/.test(txt),
  };
});
check("caseta «Starea evidenței» a rămas", panou.areStarea);
// „Coduri cu stoc” a ieșit dinadins pe 16.09.2026 (V58): pe Anexa 1 stocul e zero prin construcție.
check("dala de termene a rămas", panou.areDale === 1, panou.areDale + " din 1");
check("listele au rămas", panou.areTermene && panou.areAutorizatii);
check("dala de cantitate spune și pe câte mișcări", panou.numaraMiscari);

// ------------------------------------------------------ 5. BANDA DEPINDE DE DATE, NU E UN ȘIR FIX
// Verificările de până aici ar trece la fel de bine dacă banda ar scrie mereu același lucru: pe
// tenantul demo ramura de sus e adevărată. Se comută pe o firmă fără termene generate — deci pe
// altă ramură — și se cere ca propoziția să **se schimbe**. E aceeași lecție ca la paletă pe 08.09
// (numără, nu doar căuta) și ca la restrângerea de pe 08.09 (probează și cazul celălalt).
await login(page, "platform");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
const firme = (await companies(page)).map((nume) => ({ nume }));
const alta = firme.find((f) => !/Demo Reciclare/.test(f.nume));
check("există o a doua firmă pe care să se probeze cealaltă ramură", Boolean(alta),
  alta?.nume ?? "(niciuna)");
if (alta) {
  await switchCompany(page, new RegExp(alta.nume.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  await page.waitForTimeout(1500);
  await page.waitForFunction(
    () => {
      const b = document.querySelector('[data-testid="next-action"]');
      return b && b.textContent.includes("Următoarea acțiune");
    },
    { timeout: 15000 }
  );
  const bandaAlta = await page.evaluate(() => {
    const b = document.querySelector('[data-testid="next-action"]');
    return {
      text: b.textContent.replace(/\s+/g, " ").trim(),
      href: b.querySelector("a")?.getAttribute("href") ?? null,
      // Tonul spune ce fel de afirmație face banda: chenarul roșu = ceva curge (LED-ul „Acum”),
      // chihlimbar = curând, neutru = nimic de făcut. În „Cântar” tonul stă pe chenar, nu pe fond.
      rosu: b.className.includes("border-state-bad"),
      verde: !b.className.includes("border-state-bad") && !b.className.includes("border-state-warn"),
    };
  });
  check("pe altă firmă banda spune altceva", bandaAlta.text !== banda.text,
    bandaAlta.text.slice(0, 80));
  check("și își schimbă și tonul, nu doar cifra", bandaAlta.rosu !== banda.rosu,
    bandaAlta.rosu ? "roșu" : bandaAlta.verde ? "verde" : "chihlimbar");
  check("iar drumul rămâne unul real", Boolean(bandaAlta.href), bandaAlta.href ?? "(niciunul)");
  await shot(page, "10-panou-actiune-alta-firma");
}

// ------------------------------------------------------ 6. NU SE DERULEAZĂ LATERAL PE TELEFON
// Banda are un link în dreapta și un text lung în stânga — exact geometria care a rupt lucruri de
// trei ori (07.09 cuprinsul, 08.09 butonul de acțiune din Termene).
await login(page, "admin");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(900);
await page.setViewportSize({ width: 375, height: 780 });
await page.waitForTimeout(700);
const ingust = await page.evaluate(() => ({
  scroll: document.documentElement.scrollWidth - document.documentElement.clientWidth,
  bandaLata: Math.round(
    document.querySelector('[data-testid="next-action"]')?.getBoundingClientRect().width ?? 0
  ),
}));
check("pagina nu se derulează lateral la 375px", ingust.scroll <= 0, ingust.scroll + "px");
check("banda intră în ecran", ingust.bandaLata > 0 && ingust.bandaLata <= 375, ingust.bandaLata + "px");
await shot(page, "10-panou-actiune-telefon");
await page.setViewportSize({ width: 1440, height: 900 });
await page.waitForTimeout(500);
await shot(page, "10-panou-actiune");

// ------------------------------------------------------ 7. GENERARE: TABUL „TOTALUL ANULUI"
// Ecranul „Evidențe" a fost scos pe 18.09.2026: agrega exact mișcările registrului Anexa 1, adică
// exact rândurile ecranului „Generare". Ce avea numai el — totalul anului pe cod, pentru depunerea
// din 15 martie — a devenit al doilea tab de acolo, iar adresa veche redirectează.
//
// Panoul apare numai când anul are linii de evidență calculate. Pe o bază proaspătă nimeni n-a
// apăsat încă „Recalculează", deci proba o face singură — altfel ar trece pe lângă tabel fără să-l
// vadă. Regenerarea rescrie un cache, nu date, deci nu lasă nimic în urmă pentru celelalte probe.
const ANUL_TOTAL = 2026;
await page.goto(BASE + "/generare", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.evaluate(async (an) => {
  await fetch("/api/v1/evidences/regenerate?year=" + an, {
    method: "POST",
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
}, ANUL_TOTAL);

// Adresa veche duce la tab, nu la un 404: linkurile din mailuri și din notițe rămân bune.
await page.goto(BASE + "/evidente?an=" + ANUL_TOTAL, { waitUntil: "networkidle" });
await page.waitForTimeout(1000);
const dupaRedirect = await page.evaluate(() => location.pathname + location.search);
check("adresa veche /evidente duce pe tabul totalului",
  dupaRedirect.startsWith("/generare") && dupaRedirect.includes("tab=total"), dupaRedirect);

const taburi = await page.evaluate(() =>
  [...document.querySelectorAll('[role="tab"]')].map((t) => ({
    text: t.textContent.replace(/\s+/g, " ").trim(),
    ales: t.getAttribute("aria-selected") === "true",
  }))
);
// Trei de pe 18.09.2026: „Ambalaje" a venit după „Totalul anului", din același motiv — își ținea
// un al doilea registru al acelorași mișcări.
check("ecranul are trei taburi", taburi.length === 3, JSON.stringify(taburi));
// Banda de taburi nu e o cutie care derulează: `overflow-x` o face și pe verticală `auto`, iar
// butoanele ies 1px în jos (`-mb-px`, ca subliniera să acopere chenarul) — atât i-a trebuit ca
// macOS să deseneze un fir gri lipit după ultimul tab, luat drept buton (18.09.2026).
const bandaTaburi = await page.evaluate(() => {
  const t = document.querySelector('[role="tablist"]');
  if (!t) return null;
  const c = getComputedStyle(t);
  return {
    x: c.overflowX,
    y: c.overflowY,
    deruleaza: t.scrollHeight > t.clientHeight || t.scrollWidth > t.clientWidth,
  };
});
// Se cere `overflow: visible` pe amândouă axele, nu absența depășirii: cei 1px în jos rămân
// (`-mb-px` e chiar rostul lor), dar pe o cutie `visible` niciun browser nu desenează indicator.
check("iar banda lor nu e o cutie care derulează, deci n-are fir gri după ultimul tab",
  bandaTaburi !== null && bandaTaburi.x === "visible" && bandaTaburi.y === "visible",
  JSON.stringify(bandaTaburi));
check("iar cel ales e tabul totalului", taburi.find((t) => t.ales)?.text === "Totalul anului",
  JSON.stringify(taburi));

// Cifrele: proba le recalculează din API, pe drumul ei — nu cheamă `formatKg`, altfel ar greși la
// fel ca ecranul — și compară cod cu cod. O împărțire la 1000 rămasă undeva cade aici.
const total = await page.evaluate(async (an) => {
  const res = await fetch("/api/v1/evidences?year=" + an, {
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
  const linii = await res.json();
  const kg = {};
  for (const r of linii) {
    const k = (kg[r.wasteCode] ??= [0, 0, 0]);
    k[0] += r.totalGenerated;
    k[1] += r.totalRecovered;
    k[2] += r.totalDisposed;
  }
  const tabel = document.querySelector('[role="tabpanel"] table') ?? document.querySelector("table");
  if (!tabel) return null;
  const antet = [...tabel.querySelectorAll("thead th")].map((th) => th.textContent.trim());
  // Rândul de total, citit întreg: eticheta lui trebuie să numere TOATE codurile anului, nu câte
  // se văd pe pagină.
  const randTotal = [...tabel.querySelectorAll("tbody tr")].pop()?.textContent.replace(/\s+/g, " ").trim() ?? "";
  // Ultimul rând e totalul general („5 coduri de deșeu"), nu un cod — nu intră la socoteală.
  const randuri = [...tabel.querySelectorAll("tbody tr")]
    .map((tr) => {
      const td = [...tr.querySelectorAll("td")];
      const cod = td[0]?.querySelector("span")?.textContent.trim() ?? "";
      return { cod, cifre: td.slice(1, 4).map((c) => c.textContent.trim()) };
    })
    .filter((r) => /^\d/.test(r.cod) && r.cod.includes(" "));
  return { kg, antet, randuri, randTotal };
}, ANUL_TOTAL);
check("tabelul totalului există", total !== null);
if (total) {
  const coduri = Object.keys(total.kg);
  // Zece coduri pe pagină (18.09.2026: „să se vadă primele intrări, să nu se lungească pe tot
  // ecranul"). Pe baza demo sunt sub zece, deci aici se verifică plafonul, nu a doua pagină;
  // paginarea cu adevărat plină s-a probat cu mâna, pe 18 coduri — vezi `README.md`.
  check("arată primele coduri ale anului, cel mult zece pe pagină",
    total.randuri.length === Math.min(coduri.length, 10) && coduri.length > 0,
    `${total.randuri.length} rânduri, ${coduri.length} coduri în ${ANUL_TOTAL}`);
  // Rândul de total e al ANULUI, nu al paginii: altfel, pe pagina a doua, cifra din SIM ar fi alta.
  check("iar rândul de total numără toate codurile anului, nu câte se văd",
    total.randTotal.startsWith(`${coduri.length} coduri de deșeu`)
      || total.randTotal.startsWith(`${coduri.length} cod de deșeu`),
    `${total.randTotal.slice(0, 40)} · ${coduri.length} coduri`);
  check("și o coloană de stare, ca să știi dacă se poate depune",
    total.antet.includes("Stare"), total.antet.join(" | "));
  const gresite = [];
  for (const { cod, cifre } of total.randuri) {
    cifre.forEach((text, i) => {
      // „1.234,5" → 1234.5: punctul grupează, virgula e zecimala.
      const valoare = Number(text.replace(/\./g, "").replace(",", "."));
      const asteptat = total.kg[cod]?.[i] ?? NaN;
      if (Math.abs(valoare - asteptat) > 0.0005) gresite.push(`${cod}[${i}] ${text} ≠ ${asteptat}`);
    });
  }
  check("fiecare cifră e exact kilogramele din API", gresite.length === 0,
    gresite.slice(0, 3).join(" | ") || `${total.randuri.length * 3} cifre`);
  // La generator nu există stoc (proprietarul, 18.09.2026; aceeași decizie ca `V58`): coloana a
  // ieșit, iar tabelul are cinci capete — cod, generat, valorificat, eliminat, stare.
  check("și nicio coloană de stoc — generatorul n-are stoc",
    !total.antet.some((h) => /stoc/i.test(h)) && total.antet.length === 5, total.antet.join(" | "));
  // Garda: o probă care compară numai zerouri n-ar vedea un factor greșit.
  check("și cel puțin o cifră e peste 1 kg",
    total.randuri.some((r) => r.cifre.some((c) => Number(c.replace(/\./g, "").replace(",", ".")) >= 1)));
}

// Documentele stau lângă cifrele din care ies; rezumatele neoficiale, într-un meniu care spune ce
// sunt. Cele cinci butoane de altădată, toate la fel de vizibile, erau felul în care cineva depune
// hârtia greșită.
const documente = await page.evaluate(() => {
  const panou = document.querySelector('[role="tabpanel"]') ?? document.body;
  return [...panou.querySelectorAll("button")].map((b) => b.textContent.replace(/\s+/g, " ").trim());
});
// Butonul poartă numele documentului, nu „Descarcă" (18.09.2026): două butoane la fel de anonime,
// unul lângă altul, sunt felul în care cineva depune hârtia greșită.
check("fișa și centralizata își poartă numele pe buton",
  documente.some((b) => /^Evidența gestiunii deșeurilor$/.test(b))
    && documente.some((b) => /^Evidența centralizată$/.test(b)), documente.join(" | "));

// Sus, nu sub tabel (proprietarul, 18.09.2026: „butoanele de evidențele gestiunii sus, nu ascunse
// jos"). Cu douăzeci de coduri, un buton de sub tabel e sub marginea ecranului. Se compară locul în
// DOM, nu pixelii: o probă pe coordonate ar fi trecut și cu tabelul gol.
const ordinea = await page.evaluate(() => {
  const panou = document.querySelector('[role="tabpanel"]') ?? document.body;
  const buton = [...panou.querySelectorAll("button")]
    .find((b) => /^Evidența gestiunii deșeurilor$/.test(b.textContent.trim()));
  const tabel = panou.querySelector("table");
  if (!buton || !tabel) return null;
  // DOCUMENT_POSITION_FOLLOWING = tabelul vine DUPĂ buton.
  return { inainte: Boolean(buton.compareDocumentPosition(tabel) & Node.DOCUMENT_POSITION_FOLLOWING) };
});
check("iar documentele stau deasupra tabelului, nu sub el",
  ordinea !== null && ordinea.inainte, JSON.stringify(ordinea));
check("iar exporturile generice stau în meniu",
  documente.some((b) => /Alte descărcări/.test(b)) && !documente.some((b) => /^Rezumat (Excel|PDF)$/.test(b)),
  documente.join(" | "));

// „Recalculează acum" e singurul buton de aici care SCHIMBĂ date — rescrie evidența, și anii de
// după, fiindcă stocul se reportează. Stă ultimul și cu fundal stins (`variant="muted"`), ca să nu
// fie apăsat pe 15 martie de cineva care crede că scoate un document. Proba cere ca el să nu aibă
// chenarul celor două evidențe oficiale.
const recalc = await page.evaluate(() => {
  const panou = document.querySelector('[role="tabpanel"]') ?? document.body;
  const nume = (b) => b.textContent.replace(/\s+/g, " ").trim();
  const buton = [...panou.querySelectorAll("button")].find((b) => /^Recalculează acum$/.test(nume(b)));
  const fisa = [...panou.querySelectorAll("button")]
    .find((b) => /^Evidența gestiunii deșeurilor$/.test(nume(b)));
  if (!buton || !fisa) return null;
  const c = getComputedStyle(buton);
  return {
    ultimul: Boolean(fisa.compareDocumentPosition(buton) & Node.DOCUMENT_POSITION_FOLLOWING),
    altChenar: c.borderColor !== getComputedStyle(fisa).borderColor,
    altFundal: c.backgroundColor !== getComputedStyle(fisa).backgroundColor,
  };
});
check("«Recalculează acum» stă după documente și nu arată ca ele",
  recalc !== null && recalc.ultimul && recalc.altChenar && recalc.altFundal, JSON.stringify(recalc));

await page.evaluate(() => {
  [...document.querySelectorAll("button")]
    .find((b) => /Alte descărcări/.test(b.textContent))
    ?.click();
});
await page.waitForTimeout(400);
const meniu = await page.evaluate(() => {
  const m = document.querySelector('[role="menu"]');
  return {
    deschis: Boolean(m),
    itemi: m ? [...m.querySelectorAll('[role="menuitem"]')].map((i) => i.textContent.trim()) : [],
  };
});
check("meniul se deschide", meniu.deschis);
check("și conține exact cele două exporturi", meniu.itemi.length === 2, meniu.itemi.join(" | "));
await shot(page, "10-generare-totalul-anului");

// Iar descărcarea chiar pleacă — un meniu care arată bine și nu descarcă nimic e mai rău decât
// cinci butoane.
const descarcare = page.waitForEvent("download", { timeout: 20000 }).catch(() => null);
await page.evaluate(() => {
  [...document.querySelectorAll('[role="menuitem"]')]
    .find((i) => /Rezumat Excel/.test(i.textContent))
    ?.click();
});
const fisier = await descarcare;
check("iar «Rezumat Excel» chiar descarcă", Boolean(fisier),
  fisier ? fisier.suggestedFilename() : "(nimic)");

// ------------------------------------------------------ 8. PARTENERI: SUGESTIA NU MAI MUTĂ COVORUL
// Apăsai pe firma sugerată și același dialog devenea „Editează partener": tot ce completasei
// dispărea, iar singurul semn era titlul. Două drumuri de probat, fiindcă sunt două tratamente:
// pe un formular în care nu e decât numele se comută pe loc și banda o spune; pe unul în care s-au
// completat și alte rubrici, se întreabă întâi.
async function deschideAdaugaPartener() {
  await page.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
  await page.waitForTimeout(800);
  await page.evaluate(() => {
    [...document.querySelectorAll("button")]
      .find((b) => b.textContent.includes("Adaugă partener"))
      ?.click();
  });
  await page.waitForTimeout(400);
}

// (a) doar numele tastat → se comută direct, dar cu banda care spune ce s-a întâmplat
await deschideAdaugaPartener();
await page.fill("#p-name", "Colector");
await page.waitForTimeout(400);
const sugestii = await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  const li = [...d.querySelectorAll("ul li button")].map((b) => b.textContent.trim());
  return li;
});
check("sugestia de duplicat apare de la două litere", sugestii.length > 0, sugestii.join(" | "));

await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  d.querySelector("ul li button")?.click();
});
await page.waitForTimeout(500);
const dupaComutare = await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  return {
    titlu: d?.querySelector("h2, h3")?.textContent.trim() ?? "",
    text: d?.textContent.replace(/\s+/g, " ") ?? "",
    nume: d?.querySelector("#p-name")?.value ?? "",
  };
});
check("pe un formular cu doar numele se comută pe loc, fără întrebare",
  /Editează/.test(dupaComutare.titlu), dupaComutare.titlu);
check("dar banda spune că s-a comutat",
  /Editezi un partener care există deja/.test(dupaComutare.text));
check("și de unde ai venit", /din sugestia de duplicat/.test(dupaComutare.text));
check("iar numele e al partenerului deschis, nu ce tastasei",
  dupaComutare.nume !== "Colector" && dupaComutare.nume.length > 0, dupaComutare.nume);
await shot(page, "10-parteneri-comutare");

// și drumul înapoi pune la loc ce tastasei
await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  [...d.querySelectorAll("button")].find((b) => /Înapoi la adăugare/.test(b.textContent))?.click();
});
await page.waitForTimeout(400);
const inapoi = await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  return {
    titlu: d?.querySelector("h2, h3")?.textContent.trim() ?? "",
    nume: d?.querySelector("#p-name")?.value ?? "",
    fataBanda: /Editezi un partener care există deja/.test(d?.textContent ?? ""),
  };
});
check("«Înapoi la adăugare» revine la adăugare", /Adaugă/.test(inapoi.titlu), inapoi.titlu);
check("cu numele tastat pus la loc", inapoi.nume === "Colector", inapoi.nume);
check("și fără banda de comutare", !inapoi.fataBanda);
await page.keyboard.press("Escape");
await page.waitForTimeout(300);

// (b) formular cu mai mult decât numele → se întreabă întâi
await deschideAdaugaPartener();
await page.fill("#p-name", "Colector");
await page.fill("#p-cui", "RO12345678");
await page.waitForTimeout(400);
await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  d.querySelector("ul li button")?.click();
});
await page.waitForTimeout(500);
const intrebare = await page.evaluate(() => {
  const dialoguri = [...document.querySelectorAll('div[role="dialog"][aria-modal="true"]')];
  const txt = dialoguri.map((d) => d.textContent).join(" ");
  return {
    intreaba: /Deschizi fișa partenerului existent/.test(txt),
    spuneCeSePierde: /nu se salvează/.test(txt),
    // Formularul de dedesubt e încă cel de adăugare: întrebarea n-a comutat nimic încă.
    incaAdauga: dialoguri.some((d) => /Adaugă partener/.test(d.querySelector("h2, h3")?.textContent ?? "")),
  };
});
check("pe un formular început se întreabă întâi", intrebare.intreaba);
check("și întrebarea spune ce se pierde", intrebare.spuneCeSePierde);
check("iar până la răspuns nu s-a comutat nimic", intrebare.incaAdauga);
await shot(page, "10-parteneri-intrebare");

// Escape peste întrebare închide doar întrebarea — teancul din Dialog (07.09).
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
const dupaEsc = await page.evaluate(() => {
  const dialoguri = [...document.querySelectorAll('div[role="dialog"][aria-modal="true"]')];
  return {
    cate: dialoguri.length,
    cui: document.querySelector("#p-cui")?.value ?? "",
  };
});
check("Escape închide doar întrebarea, nu și formularul", dupaEsc.cate === 1, dupaEsc.cate + " dialog(uri)");
check("iar ce tastasei e încă acolo", dupaEsc.cui === "RO12345678", dupaEsc.cui);
await page.keyboard.press("Escape");
await page.waitForTimeout(300);

// ---------------------------------------------- 9. BADGE-UL DE AUTORIZAȚIE DUCE LA PARTENER
// „Autorizație expirată" spunea, în chiar textul lui, „actualizeaz-o în fișa lui" — și nu ducea
// nicăieri. Al doilea fund de sac, după cel roșu reparat pe 07.09.
await page.goto(BASE + "/miscari?luna=2026", { waitUntil: "networkidle" });
await page.waitForTimeout(1500);
const badge = await page.evaluate(() => {
  const a = [...document.querySelectorAll("a")].find((x) =>
    /Autorizație expirată/.test(x.textContent)
  );
  if (!a) return null;
  return {
    href: a.getAttribute("href"),
    text: a.textContent.replace(/\s+/g, " ").trim(),
    // Motivul întreg rămâne pentru cititorul de ecran, fiindcă `Tooltip` nu poate înveli un link.
    aria: (a.getAttribute("aria-label") ?? "").slice(0, 60),
  };
});
check("există un rând cu autorizație expirată la data predării", Boolean(badge),
  badge?.text ?? "(niciunul)");
if (badge) {
  check("badge-ul e un link către partener", /\/parteneri\?partener=/.test(badge.href), badge.href);
  check("și scrie data pe ecran, nu doar în spatele unui hover", /\d{2}\.\d{2}\.\d{4}/.test(badge.text),
    badge.text);
  check("iar motivul rămâne pentru cititorul de ecran", /Autorizația de mediu/.test(badge.aria),
    badge.aria);

  await page.goto(BASE + badge.href, { waitUntil: "networkidle" });
  await page.waitForTimeout(1500);
  const fisa = await page.evaluate(() => {
    const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
    return {
      deschis: Boolean(d),
      titlu: d?.querySelector("h2, h3")?.textContent.trim() ?? "",
      nume: d?.querySelector("#p-name")?.value ?? "",
      // Parametrul se consumă la deschidere, ca `?miscare=`: un refresh n-ar trebui să-l redeschidă.
      url: location.pathname + location.search,
    };
  });
  check("linkul deschide chiar fișa partenerului", fisa.deschis && /Editează/.test(fisa.titlu),
    fisa.titlu + " — " + fisa.nume);
  check("iar parametrul se consumă la deschidere", !/partener=/.test(fisa.url), fisa.url);
  await shot(page, "10-partener-din-badge");
  await page.keyboard.press("Escape");
  await page.waitForTimeout(300);
}

console.log("");
if (fails === 0) console.log("✓ proba 10 trece.");
else console.log(`✗ proba 10: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
