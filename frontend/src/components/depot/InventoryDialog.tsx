import { useMemo, useState } from "react";
import { FileText, Plus, Trash2 } from "lucide-react";
import {
  fetchInventoryPdf,
  useInventory,
  useInventoryAction,
  useInventoryOperationsDuring,
  useOpenInventory,
  type LinesInput,
} from "@/hooks/useInventory";
import { useWasteArticles } from "@/hooks/useWasteArticles";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { openPdfInTab } from "@/lib/openFileInTab";
import { strings } from "@/lib/strings";
import { todayIso } from "@/lib/utils";
import type {
  CountMethod,
  DeclarationAnswer,
  Inventory,
  InventoryKind,
  InventoryLine,
  InventoryMember,
  ShortageNature,
} from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";

const t = strings.inventory;
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
const kg = (value: number | null) => (value == null ? "—" : kgFormat.format(value));
const roDate = (iso: string | null) => (iso ? iso.split("-").reverse().join(".") : "—");
const PAGE = 10;

type Step = "decision" | "declaration" | "count" | "pv";

function num(value: string): number | null {
  const cleaned = value.replace(",", ".").trim();
  if (cleaned === "") return null;
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : null;
}

export const STATUS_VARIANT = {
  OPEN: "warning",
  CLOSED: "default",
  APPROVED: "success",
  CANCELLED: "danger",
} as const;

/**
 * D3.5 — un inventar, pe patru pași: decizia (pct. 6), declarația gestionarului (pct. 8 lit. a)), numărarea (faptic,
 * metodă, explicații) și procesul-verbal (pct. 42). Acțiunile urmează starea: deschis → PV încheiat → aprobat.
 * Operatorul vede, nu scrie (serverul la fel).
 */
