import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Button } from "@/components/ui/button";
import { strings } from "@/lib/strings";
import type { TourStep } from "@/lib/tour";
import { CUTOUT_RADIUS, boxWidthFor, cutoutRect, placeBox, type BoxPosition, type Rect } from "@/components/tour/placement";

const t = strings.tour;

interface TourProps {
  step: TourStep;
  /** Crește la fiecare mutare: caseta se montează din nou (cu focusul pe „Înainte”). */
  stepKey: number;
  /** Ținta s-a găsit (sau pasul n-o are): caseta se arată. Până atunci e doar întunericul. */
  ready: boolean;
  /** Elementul luminat; `null` = caseta în mijloc, fără decupaj. */
  target: HTMLElement | null;
  /** Al câtelea pas din câți se arată pe lățimea de acum. */
  n: number;
  total: number;
  isFirst: boolean;
  isLast: boolean;
  onNext: () => void;
  onBack: () => void;
  onSkip: () => void;
}

interface OverlayLayout {
  cut: Rect | null;
  pos: BoxPosition | null;
}

function sameLayout(a: OverlayLayout, b: OverlayLayout): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

/** Tab se învârte numai între butoanele casetei. */
function cycleFocus(box: HTMLElement | null, backwards: boolean) {
  if (!box) return;
  const items = Array.from(box.querySelectorAll<HTMLElement>("button:not([disabled])"));
  if (items.length === 0) {
    box.focus();
    return;
  }
  const at = items.indexOf(document.activeElement as HTMLElement);
  const next = at === -1 ? (backwards ? items.length - 1 : 0) : (at + (backwards ? -1 : 1) + items.length) % items.length;
  items[next].focus();
}

/**
 * Stratul turului, peste toată aplicația: întunericul cu ținta decupată și caseta pasului.
 *
 * <p>`z-[75]`: peste dialoguri (`z-50`), notificări (`z-[60]`), paletă și tooltip (`z-[70]`); sub
 * legătura „Sari la conținut” (`z-[80]`), care oricum nu primește focus cât timp turul îl ține. Un
 * clic pe întuneric nu face nimic: nici nu închide turul, nici nu ajunge la pagină.
 *
 * <p>Conturul țintei e `stroke-brand` (`--brand`, definit pe toate cele trei teme), întunericul e
 * `fill-scrim` (`--scrim`), ca fundalul dialogurilor. Mișcarea: o singură apariție (`animate-fade-in`),
 * scoasă de tot la `prefers-reduced-motion`.
 */
