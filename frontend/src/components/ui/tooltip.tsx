import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { HelpCircle } from "lucide-react";
import { cn } from "@/lib/utils";

interface TooltipProps {
  /** Textul explicativ. Poate fi lung: e limitat la o coloană citibilă, nu la o linie. */
  content: React.ReactNode;
  children: React.ReactElement | React.ReactNode;
  /** Lățimea maximă a bulei, în clase Tailwind. Implicit `max-w-xs`. */
  className?: string;
}

/**
 * Explicația care până acum stătea în atributul `title`.
 *
 * <p>`title` are două defecte pe care aplicația asta nu și le permite: pe touch nu apare niciodată,
 * iar întârzierea și stilul le decide browserul. Or tocmai rubricile importante îl foloseau —
 * „Fără cod R/D", „De cântărit", ce conține dosarul de control. Aici bula apare la hover, la focus
 * de tastatură **și** la apăsare, deci răspunde la fel pe telefon ca pe desktop.
 *
 * <p>Se randează prin portal, cu poziționare `fixed`: declanșatorul stă adesea într-o celulă de
 * tabel, iar tabelele au `overflow-x-auto`, care ar tăia orice bulă poziționată în interiorul lor.
 * Poziția se recalculează la scroll și la redimensionare, fiindcă `fixed` nu urmărește pagina.
 */
export function Tooltip({ content, children, className }: TooltipProps) {
  const [open, setOpen] = useState(false);
  const [coords, setCoords] = useState<{ top: number; left: number; below: boolean } | null>(null);
  const triggerRef = useRef<HTMLSpanElement>(null);
  const bubbleRef = useRef<HTMLDivElement>(null);
  const id = useId();

  const place = useCallback(() => {
    const trigger = triggerRef.current;
    const bubble = bubbleRef.current;
    if (!trigger || !bubble) return;
    const t = trigger.getBoundingClientRect();
    const b = bubble.getBoundingClientRect();
    const GAP = 8;
    // Deasupra dacă încape; altfel dedesubt. Fără spațiu nici sus, nici jos, rămâne dedesubt și
    // se lasă să iasă din ecran — mai bine tăiată decât peste ce explică.
    const below = t.top - b.height - GAP < 8;
    const top = below ? t.bottom + GAP : t.top - b.height - GAP;
    // Centrată pe declanșator, apoi împinsă înăuntru dacă ar depăși marginea ferestrei.
    const raw = t.left + t.width / 2 - b.width / 2;
    const left = Math.min(Math.max(8, raw), window.innerWidth - b.width - 8);
    setCoords({ top, left, below });
  }, []);

  // `useLayoutEffect` ca prima vopsire să fie deja la locul ei: cu `useEffect` bula apărea o
  // fracțiune de secundă în colțul din stânga-sus, apoi sărea.
  useLayoutEffect(() => {
    if (open) place();
  }, [open, place]);

  useEffect(() => {
    if (!open) return;
    function onAway(e: MouseEvent) {
      if (!triggerRef.current?.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    // `capture: true` ca să prindem și scrollul containerelor interioare, nu doar al ferestrei.
    window.addEventListener("scroll", place, true);
    window.addEventListener("resize", place);
    document.addEventListener("mousedown", onAway);
    document.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("scroll", place, true);
      window.removeEventListener("resize", place);
      document.removeEventListener("mousedown", onAway);
      document.removeEventListener("keydown", onKey);
    };
  }, [open, place]);

  return (
    <>
      <span
        ref={triggerRef}
        tabIndex={0}
        role="button"
        aria-describedby={open ? id : undefined}
        aria-expanded={open}
        className="inline-flex cursor-help items-center rounded"
        onMouseEnter={() => setOpen(true)}
        onMouseLeave={() => setOpen(false)}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onClick={(e) => {
          // Pe touch nu există hover: apăsarea e singurul fel de a cere explicația. Nu lăsăm
          // evenimentul să urce, ca o bulă dintr-un rând de tabel să nu deschidă și rândul.
          e.preventDefault();
          e.stopPropagation();
          setOpen((o) => !o);
        }}
      >
        {children}
      </span>
      {open &&
        createPortal(
          <div
            ref={bubbleRef}
            id={id}
            role="tooltip"
            style={{
              top: coords?.top ?? -9999,
              left: coords?.left ?? -9999,
              // Invizibilă până se măsoară, ca să nu clipească la poziția de pornire.
              visibility: coords ? "visible" : "hidden",
            }}
            className={cn(
              "pointer-events-none fixed z-[70] max-w-xs animate-fade-in rounded-lg bg-gray-900 px-3 py-2 text-xs leading-relaxed text-white shadow-popover",
              className
            )}
          >
            {content}
          </div>,
          document.body
        )}
    </>
  );
}

/**
 * Semnul de întrebare de lângă o etichetă. Are nevoie de `label`: un declanșator fără nume e o
 * țintă pe care un cititor de ecran o anunță „buton", și atât.
 */
export function InfoHint({ content, label }: { content: React.ReactNode; label: string }) {
  return (
    <Tooltip content={content}>
      <HelpCircle
        className="h-3.5 w-3.5 text-content-subtle transition-colors hover:text-content-muted"
        aria-label={label}
      />
    </Tooltip>
  );
}
