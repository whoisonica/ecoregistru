// Proba 53: trei din cele șase defecte mici ale depozitului (27.09.2026), cele care se văd doar în browser.
//
// Ce apără: (3) tabul „Transferuri” arată zece rânduri pe pagină, nu 25 — cu 11 transferuri noi pagina întâi are 10 și
// „Înainte” duce la a doua; (6) un transfer spre un depozit dezactivat după plecare își arată destinația cu
// „(dezactivat)”, nu „Alege depozitul”; (5) garda la închidere: dialogul de cântar neatins se închide la Escape fără
// întrebare, cu o observație scrisă întreabă „Închizi fără să salvezi?”, iar dialogul de inventar întreabă la fel, și
// la schimbarea pasului cu o declarație nesalvată întreabă „Treci la alt pas fără să salvezi?”; după „Salvează” pasul
// se schimbă fără întrebare.
//
// ⚠️ Lasă în urmă depozitele „Proba 53 A/B <număr>” (dezactivate), 11 transferuri anulate și un inventar anulat.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const stamp = Date.now() % 100000;
const A = `Proba 53 A ${stamp}`;
const B = `Proba 53 B ${stamp}`;
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const today = iso(new Date());
const api = (page, method, url, body) =>
  page.evaluate(
    async ({ method, url, body }) => {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem("eco_token") },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = (res.headers.get("content-type") ?? "").includes("json") ? await res.text() : "";
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    { method, url, body }
  );
const dialogs = (page) => page.locator('div[role="dialog"]');
const top = (page) => dialogs(page).last();
const hasText = async (page, re) => re.test((await page.textContent("body")) ?? "");

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const a = (await api(page, "POST", "/api/v1/work-points", { name: A, address: "Str. Probei 53, Cluj" })).json;
const b = (await api(page, "POST", "/api/v1/work-points", { name: B, address: "Str. Probei 53, Turda" })).json;
const made = [];
for (let i = 0; i < 11; i++) {
  const r = await api(page, "POST", "/api/v1/weighing-operations", {
    type: "TRANSFER",
    workPointId: a.id,
    targetWorkPointId: b.id,
    date: today,
    notes: `Proba 53 #${i + 1}`,
  });
  if (r.status === 200 || r.status === 201) made.push(r.json);
}
check("cele 11 transferuri se creează", made.length === 11, `${made.length}`);

// 3 — zece rânduri pe pagină.
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.click('[role="tab"]:has-text("Transferuri")');
await page.waitForTimeout(900);
const rows = await page.locator("tbody tr").count();
check("pagina întâi are 10 rânduri", rows === 10, `${rows}`);
const next = page.locator('button:text-is("Înainte")');
check("„Înainte” duce la a doua pagină", (await next.count()) === 1 && (await next.isEnabled()));
const height = await page.evaluate(() => document.documentElement.scrollHeight - window.innerHeight);
check("tabul nu derulează la 1440×900", height <= 0, `${height}px`);
await shot(page, "53-transferuri-10");

// 5 — garda la cântar: neatins se închide, cu o observație întreabă.
await page.click(`tbody tr:has-text("${A}") button:has-text("Deschide")`);
await page.waitForTimeout(900);
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
check("dialogul neatins se închide la Escape", (await dialogs(page).count()) === 0);
await page.click(`tbody tr:has-text("${A}") button:has-text("Deschide")`);
await page.waitForTimeout(900);
await top(page).locator("textarea").last().fill("scris și nesalvat");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
check("cu o observație scrisă, Escape întreabă", await hasText(page, /Închizi fără să salvezi\?/));
await shot(page, "53-garda-cantar");
await top(page).locator('button:has-text("Închide, fără să salvez")').click();
await page.waitForTimeout(500);
check("„Închide, fără să salvez” închide dialogul", (await dialogs(page).count()) === 0);

// 6 — destinația dezactivată rămâne pe transfer.
const off = await api(page, "DELETE", `/api/v1/work-points/${b.id}`);
check("depozitul B se dezactivează", off.status < 300, `HTTP ${off.status}`);
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.click('[role="tab"]:has-text("Transferuri")');
await page.waitForTimeout(900);
await page.click(`tbody tr:has-text("${A}") button:has-text("Deschide")`);
await page.waitForTimeout(1000);
const target = await page.evaluate(() => {
  const s = document.querySelector("#wo-target");
  return s ? s.options[s.selectedIndex]?.textContent ?? "" : null;
});
check("destinația arată „B (dezactivat)”", target?.includes(B) && target.includes("(dezactivat)"), String(target));
await shot(page, "53-destinatie-dezactivata");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// 5 — garda la inventar: la închidere și la schimbarea pasului.
await page.click('[role="tab"]:has-text("Inventar")');
await page.waitForTimeout(600);
await page.selectOption("#inv-depot", { label: A });
await page.waitForTimeout(900);
await page.click('button:has-text("Inventar nou")');
await page.waitForTimeout(600);
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
check("inventarul nou, neatins, se închide la Escape", (await dialogs(page).count()) === 0);
await page.click('button:has-text("Inventar nou")');
await page.waitForTimeout(600);
await page.fill("#inv-dn", "53");
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
check("inventarul cu decizia scrisă întreabă la Escape", await hasText(page, /Închizi fără să salvezi\?/));
await top(page).locator('button:has-text("Anulează")').click();
await page.waitForTimeout(400);
check("„Anulează” lasă formularul cum era", (await page.inputValue("#inv-dn")) === "53");
await page.fill("#inv-keeper", "Ion Gestionar");
await top(page).locator('input[placeholder="Nume"]').first().fill("Ana Președinte");
await top(page).locator('button:has-text("Deschide inventarul")').click();
await page.waitForTimeout(1200);
check("inventarul se deschide pe declarație", (await page.locator("#inv-le").count()) === 1);
await page.fill("#inv-le", "Borderou 53");
await top(page).locator('[role="tab"]:has-text("Numărare")').click();
await page.waitForTimeout(400);
check("declarația nesalvată întreabă la schimbarea pasului", await hasText(page, /Treci la alt pas fără să salvezi\?/));
await shot(page, "53-garda-pas");
await top(page).locator('button:has-text("Anulează")').click();
await page.waitForTimeout(300);
await top(page).locator('button:has-text("Salvează")').click();
await page.waitForTimeout(900);
await top(page).locator('[role="tab"]:has-text("Numărare")').click();
await page.waitForTimeout(500);
check("după „Salvează”, pasul se schimbă fără întrebare", !(await hasText(page, /Treci la alt pas fără să salvezi\?/)));
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
check("inventarul salvat se închide fără întrebare", (await dialogs(page).count()) === 0);

// Curățenie.
for (const op of made) await api(page, "POST", `/api/v1/weighing-operations/${op.id}/cancel`, { reason: "Proba 53" });
for (const inv of (await api(page, "GET", `/api/v1/inventories?workPointId=${a.id}`)).json ?? []) {
  await api(page, "POST", `/api/v1/inventories/${inv.id}/cancel`, { reason: "Proba 53" });
}
await api(page, "DELETE", `/api/v1/work-points/${a.id}`);
const problems = [...new Set(page.problems)];
if (problems.length > 0) {
  console.log("  FAIL consola/rețeaua");
  for (const problem of problems) console.log(`         ${problem}`);
  fails += problems.length;
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 53 trece." : `\n✗ Proba 53: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
