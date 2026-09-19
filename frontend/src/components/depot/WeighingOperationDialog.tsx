import { useEffect, useMemo, useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { usePartners } from "@/hooks/usePartners";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { useNaturalPersons } from "@/hooks/useNaturalPersons";
import { useWasteArticles } from "@/hooks/useWasteArticles";
import { useVehicles } from "@/hooks/useVehicles";
import { useDrivers } from "@/hooks/useDrivers";
import {
  useCancelWeighingOperation,
  useCreateWeighingOperation,
  useFinalizeWeighingOperation,
  useSaveWeighingLines,
  openWeighingDocument,
  useCashCheck,
  useUpdateWeighingOperation,
} from "@/hooks/useWeighingOperations";
import { canManage, canWrite } from "@/lib/roles";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import type {
  DepotPaymentMethod,
  WasteOperationCode,
  WeighingOperation,
  WeighingOperationInput,
  WeighingOperationType,
} from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { Dialog } from "@/components/ui/dialog";
import { DateInput } from "@/components/ui/date-input";
import { FormSection } from "@/components/ui/form-section";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { todayIso } from "@/lib/utils";

const t = strings.weighing;
const codeLabels = strings.enums.wasteOperationCode;

const leiFormat = new Intl.NumberFormat("ro-RO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const lei = (value: number) => `${leiFormat.format(value)} lei`;
const round2 = (value: number) => Math.round(value * 100) / 100;

interface LineDraft {
  key: string;
  articleId: string;
  gross: string;
  tare: string;
  net: string;
  final: string;
  price: string;
  code: WasteOperationCode | "";
}

const emptyLine = (): LineDraft => ({
  key: crypto.randomUUID(),
  articleId: "",
  gross: "",
  tare: "",
  net: "",
  final: "",
  price: "",
  code: "",
});

/** Câmp gol → null; altfel numărul. Virgula se acceptă: la cântar se scrie „12,5”. */
function num(value: string): number | null {
  const cleaned = value.replace(",", ".").trim();
  if (cleaned === "") return null;
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : null;
}

/** Neto e brut − tara când amândouă sunt cântărite; altfel ce s-a scris direct. Aceeași regulă ca pe server. */
function netOf(line: LineDraft): number | null {
  const gross = num(line.gross);
  const tare = num(line.tare);
  if (gross != null && tare != null) return round2(gross - tare);
  return num(line.net);
}

/**
 * Formularul unei operațiuni de cântar (D1.15): capul, liniile și plata, într-un singur loc — cum
 * arată bonul pe care îl completează omul de la poartă.
 *
 * <p>În stilul formularelor actuale, nu pe pași: decizia proprietarului din 15.09.2026
 * („formurile de pe fe actual sunt ok”).
 *
 * <p>Se salvează în două cereri, ca pe server: capul (creare sau editare) și apoi tot cântarul odată.
 * Cât operațiunea e finalizată sau anulată, formularul e doar de citit — nu se mai atinge nimic din
 * ce a intrat în registre (D1.5).
 */
export function WeighingOperationDialog({
  open,
  type,
  operation,
  onClose,
}: {
  open: boolean;
  /** Direcția, când se creează una nouă. Pe una existentă se ia de la ea: tipul nu se schimbă. */
  type: WeighingOperationType;
  operation: WeighingOperation | null;
  onClose: () => void;
}) {
  const { user } = useAuth();
  const { data: company } = useCurrentCompany();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const createMut = useCreateWeighingOperation();
  const updateMut = useUpdateWeighingOperation();
  const linesMut = useSaveWeighingLines();
  const finalizeMut = useFinalizeWeighingOperation();
  const cancelMut = useCancelWeighingOperation();

  const workPoints = useWorkPoints();
  const partners = usePartners();
  const persons = useNaturalPersons();
  const articles = useWasteArticles();

  const direction = operation?.type ?? type;
  const inbound = direction === "IN";
  const editable = !operation || operation.status === "IN_PROGRESS";
  // Serverul spune dacă omul ăsta vede prețurile (D1.8); regula nu se reface aici. Cât firma nu s-a
  // încărcat, rubrica lipsește — mai bine o rubrică apărută târziu decât una care se ia înapoi.
  const pricesVisible = Boolean(company?.pricesVisible);
  const approver = canManage(user?.role);
  // D1.13 — documentele de transport: doar la o ieșire salvată și neanulată. Intrarea n-are formular
  // de la noi (îl face expeditorul, iar persoana fizică n-are deloc — AX).
  const printable = Boolean(operation && !inbound && operation.status !== "CANCELLED");
  const [printing, setPrinting] = useState<"anexa3" | "aviz" | "borderou" | null>(null);
  // D1.11 — borderoul: o intrare finalizată de la o persoană fizică. Poartă prețuri și, la metal, CNP-ul,
  // deci îl tipărește cine scrie și vede prețurile (serverul verifică la fel).
  const borderouReady = Boolean(
    operation &&
      operation.type === "IN" &&
      operation.naturalPersonId &&
      operation.status === "FINALIZED" &&
      canWrite(user?.role) &&
      company?.pricesVisible
  );

  // Plafonul de numerar se verifică pe ce e salvat: suma zilei vine din toate operațiunile persoanei.
  const cashCheck = useCashCheck(
    operation?.id ?? null,
    Boolean(operation?.naturalPersonId && operation.paymentMethod === "NUMERAR" && canWrite(user?.role))
  );

  async function printDocument(document: "anexa3" | "aviz" | "borderou") {
    if (!operation) return;
    setPrinting(document);
    try {
      await openWeighingDocument(operation, document);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.documentError), "error");
    } finally {
      setPrinting(null);
    }
  }

  const [date, setDate] = useState(operation?.date ?? todayIso());
  const [workPointId, setWorkPointId] = useState(operation?.workPointId ?? "");
  const [fromPerson, setFromPerson] = useState(Boolean(operation?.naturalPersonId));
  const [partnerId, setPartnerId] = useState(operation?.partnerId ?? "");
  const [personId, setPersonId] = useState(operation?.naturalPersonId ?? "");
  const [driverName, setDriverName] = useState(operation?.driverName ?? "");
  const [vehicle, setVehicle] = useState(operation?.vehicleRegistration ?? "");
  const vehicles = useVehicles();
  /**
   * D2.1 — vehiculul din flotă cu numărul scris, comparat cum îl salvează serverul (majuscule, fără
   * spații și cratime). Un număr care nu e în flotă rămâne text: mașina ocazională a unui furnizor.
   */
  const fleetVehicle = useMemo(() => {
    const typed = vehicle.replace(/[\s-]/g, "").toUpperCase();
    return typed ? (vehicles.data ?? []).find((v) => v.registration === typed) ?? null : null;
  }, [vehicle, vehicles.data]);
  const fleetDocumentsExpired = Boolean(
    fleetVehicle &&
      [fleetVehicle.itpExpiry, fleetVehicle.transportLicenseExpiry].some(
        (d) => d && d < todayIso()
      )
  );
  const drivers = useDrivers();
  /**
   * D2.2 — șoferul din listă cu numele scris (fără diferență de majuscule). Doar o potrivire unică leagă
   * fișa; un nume necunoscut sau purtat de doi rămâne text, ca șoferul ocazional al unui furnizor.
   */
  const listedDriver = useMemo(() => {
    const typed = driverName.trim().toLocaleLowerCase("ro");
    if (!typed) return null;
    const matches = (drivers.data ?? []).filter((d) => d.active && d.name.toLocaleLowerCase("ro") === typed);
    return matches.length === 1 ? matches[0] : null;
  }, [driverName, drivers.data]);
  const driverAttestationExpired = Boolean(
    listedDriver?.attestationExpiry && listedDriver.attestationExpiry < todayIso()
  );
  function changeDriverName(value: string) {
    setDriverName(value);
    const typed = value.trim().toLocaleLowerCase("ro");
    const matches = (drivers.data ?? []).filter((d) => d.active && d.name.toLocaleLowerCase("ro") === typed);
    // Mașina lui obișnuită intră doar într-o rubrică goală: ce a scris omul nu se rescrie.
    if (matches.length === 1 && matches[0].vehicleRegistration && !vehicle.trim()) {
      setVehicle(matches[0].vehicleRegistration);
    }
  }
  const [orderNumber, setOrderNumber] = useState(operation?.orderNumber ?? "");
  const [notes, setNotes] = useState(operation?.notes ?? "");
  const [truckGross, setTruckGross] = useState(operation?.grossKg?.toString() ?? "");
  const [truckTare, setTruckTare] = useState(operation?.tareKg?.toString() ?? "");
  const [payment, setPayment] = useState<DepotPaymentMethod | "">(operation?.paymentMethod ?? "");
  const [receipt, setReceipt] = useState(operation?.receiptNumber ?? "");
  const [ownHousehold, setOwnHousehold] = useState(operation?.ownHousehold ?? false);
  /**
   * Operațiunea pe care o scrie formularul. La deschidere e cea dată; la prima salvare a uneia noi
   * devine cea creată — altfel o a doua apăsare (după un refuz pe linii sau pe declarație) ar crea
   * încă o operațiune, cu încă un număr consumat.
   */
  const [savedId, setSavedId] = useState<string | null>(operation?.id ?? null);
  const [cancelling, setCancelling] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [lines, setLines] = useState<LineDraft[]>(
    operation?.lines.length
      ? operation.lines.map((l) => ({
          key: l.id,
          articleId: l.articleId ?? "",
          gross: l.grossKg?.toString() ?? "",
          tare: l.tareKg?.toString() ?? "",
          net: l.netKg?.toString() ?? "",
          final: l.finalKg?.toString() ?? "",
          price: l.unitPrice?.toString() ?? "",
          code: l.operationCode ?? "",
        }))
      : [emptyLine()]
  );

  // Un singur depozit: nu se alege ce n-are alternativă.
  const openWorkPoints = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  useEffect(() => {
    if (!workPointId && openWorkPoints.length > 0) setWorkPointId(openWorkPoints[0].id);
  }, [openWorkPoints, workPointId]);

  const allArticles = useMemo(() => articles.data ?? [], [articles.data]);
  /**
   * Toate sortimentele, nu doar cele active: o linie veche poate ține unul scos între timp din
   * catalog, iar fără el aici linia n-ar mai ști ce e — nici că e metal, nici cum se cheamă. Lista de
   * ales rămâne a celor active, plus sortimentul chiar al liniei (vezi mai jos), ca salvarea să nu-l
   * piardă tăcut.
   */
  const articleById = useMemo(() => new Map(allArticles.map((a) => [a.id, a])), [allArticles]);
  const activeArticles = useMemo(() => allArticles.filter((a) => a.active), [allArticles]);

  const totals = useMemo(() => {
    let kg = 0;
    let value = 0;
    let metalValue = 0;
    for (const line of lines) {
      const final = num(line.final) ?? netOf(line);
      const price = num(line.price);
      if (final == null) continue;
      kg += final;
      if (price == null) continue;
      const lineValue = round2(final * price);
      value += lineValue;
      if (articleById.get(line.articleId)?.metal) metalValue += lineValue;
    }
    // Cotele vin de la server pe operațiune. Pe una nouă, care încă nu există, se folosesc cele din
    // lege ca **previzualizare** — singurul loc din frontend care le știe. Nu pot minți un document:
    // la finalizare serverul recalculează și scrie ce spune `service/DepotRetentions`.
    const afmRate = operation?.afmRate ?? 0.02;
    const taxRate = operation?.incomeTaxRate ?? 0.1;
    const afm = inbound ? round2(value * afmRate) : 0;
    const tax = inbound && fromPerson ? round2(metalValue * taxRate) : 0;
    return { kg: round2(kg), value: round2(value), afm, tax, net: round2(value - afm - tax), afmRate, taxRate };
  }, [lines, articleById, inbound, fromPerson, operation]);

  const needsDeclaration =
    inbound && fromPerson && lines.some((l) => articleById.get(l.articleId)?.metal);

  const busy =
    createMut.isPending || updateMut.isPending || linesMut.isPending || finalizeMut.isPending;

  function patch(key: string, change: Partial<LineDraft>) {
    setLines((current) => current.map((l) => (l.key === key ? { ...l, ...change } : l)));
  }

  /** Tara unei linii e brutul liniei dinainte: cântărirea e succesivă, nu se scrie de două ori. */
  function addLine() {
    setLines((current) => {
      const previous = current[current.length - 1];
      const next = emptyLine();
      if (previous?.gross) next.tare = previous.gross;
      return [...current, next];
    });
  }

  function headInput(): WeighingOperationInput {
    return {
      type: direction,
      workPointId,
      date,
      partnerId: fromPerson ? null : partnerId || null,
      naturalPersonId: fromPerson ? personId || null : null,
      driverId: listedDriver?.id ?? null,
      driverName: driverName.trim() || null,
      vehicleId: fleetVehicle?.id ?? null,
      vehicleRegistration: vehicle.trim() || null,
      orderNumber: orderNumber.trim() || null,
      paymentMethod: payment || null,
      receiptNumber: receipt.trim() || null,
      ownHousehold: fromPerson ? ownHousehold : null,
      notes: notes.trim() || null,
    };
  }

  async function save(): Promise<string | null> {
    const head = headInput();
    const id = savedId
      ? (await updateMut.mutateAsync({ id: savedId, input: head })).id
      : (await createMut.mutateAsync(head)).id;
    setSavedId(id);
    const filled = lines.filter((l) => l.articleId && (netOf(l) != null || num(l.final) != null));
    if (filled.length > 0) {
      await linesMut.mutateAsync({
        id,
        input: {
          grossKg: num(truckGross),
          tareKg: num(truckTare),
          lines: filled.map((l) => ({
            articleId: l.articleId,
            grossKg: num(l.gross),
            tareKg: num(l.tare),
            // Neto se trimite doar când n-a fost calculat din brut și tara: serverul refuză un neto
            // care nu se potrivește cu cântărirea.
            netKg: num(l.gross) != null && num(l.tare) != null ? null : num(l.net),
            finalKg: num(l.final),
            unitPrice: pricesVisible ? num(l.price) : null,
            operationCode: inbound ? null : (l.code || null) as WasteOperationCode | null,
            notes: null,
          })),
        },
      });
    }
    return id;
  }

  async function handleSave() {
    try {
      await save();
      notify(t.saved, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function handleFinalize() {
    // Fără nicio linie completă, finalizarea ar fi refuzată de server — dar abia după ce a creat
    // operațiunea și i-a dat un număr. Se oprește aici.
    if (!lines.some((l) => l.articleId && (netOf(l) != null || num(l.final) != null))) {
      notify(t.linesRequired, "error");
      return;
    }
    confirm({
      title: t.confirmFinalizeTitle,
      message: t.confirmFinalize,
      confirmLabel: t.finalize,
      onConfirm: async () => {
        try {
          const id = await save();
          if (id) await finalizeMut.mutateAsync(id);
          notify(t.finalized, "success");
          onClose();
        } catch (err) {
          notify(apiErrorMessage(err, t.saveError), "error");
        }
      },
    });
  }

  async function handleCancel() {
    if (!operation || !cancelReason.trim()) return;
    try {
      await cancelMut.mutateAsync({ id: operation.id, reason: cancelReason.trim() });
      notify(t.cancelled, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  const title = operation
    ? `${inbound ? t.tabIn : t.tabOut} · ${operation.number}`
    : inbound
      ? t.newIn
      : t.newOut;

  return (
    <>
      <Dialog
        open={open}
        onClose={onClose}
        title={title}
        size="2xl"
        busy={busy}
        description={
          operation?.status === "CANCELLED" ? (
            <span className="text-state-bad-text">
              {t.cancelledBecause} {operation.cancelReason}
            </span>
          ) : undefined
        }
        footer={
          <>
            <Button variant="outline" onClick={onClose} disabled={busy}>
              {strings.common.close}
            </Button>
            {borderouReady && (
              <Button
                variant="outline"
                onClick={() => printDocument("borderou")}
                disabled={busy || printing !== null}
              >
                {printing === "borderou" ? strings.movements.avizDownloading : t.printBorderou}
              </Button>
            )}
            {printable && (
              // Pe telefon, cele două documente stau pe un rând: subsolul are deja patru butoane.
              <div className="grid grid-cols-2 gap-2 sm:flex">
                {canWrite(user?.role) && (
                  <Button
                    variant="outline"
                    onClick={() => printDocument("anexa3")}
                    disabled={busy || printing !== null}
                  >
                    {printing === "anexa3" ? strings.movements.anexa3Downloading : t.printAnexa3}
                  </Button>
                )}
                <Button
                  variant="outline"
                  onClick={() => printDocument("aviz")}
                  disabled={busy || printing !== null}
                >
                  {printing === "aviz" ? strings.movements.avizDownloading : t.printAviz}
                </Button>
              </div>
            )}
            {editable && (
              <Button variant="outline" onClick={handleSave} disabled={busy}>
                {busy ? strings.common.saving : t.save}
              </Button>
            )}
            {editable &&
              (approver ? (
                <Button onClick={handleFinalize} disabled={busy}>
                  {t.finalize}
                </Button>
              ) : (
                <p className="self-center text-xs text-content-muted">{t.finalizeHint}</p>
              ))}
            {operation && operation.status !== "CANCELLED" && approver && (
              <Button
                variant="ghost"
                className="text-state-bad-text"
                onClick={() => setCancelling(true)}
                disabled={busy}
              >
                {t.cancelOperation}
              </Button>
            )}
          </>
        }
      >
        <div className="space-y-6">
          <FormSection title={t.sectionWho}>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="wo-date">{t.date}</Label>
                <DateInput
                  id="wo-date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  disabled={!editable}
                />
              </div>
              <div>
                <Label htmlFor="wo-wp">{t.workPoint}</Label>
                <Select
                  id="wo-wp"
                  value={workPointId}
                  onChange={(e) => setWorkPointId(e.target.value)}
                  disabled={!editable}
                >
                  {openWorkPoints.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.name}
                    </option>
                  ))}
                </Select>
              </div>
            </div>

            {inbound && (
              <PillGroup
                name="wo-from"
                options={[
                  { value: "FIRM", label: t.fromCompany },
                  { value: "PERSON", label: t.fromPerson },
                ]}
                selected={[fromPerson ? "PERSON" : "FIRM"]}
                onToggle={(value) => setFromPerson(value === "PERSON")}
                disabled={!editable}
              />
            )}

            {fromPerson && inbound ? (
              <div>
                <Label htmlFor="wo-person">{t.person}</Label>
                <Select
                  id="wo-person"
                  value={personId}
                  onChange={(e) => setPersonId(e.target.value)}
                  disabled={!editable}
                >
                  <option value="">{t.personPlaceholder}</option>
                  {(persons.data ?? [])
                    .filter((p) => p.active || p.id === personId)
                    .map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                        {p.cnpLastDigits ? ` · ...${p.cnpLastDigits}` : ""}
                      </option>
                    ))}
                </Select>
                <p className="mt-1 text-xs text-content-muted">{t.personMissing}</p>
              </div>
            ) : (
              <div>
                <Label htmlFor="wo-partner">{t.partner}</Label>
                <Select
                  id="wo-partner"
                  value={partnerId}
                  onChange={(e) => setPartnerId(e.target.value)}
                  disabled={!editable}
                >
                  <option value="">{t.partnerPlaceholder}</option>
                  {(partners.data ?? [])
                    .filter((p) => p.active || p.id === partnerId)
                    .map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                        {p.cui ? ` · ${p.cui}` : ""}
                      </option>
                    ))}
                </Select>
              </div>
            )}
          </FormSection>

          <FormSection title={t.sectionTransport}>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <Label htmlFor="wo-driver">{t.driver}</Label>
                <Input
                  id="wo-driver"
                  list="wo-drivers"
                  value={driverName}
                  onChange={(e) => changeDriverName(e.target.value)}
                  disabled={!editable}
                />
                <datalist id="wo-drivers">
                  {(drivers.data ?? [])
                    .filter((d) => d.active)
                    .map((d) => (
                      <option key={d.id} value={d.name}>
                        {d.partnerName ?? d.vehicleRegistration ?? ""}
                      </option>
                    ))}
                </datalist>
                {driverName.trim() && (
                  <p className="mt-1 text-xs text-content-muted">
                    {listedDriver ? t.driverFromList : t.driverOccasional}
                  </p>
                )}
                {driverAttestationExpired && (
                  <Badge variant="danger" className="mt-1">
                    {t.driverAttestationExpired}
                  </Badge>
                )}
              </div>
              <div>
                <Label htmlFor="wo-vehicle">{t.vehicle}</Label>
                <Input
                  id="wo-vehicle"
                  list="wo-fleet"
                  className="font-mono"
                  value={vehicle}
                  onChange={(e) => setVehicle(e.target.value)}
                  disabled={!editable}
                />
                <datalist id="wo-fleet">
                  {(vehicles.data ?? [])
                    .filter((v) => v.active)
                    .map((v) => (
                      <option key={v.id} value={v.registration}>
                        {v.kind ?? ""}
                      </option>
                    ))}
                </datalist>
                {fleetVehicle && (
                  <p className="mt-1 text-xs text-content-muted">
                    {t.vehicleFromFleet}
                    {fleetVehicle.standardTareKg != null &&
                      ` · ${t.vehicleStandardTare} ${fleetVehicle.standardTareKg.toLocaleString("ro-RO")} kg`}
                  </p>
                )}
                {fleetDocumentsExpired && (
                  <Badge variant="danger" className="mt-1">
                    {t.vehicleDocumentsExpired}
                  </Badge>
                )}
              </div>
              <div>
                <Label htmlFor="wo-order">{t.orderNumber}</Label>
                <Input
                  id="wo-order"
                  value={orderNumber}
                  onChange={(e) => setOrderNumber(e.target.value)}
                  disabled={!editable}
                />
              </div>
            </div>
          </FormSection>

          <FormSection title={t.sectionScale} description={t.truckHint}>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="wo-gross">{t.truckGross}</Label>
                <Input
                  id="wo-gross"
                  inputMode="decimal"
                  value={truckGross}
                  onChange={(e) => setTruckGross(e.target.value)}
                  disabled={!editable}
                />
              </div>
              <div>
                <Label htmlFor="wo-tare">{t.truckTare}</Label>
                <Input
                  id="wo-tare"
                  inputMode="decimal"
                  value={truckTare}
                  onChange={(e) => setTruckTare(e.target.value)}
                  disabled={!editable}
                />
              </div>
            </div>

            <div className="space-y-3">
              {lines.map((line, index) => {
                const article = articleById.get(line.articleId);
                const net = netOf(line);
                const final = num(line.final) ?? net;
                return (
                  <div key={line.key} className="border border-line p-3">
                    <div className="mb-2 flex items-center justify-between gap-2">
                      <span className="font-mono text-xs uppercase tracking-wide text-content-muted">
                        {index + 1}
                        {article && (
                          <span className="ml-2 inline-flex items-center normal-case text-content">
                            <BinSwatch code={article.wasteCode} hazardous={article.hazardous} />
                            {article.wasteCode}
                          </span>
                        )}
                      </span>
                      {editable && lines.length > 1 && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setLines((c) => c.filter((l) => l.key !== line.key))}
                        >
                          <Trash2 className="mr-1 h-3.5 w-3.5" />
                          {t.lineRemove}
                        </Button>
                      )}
                    </div>
                    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
                      <div className="col-span-2">
                        <Label htmlFor={`wo-art-${line.key}`}>{t.lineArticle}</Label>
                        <Select
                          id={`wo-art-${line.key}`}
                          value={line.articleId}
                          onChange={(e) => patch(line.key, { articleId: e.target.value })}
                          disabled={!editable}
                        >
                          <option value="">—</option>
                          {activeArticles
                            .concat(
                              // Sortimentul liniei, chiar dacă a fost scos din catalog între timp.
                              article && !article.active ? [article] : []
                            )
                            .filter(
                              (a) =>
                                a.id === line.articleId ||
                                !(fromPerson && inbound && a.forbiddenFromIndividuals)
                            )
                            .map((a) => (
                              <option key={a.id} value={a.id}>
                                {a.name} · {a.wasteCode}
                              </option>
                            ))}
                        </Select>
                      </div>
                      <div>
                        <Label htmlFor={`wo-g-${line.key}`}>{t.lineGross}</Label>
                        <Input
                          id={`wo-g-${line.key}`}
                          inputMode="decimal"
                          value={line.gross}
                          onChange={(e) => patch(line.key, { gross: e.target.value })}
                          disabled={!editable}
                        />
                      </div>
                      <div>
                        <Label htmlFor={`wo-t-${line.key}`}>{t.lineTare}</Label>
                        <Input
                          id={`wo-t-${line.key}`}
                          inputMode="decimal"
                          value={line.tare}
                          onChange={(e) => patch(line.key, { tare: e.target.value })}
                          disabled={!editable}
                        />
                      </div>
                      <div>
                        <Label htmlFor={`wo-n-${line.key}`}>{t.lineNet}</Label>
                        <Input
                          id={`wo-n-${line.key}`}
                          inputMode="decimal"
                          value={line.gross && line.tare ? (net ?? "").toString() : line.net}
                          onChange={(e) => patch(line.key, { net: e.target.value })}
                          disabled={!editable || Boolean(line.gross && line.tare)}
                          className={line.gross && line.tare ? "bg-surface-sunken" : undefined}
                        />
                      </div>
                      <div>
                        <Label htmlFor={`wo-f-${line.key}`}>{t.lineFinal}</Label>
                        <Input
                          id={`wo-f-${line.key}`}
                          inputMode="decimal"
                          placeholder={net != null ? String(net) : ""}
                          value={line.final}
                          onChange={(e) => patch(line.key, { final: e.target.value })}
                          disabled={!editable}
                        />
                      </div>
                      {pricesVisible && (
                        <div>
                          <Label htmlFor={`wo-p-${line.key}`}>{t.linePrice}</Label>
                          <Input
                            id={`wo-p-${line.key}`}
                            inputMode="decimal"
                            value={line.price}
                            onChange={(e) => patch(line.key, { price: e.target.value })}
                            disabled={!editable}
                          />
                        </div>
                      )}
                      {!inbound && (
                        <div>
                          <Label htmlFor={`wo-c-${line.key}`}>{t.lineCode}</Label>
                          <Select
                            id={`wo-c-${line.key}`}
                            value={line.code}
                            onChange={(e) =>
                              patch(line.key, { code: e.target.value as WasteOperationCode | "" })
                            }
                            disabled={!editable}
                          >
                            <option value="">—</option>
                            {(Object.keys(codeLabels) as WasteOperationCode[]).map((c) => (
                              <option key={c} value={c}>
                                {codeLabels[c]}
                              </option>
                            ))}
                          </Select>
                        </div>
                      )}
                      {pricesVisible && final != null && num(line.price) != null && (
                        <div className="self-end">
                          <span className="text-xs text-content-muted">{t.lineTotal}</span>
                          <p className="font-mono tabular-nums text-content">
                            {lei(round2(final * (num(line.price) as number)))}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
              {editable && (
                <Button variant="outline" size="sm" onClick={addLine}>
                  <Plus className="mr-2 h-4 w-4" />
                  {t.lineAdd}
                </Button>
              )}
              <p className="text-xs text-content-muted">{t.lineTareHint}</p>
            </div>
          </FormSection>

          <FormSection title={t.sectionPayment}>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="wo-pay">{t.paymentMethod}</Label>
                <PillGroup
                  name="wo-pay"
                  options={[
                    { value: "VIREMENT", label: t.paymentVirement },
                    { value: "NUMERAR", label: t.paymentNumerar },
                  ]}
                  selected={payment ? [payment] : []}
                  onToggle={(value) =>
                    setPayment((current) =>
                      current === value ? "" : (value as DepotPaymentMethod)
                    )
                  }
                  disabled={!editable}
                />
              </div>
              {payment === "NUMERAR" && (
                <div>
                  <Label htmlFor="wo-receipt">{t.receiptNumber}</Label>
                  <Input
                    id="wo-receipt"
                    value={receipt}
                    onChange={(e) => setReceipt(e.target.value)}
                    disabled={!editable}
                  />
                </div>
              )}
            </div>
            {fromPerson && inbound && <p className="text-xs text-content-muted">{t.cashHint}</p>}
            {cashCheck.data?.aboveLimit && (
              <p role="alert" className="text-sm text-state-warn-text">
                {t.cashAboveLimit}
                {cashCheck.data.paidToday != null && (
                  <span className="mt-1 block font-mono">
                    {t.cashPaidToday} {lei(cashCheck.data.paidToday)}
                  </span>
                )}
              </p>
            )}

            {fromPerson && inbound && (
              <Switch
                id="wo-household"
                checked={ownHousehold}
                onChange={setOwnHousehold}
                disabled={!editable}
                label={
                  <>
                    {t.ownHousehold}
                    {needsDeclaration && !ownHousehold && (
                      <Badge variant="warning" className="ml-2">
                        {strings.common.requiredField}
                      </Badge>
                    )}
                  </>
                }
                description={t.ownHouseholdHint}
              />
            )}

            {pricesVisible && (
              <dl className="grid grid-cols-2 gap-x-6 gap-y-2 border border-line bg-surface-sunken p-3 sm:grid-cols-4">
                <Figure label={t.totalKg} value={`${totals.kg} kg`} />
                <Figure label={t.totalValue} value={lei(totals.value)} />
                {inbound && (
                  <Figure
                    label={`${t.withheldAfm} · ${Number((totals.afmRate * 100).toFixed(2))}%`}
                    value={`− ${lei(totals.afm)}`}
                  />
                )}
                {inbound && fromPerson && (
                  <Figure
                    label={`${t.withheldTax} · ${Number((totals.taxRate * 100).toFixed(2))}%`}
                    value={`− ${lei(totals.tax)}`}
                  />
                )}
                {inbound && (
                  <div className="col-span-2 sm:col-span-4">
                    <dt className="text-xs text-content-muted">
                      {t.withheldNet}
                      <Tooltip content={t.withheldHint}>
                        <span className="ml-2 cursor-help font-mono text-content-subtle">?</span>
                      </Tooltip>
                    </dt>
                    <dd className="font-mono text-lg tabular-nums text-content-strong">
                      {lei(totals.net)}
                    </dd>
                  </div>
                )}
              </dl>
            )}

            <div>
              <Label htmlFor="wo-notes">{t.notes}</Label>
              <Textarea
                id="wo-notes"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                disabled={!editable}
              />
            </div>
          </FormSection>
        </div>
      </Dialog>

      <Dialog
        open={cancelling}
        onClose={() => setCancelling(false)}
        title={t.confirmCancelTitle}
        description={t.confirmCancelBody}
        size="md"
        footer={
          <>
            <Button variant="outline" onClick={() => setCancelling(false)}>
              {strings.common.close}
            </Button>
            <Button
              className="bg-state-bad text-white hover:bg-state-bad"
              onClick={handleCancel}
              disabled={!cancelReason.trim() || cancelMut.isPending}
            >
              {t.cancelOperation}
            </Button>
          </>
        }
      >
        <Label htmlFor="wo-cancel-reason">{t.cancelReason}</Label>
        <Textarea
          id="wo-cancel-reason"
          rows={3}
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
          placeholder={t.cancelReasonPlaceholder}
        />
      </Dialog>

      {confirmDialog}
    </>
  );
}

function Figure({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-content-muted">{label}</dt>
      <dd className="font-mono tabular-nums text-content-strong">{value}</dd>
    </div>
  );
}
