/*
 * Fără `import`, dinadins: `api.ts` citește `import.meta.env` și nu se poate încărca sub `npm test`.
 */

/** Ruta de pe server — sub `/api/v1`, ca toate celelalte. */
export const SIGN_OUT_PATH = "/api/v1/auth/sign-out";

/**
 * Cererea de „Deconectare”, cu tokenul pus de mână (29.09.2026).
 *
 * <p>Pleca pe `/auth/sign-out` — fără `/api/v1`, deci într-o rută care nu există — și, chiar cu
 * ruta bună, fără token: interceptorul de cereri al lui axios rulează asincron, după ce `logout()`
 * golise deja `localStorage`. Serverul nu afla niciodată de ieșire, iar tokenul copiat înainte
 * rămânea bun opt ore. Tokenul se citește deci înainte de golire și se trimite în antet.
 *
 * <p>`null` când nu e niciun token: nu e nicio sesiune de stins.
 */
export function signOutRequest(token: string | null): { url: string; headers: Record<string, string> } | null {
  if (!token) return null;
  return { url: SIGN_OUT_PATH, headers: { Authorization: `Bearer ${token}` } };
}
