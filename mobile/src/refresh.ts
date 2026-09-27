/**
 * G1 — o reîmprospătare căzută scoate omul din cont numai când serverul a refuzat sesiunea.
 *
 * <p>`/auth/refresh` refuză o sesiune moartă (revocată, expirată, cont oprit) mereu cu 400
 * `device.session.invalid`. Fără semnal, cu serverul căzut (5xx) sau ocupat (429), sesiunea lungă e
 * încă bună: dacă aplicația ar ieși din cont atunci, magazionerul fără semnal la rampă ar fi scos
 * din cont și n-ar mai putea pune predări în coadă. Aceeași graniță ca refuzul din coadă (`outbox.ts`).
 */
export function sessionIsDead(error: unknown): boolean {
  const status = (error as { status?: unknown } | null)?.status;
  return typeof status === "number" && status >= 400 && status < 500 && status !== 429;
}

/**
 * Ce se scrie după o reîmprospătare, care poate dura (semnal slab): sesiunea de <b>acum</b>, cu tokenurile
 * noi — nu cea de la pornirea cererii. Altfel o firmă aleasă între timp se întorcea la cea veche, iar un om
 * ieșit din cont era băgat înapoi. Null când sesiunea s-a schimbat între timp (ieșire sau alt cont):
 * tokenurile noi nu mai sunt ale nimănui și se sting.
 */
export function afterRefresh<S extends { auth: { refreshToken: string | null } }, A>(
  before: S,
  now: S | null,
  fresh: A,
): (Omit<S, "auth"> & { auth: A }) | null {
  if (!now || now.auth.refreshToken !== before.auth.refreshToken) return null;
  return { ...now, auth: fresh };
}

/**
 * Ieșirea din cont trimisă fără răspuns (`error` null = a mers): se ține minte și se trimite din nou la
 * următoarea pornire, altfel sesiunea rămânea vie pe server 60 de zile, cu notificările firmei pe telefon.
 * Aceeași graniță ca la reîmprospătare: se renunță numai când serverul a răspuns cu un refuz.
 */
export function keepPendingLogout(error: unknown): boolean {
  return error != null && !sessionIsDead(error);
}
