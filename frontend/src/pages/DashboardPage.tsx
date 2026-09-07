import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
  Package,
  Plus,
  Scale,
  Trash2,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useEvidences } from "@/hooks/useEvidences";
import { useMovements } from "@/hooks/useMovements";
import { useDeadlines } from "@/hooks/useDeadlines";
import { usePartners } from "@/hooks/usePartners";
import type { DeadlineStatus } from "@/lib/types";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardHeader } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { ListSkeleton, Skeleton } from "@/components/ui/skeleton";
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

/** Cantitățile vin din backend în kilograme; se scriu cu separatorul românesc. */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 });

/**
 * Câte zile mai sunt până la o dată, socotite pe zile calendaristice.
 *
 * <p>Se compară la miezul nopții, nu la ora curentă: altfel un termen de mâine dimineață ar ieși
 * „0 zile" după-amiaza, ceea ce e adevărat în ore și fals în felul în care se citește un calendar.
 */
function daysUntil(iso: string): number {
  const [y, m, d] = iso.split("-").map(Number);
  const target = new Date(y, m - 1, d);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

function StatTile({
  icon: Icon,
  value,
  label,
  sub,
  tone,
  loading = false,
}: {
  icon: typeof Scale;
  value: string;
  label: string;
  sub: string;
  tone: "brand" | "amber" | "red";
  loading?: boolean;
}) {
  const toneClasses = {
    brand: "bg-brand-muted text-brand",
    amber: "bg-amber-100 text-amber-700",
    red: "bg-red-100 text-red-700",
  }[tone];
  return (
    <Card>
      <div className="flex items-center gap-3">
        <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${toneClasses}`}>
          <Icon className="h-5 w-5" aria-hidden />
        </div>
        {loading ? (
          <Skeleton className="h-8 w-24" />
        ) : (
          <div className="truncate text-3xl font-bold text-content">{value}</div>
        )}
      </div>
      <div className="mt-3 text-sm font-medium text-content-muted">{label}</div>
      <div className="text-xs text-content-subtle">{sub}</div>
    </Card>
  );
}

/**
 * Un lucru care stă în calea depunerii, cu urmarea lui și cu drumul spre reparație.
 *
 * <p>Roșu și galben nu sunt aceeași afirmație, și au fost ținute distincte peste tot în aplicație:
 * o ieșire fără cod R/D e o **greșeală** (cantitatea nu intră în nicio coloană oficială), pe când
 * o linie care așteaptă cântarul e o **așteptare legitimă**. Panoul păstrează diferența.
 */
function Blocker({
  icon: Icon,
  tone,
  title,
  hint,
  to,
}: {
  icon: typeof AlertTriangle;
  tone: "danger" | "warning";
  title: string;
  hint: string;
  to: string;
}) {
  return (
    <div
      className={cn(
        "flex items-start gap-3 rounded-lg border p-3",
        tone === "danger" ? "border-red-200 bg-red-50" : "border-amber-200 bg-amber-50"
      )}
    >
      <Icon
        className={cn(
          "mt-0.5 h-4 w-4 shrink-0",
          tone === "danger" ? "text-red-600" : "text-amber-600"
        )}
        aria-hidden
      />
      <div className="min-w-0 flex-1">
        <p
          className={cn(
            "text-sm font-medium",
            tone === "danger" ? "text-red-900" : "text-amber-900"
          )}
        >
          {title}
        </p>
        <p className={cn("mt-0.5 text-xs", tone === "danger" ? "text-red-800" : "text-amber-800")}>
          {hint}
        </p>
      </div>
      <Link
        to={to}
        className={cn(
          "shrink-0 whitespace-nowrap text-xs font-medium hover:underline",
          tone === "danger" ? "text-red-700" : "text-amber-700"
        )}
      >
        {t.blockerFix}
      </Link>
    </div>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const canAdd = user?.role !== "CLIENT_VIEWER";

  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;

  const { data: movements, isLoading: loadingMovements } = useMovements({ year, month });
  const { data: deadlines, isLoading: loadingDeadlines } = useDeadlines(year);
  const { data: partners, isLoading: loadingPartners } = usePartners();
  /**
   * Evidența anului întreg, nu a lunii: ce blochează depunerea e o întrebare despre an, fiindcă
   * fișa și declarația acoperă anul. O linie fără cod R/D din martie strică depunerea din martie
   * anul viitor, oricât de curată ar fi luna curentă.
   */
  const { data: evidences, isLoading: loadingEvidences } = useEvidences({ year });

  const openDeadlines = useMemo(
    () =>
      [...(deadlines ?? [])]
        .filter((d) => d.status !== "DONE")
        .sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
    [deadlines]
  );
  const overdueCount = openDeadlines.filter((d) => d.status === "OVERDUE").length;
  const nextDeadline = openDeadlines.find((d) => d.status !== "OVERDUE");

  const expiringPartners = useMemo(
    () => (partners ?? []).filter((p) => p.active && p.expiringSoon),
    [partners]
  );

  /** Cele două feluri de „nu e gata", numărate pe linii de evidență. */
  const blockers = useMemo(() => {
    const rows = evidences ?? [];
    return {
      missingCode: rows.filter((r) => r.totalUnclassifiedOut > 0).length,
      awaitingWeighing: rows.filter((r) => r.awaitingWeighing).length,
    };
  }, [evidences]);
  const blockerCount = blockers.missingCode + blockers.awaitingWeighing;

  /** Kilogramele lunii — cifra pe care o caută cineva, spre deosebire de numărul de rânduri. */
  const generatedThisMonth = useMemo(
    () => (movements ?? []).reduce((sum, m) => sum + (m.quantity ?? 0), 0),
    [movements]
  );
  /** Stocul de la ultima lună calculată a anului, pe toate codurile. */
  const currentStock = useMemo(() => {
    const rows = evidences ?? [];
    if (rows.length === 0) return 0;
    const lastMonth = Math.max(...rows.map((r) => r.month));
    return rows.filter((r) => r.month === lastMonth).reduce((sum, r) => sum + r.closingStock, 0);
  }, [evidences]);

  const monthLabel = strings.months[month - 1];

  return (
    <div>
      <PageHeader
        title={t.title}
        description={
          <>
            {t.welcome}, <span className="font-medium">{user?.email}</span>.
          </>
        }
        actions={
          canAdd && (
            <Link to="/miscari">
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                {t.addMovement}
              </Button>
            </Link>
          )
        }
      />

      {/* Starea de conformitate, înaintea oricărei cifre: e întrebarea pentru care clientul
          deschide aplicația, iar până acum răspunsul se afla derulând Evidențe. */}
      <Card className="mt-6">
        <CardHeader
          title={t.statusTitle.replace("{year}", String(year))}
          action={
            <Link
              to="/evidente"
              className="flex items-center gap-1 text-sm text-brand hover:underline"
            >
              {t.viewAll}
              <ChevronRight className="h-4 w-4" aria-hidden />
            </Link>
          }
        />
        {loadingEvidences ? (
          <div className="mt-4 space-y-2">
            <Skeleton className="h-14 w-full rounded-lg" />
          </div>
        ) : blockerCount === 0 ? (
          <div className="mt-4 flex items-start gap-3 rounded-lg border border-emerald-200 bg-emerald-50 p-3">
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" aria-hidden />
            <div>
              <p className="text-sm font-medium text-emerald-900">{t.statusOk}</p>
              <p className="mt-0.5 text-xs text-emerald-800">{t.statusOkHint}</p>
            </div>
          </div>
        ) : (
          <div className="mt-4 space-y-2">
            {blockers.missingCode > 0 && (
              <Blocker
                icon={Trash2}
                tone="danger"
                title={t.blockerMissingCode.replace("{n}", String(blockers.missingCode))}
                hint={t.blockerMissingCodeHint}
                to="/evidente?vedere=monthly"
              />
            )}
            {blockers.awaitingWeighing > 0 && (
              <Blocker
                icon={Scale}
                tone="warning"
                title={t.blockerAwaitingWeighing.replace(
                  "{n}",
                  String(blockers.awaitingWeighing)
                )}
                hint={t.blockerAwaitingWeighingHint}
                to="/miscari"
              />
            )}
          </div>
        )}
      </Card>

      {/* Cifrele. Kilograme, nu rânduri: nimeni nu se uită la câte înregistrări are luna. */}
      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatTile
          icon={Package}
          value={kgFormat.format(generatedThisMonth)}
          label={t.statGenerated.replace("{month}", monthLabel)}
          sub={t.statGeneratedSub}
          tone="brand"
          loading={loadingMovements}
        />
        <StatTile
          icon={Scale}
          value={kgFormat.format(currentStock)}
          label={t.statStock}
          sub={t.statStockSub}
          tone={currentStock < 0 ? "red" : "brand"}
          loading={loadingEvidences}
        />
        <StatTile
          icon={CalendarClock}
          value={String(openDeadlines.length)}
          label={t.statDeadlines}
          sub={
            overdueCount > 0
              ? t.statDeadlinesOverdue.replace("{n}", String(overdueCount))
              : nextDeadline
                ? t.statDeadlinesNext
                    .replace("{label}", strings.enums.reportType[nextDeadline.reportType])
                    .replace("{days}", String(daysUntil(nextDeadline.dueDate)))
                : t.statDeadlinesNone
          }
          tone={overdueCount > 0 ? "red" : openDeadlines.length > 0 ? "amber" : "brand"}
          loading={loadingDeadlines}
        />
      </div>

      {/* Detail lists */}
      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Upcoming deadlines */}
        <Card>
          <CardHeader
            title={t.upcomingTitle}
            action={
              <Link
                to="/termene"
                className="flex items-center gap-1 text-sm text-brand hover:underline"
              >
                {t.viewAll}
                <ChevronRight className="h-4 w-4" aria-hidden />
              </Link>
            }
          />
          {loadingDeadlines ? (
            <ListSkeleton />
          ) : openDeadlines.length === 0 ? (
            <p className="mt-4 text-sm text-content-subtle">{t.upcomingEmpty}</p>
          ) : (
            <ul className="mt-3 divide-y divide-line">
              {openDeadlines.slice(0, 5).map((d) => {
                const days = daysUntil(d.dueDate);
                return (
                  <li key={d.id} className="flex items-center justify-between gap-3 py-2.5">
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-medium text-content">
                        {strings.enums.reportType[d.reportType]}
                      </div>
                      {/* Data singură cere o socoteală în cap; numărul de zile e răspunsul. */}
                      <div className="truncate text-xs text-content-subtle">
                        {formatDate(d.dueDate)}
                        {d.status !== "OVERDUE" && days >= 0 && (
                          <> · {t.daysShort.replace("{n}", String(days))}</>
                        )}
                      </div>
                    </div>
                    <Badge variant={statusVariant[d.status]} className="shrink-0">
                      {strings.enums.deadlineStatus[d.status]}
                    </Badge>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>

        {/* Expiring partner authorizations */}
        <Card>
          <CardHeader
            title={t.expiringTitle}
            action={
              <Link
                to="/parteneri"
                className="flex items-center gap-1 text-sm text-brand hover:underline"
              >
                {t.viewAll}
                <ChevronRight className="h-4 w-4" aria-hidden />
              </Link>
            }
          />
          {loadingPartners ? (
            <ListSkeleton />
          ) : expiringPartners.length === 0 ? (
            <p className="mt-4 text-sm text-content-subtle">{t.expiringEmpty}</p>
          ) : (
            <ul className="mt-3 divide-y divide-line">
              {expiringPartners.slice(0, 5).map((p) => {
                const days = p.authorizationExpiry ? daysUntil(p.authorizationExpiry) : null;
                return (
                  <li key={p.id} className="flex items-center justify-between gap-3 py-2.5">
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-medium text-content">{p.name}</div>
                      <div className="truncate text-xs text-content-subtle">
                        {p.authorizationNumber ?? strings.partners.noAuthorization}
                      </div>
                    </div>
                    <Badge
                      variant={days != null && days < 0 ? "danger" : "warning"}
                      className="shrink-0"
                    >
                      {days == null
                        ? strings.partners.expiringSoon
                        : days < 0
                          ? t.statExpiringPast
                          : t.statExpiringDays.replace("{days}", String(days))}
                    </Badge>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>
      </div>

    </div>
  );
}
