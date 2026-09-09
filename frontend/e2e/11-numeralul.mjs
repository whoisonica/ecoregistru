// Proba 11: numeralul românesc, pe toate ecranele deodată.
//
// Regula are **trei** forme, nu două: `1 linie` · `2 linii` · **`20 de linii`**. „De" intră când
// ultimele două cifre sunt 0 sau ≥ 20, și se întoarce la fiecare sută — `101 linii`, dar
// `120 de linii`. Ecranul a scris „1 linii" luni întregi și s-a văzut abia uitându-mă la o
// captură, cu toate verificările verzi (08.09.2026); backendul reparase deja aceeași greșeală pe
// 06.09, la mailul de expirare.
//
// **De ce proba asta nu seamănă cu celelalte.** Celelalte întreabă dacă un anume text e acolo.
// Asta n-are voie: un șir nou scris mâine ar trece pe lângă orice listă fixă, exact cum au trecut
// cele de pe Ambalaje pe lângă felia din 08.09. Deci se **citește tot ce scrie pe ecran**, se
// culeg toate perechile «număr + substantiv cunoscut», și se verifică forma fiecăreia. Ce se rupe
// aici nu e un ecran anume, ci regula — deci un ecran adăugat mâine intră singur sub ea.
//
// Nu se plânge de un substantiv necunoscut: lista de mai jos e ce **știm** că se numără. Cine
// adaugă un substantiv nou îl trece aici, și de-atunci e păzit peste tot.
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

/**
 * Substantivele care se numără în aplicație, cu perechea singular → plural.
 *
 * <p>Sunt cele care apar după `countOf` sau după un `{count}`, plus câteva pe care le scrie textul
 * de lege citat în interfață („3 ani", „12 luni") — alea trebuie să treacă, nu să pice, și trec
 * fiindcă sunt scrise corect.
 */
const SUBSTANTIVE = [
  ["linie", "linii"],
  ["mișcare", "mișcări"],
  ["preluare", "preluări"],
  ["zi", "zile"],
  ["cod", "coduri"],
  ["termen", "termene"],
  ["fișier", "fișiere"],
  ["rând", "rânduri"],
  ["operațiune", "operațiuni"],
  ["partener", "parteneri"],
  ["lună", "luni"],
  ["an", "ani"],
];

/** Regula din `countOf`, scrisă a doua oară dinadins: dacă amândouă greșesc la fel, nu e o probă. */
function formaCeruta(n) {
  if (n === 1) return "singular";
  const ultimele = Math.abs(n) % 100;
  return ultimele === 0 || ultimele >= 20 ? "plural cu «de»" : "plural";
}

/**
 * Culege din textul unei pagini toate perechile «număr (+ „de") + substantiv cunoscut» și spune,
 * pentru fiecare, ce formă s-a scris.
 *
 * <p>Numărul se citește pe ultimele două cifre, deci un separator de mii („1.234 de linii") nu
 * schimbă nimic: 34 e tot 34. Un „de" între cifră și substantiv se prinde explicit, altfel
 * „20 de linii" ar fi arătat ca un „20" fără substantiv și ar fi scăpat.
 */
function culege(text, substantive) {
  const alternative = substantive.flat().join("|");
  const re = new RegExp(String.raw`(\d[\d.,]*)\s+(de\s+)?(${alternative})\b`, "gu");
  const gasite = [];
  for (const m of text.matchAll(re)) {
    const cifre = m[1].replace(/[^\d]/g, "");
    if (cifre === "") continue;
    const n = Number(cifre.slice(-4));
    const areDe = Boolean(m[2]);
    const cuvant = m[3];
    const pereche = substantive.find((p) => p.includes(cuvant));
    const esteSingular = pereche[0] === cuvant && pereche[0] !== pereche[1];
    gasite.push({
      bucata: m[0],
      n,
      scris: areDe ? "plural cu «de»" : esteSingular ? "singular" : "plural",
    });
  }
  return gasite;
}

/**
 * Cuvintele care **nu au ce căuta** în aceeaşi propoziţie cu un „1 <substantiv la singular>".
 *
 * <p>De ce există lista asta. Culegerea de mai sus vede substantivul şi atât — dar în jurul cifrei
 * mai stau un verb şi un pronume, şi ele se acordă la fel. Pe 09.09.2026, cu numărătorile deja
 * reparate şi cu toate verificările verzi, ecranul de Ambalaje scria **„1 mişcare încă de cântărit
 * — cantitatea LOR lipseşte"**, iar cel de Mişcări „1 fişier N-AU urcat". S-au văzut uitându-mă la
 * captură, exact ca „1 linii" cu o zi înainte. Deci acelaşi defect, cu o zi mai târziu şi un
 * cuvânt mai încolo.
 *
 * <p>E o euristică, nu o regulă de gramatică: raportează propoziţia întreagă, ca să se poată citi
 * dacă e o greşeală adevărată. Ce nu e — un citat de lege, o enumerare — se scoate de aici cu
 * motivul scris.
 */
