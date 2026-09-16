// Proba 25: „Parolă uitată” și „Alege-ți parola” în direcția „Poster”, ca loginul și cererea de cont
// (docs/stil-interfata.md, „Paginile de dinaintea contului”, 17.09.2026).
//
// (1) Amândouă stau în `PublicShell`: semnul în `header`, titlul mare cu fraza verde, linkul din colț spre
// login, subsolul juridic o singură dată, butonul principal în verdele landingului (`bg-mark`). (2) Parola
// uitată spune același lucru pentru o adresă fără cont. (3) Fără cod, pagina de parolă spune ce lipsește în
// loc să arate un formular; parolele diferite se opresc în ecran, iar un cod greșit primește eroarea
// serverului. (4) La 375px nu se derulează lateral, iar „faptele” din stânga lipsesc.
//
// Nu lasă nimic în urmă: adresa nu are cont, iar codul nu există.
import { launch, newPage, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;

function check(name, ok, detail = "") {
  if (ok) console.log(`  OK   ${name}${detail ? " — " + detail : ""}`);
  else {
    console.log(`  FAIL ${name}${detail ? " — " + detail : ""}`);
    fails++;
  }
}

async function shell(page, label) {
  check(`${label}: semnul în header`, Boolean(await page.$("header svg")));
  check(`${label}: titlul mare are fraza verde`, Boolean(await page.$("h2 span.text-mark")));
  check(`${label}: linkul din colț duce la login`, Boolean(await page.$('a[href="/login"]:has-text("Autentifică-te")')));
  check(`${label}: subsolul juridic o singură dată`, (await page.$$("footer")).length === 1);
  check(`${label}: nu mai e cardul vechi`, !(await page.$(".max-w-sm")));
}

// ------------------------------------------------------------ (1)(2) PAROLĂ UITATĂ
const page = await newPage(browser, { width: 1440, height: 900 });
await page.goto(BASE + "/parola-uitata", { waitUntil: "networkidle" });
await shell(page, "parola uitată");
check("parola uitată: „faptele” se văd pe desktop", (await page.getByText("Invitația expirată se reînnoiește tot de aici", { exact: true }).isVisible()));
const submit = page.locator('button[type="submit"]');
check("parola uitată: butonul e în verdele landingului", (await submit.getAttribute("class")).includes("bg-mark"));
await shot(page, "parola_uitata");
await page.fill("#fp-email", "proba25-fara-cont@exemplu.ro");
await submit.click();
await page.waitForSelector("h1:has-text('linkul de resetare e pe drum')", { timeout: 10000 }).catch(() => {});
check("adresa fără cont primește același răspuns", (await page.textContent("h1"))?.includes("linkul de resetare e pe drum"));
check("după trimitere, drumul înapoi la login", Boolean(await page.$('a[href="/login"]:has-text("Înapoi la autentificare")')));
await shot(page, "parola_uitata_trimis");

// ---------------------------------------------------------------- (1)(3) ALEGE PAROLA
const anon = await newPage(browser, { width: 1440, height: 900 });
await anon.goto(BASE + "/reseteaza-parola", { waitUntil: "networkidle" });
check("fără cod: eroarea spune ce lipsește", (await anon.textContent('[role="alert"]'))?.includes("îi lipsește codul"));
check("fără cod: niciun formular", !(await anon.$("form")));
check("fără cod: linkul spre un link nou", Boolean(await anon.$('a[href="/parola-uitata"]')));

await anon.goto(BASE + "/reseteaza-parola?code=proba-25-inexistent", { waitUntil: "networkidle" });
await shell(anon, "alege parola");
check("alege parola: butonul e în verdele landingului",
  (await anon.locator('button[type="submit"]').getAttribute("class")).includes("bg-mark"));
check("alege parola: bara de putere a rămas", Boolean(await anon.fill("#rp-pass", "Parola-lunga-2034").then(() => anon.$('[data-testid="password-strength"]'))));
await anon.fill("#rp-confirm", "Alta-parola-2034");
await anon.click('button[type="submit"]');
check("parolele diferite se opresc în ecran", (await anon.textContent('[role="alert"]'))?.includes("nu coincid"));
await shot(anon, "alege_parola_nu_coincid");

await anon.fill("#rp-confirm", "Parola-lunga-2034");
const serverAnswer = anon.waitForResponse((r) => r.url().includes("/api/v1/auth/reset-password"));
await anon.click('button[type="submit"]');
const res = await serverAnswer;
await anon.waitForTimeout(300);
check("codul greșit e refuzat de server", res.status() >= 400, `HTTP ${res.status()}`);
check("și refuzul apare pe ecran", Boolean(await anon.$('[role="alert"]')));
// Refuzul e chiar ce se probează, nu o problemă.
anon.problems = anon.problems.filter((p) => !p.includes("/api/v1/auth/reset-password") && !p.includes("status of 4"));

// ------------------------------------------------------------------------ (4) 375px
const phone = await newPage(browser, { width: 375, height: 812 });
for (const path of ["/parola-uitata", "/reseteaza-parola?code=proba-25-inexistent"]) {
  await phone.goto(BASE + path, { waitUntil: "networkidle" });
  const overflow = await phone.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  check(`375px ${path}: fără derulare laterală`, overflow <= 0, `${overflow}px`);
  check(`375px ${path}: „faptele” ascunse`, !(await phone.getByText("Evidența și documentele firmei rămân neatinse", { exact: true }).isVisible()));
  await shot(phone, "telefon" + path.split("?")[0].replace(/\//g, "_"));
}

for (const p of [page, anon, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}

await browser.close();
console.log("");
console.log(fails === 0 ? "✓ Proba 25 trece." : `✗ Proba 25: ${fails} eșecuri`);
process.exit(fails === 0 ? 0 : 1);
