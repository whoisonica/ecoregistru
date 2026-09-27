const WEEKDAYS = ["duminică", "luni", "marți", "miercuri", "joi", "vineri", "sâmbătă"];

/** Zile întregi între două date yyyy-MM-dd, în UTC, ca schimbarea orei să nu mute nimic. */
function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000);
}

/**
 * Termenul SIATD al unei recepții, pe rândul din Cântar → SIATD (F6a): „azi”, „mâine”, „joi 08.10”, iar pentru un termen
 * trecut „trecut de 2 zile”. `today` vine din afară (ziua României), nu din ceasul browserului.
 */
export function formatDue(due: string, today: string): string {
  const diff = daysBetween(today, due);
  if (diff === 0) return "azi";
  if (diff === 1) return "mâine";
  if (diff < 0) return diff === -1 ? "trecut de o zi" : `trecut de ${-diff} zile`;
  const [, month, day] = due.split("-");
  return `${WEEKDAYS[new Date(`${due}T00:00:00Z`).getUTCDay()]} ${day}.${month}`;
}
