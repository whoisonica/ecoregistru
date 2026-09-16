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
  antet.butoane.some((b) => /Evidența gestiunii deșeurilor generate/.test(b)) &&
    antet.butoane.some((b) => /Evidența gestiunii deșeurilor centralizată/.test(b)),
  antet.butoane.filter((b) => /Evidența gestiunii/.test(b)).join(" | "));
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

// ------------------------------------------------------ 8. EVIDENȚE: CIFRA ÎN TONE (QA-TRACE G6)
// OUG 92/2021 art. 48 alin. (1) cere cantitatea „în tone" la depunerea din 15 martie, iar ecranul o
// calculează din kilograme. O greșeală de 1000× aici n-ar cădea nicăieri altundeva: fișa și
// declarația rămân în kg. Proba recalculează din API, pe drumul ei — nu cheamă `formatTonnes`, altfel
// ar greși la fel ca ecranul — și compară cifra citită de pe pagină, cod cu cod.
// Panoul apare numai când anul are linii de evidență calculate. Pe o bază proaspătă nimeni n-a
// apăsat încă „Regenerează", deci proba o face singură — altfel ar trece pe lângă panou fără să-l vadă.
// Regenerarea rescrie un cache, nu date, deci nu lasă nimic în urmă pentru celelalte probe.
const ANUL_TONE = 2026;
await page.evaluate(async (an) => {
  await fetch("/api/v1/evidences/regenerate?year=" + an, {
    method: "POST",
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
}, ANUL_TONE);
// Panoul stă în vederea lunară: implicitul paginii e „Predări", unde nu există.
await page.goto(BASE + "/evidente?an=" + ANUL_TONE + "&vedere=monthly", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
const tone = await page.evaluate(async () => {
  const titlu = [...document.querySelectorAll("h3")].find((h) => /în tone/.test(h.textContent));
  if (!titlu) return null;
  const an = (titlu.textContent.match(/anului (\d{4})/) ?? [])[1];
  const res = await fetch("/api/v1/evidences?year=" + an, {
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
  const kg = {};
  for (const r of await res.json()) {
    const k = (kg[r.wasteCode] ??= [0, 0, 0]);
    k[0] += r.totalGenerated;
    k[1] += r.totalRecovered;
    k[2] += r.totalDisposed;
  }
  const randuri = [...titlu.closest("div").querySelectorAll("tbody tr")].map((tr) => {
    const td = [...tr.querySelectorAll("td")];
    return { cod: td[0].querySelector("span").textContent.trim(), cifre: td.slice(1, 4).map((c) => c.textContent.trim()) };
  });
  return { an, kg, randuri };
});
check("panoul în tone există pe Evidențe", tone !== null);
if (tone) {
  const coduri = Object.keys(tone.kg);
  check("are câte un rând pentru fiecare cod din evidența anului", tone.randuri.length === coduri.length && coduri.length > 0,
    `${tone.randuri.length} rânduri, ${coduri.length} coduri în ${tone.an}`);
  const gresite = [];
  for (const { cod, cifre } of tone.randuri) {
    cifre.forEach((text, i) => {
      // „1.234,500" → 1234.5: punctul grupează, virgula e zecimala. Trei zecimale, fiindcă a treia e kilogramul.
      const valoare = Number(text.replace(/\./g, "").replace(",", "."));
      const asteptat = (tone.kg[cod]?.[i] ?? NaN) / 1000;
      if (!/,\d{3}$/.test(text) || Math.abs(valoare - asteptat) > 0.0005) gresite.push(`${cod}[${i}] ${text} ≠ ${asteptat}`);
    });
  }
  check("fiecare cifră e kilogramele din API împărțite la 1000, cu trei zecimale", gresite.length === 0,
    gresite.slice(0, 3).join(" | ") || `${tone.randuri.length * 3} cifre`);
  // Garda: o probă care compară numai zerouri n-ar vedea un factor greșit.
  check("și cel puțin o cifră e diferită de zero", tone.randuri.some((r) => r.cifre.some((c) => /[1-9]/.test(c))));
}

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
