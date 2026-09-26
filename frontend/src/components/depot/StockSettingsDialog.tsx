import { useMemo, useState, type FormEvent } from "react";
import { Trash2 } from "lucide-react";
import {
  useAddAuthorizedLimit,
  useDeleteAuthorizedLimit,
  useDeleteStockThreshold,
  useSaveStockThreshold,
  useStockThresholds,
} from "@/hooks/useStock";
import { useWasteArticles } from "@/hooks/useWasteArticles";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import type { LimitKind, LimitPeriod, LimitUnit, StockLimitStatus } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormSection } from "@/components/ui/form-section";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { useToast } from "@/components/ui/toast";

const t = strings.weighing;

function num(value: string): number | null {
  const cleaned = value.replace(",", ".").trim();
  if (cleaned === "") return null;
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : null;
}

/**
 * D3.3 și D3.4 — pragurile pe sortiment și limitele din autorizație ale unui depozit. Le scrie cine administrează firma
 * (serverul la fel). O limită se tastează cum e scrisă în autorizație; nu se editează, se șterge și se scrie din nou.
 */
export function StockSettingsDialog({
  workPointId,
  depotName,
  limits,
  onClose,
}: {
  workPointId: string;
  depotName: string;
  limits: StockLimitStatus[];
  onClose: () => void;
}) {
  const { notify } = useToast();
  const articles = useWasteArticles();
  const thresholds = useStockThresholds(workPointId);
  const saveThreshold = useSaveStockThreshold();
  const deleteThreshold = useDeleteStockThreshold();
  const addLimit = useAddAuthorizedLimit();
  const deleteLimit = useDeleteAuthorizedLimit();
  const activeArticles = useMemo(() => (articles.data ?? []).filter((a) => a.active), [articles.data]);

  const [articleId, setArticleId] = useState("");
  const [min, setMin] = useState("");
  const [max, setMax] = useState("");
  const [kind, setKind] = useState<LimitKind>("STORED");
  const [codeId, setCodeId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState<LimitUnit>("T");
  const [period, setPeriod] = useState<LimitPeriod>("AT_ONCE");
  const [days, setDays] = useState("");
  const [approximate, setApproximate] = useState(false);
  const [note, setNote] = useState("");

  // Codurile pe care le au sortimentele firmei: autorizația le numește pe coduri, catalogul le are deja.
  const codes = useMemo(() => {
    const seen = new Map<string, string>();
    for (const a of activeArticles) if (a.wasteCodeId) seen.set(a.wasteCodeId, a.wasteCode);
    return [...seen.entries()].sort((x, y) => x[1].localeCompare(y[1]));
  }, [activeArticles]);

  async function submitThreshold(e: FormEvent) {
    e.preventDefault();
    try {
      await saveThreshold.mutateAsync({ workPointId, articleId, minKg: num(min), maxKg: num(max) });
      notify(t.stockSaved, "success");
      setMin("");
      setMax("");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  async function submitLimit(e: FormEvent) {
    e.preventDefault();
    try {
      await addLimit.mutateAsync({
        workPointId,
        kind,
        wasteCodeId: codeId || null,
        quantity: num(quantity) ?? 0,
        unit,
        period,
        maxStorageDays: num(days),
        approximate,
        note: note.trim() || null,
      });
      notify(t.stockSaved, "success");
      setQuantity("");
      setDays("");
      setNote("");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  async function remove(action: () => Promise<unknown>) {
    try {
      await action();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.stockSettingsTitle.replace("{depot}", depotName)}
      description={t.stockSettingsHint}
      size="2xl"
      footer={
        <Button variant="outline" onClick={onClose}>
          {strings.common.close}
        </Button>
      }
    >
      <div className="space-y-6">
        <FormSection title={t.stockThresholdsTitle}>
          <ul className="space-y-1 text-sm">
            {(thresholds.data ?? []).map((th) => (
              <li key={th.id} className="flex items-center justify-between gap-2 border-b border-line py-1">
                <span>
                  {th.articleName}
                  <span className="ml-2 font-mono text-xs text-content-muted">
                    {th.minKg ?? "—"} … {th.maxKg ?? "—"} kg
                  </span>
                </span>
                <Button variant="ghost" size="sm" aria-label={strings.common.delete}
                  onClick={() => remove(() => deleteThreshold.mutateAsync(th.id))}>
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </li>
            ))}
          </ul>
          <form onSubmit={submitThreshold} className="grid grid-cols-1 gap-3 sm:grid-cols-4">
            <div className="sm:col-span-2">
              <Label htmlFor="sts-article">{t.stockArticle}</Label>
              <Select id="sts-article" value={articleId} onChange={(e) => setArticleId(e.target.value)}>
                <option value="">—</option>
                {activeArticles.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} · {a.wasteCode}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="sts-min">{t.stockMin}</Label>
              <Input id="sts-min" inputMode="decimal" value={min} onChange={(e) => setMin(e.target.value)} />
            </div>
            <div>
              <Label htmlFor="sts-max">{t.stockMax}</Label>
              <Input id="sts-max" inputMode="decimal" value={max} onChange={(e) => setMax(e.target.value)} />
            </div>
            <div className="sm:col-span-4">
              <Button type="submit" variant="outline" disabled={!articleId || saveThreshold.isPending}>
                {t.stockSaveThreshold}
              </Button>
            </div>
          </form>
        </FormSection>

        <FormSection title={t.stockLimitsTitle}>
          <ul className="space-y-1 text-sm">
            {limits.map((l) => (
              <li key={l.id} className="flex items-center justify-between gap-2 border-b border-line py-1">
                <span>
                  {t.stockLimitKind[l.kind]} · {l.wasteCode ?? t.stockLimitAllCodes} ·{" "}
                  <span className="font-mono">
                    {l.quantity} {l.unit === "M3" ? "m³" : l.unit.toLowerCase()}
                  </span>{" "}
                  {t.stockLimitPeriod[l.period]}
                  {l.maxStorageDays != null && ` · ${l.maxStorageDays} zile`}
                  {l.approximate && ` · ${t.stockLimitApproximate}`}
                  {l.note && <span className="block text-xs text-content-muted">{l.note}</span>}
                </span>
                <Button variant="ghost" size="sm" aria-label={strings.common.delete}
                  onClick={() => remove(() => deleteLimit.mutateAsync(l.id))}>
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </li>
            ))}
          </ul>
          <form onSubmit={submitLimit} className="space-y-3">
            <PillGroup
              name="stl-kind"
              options={(["STORED", "TREATED", "OUTPUT"] as LimitKind[]).map((k) => ({ value: k, label: t.stockLimitKind[k] }))}
              selected={[kind]}
              onToggle={(v) => setKind(v)}
            />
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div>
                <Label htmlFor="stl-code">{t.stockLimitCode}</Label>
                <Select id="stl-code" value={codeId} onChange={(e) => setCodeId(e.target.value)}>
                  <option value="">{t.stockLimitAllCodes}</option>
                  {codes.map(([id, code]) => (
                    <option key={id} value={id}>
                      {code}
                    </option>
                  ))}
                </Select>
              </div>
              <div>
                <Label htmlFor="stl-qty">{t.stockLimitQuantity}</Label>
                <Input id="stl-qty" inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
              </div>
              <div>
                <Label id="stl-unit-label">{t.stockLimitUnit}</Label>
                <PillGroup
                  name="stl-unit"
                  aria-labelledby="stl-unit-label"
                  options={(["T", "KG", "M3"] as LimitUnit[]).map((u) => ({ value: u, label: u === "M3" ? "m³" : u.toLowerCase() }))}
                  selected={[unit]}
                  onToggle={(v) => setUnit(v)}
                />
              </div>
              <div>
                <Label htmlFor="stl-days">{t.stockLimitDays}</Label>
                <Input id="stl-days" inputMode="numeric" value={days} onChange={(e) => setDays(e.target.value)} />
              </div>
            </div>
            <PillGroup
              name="stl-period"
              options={(["AT_ONCE", "MONTH", "YEAR"] as LimitPeriod[]).map((p) => ({ value: p, label: t.stockLimitPeriod[p] }))}
              selected={[period]}
              onToggle={(v) => setPeriod(v)}
            />
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="stl-note">{t.stockLimitNote}</Label>
                <Input id="stl-note" maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
              </div>
              <Switch id="stl-approx" checked={approximate} onChange={setApproximate} label={t.stockLimitApproximate} />
            </div>
            <Button type="submit" variant="outline" disabled={!num(quantity) || addLimit.isPending}>
              {t.stockAddLimit}
            </Button>
          </form>
        </FormSection>
      </div>
    </Dialog>
  );
}
