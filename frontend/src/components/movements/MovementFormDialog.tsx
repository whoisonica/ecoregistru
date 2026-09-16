import { useCallback, useMemo, useRef, useState, type FormEvent } from "react";
import { ArrowRight, Trash2, Paperclip } from "lucide-react";
import { usePartners } from "@/hooks/usePartners";
import { useDrivers } from "@/hooks/useDrivers";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useWasteCodeSearch } from "@/hooks/useWasteCodes";
import {
  useCreateMovement,
  useUpdateMovement,
  useAddAttachment,
  useDeleteAttachment,
} from "@/hooks/useMovements";
import type {
  PackagingCategory,
  PackagingMaterial,
  PackagingOrigin,
  TransportDestination,
  TransportMeans,
  WasteDestination,
  MovementDirection,
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
import { withCount } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Combobox, type ComboboxItem } from "@/components/ui/combobox";
import { FileDropzone } from "@/components/ui/file-dropzone";
import { Dialog } from "@/components/ui/dialog";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { FormSection } from "@/components/ui/form-section";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { partnerRoleLabel } from "@/components/PartnerRoleBadge";
import { useAnexa2Threshold } from "@/hooks/useAnexa2";
import { useAttachmentOpen } from "@/hooks/useAttachment";
import {
  suggestedPackagingMaterial,
  operationsFor,
  isExit,
  ALL_CODES,
  R_CODES,
  D_CODES,
  suggestedDestinations,
  todayIso,
  type FieldErrors,
  type ExitOperation,
} from "@/components/movements/movementRules";
import { PackagingFields } from "@/components/movements/PackagingFields";
import { Anexa2Fields } from "@/components/movements/Anexa2Fields";

const t = strings.movements;
const e = strings.enums;

/** Nota 5 a fișei, cum o folosesc clienții: groapa orașului, incinerare, valorificare, altele. */
const OFFERED_DESTINATIONS: string[] = ["DO", "I", "Vr", "A"];

/**
 * Un cod din Lista europeană, citit după denumire (proprietarul, 16.09.2026): denumirea întâi,
 * codul lângă ea și dedesubt, ca lista să se parcurgă după ce e deșeul, nu după cifre.
 */
function wasteCodeItem(id: string, code: string, name: string, hazardous: boolean): ComboboxItem {
  const title = name.charAt(0).toUpperCase() + name.slice(1);
  return {
    id,
    label: `${title} (${code})`,
    sublabel: hazardous ? `${code} · ${t.hazardous}` : code,
  };
}

interface MovementFormDialogProps {
  editing: WasteMovement | null;
  /**
   * Mișcarea de la care pornește una nouă. Se citește la fel ca `editing` pentru valorile de
   * pornire, dar **nu** face din formular o editare: se salvează o înregistrare nouă.
   */
  duplicateOf?: WasteMovement | null;
  workPoints: { id: string; name: string }[];
  defaultWorkPointId?: string;
  /** Ecranul din care s-a deschis — și deci registrul în care intră cantitatea. */
  screen: WasteRegister;
  /** Pe art. 48, direcția ecranului: „Intrări" pornește pe preluare, „Ieșiri" pe valorificare. */
  direction?: MovementDirection;
  onClose: () => void;
}