const NUMAI_LA_PLURAL = [/\blor\b/, /\bn-au\b/, /\bnu au\b/, /\bele\b/, /\bastea\b/, /\bacestea\b/];

/** Propoziţiile unei pagini, nelipite între ele: fiecare rând de listă rămâne al lui. */
function propozitii(textBrut) {
  return textBrut
    .split(/\n+/)
    .flatMap((rand) => rand.split(/(?<=[.!?;])\s+/))
    .map((t) => t.trim())
    .filter(Boolean);
}

/** Propoziţiile în care o numărătoare la singular stă lângă un cuvânt care cere plural. */
function dezacorduri(textBrut, substantive) {
  const singulare = substantive.map(([unu]) => unu).join("|");
  const unSingur = new RegExp(String.raw`\b1\s+(${singulare})\b`, "u");
  return propozitii(textBrut).filter(
    (fraza) => unSingur.test(fraza) && NUMAI_LA_PLURAL.some((re) => re.test(fraza))
  );
}

// ------------------------------------------------------ 0. REGULA, ÎNAINTE DE ECRANE
// Dacă `formaCeruta` de aici nu e regula adevărată, tot restul probei minte în liniște.
check("regula: 1 e singular", formaCeruta(1) === "singular");
check("regula: 2 și 19 sunt plural simplu",
  formaCeruta(2) === "plural" && formaCeruta(19) === "plural");
check("regula: 20 și 100 cer «de»",
  formaCeruta(20) === "plural cu «de»" && formaCeruta(100) === "plural cu «de»");
check("regula: 101 se întoarce la plural simplu, 120 nu",
  formaCeruta(101) === "plural" && formaCeruta(120) === "plural cu «de»");
check("culegerea vede și forma cu «de»",
  culege("20 de linii", SUBSTANTIVE)[0]?.scris === "plural cu «de»");
check("și n-o confundă cu pluralul simplu",
  culege("2 linii", SUBSTANTIVE)[0]?.scris === "plural");
check("iar mia nu schimbă ultimele două cifre",
  culege("1.234 de linii", SUBSTANTIVE)[0]?.n === 1234);
check("euristica de acord prinde chiar propoziția găsită pe captură",
  dezacorduri("1 mișcare încă de cântărit — cantitatea lor lipsește.", SUBSTANTIVE).length === 1);
check("și nu se plânge de forma reparată",
  dezacorduri("1 mișcare încă de cântărit — cantitatea lipsește.", SUBSTANTIVE).length === 0);
check("nici de un plural adevărat",
  dezacorduri("3 mișcări încă de cântărit — cantitatea lor lipsește.", SUBSTANTIVE).length === 0);

await login(page, "admin");

// ------------------------------------------------------ 1. TOATE ECRANELE, PE RÂND
// Ordinea e cea din bara laterală. Anul e cel al datelor demo, nu cel curent, iar Mișcările se
// cer pe **tot anul** (`?luna=2026`), nu pe luna curentă: ecranele astea trei sunt goale pe
// septembrie, iar un ecran gol n-are ce număra — adică ar trece din motivul greșit.
//
// `AN` se citește o dată, din datele care chiar sunt acolo. Scris de mână, ar fi o probă care
// începe să treacă pe gol la 1 ianuarie, fără ca nimeni s-o atingă.
const AN = new Date().getFullYear();
const ECRANE = [
  ["/", "Panou"],
  [`/miscari?luna=${AN}`, "Mișcări"],
  [`/evidente?an=${AN}`, "Evidențe"],
  [`/ambalaje?an=${AN}`, "Ambalaje"],
  ["/parteneri", "Parteneri"],
  ["/termene", "Termene"],
  ["/dosar-control", "Dosar de control"],
  ["/setari", "Setări"],
];

/** Ecranele pe care **știm** că sunt numărători: dacă vreunul dă zero, nu s-a citit nimic. */
const CU_NUMARATORI = new Set(["Panou", "Ambalaje", "Termene"]);

let perechi = 0;
const gresite = [];
const goale = [];
const dezacordate = [];

