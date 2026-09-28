import { useQuery, useQueryClient } from "@tanstack/react-query";
import { strings } from "@web/strings";
import type {
  PackagingCategory,
  PackagingMaterial,
  Partner,
  PartnerType,
  PhysicalState,
  StorageType,
  TransportDestination,
  TransportMeans,
  Unit,
  WasteCode,
  WasteDestination,
  WasteMovement,
  WasteMovementInput,
  WasteOperationCode,
} from "@web/types";
import {
  destinationsFor,
  PACKAGING_MATERIALS,
  suggestedDestinations,
  suggestedPackagingMaterial,
} from "@/components/movements/movementRules";
import { isValidCnp } from "@/lib/cnp";
import { declarationOf } from "@/lib/deadlines";
import * as Crypto from "expo-crypto";
import { Stack, useLocalSearchParams, useRouter } from "expo-router";
import { extractTextFromImage, isSupported } from "expo-text-extractor";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  View,
} from "react-native";

import * as api from "../src/api";
import { parseAviz, cuiDigits, type AvizReading } from "../src/aviz/parse";
import { canWrite } from "../src/auth";
import { Bin } from "../src/components/Bin";
import { DateField, dateLabel } from "../src/components/DateField";
import { Fold } from "../src/components/Fold";
import { Field, Input, MultiPills, Pills, PrimaryButton } from "../src/components/Form";
import { Icon } from "../src/components/Icon";
import { OfflineBand } from "../src/components/OfflineBand";
import { QuantityField } from "../src/components/QuantityField";
import { Chip, Group, Note, rowStyles, SectionHead } from "../src/components/Rows";
import { StepFoot } from "../src/components/StepFoot";
import { StepHead } from "../src/components/StepHead";
import { formatDate, formatQuantity } from "../src/format";
import { useHandoverData } from "../src/handover";
import type { HandoverDraft } from "../src/handoverDraft";
import {
  codeInProfile,
  initialPackagingOnMarket,
  parseQuantity,
  queuedToMovement,
  weightRecorded,
  yearInRange,
} from "../src/handoverForm";
import { canSaveEarly, STEP_FIELDS, stepBlocked, stepNote, stepOf, type FieldKey, type Step } from "../src/handoverSteps";
import { haptic } from "../src/haptics";
import { lastSaved, rememberSaved } from "../src/lastSaved";
import { editBody } from "../src/movementEdit";
import { confirmDeclared } from "../src/declared";
import { reportError } from "../src/monitoring";
import { useOnline } from "../src/online";
import { db, drain, enqueue, get as getQueued, localPhoto, photoExists, resubmit } from "../src/outbox";
import { PhotoMissingError } from "../src/outboxRules";
import { useSession } from "../src/session";
import { colors, fonts, radius } from "../src/theme";
import { dropDraft, writeDraft } from "../src/useDraft";
import { loadDraft } from "../src/handoverDraft";

const t = strings.movements;
const m = strings.mobile;
const e = strings.enums;

type Fate = "RECOVERED" | "DISPOSED";
/** Rubricile care pot veni din poză și trebuie confirmate. */
type ReadField = "date" | "wasteCode" | "quantity" | "partner" | "documentReference" | "vehicle";

const ALL_CODES = Object.keys(e.wasteOperationCode) as WasteOperationCode[];

/**
 * M1b — predarea deșeului propriu: generare + transport spre valorificare sau eliminare, pe Anexa 1.
 * Exact ce salvează „Generare” pe web (`MovementsPage.buildInput`): `operation` e soarta aleasă,
 * `register` e `ANEXA_1`, codul R/D e obligatoriu (G3).
 *
 * <p>F4 (valul B, 27.09.2026): același formular ca date, pe **trei pași** cu progres sus și buton lipit
 * jos — „Ce și cât” (poza, data, codul, cantitatea), „Cui și cum” („La fel ca data trecută” ca prim card,
 * destinatarul, soarta și codul R/D, cine transportă), „Pe fișă și transport” (rubricile rar schimbate,
 * pliate cu rezumatul pe rând; Anexa 3; documentul). Starea trăiește în componenta asta peste pași, ce
 * se desenează alege `step` (`src/handoverSteps.ts`). Un formular închis pe la mijloc rămâne **ciornă**
 * (`src/handoverDraft.ts`) și se reia de pe „Adaugă” cu `?draft=1`.
 *
 * <p>Din 19.09.2026 serverul refuză o predare pe Anexa 1 fără ce tipăresc fișa și anexele de
 * ambalaje (`validateOwnWasteHandover`, BUG-023): starea, depozitarea, mijlocul de transport,
 * destinația, materialul și felul ambalajului, partenerul autorizat. Le cere și telefonul, înainte
 * ca predarea să intre în coadă — altfel ar fi ieșit „refuzată” abia la trimitere.
 *
 * <p>Ce vine din poză e o propunere cu rândul ei de pe aviz; „Continuă” și „Salvează” nu pleacă până nu
 * e confirmată fiecare de pe pasul lor. Schimbarea unei valori o confirmă și ea: omul a pus-o, nu camera.
 *
 * <p>M1f — cu `?edit=<id>` același formular corectează o predare: rubricile vin din `GET /movements/{id}`,
 * iar salvarea e `PUT`, direct, numai cu semnal (nu prin coadă). Cererea pornește de la predarea de pe
 * server (`editBody`), fiindcă `PUT` înlocuiește tot și telefonul n-are toate rubricile webului.
 *
 * <p>Cu `?outbox=<id>` corectează o predare refuzată din coadă: rubricile vin din cererea salvată pe
 * telefon, iar salvarea rescrie rândul, care pleacă din nou cu aceeași cheie (`resubmit`).
 */
