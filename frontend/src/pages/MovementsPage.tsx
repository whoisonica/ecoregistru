import { useMemo, useRef, useState, type FormEvent } from "react";
import { ArrowRight, Copy, Plus, Pencil, Trash2, Paperclip, FileText, Scale, Truck } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { usePartners } from "@/hooks/usePartners";
import { useDrivers } from "@/hooks/useDrivers";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useWasteCodeSearch } from "@/hooks/useWasteCodes";
import {
  useMovements,
  useCreateMovement,
  useUpdateMovement,
  useDeleteMovement,
  useRecordWeight,
  useAddAttachment,
  useDeleteAttachment,
} from "@/hooks/useMovements";
import type {
  CompanyType,
  PackagingCategory,
  PackagingMaterial,
  PackagingOrigin,
  PartnerType,
  TransportDestination,
  TransportMeans,
  WasteDestination,
  MovementFilters,
  PhysicalState,
  StorageType,
  TreatmentMethod,
  Unit,
  WasteMovement,
  WasteMovementInput,
  WasteOperation,
  WasteOperationCode,
  WasteRegister,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useUrlState } from "@/hooks/useUrlState";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Combobox, type ComboboxItem } from "@/components/ui/combobox";
import { FileDropzone } from "@/components/ui/file-dropzone";
import { Dialog } from "@/components/ui/dialog";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { FormSection } from "@/components/ui/form-section";
import { Table, THead, TBody, TR, TH, TD, SortableTH, Pagination } from "@/components/ui/table";
import { RowAction, RowActions, TableSearch } from "@/components/ui/table-toolbar";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useTableView } from "@/hooks/useTableView";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { partnerRoleLabel } from "@/components/PartnerRoleBadge";
import { canPrintAnexa3, useAnexa3Download } from "@/hooks/useAnexa3";

const t = strings.movements;
const e = strings.enums;

/** Rândurile de material ale Anexei 1 Ambalaje, în ordinea formularului. */
const PACKAGING_MATERIALS: PackagingMaterial[] = [
  "STICLA",
  "PET",
  "ALTE_PLASTICE",
  "HARTIE_CARTON",
  "ALUMINIU",
  "OTEL",
  "LEMN",
  "ALTELE",
];

/**
 * Ce material propune codul de deşeu, acolo unde îl decide singur. `15 01 04` nu apare aici
 * dinadins: „ambalaje metalice" acoperă şi aluminiul, şi oţelul, iar formularul are rând pentru
 * fiecare — deci întreabă, nu ghiceşte. `15 01 02` propune „Alte plastice", fiindcă PET-ul e
 * afirmaţia mai îngustă şi e a clientului.
 */
function suggestedPackagingMaterial(codeLabel: string): PackagingMaterial | null {
  if (codeLabel.startsWith("15 01 01")) return "HARTIE_CARTON";
  if (codeLabel.startsWith("15 01 02")) return "ALTE_PLASTICE";
  if (codeLabel.startsWith("15 01 03")) return "LEMN";
  if (codeLabel.startsWith("15 01 07")) return "STICLA";
  return null;
}

/**
 * Which operations the account may record, by company type — the same rule the backend enforces
 * through CompanyType.allowedOperations(). A plain generator has no art. 48 register, so it never
 * takes waste over; a collector keeps Anexa 1 too (art. 2 alin. (1)), so it never loses GENERATED.
 * UNCLASSIFIED_OUT is in no list: it is the state of legacy rows, written by a migration.
 */
function operationsFor(type: CompanyType | undefined): WasteOperation[] {
  // La un generator rămâne o singură opţiune, fiindcă mişcarea lui porneşte mereu de la generare:
  // ce se întâmplă cu deşeul după se alege mai jos, sub transport. Cererea specialistei, 25.08.2026:
  // „aici, la operaţiune, trebuie să rămână Generator [...] şi după, mai jos, trebuie pus în tab cu
  // Valorificare/Eliminare [...] după ce alegi la Transport spre Valorificare să apară următoarele
  // taburi cu codurile". Un cont care poate prelua de la terţi păstrează şi ieşirea directă: marfa
  // preluată n-a fost generată de el, deci nu se poate scrie ca generare urmată de predare.
  return type && type !== "GENERATOR"
    ? ["GENERATED", "COLLECTED", "RECOVERED", "DISPOSED"]
    : ["GENERATED"];
}

/**
 * Ce rubrică a formularului de mişcare e greşită. `form` e pentru ce nu ţine de o rubrică anume.
 */
type FieldErrors = Partial<
  Record<
    "workPointId" | "date" | "wasteCode" | "quantity" | "partnerId" | "operationCode" | "register" | "form",
    string
  >
>;

/** Cele două operaţiuni care scot cantitatea de pe amplasament. */
type ExitOperation = "RECOVERED" | "DISPOSED";

function isExit(operation: WasteOperation): boolean {
  return operation === "RECOVERED" || operation === "DISPOSED";
}
const ALL_CODES = Object.keys(e.wasteOperationCode) as WasteOperationCode[];
const R_CODES = ALL_CODES.filter((c) => c.startsWith("R"));
const D_CODES = ALL_CODES.filter((c) => c.startsWith("D"));

/**
 * Ce se bifează la "Destinat:" pe Anexa 3, după ce este destinatarul.
 *
 * <p>Răspunsul specialistei din 24.08.2026 (A3.1), verbatim: „când pleacă la colector se pot bifa
 * valorificării şi colectării, dacă se poate valorifica. Iar când pleacă la valorificator, doar
 * valorificării."
 *
 * <p><b>De ce după partener și nu după codul R/D.</b> Prima variantă a feliei prebifa din familia
 * codului — R la valorificare, D la eliminare — și era greșită: pe Anexa 3 primită de la Hamburger
 * Recycling, marfa pleacă la un colector sub codul 15 01 01 și caseta pretipărită e „colectării".
 * Caseta spune ce face destinatarul cu marfa, nu ce cod a ales expeditorul. De asta a fost nevoie
 * de tipul de partener „Valorificator": codul nu poate face diferența.
 *
 * <p>Eliminarea nu se prebifează: n-am întrebat-o și nu se ghicește pe un formular oficial. La un
 * generator, caseta rămâne goală până o bifează omul.
 *
 * @returns casetele sugerate, sau o listă goală când nu avem ce sugera
 */
