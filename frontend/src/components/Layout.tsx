import { NavLink, useNavigate } from "react-router-dom";
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
          <Building2 className="h-4 w-4 shrink-0" />
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
      <div className="text-[11px] font-medium uppercase tracking-wide text-brand/70">
        {strings.header.currentCompany}
      </div>
      <Select
        className="mt-1 h-9 bg-white"
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
      <aside className="flex w-60 shrink-0 flex-col border-r border-gray-200 bg-white">
        <div className="px-5 py-5">
          <div className="text-lg font-bold text-brand">{strings.appName}</div>
          <div className="text-xs text-gray-400">{strings.tagline}</div>
        </div>
        <div className="px-3 pb-3">
          <CompanyBlock />
        </div>
        <nav className="flex-1 space-y-1 px-3">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium",
                  isActive ? "bg-brand-muted text-brand" : "text-gray-600 hover:bg-gray-100"
                )
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-gray-200 p-3">
          <div className="px-2 pb-2 text-xs text-gray-500">
            <div className="truncate font-medium text-gray-700">{user?.email}</div>
            <div>{user?.role}</div>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
          >
            <LogOut className="h-4 w-4" />
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
      <main key={tenantId ?? "fara-companie"} className="flex-1 overflow-auto p-8">
        {children}
      </main>
    </div>
  );
}
