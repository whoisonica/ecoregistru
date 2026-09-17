#!/usr/bin/env node
// Conturile demo WasteHouse (plan-curatenie-prod-demo.md, Faza 3): o firmă generator cu un an de date și un cabinet
// de consultant cu trei firme. Totul trece prin API-ul aplicației, ca un om la tastatură: validările, termenele și
// jurnalul de audit ies ca la un client real, iar un endpoint schimbat face scriptul să cadă vizibil.
//
//   node scripts/demo/seed-demo.mjs --api http://localhost:8080
//   node scripts/demo/seed-demo.mjs --api https://ecoregistru-api-5ba7c1d5e3e3.herokuapp.com
//
// Autentificarea: contul PLATFORM_ADMIN. Emailul din WH_EMAIL (obligatoriu), parola din
// WH_PASSWORD sau, fără ea, cerută în terminal fără ecou. Parola nu stă nicăieri în repo.
//
// Utilizatorii demo sunt aliasuri ale adminului (nume+demo-generator@… ): primesc invitația cu
// linkul de setare a parolei, ca orice client. Fără adrese inventate — ar da bounce la Brevo.
//
// Nu rulează de două ori: dacă un CUI demo există deja, se oprește înainte de a scrie ceva.
// Nu creează abonamente, deci firmele demo nu intră la facturare.
import fs from "node:fs";
import path from "node:path";
import readline from "node:readline";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const args = process.argv.slice(2);
const API = (args[args.indexOf("--api") + 1] || "").replace(/\/$/, "");
if (!args.includes("--api") || !API.startsWith("http")) {
  console.error("folosire: node scripts/demo/seed-demo.mjs --api <url api>");
  process.exit(2);
}
const EMAIL = process.env.WH_EMAIL;
if (!EMAIL) {
  console.error("WH_EMAIL lipsește: emailul contului PLATFORM_ADMIN");
  process.exit(2);
}
const DATA = JSON.parse(fs.readFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), "demo-data.json"), "utf8"));
const TODAY = new Date().toISOString().slice(0, 10);

// ---------------------------------------------------------------------------------------------------- HTTP
let token;
async function call(method, url, { body, tenant, form } = {}) {
  const headers = { Authorization: `Bearer ${token}` };
  if (tenant) headers["X-Tenant-Id"] = tenant;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  for (let attempt = 1; ; attempt++) {
    const res = await fetch(API + url, { method, headers, body: form ?? (body === undefined ? undefined : JSON.stringify(body)) });
    const text = await res.text();
    if (res.ok) return text ? JSON.parse(text) : null;
    if (res.status >= 500 && attempt < 3) { await sleep(1500 * attempt); continue; }
    throw new Error(`${method} ${url} → ${res.status} ${text.slice(0, 400)}\n  corp: ${JSON.stringify(body)?.slice(0, 400)}`);
  }
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function password() {
  if (process.env.WH_PASSWORD) return process.env.WH_PASSWORD;
  if (!process.stdin.isTTY) throw new Error("WH_PASSWORD lipsește și nu am terminal în care s-o cer");
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout, terminal: true });
  rl._writeToOutput = (s) => { if (s.includes("Parola")) process.stdout.write(s); };
  const p = await new Promise((r) => rl.question(`Parola pentru ${EMAIL}: `, r));
  rl.close();
  process.stdout.write("\n");
  return p;
}

