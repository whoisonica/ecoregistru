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
 * Kilograme: punct la mii, virgulă la zecimale, **întotdeauna trei zecimale** — „1.060,000”.
 *
 * <p>Zecimalele sunt forțate dinadins (G06, 20.09.2026). Până acum aceeași cantitate se scria în
 * patru feluri: „35.125" în listă, „35,125" pe total, „35.125" pe fișă, „120,500" pe aviz — iar
 * „35.125" și „35,125" sunt, în românește, două numere care diferă de o mie de ori. Un cititor
 * n-avea din ce să deducă ce convenție s-a folosit pe hârtia din mână.
 *
 * <p>Regula care omoară ambiguitatea nu e „alegem punctul" sau „alegem virgula", ci **grupul de
 * după virgulă are mereu exact trei cifre**: atunci virgula e mereu ultima și punctul e mereu la
 * mii, indiferent de cifră. Răspunde și obiecției de la 17.09 („1,060” arată ca tone): „1.060,000”
 * nu poate fi citit ca tone.
 */
const kgFormat = new Intl.NumberFormat("ro-RO", {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

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

/**
 * O cantitate de pe un rând, în unitatea ei — **singurul mod în care se scrie una pe ecran**.
 *
 * <p>Până pe 20.09.2026, lista de mişcări tipărea cifra brută din JSON (`{m.quantity}`), adică aşa
 * cum o scrie JavaScript: „35.125", cu punct zecimal. Deasupra ei, banda de totaluri scria „12.640"
 * — douăsprezece mii şase sute patruzeci, rotunjite dinadins, fiindcă e un rezumat. Cele două stăteau
 * una sub alta şi **nu se puteau deosebi**: acelaşi semn, două înţelesuri care diferă de o mie de ori.
 * Chiar invariantul pe care fişierul ăsta îl declară rezolvat (G06).
 *
 * <p>Acum orice cifră de transcris trece pe aici, deci are mereu virgulă şi exact trei zecimale, în
 * unitatea rândului. Banda şi panoul rămân rotunjite la kilogram întreg, tot dinadins — sunt
 * rezumate, nu cifre de transcris —, iar lipsa virgulei e chiar semnul că e un rezumat.
 */
export function formatQuantity(value: number, unit: Unit): string {
  return unit === "TONS" ? formatTonnesValue(value) : formatKg(value);
}
