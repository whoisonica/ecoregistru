import { useMemo, useState, type FormEvent } from "react";
import { Plus, Pencil, Trash2, Paperclip } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { usePartners } from "@/hooks/usePartners";
import { useWasteCodeSearch } from "@/hooks/useWasteCodes";
import {
  useMovements,
  useCreateMovement,
  useUpdateMovement,
  useDeleteMovement,
  useAddAttachment,
  useDeleteAttachment,
} from "@/hooks/useMovements";
import type {
  MovementFilters,
  PhysicalState,
  WasteMovement,
  WasteMovementInput,
  WasteOperation,
  WasteOperationCode,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Combobox, type ComboboxItem } from "@/components/ui/combobox";
import { FileDropzone } from "@/components/ui/file-dropzone";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";

const t = strings.movements;
const e = strings.enums;

const OPERATIONS: WasteOperation[] = [
  "GENERATED",
  "COLLECTED",
  "HANDED_OVER",
  "RECOVERED",
  "DISPOSED",
];
const ALL_CODES = Object.keys(e.wasteOperationCode) as WasteOperationCode[];
const R_CODES = ALL_CODES.filter((c) => c.startsWith("R"));
const D_CODES = ALL_CODES.filter((c) => c.startsWith("D"));

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function formatDate(iso: string) {
  const [y, m, d] = iso.split("-");
  return `${d}.${m}.${y}`;
}

