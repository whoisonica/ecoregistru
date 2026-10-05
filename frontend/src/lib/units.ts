import type { Unit } from "@/lib/types";

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
 * Kilograme: punct la mii, virgulă la zecimale — „1.060”, iar un rând vechi cu grame „35,125”.
 *
 * <p>G06 (20.09.2026) a pus aici **întotdeauna trei zecimale**, ca „35.125" și „35,125" să nu mai
 * poată fi confundate. Din 30.09.2026 cantitatea în kg se tastează numai întreagă („te lasa sa pui
 * cu ,000”), deci „150,000 kg” pe Generare era zgomot (proprietarul, 05.10.2026: „in generare a
 * ramas 150,000 kg”). Acum un kg întreg se scrie fără virgulă, iar un rând vechi care chiar are
 * zecimale le păstrează pe toate trei.
 *
 * <p>Invariantul lui G06 rămâne: formatul e mereu cel românesc, deci punctul e mereu la mii, iar o
 * virgulă, când apare, e urmată de exact trei cifre. „35.125” e treizeci și cinci de mii, „35,125”
 * e treizeci și cinci de kilograme — tot nu se pot citi unul drept celălalt.
 */
const kgWholeFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 });
const kgFormat = new Intl.NumberFormat("ro-RO", {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

export function formatKg(kilograms: number): string {
  // Pe grame, nu `Number.isInteger`: o sumă de zecimale („0,1 + 0,2”) nu iese întreagă la bit.
  return Math.round(kilograms * 1000) % 1000 === 0 ? kgWholeFormat.format(kilograms) : kgFormat.format(kilograms);
}

/**
 * Kilograme rotunjite, pentru rezumate (Acasă) — „15 kg”, nu „15,000 kg”. Nimeni nu cântărește la
 * gram (proprietarul, 29.09.2026). Lipsa virgulei spune că e un rezumat, nu o cifră de transcris.
 */
const kgSummaryFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 });

export function formatKgSummary(kilograms: number): string {
  return kgSummaryFormat.format(kilograms);
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

/**
 * O cantitate de pe un rând, în unitatea ei — **singurul mod în care se scrie una pe ecran**.
 *
 * <p>Până pe 20.09.2026, lista de mişcări tipărea cifra brută din JSON (`{m.quantity}`), adică aşa
 * cum o scrie JavaScript: „35.125", cu punct zecimal. Deasupra ei, banda de totaluri scria „12.640"
 * — douăsprezece mii şase sute patruzeci, rotunjite dinadins, fiindcă e un rezumat. Cele două stăteau
 * una sub alta şi **nu se puteau deosebi**: acelaşi semn, două înţelesuri care diferă de o mie de ori.
 * Chiar invariantul pe care fişierul ăsta îl declară rezolvat (G06).
 *
 * <p>Acum orice cifră de transcris trece pe aici, în unitatea rândului: kilogramele ca la `formatKg`
 * (întregi, din 05.10.2026), tonele cu exact trei zecimale. Banda şi panoul rămân rotunjite la
 * kilogram întreg, tot dinadins — sunt rezumate, nu cifre de transcris.
 */
export function formatQuantity(value: number, unit: Unit): string {
  return unit === "TONS" ? formatTonnesValue(value) : formatKg(value);
}

/**
 * Cantitatea tastată în formular, scrisă ca pe listă (29.09.2026). Bonul formularului arăta textul brut
 * din `<input type="number">` — „35.125 kg”, cu punctul zecimal al lui JavaScript, adică exact
 * ambiguitatea pe care G06 a scos-o din rest. `null` pe o valoare care nu e număr: bonul o arată cum e.
 */
export function formatQuantityInput(raw: string, unit: Unit): string | null {
  const value = Number(raw);
  return raw.trim() !== "" && Number.isFinite(value) ? formatQuantity(value, unit) : null;
}

/** Număr cu exact `digits` zecimale, în format românesc („1.234,500”). */
export function formatDecimal(n: number, digits: number): string {
  return new Intl.NumberFormat("ro-RO", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(n);
}
