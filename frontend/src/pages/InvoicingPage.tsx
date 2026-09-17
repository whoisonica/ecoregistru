import { useEffect, useState, type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Receipt } from "lucide-react";
import {
  useCheckInvoicePayment,
  useInvoicePage,
  useDiscardInvoice,
  useLastBillingRun,
  useRunBilling,
} from "@/hooks/useSubscriptions";
import type {
  BillingInvoiceRow,
  BillingOwnerRef,
  InvoiceFilter,
  InvoicePage,
  LastBillingRun,
  SubscriptionOwner,
} from "@/lib/types";
import { useUrlState } from "@/hooks/useUrlState";
import { Select } from "@/components/ui/select";
import { Pagination } from "@/components/ui/table";
import { TableSearch } from "@/components/ui/table-toolbar";
import { api, apiErrorMessage } from "@/lib/api";
import { countOf } from "@/lib/count";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { PageHeader } from "@/components/ui/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PillGroup } from "@/components/ui/pill-group";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { checkedWhen, SubscriptionDialog } from "@/components/SubscriptionDialog";

const t = strings.invoicing;

const FILTERS: InvoiceFilter[] = ["ACTION", "FAILED", "OVERDUE", "UNPAID", "PAID", "ALL"];
const PAGE_SIZE = 50;

type Variant = "muted" | "warning" | "success" | "danger";

function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { maximumFractionDigits: 2 })} lei`;
}

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

const isFailed = (i: BillingInvoiceRow) => i.status === "DRAFT" && Boolean(i.lastError);
const isOverdue = (i: BillingInvoiceRow) => i.status === "ISSUED" && i.dueDate != null && i.dueDate < todayIso();

function stateOf(i: BillingInvoiceRow): [Variant, string] {
  if (i.status === "PAID") return ["success", t.statePaid];
  if (isOverdue(i)) return ["danger", t.stateOverdue];
  if (i.status === "ISSUED") return ["warning", t.stateIssued];
  return isFailed(i) ? ["danger", t.stateFailed] : ["muted", t.stateDraft];
}

/** Ultimele 12 luni, cea curentă prima: `2026-09` → „septembrie 2026”. */
function recentMonths() {
  const now = new Date();
  return Array.from({ length: 12 }, (_, k) => {
    const d = new Date(now.getFullYear(), now.getMonth() - k, 1);
    return {
      value: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`,
      label: d.toLocaleString("ro-RO", { month: "long", year: "numeric" }),
    };
  });
}

/** Căutarea pleacă la server după ce omul se oprește din scris, nu la fiecare tastă. */
function useDebounced(value: string, ms = 300) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(id);
  }, [value, ms]);
  return debounced;
}

/**
 * F-A (todo-clienti-abonamente.md, 17.09.2026) — facturarea tuturor clienților, pe un ecran al ei. Butonul de
 * rulare stătea în dialogul unei singure firme, dar rula pentru toate, iar rezultatul era un toast care dispărea:
 * „2 căzute” de la 06:30 nu le vedea nimeni. Aici ultima rulare rămâne, cu ce e de făcut pe fiecare.
 *
 * <p>F-B2: la 2000 de facturi, tabelul se tăia în browser după ce le primea pe toate. Acum serverul dă o pagină de
 * 50, filtrată, iar ecranul pornește pe „De rezolvat”: ce cere o mână, nu istoricul.
 */
