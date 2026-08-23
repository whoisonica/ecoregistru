import { FileText } from "lucide-react";
import { useMovements } from "@/hooks/useMovements";
import { canPrintAnexa3, useAnexa3Download } from "@/hooks/useAnexa3";
import type { MovementFilters, WasteMovement } from "@/lib/types";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";

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

  if (isLoading) return <p className="text-sm text-gray-500">{strings.common.loading}</p>;
  if (isError) return <p className="text-sm text-red-600">{t.handoversLoadError}</p>;

  return (
    <>
      <p className="mb-3 text-sm text-gray-500">{t.handoversSubtitle}</p>
      <div className="overflow-x-auto">
        <Table>
          <THead>
            <TR>
              <TH>{t.colHandoverDate}</TH>
              <TH>{t.colWasteCode}</TH>
              <TH className="text-right">{m.quantity}</TH>
              <TH>{t.colOperationCode}</TH>
              <TH>{t.colPartnerName}</TH>
              <TH>{t.colWorkPoint}</TH>
              <TH className="text-right">{strings.common.actions}</TH>
            </TR>
          </THead>
          <TBody>
            {rows.length === 0 && (
              <TR>
                <TD colSpan={7} className="text-center text-gray-400">
                  {t.emptyHandovers}
                </TD>
              </TR>
            )}
            {rows.map((mv: WasteMovement) => (
              <TR key={mv.id}>
                <TD className="whitespace-nowrap">
                  {/* The date the waste actually left; the unloading date when it is known. */}
                  {formatDate(mv.unloadDate ?? mv.date)}
                </TD>
                <TD>
                  <span className="font-medium text-gray-900">{mv.wasteCode}</span>
                  {mv.hazardous && (
                    <Badge variant="danger" className="ml-2">
                      {t.hazardous}
                    </Badge>
                  )}
                  <span className="block max-w-xs truncate text-xs text-gray-400">
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
                      <span className="font-medium text-gray-900">
                        {mv.treatmentPurpose ?? mv.operationCode.charAt(0)}
                      </span>
                      <span className="ml-1 text-gray-500">{mv.operationCode}</span>
                    </>
                  ) : (
                    <Badge variant="warning" title={t.incompleteHint}>
                      {t.incomplete}
                    </Badge>
                  )}
                </TD>
                <TD>
                  {mv.partnerName ?? <span className="text-gray-400">{t.ownSite}</span>}
                </TD>
                <TD>{mv.workPointName}</TD>
                <TD className="text-right">
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
      </div>
    </>
  );
}
