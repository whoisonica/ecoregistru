/**
 * Ultima predare salvată de pe telefon, cât trăiește aplicația: ecranul de după „Salvează” (bonul) o
 * citește, iar „Încă o predare, la fel” o refolosește. Nu se scrie pe disc: e despre clipa de acum,
 * și coada (`outbox`) rămâne singura evidență a ce n-a plecat.
 *
 * <p>`sent` ține id-ul de pe server al fiecărui rând trimis din coadă în sesiunea asta, fiindcă rândul
 * se șterge după trimitere și bonul n-ar mai avea de unde ști numărul.
 */
export interface LastSaved {
  /** Cheia rândului din coadă (și de idempotență). */
  outboxId: string;
  payload: Record<string, unknown>;
  wasteCode: string;
  wasteCodeName: string;
  quantity: string;
  date: string;
  partnerName: string | null;
  operationCode: string | null;
  documentReference: string | null;
  vehicle: string | null;
  driverName: string | null;
  workPointName: string | null;
  hasPhoto: boolean;
}

let last: LastSaved | null = null;
const sent = new Map<string, string>();
const listeners = new Set<() => void>();

export function rememberSaved(item: LastSaved) {
  last = item;
}

export function lastSaved(): LastSaved | null {
  return last;
}

/** Coada a dus rândul pe server: de aici bonul își ia numărul și Anexa 3. */
export function noteSent(outboxId: string, movementId: string) {
  sent.set(outboxId, movementId);
  listeners.forEach((fn) => fn());
}

export function sentMovementId(outboxId: string): string | null {
  return sent.get(outboxId) ?? null;
}

export function onSent(fn: () => void) {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
}
