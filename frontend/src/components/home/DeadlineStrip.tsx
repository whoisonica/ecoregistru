import { Link } from "react-router-dom";
import { CalendarPlus, ChevronRight } from "lucide-react";
import type { DashboardData } from "@/hooks/useDashboardData";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { daysLabel, daysUntil } from "@/lib/deadlines";
import { saveBlob } from "@/lib/download";
import { deadlinesByMonth, deadlinesIcs } from "@/lib/home";
import { NEAR_DEADLINE_DAYS } from "@/lib/readiness";
import { strings } from "@/lib/strings";
import { cn, formatDate, withCount } from "@/lib/utils";
import type { Deadline } from "@/lib/types";

const t = strings.dashboard;

/** Propoziția de sub titlu: aceeași ca pe dala de termene de dinainte (probele 9, 10 și 11 o citesc). */
function summary(d: DashboardData): string {
  if (d.overdueCount > 0)
    return withCount(t.statDeadlinesOverdue, d.overdueCount, "termen depășit", "termene depășite");
  if (d.nextDeadline)
    return t.statDeadlinesNext
      .replace("{label}", strings.enums.reportType[d.nextDeadline.reportType])
      .replace("{days}", daysLabel(d.nextDeadline) ?? "");
  return t.statDeadlinesNone;
}

function tone(dl: Deadline): string {
  if (dl.status === "OVERDUE") return "bg-state-bad";
  if (daysUntil(dl.dueDate) <= NEAR_DEADLINE_DAYS) return "bg-state-warn";
  return "border-2 border-content";
}

/**
 * Termenele pe douăsprezece luni, de la luna curentă, și „Adaugă în calendar”: fișierul `.ics` se
 * face în browser, din aceeași listă, fără endpoint. Coloane pe lună, nu o axă cu puncte: o firmă
 * cu AFM lunar are douăsprezece termene, iar etichetele puse pe o axă s-ar fi călcat una pe alta.
 */
export function DeadlineStrip({ d }: { d: DashboardData }) {
  const now = new Date();
  const cols = deadlinesByMonth(d.openDeadlines, now);

  function downloadIcs() {
    const ics = deadlinesIcs(d.openDeadlines, (dl) => strings.enums.reportType[dl.reportType]);
    saveBlob(new Blob([ics], { type: "text/calendar;charset=utf-8" }), t.timelineIcsFile);
  }

  return (
    <Card data-testid="home-deadlines" className="relative">
      <CardHeader
        title={t.timelineTitle}
        action={
          <div className="flex items-center gap-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={downloadIcs}
              disabled={d.loadingDeadlines || d.failedDeadlines || d.openDeadlines.length === 0}
              aria-describedby="ics-hint"
            >
              <CalendarPlus className="mr-1.5 h-4 w-4" aria-hidden />
              {t.timelineIcs}
            </Button>
            <Link to="/termene" className="hidden items-center gap-1 text-sm font-medium text-brand-700 hover:underline sm:flex">
              {t.viewAll}
              <ChevronRight className="h-4 w-4" aria-hidden />
            </Link>
          </div>
        }
        className="border-b-2 border-content pb-2.5"
      />
      <span id="ics-hint" className="sr-only">
        {t.timelineIcsHint}
      </span>
      {d.loadingDeadlines ? (
        <Skeleton className="mt-4 h-20 w-full" />
      ) : (
        <>
          <p data-testid="stat-deadlines" className="mt-3 text-sm text-content-muted">
            {d.failedDeadlines ? t.statLoadError : summary(d)}
          </p>
          {!d.failedDeadlines && (
            // Pe telefon lunile fără termen se ascund (în afară de cea curentă): douăsprezece celule
            // cu „—” ar fi fost o jumătate de ecran de nimic.
            <ol className="mt-3 grid grid-cols-3 gap-x-3 gap-y-4 sm:grid-cols-6 xl:grid-cols-12">
              {cols.map((list, i) => {
                const date = new Date(now.getFullYear(), now.getMonth() + i, 1);
                return (
                  <li key={i} className={cn("min-w-0", list.length === 0 && i > 0 && "hidden sm:block")}>
                    <div
                      className={cn(
                        "eyebrow border-b border-line pb-1",
                        i === 0 && "font-semibold text-content"
                      )}
                    >
                      {strings.months[date.getMonth()].slice(0, 3)}
                      {date.getMonth() === 0 && ` ${date.getFullYear()}`}
                    </div>
                    {list.length === 0 ? (
                      <div className="mt-1.5 font-mono text-xs text-content-subtle">—</div>
                    ) : (
                      <ul className="mt-1.5 space-y-1.5">
                        {list.map((dl) => (
                          <li key={dl.id ?? dl.dueDate + dl.reportType} className="flex items-start gap-1.5 text-xs">
                            <span className={cn("mt-[3px] h-2 w-2 shrink-0 rounded-sm", tone(dl))} aria-hidden />
                            <span className="min-w-0 text-content">
                              <span className="font-mono">
                                {dl.status === "OVERDUE" ? formatDate(dl.dueDate).slice(0, 5) : dl.dueDate.slice(8, 10)}
                              </span>{" "}
                              {t.reportShort[dl.reportType]}
                            </span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </li>
                );
              })}
            </ol>
          )}
        </>
      )}
    </Card>
  );
}
