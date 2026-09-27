/**
 * Ce spune coada despre un rând care se reîncearcă (`outbox.ts`), fără nimic de Expo, ca să se poată proba
 * în Node. Forma erorii e cea a lui `ApiError` din `api.ts`: `status` și propoziția serverului.
 */
type ServerError = { status: number; serverMessage?: string | null };

function serverError(error: unknown): ServerError | null {
  const status = (error as { status?: unknown } | null)?.status;
  return typeof status === "number" ? (error as ServerError) : null;
}

/**
 * Propoziția scrisă pe un rând „de trimis” după o eroare a serverului (5xx, 429). Înainte rândul rămânea
 * „de trimis” fără niciun cuvânt, oricât l-ar fi respins serverul. Fără semnal nu se scrie nimic.
 */
export function retryNote(error: unknown): string | null {
  const e = serverError(error);
  return e ? (e.serverMessage ?? `HTTP ${e.status}`) : null;
}

/** După câte încercări căzute pe 5xx un rând e un defect al serverului, nu o toană a lui. */
export const STUCK_AFTER = 5;

/**
 * Un rând pe care serverul îl tot respinge cu 5xx se raportează în Sentry — o singură dată, la a cincea
 * încercare. `ApiError` nu se raporta niciodată, deci o cerere pe care serverul n-o poate primi stătea
 * „de trimis” pe telefon fără să afle nimeni.
 */
export function stuckOnServer(error: unknown, attempts: number): boolean {
  const e = serverError(error);
  return !!e && e.status >= 500 && attempts === STUCK_AFTER;
}
