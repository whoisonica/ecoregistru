import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Menu } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { canManage as roleCanManage } from "@/lib/roles";
import { useBillingAccess, useCanWrite } from "@/hooks/useBillingAccess";
import { usePanelData } from "@/hooks/usePanelData";
import { BillingBanner } from "@/components/BillingBanner";
import { strings } from "@/lib/strings";
import { buildNav } from "@/lib/navItems";
import { SCREEN_PATH } from "@/lib/movementScreens";
import { cn } from "@/lib/utils";
import {
  CommandPalette,
  useActionCommands,
  useNavigationCommands,
} from "@/components/CommandPalette";
import { useHotkey } from "@/hooks/useHotkey";
import { Panel } from "@/components/panel/Panel";
import { MobileBar } from "@/components/panel/MobileBar";
import { BrandName } from "@/components/BrandName";
import type { ReactNode } from "react";

const PANEL_STORAGE_KEY = "wh.panel";

/** Starea „strâns" a panoului, ținută în browser. Citirea poate arunca (fereastră privată): try/catch. */
function readCollapsed(): boolean {
  try {
    return localStorage.getItem(PANEL_STORAGE_KEY) === "collapsed";
  } catch {
    return false;
  }
}

/**
 * Ecranele care își leagă singure tasta N (acțiunea lor principală). Pe restul, N = adaugă deșeuri.
 * Regula din `todo-ui-cantar.md` §6: N e acțiunea principală a ecranului curent.
 */
const OWNS_N = new Set<string>([...Object.values(SCREEN_PATH), "/parteneri", "/setari"]);

/**
 * Cadrul aplicației: panoul din stânga (direcția „Cântar”), pagina, bara de jos pe telefon,
 * paleta și scurtăturile globale.
 *
 * <p>Pe `lg` panoul e o coloană de 262px (sau o șină de 64px când e strâns cu `[`); sub `lg` e un
 * sertar care intră din stânga peste pagină, deschis din „Mai mult" (bara de jos) sau din banda de
 * sus.
 */
