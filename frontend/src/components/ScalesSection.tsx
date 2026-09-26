import { useState, type FormEvent } from "react";
import { SETTINGS_CARD } from "@/components/ui/card";
import { History, Pencil, Plus, Scale as ScaleIcon, Trash2 } from "lucide-react";
import { useAddScaleEvent, useDeleteScale, useDeleteScaleEvent, useSaveScale, useScales } from "@/hooks/useScales";
import type {
  Scale,
  ScaleAccuracyClass,
  ScaleEventInput,
  ScaleEventKind,
  ScaleStatus,
  WorkPoint,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { todayIso } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DateInput } from "@/components/ui/date-input";
import { Select } from "@/components/ui/select";
import { PillGroup } from "@/components/ui/pill-group";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { ScaleStateBadge } from "@/components/depot/ScaleStateBadge";
import { ScaleFile } from "@/components/depot/ScaleFile";

const t = strings.settings.scales;
const CLASSES: ScaleAccuracyClass[] = ["I", "II", "III", "IIII"];
const STATUSES: ScaleStatus[] = ["IN_USE", "OUT_OF_USE", "SEALED"];
const KINDS: ScaleEventKind[] = ["VERIFICATION", "REPAIR", "INCIDENT"];

function emptyEvent(): ScaleEventInput {
  return {
    kind: "VERIFICATION",
    date: todayIso(),
    admitted: true,
    bulletinNumber: null,
    validUntil: null,
    laboratory: null,
    verifier: null,
    notes: null,
  };
}

/**
 * Cântarele depozitelor (D2.3). Starea (verificat, expirat, nedeclarat...) o calculează serverul din
 * fișă și istoric; aici se țin doar faptele. Scrie oricine scrie în firmă, ca la flotă.
 */
