// Unelte comune pentru probele de interfață. Conduc Chrome-ul instalat pe mașină
// (`channel: "chrome"`), deci nu se descarcă niciun browser.
import { chromium } from "playwright-core";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

export const BASE = process.env.E2E_BASE ?? "http://localhost:5173";
/**
 * Ce browser se conduce. Implicit Chrome-ul instalat pe maşină — de asta `playwright-core`, care
 * nu descarcă nimic. `E2E_CHANNEL=msedge` merge la fel.
 *
 * <p>`E2E_CHANNEL=` (gol) foloseşte Chromium-ul lui Playwright, din cache. E ieşirea pentru o
 * maşină fără niciun browser din familia Chromium — probat pe 07.09.2026 pe un Mac care avea doar
 * Safari, unde suita nu putea porni deloc.
 */
export const CHANNEL = process.env.E2E_CHANNEL ?? "chrome";
/** Lângă suită, nu în directorul din care s-a pornit comanda. Gitignored. */
export const SHOTS = path.join(path.dirname(fileURLToPath(import.meta.url)), "shots");

export const ACCOUNTS = {
  admin: { email: "admin@demo.ro", password: "Parola123" },
  operator: { email: "operator@demo.ro", password: "Parola123" },
  viewer: { email: "viewer@demo.ro", password: "Parola123" },
  platform: { email: "platform@ecoregistru.ro", password: "Parola123" },
};

/** Zgomot cunoscut, care nu spune nimic despre ce testăm. */
const IGNORED = [
  /Download the React DevTools/i,
  /\[vite\] connect/i,
  /favicon\.ico/i,
  // Zgomot de bibliotecă, preexistent: React Router anunță schimbări din v7.
  /React Router Future Flag Warning/i,
];

export async function launch() {
  fs.mkdirSync(SHOTS, { recursive: true });
  const browser = await chromium.launch({
    ...(CHANNEL ? { channel: CHANNEL } : {}),
    headless: true,
    args: ["--disable-dev-shm-usage"],
  });
  return browser;
}

/**
 * O pagină care își ține minte tot ce a mers prost: erori de consolă, excepții
 * nerezolvate și răspunsuri HTTP de 400 în sus.
 */
export async function newPage(browser, { width = 1440, height = 900 } = {}) {
  const context = await browser.newContext({ viewport: { width, height } });
  const page = await context.newPage();
  page.problems = [];

  page.on("console", (msg) => {
    if (msg.type() !== "error" && msg.type() !== "warning") return;
    const text = msg.text();
    if (IGNORED.some((re) => re.test(text))) return;
    page.problems.push(`[consolă ${msg.type()}] ${text}`);
  });
  page.on("pageerror", (err) => {
    page.problems.push(`[excepție] ${err.message}`);
  });
  page.on("response", (res) => {
    if (res.status() >= 400) {
      page.problems.push(`[HTTP ${res.status()}] ${res.request().method()} ${res.url()}`);
    }
  });
  return page;
}

export async function login(page, who = "admin") {
  const { email, password } = ACCOUNTS[who];
  await page.goto(BASE + "/login", { waitUntil: "networkidle" });
  await page.fill("#login-email", email);
  await page.fill("#login-password", password);
  await page.click('button[type="submit"]');
  await page.waitForURL((u) => !u.pathname.startsWith("/login"), { timeout: 15000 });
  await page.waitForLoadState("networkidle");
}

export async function shot(page, name) {
  await page.screenshot({ path: path.join(SHOTS, name + ".png"), fullPage: true });
}

/** Adevărat dacă selectorul există și e vizibil, fără să arunce când lipsește. */
export async function visible(page, selector) {
  const el = await page.$(selector);
  return el ? await el.isVisible() : false;
}

export function report(title, problems) {
  if (problems.length === 0) {
    console.log(`  OK   ${title}`);
    return 0;
  }
  console.log(`  FAIL ${title}`);
  for (const p of [...new Set(problems)]) console.log(`         ${p}`);
  return problems.length;
}

/**
 * Clic pe un element din tabel, fără derularea automată a lui Playwright.
 *
 * <p>Playwright derulează după geometria proprie și ignoră `scroll-margin`, deci ar duce rândul
 * chiar sub antetul lipicios și ar raporta un clic interceptat — un artefact al probei, nu o
 * problemă a aplicației. Aici derulăm noi, **verificăm cu `elementFromPoint` că ținta chiar e
 * deasupra** (verificarea pe care n-o vrem pierdută), și apăsăm cu mausul la coordonatele alea.
 */
export async function clickAt(page, selector) {
  const box = await page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (!el) return { eroare: "element negăsit: " + sel };
    el.scrollIntoView({ block: "center" });
    const r = el.getBoundingClientRect();
    const x = r.left + r.width / 2;
    const y = r.top + r.height / 2;
    const top = document.elementFromPoint(x, y);
    return { x, y, acoperit: !(top === el || el.contains(top) || el.contains(top?.parentElement)) };
  }, selector);
  if (box.eroare) throw new Error(box.eroare);
  if (box.acoperit) throw new Error("ținta e acoperită de alt element: " + selector);
  await page.mouse.click(box.x, box.y);
}
