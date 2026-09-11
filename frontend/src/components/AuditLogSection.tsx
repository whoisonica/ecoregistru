import { useMemo, useState } from "react";
import { History } from "lucide-react";
import { useAuditLog } from "@/hooks/useAuditLog";
import type { AuditChange, AuditLogEntry, AuditLogFilters } from "@/lib/types";
import { strings } from "@/lib/strings";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useRemoteTableView } from "@/hooks/useTableView";

const t = strings.settings.audit;
const e = strings.enums;

/**
 * Momentul, cu ora.
 *
 * <p>⚠️ Nu `formatDate`, și diferența nu e cosmetică: aceea taie șirul ISO dinadins, fiindcă
 * primește o **zi calendaristică** — data unei mișcări e 11 septembrie oriunde ai citi-o. Aici e un
 * **moment**, iar momentul se citește în fusul celui care se uită la el; un jurnal care ar scrie
 * ora UTC ar pune pe rând o oră la care nimeni n-a fost la birou.
 */
function formatMoment(iso: string): string {
  const at = new Date(iso);
  return Number.isNaN(at.getTime()) ? iso : at.toLocaleString("ro-RO", { dateStyle: "short", timeStyle: "short" });
}

/** Fapta, colorată după cât de ireversibilă e. */
function actionVariant(action: AuditLogEntry["action"]) {
  if (action === "DELETE") return "danger" as const;
  if (action === "DEACTIVATE") return "warning" as const;
  if (action === "CREATE" || action === "REACTIVATE") return "success" as const;
  return "muted" as const;
}

/** „Cantitate: 5,000 → 7,500". Golul se scrie ca gol, nu ca o săgeată care pornește de nicăieri. */
function changeLine(change: AuditChange): string {
  const label = e.auditField[change.field] ?? change.field;
  const from = change.from ?? t.emptyValue;
  const to = change.to ?? t.emptyValue;
  return `${label}: ${from} ${t.changeArrow} ${to}`;
}

/**
 * P1.11 — jurnalul de audit al firmei.
 *
 * <p>Stă în Setări, sub utilizatori, fiindcă răspunde la aceeași întrebare ca ei, cu o zi
 * întârziere: acolo scrie cine are voie, aici scrie ce a făcut.
 *
 * <p><b>Numai citire, și numai pentru administratori.</b> Nu există niciun buton care să schimbe un
 * rând de aici — un jurnal care se poate rescrie nu răspunde la întrebarea pentru care există. Iar
 * accesul e al administratorului fiindcă rândurile numesc oameni: întrebarea e despre date,
 * răspunsul e despre un coleg.
 *
 * <p>Paginat la server din prima zi: e singura tabelă despre care se știe de la început că numai
 * crește.
 */
export function AuditLogSection({ canManage }: { canManage: boolean }) {
  const [entityType, setEntityType] = useState("");
  const filters = useMemo<AuditLogFilters>(
    () => (entityType ? { entityType } : {}),
    [entityType]
  );
  const table = useRemoteTableView<AuditLogEntry>({ resetOn: filters });
  const { data, isLoading, isError } = useAuditLog(filters, table.params, canManage);
  const view = table.bind(data);

  if (!canManage) return null;

  return (
    <section id="jurnal-audit" className="mt-8 scroll-mt-20">
      <h2 className="mb-1 text-lg font-semibold text-content">{t.title}</h2>
      <p className="mb-3 max-w-3xl text-sm text-content-muted">{t.subtitle}</p>

      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

      {!isError && (
        <div>
          <TableToolbar view={view} placeholder={t.searchPlaceholder}>
            <Select
              aria-label={t.filterType}
              value={entityType}
              onChange={(event) => setEntityType(event.target.value)}
              className="w-auto"
            >
              <option value="">{t.allTypes}</option>
              {Object.entries(e.auditEntity).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </Select>
          </TableToolbar>

          <Table stickyHeader>
            <THead sticky>
              <TR>
                <TH className="whitespace-nowrap">{t.colWhen}</TH>
                <TH>{t.colWho}</TH>
                <TH>{t.colWhat}</TH>
                <TH>{t.colAction}</TH>
                <TH>{t.colChanges}</TH>
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={5}
                  loading={isLoading}
                  icon={History}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((row) => (
                <TR key={row.id}>
                  <TD className="whitespace-nowrap text-content-muted">
                    {formatMoment(row.occurredAt)}
                  </TD>
                  <TD>
                    <span className="block max-w-[16rem] truncate font-medium text-content">
                      {row.actorEmail ?? t.deletedActor}
                    </span>
                    {row.actorRole && (
                      <span className="text-xs text-content-subtle">{e.role[row.actorRole]}</span>
                    )}
                  </TD>
                  <TD>
                    <span className="font-medium text-content">
                      {e.auditEntity[row.entityType] ?? row.entityType}
                    </span>
                    {row.label && (
                      <span className="block max-w-xs truncate text-xs text-content-subtle">
                        {row.label}
                      </span>
                    )}
                  </TD>
                  <TD className="whitespace-nowrap">
                    <Badge variant={actionVariant(row.action)}>{e.auditAction[row.action]}</Badge>
                  </TD>
                  <TD>
                    {row.changes.length === 0 ? (
                      <span className="text-content-subtle">{t.noChanges}</span>
                    ) : (
                      <ul className="space-y-0.5 text-xs text-content-muted">
                        {/* Trei rubrici încap pe un rând de tabel; restul se numără. O modificare
                            de treizeci de câmpuri ar împinge rândurile vecine în afara ecranului. */}
                        {row.changes.slice(0, 3).map((change) => (
                          <li key={change.field} className="max-w-md truncate">
                            {changeLine(change)}
                          </li>
                        ))}
                        {row.changes.length > 3 && (
                          <li className="text-content-subtle">
                            {t.moreChanges.replace("{count}", String(row.changes.length - 3))}
                          </li>
                        )}
                      </ul>
                    )}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
          <TablePagination view={view} />
          <p className="mt-2 text-xs text-content-subtle">{t.retention}</p>
        </div>
      )}
    </section>
  );
}
