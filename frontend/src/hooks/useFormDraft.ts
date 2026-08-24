import { useEffect, useRef, useState } from "react";

/** Drafts older than this are ignored: a form abandoned last month is not a form in progress. */
const MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000;
const DEBOUNCE_MS = 400;

interface StoredDraft<T> {
  version: number;
  savedAt: number;
  values: T;
}

/**
 * Keeps a long form alive across a lost page.
 *
 * The intake form has six sections, and a session expiring in another tab used to take all of it
 * with no warning (proba de acceptanță, 24.08.2026). Nothing here decides anything for the user:
 * the draft is restored visibly, with a way to throw it away, and it never outlives a submit.
 *
 * @param key      localStorage key, one per form
 * @param values   the current values, rebuilt on every render
 * @param apply    puts a restored draft back into the form's state
 * @param isEmpty  true when there is nothing worth keeping — an untouched or just-discarded
 *                 form. Without it, opening the page would store a blank draft and the next
 *                 visit would announce it had restored… nothing.
 * @param version  bump when the field set changes, so an old shape is dropped instead of applied
 */
export function useFormDraft<T extends object>(
  key: string,
  values: T,
  apply: (values: T) => void,
  isEmpty: (values: T) => boolean,
  version = 1
) {
  const [restored, setRestored] = useState(false);
  const applyRef = useRef(apply);
  applyRef.current = apply;
  const isEmptyRef = useRef(isEmpty);
  isEmptyRef.current = isEmpty;
  // Nothing is written until the draft has been read: a first render must not overwrite the
  // stored draft with the empty form it is about to replace.
  const loaded = useRef(false);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(key);
      if (raw) {
        const draft = JSON.parse(raw) as StoredDraft<T>;
        if (draft.version === version
            && Date.now() - draft.savedAt < MAX_AGE_MS
            && !isEmptyRef.current(draft.values)) {
          applyRef.current(draft.values);
          setRestored(true);
        } else {
          localStorage.removeItem(key);
        }
      }
    } catch {
      // A malformed or unreadable draft is not worth an error on a form the user just opened.
      localStorage.removeItem(key);
    }
    loaded.current = true;
  }, [key, version]);

  useEffect(() => {
    if (!loaded.current) return;
    const handle = window.setTimeout(() => {
      try {
        if (isEmptyRef.current(values)) {
          localStorage.removeItem(key);
          return;
        }
        const draft: StoredDraft<T> = { version, savedAt: Date.now(), values };
        localStorage.setItem(key, JSON.stringify(draft));
      } catch {
        // Storage full or blocked (private mode): the form still works, it just is not kept.
      }
    }, DEBOUNCE_MS);
    return () => window.clearTimeout(handle);
  }, [key, version, values]);

  function discard() {
    localStorage.removeItem(key);
    setRestored(false);
  }

  return { restored, discard };
}
