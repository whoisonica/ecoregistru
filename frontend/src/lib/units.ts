/**
 * Formatarea cantităților de deșeu. Un singur loc, fiindcă un factor 1000 pus de mână în două
 * ecrane e cum se raportează de o mie de ori mai mult sau mai puțin.
 *
 * **Totul se arată în kilograme.** Evidența se ține în kg, formularele tipărite sunt în kg (toate
 * cele 33 de foi completate primite de la specialistă), iar depunerea din 15 martie se face tot în kg
 * (Andreea, 14.09.2026, întrebarea AF). OUG 92/2021 art. 48 alin. (1) scrie „în tone”, iar între
 * 04.09 și 17.09.2026 Evidențe și Termene arătau o cifră în tone după el — „1,060 t” se citea ca o
 * mie de tone (proprietarul, 17.09.2026), deci s-a renunțat.
 *
 * Singura cifră rămasă în tone e pragul de 1 t/an al Anexei 2 (HG 1061/2008), calculat pe server.
 */

/**
 * Kilograme, cu punct la mii și fără zecimale forțate: „1.060”, nu „1,060” (care arată ca tone).
 */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });

export function formatKg(kilograms: number): string {
  return kgFormat.format(kilograms);
}

/**
 * O cifră care e **deja** în tone — pragul de 1 t/an al Anexei 2. Trei zecimale: a treia e chiar
 * kilogramul, ultima cifră care mai înseamnă ceva fizic.
 */
const tonnesFormat = new Intl.NumberFormat("ro-RO", {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

export function formatTonnesValue(tonnes: number): string {
  return tonnesFormat.format(tonnes);
}
