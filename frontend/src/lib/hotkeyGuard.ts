/*
 * Partea pură din `useHotkey`: când o tastă simplă e comandă și când nu. Fără DOM și fără React, ca să
 * se poată proba sub `npm test`.
 */

/** Ce citește garda dintr-un element — un `HTMLElement` are toate trei. */
export interface KeyTarget {
  tagName: string;
  isContentEditable?: boolean;
  getAttribute(name: string): string | null;
}

/** Unde o tastă simplă înseamnă text, nu comandă. */
export function isTypingTarget(el: KeyTarget | null): boolean {
  if (!el) return false;
  const tag = el.tagName;
  return (
    tag === "INPUT" ||
    tag === "TEXTAREA" ||
    tag === "SELECT" ||
    Boolean(el.isContentEditable) ||
    // Comboboxul nostru e un buton cu rol de combobox: săgețile și literele sunt ale lui.
    el.getAttribute("role") === "combobox"
  );
}

export interface HotkeyContext {
  /** Apăsarea vine dintr-un câmp de text (`isTypingTarget`). */
  typing: boolean;
  /** Pe ecran e un strat modal (`[aria-modal="true"]`: `Dialog`, paleta de comenzi). */
  modalOpen: boolean;
  whileTyping: boolean;
  inDialog: boolean;
}

/**
 * Dacă scurtătura are voie să lucreze.
 *
 * <p>Cu un dialog deschis, tastele paginii de dedesubt tac (29.09.2026). Până acum le oprea doar un
 * câmp de text: cu focusul pe un buton din formularul de mișcare, o cifră schimba ecranul și arunca
 * formularul, iar N pe „Generare” golea `editing` sub dialogul deschis — „Salvează” crea apoi o
 * mișcare **nouă**, dublura celei editate. Lucrează doar ce e legat pentru dialog (`inDialog`): Escape
 * și Ctrl+K ale paletei de comenzi, care e ea însăși un strat modal.
 */
export function hotkeyAllowed({ typing, modalOpen, whileTyping, inDialog }: HotkeyContext): boolean {
  if (typing && !whileTyping) return false;
  if (modalOpen && !inDialog) return false;
  return true;
}
