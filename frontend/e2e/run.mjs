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
  ["9-restrangeri-si-semne.mjs", "rubrica restrânsă, grila care spune că a salvat, panoul fără dala de stoc, termenele"],
  ["10-panou-actiunea-urmatoare.mjs", "banda «următoarea acțiune» din capul Panoului"],
  ["11-numeralul.mjs", "numeralul românesc, pe toate ecranele deodată"],
  ["12-pagini-legale.mjs", "termenii și politica: se deschid fără cont, și se ajunge la ele"],
  ["13-drumul-aprobarii.mjs", "cei nouă pași ai aprobării, și cine face fiecare"],
  ["14-codul-oglinda.mjs", "codul-oglindă declarat nepericulos fără document justificativ"],
  ["15-jurnal-audit.mjs", "cine a modificat cantitatea asta, şi ce spune ecranul despre asta"],
  ["16-import.mjs", "importul din Excel: șablonul, verificarea și butonul care se deblochează"],
  ["17-persoane-fizice.mjs", "tabul «Persoane fizice»: CNP mascat, cifra de control, editarea, vizualizatorul"],
  ["18-panou-cantar.mjs", "panoul «Cântar»: eticheta firmei, afișajul, tastele, Intrări/Ieșiri, pubelele, bara de jos"],
  ["19-cantar-operatiuni.mjs", "ecranul «Cântar»: operațiunile depozitului, neto din cântar, cât se reține din plată"],
  ["20-flota.mjs", "flota din Setări: numărul normalizat, ITP-ul și licența care expiră, vehiculul recunoscut la cântar"],
  ["21-soferi.mjs", "șoferii din Setări: depozitul implicit, atestatul expirat, șoferul ales din listă la cântar"],
  ["22-lansare-marunte.mjs", "Istoric pe rândul de mișcare, cât cântărește dosarul, parola cu «Arată» și putere, limitele la șoferi"],
  ["23-cereri-1609.mjs", "Generare fără «Operațiune», scopul V/E, destinația și autorizația destinatarului, ANAF la CUI, Anexa 3 la generator"],
  ["24-primii-pasi.mjs", "«Primii pași» pe Acasă: nu la firma care are tot, pașii bifați din date, «Ascunde» care ține, 375px"],
  ["25-parola-poster.mjs", "«Parolă uitată» și «Alege-ți parola» în PublicShell: fără cod, parole diferite, cod greșit, 375px"],
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
