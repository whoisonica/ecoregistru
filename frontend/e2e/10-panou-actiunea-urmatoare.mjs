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
// Tenantul demo are termene AFM depășite din lunile trecute ale anului, deci ramura pe care o
// probăm aici e cea de sus. Verificăm **și** că ce e mai jos nu s-a mutat: banda nu ține locul
// casetei de blocaje, o dublează dinadins, iar dacă una dispare felia s-a înțeles greșit.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

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
  };
});

// ------------------------------------------------------ 1. BANDA EXISTĂ ȘI SPUNE UN SINGUR LUCRU
check("panoul are banda de acțiune", banda.text.includes("Următoarea acțiune"));
check("banda numește un lucru, nu cinci", banda.text.split("Următoarea acțiune").length === 2);
check("și are un drum către el", Boolean(banda.href), banda.href ?? "(niciunul)");
check("eticheta linkului rămâne pe un rând", banda.inaltimeLink > 0 && banda.inaltimeLink <= 28,
  banda.inaltimeLink + "px");

// ------------------------------------------------------ 2. ALEGE CEL MAI SCUMP LUCRU DESCHIS
// Pe tenantul demo sunt termene AFM depășite, deci banda trebuie să le numească pe ele — nu
// cântarul, nu autorizațiile. Dacă ordinea s-ar inversa, exact asta ar trece neobservat.
const dala = await page.evaluate(() => {
  const t = [...document.querySelectorAll("div")].find(
    (d) => d.textContent.trim() === "Termene de făcut" && d.children.length === 0
  );
  return t?.parentElement?.textContent.replace(/\s+/g, " ").trim() ?? "";
});
const depasite = Number(/(\d+) depășite/.exec(dala)?.[1] ?? 0);
check("tenantul demo chiar are termene depășite, deci ramura se probează pe date, nu pe gol",
  depasite > 0, depasite + " depășite");
check("banda le numește pe ele, nu cântarul sau autorizațiile",
  /depășit|depășite/.test(banda.text), banda.text.slice(0, 90));
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
    (d) => d.children.length === 0 && /^(Coduri cu stoc|Termene de făcut)$/.test(d.textContent.trim())
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
check("dalele au rămas", panou.areDale === 2, panou.areDale + " din 2");
check("listele au rămas", panou.areTermene && panou.areAutorizatii);
check("dala de cantitate spune și pe câte mișcări", panou.numaraMiscari);

// ------------------------------------------------------ 5. BANDA DEPINDE DE DATE, NU E UN ȘIR FIX
// Verificările de până aici ar trece la fel de bine dacă banda ar scrie mereu același lucru: pe
// tenantul demo ramura de sus e adevărată. Se comută pe o firmă fără termene generate — deci pe
// altă ramură — și se cere ca propoziția să **se schimbe**. E aceeași lecție ca la paletă pe 08.09
// (numără, nu doar căuta) și ca la restrângerea de pe 08.09 (probează și cazul celălalt).
await login(page, "platform");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForFunction(
  () => document.querySelectorAll("#tenant-switcher option").length > 1,
  { timeout: 15000 }
);
const firme = await page.$$eval("#tenant-switcher option", (os) =>
  os.map((o) => ({ id: o.value, nume: o.textContent.trim() })).filter((o) => o.id)
);
const alta = firme.find((f) => !/Demo Reciclare/.test(f.nume));
check("există o a doua firmă pe care să se probeze cealaltă ramură", Boolean(alta),
  alta?.nume ?? "(niciuna)");
