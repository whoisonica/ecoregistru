import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { Briefcase, ChevronRight } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { companiesKey } from "@/hooks/useCompanies";
import { useConsultancyOverview } from "@/hooks/useConsultancies";
import { useTableView } from "@/hooks/useTableView";
import type { ConsultancyOverviewRow } from "@/lib/types";
import { strings } from "@/lib/strings";
import { countOf } from "@/lib/utils";
import { daysUntil } from "@/lib/deadlines";
import { PageHeader } from "@/components/ui/page-header";
import { Badge } from "@/components/ui/badge";
import { Button, LinkButton } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { TableFallbackRow } from "@/components/ui/table-fallback";

const t = strings.consultancyOverview;

/** „15.09.2026" din „2026-09-15", fără fus orar: e o dată de calendar, nu un moment. */
function formatDate(iso: string): string {
  const [y, m, d] = iso.split("-");
  return `${d}.${m}.${y}`;
}

function needsAttention(r: ConsultancyOverviewRow): boolean {
  return (
    !r.deadlinesGenerated ||
    r.overdueDeadlines > 0 ||
    r.linesWithoutOperationCode + r.linesAwaitingWeighing + r.unprovenMirrorMovements > 0 ||
    r.partnersExpiring > 0
  );
}

/**
 * P2.13, felia 2 — „Firmele mele": panoul cabinetului.
 *
 * <p>Un consultant cu 20 de firme comuta firma de 20 de ori ca să afle cine e în întârziere. Aici vede
 * tot pe un ecran, cu aceleași cifre ca pe Panoul fiecărei firme, și ajunge dintr-un clic pe ecranul
 * care rezolvă: celula de termene duce la Termene, blocajele la Evidențe, partenerii la Parteneri — pe
 * firma aceea, aleasă în comutator.
 *
 * <p>Ordinea vine de la server (depășite întâi, firmele la zi la coadă); sortarea pe nume rămâne la
 * îndemână, fiindcă într-un portofoliu mare cine caută o firmă anume o caută după nume.
 */
export function ConsultancyOverviewPage() {
  const { user, switchTenant } = useAuth();
  const isConsultant = user?.role === "CONSULTANT";
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { data: rows, isLoading, isError } = useConsultancyOverview(isConsultant);

  const view = useTableView(rows ?? [], {
    searchText: (r) => [r.name, r.cui].filter(Boolean).join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });

  /**
   * Comută pe firmă și deschide ecranul. Cache-ul firmei de dinainte se aruncă exact ca în
   * comutatorul din bara laterală (`CompanyBlock`), altfel ecranul ar arăta o clipă datele altei firme.
   */
  function openCompany(companyId: string, path: string) {
    queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== companiesKey[0] });
    switchTenant(companyId);
    navigate(path);
  }

  if (!isConsultant) {
    return <PageHeader title={t.title} description={t.consultantsOnly} />;
  }

  const attention = (rows ?? []).filter(needsAttention).length;

  return (
    <div>
      <PageHeader
        title={t.title}
        description={
          <>
            {t.subtitle}
            {rows && rows.length > 0 && (
              <span className="mt-1 block font-medium text-content">
                {attention === 0
                  ? t.summaryAllClear
                  : t.summary
                      .replace("{attention}", String(attention))
                      .replace("{total}", countOf(rows.length, "firmă", "firme"))}
              </span>
            )}
          </>
        }
      />

      <section className="mt-6">
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isError && !isLoading && rows?.length === 0 && (
          <EmptyState
            icon={Briefcase}
            title={t.empty}
            description={t.emptyHint}
            action={
              <LinkButton to="/clienti" variant="outline">
                {t.emptyAction}
              </LinkButton>
            }
          />
        )}

        {!isError && (isLoading || (rows?.length ?? 0) > 0) && (
          <>
            <TableToolbar view={view} placeholder={t.searchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                    {t.company}
                  </SortableTH>
                  <TH>{t.deadlines}</TH>
                  <TH>{t.blockers}</TH>
                  <TH>{t.partners}</TH>
                  <TH sticky="right" className="text-right">
                    {strings.common.actions}
                  </TH>
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={5}
                    loading={isLoading}
                    icon={Briefcase}
                    title={strings.common.noResults}
                    description={strings.common.noResultsHint}
                  />
                )}
                {view.visible.map((r) => (
                  <TR key={r.companyId}>
                    <TD className="font-medium text-content">
                      {r.name}
                      {r.cui && <div className="text-xs font-normal text-content-subtle">{r.cui}</div>}
                    </TD>
                    <TD>
                      <DeadlineCell row={r} onOpen={() => openCompany(r.companyId, "/termene")} />
                    </TD>
                    <TD>
                      <BlockersCell row={r} onOpen={() => openCompany(r.companyId, "/evidente")} />
                    </TD>
                    <TD>
                      {r.partnersExpiring > 0 ? (
                        <button
                          type="button"
                          className="text-left"
                          onClick={() => openCompany(r.companyId, "/parteneri")}
                        >
                          <Badge variant="warning">
                            {t.partnersExpiring.replace("{count}", String(r.partnersExpiring))}
                          </Badge>
                        </button>
                      ) : (
                        <span className="text-content-subtle">{t.none}</span>
                      )}
                    </TD>
                    <TD sticky="right" className="text-right">
                      <Button variant="ghost" size="sm" onClick={() => openCompany(r.companyId, "/")}>
                        {t.open}
                        <ChevronRight className="ml-1 h-3.5 w-3.5" />
                      </Button>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
            <TablePagination view={view} />
          </>
        )}
      </section>
    </div>
  );
}