export function InvoicingPage() {
  const lastRun = useLastBillingRun();
  const [filterParam, setFilterParam] = useUrlState("filtru", "ACTION");
  const filter = (FILTERS.includes(filterParam as InvoiceFilter) ? filterParam : "ACTION") as InvoiceFilter;
  const [month, setMonth] = useUrlState("luna");
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const [page, setPage] = useState(0);
  useEffect(() => setPage(0), [filter, month, q]);

  const invoices = useInvoicePage({ filter, month, q, page, size: PAGE_SIZE });
  const runMut = useRunBilling();
  const checkMut = useCheckInvoicePayment();
  const discardMut = useDiscardInvoice();
  const [confirm, confirmDialog] = useConfirm();
  const { notify } = useToast();
  const [owner, setOwner] = useState<SubscriptionOwner | null>(null);
  const months = recentMonths();

  const data: InvoicePage | undefined = invoices.data;
  const rows = data?.content ?? [];

  async function run() {
    try {
      const result = await runMut.mutateAsync();
      if (!result.configured) notify(strings.subscriptions.runBillingOff, "error");
    } catch (err) {
      notify(apiErrorMessage(err, strings.subscriptions.runBillingError), "error");
    }
  }

  async function check(invoice: BillingInvoiceRow) {
    const number = `${invoice.fgoSerie} ${invoice.fgoNumar}`;
    try {
      await checkMut.mutateAsync(invoice.id);
      // Factura poate ieși din filtrul curent (o restantă plătită nu mai e „de rezolvat”): se caută după număr.
      const found = await api.get<InvoicePage>("/api/v1/subscriptions/invoices", {
        params: { filter: "ALL", q: number, size: 20 },
      });
      const fresh = found.data.content.find((i) => i.id === invoice.id);
      if (fresh?.status === "PAID") notify(t.checkedPaid.replace("{number}", number), "success");
      else notify(t.checkedUnpaid.replace("{number}", number), "info");
    } catch (err) {
      notify(apiErrorMessage(err, t.checkError), "error");
    }
  }

  function stop(invoiceId: string, client: string) {
    confirm({
      title: t.stopTitle.replace("{client}", client),
      message: t.stopMessage,
      confirmLabel: t.stop,
      tone: "danger",
      onConfirm: async () => {
        try {
          await discardMut.mutateAsync(invoiceId);
          notify(t.stopped.replace("{client}", client), "success");
        } catch (err) {
          notify(apiErrorMessage(err, t.stopError), "error");
        }
      },
    });
  }

  const navigate = useNavigate();
  // F-D: o firmă își are abonamentul pe pagina ei; dialogul a rămas doar pentru cabinete.
  const open = (ref: BillingOwnerRef, name: string) =>
    ref.kind === "company" ? navigate(`/clienti/${ref.id}?tab=abonament`) : setOwner({ ...ref, name });

  return (
    <div>
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          <Button onClick={run} disabled={runMut.isPending}>
            {runMut.isPending ? t.running : t.run}
          </Button>
        }
      />
      {confirmDialog}
      {owner && <SubscriptionDialog owner={owner} onClose={() => setOwner(null)} />}

      <LastRunPanel
        run={lastRun.data}
        loading={lastRun.isLoading}
        error={lastRun.isError}
        onOpen={open}
        onStop={stop}
        busy={discardMut.isPending}
      />

      <section className="mt-8">
        <h2 id="invoice-filter" className="mb-3 text-lg font-semibold text-content">
          {t.invoices}
        </h2>
        <PillGroup
          name="invoice-filter"
          aria-labelledby="invoice-filter"
          options={FILTERS.map((f) => ({
            value: f,
            label: (
              <>
                {t.filters[f]} <span className="font-mono text-xs opacity-70">{data ? data.counts[f] : "?"}</span>
              </>
            ),
          }))}
          selected={[filter]}
          onToggle={(f) => setFilterParam(f)}
          className="mb-3"
        />
        <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center">
          <TableSearch value={search} onChange={setSearch} placeholder={t.searchPlaceholder} className="flex-1" />
          <Select
            aria-label={t.month}
            value={month}
            onChange={(e) => setMonth(e.target.value)}
            className="sm:w-56"
          >
            <option value="">{t.allMonths}</option>
            {months.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </Select>
        </div>
        {invoices.isError && <p className="mb-3 text-sm text-state-bad-text">{t.loadError}</p>}
        <Table>
          <THead>
            <TR>
              <TH>{t.number}</TH>
              <TH>{t.client}</TH>
              <TH>{t.period}</TH>
              <TH className="text-right">{strings.subscriptions.total}</TH>
              <TH>{t.status}</TH>
              <TH>{t.due}</TH>
              <TH className="text-right">{t.payment}</TH>
            </TR>
          </THead>
          <TBody>
            {rows.length === 0 && (
              <TableFallbackRow
                columns={7}
                loading={invoices.isLoading}
                icon={Receipt}
                title={filter === "ACTION" && !q && !month ? t.noInvoicesAction : t.noInvoices}
              />
            )}
            {rows.map((invoice) => {
              const [variant, label] = stateOf(invoice);
              return (
                <TR key={invoice.id}>
                  <TD className="whitespace-nowrap font-mono text-content">
                    {invoice.fgoNumar ? `${invoice.fgoSerie} ${invoice.fgoNumar}` : "—"}
                  </TD>
                  {/* La 375px coloana se strângea la trei cuvinte pe rând; cu lățimea ei, tabelul se derulează în cutie. */}
                  <TD className="min-w-[14rem] text-content">
                    {invoice.client}
                    {isFailed(invoice) && (
                      <span className="mt-0.5 block max-w-[40ch] text-xs text-state-bad-text">{invoice.lastError}</span>
                    )}
                  </TD>
                  <TD className="whitespace-nowrap">
                    {formatDate(invoice.periodStart).slice(0, 5)} – {formatDate(invoice.periodEnd).slice(0, 5)}
                  </TD>
                  <TD className="whitespace-nowrap text-right tabular-nums">{lei(invoice.total)}</TD>
                  <TD>
                    <Badge variant={variant}>{label}</Badge>
                  </TD>
                  <TD className="whitespace-nowrap">
                    {invoice.status === "PAID" && invoice.paidAt ? (
                      <>
                        {t.paidOn.replace("{date}", formatDate(invoice.paidAt))}
                        <span className="block text-xs text-content-muted">
                          {invoice.paidBy === "CARD" ? t.byCard : t.byTransfer}
                        </span>
                      </>
                    ) : (
                      <>
                        {formatDate(invoice.dueDate) || "—"}
                        {invoice.overdueMailedAt && (
                          <span className="block text-xs text-content-muted">{t.reminderSent}</span>
                        )}
                      </>
                    )}
                  </TD>
                  <TD className="text-right">
                    <span className="inline-flex flex-wrap items-center justify-end gap-x-3 gap-y-1">
                      {invoice.status === "ISSUED" && (
                        <>
                          <span className="text-xs text-content-muted">
                            {invoice.paymentCheckedAt
                              ? t.checkedAt.replace("{when}", checkedWhen(invoice.paymentCheckedAt))
                              : t.neverChecked}
                          </span>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => check(invoice)}
                            disabled={checkMut.isPending}
                          >
                            {t.checkPayment}
                          </Button>
                        </>
                      )}
                      {isFailed(invoice) && (
                        <>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => open({ kind: invoice.ownerKind, id: invoice.ownerId }, invoice.client)}
                          >
                            {t.fix}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => stop(invoice.id, invoice.client)}
                            disabled={discardMut.isPending}
                          >
                            {t.stop}
                          </Button>
                        </>
                      )}
                      {invoice.fgoLink && (
                        <a href={invoice.fgoLink} target="_blank" rel="noreferrer" className="text-brand underline">
                          {strings.subscriptions.invoicePdf}
                        </a>
                      )}
                    </span>
                  </TD>
                </TR>
              );
            })}
          </TBody>
        </Table>
        {data && (
          <Pagination
            page={data.page}
            pageCount={Math.max(1, Math.ceil(data.total / PAGE_SIZE))}
            onPage={setPage}
            matchCount={data.total}
            pageSize={PAGE_SIZE}
          />
        )}
      </section>
    </div>
  );
}

