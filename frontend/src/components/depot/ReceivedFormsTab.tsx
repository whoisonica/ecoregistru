import { useMemo, useState, type FormEvent } from "react";
import { FileText, PenLine } from "lucide-react";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { downloadReceivedFormsRegister, useReceivedForms, useRecordReceivedForm } from "@/hooks/useReceivedForms";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate, todayIso } from "@/lib/utils";
import type { ReceivedForm, ReceivedFormInput } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useTableView } from "@/hooks/useTableView";
import { useToast } from "@/components/ui/toast";

const t = strings.weighing;
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });

type Draft = Omit<ReceivedFormInput, "quantityKg"> & { quantity: string };

const emptyDraft = (workPointId: string): Draft => ({
  workPointId,
  receivedOn: todayIso(),
  formKind: "ANEXA_3",
  formSeries: "",
  formNumber: "",
  formDate: "",
  senderName: "",
  senderCui: "",
  wasteDescription: "",
  quantity: "",
  correctionReason: "",
});

/**
 * D2.6 — registrul formularelor de transport primite, pe depozit și pe an. Se scrie, nu se rescrie: „Corectează”
 * deschide formularul completat cu rândul greșit și îl trece ca rând nou, cu motivul (registrul păstrează ambele).
 * Recepția unui transfer își trece singură formularul (D2.5).
 */
