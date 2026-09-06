import { useMemo, useState } from "react";
import { RefreshCw, Check, RotateCcw, CalendarClock } from "lucide-react";
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
import { useUrlNumber } from "@/hooks/useUrlState";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
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

function formatDate(iso: string): string {
  const [y, m, d] = iso.split("-");
  return `${d}.${m}.${y}`;
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
            <Table>
              <THead>
                <TR>
                  <TH>{t.colReportType}</TH>
                  <TH>{t.colDueDate}</TH>
                  <TH>{t.colStatus}</TH>
                  <TH>{t.colNote}</TH>
                  {canManage && <TH className="text-right">{strings.common.actions}</TH>}
                </TR>
              </THead>
              <TBody>
                {(isLoading || rows.length === 0) && (
                  <TableFallbackRow
                    columns={canManage ? 5 : 4}
                    loading={isLoading}
                    icon={CalendarClock}
                    title={t.empty.replace("{year}", String(year))}
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
                {rows.map((d) => (
                  <TR key={d.id}>
                    <TD className="font-medium text-content">
                      {strings.enums.reportType[d.reportType]}
                    </TD>
                    <TD className="whitespace-nowrap">{formatDate(d.dueDate)}</TD>
                    <TD>
                      <Badge variant={statusVariant[d.status]}>
                        {strings.enums.deadlineStatus[d.status]}
                      </Badge>
                    </TD>
                    <TD className="max-w-xs truncate text-content-muted">
                      {d.completionNote ?? "—"}
                    </TD>
                    {canManage && (
                      <TD className="text-right">
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
                ))}
              </TBody>
            </Table>
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