function LastRunPanel({
  run,
  loading,
  error,
  onOpen,
  onStop,
  busy,
}: {
  run: LastBillingRun | null | undefined;
  loading: boolean;
  error: boolean;
  onOpen: (owner: BillingOwnerRef, client: string) => void;
  onStop: (invoiceId: string, client: string) => void;
  busy: boolean;
}) {
  // Emisele și plătitele pot fi sute la o rulare: stau strânse, căderile rămân la vedere (F-B2).
  const [expanded, setExpanded] = useState(false);
  if (loading) return <p className="mt-6 text-sm text-content-muted">{strings.common.loading}</p>;
  if (error) return <p className="mt-6 text-sm text-state-bad-text">{t.runLoadError}</p>;

  const stillFailed = new Set(run?.stillFailed ?? []);
  const others = run
    ? run.result.issuedInvoices.length + run.result.paidInvoices.length + run.result.notStarted.length
    : 0;

  return (
    <section aria-labelledby="last-run" className="mt-6 rounded-md border border-line bg-surface">
      <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1 border-b border-line px-4 py-3">
        <h2 id="last-run" className="text-sm font-semibold text-content">
          {t.lastRun}
        </h2>
        {run ? (
          <>
            <span className="font-mono text-xs text-content-muted">
              {checkedWhen(run.startedAt)} · {run.kind === "MANUAL" ? t.kindManual : t.kindScheduled}
            </span>
            <span className="flex flex-wrap gap-x-4 gap-y-1">
              {/* `countOf` cere n ≥ 1: „0 de facturi emise” se citea prost, iar un zero nu spune nimic aici. */}
              {run.result.issued > 0 && (
                <Badge variant="warning">{countOf(run.result.issued, t.issuedOne, t.issuedMany)}</Badge>
              )}
              {run.result.paid > 0 && (
                <Badge variant="success">{countOf(run.result.paid, t.paidOne, t.paidMany)}</Badge>
              )}
              {run.result.failed > 0 && (
                <Badge variant="danger">{countOf(run.result.failed, t.failedOne, t.failedMany)}</Badge>
              )}
              {run.result.notStarted.length > 0 && (
                <Badge variant="muted">
                  {countOf(run.result.notStarted.length, t.notStartedOne, t.notStartedMany)}
                </Badge>
              )}
            </span>
            {others > 0 && (
              <Button
                size="sm"
                variant="ghost"
                className="ml-auto"
                aria-expanded={expanded}
                onClick={() => setExpanded((v) => !v)}
              >
                {expanded ? t.hideDetails : `${t.details} (${others})`}
              </Button>
            )}
          </>
        ) : (
          <span className="text-sm text-content-muted">{t.noRun}</span>
        )}
      </div>
      {run && (
        <ul className="divide-y divide-line">
          {run.result.failures.map((f) => (
            <RunRow
              key={`f-${f.invoiceId}`}
              variant="danger"
              state={t.rowFailed}
              client={f.client}
              detail={<span className="text-state-bad-text">{f.reason}</span>}
            >
              {stillFailed.has(f.invoiceId) && f.reason.startsWith("CUI-ul") && (
                <Link
                  to={f.owner?.kind === "company" ? `/clienti/${f.owner.id}` : "/clienti"}
                  className="text-xs font-semibold text-brand-700 hover:underline"
                >
                  {t.fixCui}
                </Link>
              )}
              {stillFailed.has(f.invoiceId) && f.owner && (
                <Button size="sm" variant="outline" onClick={() => onOpen(f.owner!, f.client)}>
                  {t.openSubscription}
                </Button>
              )}
              {stillFailed.has(f.invoiceId) && (
                <Button size="sm" variant="outline" onClick={() => onStop(f.invoiceId, f.client)} disabled={busy}>
                  {t.stop}
                </Button>
              )}
            </RunRow>
          ))}
          {expanded &&
            run.result.issuedInvoices.map((d) => (
              <RunRow key={`i-${d.invoiceId}`} variant="warning" state={t.rowIssued} number={d.number} client={d.client}
                detail={lei(d.total)} />
            ))}
          {expanded &&
            run.result.paidInvoices.map((d) => (
              <RunRow key={`p-${d.invoiceId}`} variant="success" state={t.rowPaid} number={d.number} client={d.client}
                detail={lei(d.total)} />
            ))}
          {expanded &&
            run.result.notStarted.map((n) => (
              <RunRow
                key={`n-${n.owner.id}`}
                variant="muted"
                state={t.rowNotStarted}
                client={n.client}
                detail={t.startsOn.replace("{date}", formatDate(n.startsOn))}
              >
                <Button size="sm" variant="outline" onClick={() => onOpen(n.owner, n.client)}>
                  {t.openSubscription}
                </Button>
              </RunRow>
            ))}
          {run.result.failed + run.result.issued + run.result.paid + run.result.notStarted.length === 0 && (
            <li className="px-4 py-3 text-sm text-content-muted">{t.nothingToDo}</li>
          )}
          {run.result.failures.length === 0 && others > 0 && !expanded && (
            <li className="px-4 py-3 text-sm text-content-muted">{t.nothingToFix}</li>
          )}
        </ul>
      )}
    </section>
  );
}

function RunRow({
  variant,
  state,
  number,
  client,
  detail,
  children,
}: {
  variant: Variant;
  state: string;
  number?: string;
  client: string;
  detail: ReactNode;
  children?: ReactNode;
}) {
  return (
    <li className="flex flex-col gap-2 px-4 py-3 text-sm sm:flex-row sm:items-center">
      <span className="w-36 shrink-0">
        <Badge variant={variant}>{state}</Badge>
      </span>
      <span className="min-w-0 flex-1">
        {number && <span className="mr-2 font-mono text-content">{number}</span>}
        <span className="font-medium text-content">{client}</span>
        <span className="block text-xs text-content-muted">{detail}</span>
      </span>
      {children && <span className="flex flex-wrap items-center gap-2 sm:justify-end">{children}</span>}
    </li>
  );
}
