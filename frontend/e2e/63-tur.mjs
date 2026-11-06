// Proba 63 (06.11.2026, turul aplicației): la prima intrare a unui client, turul trece prin ecranele reale ale
// generatorului, câte o casetă pe fiecare. Ce verifică:
//   - pornește singur la login-ul unui cont cu `tour_seen_at` gol, pe Acasă, cu „Bun venit în eco360”;
//   - Înainte / Înapoi / săgețile merg pas cu pas, iar tastele aplicației (cifra 2) nu mișcă nimic sub tur;
//   - fiecare pas stă pe ecranul lui, caseta e în ecran și nu acoperă ținta, ținta e adusă în ecran;
//   - „Gata” lasă omul pe Acasă și scrie în bază că a văzut turul; la reîncărcare și la un login nou nu mai apare;
//   - se poate relua din meniul contului și din căutare (Ctrl K), iar Esc / „Sari peste” îl închid pe loc;
//   - un client nou-nouț (firmă de generator goală, administrator invitat): turul are și „Primii pași”, iar
//     „Adaugă deșeuri” se luminează și stins (fără punct de lucru);
//   - vizualizatorul n-are „Primii pași”, iar la predări i se spune „Aici vezi”;
//   - la 375px pașii meniului și ai căutării se sar, caseta stă jos în ecran, fără derulare laterală;
//   - capturi la 1440 în cele trei teme și la 375.
// Turul se golește în bază cu `psql` (`E2E_DB`, implicit `ecoregistru`, ca proba 29): conturile demo îl au „văzut”
// din seeder. La sfârșit turul e iar „văzut” pe admin și pe vizualizator (îl închid chiar pașii probei).
// ⚠️ Lasă în urmă firma „Proba 63 Client nou <număr>” și administratorul ei `tur63-<număr>@proba.ro`.
import { execFileSync } from "node:child_process";
import { launch, newPage, login, report, shot, validCui, BASE } from "./lib.mjs";

const sql = (statement) =>
  execFileSync("psql", ["-h", "localhost", "-U", "eco", "-d", process.env.E2E_DB ?? "ecoregistru", "-tAc", statement], {
    env: { ...process.env, PGPASSWORD: process.env.E2E_DB_PASSWORD ?? "eco" },
    encoding: "utf8",
  }).trim();
const resetTour = (email) => sql(`UPDATE app_users SET tour_seen_at = NULL WHERE email = '${email}'`);
const seen = (email) => sql(`SELECT tour_seen_at IS NOT NULL FROM app_users WHERE email = '${email}'`) === "t";

const BOX = '[data-testid="tour-box"]';
const ADMIN = "admin@demo.ro";
const VIEWER = "viewer@demo.ro";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

const stepId = (page) => page.getAttribute(BOX, "data-step-id").catch(() => null);
const boxText = (page) => page.locator(BOX).innerText().catch(() => "");
const path = (page) => new URL(page.url()).pathname;

async function waitBox(page, timeout = 8000) {
  return page
    .waitForSelector(BOX, { state: "visible", timeout })
    .then(() => true)
    .catch(() => false);
}

/**
 * Așteaptă să se schimbe pasul (sau să se închidă turul) după o apăsare. Între doi pași caseta lipsește cât
 * motorul caută ținta (rămâne doar stratul), deci „închis” înseamnă fără strat, nu fără casetă.
 */
async function changed(page, before) {
  await page.waitForFunction(
    ([sel, b]) => {
      const el = document.querySelector(sel);
      if (el) return el.getAttribute("data-step-id") !== b;
      return !document.querySelector('[data-testid="tour-overlay"]');
    },
    [BOX, before],
    { timeout: 8000 }
  );
  // Poziția casetei se mai așază o dată după derularea țintei.
  await page.waitForTimeout(350);
}

async function press(page, button) {
  const before = await stepId(page);
  await page.click(`${BOX} button:has-text("${button}")`);
  await changed(page, before);
}

