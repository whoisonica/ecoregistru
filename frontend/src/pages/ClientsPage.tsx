import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Briefcase, Building2, Plus, Receipt, UserPlus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { isMultiCompany } from "@/lib/roles";
import { AssignConsultancyDialog, ConsultanciesSection } from "@/components/ConsultanciesSection";
import { ConsultancyTeamSection } from "@/components/ConsultancyTeamSection";
import { ConsultancyBrandingSection } from "@/components/ConsultancyBrandingSection";
import { InviteUserDialog } from "@/components/InviteUserDialog";
import { useClientOverview, useCompanies } from "@/hooks/useCompanies";
import type { Company } from "@/lib/types";
import { AccountRequestsSection } from "@/components/AccountRequestsSection";
import { useAccountRequests } from "@/hooks/useAccountRequests";
import { useInvoiceMoney } from "@/hooks/useSubscriptions";
import { useConsultancies } from "@/hooks/useConsultancies";
import { useUrlState } from "@/hooks/useUrlState";
import { useHotkey } from "@/hooks/useHotkey";
import {
  byAttention,
  clientRow,
  CLIENT_FILTERS,
  CONSULTANT_FILTERS,
  MATCHES,
  type ClientFilter,
  type ClientRow,
} from "@/lib/clients";
import { countOf } from "@/lib/count";
import { formatDate, todayIso } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Menu, MenuItem } from "@/components/ui/menu";
import { PageTabs, type PageTab } from "@/components/ui/page-tabs";
import { PillGroup } from "@/components/ui/pill-group";
import { Tooltip } from "@/components/ui/tooltip";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Badge } from "@/components/ui/badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";

const t = strings.clients;
const typeLabels = strings.enums.companyType;

