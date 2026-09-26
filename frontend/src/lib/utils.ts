import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { countOf } from "@/lib/count";
import { formatDate, todayIso } from "@/lib/dates";

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

export { countOf, formatDate, todayIso };

/**
 * Un şir din `strings.ts` cu numeralul deja acordat pus în locul lui `{count}`.
 *
 * <p>Convenţia care iese de aici, şi care ţine cât timp e respectată peste tot:
 * **`{count}` e un grup nominal** („3 mişcări"), construit de `countOf`; **`{n}` e o cifră goală**
 * — un ordinal („fişierul 2 din 5") sau un număr între paranteze („Inactive (4)"), unde nu urmează
 * niciun substantiv de acordat. Cine scrie un şir nou alege placeholderul după asta, iar cine
 * caută `{count}` găseşte toate locurile în care regula de limbă chiar se aplică.
 *
 * <p>Exista înainte de felia asta o singură cale prin `countOf`, chemată de şase ori pe Panou şi
 * o dată în `deadlines.ts`; restul ecranelor înlocuiau cifra direct şi scriau „1 mişcări".
 */
export function withCount(template: string, n: number, one: string, many: string): string {
  return template.replace("{count}", countOf(n, one, many));
}