function DeadlineCell({ row, onOpen }: { row: ConsultancyOverviewRow; onOpen: () => void }) {
  if (!row.deadlinesGenerated && row.overdueDeadlines === 0) {
    // Nu „La zi": fără termene generate, lipsa depășirilor nu dovedește nimic.
    return (
      <button type="button" className="text-left" onClick={onOpen}>
        <Badge variant="warning">{t.notGenerated}</Badge>
        <span className="mt-1 block text-xs text-brand hover:underline">{t.generate} →</span>
      </button>
    );
  }
  const next = row.nextDeadline;
  const days = next ? daysUntil(next.dueDate) : null;
  return (
    <button type="button" className="space-y-1 text-left" onClick={onOpen}>
      {row.overdueDeadlines > 0 && (
        <Badge variant="danger">
          {row.overdueDeadlines === 1
            ? t.overdueOne
            : t.overdue.replace("{count}", String(row.overdueDeadlines))}
        </Badge>
      )}
      <span className="block text-sm text-content">
        {next ? (
          <>
            {t.next.replace("{date}", formatDate(next.dueDate))}
            <span className={days !== null && days <= 7 ? "ml-1 font-medium text-amber-700" : "ml-1 text-content-subtle"}>
              ({days === 0
                ? strings.deadlines.daysToday
                : days === 1
                  ? strings.deadlines.daysTomorrow
                  : strings.deadlines.daysLeft.replace("{count}", countOf(days ?? 0, "zi", "zile"))})
            </span>
            <span className="block text-xs text-content-subtle">
              {strings.enums.reportType[next.reportType]}
            </span>
          </>
        ) : (
          <span className="text-content-subtle">{t.noNext}</span>
        )}
      </span>
    </button>
  );
}

function BlockersCell({ row, onOpen }: { row: ConsultancyOverviewRow; onOpen: () => void }) {
  const items = [
    { count: row.linesWithoutOperationCode, label: t.withoutCode },
    { count: row.linesAwaitingWeighing, label: t.awaitingWeighing },
    { count: row.unprovenMirrorMovements, label: t.mirror },
  ].filter((i) => i.count > 0);
  if (items.length === 0) {
    return <Badge variant="success">{t.clear}</Badge>;
  }
  return (
    <button type="button" className="flex flex-col items-start gap-1 text-left" onClick={onOpen}>
      {items.map((i) => (
        <Badge key={i.label} variant="danger">
          {i.label.replace("{count}", String(i.count))}
        </Badge>
      ))}
    </button>
  );
}
