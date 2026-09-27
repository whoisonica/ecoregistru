import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Radio } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useSiatdReceptions, useUnconfirmSiatd } from "@/hooks/useSiatd";
import { useTableView } from "@/hooks/useTableView";
import { apiErrorMessage } from "@/lib/api";
import { formatDate, todayIso } from "@/lib/dates";
import { formatKg } from "@/lib/units";
import { canManage } from "@/lib/roles";
import { formatDue } from "@/lib/siatdDue";
import { strings } from "@/lib/strings";
import type { SiatdReceptionRow, SiatdState } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { TablePagination } from "@/components/ui/table-toolbar";
import { useToast } from "@/components/ui/toast";
import { SiatdConfirmDialog } from "./SiatdConfirmDialog";

const t = strings.siatd;
const STATES: SiatdState[] = ["PENDING", "CONFIRMED", "MISSED"];

/**
 * F6a — Cântar → SIATD: recepțiile cu termen de confirmare, pe trei subtaburi. Zece pe pagină, ca celelalte liste, ca
 * ecranul să încapă la 1440×900. Cine aprobă le bifează — una cu codul SIATD, sau mai multe deodată, fără cod.
 */
export function SiatdTab({ onOpenOperation }: { onOpenOperation: (id: string) => void }) {
  const { user } = useAuth();
  const { data: company } = useCurrentCompany();
  const approver = canManage(user?.role);
  const [state, setState] = useState<SiatdState>("PENDING");
  const anyModule = Object.keys(company?.siatdEnrolledFrom ?? {}).length > 0;

  if (company && !anyModule) {
    return (
      <EmptyState
        icon={Radio}
        title={t.noModules}
        description={t.noModulesHint}
        action={
          <Link to="/setari/siatd" className="text-sm font-medium text-brand-700 underline hover:no-underline">
            {t.toSettings}
          </Link>
        }
      />
    );
  }

  return (
    <div>
      <div role="tablist" aria-label={t.tab} className="mb-3 flex gap-1 border-b border-line">
        {STATES.map((s) => (
          <button
            key={s}
            type="button"
            role="tab"
            aria-selected={state === s}
            onClick={() => setState(s)}
            className={
              "-mb-px border-b-2 px-3 py-2 text-sm font-medium " +
              (state === s ? "border-brand-600 text-content-strong" : "border-transparent text-content-muted hover:text-content")
            }
          >
            {t.subtabs[s]}
          </button>
        ))}
      </div>
      {/* Un subtab nou pornește fără bife și de la prima pagină. */}
      <SiatdTable key={state} state={state} approver={approver} onOpenOperation={onOpenOperation} />
    </div>
  );
}