function suggestedDestinations(
  partnerType: PartnerType | null | undefined,
  operation: WasteOperation
): TransportDestination[] {
  if (operation !== "RECOVERED") return [];
  if (partnerType === "COLLECTOR") return ["COLECTARE", "VALORIFICARE"];
  if (partnerType === "RECOVERER") return ["VALORIFICARE"];
  return [];
}

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
  // Filtrele stau în bara de adrese: se păstrează la navigare și se pot trimite ca link.
  const [monthFilter, setMonthFilter] = useUrlState("luna"); // "yyyy-MM" sau ""
  const [workPointFilter, setWorkPointFilter] = useUrlState("punct");

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
  const rows = useMemo(() => movements ?? [], [movements]);
  /**
   * Căutarea, sortarea și paginarea lucrează pe rândurile deja aduse. Filtrele de sus rămân ce
   * erau — ele restrâng *cererea*, pe an și pe punct de lucru; astea de aici așază ce a venit.
   *
   * <p>Textul în care se caută sunt coloanele pe care le-ar tasta cineva: codul, denumirea,
   * partenerul, secția, punctul de lucru, numărul documentului. Nu tot obiectul — o potrivire pe
   * un id nu ajută pe nimeni.
   */
  const view = useTableView(rows, {
    searchText: (m) =>
      [
        m.wasteCode,
        m.wasteCodeName,
        m.partnerName,
        m.internalGeneratorName,
        m.workPointName,
        m.documentReference,
        formatDate(m.date),
      ]
        .filter(Boolean)
        .join(" "),
    comparators: {
      date: (a, b) => a.date.localeCompare(b.date),
      wasteCode: (a, b) => a.wasteCode.localeCompare(b.wasteCode, "ro"),
      // Cantitatea lipsă („de cântărit") stă la coadă în ambele sensuri: nu e nici mică, nici
      // mare, e nespusă, și n-are ce căuta amestecată printre cifre.
      quantity: (a, b) => {
        if (a.quantity == null) return 1;
        if (b.quantity == null) return -1;
        return a.quantity - b.quantity;
      },
      partnerName: (a, b) => (a.partnerName ?? "").localeCompare(b.partnerName ?? "", "ro"),
      workPointName: (a, b) => a.workPointName.localeCompare(b.workPointName, "ro"),
    },
    initialSort: { key: "date", direction: "desc" },
  });
  const deleteMut = useDeleteMovement();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WasteMovement | null>(null);
  // Mișcarea de la care pornește una nouă. Separată de `editing`, fiindcă răspunde la altă
  // întrebare: de unde se iau valorile, nu ce se face cu ele la salvare.
  const [duplicating, setDuplicating] = useState<WasteMovement | null>(null);
  // Mișcarea căreia i-a venit cântarul de la destinatar; null = dialogul e închis.
  const [weighing, setWeighing] = useState<WasteMovement | null>(null);

  const hasFilters = Boolean(monthFilter || workPointFilter);
  const { download: downloadAnexa3, downloadingId } = useAnexa3Download();

  function openCreate() {
    setEditing(null);
    setDuplicating(null);
    setDialogOpen(true);
  }

  function openEdit(m: WasteMovement) {
    setEditing(m);
    setDuplicating(null);
    setDialogOpen(true);
  }

  /**
   * Aceeași marfă, același partener, alt transport.
   *
   * <p>Treizeci de predări pe lună însemnau treizeci de deschideri ale unui formular cu treizeci
   * de rubrici, dintre care aceleași douăzeci și opt de fiecare dată. Duplicarea le aduce pe
   * toate, în afară de cele două care chiar diferă: data și numărul documentului.
   */
  function openDuplicate(m: WasteMovement) {
    setEditing(null);
    setDuplicating(m);
    setDialogOpen(true);
  }

  function handleDelete(m: WasteMovement) {
    // Identitatea rândului în corpul dialogului: `window.confirm` nu putea decât un șir fix, deci
    // întreba „sigur ștergi această mișcare?" fără să spună vreodată *care*. Cu patru butoane pe
    // rând și rânduri care se aseamănă, asta e chiar informația care oprește greșeala.
    confirm({
      title: t.confirmDeleteTitle,
      message: (
        <>
          <strong className="text-content">{m.wasteCode}</strong> — {m.wasteCodeName},{" "}
          {formatDate(m.date)}
          {m.quantity != null ? `, ${m.quantity} ${e.unit[m.unit]}` : ""}
          {m.partnerName ? `, ${m.partnerName}` : ""}. {t.confirmDelete}
        </>
      ),
      tone: "danger",
      onConfirm: () =>
        deleteMut.mutate(m.id, {
          onSuccess: () => notify(t.deleted, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  return (
    <div>
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          canWrite && (
            <Button onClick={openCreate} disabled={activeWorkPoints.length === 0}>
              <Plus className="mr-2 h-4 w-4" />
              {t.add}
            </Button>
          )
        }
      />

      {canWrite && activeWorkPoints.length === 0 && (
        <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {t.noWorkPointHint}
        </p>
      )}

      {/* Filters */}
      <div className="mt-6 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <div>
          <Label htmlFor="filter-month">{t.filterMonth}</Label>
          <Input
            id="filter-month"
            type="month"
            value={monthFilter}
            onChange={(ev) => setMonthFilter(ev.target.value)}
            className="w-full sm:w-44"
          />
        </div>
        <div>
          <Label htmlFor="filter-wp">{t.filterWorkPoint}</Label>
          <Select
            id="filter-wp"
            value={workPointFilter}
            onChange={(ev) => setWorkPointFilter(ev.target.value)}
            className="w-full sm:w-56"
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
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isError && (
          <>
            <TableSearch
              value={view.query}
              onChange={view.search}
              placeholder={t.searchPlaceholder}
              matchCount={view.matchCount}
              className="mb-3"
            />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="date" sort={view.sort} onSort={view.toggleSort}>
                    {t.colDate}
                  </SortableTH>
                  <SortableTH sortKey="wasteCode" sort={view.sort} onSort={view.toggleSort}>
                    {t.colWasteCode}
                  </SortableTH>
                  <TH>{t.colOperation}</TH>
                  <SortableTH
                    sortKey="quantity"
                    sort={view.sort}
                    onSort={view.toggleSort}
                    align="right"
                  >
                    {t.colQuantity}
                  </SortableTH>
                  <SortableTH sortKey="partnerName" sort={view.sort} onSort={view.toggleSort}>
                    {t.colPartner}
                  </SortableTH>
                  <TH>{t.colInternalGenerator}</TH>
                  <SortableTH sortKey="workPointName" sort={view.sort} onSort={view.toggleSort}>
                    {t.colWorkPoint}
                  </SortableTH>
                  <TH className="text-center">{t.colAttachments}</TH>
                  {canWrite && <TH className="text-right">{strings.common.actions}</TH>}
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={canWrite ? 9 : 8}
                    loading={isLoading}
                    icon={Truck}
                    title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                    description={
                      view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                    }
                  />
                )}
                {view.visible.map((m) => (
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
                      {(m.storageType || m.treatmentMethod) && (
                        <span className="mt-0.5 block text-xs text-gray-500">
                          {[
                            m.storageType && e.storageType[m.storageType],
                            m.treatmentMethod && e.treatmentMethod[m.treatmentMethod],
                          ]
                            .filter(Boolean)
                            .join(" · ")}
                        </span>
                      )}
                    </TD>
                    <TD>
                      {/* A legacy exit is the one row on this screen that is wrong as it stands, so
                          it is red, not grey: the quantity left the site but reaches neither
                          official column of Anexa 1. Editing the row is how it gets completed. */}
                      {m.operation === "UNCLASSIFIED_OUT" ? (
                        <Badge variant="danger" title={t.missingCodeHint}>
                          {t.missingCode}
                        </Badge>
                      ) : (
                        <>
                          {e.wasteOperation[m.operation]}
                          {m.operationCode && (
                            <span className="ml-1 text-xs text-gray-400">({m.operationCode})</span>
                          )}
                        </>
                      )}
                    </TD>
                    <TD className="whitespace-nowrap text-right">
                      {m.quantity != null ? (
                        <>
                          {m.quantity} {e.unit[m.unit]}
                        </>
                      ) : (
                        <Badge variant="warning" title={t.awaitingWeighingHint}>
                          {t.awaitingWeighing}
                        </Badge>
                      )}
                    </TD>
                    <TD>
                      {m.partnerName || "—"}
                      {/* Galben, nu roșu: predarea chiar a avut loc, iar rândul nu e greșit — spre
                          deosebire de UNCLASSIFIED_OUT de mai sus, care nu intră în nicio coloană
                          oficială. Aici lipsește o condiție de legalitate a predării (OUG 92/2021
                          art. 23 alin. (1)), pe care clientul o poate lămuri cu partenerul; e
                          aceeași familie cu „De cântărit". */}
                      {m.recipientAuthorizationExpired && (
                        <Badge
                          variant="warning"
                          className="mt-0.5 block w-fit"
                          title={t.authExpiredAtHandoverHint(
                            m.recipientAuthorizationExpiry
                              ? formatDate(m.recipientAuthorizationExpiry)
                              : null,
                          )}
                        >
                          {t.authExpiredAtHandover}
                        </Badge>
                      )}
                    </TD>
                    <TD>{m.internalGeneratorName || "—"}</TD>
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
                        {/* Una afară, restul în meniu. Patru butoane cu text pe fiecare rând
                            înseamnă vreo 380px de comenzi repetate, într-un tabel care are deja
                            nouă coloane. Afară rămâne cea care e chiar de făcut acum: cântarul,
                            când lipsește cifra; altfel, editarea. */}
                        <div className="flex items-center justify-end gap-1">
                          {m.quantity == null ? (
                            <Button variant="ghost" size="sm" onClick={() => setWeighing(m)}>
                              <Scale className="mr-1 h-3.5 w-3.5" />
                              {t.recordWeight}
                            </Button>
                          ) : (
                            <Button variant="ghost" size="sm" onClick={() => openEdit(m)}>
                              <Pencil className="mr-1 h-3.5 w-3.5" />
                              {strings.common.edit}
                            </Button>
                          )}
                          <RowActions>
                            {m.quantity == null && (
                              <RowAction icon={Pencil} onClick={() => openEdit(m)}>
                                {strings.common.edit}
                              </RowAction>
                            )}
                            <RowAction icon={Copy} onClick={() => openDuplicate(m)}>
                              {t.duplicate}
                            </RowAction>
                            {canPrintAnexa3(m) && (
                              <RowAction
                                icon={FileText}
                                disabled={downloadingId === m.id}
                                onClick={() => downloadAnexa3(m)}
                              >
                                {downloadingId === m.id ? t.anexa3Downloading : t.anexa3Download}
                              </RowAction>
                            )}
                            <RowAction icon={Trash2} tone="danger" onClick={() => handleDelete(m)}>
                              {strings.common.delete}
                            </RowAction>
                          </RowActions>
                        </div>
                      </TD>
                    )}
                  </TR>
                ))}
              </TBody>
            </Table>
            <Pagination
              page={view.page}
              pageCount={view.pageCount}
              onPage={view.setPage}
              matchCount={view.matchCount}
              pageSize={view.pageSize}
            />
          </>
        )}
      </section>

      {weighing && (
        <RecordWeightDialog movement={weighing} onClose={() => setWeighing(null)} />
      )}

      {dialogOpen && (
        <MovementFormDialog
          editing={editing}
          duplicateOf={duplicating}
          workPoints={activeWorkPoints.map((w) => ({ id: w.id, name: w.name }))}
          defaultWorkPointId={workPointFilter || activeWorkPoints[0]?.id}
          onClose={() => setDialogOpen(false)}
        />
      )}

      {confirmDialog}
    </div>
  );
}

