import { ArrowRightLeft, FileText } from "lucide-react";
import { useMovements } from "@/hooks/useMovements";
import { canPrintAnexa3, useAnexa3Download } from "@/hooks/useAnexa3";
import type { MovementFilters, WasteMovement } from "@/lib/types";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tooltip } from "@/components/ui/tooltip";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";

const t = strings.evidences;
const m = strings.movements;
const e = strings.enums;

function formatDate(iso: string) {
  const [y, mo, d] = iso.split("-");
  return `${d}.${mo}.${y}`;
}

/**
 * The handover register — what the Evidenţe tab shows by default, as asked at the 23.08.2026
 * meeting: "scot generat / adaug cantitate, data când s-o predat; la valorificare să apară
 * partenerul şi cod V/R/D. Şi atât."
 *
 * <p>So this is not the monthly Anexa 1 aggregate; it is the line-by-line record of what left the
 * site — the paper trail behind the declaration, and the row Anexa 3 is printed from. The monthly
 * view lives next to it, because the running stock is the one figure nobody can reconstruct by eye
 * and the one the form actually asks for.
 *
 * <p>Only exits are listed. Generation has its own tab, and a movement that stayed on site has
 * nothing to prove to anyone.
 */
export function HandoverRegister({ filters }: { filters: MovementFilters }) {
  const { data: movements, isLoading, isError } = useMovements(filters);
  const { download, downloadingId } = useAnexa3Download();

  const rows = (movements ?? []).filter(
    (mv) => mv.operation === "RECOVERED" || mv.operation === "DISPOSED"
      || mv.operation === "UNCLASSIFIED_OUT"
  );

  const view = useTableView(rows, {
    searchText: (mv) =>
      [mv.wasteCode, mv.wasteCodeName, mv.partnerName, mv.workPointName, mv.operationCode]
        .filter(Boolean)
        .join(" "),
    comparators: {
      date: (a, b) => a.date.localeCompare(b.date),
      wasteCode: (a, b) => a.wasteCode.localeCompare(b.wasteCode, "ro"),
      quantity: (a, b) => {
        if (a.quantity == null) return 1;
        if (b.quantity == null) return -1;
        return a.quantity - b.quantity;
      },
      partnerName: (a, b) => (a.partnerName ?? "").localeCompare(b.partnerName ?? "", "ro"),
    },
    initialSort: { key: "date", direction: "desc" },
  });

  if (isError) return <p className="text-sm text-red-600">{t.handoversLoadError}</p>;

  return (
    <>
      <p className="mb-3 text-sm text-content-muted">{t.handoversSubtitle}</p>
      <TableToolbar view={view} placeholder={t.handoversSearchPlaceholder} />
      <div>
        <Table stickyHeader>
          <THead sticky>
            <TR>
              <SortableTH sortKey="date" sort={view.sort} onSort={view.toggleSort}>
                {t.colHandoverDate}
              </SortableTH>
              <SortableTH sortKey="wasteCode" sort={view.sort} onSort={view.toggleSort}>
                {t.colWasteCode}
              </SortableTH>
              <TH className="text-right">{m.quantity}</TH>
              <TH>{t.colOperationCode}</TH>
              <TH>{t.colPartnerName}</TH>
              <TH>{t.colWorkPoint}</TH>
              <TH sticky="right" className="text-right">{strings.common.actions}</TH>
            </TR>
          </THead>
          <TBody>
            {(isLoading || view.visible.length === 0) && (
              <TableFallbackRow
                columns={7}
                loading={isLoading}
                icon={ArrowRightLeft}
                title={view.emptiedBySearch ? strings.common.noResults : t.emptyHandovers}
              />
            )}
            {view.visible.map((mv: WasteMovement) => (
              <TR key={mv.id}>
                <TD className="whitespace-nowrap">
                  {/* The date the waste actually left; the unloading date when it is known. */}
                  {formatDate(mv.unloadDate ?? mv.date)}
                </TD>
                <TD>
                  <span className="font-medium text-content">{mv.wasteCode}</span>
                  {mv.hazardous && (
                    <Badge variant="danger" className="ml-2">
                      {t.hazardous}
                    </Badge>
                  )}
                  <span className="block max-w-xs truncate text-xs text-content-subtle">
                    {mv.wasteCodeName}
                  </span>
                </TD>
                <TD className="whitespace-nowrap text-right">
                  {mv.quantity != null ? (
                    <>
                      {mv.quantity} {e.unit[mv.unit]}
                    </>
                  ) : (
                    <Badge variant="warning" title={m.awaitingWeighingHint}>
                      {m.awaitingWeighing}
                    </Badge>
                  )}
                </TD>
                <TD className="whitespace-nowrap">
                  {mv.operationCode ? (
                    <>
                      <span className="font-medium text-content">
                        {mv.treatmentPurpose ?? mv.operationCode.charAt(0)}
                      </span>
                      <span className="ml-1 text-content-muted">{mv.operationCode}</span>
                    </>
                  ) : (
                    <Tooltip content={t.missingCodeHint}>
                      <Badge variant="danger">{t.missingCode}</Badge>
                    </Tooltip>
                  )}
                </TD>
                <TD>
                  {mv.partnerName ?? <span className="text-content-subtle">{t.ownSite}</span>}
                </TD>
                <TD>{mv.workPointName}</TD>
                <TD sticky="right" className="text-right">
                  {canPrintAnexa3(mv) && (
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={downloadingId === mv.id}
                      onClick={() => download(mv)}
                    >
                      <FileText className="mr-1 h-3.5 w-3.5" />
                      {downloadingId === mv.id ? m.anexa3Downloading : m.anexa3Download}
                    </Button>
                  )}
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
        <TablePagination view={view} />
      </div>
    </>
  );
}
