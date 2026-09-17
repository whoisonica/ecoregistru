// Proba 37: deconectarea golește cache-ul (17.09.2026).
//
// Ce se poate strica: platforma se deconectează, consultantul intră în același tab (fără reîncărcare), iar
// selectorul de firme îi arată lista platformei, rămasă în React Query — cu firme din afara cabinetului lui,
// pe care le putea și alege.
// Consultantul se scrie cu `psql` (invitația cere mail): `E2E_DB`, parola hash-ului luată de la admin@demo.ro.
// ⚠️ Lasă în urmă cabinetul „Proba 37 Cabinet <număr>”, firma „Proba 37 <număr>” și consultant37-<număr>@demo.ro.
import { execFileSync } from "node:child_process";
import { launch, newPage, login, companies, validCui, ACCOUNTS, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-8);
const FIRM = `Proba 37 ${RUN}`;
const EMAIL = `consultant37-${RUN}@demo.ro`;

function sql(statement) {
  return execFileSync("psql", ["-h", "localhost", "-U", "eco", "-d", process.env.E2E_DB ?? "ecoregistru", "-tAc", statement], {
    env: { ...process.env, PGPASSWORD: process.env.E2E_DB_PASSWORD ?? "eco" },
    encoding: "utf8",
  }).trim().split("\n")[0];
}

async function api(method, path, body) {
  return page.evaluate(
    async ([method, path, body]) => {
      const headers = { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" };
      const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
      return res.ok ? await res.json() : { error: res.status };
    },
    [method, path, body]
  );
}

await login(page, "platform");
const cabinet = await api("POST", "/api/v1/consultancies", { name: `Proba 37 Cabinet ${RUN}`, cui: validCui() });
const firm = await api("POST", "/api/v1/companies", {
  name: FIRM, cui: validCui(), type: "GENERATOR", afmObligation: false, address: "Cluj-Napoca",
});
const moved = await api("PUT", `/api/v1/companies/${firm.id}/consultancy`, { consultancyId: cabinet.id });
check("cabinetul și firma lui există", Boolean(cabinet.id && firm.id && !moved.error), JSON.stringify({ cabinet, firm, moved }));
sql(
  `insert into app_users (id, email, password, role, consultancy_id, enabled, created_at, token_version)
   select gen_random_uuid(), '${EMAIL}', password, 'CONSULTANT', '${cabinet.id}', true, now(), 0
   from app_users where email = '${ACCOUNTS.admin.email}'`
);

// Platforma deschide selectorul: lista tuturor firmelor intră în cache.
await page.reload({ waitUntil: "networkidle" });
const all = await companies(page);
check("platforma vede mai multe firme", all.length > 1, all.join(" · "));

// Deconectare din meniul contului, apoi consultantul în același tab — fără `goto`, ca aplicația să nu se reîncarce.
await page.click('button[aria-label="Meniul contului"]');
await page.click('[role="menuitem"]:has-text("Deconectare")');
await page.waitForURL((u) => u.pathname.startsWith("/login"), { timeout: 15000 });
await page.fill("#login-email", EMAIL);
await page.fill("#login-password", ACCOUNTS.admin.password);
await page.click('button[type="submit"]');
await page.waitForURL((u) => !u.pathname.startsWith("/login"), { timeout: 15000 });
await page.waitForLoadState("networkidle");

const mine = await companies(page);
check("consultantul vede numai firma cabinetului", mine.length === 1 && mine[0] === FIRM, mine.join(" · "));

await browser.close();
process.exit(fails ? 1 : 0);