export function ScalesSection({ workPoints, canManage }: { workPoints: WorkPoint[]; canManage: boolean }) {
  const { data: scales, isLoading, isError } = useScales();
  const saveMut = useSaveScale();
  const deleteMut = useDeleteScale();
  const addEventMut = useAddScaleEvent();
  const deleteEventMut = useDeleteScaleEvent();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [editing, setEditing] = useState<Scale | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [workPointId, setWorkPointId] = useState("");
  const [name, setName] = useState("");
  const [serial, setSerial] = useState("");
  const [kind, setKind] = useState("");
  const [accuracyClass, setAccuracyClass] = useState<ScaleAccuracyClass | null>(null);
  const [division, setDivision] = useState("");
  const [commissionedOn, setCommissionedOn] = useState("");
  const [brmlDeclaredOn, setBrmlDeclaredOn] = useState("");
  const [brmlReference, setBrmlReference] = useState("");
  const [status, setStatus] = useState<ScaleStatus>("IN_USE");
  const [nameError, setNameError] = useState(false);

  const [historyOf, setHistoryOf] = useState<string | null>(null);
  const [event, setEvent] = useState<ScaleEventInput>(emptyEvent);
  const historyScale = (scales ?? []).find((s) => s.id === historyOf) ?? null;

  const view = useTableView(scales ?? [], {
    searchText: (s) => [s.name, s.serialNumber, s.kind, s.workPointName].filter(Boolean).join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });
  const depots = workPoints.filter((w) => w.active || w.id === workPointId);

  function openWith(s: Scale | null) {
    setEditing(s);
    setWorkPointId(s?.workPointId ?? (depots.length === 1 ? depots[0].id : ""));
    setName(s?.name ?? "");
    setSerial(s?.serialNumber ?? "");
    setKind(s?.kind ?? "");
    setAccuracyClass(s?.accuracyClass ?? null);
    setDivision(s?.divisionKg == null ? "" : String(s.divisionKg));
    setCommissionedOn(s?.commissionedOn ?? "");
    setBrmlDeclaredOn(s?.brmlDeclaredOn ?? "");
    setBrmlReference(s?.brmlReference ?? "");
    setStatus(s?.status ?? "IN_USE");
    setNameError(false);
    setFormOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setNameError(true);
      return;
    }
    try {
      await saveMut.mutateAsync({
        id: editing?.id ?? null,
        input: {
          workPointId,
          name: name.trim(),
          serialNumber: serial.trim() || null,
          kind: kind.trim() || null,
          accuracyClass,
          divisionKg: division.trim() ? Number(division.replace(",", ".")) : null,
          commissionedOn: commissionedOn || null,
          brmlDeclaredOn: brmlDeclaredOn || null,
          brmlReference: brmlReference.trim() || null,
          status,
        },
      });
      notify(t.saved, "success");
      setFormOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function handleDelete(s: Scale) {
    confirm({
      title: t.confirmDeleteTitle,
      message: (
        <>
          <strong className="text-content">{s.name}</strong>. {t.confirmDelete}
        </>
      ),
      confirmLabel: t.delete,
      tone: "danger",
      onConfirm: () =>
        deleteMut.mutate(s.id, {
          onSuccess: () => notify(t.deleted, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  function openHistory(s: Scale) {
    setEvent(emptyEvent());
    setHistoryOf(s.id);
  }

  async function handleAddEvent(e: FormEvent) {
    e.preventDefault();
    if (!historyOf) return;
    const verification = event.kind === "VERIFICATION";
    try {
      await addEventMut.mutateAsync({
        scaleId: historyOf,
        input: {
          ...event,
          admitted: verification ? event.admitted : null,
          bulletinNumber: verification ? event.bulletinNumber?.trim() || null : null,
          validUntil: verification && event.admitted ? event.validUntil || null : null,
          laboratory: event.laboratory?.trim() || null,
          verifier: event.verifier?.trim() || null,
          notes: event.notes?.trim() || null,
        },
      });
      notify(t.eventAdded, "success");
      setEvent(emptyEvent());
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function removeEvent(eventId: string) {
    if (!historyOf) return;
    deleteEventMut.mutate(
      { scaleId: historyOf, eventId },
      {
        onSuccess: () => notify(t.eventDeleted, "success"),
        onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
      },
    );
  }

  const columns = canManage ? 5 : 4;

  return (
    <section id="cantare" className={SETTINGS_CARD}>
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={() => openWith(null)}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      {isError && <p className="text-sm text-state-bad-text">{t.loadError}</p>}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                  {t.name}
                </SortableTH>
                <TH>{t.workPoint}</TH>
                <TH>{t.verification}</TH>
                <TH>{t.status}</TH>
                {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={columns}
                  loading={isLoading}
                  icon={ScaleIcon}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((s) => (
                <TR key={s.id}>
                  <TD>
                    <span className="font-medium text-content">{s.name}</span>
                    {(s.serialNumber || s.kind) && (
                      <span className="block text-xs text-content-muted">
                        {[s.kind, s.serialNumber && `${t.serialNumber} ${s.serialNumber}`].filter(Boolean).join(" · ")}
                      </span>
                    )}
                  </TD>
                  <TD>{s.workPointName}</TD>
                  <TD>
                    <ScaleStateBadge state={s.state} validUntil={s.validUntil} />
                  </TD>
                  <TD>{t.statusLabels[s.status]}</TD>
                  {canManage && (
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openHistory(s)}>
                          <History className="mr-1 h-3.5 w-3.5" />
                          {t.history}
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => openWith(s)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => handleDelete(s)}>
                          <Trash2 className="mr-1 h-3.5 w-3.5" />
                          {t.delete}
                        </Button>
                      </div>
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
        onClose={() => setFormOpen(false)}
        title={editing ? t.editTitle : t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setFormOpen(false)} disabled={saveMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="scale-form" disabled={saveMut.isPending}>
              {saveMut.isPending ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="scale-form" onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="sc-name">{t.name}</Label>
              <Input
                id="sc-name"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (nameError) setNameError(false);
                }}
                placeholder={t.namePlaceholder}
                autoFocus
              />
              {nameError && <p className="mt-1 text-xs text-state-bad-text">{strings.common.requiredField}</p>}
            </div>
            <div>
              <Label htmlFor="sc-depot">{t.workPoint}</Label>
              <Select id="sc-depot" value={workPointId} onChange={(e) => setWorkPointId(e.target.value)}>
                <option value="" disabled>
                  —
                </option>
                {depots.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="sc-kind">{t.kind}</Label>
              <Input id="sc-kind" value={kind} onChange={(e) => setKind(e.target.value)} placeholder={t.kindPlaceholder} />
            </div>
            <div>
              <Label htmlFor="sc-serial">{t.serialNumber}</Label>
              <Input id="sc-serial" className="font-mono" value={serial} onChange={(e) => setSerial(e.target.value)} />
            </div>
            <div>
              <Label id="sc-class-label">{t.accuracyClass}</Label>
              <PillGroup<ScaleAccuracyClass>
                name="sc-class"
                aria-labelledby="sc-class-label"
                options={CLASSES.map((c) => ({ value: c, label: c }))}
                selected={accuracyClass ? [accuracyClass] : []}
                onToggle={(value) => setAccuracyClass(value === accuracyClass ? null : value)}
              />
              <p className="mt-1 text-xs text-content-muted">{t.plateHint}</p>
            </div>
            <div>
              <Label htmlFor="sc-division">{t.division}</Label>
              <Input
                id="sc-division"
                inputMode="decimal"
                className="font-mono"
                value={division}
                onChange={(e) => setDivision(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="sc-commissioned">{t.commissionedOn}</Label>
              <DateInput id="sc-commissioned" value={commissionedOn} onChange={(e) => setCommissionedOn(e.target.value)} />
              <p className="mt-1 text-xs text-content-muted">{t.commissionedHint}</p>
            </div>
            <div>
              <Label htmlFor="sc-brml">{t.brmlDeclaredOn}</Label>
              <DateInput id="sc-brml" value={brmlDeclaredOn} onChange={(e) => setBrmlDeclaredOn(e.target.value)} />
              <p className="mt-1 text-xs text-content-muted">{t.brmlHint}</p>
            </div>
            <div>
              <Label htmlFor="sc-brml-ref">{t.brmlReference}</Label>
              <Input id="sc-brml-ref" value={brmlReference} onChange={(e) => setBrmlReference(e.target.value)} />
            </div>
            <div>
              <Label id="sc-status-label">{t.status}</Label>
              <PillGroup<ScaleStatus>
                name="sc-status"
                aria-labelledby="sc-status-label"
                options={STATUSES.map((s) => ({ value: s, label: t.statusLabels[s] }))}
                selected={[status]}
                onToggle={setStatus}
              />
            </div>
          </div>
        </form>
      </Dialog>

      <Dialog
        open={historyScale != null}
        onClose={() => setHistoryOf(null)}
        title={historyScale ? `${t.historyTitle} · ${historyScale.name}` : t.historyTitle}
        footer={
          <Button variant="outline" onClick={() => setHistoryOf(null)}>
            {strings.common.close}
          </Button>
        }
      >
        {historyScale && (
          <div className="space-y-5">
            <ScaleStateBadge state={historyScale.state} validUntil={historyScale.validUntil} />
            {/* V70 — dovada declarării la BRML, lângă starea pe care o condiționează. */}
            <div className="flex flex-wrap items-center gap-2 text-sm">
              <span className="text-content-muted">{t.brmlProof}</span>
              <ScaleFile
                scaleId={historyScale.id}
                document={historyScale.brmlProof}
                canManage={canManage}
                label={t.brmlProof}
              />
            </div>

            {historyScale.events.length === 0 ? (
              <p className="text-sm text-content-muted">{t.historyEmpty}</p>
            ) : (
              <Table>
                <THead>
                  <TR>
                    <TH>{t.eventDate}</TH>
                    <TH>{t.eventKind}</TH>
                    <TH>{t.eventValidUntil}</TH>
                    <TH>
                      <span className="sr-only">{strings.common.actions}</span>
                    </TH>
                  </TR>
                </THead>
                <TBody>
                  {historyScale.events.map((ev) => (
                    <TR key={ev.id}>
                      <TD className="font-mono tabular-nums">{formatDate(ev.date)}</TD>
                      <TD>
                        {t.eventKindLabels[ev.kind]}
                        {ev.admitted != null && ` · ${ev.admitted ? t.admitted : t.rejected}`}
                        {ev.bulletinNumber && <span className="ml-2 whitespace-nowrap font-mono text-xs">{ev.bulletinNumber}</span>}
                        {ev.kind === "VERIFICATION" && (
                          <span className="block">
                            <ScaleFile
                              scaleId={historyScale.id}
                              eventId={ev.id}
                              document={ev.bulletinFile}
                              canManage={canManage}
                              label={t.bulletinFile}
                            />
                          </span>
                        )}
                        {(ev.laboratory || ev.verifier || ev.notes) && (
                          <span className="block text-xs text-content-muted">
                            {[ev.laboratory, ev.verifier, ev.notes].filter(Boolean).join(" · ")}
                          </span>
                        )}
                      </TD>
                      <TD className="font-mono tabular-nums">{ev.validUntil ? formatDate(ev.validUntil) : "—"}</TD>
                      <TD className="text-right">
                        <Button variant="ghost" size="sm" onClick={() => removeEvent(ev.id)} aria-label={t.deleteEvent}>
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </TD>
                    </TR>
                  ))}
                </TBody>
              </Table>
            )}

            <form onSubmit={handleAddEvent} className="space-y-3 border-t border-line pt-4">
              <h3 className="eyebrow">{t.addEvent}</h3>
              <div>
                <Label id="sc-ev-kind-label">{t.eventKind}</Label>
                <PillGroup<ScaleEventKind>
                  name="sc-ev-kind"
                  aria-labelledby="sc-ev-kind-label"
                  options={KINDS.map((k) => ({ value: k, label: t.eventKindLabels[k] }))}
                  selected={[event.kind]}
                  onToggle={(kind) => setEvent({ ...event, kind })}
                />
                {event.kind !== "VERIFICATION" && <p className="mt-1 text-xs text-content-muted">{t.repairHint}</p>}
              </div>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <Label htmlFor="sc-ev-date">{t.eventDate}</Label>
                  <DateInput
                    id="sc-ev-date"
                    value={event.date}
                    onChange={(e) => setEvent({ ...event, date: e.target.value })}
                  />
                </div>
                {event.kind === "VERIFICATION" && (
                  <>
                    <div>
                      <Label id="sc-ev-result-label">{t.eventResult}</Label>
                      <PillGroup<"ADMIS" | "RESPINS">
                        name="sc-ev-result"
                        aria-labelledby="sc-ev-result-label"
                        options={[
                          { value: "ADMIS", label: t.admitted },
                          { value: "RESPINS", label: t.rejected },
                        ]}
                        selected={[event.admitted ? "ADMIS" : "RESPINS"]}
                        onToggle={(v) => setEvent({ ...event, admitted: v === "ADMIS" })}
                      />
                    </div>
                    <div>
                      <Label htmlFor="sc-ev-bulletin">{t.bulletinNumber}</Label>
                      <Input
                        id="sc-ev-bulletin"
                        className="font-mono"
                        value={event.bulletinNumber ?? ""}
                        onChange={(e) => setEvent({ ...event, bulletinNumber: e.target.value })}
                      />
                    </div>
                    {event.admitted && (
                      <div>
                        <Label htmlFor="sc-ev-until">{t.eventValidUntil}</Label>
                        <DateInput
                          id="sc-ev-until"
                          value={event.validUntil ?? ""}
                          onChange={(e) => setEvent({ ...event, validUntil: e.target.value })}
                        />
                        <p className="mt-1 text-xs text-content-muted">{t.eventValidUntilHint}</p>
                      </div>
                    )}
                    <div>
                      <Label htmlFor="sc-ev-lab">{t.laboratory}</Label>
                      <Input
                        id="sc-ev-lab"
                        value={event.laboratory ?? ""}
                        onChange={(e) => setEvent({ ...event, laboratory: e.target.value })}
                      />
                    </div>
                    <div>
                      <Label htmlFor="sc-ev-verifier">{t.verifier}</Label>
                      <Input
                        id="sc-ev-verifier"
                        value={event.verifier ?? ""}
                        onChange={(e) => setEvent({ ...event, verifier: e.target.value })}
                      />
                    </div>
                  </>
                )}
                <div className="sm:col-span-2">
                  <Label htmlFor="sc-ev-notes">{t.eventNotes}</Label>
                  <Input
                    id="sc-ev-notes"
                    value={event.notes ?? ""}
                    onChange={(e) => setEvent({ ...event, notes: e.target.value })}
                  />
                </div>
              </div>
              <Button type="submit" disabled={addEventMut.isPending}>
                <Plus className="mr-2 h-4 w-4" />
                {t.addEvent}
              </Button>
            </form>
          </div>
        )}
      </Dialog>

      {confirmDialog}
    </section>
  );
}
