/**
 * B2 (28.09.2026): pozele din coadă și din ciornă se țin pe disc cu adresa întreagă. Pe iOS containerul
 * aplicației (`…/Containers/Data/Application/<UUID>/`) își poate schimba UUID-ul la o actualizare; fișierele
 * se mută cu el, dar adresa scrisă în bază arată spre cel vechi, deci poza „dispărea” și rândul nu mai pleca.
 *
 * <p>Nu se rescrie nimic în bază: la citire, adresa se mută sub containerul de acum, luat din folderul
 * documentelor. Rândurile vechi se repară singure, iar pe Android (căi fixe) adresa rămâne cum e.
 */
const CONTAINER = /^(.*\/Containers\/Data\/Application\/)[^/]+\//;

export function rebasePhoto(uri: string | null, documentUri: string): string | null {
  if (!uri) return uri;
  const now = CONTAINER.exec(documentUri);
  if (!now || !CONTAINER.test(uri)) return uri;
  return uri.replace(CONTAINER, now[0]);
}
