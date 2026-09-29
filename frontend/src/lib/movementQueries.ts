/*
 * Fără `import`, dinadins: se probează sub `npm test`, unde `api.ts` nu se poate încărca.
 */

/**
 * Tot ce se citește din mișcări și trebuie recitit după orice scriere a lor — salvare, ștergere,
 * cântărire, atașament. `useMovements.ts` le invalidează pe toate dintr-un loc.
 *
 * <p>29.09.2026: rămâneau doar mișcările și evidența. Tabul „Ambalaje” (`["packaging"]`) și dosarul de
 * control (`["audit-file-contents"]`, `["audit-file-size"]`) păstrau 30 de secunde (`staleTime` din
 * `main.tsx`) cifrele de dinainte: o predare tocmai cântărită apărea încă „de cântărit”, iar dosarul
 * număra mișcarea ștearsă.
 */
export const MOVEMENT_DEPENDENT_KEYS: readonly (readonly string[])[] = [
  ["movements"],
  ["evidences"],
  ["packaging"],
  ["audit-file-contents"],
  ["audit-file-size"],
];
