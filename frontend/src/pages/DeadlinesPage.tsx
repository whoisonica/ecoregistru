import { useMemo, useState } from "react";
import { RefreshCw, Check, ChevronRight, RotateCcw, CalendarClock } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import {
  useDeadlines,
  useRegenerateDeadlines,
  useCompleteDeadline,
  useReopenDeadline,
} from "@/hooks/useDeadlines";
import type { Deadline, DeadlineStatus } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { cn, formatDate } from "@/lib/utils";
import { useUrlNumber } from "@/hooks/useUrlState";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import type { BadgeProps } from "@/components/ui/badge";

const t = strings.deadlines;

/** Year options: current year down to five years back. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

const statusVariant: Record<DeadlineStatus, BadgeProps["variant"]> = {
  UPCOMING: "warning",
  DONE: "success",
  OVERDUE: "danger",
};

/**
 * Câte zile mai sunt, socotite pe zile calendaristice — aceeaşi socoteală ca pe Panou, care o avea
 * de mult, în timp ce tabelul lăsa clientul s-o facă în cap.
 *
 * <p>Se compară la miezul nopţii, nu la ora curentă: altfel un termen de mâine dimineaţă ar ieşi
 * „0 zile" după-amiaza — adevărat în ore, fals în felul în care se citeşte un calendar.
 */
