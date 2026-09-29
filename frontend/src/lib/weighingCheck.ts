/**
 * Verificarea „sunt linii necântărite?” de dinaintea descărcărilor din dosar, peste anii ceruți.
 *
 * <p>Trei stări, nu două (29.09.2026): până acum descărcarea putea pleca înainte ca anii să fi venit, iar
 * o listă încă neîncărcată arăta ca una fără linii necântărite — avertismentul se sărea tăcut, exact ca
 * la eroarea reparată pe 20.09. `loading` = se așteaptă, `unknown` = un an n-a putut fi citit (se spune,
 * nu se ghicește), `ready` = răspunsul, cu liniile găsite.
 */
export type WeighingCheck<T> =
  | { state: "loading" }
  | { state: "unknown" }
  | { state: "ready"; pending: T[] };

export function weighingCheck<T extends { awaitingWeighing: boolean }>(
  years: { isLoading: boolean; isError: boolean; data?: T[] }[]
): WeighingCheck<T> {
  if (years.some((y) => y.isError)) return { state: "unknown" };
  if (years.some((y) => y.isLoading)) return { state: "loading" };
  return { state: "ready", pending: years.flatMap((y) => y.data ?? []).filter((r) => r.awaitingWeighing) };
}
