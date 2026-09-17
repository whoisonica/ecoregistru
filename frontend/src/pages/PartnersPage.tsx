import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { Ban, Building2, ChevronRight, Factory, MapPin, Pencil, Plus, Recycle, RotateCcw, Truck, Users, Warehouse } from "lucide-react";
import { CuiField } from "@/components/AnafLookup";
import { useCanWrite } from "@/hooks/useBillingAccess";
import {
  usePartners,
  useCreatePartner,
  useUpdatePartner,
  useDeactivatePartner,
  useReactivatePartner,
} from "@/hooks/usePartners";
import type {
  DriverInput,
  Partner,
  PackagingOrigin,
  PartnerInput,
  PartnerType,
  PartnerWorkPointInput,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useHotkey } from "@/hooks/useHotkey";
import { useUrlState } from "@/hooks/useUrlState";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { ChoiceCards } from "@/components/ui/choice-cards";
import { PillGroup } from "@/components/ui/pill-group";
import { Switch } from "@/components/ui/switch";
import { Stepper } from "@/components/ui/stepper";
import { FormStepRail } from "@/components/ui/form-steps";
import { Tooltip } from "@/components/ui/tooltip";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { missingLast, useTableView } from "@/hooks/useTableView";
import { useActiveFilter } from "@/components/ui/active-filter";
import { fold, formatDate } from "@/lib/utils";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { Menu, MenuItem } from "@/components/ui/menu";
import { PartnerRoleBadge } from "@/components/PartnerRoleBadge";
import { NaturalPersonsSection } from "@/components/NaturalPersonsSection";
import { registersFor } from "@/lib/movementScreens";

const t = strings.partners;
const typeLabels = strings.enums.partnerType;
const roleLabels = strings.enums.partnerRole;
/** Cardul „Doar le transportă”: tipul gol (`""`), care nu poate fi valoarea unui radio. */
const NONE_TYPE = "NONE" as const;
/** Pașii formularului: 0 detalii, 1 ce face, 2 autorizația, 3 puncte de lucru și șoferi. */
const AUTH_STEP = 2;
const PLACES_STEP = 3;
const LAST_STEP = PLACES_STEP;

/** „1 șofer”, „3 șoferi”, „20 de șoferi”: de la 20 în sus româna cere „de”. */
function countLabel(n: number, one: string, many: string): string {
  if (n === 1) return `1 ${one}`;
  const mod = n % 100;
  return mod === 0 || mod >= 20 ? `${n} de ${many}` : `${n} ${many}`;
}

function emptyDriver(): DriverInput {
  return { name: "", identification: "", cnp: "", vehicleRegistration: "" };
}

/**
 * Filter values. The first four are the commercial role — "none" surfaces the partners still to be
 * classified — and "carrier" is a different axis riding in the same select, because it is one value
 * and a second dropdown for it would cost more than it explains.
 */
type RoleFilter = "" | "client" | "supplier" | "none" | "carrier";

/** Formats an authorization expiry as a status badge, mirroring backend `expiringSoon`. */
function ExpiryBadge({ partner }: { partner: Partner }) {
  // V41: data care vine prima dintre expirare și sfârșitul vizei anuale.
  if (!partner.authorizationValidUntil) {
    // Colectorul sau valorificatorul cu autorizație, dar fără viză: galben, nu roșu. Legea nu-i
    // cere generatorului să noteze viza; semnul spune doar ce e de cerut (17.09.2026). Dacă lipsa
    // ei ar trebui să oprească predarea e întrebarea BB pentru specialistă (intrebari-specialist.md).
    if (partner.authorizationNumber && (partner.type === "COLLECTOR" || partner.type === "RECOVERER")) {
      return <Badge variant="warning">{t.visaMissing}</Badge>;
    }
    return <span className="text-content-subtle">{t.noAuthorization}</span>;
  }
  const date = partner.authorizationValidUntil;
  const isExpired = new Date(date) < new Date(new Date().toDateString());
  if (isExpired) {
    return <Badge variant="danger">{t.expired}</Badge>;
  }
  // Data se scrie cum se scrie în România. Aici rămăsese ISO brut — `2026-11-15` lângă
  // `15.11.2026` pe Mișcări și pe Termene, în același produs.
  if (partner.expiringSoon) {
    return <Badge variant="warning">{`${t.expiringSoon} · ${formatDate(date)}`}</Badge>;
  }
  return <Badge variant="success">{formatDate(date)}</Badge>;
}

/**
 * Rândul de sub numele partenerului: punctele de lucru și șoferii lui, la vedere. Până pe 17.09.2026
 * stăteau doar în fișă, la coada pasului 1 și sub un card de transport, deci cine voia să adauge
 * unul mai târziu nu știa unde. Numărul deschide fișa pe pasul lor; „+” deschide și un rând gol.
 * Șoferii se arată numai la cine transportă: doar acolo îi alege formularul de mișcare.
 */
function PartnerPlaces({
  partner,
  canManage,
  onOpen,
}: {
  partner: Partner;
  canManage: boolean;
  onOpen: (addRow?: "workPoint" | "driver") => void;
}) {
  const wpCount = partner.workPoints?.length ?? 0;
  const driverCount = partner.carrier ? partner.drivers?.length ?? 0 : 0;
  const items: { key: string; icon: typeof MapPin; label: string; addRow?: "workPoint" | "driver" }[] = [];
  if (wpCount > 0) {
    items.push({ key: "wp", icon: MapPin, label: countLabel(wpCount, t.workPointOne, t.workPointMany) });
  } else if (canManage) {
    items.push({ key: "wp", icon: Plus, label: t.addWorkPointShort, addRow: "workPoint" });
  }
  if (driverCount > 0) {
    items.push({ key: "dr", icon: Truck, label: countLabel(driverCount, t.driverOne, t.driverMany) });
  } else if (canManage && partner.carrier) {
    items.push({ key: "dr", icon: Plus, label: t.addDriverShort, addRow: "driver" });
  }
  if (items.length === 0) return null;
  return (
    <div className="mt-0.5 flex flex-wrap gap-x-3 gap-y-0.5 text-xs">
      {items.map(({ key, icon: Icon, label, addRow }) =>
        canManage ? (
          <button
            key={key}
            type="button"
            onClick={() => onOpen(addRow)}
            className={
              "inline-flex items-center gap-1 whitespace-nowrap underline-offset-2 hover:underline " +
              (addRow ? "text-brand-700" : "text-content-muted hover:text-content")
            }
          >
            <Icon aria-hidden className="h-3 w-3" />
            {label}
          </button>
        ) : (
          <span key={key} className="inline-flex items-center gap-1 whitespace-nowrap text-content-muted">
            <Icon aria-hidden className="h-3 w-3" />
            {label}
          </span>
        )
      )}
    </div>
  );
}

/**
 * Aniversarea emiterii care vine după `after` — propunerea pentru perioada vizei. Procedura
 * (Ordinul 1150/2020, art. 5 alin. (4)) socotește anul de viză de la ziua și luna emiterii. Doar
 * propunere: perioada o scrie agenția pe decizie. 29 februarie cade pe 28 în anii fără el.
 */
function nextAnniversary(issueDate: string, after: string): string {
  const month = issueDate.slice(5, 7);
  const day = month === "02" && issueDate.slice(8, 10) === "29" ? "28" : issueDate.slice(8, 10);
  const year = Number(after.slice(0, 4));
  const sameYear = `${year}-${month}-${day}`;
  return sameYear > after ? sameYear : `${year + 1}-${month}-${day}`;
}

