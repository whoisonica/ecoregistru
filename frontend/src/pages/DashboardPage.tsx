import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
  HelpCircle,
  Package,
  Plus,
  Scale,
  Trash2,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useDashboardData, type NextAction } from "@/hooks/useDashboardData";
import { useCurrentCompany } from "@/hooks/useCompanies";
import type { DeadlineStatus } from "@/lib/types";
import { strings } from "@/lib/strings";
import { cn, countOf, formatDate, withCount } from "@/lib/utils";
import { daysLabel, daysUntil } from "@/lib/deadlines";
import { screensFor, SCREEN_PATH } from "@/lib/movementScreens";
import { LinkButton } from "@/components/ui/button";
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

/** Cantitățile vin din backend în kilograme; se scriu cu separatorul românesc. */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 });

/**
 * O dală cu o cifră mare, în mono, ca pe afișajul cântarului.
 */
function StatTile({
  icon: Icon,
  value,
  unit,
  label,
  sub,
  tone,
  testId,
  loading = false,
  failed = false,
  children,
}: {
  icon: typeof Scale;
  value: string;
  unit?: string;
  label: string;
  sub: string;
  tone: "brand" | "amber" | "red";
  /** Cârligul probelor: proba 11 (și 10) citesc dala de termene, proba 9 pe cea de stoc. */
  testId?: string;
  loading?: boolean;
  /**
   * Sursa n-a răspuns. Se arată „—", nu cifra socotită din lista goală: un `0` mare şi verde
   * pentru „n-am putut citi" e aceeaşi minciună ca „Eşti la zi" din bandă, doar mai scurtă.
   */
  failed?: boolean;
  /** Detaliul care face cifra să însemne ceva — la stoc, chiar codurile care îl poartă. */
  children?: ReactNode;
}) {
  const toneClass = failed
    ? "text-content-subtle"
    : { brand: "text-content", amber: "text-state-warn-text", red: "text-state-bad-text" }[tone];
  return (
    <Card data-testid={testId}>
      <div className="flex items-center justify-between gap-3">
        <div className="eyebrow">{label}</div>
        <Icon className="h-4 w-4 shrink-0 text-content-subtle" aria-hidden />
      </div>
      {loading ? (
        <Skeleton className="mt-2 h-8 w-24" />
      ) : (
        <div className={cn("mt-1 truncate font-mono text-[1.75rem] font-medium leading-tight", toneClass)}>
          {failed ? "—" : value}
          {!failed && unit && <span className="ml-1 text-xs font-normal text-content-muted">{unit}</span>}
        </div>
      )}
      <div className="mt-1 text-xs text-content-muted">{failed ? t.statLoadError : sub}</div>
      {!loading && !failed && children}
    </Card>
  );
}

/**
 * Un lucru care stă în calea depunerii, cu urmarea lui și cu drumul spre reparație.
 *
 * <p>Roșu și galben nu sunt aceeași afirmație: o ieșire fără cod R/D e o **greșeală** (cantitatea
 * nu intră în nicio coloană oficială), pe când o linie care așteaptă cântarul e o **așteptare
 * legitimă**. Acasă păstrează diferența — ca LED, nu ca fundal colorat.
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
    <div className="flex items-start gap-3 border-t border-line py-3 first:border-t-0">
      <Icon
        className={cn("mt-0.5 h-4 w-4 shrink-0", tone === "danger" ? "text-state-bad" : "text-state-warn")}
        aria-hidden
      />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-content">
          <Badge variant={tone} className="mr-2 align-middle" />
          {title}
        </p>
        <p className="mt-0.5 text-xs text-content-muted">{hint}</p>
      </div>
      <Link to={to} className="shrink-0 whitespace-nowrap text-xs font-semibold text-brand-700 hover:underline">
        {t.blockerFix}
      </Link>
    </div>
  );
}

/**
 * Banda din capul ecranului: **un** lucru de făcut, nu cinci de citit. Tace cât timp datele n-au
 * venit toate, și spune „nu știu" când o sursă a căzut (`useDashboardData`).
 *
 * <p>„Cântar”: o cutie cu chenar grafit, LED-ul stării, propoziția și butonul — nu un banner colorat.
 */