export function InventoryDialog({
  workPointId,
  depotName,
  inventoryId,
  canManage,
  onClose,
}: {
  workPointId: string;
  depotName: string;
  inventoryId: string | null;
  canManage: boolean;
  onClose: () => void;
}) {
  const { notify } = useToast();
  const [id, setId] = useState<string | null>(inventoryId);
  const { data: inv } = useInventory(id);
  const [step, setStep] = useState<Step>("decision");
  const action = useInventoryAction();
  const [confirm, confirmElement] = useConfirm();
  const [cancelling, setCancelling] = useState(false);
  const [reason, setReason] = useState("");
  const editable = canManage && (!inv || inv.status === "OPEN");

  async function run(name: string, body?: unknown, message?: string) {
    if (!id) return;
    try {
      await action.mutateAsync({ id, action: name, body });
      if (message) notify(message, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.loadError), "error");
    }
  }

  async function pdf(document: keyof typeof t.documents) {
    if (!id) return;
    try {
      await openPdfInTab(() => fetchInventoryPdf(id, document), `inventar-${document}.pdf`);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.pdfError), "error");
    }
  }

  const title = inv ? t.dialogTitle.replace("{n}", String(inv.number)) : t.dialogNew;

  const footer = inv && canManage && (
    <div className="flex flex-wrap justify-end gap-2">
      {(inv.status === "OPEN" || inv.status === "CLOSED") && (
        <Button variant="outline" onClick={() => setCancelling(true)}>
          {t.cancel}
        </Button>
      )}
      {inv.status === "OPEN" && inv.warnings.includes("BOOK_CHANGED") && (
        <Button variant="outline" onClick={() => run("recalculate", undefined, t.recalculated)}>
          {t.recalculate}
        </Button>
      )}
      {inv.status === "OPEN" && <Button onClick={() => run("close", undefined, t.closed)}>{t.close}</Button>}
      {inv.status === "CLOSED" && (
        <>
          <Button variant="outline" onClick={() => run("reopen")}>
            {t.reopen}
          </Button>
          <Button
            onClick={() =>
              confirm({
                title: t.approveTitle,
                message: t.approveMessage,
                confirmLabel: t.approve,
                onConfirm: () => run("approve", undefined, t.approved),
              })
            }
          >
            {t.approve}
          </Button>
        </>
      )}
    </div>
  );

  return (
    <Dialog open size="2xl" onClose={onClose} title={title} description={depotName} busy={action.isPending} footer={footer}>
      {inv && (
        <div className="mb-3 flex flex-wrap items-center gap-x-4 gap-y-2">
          <Badge variant={STATUS_VARIANT[inv.status]}>{t.status[inv.status]}</Badge>
          <span className="font-mono text-xs tabular-nums text-content-muted">
            {t.totals.replace("{plus}", kg(inv.surplusKg)).replace("{minus}", kg(inv.shortageKg))}
          </span>
          <div className="ml-auto flex flex-wrap gap-1">
            {(Object.keys(t.documents) as (keyof typeof t.documents)[]).map((d) => (
              <Button key={d} variant="outline" size="sm" onClick={() => pdf(d)}>
                <FileText className="mr-1 h-3.5 w-3.5" />
                {t.documents[d]}
              </Button>
            ))}
          </div>
        </div>
      )}
      {inv && inv.warnings.length > 0 && (
        <ul role="status" className="mb-3 space-y-1 border border-line bg-surface-sunken px-3 py-2 text-xs">
          {inv.warnings.map((w) => (
            <li key={w} className="text-state-warn-text">
              {t.warnings[w]}
            </li>
          ))}
        </ul>
      )}
      {inv?.status === "CANCELLED" && inv.cancelReason && (
        <p className="mb-3 text-xs text-content-muted">
          {t.cancelReason}: {inv.cancelReason}
        </p>
      )}

      <div role="tablist" className="mb-4 flex gap-1 border-b border-line">
        {(Object.keys(t.steps) as Step[]).map((s) => (
          <button
            key={s}
            type="button"
            role="tab"
            aria-selected={step === s}
            disabled={!inv && s !== "decision"}
            onClick={() => setStep(s)}
            className={
              "-mb-px border-b-2 px-3 py-2 text-sm font-medium disabled:opacity-40 " +
              (step === s ? "border-brand-600 text-content-strong" : "border-transparent text-content-muted hover:text-content")
            }
          >
            {t.steps[s]}
          </button>
        ))}
      </div>

      {/* Un inventar existent se arată abia după ce vine: pașii își iau starea inițială din el o singură dată. */}
      {id && !inv && <p className="text-sm text-content-muted">{t.loading}</p>}
      {step === "decision" && (!id || inv) && (
        <DecisionStep
          key={inv?.id ?? "new"}
          workPointId={workPointId}
          inv={inv ?? null}
          editable={editable}
          onOpened={(opened) => {
            setId(opened.id);
            setStep("declaration");
          }}
          onSave={(body) => run("header", body, t.saved)}
        />
      )}
      {step === "declaration" && inv && (
        <DeclarationStep key={inv.id} inv={inv} editable={editable} onSave={(body) => run("declaration", body, t.saved)} />
      )}
      {step === "count" && inv && (
        // Liniile vin de la server după fiecare salvare sau recalculare: tabelul local pornește din nou din ele.
        <CountStep
          key={inv.lines.map((l) => `${l.id}:${l.bookKg}:${l.countedKg}`).join("|") + inv.status}
          inv={inv}
          editable={editable}
          onSave={(body) => run("lines", body, t.saved)}
        />
      )}
      {step === "pv" && inv && <PvStep key={inv.id} inv={inv} editable={editable} onSave={(body) => run("pv", body, t.saved)} />}

      {cancelling && (
        <Dialog
          open
          size="sm"
          onClose={() => setCancelling(false)}
          title={t.cancel}
          footer={
            <Button
              variant="danger"
              disabled={!reason.trim()}
              onClick={async () => {
                await run("cancel", { reason }, t.cancelled);
                setCancelling(false);
              }}
            >
              {t.cancel}
            </Button>
          }
        >
          <Label htmlFor="inv-cancel">{t.cancelReason}</Label>
          <Textarea id="inv-cancel" rows={3} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Dialog>
      )}
      {confirmElement}
    </Dialog>
  );
}

// --- Decizia ---

