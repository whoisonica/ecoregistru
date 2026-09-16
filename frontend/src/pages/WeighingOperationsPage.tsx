import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ArrowDownToLine, ArrowUpFromLine, Scale } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import {
  useDepotRetentions,
  useWeighingOperation,
  useWeighingOperations,
} from "@/hooks/useWeighingOperations";
import { useTableView } from "@/hooks/useTableView";
import { useHotkey } from "@/hooks/useHotkey";
import { canWrite } from "@/lib/roles";
import { registersFor } from "@/lib/movementScreens";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import type { WeighingOperation, WeighingOperationType } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { PageHeader } from "@/components/ui/page-header";
import { MonthInput, currentMonth } from "@/components/ui/month-input";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { Tooltip } from "@/components/ui/tooltip";
import { EmptyState } from "@/components/ui/empty-state";
import { WeighingOperationDialog } from "@/components/depot/WeighingOperationDialog";

const t = strings.weighing;

// Cântarul dă trei zecimale; banii, două. Aceleași formate ca pe Evidențe și pe Abonament.
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
const leiFormat = new Intl.NumberFormat("ro-RO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const formatKg = (kg: number) => kgFormat.format(kg);
const formatLei = (lei: number) => `${leiFormat.format(lei)} lei`;

/**
 * Ecranul „Cântar” (D1.15): operațiunile depozitului, pe direcții și pe lună, fiecare cu liniile ei.
 *
 * <p>Nu dublează „Intrări” și „Ieșiri”: acolo stau <b>rândurile de registru</b> — ce s-a întâmplat,
 * așa cum se raportează. Aici stă <b>bonul de cântar</b>, cu brutul, tara, ce s-a acceptat și cât s-a
 * plătit, adică lucrul pe care îl face omul de la poartă. O operațiune ajunge pe celălalt ecran abia
 * după ce e finalizată (D1.3).
 *
 * <p>Ecranul e al firmelor care țin registrul art. 48; celelalte n-au depozit, deci nici intrare în
 * meniu (`lib/navItems.ts`).
 */
export function WeighingOperationsPage() {
  const { user } = useAuth();
  const { data: company } = useCurrentCompany();
  const writes = canWrite(user?.role);

  const [type, setType] = useState<WeighingOperationType>("IN");
  const [month, setMonth] = useState(currentMonth());
  const [openId, setOpenId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  // `?op=…` vine de pe „Intrări”/„Ieșiri”, de pe un rând care e linie de cântar. Operațiunea lui
  // poate fi din altă lună decât cea aleasă aici, deci se cere separat.
  const [params, setParams] = useSearchParams();
  const linkedId = params.get("op");
  const linked = useWeighingOperation(linkedId);

  const [year, monthNumber] = useMemo(() => {
    const [y, m] = month.split("-");
    return [Number(y), Number(m)];
  }, [month]);

  const { data, isLoading, isError } = useWeighingOperations({ type, year, month: monthNumber });
  // Banda reținerilor o vede doar cine administrează firma și vede prețurile; altfel serverul dă 403
  // și rămâne ascunsă. `retry: false` în hook, ca un 403 să nu fie reîncercat de trei ori.
  const retentions = useDepotRetentions(year, monthNumber);

  const operations = useMemo(() => data ?? [], [data]);
  const view = useTableView(operations, {
    searchText: (o) =>
      [String(o.number), o.partnerName, o.naturalPersonName, o.workPointName]
        .filter(Boolean)
        .join(" "),
    comparators: {
      number: (a, b) => a.number - b.number,
      date: (a, b) => a.date.localeCompare(b.date),
    },
  });

  useHotkey("n", () => {
    if (writes) setCreating(true);
  });

  const inbound = type === "IN";

  // Firmele fără registru art. 48 n-au depozit, deci nici intrare în meniu. Linkul direct se
  // explică, nu se randează gol.
  if (company && !registersFor(company.type).includes("ART_48")) {
    return (
      <div>
        <PageHeader title={t.title} description={t.subtitle} />
        <EmptyState icon={Scale} title={t.noDepot} description={t.noDepotHint} />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          writes && (
            <Button hotkey="N" onClick={() => setCreating(true)}>
              {inbound ? (
                <ArrowDownToLine className="mr-2 h-4 w-4" />
              ) : (
                <ArrowUpFromLine className="mr-2 h-4 w-4" />
              )}
              {inbound ? t.newIn : t.newOut}
            </Button>
          )
        }
      />

      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div role="tablist" className="flex gap-1 border-b border-line">
          {([
            { id: "IN", label: t.tabIn },
            { id: "OUT", label: t.tabOut },
          ] as const).map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={type === item.id}
              onClick={() => setType(item.id)}
              className={
                "-mb-px border-b-2 px-3 py-2 text-sm font-medium " +
                (type === item.id
                  ? "border-brand-600 text-content-strong"
                  : "border-transparent text-content-muted hover:text-content")
              }
            >
              {item.label}
            </button>
          ))}
        </div>
        <MonthInput id="weighing-month" value={month} onChange={setMonth} />
      </div>

      {retentions.data && <RetentionsStrip report={retentions.data} />}

      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="number" sort={view.sort} onSort={view.toggleSort}>
                  {t.number}
                </SortableTH>
                <SortableTH sortKey="date" sort={view.sort} onSort={view.toggleSort}>
                  {t.date}
                </SortableTH>
                <TH>{inbound ? t.counterpartyIn : t.counterpartyOut}</TH>
                <TH>{t.articles}</TH>
                <TH className="text-right">{t.quantity}</TH>
                <TH className="text-right">{t.value}</TH>
                <TH>{t.status}</TH>
                <TH sticky="right" className="text-right">
                  {strings.common.actions}
                </TH>
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={8}
                  loading={isLoading}
                  icon={Scale}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((o) => (
                <OperationRow key={o.id} operation={o} onOpen={() => setOpenId(o.id)} />
              ))}
            </TBody>
          </Table>
          <TablePagination view={view} />
        </>
      )}

      {(creating || openId || linked.data) && (
        <WeighingOperationDialog
          open
          type={type}
          operation={
            creating ? null : operations.find((o) => o.id === openId) ?? linked.data ?? null
          }
          onClose={() => {
            setCreating(false);
            setOpenId(null);
            if (linkedId) {
              params.delete("op");
              setParams(params, { replace: true });
            }
          }}
        />
      )}
    </div>
  );
}