// --- "A venit cântarul" ------------------------------------------------------

/**
 * Fills in the weight the recipient sent back, and nothing else.
 *
 * <p>Asked for on 24.08.2026: the movement form greys the quantity out while "se cântărește la
 * descărcare" is ticked, so the only way to add the figure later was to untick the box — which
 * threw away the fact that the recipient did the weighing. One field, one call, and the monthly
 * line stops being provisional.
 */
function RecordWeightDialog({
  movement,
  onClose,
}: {
  movement: WasteMovement;
  onClose: () => void;
}) {
  const { notify } = useToast();
  const recordMut = useRecordWeight();
  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState<Unit>(movement.unit);

  function submit(ev: FormEvent) {
    ev.preventDefault();
    const value = Number(quantity);
    if (!Number.isFinite(value) || value <= 0) {
      notify(t.recordWeightError, "error");
      return;
    }
    recordMut.mutate(
      { id: movement.id, quantity: value, unit },
      {
        onSuccess: () => {
          notify(t.recordWeightSaved, "success");
          onClose();
        },
        onError: (err) => notify(apiErrorMessage(err, t.recordWeightError), "error"),
      }
    );
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.recordWeightTitle}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {strings.common.cancel}
          </Button>
          <Button type="submit" form="weight-form" disabled={recordMut.isPending}>
            {recordMut.isPending ? strings.common.saving : strings.common.save}
          </Button>
        </>
      }
    >
      <form id="weight-form" onSubmit={submit} className="space-y-3">
        <p className="text-sm text-gray-600">
          {movement.wasteCode} — {movement.wasteCodeName}
          {movement.partnerName ? `, ${movement.partnerName}` : ""}, {formatDate(movement.date)}
        </p>
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <Label htmlFor="wg-qty">{t.quantity}</Label>
            <Input
              id="wg-qty"
              type="number"
              step="0.001"
              min="0"
              autoFocus
              value={quantity}
              onChange={(ev) => setQuantity(ev.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="wg-unit">{t.unit}</Label>
            <Select id="wg-unit" value={unit} onChange={(ev) => setUnit(ev.target.value as Unit)}>
              <option value="KG">{e.unit.KG}</option>
              <option value="TONS">{e.unit.TONS}</option>
            </Select>
          </div>
        </div>
        <p className="text-xs text-gray-500">{t.recordWeightHint}</p>
      </form>
    </Dialog>
  );
}

// --- Add / edit dialog -------------------------------------------------------

interface MovementFormDialogProps {
  editing: WasteMovement | null;
  /**
   * Mișcarea de la care pornește una nouă. Se citește la fel ca `editing` pentru valorile de
   * pornire, dar **nu** face din formular o editare: se salvează o înregistrare nouă.
   */
  duplicateOf?: WasteMovement | null;
  workPoints: { id: string; name: string }[];
  defaultWorkPointId?: string;
  onClose: () => void;
}