function SiatdTable({
  state,
  approver,
  onOpenOperation,
}: {
  state: SiatdState;
  approver: boolean;
  onOpenOperation: (id: string) => void;
}) {
  const { data, isLoading, isError } = useSiatdReceptions(state);
  const rows = useMemo(() => data ?? [], [data]);
  const view = useTableView(rows, { pageSize: 10 });
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [confirming, setConfirming] = useState<string[] | null>(null);
  const unconfirmMut = useUnconfirmSiatd();
  const { notify } = useToast();
  const today = todayIso();
  const selectable = approver && state !== "CONFIRMED";
  const confirmed = state === "CONFIRMED";
  const columns = 7 + (selectable ? 1 : 0) + (approver ? 1 : 0);

  function toggle(id: string, on: boolean) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (on) next.add(id);
      else next.delete(id);
      return next;
    });
  }

  const pageIds = view.visible.map((r) => r.operationId);
  const allOnPage = pageIds.length > 0 && pageIds.every((id) => selected.has(id));

  async function unconfirm(id: string) {
    try {
      await unconfirmMut.mutateAsync(id);
      notify(t.unconfirmed, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.error), "error");
    }
  }

  if (isError) return <p className="text-sm text-state-bad-text">{t.loadError}</p>;

  return (
    <>
      {selectable && selected.size > 0 && (
        <div className="mb-2 flex justify-end">
          <Button onClick={() => setConfirming([...selected])}>
            {t.confirmSelected.replace("{count}", String(selected.size))}
          </Button>
        </div>
      )}
      <Table>
        <THead>
          <TR>
            {selectable && (
              <TH className="w-8">
                <input
                  type="checkbox"
                  aria-label={t.selectAll}
                  className="h-4 w-4 rounded border-line-strong"
                  checked={allOnPage}
                  onChange={(e) => pageIds.forEach((id) => toggle(id, e.target.checked))}
                />
              </TH>
            )}
            <TH>{t.columns.date}</TH>
            <TH>{t.columns.number}</TH>
            <TH>{t.columns.workPoint}</TH>
            <TH>{t.columns.partner}</TH>
            <TH>{t.columns.modules}</TH>
            <TH className="text-right">{t.columns.kg}</TH>
            <TH>{confirmed ? t.columns.confirmed : t.columns.due}</TH>
            {approver && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
          </TR>
        </THead>
        <TBody>
          {(isLoading || view.visible.length === 0) && (
            <TableFallbackRow columns={columns} loading={isLoading} icon={Radio} title={t.empty[state]} />
          )}
          {view.visible.map((r) => (
            <Row
              key={r.operationId}
              row={r}
              state={state}
              today={today}
              selectable={selectable}
              approver={approver}
              checked={selected.has(r.operationId)}
              onToggle={(on) => toggle(r.operationId, on)}
              onOpen={() => onOpenOperation(r.operationId)}
              onConfirm={() => setConfirming([r.operationId])}
              onUnconfirm={() => unconfirm(r.operationId)}
              busy={unconfirmMut.isPending}
            />
          ))}
        </TBody>
      </Table>
      <TablePagination view={view} />
      {confirming && (
        <SiatdConfirmDialog
          operationIds={confirming}
          onClose={() => setConfirming(null)}
          onDone={() => setSelected(new Set())}
        />
      )}
    </>
  );
}

function Row({
  row,
  state,
  today,
  selectable,
  approver,
  checked,
  onToggle,
  onOpen,
  onConfirm,
  onUnconfirm,
  busy,
}: {
  row: SiatdReceptionRow;
  state: SiatdState;
  today: string;
  selectable: boolean;
  approver: boolean;
  checked: boolean;
  onToggle: (on: boolean) => void;
  onOpen: () => void;
  onConfirm: () => void;
  onUnconfirm: () => void;
  busy: boolean;
}) {
  const due = formatDue(row.due, today);
  const urgent = state === "MISSED" || row.due <= today;
  return (
    <TR data-testid="siatd-row">
      {selectable && (
        <TD>
          <input
            type="checkbox"
            aria-label={t.selectRow.replace("{number}", String(row.number))}
            className="h-4 w-4 rounded border-line-strong"
            checked={checked}
            onChange={(e) => onToggle(e.target.checked)}
          />
        </TD>
      )}
      <TD className="whitespace-nowrap font-mono tabular-nums">{formatDate(row.date)}</TD>
      <TD>
        <button type="button" onClick={onOpen} className="font-mono tabular-nums text-brand-700 underline hover:no-underline">
          {row.number}
        </button>
      </TD>
      <TD>{row.workPointName}</TD>
      <TD>{row.partnerName ?? "—"}</TD>
      <TD className="whitespace-nowrap">{row.modules.map((m) => t.modules[m]).join(", ")}</TD>
      <TD className="whitespace-nowrap text-right font-mono tabular-nums">{formatKg(row.netKg)}</TD>
      <TD className="whitespace-nowrap">
        {state === "CONFIRMED" ? (
          <span>
            <span className="font-mono tabular-nums">{formatDate(row.confirmedAt)}</span>
            {row.code && <span className="ml-2 font-mono text-content-muted">{row.code}</span>}
          </span>
        ) : (
          <Badge variant={state === "MISSED" ? "danger" : urgent ? "warning" : "default"}>{due}</Badge>
        )}
      </TD>
      {approver && (
        <TD sticky="right" className="text-right">
          {state === "CONFIRMED" ? (
            <Button variant="ghost" size="sm" onClick={onUnconfirm} disabled={busy}>
              {t.unconfirm}
            </Button>
          ) : (
            <Button variant="outline" size="sm" onClick={onConfirm}>
              {t.confirm}
            </Button>
          )}
        </TD>
      )}
    </TR>
  );
}
