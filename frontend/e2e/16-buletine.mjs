// Proba 16: buletinele de analiză, pe ecran (G-7).
//
// OUG 92/2021 art. 8 alin. (4) cere caracterizarea deşeurilor periculoase, iar art. 48 alin. (2)
// cere ca buletinul să fie **deţinut** — hârtia, nu declaraţia că există. Felia s-a livrat cu teste de
// backend (`AnalysisBulletinIT`) şi fără nicio probă de ecran; secţiunea din Setări n-a fost deschisă
// de nicio suită până aici (`QA-TRACE.md`, punctul 9).
//
// Ce se probează: formularul cere **toate patru** rubricile şi le marchează fiecare separat — nu pe
// toate deodată, controlul e o rubrică completată care trebuie să-şi piardă marcajul —, un formular
// gol nu pleacă la server, data nu poate trece de azi nici în rubrică, nici în API, iar un cititor
// vede lista fără butonul de încărcare şi primeşte 403 dacă încearcă pe lângă ecran.
//
// ⚠️ **Ce nu se poate proba aici, şi e scris ca atare, nu ca OK:** istoricul („Anterior" pe al doilea
// buletin al aceluiaşi cod) şi stingerea badge-ului „Cod-oglindă" după încărcare. Amândouă cer o
// încărcare reuşită, iar fişierul urcă direct la Cloudinary — fără `CLOUDINARY_URL` pe backendul local
// nu există cale. Regula de la badge e ţinută de `MirrorWasteCodeIT`; aici ar fi fost doar drumul.
//
// Nu scrie nimic în bază: toate cererile de mai jos sunt refuzate **înainte** de urcare. Singura care
// poate trece e controlul pozitiv, pe un backend care are Cloudinary — atunci buletinul se şterge pe loc.
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
const skip = (name, why) => console.log(`  ——   ${name} — nerulat: ${why}`);

// Aceeaşi zi pe care o calculează rubrica (`toISOString`, deci UTC), ca `max` să se compare cu ea.
const today = new Date().toISOString().slice(0, 10);
const tomorrow = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);

await login(page, "admin");

// ------------------------------------------------------------ API — înaintea ecranului
// `17 05 04` e din nomenclatorul oficial, deci proba nu depinde de ce a seedat cineva.
const api = await page.evaluate(
  async ([today, tomorrow]) => {
    const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
    const list = async () => (await (await fetch("/api/v1/analysis-bulletins", { headers: auth })).json()).length;
    const codes = await (await fetch("/api/v1/waste-codes?q=" + encodeURIComponent("17 05 04"), { headers: auth })).json();
    const wasteCodeId = codes.find((c) => c.code === "17 05 04")?.id;
    if (!wasteCodeId) return null;

    const post = async ({ date, laboratory = "Laborator probă 16", withFile = true }) => {
      const form = new FormData();
      form.append("wasteCodeId", wasteCodeId);
      form.append("issueDate", date);
      form.append("laboratory", laboratory);
      if (withFile) form.append("file", new Blob(["%PDF-1.4 proba 16"], { type: "application/pdf" }), "buletin-proba.pdf");
      const res = await fetch("/api/v1/analysis-bulletins", { method: "POST", headers: auth, body: form });
      const text = await res.text();
      let id = null;
      try { id = JSON.parse(text).id ?? null; } catch { /* corp de eroare sau HTML */ }
      return { status: res.status, text, id };
    };

    const before = await list();
    const future = await post({ date: tomorrow });
    const control = await post({ date: today });
    // Controlul poate chiar încărca pe un backend cu Cloudinary: se şterge pe loc.
    let controlDeleted = null;
    if (control.status === 200 && control.id) {
      controlDeleted = (await fetch("/api/v1/analysis-bulletins/" + control.id, { method: "DELETE", headers: auth })).ok;
    }
    const noFile = await post({ date: today, withFile: false });
    const blankLab = await post({ date: today, laboratory: "   " });
    const after = await list();
    return { before, after, future, control, controlDeleted, noFile, blankLab };
  },
  [today, tomorrow]
);

if (!api) {
  console.log("  ——   n-am găsit codul 17 05 04 în nomenclator: proba n-are pe ce rula.");
  await browser.close();
  process.exit(1);
}

const has = (r, code) => r.text.includes(code);
check("API: data în viitor e refuzată cu motivul ei",
  api.future.status === 400 && has(api.future, "bulletin.issue.date.future"), `${api.future.status}`);
// Controlul pozitiv: acelaşi corp, data de azi. Dacă şi el ar lua codul datei, refuzul de mai sus n-ar
// spune nimic despre dată. Local ajunge până la urcare şi cade acolo (fără Cloudinary) — ce contează
// e că **nu** mai e refuzat pentru dată.
check("API: controlul — aceeaşi cerere cu data de azi nu e refuzată pentru dată",
  !has(api.control, "bulletin.issue.date.future"),
  `${api.control.status}${api.control.status === 200 ? " (încărcat şi şters: " + api.controlDeleted + ")" : " — local se opreşte la urcare, fără Cloudinary"}`);
check("API: fără fişier e refuzat cu motivul lui",
  api.noFile.status === 400 && has(api.noFile, "bulletin.file.required"), `${api.noFile.status}`);
check("API: laboratorul gol e refuzat cu motivul lui",
  api.blankLab.status === 400 && has(api.blankLab, "bulletin.laboratory.required"), `${api.blankLab.status}`);
check("API: nimic scris", api.after === api.before, `${api.before} → ${api.after}`);

// ------------------------------------------------------------ ECRAN — secţiunea
await page.goto(BASE + "/setari#buletine-analiza", { waitUntil: "networkidle" });
await page.waitForTimeout(800);

