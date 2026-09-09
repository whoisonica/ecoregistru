import { useMemo, type ReactNode } from "react";
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
import type { DeadlineStatus, MonthlyEvidence } from "@/lib/types";
import { strings } from "@/lib/strings";
import { cn, countOf, formatDate, withCount } from "@/lib/utils";
import { daysLabel, daysUntil, documentFor } from "@/lib/deadlines";
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

/** Cantitățile vin din backend în kilograme; se scriu cu separatorul românesc. */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 });

/**
 * Cât de aproape trebuie să fie un termen ca să merite să fie **acțiunea următoare**.
 *
 * <p>Treizeci de zile, fiindcă atât ia strâns un dosar: regenerarea evidenței, verificarea liniilor
 * roșii, scoaterea documentului. Mai devreme de-atât, banda ar numi luni întregi un lucru pe care
 * nimeni nu-l face azi — iar o bandă care spune mereu același lucru devine tapet în trei zile, exact
 * ce s-a reparat pe 07.09 la bannerul galben permanent de pe Evidențe.
 */
const NEAR_DEADLINE_DAYS = 30;

function StatTile({
  icon: Icon,
  value,
  label,
  sub,
  tone,
  loading = false,
  children,
}: {
  icon: typeof Scale;
  value: string;
  label: string;
  sub: string;
  tone: "brand" | "amber" | "red";
  loading?: boolean;
  /** Detaliul care face cifra să însemne ceva — la stoc, chiar codurile care îl poartă. */
  children?: ReactNode;
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
      {!loading && children}
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

/** Ce anume e de făcut, o singură dată, cu drumul către el. `null` = nu s-a putut încă decide. */
type NextAction = {
  tone: "danger" | "warning" | "ok";
  title: string;
  hint: string;
  to: string;
  cta: string;
};

/**
 * Banda din capul panoului: **un** lucru de făcut, nu cinci de citit.
 *
 * <p>Cât timp datele n-au venit toate, banda **tace** — nu arată o afirmație pe jumătate de răspuns.
 * „Ești la zi" scris peste un `partners` încă neîncărcat ar fi exact felul de verde fals pentru care
 * s-a reparat citirea evidenței pe 07.09.
 */
function NextActionBand({ action, loading }: { action: NextAction | null; loading: boolean }) {
  if (loading || !action) {
    return <Skeleton className="mt-6 h-[4.5rem] w-full rounded-xl" />;
  }
  const tone = {
    danger: {
      box: "border-red-200 bg-red-50",
      icon: "text-red-600",
      title: "text-red-900",
      hint: "text-red-800",
      link: "text-red-700",
      Icon: AlertTriangle,
    },
    warning: {
      box: "border-amber-200 bg-amber-50",
      icon: "text-amber-600",
      title: "text-amber-900",
      hint: "text-amber-800",
      link: "text-amber-700",
      Icon: CalendarClock,
    },
    ok: {
      box: "border-emerald-200 bg-emerald-50",
      icon: "text-emerald-600",
      title: "text-emerald-900",
      hint: "text-emerald-800",
      link: "text-emerald-700",
      Icon: CheckCircle2,
    },
  }[action.tone];
  const { Icon } = tone;
  return (
    <div
      data-testid="next-action"
      className={cn(
        "mt-6 flex flex-col gap-3 rounded-xl border p-4 sm:flex-row sm:items-center",
        tone.box
      )}
    >
      <Icon className={cn("h-5 w-5 shrink-0", tone.icon)} aria-hidden />
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium uppercase tracking-wide text-content-subtle">
          {t.nextTitle}
        </p>
        <p className={cn("text-sm font-semibold", tone.title)}>{action.title}</p>
        <p className={cn("mt-0.5 text-xs", tone.hint)}>{action.hint}</p>
      </div>
      {/* `whitespace-nowrap`, ca la coloana de acțiuni din Termene: o etichetă de două cuvinte
          ruptă pe două rânduri crește banda și se citește greu (defectul din 07.09 și 08.09). */}
      <Link
        to={action.to}
        className={cn(
          "shrink-0 whitespace-nowrap text-sm font-medium hover:underline",
          tone.link
        )}
      >
        {action.cta} →
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

  /**
   * Kilogramele lunii — cifra pe care o caută cineva, spre deosebire de numărul de rânduri.
   *
   * <p>⚠️ **Fiecare mișcare își poartă unitatea.** Suma aduna `quantity` brut, deci o mișcare de
   * 1000 kg și una de 1 tonă ieșeau ca `1001` — aceeași cantitate numărată o dată corect și o dată
   * de o mie de ori mai mic. Motorul de evidență normalizează la kilograme de mult (`KG_PER_TON`
   * în `EvidenceCalculator`); panoul citea mișcările direct și rămăsese fără conversia aia.
   */
  const generatedThisMonth = useMemo(
    () =>
      (movements ?? []).reduce(
        (sum, m) => sum + (m.quantity ?? 0) * (m.unit === "TONS" ? 1000 : 1),
        0
      ),
    [movements]
  );
  /**
   * Stocul, **pe coduri**. Până pe 08.09.2026 cifra era suma închiderilor peste toate codurile —
   * hârtie plus ulei uzat plus menajer, adică o cantitate care nu există fizic nicăieri. Mai rău:
   * un stoc negativ pe un cod (ieşiri neacoperite, exact ce nu se poate depune) se scădea din
   * pozitivele celorlalte şi dispărea din ochi.
   *
   * <p>Se numără codurile care chiar au stoc şi se numesc primele trei. Stocul unei perechi
   * (punct de lucru, cod) e închiderea **ultimei ei luni calculate**, nu a ultimei luni din tot
   * setul: perechile pot avea lungimi diferite, iar un maxim luat peste tot ar sări perechea care
   * se termină mai devreme.
   *
   * <p>Ordinea: întâi negativele, apoi pozitivele, fiecare după mărime. Un stoc negativ e
   * interesant oricât de mic ar fi — dacă s-ar ordona doar după mărime, un −1 ar cădea sub prag
   * exact când e singurul lucru de văzut.
   */
  const stock = useMemo(() => {
    const lastPerPair = new Map<string, MonthlyEvidence>();
    for (const r of evidences ?? []) {
      const key = `${r.workPointId}|${r.wasteCodeId}`;
      const prev = lastPerPair.get(key);
      if (!prev || r.month > prev.month) lastPerPair.set(key, r);
    }
    const byCode = new Map<string, number>();
    for (const r of lastPerPair.values()) {
      byCode.set(r.wasteCode, (byCode.get(r.wasteCode) ?? 0) + r.closingStock);
    }
    const withStock = [...byCode.entries()]
      .map(([code, kg]) => ({ code, kg }))
      .filter((x) => x.kg !== 0)
      .sort((a, b) => {
        if (a.kg < 0 !== b.kg < 0) return a.kg < 0 ? -1 : 1;
        return Math.abs(b.kg) - Math.abs(a.kg);
      });
    return {
      count: withStock.length,
      negative: withStock.filter((x) => x.kg < 0).length,
      top: withStock.slice(0, 3),
      rest: Math.max(0, withStock.length - 3),
    };
  }, [evidences]);

  /** Câte mișcări s-au înregistrat luna asta — cifra care spune dacă evidența se ține la zi. */
  const movementCount = movements?.length ?? 0;

  const monthLabel = strings.months[month - 1];

  /**
   * Un singur lucru de făcut, ales după cât costă dacă rămâne nefăcut.
   *
   * <p>Nu inventează nicio cifră: fiecare ramură citește exact numărul pe care ecranul îl arată deja
   * mai jos. Ce adaugă e **ordinea** — ce se ia întâi — și drumul, care pe termene e chiar documentul
   * care stinge termenul, pe **anul raportat** (decizia 59).
   *
   * <p>Contribuțiile AFM cad pe `/termene`, nu pe un document: nu tipărim niciun formular pentru
   * ele, iar `documentFor` întoarce `null` tocmai ca să nu promitem unul.
   */
  const nextAction = useMemo<NextAction | null>(() => {
    // 1. Un termen depășit curge deja — nimic din ce e mai jos nu costă mai mult.
    const overdue = openDeadlines.filter((d) => d.status === "OVERDUE");
    if (overdue.length === 1) {
      const d = overdue[0];
      const doc = documentFor(d);
      return {
        tone: "danger",
        title: t.nextDeadline
          .replace("{label}", strings.enums.reportType[d.reportType])
          .replace("{days}", daysLabel(d) ?? ""),
        hint: t.nextOverdueHint,
        to: doc?.to ?? "/termene",
        cta: doc?.label ?? t.nextOverdueCta,
      };
    }
    if (overdue.length > 1) {
      return {
        tone: "danger",
        title: t.nextOverdue.replace("{count}", countOf(overdue.length, "termen", "termene")),
        hint: t.nextOverdueHint,
        to: "/termene",
        cta: t.nextOverdueCta,
      };
    }
    // 2. Ieșirea fără cod R/D blochează depunerea următoare — aceeași țintă ca blocajul roșu.
    if (blockers.missingCode > 0) {
      return {
        tone: "danger",
        title: t.nextMissingCode.replace("{count}", countOf(blockers.missingCode, "linie", "linii")),
        hint: t.nextMissingCodeHint,
        to: "/evidente?vedere=handovers&problema=cod-rd",
        cta: t.blockerFix,
      };
    }
    // 3. Un termen care se mai poate prinde, cu documentul care îl stinge.
    if (nextDeadline && daysUntil(nextDeadline.dueDate) <= NEAR_DEADLINE_DAYS) {
      const doc = documentFor(nextDeadline);
      return {
        tone: "warning",
        title: t.nextDeadline
          .replace("{label}", strings.enums.reportType[nextDeadline.reportType])
          .replace("{days}", daysLabel(nextDeadline) ?? ""),
        hint: t.nextDeadlineHint,
        to: doc?.to ?? "/termene",
        cta: doc?.label ?? t.nextDeadlineCta,
      };
    }
    // 4. O autorizație pe terminate se mai poate reînnoi; riscul e al clientului (decizia 41).
    if (expiringPartners.length > 0) {
      return {
        tone: "warning",
        title: t.nextExpiring.replace("{count}", countOf(expiringPartners.length, "partener", "parteneri")),
        hint: t.nextExpiringHint,
        to: "/parteneri",
        cta: t.nextExpiringCta,
      };
    }
    // 5. Cântarul e o așteptare legitimă, nu o greșeală (decizia 13) — deci ultimul.
    if (blockers.awaitingWeighing > 0) {
      return {
        tone: "warning",
        title: t.nextWeighing.replace("{count}", countOf(blockers.awaitingWeighing, "linie", "linii")),
        hint: t.nextWeighingHint,
        to: "/miscari",
        cta: t.nextWeighingCta,
      };
    }
    return { tone: "ok", title: t.nextNothing, hint: t.nextNothingHint, to: "/evidente", cta: t.viewAll };
  }, [openDeadlines, nextDeadline, blockers, expiringPartners]);

  /**
   * Banda tace până vin **toate** cele trei surse din care alege. Fără garda asta, un `partners`
   * întârziat ar scrie „Ești la zi" o clipă, peste o autorizație care expiră — o afirmație falsă,
   * din exact motivul pentru care o probă poate trece verde: premisa nu s-a întâmplat încă.
   */
  const nextActionLoading = loadingDeadlines || loadingEvidences || loadingPartners;

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

      {/* Un lucru de făcut, înaintea stării: „sunt în regulă?" are răspunsul mai jos, dar
          „ce fac acum?" n-avea niciunul — se citeau cinci locuri și se trăgea singur concluzia. */}
      <NextActionBand action={nextAction} loading={nextActionLoading} />

      {/* Starea de conformitate, înaintea oricărei cifre: e întrebarea pentru care clientul
          deschide aplicația, iar până acum răspunsul se afla derulând Evidențe. */}
      <Card className="mt-4">
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
                title={t.blockerMissingCode.replace("{count}", countOf(blockers.missingCode, "linie", "linii"))}
                hint={t.blockerMissingCodeHint}
                /* Ducea la vederea lunară, unde rândul e un agregat pe (punct de lucru, cod, lună)
                   și nu se poate deschide nicio mișcare — deci „Repară" promitea mai mult decât
                   ținea. Registrul de predări e nivelul la care un rând **este** o mișcare, iar
                   filtrul îl restrânge la ce blochează chiar depunerea. */
                to="/evidente?vedere=handovers&problema=cod-rd"
              />
            )}
            {blockers.awaitingWeighing > 0 && (
              <Blocker
                icon={Scale}
                tone="warning"
                title={t.blockerAwaitingWeighing.replace(
                  "{count}",
                  countOf(blockers.awaitingWeighing, "linie", "linii")
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
          sub={
            movementCount === 0
              ? t.statGeneratedSubNone
              : t.statGeneratedSub.replace("{count}", countOf(movementCount, "mișcare", "mișcări"))
          }
          tone="brand"
          loading={loadingMovements}
        />
        <StatTile
          icon={Scale}
          value={String(stock.count)}
          label={t.statStock}
          sub={
            stock.negative > 0
              ? withCount(t.statStockNegative, stock.negative, "cod", "coduri")
              : t.statStockSub
          }
          tone={stock.negative > 0 ? "red" : "brand"}
          loading={loadingEvidences}
        >
          {stock.top.length > 0 && (
            <ul className="mt-2 space-y-0.5 text-xs">
              {stock.top.map((row) => (
                <li key={row.code} className="flex items-center justify-between gap-2">
                  <span className="truncate text-content-muted">{row.code}</span>
                  <span
                    className={cn(
                      "shrink-0 tabular-nums",
                      row.kg < 0 ? "font-medium text-red-700" : "text-content"
                    )}
                  >
                    {kgFormat.format(row.kg)} kg
                  </span>
                </li>
              ))}
              {stock.rest > 0 && (
                <li className="text-content-subtle">
                  {withCount(t.statStockMore, stock.rest, "cod", "coduri")}
                </li>
              )}
            </ul>
          )}
        </StatTile>
        <StatTile
          icon={CalendarClock}
          value={String(openDeadlines.length)}
          label={t.statDeadlines}
          sub={
            overdueCount > 0
              ? withCount(t.statDeadlinesOverdue, overdueCount, "termen depășit", "termene depășite")
              : nextDeadline
                ? t.statDeadlinesNext
                    .replace("{label}", strings.enums.reportType[nextDeadline.reportType])
                    .replace("{days}", daysLabel(nextDeadline) ?? "")
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
                /* Aceeaşi formulare ca pe Termene, din aceeaşi funcţie: „azi" / „mâine" / „în N
                   zile" / „depăşit de N zile". Panoul avea un şir propriu care spunea numai zilele
                   rămase şi tăcea pe cele depăşite — două nume pentru acelaşi lucru, iar cel de aici
                   scria „1 zile". */
                const days = daysLabel(d);
                return (
                  <li key={d.id} className="flex items-center justify-between gap-3 py-2.5">
                    <div className="min-w-0 flex-1">
                      <div className="truncate text-sm font-medium text-content">
                        {strings.enums.reportType[d.reportType]}
                      </div>
                      {/* Data singură cere o socoteală în cap; numărul de zile e răspunsul. */}
                      <div className="truncate text-xs text-content-subtle">
                        {formatDate(d.dueDate)}
                        {days && <> · {days}</>}
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