function NextActionBand({ action, loading }: { action: NextAction | null; loading: boolean }) {
  if (loading || !action) {
    return <Skeleton className="mt-5 h-[4.5rem] w-full rounded-lg" />;
  }
  const led: Record<NextAction["tone"], BadgeProps["variant"]> = {
    danger: "danger",
    warning: "warning",
    ok: "success",
    start: "default",
    unknown: "muted",
  };
  const word: Record<NextAction["tone"], string> = {
    danger: t.toneNow,
    warning: t.toneSoon,
    ok: t.toneOk,
    start: t.toneStart,
    unknown: t.toneUnknown,
  };
  return (
    <div
      data-testid="next-action"
      className={cn(
        "mt-5 flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-center",
        action.tone === "danger" ? "border-state-bad" : action.tone === "warning" ? "border-state-warn" : "border-content"
      )}
    >
      <div className="min-w-0 flex-1">
        <p className="eyebrow">
          {t.nextTitle} · <Badge variant={led[action.tone]}>{word[action.tone]}</Badge>
        </p>
        <p className="mt-1 text-base font-semibold text-content">{action.title}</p>
        <p className="mt-0.5 text-xs text-content-muted">{action.hint}</p>
      </div>
      {action.tone === "unknown" ? (
        // Aici nu e unde să duci pe cineva: ce lipseşte e răspunsul, nu ecranul.
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="shrink-0 whitespace-nowrap text-sm font-semibold text-content hover:underline"
        >
          {action.cta}
        </button>
      ) : (
        <LinkButton to={action.to} variant={action.tone === "ok" ? "outline" : "default"} className="shrink-0">
          {action.cta}
        </LinkButton>
      )}
    </div>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const canAdd = user?.role !== "CLIENT_VIEWER";
  const { data: company } = useCurrentCompany();
  const d = useDashboardData();
  const screens = screensFor(company?.type);
  const firstScreen = screens[0];
  const addLabel =
    firstScreen === "IN"
      ? strings.panel.addInbound
      : screens.includes("IN")
        ? strings.panel.addOwnWaste
        : strings.panel.addWaste;

  return (
    <div>
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          canAdd && (
            <LinkButton to={`${SCREEN_PATH[firstScreen]}?nou=1`}>
              <Plus className="mr-2 h-4 w-4" />
              {addLabel}
            </LinkButton>
          )
        }
      />

      {/* Un lucru de făcut, înaintea stării: „sunt în regulă?" are răspunsul mai jos, dar
          „ce fac acum?" n-avea niciunul — se citeau cinci locuri și se trăgea singur concluzia. */}
      <NextActionBand action={d.nextAction} loading={d.nextActionLoading} />

      {/* Cifrele. Kilograme, nu rânduri: nimeni nu se uită la câte înregistrări are luna. */}
      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <StatTile
          icon={Package}
          value={kgFormat.format(d.generatedThisMonth)}
          unit={strings.panel.kg}
          label={t.statGenerated.replace("{month}", d.monthLabel)}
          sub={
            d.movementCount === 0
              ? t.statGeneratedSubNone
              : t.statGeneratedSub.replace("{count}", countOf(d.movementCount, "mișcare", "mișcări"))
          }
          tone="brand"
          loading={d.loadingMovements}
          failed={d.failedMovements}
        />
        <StatTile
          icon={CalendarClock}
          value={String(d.openDeadlines.length)}
          label={t.statDeadlines}
          sub={
            d.overdueCount > 0
              ? withCount(t.statDeadlinesOverdue, d.overdueCount, "termen depășit", "termene depășite")
              : d.nextDeadline
                ? t.statDeadlinesNext
                    .replace("{label}", strings.enums.reportType[d.nextDeadline.reportType])
                    .replace("{days}", daysLabel(d.nextDeadline) ?? "")
                : t.statDeadlinesNone
          }
          tone={d.overdueCount > 0 ? "red" : d.openDeadlines.length > 0 ? "amber" : "brand"}
          testId="stat-deadlines"
          loading={d.loadingDeadlines}
          failed={d.failedDeadlines}
        />
      </div>

      {/* Starea de conformitate: e întrebarea pentru care clientul deschide aplicația, iar până
          acum răspunsul se afla derulând Evidențe. */}
      <Card className="mt-4">
        <CardHeader
          title={t.statusTitle.replace("{year}", String(d.year))}
          action={
            <Link to="/evidente" className="flex items-center gap-1 text-sm font-medium text-brand-700 hover:underline">
              {t.viewAll}
              <ChevronRight className="h-4 w-4" aria-hidden />
            </Link>
          }
        />
        {d.loadingEvidences ? (
          <div className="mt-4 space-y-2">
            <Skeleton className="h-14 w-full rounded-lg" />
          </div>
        ) : d.failedEvidences ? (
          /* „Nimic nu blochează documentele" e o afirmaţie despre linii; fără linii citite, e o
             afirmaţie despre nimic. Aceeaşi regulă ca la bandă. */
          <StatusNote icon={HelpCircle} title={t.statusUnknown} hint={t.statusUnknownHint} />
        ) : (d.evidences ?? []).length === 0 ? (
          /* Zero blocaje şi zero de raportat nu sunt acelaşi lucru. */
          <StatusNote icon={HelpCircle} title={t.statusEmpty.replace("{year}", String(d.year))} hint={t.statusEmptyHint} />
        ) : d.blockerCount === 0 ? (
          <StatusNote icon={CheckCircle2} tone="ok" title={t.statusOk} hint={t.statusOkHint} />
        ) : (
          <div className="mt-3">
            {d.blockers.missingCode > 0 && (
              <Blocker
                icon={Trash2}
                tone="danger"
                title={t.blockerMissingCode.replace("{count}", countOf(d.blockers.missingCode, "linie", "linii"))}
                hint={t.blockerMissingCodeHint}
                /* Registrul de predări e nivelul la care un rând **este** o mișcare, iar filtrul
                   îl restrânge la ce blochează chiar depunerea. */
                to="/evidente?vedere=handovers&problema=cod-rd"
              />
            )}
            {d.blockers.awaitingWeighing > 0 && (
              <Blocker
                icon={Scale}
                tone="warning"
                title={t.blockerAwaitingWeighing.replace(
                  "{count}",
                  countOf(d.blockers.awaitingWeighing, "linie", "linii")
                )}
                hint={t.blockerAwaitingWeighingHint}
                to="/miscari"
              />
            )}
          </div>
        )}
      </Card>

      {/* Detail lists */}
      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader
            title={t.upcomingTitle}
            action={
              <Link to="/termene" className="flex items-center gap-1 text-sm font-medium text-brand-700 hover:underline">
                {t.viewAll}
                <ChevronRight className="h-4 w-4" aria-hidden />
              </Link>
            }
          />
          {d.loadingDeadlines ? (
            <ListSkeleton />
          ) : d.failedDeadlines ? (
            <p className="mt-4 text-sm text-content-subtle">{t.listLoadError}</p>
          ) : d.openDeadlines.length === 0 ? (
            <p className="mt-4 text-sm text-content-subtle">{t.upcomingEmpty}</p>
          ) : (
            <ul className="mt-3 divide-y divide-line">
              {d.openDeadlines.slice(0, 5).map((dl) => {
                const days = daysLabel(dl);
                return (
                  <li key={dl.id} className="flex items-center justify-between gap-3 py-2.5">
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-medium text-content">
                        {strings.enums.reportType[dl.reportType]}
                      </div>
                      <div className="truncate font-mono text-xs text-content-muted">
                        {formatDate(dl.dueDate)}
                        {days && <> · {days}</>}
                      </div>
                    </div>
                    <Badge variant={statusVariant[dl.status]} className="shrink-0">
                      {strings.enums.deadlineStatus[dl.status]}
                    </Badge>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>

        <Card>
          <CardHeader
            title={t.expiringTitle}
            action={
              <Link to="/parteneri" className="flex items-center gap-1 text-sm font-medium text-brand-700 hover:underline">
                {t.viewAll}
                <ChevronRight className="h-4 w-4" aria-hidden />
              </Link>
            }
          />
          {d.loadingPartners ? (
            <ListSkeleton />
          ) : d.failedPartners ? (
            <p className="mt-4 text-sm text-content-subtle">{t.listLoadError}</p>
          ) : d.expiringPartners.length === 0 ? (
            <p className="mt-4 text-sm text-content-subtle">{t.expiringEmpty}</p>
          ) : (
            <ul className="mt-3 divide-y divide-line">
              {d.expiringPartners.slice(0, 5).map((p) => {
                const days = p.authorizationValidUntil ? daysUntil(p.authorizationValidUntil) : null;
                return (
                  <li key={p.id} className="flex items-center justify-between gap-3 py-2.5">
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-medium text-content">{p.name}</div>
                      <div className="truncate font-mono text-xs text-content-muted">
                        {p.authorizationNumber ?? strings.partners.noAuthorization}
                      </div>
                    </div>
                    <Badge variant={days != null && days < 0 ? "danger" : "warning"} className="shrink-0">
                      {days == null
                        ? strings.partners.expiringSoon
                        : days < 0
                          ? t.statExpiringPast
                          : days === 0
                            ? t.statExpiringToday
                            : days === 1
                              ? t.statExpiringTomorrow
                              : t.statExpiringDays.replace("{count}", countOf(days, "zi", "zile"))}
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

function StatusNote({
  icon: Icon,
  tone = "unknown",
  title,
  hint,
}: {
  icon: typeof HelpCircle;
  tone?: "ok" | "unknown";
  title: string;
  hint: string;
}) {
  return (
    <div className="mt-4 flex items-start gap-3 rounded-md border border-line-strong p-3">
      <Icon className={cn("mt-0.5 h-4 w-4 shrink-0", tone === "ok" ? "text-state-ok" : "text-content-subtle")} aria-hidden />
      <div>
        <p className="text-sm font-medium text-content">{title}</p>
        <p className="mt-0.5 text-xs text-content-muted">{hint}</p>
      </div>
    </div>
  );
}
