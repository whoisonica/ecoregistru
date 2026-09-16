import { useMemo, useState, type ReactNode } from "react";
import {
  RefreshCw,
  Check,
  ChevronRight,
  RotateCcw,
  CalendarClock,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useCanWrite } from "@/hooks/useBillingAccess";
import {
  useDeadlines,
  useUpcomingDeadlines,
  useRegenerateDeadlines,
  useCompleteDeadline,
  useReopenDeadline,
} from "@/hooks/useDeadlines";
import type { Deadline, DeadlineStatus } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { cn, formatDate, withCount } from "@/lib/utils";
import { daysLabel, documentFor } from "@/lib/deadlines";
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

/**
 * Anul viitor, apoi anul curent și cinci ani înapoi. Anul viitor e acolo fiindcă următorul termen al
 * unui fel stă des în el (15 martie pentru datele de anul ăsta).
 */
function yearOptions(): number[] {
  const next = new Date().getFullYear() + 1;
  return Array.from({ length: 7 }, (_, i) => next - i);
}

const statusVariant: Record<DeadlineStatus, BadgeProps["variant"]> = {
  UPCOMING: "warning",
  DONE: "success",
  OVERDUE: "danger",
};

export function DeadlinesPage() {
  const canManage = useCanWrite();

  // „De făcut” nu are an: termenele nebifate, câte unul pe fiecare fel, stau des în anul următor
  // (15 martie pentru datele de acum). Anul se alege doar pentru istoricul celor bifate.
  const upcoming = useUpcomingDeadlines();
  const [year, setYear] = useUrlNumber("an", new Date().getFullYear());
  const history = useDeadlines(year);
  const regenerateMut = useRegenerateDeadlines();
  const completeMut = useCompleteDeadline();
  const reopenMut = useReopenDeadline();
  const { notify } = useToast();

  const [completing, setCompleting] = useState<Deadline | null>(null);
  const [note, setNote] = useState("");

  const todo = useMemo(
    () => (upcoming.data ?? []).filter((d) => d.status !== "DONE"),
    [upcoming.data],
  );
  const done = useMemo(
    () => (history.data ?? []).filter((d) => d.status === "DONE"),
    [history.data],
  );

  function handleRegenerate() {
    regenerateMut.mutate(undefined, {
      onSuccess: (res) =>
        notify(
          // Zero termene noi nu e o eroare — calendarul era deja complet —, dar „S-au generat 0 de
          // termene noi" se citește ca una.
          res.generated === 0
            ? t.generatedNone
            : withCount(
                t.generated,
                res.generated,
                "termen nou",
                "termene noi",
              ),
          "success",
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
      },
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
            <Button
              onClick={handleRegenerate}
              disabled={regenerateMut.isPending}
            >
              <RefreshCw
                className={`mr-2 h-4 w-4 ${regenerateMut.isPending ? "animate-spin" : ""}`}
              />
              {regenerateMut.isPending ? t.generating : t.generate}
            </Button>
          )
        }
      />

      <section className="mt-6" data-testid="deadlines-todo">
        <h2 className="text-base font-semibold text-content">{t.todoTitle}</h2>
        <DeadlinesTable
          rows={todo}
          loading={upcoming.isLoading}
          error={upcoming.isError}
          emptyTitle={t.todoEmpty}
          emptyHint={canManage ? t.emptyHint : undefined}
          emptyAction={
            canManage && (
              <Button
                onClick={handleRegenerate}
                disabled={regenerateMut.isPending}
              >
                <RefreshCw
                  className={`mr-2 h-4 w-4 ${regenerateMut.isPending ? "animate-spin" : ""}`}
                />
                {t.generate}
              </Button>
            )
          }
          canManage={canManage}
          onComplete={openComplete}
          onReopen={handleReopen}
          reopenPending={reopenMut.isPending}
        />
      </section>

      <section className="mt-10" data-testid="deadlines-done">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 className="text-base font-semibold text-content">
            {t.doneTitle}
          </h2>
          <div>
            <Label htmlFor="dl-year">{t.filterYear}</Label>
            <Select
              id="dl-year"
              value={String(year)}
              onChange={(ev) => setYear(Number(ev.target.value))}
              className="w-32"
            >
              {yearOptions().map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </Select>
          </div>
        </div>
        <DeadlinesTable
          rows={done}
          loading={history.isLoading}
          error={history.isError}
          emptyTitle={t.doneEmpty.replace("{year}", String(year))}
          canManage={canManage}
          onComplete={openComplete}
          onReopen={handleReopen}
          reopenPending={reopenMut.isPending}
        />
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

/** Un tabel de termene cu căutarea și sortarea lui; pagina are două: „De făcut” și „Bifate”. */
function DeadlinesTable({
  rows,
  loading,
  error,
  emptyTitle,
  emptyHint,
  emptyAction,
  canManage,
  onComplete,
  onReopen,
  reopenPending,
}: {
  rows: Deadline[];
  loading: boolean;
  error: boolean;
  emptyTitle: string;
  emptyHint?: string;
  emptyAction?: ReactNode;
  canManage: boolean;
  onComplete: (d: Deadline) => void;
  onReopen: (d: Deadline) => void;
  reopenPending: boolean;
}) {
  const view = useTableView(rows, {
    searchText: (d) =>
      [strings.enums.reportType[d.reportType], d.completionNote]
        .filter(Boolean)
        .join(" "),
    comparators: {
      reportType: (a, b) =>
        strings.enums.reportType[a.reportType].localeCompare(
          strings.enums.reportType[b.reportType],
          "ro",
        ),
      dueDate: (a, b) => a.dueDate.localeCompare(b.dueDate),
    },
    initialSort: { key: "dueDate", direction: "asc" },
  });

  if (error) return <p className="mt-3 text-sm text-red-600">{t.loadError}</p>;

  return (
    <div className="mt-3">
      <TableToolbar view={view} placeholder={t.searchPlaceholder} />
      <Table stickyHeader>
        <THead sticky>
          <TR>
            <SortableTH
              sortKey="reportType"
              sort={view.sort}
              onSort={view.toggleSort}
            >
              {t.colReportType}
            </SortableTH>
            <SortableTH
              sortKey="dueDate"
              sort={view.sort}
              onSort={view.toggleSort}
            >
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
          {(loading || view.visible.length === 0) && (
            <TableFallbackRow
              columns={canManage ? 6 : 5}
              loading={loading}
              icon={CalendarClock}
              title={
                view.emptiedBySearch ? strings.common.noResults : emptyTitle
              }
              description={view.emptiedBySearch ? undefined : emptyHint}
              action={view.emptiedBySearch ? undefined : emptyAction}
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
                        d.status === "OVERDUE"
                          ? "text-red-600"
                          : "text-content-subtle",
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
                        onClick={() => onReopen(d)}
                        disabled={reopenPending}
                      >
                        <RotateCcw className="mr-1 h-3.5 w-3.5" />
                        {t.reopen}
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => onComplete(d)}
                      >
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
  );
}