export function ReceivedFormsTab({
  canWrite,
  creating,
  onCreatingChange,
}: {
  canWrite: boolean;
  /** Butonul principal al ecranului (tasta N) deschide formularul. */
  creating: boolean;
  onCreatingChange: (open: boolean) => void;
}) {
  const workPoints = useWorkPoints();
  const depots = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  const [chosenDepot, setChosenDepot] = useState("");
  const workPointId = chosenDepot || depots[0]?.id || "";
  const [year, setYear] = useState(new Date().getFullYear());
  const { data, isLoading, isError } = useReceivedForms(workPointId, year);
  const recordMut = useRecordReceivedForm();
  const { notify } = useToast();
  const [correcting, setCorrecting] = useState<ReceivedForm | null>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [downloading, setDownloading] = useState(false);

  const rows = useMemo(() => data ?? [], [data]);
  const view = useTableView(rows, {
    searchText: (r) => [r.formNumber, r.formSeries, r.senderName, r.wasteDescription].filter(Boolean).join(" "),
  });

  const formOpen = creating || correcting != null;
  const current: Draft = draft ?? emptyDraft(workPointId);

  function openCorrection(row: ReceivedForm) {
    setCorrecting(row);
    setDraft({
      workPointId: row.workPointId,
      receivedOn: row.receivedOn,
      formKind: row.formKind,
      formSeries: row.formSeries ?? "",
      formNumber: row.formNumber,
      formDate: row.formDate ?? "",
      senderName: row.senderName,
      senderCui: row.senderCui ?? "",
      wasteDescription: row.wasteDescription ?? "",
      quantity: row.quantityKg?.toString() ?? "",
      correctionReason: "",
    });
  }

  function close() {
    setCorrecting(null);
    setDraft(null);
    onCreatingChange(false);
  }

  function set(change: Partial<Draft>) {
    setDraft({ ...current, ...change });
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const quantity = current.quantity.replace(",", ".").trim();
    const input: ReceivedFormInput = {
      workPointId: current.workPointId || workPointId,
      receivedOn: current.receivedOn,
      formKind: current.formKind,
      formSeries: current.formSeries?.trim() || null,
      formNumber: current.formNumber.trim(),
      formDate: current.formDate || null,
      senderName: current.senderName.trim(),
      senderCui: current.senderCui?.trim() || null,
      wasteDescription: current.wasteDescription?.trim() || null,
      quantityKg: quantity ? Number(quantity) : null,
      correctionReason: correcting ? current.correctionReason?.trim() || null : null,
    };
    try {
      await recordMut.mutateAsync({ correctsId: correcting?.id, input });
      notify(correcting ? t.formsCorrected : t.formsSaved, "success");
      close();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  async function downloadPdf() {
    setDownloading(true);
    try {
      await downloadReceivedFormsRegister(workPointId, year);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.formsPdfError), "error");
    } finally {
      setDownloading(false);
    }
  }

  const years = Array.from({ length: 4 }, (_, i) => new Date().getFullYear() - i);

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-wrap items-end gap-2">
          <div>
            <Label htmlFor="rf-depot">{t.formsDepot}</Label>
            <Select id="rf-depot" value={workPointId} onChange={(e) => setChosenDepot(e.target.value)}>
              {depots.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name}
                </option>
              ))}
            </Select>
          </div>
          <Select aria-label={t.formsReceivedOn} value={String(year)} onChange={(e) => setYear(Number(e.target.value))}>
            {years.map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </Select>
        </div>
        <Button variant="outline" onClick={downloadPdf} disabled={downloading || !workPointId}>
          <FileText className="mr-2 h-4 w-4" />
          {t.formsPdf}
        </Button>
      </div>

      {isError && <p className="text-sm text-state-bad-text">{t.formsLoadError}</p>}
      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <TH>{t.formsEntry}</TH>
                <TH>{t.formsReceivedOn}</TH>
                <TH>{t.formsForm}</TH>
                <TH>{t.formsSender}</TH>
                <TH className="text-right">{t.formsQuantity}</TH>
                <TH>{t.formsNotes}</TH>
                {canWrite && (
                  <TH sticky="right" className="text-right">
                    {strings.common.actions}
                  </TH>
                )}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={canWrite ? 7 : 6}
                  loading={isLoading}
                  icon={FileText}
                  title={view.emptiedBySearch ? strings.common.noResults : t.formsEmpty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.formsEmptyHint}
                />
              )}
              {view.visible.map((r) => (
                <TR key={r.id}>
                  <TD className="font-mono tabular-nums">{r.entryNo}</TD>
                  <TD className="whitespace-nowrap font-mono text-xs">{formatDate(r.receivedOn)}</TD>
                  <TD>
                    <span className="whitespace-nowrap">
                      {r.formKind === "ANEXA_2" ? t.formsKindAnexa2 : t.formsKindAnexa3} {r.formSeries ?? ""}{" "}
                      <span className="font-mono">{r.formNumber}</span>
                    </span>
                    {r.wasteDescription && <span className="block text-xs text-content-muted">{r.wasteDescription}</span>}
                  </TD>
                  <TD>
                    {r.senderName}
                    {r.senderCui && <span className="block text-xs text-content-muted">{r.senderCui}</span>}
                  </TD>
                  <TD className="text-right font-mono tabular-nums">
                    {r.quantityKg == null ? "—" : kgFormat.format(r.quantityKg)}
                  </TD>
                  <TD className="text-xs text-content-muted">
                    {r.correctsEntryNo != null && (
                      <span className="block">
                        {t.formsCorrects} {r.correctsEntryNo}: {r.correctionReason}
                      </span>
                    )}
                    {r.correctedBy != null && (
                      <span className="block">
                        {t.formsCorrectedBy} {r.correctedBy}
                      </span>
                    )}
                  </TD>
                  {canWrite && (
                    <TD sticky="right" className="text-right">
                      {r.correctedBy == null && (
                        <Button variant="ghost" size="sm" onClick={() => openCorrection(r)}>
                          <PenLine className="mr-1 h-3.5 w-3.5" />
                          {t.formsCorrect}
                        </Button>
                      )}
                    </TD>
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
          <TablePagination view={view} />
        </>
      )}

      <Dialog
        open={formOpen}
        onClose={close}
        title={correcting ? `${t.formsCorrectTitle} ${correcting.entryNo}` : t.formsTitle}
        description={t.formsHint}
        size="lg"
        busy={recordMut.isPending}
        footer={
          <>
            <Button variant="outline" onClick={close} disabled={recordMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="received-form" disabled={recordMut.isPending}>
              {recordMut.isPending ? strings.common.saving : correcting ? t.formsCorrect : t.formsNew}
            </Button>
          </>
        }
      >
        <form id="received-form" onSubmit={submit} className="space-y-3">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="rf-received">{t.formsReceivedOn}</Label>
              <DateInput id="rf-received" value={current.receivedOn} onChange={(e) => set({ receivedOn: e.target.value })} />
            </div>
            <div>
              <Label id="rf-kind-label">{t.formsKind}</Label>
              <PillGroup
                name="rf-kind"
                aria-labelledby="rf-kind-label"
                options={[
                  { value: "ANEXA_3", label: t.formsKindAnexa3 },
                  { value: "ANEXA_2", label: t.formsKindAnexa2 },
                ]}
                selected={[current.formKind]}
                onToggle={(value) => set({ formKind: value as Draft["formKind"] })}
              />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <Label htmlFor="rf-series">{t.formsSeries}</Label>
              <Input id="rf-series" maxLength={20} value={current.formSeries ?? ""} onChange={(e) => set({ formSeries: e.target.value })} />
            </div>
            <div>
              <Label htmlFor="rf-number">{t.formsNumber}</Label>
              <Input id="rf-number" maxLength={30} value={current.formNumber} onChange={(e) => set({ formNumber: e.target.value })} />
            </div>
            <div>
              <Label htmlFor="rf-date">{t.formsDate}</Label>
              <DateInput id="rf-date" value={current.formDate ?? ""} onChange={(e) => set({ formDate: e.target.value })} />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div className="sm:col-span-2">
              <Label htmlFor="rf-sender">{t.formsSender}</Label>
              <Input id="rf-sender" maxLength={255} value={current.senderName} onChange={(e) => set({ senderName: e.target.value })} />
            </div>
            <div>
              <Label htmlFor="rf-cui">{t.formsSenderCui}</Label>
              <Input id="rf-cui" maxLength={20} value={current.senderCui ?? ""} onChange={(e) => set({ senderCui: e.target.value })} />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div className="sm:col-span-2">
              <Label htmlFor="rf-waste">{t.formsWaste}</Label>
              <Input id="rf-waste" maxLength={500} value={current.wasteDescription ?? ""} onChange={(e) => set({ wasteDescription: e.target.value })} />
            </div>
            <div>
              <Label htmlFor="rf-qty">{t.formsQuantity}</Label>
              <Input id="rf-qty" inputMode="decimal" value={current.quantity} onChange={(e) => set({ quantity: e.target.value })} />
            </div>
          </div>
          {correcting && (
            <div>
              <Label htmlFor="rf-reason">{t.formsReason}</Label>
              <Textarea id="rf-reason" rows={2} maxLength={500} value={current.correctionReason ?? ""}
                onChange={(e) => set({ correctionReason: e.target.value })} />
            </div>
          )}
        </form>
      </Dialog>
    </div>
  );
}
