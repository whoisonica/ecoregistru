// Proba 15: jurnalul de audit — cine, ce, când (P1.11).
//
// Jurnalul e singura felie din aplicaţie care se poate strica **fără să se vadă**: o modificare
// neînregistrată arată exact ca una care n-a avut loc, iar diferenţa se descoperă abia când cineva
// deschide ecranul ca să răspundă la o întrebare de la un control. De aceea proba nu se uită la
// „e un tabel acolo": face o modificare adevărată prin API şi apoi cere jurnalului să o povestească.
//
// Ce afirmă, în ordine: (1) o modificare de cantitate ajunge în jurnal, cu autorul, cu ambele
// valori şi cu numele rubricii în româneşte; (2) ştergerea se scrie ca ştergere, nu ca un boolean
// întors; (3) ecranul din Setări o arată; (4) jurnalul e al administratorului — un operator
// primeşte uşa închisă, nu un tabel gol.
//
// ⚠️ Lasă în urmă o mişcare ştearsă (moale) la fiecare rulare, pe un an îndepărtat ales ca să nu
// atingă nimic din ce citesc celelalte probe. Nu curăţă după ea: ştergerea e chiar fapta probată,
// iar o curăţare ar însemna încă un rând de jurnal despre proba însăşi.
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

// --------------------------------------------------- O faptă adevărată, făcută prin API
const story = await page.evaluate(async () => {
  const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
  const json = async (url, init = {}) => {
    const res = await fetch(url, { ...init, headers: { ...auth, ...(init.headers || {}) } });
    return res.ok ? res.json() : null;
  };
  const workPoints = await json("/api/v1/work-points");
  const codes = await json("/api/v1/waste-codes?q=" + encodeURIComponent("20 01 01"));
  const wasteCodeId = (codes || []).find((c) => c.code === "20 01 01")?.id;
  const workPointId = workPoints?.[0]?.id;
  if (!wasteCodeId || !workPointId) return null;

  const body = (quantity) =>
    JSON.stringify({
      workPointId,
      date: "2033-07-14",
      wasteCodeId,
      quantity,
      unit: "KG",
      physicalState: "SOLID",
      operation: "GENERATED",
      notes: "proba 15 — jurnal de audit",
    });

  const created = await json("/api/v1/movements", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: body(4.25),
  });
  if (!created) return null;
  await json("/api/v1/movements/" + created.id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: body(8.75),
  });
  await fetch("/api/v1/movements/" + created.id, { method: "DELETE", headers: auth });

  const page1 = await json("/api/v1/audit-log?entityId=" + created.id + "&size=50");
  return { id: created.id, rows: page1?.content ?? [] };
});

if (!story) {
  console.log("  ——   nu s-a putut scrie mişcarea de probă: proba n-are pe ce rula.");
  await browser.close();
  process.exit(2);
}

const actions = story.rows.map((r) => r.action);
check("cele trei fapte sunt în jurnal", story.rows.length === 3, actions.join(", "));
check("cea mai nouă e prima", actions[0] === "DELETE", actions.join(" ← "));
check(
  "ştergerea e scrisă ca ştergere, nu ca „deleted: false → true”",
  actions.includes("DELETE") && !actions.includes("UPDATE_DELETED"),
  actions.join(", ")
);

const update = story.rows.find((r) => r.action === "UPDATE");
const quantity = update?.changes?.find((c) => c.field === "quantity");
check("modificarea numeşte rubrica", Boolean(quantity), JSON.stringify(update?.changes ?? []));
check(
  "şi poartă amândouă valorile",
  Boolean(quantity) && quantity.from.startsWith("4.2") && quantity.to.startsWith("8.7"),
  quantity ? `${quantity.from} → ${quantity.to}` : ""
);
check(
  "`updatedAt` şi `version` nu apar ca modificări",
  (update?.changes ?? []).every((c) => c.field !== "updatedAt" && c.field !== "version"),
  (update?.changes ?? []).map((c) => c.field).join(", ")
);
check(
  "fiecare rând are un autor",
  story.rows.every((r) => r.actorEmail === "admin@demo.ro" && r.actorRole === "ADMIN"),
  story.rows.map((r) => r.actorEmail).join(", ")
);
check(
  "şi o etichetă care spune despre ce rând e vorba",
  story.rows.every((r) => (r.label || "").includes("2033-07-14")),
  story.rows.map((r) => r.label).join(" | ")
);

// --------------------------------------------------- Ecranul
await page.goto(BASE + "/setari", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);

const screen = await page.evaluate(() => {
  const section = document.querySelector("#jurnal-audit");
  if (!section) return null;
  const nav = [...document.querySelectorAll('a[href^="#"]')].map((a) => a.getAttribute("href"));
  return {
    titlu: section.querySelector("h2")?.textContent?.trim() ?? "",
    randuri: section.querySelectorAll("tbody tr").length,
    text: section.textContent.replace(/\s+/g, " "),
    inCuprins: nav.includes("#jurnal-audit"),
  };
});

check("secţiunea există în Setări", screen !== null);
if (screen) {
  check("şi e în cuprinsul paginii", screen.inCuprins, JSON.stringify(screen.titlu));
  check("are rânduri", screen.randuri > 0, screen.randuri + " rânduri");
  check(
    "faptele sunt scrise în româneşte, nu ca în backend",
    /Ştergere|Ștergere/.test(screen.text) && !/DELETE|WasteMovement/.test(screen.text),
    screen.text.slice(0, 120)
  );
  check(
    "iar rubrica schimbată îşi poartă numele românesc",
    screen.text.includes("Cantitate"),
    ""
  );
  check(
    "jurnalul îşi spune termenul de păstrare",
    screen.text.includes("art. 48 alin. (5)"),
    ""
  );
}
await shot(page, "15-jurnal-audit");

// --------------------------------------------------- Uşa, nu tabelul gol
await page.evaluate(() => localStorage.clear());
await login(page, "operator");
const asOperator = await page.evaluate(async () => {
  const res = await fetch("/api/v1/audit-log", {
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
  return res.status;
});
check("un operator primeşte uşa închisă, nu un tabel gol", asOperator === 403, "HTTP " + asOperator);

const operatorScreen = await (async () => {
  await page.goto(BASE + "/setari", { waitUntil: "networkidle" });
  await page.waitForTimeout(900);
  return page.evaluate(() => document.querySelector("#jurnal-audit") !== null);
})();
check("şi nu vede secţiunea deloc", operatorScreen === false);

console.log("");
if (fails === 0) console.log("✓ proba 15 trece.");
else console.log(`✗ proba 15: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