// ---------------------------------------------------------------------------------------------------- date
// Generator pseudo-aleator cu sămânță: aceleași date la fiecare rulare, deci demo-ul local = demo-ul de pe producție.
let seed = 20260917;
const rnd = () => ((seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648);
const round5 = (x) => Math.max(1, Math.round(x / 5) * 5);
const iso = (d) => d.toISOString().slice(0, 10);

/** Datele predărilor unui flux: `every` pe an, întinse pe perioada demo, niciodată în weekend sau în viitor. */
function datesFor(stream) {
  const from = new Date(DATA.from + "T00:00:00Z");
  const to = new Date(TODAY + "T00:00:00Z");
  const days = (to - from) / 86400000;
  const n = Math.max(1, Math.round((stream.every * days) / 365));
  const step = days / n;
  const out = [];
  for (let i = 0; i < n; i++) {
    const d = new Date(from.getTime() + (i * step + rnd() * step * 0.6) * 86400000);
    if (d.getUTCDay() === 0) d.setUTCDate(d.getUTCDate() + 1);
    if (d.getUTCDay() === 6) d.setUTCDate(d.getUTCDate() - 1);
    if (d > to) continue;
    out.push(d);
  }
  return out;
}

const destinationsFor = (op) => (op.startsWith("R") ? ["COLECTARE", "VALORIFICARE"] : ["COLECTARE", "ELIMINARE"]);
/** Nota 5 a fișei (HG 856/2002): Vr = valorificare prin agenți autorizați, DO = depozit, I = incinerare. */
const wasteDestinationFor = (op) => (op.startsWith("R") ? "Vr" : op === "D10" ? "I" : "DO");
/** RM = recipienți metalici (lichidele), CT = container transportabil. */
const storageFor = (state) => (state === "LIQUID" ? "RM" : "CT");

// ---------------------------------------------------------------------------------------------------- pași
const wasteCodes = new Map();
async function codeId(code) {
  // Lista e limitată la primele rezultate, deci fiecare cod se caută.
  const bare = code.replace(/\*/g, "");
  if (!wasteCodes.has(bare)) {
    const hits = await call("GET", `/api/v1/waste-codes?q=${encodeURIComponent(bare)}`);
    const c = hits.find((h) => h.code === bare);
    if (!c) throw new Error(`codul ${code} nu e în catalog`);
    if (c.hazardous !== code.endsWith("*")) throw new Error(`codul ${code}: steluța nu se potrivește cu catalogul`);
    wasteCodes.set(bare, c.id);
  }
  return wasteCodes.get(bare);
}

const alias = (a) => EMAIL.replace("@", `+${a}@`);

async function createCompany(spec) {
  const c = spec.company;
  const codes = [...new Set(spec.streams.map((s) => s.code))];
  const company = await call("POST", "/api/v1/companies", {
    body: {
      ...c,
      contactEmail: alias("demo-contact"),
      authorizedOperationCodes: [],
      marketRoles: c.marketRoles ?? [],
      afmContributions: c.afmContributions ?? [],
      authorizedWasteCodeIds: await Promise.all(codes.map(codeId)),
    },
  });
  const t = company.id;
  console.log(`\n■ ${c.name}  (${company.id})`);

  const wp = {};
  const sections = {};
  for (const w of spec.workPoints) {
    wp[w.key] = (await call("POST", "/api/v1/work-points", { tenant: t, body: { name: w.name, address: w.address } })).id;
    for (const s of w.sections) {
      sections[`${w.key}/${s}`] = (await call("POST", "/api/v1/internal-generators", {
        tenant: t, body: { workPointId: wp[w.key], name: s },
      })).id;
    }
  }
  console.log(`  puncte de lucru ${spec.workPoints.length}, secții ${Object.keys(sections).length}`);

  const partners = {};
  for (const key of [...new Set(spec.streams.map((s) => s.partner))]) {
    const p = DATA.partners[key];
    partners[key] = (await call("POST", "/api/v1/partners", {
      tenant: t, body: { ...p, client: false, carrier: false, heavyVehicles: false, workPoints: [], drivers: [] },
    })).id;
  }
  console.log(`  parteneri ${Object.keys(partners).length}`);

  let count = 0;
  let kg = 0;
  for (const s of spec.streams) {
    const wasteCodeId = await codeId(s.code);
    for (const d of datesFor(s)) {
      const month = d.getUTCMonth() + 1;
      const q = round5((s.min + rnd() * (s.max - s.min)) * (s.peak?.includes(month) ? 1.4 : 1));
      await call("POST", "/api/v1/movements", {
        tenant: t,
        body: {
          clientGeneratedId: crypto.randomUUID(),
          workPointId: wp[s.wp],
          internalGeneratorId: sections[`${s.wp}/${s.section}`],
          date: iso(d),
          wasteCodeId,
          quantity: q,
          weighedAtUnloading: false,
          unit: "KG",
          operation: s.op.startsWith("R") ? "RECOVERED" : "DISPOSED",
          operationCode: s.op,
          register: "ANEXA_1",
          physicalState: s.state,
          storageType: storageFor(s.state),
          transportMeans: "AS",
          wasteDestination: wasteDestinationFor(s.op),
          partnerId: partners[s.partner],
          transportDestinations: destinationsFor(s.op),
          documentReference: `FEI ${String(1000 + count).padStart(5, "0")}`,
        },
      });
      count++;
      kg += q;
    }
  }
  console.log(`  predări ${count}, ${Math.round(kg).toLocaleString("ro-RO")} kg`);

  for (const m of spec.packagingMarket ?? []) {
    await call("PUT", "/api/v1/packaging/market", { tenant: t, body: { ...m, year: 2025 } });
  }
  if (spec.packagingMarket) console.log(`  ambalaje pe piață 2025: ${spec.packagingMarket.length} materiale`);

  // Termenele se generează numai înainte (următorul pe fiecare fel), deci nu există termene trecute de bifat.
  const next = await call("POST", "/api/v1/deadlines/regenerate", { tenant: t });
  console.log(`  termene adăugate la regenerare: ${next.generated}`);
  return company;
}

// ---------------------------------------------------------------------------------------------------- main
const login = await fetch(API + "/api/v1/auth/login", {
  method: "POST", headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ email: EMAIL, password: await password() }),
});
if (!login.ok) throw new Error(`login ${login.status}: ${await login.text()}`);
const session = await login.json();
if (session.role !== "PLATFORM_ADMIN") throw new Error(`${EMAIL} e ${session.role}, nu PLATFORM_ADMIN`);
token = session.token;

