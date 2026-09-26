/**
 * Aceeași regulă ca `ValidCnp` din backend: 13 cifre, cheia 279146358279, restul 10 se scrie 1.
 *
 * <p>Aici, în `lib/`, ca s-o citească și telefonul (CNP-ul delegatului pe Anexa 3 / aviz, M1e): fără ea, o
 * cifră greșit tastată trecea de formular și predarea ieșea „refuzată” abia din coadă. Copia din
 * `NaturalPersonsSection` (depozitul) rămâne până o mută cine lucrează acolo.
 */
export function isValidCnp(cnp: string): boolean {
  if (!/^\d{13}$/.test(cnp)) return false;
  const key = "279146358279";
  let sum = 0;
  for (let i = 0; i < 12; i++) sum += Number(cnp[i]) * Number(key[i]);
  const control = sum % 11 === 10 ? 1 : sum % 11;
  return control === Number(cnp[12]);
}