if (alta) {
  await page.selectOption("#tenant-switcher", alta.id);
  await page.waitForTimeout(2500);
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
      // Tonul spune ce fel de afirmație face banda: roșu = ceva curge, verde = nimic de făcut.
      rosu: b.className.includes("red"),
      verde: b.className.includes("emerald"),
    };
  });
  check("pe altă firmă banda spune altceva", bandaAlta.text !== banda.text,
    bandaAlta.text.slice(0, 80));
  check("și își schimbă și tonul, nu doar cifra", bandaAlta.rosu !== banda.text.includes("depășite"),
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

// ------------------------------------------------------ 7. EVIDENȚE: CINCI BUTOANE DEVENITE TREI
// Antetul avea cinci butoane la fel de vizibile — două documente oficiale, două exporturi generice
// pe care scrie „rezumat neoficial", și „Regenerează". Toate cinci arătau ca același fel de lucru,
// iar titlul paginii se strângea pe trei rânduri ca să le facă loc. Exporturile intră într-un meniu.
await page.goto(BASE + "/evidente", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
const antet = await page.evaluate(() => {
  const h1 = document.querySelector("h1");
  const zonaAntet = h1?.closest("div")?.parentElement ?? document.body;
  const butoane = [...zonaAntet.querySelectorAll("button")].map((b) =>
    b.textContent.replace(/\s+/g, " ").trim()
  );
  return {
    butoane,
    // Câte rânduri ocupă titlul: trei însemna că butoanele îl striveau.
    randuriTitlu: h1 ? Math.round(h1.getBoundingClientRect().height / 32) : 0,
    latimeTitlu: h1 ? Math.round(h1.getBoundingClientRect().width) : 0,
  };
});
check("antetul nu mai are cinci butoane deodată", antet.butoane.length <= 4,
  antet.butoane.length + ": " + antet.butoane.join(" | "));
check("cele două documente oficiale au rămas afară",
  antet.butoane.some((b) => /Evidența gestiunii/.test(b)) &&
    antet.butoane.some((b) => /Declarația anuală/.test(b)),
  antet.butoane.filter((b) => /Evidența gestiunii|Declarația anuală/.test(b)).join(" | "));
check("iar exporturile generice au intrat în meniu",
  antet.butoane.some((b) => /Alte descărcări/.test(b)) &&
    !antet.butoane.some((b) => /^Rezumat (Excel|PDF)$/.test(b)));
check("titlul nu se mai strânge pe trei rânduri", antet.randuriTitlu <= 2,
  antet.randuriTitlu + " rânduri, " + antet.latimeTitlu + "px");

// Meniul se deschide, spune ce sunt lucrurile din el, și se închide la Escape — comportamentul e
// împrumutat de la meniul de rând, deci dacă unul se strică se strică amândouă.
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
    spuneCeSunt: m ? /rezumat neoficial/i.test(m.textContent) : false,
  };
});
check("meniul se deschide", meniu.deschis);
check("și conține exact cele două exporturi", meniu.itemi.length === 2, meniu.itemi.join(" | "));
check("și spune pe față că sunt un rezumat neoficial", meniu.spuneCeSunt);
await shot(page, "10-evidente-antet");
await page.keyboard.press("Escape");
await page.waitForTimeout(300);
const dupaEscape = await page.evaluate(() => Boolean(document.querySelector('[role="menu"]')));
check("și Escape îl închide", !dupaEscape);

// Iar descărcarea chiar pleacă — un meniu care arată bine și nu descarcă nimic e mai rău decât
// cinci butoane.
await page.evaluate(() => {
  [...document.querySelectorAll("button")]
    .find((b) => /Alte descărcări/.test(b.textContent))
    ?.click();
});
await page.waitForTimeout(300);
const descarcare = page.waitForEvent("download", { timeout: 20000 }).catch(() => null);
await page.evaluate(() => {
  [...document.querySelectorAll('[role="menuitem"]')]
    .find((i) => /Rezumat Excel/.test(i.textContent))
    ?.click();
});
const fisier = await descarcare;
check("iar «Rezumat Excel» chiar descarcă", Boolean(fisier),
  fisier ? fisier.suggestedFilename() : "(nimic)");

console.log("");
if (fails === 0) console.log("✓ proba 10 trece.");
else console.log(`✗ proba 10: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