const demoCuis = [DATA.generator.company.cui, DATA.cabinet.consultancy.cui, ...DATA.cabinet.companies.map((c) => c.company.cui)]
  .map((c) => c.replace(/^RO/, ""));
const existing = [
  ...(await call("GET", "/api/v1/companies")),
  ...(await call("GET", "/api/v1/consultancies")),
].filter((x) => demoCuis.includes(String(x.cui).replace(/^RO/, "")));
if (existing.length) {
  console.error(`Există deja: ${existing.map((x) => x.name).join(", ")} — nu scriu nimic.`);
  process.exit(1);
}
console.log(`API ${API}, autentificat ca ${EMAIL}. Perioada ${DATA.from} … ${TODAY}.`);

const gen = await createCompany(DATA.generator);
for (const u of DATA.generator.users) {
  await call("POST", `/api/v1/companies/${gen.id}/users`, {
    body: { email: alias(u.alias), role: u.role, firstName: u.firstName, lastName: u.lastName },
  });
  console.log(`  invitat ${alias(u.alias)} (${u.role})`);
}

const cab = await call("POST", "/api/v1/consultancies", { body: DATA.cabinet.consultancy });
console.log(`\n■ ${cab.name}  (${cab.id})`);
for (const spec of DATA.cabinet.companies) {
  const c = await createCompany(spec);
  await call("PUT", `/api/v1/companies/${c.id}/consultancy`, { body: { consultancyId: cab.id } });
  console.log(`  mutată în ${cab.name}`);
}
const k = DATA.cabinet.consultant;
await call("POST", `/api/v1/consultancies/${cab.id}/users`, {
  body: { email: alias(k.alias), firstName: k.firstName, lastName: k.lastName },
});
console.log(`  invitat ${alias(k.alias)} (CONSULTANT)`);
console.log("\nGata. Parolele se aleg din mailurile de invitație.");
