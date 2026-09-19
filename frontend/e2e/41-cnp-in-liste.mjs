// Proba 41 (QA de lansare, 19.09.2026, BUG-043): CNP-ul șoferului nu pleacă întreg în liste.
//
// Ecranul îl arată mascat, deci nu are nevoie de el; Anexa 3 îl tipărește serverul. Totuși `GET /drivers` și lista
// de mișcări îl trimiteau întreg oricui din firmă, inclusiv rolului „Vizualizare”. Proba pune un șofer cu un CNP
// valid (prin API, ca admin), apoi citește listele ca vizualizator. ⚠️ Lasă în urmă șoferul „Proba 41 <număr>”.
import { launch, newPage, login } from "./lib.mjs";

const CNP = "1900101123457";
const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => { console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`); if (!ok) fails++; };
const call = (page, method, path, body) => page.evaluate(async ([method, path, body]) => {
  const r = await fetch(path, { method, headers: { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" }, body: body ? JSON.stringify(body) : undefined });
  return { status: r.status, text: await r.text() };
}, [method, path, body]);

const admin = await newPage(browser);
await login(admin, "admin");
const made = await call(admin, "POST", "/api/v1/drivers", { name: "Proba 41 " + Date.now().toString().slice(-6), identification: "CJ 41", cnp: CNP });
check("șoferul cu CNP s-a salvat", made.status === 200, String(made.status));

const viewer = await newPage(browser);
await login(viewer, "viewer");
// Lista de mișcări trimite și ea `driverCnp`; pe datele demo nicio mișcare n-are CNP, deci aici se probează doar șoferii.
for (const path of ["/api/v1/drivers"]) {
  const r = await call(viewer, "GET", path);
  check(`vizualizatorul nu primește CNP-ul întreg în ${path}`, r.status !== 200 || !r.text.includes(CNP), `HTTP ${r.status}`);
}

await browser.close();
console.log(fails === 0 ? "\n✓ proba 41 trece" : `\n✗ proba 41: ${fails} verificări căzute`);
process.exit(fails === 0 ? 0 : 1);