/** Caseta, ținta și ecranul, în coordonatele ferestrei. */
async function geometry(page) {
  return page.evaluate((sel) => {
    const rect = (el) => {
      if (!el) return null;
      const r = el.getBoundingClientRect();
      return { l: r.left, t: r.top, r: r.right, b: r.bottom, w: r.width, h: r.height };
    };
    const box = document.querySelector(sel);
    const id = box?.getAttribute("data-step-id");
    return { id, box: rect(box), vw: window.innerWidth, vh: window.innerHeight, sw: document.documentElement.scrollWidth };
  }, BOX);
}

async function targetRect(page, target) {
  return page.evaluate((t) => {
    const all = [...document.querySelectorAll(`[data-tour="${t}"]`)].filter((el) => {
      const r = el.getBoundingClientRect();
      return r.width > 0 && r.height > 0;
    });
    if (all.length === 0) return null;
    const r = all[0].getBoundingClientRect();
    return { l: r.left, t: r.top, r: r.right, b: r.bottom, w: r.width, h: r.height, count: all.length };
  }, target);
}

const overlaps = (a, b) => a.l < b.r && b.l < a.r && a.t < b.b && b.t < a.b;

/** Ce așteaptă proba pe fiecare pas: ecranul și ținta luminată (sau nimic, caseta în mijloc). */
const WIDE_STEPS = {
  welcome: { path: "/", target: null },
  nav: { path: "/", target: "nav" },
  "first-steps": { path: "/", target: "first-steps" },
  movements: { path: "/generare", target: "add-waste" },
  annual: { path: "/generare", target: "tab-total" },
  packaging: { path: "/generare", target: "tab-ambalaje" },
  deadlines: { path: "/termene", target: "deadlines-todo" },
  energy: { path: "/energie", target: "page-header" },
  "environmental-report": { path: "/raport-mediu", target: "page-header" },
  "audit-file": { path: "/dosar-control", target: "audit-download" },
  partners: { path: "/parteneri", target: "partners-list" },
  settings: { path: "/setari", target: "page-header" },
  search: { path: "/setari", target: "search" },
  end: { path: "/", target: null },
};

/** Merge cu „Înainte” până la ultimul pas și verifică fiecare pas pe drum. Întoarce id-urile văzute. */
async function walk(page, label, { targets = WIDE_STEPS, narrow = false, shots = {} } = {}) {
  const ids = [];
  for (let guard = 0; guard < 20; guard++) {
    const g = await geometry(page);
    if (!g.box) break;
    ids.push(g.id);
    const want = targets[g.id];
    if (!want) {
      check(`${label}: pasul „${g.id}” e unul cunoscut`, false);
    } else {
      check(`${label}: „${g.id}” stă pe ${want.path}`, path(page) === want.path, path(page));
      const margin = narrow ? 16 : 8;
      check(
        `${label}: „${g.id}”, caseta e în ecran`,
        g.box.l >= margin - 1 && g.box.t >= 0 && g.box.r <= g.vw - margin + 1 && g.box.b <= g.vh,
        JSON.stringify(g.box)
      );
      if (narrow) check(`${label}: „${g.id}”, fără derulare laterală`, g.sw <= g.vw, `${g.sw} > ${g.vw}`);
      const target = want.target;
      if (target) {
        const r = await targetRect(page, target);
        check(`${label}: „${g.id}”, ținta [data-tour="${target}"] există o singură dată`, r !== null && r.count === 1, JSON.stringify(r));
        if (r) {
          if (r.h < g.vh - 40) check(`${label}: „${g.id}”, ținta e adusă în ecran`, r.t >= -1 && r.b <= g.vh + 1, JSON.stringify(r));
          // Pe telefon caseta stă jos peste pagină; pe ecran lat nu are voie să acopere ce arată.
          if (!narrow && r.h < g.vh / 2) check(`${label}: „${g.id}”, caseta nu acoperă ținta`, !overlaps(g.box, r), JSON.stringify({ box: g.box, r }));
        }
      }
    }
    if (shots[g.id]) await shot(page, shots[g.id]);
    const last = (await page.locator(`${BOX} button:has-text("Gata")`).count()) > 0;
    if (last) break;
    await press(page, "Înainte");
  }
  return ids;
}