function DecisionStep({
  workPointId,
  inv,
  editable,
  onOpened,
  onSave,
}: {
  workPointId: string;
  inv: Inventory | null;
  editable: boolean;
  onOpened: (inv: Inventory) => void;
  onSave: (body: unknown) => void;
}) {
  const { notify } = useToast();
  const open = useOpenInventory();
  const [decisionNumber, setDecisionNumber] = useState(inv?.decisionNumber ?? "");
  const [decisionDate, setDecisionDate] = useState(inv?.decisionDate ?? todayIso());
  const [kind, setKind] = useState<InventoryKind>(inv?.kind ?? "ANNUAL");
  const [countsAsAnnual, setCountsAsAnnual] = useState(inv?.countsAsAnnual ?? false);
  const [mode, setMode] = useState(inv?.mode ?? "");
  const [method, setMethod] = useState(inv?.method ?? "");
  const [startsOn, setStartsOn] = useState(inv?.startsOn ?? todayIso());
  const [endsOn, setEndsOn] = useState(inv?.endsOn ?? todayIso());
  const [keeper, setKeeper] = useState(inv?.keeperName ?? "");
  const [receiving, setReceiving] = useState(inv?.receivingKeeperName ?? "");
  const [representative, setRepresentative] = useState(inv?.keeperRepresentative ?? "");
  const [members, setMembers] = useState<InventoryMember[]>(
    inv?.commission.length ? inv.commission : [{ name: "", role: "", president: true }]
  );

  const body = {
    workPointId,
    decisionNumber,
    decisionDate: decisionDate || null,
    kind,
    countsAsAnnual: kind !== "ANNUAL" && countsAsAnnual,
    mode,
    method,
    startsOn,
    endsOn,
    commission: members.filter((m) => m.name.trim()),
    keeperName: keeper,
    receivingKeeperName: kind === "HANDOVER" ? receiving || null : null,
    keeperRepresentative: representative || null,
  };

  async function submit() {
    if (inv) {
      onSave(body);
      return;
    }
    try {
      const opened = await open.mutateAsync(body);
      notify(t.opened, "success");
      onOpened(opened);
    } catch (err) {
      notify(apiErrorMessage(err, t.loadError), "error");
    }
  }

  return (
    <fieldset disabled={!editable} className="space-y-3">
      <div className="grid gap-3 sm:grid-cols-4">
        <div>
          <Label htmlFor="inv-dn">{t.decisionNumber}</Label>
          <Input id="inv-dn" value={decisionNumber} onChange={(e) => setDecisionNumber(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="inv-dd">{t.decisionDate}</Label>
          <DateInput id="inv-dd" value={decisionDate} onChange={(e) => setDecisionDate(e.target.value)} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="inv-kind">{t.kindLabel}</Label>
          <Select id="inv-kind" value={kind} onChange={(e) => setKind(e.target.value as InventoryKind)}>
            {(Object.keys(t.kind) as InventoryKind[]).map((k) => (
              <option key={k} value={k}>
                {t.kind[k]}
              </option>
            ))}
          </Select>
        </div>
      </div>
      {kind !== "ANNUAL" && (
        <Switch id="inv-annual" checked={countsAsAnnual} onChange={setCountsAsAnnual} label={t.countsAsAnnual} />
      )}
      <div className="grid gap-3 sm:grid-cols-4">
        <div className="sm:col-span-2">
          <Label htmlFor="inv-mode">{t.mode}</Label>
          <Input id="inv-mode" value={mode} onChange={(e) => setMode(e.target.value)} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="inv-method">{t.method}</Label>
          <Input id="inv-method" value={method} onChange={(e) => setMethod(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="inv-start">
            {t.startsOn}
            <Tooltip content={t.startsOnHint}>
              <span className="ml-1 cursor-help text-content-subtle">?</span>
            </Tooltip>
          </Label>
          <DateInput id="inv-start" value={startsOn} disabled={Boolean(inv)} onChange={(e) => setStartsOn(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="inv-end">{t.endsOn}</Label>
          <DateInput id="inv-end" value={endsOn} onChange={(e) => setEndsOn(e.target.value)} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="inv-keeper">{t.keeper}</Label>
          <Input id="inv-keeper" value={keeper} onChange={(e) => setKeeper(e.target.value)} />
        </div>
        {kind === "HANDOVER" && (
          <div className="sm:col-span-2">
            <Label htmlFor="inv-receiving">{t.receivingKeeper}</Label>
            <Input id="inv-receiving" value={receiving} onChange={(e) => setReceiving(e.target.value)} />
          </div>
        )}
        <div className="sm:col-span-2">
          <Label htmlFor="inv-rep">{t.keeperRepresentative}</Label>
          <Input id="inv-rep" value={representative} onChange={(e) => setRepresentative(e.target.value)} />
        </div>
      </div>
      <div>
        <Label>
          {t.commission}
          <Tooltip content={t.commissionHint}>
            <span className="ml-1 cursor-help text-content-subtle">?</span>
          </Tooltip>
        </Label>
        <div className="space-y-2">
          {members.map((m, i) => (
            <div key={i} className="flex flex-wrap items-center gap-2">
              <Input
                aria-label={t.memberName}
                placeholder={t.memberName}
                className="w-56"
                value={m.name}
                onChange={(e) => setMembers(members.map((x, j) => (j === i ? { ...x, name: e.target.value } : x)))}
              />
              <Input
                aria-label={t.memberRole}
                placeholder={t.memberRole}
                className="w-48"
                value={m.role ?? ""}
                onChange={(e) => setMembers(members.map((x, j) => (j === i ? { ...x, role: e.target.value } : x)))}
              />
              <label className="flex items-center gap-1 text-sm">
                <input
                  type="radio"
                  name="inv-president"
                  checked={m.president}
                  onChange={() => setMembers(members.map((x, j) => ({ ...x, president: j === i })))}
                />
                {t.president}
              </label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                aria-label={t.removeLine}
                onClick={() => setMembers(members.filter((_, j) => j !== i))}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            </div>
          ))}
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="mt-2"
          onClick={() => setMembers([...members, { name: "", role: "", president: members.length === 0 }])}
        >
          <Plus className="mr-1 h-3.5 w-3.5" />
          {t.addMember}
        </Button>
      </div>
      {editable && (
        <div className="flex justify-end">
          <Button onClick={submit} disabled={open.isPending}>
            {inv ? t.save : t.openAction}
          </Button>
        </div>
      )}
    </fieldset>
  );
}

// --- Declarația ---

function DeclarationStep({ inv, editable, onSave }: { inv: Inventory; editable: boolean; onSave: (body: unknown) => void }) {
  const [answers, setAnswers] = useState<DeclarationAnswer[]>(
    inv.declaration ?? t.questions.map(() => ({ yes: false, detail: null }))
  );
  const [lastEntry, setLastEntry] = useState(inv.lastEntryDoc ?? "");
  const [lastExit, setLastExit] = useState(inv.lastExitDoc ?? "");
  const [date, setDate] = useState(inv.declarationDate ?? inv.startsOn);

  return (
    <fieldset disabled={!editable} className="space-y-3">
      <p className="text-xs text-content-muted">{t.declarationIntro}</p>
      <ol className="space-y-2">
        {t.questions.map((q, i) => (
          <li key={i} className="flex flex-wrap items-center gap-3">
            <span className="w-72 text-sm">
              {i + 1}. {q}
            </span>
            <PillGroup
              name={`inv-q${i}`}
              aria-label={q}
              options={[
                { value: "NO", label: t.no },
                { value: "YES", label: t.yes },
              ]}
              selected={[answers[i]?.yes ? "YES" : "NO"]}
              onToggle={(v) => setAnswers(answers.map((a, j) => (j === i ? { ...a, yes: v === "YES" } : a)))}
            />
            {answers[i]?.yes && (
              <Input
                aria-label={t.detail}
                placeholder={t.detail}
                className="min-w-[14rem] flex-1"
                value={answers[i].detail ?? ""}
                onChange={(e) => setAnswers(answers.map((a, j) => (j === i ? { ...a, detail: e.target.value } : a)))}
              />
            )}
          </li>
        ))}
      </ol>
      <div className="grid gap-3 sm:grid-cols-3">
        <div>
          <Label htmlFor="inv-le">{t.lastEntryDoc}</Label>
          <Input id="inv-le" value={lastEntry} onChange={(e) => setLastEntry(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="inv-lx">{t.lastExitDoc}</Label>
          <Input id="inv-lx" value={lastExit} onChange={(e) => setLastExit(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="inv-ddate">{t.declarationDate}</Label>
          <DateInput id="inv-ddate" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
      </div>
      {editable && (
        <div className="flex justify-end">
          <Button
            onClick={() => onSave({ answers, lastEntryDoc: lastEntry || null, lastExitDoc: lastExit || null, declarationDate: date })}
          >
            {t.save}
          </Button>
        </div>
      )}
    </fieldset>
  );
}

// --- Numărarea ---

type EditLine = LinesInput["lines"][number] & {
  key: string;
  bookKg: number;
  wasteCode: string;
  label: string;
  hazardous: boolean;
  counted: string;
};

function toEdit(l: InventoryLine): EditLine {
  return {
    key: l.id ?? `${l.articleId}|${l.wasteCodeId}`,
    id: l.id,
    articleId: l.articleId,
    wasteCodeId: l.wasteCodeId,
    countedKg: l.countedKg,
    counted: l.countedKg == null ? "" : String(l.countedKg),
    countMethod: l.countMethod,
    technicalData: l.technicalData,
    explanation: l.explanation,
    shortageNature: l.shortageNature,
    responsiblePerson: l.responsiblePerson,
    slowMoving: l.slowMoving,
    bookKg: l.bookKg,
    wasteCode: l.wasteCode,
    label: l.articleName ?? l.wasteName,
    hazardous: l.hazardous,
  };
}

function CountStep({ inv, editable, onSave }: { inv: Inventory; editable: boolean; onSave: (body: LinesInput) => void }) {
  const [rows, setRows] = useState<EditLine[]>(() => inv.lines.map(toEdit));
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [adding, setAdding] = useState("");
  const articles = useWasteArticles();
  const active = useMemo(() => (articles.data ?? []).filter((a) => a.active), [articles.data]);

  const pages = Math.max(1, Math.ceil(rows.length / PAGE));
  const visible = rows.slice(page * PAGE, page * PAGE + PAGE);
  const current = rows.find((r) => r.key === selected) ?? null;
  const update = (key: string, patch: Partial<EditLine>) =>
    setRows(rows.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  const diff = (r: EditLine) => {
    const c = num(r.counted);
    return c == null ? null : Math.round((c - r.bookKg) * 1000) / 1000;
  };

  function addFound() {
    const a = active.find((x) => x.id === adding);
    if (!a) return;
    const key = `new-${a.id}-${rows.length}`;
    setRows([
      ...rows,
      {
        key,
        id: null,
        articleId: a.id,
        wasteCodeId: a.wasteCodeId,
        countedKg: null,
        counted: "",
        countMethod: null,
        technicalData: null,
        explanation: null,
        shortageNature: null,
        responsiblePerson: null,
        slowMoving: false,
        bookKg: 0,
        wasteCode: a.wasteCode,
        label: a.name,
        hazardous: a.hazardous,
      },
    ]);
    setAdding("");
    setSelected(key);
    setPage(Math.floor(rows.length / PAGE));
  }

  function save() {
    onSave({
      lines: rows.map((r) => ({
        id: r.id,
        articleId: r.articleId,
        wasteCodeId: r.wasteCodeId,
        countedKg: num(r.counted),
        countMethod: r.countMethod,
        technicalData: r.technicalData,
        explanation: r.explanation,
        shortageNature: r.shortageNature,
        responsiblePerson: r.responsiblePerson,
        slowMoving: r.slowMoving,
      })),
    });
  }

  return (
    <div className="space-y-3">
      <Table>
        <THead>
          <TR>
            <TH>{t.code}</TH>
            <TH>{t.article}</TH>
            <TH className="text-right">{t.book}</TH>
            <TH className="text-right">{t.counted}</TH>
            <TH className="text-right">{t.difference}</TH>
            <TH>{t.methodCol}</TH>
            <TH />
          </TR>
        </THead>
        <TBody>
          {visible.map((r) => {
            const d = diff(r);
            const changed = r.id != null && inv.changedLineIds.includes(r.id);
            return (
              <TR key={r.key} aria-selected={selected === r.key}>
                <TD className="whitespace-nowrap">
                  <span className="inline-flex items-center font-mono text-xs">
                    <BinSwatch code={r.wasteCode} hazardous={r.hazardous} />
                    {r.wasteCode}
                  </span>
                </TD>
                <TD>
                  {r.label}
                  {changed && (
                    <Badge variant="warning" className="ml-2">
                      {t.lineChanged}
                    </Badge>
                  )}
                </TD>
                <TD className="text-right font-mono tabular-nums">{kg(r.bookKg)}</TD>
                <TD className="text-right">
                  <Input
                    aria-label={`${t.counted} ${r.label}`}
                    inputMode="decimal"
                    className="ml-auto w-28 text-right font-mono"
                    disabled={!editable}
                    value={r.counted}
                    onChange={(e) => update(r.key, { counted: e.target.value })}
                  />
                </TD>
                <TD
                  className={
                    "text-right font-mono tabular-nums " +
                    (d == null || d === 0 ? "" : d > 0 ? "text-state-ok-text" : "text-state-bad-text")
                  }
                >
                  {d == null ? "—" : (d > 0 ? "+" : "") + kg(d)}
                </TD>
                <TD className="text-xs">{r.countMethod ? t.methodLabel[r.countMethod] : "—"}</TD>
                <TD className="text-right">
                  <Button variant="outline" size="sm" onClick={() => setSelected(selected === r.key ? null : r.key)}>
                    {t.details}
                  </Button>
                </TD>
              </TR>
            );
          })}
        </TBody>
      </Table>
      {pages > 1 && (
        <div className="flex items-center justify-end gap-2 text-xs">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
            ‹
          </Button>
          <span className="font-mono tabular-nums">
            {page + 1} / {pages}
          </span>
          <Button variant="outline" size="sm" disabled={page >= pages - 1} onClick={() => setPage(page + 1)}>
            ›
          </Button>
        </div>
      )}

      {current && (
        <fieldset disabled={!editable} className="space-y-3 border border-line bg-surface-sunken p-3">
          <p className="text-sm font-medium">{current.label}</p>
          <div>
            <Label id="inv-method-label">{t.methodCol}</Label>
            <PillGroup
              name="inv-method"
              aria-labelledby="inv-method-label"
              options={(["WEIGHED", "COUNTED", "MEASURED", "TECHNICAL"] as CountMethod[]).map((m) => ({
                value: m,
                label: t.methodLabel[m],
              }))}
              selected={current.countMethod ? [current.countMethod] : []}
              onToggle={(v) => update(current.key, { countMethod: v })}
            />
          </div>
          {current.countMethod === "TECHNICAL" && (
            <div>
              <Label htmlFor="inv-tech">
                {t.technicalData}
                <Tooltip content={t.technicalHint}>
                  <span className="ml-1 cursor-help text-content-subtle">?</span>
                </Tooltip>
              </Label>
              <Input
                id="inv-tech"
                value={current.technicalData ?? ""}
                onChange={(e) => update(current.key, { technicalData: e.target.value })}
              />
            </div>
          )}
          <div>
            <Label htmlFor="inv-expl">{t.explanation}</Label>
            <Input
              id="inv-expl"
              value={current.explanation ?? ""}
              onChange={(e) => update(current.key, { explanation: e.target.value })}
            />
          </div>
          {(diff(current) ?? 0) < 0 && (
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <Label id="inv-nature-label">{t.nature}</Label>
                <PillGroup
                  name="inv-nature"
                  aria-labelledby="inv-nature-label"
                  options={(["NON_IMPUTABLE", "IMPUTABLE"] as ShortageNature[]).map((n) => ({
                    value: n,
                    label: t.natureLabel[n],
                  }))}
                  selected={current.shortageNature ? [current.shortageNature] : []}
                  onToggle={(v) => update(current.key, { shortageNature: v })}
                />
              </div>
              {current.shortageNature === "IMPUTABLE" && (
                <div>
                  <Label htmlFor="inv-resp">{t.responsible}</Label>
                  <Input
                    id="inv-resp"
                    value={current.responsiblePerson ?? ""}
                    onChange={(e) => update(current.key, { responsiblePerson: e.target.value })}
                  />
                </div>
              )}
            </div>
          )}
          <Switch
            id="inv-slow"
            checked={current.slowMoving}
            onChange={(v) => update(current.key, { slowMoving: v })}
            label={t.slowMoving}
          />
        </fieldset>
      )}

      {editable && (
        <div className="flex flex-wrap items-end justify-between gap-2">
          <div className="flex items-end gap-2">
            <div>
              <Label htmlFor="inv-add">
                {t.addFound}
                <Tooltip content={t.addFoundHint}>
                  <span className="ml-1 cursor-help text-content-subtle">?</span>
                </Tooltip>
              </Label>
              <Select id="inv-add" value={adding} onChange={(e) => setAdding(e.target.value)}>
                <option value="">{t.article}</option>
                {active.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} · {a.wasteCode}
                  </option>
                ))}
              </Select>
            </div>
            <Button variant="outline" disabled={!adding} onClick={addFound}>
              <Plus className="mr-1 h-4 w-4" />
              {t.addLine}
            </Button>
          </div>
          <Button onClick={save}>{t.save}</Button>
        </div>
      )}
    </div>
  );
}

// --- Procesul-verbal ---

function PvStep({ inv, editable, onSave }: { inv: Inventory; editable: boolean; onSave: (body: unknown) => void }) {
  const during = useInventoryOperationsDuring(inv.id);
  const [pvDate, setPvDate] = useState(inv.pvDate ?? todayIso());
  const [fields, setFields] = useState({
    causes: inv.pvCauses ?? "",
    measures: inv.pvMeasures ?? "",
    slowStock: inv.pvSlowStock ?? "",
    storageFindings: inv.pvStorageFindings ?? "",
    other: inv.pvOther ?? "",
    keeperObjections: inv.keeperObjections ?? "",
    commissionConclusions: inv.commissionConclusions ?? "",
  });
  const labels: Record<keyof typeof fields, string> = {
    causes: t.pvCauses,
    measures: t.pvMeasures,
    slowStock: t.pvSlowStock,
    storageFindings: t.pvStorage,
    other: t.pvOther,
    keeperObjections: t.keeperObjections,
    commissionConclusions: t.commissionConclusions,
  };

  return (
    <fieldset disabled={!editable} className="space-y-3">
      <div className="w-48">
        <Label htmlFor="inv-pvdate">{t.pvDate}</Label>
        <DateInput id="inv-pvdate" value={pvDate} onChange={(e) => setPvDate(e.target.value)} />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {(Object.keys(fields) as (keyof typeof fields)[]).map((k) => (
          <div key={k}>
            <Label htmlFor={`inv-pv-${k}`}>{labels[k]}</Label>
            <Textarea
              id={`inv-pv-${k}`}
              rows={2}
              value={fields[k]}
              onChange={(e) => setFields({ ...fields, [k]: e.target.value })}
            />
          </div>
        ))}
      </div>
      <section>
        <h3 className="text-xs font-medium uppercase tracking-wide text-content-muted">
          {t.during}
          <Tooltip content={t.duringHint}>
            <span className="ml-1 cursor-help text-content-subtle">?</span>
          </Tooltip>
        </h3>
        <ul className="mt-1 text-sm">
          {(during.data ?? []).length === 0 && <li className="text-content-muted">{t.duringNone}</li>}
          {(during.data ?? []).map((o) => (
            <li key={o.id}>
              {t.opType[o.type as keyof typeof t.opType] ?? o.type} nr. {o.number} ·{" "}
              {roDate(o.date)}
              {o.counterparty ? ` · ${o.counterparty}` : ""}
            </li>
          ))}
        </ul>
      </section>
      {editable && (
        <div className="flex justify-end">
          <Button onClick={() => onSave({ pvDate, ...fields })}>{t.save}</Button>
        </div>
      )}
    </fieldset>
  );
}
