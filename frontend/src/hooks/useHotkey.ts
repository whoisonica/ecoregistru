import { useEffect, useRef } from "react";

/** Unde o tastă simplă înseamnă text, nu comandă. */
function isTyping(target: EventTarget | null): boolean {
  const el = target as HTMLElement | null;
  if (!el) return false;
  const tag = el.tagName;
  return (
    tag === "INPUT" ||
    tag === "TEXTAREA" ||
    tag === "SELECT" ||
    el.isContentEditable ||
    // Comboboxul nostru e un buton cu rol de combobox: săgețile și literele sunt ale lui.
    el.getAttribute("role") === "combobox"
  );
}

interface HotkeyOptions {
  /** Cere Ctrl (sau Cmd pe Mac). Fără el, tasta e o literă simplă. */
  ctrl?: boolean;
  /**
   * Lasă scurtătura să lucreze și dintr-un câmp de text. Implicit `false`: `n` apăsat în timp ce
   * se scrie o notă trebuie să scrie un „n", nu să deschidă un formular.
   */
  whileTyping?: boolean;
  enabled?: boolean;
}

/**
 * O scurtătură de tastatură, atâta timp cât componenta e pe ecran.
 *
 * <p>Aplicația se folosește toată ziua, de aceiași oameni, pentru introdus date — și n-avea nicio
 * scurtătură. Fiecare căutare începea cu o plimbare a mâinii la maus.
 *
 * <p>Handlerul se ține într-o referință, ca schimbarea lui între randări să nu re-lege ascultătorul
 * la fiecare tastă apăsată — altfel un `useEffect` cu `onPress` în dependențe ar dezabona și
 * reabona de zeci de ori pe secundă în timpul scrisului.
 */
export function useHotkey(
  key: string,
  onPress: (event: KeyboardEvent) => void,
  { ctrl = false, whileTyping = false, enabled = true }: HotkeyOptions = {}
) {
  const handler = useRef(onPress);
  handler.current = onPress;

  useEffect(() => {
    if (!enabled) return;
    function onKeyDown(e: KeyboardEvent) {
      if (e.key.toLowerCase() !== key.toLowerCase()) return;
      // `metaKey` pentru Mac: acolo Ctrl+K e altceva, iar reflexul e Cmd+K.
      if (ctrl !== (e.ctrlKey || e.metaKey)) return;
      // Alt schimbă înțelesul tastei în multe aranjamente; nu ne-o însușim.
      if (e.altKey) return;
      if (!whileTyping && isTyping(e.target)) return;
      e.preventDefault();
      handler.current(e);
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [key, ctrl, whileTyping, enabled]);
}