for (const [cale, nume] of ECRANE) {
  await page.goto(BASE + cale, { waitUntil: "networkidle" });
  // Panoul tace până vin toate sursele; celelalte ecrane au tabele care se umplu după prima
  // randare. Fără pauza asta s-ar citi scheletul, adică zero perechi — proba ar trece pe gol.
  await page.waitForTimeout(1200);
  const textBrut = await page.evaluate(() => document.body.innerText);
  const text = textBrut.replace(/\s+/g, " ");
  for (const fraza of dezacorduri(textBrut, SUBSTANTIVE)) {
    dezacordate.push(`${nume}: „${fraza.slice(0, 100)}"`);
  }
  const gasite = culege(text, SUBSTANTIVE);
  perechi += gasite.length;
  for (const g of gasite) {
    if (g.scris !== formaCeruta(g.n)) {
      gresite.push(`${nume}: „${g.bucata}" — trebuia ${formaCeruta(g.n)}`);
    }
  }
  if (CU_NUMARATORI.has(nume) && gasite.length === 0) goale.push(nume);
  check(`${nume} — ${gasite.length} numărători citite`, true,
    gasite.map((g) => g.bucata).slice(0, 4).join(" | "));
}

// Garda care lipsea pe 08.09 la restrângeri: o probă care caută greșeli trece verde și când n-a
// citit nimic. Cele două cifre de mai jos spun că a avut ce citi — și nu doar în total, ci pe
// fiecare ecran despre care știm că numără ceva.
check("ecranele chiar au numărători de verificat, deci proba nu trece pe gol",
  perechi >= 10, perechi + " perechi «număr + substantiv»");
check("și niciun ecran cu numărători cunoscute n-a ieșit gol",
  goale.length === 0, goale.join(", ") || "niciunul gol");
check("toate se acordă", gresite.length === 0, gresite.join(" · ") || "niciuna greșită");
// Al doilea fel de dezacord, cel care nu se vede în substantiv: verbul și pronumele din jurul lui.
check("și nimic din jurul lor nu rămâne la plural",
  dezacordate.length === 0, dezacordate.join(" · ") || "nicio propoziție");

// ------------------------------------------------------ 2. CELE TREI FORME, PE UN ECRAN ADEVĂRAT
// Verificarea de sus găsește ce e **greșit**; asta găsește ce e **absent**. Termenele au zile
// rămase pe fiecare rând nefinalizat, deci acolo se vede că `daysLabel` chiar trece prin `countOf`
// și nu scrie „1 zile" sau „30 zile".
await page.goto(BASE + "/termene", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
const zile = await page.evaluate(() =>
  [...document.querySelectorAll("td")]
    .map((td) => td.textContent.trim())
    .filter((t) => /\bzi\b|\bzile\b/.test(t))
);
check("Termene scrie zilele rămase pe rânduri", zile.length > 0, zile.length + " rânduri");
check("și niciunul nu scrie «1 zile» sau «N zile» fără «de» de la 20 în sus",
  zile.every((t) => {
    const g = culege(t, SUBSTANTIVE);
    return g.every((x) => x.scris === formaCeruta(x.n));
  }),
  zile.slice(0, 3).join(" | "));
await shot(page, "11-termene-zile");

// ------------------------------------------------------ 3. DALA DE TERMENE DE PE PANOU
// Aici stătea „{n} depășite", cu adjectivul neacordat — cazul în care substantivul singur nu
// ajunge, fiindcă și adjectivul trebuie să se miște cu el.
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForFunction(
  () => document.querySelector('[data-testid="next-action"]')?.textContent.includes("Următoarea acțiune"),
  { timeout: 15000 }
);
const dala = await page.evaluate(() => {
  const e = [...document.querySelectorAll("div")].find(
    (d) => d.textContent.trim() === "Termene de făcut" && d.children.length === 0
  );
  return e?.parentElement?.textContent.replace(/\s+/g, " ").trim() ?? "";
});
check("dala de termene spune și substantivul, nu doar adjectivul",
  /termen(e)? depășit(e)?|Următorul termen|Niciun termen/.test(dala), dala.slice(0, 70));
const dalaGasite = culege(dala, SUBSTANTIVE);
check("și se acordă", dalaGasite.every((x) => x.scris === formaCeruta(x.n)),
  dalaGasite.map((x) => x.bucata).join(" | ") || "nimic de numărat");

console.log("");
if (fails === 0) console.log("✓ proba 11 trece.");
else console.log(`✗ proba 11: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
