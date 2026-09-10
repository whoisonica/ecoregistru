// Rulează toate probele, una după alta, și iese cu cod diferit de zero dacă vreuna cade.
//
// Secvenţial dinadins: fiecare probă porneşte propriul Chrome, iar cinci deodată pe o maşină de
// lucru înseamnă memorie mâncată degeaba — sesiunea în care s-a scris suita a pierdut un server
// de dev exact aşa.
import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const suites = [
  ["1-ecrane.mjs", "ecranele se deschid"],
  ["2-tabele.mjs", "coloana de acţiuni rămâne la îndemână"],
  ["3-interactiuni.mjs", "căutare, sortare, filtre, scurtături"],
  ["4-formular.mjs", "formularul de mişcare"],
  ["5-telefon.mjs", "ecran îngust"],
  ["6-cerere-si-rapoarte.mjs", "cererea de cont, inboxul ei şi drumul spre mişcarea vinovată"],
  ["7-firma-si-reactivare.mjs", "datele firmei, reactivarea şi garda formularului"],
  ["8-atasamente-partener-paleta.mjs", "atașamentele, partenerul pe secțiuni, cuprinsul și paleta"],
  ["9-restrangeri-si-semne.mjs", "rubrica restrânsă, grila care spune că a salvat, stocul pe coduri, termenele"],
  ["10-panou-actiunea-urmatoare.mjs", "banda «următoarea acțiune» din capul Panoului"],
  ["11-numeralul.mjs", "numeralul românesc, pe toate ecranele deodată"],
  ["12-pagini-legale.mjs", "termenii și politica: se deschid fără cont, și se ajunge la ele"],
];

const only = process.argv[2];
const chosen = only ? suites.filter(([f]) => f.includes(only)) : suites;
if (chosen.length === 0) {
  console.error(`Nicio probă nu se potriveşte cu „${only}". Există: ${suites.map(([f]) => f).join(", ")}`);
  process.exit(2);
}

function run(file) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [path.join(here, file)], { stdio: "inherit" });
    child.on("exit", (code) => resolve(code ?? 1));
    child.on("error", () => resolve(1));
  });
}

const failed = [];
for (const [file, what] of chosen) {
  console.log(`\n━━━ ${file} — ${what}\n`);
  const code = await run(file);
  if (code !== 0) failed.push(file);
}

console.log("");
if (failed.length === 0) {
  console.log(`✓ ${chosen.length} probe, toate trec.`);
  process.exit(0);
}
console.log(`✗ ${failed.length} din ${chosen.length} au căzut: ${failed.join(", ")}`);
process.exit(1);
