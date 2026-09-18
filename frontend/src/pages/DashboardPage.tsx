import { useState } from "react";
import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useDashboardData, type NextAction } from "@/hooks/useDashboardData";
import { FirstSteps } from "@/components/panel/FirstSteps";
import { DeadlineStrip } from "@/components/home/DeadlineStrip";
import { WasteCodesCard, YearMonthsCard } from "@/components/home/YearCards";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { strings } from "@/lib/strings";
import { cn, countOf } from "@/lib/utils";
import { screensFor, SCREEN_PATH } from "@/lib/movementScreens";
import { LinkButton } from "@/components/ui/button";
import { Badge, type BadgeProps } from "@/components/ui/badge";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/skeleton";

const t = strings.dashboard;

const led: Record<NextAction["tone"], BadgeProps["variant"]> = {
  danger: "danger",
  warning: "warning",
  ok: "success",
  start: "default",
  unknown: "muted",
};

/**
 * Banda din capul ecranului: **un** lucru de făcut, nu cinci de citit. Tace cât timp datele n-au
 * venit toate, și spune „nu știu" când o sursă a căzut (`useDashboardData`).
 *
 * <p>„Cântar”: o cutie cu chenar grafit, LED-ul stării, propoziția și butonul — nu un banner colorat.
 */
function NextActionBand({ actions, loading }: { actions: NextAction[]; loading: boolean }) {
  const [open, setOpen] = useState(false);
  const action = actions[0];
  const rest = actions.slice(1);
  if (loading || !action) {
    return <Skeleton className="mt-5 h-[4.5rem] w-full rounded-lg" />;
  }
  const word: Record<NextAction["tone"], string> = {
    danger: t.toneNow,
    warning: t.toneSoon,
    ok: t.toneOk,
    start: t.toneStart,
    unknown: t.toneUnknown,
  };
  return (
    <div className="mt-5">
    <div
      data-testid="next-action"
      className={cn(
        "flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-center",
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
        <div className="flex shrink-0 items-center gap-4">
          {rest.length > 0 && (
            <button
              type="button"
              onClick={() => setOpen((v) => !v)}
              aria-expanded={open}
              className="whitespace-nowrap font-mono text-xs text-content-muted hover:text-content"
            >
              {open ? t.lessActions : t.moreActions.replace("{n}", String(rest.length))}
            </button>
          )}
          <LinkButton to={action.to} variant={action.tone === "ok" ? "outline" : "default"}>
            {action.cta}
          </LinkButton>
        </div>
      )}
    </div>
    {/* Celelalte, sub bandă și nu în ea: banda numește un singur lucru (proba 10). */}
    {open && rest.length > 0 && (
      <ul data-testid="more-actions" className="mt-2 rounded-lg border border-line px-4">
        {rest.map((a) => (
          <li key={a.title} className="flex items-center gap-3 border-t border-line py-2.5 first:border-t-0">
            <Badge variant={led[a.tone]} aria-hidden />
            <span className="min-w-0 flex-1 text-sm text-content">{a.title}</span>
            <Link to={a.to} className="shrink-0 whitespace-nowrap text-xs font-semibold text-brand-700 hover:underline">
              {a.cta}
            </Link>
          </li>
        ))}
      </ul>
    )}
    </div>
  );
}

function greeting(now: Date): string {
  const h = now.getHours();
  return h < 11 ? t.greetingMorning : h < 18 ? t.greetingDay : t.greetingEvening;
}

/** „Vineri, 18 septembrie 2026” — ziua, ca pe o foaie de calendar. */
function today(now: Date): string {
  const s = new Intl.DateTimeFormat("ro-RO", { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(now);
  return s.charAt(0).toUpperCase() + s.slice(1);
}

/**
 * Acasă, varianta A (aleasă de proprietar pe 18.09.2026, din cinci desenate): banda cu lucrul de
 * făcut, anul pe luni, deșeurile anului și termenele pe douăsprezece luni. Din machetă au ieșit,
 * după ce proprietarul le-a văzut pe localhost, „Dacă vine controlul azi” și „Ultimele predări”.
 * Totul din listele pe care Panoul le avea deja.
 */
export function DashboardPage() {
  const { user } = useAuth();
  const canAdd = user?.role !== "CLIENT_VIEWER";
  const { data: company } = useCurrentCompany();
  const d = useDashboardData();
  const screens = screensFor(company?.type);
  const firstScreen = screens[0];
  const generator = screens.includes("GENERATED");
  const addLabel =
    firstScreen === "IN"
      ? strings.panel.addInbound
      : screens.includes("IN")
        ? strings.panel.addOwnWaste
        : strings.panel.addWaste;
  const now = new Date();
  const points = (d.workPoints ?? []).filter((w) => w.active).length;

  return (
    <div>
      <p className="eyebrow">{today(now)}</p>
      <PageHeader
        className="mt-1.5"
        title={greeting(now)}
        description={
          company && (
            <>
              {company.name}
              {d.workPoints && <> · {countOf(points, "punct de lucru", "puncte de lucru")}</>}
            </>
          )
        }
        actions={
          canAdd && (
            <LinkButton to={`${SCREEN_PATH[firstScreen]}?nou=1`}>
              <Plus className="mr-2 h-4 w-4" />
              {addLabel}
            </LinkButton>
          )
        }
      />

      <NextActionBand actions={d.actions} loading={d.nextActionLoading} />

      {/* Cont nou: drumul până la primul document, pas cu pas. Dispare când e gata tot. */}
      <FirstSteps />

      {generator && (
        <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
          <YearMonthsCard d={d} />
          <WasteCodesCard d={d} />
        </div>
      )}

      <div className="mt-4">
        <DeadlineStrip d={d} />
      </div>
    </div>
  );
}