// ———————————————————————————————————————— admin, 1440 ————————————————————————————————————————
console.log("\n  — administratorul, 1440×900");
resetTour(ADMIN);
{
  const page = await newPage(browser, { tour: true });
  await login(page, "admin");
  check("turul pornește singur la primul login", await waitBox(page));
  check("pornește pe Acasă", path(page) === "/", path(page));
  check("primul pas e „Bun venit în eco360”", (await boxText(page)).includes("Bun venit în eco360"), (await boxText(page)).replace(/\s+/g, " "));
  check("„Înapoi” nu e de apăsat pe primul pas", (await page.locator(`${BOX} button:has-text("Înapoi"):enabled`).count()) === 0);
  await shot(page, "63-tur-1440-bun-venit");

  // Sub tur, tastele aplicației tac: cifra 2 ar deschide Generare.
  await page.keyboard.press("2");
  await page.waitForTimeout(600);
  check("tasta 2 nu deschide Generare sub tur", path(page) === "/" && (await stepId(page)) === "welcome", path(page));

  await page.keyboard.press("ArrowRight");
  await changed(page, "welcome");
  check("săgeata dreapta merge la pasul următor", (await stepId(page)) === "nav", String(await stepId(page)));
  await page.keyboard.press("ArrowLeft");
  await changed(page, "nav");
  check("săgeata stânga se întoarce", (await stepId(page)) === "welcome", String(await stepId(page)));

  const hasFirstSteps = (await targetRect(page, "first-steps")) !== null;
  const ids = await walk(page, "admin", {
    shots: { movements: "63-tur-1440-predari", "audit-file": "63-tur-1440-dosar", end: "63-tur-1440-final" },
  });
  const expected = [
    "welcome",
    "nav",
    ...(hasFirstSteps ? ["first-steps"] : []),
    "movements",
    "annual",
    "packaging",
    "deadlines",
    "energy",
    "environmental-report",
    "audit-file",
    "partners",
    "settings",
    "search",
    "end",
  ];
  check("pașii vin în ordinea din tur", ids.join(",") === expected.join(","), ids.join(","));
  check(
    "progresul ultimului pas spune „N din N”",
    /(\d+) din \1/.test(await boxText(page)),
    (await boxText(page)).replace(/\s+/g, " ")
  );

  await page.click(`${BOX} button:has-text("Gata")`);
  await page.waitForSelector(BOX, { state: "detached", timeout: 5000 }).catch(() => {});
  check("„Gata” închide turul", (await page.$(BOX)) === null);
  await page.waitForTimeout(800);
  check("„Gata” lasă omul pe Acasă", path(page) === "/", path(page));
  check("în bază, turul e văzut", seen(ADMIN));

  await page.reload({ waitUntil: "networkidle" });
  check("după reîncărcare nu mai pornește", !(await waitBox(page, 3000)));

  // Din meniul contului.
  await page.locator('button[aria-label="Meniul contului"]:visible').first().click();
  await page.click('[data-testid="tour-start"]');
  check("„Turul aplicației” din meniul contului îl pornește", await waitBox(page));
  check("reluat, începe de la „Bun venit”", (await stepId(page)) === "welcome", String(await stepId(page)));
  await press(page, "Înainte");
  await press(page, "Înainte");
  const where = path(page);
  await page.keyboard.press("Escape");
  await page.waitForTimeout(500);
  check("Esc îl închide", (await page.$(BOX)) === null);
  check("Esc lasă omul unde era", path(page) === where, `${path(page)} ≠ ${where}`);

  // Din căutare.
  await page.keyboard.press("Control+k");
  await page.fill('input[aria-label="Caută sau sari la"]', "tur");
  await page.waitForTimeout(300);
  const option = page.locator('[role="option"]', { hasText: "Turul aplicației" });
  check("căutarea găsește „Turul aplicației”", (await option.count()) > 0);
  if ((await option.count()) > 0) {
    await option.first().click();
    check("din căutare, turul pornește", await waitBox(page));
    await page.click(`${BOX} button:has-text("Sari peste")`);
    await page.waitForTimeout(500);
    check("„Sari peste” îl închide", (await page.$(BOX)) === null);
  }

  // Un login nou: tot văzut.
  const again = await newPage(browser, { tour: true });
  await login(again, "admin");
  check("la un login nou, de pe alt browser, nu mai pornește", !(await waitBox(again, 3000)));
  fails += report("admin, 1440: fără erori de consolă sau HTTP", [...page.problems, ...again.problems]);
  await again.context().close();
  await page.context().close();
}

