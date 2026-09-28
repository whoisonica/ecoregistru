/*
 * Fără niciun `import`, dinadins, ca `count.ts`: `deadlines.ts` îl cheamă, iar aplicația de telefon
 * (`mobile/`) importă `deadlines.ts` direct. Din `utils.ts`, `formatDate` trăgea după el `clsx` și
 * `tailwind-merge`, pe care Metro nu le vede — telefonul nu mai pornea (26.09.2026). `utils.ts` le
 * reexportă, deci webul le cheamă ca înainte.
 */

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
  // BUG-062: un moment (`2026-03-12T22:30:00Z`, bifa unui termen, data unei cereri) e altceva decât
  // o zi: tăiat, o bifă de la 01:30, ora României, arăta ziua de ieri. Se citește în ora locală.
  if (iso.includes("T")) {
    const t = new Date(iso);
    if (!Number.isNaN(t.getTime())) {
      const pad = (n: number) => String(n).padStart(2, "0");
      return `${pad(t.getDate())}.${pad(t.getMonth() + 1)}.${t.getFullYear()}`;
    }
  }
  const [y, m, d] = iso.slice(0, 10).split("-");
  return d && m && y ? `${d}.${m}.${y}` : iso;
}

/**
 * Ziua de azi, `yyyy-MM-dd`, din calendarul local. BUG-037: `toISOString()` dă ziua din UTC, deci
 * între 00:00 și 03:00, ora României, propunea ziua de ieri — la 1 ianuarie, anul trecut.
 */
export function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/**
 * D2 (28.09.2026) — luna unei zile, dacă e încheiată față de azi; `null` pentru luna curentă sau una viitoare.
 * O operațiune dintr-o lună încheiată schimbă totaluri poate deja declarate (reținerile, stocul, registrele),
 * deci se finalizează sau se anulează doar după o confirmare. Serverul cere aceeași confirmare.
 */
export function closedMonth(
  iso: string | null | undefined,
  today: string = todayIso()
): { year: number; month: number } | null {
  if (!iso) return null;
  const key = iso.slice(0, 7);
  return key < today.slice(0, 7) ? { year: Number(key.slice(0, 4)), month: Number(key.slice(5, 7)) } : null;
}
