// Ce vede magazinul în build-ul de producție: `npm run test:store`.
//
// Pluginurile Expo pun singure texte și permisiuni pe care aplicația nu le folosește: `expo-secure-store` cere Face ID
// cu un text în engleză („Allow $(PRODUCT_NAME) to access your Face ID…”) și biometria pe Android, iar șablonul
// Android declară `SYSTEM_ALERT_WINDOW` (fereastra peste alte aplicații, a meniului de dezvoltare). Până pe
// 27.09.2026 le aveam pe toate, plus o declarație de confidențialitate fără nicio dată colectată — adică Apple ar fi
// aflat din aplicație că nu trimitem nici e-mailul, nici poza avizului. Proba citește configurația după pluginuri
// (`expo config --type introspect`), deci prinde și un plugin nou care le aduce înapoi.
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { test } from "node:test";

const config = JSON.parse(
  execFileSync("npx", ["expo", "config", "--type", "introspect", "--json"], {
    cwd: new URL("..", import.meta.url).pathname,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "ignore"],
  }),
);
const infoPlist: Record<string, unknown> = config._internal.modResults.ios.infoPlist;
const manifest = config._internal.modResults.android.manifest.manifest;

test("nicio cerere de Face ID: aplicația nu folosește biometria", () => {
  assert.equal("NSFaceIDUsageDescription" in infoPlist, false);
});

test("fiecare cerere de permisiune pe iPhone e scrisă de noi, în română", () => {
  for (const [key, text] of Object.entries(infoPlist)) {
    if (!key.endsWith("UsageDescription")) continue;
    // Al clientului de dezvoltare: faza „Strip Local Network Keys for Release” îl scoate din build-ul de producție.
    if (key === "NSLocalNetworkUsageDescription" && String(text).startsWith("Expo Dev Launcher")) continue;
    assert.match(String(text), /^WasteHouse /, `${key}: ${text}`);
  }
});

test("nicio cerere de calendar: „În calendar” deschide formularul sistemului, care nu cere acces (F9)", () => {
  for (const key of Object.keys(infoPlist)) assert.equal(/^NS(Calendars|Reminders)/.test(key), false, key);
});

test("Release pe Android fără fereastra peste alte aplicații, fără biometrie și fără calendar", () => {
  const removed = (manifest["uses-permission"] ?? [])
    .filter((p: { $: Record<string, string> }) => p.$["tools:node"] === "remove")
    .map((p: { $: Record<string, string> }) => p.$["android:name"]);
  for (const name of ["SYSTEM_ALERT_WINDOW", "USE_BIOMETRIC", "USE_FINGERPRINT", "READ_CALENDAR", "WRITE_CALENDAR"]) {
    assert.ok(removed.includes(`android.permission.${name}`), name);
  }
});

test("declarația de confidențialitate spune ce trimite aplicația, fără urmărire", () => {
  const privacy = config.ios.privacyManifests;
  assert.equal(privacy.NSPrivacyTracking, false);
  const types = privacy.NSPrivacyCollectedDataTypes.map((t: Record<string, unknown>) => {
    assert.equal(t.NSPrivacyCollectedDataTypeTracking, false);
    return t.NSPrivacyCollectedDataType;
  });
  for (const type of ["EmailAddress", "PhotosorVideos", "OtherUserContent", "CrashData"]) {
    assert.ok(types.includes(`NSPrivacyCollectedDataType${type}`), type);
  }
});