const section = page.locator("section#buletine-analiza");
check("secţiunea există în Setări", (await section.count()) === 1);
check("secţiunea are titlul ei", ((await section.locator("h2").first().textContent()) ?? "").includes("Buletine de analiză"));
check("nota despre valabilitate e pe ecran",
  ((await section.textContent()) ?? "").includes("nu are termen de valabilitate"));

const addButton = section.getByRole("button", { name: "Încarcă buletin" }).first();
check("un admin vede butonul de încărcare", (await addButton.count()) > 0);

// ------------------------------------------------------------ FORMULARUL GOL
await addButton.click();
await page.waitForTimeout(500);
const dialog = page.locator('div[role="dialog"][aria-modal="true"]');
check("butonul deschide formularul", ((await dialog.textContent()) ?? "").includes("Încarcă buletin de analiză"));

const posted = [];
const onRequest = (r) => {
  if (r.method() === "POST" && r.url().includes("/api/v1/analysis-bulletins")) posted.push(r.url());
};
page.on("request", onRequest);

await page.click('button[type="submit"][form="bulletin-form"]');
await page.waitForTimeout(500);

const marks = () => page.evaluate(() =>
  Object.fromEntries(["bl-code-err", "bl-date-err", "bl-lab-err", "bl-file-err"].map((id) => [
    id.replace("bl-", "").replace("-err", ""),
    (document.getElementById(id)?.textContent ?? "").trim(),
  ]))
);
const empty = await marks();
check("formularul gol marchează toate patru rubricile",
  Object.values(empty).every((m) => m === "Câmp obligatoriu."), JSON.stringify(empty));

const linked = await page.evaluate(() =>
  ["bl-date", "bl-lab", "bl-file"].map((id) => {
    const el = document.getElementById(id);
    const to = el?.getAttribute("aria-describedby");
    return el?.getAttribute("aria-invalid") === "true" && !!to && !!document.getElementById(to);
  })
);
check("fiecare rubrică marcată e legată de mesajul ei", linked.every(Boolean), JSON.stringify(linked));
check("formularul gol nu pleacă la server", posted.length === 0, `${posted.length} cereri POST`);
await shot(page, "16-buletine-formular-gol");

// Controlul: o singură rubrică completată trebuie să-şi piardă marcajul, iar celelalte să şi-l ţină.
// Un formular care marchează orice la orice trimitere ar trece verificarea de mai sus.
await page.fill("#bl-lab", "Laborator probă 16");
await page.click('button[type="submit"][form="bulletin-form"]');
await page.waitForTimeout(500);
const partial = await marks();
check("controlul: laboratorul completat nu mai e marcat", partial.lab === "", JSON.stringify(partial));
check("controlul: celelalte trei rămân marcate",
  partial.code !== "" && partial.date !== "" && partial.file !== "", JSON.stringify(partial));
check("nici acum nu pleacă nimic la server", posted.length === 0, `${posted.length} cereri POST`);
page.off("request", onRequest);

// ------------------------------------------------------------ DATA
const dateLimit = await page.evaluate((tomorrow) => {
  const el = document.getElementById("bl-date");
  const max = el?.getAttribute("max");
  el.value = tomorrow;
  return { max, overflow: el.validity.rangeOverflow };
}, tomorrow);
check("rubrica de dată nu trece de azi", dateLimit.max === today, `max = ${dateLimit.max}`);
check("o dată de mâine e peste limita rubricii", dateLimit.overflow === true, JSON.stringify(dateLimit));

await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// ------------------------------------------------------------ CITITORUL
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const viewer = await ctx.newPage();
  await login(viewer, "viewer");
  await viewer.goto(BASE + "/setari#buletine-analiza", { waitUntil: "networkidle" });
  await viewer.waitForTimeout(800);
  const vSection = viewer.locator("section#buletine-analiza");
  check("cititorul vede secţiunea", (await vSection.count()) === 1);
  check("cititorul nu vede butonul de încărcare",
    (await vSection.getByRole("button", { name: "Încarcă buletin" }).count()) === 0);
  const door = await viewer.evaluate(async (today) => {
    const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
    const read = (await fetch("/api/v1/analysis-bulletins", { headers: auth })).status;
    const form = new FormData();
    form.append("wasteCodeId", "00000000-0000-0000-0000-000000000000");
    form.append("issueDate", today);
    form.append("laboratory", "x");
    form.append("file", new Blob(["x"], { type: "application/pdf" }), "x.pdf");
    const write = (await fetch("/api/v1/analysis-bulletins", { method: "POST", headers: auth, body: form })).status;
    return { read, write };
  }, today);
  check("cititorul citeşte lista", door.read === 200, `${door.read}`);
  check("cititorul care încearcă pe lângă ecran primeşte 403", door.write === 403, `${door.write}`);
  await ctx.close();
}

// ------------------------------------------------------------ CE NU SE POATE PROBA LOCAL
const noStorage = "cere o încărcare reuşită, iar backendul local n-are CLOUDINARY_URL";
skip("al doilea buletin pe acelaşi cod apare ca „Anterior”, nu îl înlocuieşte pe primul", noStorage);
skip("badge-ul „Cod-oglindă” se stinge după încărcarea buletinului pe cod", noStorage + " (regula: MirrorWasteCodeIT)");

await browser.close();
console.log("");
console.log(fails === 0 ? "REZULTAT: buletinele trec (2 verificări nerulate, scrise mai sus)" : `REZULTAT: ${fails} eșecuri`);
process.exit(fails === 0 ? 0 : 1);