export function MovementsPage() {
  const { user } = useAuth();
  const canWrite =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const { data: workPoints } = useWorkPoints();
  const activeWorkPoints = useMemo(
    () => (workPoints ?? []).filter((w) => w.active),
    [workPoints]
  );

  // --- Filters ---
  const [monthFilter, setMonthFilter] = useState(""); // "yyyy-MM" or ""
  const [workPointFilter, setWorkPointFilter] = useState("");

  const filters: MovementFilters = useMemo(() => {
    const f: MovementFilters = {};
    if (monthFilter) {
      const [y, m] = monthFilter.split("-");
      f.year = Number(y);
      f.month = Number(m);
    }
    if (workPointFilter) f.workPointId = workPointFilter;
    return f;
  }, [monthFilter, workPointFilter]);

  const { data: movements, isLoading, isError } = useMovements(filters);
  const deleteMut = useDeleteMovement();
  const { notify } = useToast();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WasteMovement | null>(null);

  const hasFilters = Boolean(monthFilter || workPointFilter);

  function openCreate() {
    setEditing(null);
    setDialogOpen(true);
  }

  function openEdit(m: WasteMovement) {
    setEditing(m);
    setDialogOpen(true);
  }

  function handleDelete(m: WasteMovement) {
    if (!window.confirm(t.confirmDelete)) return;
    deleteMut.mutate(m.id, {
      onSuccess: () => notify(t.deleted, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t.title}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.subtitle}</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate} disabled={activeWorkPoints.length === 0}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      {canWrite && activeWorkPoints.length === 0 && (
        <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {t.noWorkPointHint}
        </p>
      )}

      {/* Filters */}
      <div className="mt-6 flex flex-wrap items-end gap-3">
        <div>
          <Label htmlFor="filter-month">{t.filterMonth}</Label>
          <Input
            id="filter-month"
            type="month"
            value={monthFilter}
            onChange={(ev) => setMonthFilter(ev.target.value)}
            className="w-44"
          />
        </div>
        <div>
          <Label htmlFor="filter-wp">{t.filterWorkPoint}</Label>
          <Select
            id="filter-wp"
            value={workPointFilter}
            onChange={(ev) => setWorkPointFilter(ev.target.value)}
            className="w-56"
          >
            <option value="">{t.filterAll}</option>
            {activeWorkPoints.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </Select>
        </div>
        {hasFilters && (
          <Button
            variant="ghost"
            onClick={() => {
              setMonthFilter("");
              setWorkPointFilter("");
            }}
          >
            {t.clearFilters}
          </Button>
        )}
      </div>

      <section className="mt-4">
        {isLoading && <p className="text-sm text-gray-500">{strings.common.loading}</p>}
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isLoading && !isError && (
          <Table>
            <THead>
              <TR>
                <TH>{t.colDate}</TH>
                <TH>{t.colWasteCode}</TH>
                <TH>{t.colOperation}</TH>
                <TH className="text-right">{t.colQuantity}</TH>
                <TH>{t.colPartner}</TH>
                <TH>{t.colWorkPoint}</TH>
                <TH className="text-center">{t.colAttachments}</TH>
                {canWrite && <TH className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(movements ?? []).length === 0 && (
                <TR>
                  <TD colSpan={canWrite ? 8 : 7} className="text-center text-gray-400">
                    {t.empty}
                  </TD>
                </TR>
              )}
              {(movements ?? []).map((m) => (
                <TR key={m.id}>
                  <TD className="whitespace-nowrap">{formatDate(m.date)}</TD>
                  <TD>
                    <span className="font-medium text-gray-900">{m.wasteCode}</span>
                    {m.hazardous && (
                      <Badge variant="danger" className="ml-2">
                        {t.hazardous}
                      </Badge>
                    )}
                    <span className="block max-w-xs truncate text-xs text-gray-400">
                      {m.wasteCodeName}
                    </span>
                  </TD>
                  <TD>
                    {e.wasteOperation[m.operation]}
                    {m.operationCode && (
                      <span className="ml-1 text-xs text-gray-400">({m.operationCode})</span>
                    )}
                  </TD>
                  <TD className="whitespace-nowrap text-right">
                    {m.quantity} {e.unit[m.unit]}
                  </TD>
                  <TD>{m.partnerName || "—"}</TD>
                  <TD>{m.workPointName}</TD>
                  <TD className="text-center">
                    {m.attachments.length > 0 ? (
                      <span className="inline-flex items-center gap-1 text-gray-500">
                        <Paperclip className="h-3.5 w-3.5" />
                        {m.attachments.length}
                      </span>
                    ) : (
                      "—"
                    )}
                  </TD>
                  {canWrite && (
                    <TD className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(m)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-600 hover:bg-red-50"
                          onClick={() => handleDelete(m)}
                        >
                          <Trash2 className="mr-1 h-3.5 w-3.5" />
                          {strings.common.delete}
                        </Button>
                      </div>
                    </TD>
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
        )}
      </section>

      {dialogOpen && (
        <MovementFormDialog
          editing={editing}
          workPoints={activeWorkPoints.map((w) => ({ id: w.id, name: w.name }))}
          defaultWorkPointId={workPointFilter || activeWorkPoints[0]?.id}
          onClose={() => setDialogOpen(false)}
        />
      )}
    </div>
  );
}

// --- Add / edit dialog -------------------------------------------------------

interface MovementFormDialogProps {
  editing: WasteMovement | null;
  workPoints: { id: string; name: string }[];
  defaultWorkPointId?: string;
  onClose: () => void;
}

function MovementFormDialog({
  editing,
  workPoints,
  defaultWorkPointId,
  onClose,
}: MovementFormDialogProps) {
  const { notify } = useToast();
  const createMut = useCreateMovement();
  const updateMut = useUpdateMovement();
  const addAttachmentMut = useAddAttachment();
  const deleteAttachmentMut = useDeleteAttachment();
  const { data: partners } = usePartners();

  const [workPointId, setWorkPointId] = useState(editing?.workPointId ?? defaultWorkPointId ?? "");
  const [date, setDate] = useState(editing?.date ?? todayIso());
  const [wasteCode, setWasteCode] = useState<ComboboxItem | null>(
    editing
      ? { id: editing.wasteCodeId, label: `${editing.wasteCode} — ${editing.wasteCodeName}` }
      : null
  );
  const [codeQuery, setCodeQuery] = useState("");
  const [quantity, setQuantity] = useState(editing ? String(editing.quantity) : "");
  const [unit, setUnit] = useState(editing?.unit ?? "KG");
  const [operation, setOperation] = useState<WasteOperation>(editing?.operation ?? "GENERATED");
  const [physicalState, setPhysicalState] = useState<PhysicalState | "">(
    editing?.physicalState ?? ""
  );
  const [operationCode, setOperationCode] = useState<WasteOperationCode | "">(
    editing?.operationCode ?? ""
  );
  const [partnerId, setPartnerId] = useState(editing?.partnerId ?? "");
  const [documentReference, setDocumentReference] = useState(editing?.documentReference ?? "");
  const [notes, setNotes] = useState(editing?.notes ?? "");
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [error, setError] = useState<string | null>(null);

  const codeSearch = useWasteCodeSearch(codeQuery);
  const codeItems: ComboboxItem[] = (codeSearch.data ?? []).map((w) => ({
    id: w.id,
    label: `${w.code} — ${w.name}`,
    sublabel: w.hazardous ? t.hazardous : undefined,
  }));

  const requiresPartner = operation === "HANDED_OVER";
  // Every movement that takes waste off the site names its operation: Anexa 1 cap. 3 and cap. 4
  // report the quantity next to "Operaţia de valorificare"/"de eliminare" and the operator doing
  // it. A handover takes either family — what happens to the waste is the partner's operation.
  const isHandover = operation === "HANDED_OVER";
  const requiresCode = isHandover || operation === "RECOVERED" || operation === "DISPOSED";
  const codeOptions =
    operation === "RECOVERED" ? R_CODES : operation === "DISPOSED" ? D_CODES : ALL_CODES;

  const isSaving =
    createMut.isPending || updateMut.isPending || addAttachmentMut.isPending;

  function validate(): string | null {
    if (!workPointId) return strings.common.requiredField;
    if (!date) return strings.common.requiredField;
    if (!wasteCode) return t.wasteCodePlaceholder;
    const qty = Number(quantity);
    if (!quantity || Number.isNaN(qty) || qty <= 0) return t.quantity + ": " + strings.common.requiredField;
    if (requiresPartner && !partnerId) return t.partnerRequired;
    if (operation === "RECOVERED" && (!operationCode || !operationCode.startsWith("R")))
      return t.recoveryCodeRequired;
    if (operation === "DISPOSED" && (!operationCode || !operationCode.startsWith("D")))
      return t.disposalCodeRequired;
    if (isHandover && !operationCode) return t.handoverCodeRequired;
    return null;
  }

  function buildInput(): WasteMovementInput {
    return {
      clientGeneratedId: editing ? undefined : crypto.randomUUID(),
      workPointId,
      date,
      wasteCodeId: wasteCode!.id,
      quantity: Number(quantity),
      unit,
      operation,
      physicalState: physicalState || null,
      // Backend rejects operationCode on non-R/D operations, so only send it when relevant.
      operationCode: requiresCode ? (operationCode as WasteOperationCode) : null,
      partnerId: partnerId || null,
      documentReference: documentReference.trim() || null,
      notes: notes.trim() || null,
    };
  }

  async function handleSubmit(ev: FormEvent) {
    ev.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    const input = buildInput();
    try {
      const movementId = editing
        ? (await updateMut.mutateAsync({ id: editing.id, input }), editing.id)
        : (await createMut.mutateAsync(input)).id;
      for (const file of pendingFiles) {
        await addAttachmentMut.mutateAsync({ movementId, file });
      }
      notify(editing ? t.updated : t.created, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function handleDeleteAttachment(attachmentId: string) {
    if (!editing) return;
    deleteAttachmentMut.mutate(
      { movementId: editing.id, attachmentId },
      {
        onSuccess: () => notify(t.attachmentDeleted, "success"),
        onError: (err) => notify(apiErrorMessage(err, t.attachmentError), "error"),
      }
    );
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? t.editTitle : t.addTitle}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={isSaving}>
            {strings.common.cancel}
          </Button>
          <Button type="submit" form="movement-form" disabled={isSaving}>
            {isSaving ? strings.common.saving : strings.common.save}
          </Button>
        </>
      }
    >
      <form id="movement-form" onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="mv-wp">{t.filterWorkPoint}</Label>
            <Select id="mv-wp" value={workPointId} onChange={(ev) => setWorkPointId(ev.target.value)}>
              {workPoints.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="mv-date">{t.date}</Label>
            <DateInput id="mv-date" value={date} onChange={(ev) => setDate(ev.target.value)} />
          </div>
        </div>

        <div>
          <Label htmlFor="mv-code">{t.wasteCode}</Label>
          <Combobox
            id="mv-code"
            value={wasteCode}
            onSelect={setWasteCode}
            onQueryChange={setCodeQuery}
            items={codeItems}
            loading={codeSearch.isFetching}
            placeholder={t.wasteCodePlaceholder}
            searchPlaceholder={t.wasteCodeSearch}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="mv-qty">{t.quantity}</Label>
            <Input
              id="mv-qty"
              type="number"
              step="any"
              min="0"
              value={quantity}
              onChange={(ev) => setQuantity(ev.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="mv-unit">{t.unit}</Label>
            <Select id="mv-unit" value={unit} onChange={(ev) => setUnit(ev.target.value as typeof unit)}>
              <option value="KG">{e.unit.KG}</option>
              <option value="TONS">{e.unit.TONS}</option>
            </Select>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="mv-op">{t.operation}</Label>
            <Select
              id="mv-op"
              value={operation}
              onChange={(ev) => {
                setOperation(ev.target.value as WasteOperation);
                setOperationCode(""); // reset — options depend on operation
              }}
            >
              {OPERATIONS.map((op) => (
                <option key={op} value={op}>
                  {e.wasteOperation[op]}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="mv-state">{t.physicalState}</Label>
            <Select
              id="mv-state"
              value={physicalState}
              onChange={(ev) => setPhysicalState(ev.target.value as typeof physicalState)}
            >
              <option value="">{t.physicalStatePlaceholder}</option>
              {Object.entries(e.physicalState).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </div>
        </div>

        {requiresCode && (
          <div>
            <Label htmlFor="mv-code-rd">
              {t.operationCode}
              <span className="text-red-600"> *</span>
            </Label>
            <Select
              id="mv-code-rd"
              value={operationCode}
              onChange={(ev) => setOperationCode(ev.target.value as WasteOperationCode)}
            >
              <option value="">{strings.common.requiredField}</option>
              {codeOptions.map((c) => (
                <option key={c} value={c}>
                  {e.wasteOperationCode[c]}
                </option>
              ))}
            </Select>
            {isHandover && <p className="mt-1 text-xs text-gray-500">{t.handoverCodeHint}</p>}
          </div>
        )}

        <div>
          <Label htmlFor="mv-partner">
            {t.partner}
            {requiresPartner && <span className="text-red-600"> *</span>}
          </Label>
          <Select id="mv-partner" value={partnerId} onChange={(ev) => setPartnerId(ev.target.value)}>
            <option value="">{t.partnerPlaceholder}</option>
            {(partners ?? [])
              .filter((p) => p.active)
              .map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({e.partnerType[p.type]})
                </option>
              ))}
          </Select>
        </div>

        <div>
          <Label htmlFor="mv-doc">{t.documentReference}</Label>
          <Input
            id="mv-doc"
            value={documentReference}
            onChange={(ev) => setDocumentReference(ev.target.value)}
            placeholder={t.documentReferencePlaceholder}
          />
        </div>

        <div>
          <Label htmlFor="mv-notes">{t.notes}</Label>
          <Textarea
            id="mv-notes"
            value={notes}
            onChange={(ev) => setNotes(ev.target.value)}
            rows={2}
          />
        </div>

        <div>
          <Label>{t.attachments}</Label>
          {editing && editing.attachments.length > 0 && (
            <ul className="mb-2 space-y-1">
              {editing.attachments.map((a) => (
                <li
                  key={a.id}
                  className="flex items-center justify-between gap-2 rounded border border-gray-200 px-2 py-1 text-sm"
                >
                  <a
                    href={a.url}
                    target="_blank"
                    rel="noreferrer"
                    className="flex min-w-0 items-center gap-2 text-brand hover:underline"
                  >
                    <Paperclip className="h-3.5 w-3.5 shrink-0" />
                    <span className="truncate">{a.fileName}</span>
                  </a>
                  <button
                    type="button"
                    onClick={() => handleDeleteAttachment(a.id)}
                    className="shrink-0 text-gray-400 hover:text-red-600"
                    aria-label={strings.common.delete}
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </li>
              ))}
            </ul>
          )}
          <FileDropzone files={pendingFiles} onChange={setPendingFiles} disabled={isSaving} />
        </div>
      </form>
    </Dialog>
  );
}