export function Tour({ step, stepKey, ready, target, n, total, isFirst, isLast, onNext, onBack, onSkip }: TourProps) {
  const overlayRef = useRef<HTMLDivElement>(null);
  const boxRef = useRef<HTMLDivElement>(null);
  const primaryRef = useRef<HTMLButtonElement>(null);
  const [layout, setLayout] = useState<OverlayLayout>({ cut: null, pos: null });

  // `useId` dă „:r1:”, iar două puncte într-un `url(#…)` nu sunt de încredere în toate browserele.
  const uid = useId().replace(/:/g, "");
  const maskId = `tur-masca-${uid}`;
  const labelId = `tur-eticheta-${uid}`;
  const titleId = `tur-titlu-${uid}`;
  const bodyId = `tur-text-${uid}`;

  const measure = useCallback(() => {
    const viewport = { width: window.innerWidth, height: window.innerHeight };
    let rect: Rect | null = null;
    if (target?.isConnected) {
      const r = target.getBoundingClientRect();
      if (r.width > 0 || r.height > 0) rect = { top: r.top, left: r.left, width: r.width, height: r.height };
    }
    const box = boxRef.current;
    const next: OverlayLayout = {
      cut: rect ? cutoutRect(rect) : null,
      pos: box ? placeBox(rect, { width: box.offsetWidth, height: box.offsetHeight }, viewport) : null,
    };
    setLayout((prev) => (sameLayout(prev, next) ? prev : next));
  }, [target]);

  // Înainte de vopsire, ca o casetă nouă să nu apară o clipă în locul celei vechi.
  useLayoutEffect(() => {
    measure();
  }, [measure, ready, stepKey]);

  // Fereastra se schimbă, `main#continut` se derulează (de aici `capture`: derularea nu urcă), ținta
  // sau caseta își schimbă mărimea (o listă care se încarcă, textul care se rupe altfel).
  useEffect(() => {
    window.addEventListener("resize", measure);
    window.addEventListener("scroll", measure, true);
    const observer = typeof ResizeObserver === "undefined" ? null : new ResizeObserver(() => measure());
    if (target) observer?.observe(target);
    if (boxRef.current) observer?.observe(boxRef.current);
    return () => {
      window.removeEventListener("resize", measure);
      window.removeEventListener("scroll", measure, true);
      observer?.disconnect();
    };
  }, [measure, ready, stepKey, target]);

  const placed = layout.pos !== null;
  useEffect(() => {
    if (ready && placed) primaryRef.current?.focus();
  }, [ready, placed, stepKey]);

  // Un clic pe întuneric nu mută focusul de pe casetă.
  useEffect(() => {
    const overlay = overlayRef.current;
    if (!overlay) return;
    const keep = (e: MouseEvent) => {
      if (!boxRef.current?.contains(e.target as Node)) e.preventDefault();
    };
    overlay.addEventListener("mousedown", keep);
    return () => overlay.removeEventListener("mousedown", keep);
  }, []);

  /**
   * Tastele, pe `window` și în faza de captură: ajung aici înaintea oricărui ascultător al aplicației.
   * Toate se opresc aici: cifrele, N, `/` sau Ctrl K n-au ce căuta pe pagina de dedesubt cât timp turul e
   * pe ecran — nici în clipa dintre doi pași, când caseta (`aria-modal`, pe care o citește `useHotkey`)
   * încă nu e montată. Ce face browserul singur (Enter sau spațiu pe un buton) rămâne.
   */
  const keys = useRef({ ready, isFirst, onNext, onBack, onSkip });
  keys.current = { ready, isFirst, onNext, onBack, onSkip };
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      const { ready: shown, isFirst: first, onNext: next, onBack: back, onSkip: skip } = keys.current;
      const box = boxRef.current;
      switch (e.key) {
        case "Escape":
          e.preventDefault();
          skip();
          break;
        case "ArrowRight":
          e.preventDefault();
          if (shown && !e.repeat) next();
          break;
        case "ArrowLeft":
          e.preventDefault();
          if (shown && !first && !e.repeat) back();
          break;
        case "Enter":
          // Pe un buton al casetei, Enter îl apasă pe el („Sari peste”, „Înapoi”); altfel e „Înainte”.
          if (e.target instanceof HTMLButtonElement && box?.contains(e.target)) break;
          e.preventDefault();
          if (shown && !e.repeat) next();
          break;
        case "Tab":
          e.preventDefault();
          cycleFocus(box, e.shiftKey);
          break;
      }
      e.stopPropagation();
    }
    window.addEventListener("keydown", onKeyDown, true);
    return () => window.removeEventListener("keydown", onKeyDown, true);
  }, []);

  const { cut, pos } = layout;
  // Lățimea vine din fereastră, nu din poziție: înălțimea măsurată atârnă de ea.
  const width = boxWidthFor(window.innerWidth);
  const progress = t.progress.replace("{n}", String(n)).replace("{total}", String(total));

  return createPortal(
    <div ref={overlayRef} data-testid="tour-overlay" className="fixed inset-0 z-[75] animate-fade-in motion-reduce:animate-none">
      <svg className="absolute inset-0 h-full w-full" aria-hidden="true" focusable="false">
        <defs>
          <mask id={maskId}>
            <rect width="100%" height="100%" fill="white" />
            {cut && <rect x={cut.left} y={cut.top} width={cut.width} height={cut.height} rx={CUTOUT_RADIUS} fill="black" />}
          </mask>
        </defs>
        <rect width="100%" height="100%" className="fill-scrim" mask={`url(#${maskId})`} />
        {cut && (
          <rect
            x={cut.left}
            y={cut.top}
            width={cut.width}
            height={cut.height}
            rx={CUTOUT_RADIUS}
            fill="none"
            strokeWidth={2}
            className="stroke-brand"
          />
        )}
      </svg>

      {ready && (
        <div
          key={stepKey}
          ref={boxRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby={`${labelId} ${titleId}`}
          aria-describedby={bodyId}
          tabIndex={-1}
          data-testid="tour-box"
          data-step-id={step.id}
          className="absolute animate-fade-in rounded-md border border-line-strong bg-surface p-4 text-content outline-none motion-reduce:animate-none"
          style={{
            width,
            top: pos?.top ?? 0,
            left: pos?.left ?? 0,
            // Până la prima măsurătoare nu se știe unde stă: nevăzută, ca să nu clipească în colț.
            visibility: pos ? undefined : "hidden",
          }}
        >
          <span id={labelId} className="sr-only">
            {t.dialogLabel}
          </span>
          <div className="flex items-baseline justify-between gap-3">
            <h2 id={titleId} className="text-sm font-semibold text-content">
              {step.title}
            </h2>
            <span className="shrink-0 font-mono text-xs text-content-subtle">{progress}</span>
          </div>
          <p id={bodyId} className="mt-1.5 text-sm text-content-muted">
            {step.body}
          </p>
          <div className="mt-4 flex items-center gap-2">
            {!isLast && (
              <Button type="button" variant="ghost" size="sm" onClick={onSkip}>
                {t.skip}
              </Button>
            )}
            <div className="ml-auto flex items-center gap-2">
              {!isFirst && (
                <Button type="button" variant="outline" size="sm" onClick={onBack}>
                  {t.back}
                </Button>
              )}
              <Button ref={primaryRef} type="button" size="sm" onClick={onNext}>
                {isLast ? t.done : t.next}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>,
    document.body
  );
}
