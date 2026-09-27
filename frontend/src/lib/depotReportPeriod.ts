/** D4.7 — perioada rapoartelor de depozit: scurtăturile și aceeași limită ca serverul. */

export type Shortcut = "LAST_MONTH" | "THIS_MONTH" | "QUARTER" | "YEAR";

const iso = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

/** Intervalul unei scurtături, pe calendarul de azi; trimestrul și anul întregi, ca declarațiile. */
export function periodOf(shortcut: Shortcut, today = new Date()): { from: string; to: string } {
  const y = today.getFullYear();
  const m = today.getMonth();
  switch (shortcut) {
    case "LAST_MONTH":
      return { from: iso(new Date(y, m - 1, 1)), to: iso(new Date(y, m, 0)) };
    case "THIS_MONTH":
      return { from: iso(new Date(y, m, 1)), to: iso(new Date(y, m + 1, 0)) };
    case "QUARTER": {
      const q = Math.floor(m / 3) * 3;
      return { from: iso(new Date(y, q, 1)), to: iso(new Date(y, q + 3, 0)) };
    }
    case "YEAR":
      return { from: iso(new Date(y, 0, 1)), to: iso(new Date(y, 11, 31)) };
  }
}

/** Cel mult un an, cu „De la” înainte de „Până la” — aceeași regulă ca serverul (366 de zile). */
export function periodValid(from: string, to: string): boolean {
  if (!from || !to || from > to) return false;
  const days = (Date.parse(to) - Date.parse(from)) / 86_400_000 + 1;
  return days <= 366;
}
