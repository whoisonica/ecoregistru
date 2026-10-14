/**
 * Tema de culori (15.10.2026, `docs/specs/2026-10-14-teme-culori-plan.md`): Luminos (implicit), Pădure, Întunecat.
 * Alegerea stă în browser, ca panoul strâns: fără API, fără migrare; pe alt calculator omul o alege din nou.
 * Culorile sunt în `index.css`, după atributul `data-theme` de pe `<html>`; Luminos e `:root`, deci fără atribut.
 * Scriptul din `index.html` face același lucru înainte de bundle, ca pagina să nu clipească în tema greșită:
 * cheia și valorile de acolo trebuie să rămână ca aici.
 */
export type Theme = "luminos" | "padure" | "intunecat";

export const THEME_STORAGE_KEY = "eco360.tema";
export const THEMES: readonly Theme[] = ["luminos", "padure", "intunecat"];
export const DEFAULT_THEME: Theme = "luminos";

/** Ce stă în `localStorage` → o temă; orice altceva (gol, o valoare veche, o greșeală) → Luminos. */
export function parseTheme(raw: string | null | undefined): Theme {
  return THEMES.includes(raw as Theme) ? (raw as Theme) : DEFAULT_THEME;
}

type Storage = Pick<globalThis.Storage, "getItem" | "setItem">;
type Root = { setAttribute(name: string, value: string): void; removeAttribute(name: string): void };

/** Citirea poate arunca (fereastră privată, date blocate): atunci e Luminos. */
export function readTheme(storage: Storage | undefined = globalThis.localStorage): Theme {
  try {
    return parseTheme(storage?.getItem(THEME_STORAGE_KEY));
  } catch {
    return DEFAULT_THEME;
  }
}

/** Pune tema pe pagină pe loc și o ține minte; dacă browserul nu lasă scrierea, tema rămâne măcar până la reîncărcare. */
export function applyTheme(
  theme: Theme,
  root: Root | undefined = globalThis.document?.documentElement,
  storage: Storage | undefined = globalThis.localStorage
): void {
  if (theme === DEFAULT_THEME) root?.removeAttribute("data-theme");
  else root?.setAttribute("data-theme", theme);
  try {
    storage?.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // fereastră privată: alegerea ține cât pagina
  }
}
