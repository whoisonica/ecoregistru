import { useMemo, useState, type FormEvent } from "react";
import { Plus, Trash2 } from "lucide-react";
import { useSaveStockOpening } from "@/hooks/useInventory";
import { useWasteArticles } from "@/hooks/useWasteArticles";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { todayIso } from "@/lib/utils";
import type { StockOpening, StockOpeningSource } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { ChoiceCards } from "@/components/ui/choice-cards";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";

const t = strings.inventory;

type Row = { articleId: string; kg: string };

function num(value: string): number | null {
  const cleaned = value.replace(",", ".").trim();
  if (cleaned === "") return null;
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : null;
}

/**
 * D3.5 — nota de preluare a soldurilor: stocul de dinaintea aplicației, cu sursa lui și cu cine confirmă
 * reconcilierea. Se salvează ca ciornă; confirmarea se face din tab, după ce nota e completă.
 */
export function StockOpeningDialog({
  workPointId,
  depotName,
  opening,
  onClose,
}: {
  workPointId: string;
  depotName: string;
  opening: StockOpening | null;
  onClose: () => void;
}) {
  const { notify } = useToast();
  const save = useSaveStockOpening();
  const articles = useWasteArticles();
  const active = useMemo(() => (articles.data ?? []).filter((a) => a.active), [articles.data]);

  const [cutOff, setCutOff] = useState(opening?.cutOffDate ?? opening?.firstMovementOn ?? todayIso());
  const [source, setSource] = useState<StockOpeningSource>(opening?.source ?? "STOCK_CARDS");
  const [keeper, setKeeper] = useState(opening?.keeperName ?? "");
  const [accountant, setAccountant] = useState(opening?.accountantName ?? "");
  const [notes, setNotes] = useState(opening?.notes ?? "");
  const [rows, setRows] = useState<Row[]>(
    opening?.lines.length
      ? opening.lines.map((l) => ({ articleId: l.articleId ?? "", kg: String(l.kg) }))
      : [{ articleId: "", kg: "" }]
  );

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      await save.mutateAsync({
        id: opening?.id ?? null,
        input: {
          workPointId,
          cutOffDate: cutOff,
          source,
          keeperName: keeper,
          accountantName: accountant,
          notes,
          lines: rows
            .filter((r) => r.articleId)
            .map((r) => ({ articleId: r.articleId, wasteCodeId: null, kg: num(r.kg) ?? 0 })),
        },
      });
      notify(t.saved, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.loadError), "error");
    }
  }

  return (
    <Dialog
      open
      size="xl"
      onClose={onClose}
      title={t.openingDialog}
      description={depotName}
      busy={save.isPending}
      footer={
        <Button type="submit" form="stock-opening-form" disabled={save.isPending}>
          {t.save}
        </Button>
      }
    >
      <form id="stock-opening-form" onSubmit={submit} className="space-y-4">
        <p className="text-xs text-content-muted">{t.openingHint}</p>
        <div className="grid gap-3 sm:grid-cols-3">
          <div>
            <Label htmlFor="so-cutoff">{t.cutOffDate}</Label>
            <DateInput id="so-cutoff" value={cutOff} onChange={(e) => setCutOff(e.target.value)} />
            {opening?.firstMovementOn && (
              <p className="mt-1 text-xs text-content-muted">
                {t.cutOffHint.replace("{date}", opening.firstMovementOn.split("-").reverse().join("."))}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="so-keeper">{t.keeper}</Label>
            <Input id="so-keeper" value={keeper} onChange={(e) => setKeeper(e.target.value)} />
          </div>
          <div>
            <Label htmlFor="so-accountant">{t.accountant}</Label>
            <Input id="so-accountant" value={accountant} onChange={(e) => setAccountant(e.target.value)} />
          </div>
        </div>
        <div>
          <Label id="so-source-label">{t.source}</Label>
          <ChoiceCards
            name="so-source"
            aria-labelledby="so-source-label"
            columns={3}
            value={source}
            onChange={setSource}
            options={(["STOCK_CARDS", "ACCOUNTING", "PHYSICAL_COUNT"] as const).map((v) => ({
              value: v,
              label: t.sourceLabel[v],
              description: t.sourceHint[v],
            }))}
          />
        </div>
        <div>
          <Label>{t.lines}</Label>
          <div className="space-y-2">
            {rows.map((row, i) => (
              <div key={i} className="flex items-end gap-2">
                <div className="flex-1">
                  <Select
                    aria-label={t.article}
                    value={row.articleId}
                    onChange={(e) => setRows(rows.map((r, j) => (j === i ? { ...r, articleId: e.target.value } : r)))}
                  >
                    <option value="">{t.article}</option>
                    {active.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.name} · {a.wasteCode}
                      </option>
                    ))}
                  </Select>
                </div>
                <div className="w-32">
                  <Input
                    aria-label={t.kg}
                    inputMode="decimal"
                    placeholder={t.kg}
                    value={row.kg}
                    onChange={(e) => setRows(rows.map((r, j) => (j === i ? { ...r, kg: e.target.value } : r)))}
                  />
                </div>
                <Button
                  type="button"
                  variant="outline"
                  aria-label={t.removeLine}
                  onClick={() => setRows(rows.length === 1 ? [{ articleId: "", kg: "" }] : rows.filter((_, j) => j !== i))}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>
          <Button type="button" variant="outline" className="mt-2" onClick={() => setRows([...rows, { articleId: "", kg: "" }])}>
            <Plus className="mr-2 h-4 w-4" />
            {t.addLine}
          </Button>
        </div>
        <div>
          <Label htmlFor="so-notes">{t.notes}</Label>
          <Textarea id="so-notes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>
      </form>
    </Dialog>
  );
}