// ———————————————————————————————————————— cele trei teme ————————————————————————————————————————
console.log("\n  — temele, 1440×900");
for (const theme of ["luminos", "padure", "intunecat"]) {
  resetTour(ADMIN);
  const page = await newPage(browser, { tour: true });
  await page.addInitScript((t) => {
    try {
      localStorage.setItem("eco360.tema", t);
    } catch {}
  }, theme);
  await login(page, "admin");
  check(`${theme}: turul pornește`, await waitBox(page));
  // Până la predări: pasul cu decupaj și caseta lângă buton.
  for (let i = 0; i < 6 && (await stepId(page)) !== "movements"; i++) await press(page, "Înainte");
  await shot(page, `63-tur-1440-${theme}`);
  await page.click(`${BOX} button:has-text("Sari peste")`);
  await page.waitForTimeout(500);
  check(`${theme}: „Sari peste” scrie turul văzut`, seen(ADMIN));
  fails += report(`${theme}: fără erori`, page.problems);
  await page.context().close();
}

// ———————————————————————————————————————— clientul nou, 1440 ————————————————————————————————————————
console.log("\n  — clientul nou (firmă goală), 1440×900");
{
  const RUN = Date.now().toString().slice(-8);
  const email = `tur63-${RUN}@proba.ro`;
  const setup = await newPage(browser);
  await login(setup, "platform");
  // Firma și invitația ca la „Client nou”: fără punct de lucru, fără parteneri, fără predări.
  const created = await setup.evaluate(
    async ([name, cui, mail]) => {
      const headers = { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" };
      const res = await fetch("/api/v1/companies", {
        method: "POST",
        headers,
        body: JSON.stringify({ name, cui, type: "GENERATOR", afmObligation: false, address: "Cluj-Napoca" }),
      });
      if (!res.ok) return "companie HTTP " + res.status;
      const id = (await res.json()).id;
      const invite = await fetch("/api/v1/users", {
        method: "POST",
        headers: { ...headers, "X-Tenant-Id": id },
        body: JSON.stringify({ email: mail, role: "ADMIN", firstName: "Tur", lastName: "Proba" }),
      });
      if (!invite.ok) return "invitație HTTP " + invite.status + " " + (await invite.text());
      return id;
    },
    [`Proba 63 Client nou ${RUN}`, validCui(), email]
  );
  check("firma goală și invitația există", /^[0-9a-f-]{36}$/.test(String(created)), String(created));
  await setup.context().close();
  // Parola o pune omul din linkul de invitație; aici, aceeași cu a conturilor demo (`E2E_PASSWORD`).
  sql(
    `UPDATE app_users SET enabled = true, password = (SELECT password FROM app_users WHERE email = '${ADMIN}') WHERE email = '${email}'`
  );
  check("invitatul n-a văzut turul", !seen(email));

  const page = await newPage(browser, { tour: true });
  await page.goto(BASE + "/login", { waitUntil: "networkidle" });
  await page.fill("#login-email", email);
  await page.fill("#login-password", process.env.E2E_PASSWORD);
  await page.click('button[type="submit"]');
  await page.waitForURL((u) => !u.pathname.startsWith("/login"), { timeout: 15000 });
  check("clientul nou: turul pornește singur", await waitBox(page));
  const ids = await walk(page, "client nou", { shots: { "first-steps": "63-tur-1440-client-nou-primii-pasi", movements: "63-tur-1440-client-nou-predari" } });
  check("clientul nou: are „Primii pași”, al treilea", ids[2] === "first-steps", ids.join(","));
  check("clientul nou: are toți cei 14 pași", ids.length === 14, ids.join(","));
  await page.click(`${BOX} button:has-text("Gata")`);
  await page.waitForTimeout(800);
  check("clientul nou: la „Gata” rămâne pe Acasă, cu „Primii pași”", path(page) === "/" && (await targetRect(page, "first-steps")) !== null, path(page));
  check("clientul nou: în bază, turul e văzut", seen(email));
  fails += report("clientul nou: fără erori de consolă sau HTTP", page.problems);
  await page.context().close();
}

// ———————————————————————————————————————— vizualizatorul ————————————————————————————————————————
console.log("\n  — vizualizatorul, 1440×900");
resetTour(VIEWER);
{
  const page = await newPage(browser, { tour: true });
  await login(page, "viewer");
  check("vizualizator: turul pornește singur", await waitBox(page));
  let movementsText = "";
  let settingsText = "";
  const ids = [];
  for (let guard = 0; guard < 20; guard++) {
    const id = await stepId(page);
    if (!id) break;
    ids.push(id);
    if (id === "movements") movementsText = await boxText(page);
    if (id === "settings") settingsText = await boxText(page);
    if ((await page.locator(`${BOX} button:has-text("Gata")`).count()) > 0) break;
    await press(page, "Înainte");
  }
  check("vizualizator: fără „Primii pași”", !ids.includes("first-steps"), ids.join(","));
  check("vizualizator: la predări „Aici vezi”", movementsText.includes("Aici vezi"), movementsText.replace(/\s+/g, " "));
  check("vizualizator: la Setări fără colegii de invitat", settingsText !== "" && !settingsText.includes("inviți"), settingsText.replace(/\s+/g, " "));
  await page.click(`${BOX} button:has-text("Gata")`);
  await page.waitForTimeout(800);
  check("vizualizator: în bază, turul e văzut", seen(VIEWER));
  fails += report("vizualizator: fără erori", page.problems);
  await page.context().close();
}

// ———————————————————————————————————————— telefon, 375 ————————————————————————————————————————
console.log("\n  — administratorul, 375×740");
resetTour(ADMIN);
{
  const page = await newPage(browser, { width: 375, height: 740, tour: true });
  await login(page, "admin");
  check("375: turul pornește singur", await waitBox(page));
  const ids = await walk(page, "375", { narrow: true, shots: { movements: "63-tur-375-predari", "audit-file": "63-tur-375-dosar" } });
  check("375: fără pașii meniului și ai căutării", !ids.includes("nav") && !ids.includes("search"), ids.join(","));
  check("375: ajunge la final", ids.at(-1) === "end", ids.join(","));
  const g = await geometry(page);
  check("375: caseta stă jos", g.box && g.vh - g.box.b <= 24, JSON.stringify(g.box));
  await page.click(`${BOX} button:has-text("Gata")`);
  await page.waitForTimeout(800);
  check("375: în bază, turul e văzut", seen(ADMIN));
  fails += report("375: fără erori", page.problems);
  await page.context().close();
}

await browser.close();
console.log(fails === 0 ? "\n  Proba 63 trece." : `\n  Proba 63: ${fails} căderi.`);
process.exit(fails === 0 ? 0 : 1);
