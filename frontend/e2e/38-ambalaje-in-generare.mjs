// Proba 38: „Ambalaje” a intrat în „Generare”, iar registrul lui a devenit filtru (18.09.2026).
//
// Ecranul `/ambalaje` își ținea propriul registru de mișcări pe coduri 15 01 xx — aceleași rânduri
// pe care le arată „Generare”, într-un al doilea tabel, fără căutare și fără sortare la server.
// Proprietarul: „să scoatem mișcări din ambalaje și să facem în generare ambalaje, iar în mișcări
// niște filtre frumoase, gen ambalaje sau ambalaj pus de noi pe piață”.
//
// Ce se probează: (1) adresa veche duce la tabul nou și meniul nu mai are intrarea; (2) tabul are
// tabelele și documentul, dar **niciun** registru de mișcări; (3) tastele filtrează chiar la server,
// fiecare altceva; (4) rândul filtrat își spune ambalajul sub cod, fără să lățească tabelul;
// (5) semnalul din tab duce la rândurile lui. Nu lasă nimic în urmă.
import { execFileSync } from "node:child_process";
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

/** Ca la proba 37: `E2E_DB`. Rândul „fără material” e unul vechi, pe care API-ul nu-l mai primește. */
function sql(statement) {
  return execFileSync("psql", ["-h", "localhost", "-U", "eco", "-d", process.env.E2E_DB ?? "ecoregistru", "-tAc", statement], {
    env: { ...process.env, PGPASSWORD: process.env.E2E_DB_PASSWORD ?? "eco" },
    encoding: "utf8",
  }).trim();
}

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

const AN = new Date().getFullYear();