export function MovementFormDialog({
  editing,
  duplicateOf,
  workPoints,
  defaultWorkPointId,
  screen,
  direction,
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
  const { open: openAttachment, openingId } = useAttachmentOpen();
  const { data: partners } = usePartners();
  const { data: drivers } = useDrivers();
  const { data: company } = useCurrentCompany();

  const [workPointId, setWorkPointId] = useState(() => {
    const preferred = initial?.workPointId ?? defaultWorkPointId ?? "";
    // La editare, valoarea mișcării rămâne oricare ar fi — punctul ei apare în opțiuni chiar dacă
    // a fost dezactivat între timp. La una nouă, implicitul vine din filtrul ecranului, care poate
    // purta un `?punct=` rămas în adresă către un punct care nu mai e activ; atunci cade pe primul
    // din listă, ca să nu pornim cu un id pe care select-ul n-are cum să-l arate.
    if (initial || workPoints.some((w) => w.id === preferred)) return preferred;
    return workPoints[0]?.id ?? "";
  });
  /**
   * Ce se oferă în select: punctele active, plus cel al mișcării editate dacă între timp a fost
   * dezactivat. O mișcare veche trebuie să rămână salvabilă fără să-și piardă tăcut amplasamentul —
   * același tratament pe care îl primesc deja codul de deșeu și codul R/D din profil.
   */
  const workPointOptions = useMemo(() => {
    const options = workPoints.map((w) => ({ id: w.id, name: w.name, inactive: false }));
    if (initial?.workPointId && !options.some((w) => w.id === initial.workPointId)) {
      options.push({ id: initial.workPointId, name: initial.workPointName, inactive: true });
    }
    return options;
  }, [workPoints, initial?.workPointId, initial?.workPointName]);
  const [date, setDate] = useState(editing?.date ?? todayIso());
  const [wasteCode, setWasteCode] = useState<ComboboxItem | null>(
    initial ? wasteCodeItem(initial.wasteCodeId, initial.wasteCode, initial.wasteCodeName, initial.hazardous) : null
  );
  /**
   * Codul și periculozitatea codului ales, ținute separat de ce scrie în listă. Lista se citește
   * după denumire (proprietarul, 16.09.2026), deci eticheta nu mai începe cu codul, iar regulile de
   * mai jos — ambalaj, periculos, cap. 18 — nu se mai pot sprijini pe ea.
   */
  const [codeMeta, setCodeMeta] = useState<{ code: string; hazardous: boolean } | null>(
    initial ? { code: initial.wasteCode, hazardous: initial.hazardous } : null
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
    initialOwnExit
      ? "GENERATED"
      : (initial?.operation ??
        (screen === "ANEXA_1" ? "GENERATED" : direction === "OUT" ? "RECOVERED" : "COLLECTED"))
  );
  const [fate, setFate] = useState<ExitOperation | "">(
    initialOwnExit ? (initial.operation as ExitOperation) : ""
  );
  const [physicalState, setPhysicalState] = useState<PhysicalState | "">(
    initial?.physicalState ?? ""
  );
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
  const chosenPartner = useMemo(
    () => (partners ?? []).find((p) => p.id === partnerId),
    [partners, partnerId]
  );
  const recipientWorkPoints = chosenPartner?.workPoints ?? [];
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
  // `editing?`, nu `initial?`: la duplicare, data descărcării e una din cele care **chiar** diferă
  // între două transporturi, ca data și numărul documentului. Duplicând o predare din martie o
  // porneai cu încărcarea azi și descărcarea în martie — pe un formular semnat de destinatar.
  const [loadDate, setLoadDate] = useState(editing?.loadDate ?? "");
  const [unloadDate, setUnloadDate] = useState(editing?.unloadDate ?? "");
  // Null = "ca la firmă": alegerea de pe firmă (V19), iar în lipsa ei unitatea mișcării.
  const [anexa3Unit, setAnexa3Unit] = useState<Unit | "">(initial?.anexa3Unit ?? "");
  const [transportPartnerId, setTransportPartnerId] = useState(initial?.transportPartnerId ?? "");
  const [driverName, setDriverName] = useState(initial?.driverName ?? "");
  const [driverIdentification, setDriverIdentification] = useState(
    initial?.driverIdentification ?? ""
  );
  const [driverCnp, setDriverCnp] = useState(initial?.driverCnp ?? "");
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
  // --- Anexa 2, cele patru rubrici pe care formularul de transport periculos le cere în plus ---
  // `editing?`, nu `initial?`, la numărul formularului: numărul îl dă agenția pentru **un**
  // transport, deci duplicând o predare ai duplica numărul altcuiva pe o hârtie nouă.
  const [anexa2Number, setAnexa2Number] = useState(editing?.anexa2Number ?? "");
  const [anexa2ApprovalNumber, setAnexa2ApprovalNumber] = useState(
    editing?.anexa2ApprovalNumber ?? ""
  );
  const [anexa2Packaging, setAnexa2Packaging] = useState(initial?.anexa2Packaging ?? "");
  /**
   * Bifa „< 1t/an", în trei stări. `""` înseamnă „cum reiese din evidență" și e implicitul: atunci
   * formularul tipărește ce propune cumulul anual pe cod, iar propunerea se mișcă singură când mai
   * intră mișcări. `"true"`/`"false"` e răspunsul omului, și el bate propunerea — actul nu
   * definește „aceeași categorie", deci ultimul cuvânt e al celui care semnează.
   */
  const [anexa2BelowOneTon, setAnexa2BelowOneTon] = useState<"" | "true" | "false">(
    initial?.anexa2BelowOneTon == null ? "" : initial.anexa2BelowOneTon ? "true" : "false"
  );
  // Adevărat cât timp bifele sunt ale noastre, nu ale lui: atunci scrie sub ele de unde vin.
  const [destinationsPrefilled, setDestinationsPrefilled] = useState(false);
  const [notes, setNotes] = useState(initial?.notes ?? "");
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [errors, setErrors] = useState<FieldErrors>({});
  /**
   * Garda de la închiderea accidentală.
   *
   * <p>Formularul are treizeci de rubrici în opt secțiuni, iar Escape sau un clic pe fundal le
   * ștergeau pe toate fără o vorbă. E jumătatea cealaltă a defectului reparat la Escape-ul din
   * combobox — acolo se pierdea tot fiindcă tasta trecea prin listă la dialog; aici se pierdea
   * tot fiindcă dialogul făcea exact ce i se cerea.
   *
   * <p>Se marchează din `onChange`-ul formularului, nu din cele treizeci de `setState`: evenimentul
   * urcă din orice rubrică nativă — text, select, bifă, chiar și căutarea din combobox — deci o
   * rubrică adăugată mâine intră singură sub gardă. Cele două căi care **nu** trec prin el (alegerea
   * unui cod din listă și fișierele lăsate cu mouse-ul peste zonă) marchează pe față, mai jos.
   */
  const [dirty, setDirty] = useState(false);
  const markDirty = useCallback(() => setDirty(true), []);
  const [confirmClose, closeConfirmation] = useConfirm();
  const formRef = useRef<HTMLFormElement>(null);
  /**
   * Al câtelea fișier se urcă acum. Urcarea e secvențială — și rămâne așa, fiindcă backendul
   * primește câte unul — dar până acum nu se vedea nimic: o scanare de câțiva megaocteți pe
   * conexiunea din depozit arăta ca o aplicație blocată, iar reflexul era să se apese din nou.
   */
  const [upload, setUpload] = useState<{ index: number; total: number; name: string } | null>(
    null
  );
  /**
   * Cheia de idempotență a mișcării care se creează din formularul ăsta — una singură, cât trăiește
   * dialogul.
   *
   * <p>Se genera în `buildInput()`, care se apelează **la fiecare** apăsare pe Salvează. Iar
   * salvarea are două jumătăți: mișcarea, apoi atașamentele, una câte una. Dacă al doilea fișier
   * cădea — scanare de câțiva megaocteți, conexiunea din depozit — mișcarea era deja creată, dar
   * mesajul spunea „Salvarea a eșuat" și dialogul rămânea deschis. Omul apăsa din nou, `buildInput`
   * scotea alt UUID, backendul n-avea pe ce să recunoască cererea, și ieșeau **două mișcări** cu
   * aceeași cantitate în aceeași lună — adică o dublare a cifrei chiar în fișa de gestiune.
   *
   * <p>Ținută într-un `useRef`, cheia e aceeași la a doua încercare, deci a doua cerere se
   * recunoaște ca fiind aceeași faptă. Exact la ce servește `clientGeneratedId`.
   */
  const idempotencyKey = useRef(crypto.randomUUID());

  const codeSearch = useWasteCodeSearch(codeQuery);

  // The waste codes on the account's authorization. With a profile answered, the picker opens on
  // those four or five instead of on the 842 of the European List, and typing still searches the
  // whole nomenclator — a code that turns up once a year must stay reachable.
  const profileWasteCodes = company?.authorizedWasteCodes ?? [];
  const searchResults = codeSearch.data ?? [];
  const shownCodes =
    profileWasteCodes.length > 0 && !codeQuery.trim() ? profileWasteCodes : searchResults;
  const codeItems: ComboboxItem[] = shownCodes.map((w) =>
    wasteCodeItem(w.id, w.code, w.name, w.hazardous)
  );

  // O mișcare veche poate purta o operațiune pe care ecranul n-o mai oferă (generarea unui colector
  // pur, de dinainte de cele două ecrane): rămâne în listă, ca rândul să se poată salva neschimbat.
  const offered = operationsFor(screen, direction);
  const operations =
    offered.includes(operation) || operation === "UNCLASSIFIED_OUT" ? offered : [operation, ...offered];
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
   * Registrul unei ieşiri. Până pe 14.09.2026 formularul întreba provenienţa la ieşirea directă a
   * unui cont care preia de la terţi; de atunci o spune ecranul: „Generare" e deşeul firmei
   * (Anexa 1), „Intrări şi ieşiri" e marfa preluată (art. 48). Generarea rămâne pe Anexa 1 oriunde
   * s-ar edita, iar o linie veche fără cod îşi păstrează registrul ei. Fără registru, aceeaşi
   * valorificare ar cădea pe Anexa 1, deci marfa altcuiva s-ar declara ca deşeul firmei.
   * La preluare nu se trimite nimic: backendul o forţează pe art. 48.
   */
  const screenRegister: WasteRegister | null = !requiresCode
    ? null
    : operation === "GENERATED"
      ? "ANEXA_1"
      : operation === "UNCLASSIFIED_OUT"
        ? (initial?.register ?? null)
        : screen;
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
  const isPackagingCode = (codeMeta?.code ?? "").startsWith("15 01");
  const suggestedMaterial = suggestedPackagingMaterial(codeMeta?.code ?? "");

  /**
   * Cele două formulare de transport din HG 1061/2008 sunt aceeași întrebare cu răspuns opus:
   * anexa 3 pentru nepericuloase, anexa 2 pentru periculoase. Blocul de transport e comun —
   * transportator, delegat, mașină, cele cinci bife — și doar rubricile proprii diferă.
   */
  const isHazardousCode = codeMeta?.hazardous ?? false;
  // Capitolul 18: art. 24 dă formularul transportatorului, pe rută. Nu e o variantă a Anexei 2,
  // e alt flux — deci nu se oferă, se explică.
  const isMedicalCode = isHazardousCode && (codeMeta?.code ?? "").startsWith("18");
  const showTransportSection = isExit(effectiveOperation) && Boolean(partnerId);
  const showAnexa3Section = showTransportSection && !isHazardousCode;
  // Specialista, 14.09.2026: „anexa 2 o păstrăm doar pentru colectori". La un generator blocul de
  // transport rămâne — șoferul și mașina se țin oricum —, dar fără rubricile formularului.
  const collectorForms = company != null && company.type !== "GENERATOR";
  const showAnexa2Section =
    showTransportSection && isHazardousCode && !isMedicalCode && collectorForms;

  /**
   * Cifra din spatele bifei „< 1t/an" — cerută numai pentru o mișcare deja salvată, fiindcă pragul
   * se citește din evidența anului și n-are ce răspunde pentru una care încă nu există.
   */
  const { data: anexa2Threshold } = useAnexa2Threshold(editing?.id, showAnexa2Section);
  /**
   * Ce va tipări formularul: răspunsul omului dacă l-a dat, altfel propunerea. `null` înseamnă că
   * încă nu știm niciuna — mișcare nesalvată — și atunci ecranul nu inventează un răspuns.
   */
  const anexa2Effective =
    anexa2BelowOneTon === ""
      ? (anexa2Threshold?.belowOneTon ?? null)
      : anexa2BelowOneTon === "true";

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
    // Registrul, același pe care îl trimite `buildInput`.
    const effectiveRegister = screenRegister;
    if (effectiveRegister === "ANEXA_1") out.push(t.effectAnexa1);
    // La preluare registrul îl forțează backendul, deci nu se citește din `register`.
    if (effectiveRegister === "ART_48" || operation === "COLLECTED") out.push(t.effectArt48);
    if (effectiveOperation === "RECOVERED") {
      out.push(
        t.effectRecovered.replace("{code}", operationCode || "—")
      );
    } else if (effectiveOperation === "DISPOSED") {
      out.push(t.effectDisposed.replace("{code}", operationCode || "—"));
    }
    if (isPackagingCode && packagingOnMarket !== false) out.push(t.effectPackaging);
    if (showAnexa3Section) out.push(t.effectAnexa3);
    if (showAnexa2Section) out.push(t.effectAnexa2);
    return out;
  }, [
    wasteCode,
    screenRegister,
    requiresCode,
    operation,
    effectiveOperation,
    operationCode,
    isPackagingCode,
    packagingOnMarket,
    showAnexa3Section,
    showAnexa2Section,
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
    // Generarea nu mai rămâne în stoc (proprietarul, 16.09.2026): un generator n-are cântar, află
    // cantitatea abia la predare, de pe tichetul colectorului — deci rândul e mereu o predare.
    if (showsFate && operation === "GENERATED" && !fate) errs.fate = t.fateRequired;
    if (effectiveOperation === "RECOVERED" && (!operationCode || !operationCode.startsWith("R")))
      errs.operationCode = t.recoveryCodeRequired;
    if (effectiveOperation === "DISPOSED" && (!operationCode || !operationCode.startsWith("D")))
      errs.operationCode = t.disposalCodeRequired;
    // Cine preia deșeul trebuie să aibă autorizație de mediu (proprietarul, 16.09.2026). Se cere
    // numărul din fișa partenerului; expirarea rămâne avertisment, ca până acum (decizia 36).
    if (requiresCode && partnerId && !chosenPartner?.authorizationNumber?.trim())
      errs.partnerId = t.partnerNeedsAuthorization;
    // Nota 5 a fișei: pe o predare de deșeu propriu destinația nu rămâne goală.
    if (screenRegister === "ANEXA_1" && !wasteDestination) errs.wasteDestination = t.wasteDestinationRequired;
    if (isLegacyExit) errs.form = t.legacyExitHint;
    return errs;
  }

  function buildInput(): WasteMovementInput {
    return {
      clientGeneratedId: editing ? undefined : idempotencyKey.current,
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
      // Numai la ieşire, şi explicit, fiindcă backendul cere registrul la orice ieşire de pe un cont
      // care ţine şi art. 48 (decizia 23). Nu se ghiceşte nimic: îl spune ecranul ales de om.
      register: screenRegister,
      partnerId: partnerId || null,
      internalGeneratorId: internalGeneratorId || null,
      documentReference: documentReference.trim() || null,
      notes: notes.trim() || null,
      loadDate: loadDate || null,
      unloadDate: unloadDate || null,
      partnerWorkPointId: partnerWorkPointId || null,
      anexa3Unit: anexa3Unit || null,
      transportPartnerId: transportPartnerId || null,
      driverName: driverName.trim() || null,
      driverIdentification: driverIdentification.trim() || null,
      driverCnp: driverCnp.trim() || null,
      vehicleRegistration: vehicleRegistration.trim() || null,
      transportDestinations,
      anexa2Number: anexa2Number.trim() || null,
      anexa2ApprovalNumber: anexa2ApprovalNumber.trim() || null,
      anexa2Packaging: anexa2Packaging.trim() || null,
      anexa2BelowOneTon: anexa2BelowOneTon === "" ? null : anexa2BelowOneTon === "true",
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

    // Salvarea are două jumătăți, iar ele nu eșuează la fel: mișcarea e cantitatea din fișă,
    // atașamentele sunt hârtii lângă ea. Ținute într-un singur `try`, o urcare căzută spunea
    // „Salvarea a eșuat" peste o mișcare deja înregistrată — și reflexul, apăsatul din nou, o
    // înregistra a doua oară.
    let movementId: string;
    try {
      movementId = editing
        ? (await updateMut.mutateAsync({ id: editing.id, input }), editing.id)
        : (await createMut.mutateAsync(input)).id;
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
      return;
    }

    const failed: File[] = [];
    let firstError: unknown = null;
    for (const [index, file] of pendingFiles.entries()) {
      setUpload({ index: index + 1, total: pendingFiles.length, name: file.name });
      try {
        await addAttachmentMut.mutateAsync({ movementId, file });
      } catch (err) {
        failed.push(file);
        firstError ??= err;
      }
    }
    // Și pe eroare: altfel bara ar rămâne pe ecran peste un formular care nu mai lucrează.
    setUpload(null);

    if (failed.length > 0) {
      // În coadă rămân doar cele căzute, deci a doua apăsare le urcă pe alea și nu le repetă pe
      // cele urcate. Mișcarea trece din nou pe la server, dar cu aceeași cheie de idempotență,
      // deci se recunoaște ca aceeași și nu se dublează.
      setPendingFiles(failed);
      // Mesajul nostru primul: motivul serverului („Cloudinary nu e configurat") e util, dar ce
      // trebuie citit întâi e că mișcarea **e** salvată.
      const detail = apiErrorMessage(firstError, "");
      notify(
        withCount(t.attachmentsFailedSaved, failed.length, "fișier n-a urcat", "fișiere n-au urcat") +
          (detail ? ` (${detail})` : ""),
        "error"
      );
      return;
    }

    notify(editing ? t.updated : t.created, "success");
    onClose();
  }

  /**
   * Închiderea cerută de om — Escape, clic pe fundal, „×" sau „Anulează". Toate patru trec pe
   * aici, fiindcă toate patru pierd la fel de mult; ce le deosebește e cât de ușor se apasă din
   * greșeală, nu ce rămâne în urmă. Pe un formular neatins nu întreabă nimic.
   */
  function requestClose() {
    if (!dirty) {
      onClose();
      return;
    }
    confirmClose({
      title: strings.common.discardTitle,
      message: strings.common.discardMessage,
      confirmLabel: strings.common.discardConfirm,
      tone: "danger",
      onConfirm: onClose,
    });
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
      onClose={requestClose}
      title={
        editing
          ? screen === "ANEXA_1"
            ? t.generatorEditTitle
            : direction === "IN"
              ? t.inEditTitle
              : direction === "OUT"
                ? t.outEditTitle
                : t.editTitle
          : duplicateOf
            ? t.duplicateTitle
            : screen === "ANEXA_1"
              ? t.generatorAddTitle
              : direction === "IN"
                ? t.inAddTitle
                : direction === "OUT"
                  ? t.outAddTitle
                  : t.addTitle
      }
      busy={isSaving}
      footer={
        <>
          {upload && (
            <div className="mr-auto min-w-0 text-xs text-content-muted sm:max-w-xs">
              <p className="truncate">
                {t.uploadingFile
                  .replace("{n}", String(upload.index))
                  .replace("{total}", String(upload.total))
                  .replace("{name}", upload.name)}
              </p>
              <div className="mt-1 h-1 w-full overflow-hidden rounded-full bg-surface-sunken">
                <div
                  className="h-full rounded-full bg-brand transition-all"
                  style={{ width: `${(upload.index / upload.total) * 100}%` }}
                />
              </div>
            </div>
          )}
          <Button variant="outline" onClick={requestClose} disabled={isSaving}>
            {strings.common.cancel}
          </Button>
          <Button type="submit" form="movement-form" loading={isSaving}>
            {isSaving ? strings.common.saving : strings.common.save}
          </Button>
        </>
      }
    >
      <form
        ref={formRef}
        id="movement-form"
        onSubmit={handleSubmit}
        onChange={markDirty}
        className="space-y-6"
      >
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
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="mv-wp">{t.filterWorkPoint}</Label>
              <Select
                id="mv-wp"
                value={workPointId}
                onChange={(ev) => setWorkPointId(ev.target.value)}
                {...invalidProps("mv-wp-err", errors.workPointId)}
              >
                {/* Opțiunea goală există ca select-ul să nu poată minți. Fără ea, o valoare care
                    nu se află printre opțiuni — punctul de lucru al unei mișcări vechi, între timp
                    dezactivat, sau un `?punct=` rămas în adresă — lăsa browserul să afișeze primul
                    rând din listă în timp ce starea ținea cu totul altceva. Se vedea un punct de
                    lucru ales și se salva altul. */}
                <option value="">{t.workPointPlaceholder}</option>
                {workPointOptions.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                    {w.inactive ? ` ${t.workPointInactiveSuffix}` : ""}
                  </option>
                ))}
              </Select>
              <FieldError id="mv-wp-err" message={errors.workPointId} />
            </div>
            <div>
              <Label htmlFor="mv-date">{t.date}</Label>
              <DateInput
                id="mv-date"
                value={date}
                onChange={(ev) => setDate(ev.target.value)}
                {...invalidProps("mv-date-err", errors.date)}
              />
              <FieldError id="mv-date-err" message={errors.date} />
            </div>
          </div>

          <div>
            <Label htmlFor="mv-code">{t.wasteCode}</Label>
            <Combobox
              id="mv-code"
              value={wasteCode}
              onSelect={(item) => {
                // Alegerea din listă e un clic pe un rând, nu o schimbare de rubrică: nu urcă
                // niciun `change` până la formular, deci garda se marchează aici.
                markDirty();
                setWasteCode(item);
                const picked = item ? shownCodes.find((w) => w.id === item.id) : undefined;
                setCodeMeta(picked ? { code: picked.code, hazardous: picked.hazardous } : null);
              }}
              onQueryChange={setCodeQuery}
              items={codeItems}
              loading={codeSearch.isFetching}
              placeholder={t.wasteCodePlaceholder}
              searchPlaceholder={t.wasteCodeSearch}
              invalid={errors.wasteCode ? "mv-code-err" : undefined}
            />
            <FieldError id="mv-code-err" message={errors.wasteCode} />
          </div>
        </FormSection>

        <FormSection title={t.sectionQuantity}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
                className="mt-0.5 h-4 w-4 rounded border-line-strong"
                checked={weighedAtUnloading}
                onChange={(ev) => setWeighedAtUnloading(ev.target.checked)}
              />
              <span>
                <span className="font-medium text-content-strong">{t.weighedAtUnloading}</span>
                <span className="block text-xs text-content-muted">{t.weighedAtUnloadingHint}</span>
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
                <p className="mt-1 text-xs text-content-muted">{t.volumeM3Hint}</p>
              </div>
            )}
          </div>
        </FormSection>

        <FormSection title={t.sectionOperation}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {/* Pe „Generare" operațiunea e una singură și nu se alege: rândul e mereu o predare, iar
                unde pleacă deșeul se spune mai jos, sub transport. Un select cu o singură opțiune,
                „Generare", se citea ca vechiul „rămâne în stoc" (proprietarul, 16.09.2026). */}
            {operations.length > 1 && (
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
            </div>
            )}
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

          {operations.length === 1 && (
            <p className="text-xs text-content-muted">{t.operationGeneratorHint}</p>
          )}

          {operation === "COLLECTED" && (
            <p className="rounded-md border border-line bg-surface-muted px-3 py-2 text-xs text-content-strong">
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
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
              <p className="mt-1 text-xs text-content-muted">{t.treatmentMethodHint}</p>
            </div>
            {/* Nota 3 a fișei: scopul nu se alege, îl dă codul R/D — V la valorificare, E la
                eliminare. Se arată aici ca omul să vadă ce se tipărește în coloana „Scopul". */}
            {requiresCode && (
              <div>
                <span className="block text-sm font-medium text-content-strong">{t.treatmentPurpose}</span>
                <p className="mt-1.5 font-mono text-sm text-content" data-testid="mv-purpose">
                  {effectiveOperation === "RECOVERED"
                    ? e.treatmentPurpose.V
                    : effectiveOperation === "DISPOSED"
                      ? e.treatmentPurpose.E
                      : "—"}
                </p>
                <p className="mt-1 text-xs text-content-muted">{t.treatmentPurposeHint}</p>
              </div>
            )}
          </div>
        </FormSection>

        <FormSection title={t.sectionTransport}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
                {...invalidProps("mv-destination-err", errors.wasteDestination)}
              >
                <option value="">{t.nomenclatorPlaceholder}</option>
                {/* Patru destinații (proprietarul, 16.09.2026); una veche din afara lor rămâne în
                    listă la editare, ca rândul să se poată salva neschimbat. */}
                {Object.entries(e.wasteDestination)
                  .filter(([value]) => OFFERED_DESTINATIONS.includes(value) || value === initial?.wasteDestination)
                  .map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
              </Select>
              <FieldError id="mv-destination-err" message={errors.wasteDestination} />
            </div>
          </div>

          {/* Ce se întâmplă cu deşeul stă sub transport, fiindcă de transport atârnă: „după ce alegi
              la Transport spre Valorificare să apară următoarele taburi cu codurile de valorificare,
              sau cu codurile de eliminare, în funcţie de cum o să fie transportul" (specialista,
              25.08.2026). Sus rămâne de unde vine deşeul; aici, unde ajunge. */}
          {(showsFate || requiresCode) && (
            <div className="space-y-3 rounded-md border border-line-strong p-3">
              <div>
                <span className="text-sm font-semibold text-content-strong">{t.fateTitle}</span>
                <p className="text-xs text-content-muted">{t.fateHint}</p>
              </div>

              {showsFate && (
                <div className="space-y-2">
                  {/* Ca la provenienţă: fiecare opţiune îşi spune efectul, fiindcă alegerea nu schimbă
                      un câmp, ci coloana din fişă în care intră cantitatea. */}
                  {(
                    [
                      ["RECOVERED", t.fateRecovery, t.fateRecoveryEffect],
                      ["DISPOSED", t.fateDisposal, t.fateDisposalEffect],
                    ] as const
                  ).map(([value, label, effect]) => (
                    <label key={value} className="flex cursor-pointer gap-2">
                      <input
                        type="radio"
                        name="mv-fate"
                        className="mt-1 h-4 w-4 shrink-0"
                        checked={fate === value}
                        {...(value === "RECOVERED" ? invalidProps("mv-fate-err", errors.fate) : {})}
                        onChange={() => {
                          setFate(value);
                          setOperationCode(""); // familia de coduri se schimbă cu alegerea
                        }}
                      />
                      <span>
                        <span className="text-sm font-medium">{label}</span>
                        <span className="block text-xs text-content-muted">{effect}</span>
                      </span>
                    </label>
                  ))}
                  <FieldError id="mv-fate-err" message={errors.fate} />
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
                  <p className="mt-1 text-xs text-content-muted">
                    {effectiveOperation === "DISPOSED" ? t.operationCodeHintDisposal : t.operationCodeHintRecovery}
                  </p>
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
            <p className="mt-1 text-xs text-content-muted">{t.partnerHint}</p>
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
              <p className="mt-1 text-xs text-content-muted">{t.partnerWorkPointHint}</p>
            </div>
          )}
        </FormSection>

        {isPackagingCode && (
          <PackagingFields
            collected={operation === "COLLECTED"}
            suggestedMaterial={suggestedMaterial}
            packagingOnMarket={packagingOnMarket}
            setPackagingOnMarket={setPackagingOnMarket}
            packagingOrigin={packagingOrigin}
            setPackagingOrigin={setPackagingOrigin}
            packagingMaterial={packagingMaterial}
            setPackagingMaterial={setPackagingMaterial}
            packagingCategory={packagingCategory}
            setPackagingCategory={setPackagingCategory}
            packagingReusable={packagingReusable}
            setPackagingReusable={setPackagingReusable}
            packagingHazardousContent={packagingHazardousContent}
            setPackagingHazardousContent={setPackagingHazardousContent}
          />
        )}

        {/* Anexa 3 e dovada predării, deci n-are cum să existe fără destinatar. Până acum condiţia
            era tăcută: alegeai codul, secţiunea nu apărea, şi nu scria nicăieri de ce. */}
        {requiresCode && !showTransportSection && (
          <p className="rounded-md border border-line bg-surface-muted px-3 py-2 text-xs text-content-strong">
            {isHazardousCode
              ? collectorForms
                ? t.anexa2NeedsPartner
                : t.anexa2ByCollector
              : t.anexa3NeedsPartner}
          </p>
        )}

        {/* Clinicile sunt clienți-țintă, iar art. 24 le scoate din formularul ăsta. Se spune aici,
            pe ecran, nu la control. */}
        {showTransportSection && isMedicalCode && (
          <p className="rounded-md border border-line bg-surface-muted px-3 py-2 text-xs text-content-strong">
            {t.anexa2Medical}
          </p>
        )}

        {showTransportSection && (
          <div className="space-y-3 rounded-md border border-line bg-surface-muted p-3">
            <div>
              <span className="text-sm font-semibold text-content-strong">
                {showAnexa2Section
                  ? t.anexa2Section
                  : isHazardousCode
                    ? t.sectionTransport
                    : t.anexa3Section}
              </span>
              <p className="text-xs text-content-muted">
                {showAnexa2Section
                  ? t.anexa2SectionHint
                  : isHazardousCode
                    ? isMedicalCode
                      ? ""
                      : t.anexa2ByCollector
                    : t.anexa3SectionHint}
              </p>
            </div>
            {/* Numărul de exemplare e al unui formular; fără formular, n-are ce spune. */}
            {(showAnexa2Section || !isHazardousCode) && (
              <p className="text-xs text-content-muted">
                {!showAnexa2Section
                  ? t.anexa3Copies
                  : anexa2Effective === null
                    ? t.anexa2ThresholdAfterSave
                    : anexa2Effective
                      ? t.anexa2Copies3
                      : t.anexa2Copies6}
              </p>
            )}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              {/* Ordinea cerută pe 24.08: încărcarea întâi, descărcarea după — ca pe formular.
                  Încărcarea se alege de pe 15.09.2026 (specialista); goală, se tipărește data
                  mișcării, ca înainte. */}
              <div>
                <Label htmlFor="mv-load">{t.loadDate}</Label>
                <DateInput
                  id="mv-load"
                  value={loadDate}
                  onChange={(ev) => setLoadDate(ev.target.value)}
                />
                <p className="mt-1 text-xs text-content-muted">{t.loadDateHint}</p>
              </div>
              <div>
                <Label htmlFor="mv-unload">{t.unloadDate}</Label>
                <DateInput
                  id="mv-unload"
                  value={unloadDate}
                  min={loadDate || date}
                  onChange={(ev) => setUnloadDate(ev.target.value)}
                />
              </div>
              {showAnexa2Section ? (
                /* Anexa 2 n-are unitate de ales: rubricile ei scriu „în tone" chiar pe formular.
                   În locul ei stă numărul, singurul câmp al acestui formular pe care nu-l alocăm
                   noi — îl scrie agenția (nota *1) a modelului). */
                <div>
                  <Label htmlFor="mv-anexa2-number">{t.anexa2Number}</Label>
                  <Input
                    id="mv-anexa2-number"
                    value={anexa2Number}
                    onChange={(ev) => setAnexa2Number(ev.target.value)}
                  />
                  <p className="mt-1 text-xs text-content-muted">{t.anexa2NumberHint}</p>
                </div>
              ) : (
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
                <p className="mt-1 text-xs text-content-muted">{t.anexa3UnitHint}</p>
              </div>
              )}
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
                <p className="mt-1 text-xs text-content-muted">
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
                      setDriverCnp(picked.cnp ?? "");
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
                <p className="mt-1 text-xs text-content-muted">
                  {availableDrivers.length > 0
                    ? t.driverPickHint
                    : transportPartnerId
                      ? t.driverPickNoneCarrier
                      : t.driverPickNoneOwn}
                </p>
              </div>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
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
                <Label htmlFor="mv-driver-cnp">{strings.common.cnp}</Label>
                <Input
                  id="mv-driver-cnp"
                  inputMode="numeric"
                  maxLength={13}
                  value={driverCnp}
                  onChange={(ev) => setDriverCnp(ev.target.value)}
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
              <span className="block text-sm font-medium text-content-strong">
                {t.transportDestinations}
              </span>
              <p className="text-xs text-content-muted">
                {destinationsPrefilled ? t.destinationsPrefilled : t.transportDestinationsHint}
              </p>
              <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1">
                {(Object.keys(e.transportDestination) as TransportDestination[]).map((d) => (
                  <label key={d} className="flex items-center gap-1.5 text-sm">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-line-strong"
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
            {showAnexa2Section && (
              <Anexa2Fields
                anexa2BelowOneTon={anexa2BelowOneTon}
                setAnexa2BelowOneTon={setAnexa2BelowOneTon}
                anexa2Threshold={anexa2Threshold}
                anexa2Effective={anexa2Effective}
                anexa2ApprovalNumber={anexa2ApprovalNumber}
                setAnexa2ApprovalNumber={setAnexa2ApprovalNumber}
                anexa2Packaging={anexa2Packaging}
                setAnexa2Packaging={setAnexa2Packaging}
              />
            )}
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
                    className="flex items-center justify-between gap-2 rounded border border-line px-2 py-1 text-sm"
                  >
                    <button
                      type="button"
                      onClick={() => openAttachment(editing.id, a)}
                      disabled={openingId === a.id}
                      className="flex min-w-0 items-center gap-2 text-left text-brand hover:underline disabled:opacity-60"
                    >
                      <Paperclip className="h-3.5 w-3.5 shrink-0" />
                      <span className="truncate">{a.fileName}</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDeleteAttachment(a.id)}
                      className="shrink-0 text-content-subtle hover:text-red-600"
                      aria-label={strings.common.delete}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <FileDropzone
              files={pendingFiles}
              onChange={(files) => {
                // Fișierele lăsate cu mouse-ul peste zonă nu trec prin `<input type="file">`.
                markDirty();
                setPendingFiles(files);
              }}
              disabled={isSaving}
              onReject={(message) => notify(message, "error")}
            />
          </div>
        </FormSection>
      </form>
      {closeConfirmation}
    </Dialog>
  );
}
