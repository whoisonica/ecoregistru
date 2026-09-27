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