export function PartnersPage() {
  const canManage = useCanWrite();

  const { data: partners, isLoading, isError } = usePartners();
  const { data: company } = useCurrentCompany();
  // D1.7b: persoanele fizice sunt ale depozitului, deci tabul apare doar la firma cu art. 48.
  const hasDepot = Boolean(company) && registersFor(company?.type).includes("ART_48");
  const [tab, setTab] = useUrlState("tab");
  const personsTab = hasDepot && tab === "persoane-fizice";
  const createMut = useCreatePartner();
  const updateMut = useUpdatePartner();
  const deactivateMut = useDeactivatePartner();
  const reactivateMut = useReactivatePartner();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [step, setStep] = useState(0);
  /** „Detalii de pe hârtii” deschis: la editare, dacă e ceva completat acolo, ca nimic să nu pară pierdut. */
  const [authDetailsOpen, setAuthDetailsOpen] = useState(false);
  /** Bifa „Autorizație integrată veche, cu termen?”: arată data expirării. */
  const [hasOldExpiry, setHasOldExpiry] = useState(false);
  const [editing, setEditing] = useState<Partner | null>(null);
  const [name, setName] = useState("");
  const [cui, setCui] = useState("");
  const [authorizationNumber, setAuthorizationNumber] = useState("");
  const [authorizationExpiry, setAuthorizationExpiry] = useState("");
  const [authorizationIssueDate, setAuthorizationIssueDate] = useState("");
  const [visaDecisionNumber, setVisaDecisionNumber] = useState("");
  const [visaDecisionDate, setVisaDecisionDate] = useState("");
  const [visaValidUntil, setVisaValidUntil] = useState("");
  // "" = „doar transportator": o firmă de transport pură nu face nimic cu deșeul, deci n-are tip.
  const [type, setType] = useState<PartnerType | "">("COLLECTOR");
  const [isClient, setIsClient] = useState(false);
  const [isSupplier, setIsSupplier] = useState(true);
  const [address, setAddress] = useState("");
  const [workPoints, setWorkPoints] = useState<PartnerWorkPointInput[]>([]);
  const [isCarrier, setIsCarrier] = useState(false);
  // Provenienţa pentru Anexa 3 Ambalaje. Stă aici, nu pe mişcare, fiindcă nota 2 a anexei
  // descrie sursa, nu transportul: un colector de la care cumperi e colector la fiecare
  // transport. „Populaţie" lipseşte dinadins din listă — o persoană fizică nu e partener.
  const [packagingOrigin, setPackagingOrigin] = useState<"" | PackagingOrigin>("");
  const [drivers, setDrivers] = useState<DriverInput[]>([]);
  const [tradeRegisterNumber, setTradeRegisterNumber] = useState("");
  // Vehicule peste 3,5 t: numai atunci se cere licența de transport (specialista, 15.09.2026).
  const [heavyVehicles, setHeavyVehicles] = useState(false);
  const [transportLicenseNumber, setTransportLicenseNumber] = useState("");
  const [transportLicenseExpiry, setTransportLicenseExpiry] = useState("");
  /**
   * Numele tastat înainte de a apăsa pe o sugestie de duplicat. `null` = dialogul s-a deschis
   * normal. Ţine două lucruri deodată: că **s-a comutat**, şi ce se pune la loc dacă omul se
   * răzgândeşte.
   */
  const [switchedFrom, setSwitchedFrom] = useState<string | null>(null);
  const [nameError, setNameError] = useState(false);
  const [roleError, setRoleError] = useState(false);
  const [typeError, setTypeError] = useState(false);
  const [authError, setAuthError] = useState(false);
  /** Cui îi predai deșeul: colectorul și valorificatorul. Lor li se cere autorizația (16.09.2026). */
  const needsAuthorization = type === "COLLECTOR" || type === "RECOVERER";
  const [roleFilterRaw, setRoleFilter] = useUrlState("rol");
  const roleFilter = roleFilterRaw as RoleFilter;

  /**
   * Ce parteneri are deja firma, potriviti pe ce s-a tastat. Doua litere e pragul cerut pe
   * 24.08.2026: "cand adaugi partener si scrii sa apara din db ce clienti sunt dupa primele 2
   * litere". Se cauta si la inceput, si in interiorul numelui, fiindca "SC Retim SA" se cauta la
   * fel de des dupa "re" ca dupa "sc".
   *
   * Nu e un apel nou: lista partenerilor e deja incarcata pentru tabel, si e a tenantului. La
   * editare nu se sugereaza nimic — partenerul exista deja, iar propriul nume nu e un duplicat.
   */
  // `fold` aici nu e doar comoditate, ca la căutarea din tabel: sugestia asta există ca să nu se
  // creeze un partener de două ori. Cine tastează „deseuri" nu vedea „Transport Deșeuri SRL", deci
  // îl adăuga încă o dată — iar duplicatul rămâne în nomenclator şi pe documentele tipărite.
  const nameSuggestions =
    editing || name.trim().length < 2
      ? []
      : (partners ?? [])
          .filter((p) => fold(p.name).includes(fold(name.trim())))
          .slice(0, 5);

  /**
   * S-a completat şi altceva în afară de nume? Se citeşte din starea care există deja, fără să se
   * mai adauge un „touched": rubricile astea sunt exact cele pe care le-ar arunca o comutare.
   * Tipul şi rolurile nu intră — au implicit la deschidere, deci n-ar deosebi nimic.
   */
  const formHasMoreThanName = Boolean(
    cui ||
      address ||
      authorizationNumber ||
      authorizationExpiry ||
      authorizationIssueDate ||
      visaDecisionNumber ||
      visaDecisionDate ||
      visaValidUntil ||
      tradeRegisterNumber ||
      transportLicenseNumber ||
      transportLicenseExpiry ||
      packagingOrigin ||
      drivers.length > 0 ||
      workPoints.length > 0
  );

  const filteredByRole = useMemo(
    () =>
      (partners ?? []).filter((p) => {
        if (roleFilter === "client") return p.client;
        if (roleFilter === "supplier") return p.supplier;
        if (roleFilter === "none") return !p.client && !p.supplier;
        if (roleFilter === "carrier") return p.carrier;
        return true;
      }),
    [partners, roleFilter]
  );

  /**
   * Filtrul de rol restrânge, apoi vederea caută, sortează și paginează ce a rămas. Ordinea
   * contează: căutarea peste tot, urmată de filtru, ar arăta un număr de potriviri din care o
   * parte nici nu se vede.
   */
  // Rolul restrânge, apoi starea: „furnizori activi" e întrebarea obișnuită, iar cimitirul de
  // parteneri scoși din uz nu trebuie să stea în calea ei.
  const { rows: activePartners, control: activeFilter } = useActiveFilter(filteredByRole);
  const view = useTableView(activePartners, {
    searchText: (p) =>
      [p.name, p.cui, p.authorizationNumber, p.address].filter(Boolean).join(" "),
    comparators: {
      name: (a, b) => a.name.localeCompare(b.name, "ro"),
      cui: (a, b) => (a.cui ?? "").localeCompare(b.cui ?? "", "ro"),
      // Autorizația fără dată stă la coadă, în ambele sensuri: „nu se știe" nu e nici devreme,
      // nici târziu. `|| null` păstrează înțelesul de dinainte, în care și șirul gol e o lipsă.
      authorizationExpiry: missingLast(
        (p) => p.authorizationValidUntil || null,
        (x, y) => x.localeCompare(y)
      ),
    },
    initialSort: { key: "name", direction: "asc" },
  });
  const visiblePartners = view.visible;

  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openCreate() {
    setEditing(null);
    setName("");
    setCui("");
    setAuthorizationNumber("");
    setAuthorizationExpiry("");
    setAuthorizationIssueDate("");
    setVisaDecisionNumber("");
    setVisaDecisionDate("");
    setVisaValidUntil("");
    setType("COLLECTOR");
    setIsClient(false);
    setIsSupplier(true);
    setIsCarrier(false);
    setPackagingOrigin("");
    setDrivers([]);
    setAddress("");
    setWorkPoints([]);
    setTradeRegisterNumber("");
    setHeavyVehicles(false);
    setTransportLicenseNumber("");
    setTransportLicenseExpiry("");
    setNameError(false);
    setRoleError(false);
    setTypeError(false);
    setAuthError(false);
    setSwitchedFrom(null);
    setStep(0);
    setAuthDetailsOpen(false);
    setHasOldExpiry(false);
    setDialogOpen(true);
  }

  /**
   * `?nou=1` — formularul gol, cerut din paletă (Ctrl+K → „Adaugă partener").
   *
   * <p>Parametrul se consumă la deschidere, ca `?miscare=` pe Mișcări: lăsat în adresă, un refresh
   * ar redeschide dialogul peste ce lucrezi. Se consumă și pe un rol care nu poate scrie.
   *
   * <p>`openCreate` golește optsprezece rubrici, deci nu se rescrie aici; se ține prin `ref`, ca
   * ascultătorii din `Dialog` — funcția e alta la fiecare randare, iar în dependențele efectului
   * l-ar reporni la fiecare tastă.
   */
  const openCreateRef = useRef(openCreate);
  openCreateRef.current = openCreate;

  /**
   * `?partener=<id>` — deschide fişa partenerului cerut, ca `?miscare=` pe Mişcări.
   *
   * <p>Vine din badge-ul „Autorizaţie expirată" de pe o predare: badge-ul spunea că lipseşte o
   * condiţie de legalitate a predării (decizia 36) şi nu ducea nicăieri — al doilea fund de sac
   * din aplicaţie, după cel roşu reparat pe 07.09. Numărul şi data autorizaţiei se editează în
   * fişa partenerului, deci acolo duce.
   *
   * <p>Parametrul se **consumă** la deschidere: lăsat în adresă, un refresh ar redeschide dialogul
   * peste ce lucrezi. Dacă partenerul nu mai e printre rândurile aduse — dezactivat, sau link
   * vechi — se **spune**, nu se deschide un formular gol.
   */
  const [focusPartner, setFocusPartner] = useUrlState("partener");
  const openEditRef = useRef(openEdit);
  openEditRef.current = openEdit;
  useEffect(() => {
    if (!focusPartner) return;
    const found = (partners ?? []).find((p) => p.id === focusPartner);
    if (found) {
      // Linkul vine din „Autorizație expirată”, deci fișa se deschide direct pe autorizație.
      openEditRef.current(found, AUTH_STEP); // deschide și dialogul
      setFocusPartner("");
      return;
    }
    if (!isLoading && partners) {
      notify(t.partnerNotFound, "error");
      setFocusPartner("");
    }
  }, [focusPartner, partners, isLoading, setFocusPartner, notify]);
  const [newParam, setNewParam] = useUrlState("nou");
  useEffect(() => {
    if (!newParam) return;
    setNewParam("");
    if (canManage) openCreateRef.current();
  }, [newParam, setNewParam, canManage]);

  /**
   * `addRow` vine din tabel („+ Punct de lucru”, „+ Șofer”): fișa se deschide pe pasul 4 cu un rând
   * gol și cursorul în el, ca omul să nu mai caute butonul de adăugare.
   */
  function openEdit(p: Partner, startStep = 0, addRow?: "workPoint" | "driver") {
    setSwitchedFrom(null);
    setEditing(p);
    setName(p.name);
    setCui(p.cui ?? "");
    setAuthorizationNumber(p.authorizationNumber ?? "");
    setAuthorizationExpiry(p.authorizationExpiry ?? "");
    setAuthorizationIssueDate(p.authorizationIssueDate ?? "");
    setVisaDecisionNumber(p.visaDecisionNumber ?? "");
    setVisaDecisionDate(p.visaDecisionDate ?? "");
    setVisaValidUntil(p.visaValidUntil ?? "");
    setType(p.type ?? "");
    setIsClient(p.client);
    setIsSupplier(p.supplier);
    setIsCarrier(p.carrier);
    setPackagingOrigin(p.packagingOrigin ?? "");
    const loadedDrivers: DriverInput[] = (p.drivers ?? []).map((d) => ({
      id: d.id,
      name: d.name,
      identification: d.identification ?? "",
      cnp: d.cnp ?? "",
      vehicleRegistration: d.vehicleRegistration ?? "",
    }));
    setDrivers(addRow === "driver" ? [...loadedDrivers, emptyDriver()] : loadedDrivers);
    setAddress(p.address ?? "");
    const loadedWorkPoints: PartnerWorkPointInput[] = (p.workPoints ?? []).map((wp) => ({
      id: wp.id,
      name: wp.name ?? "",
      address: wp.address,
    }));
    setWorkPoints(addRow === "workPoint" ? [...loadedWorkPoints, { name: "", address: "" }] : loadedWorkPoints);
    setTradeRegisterNumber(p.tradeRegisterNumber ?? "");
    setHeavyVehicles(p.heavyVehicles);
    setTransportLicenseNumber(p.transportLicenseNumber ?? "");
    setTransportLicenseExpiry(p.transportLicenseExpiry ?? "");
    setNameError(false);
    setRoleError(false);
    setTypeError(false);
    setAuthError(false);
    setAuthDetailsOpen(
      Boolean(p.authorizationIssueDate || p.visaDecisionNumber || p.visaDecisionDate || p.authorizationExpiry)
    );
    setHasOldExpiry(Boolean(p.authorizationExpiry));
    setStep(startStep);
    setDialogOpen(true);
    if (addRow) {
      // După ce dialogul și-a pus singur focusul pe prima rubrică.
      const id = addRow === "driver"
        ? `p-driver-name-${loadedDrivers.length}`
        : `p-wp-name-${loadedWorkPoints.length}`;
      setTimeout(() => document.getElementById(id)?.focus(), 80);
    }
  }

  /**
   * Regulile unui pas, în ordinea de dinainte (denumire · rol · tip · autorizație). Pașii doar le
   * împart: la salvare trec toate, iar prima căzută duce omul pe pasul ei.
   */
  function checkStep(index: number): boolean {
    if (index === 0 && !name.trim()) {
      setNameError(true);
      return false;
    }
    if (index === 1) {
      // Mirrors the backend rule: a row the screen colours by role cannot have none.
      if (!isClient && !isSupplier) {
        setRoleError(true);
        return false;
      }
      // Mirrors the backend rule: either they do something with the waste, or they haul it.
      if (!type && !isCarrier) {
        setTypeError(true);
        return false;
      }
    }
    // Aceeași regulă ca serverul: cine preia deșeul are autorizație de mediu.
    if (index === 2 && needsAuthorization && !authorizationNumber.trim()) {
      setAuthError(true);
      return false;
    }
    return true;
  }

  /** Mută pe pas și pune focusul pe prima rubrică greșită, după ce pasul s-a arătat. */
  function goToStep(index: number) {
    setStep(index);
    requestAnimationFrame(() => {
      const form = document.getElementById("partner-form");
      const target =
        form?.querySelector<HTMLElement>('[data-invalid="true"]:not([hidden] *)') ??
        (index === 0 ? document.getElementById("p-cui") : null);
      target?.focus({ preventScroll: true });
      document.getElementById("p-step-title")?.scrollIntoView({ block: "nearest" });
    });
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    // La adăugare, Enter și butonul principal înseamnă „Continuă” până la ultimul pas.
    if (!editing && step < LAST_STEP) {
      if (checkStep(step)) goToStep(step + 1);
      return;
    }
    for (let index = 0; index <= LAST_STEP; index++) {
      if (!checkStep(index)) {
        goToStep(index);
        return;
      }
    }
    const input: PartnerInput = {
      name: name.trim(),
      cui: cui.trim() || null,
      authorizationNumber: authorizationNumber.trim() || null,
      authorizationExpiry: authorizationExpiry || null,
      authorizationIssueDate: authorizationIssueDate || null,
      visaDecisionNumber: visaDecisionNumber.trim() || null,
      visaDecisionDate: visaDecisionDate || null,
      visaValidUntil: visaValidUntil || null,
      type: type || null,
      client: isClient,
      supplier: isSupplier,
      carrier: isCarrier,
      packagingOrigin: packagingOrigin || null,
      address: address.trim() || null,
      // Rândurile fără adresă se aruncă: un punct de lucru fără adresă nu e nimic pe Anexa 3.
      workPoints: workPoints
        .filter((wp) => wp.address.trim() !== "")
        .map((wp) => ({ id: wp.id, name: wp.name?.trim() || null, address: wp.address.trim() })),
      tradeRegisterNumber: tradeRegisterNumber.trim() || null,
      heavyVehicles: isCarrier && heavyVehicles,
      transportLicenseNumber: heavyVehicles ? transportLicenseNumber.trim() || null : null,
      transportLicenseExpiry: heavyVehicles ? transportLicenseExpiry || null : null,
      // Un rând fără nume nu e un delegat. Restul rubricilor pot lipsi: pe formular se scriu de
      // mână oricum, iar aici sunt doar ce se precompletează.
      drivers: drivers
        .filter((d) => d.name.trim() !== "")
        .map((d) => ({
          id: d.id,
          name: d.name.trim(),
          identification: d.identification?.trim() || null,
          cnp: d.cnp?.trim() || null,
          vehicleRegistration: d.vehicleRegistration?.trim() || null,
        })),
    };
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, input });
        notify(t.updated, "success");
      } else {
        await createMut.mutateAsync(input);
        notify(t.created, "success");
      }
      setDialogOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  /**
   * Sugestia de duplicat ducea la fişa existentă **pe tăcute**.
   *
   * <p>Apăsai pe firma sugerată şi acelaşi dialog devenea „Editează partener": tot ce completasei
   * dispărea, iar singurul semn era titlul, pe care nu se uită nimeni când tocmai a apăsat pe altă
   * parte a ecranului. Sugestia există ca să nu se creeze un partener de două ori, deci comutarea e
   * fapta bună — ce lipsea era să **se vadă** că s-a întâmplat, şi drumul înapoi.
   *
   * <p>Două tratamente, după cât ai apucat să scrii, ca la garda de pe formularul de mişcare
   * (07.09): pe un formular în care nu e decât numele, se comută pe loc şi banda de sus o spune; pe
   * unul în care s-au completat şi alte rubrici, se **întreabă întâi** — acolo comutarea chiar
   * aruncă muncă.
   */
  function openSuggested(p: Partner) {
    const typed = name;
    const go = () => {
      openEdit(p);
      setSwitchedFrom(typed);
    };
    if (!formHasMoreThanName) return go();
    confirm({
      title: t.suggestionSwitchTitle,
      message: (
        <>
          <strong className="text-content">{p.name}</strong>
          {p.cui ? ` — CUI ${p.cui}` : ""}. {t.suggestionSwitchConfirm}
        </>
      ),
      confirmLabel: t.suggestionSwitchGo,
      onConfirm: go,
    });
  }

  /** Înapoi la adăugare, cu numele tastat pus la loc — restul rubricilor s-au pierdut oricum. */
  function backToCreate() {
    const typed = switchedFrom ?? "";
    openCreate();
    setName(typed);
  }

  function handleDeactivate(p: Partner) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{p.name}</strong>
          {p.cui ? ` — CUI ${p.cui}` : ""}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () => deactivate(p),
    });
  }

  function deactivate(p: Partner) {
    deactivateMut.mutate(p.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  function reactivate(p: Partner) {
    reactivateMut.mutate(p.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  /**
   * Cui i se cere provenienţa ambalajelor. Rubrica alimentează Anexa 3 Ambalaje, pe care o depun
   * doar operatorii care preiau deşeuri de ambalaje de la terţi (Ordinul 794/2012, art. 4), deci
   * un cont de generator pur n-are ce răspunde. Partenerul care **are** deja un răspuns îl arată
   * oricum: o valoare scrisă cândva trebuie să rămână vizibilă şi editabilă, cum rămâne codul R/D
   * al unei mişcări vechi. Cât timp firma nu s-a încărcat nu se ghiceşte — rubrica apare când se
   * ştie că se aplică.
   */
  /** Ce scrie sub „Ce face pentru tine” în cuprins, ca la pașii trecuți din cererea de cont. */
  const filledWorkPoints = workPoints.filter((wp) => wp.address.trim() !== "").length;
  const filledDrivers = isCarrier ? drivers.filter((d) => d.name.trim() !== "").length : 0;
  const stepFourSummary = [
    filledWorkPoints > 0 && countLabel(filledWorkPoints, t.workPointOne, t.workPointMany),
    filledDrivers > 0 && countLabel(filledDrivers, t.driverOne, t.driverMany),
  ]
    .filter(Boolean)
    .join(" · ");

  const stepTwoSummary = [
    type ? typeLabels[type] : t.typeNoneShort,
    isClient && roleLabels.client,
    isSupplier && roleLabels.supplier,
    isCarrier && type !== "" && t.summaryCarrier,
  ]
    .filter(Boolean)
    .join(" · ");

  /**
   * „Generator” ca tip de partener: firma de la care **primești** deșeu. Un cont de generator pur
   * doar predă, deci cardul l-ar încurca la primul partener (proprietarul, 17.09.2026). Rămâne la
   * colectori, unde e sursa intrărilor, și pe un partener care îl are deja — o valoare salvată nu
   * dispare din formular. Cât timp firma nu s-a încărcat nu se arată, ca proveniența de mai jos.
   */
  const offersGeneratorType =
    (company != null && company.type !== "GENERATOR") || type === "GENERATOR";

  const asksPackagingOrigin =
    (company != null && company.type !== "GENERATOR") || packagingOrigin !== "";

  // `n` deschide formularul, unde contul are voie. Scurtătura tace pe un cont care
  // n-ar putea salva oricum: o comandă care nu face nimic e mai rea decât una lipsă.
  useHotkey("n", openCreate, { enabled: Boolean(canManage) && !personsTab });

  return (
    <div>
      <PageHeader
        title={t.title}
        // Pe tabul „Persoane fizice” antetul spune ce e lista de dedesubt, nu ce sunt firmele.
        description={personsTab ? strings.naturalPersons.subtitle : t.subtitle}
        actions={
          canManage && !personsTab && (
            <Button onClick={openCreate}>
              <Plus className="mr-2 h-4 w-4" />
              {t.add}
            </Button>
          )
        }
      />

      {hasDepot && (
        <div role="tablist" className="mt-6 flex gap-1 border-b border-line">
          {[
            { id: "", label: strings.naturalPersons.tabFirms },
            { id: "persoane-fizice", label: strings.naturalPersons.tab },
          ].map((item) => {
            const selected = (item.id === "persoane-fizice") === personsTab;
            return (
              <button
                key={item.id || "firme"}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setTab(item.id)}
                className={
                  "-mb-px border-b-2 px-3 py-2 text-sm font-medium " +
                  (selected
                    ? "border-brand-600 text-content-strong"
                    : "border-transparent text-content-muted hover:text-content")
                }
              >
                {item.label}
              </button>
            );
          })}
        </div>
      )}

      {personsTab ? (
        <NaturalPersonsSection canManage={Boolean(canManage)} />
      ) : (
      <>
      <div className="mt-6 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <div>
          <Label htmlFor="filter-role">{t.filterRole}</Label>
          <Select
            id="filter-role"
            value={roleFilter}
            onChange={(ev) => setRoleFilter(ev.target.value as RoleFilter)}
            className="w-full sm:w-56"
          >
            <option value="">{t.filterRoleAll}</option>
            <option value="client">{roleLabels.client}</option>
            <option value="supplier">{roleLabels.supplier}</option>
            <option value="none">{roleLabels.none}</option>
            {/* Altă axă, același select: e o singură valoare, iar un al doilea dropdown ar
                costa mai mult decât explică. */}
            <optgroup label={t.carrierColumn}>
              <option value="carrier">{t.filterCarrier}</option>
            </optgroup>
          </Select>
        </div>
      </div>

      <section className="mt-4">
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isError && (
          <>
            <TableToolbar view={view} placeholder={t.searchPlaceholder}>
              {activeFilter}
            </TableToolbar>
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                    {t.name}
                  </SortableTH>
                  <SortableTH sortKey="cui" sort={view.sort} onSort={view.toggleSort}>
                    {t.cui}
                  </SortableTH>
                  <TH>{t.role}</TH>
                  <TH>{t.type}</TH>
                  <TH>{t.carrierColumn}</TH>
                  <TH>{t.authorizationNumber}</TH>
                  <SortableTH
                    sortKey="authorizationExpiry"
                    sort={view.sort}
                    onSort={view.toggleSort}
                  >
                    {t.authorizationValidUntil}
                  </SortableTH>
                  <TH>{strings.common.status}</TH>
                  {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
                </TR>
              </THead>
              <TBody>
                {(isLoading || visiblePartners.length === 0) && (
                  <TableFallbackRow
                    columns={canManage ? 9 : 8}
                    loading={isLoading}
                    icon={Users}
                    title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                    description={
                      view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                    }
                    action={
                      canManage && (
                        <Button onClick={openCreate}>
                          <Plus className="mr-2 h-4 w-4" />
                          {t.add}
                        </Button>
                      )
                    }
                  />
                )}
                {visiblePartners.map((p) => (
                  <TR key={p.id}>
                    <TD>
                      <div className="font-medium text-content">{p.name}</div>
                      <PartnerPlaces
                        partner={p}
                        canManage={Boolean(canManage)}
                        onOpen={(addRow) => openEdit(p, PLACES_STEP, addRow)}
                      />
                    </TD>
                    <TD>{p.cui || "—"}</TD>
                    <TD>
                      <PartnerRoleBadge partner={p} />
                    </TD>
                    <TD>
                      {p.type ? (
                        typeLabels[p.type]
                      ) : (
                        <span className="text-content-muted">{t.typeNoneShort}</span>
                      )}
                    </TD>
                    <TD>
                      {p.carrier ? (
                        <Badge variant="muted">{t.carrierYes}</Badge>
                      ) : (
                        <span className="text-content-subtle">{t.carrierNo}</span>
                      )}
                    </TD>
                    <TD>{p.authorizationNumber || "—"}</TD>
                    <TD>
                      <ExpiryBadge partner={p} />
                    </TD>
                    <TD>
                      {p.active ? (
                        <Badge variant="success">{t.active}</Badge>
                      ) : (
                        <Badge variant="muted">{t.inactive}</Badge>
                      )}
                    </TD>
                    {canManage && (
                      <TD sticky="right" className="text-right">
                        {/* „Dezactivează” a intrat în „⋯” lângă „Puncte de lucru și șoferi” (17.09.2026):
                            rândul de sub nume cerea lățimea, iar tabelul nu mai încăpea la 1440px. */}
                        <div className="flex items-center justify-end gap-1">
                          <Button variant="ghost" size="sm" onClick={() => openEdit(p)}>
                            <Pencil className="mr-1 h-3.5 w-3.5" />
                            {strings.common.edit}
                          </Button>
                          <Menu>
                            <MenuItem icon={MapPin} onClick={() => openEdit(p, PLACES_STEP)}>
                              {t.step4Name}
                            </MenuItem>
                            {p.active ? (
                              <MenuItem icon={Ban} tone="danger" onClick={() => handleDeactivate(p)}>
                                {t.deactivate}
                              </MenuItem>
                            ) : (
                              <MenuItem icon={RotateCcw} onClick={() => reactivate(p)}>
                                {strings.common.reactivate}
                              </MenuItem>
                            )}
                          </Menu>
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
      </section>
      </>
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.editTitle : t.addTitle}
        // `2xl`: în stânga stă cuprinsul pașilor, ca la cererea de cont; rândul unui șofer are
        // nevoie și el de lățimea de dinainte.
        size="2xl"
        footer={
          <>
            {step > 0 && (
              <Button
                variant="ghost"
                className="sm:mr-auto"
                onClick={() => goToStep(step - 1)}
                disabled={isSubmitting}
              >
                ← {t.stepBack}
              </Button>
            )}
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            {/* La editare se salvează de pe orice pas: cine a venit să schimbe viza nu trece prin
                denumire. La adăugare, „Continuă” își verifică pasul, iar Salvează stă pe ultimul. */}
            {editing && step < LAST_STEP && (
              <Button variant="outline" onClick={() => goToStep(step + 1)} disabled={isSubmitting}>
                {t.stepContinue} →
              </Button>
            )}
            <Button type="submit" form="partner-form" disabled={isSubmitting}>
              {!editing && step < LAST_STEP
                ? `${t.stepContinue} →`
                : isSubmitting
                  ? strings.common.saving
                  : strings.common.save}
            </Button>
          </>
        }
      >
        {/* Trei pași, ca cererea de cont: cine e · ce face · autorizația. Rubricile sunt aceleași ca
            înainte și se salvează la fel; pașii doar le împart. Toți pașii rămân montați (ascunși
            cu `hidden`), deci o rubrică nu-și pierde valoarea la trecerea dintre pași. */}
        <div className="md:grid md:grid-cols-[210px_minmax(0,1fr)] md:gap-7">
          <FormStepRail
            className="hidden md:block md:sticky md:top-0 md:self-start"
            label={t.stepsLabel}
            current={step}
            onSelect={goToStep}
            steps={[
              { name: t.step1Name, summary: [name.trim(), cui.trim()].filter(Boolean).join(" · "), invalid: nameError },
              { name: t.step2Name, summary: stepTwoSummary, invalid: roleError || typeError },
              {
                name: t.step3Name,
                summary: authorizationNumber.trim()
                  ? t.summaryAuthorization.replace("{n}", authorizationNumber.trim())
                  : undefined,
                invalid: authError,
              },
              { name: t.step4Name, summary: stepFourSummary || undefined },
            ]}
          />

          <form id="partner-form" onSubmit={handleSubmit} className="min-w-0 space-y-5">
            <Stepper className="md:hidden" steps={[t.stepShort1, t.stepShort2, t.stepShort3, t.stepShort4]} current={step} />

            {/* Ai venit aici dintr-o sugestie de duplicat: acelaşi dialog, altă faptă. Fără rândul
                ăsta, singurul semn era titlul — iar cine tocmai a apăsat pe o sugestie se uită la
                locul unde a apăsat, nu la antet. */}
            {switchedFrom !== null && editing && (
              <div className="flex flex-col gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 sm:flex-row sm:items-center">
                <p className="min-w-0 flex-1 text-xs text-amber-900">
                  <strong className="font-medium">{t.suggestionSwitchedTitle}</strong>{" "}
                  {t.suggestionSwitchedHint.replace("{name}", switchedFrom)}
                </p>
                <button
                  type="button"
                  onClick={backToCreate}
                  className="shrink-0 whitespace-nowrap text-xs font-medium text-amber-800 underline underline-offset-2"
                >
                  {t.suggestionSwitchedBack}
                </button>
              </div>
            )}

            <div>
              <div className="eyebrow text-content-muted">{t.stepOf.replace("{n}", String(step + 1))}</div>
              <h3 id="p-step-title" className="mt-1 text-xl font-semibold leading-7 text-content">
                {[t.step1Title, t.step2Title, t.step3Title, t.step4Title][step]}
              </h3>
              <p className="mt-0.5 text-sm text-content-muted">
                {step === 0
                  ? t.step1Subtitle
                  : step === 1
                    ? t.step2Subtitle
                    : step === 3
                      ? t.step4Subtitle
                      : needsAuthorization
                        ? t.step3SubtitleRequired
                        : t.step3SubtitleOptional}
                {step === 2 && (
                  <>
                    {" "}
                    <Tooltip content={t.sectionAuthorizationHint} className="max-w-sm">
                      <span className="font-medium text-content underline decoration-dotted underline-offset-2">
                        {t.whyLink}
                      </span>
                    </Tooltip>
                  </>
                )}
              </p>
            </div>

            {/* ------------------------------------------------ 1. DETALII DESPRE PARTENER */}
            <div hidden={step !== 0} className="space-y-4">
              {/* CUI-ul întâi: ANAF completează denumirea, adresa și Registrul Comerțului, iar
                  sugestia de duplicat apare și după completare, fiindcă citește denumirea. */}
              <CuiField
                id="p-cui"
                autoFocus={!editing}
                label={t.cui}
                value={cui}
                onChange={setCui}
                placeholder={t.cuiPlaceholder}
                targets={[
                  {
                    label: t.anafFieldName,
                    current: name,
                    pick: (f) => f.name,
                    set: (v) => {
                      setName(v);
                      setNameError(false);
                    },
                  },
                  { label: t.anafFieldAddress, current: address, pick: (f) => f.address, set: setAddress },
                  {
                    label: t.anafFieldRegistry,
                    current: tradeRegisterNumber,
                    pick: (f) => f.tradeRegisterNumber,
                    set: setTradeRegisterNumber,
                  },
                ]}
              />
              <div>
                <Label htmlFor="p-name">{t.name}</Label>
                <Input
                  id="p-name"
                  value={name}
                  onChange={(e) => {
                    setName(e.target.value);
                    if (nameError) setNameError(false);
                  }}
                />
                {nameError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
                {nameSuggestions.length > 0 && (
                  <div className="mt-1 rounded-md border border-amber-200 bg-amber-50 px-2 py-1.5">
                    <p className="text-xs font-medium text-amber-800">{t.nameSuggestions}</p>
                    <ul className="mt-0.5 space-y-0.5">
                      {nameSuggestions.map((p) => (
                        <li key={p.id}>
                          <button
                            type="button"
                            className="text-xs text-amber-900 underline underline-offset-2"
                            onClick={() => openSuggested(p)}
                          >
                            {p.name}
                            {p.cui ? ` — ${p.cui}` : ""}
                            {!p.active ? ` (${t.inactive})` : ""}
                          </button>
                        </li>
                      ))}
                    </ul>
                    <p className="mt-0.5 text-xs text-amber-700">{t.nameSuggestionsHint}</p>
                  </div>
                )}
              </div>
              <div>
                <Label htmlFor="p-address">{t.address}</Label>
                <Input id="p-address" value={address} onChange={(e) => setAddress(e.target.value)} />
                <p className="mt-1 text-xs text-content-muted">{t.anexa3Hint}</p>
              </div>
              <div>
                <Label htmlFor="p-reg">{t.tradeRegisterNumber}</Label>
                <Input
                  id="p-reg"
                  value={tradeRegisterNumber}
                  onChange={(e) => setTradeRegisterNumber(e.target.value)}
                  placeholder={t.tradeRegisterNumberPlaceholder}
                />
              </div>
            </div>

            {/* ------------------------------------------------ 2. CE FACE PENTRU TINE */}
            <div hidden={step !== 1} className="space-y-6">
              <div>
                <span id="p-type-label" className="mb-2 block text-sm font-semibold text-content-strong">
                  {t.typeQuestion}
                </span>
                {/* „Doar le transportă” e tipul gol de dinainte („— doar transportator —”). Singur
                    nu se poate salva fără bifa de transportator, deci alegerea o pune și pe ea. */}
                <ChoiceCards
                  name="p-type"
                  aria-labelledby="p-type-label"
                  columns={2}
                  value={type === "" ? NONE_TYPE : type}
                  onChange={(value) => {
                    if (value === NONE_TYPE) {
                      setType("");
                      setIsCarrier(true);
                    } else {
                      setType(value);
                    }
                    if (typeError) setTypeError(false);
                  }}
                  options={[
                    { value: "COLLECTOR", label: typeLabels.COLLECTOR, description: t.typeCollectorHint, icon: <Warehouse className="h-5 w-5" /> },
                    { value: "RECOVERER", label: typeLabels.RECOVERER, description: t.typeRecovererHint, icon: <Recycle className="h-5 w-5" /> },
                    ...(offersGeneratorType
                      ? [{ value: "GENERATOR" as const, label: typeLabels.GENERATOR, description: t.typeGeneratorHint, icon: <Factory className="h-5 w-5" /> }]
                      : []),
                    { value: NONE_TYPE, label: t.typeNoneCard, description: t.typeNoneCardHint, icon: <Truck className="h-5 w-5" /> },
                  ]}
                />
                {/* Cârligul vechi al probelor: rubrica tipului se recunoaște după id. */}
                <span id="p-type" hidden />
                {typeError && (
                  <p data-field-error className="mt-1 text-xs text-state-bad-text">
                    {t.typeRequired}
                  </p>
                )}
              </div>

              {/* Transportatorul e o bifă, nu un tip: aceeași firmă e des și colector, și
                  transportator. Licența și șoferii apar numai bifat. */}
              <div>
                <span id="p-carrier-label" className="mb-2 block text-sm font-semibold text-content-strong">
                  {t.carrierQuestion}
                </span>
                {/* Două răspunsuri spuse cu vorbele omului, în locul bifei „Transportator”. Salvează
                    aceeași bifă (`carrier`); la „Doar le transportă” nu există alt răspuns. */}
                <ChoiceCards
                  name="p-carrier"
                  aria-labelledby="p-carrier-label"
                  columns={2}
                  value={isCarrier ? "yes" : "no"}
                  onChange={(value) => {
                    setIsCarrier(value === "yes");
                    if (typeError) setTypeError(false);
                  }}
                  options={[
                    {
                      value: "yes",
                      label: t.carrierYesCard,
                      description: t.carrierYesCardHint,
                      icon: <Truck className="h-5 w-5" />,
                    },
                    {
                      value: "no",
                      label: t.carrierNoCard,
                      description: type === "" ? t.carrierNoCardDisabled : t.carrierNoCardHint,
                      icon: <Building2 className="h-5 w-5" />,
                      disabled: type === "",
                    },
                  ]}
                />
                {!type && !isCarrier && <p className="mt-1 text-xs text-content-muted">{t.typeNoneHint}</p>}

                {isCarrier && (
                  <div className="mt-3 space-y-4 border-l-2 border-line pl-4">
                    <div>
                      <span className="block text-sm font-semibold text-content-strong">{t.carrierDetails}</span>
                      <p className="mt-0.5 text-xs text-content-muted">{t.carrierDetailsHint}</p>
                    </div>
                    <Switch
                      id="p-heavy-vehicles"
                      checked={heavyVehicles}
                      onChange={setHeavyVehicles}
                      label={t.heavyVehicles}
                      description={t.heavyVehiclesHint}
                    />
                    {heavyVehicles && (
                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <div>
                          <Label htmlFor="p-licence">{t.transportLicenseNumber}</Label>
                          <Input
                            id="p-licence"
                            value={transportLicenseNumber}
                            onChange={(e) => setTransportLicenseNumber(e.target.value)}
                          />
                        </div>
                        <div>
                          <Label htmlFor="p-licence-expiry">{t.transportLicenseExpiry}</Label>
                          <DateInput
                            id="p-licence-expiry"
                            value={transportLicenseExpiry}
                            onChange={(e) => setTransportLicenseExpiry(e.target.value)}
                          />
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div>
                <span className="mb-2 block text-sm font-semibold text-content-strong">{t.roleQuestion}</span>
                <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                  <Switch
                    id="p-role-client"
                    checked={isClient}
                    onChange={(checked) => {
                      setIsClient(checked);
                      if (roleError) setRoleError(false);
                    }}
                    label={roleLabels.client}
                    description={roleLabels.clientHint}
                  />
                  <Switch
                    id="p-role-supplier"
                    checked={isSupplier}
                    onChange={(checked) => {
                      setIsSupplier(checked);
                      if (roleError) setRoleError(false);
                    }}
                    label={roleLabels.supplier}
                    description={roleLabels.supplierHint}
                  />
                </div>
                {roleError && (
                  <p data-field-error className="mt-1 text-xs text-state-bad-text">
                    {t.roleRequired}
                  </p>
                )}
              </div>

              {/* Provenienţa stă aici, nu pe fiecare mişcare: nota 2 a Anexei 3 Ambalaje descrie
                  **sursa**, nu transportul (decizia 43), şi se întreabă numai de la conturile care
                  pot prelua de la terţi — un generator pur n-o depune niciodată. */}
              {asksPackagingOrigin && (
                <div id="p-pkg-origin">
                  <span id="p-pkg-origin-label" className="mb-2 block text-sm font-semibold text-content-strong">
                    {strings.packagingOrigin.label}
                  </span>
                  <PillGroup
                    name="p-pkg-origin"
                    aria-labelledby="p-pkg-origin-label"
                    selected={[packagingOrigin]}
                    onToggle={(value) => setPackagingOrigin(value)}
                    options={[
                      { value: "", label: strings.packagingOrigin.none },
                      { value: "GENERATOR_PJ", label: strings.packagingOrigin.GENERATOR_PJ },
                      { value: "COLECTOR", label: strings.packagingOrigin.COLECTOR },
                      { value: "COMERCIANT", label: strings.packagingOrigin.COMERCIANT },
                    ]}
                  />
                  <p className="mt-1 text-xs text-content-muted">{strings.packagingOrigin.hintPartner}</p>
                </div>
              )}

            </div>

            {/* ------------------------------------------------ 3. AUTORIZAȚIA DE MEDIU */}
            <div hidden={step !== 2} className="space-y-5">
              {/* Sus, cele două întrebări care contează: numărul (obligatoriu la cine preia deșeul)
                  și până când ține viza, de care atârnă avertismentul. Restul rubricilor, aceleași
                  ca înainte, stau sub „Detalii de pe hârtii” (17.09.2026). */}
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <Label htmlFor="p-auth-number" required={needsAuthorization}>
                    {t.authorizationNumber}
                  </Label>
                  <Input
                    id="p-auth-number"
                    value={authorizationNumber}
                    onChange={(e) => {
                      setAuthorizationNumber(e.target.value);
                      if (authError) setAuthError(false);
                    }}
                    placeholder={t.authorizationNumberPlaceholder}
                    {...invalidProps("p-auth-number-err", authError ? t.authorizationRequired : undefined)}
                  />
                  <FieldError id="p-auth-number-err" message={authError ? t.authorizationRequired : undefined} />
                </div>
                <div>
                  <Label htmlFor="p-visa-until">{t.visaUntilAsk}</Label>
                  <DateInput
                    id="p-visa-until"
                    value={visaValidUntil}
                    onChange={(e) => setVisaValidUntil(e.target.value)}
                  />
                  {authorizationIssueDate && !visaValidUntil && (() => {
                    const proposal = nextAnniversary(
                      authorizationIssueDate,
                      visaDecisionDate || new Date().toISOString().slice(0, 10)
                    );
                    return (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="mt-2"
                        onClick={() => setVisaValidUntil(proposal)}
                      >
                        {t.visaValidUntilSuggest.replace("{date}", formatDate(proposal))}
                      </Button>
                    );
                  })()}
                </div>
              </div>
              <p className="-mt-2 text-xs text-content-muted">{t.visaUntilHint}</p>

              <div className="rounded-lg border border-line">
                <button
                  type="button"
                  aria-expanded={authDetailsOpen}
                  aria-controls="p-auth-details"
                  onClick={() => setAuthDetailsOpen((open) => !open)}
                  className="flex w-full items-center gap-2 px-3.5 py-2.5 text-left text-sm font-semibold text-content-strong hover:bg-surface-muted"
                >
                  <ChevronRight
                    aria-hidden
                    className={"h-4 w-4 shrink-0 transition-transform " + (authDetailsOpen ? "rotate-90" : "")}
                  />
                  {t.authDetails}
                  <span className="font-normal text-content-muted">· {t.authDetailsOptional}</span>
                </button>
                <div id="p-auth-details" hidden={!authDetailsOpen} className="space-y-4 border-t border-line px-3.5 py-3.5">
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <div>
                      <Label htmlFor="p-auth-issue">{t.authorizationIssueDate}</Label>
                      <DateInput
                        id="p-auth-issue"
                        value={authorizationIssueDate}
                        onChange={(e) => setAuthorizationIssueDate(e.target.value)}
                      />
                    </div>
                    <div>
                      <Label htmlFor="p-visa-number">{t.visaDecisionNumber}</Label>
                      <Input
                        id="p-visa-number"
                        value={visaDecisionNumber}
                        onChange={(e) => setVisaDecisionNumber(e.target.value)}
                      />
                    </div>
                    <div>
                      <Label htmlFor="p-visa-date">{t.visaDecisionDate}</Label>
                      <DateInput
                        id="p-visa-date"
                        value={visaDecisionDate}
                        onChange={(e) => setVisaDecisionDate(e.target.value)}
                      />
                    </div>
                  </div>
                  <p className="text-xs text-content-muted">{t.authorizationIssueDateHint}</p>

                  <div>
                    <label className="flex items-start gap-2 text-sm">
                      <input
                        type="checkbox"
                        className="mt-0.5 h-4 w-4 rounded border-line-strong"
                        checked={hasOldExpiry}
                        onChange={(e) => {
                          setHasOldExpiry(e.target.checked);
                          // Debifat de om: autorizația nu are termen, deci data scrisă nu mai e a ei.
                          if (!e.target.checked) setAuthorizationExpiry("");
                        }}
                      />
                      <span>
                        <span className="font-medium text-content-strong">{t.oldExpiryAsk}</span>
                        <span className="block text-xs text-content-muted">{t.oldExpiryHint}</span>
                      </span>
                    </label>
                    {hasOldExpiry && (
                      <div className="mt-3 pl-6">
                        <Label htmlFor="p-auth-expiry">{t.authorizationExpiryOptional}</Label>
                        <DateInput
                          id="p-auth-expiry"
                          className="sm:max-w-[16rem]"
                          value={authorizationExpiry}
                          onChange={(e) => setAuthorizationExpiry(e.target.value)}
                        />
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* ------------------------------------------------ 4. PUNCTE DE LUCRU ȘI ȘOFERI */}
            {/* Pas separat din 17.09.2026: punctele de lucru stăteau la coada pasului 1, iar șoferii
                sub cardul „Vine el și îl ia” — cine voia să adauge unul mai târziu nu-i găsea. Tabelul
                deschide fișa direct aici. */}
            <div hidden={step !== 3} className="space-y-6">
              <div>
                <span className="block text-sm font-medium text-content-strong">{t.workPoints}</span>
                <p className="mt-0.5 text-xs text-content-muted">{t.workPointsHint}</p>
                <div className="mt-2 space-y-2">
                  {workPoints.map((wp, index) => (
                    <div
                      key={wp.id ?? `new-${index}`}
                      className="flex flex-col gap-2 sm:flex-row sm:items-end"
                    >
                      <div className="w-full sm:w-52">
                        <Label htmlFor={`p-wp-name-${index}`}>{t.workPointName}</Label>
                        <Input
                          id={`p-wp-name-${index}`}
                          value={wp.name ?? ""}
                          placeholder={t.workPointNamePlaceholder}
                          onChange={(e) =>
                            setWorkPoints((prev) =>
                              prev.map((x, i) => (i === index ? { ...x, name: e.target.value } : x))
                            )
                          }
                        />
                      </div>
                      <div className="flex-1">
                        <Label htmlFor={`p-wp-address-${index}`}>{t.workPointAddress}</Label>
                        <Input
                          id={`p-wp-address-${index}`}
                          value={wp.address}
                          onChange={(e) =>
                            setWorkPoints((prev) =>
                              prev.map((x, i) => (i === index ? { ...x, address: e.target.value } : x))
                            )
                          }
                        />
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="self-end text-red-600 hover:bg-red-50 sm:mb-1"
                        onClick={() => setWorkPoints((prev) => prev.filter((_, i) => i !== index))}
                      >
                        {t.removeWorkPoint}
                      </Button>
                    </div>
                  ))}
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-2"
                  onClick={() => setWorkPoints((prev) => [...prev, { name: "", address: "" }])}
                >
                  <Plus className="mr-1 h-3.5 w-3.5" />
                  {t.addWorkPoint}
                </Button>
              </div>
              {isCarrier ? (
                <div id="p-drivers">
                  <div>
                    <span className="block text-sm font-medium text-content-strong">{t.drivers}</span>
                    <p className="mt-0.5 text-xs text-content-muted">{t.driversHint}</p>
                    {/* Aceeași notă ca în „Șoferii noștri" din Setări: se scrie o dată, se arată în
                        amândouă locurile unde chiar se tastează actul de identitate. */}
                    <p className="mt-0.5 text-xs text-content-subtle">{strings.common.driversPrivacy}</p>
                    <div className="mt-2 space-y-2">
                      {drivers.map((d, index) => (
                        <div
                          key={d.id ?? `new-${index}`}
                          className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-end"
                        >
                          {/* Numele pe rândul lui: cu CNP-ul (15.09.2026), patru rubrici într-un
                              rând îl strângeau la câțiva pixeli. */}
                          <div className="w-full sm:basis-full">
                            <Label htmlFor={`p-driver-name-${index}`}>{t.driverName}</Label>
                            <Input
                              id={`p-driver-name-${index}`}
                              maxLength={255}
                              value={d.name}
                              placeholder={t.driverNamePlaceholder}
                              onChange={(e) =>
                                setDrivers((prev) =>
                                  prev.map((x, i) => (i === index ? { ...x, name: e.target.value } : x))
                                )
                              }
                            />
                          </div>
                          <div className="w-full sm:w-40">
                            <Label htmlFor={`p-driver-id-${index}`}>{t.driverIdentification}</Label>
                            <Input
                              id={`p-driver-id-${index}`}
                              maxLength={100}
                              value={d.identification ?? ""}
                              placeholder={t.driverIdentificationPlaceholder}
                              onChange={(e) =>
                                setDrivers((prev) =>
                                  prev.map((x, i) =>
                                    i === index ? { ...x, identification: e.target.value } : x
                                  )
                                )
                              }
                            />
                          </div>
                          <div className="w-full sm:w-36">
                            <Label htmlFor={`p-driver-cnp-${index}`}>{strings.common.cnp}</Label>
                            <Input
                              id={`p-driver-cnp-${index}`}
                              inputMode="numeric"
                              maxLength={13}
                              value={d.cnp ?? ""}
                              onChange={(e) =>
                                setDrivers((prev) =>
                                  prev.map((x, i) => (i === index ? { ...x, cnp: e.target.value } : x))
                                )
                              }
                            />
                          </div>
                          <div className="w-full sm:w-36">
                            <Label htmlFor={`p-driver-plate-${index}`}>{t.driverVehicle}</Label>
                            <Input
                              id={`p-driver-plate-${index}`}
                              maxLength={50}
                              value={d.vehicleRegistration ?? ""}
                              placeholder={t.driverVehiclePlaceholder}
                              onChange={(e) =>
                                setDrivers((prev) =>
                                  prev.map((x, i) =>
                                    i === index ? { ...x, vehicleRegistration: e.target.value } : x
                                  )
                                )
                              }
                            />
                          </div>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="self-end text-red-600 hover:bg-red-50 sm:mb-1"
                            onClick={() => setDrivers((prev) => prev.filter((_, i) => i !== index))}
                          >
                            {t.removeDriver}
                          </Button>
                        </div>
                      ))}
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="mt-2"
                      onClick={() =>
                        setDrivers((prev) => [
                          ...prev,
                          emptyDriver(),
                        ])
                      }
                    >
                      <Plus className="mr-1 h-3.5 w-3.5" />
                      {t.addDriver}
                    </Button>
                  </div>
                </div>
              ) : (
                <div id="p-drivers" className="rounded-lg border border-line px-3.5 py-3">
                  <span className="block text-sm font-medium text-content-strong">{t.drivers}</span>
                  <p className="mt-0.5 text-xs text-content-muted">{t.driversNotCarrier}</p>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="mt-2"
                    onClick={() => {
                      setIsCarrier(true);
                      setDrivers((prev) => (prev.length > 0 ? prev : [emptyDriver()]));
                    }}
                  >
                    <Truck className="mr-1 h-3.5 w-3.5" />
                    {t.driversMakeCarrier}
                  </Button>
                </div>
              )}
            </div>
          </form>
        </div>
      </Dialog>

      {confirmDialog}
    </div>
  );
}
