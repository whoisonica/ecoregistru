import { useMemo } from "react";
import { Link } from "react-router-dom";
import { Truck, CalendarClock, ShieldAlert, Plus, ChevronRight } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useMovements } from "@/hooks/useMovements";
import { useDeadlines } from "@/hooks/useDeadlines";
import { usePartners } from "@/hooks/usePartners";
import type { DeadlineStatus } from "@/lib/types";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import type { BadgeProps } from "@/components/ui/badge";

const t = strings.dashboard;

const statusVariant: Record<DeadlineStatus, BadgeProps["variant"]> = {
  UPCOMING: "warning",
  DONE: "success",
  OVERDUE: "danger",
};

function formatDate(iso: string): string {
  const [y, m, d] = iso.split("-");
  return `${d}.${m}.${y}`;
}

function StatTile({
  icon: Icon,
  value,
  label,
  sub,
  tone,
}: {
  icon: typeof Truck;
  value: number;
  label: string;
  sub: string;
  tone: "brand" | "amber" | "red";
}) {
  const toneClasses = {
    brand: "bg-brand-muted text-brand",
    amber: "bg-amber-100 text-amber-700",
    red: "bg-red-100 text-red-700",
  }[tone];
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5">
      <div className="flex items-center gap-3">
        <div className={`flex h-10 w-10 items-center justify-center rounded-lg ${toneClasses}`}>
          <Icon className="h-5 w-5" />
        </div>
        <div className="text-3xl font-bold text-gray-900">{value}</div>
      </div>
      <div className="mt-3 text-sm font-medium text-gray-700">{label}</div>
      <div className="text-xs text-gray-400">{sub}</div>
    </div>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const canAdd = user?.role !== "CLIENT_VIEWER";

  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;

  const { data: movements } = useMovements({ year, month });
  const { data: deadlines } = useDeadlines(year);
  const { data: partners } = usePartners();

  const openDeadlines = useMemo(
    () =>
      [...(deadlines ?? [])]
        .filter((d) => d.status !== "DONE")
        .sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
    [deadlines]
  );
  const overdueCount = openDeadlines.filter((d) => d.status === "OVERDUE").length;

  const expiringPartners = useMemo(
    () => (partners ?? []).filter((p) => p.active && p.expiringSoon),
    [partners]
  );

  const monthLabel = strings.months[month - 1];

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t.title}</h1>
          <p className="mt-1 text-gray-500">
            {t.welcome}, <span className="font-medium">{user?.email}</span>.
          </p>
        </div>
        {canAdd && (
          <Link to="/miscari">
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              {t.addMovement}
            </Button>
          </Link>
        )}
      </div>

      {/* Stat tiles */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatTile
          icon={Truck}
          value={movements?.length ?? 0}
          label={t.statMovements}
          sub={t.statMovementsSub.replace("{month}", monthLabel)}
          tone="brand"
        />
        <StatTile
          icon={CalendarClock}
          value={openDeadlines.length}
          label={t.statDeadlines}
          sub={t.statDeadlinesSub.replace("{overdue}", String(overdueCount))}
          tone={overdueCount > 0 ? "red" : "amber"}
        />
        <StatTile
          icon={ShieldAlert}
          value={expiringPartners.length}
          label={t.statExpiring}
          sub={t.statExpiringSub}
          tone={expiringPartners.length > 0 ? "amber" : "brand"}
        />
      </div>

      {/* Detail lists */}
      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        {/* Upcoming deadlines */}
        <div className="rounded-xl border border-gray-200 bg-white p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">{t.upcomingTitle}</h2>
            <Link
              to="/termene"
              className="flex items-center gap-1 text-sm text-brand hover:underline"
            >
              {t.viewAll}
              <ChevronRight className="h-4 w-4" />
            </Link>
          </div>
          {openDeadlines.length === 0 ? (
            <p className="mt-4 text-sm text-gray-400">{t.upcomingEmpty}</p>
          ) : (
            <ul className="mt-3 divide-y divide-gray-100">
              {openDeadlines.slice(0, 5).map((d) => (
                <li key={d.id} className="flex items-center justify-between py-2.5">
                  <div>
                    <div className="text-sm font-medium text-gray-800">
                      {strings.enums.reportType[d.reportType]}
                    </div>
                    <div className="text-xs text-gray-400">{formatDate(d.dueDate)}</div>
                  </div>
                  <Badge variant={statusVariant[d.status]}>
                    {strings.enums.deadlineStatus[d.status]}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Expiring partner authorizations */}
        <div className="rounded-xl border border-gray-200 bg-white p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">{t.expiringTitle}</h2>
            <Link
              to="/parteneri"
              className="flex items-center gap-1 text-sm text-brand hover:underline"
            >
              {t.viewAll}
              <ChevronRight className="h-4 w-4" />
            </Link>
          </div>
          {expiringPartners.length === 0 ? (
            <p className="mt-4 text-sm text-gray-400">{t.expiringEmpty}</p>
          ) : (
            <ul className="mt-3 divide-y divide-gray-100">
              {expiringPartners.slice(0, 5).map((p) => (
                <li key={p.id} className="flex items-center justify-between py-2.5">
                  <div>
                    <div className="text-sm font-medium text-gray-800">{p.name}</div>
                    <div className="text-xs text-gray-400">
                      {p.authorizationNumber ?? strings.partners.noAuthorization}
                    </div>
                  </div>
                  <Badge variant="warning">
                    {p.authorizationExpiry
                      ? formatDate(p.authorizationExpiry)
                      : strings.partners.expiringSoon}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
