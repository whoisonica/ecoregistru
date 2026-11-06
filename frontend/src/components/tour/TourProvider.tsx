import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { isMultiCompany } from "@/lib/roles";
import { nextVisibleIndex, shouldAutoStart, tourSteps, type TourStep } from "@/lib/tour";
import { Tour } from "@/components/tour/Tour";
import { WIDE_QUERY } from "@/components/tour/placement";

/**
 * CONTRACT (06.11.2026): `AccountMenu` și paleta de comenzi folosesc interfața asta, deci nu se schimbă.
 */
export interface TourContextValue {
  /** Pornește turul de la primul pas (din meniul contului sau din paletă). */
  start: () => void;
  /** Turul e pe ecran acum. */
  active: boolean;
  /** Firma curentă are tur (ecranul „Generare”): altfel intrările din meniu și paletă nu apar. */
  available: boolean;
}

const TourContext = createContext<TourContextValue>({ start: () => {}, active: false, available: false });

/** Cât așteaptă un pas ținta: paginile își aduc întâi datele (lista „Primii pași” apare după patru cereri). */
const TARGET_TIMEOUT_MS = 2500;
/**
 * Mai devreme de atât nu se trage concluzia că ținta lipsește, chiar dacă nu se mai cere nimic de la
 * server: pagina abia s-a montat și cererile ei poate n-au plecat încă.
 */
const TARGET_MIN_WAIT_MS = 600;
/** Cât trebuie să stea liniștea (nicio cerere în zbor) până când ținta se socotește lipsă. */
const TARGET_IDLE_GRACE_MS = 400;

function subscribeWide(onChange: () => void) {
  const mq = window.matchMedia(WIDE_QUERY);
  mq.addEventListener("change", onChange);
  return () => mq.removeEventListener("change", onChange);
}
const readWide = () => window.matchMedia(WIDE_QUERY).matches;

/** Prima țintă care chiar se vede: aceeași țintă poate sta și într-un loc ascuns pe ecranul îngust. */
function findTarget(target: string): HTMLElement | null {
  for (const el of document.querySelectorAll<HTMLElement>(`[data-tour="${target}"]`)) {
    if (el.getClientRects().length > 0) return el;
  }
  return null;
}

/**
 * Așteaptă ținta în pagină, cadru cu cadru. Renunță la `TARGET_TIMEOUT_MS` sau mai devreme, când
 * pagina s-a liniștit (nicio cerere TanStack în zbor) și ținta tot n-a apărut. Întoarce anularea.
 */
function waitForTarget(target: string, isIdle: () => boolean, done: (el: HTMLElement | null) => void): () => void {
  const startedAt = performance.now();
  let idleSince: number | null = null;
  let frame = 0;
  const tick = () => {
    const el = findTarget(target);
    if (el) return done(el);
    const now = performance.now();
    const elapsed = now - startedAt;
    if (elapsed >= TARGET_TIMEOUT_MS) return done(null);
    if (elapsed >= TARGET_MIN_WAIT_MS && isIdle()) {
      idleSince ??= now;
      if (now - idleSince >= TARGET_IDLE_GRACE_MS) return done(null);
    } else {
      idleSince = null;
    }
    frame = requestAnimationFrame(tick);
  };
  frame = requestAnimationFrame(tick);
  return () => cancelAnimationFrame(frame);
}

/**
 * Aduce ținta la vedere în `main#continut`, care e containerul de derulare (documentul nu se
 * derulează). Fără animație: turul nu mișcă pagina mai mult decât trebuie. Ținta ajunge sus, unde
 * începe conținutul: pe ecran lat caseta are loc dedesubt, pe telefon rămâne deasupra casetei lipite jos.
 */
function bringIntoView(el: HTMLElement, wide: boolean) {
  const main = document.getElementById("continut");
  if (!main || !main.contains(el)) {
    // În panou (meniul, căutarea): coloana lui se derulează singură, dacă e cazul.
    el.scrollIntoView({ block: "nearest", inline: "nearest" });
    return;
  }
  const box = main.getBoundingClientRect();
  const rect = el.getBoundingClientRect();
  const inset = parseFloat(getComputedStyle(main).paddingTop) || 0;
  const viewTop = box.top + inset;
  // Pe telefon jumătatea de jos e a casetei.
  const viewBottom = wide ? box.bottom : box.top + box.height / 2;
  if (rect.top >= viewTop && rect.bottom <= viewBottom) return;
  main.scrollTop += rect.top - viewTop;
}

interface Run {
  index: number;
  /** Direcția mutării: un pas fără țintă (care o cere) se sare tot încolo. */
  dir: 1 | -1;
  /** Ultimul pas arătat: acolo se rămâne când înapoi nu mai e nimic de arătat. */
  from: number;
  /** Crește la fiecare mutare, ca o țintă găsită să fie a mutării ei, nu a unei vizite vechi. */
  seq: number;
}

/**
 * Motorul turului: pașii din `lib/tour.ts`, navigarea între ecrane, așteptarea țintei și caseta
 * (`Tour.tsx`). Stă în `AppShell` (`App.tsx`), deasupra lui `Layout`: și paleta, și meniul contului
 * din panou îl cheamă. Instanța rămâne aceeași între ecrane (React o păstrează, ca pe `Layout`).
 */
