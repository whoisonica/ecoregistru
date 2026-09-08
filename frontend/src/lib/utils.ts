import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/** Tailwind-aware className combiner (shadcn convention). */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Textul adus la forma în care se caută: litere mici, fără diacritice.
 *
 * <p>Fără ea, `miscari` nu găsea **Mișcări**, iar `deseuri` întorcea zero rânduri din cinci —
 * măsurat pe 07.09.2026. Nu erau rezultate parțiale: ecranul spunea „Niciun rezultat" pentru un
 * cuvânt care se vede în tabel. Or aplicația se folosește toată ziua, la introdus date, pe o
 * tastatură pe care diacriticele se scriu mai greu decât se citesc.
 *
 * <p>`NFD` desface litera în literă + semn, iar `\p{Diacritic}` scoate semnul: `ș → s`, `â → a`,
 * `î → i`, `ț → t`, `ă → a`. **Pliază şi cedila peste virgulă** — `deşeuri` și `deșeuri` devin
 * același lucru — ceea ce contează aici cu totul aparte: și codul, și datele proiectului le
 * amestecă pe amândouă, deci fără pliere două scrieri ale aceluiași cuvânt nu se găseau una pe
 * alta.
 *
 * <p>Se aplică **și** pe textul căutat, **și** pe ce s-a tastat. Aplicată pe una singură, ar muta
 * problema în loc s-o rezolve.
 */
export function fold(text: string): string {
  return text.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();
}

/**
 * O dată ISO (`2026-11-15`) scrisă cum se scrie în România: `15.11.2026`.
 *
 * <p>Funcția asta era copiată identic în patru ecrane — și lipsea din alte trei, care afișau data
 * brută din backend. Aplicația avea trei formate deodată: `15.11.2026` pe Mișcări, Termene și
 * Panou; `2026-11-15` pe Parteneri (badge-ul de expirare) și în registrul de ambalaje;
 * `toLocaleDateString` în inboxul de cereri. Pe un produs care tipărește formulare oficiale, data
 * e chiar rubrica pe care se uită omul întâi.
 *
 * <p>Nu trece prin `Date`: `new Date("2026-11-15")` e miezul nopții **UTC**, deci într-un fus
 * negativ ar scrie ziua dinainte. Aici se taie șirul, fiindcă ce vine de la server e o zi
 * calendaristică, nu un moment.
 *
 * @returns șirul gol pentru o valoare lipsă, ca apelantul să poată alege singur ce pune în loc
 */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  const [y, m, d] = iso.slice(0, 10).split("-");
  return d && m && y ? `${d}.${m}.${y}` : iso;
}

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
