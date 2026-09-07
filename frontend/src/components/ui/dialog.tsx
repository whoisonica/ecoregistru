import { useEffect, useId, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";

/**
 * Cât de lat are voie să fie dialogul.
 *
 * <p>Până acum era o singură lățime, `max-w-lg`, adică 512px — și în ea intra și formularul de
 * mișcare, cu treizeci de rubrici și rânduri de câte trei coloane, deci vreo 150px de câmp. Un
 * dialog de confirmare și formularul ăla n-au ce căuta la aceeași măsură.
 */
export type DialogSize = "sm" | "md" | "lg" | "xl" | "2xl";

const sizeClass: Record<DialogSize, string> = {
  sm: "max-w-sm",
  md: "max-w-md",
  lg: "max-w-lg",
  xl: "max-w-2xl",
  "2xl": "max-w-4xl",
};

interface DialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  /** Linia de sub titlu, când titlul singur nu spune ce se întâmplă aici. */
  description?: ReactNode;
  children: ReactNode;
  /** Butoanele de acțiune din subsol (aliniate la dreapta). */
  footer?: ReactNode;
  /** Implicit `lg` — măsura de dinainte, ca dialogurile existente să nu se miște. */
  size?: DialogSize;
  /**
   * Cât timp e adevărat, dialogul nu se închide: nici cu Escape, nici cu clic pe fundal, nici pe
   * butonul de închidere.
   *
   * <p>E pentru fapta pe care închiderea ar rupe-o la mijloc — urcarea a cinci atașamente, una
   * după alta. Până acum butonul „Anulează" se dezactiva, dar fundalul și „×" rămâneau vii, deci
   * cel mai obișnuit reflex era și singurul care strica.
   */
  busy?: boolean;
}

/** Ce poate primi focus înăuntru. Folosit și pentru capcana de Tab, și pentru focusul inițial. */
const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Modal ușor: portal + fundal, se închide cu Escape sau clic în afară.
 *
 * <p>Ce s-a adăugat pe 07.09.2026, și de ce: focusul intra în dialog dar putea ieși din el cu Tab,
 * ajungând pe formularul de dedesubt — pe care omul îl vedea acoperit. La închidere focusul cădea
 * la începutul paginii, deci cine deschidea un dialog de pe al zecelea rând al tabelului se
 * întorcea în capul listei. Amândouă se repară aici, o dată, pentru toate cele nouă dialoguri.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = "lg",
  busy = false,
}: DialogProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  // Cine avea focusul înainte să deschidem: acolo îl punem înapoi la închidere.
  const returnFocusRef = useRef<HTMLElement | null>(null);
  const titleId = useId();
  const descId = useId();
  /**
   * Ce citește ascultătorul de taste, fără să atârne de identitatea lor.
   *
   * <p>`onClose` e o funcție scrisă inline la aproape toți apelanții, iar `busy` comută la fiecare
   * salvare — deci amândouă se schimbă des. Ținute în dependențele efectului, îl reporneau; ținute
   * aici, ascultătorul se pune o dată, la deschidere. Același tipar ca în `useHotkey`.
   */
  const latest = useRef({ onClose, busy });
  latest.current = { onClose, busy };

  /**
   * Scrollul blocat și focusul redat — legate **numai** de deschidere.
   *
   * <p>Stăteau în același efect cu ascultătorul de taste, care depindea de `onClose` și de `busy`.
   * Deci cleanup-ul lui — cel care dă focusul înapoi elementului de dinaintea deschiderii — rula
   * la fiecare schimbare a lor, **cu dialogul încă deschis**: apăsai Salvează și focusul sărea pe
   * butonul din spatele dialogului. Pe o salvare respinsă de server, formularul rămânea deschis cu
   * focusul afară, iar capcana de Tab nu-l mai aducea înapoi.
   */
  useEffect(() => {
    if (!open) return;
    returnFocusRef.current = document.activeElement as HTMLElement | null;
    // Pagina de dedesubt nu se mai derulează cât timp dialogul e deschis: altfel rotița mouse-ului
    // pe fundal mișcă lista, iar dialogul pare că plutește peste altceva decât ce ai lăsat.
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
      returnFocusRef.current?.focus?.();
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;

    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") {
        // Un strat dinăuntru s-a ocupat deja de Escape — lista comboboxului, de pildă. El se
        // închide, dialogul din jurul lui rămâne deschis.
        if (e.defaultPrevented) return;
        if (!latest.current.busy) latest.current.onClose();
        return;
      }
      if (e.key !== "Tab") return;
      const panel = panelRef.current;
      if (!panel) return;
      const items = Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
        (el) => el.offsetParent !== null || el === document.activeElement
      );
      if (items.length === 0) {
        e.preventDefault();
        panel.focus();
        return;
      }
      const first = items[0];
      const last = items[items.length - 1];
      // Ciclu închis: de la ultimul, Tab duce la primul; de la primul, Shift+Tab la ultimul.
      if (e.shiftKey && (document.activeElement === first || !panel.contains(document.activeElement))) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  // Focusul inițial, dar numai dacă nu l-a luat deja cineva dinăuntru: câteva formulare pun
  // `autoFocus` pe primul câmp, și acela e răspunsul mai bun decât panoul însuși.
  useEffect(() => {
    if (!open) return;
    const panel = panelRef.current;
    if (!panel || panel.contains(document.activeElement)) return;
    const first = panel.querySelector<HTMLElement>(FOCUSABLE);
    (first ?? panel).focus();
  }, [open]);

  if (!open) return null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <div
        className="absolute inset-0 animate-fade-in bg-black/40"
        onClick={busy ? undefined : onClose}
        aria-hidden
      />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descId : undefined}
        tabIndex={-1}
        className={cn(
          // Pe telefon urcă de jos și ocupă lățimea întreagă, cu colțurile rotunjite doar sus —
          // gestul obișnuit al unei foi modale. De la `sm` în sus e dialogul centrat de dinainte.
          "relative z-10 flex max-h-[92vh] w-full animate-slide-up flex-col rounded-t-2xl bg-surface shadow-xl outline-none sm:max-h-[90vh] sm:rounded-xl",
          sizeClass[size]
        )}
      >
        <div className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
          <div className="min-w-0">
            <h2 id={titleId} className="text-lg font-semibold text-content">
              {title}
            </h2>
            {description && (
              <p id={descId} className="mt-0.5 text-sm text-content-muted">
                {description}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="-mr-1 shrink-0 rounded-md p-1 text-content-subtle transition-colors hover:bg-surface-sunken hover:text-content-muted disabled:opacity-40 disabled:hover:bg-transparent"
            aria-label={strings.common.close}
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-5 py-4">{children}</div>
        {footer && (
          <div className="flex flex-col-reverse gap-2 border-t border-line px-5 py-4 sm:flex-row sm:justify-end">
            {footer}
          </div>
        )}
      </div>
    </div>,
    document.body
  );
}
