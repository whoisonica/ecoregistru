/**
 * BUG-024 (17.09.2026) — firma aleasă ține de tab, nu de browser.
 *
 * Înainte stătea numai în `localStorage`, comun tuturor taburilor: o schimbare de firmă într-un tab
 * trimitea cererile celuilalt spre firma nouă, în timp ce ecranul lui arăta tot firma veche. Așa a
 * plecat o verificare de import a Onsia SRL spre Ardeal Reciclare SRL.
 *
 * Acum fiecare tab își ține firma în `sessionStorage`, care nu se împarte între taburi și rezistă la
 * reîncărcare. `localStorage` rămâne doar ultima alegere, de unde pornește un tab nou. La prima
 * citire, tabul și-o copiază și de atunci nu mai ascultă de ce aleg celelalte.
 */
type Storage = Pick<globalThis.Storage, "getItem" | "setItem" | "removeItem">;

export function tabScopedStore(key: string, tab: Storage, shared: Storage) {
  return {
    get(): string | null {
      const own = tab.getItem(key);
      if (own !== null) return own;
      const last = shared.getItem(key);
      if (last !== null) tab.setItem(key, last);
      return last;
    },
    set(value: string) {
      tab.setItem(key, value);
      shared.setItem(key, value);
    },
    clear() {
      tab.removeItem(key);
      shared.removeItem(key);
    },
  };
}