function OperationRow({ operation, onOpen }: { operation: WeighingOperation; onOpen: () => void }) {
  const kg = operation.lines.reduce((sum, l) => sum + (l.finalKg ?? 0), 0);
  const value = operation.lines.reduce((sum, l) => sum + (l.totalValue ?? 0), 0);
  const priced = operation.lines.some((l) => l.totalValue != null);
  const counterparty = operation.partnerName ?? operation.naturalPersonName;

  return (
    <TR>
      <TD className="font-mono tabular-nums text-content">{operation.number}</TD>
      <TD className="whitespace-nowrap font-mono text-xs">{formatDate(operation.date)}</TD>
      <TD className="font-medium text-content">
        {counterparty ?? <span className="text-content-subtle">—</span>}
      </TD>
      <TD>
        <span className="flex flex-wrap items-center gap-x-3 gap-y-1">
          {operation.lines.slice(0, 3).map((l) => (
            <span key={l.id} className="inline-flex items-center">
              <BinSwatch code={l.wasteCode} />
              <span>{l.articleName ?? l.wasteCode}</span>
            </span>
          ))}
          {operation.lines.length > 3 && (
            <span className="text-content-muted">+{operation.lines.length - 3}</span>
          )}
          {operation.lines.length === 0 && <span className="text-content-subtle">—</span>}
        </span>
      </TD>
      <TD className="text-right font-mono tabular-nums">{formatKg(kg)}</TD>
      <TD className="text-right font-mono tabular-nums">
        {priced ? formatLei(value) : <span className="text-content-subtle">—</span>}
      </TD>
      <TD>
        <StatusBadge operation={operation} />
      </TD>
      <TD sticky="right" className="text-right">
        <Button variant="ghost" size="sm" onClick={onOpen}>
          {t.open}
        </Button>
      </TD>
    </TR>
  );
}

function StatusBadge({ operation }: { operation: WeighingOperation }) {
  if (operation.status === "FINALIZED") return <Badge variant="success">{t.statusFinalized}</Badge>;
  if (operation.status === "IN_PROGRESS") return <Badge variant="warning">{t.statusInProgress}</Badge>;
  return (
    <Tooltip content={`${t.cancelledBecause} ${operation.cancelReason ?? ""}`}>
      <Badge variant="muted">{t.statusCancelled}</Badge>
    </Tooltip>
  );
}

/**
 * Banda reținerilor (D1.9, D1.10): cât s-a reținut luna asta și până când se declară. Cotele vin de
 * la server, ca o lună veche să se citească cu cota de atunci, nu cu cea de azi.
 */
function RetentionsStrip({
  report,
}: {
  report: NonNullable<ReturnType<typeof useDepotRetentions>["data"]>;
}) {
  const r = t.retentions;
  const nothing = report.afmContribution === 0 && report.incomeTax === 0;
  return (
    <section className="mb-4 border border-line bg-surface-sunken px-4 py-3">
      <div className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2">
        <h2 className="text-xs font-medium uppercase tracking-wide text-content-muted">
          {r.title}
          <Tooltip content={r.hint}>
            <span className="ml-2 cursor-help font-mono text-content-subtle">?</span>
          </Tooltip>
        </h2>
        {nothing ? (
          <p className="text-sm text-content-muted">{r.empty}</p>
        ) : (
          <dl className="flex flex-wrap items-baseline gap-x-8 gap-y-2">
            <Amount
              label={`${r.afm} · ${percent(report.afmRate)}`}
              value={report.afmContribution}
              base={report.afmBase}
            />
            <Amount
              label={`${r.tax} · ${percent(report.incomeTaxRate)}`}
              value={report.incomeTax}
              base={report.incomeTaxBase}
            />
            <div>
              <dt className="text-xs text-content-muted">{r.due}</dt>
              <dd className="font-mono tabular-nums text-content">{formatDate(report.dueDate)}</dd>
            </div>
          </dl>
        )}
      </div>
    </section>
  );
}

function Amount({ label, value, base }: { label: string; value: number; base: number }) {
  return (
    <div>
      <dt className="text-xs text-content-muted">{label}</dt>
      <dd className="font-mono tabular-nums text-content-strong">
        {formatLei(value)}
        <span className="ml-2 text-xs text-content-muted">
          {t.retentions.base} {formatLei(base)}
        </span>
      </dd>
    </div>
  );
}

/** 0.02 → „2%”; cotele vin ca fracție de la server. */
function percent(rate: number): string {
  return `${Number((rate * 100).toFixed(2))}%`;
}
