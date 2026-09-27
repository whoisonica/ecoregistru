import { useMemo, useState } from "react";
import { ClipboardList, FileText, Plus } from "lucide-react";
import {
  fetchStockOpeningPdf,
  useConfirmStockOpening,
  useDeleteStockOpening,
  useInventories,
  useStockOpenings,
} from "@/hooks/useInventory";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { openPdfInTab } from "@/lib/openFileInTab";
import { strings } from "@/lib/strings";
import type { StockOpening } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";
import { InventoryDialog, STATUS_VARIANT } from "@/components/depot/InventoryDialog";
import { StockOpeningDialog } from "@/components/depot/StockOpeningDialog";

const t = strings.inventory;
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
const roDate = (iso: string | null) => (iso ? iso.split("-").reverse().join(".") : "—");

/**
 * D3.5 — tabul „Inventar” de pe „Cântar”: pe depozitul ales, soldul preluat (o singură notă confirmată) și
 * inventarele lui. Scrie doar cine administrează firma; ceilalți văd și deschid documentele.
 */
export function InventoryTab({ canManage }: { canManage: boolean }) {
  const { notify } = useToast();
  const workPoints = useWorkPoints();
  const depots = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  const [picked, setPicked] = useState("");
  const depot = picked || (depots.length === 1 ? depots[0].id : "");
  const depotName = depots.find((w) => w.id === depot)?.name ?? "";
  const openings = useStockOpenings(depot);
  const inventories = useInventories(depot);
  const confirmOpening = useConfirmStockOpening();
  const deleteOpening = useDeleteStockOpening();
  const [confirm, confirmElement] = useConfirm();
  const [editingOpening, setEditingOpening] = useState<StockOpening | "new" | null>(null);
  const [openInventory, setOpenInventory] = useState<string | "new" | null>(null);

  const opening =
    (openings.data ?? []).find((o) => o.status === "CONFIRMED") ?? (openings.data ?? []).find((o) => o.status === "DRAFT");

  async function pdf(o: StockOpening) {
    try {
      await openPdfInTab(() => fetchStockOpeningPdf(o.id), "nota-preluare-solduri.pdf");
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.pdfError), "error");
    }
  }

  async function run(fn: () => Promise<unknown>, message: string) {
    try {
      await fn();
      notify(message, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.loadError), "error");
    }
  }

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-end gap-3">
        <div>
          <Label htmlFor="inv-depot">{t.depot}</Label>
          <Select id="inv-depot" value={depot} onChange={(e) => setPicked(e.target.value)}>
            <option value="">{t.pickDepot}</option>
            {depots.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </Select>
        </div>
        {canManage && depot && (
          <Button className="ml-auto" onClick={() => setOpenInventory("new")}>
            <Plus className="mr-2 h-4 w-4" />
            {t.newInventory}
          </Button>
        )}
      </div>

      {!depot && <p className="mb-3 text-xs text-content-muted">{t.pickDepot}</p>}

      {depot && (
        <section aria-label={t.openingTitle} className="mb-4 flex flex-wrap items-center gap-3 border border-line bg-surface-sunken px-4 py-3">
          <h2 className="text-xs font-medium uppercase tracking-wide text-content-muted">
            {t.openingTitle}
            <Tooltip content={t.openingHint}>
              <span className="ml-1 cursor-help text-content-subtle">?</span>
            </Tooltip>
          </h2>
          {!opening && <Badge variant="default">{t.openingNone}</Badge>}
          {opening && (
            <>
              <Badge variant={opening.status === "CONFIRMED" ? "success" : "warning"}>
                {opening.status === "CONFIRMED" ? t.openingConfirmed : t.openingDraft}
              </Badge>
              <span className="font-mono text-xs tabular-nums">
                {t.openingSummary
                  .replace("{n}", opening.number == null ? "—" : String(opening.number))
                  .replace("{date}", roDate(opening.cutOffDate))
                  .replace("{lines}", String(opening.lines.length))}
              </span>
              <Button variant="outline" size="sm" onClick={() => pdf(opening)}>
                <FileText className="mr-1 h-3.5 w-3.5" />
                {t.pdf}
              </Button>
            </>
          )}
          {canManage && (
            <div className="ml-auto flex gap-2">
              {!opening && (
                <Button variant="outline" size="sm" onClick={() => setEditingOpening("new")}>
                  {t.openingNew}
                </Button>
              )}
              {opening?.status === "DRAFT" && (
                <>
                  <Button variant="outline" size="sm" onClick={() => setEditingOpening(opening)}>
                    {t.openingEdit}
                  </Button>
                  <Button
                    variant="danger-ghost"
                    size="sm"
                    onClick={() => run(() => deleteOpening.mutateAsync(opening.id), t.deleted)}
                  >
                    {t.openingDelete}
                  </Button>
                  <Button
                    size="sm"
                    onClick={() =>
                      confirm({
                        title: t.confirmOpeningTitle,
                        message: t.confirmOpeningMessage,
                        confirmLabel: t.openingConfirm,
                        onConfirm: () => run(() => confirmOpening.mutateAsync(opening.id), t.confirmed),
                      })
                    }
                  >
                    {t.openingConfirm}
                  </Button>
                </>
              )}
            </div>
          )}
        </section>
      )}

      {depot && (
        <>
          {inventories.isError && <p className="text-sm text-state-bad-text">{t.loadError}</p>}
          <Table>
            <THead>
              <TR>
                <TH>{t.colNumber}</TH>
                <TH>{t.colKind}</TH>
                <TH>{t.colPeriod}</TH>
                <TH>{t.colStatus}</TH>
                <TH className="text-right">{t.colDifferences}</TH>
              </TR>
            </THead>
            <TBody>
              {(inventories.isLoading || (inventories.data ?? []).length === 0) && (
                <TableFallbackRow
                  columns={5}
                  loading={inventories.isLoading}
                  icon={ClipboardList}
                  title={t.empty}
                  description={t.emptyHint}
                />
              )}
              {(inventories.data ?? []).map((i) => (
                <TR key={i.id} className="cursor-pointer hover:bg-surface-muted" onClick={() => setOpenInventory(i.id)}>
                  <TD className="font-mono tabular-nums">{i.number}</TD>
                  <TD>{t.kind[i.kind]}</TD>
                  <TD className="whitespace-nowrap font-mono text-xs tabular-nums">
                    {roDate(i.startsOn)} – {roDate(i.endsOn)}
                  </TD>
                  <TD>
                    <Badge variant={STATUS_VARIANT[i.status]}>{t.status[i.status]}</Badge>
                    {i.warnings.length > 0 && (
                      <Badge variant="warning" className="ml-2">
                        {i.warnings.length}
                      </Badge>
                    )}
                  </TD>
                  <TD className="text-right font-mono tabular-nums">
                    +{kgFormat.format(i.surplusKg)} / −{kgFormat.format(i.shortageKg)}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </>
      )}

      {editingOpening && (
        <StockOpeningDialog
          workPointId={depot}
          depotName={depotName}
          opening={editingOpening === "new" ? null : editingOpening}
          onClose={() => setEditingOpening(null)}
        />
      )}
      {openInventory && (
        <InventoryDialog
          workPointId={depot}
          depotName={depotName}
          inventoryId={openInventory === "new" ? null : openInventory}
          canManage={canManage}
          onClose={() => setOpenInventory(null)}
        />
      )}
      {confirmElement}
    </div>
  );
}