export default function PredareScreen() {
  const { photo: photoParam, edit, outbox, again, repeat, draft: draftParam } = useLocalSearchParams<{
    photo?: string;
    edit?: string;
    outbox?: string;
    again?: string;
    /** F7: „Repetă predarea” de pe predarea deschisă — id-ul ei; se salvează ca predare nouă. */
    repeat?: string;
    draft?: string;
  }>();
  const { auth, session } = useSession();
  const router = useRouter();
  const queryClient = useQueryClient();
  const online = useOnline();
  const { company, workPoints, partners, recent, drivers } = useHandoverData();
  const scrollRef = useRef<ScrollView>(null);

  // Cheia de idempotență a predării, dată o dată, la deschiderea formularului (todo-mobil §10).
  const id = useRef(Crypto.randomUUID()).current;

  const [step, setStep] = useState<Step>(1);
  // Poza: din parametru (camera / galeria) sau din ciorna reluată.
  const [photo, setPhoto] = useState<string | null>(photoParam ?? null);

  // M1f: predarea de corectat, aceeași cheie ca ecranul ei — după salvare se reîncarcă amândouă.
  // F7: tot de aici vine și predarea de repetat; ea nu e `original`, deci salvarea face una nouă.
  const fromServer = edit ?? repeat;
  const editing = useQuery({
    queryKey: ["movements", "one", session?.tenantId, fromServer],
    queryFn: () => api.movement(auth!, fromServer!),
    enabled: !!auth && !!fromServer,
    retry: 0,
  });
  const original = edit ? editing.data : undefined;
  const repeated = repeat ? editing.data : undefined;

  // Predarea refuzată din coadă, redeschisă; numele codului de deșeu vine din profilul firmei.
  const queuedRow = useQuery({
    queryKey: ["outbox-row", outbox],
    queryFn: () => getQueued(outbox!, session!.email),
    enabled: !!outbox && !!session,
    // Rândul se citește proaspăt la fiecare deschidere: între timp poate fi fost corectat și refuzat iar.
    staleTime: 0,
    gcTime: 0,
  });
  const queued = queuedRow.data ?? null;
  const queuedMovement = useMemo(
    () =>
      queued && !company.isLoading
        ? queuedToMovement(queued.payload, queued.summary, company.data?.authorizedWasteCodes ?? [])
        : undefined,
    [queued, company.isLoading, company.data],
  );
  // „Încă o predare, la fel” (de pe bon, 27.09.2026): destinatarul, codul și rubricile de pe fișă din
  // ultima predare salvată; data, cantitatea, documentul și mașina sunt ale drumului de azi.
  const againMovement = useMemo(() => {
    const prev = again ? lastSaved() : null;
    return prev && !company.isLoading
      ? queuedToMovement(prev.payload, { wasteCode: prev.wasteCode }, company.data?.authorizedWasteCodes ?? [])
      : undefined;
  }, [again, company.isLoading, company.data]);

  const activeWorkPoints = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  const [workPointId, setWorkPointId] = useState("");
  useEffect(() => {
    if (!workPointId && activeWorkPoints.length === 1) setWorkPointId(activeWorkPoints[0].id);
  }, [activeWorkPoints, workPointId]);

  const [date, setDate] = useState(todayIso());
  const [wasteCode, setWasteCode] = useState<WasteCode | null>(null);
  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState<Unit>("KG");
  const [weighed, setWeighed] = useState(false);
  const [fate, setFate] = useState<Fate | "">("");
  const [operationCode, setOperationCode] = useState<WasteOperationCode | "">("");
  const [partnerId, setPartnerId] = useState("");
  // `null` = predare veche fără răspuns, socotită „da” de server (`initialPackagingOnMarket`).
  const [packagingOnMarket, setPackagingOnMarket] = useState<boolean | null>(false);
  const [packagingMaterial, setPackagingMaterial] = useState<PackagingMaterial | "">("");
  const [packagingCategory, setPackagingCategory] = useState<PackagingCategory | "">("");
  const [physicalState, setPhysicalState] = useState<PhysicalState | "">("");
  const [storageType, setStorageType] = useState<StorageType | "">("");
  const [transportMeans, setTransportMeans] = useState<TransportMeans | "">("");
  const [wasteDestination, setWasteDestination] = useState<WasteDestination | "">("");
  const [documentReference, setDocumentReference] = useState("");
  const [vehicle, setVehicle] = useState("");
  // ── Anexa 3: transportul (ca `TransportFields` pe web); datele sunt `yyyy-MM-dd` sau "" ──
  const [partnerWorkPointId, setPartnerWorkPointId] = useState("");
  const [loadDate, setLoadDate] = useState("");
  const [unloadDate, setUnloadDate] = useState("");
  const [anexa3Unit, setAnexa3Unit] = useState<Unit | "">("");
  const [transportPartnerId, setTransportPartnerId] = useState("");
  const [driverId, setDriverId] = useState("");
  const [driverName, setDriverName] = useState("");
  const [driverIdentification, setDriverIdentification] = useState("");
  const [driverCnp, setDriverCnp] = useState("");
  const [transportDestinations, setTransportDestinations] = useState<TransportDestination[]>([]);
  const [destinationsPrefilled, setDestinationsPrefilled] = useState(false);
  const [showErrors, setShowErrors] = useState(false);
  const [appliedLast, setAppliedLast] = useState(false);

  // ── M1f: rubricile predării de corectat, puse o singură dată ─────────────────
  const prefilled = useRef(false);
  const [submitting, setSubmitting] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  // B3: poza ciornei nu mai e pe telefon (cache golit de sistem); formularul merge mai departe fără ea.
  const [photoGone, setPhotoGone] = useState(false);
  const source = original ?? queuedMovement ?? againMovement ?? repeated;
  useEffect(() => {
    if (!source || prefilled.current) return;
    prefilled.current = true;
    const mv = source;
    setWorkPointId(mv.workPointId);
    setDate(mv.date);
    setWasteCode({ id: mv.wasteCodeId, code: mv.wasteCode, name: mv.wasteCodeName, hazardous: mv.hazardous, metalSuggested: false });
    setQuantity(mv.quantity == null ? "" : String(mv.quantity).replace(".", ","));
    setUnit(mv.unit);
    setWeighed(mv.weighedAtUnloading);
    setFate(mv.operation === "RECOVERED" || mv.operation === "DISPOSED" ? mv.operation : "");
    setOperationCode(mv.operationCode ?? "");
    setPartnerId(mv.partnerId ?? "");
    setPackagingOnMarket(initialPackagingOnMarket(mv));
    setPackagingMaterial(mv.packagingMaterial ?? "");
    setPackagingCategory(mv.packagingCategory ?? "");
    setPhysicalState(mv.physicalState ?? "");
    setStorageType(mv.storageType ?? "");
    setTransportMeans(mv.transportMeans ?? "");
    setWasteDestination(mv.wasteDestination ?? "");
    setDocumentReference(mv.documentReference ?? "");
    setVehicle(mv.vehicleRegistration ?? "");
    setPartnerWorkPointId(mv.partnerWorkPointId ?? "");
    setLoadDate(mv.loadDate ?? "");
    setUnloadDate(mv.unloadDate ?? "");
    setAnexa3Unit(mv.anexa3Unit ?? "");
    setTransportPartnerId(mv.transportPartnerId ?? "");
    setDriverName(mv.driverName ?? "");
    setDriverIdentification(mv.driverIdentification ?? "");
    setDriverCnp(mv.driverCnp ?? "");
    setTransportDestinations(mv.transportDestinations ?? []);
    if (again || repeat) {
      setDate(todayIso());
      setQuantity("");
      setWeighed(false);
      setDocumentReference("");
      setVehicle("");
      setLoadDate("");
      setUnloadDate("");
    }
  }, [source, again, repeat]);

  // ── citirea avizului ───────────────────────────────────────────────────────
  const [lines, setLines] = useState<string[] | null>(null);
  const [ocrFailed, setOcrFailed] = useState(false);
  useEffect(() => {
    if (!photoParam) return;
    if (!isSupported) return setOcrFailed(true);
    extractTextFromImage(photoParam)
      .then(setLines)
      .catch(() => setOcrFailed(true));
  }, [photoParam]);

  const [unknownCui, setUnknownCui] = useState<string | null>(null);
  const [pending, setPending] = useState<Partial<Record<ReadField, string>>>({});
  const confirm = (f: ReadField) => setPending(({ [f]: _, ...rest }) => rest);
  const applied = useRef(false);

  // Se aplică o singură dată, după ce și textul, și listele firmei sunt aici — altfel un CUI citit
  // înainte să vină partenerii ar fi ieșit „necunoscut”. Fără semnal și fără cache, listele cad pe
  // eroare, și atunci se citește cu ce este.
  const listsSettled = !company.isLoading && !partners.isLoading;
  useEffect(() => {
    if (!lines || !listsSettled || applied.current) return;
    applied.current = true;
    const profileCodes = company.data?.authorizedWasteCodes ?? [];
    const reading: AvizReading = parseAviz(lines, {
      ownCui: company.data?.cui,
      wasteCodes: profileCodes.map((c) => c.code),
      partners: (partners.data ?? []).filter((p) => p.active).map((p) => ({ id: p.id, cui: p.cui })),
    });
    const next: Partial<Record<ReadField, string>> = {};
    if (reading.date) {
      setDate(reading.date.value);
      next.date = reading.date.source;
    }
    const code = reading.wasteCode && profileCodes.find((c) => c.code === reading.wasteCode!.value);
    if (code) {
      setWasteCode(code);
      next.wasteCode = reading.wasteCode!.source;
    }
    if (reading.quantity) {
      setQuantity(String(reading.quantity.value.amount).replace(".", ","));
      setUnit(reading.quantity.value.unit);
      next.quantity = reading.quantity.source;
    }
    if (reading.partnerId) {
      setPartnerId(reading.partnerId.value);
      next.partner = reading.partnerId.source;
    } else if (reading.unknownCui) {
      setUnknownCui(reading.unknownCui.value);
    }
    if (reading.documentNumber) {
      setDocumentReference(reading.documentNumber.value);
      next.documentReference = reading.documentNumber.source;
    }
    if (reading.vehicle) {
      setVehicle(reading.vehicle.value);
      next.vehicle = reading.vehicle.source;
    }
    setPending(next);
  }, [lines, listsSettled, company.data, partners.data]);

  const reading = !!photoParam && !ocrFailed && (!lines || !applied.current);
  const readNothing =
    !!photoParam && (ocrFailed || (lines != null && applied.current && Object.keys(pending).length === 0 && !unknownCui));

  // ── ciorna (F4) ────────────────────────────────────────────────────────────
  // Numai pe o predare nouă: corectura și rândul refuzat au deja unde stau.
  const draftable = !edit && !outbox && !!session?.tenantId;
  const owner = session?.email;
  const tenantId = session?.tenantId;
  const restoring = useRef(!!draftParam);
  const [restored, setRestored] = useState(!draftParam);
  useEffect(() => {
    if (!draftParam || !owner || !tenantId) return;
    db()
      .then((d) => loadDraft(d, owner, tenantId))
      .then((saved) => {
        if (saved) {
          const f = saved.fields as Record<string, unknown>;
          const s = <T,>(k: string, fallback: T) => (k in f ? (f[k] as T) : fallback);
          setStep(saved.step);
          const savedPhoto = localPhoto(saved.photo);
          const kept = savedPhoto && photoExists(savedPhoto) ? savedPhoto : null;
          setPhoto(kept);
          setPhotoGone(!!savedPhoto && !kept);
          setWorkPointId(s("workPointId", ""));
          setDate(s("date", todayIso()));
          setWasteCode(s<WasteCode | null>("wasteCode", null));
          setQuantity(s("quantity", ""));
          setUnit(s<Unit>("unit", "KG"));
          setWeighed(s("weighed", false));
          setFate(s<Fate | "">("fate", ""));
          setOperationCode(s<WasteOperationCode | "">("operationCode", ""));
          setPartnerId(s("partnerId", ""));
          setPackagingOnMarket(s<boolean | null>("packagingOnMarket", false));
          setPackagingMaterial(s<PackagingMaterial | "">("packagingMaterial", ""));
          setPackagingCategory(s<PackagingCategory | "">("packagingCategory", ""));
          setPhysicalState(s<PhysicalState | "">("physicalState", ""));
          setStorageType(s<StorageType | "">("storageType", ""));
          setTransportMeans(s<TransportMeans | "">("transportMeans", ""));
          setWasteDestination(s<WasteDestination | "">("wasteDestination", ""));
          setDocumentReference(s("documentReference", ""));
          setVehicle(s("vehicle", ""));
          setPartnerWorkPointId(s("partnerWorkPointId", ""));
          setLoadDate(s("loadDate", ""));
          setUnloadDate(s("unloadDate", ""));
          setAnexa3Unit(s<Unit | "">("anexa3Unit", ""));
          setTransportPartnerId(s("transportPartnerId", ""));
          setDriverId(s("driverId", ""));
          setDriverName(s("driverName", ""));
          setDriverIdentification(s("driverIdentification", ""));
          setDriverCnp(s("driverCnp", ""));
          setTransportDestinations(s<TransportDestination[]>("transportDestinations", []));
          setUnknownCui(s<string | null>("unknownCui", null));
          setAppliedLast(s("appliedLast", false));
          setPending(Object.fromEntries(saved.pending.map((k) => [k, f[`pending:${k}`] ?? ""])) as Partial<Record<ReadField, string>>);
          // Poza a fost citită când s-a făcut ciorna; nu se mai citește o dată.
          applied.current = true;
        }
      })
      .catch(() => {})
      .finally(() => {
        restoring.current = false;
        setRestored(true);
      });
  }, [draftParam, owner, tenantId]);

  const fields = {
    workPointId, date, wasteCode, quantity, unit, weighed, fate, operationCode, partnerId, packagingOnMarket,
    packagingMaterial, packagingCategory, physicalState, storageType, transportMeans, wasteDestination,
    documentReference, vehicle, partnerWorkPointId, loadDate, unloadDate, anexa3Unit, transportPartnerId, driverId,
    driverName, driverIdentification, driverCnp, transportDestinations, unknownCui, appliedLast,
    ...Object.fromEntries(Object.entries(pending).map(([k, v]) => [`pending:${k}`, v])),
  };
  // Ceva atins de om: o ciornă goală n-ar spune nimic pe „Adaugă”.
  const dirty = !!(photo || wasteCode || quantity.trim() || partnerId || fate || documentReference.trim() || vehicle.trim());
  const fieldsJson = JSON.stringify(fields);
  useEffect(() => {
    if (!draftable || !restored || restoring.current || !owner || !tenantId || !dirty) return;
    const handle = setTimeout(() => {
      const draft: HandoverDraft = {
        savedAt: Date.now(),
        step,
        photo,
        fields: JSON.parse(fieldsJson),
        pending: Object.keys(pending),
        summary: {
          wasteCode: wasteCode?.code ?? null,
          quantity: quantity.trim() ? `${quantity.trim()} ${unit === "KG" ? "kg" : "t"}` : "",
        },
      };
      writeDraft(owner, tenantId, draft).catch(() => {});
    }, 400);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fieldsJson, step, photo, draftable, restored, owner, tenantId, dirty]);

  // ── G3: codul R/D ──────────────────────────────────────────────────────────
  const profileOps = company.data?.authorizedOperationCodes ?? [];
  const familyCodes = ALL_CODES.filter((c) =>
    fate === "RECOVERED" ? c.startsWith("R") : fate === "DISPOSED" ? c.startsWith("D") : false,
  );
  const codeOptions = profileOps.length === 0 ? familyCodes : familyCodes.filter((c) => profileOps.includes(c));

  /**
   * „La fel ca data trecută”: ultima predare a aceluiași deșeu — către același partener, dacă e ales;
   * altfel către oricare. Numai o propunere — nu se pune singură, se aplică la apăsare, ca pe web.
   */
  const last = useMemo(() => {
    if (!wasteCode) return null;
    return (
      (recent.data?.content ?? []).find(
        (mv) => mv.wasteCodeId === wasteCode.id && mv.operationCode && (!partnerId || mv.partnerId === partnerId),
      ) ?? null
    );
  }, [recent.data, partnerId, wasteCode]);

  /** „La fel ca data trecută” pune ce ar pune și pe web (`applyLast`), fără cantitate, dată și document. */
  const applyLast = () => {
    if (!last?.operationCode) return;
    if (!partnerId && last.partnerId) {
      setPartnerId(last.partnerId);
      confirm("partner");
    }
    setFate(last.operationCode.startsWith("R") ? "RECOVERED" : "DISPOSED");
    setOperationCode(last.operationCode);
    setPhysicalState(last.physicalState ?? "");
    setStorageType(last.storageType ?? "");
    setTransportMeans(last.transportMeans ?? "");
    setWasteDestination(last.wasteDestination ?? "");
    setPackagingMaterial(last.packagingMaterial ?? "");
    setPackagingCategory(last.packagingCategory ?? "");
    if (last.packagingOnMarket != null) setPackagingOnMarket(last.packagingOnMarket);
    // Transportul, ca pe web: aceeași firmă, același șofer, aceeași mașină. Datele nu — sunt ale drumului de azi.
    setPartnerWorkPointId(last.partnerWorkPointId ?? "");
    setAnexa3Unit(last.anexa3Unit ?? "");
    setTransportPartnerId(last.transportPartnerId ?? "");
    setDriverId("");
    setDriverName(last.driverName ?? "");
    setDriverIdentification(last.driverIdentification ?? "");
    setDriverCnp(last.driverCnp ?? "");
    if (last.vehicleRegistration && !vehicle.trim()) setVehicle(last.vehicleRegistration);
    setTransportDestinations(last.transportDestinations ?? []);
    setDestinationsPrefilled(false);
    setAppliedLast(true);
    haptic.press();
    goTo(3);
  };

  // Destinația din tabăra soartei alese (nota 5); schimbarea soartei golește una din tabăra cealaltă.
  const offeredDestinations = useMemo(() => destinationsFor(fate), [fate]);
  useEffect(() => {
    if (wasteDestination && !offeredDestinations.includes(wasteDestination)) setWasteDestination("");
  }, [offeredDestinations, wasteDestination]);

  // ── destinatarul ───────────────────────────────────────────────────────────
  const partner = (partners.data ?? []).find((p) => p.id === partnerId) ?? null;

  /**
   * Alegerea destinatarului, ca pe web: punctul lui de lucru se golește (era al celuilalt), iar
   * „Destinat:” se propune din felul partenerului numai peste o rubrică neatinsă.
   */
  const pickPartner = (pid: string) => {
    setPartnerId(pid);
    setPartnerWorkPointId("");
    if (transportDestinations.length === 0 && fate) {
      const chosen = (partners.data ?? []).find((p) => p.id === pid);
      const suggested = suggestedDestinations(chosen?.type, fate);
      if (suggested.length > 0) {
        setTransportDestinations(suggested);
        setDestinationsPrefilled(true);
      }
    }
  };

  /**
   * Cine poate transporta: cei bifați „Transportator” în Parteneri, plus destinatarul — de cele mai
   * multe ori chiar el vine cu mașina. Pe web lista are toți partenerii (grupați); pe telefon ar fi o
   * listă derulantă lungă, deci restul rămân pe web.
   */
  const carrierOptions = useMemo(() => {
    const active = (partners.data ?? []).filter((p) => p.active);
    const list = active.filter((p) => p.carrier);
    if (partner && !list.some((p) => p.id === partner.id)) list.push(partner);
    const chosen = active.find((p) => p.id === transportPartnerId);
    if (chosen && !list.some((p) => p.id === chosen.id)) list.push(chosen);
    return list;
  }, [partners.data, partner, transportPartnerId]);
  /** Șoferii transportatorului ales; fără transportator, ai noștri (`partnerId` null). */
  const availableDrivers = useMemo(
    () =>
      (drivers.data ?? []).filter(
        (d) => d.active && (transportPartnerId ? d.partnerId === transportPartnerId : d.partnerId === null),
      ),
    [drivers.data, transportPartnerId],
  );
  const pickDriver = (did: string) => {
    setDriverId(did);
    const d = availableDrivers.find((x) => x.id === did);
    if (!d) return;
    // Alegerea precompletează cele trei rubrici (rămân editabile) și mașina, dacă e goală.
    setDriverName(d.name);
    setDriverIdentification(d.identification ?? "");
    setDriverCnp(d.cnp ?? "");
    if (d.vehicleRegistration && !vehicle.trim()) setVehicle(d.vehicleRegistration);
  };
  const [partnerQuery, setPartnerQuery] = useState("");
  const partnerMatches = useMemo(() => {
    const q = partnerQuery.trim().toLowerCase();
    const digits = cuiDigits(q);
    return (partners.data ?? [])
      .filter((p) => p.active)
      .filter((p) => !q || p.name.toLowerCase().includes(q) || (digits.length > 1 && cuiDigits(p.cui).includes(digits)))
      .slice(0, 6);
  }, [partners.data, partnerQuery]);

  // ── codul de deșeu ─────────────────────────────────────────────────────────
  const profileCodes = company.data?.authorizedWasteCodes ?? [];
  const [codeQuery, setCodeQuery] = useState("");
  const [codeSearchOpen, setCodeSearchOpen] = useState(false);
  const codeSearch = useQuery({
    queryKey: ["waste-codes", codeQuery.trim()],
    queryFn: () => api.wasteCodes(auth!, codeQuery.trim()),
    enabled: !!auth && codeQuery.trim().length >= 2,
    retry: 0,
  });
  const searching = codeSearchOpen || profileCodes.length === 0;
  const shownCodes = searching && codeQuery.trim().length >= 2 ? (codeSearch.data ?? []).slice(0, 8) : searching ? [] : profileCodes;
  const isPackaging = wasteCode?.code.startsWith("15 01") ?? false;
  // Codul care își spune singur materialul nu-l mai cere (ca pe web și ca `PackagingMaterial.resolve`).
  const suggestedMaterial = wasteCode ? suggestedPackagingMaterial(wasteCode.code) : null;

  // ── validarea ──────────────────────────────────────────────────────────────
  const isoDate = date || null;
  // Rubricile Anexei 3 se văd (și pleacă) numai cu destinatar ales; fără el nu le cerem și nu le trimitem.
  const isoLoad = partner && loadDate ? loadDate : null;
  const isoUnload = partner && unloadDate ? unloadDate : null;
  // Cântărită la descărcare: câmpul e închis până vine greutatea; după aceea se corectează ca oricare.
  const quantityOpen = !weighed || weightRecorded(weighed, original);
  const amount = parseQuantity(quantity);
  const onMarket = packagingOnMarket !== false;
  const errors: Partial<Record<FieldKey, string | undefined>> = {
    workPoint: !workPointId ? strings.common.requiredField : undefined,
    date: !isoDate ? m.dateFormat : !yearInRange(isoDate) ? m.dateOutOfRange : undefined,
    wasteCode: !wasteCode ? t.wasteCodePlaceholder : undefined,
    quantity: !quantityOpen
      ? undefined
      : !quantity.trim()
        ? strings.common.requiredField
        : amount == null
          ? m.quantityFormat
          : undefined,
    fate: !fate ? strings.common.requiredField : undefined,
    operationCode:
      fate === "RECOVERED" && !operationCode.startsWith("R")
        ? t.recoveryCodeRequired
        : fate === "DISPOSED" && !operationCode.startsWith("D")
          ? t.disposalCodeRequired
          : !codeInProfile(operationCode, profileOps)
            ? m.codeNotInProfile
            : undefined,
    partner:
      weighed && !partnerId
        ? t.weighingNeedsPartner
        : partner && !partner.authorizationNumber?.trim()
          ? t.partnerNeedsAuthorization
          : undefined,
    physicalState: !physicalState ? t.physicalStateRequired : undefined,
    storageType: !storageType ? t.storageTypeRequired : undefined,
    transportMeans: !transportMeans ? t.transportMeansRequired : undefined,
    wasteDestination: !wasteDestination ? t.wasteDestinationRequired : undefined,
    packagingMaterial:
      isPackaging && !packagingMaterial && !suggestedMaterial ? t.packagingMaterialRequired : undefined,
    packagingCategory:
      isPackaging && onMarket && !packagingCategory ? t.packagingCategoryRequired : undefined,
    driverCnp: partner && driverCnp.trim() && !isValidCnp(driverCnp.trim()) ? strings.naturalPersons.cnpInvalid : undefined,
    unloadDate: isoUnload && isoUnload < (isoLoad ?? isoDate ?? "") ? m.unloadBeforeLoad : undefined,
  };
  const unconfirmed = Object.keys(pending).length;
  const valid = Object.values(errors).every((v) => !v);

  // ── pașii ──────────────────────────────────────────────────────────────────
  const goTo = (next: Step) => {
    setShowErrors(false);
    setStep(next);
    requestAnimationFrame(() => scrollRef.current?.scrollTo({ y: 0, animated: false }));
  };
  const next = () => {
    if (stepBlocked(step, errors, pending)) {
      setShowErrors(true);
      haptic.warning();
      return;
    }
    haptic.tap();
    goTo((step + 1) as Step);
  };
  const back = () => {
    if (step === 1) return router.back();
    haptic.tap();
    goTo((step - 1) as Step);
  };
  /** Primul pas cu ceva de completat sau de confirmat, dacă e vreunul. */
  const firstBrokenStep = (): Step | null => {
    for (const s of [1, 2, 3] as Step[]) if (stepBlocked(s, errors, pending)) return s;
    return null;
  };

  const save = async () => {
    setShowErrors(true);
    const broken = firstBrokenStep();
    if (broken && broken !== step) {
      // Ceva de pe un pas trecut s-a stricat între timp (un cod scos din profil, o listă venită mai târziu).
      setSaveError(m.noteFixStep(broken));
      goTo(broken);
      setShowErrors(true);
      return;
    }
    if (!valid || unconfirmed > 0 || !session || !wasteCode || !isoDate) return;
    const onScreen = {
        workPointId,
        date: isoDate,
        wasteCodeId: wasteCode.id,
        quantity: quantityOpen ? amount : null,
        weighedAtUnloading: weighed,
        unit,
        operation: fate,
        register: "ANEXA_1",
        operationCode,
        partnerId: partnerId || null,
        documentReference: documentReference.trim() || null,
        vehicleRegistration: vehicle.trim() || null,
        ...(partner
          ? {
              partnerWorkPointId: partnerWorkPointId || null,
              loadDate: isoLoad,
              unloadDate: isoUnload,
              anexa3Unit: anexa3Unit || null,
              transportPartnerId: transportPartnerId || null,
              driverName: driverName.trim() || null,
              driverIdentification: driverIdentification.trim() || null,
              driverCnp: driverCnp.trim() || null,
              transportDestinations,
            }
          : original
            ? // La corectură, fără destinatar rubricile Anexei 3 se golesc: altfel ar rămâne cele ale celui scos.
              {
                partnerWorkPointId: null,
                loadDate: null,
                unloadDate: null,
                anexa3Unit: null,
                transportPartnerId: null,
                driverName: null,
                driverIdentification: null,
                driverCnp: null,
                transportDestinations: [],
              }
            : {}),
        physicalState,
        storageType,
        transportMeans,
        wasteDestination,
        packagingOnMarket: isPackaging ? packagingOnMarket : null,
        packagingMaterial: isPackaging ? packagingMaterial || null : null,
        packagingCategory: isPackaging && onMarket ? packagingCategory || null : null,
    };
    if (original) return saveEdit(original, onScreen, isoDate);
    const summary = {
      wasteCode: wasteCode.code,
      quantity: quantityOpen && amount != null ? `${formatQuantity(amount, unit)} ${e.unit[unit]}` : t.awaitingWeighing,
      partnerName: partner?.name ?? null,
      date: isoDate,
    };
    setSaveError(null);
    try {
      if (queued) await resubmit(queued.id, onScreen, summary);
      else
        await enqueue({
          id,
          owner: session.email,
          tenantId: session.tenantId,
          photoUri: photo ?? null,
          payload: onScreen,
          summary,
        });
    } catch (error) {
      if (error instanceof PhotoMissingError) {
        // Poza a plecat de pe disc între timp: fără ea predarea se poate salva, deci n-o mai cerem.
        setPhoto(null);
        setPhotoGone(true);
        setSaveError(m.photoGone);
        return;
      }
      // Poza n-a putut fi copiată sau baza telefonului e plină: omul trebuie să afle, nu să apese degeaba.
      reportError(error, { where: "predare", step: "enqueue" });
      setSaveError(t.saveError);
      return;
    }
    // Predarea e în coadă: ciorna nu mai are ce păstra.
    if (draftable && owner && tenantId) dropDraft(owner, tenantId).catch(() => {});
    const rowId = queued ? queued.id : id;
    rememberSaved({
      outboxId: rowId,
      payload: onScreen,
      wasteCode: wasteCode.code,
      wasteCodeName: wasteCode.name,
      quantity: summary.quantity,
      date: isoDate,
      partnerName: partner?.name ?? null,
      operationCode: operationCode || null,
      documentReference: documentReference.trim() || null,
      vehicle: vehicle.trim() || null,
      driverName: partner ? driverName.trim() || null : null,
      workPointName: activeWorkPoints.find((w) => w.id === workPointId)?.name ?? null,
      hasPhoto: !!(photo ?? queued?.photoUri),
    });
    haptic.press();
    // Cu semnal pleacă pe loc; fără, rămâne în „De trimis”. Bonul (`/gata`) urmărește rândul din coadă.
    if (auth) {
      drain(auth, session.email)
        .then((sent) => {
          if (sent) queryClient.invalidateQueries({ queryKey: ["movements"] });
        })
        .catch(() => {});
    }
    router.replace({ pathname: "/gata", params: { outbox: rowId } });
  };

  /**
   * M1f — corectura pleacă direct, cu semnal. Întâi întrebarea webului când anul (cel nou sau cel vechi)
   * e deja declarat; dacă termenele nu se pot aduce, salvarea merge mai departe, ca pe web.
   */
  // `onScreen` e aceeași cerere ca la adăugare; validarea de mai sus garantează că nu mai e niciun „""”.
  const saveEdit = async (mv: WasteMovement, onScreen: object, newDate: string) => {
    if (!auth || submitting) return;
    setSubmitting(true);
    setSaveError(null);
    try {
      for (const year of new Set([Number(newDate.slice(0, 4)), Number(mv.date.slice(0, 4))])) {
        const declaration = await api
          .deadlines(auth, year + 1)
          .then((list) => declarationOf(list, year))
          .catch(() => undefined);
        if (declaration && !(await confirmDeclared(year, declaration))) return;
      }
      await api.updateMovement(auth, mv.id, editBody(mv, onScreen as Partial<WasteMovementInput>));
      await queryClient.invalidateQueries({ queryKey: ["movements"] });
      router.back();
    } catch (error) {
      setSaveError(
        error instanceof api.ApiError && error.serverMessage
          ? error.serverMessage
          : error instanceof TypeError ? m.movementEditOffline : t.saveError,
      );
    } finally {
      setSubmitting(false);
    }
  };

  const err = (k: FieldKey) => (showErrors ? errors[k] : undefined);

  // ── rândurile pliate de pe pasul 3 ─────────────────────────────────────────
  // Un rând fără valoare stă deschis; unul cu valoare, pliat. Omul poate răsturna oricare.
  const [foldState, setFoldState] = useState<Record<string, boolean>>({});
  const foldOpen = (key: string, hasValue: boolean) => foldState[key] ?? !hasValue;
  const toggleFold = (key: string, hasValue: boolean) => setFoldState((s) => ({ ...s, [key]: !foldOpen(key, hasValue) }));
  const closeFold = (key: string) => setFoldState((s) => ({ ...s, [key]: false }));
  const fold = <T extends string>(
    key: FieldKey | "loadDate" | "unloadDate" | "driver",
    label: string,
    value: T | "",
    summary: string | null,
    body: React.ReactNode,
    first?: boolean,
  ) => (
    <Fold
      key={key}
      testID={`fold-${key}`}
      label={label}
      summary={value ? summary : null}
      error={key in errors ? err(key as FieldKey) : undefined}
      open={foldOpen(key, !!value)}
      onToggle={() => toggleFold(key, !!value)}
      first={first}
    >
      {body}
    </Fold>
  );
  /** Pastilele unui rând pliat: alegerea îl pliază la loc. */
  const pick = <T extends string>(key: string, set: (v: T) => void) => (v: T) => {
    set(v);
    closeFold(key);
  };

  const vehicleField = (
    <Field label={t.vehicleRegistration} read={pending.vehicle} onConfirm={() => confirm("vehicle")} testID="f-vehicle">
      <Input
        value={vehicle}
        onChangeText={(v) => {
          setVehicle(v);
          confirm("vehicle");
        }}
        autoCapitalize="characters"
      />
    </Field>
  );

  const editingMode = !!edit || !!outbox;
  const kicker = editingMode ? m.stepEditKicker(step, 3) : m.stepKicker(step, 3);
  const title = step === 1 ? m.step1Title : step === 2 ? m.step2Title : m.step3Title;
  const singleWorkPoint = activeWorkPoints.length === 1 ? activeWorkPoints[0].name : null;
  const note = stepNote(step, { pending, singleWorkPoint, online, hasPhoto: !!photo });
  const noteText =
    note.kind === "pending"
      ? m.notePending(note.n)
      : note.kind === "singleWorkPoint"
        ? m.noteSingleWorkPoint(note.name)
        : note.kind === "nextSheet"
          ? m.noteNextSheet
          : note.kind === "offline"
            ? m.noteOffline
            : note.kind === "onlinePhoto"
              ? m.noteOnlinePhoto
              : note.kind === "onlineNoPhoto"
                ? m.noteOnlineNoPhoto
                : null;
  const stepErrorCount = showErrors ? STEP_FIELDS[step].filter((k) => errors[k]).length : 0;
  const footNote = stepErrorCount > 0 ? m.fixErrors(stepErrorCount) : saveError ?? noteText;
  const footWarn = stepErrorCount > 0 || !!saveError || note.kind === "pending";
  // Pe o predare repetată, completă pe toți pașii, „Salvează” vine de pe pasul 1: restul e cel de data trecută.
  const early = canSaveEarly(step, !!(again || repeat), errors, pending);
  const earlyNote = operationCode && partner ? m.earlyKeeps(operationCode, partner.name) : m.earlyKeepsRest;

  return (
    <>
      <Stack.Screen options={{ headerShown: false }} />
      <KeyboardAvoidingView style={styles.fill} behavior={Platform.OS === "ios" ? "padding" : undefined}>
        <ScrollView
          ref={scrollRef}
          style={styles.fill}
          contentContainerStyle={styles.scroll}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="on-drag"
        >
          <StepHead step={step} total={3} kicker={kicker} title={title} back={{ label: step === 1 ? m.stepCancel : m.stepBack, onPress: back }} />
          <OfflineBand />
          <View style={styles.body}>
            {fromServer && !editing.data ? (
              <Note tone={editing.isError ? "alert" : undefined}>{editing.isError ? m.movementError : m.movementEditLoading}</Note>
            ) : null}

            {/* ── Pasul 1: ce și cât ─────────────────────────────────────────── */}
            {step === 1 ? (
              <>
                {photo ? (
                  <View style={styles.photoBox}>
                    <Image source={{ uri: photo }} style={styles.photo} resizeMode="cover" />
                    {reading ? (
                      <View style={styles.readingOverlay} testID="aviz-reading">
                        <ActivityIndicator color={colors.lcdDigit} />
                        <Text style={styles.readingText}>{m.reading}</Text>
                      </View>
                    ) : null}
                  </View>
                ) : null}
                {photo && !reading ? (
                  <Pressable onPress={() => setPhoto(null)} testID="photo-remove" style={styles.photoRemove}>
                    <Text style={styles.photoRemoveText}>{m.photoRemove}</Text>
                  </Pressable>
                ) : null}
                {photoGone ? <Note tone="alert">{m.photoGone}</Note> : null}
                {readNothing ? <Note tone="alert">{m.readNothing}</Note> : null}

                {activeWorkPoints.length !== 1 || (workPoints.isError && !workPoints.data) ? (
                  <Group>
                    <Field label={t.filterWorkPoint} error={err("workPoint")}>
                      {workPoints.isError && !workPoints.data ? (
                        <Text style={styles.hint}>{m.listUnavailable}</Text>
                      ) : (
                        <Pills
                          testID="wp"
                          options={activeWorkPoints.map((w) => ({ value: w.id, label: w.name }))}
                          value={workPointId}
                          onChange={setWorkPointId}
                        />
                      )}
                    </Field>
                  </Group>
                ) : null}

                <Group>
                  <Field label={t.date} read={pending.date} onConfirm={() => confirm("date")} error={err("date")} testID="f-date">
                    <DateField
                      value={isoDate}
                      onChange={(iso) => {
                        setDate(iso);
                        confirm("date");
                      }}
                      min={new Date(2000, 0, 1)}
                      max={new Date(new Date().getFullYear() + 10, 11, 31)}
                      testID="date"
                    />
                  </Field>
                </Group>

                <View style={styles.secHead}>
                  <SectionHead>{t.wasteCode}</SectionHead>
                  {profileCodes.length > 0 && !wasteCode ? (
                    <Pressable onPress={() => setCodeSearchOpen((o) => !o)} testID="code-search-toggle" hitSlop={8}>
                      <Text style={styles.linkText}>{codeSearchOpen ? m.codeSearchClose : m.codeSearchOpen}</Text>
                    </Pressable>
                  ) : null}
                </View>
                <Group>
                  <Field read={pending.wasteCode} onConfirm={() => confirm("wasteCode")} error={err("wasteCode")} testID="f-code">
                    {wasteCode ? (
                      <CodeRow code={wasteCode} chosen onPress={() => { setWasteCode(null); confirm("wasteCode"); }} testID="code-chosen" />
                    ) : (
                      <>
                        {searching ? (
                          <Input value={codeQuery} onChangeText={setCodeQuery} placeholder={t.wasteCodeSearch} testID="code-search" autoFocus={codeSearchOpen} />
                        ) : null}
                        {searching && codeQuery.trim().length < 2 ? <Text style={styles.hint}>{m.wasteCodeNoProfile}</Text> : null}
                        {shownCodes.map((c) => (
                          <CodeRow key={c.id} code={c} onPress={() => { setWasteCode(c); confirm("wasteCode"); }} testID="code-option" />
                        ))}
                      </>
                    )}
                  </Field>
                </Group>

                <SectionHead>{t.sectionQuantity}</SectionHead>
                <Group>
                  <Field read={pending.quantity} onConfirm={() => confirm("quantity")} error={err("quantity")} testID="f-qty">
                    <QuantityField
                      value={quantityOpen ? quantity : ""}
                      editable={quantityOpen}
                      onChange={(v) => {
                        setQuantity(v);
                        confirm("quantity");
                      }}
                      unit={unit}
                      onUnit={(u) => {
                        setUnit(u);
                        confirm("quantity");
                      }}
                      testID="qty"
                    />
                  </Field>
                  <Sep />
                  <View style={styles.switchRow}>
                    <View style={styles.switchText}>
                      <Text style={rowStyles.title}>{t.weighedAtUnloading}</Text>
                    </View>
                    <Switch value={weighed} onValueChange={setWeighed} testID="weighed" />
                  </View>
                </Group>
              </>
            ) : null}

            {/* ── Pasul 2: cui și cum ─────────────────────────────────────────── */}
            {step === 2 ? (
              <>
                {last?.operationCode ? (
                  <View style={styles.lastCard} testID="same-as-last-card">
                    <View style={styles.lastHead}>
                      <Text style={styles.lastKicker}>{m.sameAsLastTitle.toUpperCase()}</Text>
                      <Text style={styles.lastKicker}>{formatDate(last.date)}</Text>
                    </View>
                    <Text style={styles.lastTitle}>{m.sameAsLastTo(last.operationCode, last.partnerName ?? "")}</Text>
                    <Text style={styles.lastSub} numberOfLines={2}>
                      {[
                        last.wasteCodeName,
                        last.quantity != null ? `${formatQuantity(last.quantity, last.unit)} ${e.unit[last.unit]}` : null,
                        last.transportPartnerName,
                        last.driverName,
                        last.vehicleRegistration,
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </Text>
                    <Text style={styles.lastFills}>{m.sameAsLastFills}</Text>
                    <PrimaryButton
                      testID="same-as-last"
                      tone={appliedLast ? "quiet" : "green"}
                      label={appliedLast ? m.sameAsLastApplied : m.sameAsLastApply}
                      onPress={applyLast}
                    />
                  </View>
                ) : null}

                <SectionHead>{t.sectionRecipient}</SectionHead>
                <Group>
                  <Field read={pending.partner} onConfirm={() => confirm("partner")} error={err("partner")} testID="f-partner">
                    {partner ? (
                      <ChosenRow
                        title={partner.name}
                        sub={[partner.cui, partner.authorizationNumber ? `${strings.partners.authorizationNumber} ${partner.authorizationNumber}` : null].filter(Boolean).join(" · ")}
                        onChange={() => {
                          pickPartner("");
                          confirm("partner");
                        }}
                      />
                    ) : (
                      <>
                        {unknownCui ? (
                          <UnknownPartner
                            cui={unknownCui}
                            canAdd={canWrite(session?.role)}
                            onAdded={(p) => {
                              setUnknownCui(null);
                              pickPartner(p.id);
                            }}
                          />
                        ) : null}
                        {partners.isError && !partners.data ? (
                          <Text style={styles.hint}>{m.listUnavailable}</Text>
                        ) : (
                          <>
                            <Input value={partnerQuery} onChangeText={setPartnerQuery} placeholder={m.partnerSearch} testID="partner-search" />
                            {partnerMatches.length === 0 ? <Text style={styles.hint}>{m.partnerNone}</Text> : null}
                            {partnerMatches.map((p) => (
                              <OptionRow key={p.id} title={p.name} sub={p.cui ?? ""} testID="partner-option" onPress={() => pickPartner(p.id)} />
                            ))}
                          </>
                        )}
                      </>
                    )}
                  </Field>
                  {/* Punctul de lucru al destinatarului — numai când are mai multe; cu unul, Anexa 3 îl scrie pe acela. */}
                  {partner && partner.workPoints.length > 1 ? (
                    <>
                      <Sep />
                      <Field label={t.partnerWorkPoint}>
                        <Pills
                          testID="partner-wp"
                          options={partner.workPoints.map((wp) => ({ value: wp.id, label: wp.name ? `${wp.name}, ${wp.address}` : wp.address }))}
                          value={partnerWorkPointId}
                          onChange={setPartnerWorkPointId}
                        />
                        <Text style={styles.hint}>{t.partnerWorkPointHint}</Text>
                      </Field>
                    </>
                  ) : null}
                </Group>

                <SectionHead>{m.stepFateTitle}</SectionHead>
                <Group>
                  <Field label={t.operation} error={err("fate")}>
                    <Pills
                      testID="fate"
                      options={[
                        { value: "RECOVERED" as Fate, label: t.fateRecovery },
                        { value: "DISPOSED" as Fate, label: t.fateDisposal },
                      ]}
                      value={fate}
                      onChange={(f) => {
                        setFate(f);
                        if (operationCode && !operationCode.startsWith(f === "RECOVERED" ? "R" : "D")) setOperationCode("");
                      }}
                    />
                  </Field>
                  <Sep />
                  <Field label={t.operationCode} error={err("operationCode")}>
                    {fate ? (
                      <Pills testID="op" options={codeOptions.map((c) => ({ value: c, label: c }))} value={operationCode} onChange={setOperationCode} />
                    ) : (
                      <Text style={styles.hint}>{t.fateTitle}</Text>
                    )}
                    {operationCode ? <Text style={styles.hint}>{e.wasteOperationCode[operationCode]}</Text> : null}
                  </Field>
                </Group>

                {partner ? (
                  <>
                    <SectionHead>{t.sectionTransport}</SectionHead>
                    <Group>
                      <Field label={m.whoTransports}>
                        <Pills
                          testID="carrier"
                          options={[{ value: "OWN", label: m.carrierOwn }, ...carrierOptions.map((p) => ({ value: p.id, label: p.name }))]}
                          value={transportPartnerId || "OWN"}
                          onChange={(v) => {
                            // Șoferii sunt ai transportatorului: schimbi firma, alegerea nu mai e a ei. Textul rămâne.
                            setTransportPartnerId(v === "OWN" ? "" : v);
                            setDriverId("");
                          }}
                        />
                      </Field>
                    </Group>
                  </>
                ) : null}
              </>
            ) : null}

            {/* ── Pasul 3: pe fișă și transport ──────────────────────────────── */}
            {step === 3 ? (
              <>
                <SectionHead>{m.stepSheet}</SectionHead>
                <Group>
                  {fold("physicalState", t.physicalState, physicalState, physicalState ? e.physicalState[physicalState] : null,
                    <Pills testID="state" options={nomenclator(e.physicalState)} value={physicalState} onChange={pick<PhysicalState>("physicalState", setPhysicalState)} />, true)}
                  {fold("storageType", t.askStorage, storageType, storageType ? e.storageType[storageType] : null,
                    <Pills testID="storage" options={codes(e.storageType)} value={storageType} onChange={pick<StorageType>("storageType", setStorageType)} />)}
                  {fold("transportMeans", t.askTransportMeans, transportMeans, transportMeans ? e.transportMeans[transportMeans] : null,
                    <Pills testID="means" options={codes(e.transportMeans)} value={transportMeans} onChange={pick<TransportMeans>("transportMeans", setTransportMeans)} />)}
                  {fold("wasteDestination", t.askDestination, wasteDestination, wasteDestination ? e.wasteDestination[wasteDestination] : null,
                    fate ? (
                      <Pills testID="dest" options={offeredDestinations.map((d) => ({ value: d, label: d }))} value={wasteDestination} onChange={pick<WasteDestination>("wasteDestination", setWasteDestination)} />
                    ) : (
                      <Text style={styles.hint}>{t.fateTitle}</Text>
                    ))}
                </Group>
                <Text style={styles.hint}>{appliedLast ? m.foldHintApplied : m.foldHint}</Text>

                {isPackaging ? (
                  <>
                    <SectionHead>{m.stepPackaging}</SectionHead>
                    <Group>
                      <View style={styles.switchRow}>
                        <View style={styles.switchText}>
                          <Text style={rowStyles.title}>{t.packagingOnMarket}</Text>
                          <Text style={rowStyles.sub}>{m.packagingOnWeb}</Text>
                        </View>
                        <Switch value={onMarket} onValueChange={setPackagingOnMarket} testID="on-market" />
                      </View>
                      {fold("packagingMaterial", t.packagingMaterial, packagingMaterial || suggestedMaterial || "",
                        packagingMaterial ? e.packagingMaterial[packagingMaterial] : suggestedMaterial ? `${e.packagingMaterial[suggestedMaterial]} ${t.packagingFromCode}` : null,
                        <Pills testID="material" options={PACKAGING_MATERIALS.map((v) => ({ value: v, label: e.packagingMaterial[v] }))} value={packagingMaterial || suggestedMaterial} onChange={pick<PackagingMaterial>("packagingMaterial", setPackagingMaterial)} />)}
                      {onMarket
                        ? fold("packagingCategory", t.packagingCategory, packagingCategory, packagingCategory ? e.packagingCategory[packagingCategory] : null,
                            <Pills testID="category" options={nomenclator(e.packagingCategory)} value={packagingCategory} onChange={pick<PackagingCategory>("packagingCategory", setPackagingCategory)} />)
                        : null}
                    </Group>
                  </>
                ) : null}

                {/* Anexa 3 (la periculoase, doar transportul): aceleași rubrici și aceeași ordine ca pe web
                    (`TransportFields`), numai când există destinatar — fără el nu pleacă nimic nicăieri. */}
                {partner ? (
                  <>
                    <SectionHead>{wasteCode?.hazardous ? t.sectionTransport : t.anexa3Section}</SectionHead>
                    <Group>
                      {fold("loadDate", t.loadDate, loadDate, loadDate ? dateLabel(loadDate) : null,
                        <DateRow value={loadDate} onChange={(v) => { setLoadDate(v); if (v) closeFold("loadDate"); }} hint={t.loadDateHint} testID="load" />, true)}
                      {fold("unloadDate", t.unloadDate, unloadDate, unloadDate ? dateLabel(unloadDate) : null,
                        <DateRow value={unloadDate} onChange={(v) => { setUnloadDate(v); if (v) closeFold("unloadDate"); }} testID="unload" />)}
                      {!wasteCode?.hazardous ? (
                        <>
                          <Sep />
                          <Field label={t.anexa3Unit}>
                            <Pills
                              testID="a3unit"
                              options={[
                                { value: "COMPANY", label: t.anexa3UnitCompany },
                                { value: "KG", label: e.unit.KG },
                                { value: "TONS", label: e.unit.TONS },
                              ]}
                              value={anexa3Unit || "COMPANY"}
                              onChange={(v) => setAnexa3Unit(v === "COMPANY" ? "" : (v as Unit))}
                            />
                          </Field>
                        </>
                      ) : null}
                      {fold("driver", m.stepDriver, driverName.trim() || driverIdentification.trim() ? "x" : "",
                        [driverName.trim(), driverIdentification.trim()].filter(Boolean).join(" · ") || null,
                        <>
                          {availableDrivers.length > 0 ? (
                            <>
                              <Text style={styles.subLabel}>{t.driverPick}</Text>
                              <Pills
                                testID="driver"
                                options={[...availableDrivers.map((d) => ({ value: d.id, label: d.name })), { value: "OTHER", label: m.driverOther }]}
                                value={driverId || "OTHER"}
                                onChange={(v) => (v === "OTHER" ? setDriverId("") : pickDriver(v))}
                              />
                            </>
                          ) : null}
                          <Text style={styles.subLabel}>{t.driverName}</Text>
                          <Input value={driverName} onChangeText={setDriverName} autoCapitalize="words" testID="f-driver" />
                          <Text style={styles.subLabel}>{t.driverIdentification}</Text>
                          <Input value={driverIdentification} onChangeText={setDriverIdentification} placeholder={t.driverIdentificationPlaceholder} autoCapitalize="characters" testID="f-driver-id" />
                          <Text style={styles.subLabel}>{strings.common.cnp}</Text>
                          <Input value={driverCnp} onChangeText={setDriverCnp} keyboardType="number-pad" maxLength={13} testID="f-driver-cnp" />
                          {err("driverCnp") ? <Text style={styles.warn}>{err("driverCnp")}</Text> : null}
                        </>)}
                      <Sep />
                      {vehicleField}
                      <Sep />
                      <Field label={t.askTransportDestinations}>
                        <MultiPills
                          testID="tdest"
                          options={(Object.keys(e.transportDestination) as TransportDestination[]).map((d) => ({ value: d, label: e.transportDestination[d] }))}
                          value={transportDestinations}
                          onChange={(v) => {
                            setTransportDestinations(v);
                            setDestinationsPrefilled(false);
                          }}
                        />
                        <Text style={styles.hint}>{destinationsPrefilled ? t.destinationsPrefilled : t.transportDestinationsHint}</Text>
                      </Field>
                    </Group>
                  </>
                ) : null}

                <SectionHead>{m.stepDocument}</SectionHead>
                <Group>
                  <Field label={t.documentReference} read={pending.documentReference} onConfirm={() => confirm("documentReference")} testID="f-doc">
                    <Input
                      value={documentReference}
                      onChangeText={(v) => {
                        setDocumentReference(v);
                        confirm("documentReference");
                      }}
                      placeholder={t.documentReferencePlaceholder}
                      autoCapitalize="characters"
                    />
                  </Field>
                  {/* Fără destinatar mașina stă aici: avizul citit din poză o poate aduce oricum. */}
                  {!partner ? (
                    <>
                      <Sep />
                      {vehicleField}
                    </>
                  ) : null}
                  {photo ? (
                    <>
                      <Sep />
                      <View style={styles.switchRow}>
                        <View style={styles.switchText}>
                          <Text style={rowStyles.title}>{m.photoProof}</Text>
                          <Text style={rowStyles.sub}>{m.photoProofSub}</Text>
                        </View>
                        <Chip label={m.photoProofChip} tone="ok" />
                      </View>
                    </>
                  ) : null}
                </Group>
              </>
            ) : null}
          </View>
        </ScrollView>
        <StepFoot
          label={step === 3 || early ? m.save : m.stepNext}
          note={early && !saveError ? earlyNote : footNote}
          warn={footWarn}
          onPress={step === 3 || early ? save : next}
          disabled={reading || submitting || (!!fromServer && !editing.data) || (!!outbox && !queued) || !restored}
          testID={step === 3 || early ? "handover-save" : "step-next"}
          secondary={early ? { label: m.earlyRestSteps, onPress: next, testID: "step-next" } : undefined}
        />
      </KeyboardAvoidingView>
    </>
  );
}

/**
 * G4 — un CUI citit care nu e partener. Căutarea la ANAF se face la apăsare; partenerul se creează
 * numai cu ce a arătat ANAF și cu cele două întrebări fără de care serverul nu-l primește (ce face
 * cu deșeul, cine facturează). Nimic nu se creează tăcut.
 */
function UnknownPartner({ cui, canAdd, onAdded }: { cui: string; canAdd: boolean; onAdded: (p: Partner) => void }) {
  const { auth, session } = useSession();
  const queryClient = useQueryClient();
  const [asked, setAsked] = useState(false);
  const [type, setType] = useState<PartnerType | "">("");
  const [role, setRole] = useState<"supplier" | "client">("supplier");
  // Serverul nu primește un colector sau un valorificator fără numărul autorizației de mediu.
  const [authorizationNumber, setAuthorizationNumber] = useState("");
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);

  const lookup = useQuery({
    queryKey: ["company-lookup", cui],
    queryFn: () => api.companyLookup(auth!, cui),
    enabled: asked && !!auth,
    retry: 0,
  });

  const add = async () => {
    if (!lookup.data || !type || !authorizationNumber.trim() || !auth) return;
    setAdding(true);
    setAddError(null);
    try {
      const created = await api.createPartner(auth, {
        name: lookup.data.name,
        cui: lookup.data.cui,
        address: lookup.data.address,
        tradeRegisterNumber: lookup.data.tradeRegisterNumber,
        authorizationNumber: authorizationNumber.trim(),
        type,
        client: role === "client",
        supplier: role === "supplier",
      });
      await queryClient.invalidateQueries({ queryKey: ["partners", session?.tenantId] });
      onAdded(created);
    } catch (error) {
      setAddError(error instanceof api.ApiError && error.serverMessage ? error.serverMessage : m.addPartnerFailed);
    } finally {
      setAdding(false);
    }
  };

  const notFound = lookup.error instanceof api.ApiError && lookup.error.status === 404;
  const offline = lookup.isError && !(lookup.error instanceof api.ApiError);

  return (
    <View style={styles.unknown} testID="unknown-cui">
      <Text style={styles.unknownText}>{m.unknownCui(cui)}</Text>
      {!asked ? (
        <PrimaryButton tone="quiet" label={m.anafLookup} onPress={() => setAsked(true)} testID="anaf-lookup" />
      ) : lookup.isLoading ? (
        <ActivityIndicator />
      ) : notFound ? (
        <Text style={styles.hint}>{m.anafNotFound}</Text>
      ) : offline ? (
        <Text style={styles.hint}>{m.anafOffline}</Text>
      ) : lookup.isError ? (
        <Text style={styles.hint}>{m.anafNotFound}</Text>
      ) : lookup.data ? (
        <View style={{ gap: 8 }}>
          <Text style={rowStyles.title}>{lookup.data.name}</Text>
          {lookup.data.address ? <Text style={rowStyles.sub}>{lookup.data.address}</Text> : null}
          {lookup.data.inactive ? <Text style={styles.warn}>{m.anafInactive}</Text> : null}
          {canAdd ? (
            <>
              <Text style={styles.subLabel}>{m.partnerWhatDoes}</Text>
              <Pills
                testID="ptype"
                options={(["COLLECTOR", "RECOVERER"] as PartnerType[]).map((v) => ({ value: v, label: e.partnerType[v] }))}
                value={type}
                onChange={setType}
              />
              <Text style={styles.subLabel}>{m.partnerWhoInvoices}</Text>
              <Pills
                testID="prole"
                options={[
                  { value: "supplier" as const, label: e.partnerRole.supplier },
                  { value: "client" as const, label: e.partnerRole.client },
                ]}
                value={role}
                onChange={setRole}
              />
              <Text style={styles.subLabel}>{strings.partners.authorizationNumber}</Text>
              <Input
                value={authorizationNumber}
                onChangeText={setAuthorizationNumber}
                placeholder={strings.partners.authorizationNumberPlaceholder}
                testID="pauth"
              />
              {addError ? <Text style={styles.warn}>{addError}</Text> : null}
              <PrimaryButton label={m.addPartner} onPress={add} disabled={!type || !authorizationNumber.trim() || adding} testID="add-partner" />
            </>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

/** Codul de deșeu ca rând de card: pubela, codul mono, denumirea; bifa pe cel ales. */
function CodeRow({ code, chosen, onPress, testID }: { code: WasteCode; chosen?: boolean; onPress: () => void; testID: string }) {
  return (
    <Pressable
      onPress={onPress}
      testID={testID}
      accessibilityRole="button"
      accessibilityState={{ selected: !!chosen }}
      style={({ pressed }) => [styles.codeRow, pressed && { opacity: 0.7 }]}
    >
      <View style={styles.codeBin}>
        <Bin code={code.code} hazardous={code.hazardous} />
      </View>
      <View style={{ flex: 1 }}>
        <Text style={rowStyles.mono}>{code.code}</Text>
        <Text style={rowStyles.sub} numberOfLines={2}>
          {code.name}
        </Text>
      </View>
      {chosen ? (
        <View style={styles.codeChosen}>
          <Icon name="check" size={16} color={colors.onAccent} strokeWidth={2.6} />
          <Text style={styles.codeChange}>{m.change}</Text>
        </View>
      ) : (
        <Icon name="right" size={18} color={colors.ink3} />
      )}
    </Pressable>
  );
}

/** O dată a Anexei 3 (opțională): selectorul telefonului și „Șterge” când e pusă. */
function DateRow({ value, onChange, hint, testID }: { value: string; onChange: (iso: string) => void; hint?: string; testID: string }) {
  return (
    <View style={{ gap: 8 }}>
      <DateField value={value || null} onChange={onChange} testID={testID} min={new Date(2000, 0, 1)} max={new Date(new Date().getFullYear() + 10, 11, 31)} />
      {hint ? <Text style={styles.hint}>{hint}</Text> : null}
      {value ? (
        <Pressable onPress={() => onChange("")} testID={`${testID}-clear`} hitSlop={8} style={{ alignSelf: "flex-end" }}>
          <Text style={styles.linkText}>{m.clearDate}</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

function ChosenRow({ title, sub, onChange }: { title: string; sub: string; onChange: () => void }) {
  return (
    <View style={styles.chosen}>
      <View style={{ flex: 1 }}>
        <Text style={rowStyles.title}>{title}</Text>
        {sub ? (
          <Text style={rowStyles.sub} numberOfLines={2}>
            {sub}
          </Text>
        ) : null}
      </View>
      <PrimaryButton tone="quiet" label={m.change} onPress={onChange} />
    </View>
  );
}

function OptionRow({ title, sub, onPress, testID }: { title: string; sub: string; onPress: () => void; testID?: string }) {
  return (
    <Text onPress={onPress} testID={testID} style={styles.option} suppressHighlighting={false}>
      <Text style={rowStyles.mono}>{title}</Text>
      {sub ? <Text style={rowStyles.sub}>{`  ${sub}`}</Text> : null}
    </Text>
  );
}

/** Pastilele unui nomenclator scurt, cu eticheta întreagă. */
function nomenclator<T extends string>(labels: Record<T, string>) {
  return (Object.keys(labels) as T[]).map((v) => ({ value: v, label: labels[v] }));
}

/** Pastilele unui nomenclator cu etichete lungi: numai codul; denumirea stă în rezumatul rândului, după alegere. */
function codes<T extends string>(labels: Record<T, string>) {
  return (Object.keys(labels) as T[]).map((v) => ({ value: v, label: v }));
}

function Sep() {
  return <View style={rowStyles.sep} />;
}

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  // Loc jos pentru butonul lipit și nota lui.
  scroll: { paddingBottom: 170 },
  body: { paddingHorizontal: 16, paddingTop: 8, gap: 8 },
  secHead: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingRight: 4 },
  photoBox: { borderRadius: radius.group, overflow: "hidden", height: 180, backgroundColor: colors.lcd },
  photo: { width: "100%", height: "100%" },
  photoRemove: { alignSelf: "flex-end", paddingHorizontal: 4, paddingVertical: 2 },
  photoRemoveText: { fontFamily: fonts.sansMedium, fontSize: 15, color: colors.greenText },
  readingOverlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: "rgba(8,12,10,0.6)",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  },
  readingText: { fontFamily: fonts.monoMedium, fontSize: 14, color: colors.lcdDigit, letterSpacing: 0.6 },
  hint: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.ink2, paddingHorizontal: 4 },
  warn: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.redText },
  subLabel: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.ink2, marginTop: 4 },
  linkText: { fontFamily: fonts.sansMedium, fontSize: 14, color: colors.greenText },
  switchRow: { flexDirection: "row", alignItems: "center", gap: 12, paddingHorizontal: 16, paddingVertical: 12 },
  switchText: { flex: 1 },
  chosen: { flexDirection: "row", alignItems: "center", gap: 12 },
  option: { paddingVertical: 10, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  unknown: { backgroundColor: colors.ground, borderRadius: 12, padding: 12, gap: 8 },
  unknownText: { fontFamily: fonts.sansMedium, fontSize: 14.5, color: colors.ink },
  codeRow: { flexDirection: "row", alignItems: "center", gap: 12, paddingVertical: 10 },
  codeBin: { width: 36, height: 36, borderRadius: 10, backgroundColor: colors.greenSoft, alignItems: "center", justifyContent: "center" },
  codeChosen: { flexDirection: "row", alignItems: "center", gap: 6, backgroundColor: colors.green, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 6 },
  codeChange: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.onAccent },
  // Primul card al pasului 2: propunerea din ultima predare, pe hârtie, cu contur (fără bloc grafit — paleta A).
  lastCard: { backgroundColor: colors.card, borderRadius: radius.group, borderWidth: 1, borderColor: colors.green, padding: 16, gap: 8 },
  lastHead: { flexDirection: "row", justifyContent: "space-between" },
  lastKicker: { fontFamily: fonts.mono, fontSize: 11, letterSpacing: 1, color: colors.greenText },
  lastTitle: { fontFamily: fonts.sansSemiBold, fontSize: 18, color: colors.ink },
  lastSub: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2 },
  lastFills: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink3 },
});