export function Layout({ children }: { children: ReactNode }) {
  const { user, logout, tenantId } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [navOpen, setNavOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(readCollapsed);
  const [paletteOpen, setPaletteOpen] = useState(false);

  const panel = usePanelData();
  const { company, screens, dashboard } = panel;
  const writable = useCanWrite();
  const manages = roleCanManage(user?.role);
  const { data: billing } = useBillingAccess();
  const nav = useMemo(() => buildNav(user?.role, company?.type), [user?.role, company?.type]);

  // O navigare închide sertarul: altfel rămâne peste pagina pe care tocmai ai cerut-o.
  useEffect(() => {
    setNavOpen(false);
  }, [location.pathname]);

  // Escape îl închide, ca la orice strat care acoperă pagina.
  useEffect(() => {
    if (!navOpen) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setNavOpen(false);
    }
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [navOpen]);

  const toggleCollapsed = useCallback(() => {
    setCollapsed((c) => {
      const next = !c;
      try {
        localStorage.setItem(PANEL_STORAGE_KEY, next ? "collapsed" : "open");
      } catch {
        // Fără memorie între sesiuni; panoul tot se strânge acum.
      }
      return next;
    });
  }, []);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  // Locurile întâi, apoi ce se poate începe: „unde ajung" e întrebarea de zece ori mai deasă.
  const navCommands = useNavigationCommands(nav);
  const actionCommands = useActionCommands(writable && Boolean(company), screens);
  const commands = useMemo(() => [...navCommands, ...actionCommands], [navCommands, actionCommands]);

  /**
   * `/` duce în caseta de căutare a ecranului curent, oriunde ar fi ea. Legătura se face prin DOM
   * (`[data-table-search]`), nu printr-un context cu referințe.
   */
  useHotkey("/", () => {
    const search = document.querySelector<HTMLInputElement>("[data-table-search]");
    if (search) {
      search.focus();
      search.select();
    }
  });

  // `[` strânge panoul — doar pe ecran mare, unde există ce strânge.
  useHotkey("[", toggleCollapsed, { enabled: window.matchMedia("(min-width: 1024px)").matches });

  /**
   * Cifrele 1–9, 0 deschid intrările meniului în ordinea afișată; F și C grupul Cabinet. Zece
   * apeluri fixe de hook, cu tasta ca argument: numărul de hook-uri nu poate depinde de meniu.
   */
  const go = useCallback(
    (key: string) => {
      const item = [...nav.main, ...nav.cabinet].find((i) => i.hotkey === key);
      if (item) navigate(item.to);
    },
    [nav, navigate]
  );
  useHotkey("1", () => go("1"));
  useHotkey("2", () => go("2"));
  useHotkey("3", () => go("3"));
  useHotkey("4", () => go("4"));
  useHotkey("5", () => go("5"));
  useHotkey("6", () => go("6"));
  useHotkey("7", () => go("7"));
  useHotkey("8", () => go("8"));
  useHotkey("9", () => go("9"));
  useHotkey("0", () => go("0"));
  useHotkey("f", () => go("F"), { enabled: nav.cabinet.some((i) => i.hotkey === "F") });
  useHotkey("c", () => go("C"), { enabled: nav.cabinet.some((i) => i.hotkey === "C") });

  // N / I / E: adaugă deșeuri, intrare, ieșire — pe ecranele care nu-și leagă singure tasta N.
  const canAdd = writable && Boolean(company);
  const hasGenerated = screens.includes("GENERATED");
  const hasCollector = screens.includes("IN");
  useHotkey(
    "n",
    () => navigate(`${hasGenerated ? SCREEN_PATH.GENERATED : SCREEN_PATH.IN}?nou=1`),
    { enabled: canAdd && !OWNS_N.has(location.pathname) }
  );
  useHotkey("i", () => navigate(`${SCREEN_PATH.IN}?nou=1`), { enabled: canAdd && hasCollector });
  useHotkey("e", () => navigate(`${SCREEN_PATH.OUT}?nou=1`), { enabled: canAdd && hasCollector });

  const mainList = nav.main.find((i) => i.screen) ?? null;

  const panelProps = {
    nav,
    company,
    screens,
    canWrite: writable,
    canManage: manages,
    monthName: dashboard.monthLabel,
    monthLabel: panel.monthLabel,
    monthKg: panel.monthKg,
    monthLoading: panel.monthLoading,
    monthFailed: panel.monthFailed,
    nextAction: dashboard.nextAction,
    nextActionLoading: dashboard.nextActionLoading,
    indicatorFor: panel.indicatorFor,
    billing,
    onOpenSearch: () => setPaletteOpen(true),
    onCloseDrawer: () => setNavOpen(false),
    email: user?.email,
    role: user?.role,
    onLogout: handleLogout,
  };

  return (
    <div className="flex h-full">
      {/* Prima oprire a tastaturii: sare peste meniu, care se repetă pe fiecare pagină. */}
      <a
        href="#continut"
        className="sr-only-focusable absolute left-4 top-4 z-[80] rounded-md bg-brand px-3 py-2 text-sm font-medium text-brand-fg"
      >
        {strings.common.skipToContent}
      </a>

      {/* Banda de sus, doar pe ecran îngust: grafit, ca panoul — firma, cifra lunii, cel mai urgent lucru. */}
      <header className="fixed inset-x-0 top-0 z-30 flex items-center gap-3 bg-panel px-3 py-2 text-white lg:hidden">
        <button
          type="button"
          onClick={() => setNavOpen(true)}
          aria-label={strings.common.openNav}
          aria-expanded={navOpen}
          aria-controls="navigatie-principala"
          className="-ml-1 rounded-md p-1.5 text-panel-text hover:bg-panel-hover"
        >
          <Menu className="h-5 w-5" aria-hidden />
        </button>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="min-w-0 truncate text-sm font-semibold">
              {company?.name ?? user?.tenantName ?? <BrandName />}
            </span>
            {company && (
              <span className="ml-auto shrink-0 font-mono text-[0.6875rem] uppercase text-lcd-digit">
                {dashboard.monthLabel.slice(0, 3)} · {panel.monthFailed ? strings.panel.unknown : panel.monthKg == null ? "—" : new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 }).format(panel.monthKg)} {strings.panel.kg}
              </span>
            )}
          </div>
          {company && dashboard.nextAction && dashboard.nextAction.tone !== "ok" && dashboard.nextAction.tone !== "start" && (
            <div
              className={cn(
                "truncate font-mono text-[0.6875rem]",
                dashboard.nextAction.tone === "danger" ? "text-lcd-bad" : dashboard.nextAction.tone === "warning" ? "text-lcd-warn" : "text-panel-dim"
              )}
            >
              ▲ {dashboard.nextAction.title}
            </div>
          )}
        </div>
      </header>

      {/* Fundalul sertarului. Nu există pe `lg`, unde panoul e parte din pagină. */}
      {navOpen && (
        <div
          className="fixed inset-0 z-40 animate-fade-in bg-black/50 lg:hidden"
          onClick={() => setNavOpen(false)}
          aria-hidden
        />
      )}

      <aside
        id="navigatie-principala"
        className={cn(
          // Pe ecran îngust: sertar peste pagină, care intră din stânga. Pe `lg`: coloana grafit,
          // în fluxul paginii, de 262px sau de 64px când e strânsă.
          "fixed inset-y-0 left-0 z-50 w-[272px] shrink-0 transition-transform duration-200 lg:static lg:z-auto lg:translate-x-0",
          collapsed ? "lg:w-16" : "lg:w-[262px]",
          navOpen ? "translate-x-0" : "-translate-x-full"
        )}
        data-collapsed={collapsed || undefined}
      >
        <Panel {...panelProps} collapsed={collapsed} onToggleCollapsed={toggleCollapsed} />
      </aside>

      <CommandPalette commands={commands} open={paletteOpen} onOpenChange={setPaletteOpen} />

      {/*
        Keyed by company: changing it remounts the page instead of leaving it on screen with the
        previous tenant's filters. The cache was emptied at switch, so the fresh mount refetches
        everything with the new X-Tenant-Id.
      */}
      <main
        id="continut"
        key={tenantId ?? "fara-companie"}
        // Sus, loc pentru banda fixă; jos, pentru bara de taburi — amândouă doar sub `lg`.
        // Banda de sus are 54px (măsurat 15.09.2026): 70px de căptușeală lasă 16px de aer, exact
        // cât `-top-4` al cuprinsului lipicios (`SectionNav`).
        className="flex-1 overflow-auto px-4 pb-24 pt-[4.375rem] sm:px-6 lg:px-8 lg:pb-8 lg:pt-6"
      >
        <BillingBanner />
        {children}
      </main>

      <MobileBar
        mainList={mainList}
        screens={screens}
        canWrite={writable && Boolean(company)}
        overdue={dashboard.overdueCount > 0}
        onMore={() => setNavOpen(true)}
      />
    </div>
  );
}
