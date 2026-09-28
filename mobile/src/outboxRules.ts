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

export type TicketStatus = "sending" | "queued" | "sent" | "photoFailed" | "rejected";

type TicketRow = { state: "PENDING" | "REJECTED"; movementId: string | null; attempts: number; error: string | null };

/**
 * Ce spune bonul de după „Salvează” (`app/gata.tsx`) despre rândul lui din coadă. „Se trimite” numai cât prima
 * încercare e în aer: după o cădere rândul e „De trimis”, chiar cu semnal. Înainte, pe rețea bună și cu serverul
 * care nu răspundea, bonul rămânea „Se trimite…” la nesfârșit (27.09.2026).
 */
export function ticketStatus({ loaded, online, row }: { loaded: boolean; online: boolean; row?: TicketRow }): TicketStatus {
  // Până la prima citire a cozii, rândul e socotit încă pe drum: nici bifă, nici vibrație pe nimic.
  if (!loaded) return online ? "sending" : "queued";
  if (!row) return "sent";
  if (row.state === "REJECTED") return row.movementId ? "photoFailed" : "rejected";
  // Predarea e pe server, poza a căzut cel puțin o dată; se reîncearcă singură.
  if (row.movementId && (row.attempts > 0 || row.error)) return "photoFailed";
  return online && row.attempts === 0 ? "sending" : "queued";
}

/**
 * Poza unui rând nu mai e pe telefon (ștearsă, sistemul a golit folderul). Nicio reîncercare n-o aduce înapoi,
 * deci rândul trece la „refuzat”, cu predarea — dacă a plecat deja — păstrată pe server.
 */
export class PhotoMissingError extends Error {
  constructor() {
    super("Photo file is missing");
    this.name = "PhotoMissingError";
  }
}

/** Fără semnal, timp expirat, cerere întreruptă: legătura, nu rândul. */
function isConnectionFailure(error: unknown): boolean {
  if (error instanceof TypeError && /network request failed/i.test(error.message)) return true;
  const name = (error as { name?: unknown } | null)?.name;
  return name === "TimeoutError" || name === "AbortError";
}

/**
 * Rândul nu se mai reîncearcă: serverul l-a refuzat (4xx, fără 429) sau poza lui nu mai e pe telefon. Rămâne
 * „refuzat”, cu propoziția lui, până îl scoate sau îl corectează omul.
 */
export function rejectsRow(error: unknown): boolean {
  if (error instanceof PhotoMissingError) return true;
  const server = serverError(error);
  return !!server && server.status >= 400 && server.status < 500 && server.status !== 429;
}

/**
 * După o cădere, coada trece la rândul următor când vina e a rândului, nu a legăturii: serverul **a răspuns**
 * cu 5xx (de pildă poza unei predări deja salvate, cu stocarea pozelor căzută), sau a căzut ceva pe telefon
 * (poză lipsă, o citire de fișier). Înainte orice cădere oprea tot, deci o poză care nu urca (28.09.2026) sau
 * una ștearsă de pe disc (B1) ținea pe loc predările de după ea la nesfârșit.
 * Fără rețea, la timp expirat sau la 429 („prea multe cereri”) n-are rost să le încercăm și pe celelalte acum.
 */
export function skipsToNext(error: unknown): boolean {
  if (isConnectionFailure(error)) return false;
  const server = serverError(error);
  return server ? server.status >= 500 : true;
}

/**
 * O singură trecere în aer; o cerere venită între timp nu primește trecerea veche (care și-a citit deja rândurile),
 * ci una în plus, pornită după ea. Oricâte cereri vin cât una e în aer se strâng într-o singură trecere în plus.
 */
export function coalesce<T>(run: () => Promise<T>): () => Promise<T> {
  let current: Promise<T> | null = null;
  let next: Promise<T> | null = null;
  const start = (): Promise<T> => {
    const p = run().finally(() => {
      if (current === p) current = null;
    });
    current = p;
    return p;
  };
  return () => {
    if (!current) return start();
    next ??= current
      .catch(() => undefined)
      .then(() => {
        next = null;
        return start();
      });
    return next;
  };
}
