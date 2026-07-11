import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Truck,
  FileSpreadsheet,
  Users,
  CalendarClock,
  FolderArchive,
  Settings,
  LogOut,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";
import type { ReactNode } from "react";

const navItems = [
  { to: "/", label: strings.nav.dashboard, icon: LayoutDashboard, end: true },
  { to: "/miscari", label: strings.nav.movements, icon: Truck },
  { to: "/evidente", label: strings.nav.evidences, icon: FileSpreadsheet },
  { to: "/parteneri", label: strings.nav.partners, icon: Users },
  { to: "/termene", label: strings.nav.deadlines, icon: CalendarClock },
  { to: "/dosar-control", label: strings.nav.auditFile, icon: FolderArchive },
  { to: "/setari", label: strings.nav.settings, icon: Settings },
];

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

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
        <nav className="flex-1 space-y-1 px-3">
          {navItems.map((item) => (
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
      <main className="flex-1 overflow-auto p-8">{children}</main>
    </div>
  );
}
