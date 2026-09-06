import { useEffect, useRef, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import {
  LayoutDashboard,
  Truck,
  FileSpreadsheet,
  Users,
  CalendarClock,
  FolderArchive,
  Package,
  Settings,
  LogOut,
  Building2,
  Menu,
  X,
  ChevronUp,
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { companiesKey, useCompanies } from "@/hooks/useCompanies";
import { Select } from "@/components/ui/select";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";
import { CommandPalette, useNavigationCommands } from "@/components/CommandPalette";
import { useHotkey } from "@/hooks/useHotkey";
import type { ReactNode } from "react";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

interface NavGroup {
  /** Lipsește la primul grup: Panoul stă singur, fără titlu peste el. */
  label?: string;
  items: NavItem[];
  /** Grupul se vede numai de către administratorul platformei. */
  platformAdminOnly?: boolean;
}

/**
 * Bara laterală, pe grupuri.
 *
 * <p>Erau nouă intrări una sub alta, în ordinea în care s-au construit ecranele — Mișcări,
 * Evidențe, Parteneri, Termene, Ambalaje, Dosar, Setări. Ordinea aia nu spune nimic despre ce ține
 * de ce: Ambalajele sunt evidență, ca Mișcările, dar stăteau după Termene; Parteneri e
 * nomenclator, ca Setări, dar stătea între Evidențe și Termene.
 *
 * <p>Grupurile răspund la „ce fac aici": înregistrez ceva, scot un document, sau configurez.
 */
export const navGroups: NavGroup[] = [
  { items: [{ to: "/", label: strings.nav.dashboard, icon: LayoutDashboard, end: true }] },
  {
    label: strings.nav.groupRecords,
    items: [
      { to: "/miscari", label: strings.nav.movements, icon: Truck },
      { to: "/evidente", label: strings.nav.evidences, icon: FileSpreadsheet },
      { to: "/ambalaje", label: strings.nav.packaging, icon: Package },
    ],
  },
  {
    label: strings.nav.groupReporting,
    items: [
      { to: "/termene", label: strings.nav.deadlines, icon: CalendarClock },
      { to: "/dosar-control", label: strings.nav.auditFile, icon: FolderArchive },
    ],
  },
  {
    label: strings.nav.groupSetup,
    items: [
      { to: "/parteneri", label: strings.nav.partners, icon: Users },
      { to: "/setari", label: strings.nav.settings, icon: Settings },
    ],
  },
  {
    label: strings.nav.groupAdmin,
    platformAdminOnly: true,
    items: [{ to: "/clienti", label: strings.nav.clients, icon: Building2 }],
  },
];

/**
 * Current-company block under the app name. Normal users see their company name (read-only).
 * PLATFORM_ADMIN gets a tenant switcher that sets X-Tenant-Id (via `switchTenant`) and drops the
 * cached data of the company being left.
 */
function CompanyBlock() {
  const { user, tenantId, switchTenant } = useAuth();
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";
  const queryClient = useQueryClient();
  const { data: companies, isError } = useCompanies(!!isPlatformAdmin);

  if (!isPlatformAdmin) {
    return (
      <div className="rounded-md bg-brand-muted px-3 py-2">
        <div className="text-[11px] font-medium uppercase tracking-wide text-brand/70">
          {strings.header.currentCompany}
        </div>
        <div className="mt-0.5 flex items-center gap-2 text-sm font-semibold text-brand">
          <Building2 className="h-4 w-4 shrink-0" aria-hidden />
          <span className="truncate">
            {user?.tenantName ?? strings.header.noCompanySelected}
          </span>
        </div>
      </div>
    );
  }

  function handleChange(id: string) {
    // Switching tenant must not mix data between companies: drop every cached query. The list of
    // companies is the one exception — it is the same for every tenant, and it is read from here,
    // outside the subtree that remounts below, so a removed query would leave this switcher
    // holding data nothing ever refetches.
    queryClient.removeQueries({
      predicate: (query) => query.queryKey[0] !== companiesKey[0],
    });
    switchTenant(id || null);
  }

  return (
    <div className="rounded-md bg-brand-muted px-3 py-2">
      <label
        htmlFor="tenant-switcher"
        className="block text-[11px] font-medium uppercase tracking-wide text-brand/70"
      >
        {strings.header.currentCompany}
      </label>
      <Select
        id="tenant-switcher"
        className="mt-1 h-9 bg-surface"
        value={tenantId ?? ""}
        onChange={(e) => handleChange(e.target.value)}
      >
        <option value="">{strings.header.selectCompany}</option>
        {companies?.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </Select>
      {isError && (
        <div className="mt-1 text-xs text-red-600">{strings.header.loadCompaniesError}</div>
      )}
    </div>
  );
}

/**
 * Contul, ca meniu.
 *
 * <p>Erau două rânduri de text — adresa și rolul — plus un buton de deconectare mereu vizibil, în
 * subsolul barei. Deconectarea e o acțiune rară care stătea în drum la fiecare privire, iar rolul
 * se afișa ca `PLATFORM_ADMIN`, adică numele constantei din backend.
 */
function UserMenu({
  email,
  role,
  onLogout,
}: {
  email?: string;
  role?: string;
  onLogout: () => void;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  // Prima literă a adresei. Nu e o poză de profil, dar e un reper care se ține minte mai ușor
  // decât un rând de text tăiat la jumătate.
  const initial = (email ?? "?").charAt(0).toUpperCase();

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={strings.common.userMenu}
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex w-full items-center gap-2.5 rounded-md px-2 py-2 text-left transition-colors hover:bg-surface-sunken"
      >
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-muted text-sm font-semibold text-brand">
          {initial}
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-xs font-medium text-content">{email}</span>
          <span className="block truncate text-[11px] text-content-subtle">
            {role ? (strings.enums.role[role as keyof typeof strings.enums.role] ?? role) : ""}
          </span>
        </span>
        <ChevronUp
          className={cn(
            "h-4 w-4 shrink-0 text-content-subtle transition-transform",
            !open && "rotate-180"
          )}
          aria-hidden
        />
      </button>
      {open && (
        <div
          role="menu"
          className="absolute bottom-full left-0 z-30 mb-1 w-full animate-slide-up overflow-hidden rounded-md border border-line bg-surface py-1 shadow-popover"
        >
          <button
            type="button"
            role="menuitem"
            onClick={onLogout}
            className="flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm text-content transition-colors hover:bg-surface-muted"
          >
            <LogOut className="h-4 w-4 shrink-0" aria-hidden />
            {strings.nav.logout}
          </button>
        </div>
      )}
    </div>
  );
}

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout, tenantId } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  /**
   * Sertarul de navigație, pe ecran îngust. Pe `lg` în sus bara e mereu acolo și starea asta nu
   * schimbă nimic — clasele `lg:` o ignoră.
   */
  const [navOpen, setNavOpen] = useState(false);

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

  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";
  const groups = navGroups.filter((g) => !g.platformAdminOnly || isPlatformAdmin);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const commands = useNavigationCommands(groups);

  /**
   * `/` duce în caseta de căutare a ecranului curent, oriunde ar fi ea.
   *
   * <p>Legătura se face prin DOM (`[data-table-search]`), nu printr-un context cu referințe:
   * ecranele care au o casetă o au deja randată, iar cele care n-au n-ar avea ce să pună în
   * context. Un selector nu cere nimic de la nimeni.
   */
  useHotkey("/", () => {
    const search = document.querySelector<HTMLInputElement>("[data-table-search]");
    if (search) {
      search.focus();
      search.select();
    }
  });

  return (
    <div className="flex h-full">
      {/* Prima oprire a tastaturii: sare peste cele nouă intrări de meniu, care se repetă pe
          fiecare pagină. Invizibil până primește focus. */}
      <a
        href="#continut"
        className="sr-only-focusable absolute left-4 top-4 z-[80] rounded-md bg-brand px-3 py-2 text-sm font-medium text-brand-fg"
      >
        {strings.common.skipToContent}
      </a>

      {/* Bara de sus, doar pe ecran îngust: butonul de meniu și numele aplicației. */}
      <header className="fixed inset-x-0 top-0 z-30 flex h-14 items-center gap-3 border-b border-line bg-surface px-4 lg:hidden">
        <button
          type="button"
          onClick={() => setNavOpen(true)}
          aria-label={strings.common.openNav}
          aria-expanded={navOpen}
          aria-controls="navigatie-principala"
          className="-ml-2 rounded-md p-2 text-content-muted hover:bg-surface-sunken"
        >
          <Menu className="h-5 w-5" />
        </button>
        <div className="min-w-0">
          <div className="truncate font-bold text-brand">{strings.appName}</div>
        </div>
      </header>

      {/* Fundalul sertarului. Nu există pe `lg`, unde bara e parte din pagină. */}
      {navOpen && (
        <div
          className="fixed inset-0 z-40 animate-fade-in bg-black/40 lg:hidden"
          onClick={() => setNavOpen(false)}
          aria-hidden
        />
      )}

      <aside
        id="navigatie-principala"
        className={cn(
          // Pe ecran îngust: sertar peste pagină, care intră din stânga. Pe `lg`: coloana
          // dintotdeauna, în fluxul paginii.
          "fixed inset-y-0 left-0 z-50 flex w-64 shrink-0 flex-col border-r border-line bg-surface transition-transform duration-200 lg:static lg:z-auto lg:w-60 lg:translate-x-0",
          navOpen ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="flex items-start justify-between px-5 py-5">
          <div className="min-w-0">
            <div className="text-lg font-bold text-brand">{strings.appName}</div>
            <div className="text-xs text-content-subtle">{strings.tagline}</div>
          </div>
          <button
            type="button"
            onClick={() => setNavOpen(false)}
            aria-label={strings.common.closeNav}
            className="-mr-2 rounded-md p-2 text-content-subtle hover:bg-surface-sunken lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="px-3 pb-3">
          <CompanyBlock />
        </div>
        <nav aria-label={strings.common.mainNav} className="flex-1 overflow-y-auto px-3 pb-3">
          {groups.map((group, index) => (
            <div key={group.label ?? "principal"} className={index > 0 ? "mt-5" : undefined}>
              {group.label && (
                <div className="px-3 pb-1.5 text-[11px] font-semibold uppercase tracking-wide text-content-subtle">
                  {group.label}
                </div>
              )}
              <div className="space-y-1">
                {group.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      cn(
                        "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                        isActive
                          ? "bg-brand-muted text-brand"
                          : "text-content-muted hover:bg-surface-sunken"
                      )
                    }
                  >
                    <item.icon className="h-4 w-4 shrink-0" aria-hidden />
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </div>
          ))}
        </nav>
        {/* Scurtăturile scrise undeva: altfel există, dar nu le găsește nimeni. Ascunse pe
            ecran îngust, unde nu e tastatură. */}
        <p className="hidden px-5 pb-3 text-[11px] leading-relaxed text-content-subtle lg:block">
          {strings.common.shortcutHint}
        </p>
        <div className="border-t border-line p-3">
          <UserMenu email={user?.email} role={user?.role} onLogout={handleLogout} />
        </div>
      </aside>

      {/*
        Keyed by company: changing it remounts the page instead of leaving it on screen with the
        previous tenant's filters — a work-point id or a partner selected for the company we just
        left means nothing in the new one. The cache was emptied in `handleChange`, so the fresh
        mount refetches everything with the new X-Tenant-Id, and nobody has to reload the page.
      */}
      <CommandPalette commands={commands} />

      <main
        id="continut"
        key={tenantId ?? "fara-companie"}
        // `pt-14` lasă loc barei fixe de sus, care există doar sub `lg`. Marginile cresc cu
        // ecranul: pe telefon 16px sunt tot ce se poate da, pe desktop rămân cele 32 de dinainte.
        className="flex-1 overflow-auto px-4 pb-8 pt-[4.5rem] sm:px-6 lg:p-8"
      >
        {children}
      </main>
    </div>
  );
}