export function TourProvider({ children }: { children: ReactNode }) {
  const { user, tenantId, markTourSeen } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();

  // Ca în `usePanelData`: consultantul fără firmă aleasă n-are ce citi.
  const hasCompany = Boolean(user) && (!isMultiCompany(user?.role) || Boolean(tenantId));
  const { data: company } = useCurrentCompany(hasCompany);
  const role = user?.role;
  const companyType = hasCompany ? company?.type : undefined;
  const steps = useMemo(() => tourSteps({ role, companyType }), [role, companyType]);
  const wide = useSyncExternalStore(subscribeWide, readWide);

  const [run, setRun] = useState<Run | null>(null);
  const [found, setFound] = useState<{ seq: number; el: HTMLElement | null } | null>(null);
  const returnFocus = useRef<HTMLElement | null>(null);

  const step: TourStep | undefined = run ? steps[run.index] : undefined;
  const active = Boolean(run && step);

  /**
   * Ce citesc ascultătorii fără să atârne de identitatea lor (tiparul din `dialog.tsx`): `navigate`
   * se schimbă la fiecare navigare, iar efectul care așteaptă ținta nu trebuie să repornească atunci.
   */
  const latest = useRef({ steps, navigate, here: "", markTourSeen });
  latest.current = { steps, navigate, here: location.pathname + location.search, markTourSeen };

  const goTo = useCallback((index: number, dir: 1 | -1, from: number) => {
    const { steps: all, navigate: nav, here } = latest.current;
    const target = all[index];
    if (!target) return;
    setFound(null);
    setRun((r) => ({ index, dir, from, seq: (r?.seq ?? 0) + 1 }));
    if (here !== target.path) {
      nav(target.path);
      // Până la randarea următoare: o a doua mutare în aceeași clipă nu mai împinge încă o intrare în istoric.
      latest.current.here = target.path;
    }
  }, []);

  const close = useCallback((goHome: boolean) => {
    setRun(null);
    setFound(null);
    latest.current.markTourSeen();
    if (goHome && latest.current.here !== "/") {
      latest.current.navigate("/");
      latest.current.here = "/";
    }
    const back = returnFocus.current;
    returnFocus.current = null;
    if (back?.isConnected) back.focus();
  }, []);

  const start = useCallback(() => {
    const first = nextVisibleIndex(latest.current.steps, -1, 1, readWide());
    if (first === null) return;
    const focused = document.activeElement;
    returnFocus.current = focused instanceof HTMLElement && focused !== document.body ? focused : null;
    goTo(first, 1, first);
  }, [goTo]);

  // Singur, o dată pe încărcarea paginii: după „Sari peste” `tourSeen` e deja `true`, dar referința
  // ține și cazul în care serverul n-a apucat să afle.
  const autoStarted = useRef(false);
  const autoStart = shouldAutoStart(user, companyType);
  useEffect(() => {
    if (!autoStart || autoStarted.current) return;
    autoStarted.current = true;
    start();
  }, [autoStart, start]);

  // Ținta pasului curent: așteptată, adusă la vedere, sau lipsă — și atunci pasul se sare ori stă în mijloc.
  useEffect(() => {
    if (!run) return;
    const current = latest.current.steps[run.index];
    if (!current?.target) return;
    return waitForTarget(
      current.target,
      () => queryClient.isFetching() === 0,
      (el) => {
        if (el) {
          bringIntoView(el, readWide());
          setFound({ seq: run.seq, el });
          return;
        }
        if (!current.requiresTarget) {
          setFound({ seq: run.seq, el: null });
          return;
        }
        const next = nextVisibleIndex(latest.current.steps, run.index, run.dir, readWide());
        if (next !== null) goTo(next, run.dir, run.from);
        // Înapoi nu mai e nimic de arătat: se rămâne pe pasul de unde s-a plecat.
        else if (run.dir === -1) goTo(run.from, 1, run.from);
        else close(false);
      }
    );
  }, [run, queryClient, goTo, close]);

  const ready = Boolean(step && (step.target === null || found?.seq === run?.seq));

  // Numărătoarea: pașii care se pot arăta pe lățimea de acum (fără cei „numai pe ecran lat” pe telefon).
  const visible = useMemo(() => steps.filter((s) => wide || !s.desktopOnly), [steps, wide]);

  const onNext = useCallback(() => {
    if (!run) return;
    const next = nextVisibleIndex(latest.current.steps, run.index, 1, readWide());
    if (next === null) close(true);
    else goTo(next, 1, run.index);
  }, [run, goTo, close]);

  const onBack = useCallback(() => {
    if (!run) return;
    const prev = nextVisibleIndex(latest.current.steps, run.index, -1, readWide());
    if (prev !== null) goTo(prev, -1, run.index);
  }, [run, goTo]);

  const onSkip = useCallback(() => close(false), [close]);

  const value = useMemo<TourContextValue>(
    () => ({ start, active, available: steps.length > 0 }),
    [start, active, steps.length]
  );

  return (
    <TourContext.Provider value={value}>
      {children}
      {run && step && (
        <Tour
          step={step}
          stepKey={run.seq}
          ready={ready}
          target={found?.seq === run.seq ? found.el : null}
          n={Math.max(1, visible.indexOf(step) + 1)}
          total={visible.length}
          isFirst={nextVisibleIndex(steps, run.index, -1, wide) === null}
          isLast={nextVisibleIndex(steps, run.index, 1, wide) === null}
          onNext={onNext}
          onBack={onBack}
          onSkip={onSkip}
        />
      )}
    </TourContext.Provider>
  );
}

export function useTour(): TourContextValue {
  return useContext(TourContext);
}