export function ClientsPage() {
  const { user } = useAuth();
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";
  // P2.13 — consultantul ajunge și el aici: aceleași firme, restrânse de server la cabinetul lui.
  const isConsultant = user?.role === "CONSULTANT";
  const multiCompany = isMultiCompany(user?.role);

  const { data: companies, isLoading, isError } = useCompanies(multiCompany);
  const [assigning, setAssigning] = useState<Company | null>(null);
  const [inviting, setInviting] = useState<Company | null>(null);
  const navigate = useNavigate();

  // F-B — abonamentul, ultima factură și oamenii fiecărei firme; banii și cererile doar la platformă.
  const overview = useClientOverview(multiCompany);
  const money = useInvoiceMoney(isPlatformAdmin);
  const requests = useAccountRequests(isPlatformAdmin);
  const consultancies = useConsultancies(isPlatformAdmin);
  const [tabParam, setTab] = useUrlState("tab");
  const newRequests = (requests.data ?? []).filter((r) => r.status === "NEW").length;
  const [filter, setFilter] = useState<ClientFilter>("ALL");
  const today = todayIso();

  const rows = useMemo(() => {
    const byId = new Map((overview.data ?? []).map((o) => [o.companyId, o]));
    return (companies ?? []).map((c) => clientRow(c, byId.get(c.id), today)).sort(byAttention);
  }, [companies, overview.data, today]);
  const filters = isPlatformAdmin ? CLIENT_FILTERS : CONSULTANT_FILTERS;
  const counts = Object.fromEntries(filters.map((f) => [f, rows.filter(MATCHES[f]).length])) as Record<
    ClientFilter,
    number
  >;

  const view = useTableView(
    rows.filter(MATCHES[filter]),
    { searchText: (r) => [r.company.name, r.company.cui].filter(Boolean).join(" ") }
  );

  // Taburile (F-B2): o singură secțiune pe ecran, ca pagina să se termine sub tabel. Tabul stă în adresă.
  const tabs: PageTab[] = isPlatformAdmin
    ? [
        { id: "", label: t.tabClients, count: companies?.length },
        { id: "cereri", label: t.tabRequests, count: requests.data ? newRequests : undefined, alert: newRequests > 0 },
        { id: "cabinete", label: t.tabCabinets, count: consultancies.data?.length },
      ]
    : [
        { id: "", label: t.tabClients, count: companies?.length },
        { id: "echipa", label: t.tabTeam },
        { id: "antet", label: t.tabBranding },
      ];
  const tab = tabs.some((x) => x.id === tabParam) ? tabParam : "";

  useHotkey("n", () => navigate("/clienti/nou"), { enabled: multiCompany && tab === "" && !inviting && !assigning });

  if (!multiCompany) {
    return (
      <div>
        <PageHeader title={t.title} description={t.onlyPlatformAdmin} />
      </div>
    );
  }

  /** F-C — clientul nou are pagina lui, în pași. */
  function openCreate() {
    navigate("/clienti/nou");
  }

  /** F-D — firma are pagina ei; tabul poate fi dat direct („⋯ → Abonament”, „Deschide firma” dintr-o cerere). */
  function openCompany(companyId: string, tabId = "") {
    navigate(`/clienti/${companyId}${tabId ? `?tab=${tabId}` : ""}`);
  }

  return (
    <div>
      <PageHeader
        title={t.title}
        description={isConsultant ? t.subtitleConsultant : t.subtitle}
        actions={
          <Button onClick={openCreate} hotkey="N">
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        }
      />

      <PageTabs tabs={tabs} selected={tab} onSelect={setTab} label={t.tabsLabel} />

      {tab === "" && (
      <>
      <ClientFigures
        rows={rows}
        overviewFailed={overview.isError}
        money={isPlatformAdmin ? money : null}
        today={today}
      />

      {isPlatformAdmin && <RequestsBand count={newRequests} names={(requests.data ?? []).filter((r) => r.status === "NEW").map((r) => r.companyName)} onOpen={() => setTab("cereri")} />}

      <section className="mt-6">
        {isError && <p className="text-sm text-state-bad-text">{t.loadError}</p>}

        {!isError && (
          <>
            <PillGroup
              name="client-filter"
              options={filters.map((f) => ({
                value: f,
                label: (
                  <>
                    {t.filters[f]}{" "}
                    <span className="font-mono text-xs opacity-70">
                      {companies && (f === "ALL" || f === "CABINETS" || overview.data) ? counts[f] : t.unknown}
                    </span>
                  </>
                ),
              }))}
              selected={[filter]}
              onToggle={setFilter}
              className="mb-3"
            />
            <TableToolbar view={view} placeholder={t.searchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <TH>{t.colClient}</TH>
                  {isPlatformAdmin && <TH>{t.colSubscription}</TH>}
                  {isPlatformAdmin && <TH>{t.colLastInvoice}</TH>}
                  <TH className="text-right">{t.colUsers}</TH>
                  <TH>{t.colProfile}</TH>
                  <TH sticky="right" className="text-right">{strings.common.actions}</TH>
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={isPlatformAdmin ? 6 : 4}
                    loading={isLoading}
                    icon={Building2}
                    title={view.emptiedBySearch || filter !== "ALL" ? strings.common.noResults : t.empty}
                    description={
                      view.emptiedBySearch || filter !== "ALL"
                        ? strings.common.noResultsHint
                        : isConsultant
                          ? t.emptyHintConsultant
                          : t.emptyHint
                    }
                    action={
                      <Button onClick={openCreate}>
                        <Plus className="mr-2 h-4 w-4" />
                        {t.add}
                      </Button>
                    }
                  />
                )}
                {view.visible.map((r) => (
                  <TR key={r.company.id}>
                    <TD className="min-w-[14rem]">
                      <span className="font-medium text-content">{r.company.name}</span>
                      <span className="block font-mono text-xs text-content-muted">
                        {r.company.cui} · {typeLabels[r.company.type]}
                      </span>
                      {!r.company.active && <Badge variant="muted">{t.inactive}</Badge>}
                    </TD>
                    {isPlatformAdmin && (
                      <TD className="whitespace-nowrap">
                        <SubscriptionCell row={r} />
                      </TD>
                    )}
                    {isPlatformAdmin && (
                      <TD className="whitespace-nowrap">
                        <InvoiceCell row={r} />
                      </TD>
                    )}
                    <TD className="text-right font-mono tabular-nums">
                      {r.overview ? (
                        r.noUsers ? <Badge variant="danger">0</Badge> : r.overview.userCount
                      ) : (
                        t.unknown
                      )}
                    </TD>
                    <TD className="whitespace-nowrap">
                      <ProfileCell row={r} />
                    </TD>
                    <TD sticky="right" className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openCompany(r.company.id)}>
                          {t.open}
                        </Button>
                        <Menu>
                          {/* O firmă dintr-un cabinet n-are abonament propriu: o plătește cabinetul. */}
                          {isPlatformAdmin && !r.company.consultancyId && (
                            <MenuItem icon={Receipt} onClick={() => openCompany(r.company.id, "abonament")}>
                              {t.subscriptionAction}
                            </MenuItem>
                          )}
                          {isPlatformAdmin && (
                            <MenuItem icon={Briefcase} onClick={() => setAssigning(r.company)}>
                              {t.assignConsultancy}
                            </MenuItem>
                          )}
                          <MenuItem icon={UserPlus} onClick={() => setInviting(r.company)}>
                            {t.invite}
                          </MenuItem>
                        </Menu>
                      </div>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
            <TablePagination view={view} />
          </>
        )}
      </section>
      </>
      )}

      {/* Inboxul cererilor publice și cabinetele sunt ale platformei; echipa, a consultantului. */}
      {isPlatformAdmin && tab === "cereri" && <AccountRequestsSection enabled onOpenCompany={openCompany} />}
      {isPlatformAdmin && tab === "cabinete" && <ConsultanciesSection />}
      {isConsultant && tab === "echipa" && <ConsultancyTeamSection />}
      {isConsultant && tab === "antet" && <ConsultancyBrandingSection />}
      {inviting && <InviteUserDialog company={inviting} onClose={() => setInviting(null)} />}
      {assigning && (
        <AssignConsultancyDialog company={assigning} onClose={() => setAssigning(null)} />
      )}
    </div>
  );
}

function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { maximumFractionDigits: 2 })} lei`;
}

/** O cifră în mono, ca pe afișaj, cu eticheta deasupra și explicația lângă. Sursa căzută arată „?”, nu „0”. */
function Figure({ label, value, sub, tone }: { label: string; value: string; sub: string; tone?: "bad" }) {
  return (
    <Card data-figure={label} className="px-4 py-3">
      <div className="eyebrow">{label}</div>
      <div className="mt-1 flex flex-wrap items-baseline gap-x-2">
        <span
          data-figure-value
          className={`font-mono text-xl tabular-nums ${tone === "bad" ? "text-state-bad-text" : "text-content"}`}
        >
          {value}
        </span>
        <span className="text-xs text-content-muted">{sub}</span>
      </div>
    </Card>
  );
}

function ClientFigures({
  rows,
  overviewFailed,
  money,
  today,
}: {
  rows: ClientRow[];
  overviewFailed: boolean;
  money: ReturnType<typeof useInvoiceMoney> | null;
  today: string;
}) {
  const active = rows.filter((r) => r.company.active).length;
  const attention = rows.filter((r) => r.reasons.length > 0);
  const failed = attention.filter((r) => r.reasons.includes("FAILED")).length;
  const overdue = attention.filter((r) => r.reasons.includes("OVERDUE")).length;
  const noUsers = attention.filter((r) => r.reasons.includes("NO_USERS")).length;
  const m = money?.data;
  const month = new Date(`${today}T12:00:00`).toLocaleString("ro-RO", { month: "long" });
  const reasons = [
    failed > 0 && countOf(failed, t.reasonFailedOne, t.reasonFailedMany),
    overdue > 0 && countOf(overdue, t.reasonOverdueOne, t.reasonOverdueMany),
    noUsers > 0 && `${noUsers} ${t.reasonNoUsers}`,
  ].filter(Boolean);

  return (
    <div className={`mt-4 grid grid-cols-2 gap-3 ${money ? "lg:grid-cols-4" : ""}`} data-testid="client-figures">
      <Figure label={t.statActive} value={String(active)} sub={t.statActiveSub.replace("{n}", String(rows.length))} />
      {money && (
        <Figure
          label={t.statPaid.replace("{month}", month)}
          value={m ? lei(m.paidThisMonth) : t.unknown}
          sub={m && m.paidThisMonthCount > 0 ? countOf(m.paidThisMonthCount, t.statPaidOne, t.statPaidMany) : "—"}
        />
      )}
      {money && (
        <Figure
          label={t.statDue}
          value={m ? lei(m.unpaid) : t.unknown}
          sub={m && m.unpaidCount > 0 ? countOf(m.unpaidCount, t.statDueOne, t.statDueMany) : "—"}
        />
      )}
      <Figure
        label={t.statAttention}
        value={overviewFailed ? t.unknown : String(attention.length)}
        sub={overviewFailed ? "—" : reasons.length > 0 ? reasons.join(" · ") : t.statAttentionNone}
        tone={attention.length > 0 && !overviewFailed ? "bad" : undefined}
      />
    </div>
  );
}

function RequestsBand({ count, names, onOpen }: { count: number; names: string[]; onOpen: () => void }) {
  if (count === 0) return null;
  return (
    <div className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-line bg-surface-muted px-4 py-2 text-sm">
      <span className="text-content">
        <span className="font-semibold">{countOf(count, t.requestOne, t.requestMany)}</span>
        {names.length > 0 && <span className="text-content-muted"> · {names.join(", ")}</span>}
      </span>
      <Button size="sm" variant="outline" onClick={onOpen}>
        {t.requestsSee}
      </Button>
    </div>
  );
}

const SUBSCRIPTION_BADGE = {
  PENDING: "warning",
  ACTIVE: "success",
  PAST_DUE: "danger",
  READ_ONLY: "danger",
  CANCELLED: "muted",
} as const;

function SubscriptionCell({ row }: { row: ClientRow }) {
  const o = row.overview;
  if (row.company.consultancyId) {
    return (
      <>
        <Badge variant="muted">{t.paidByCabinet}</Badge>
        <span className="block text-xs text-content-muted">{row.company.consultancyName}</span>
      </>
    );
  }
  if (!o) return <span className="text-content-subtle">{t.unknown}</span>;
  if (!o.subscriptionStatus || !o.plan) return <Badge variant="muted">{t.noSubscription}</Badge>;
  return (
    <>
      <Badge variant={SUBSCRIPTION_BADGE[o.subscriptionStatus]}>
        {strings.subscriptions.status[o.subscriptionStatus]}
      </Badge>
      <span className="block text-xs text-content-muted">
        {strings.subscriptions.plans[o.plan]}
        {o.monthlyPrice != null && ` · ${lei(o.monthlyPrice)}`}
      </span>
    </>
  );
}

function InvoiceCell({ row }: { row: ClientRow }) {
  const i = row.overview?.lastInvoice;
  if (!row.overview) return <span className="text-content-subtle">{t.unknown}</span>;
  if (!i) return <span className="text-content-subtle">—</span>;
  const [variant, label] =
    i.status === "PAID"
      ? (["success", t.invoicePaid] as const)
      : row.failed
        ? (["danger", t.invoiceFailed] as const)
        : i.status === "DRAFT"
          ? (["muted", t.invoiceDraft] as const)
          : row.overdueDays > 0
            ? (["danger", t.invoiceOverdue.replace("{days}", countOf(row.overdueDays, "zi", "zile"))] as const)
            : (["warning", t.invoiceDue.replace("{date}", formatDate(i.dueDate).slice(0, 5))] as const);
  return (
    <>
      <span className="font-mono text-sm text-content">
        {i.number && `${i.number} · `}
        {lei(i.total)}
      </span>
      <span className="block">
        <Badge variant={variant}>{label}</Badge>
      </span>
      {/* Motivul întreg la hover: pe trei rânduri, un client căzut umfla rândul cât trei (F-B2). */}
      {row.failed && i.lastError && (
        <Tooltip content={i.lastError}>
          <span className="block max-w-[28ch] truncate text-xs text-state-bad-text">{i.lastError}</span>
        </Tooltip>
      )}
    </>
  );
}

function ProfileCell({ row }: { row: ClientRow }) {
  if (row.cuiInvalid) return <Badge variant="danger">{t.profileCui}</Badge>;
  if (row.gaps.length === 0) return <Badge variant="success">{t.profileComplete}</Badge>;
  return (
    <Tooltip content={t.profileGapsHint.replace("{fields}", row.gaps.map((g) => t.gapFields[g]).join(", "))}>
      <Badge variant="warning">{countOf(row.gaps.length, t.profileGapOne, t.profileGapMany)}</Badge>
    </Tooltip>
  );
}
