/**
 * Conversii de unități pentru cantitățile de deșeu. Un singur loc, fiindcă factorul 1000 pus de
 * mână în două ecrane e cum se raportează de o mie de ori mai mult sau mai puțin.
 *
 * **De ce există.** Evidența pe care o ținem e integral în **kilograme**, și e corectă așa:
 * HG 856/2002 lasă „Unitatea de măsură" câmp liber pe fișă, iar toate cele 33 de foi completate
 * primite de la specialistă sunt în kg. Dar depunerea de pe **15 martie** e altceva: OUG 92/2021
 * art. 48 alin. (1) scrie „cantitatea **în tone**" de două ori, la lit. a) și la lit. c).
 *
 * Deci una e ce **ții**, alta e ce **depui** — și până acum clientul făcea conversia de mână, cod
 * cu cod, chiar în ziua depunerii. Punctul 7 al auditului de conformitate din 02.09.2026.
 *
 * **Ce NU face.** Nu atinge niciun formular tipărit. Fișa și declarația anuală rămân în kg, pe
 * hârtie și în PDF — sunt corecte, iar a le muta pe tone ar fi o abatere de la modelele completate
 * fără ca vreun act s-o ceară. Cifra în tone se vede **pe ecran**, ca ajutor la încărcarea în SIM.
 * Un document nou „fișă de depunere" ar fi un format oficial inventat, ceea ce proiectul nu face.
 *
 * ⚠️ Rămâne deschisă întrebarea **AF** — dacă portalul SIM chiar cere tone la încărcare, sau
 * acceptă kilograme. Actul zice tone; practica o știe numai ea. Afișarea e nedistructivă în
 * ambele cazuri: arată amândouă unitățile, nu înlocuiește una cu alta.
 */

/**
 * Kilograme → tone. Împărțire exactă la 1000, fără rotunjire ascunsă: `450` → `0.45`.
 *
 * Rotunjirea o face **formatarea**, nu conversia, ca să nu se piardă cifre înainte de a fi văzute.
 */
export function kgToTonnes(kilograms: number): number {
  return kilograms / 1000;
}

/**
 * Trei zecimale: la tone, a treia zecimală e chiar kilogramul, deci e ultima cifră care mai
 * înseamnă ceva fizic. Sub ea n-are ce se pierde.
 */
const tonnesFormat = new Intl.NumberFormat("ro-RO", {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

/** Cantitatea în tone, formatată pentru afișare. Primește kilograme. */
export function formatTonnes(kilograms: number): string {
  return tonnesFormat.format(kgToTonnes(kilograms));
}