function daysUntil(iso: string): number {
  const [y, m, d] = iso.split("-").map(Number);
  const target = new Date(y, m - 1, d);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

/** Zilele rămase, în cuvinte. Un termen finalizat nu le mai are: nu mai e nimic de aşteptat. */
function daysLabel(d: Deadline): string | null {
  if (d.status === "DONE") return null;
  const days = daysUntil(d.dueDate);
  if (days < 0) return t.daysOverdue.replace("{n}", String(-days));
  if (days === 0) return t.daysToday;
  if (days === 1) return t.daysTomorrow;
  return t.daysLeft.replace("{n}", String(days));
}

/**
 * Ecranul de pe care se scoate documentul care stinge termenul.
 *
 * <p>Amândouă raportările anuale acoperă **anul precedent** celui în care se depun — 15 martie
 * pentru evidenţa anului trecut (OUG 92/2021 art. 48 alin. (1)), 25 februarie pentru ambalajele
 * anului trecut (Ordinul 794/2012 art. 6, „pentru anul anterior") — deci linkul duce la anul
 * raportat, nu la anul termenului. A duce la anul termenului ar deschide un dosar gol chiar în ziua
 * depunerii.
 *
 * <p>Contribuţiile AFM n-au link, şi asta nu e o scăpare: sunt bani declaraţi în aplicaţia AFM, iar
 * aplicaţia noastră nu tipăreşte niciun formular pentru ele (vezi `docs/legislatie.md` §5.B). Un
 * link către un document care nu există ar promite mai mult decât ţinem — chiar defectul reparat pe
 * 07.09 la badge-ul roşu, pe dos.
 */
function documentFor(d: Deadline): { to: string; label: string } | null {
  const reported = Number(d.dueDate.slice(0, 4)) - 1;
  if (d.reportType === "SIM_ANNUAL") {
    return {
      to: `/evidente?an=${reported}`,
      label: t.documentEvidence.replace("{year}", String(reported)),
    };
  }
  if (d.reportType === "PACKAGING_ANNUAL") {
    return {
      to: `/ambalaje?an=${reported}`,
      label: t.documentPackaging.replace("{year}", String(reported)),
    };
  }
  return null;
}

export function DeadlinesPage() {
  const { user } = useAuth();
  const canManage =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const [year, setYear] = useUrlNumber("an", new Date().getFullYear());
  const { data: deadlines, isLoading, isError } = useDeadlines(year);
  const regenerateMut = useRegenerateDeadlines();
  const completeMut = useCompleteDeadline();
  const reopenMut = useReopenDeadline();
  const { notify } = useToast();

  const [completing, setCompleting] = useState<Deadline | null>(null);
  const [note, setNote] = useState("");

  const rows = useMemo(() => deadlines ?? [], [deadlines]);

  const view = useTableView(rows, {
    searchText: (d) =>
      [strings.enums.reportType[d.reportType], d.completionNote].filter(Boolean).join(" "),
    comparators: {
      reportType: (a, b) =>
        strings.enums.reportType[a.reportType].localeCompare(
          strings.enums.reportType[b.reportType],
          "ro"
        ),
      dueDate: (a, b) => a.dueDate.localeCompare(b.dueDate),
    },
    initialSort: { key: "dueDate", direction: "asc" },
  });

  function handleRegenerate() {
    regenerateMut.mutate(year, {
      onSuccess: (res) =>
        notify(
          t.generated
            .replace("{count}", String(res.generated))
            .replace("{year}", String(res.year)),
          "success"
        ),
      onError: (err) => notify(apiErrorMessage(err, t.generateError), "error"),
    });
  }

  function openComplete(d: Deadline) {
    setCompleting(d);
    setNote("");
  }

  function submitComplete() {
    if (!completing) return;
    completeMut.mutate(
      { id: completing.id, note: note.trim() || undefined },
      {
        onSuccess: () => {
          notify(t.completed, "success");
          setCompleting(null);
        },
        onError: (err) => notify(apiErrorMessage(err, t.actionError), "error"),
      }
    );
  }

  function handleReopen(d: Deadline) {
    reopenMut.mutate(d.id, {
      onSuccess: () => notify(t.reopened, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.actionError), "error"),
    });
  }

  return (
    <div>
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          canManage && (
            <Button onClick={handleRegenerate} disabled={regenerateMut.isPending}>
              <RefreshCw
                className={`mr-2 h-4 w-4 ${regenerateMut.isPending ? "animate-spin" : ""}`}
              />
              {regenerateMut.isPending ? t.generating : t.generate}
            </Button>
          )
        }
      />

      {/* Filters */}
      <div className="mt-6 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <div>
          <Label htmlFor="dl-year">{t.filterYear}</Label>
          <Select
            id="dl-year"
            value={String(year)}
            onChange={(ev) => setYear(Number(ev.target.value))}
            className="w-full sm:w-32"
          >
            {yearOptions().map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </Select>
        </div>
      </div>

      <section className="mt-4">
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isError && (
          <div>
            <TableToolbar view={view} placeholder={t.searchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="reportType" sort={view.sort} onSort={view.toggleSort}>
                    {t.colReportType}
                  </SortableTH>
                  <SortableTH sortKey="dueDate" sort={view.sort} onSort={view.toggleSort}>
                    {t.colDueDate}
                  </SortableTH>
                  <TH>{t.colStatus}</TH>
                  <TH>{t.colDocument}</TH>
                  <TH>{t.colNote}</TH>
                  {canManage && (
                    <TH sticky="right" className="whitespace-nowrap text-right">
                      {strings.common.actions}
                    </TH>
                  )}
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={canManage ? 6 : 5}
                    loading={isLoading}
                    icon={CalendarClock}
                    title={
                      view.emptiedBySearch
                        ? strings.common.noResults
                        : t.empty.replace("{year}", String(year))
                    }
                    description={canManage ? t.emptyHint.replace("{year}", String(year)) : undefined}
                    action={
                      canManage && (
                        <Button onClick={handleRegenerate} disabled={regenerateMut.isPending}>
                          <RefreshCw
                            className={`mr-2 h-4 w-4 ${regenerateMut.isPending ? "animate-spin" : ""}`}
                          />
                          {t.generate}
                        </Button>
                      )
                    }
                  />
                )}
                {view.visible.map((d) => {
                  const doc = documentFor(d);
                  const days = daysLabel(d);
                  return (
                  <TR key={d.id}>
                    <TD className="font-medium text-content">
                      {strings.enums.reportType[d.reportType]}
                    </TD>
                    <TD className="whitespace-nowrap">
                      {formatDate(d.dueDate)}
                      {days && (
                        <span
                          className={cn(
                            "block text-xs",
                            d.status === "OVERDUE" ? "text-red-600" : "text-content-subtle"
                          )}
                        >
                          {days}
                        </span>
                      )}
                    </TD>
                    <TD className="whitespace-nowrap">
                      <Badge variant={statusVariant[d.status]}>
                        {strings.enums.deadlineStatus[d.status]}
                      </Badge>
                    </TD>
                    <TD className="whitespace-nowrap">
                      {doc ? (
                        <Link
                          to={doc.to}
                          className="inline-flex items-center gap-1 text-sm text-brand hover:underline"
                        >
                          {doc.label}
                          <ChevronRight className="h-3.5 w-3.5" aria-hidden />
                        </Link>
                      ) : (
                        <span className="text-content-subtle">—</span>
                      )}
                    </TD>
                    <TD className="max-w-xs truncate text-content-muted">
                      {d.completionNote ?? "—"}
                    </TD>
                    {canManage && (
                      <TD sticky="right" className="whitespace-nowrap text-right">
                        {d.status === "DONE" ? (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleReopen(d)}
                            disabled={reopenMut.isPending}
                          >
                            <RotateCcw className="mr-1 h-3.5 w-3.5" />
                            {t.reopen}
                          </Button>
                        ) : (
                          <Button variant="outline" size="sm" onClick={() => openComplete(d)}>
                            <Check className="mr-1 h-3.5 w-3.5" />
                            {t.markDone}
                          </Button>
                        )}
                      </TD>
                    )}
                  </TR>
                  );
                })}
              </TBody>
            </Table>
            <TablePagination view={view} />
          </div>
        )}
      </section>

      <Dialog
        open={completing !== null}
        onClose={() => setCompleting(null)}
        title={t.completeTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setCompleting(null)}>
              {strings.common.cancel}
            </Button>
            <Button onClick={submitComplete} disabled={completeMut.isPending}>
              {completeMut.isPending ? strings.common.saving : t.markDone}
            </Button>
          </>
        }
      >
        {completing && (
          <div className="space-y-3">
            <div className="text-sm text-content-strong">
              <span className="font-medium text-content">
                {strings.enums.reportType[completing.reportType]}
              </span>
              {" — "}
              {formatDate(completing.dueDate)}
            </div>
            <div>
              <Label htmlFor="dl-note">{t.noteLabel}</Label>
              <Textarea
                id="dl-note"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder={t.notePlaceholder}
                maxLength={500}
              />
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
}