// --------------------------------------------------- (1) ADRESA VECHE ȘI MENIUL
await page.goto(BASE + `/ambalaje?an=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
const adresa = new URL(page.url());
check("„/ambalaje” duce la Generare", adresa.pathname === "/generare", adresa.pathname);
check("… pe tabul Ambalaje", adresa.searchParams.get("tab") === "ambalaje", adresa.search);
check("… cu anul cerut, nu cu luna curentă", adresa.searchParams.get("luna") === String(AN), adresa.search);

const meniu = await page.evaluate(() =>
  [...document.querySelectorAll("nav a")].map((a) => a.getAttribute("href"))
);
check("meniul n-are intrarea „Ambalaje”", !meniu.includes("/ambalaje"), meniu.join(" "));
const taburi = await page.evaluate(() =>
  [...document.querySelectorAll('[role="tablist"] [role="tab"]')].map((b) => b.innerText.trim())
);
check("Generare are trei taburi", taburi.join(" · ") === "Mișcări · Totalul anului · Ambalaje", taburi.join(" · "));

// --------------------------------------------------- (2) TABUL: TABELE, DOCUMENT, FĂRĂ REGISTRU
const text = await page.evaluate(() => document.body.innerText);
// Din 18.09.2026 seara: un singur tabel pe ecran, ales din taste; două butoane de document lipite,
// fără text dedesubt; nimic de derulat la 1440 × 900 (proprietarul: „să nu meargă pagina în jos").
const butoane = await page.evaluate(() =>
  [...document.querySelectorAll('main button[aria-haspopup="menu"]')].map((b) => b.innerText.trim())
);
check("cele două documente sunt butoane cu numele lor",
  butoane.includes("Anexa 1 Ambalaje") && butoane.includes("Anexa 3 Ambalaje"), butoane.join(" · "));
check("… fără explicație sub ele", !/Ce ai pus tu pe piața națională/.test(text));
const tasteTabel = await page.evaluate(() =>
  [...document.querySelectorAll('input[name="tabel-ambalaje"]')].map((i) => i.closest("label").innerText.trim())
);
check("tastele tabelului: Pus pe piață · Predat (+ Preluat de la alții la cine colectează)",
  tasteTabel.slice(0, 2).join(" · ") === "Pus pe piață · Predat" && !tasteTabel.includes("Ieșiri"),
  tasteTabel.join(" · "));
check("un singur tabel pe ecran", (await page.locator("main table").count()) === 1);
const inaltime = await page.evaluate(() => {
  const el = document.getElementById("continut");
  const doc = document.scrollingElement;
  return {
    pagina: Math.max(el.scrollHeight, doc.scrollHeight),
    ecran: Math.max(el.clientHeight, doc.clientHeight),
  };
});
check("la 1440 × 900 pagina nu se derulează", inaltime.pagina <= inaltime.ecran, `${inaltime.pagina}/${inaltime.ecran}`);

// Meniul Anexei 3 își alege punctul de lucru singur: fiecare punct cu cele două formate. La firma
// care colectează și n-a spus ce rol are în lanțul ambalajelor, butonul e stins (nu se tipărește
// nimic — decizia 42), iar motivul se citește pe tasta „Preluat de la alții".
const butonA3 = page.getByRole("button", { name: "Anexa 3 Ambalaje" });
if (await butonA3.isEnabled()) {
  await butonA3.click();
  await page.waitForTimeout(300);
  const meniuA3 = await page.evaluate(() => document.querySelector('[role="menu"]')?.innerText ?? "");
  check("meniul Anexei 3 are formatele pe punct de lucru", /\.xls/.test(meniuA3) && /PDF/.test(meniuA3), meniuA3.replace(/\n/g, " · "));
  await page.keyboard.press("Escape");
} else {
  await page.locator('input[name="tabel-ambalaje"][value="preluat"]').check({ force: true });
  await page.waitForTimeout(800);
  const motiv = await page.evaluate(() => document.getElementById("anexa-3")?.innerText ?? "");
  check("Anexa 3 stinsă își spune motivul pe „Preluat de la alții”", /Nu știm care tabel ți se aplică/.test(motiv), motiv.slice(0, 80));
  check("… iar secțiunea nu mai are butoane de descărcare", !/Descarcă Anexa 3/.test(motiv));
}

// „Predat" schimbă tabelul și scrie alegerea în adresă; tot fără derulare.
await page.locator('input[name="tabel-ambalaje"][value="predat"]').check({ force: true });
await page.waitForTimeout(500);
check("„Predat” stă în adresă", new URL(page.url()).searchParams.get("tabel") === "predat", page.url());
const capPredat = await page.evaluate(() =>
  [...document.querySelectorAll("main table thead th")].map((h) => h.innerText.trim()).join("|")
);
check("… și arată predările, pe operator", /Operatorul care a preluat/i.test(capPredat), capPredat);
const inaltimePredat = await page.evaluate(() => {
  const el = document.getElementById("continut");
  const doc = document.scrollingElement;
  return {
    pagina: Math.max(el.scrollHeight, doc.scrollHeight),
    ecran: Math.max(el.clientHeight, doc.clientHeight),
  };
});
check("… tot fără derulare", inaltimePredat.pagina <= inaltimePredat.ecran, `${inaltimePredat.pagina}/${inaltimePredat.ecran}`);
await page.locator('input[name="tabel-ambalaje"][value=""]').check({ force: true });
await page.waitForTimeout(400);

// Registrul se recunoaște după capul lui: un tabel cu „Data” și „Cod”. Tabelele declarației au
// „Material” pe prima coloană. Se citesc **capetele**, nu textul paginii: „Data” apare și în altă parte.
const capete = await page.evaluate(() =>
  [...document.querySelectorAll("table")].map((t) =>
    [...t.querySelectorAll("thead th")].map((h) => h.innerText.split("\n")[0].trim()).join("|")
  )
);
check("niciun tabel de mișcări pe tab", !capete.some((c) => /^Data\|/.test(c)), capete.join("  ~  "));
await shot(page, "38-tab-ambalaje");

// --------------------------------------------------- (3) TASTELE FILTREAZĂ LA SERVER
// Patru mișcări scrise anume, într-un an în care nu mai e nimic (2033), fiindcă altfel probele ar
// trece din motivul greșit: pe datele demo toate ambalajele sunt și „ale noastre", și incomplete,
// deci un filtru care nu filtrează nimic ar da aceleași cifre ca unul care filtrează tot.
const PROBA_AN = 2033;
const scrise = await page.evaluate(async (an) => {
  const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
  const json = async (url, init = {}) => {
    const res = await fetch(url, { ...init, headers: { ...auth, ...(init.headers || {}) } });
    return res.ok ? res.json() : null;
  };
  const workPointId = (await json("/api/v1/work-points"))?.[0]?.id;
  const codId = async (cod) =>
    ((await json("/api/v1/waste-codes?q=" + encodeURIComponent(cod))) || []).find((c) => c.code === cod)?.id;
  const carton = await codId("15 01 01");
  const metal = await codId("15 01 04");
  const hartie = await codId("20 01 01");
  if (!workPointId || !carton || !metal || !hartie) return null;

  const scrie = (wasteCodeId, quantity, extra) =>
    json("/api/v1/movements", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        workPointId,
        date: `${an}-05-10`,
        wasteCodeId,
        quantity,
        unit: "KG",
        physicalState: "SOLID",
        operation: "RECOVERED",
        register: "ANEXA_1",
        operationCode: "R3",
        storageType: "CT", transportMeans: "AN", physicalState: "SOLID", wasteDestination: "Vr",
        notes: "proba 38 — tastele de ambalaje",
        ...extra,
      }),
    });

  return {
    // completă: 15 01 01 dă singur materialul, felul e răspuns
    completa: await scrie(carton, 100, { packagingCategory: "SECONDARY" }),
    // fără material: 15 01 04 acoperă și aluminiul, și oțelul
    // (din 19.09.2026 serverul nu mai primește rândul fără material: se scrie cu, apoi se golește în bază)
    faraMaterial: await scrie(metal, 200, { packagingCategory: "SECONDARY", packagingMaterial: "OTEL" }),
    // ambalaj pus pe piață de furnizor: e ambalaj, dar nu hrănește Anexa 1
    aAltuia: await scrie(carton, 300, { packagingCategory: "SECONDARY", packagingOnMarket: false }),
    // nu e ambalaj deloc
    neambalaj: await scrie(hartie, 400, {}),
  };
}, PROBA_AN);
if (scrise?.faraMaterial) sql(`update waste_movements set packaging_material = null where id = '${scrise.faraMaterial.id}'`);
check("cele patru mișcări de probă s-au scris", scrise != null && Object.values(scrise).every(Boolean),
  scrise ? Object.keys(scrise).filter((k) => scrise[k]).join(" ") : "niciuna");

async function cuTasta(valoare, an = PROBA_AN) {
  const adresa = valoare ? `/generare?luna=${an}&ambalaje=${valoare}` : `/generare?luna=${an}`;
  await page.goto(BASE + adresa, { waitUntil: "networkidle" });
  await page.waitForTimeout(1000);
  return page.evaluate(() => {
    const tabel = document.querySelector("table");
    const randuri = [...tabel.querySelectorAll("tbody tr")];
    return {
      total: randuri.length,
      coduri: randuri.map((r) => (r.innerText.match(/\d\d \d\d \d\d/) || ["?"])[0]),
      apasata: document.querySelector('[role="radiogroup"] input:checked')?.value ?? "",
      latime: tabel.parentElement.scrollWidth + "/" + tabel.parentElement.clientWidth,
    };
  });
}

const tot = await cuTasta("");
const ambalaje = await cuTasta("toate");
const piata = await cuTasta("piata");
const deCompletat = await cuTasta("de-completat");

check("fără tastă, lista are toate patru", tot.total === 4, String(tot.total));
check("„Ambalaje” scoate ce nu e ambalaj", ambalaje.total === 3, `${ambalaje.total} (aștept 3)`);
check("… și sunt chiar coduri 15 01", ambalaje.coduri.every((c) => c.startsWith("15 01")), ambalaje.coduri.join(" "));
check("„Ambalaj pus de noi pe piață” scoate ambalajul altuia", piata.total === 2, `${piata.total} (aștept 2)`);
check("„De completat” lasă numai rândul fără material", deCompletat.total === 1, `${deCompletat.total} (aștept 1)`);
check("… și ăla e chiar 15 01 04", deCompletat.coduri[0] === "15 01 04", deCompletat.coduri.join(" "));
check("tasta din adresă se vede apăsată", deCompletat.apasata === "de-completat", deCompletat.apasata);

// --------------------------------------------------- (4) RÂNDUL ÎȘI SPUNE AMBALAJUL, FĂRĂ SĂ SE LĂȚEASCĂ
check("tabelul rămâne întreg la 1440px", deCompletat.latime.split("/")[0] === deCompletat.latime.split("/")[1],
  deCompletat.latime);
const subCod = await page.evaluate(() =>
  document.querySelector("table tbody tr td:nth-child(2)")?.innerText.replace(/\n/g, " · ") ?? ""
);
check("rândul de completat spune ce-i lipsește, sub cod", /Fără material/.test(subCod), subCod);
await cuTasta("");
const subCodFaraTasta = await page.evaluate(() =>
  document.querySelector("table tbody tr td:nth-child(2)")?.innerText ?? ""
);
check("fără tastă, rândul nu poartă nimic despre ambalaj",
  !/Fără material|Fără felul ambalajului|Din cod, neconfirmat/.test(subCodFaraTasta),
  subCodFaraTasta.replace(/\n/g, " · "));

// Curățenie: anul 2033 rămâne gol, ca înainte de probă.
const sterse = await page.evaluate(async (ids) => {
  const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
  let n = 0;
  for (const id of ids) {
    const res = await fetch("/api/v1/movements/" + id, { method: "DELETE", headers: auth });
    if (res.ok) n++;
  }
  return n;
}, Object.values(scrise ?? {}).map((m) => m?.id).filter(Boolean));
check("mișcările de probă s-au șters după ele", sterse === 4, `${sterse} șterse`);

// --------------------------------------------------- (5) SEMNALUL DIN TAB DUCE LA RÂNDURILE LUI
await page.goto(BASE + `/generare?tab=ambalaje&luna=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(1000);
// Semnalul e chiar linkul (un rând, nu o cutie cu „Vezi mișcările").
const link = page.getByRole("link", { name: /fără (felul|materialul) ambalajului/ }).first();
check("semnalul are un drum spre mișcări", (await link.count()) > 0);
if (await link.count()) {
  await link.click();
  await page.waitForTimeout(1200);
  const dupa = new URL(page.url());
  check("… care duce pe lista filtrată", dupa.searchParams.get("ambalaje") === "de-completat", dupa.search);
  const apasata = await page.evaluate(
    () => document.querySelector('[role="radiogroup"] input:checked')?.value ?? ""
  );
  check("… cu tasta apăsată acolo", apasata === "de-completat", apasata);
}

// --------------------------------------------------- 375px
const telefon = await newPage(browser, { width: 375, height: 800 });
await login(telefon, "admin");
await telefon.goto(BASE + `/generare?luna=${AN}&ambalaje=toate`, { waitUntil: "networkidle" });
await telefon.waitForTimeout(1200);
const ingust = await telefon.evaluate(() => ({
  pagina: document.scrollingElement.scrollWidth + "/" + document.scrollingElement.clientWidth,
  taste: document.querySelectorAll('[role="radiogroup"] label').length,
}));
check("pe telefon pagina nu derulează lateral", ingust.pagina.split("/")[0] === ingust.pagina.split("/")[1], ingust.pagina);
check("… și tastele sunt toate acolo", ingust.taste === 4, String(ingust.taste));
await shot(telefon, "38-taste-375");

if (page.problems.length) console.log("  probleme:", page.problems);
if (telefon.problems.length) console.log("  probleme (375):", telefon.problems);
console.log("");
if (fails === 0) console.log("✓ proba 38 trece.");
else console.log(`✗ proba 38: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
