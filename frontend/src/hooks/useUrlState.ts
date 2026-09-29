import { useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { withUrlParam } from "@/lib/urlParam";

/**
 * O bucată de stare care trăiește în bara de adrese.
 *
 * <p>Filtrele erau `useState`: pleca de pe Mișcări și se întorcea, iar luna aleasă se pierdea. Mai
 * rău, nu se putea trimite nimănui „uite ce am eu pe ecran" — un link către evidența pe 2025, pe
 * punctul de lucru din Cluj, n-avea cum să existe.
 *
 * <p>Scrierea e cu `replace`, nu `push`: fiecare apăsare pe un select ar fi altfel o intrare în
 * istoric, iar butonul Înapoi al browserului ar începe să desfacă filtre în loc să schimbe pagina.
 *
 * <p>Valoarea implicită nu se scrie în URL. O adresă care poartă `?an=2026&luna=` pe un ecran pe
 * care nimeni n-a atins nimic e zgomot, și face două legături diferite pentru aceeași vedere.
 */
export function useUrlState(
  key: string,
  fallback = ""
): [string, (next: string) => void] {
  const [params, setParams] = useSearchParams();
  const value = params.get(key) ?? fallback;

  const set = useCallback(
    (next: string) => {
      // Se pornește de la adresa din bara browserului, nu de la `current` (29.09.2026): React Router
      // 6 dă funcției parametrii **randării**, nu pe cei scriși de apelul dinainte, deci „Șterge
      // filtrele” — patru setteri în același clic — lăsa doar ultima ștergere și trei filtre la
      // loc. Sub `BrowserRouter` (`App.tsx`), `navigate` scrie `history.replaceState` sincron, așa
      // că `window.location.search` e deja adresa lăsată de setterul de dinainte.
      setParams(() => new URLSearchParams(withUrlParam(window.location.search, key, next, fallback)), {
        replace: true,
      });
    },
    [key, fallback, setParams]
  );

  return [value, set];
}

/** Varianta pentru filtrele care sunt numere — anul, luna. */
export function useUrlNumber(
  key: string,
  fallback: number
): [number, (next: number) => void] {
  const [raw, setRaw] = useUrlState(key, String(fallback));
  const parsed = Number(raw);
  // O valoare stricată în URL (cineva a editat adresa) nu trebuie să spargă pagina: cade pe
  // implicit, ca și cum n-ar fi fost scrisă.
  const value = Number.isFinite(parsed) ? parsed : fallback;
  const set = useCallback((next: number) => setRaw(String(next)), [setRaw]);
  return [value, set];
}
