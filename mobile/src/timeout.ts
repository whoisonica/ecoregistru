/**
 * O cerere care nu primește răspuns în timpul dat cade ca una fără semnal.
 *
 * <p>`fetch` din React Native n-are timp limită pe Android: o cerere agățată pe un semnal slab rămânea
 * în aer până la repornirea aplicației, și cu ea coada („De trimis” nu mai pleca), reîmprospătarea
 * (orice cerere cu 401 aștepta după ea) și butonul de login. Căderea are forma erorii de rețea —
 * `TypeError: Network request failed` —, deci coada o reîncearcă, iar reîmprospătarea nu scoate omul
 * din cont (`refresh.ts`).
 */
export async function fetchWithTimeout(
  input: string,
  init: RequestInit,
  ms: number,
  fetchFn: typeof fetch = fetch,
): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), ms);
  try {
    return await fetchFn(input, { ...init, signal: controller.signal });
  } catch (error) {
    if (controller.signal.aborted) throw new TypeError(`Network request failed (no answer in ${ms / 1000} s)`);
    throw error;
  } finally {
    clearTimeout(timer);
  }
}
