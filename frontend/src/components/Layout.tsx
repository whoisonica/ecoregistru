import { useEffect, useState } from "react";
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
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { companiesKey, useCompanies } from "@/hooks/useCompanies";
import { Select } from "@/components/ui/select";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

const navItems: NavItem[] = [
  { to: "/", label: strings.nav.dashboard, icon: LayoutDashboard, end: true },
  { to: "/miscari", label: strings.nav.movements, icon: Truck },
  { to: "/evidente", label: strings.nav.evidences, icon: FileSpreadsheet },
  { to: "/parteneri", label: strings.nav.partners, icon: Users },
  { to: "/termene", label: strings.nav.deadlines, icon: CalendarClock },
  { to: "/ambalaje", label: strings.nav.packaging, icon: Package },
  { to: "/dosar-control", label: strings.nav.auditFile, icon: FolderArchive },
  { to: "/setari", label: strings.nav.settings, icon: Settings },
];

/** Nav item only PLATFORM_ADMIN sees: manage the client companies (tenants). */
const clientsNavItem: NavItem = { to: "/clienti", label: strings.nav.clients, icon: Building2 };

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

  // Platform admin gets the "Clienți" entry, inserted just before Settings.
  const items =
    user?.role === "PLATFORM_ADMIN"
      ? [...navItems.slice(0, -1), clientsNavItem, navItems[navItems.length - 1]]
      : navItems;

  function handleLogout() {
    logout();
    navigate("/login");
  }

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
        <nav aria-label={strings.common.mainNav} className="flex-1 space-y-1 overflow-y-auto px-3">
          {items.map((item) => (
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
        </nav>
        <div className="border-t border-line p-3">
          <div className="px-2 pb-2 text-xs text-content-muted">
            <div className="truncate font-medium text-content">{user?.email}</div>
            <div>{user?.role}</div>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-content-muted transition-colors hover:bg-surface-sunken"
          >
            <LogOut className="h-4 w-4 shrink-0" aria-hidden />
            {strings.nav.logout}
          </button>
        </div>
      </aside>

      {/*
        Keyed by company: changing it remounts the page instead of leaving it on screen with the
        previous tenant's filters — a work-point id or a partner selected for the company we just
        left means nothing in the new one. The cache was emptied in `handleChange`, so the fresh
        mount refetches everything with the new X-Tenant-Id, and nobody has to reload the page.
      */}
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
