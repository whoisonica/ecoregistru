/**
 * Adresa de după o schimbare de filtru — partea pură din `useUrlState`, ca să se poată proba.
 *
 * <p>Pornește de la un **șir**, nu de la instanța dată de React Router: vezi comentariul din
 * `useUrlState` (29.09.2026) pentru de ce sursa e adresa curentă a browserului.
 *
 * <p>Valoarea implicită nu se scrie: `?luna=` pe un ecran neatins e zgomot.
 */
export function withUrlParam(search: string, key: string, next: string, fallback: string): string {
  const copy = new URLSearchParams(search);
  if (!next || next === fallback) copy.delete(key);
  else copy.set(key, next);
  return copy.toString();
}
