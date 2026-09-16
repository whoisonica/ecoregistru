/*
 * Fără niciun `import`, dinadins: `deadlines.ts` și `readiness.ts` îl cheamă, iar aplicația de telefon
 * (`mobile/`) le importă pe amândouă direct. În `utils.ts` ar fi tras după el `clsx` și `tailwind-merge`,
 * pe care telefonul nu le are.
 */

/**
 * Numeralul românesc, cu forma de plural pe care o cere.
 *
 * <p>Româna are **trei** forme acolo unde engleza are două: `1 linie` · `2 linii` ·
 * **`20 de linii`**. Prepoziția „de" intră de la 20 în sus și se întoarce la fiecare sută —
 * `101 linii`, dar `120 de linii` —, deci regula se citește pe ultimele două cifre, nu pe număr.
 *
 * <p>Ecranul scria până acum „1 linii cu ieșiri fără cod R/D" pe Panou, și „pe 1 mișcări din luna
 * aceasta". Backendul dăduse deja peste aceeași regulă la mailul de expirare a autorizației
 * (`theWordingAgreesWithSmallNumbers`, 06.09), unde „expiră în 60 zile" a fost reparat cu același
 * prag; interfața rămăsese în urmă.
 *
 * <p>Se cheamă doar cu `n >= 1`: „0 de linii" e corect gramatical și se citește prost, iar ecranele
 * au oricum text propriu pentru gol („nicio mișcare înregistrată luna aceasta").
 */
export function countOf(n: number, one: string, many: string): string {
  if (n === 1) return `1 ${one}`;
  const lastTwo = Math.abs(n) % 100;
  const needsDe = lastTwo === 0 || lastTwo >= 20;
  return needsDe ? `${n} de ${many}` : `${n} ${many}`;
}