function MovementFormDialog({
  editing,
  duplicateOf,
  workPoints,
  defaultWorkPointId,
  onClose,
}: MovementFormDialogProps) {
  /**
   * De unde se citesc valorile de pornire. `editing` când se editează, mișcarea-sursă când se
   * duplică — și `null` la una nouă de tot.
   *
   * <p>Peste tot mai jos, `initial` decide **ce scrie în rubrici**, iar `editing` decide **ce se
   * întâmplă la salvare**. Sunt două întrebări diferite, iar înainte era una singură.
   */
  const initial = editing ?? duplicateOf ?? null;
  const { notify } = useToast();
  const createMut = useCreateMovement();
  const updateMut = useUpdateMovement();
  const addAttachmentMut = useAddAttachment();
  const deleteAttachmentMut = useDeleteAttachment();
  const { data: partners } = usePartners();
  const { data: drivers } = useDrivers();
  const { data: company } = useCurrentCompany();

  const [workPointId, setWorkPointId] = useState(initial?.workPointId ?? defaultWorkPointId ?? "");
  const [date, setDate] = useState(editing?.date ?? todayIso());
  const [wasteCode, setWasteCode] = useState<ComboboxItem | null>(
    initial
      ? {
          id: initial.wasteCodeId,
          label: `${initial.wasteCode} — ${initial.wasteCodeName}`,
          // Same marker the search results carry, so "is this hazardous?" has one answer here.
          sublabel: initial.hazardous ? t.hazardous : undefined,
        }
      : null
  );
  const [codeQuery, setCodeQuery] = useState("");
  // Ambalaje: cele trei rubrici pe care le cere tabelul 1 al Anexei 1 Ambalaje şi pe care numai
  // mişcarea le poate purta. Se arată doar pe coduri 15 01 xx — vezi isPackagingCode.
  // Pe o mişcare nouă bifa porneşte nebifată: întrebarea e „ai pus TU ambalajul pe piaţă?", iar
  // răspunsul implicit „da" e exact ce reclama utilizatorul. Pe o mişcare veche păstrăm `null`
  // până când cineva atinge bifa, ca să nu schimbăm tăcut o cifră deja tipărită.
  const [packagingOnMarket, setPackagingOnMarket] = useState<boolean | null>(
    initial ? (initial.packagingOnMarket ?? null) : false
  );
  const [packagingMaterial, setPackagingMaterial] = useState<PackagingMaterial | "">(
    initial?.packagingMaterial ?? ""
  );
  const [packagingCategory, setPackagingCategory] = useState<PackagingCategory | "">(
    initial?.packagingCategory ?? ""
  );
  // Provenienţa de pe Anexa 3 Ambalaje. Normal se răspunde o dată, pe partener; aici e
  // suprascrierea — şi singurul loc unde se poate spune „populaţie", fiindcă o persoană fizică
  // nu e partener. Se arată numai la preluări: nota 2 întreabă de unde vine marfa preluată, deci
  // pe o generare sau pe o predare rubrica n-ar avea niciun înţeles.
  const [packagingOrigin, setPackagingOrigin] = useState<PackagingOrigin | "">(
    // Suprascrierea de pe mişcare, nu valoarea rezolvată: altfel redeschiderea unei mişcări care
    // moştenea răspunsul partenerului l-ar transforma tăcut în suprascriere proprie.
    initial?.packagingOrigin ?? ""
  );
  const [packagingReusable, setPackagingReusable] = useState(
    initial?.packagingReusable ?? false
  );
  const [packagingHazardousContent, setPackagingHazardousContent] = useState(
    initial?.packagingHazardousContent ?? false
  );
  const [quantity, setQuantity] = useState(
    initial?.quantity != null ? String(initial.quantity) : ""
  );
  const [weighedAtUnloading, setWeighedAtUnloading] = useState(
    initial?.weighedAtUnloading ?? false
  );
  const [volumeM3, setVolumeM3] = useState(
    initial?.volumeM3 != null ? String(initial.volumeM3) : ""
  );
  const [unit, setUnit] = useState(initial?.unit ?? "KG");
  /**
   * Mişcarea are două jumătăţi de când operaţiunea s-a mutat sub transport: de unde vine deşeul
   * (select-ul de sus) şi ce se întâmplă cu el ({@code fate}, blocul de după transport).
   *
   * <p>La redeschidere (sau la duplicare), o ieşire de pe Anexa 1 se citeşte înapoi ca
   * <b>generare + predare</b>: e
   * deşeul firmei, iar motorul deduce oricum generarea din ieşire (decizia 17). Una pe art. 48
   * rămâne <b>ieşire directă</b> — marfa preluată nu e generată de noi, iar a o rescrie ca generare
   * i-ar muta tăcut cantitatea pe alt formular.
   */
  const initialOwnExit =
    initial != null && isExit(initial.operation) && initial.register !== "ART_48";
  const [operation, setOperation] = useState<WasteOperation>(
    initialOwnExit ? "GENERATED" : (initial?.operation ?? "GENERATED")
  );
  const [fate, setFate] = useState<ExitOperation | "">(
    initialOwnExit ? (initial.operation as ExitOperation) : ""
  );
  const [physicalState, setPhysicalState] = useState<PhysicalState | "">(
    initial?.physicalState ?? ""
  );
  const [register, setRegister] = useState<WasteRegister | "">(initial?.register ?? "");
  const [operationCode, setOperationCode] = useState<WasteOperationCode | "">(
    initial?.operationCode ?? ""
  );
  const [storageType, setStorageType] = useState<StorageType | "">(initial?.storageType ?? "");
  const [treatmentMethod, setTreatmentMethod] = useState<TreatmentMethod | "">(
    initial?.treatmentMethod ?? ""
  );
  const [transportMeans, setTransportMeans] = useState<TransportMeans | "">(
    initial?.transportMeans ?? ""
  );
  const [wasteDestination, setWasteDestination] = useState<WasteDestination | "">(
    initial?.wasteDestination ?? ""
  );
  const [partnerId, setPartnerId] = useState(initial?.partnerId ?? "");
  const [partnerWorkPointId, setPartnerWorkPointId] = useState(
    initial?.partnerWorkPointId ?? ""
  );
  const recipientWorkPoints = useMemo(
    () => (partners ?? []).find((p) => p.id === partnerId)?.workPoints ?? [],
    [partners, partnerId]
  );
  /**
   * Transportatorii se grupează, nu se filtrează. Regula casei e că un răspuns lipsă nu restrânge
   * nimic (vezi profilul de firmă): dacă nimeni n-a bifat încă „Transportator" în Parteneri, un
   * filtru dur ar goli select-ul și ar arăta ca un defect. Așa, cei bifați stau primii și sub un
   * titlu, iar restul rămân la îndemână.
   */
  const activePartners = useMemo(
    () => (partners ?? []).filter((p) => p.active),
    [partners]
  );
  const carrierPartners = useMemo(
    () => activePartners.filter((p) => p.carrier),
    [activePartners]
  );
  const otherPartners = useMemo(
    () => activePartners.filter((p) => !p.carrier),
    [activePartners]
  );
  /**
   * „Secţia" nu se mai alege pe mişcare (25.08.2026, la cererea utilizatorului). Rubrica din cap. 2
   * al fişei se completează singură cu secţiile punctului de lucru — „Birouri, Producţie" — aşa cum
   * face decizia 19 când mişcarea nu numeşte niciuna. Valoarea existentă se **păstrează** la
   * editare: o mişcare veche care numea o secţie n-o pierde doar fiindcă i s-a deschis formularul.
   */
  const internalGeneratorId = initial?.internalGeneratorId ?? "";
  const [documentReference, setDocumentReference] = useState(
    editing?.documentReference ?? ""
  );
  const [unloadDate, setUnloadDate] = useState(initial?.unloadDate ?? "");
  // Null = "ca la firmă": alegerea de pe firmă (V19), iar în lipsa ei unitatea mișcării.
  const [anexa3Unit, setAnexa3Unit] = useState<Unit | "">(initial?.anexa3Unit ?? "");
  const [transportPartnerId, setTransportPartnerId] = useState(initial?.transportPartnerId ?? "");
  const [driverName, setDriverName] = useState(initial?.driverName ?? "");
  const [driverIdentification, setDriverIdentification] = useState(
    initial?.driverIdentification ?? ""
  );
  const [vehicleRegistration, setVehicleRegistration] = useState(
    initial?.vehicleRegistration ?? ""
  );
  /**
   * Care șofer configurat s-a ales, ca să se vadă bifat în select. `""` înseamnă „altcineva", și e
   * și implicitul la editare: ce s-a salvat pe mișcare sunt cele trei texte, nu o legătură către un
   * șofer, tocmai fiindcă formularul tipărește un instantaneu — actul de identitate de atunci,
   * mașina de atunci. Alegerea din listă doar precompletează.
   */
  const [driverId, setDriverId] = useState("");
  /**
   * Șoferii pe care îi propune formularul: ai transportatorului ales, sau ai noștri când transportăm
   * noi (`partnerId` gol pe șofer = șofer propriu).
   */
  const availableDrivers = useMemo(
    () =>
      (drivers ?? []).filter(
        (d) => d.active && (transportPartnerId ? d.partnerId === transportPartnerId : d.partnerId === null)
      ),
    [drivers, transportPartnerId]
  );
  const [transportDestinations, setTransportDestinations] = useState<TransportDestination[]>(
    initial?.transportDestinations ?? []
  );
  // Adevărat cât timp bifele sunt ale noastre, nu ale lui: atunci scrie sub ele de unde vin.
  const [destinationsPrefilled, setDestinationsPrefilled] = useState(false);
  const [notes, setNotes] = useState(initial?.notes ?? "");
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [errors, setErrors] = useState<FieldErrors>({});
  const formRef = useRef<HTMLFormElement>(null);

  const codeSearch = useWasteCodeSearch(codeQuery);

  // The waste codes on the account's authorization. With a profile answered, the picker opens on
  // those four or five instead of on the 842 of the European List, and typing still searches the
  // whole nomenclator — a code that turns up once a year must stay reachable.
  const profileWasteCodes = company?.authorizedWasteCodes ?? [];
  const searchResults = codeSearch.data ?? [];
  const shownCodes =
    profileWasteCodes.length > 0 && !codeQuery.trim() ? profileWasteCodes : searchResults;
  const codeItems: ComboboxItem[] = shownCodes.map((w) => ({
    id: w.id,
    label: `${w.code} — ${w.name}`,
    sublabel: w.hazardous ? t.hazardous : undefined,
  }));

  const operations = operationsFor(company?.type);
  /**
   * Operaţiunea care se salvează: jumătatea de jos o suprascrie pe cea de sus. „Generare +
   * transport spre valorificare" pleacă pe server ca {@code RECOVERED} — fişa n-are coloană de
   * predare (decizia 1) — iar generarea o deduce motorul din ieşire (decizia 17), deci aceeaşi
   * mişcare iese pe fişă generat 100 · valorificat 100 · stoc 0.
   */
  const effectiveOperation: WasteOperation = fate || operation;
  // A legacy row is the one case the form shows an operation nobody may choose: it has to be
  // editable, and editing it is exactly how it gets completed — alegând mai jos ce s-a întâmplat.
  const isLegacyExit = effectiveOperation === "UNCLASSIFIED_OUT";
  // Every movement that takes waste off the site names its operation: Anexa 1 cap. 3 and cap. 4
  // report the quantity next to "Operaţia de valorificare"/"de eliminare" and the operator doing
  // it — the partner, when it is not us.
  const requiresCode = isExit(effectiveOperation);
  // Blocul de sub transport apare acolo unde mişcarea porneşte de la noi: generare, sau o linie
  // veche fără cod, care exact aşa se completează. La o ieşire directă (marfă preluată) n-are ce
  // alege — operaţiunea e deja aleasă sus.
  const showsFate = operation === "GENERATED" || operation === "UNCLASSIFIED_OUT";

  /**
   * Provenienţa deşeului la ieşire. Se întreabă doar la conturile care pot prelua de la terţi:
   * la un generator pur n-ar avea sens, fiindcă tot ce iese e al lui. Fără ea, aceeaşi valorificare
   * cădea automat pe Anexa 1, deci marfa altcuiva se declara ca pusă pe piaţă de firmă.
   *
   * <p>Şi nu se mai întreabă când ieşirea vine din blocul de sub transport: acolo răspunsul e deja
   * dat sus, fiindcă „Generare" înseamnă chiar deşeul firmei. Întrebarea rămâne unde e ambiguu —
   * la ieşirea directă, care poate fi şi marfă preluată.
   */
  const asksOrigin =
    requiresCode && company != null && company.type !== "GENERATOR" && !showsFate;
  const familyCodes =
    effectiveOperation === "RECOVERED"
      ? R_CODES
      : effectiveOperation === "DISPOSED"
        ? D_CODES
        : ALL_CODES;
  // The account profile narrows the list to the operations this client actually works with, so a
  // joinery that hands cardboard to a recycler never scrolls past D7 "evacuare în mări". An empty
  // profile means the intake form has not been answered yet, and then everything stays on offer.
  // The code being edited is always kept: a movement recorded before the profile existed has to
  // remain saveable without silently losing its operation.
  // Same rule as the list button: the form covers a non-hazardous handover. The combobox marks a
  // hazardous code with its sublabel, so that is where the answer comes from.
  /**
   * Codul de ambalaje deschide blocul de mai jos. Materialul se propune din cod acolo unde codul îl
   * decide singur (15 01 01 hârtie, 15 01 02 alte plastice, 15 01 03 lemn, 15 01 07 sticlă); la
   * 15 01 04 nu se propune nimic, fiindcă acoperă şi aluminiul, şi oţelul.
   */
  const isPackagingCode = (wasteCode?.label ?? "").startsWith("15 01");
  const suggestedMaterial = suggestedPackagingMaterial(wasteCode?.label ?? "");

  const showAnexa3Section =
    isExit(effectiveOperation) &&
    Boolean(partnerId) &&
    wasteCode?.sublabel !== t.hazardous;

  const profileCodes = company?.authorizedOperationCodes ?? [];
  const codeOptions =
    profileCodes.length === 0
      ? familyCodes
      : familyCodes.filter((c) => profileCodes.includes(c) || c === initial?.operationCode);

  /**
   * Unde ajunge cantitatea, spus **înainte** de salvare.
   *
   * <p>Formularul decidea deja lucrul ăsta — din operațiune, din proveniență, din bifa de ambalaj —
   * dar nu-l arăta nicăieri: se afla după, uitându-te pe fișă. Iar alegerile care îl schimbă sunt
   * exact cele pe care omul le nimerește greșit: „preluat de la terți" scoate cantitatea de pe
   * Anexa 1 cu totul, iar bifa de ambalaj o bagă într-o a doua declarație.
   *
   * <p>Se citește din **aceleași expresii** pe care le folosește `buildInput`, nu din altele
   * paralele: o bandă care ar spune altceva decât se salvează ar fi mai rea decât nicio bandă.
   */
  const effects = useMemo(() => {
    if (!wasteCode) return [];
    const out: string[] = [];
    // Registrul, calculat exact ca în `buildInput`.
    const effectiveRegister = asksOrigin
      ? register || null
      : requiresCode && operation === "GENERATED"
        ? "ANEXA_1"
        : null;
    if (effectiveRegister === "ANEXA_1") out.push(t.effectAnexa1);
    // La preluare registrul îl forțează backendul, deci nu se citește din `register`.
    if (effectiveRegister === "ART_48" || operation === "COLLECTED") out.push(t.effectArt48);
    if (effectiveOperation === "RECOVERED") {
      out.push(
        t.effectRecovered.replace("{code}", operationCode || "—")
      );
    } else if (effectiveOperation === "DISPOSED") {
      out.push(t.effectDisposed.replace("{code}", operationCode || "—"));
    } else if (operation === "GENERATED") {
      out.push(t.effectStock);
    }
    if (isPackagingCode && packagingOnMarket !== false) out.push(t.effectPackaging);
    if (showAnexa3Section) out.push(t.effectAnexa3);
    return out;
  }, [
    wasteCode,
    asksOrigin,
    register,
    requiresCode,
    operation,
    effectiveOperation,
    operationCode,
    isPackagingCode,
    packagingOnMarket,
    showAnexa3Section,
  ]);

  const isSaving =
    createMut.isPending || updateMut.isPending || addAttachmentMut.isPending;

  /**
   * Ce e greșit, pe rubrici.
   *
   * <p>Înainte întorcea un singur șir, pus în capul unui formular care se derulează pe câteva
   * ecrane: aflai *că* e ceva greșit, nu și *unde*. Regulile sunt neatinse — aceleași condiții, în
   * aceeași ordine — doar că fiecare își spune acum numele rubricii. `form` e pentru ce nu ține de
   * o rubrică anume (linia veche fără cod R/D).
   */
  function validate(): FieldErrors {
    const errs: FieldErrors = {};
    if (!workPointId) errs.workPointId = strings.common.requiredField;
    if (!date) errs.date = strings.common.requiredField;
    if (!wasteCode) errs.wasteCode = t.wasteCodePlaceholder;
    // The recipient's weighbridge decides the figure, so the field is left empty on purpose —
    // exactly how the paper form reaches the depot.
    if (!weighedAtUnloading) {
      const qty = Number(quantity);
      if (!quantity || Number.isNaN(qty) || qty <= 0) {
        errs.quantity = strings.common.requiredField;
      }
    } else if (!partnerId) {
      errs.partnerId = t.weighingNeedsPartner;
    }
    if (effectiveOperation === "RECOVERED" && (!operationCode || !operationCode.startsWith("R")))
      errs.operationCode = t.recoveryCodeRequired;
    if (effectiveOperation === "DISPOSED" && (!operationCode || !operationCode.startsWith("D")))
      errs.operationCode = t.disposalCodeRequired;
    if (asksOrigin && !register) errs.register = t.originRequired;
    if (isLegacyExit) errs.form = t.legacyExitHint;
    return errs;
  }

  function buildInput(): WasteMovementInput {
    return {
      clientGeneratedId: editing ? undefined : crypto.randomUUID(),
      workPointId,
      date,
      wasteCodeId: wasteCode!.id,
      quantity: weighedAtUnloading ? null : Number(quantity),
      weighedAtUnloading,
      volumeM3: volumeM3 ? Number(volumeM3) : null,
      unit,
      operation: effectiveOperation,
      physicalState: physicalState || null,
      storageType: storageType || null,
      treatmentMethod: treatmentMethod || null,
      transportMeans: transportMeans || null,
      wasteDestination: wasteDestination || null,
      // Backend rejects operationCode on non-R/D operations, so only send it when relevant.
      operationCode: requiresCode ? (operationCode as WasteOperationCode) : null,
      // Numai la ieşire şi numai unde s-a întrebat. La preluare backendul o forţează pe art. 48,
      // iar la generare pe Anexa 1 — două capete fixate de lege, nu de ecran.
      // Când ieşirea s-a ales sub transport, provenienţa e deja spusă sus — „Generare" e chiar
      // deşeul firmei — şi se trimite explicit, fiindcă backendul cere registrul la orice ieşire
      // de pe un cont care ţine şi art. 48 (decizia 23). Nu se ghiceşte nimic: e alegerea omului.
      register: asksOrigin
        ? (register as WasteRegister)
        : requiresCode && operation === "GENERATED"
          ? "ANEXA_1"
          : null,
      partnerId: partnerId || null,
      internalGeneratorId: internalGeneratorId || null,
      documentReference: documentReference.trim() || null,
      notes: notes.trim() || null,
      unloadDate: unloadDate || null,
      partnerWorkPointId: partnerWorkPointId || null,
      anexa3Unit: anexa3Unit || null,
      transportPartnerId: transportPartnerId || null,
      driverName: driverName.trim() || null,
      driverIdentification: driverIdentification.trim() || null,
      vehicleRegistration: vehicleRegistration.trim() || null,
      transportDestinations,
      // Backendul le ignoră pe orice alt cod, dar nu i le trimitem degeaba.
      packagingOnMarket: isPackagingCode ? packagingOnMarket : null,
      packagingMaterial: isPackagingCode ? packagingMaterial || null : null,
      packagingCategory: isPackagingCode ? packagingCategory || null : null,
      packagingReusable: isPackagingCode ? packagingReusable : null,
      packagingOrigin:
        isPackagingCode && operation === "COLLECTED" ? packagingOrigin || null : null,
      packagingHazardousContent:
        isPackagingCode && packagingCategory === "PRIMARY" ? packagingHazardousContent : null,
    };
  }

  async function handleSubmit(ev: FormEvent) {
    ev.preventDefault();
    const found = validate();
    if (Object.keys(found).length > 0) {
      setErrors(found);
      // După ce randarea a pus semnele pe rubrici, du ochiul la prima. `data-invalid` e cârligul,
      // deci nu ținem nicio listă de referințe în paralel cu formularul.
      requestAnimationFrame(() => {
        const first = formRef.current?.querySelector<HTMLElement>('[data-invalid="true"]');
        if (!first) return;
        first.scrollIntoView({ block: "center", behavior: "smooth" });
        first.focus({ preventScroll: true });
      });
      return;
    }
    setErrors({});
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
      size="xl"
      onClose={onClose}
      title={editing ? t.editTitle : duplicateOf ? t.duplicateTitle : t.addTitle}
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
      <form ref={formRef} id="movement-form" onSubmit={handleSubmit} className="space-y-6">
        {Object.keys(errors).length > 0 && (
          <p
            role="alert"
            className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
          >
            {errors.form ?? strings.common.fixErrors}
          </p>
        )}

        <div className="rounded-lg border border-line bg-surface-muted px-3 py-2.5">
          <div className="text-xs font-semibold uppercase tracking-wide text-content-muted">
            {t.effectTitle}
          </div>
          {effects.length === 0 ? (
            <p className="mt-1 text-xs text-content-subtle">{t.effectIncomplete}</p>
          ) : (
            <ul className="mt-1.5 space-y-1">
              {effects.map((line) => (
                <li key={line} className="flex items-start gap-1.5 text-xs text-content">
                  <ArrowRight className="mt-0.5 h-3 w-3 shrink-0 text-brand" aria-hidden />
                  {line}
                </li>
              ))}
            </ul>
          )}
        </div>

        <FormSection title={t.sectionWaste}>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="mv-wp">{t.filterWorkPoint}</Label>
              <Select
                id="mv-wp"
                value={workPointId}
                onChange={(ev) => setWorkPointId(ev.target.value)}
              >
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
        </FormSection>

        <FormSection title={t.sectionQuantity}>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="mv-qty">{t.quantity}</Label>
              <Input
                id="mv-qty"
                type="number"
                step="any"
                min="0"
                value={weighedAtUnloading ? "" : quantity}
                onChange={(ev) => setQuantity(ev.target.value)}
                disabled={weighedAtUnloading}
                className={
                  weighedAtUnloading ? "bg-surface-sunken text-content-subtle" : undefined
                }
                {...invalidProps("mv-qty-err", errors.quantity)}
              />
              <FieldError id="mv-qty-err" message={errors.quantity} />
            </div>
            <div>
              <Label htmlFor="mv-unit">{t.unit}</Label>
              <Select id="mv-unit" value={unit} onChange={(ev) => setUnit(ev.target.value as typeof unit)}>
                <option value="KG">{e.unit.KG}</option>
                <option value="TONS">{e.unit.TONS}</option>
              </Select>
            </div>
          </div>

          <div>
            <label className="flex items-start gap-2 text-sm">
              <input
                type="checkbox"
                className="mt-0.5 h-4 w-4 rounded border-gray-300"
                checked={weighedAtUnloading}
                onChange={(ev) => setWeighedAtUnloading(ev.target.checked)}
              />
              <span>
                <span className="font-medium text-gray-800">{t.weighedAtUnloading}</span>
                <span className="block text-xs text-gray-500">{t.weighedAtUnloadingHint}</span>
              </span>
            </label>
            {weighedAtUnloading && (
              <div className="mt-3">
                <Label htmlFor="mv-volume">{t.volumeM3}</Label>
                <Input
                  id="mv-volume"
                  type="number"
                  step="any"
                  min="0"
                  value={volumeM3}
                  onChange={(ev) => setVolumeM3(ev.target.value)}
                />
                <p className="mt-1 text-xs text-gray-500">{t.volumeM3Hint}</p>
              </div>
            )}
          </div>
        </FormSection>

        <FormSection title={t.sectionOperation}>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="mv-op">{t.operation}</Label>
              <Select
                id="mv-op"
                value={operation}
                onChange={(ev) => {
                  setOperation(ev.target.value as WasteOperation);
                  setFate(""); // ce se întâmplă cu deşeul se alege din nou
                  setOperationCode(""); // reset — options depend on operation
                }}
              >
                {operations.map((op) => (
                  <option key={op} value={op}>
                    {e.wasteOperation[op]}
                  </option>
                ))}
                {operation === "UNCLASSIFIED_OUT" && (
                  <option value="UNCLASSIFIED_OUT">{e.wasteOperation.UNCLASSIFIED_OUT}</option>
                )}
              </Select>
              {operations.length === 1 && (
                <p className="mt-1 text-xs text-gray-500">{t.operationGeneratorHint}</p>
              )}
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

          {asksOrigin && (
            <div
              role="radiogroup"
              aria-labelledby="mv-register-title"
              className={cn(
                "rounded-md border p-3",
                errors.register ? "border-red-400 bg-red-50/40" : "border-gray-300"
              )}
              {...invalidProps("mv-register-err", errors.register)}
              tabIndex={errors.register ? -1 : undefined}
            >
              <span id="mv-register-title" className="text-sm font-medium text-gray-800">
                {t.originTitle}
                <span className="text-red-600"> *</span>
              </span>
              <p className="mt-1 text-xs text-gray-500">{t.originHint}</p>
              <div className="mt-2 space-y-2">
                {/* Fiecare opţiune îşi spune efectul: alegerea nu schimbă un câmp, ci pe ce formular
                    oficial ajunge cantitatea. */}
                <label className="flex cursor-pointer gap-2">
                  <input
                    type="radio"
                    name="mv-register"
                    className="mt-1 h-4 w-4 shrink-0"
                    checked={register === "ANEXA_1"}
                    onChange={() => setRegister("ANEXA_1")}
                  />
                  <span>
                    <span className="text-sm font-medium">{t.originOwn}</span>
                    <span className="block text-xs text-gray-500">{t.originOwnEffect}</span>
                  </span>
                </label>
                <label className="flex cursor-pointer gap-2">
                  <input
                    type="radio"
                    name="mv-register"
                    className="mt-1 h-4 w-4 shrink-0"
                    checked={register === "ART_48"}
                    onChange={() => setRegister("ART_48")}
                  />
                  <span>
                    <span className="text-sm font-medium">{t.originTakeover}</span>
                    <span className="block text-xs text-gray-500">{t.originTakeoverEffect}</span>
                  </span>
                </label>
              </div>
              <FieldError id="mv-register-err" message={errors.register} />
            </div>
          )}

          {operation === "COLLECTED" && (
            <p className="rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-xs text-gray-600">
              {t.originCollected}
            </p>
          )}

          {isLegacyExit && (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-800">
              {t.legacyExitHint}
            </p>
          )}
        </FormSection>

        <FormSection title={t.sectionHandling}>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="mv-storage">{t.storageType}</Label>
              <Select
                id="mv-storage"
                value={storageType}
                onChange={(ev) => setStorageType(ev.target.value as typeof storageType)}
              >
                <option value="">{t.nomenclatorPlaceholder}</option>
                {Object.entries(e.storageType).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="mv-treatment">{t.treatmentMethod}</Label>
              <Select
                id="mv-treatment"
                value={treatmentMethod}
                onChange={(ev) => setTreatmentMethod(ev.target.value as typeof treatmentMethod)}
              >
                <option value="">{t.nomenclatorPlaceholder}</option>
                {Object.entries(e.treatmentMethod).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </Select>
            </div>
          </div>
        </FormSection>

        <FormSection title={t.sectionTransport}>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="mv-transport-means">{t.transportMeans}</Label>
              <Select
                id="mv-transport-means"
                value={transportMeans}
                onChange={(ev) => setTransportMeans(ev.target.value as typeof transportMeans)}
              >
                <option value="">{t.nomenclatorPlaceholder}</option>
                {Object.entries(e.transportMeans).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="mv-destination">{t.wasteDestination}</Label>
              <Select
                id="mv-destination"
                value={wasteDestination}
                onChange={(ev) => setWasteDestination(ev.target.value as typeof wasteDestination)}
              >
                <option value="">{t.nomenclatorPlaceholder}</option>
                {Object.entries(e.wasteDestination).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </Select>
            </div>
          </div>

          {/* Ce se întâmplă cu deşeul stă sub transport, fiindcă de transport atârnă: „după ce alegi
              la Transport spre Valorificare să apară următoarele taburi cu codurile de valorificare,
              sau cu codurile de eliminare, în funcţie de cum o să fie transportul" (specialista,
              25.08.2026). Sus rămâne de unde vine deşeul; aici, unde ajunge. */}
          {(showsFate || requiresCode) && (
            <div className="space-y-3 rounded-md border border-gray-300 p-3">
              <div>
                <span className="text-sm font-semibold text-gray-800">{t.fateTitle}</span>
                <p className="text-xs text-gray-500">{t.fateHint}</p>
              </div>

              {showsFate && (
                <div className="space-y-2">
                  {/* Ca la provenienţă: fiecare opţiune îşi spune efectul, fiindcă alegerea nu schimbă
                      un câmp, ci coloana din fişă în care intră cantitatea. */}
                  {(
                    [
                      ["", t.fateStock, t.fateStockEffect],
                      ["RECOVERED", t.fateRecovery, t.fateRecoveryEffect],
                      ["DISPOSED", t.fateDisposal, t.fateDisposalEffect],
                    ] as const
                  ).map(([value, label, effect]) => (
                    <label key={value || "STOCK"} className="flex cursor-pointer gap-2">
                      <input
                        type="radio"
                        name="mv-fate"
                        className="mt-1 h-4 w-4 shrink-0"
                        checked={fate === value}
                        onChange={() => {
                          setFate(value);
                          setOperationCode(""); // familia de coduri se schimbă cu alegerea
                        }}
                      />
                      <span>
                        <span className="text-sm font-medium">{label}</span>
                        <span className="block text-xs text-gray-500">{effect}</span>
                      </span>
                    </label>
                  ))}
                </div>
              )}

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
                    {...invalidProps("mv-code-rd-err", errors.operationCode)}
                  >
                    <option value="">{strings.common.requiredField}</option>
                    {codeOptions.map((c) => (
                      <option key={c} value={c}>
                        {e.wasteOperationCode[c]}
                      </option>
                    ))}
                  </Select>
                  <FieldError id="mv-code-rd-err" message={errors.operationCode} />
                  <p className="mt-1 text-xs text-gray-500">{t.operationCodeHint}</p>
                </div>
              )}
            </div>
          )}
        </FormSection>

        <FormSection title={t.sectionRecipient}>
          <div>
            <Label htmlFor="mv-partner">{t.partner}</Label>
            <Select
              id="mv-partner"
              value={partnerId}
              {...invalidProps("mv-partner-err", errors.partnerId)}
              onChange={(ev) => {
                const id = ev.target.value;
                setPartnerId(id);
                // Punctul de lucru e al partenerului: dacă se schimbă partenerul, alegerea veche
                // nu mai are ce căuta pe formular.
                setPartnerWorkPointId("");
                // Se sugerează doar peste o rubrică neatinsă: o bifă pusă de om nu se rescrie,
                // fiindcă el știe despre transportul ăsta ce nu știm noi.
                if (transportDestinations.length === 0) {
                  const chosen = (partners ?? []).find((x) => x.id === id);
                  const suggested = suggestedDestinations(chosen?.type, effectiveOperation);
                  if (suggested.length > 0) {
                    setTransportDestinations(suggested);
                    setDestinationsPrefilled(true);
                  }
                }
              }}
            >
              <option value="">{t.partnerPlaceholder}</option>
              {(partners ?? [])
                .filter((p) => p.active)
                .map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({partnerRoleLabel(p)})
                  </option>
                ))}
            </Select>
            <FieldError id="mv-partner-err" message={errors.partnerId} />
            <p className="mt-1 text-xs text-gray-500">{t.partnerHint}</p>
          </div>

          {/* Punctul de lucru al destinatarului — numai când partenerul are mai multe. Cu unul
              singur nu e nimic de ales, iar Anexa 3 îl scrie oricum pe acela. */}
          {recipientWorkPoints.length > 1 && (
            <div>
              <Label htmlFor="mv-partner-wp">{t.partnerWorkPoint}</Label>
              <Select
                id="mv-partner-wp"
                value={partnerWorkPointId}
                onChange={(ev) => setPartnerWorkPointId(ev.target.value)}
              >
                <option value="">—</option>
                {recipientWorkPoints.map((wp) => (
                  <option key={wp.id} value={wp.id}>
                    {wp.name ? `${wp.name}, ${wp.address}` : wp.address}
                  </option>
                ))}
              </Select>
              <p className="mt-1 text-xs text-gray-500">{t.partnerWorkPointHint}</p>
            </div>
          )}
        </FormSection>

        {isPackagingCode && (
          <div className="space-y-3 rounded-md border border-emerald-200 bg-emerald-50/50 p-3">
            <div>
              <span className="text-sm font-semibold text-gray-800">{t.packagingSection}</span>
              <p className="text-xs text-gray-500">{t.packagingSectionHint}</p>
            </div>
            <label className="flex cursor-pointer items-start gap-2">
              <input
                type="checkbox"
                className="mt-0.5 h-4 w-4 shrink-0"
                checked={packagingOnMarket !== false}
                onChange={(ev) => setPackagingOnMarket(ev.target.checked)}
              />
              <span>
                <span className="text-sm font-medium text-gray-800">{t.packagingOnMarket}</span>
                <span className="block text-xs text-gray-500">{t.packagingOnMarketHint}</span>
              </span>
            </label>

            {operation === "COLLECTED" && (
              <div>
                <Label htmlFor="mv-pk-origin">{strings.packagingOrigin.label}</Label>
                <Select
                  id="mv-pk-origin"
                  value={packagingOrigin}
                  onChange={(ev) => setPackagingOrigin(ev.target.value as PackagingOrigin | "")}
                >
                  <option value="">{strings.packagingOrigin.fromPartner}</option>
                  <option value="POPULATIE">{strings.packagingOrigin.POPULATIE}</option>
                  <option value="GENERATOR_PJ">{strings.packagingOrigin.GENERATOR_PJ}</option>
                  <option value="COLECTOR">{strings.packagingOrigin.COLECTOR}</option>
                  <option value="COMERCIANT">{strings.packagingOrigin.COMERCIANT}</option>
                </Select>
                <p className="mt-1 text-xs text-gray-500">{strings.packagingOrigin.hintMovement}</p>
              </div>
            )}

            {packagingOnMarket === null && (
              <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
                {t.packagingLegacy}
              </p>
            )}

            {/* Rubricile de mai jos dau rândul şi coloana din tabelul 1, deci n-au sens dacă
                mişcarea nu ajunge în tabel. */}
            {packagingOnMarket !== false && (
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="mv-pk-material">{t.packagingMaterial}</Label>
                <Select
                  id="mv-pk-material"
                  value={packagingMaterial}
                  onChange={(ev) =>
                    setPackagingMaterial(ev.target.value as PackagingMaterial | "")
                  }
                >
                  <option value="">
                    {suggestedMaterial
                      ? `${e.packagingMaterial[suggestedMaterial]} ${t.packagingFromCode}`
                      : t.packagingMaterialPlaceholder}
                  </option>
                  {PACKAGING_MATERIALS.map((m) => (
                    <option key={m} value={m}>
                      {e.packagingMaterial[m]}
                    </option>
                  ))}
                </Select>
                {!suggestedMaterial && !packagingMaterial && (
                  <p className="mt-1 text-xs text-amber-700">{t.packagingMaterialNeeded}</p>
                )}
              </div>
              <div>
                <Label htmlFor="mv-pk-category">{t.packagingCategory}</Label>
                <Select
                  id="mv-pk-category"
                  value={packagingCategory}
                  onChange={(ev) => setPackagingCategory(ev.target.value as PackagingCategory | "")}
                >
                  <option value="">{t.packagingCategoryPlaceholder}</option>
                  <option value="SALES">{e.packagingCategory.SALES}</option>
                  <option value="PRIMARY">{e.packagingCategory.PRIMARY}</option>
                  <option value="SECONDARY">{e.packagingCategory.SECONDARY}</option>
                </Select>
                <p className="mt-1 text-xs text-gray-500">{t.packagingCategoryHint}</p>
              </div>
            </div>
            )}
            {packagingOnMarket !== false && (
            <div className="flex flex-wrap gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="h-4 w-4"
                  checked={packagingReusable}
                  onChange={(ev) => setPackagingReusable(ev.target.checked)}
                />
                {t.packagingReusable}
              </label>
              {/* Nota 3: ambalajele cu conţinut periculos sunt tot ambalaje primare. Bifa apare
                  numai acolo, ca să nu se poată răspunde ceva ce formularul n-ar putea tipări. */}
              {packagingCategory === "PRIMARY" && (
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    className="h-4 w-4"
                    checked={packagingHazardousContent}
                    onChange={(ev) => setPackagingHazardousContent(ev.target.checked)}
                  />
                  {t.packagingHazardous}
                </label>
              )}
            </div>
            )}
          </div>
        )}

        {/* Anexa 3 e dovada predării, deci n-are cum să existe fără destinatar. Până acum condiţia
            era tăcută: alegeai codul, secţiunea nu apărea, şi nu scria nicăieri de ce. */}
        {requiresCode && !showAnexa3Section && wasteCode?.sublabel !== t.hazardous && (
          <p className="rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-xs text-gray-600">
            {t.anexa3NeedsPartner}
          </p>
        )}

        {showAnexa3Section && (
          <div className="space-y-3 rounded-md border border-gray-200 bg-gray-50 p-3">
            <div>
              <span className="text-sm font-semibold text-gray-800">{t.anexa3Section}</span>
              <p className="text-xs text-gray-500">{t.anexa3SectionHint}</p>
            </div>
            <p className="text-xs text-gray-500">{t.anexa3Copies}</p>
            <div className="grid gap-3 sm:grid-cols-3">
              {/* Ordinea cerută pe 24.08: încărcarea întâi, descărcarea după — ca pe formular.
                  Încărcarea nu e un câmp propriu: e data mișcării, și o singură sursă de adevăr
                  e tot ce ne trebuie. Se arată ca să se vadă ce se tipărește. */}
              <div>
                <Label htmlFor="mv-load">{t.loadDate}</Label>
                <DateInput id="mv-load" value={date} disabled className="bg-gray-100 text-gray-500" />
                <p className="mt-1 text-xs text-gray-500">{t.loadDateHint}</p>
              </div>
              <div>
                <Label htmlFor="mv-unload">{t.unloadDate}</Label>
                <DateInput
                  id="mv-unload"
                  value={unloadDate}
                  onChange={(ev) => setUnloadDate(ev.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="mv-anexa3-unit">{t.anexa3Unit}</Label>
                <Select
                  id="mv-anexa3-unit"
                  value={anexa3Unit}
                  onChange={(ev) => setAnexa3Unit(ev.target.value as Unit | "")}
                >
                  <option value="">{t.anexa3UnitCompany}</option>
                  <option value="KG">{e.unit.KG}</option>
                  <option value="TONS">{e.unit.TONS}</option>
                </Select>
                <p className="mt-1 text-xs text-gray-500">{t.anexa3UnitHint}</p>
              </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {/* Transportatorul și șoferul stau alături: alegerea firmei decide ce șoferi se
                  propun, iar alăturarea face legătura vizibilă fără s-o explice nimeni. */}
              <div>
                <Label htmlFor="mv-carrier">{t.transportPartner}</Label>
                <Select
                  id="mv-carrier"
                  value={transportPartnerId}
                  onChange={(ev) => {
                    setTransportPartnerId(ev.target.value);
                    // Șoferii sunt ai transportatorului: schimbi firma, alegerea nu mai e a ei.
                    // Textul deja scris rămâne — poate a fost scris de mână, și nu se șterge munca.
                    setDriverId("");
                  }}
                >
                  <option value="">{t.transportPartnerPlaceholder}</option>
                  {carrierPartners.length > 0 && (
                    <optgroup label={t.carrierGroup}>
                      {carrierPartners.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name}
                        </option>
                      ))}
                    </optgroup>
                  )}
                  {otherPartners.length > 0 && (
                    <optgroup label={carrierPartners.length > 0 ? t.otherPartnersGroup : t.allPartnersGroup}>
                      {otherPartners.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name}
                        </option>
                      ))}
                    </optgroup>
                  )}
                </Select>
                <p className="mt-1 text-xs text-gray-500">
                  {carrierPartners.length > 0 ? t.transportPartnerHint : t.transportPartnerNoneHint}
                </p>
              </div>
              <div>
                <Label htmlFor="mv-driver-pick">{t.driverPick}</Label>
                <Select
                  id="mv-driver-pick"
                  value={driverId}
                  onChange={(ev) => {
                    const picked = availableDrivers.find((d) => d.id === ev.target.value);
                    setDriverId(ev.target.value);
                    if (picked) {
                      setDriverName(picked.name);
                      setDriverIdentification(picked.identification ?? "");
                      setVehicleRegistration(picked.vehicleRegistration ?? "");
                    }
                  }}
                  disabled={availableDrivers.length === 0}
                >
                  <option value="">{t.driverPickFreeText}</option>
                  {availableDrivers.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                      {d.vehicleRegistration ? ` — ${d.vehicleRegistration}` : ""}
                    </option>
                  ))}
                </Select>
                <p className="mt-1 text-xs text-gray-500">
                  {availableDrivers.length > 0
                    ? t.driverPickHint
                    : transportPartnerId
                      ? t.driverPickNoneCarrier
                      : t.driverPickNoneOwn}
                </p>
              </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <Label htmlFor="mv-driver">{t.driverName}</Label>
                <Input
                  id="mv-driver"
                  value={driverName}
                  onChange={(ev) => setDriverName(ev.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="mv-driver-id">{t.driverIdentification}</Label>
                <Input
                  id="mv-driver-id"
                  value={driverIdentification}
                  onChange={(ev) => setDriverIdentification(ev.target.value)}
                  placeholder={t.driverIdentificationPlaceholder}
                />
              </div>
              <div>
                <Label htmlFor="mv-plate">{t.vehicleRegistration}</Label>
                <Input
                  id="mv-plate"
                  value={vehicleRegistration}
                  onChange={(ev) => setVehicleRegistration(ev.target.value)}
                />
              </div>
            </div>
            <div>
              <span className="block text-sm font-medium text-gray-700">
                {t.transportDestinations}
              </span>
              <p className="text-xs text-gray-500">
                {destinationsPrefilled ? t.destinationsPrefilled : t.transportDestinationsHint}
              </p>
              <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1">
                {(Object.keys(e.transportDestination) as TransportDestination[]).map((d) => (
                  <label key={d} className="flex items-center gap-1.5 text-sm">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-gray-300"
                      checked={transportDestinations.includes(d)}
                      onChange={() => {
                        setDestinationsPrefilled(false);
                        setTransportDestinations((prev) =>
                          prev.includes(d) ? prev.filter((x) => x !== d) : [...prev, d]
                        );
                      }}
                    />
                    {e.transportDestination[d]}
                  </label>
                ))}
              </div>
            </div>
          </div>
        )}

        <FormSection title={t.sectionDocument}>
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
        </FormSection>

        <FormSection title={t.sectionAttachments}>
          <div>
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
        </FormSection>
      </form>
    </Dialog>
  );
}
