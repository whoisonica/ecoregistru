import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import {
  SCREEN_PATH,
  directionOf,
  registerOf,
  screenOfMovement,
  screensFor,
  type MovementScreen,
} from "@/lib/movementScreens";
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Copy,
  Plus,
  Pencil,
  Trash2,
  Paperclip,
  FileText,
  History,
  Scale,
  Truck,
} from "lucide-react";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { Menu, MenuItem } from "@/components/ui/menu";
import { useCanWrite } from "@/hooks/useBillingAccess";
import { useAuth } from "@/auth/AuthContext";
import { canManage as roleCanManage } from "@/lib/roles";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { downloadArt48Register } from "@/hooks/useEvidences";
import {
  useMovement,
  useMovements,
  useMovementTotals,
  useDeleteMovement,
} from "@/hooks/useMovements";
import type { MovementFilters, PackagingMovementFilter, WasteMovement } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { GENERATION_TABS } from "@/lib/screenTabs";
import { strings } from "@/lib/strings";
import { useHotkey } from "@/hooks/useHotkey";
import { useUrlState } from "@/hooks/useUrlState";
import { formatDate } from "@/lib/utils";
import { Button, LinkButton } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Tooltip } from "@/components/ui/tooltip";
import { Select } from "@/components/ui/select";
import { MonthInput, currentMonth, isMonthValue } from "@/components/ui/month-input";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import {
  RowAction,
  RowActions,
  TablePagination,
  TableToolbar,
} from "@/components/ui/table-toolbar";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useRemoteTableView } from "@/hooks/useTableView";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { fetchDeclaration } from "@/hooks/useDeadlines";
import { declaredText } from "@/lib/deadlines";
import {
  canPrintAnexa3,
  canPrintAviz,
  useAnexa3Download,
  useAvizDownload,
} from "@/hooks/useAnexa3";
import { canPrintAnexa2, useAnexa2Download } from "@/hooks/useAnexa2";
import { TotalsStrip } from "@/components/movements/TotalsStrip";
import { AnnualTotals } from "@/components/movements/AnnualTotals";
import { PackagingReport } from "@/components/packaging/PackagingReport";
import { materialLabels } from "@/components/packaging/packagingFormat";
import { PillGroup } from "@/components/ui/pill-group";
import { PageTabs, type PageTab } from "@/components/ui/page-tabs";
import { AttachmentsDialog } from "@/components/movements/AttachmentsDialog";
import { RecordWeightDialog } from "@/components/movements/RecordWeightDialog";
import { MovementFormDialog } from "@/components/movements/MovementFormDialog";
import { MovementSavedDialog } from "@/components/movements/MovementSavedDialog";

const t = strings.movements;
const e = strings.enums;
/** Textele ambalajelor stau la ele acasă: coloanele de mai jos sunt chiar cele din tabul „Ambalaje". */
const pk = strings.packaging;

/**
 * `/miscari`, adresa de dinainte de cele două ecrane. Linkurile vechi — din rapoarte, de pe Panou,
 * din paletă — duc tot aici, deci se trimit mai departe cu tot cu parametri: pe ecranul mișcării
 * numite în `?miscare=`, altfel pe primul ecran al firmei.
 */
/**
 * `/evidente` — adresa ecranului scos pe 18.09.2026.
 *
 * <p>„Evidențe" agrega exact mișcările registrului `ANEXA_1`, adică exact rândurile ecranului
 * „Generare": două intrări în meniu pentru același registru. Ce avea numai el — totalul anului pe
 * cod — a devenit tabul „Totalul anului" de acolo, iar documentele se iau de acolo și din Dosarul
 * de control.
 *
 * <p>Linkurile vechi se traduc, nu se pierd: „arată-mi ce blochează depunerea"
 * (`?problema=cod-rd`) ajunge pe lista de mișcări a anului, cu filtrul pus; restul, pe tabul
 * totalului. O firmă care nu ține Anexa 1 n-are unde: pleacă pe primul ei ecran.
 */
export function EvidencesRedirect() {
  const { data: company, isLoading } = useCurrentCompany();
  const [year] = useUrlState("an");
  const [workPoint] = useUrlState("punct");
  const [problem] = useUrlState("problema");
  if (isLoading) return null;
  const visible = screensFor(company?.type);
  if (!visible.includes("GENERATED")) {
    return <Navigate replace to={SCREEN_PATH[visible[0]]} />;
  }
  const params = new URLSearchParams();
  if (problem === "cod-rd") {
    params.set("luna", year || String(new Date().getFullYear()));
    params.set("problema", "cod-rd");
  } else {
    params.set("tab", "total");
    if (year) params.set("luna", year);
  }
  if (workPoint) params.set("punct", workPoint);
  return <Navigate replace to={`${SCREEN_PATH.GENERATED}?${params.toString()}`} />;
}

export function MovementsRedirect({ fallback }: { fallback?: MovementScreen } = {}) {
  const location = useLocation();
  const { data: company, isLoading } = useCurrentCompany();
  const [focusId] = useUrlState("miscare");
  const focused = useMovement(focusId || null);
  if (isLoading || (focusId && focused.isLoading)) return null;
  const visible = screensFor(company?.type);
  const target = focused.data
    ? screenOfMovement(focused.data.register, focused.data.operation, company?.type)
    : fallback && visible.includes(fallback)
      ? fallback
      : visible[0];
  return <Navigate replace to={SCREEN_PATH[target] + location.search} />;
}

/**
 * Anii din selectorul tabului anual: cel curent și cinci în urmă. Un an venit prin adresă (linkul
 * unui termen vechi) intră și el în listă, altfel select-ul ar arăta altceva decât filtrează pagina.
 */
function yearOptions(current: number): number[] {
  const now = new Date().getFullYear();
  const list = Array.from({ length: 6 }, (_, i) => now - i);
  if (Number.isFinite(current) && !list.includes(current)) list.push(current);
  return list.sort((a, b) => b - a);
}

export function MovementsPage({ screen }: { screen: MovementScreen }) {
  const canWrite = useCanWrite();
  // „Istoric” duce în jurnalul de audit, care e al administratorului (`CAN_READ` din `AuditLogController`).
  const canSeeHistory = roleCanManage(useAuth().user?.role);
  const register = registerOf(screen);
  const direction = directionOf(screen);

  const { data: workPoints } = useWorkPoints();
  const activeWorkPoints = useMemo(
    () => (workPoints ?? []).filter((w) => w.active),
    [workPoints]
  );

  // --- Filters ---
  // Filtrele stau în bara de adrese: se păstrează la navigare și se pot trimite ca link.
  /**
   * Ecranul pornește **pe luna curentă**, nu pe „tot".
   *
   * <p>Fără lună, cererea aducea toate mișcările firmei, oricâte: la doi ani de folosire cu
   * treizeci de predări pe lună sunt ~700 de rânduri la fiecare deschidere a ecranului, iar
   * `useTableView` paginează abia **după** ce au venit — deci paginarea nu apăra nimic.
   *
   * <p>Luna implicită nu se scrie în adresă (vezi `useUrlState`), deci `/miscari` rămâne un link
   * curat care înseamnă „luna asta", iar `?luna=2026-03` continuă să însemne o lună anume.
   */
  const thisMonth = useMemo(() => currentMonth(), []);
  const [monthParam, setMonthFilter] = useUrlState("luna", thisMonth);
  const [workPointFilter, setWorkPointFilter] = useUrlState("punct");
  /**
   * Tabul ecranului de generare, în adresă ca la Termene și Clienți (`?tab=`): „Mișcări" (implicit)
   * și „Totalul anului". Al doilea a fost, până pe 18.09.2026, josul unui ecran propriu —
   * „Evidențe" —, care agrega exact mișcările de aici.
   */
  const [tabParam, setTab] = useUrlState("tab");
  /**
   * Tasta de ambalaje de deasupra listei (18.09.2026): „toate" = orice mișcare pe cod 15 01 xx,
   * „piata" = numai ce hrănește Anexa 1 Ambalaje, „de-completat" = numai ce nu poate intra în ea.
   *
   * <p>Stă în adresă, ca filtrul de lună: semnalele de pe tabul „Ambalaje" trimit aici cu tasta
   * apăsată, iar linkul se poate și trimite. Valorile din adresă sunt în română; enumul serverului
   * (`PackagingFilter`) rămâne al serverului.
   */
  const [packagingParam, setPackaging] = useUrlState("ambalaje");
  /**
   * „Arată-mi doar ce blochează depunerea”, trimis prin adresă de pe Acasă, din panou și din tabul
   * „Totalul anului”. Serverul știe întrebarea (`missingOperationCode`); ecranul doar o poartă, ca
   * linkul să se poată și trimite. Până pe 18.09.2026 filtrul trăia în registrul de predări al
   * ecranului „Evidențe”, care a fost scos.
   */
  const [problem, setProblem] = useUrlState("problema");
  const onlyMissingCode = problem === "cod-rd";
  /** Decizia 19.09.2026: rândurile vechi nu se blochează, se listează ca să poată fi completate. */
  const onlyIncomplete = problem === "de-completat";
  /**
   * Taburile stau numai pe „Generare”: registrul Anexa 1 e singurul cu un raport anual de depus
   * (15 martie). Pe tabul totalului, lista și banda de totaluri nici nu se cer de la server.
   */
  const isGeneration = screen === "GENERATED";
  const tab = isGeneration && (tabParam === "total" || tabParam === "ambalaje") ? tabParam : "";
  const showList = tab === "";
  /**
   * Ce tastă de ambalaje e apăsată, tradusă pentru server. O valoare necunoscută în adresă nu
   * filtrează nimic — ca la lună: o adresă scrisă de mână nu golește ecranul.
   */
  const packaging: PackagingMovementFilter | undefined =
    packagingParam === "toate"
      ? "ANY"
      : packagingParam === "piata" && isGeneration
        ? "ON_MARKET"
        : packagingParam === "de-completat"
          ? "INCOMPLETE"
          : undefined;
  /** Rândul își spune ambalajul numai cât e o tastă apăsată: altfel ar fi gol pe orice alt cod. */
  const showPackagingInfo = packaging != null;
  // O adresă editată cu mâna (`?luna=` sau `?luna=august`) nu golește ecranul și nu-l pune să
  // aducă tot: cade pe luna curentă, ca `useUrlNumber` pe implicitul lui.
  const monthFilter = isMonthValue(monthParam) ? monthParam : thisMonth;

  const filters: MovementFilters = useMemo(() => {
    const f: MovementFilters = {};
    // `yyyy-MM` = o lună; `yyyy` = anul întreg. A doua treaptă există pentru căutare: bara
    // tabelului caută în ce s-a adus, deci fără ea o predare de acum trei luni s-ar găsi numai
    // nimerind luna ei din prima.
    const [y, m] = monthFilter.split("-");
    f.year = Number(y);
    if (m) f.month = Number(m);
    if (workPointFilter) f.workPointId = workPointFilter;
    f.register = register;
    if (direction) f.direction = direction;
    // O ieșire fără cod R/D a plecat și ea de pe amplasament: `leftSite` n-are ce căuta aici,
    // rândul căutat e tocmai cel care n-a fost clasificat.
    if (onlyMissingCode) f.missingOperationCode = true;
    if (onlyIncomplete) f.incomplete = true;
    if (packaging) f.packaging = packaging;
    return f;
  }, [monthFilter, workPointFilter, register, direction, onlyMissingCode, onlyIncomplete, packaging]);

  /**
   * Căutarea, sortarea și paginarea se fac **la server** (P3.1).
   *
   * <p>Până acum se făceau aici, peste rândurile deja aduse — ceea ce mergea cât timp un client
   * avea o lună de date și nu mai mergea la doi ani. Ecranul se apăra cu filtrul de lună, dar era
   * o apărare de bunăvoie: `?luna=2026` aducea anul întreg, iar un import de istoric l-ar fi
   * atins din ziua întâi.
   *
   * <p>Filtrele de sus rămân ce erau: ele spun *ce se caută* — luna sau anul, punctul de lucru.
   * Coloanele care sortează poartă aceleași chei ca înainte; lista lor e și pe server, ca un
   * `?sort=` scris de mână să nu poată sorta după coloane pe care ecranul nu le arată.
   */
  const table = useRemoteTableView<WasteMovement>({
    initialSort: { key: "date", direction: "desc" },
    resetOn: filters,
  });
  const { data: movements, isLoading, isError } = useMovements(filters, table.params, showList);
  const view = table.bind(movements);
  /** Rândurile paginii aduse. Nu mai e „tot ce are firma" — vezi mai sus. */
  const rows = view.visible;
  const deleteMut = useDeleteMovement();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();
  const queryClient = useQueryClient();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WasteMovement | null>(null);
  // Mișcarea de la care pornește una nouă. Separată de `editing`, fiindcă răspunde la altă
  // întrebare: de unde se iau valorile, nu ce se face cu ele la salvare.
  const [duplicating, setDuplicating] = useState<WasteMovement | null>(null);
  // Mișcarea tocmai salvată: arată ce urmează. `sameAs` e „Încă una la fel” pornit de acolo.
  const [savedMovement, setSavedMovement] = useState<WasteMovement | null>(null);
  const [sameAs, setSameAs] = useState<WasteMovement | null>(null);
  // Mișcarea căreia i-a venit cântarul de la destinatar; null = dialogul e închis.
  const [weighing, setWeighing] = useState<WasteMovement | null>(null);
  // Mișcarea ale cărei atașamente se citesc. Separată de `editing`: e o vedere, nu o editare, și
  // se deschide pe orice rol — inclusiv VIEWER, care n-are butonul „Editează".
  //
  // Se ține `id`-ul, nu obiectul: lista se reîmprospătează sub dialog (o urcare din formular, o
  // ștergere), iar un instantaneu ar arăta în continuare fișierul care tocmai a plecat.
  const [attachmentsOf, setAttachmentsOf] = useState<string | null>(null);
  const viewingAttachments = attachmentsOf
    ? (rows.find((m) => m.id === attachmentsOf) ?? null)
    : null;

  // „Șterge filtrele" apare doar când e ceva de șters. Luna curentă nu e un filtru pus de cineva,
  // e punctul de plecare al ecranului — iar ștergerea o readuce, fiindcă „nicio lună" ar însemna
  // din nou toate mișcările.
  const hasFilters = Boolean(
    monthFilter !== thisMonth || workPointFilter || onlyMissingCode || onlyIncomplete || packagingParam
  );
  // O lună anume, nu un an întreg — ce hotărăște dacă golul se explică prin filtru.
  const isSingleMonth = monthFilter.includes("-");
  const monthLabel = isSingleMonth
    ? `${strings.months[Number(monthFilter.slice(5)) - 1]} ${monthFilter.slice(0, 4)}`
    : monthFilter;
  const { download: downloadAnexa3, downloadingId } = useAnexa3Download();
  const { download: downloadAviz, downloadingId: downloadingAvizId } = useAvizDownload();
  const { download: downloadAnexa2, downloadingId: downloadingAnexa2Id } = useAnexa2Download();
  const { data: company } = useCurrentCompany();
  const location = useLocation();
  const navigate = useNavigate();
  const totals = useMovementTotals(filters, showList);

  /**
   * Mișcarea pe care o cere adresa, deschisă direct în formularul de editare.
   *
   * <p>Rapoartele numesc rândul vinovat — badge-ul roșu „Fără cod R/D" din registrul de predări,
   * rândul amber din registrul de ambalaje — și până acum se opreau acolo: aflai *care* mișcare e
   * de reparat și rămâneai să o cauți cu mâna printre lunile din filtru. Linkul poartă și luna
   * (`?luna=…&miscare=…`), fiindcă altfel rândul cerut n-ar fi printre cele aduse.
   *
   * <p>Parametrul se **consumă** la deschidere. Lăsat în adresă, un refresh ar redeschide dialogul
   * peste ce lucrezi, iar butonul Înapoi n-ar mai închide nimic.
   */
  const [focusId, setFocusId] = useUrlState("miscare");
  /**
   * Mișcarea cerută prin adresă se cere **anume**, după id, nu se caută printre rândurile aduse.
   *
   * <p>De când lista vine pe pagini, „nu e printre cele aduse" nu mai înseamnă „nu există": rândul
   * pe care îl numește un raport poate fi pe pagina a treia. Căutarea în pagină ar fi răspuns
   * „mișcarea nu mai există" tocmai rândurilor pentru care linkul a fost făcut.
   */
  const focused = useMovement(focusId || null);
  useEffect(() => {
    if (!focusId) return;
    if (focused.data) {
      // Rândul numit e al altui ecran (o intrare cerută de pe Ieșiri, o generare de pe Intrări): se
      // deschide acolo, cu tot cu parametri, ca formularul să pornească pe registrul și direcția lui.
      const own = screenOfMovement(focused.data.register, focused.data.operation, company?.type);
      if (own !== screen && company) {
        navigate(SCREEN_PATH[own] + location.search, { replace: true });
        return;
      }
      // BUG-061: cine doar citește nu primește formularul de editare cu „Salvează” activ.
      if (!canWrite) {
        setFocusId("");
        return;
      }
      // Aceleași trei atribuiri ca `openEdit`, scrise aici ca efectul să nu atârne de o funcție
      // rescrisă la fiecare randare — exact felul de dependență care fura focusul din `Dialog`.
      setEditing(focused.data);
      setDuplicating(null);
      setSameAs(null);
      setDialogOpen(true);
      setFocusId("");
      return;
    }
    // Serverul a spus că nu e: ștearsă între timp, sau link vechi. Se spune, nu se deschide un
    // formular gol.
    if (focused.isError) {
      notify(t.movementNotFound, "error");
      setFocusId("");
    }
  }, [
    focusId,
    focused.data,
    focused.isError,
    setFocusId,
    notify,
    screen,
    company,
    navigate,
    location.search,
    canWrite,
  ]);

  /**
   * `?nou=1` — formularul gol, cerut din paletă (Ctrl+K → „Adaugă mișcare").
   *
   * <p>Se consumă la deschidere, ca `?miscare=`: lăsat în adresă, un refresh ar redeschide
   * dialogul peste ce lucrezi, iar butonul Înapoi n-ar mai închide nimic. Se consumă **și** când
   * rolul nu poate scrie — altfel parametrul ar rămâne agățat de un ecran care nu face nimic cu el.
   *
   * <p>Cele trei atribuiri sunt scrise aici, nu prin `openCreate`, ca efectul să nu atârne de o
   * funcție rescrisă la fiecare randare.
   */
  const [newParam, setNewParam] = useUrlState("nou");
  useEffect(() => {
    if (!newParam) return;
    setNewParam("");
    if (!canWrite) return;
    setEditing(null);
    setDuplicating(null);
    setSameAs(null);
    setDialogOpen(true);
  }, [newParam, setNewParam, canWrite]);

  /**
   * Evidența art. 48 pe **anul** din filtru, nu pe lună: evidența se depune pe an, iar stocul de
   * început vine din anii dinainte. Punctul de lucru ales se păstrează.
   */
  const [art48Busy, setArt48Busy] = useState<"xlsx" | "pdf" | null>(null);
  async function downloadArt48(format: "xlsx" | "pdf") {
    setArt48Busy(format);
    try {
      await downloadArt48Register(filters.year!, workPointFilter || undefined, format);
    } catch (err) {
      notify(apiErrorMessage(err, t.art48Error), "error");
    } finally {
      setArt48Busy(null);
    }
  }

  function openCreate() {
    setEditing(null);
    setDuplicating(null);
    setSameAs(null);
    setDialogOpen(true);
  }

  function openEdit(m: WasteMovement) {
    setEditing(m);
    setDuplicating(null);
    setSameAs(null);
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
    setSameAs(null);
    setDialogOpen(true);
  }

  async function handleDelete(m: WasteMovement) {
    const year = Number(m.date.slice(0, 4));
    const declaration = await fetchDeclaration(queryClient, year);
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
          {declaration && (
            <span className="mt-2 block font-medium text-content">
              {declaredText(t.declaredDelete, year, declaration)}
            </span>
          )}
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

  // `n` deschide formularul, unde contul are voie. Scurtătura tace pe un cont care
  // n-ar putea salva oricum: o comandă care nu face nimic e mai rea decât una lipsă.
  useHotkey("n", openCreate, { enabled: Boolean(canWrite && activeWorkPoints.length > 0) });

  // Ecranul altui tip de firmă, deschis dintr-un link sau o adresă scrisă de mână.
  const visibleScreens = screensFor(company?.type);
  if (company && !visibleScreens.includes(screen)) {
    return <Navigate replace to={SCREEN_PATH[visibleScreens[0]] + location.search} />;
  }

  // Numele ecranului și al butonului lui, după ecran (proprietarul, 15.09.2026): „Adaugă deșeuri" pe
  // generator, „Deșeuri proprii" unde firma e și colector, „Intrare" / „Ieșire" pe art. 48.
  const hasCollectorScreens = visibleScreens.includes("IN");
  const heading = {
    GENERATED: {
      title: t.generatorTitle,
      subtitle: t.generatorSubtitle,
      add: hasCollectorScreens ? t.generatorAddOwn : t.generatorAdd,
      key: "N",
      icon: Plus,
      variant: "default" as const,
    },
    IN: { title: t.inTitle, subtitle: t.inSubtitle, add: t.inAdd, key: "I", icon: ArrowDownToLine, variant: "inbound" as const },
    OUT: { title: t.outTitle, subtitle: t.outSubtitle, add: t.outAdd, key: "E", icon: ArrowUpFromLine, variant: "outline" as const },
  }[screen];

  /** Intrările și Ieșirile își au documentul lor, „Evidența cronologică”, în meniul din antet. */
  const tabs: PageTab[] = GENERATION_TABS;

  return (
    <div>
      <PageHeader
        title={heading.title}
        description={heading.subtitle}
        actions={
          <>
            {/* Evidența cronologică art. 48 e un singur document, pe amândouă ecranele art. 48. */}
            {!isGeneration && (
              <>
                {/* Un singur buton cu meniu pentru cele două formate: numele documentului o dată,
                    nu de două ori pe antet. Explicația (anul, chestionarul SIM) stă în meniu. */}
                <Menu label={t.art48Menu} align="right" disabled={art48Busy != null}>
                  <MenuItem
                    icon={FileText}
                    onClick={() => downloadArt48("xlsx")}
                    hint={t.art48Hint.replace("{year}", String(filters.year))}
                  >
                    {t.art48Xlsx}
                  </MenuItem>
                  <MenuItem icon={FileText} onClick={() => downloadArt48("pdf")}>
                    {t.art48Pdf}
                  </MenuItem>
                </Menu>
              </>
            )}
            {canWrite && (
              <Button
                variant={heading.variant}
                onClick={openCreate}
                disabled={activeWorkPoints.length === 0}
                hotkey={heading.key}
              >
                <heading.icon className="mr-2 h-4 w-4" />
                {heading.add}
              </Button>
            )}
          </>
        }
      />

      {isGeneration && (
        <PageTabs
          tabs={tabs}
          selected={tab}
          onSelect={setTab}
          label={t.tabsLabel}
          // Un singur filtru de an: tabelul e anual, luna n-are ce filtra în el. Scrie tot în
          // `luna`, ca anul să rămână același când te întorci pe „Mișcări".
          right={
            showList ? (
              <>
                <label className="flex items-center gap-2">
                  <span className="eyebrow">{t.filterMonth}</span>
                  <MonthInput
                    id="filter-month"
                    value={monthFilter}
                    onChange={setMonthFilter}
                    allowWholeYear
                  />
                </label>
                <label className="flex items-center gap-2">
                  <span className="eyebrow">{t.filterWorkPoint}</span>
                  <Select
                    id="filter-wp"
                    aria-label={t.filterWorkPoint}
                    value={workPointFilter}
                    onChange={(ev) => setWorkPointFilter(ev.target.value)}
                    className="w-44"
                  >
                    <option value="">{t.filterAll}</option>
                    {activeWorkPoints.map((w) => (
                      <option key={w.id} value={w.id}>
                        {w.name}
                      </option>
                    ))}
                  </Select>
                </label>
                {hasFilters && (
                  <Button
                    variant="ghost"
                    onClick={() => {
                      setMonthFilter(thisMonth);
                      setWorkPointFilter("");
                      setProblem("");
                      setPackaging("");
                    }}
                  >
                    {t.clearFilters}
                  </Button>
                )}
              </>
            ) : (
              <>
                <label className="flex items-center gap-2">
                  <span className="eyebrow">{strings.evidences.filterYear}</span>
                  <Select
                    id="filter-year"
                    aria-label={strings.evidences.filterYear}
                    value={monthFilter.slice(0, 4)}
                    onChange={(ev) => setMonthFilter(ev.target.value)}
                    className="w-24"
                  >
                    {yearOptions(Number(monthFilter.slice(0, 4))).map((y) => (
                      <option key={y} value={y}>
                        {y}
                      </option>
                    ))}
                  </Select>
                </label>
                {/* Punctul de lucru filtrează totalul anului; Anexa 1 Ambalaje e a firmei întregi
                    (Ordinul 794/2012 n-o cere pe punct de lucru — numai Anexa 3, care își are
                    selectul ei, lângă tabelul ei). */}
                {tab === "total" && (
                  <label className="flex items-center gap-2">
                    <span className="eyebrow">{t.filterWorkPoint}</span>
                    <Select
                      id="filter-wp-total"
                      aria-label={t.filterWorkPoint}
                      value={workPointFilter}
                      onChange={(ev) => setWorkPointFilter(ev.target.value)}
                      className="w-44"
                    >
                      <option value="">{t.filterAll}</option>
                      {activeWorkPoints.map((w) => (
                        <option key={w.id} value={w.id}>
                          {w.name}
                        </option>
                      ))}
                    </Select>
                  </label>
                )}
              </>
            )
          }
        />
      )}

      {company?.type === "COLLECTOR" && screen === "IN" && (
        <p className="mt-4 rounded-md border border-line-strong bg-surface-muted px-3 py-2 text-sm text-content-strong">
          {t.collectorOwnWasteHint}
        </p>
      )}

      {canWrite && activeWorkPoints.length === 0 && (
        <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {t.noWorkPointHint}
        </p>
      )}

      {tab === "total" && (
        <AnnualTotals year={Number(monthFilter.slice(0, 4))} workPointId={workPointFilter || undefined} />
      )}

      {tab === "ambalaje" && (
        <PackagingReport year={Number(monthFilter.slice(0, 4))} movementsPath={SCREEN_PATH.GENERATED} />
      )}

      {showList && (
        <>
      <TotalsStrip screen={screen} totals={totals.data} loading={totals.isLoading} failed={totals.isError} />

      {/* Filtrele listei stau pe linia taburilor la Generare (ca pe „Totalul anului", 18.09.2026).
          Intrări și Ieșiri n-au taburi, deci își păstrează banda lor. */}
      {!isGeneration && (
        <div className="mt-4 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
          <div>
            <Label htmlFor="filter-month">{t.filterMonth}</Label>
            <MonthInput
              id="filter-month"
              value={monthFilter}
              onChange={setMonthFilter}
              allowWholeYear
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
                setMonthFilter(thisMonth);
                setWorkPointFilter("");
                setProblem("");
                setPackaging("");
              }}
            >
              {t.clearFilters}
            </Button>
          )}
        </div>
      )}

      {/* ---- Tastele de ambalaje (18.09.2026) ----
          Tabul „Ambalaje" își ținea până acum propriul registru: aceleași mișcări, într-un al
          doilea tabel, fără căutare și fără sortare la server. Registrul a rămas unul singur, iar
          întrebările lui sunt tastele astea. Sunt taste, nu un `Select`: sub șapte opțiuni, și se
          citesc dintr-o privire (`CLAUDE.md`, „Cântar").
          „Pus de noi pe piață" numai la Generare: Anexa 1 Ambalaje e despre deșeul propriu, iar pe
          marfa preluată întrebarea n-are răspuns. */}
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <span className="eyebrow" id="filter-packaging-label">
          {t.packagingFilterLabel}
        </span>
        <PillGroup
          name="ambalaje"
          aria-labelledby="filter-packaging-label"
          selected={[packagingParam || "tot"]}
          onToggle={(value) => setPackaging(value === "tot" ? "" : value)}
          options={[
            { value: "tot", label: t.packagingFilterAll },
            { value: "toate", label: t.packagingFilterAny },
            ...(isGeneration
              ? [{ value: "piata", label: t.packagingFilterOnMarket }]
              : []),
            { value: "de-completat", label: t.packagingFilterIncomplete },
          ]}
        />
      </div>
      <section className="mt-4">
        {/* Un filtru pus din altă parte trebuie să se vadă și să se poată scoate de aici: altfel
            tabelul pare gol pe nedrept, iar omul caută rânduri care există. */}
        {onlyMissingCode && (
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-state-bad bg-surface-muted px-3 py-2 text-sm text-state-bad-text">
            <span className="font-medium">{t.onlyMissingCode}</span>
            <button
              type="button"
              onClick={() => setProblem("")}
              className="shrink-0 font-medium underline hover:no-underline"
            >
              {t.onlyMissingCodeOff}
            </button>
          </div>
        )}
        {onlyIncomplete ? (
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-state-warn bg-surface-muted px-3 py-2 text-sm text-state-warn-text">
            <span className="font-medium">{t.onlyIncomplete}</span>
            <button type="button" onClick={() => setProblem("")} className="shrink-0 font-medium underline hover:no-underline">
              {t.onlyMissingCodeOff}
            </button>
          </div>
        ) : (
          !onlyMissingCode &&
          (totals.data?.incomplete ?? 0) > 0 && (
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-state-warn bg-surface-muted px-3 py-2 text-sm text-state-warn-text">
              <span className="font-medium">{t.incompleteRows(totals.data!.incomplete)}</span>
              <button type="button" onClick={() => setProblem("de-completat")} className="shrink-0 font-medium underline hover:no-underline">
                {t.incompleteShow}
              </button>
            </div>
          )
        )}
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isError && (
          <>
            <TableToolbar view={view} placeholder={t.searchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="date" sort={view.sort} onSort={view.toggleSort}>
                    {t.colDate}
                  </SortableTH>
                  <SortableTH sortKey="wasteCode" sort={view.sort} onSort={view.toggleSort}>
                    {t.colWasteCode}
                  </SortableTH>
                  <SortableTH
                    sortKey="quantity"
                    sort={view.sort}
                    onSort={view.toggleSort}
                    align="right"
                  >
                    {t.colQuantity}
                  </SortableTH>
                  {/* Pe Intrări operațiunea e mereu preluarea: coloana n-ar spune nimic. */}
                  {screen !== "IN" && <TH>{screen === "OUT" ? t.colRdCode : t.colOperation}</TH>}
                  <SortableTH sortKey="partnerName" sort={view.sort} onSort={view.toggleSort}>
                    {screen === "IN" ? t.colFrom : screen === "OUT" ? t.colTo : t.colPartner}
                  </SortableTH>
                  {isGeneration && <TH>{t.colInternalGenerator}</TH>}
                  <SortableTH sortKey="workPointName" sort={view.sort} onSort={view.toggleSort}>
                    {t.colWorkPoint}
                  </SortableTH>
                  <TH className="text-center">{t.colAttachments}</TH>
                  {canWrite && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={7 + (screen !== "IN" ? 1 : 0) + (isGeneration ? 1 : 0) + (canWrite ? 1 : 0)}
                    loading={isLoading}
                    icon={Truck}
                    title={
                      view.emptiedBySearch
                        ? strings.common.noResults
                        : isSingleMonth
                          ? t.emptyMonth.replace("{month}", monthLabel)
                          : t.empty
                    }
                    description={
                      view.emptiedBySearch
                        ? strings.common.noResultsHint
                        : isSingleMonth
                          ? t.emptyMonthHint
                          : t.emptyHint
                    }
                    /**
                     * Ieșirea din luna goală. Fără ea, un ecran care pornește pe luna curentă
                     * arată „nicio mișcare" unui client care are șapte sute — și nimic pe ecran
                     * n-ar spune că vina e a filtrului, nu a datelor.
                     */
                    action={
                      !view.emptiedBySearch &&
                      isSingleMonth && (
                        <Button
                          variant="outline"
                          onClick={() => setMonthFilter(monthFilter.slice(0, 4))}
                        >
                          {t.showWholeYear.replace("{year}", monthFilter.slice(0, 4))}
                        </Button>
                      )
                    }
                  />
                )}
                {view.visible.map((m) => (
                  <TR key={m.id}>
                    <TD className="whitespace-nowrap font-mono text-xs">{formatDate(m.date)}</TD>
                    <TD>
                      <BinSwatch code={m.wasteCode} hazardous={m.hazardous} />
                      <span className="font-mono font-medium text-content">{m.wasteCode}</span>
                      {m.hazardous && (
                        <Badge variant="danger" className="ml-2">
                          {t.hazardous}
                        </Badge>
                      )}
                      <span className="block max-w-[12rem] truncate text-xs text-content-muted">
                        {m.wasteCodeName}
                      </span>
                      {/* Galben, ca „Autorizație expirată”: rândul nu e greșit, dar poartă o
                          afirmație pe care actul cere s-o susții cu o hârtie. Stă lângă cod, nu
                          lângă partener, fiindcă ce se pune la îndoială e încadrarea, nu predarea
                          — și de aceea apare și pe intrări, și pe generări. */}
                      {m.mirrorClassificationUnproven && (
                        <Tooltip content={t.mirrorCodeHint(m.mirrorOf)}>
                          <Badge variant="warning" className="mt-0.5 block w-fit">
                            {t.mirrorCode}
                          </Badge>
                        </Tooltip>
                      )}
                      {(m.storageType || m.treatmentMethod) && (
                        <span className="mt-0.5 block text-xs text-content-muted">
                          {[
                            m.storageType && e.storageType[m.storageType],
                            m.treatmentMethod && e.treatmentMethod[m.treatmentMethod],
                          ]
                            .filter(Boolean)
                            .join(" · ")}
                        </span>
                      )}
                      {/* Ce ştie mişcarea despre ambalaj, sub cod, numai cât e o tastă apăsată.
                          **Nu** coloane proprii: măsurat la 1440px, trei coloane duceau tabelul la
                          1415 într-un 1114, adică la derulare laterală — iar informaţia e despre
                          încadrarea codului, deci stă unde stau şi celelalte lucruri spuse despre
                          el (codul-oglindă, felul stocării).
                          ⚠️ **Ce lipseşte, sau ce e — nu amândouă.** Materialul scris lângă un
                          „Fără felul ambalajului" e trei rânduri de mărunţişuri sub fiecare cod;
                          rândul care are ceva de reparat spune doar atât, iar cel întreg îşi spune
                          încadrarea, pe un rând. */}
                      {showPackagingInfo && (
                        <span className="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-content-muted">
                          {!m.effectivePackagingMaterial && (
                            <Badge variant="warning">{pk.missingMaterialShort}</Badge>
                          )}
                          {!m.packagingCategory && <Badge variant="warning">{pk.missingKindShort}</Badge>}
                          {m.effectivePackagingMaterial && m.packagingCategory && (
                            <span>
                              {materialLabels[m.effectivePackagingMaterial]}
                              {m.packagingMaterial == null && (
                                <span className="ml-1 text-content-subtle">({pk.fromCode})</span>
                              )}
                              {" · "}
                              {e.packagingCategory[m.packagingCategory]}
                              {m.packagingReusable && ` · ${pk.reusableShort}`}
                              {m.packagingHazardousContent && ` · ${pk.hazardousShort}`}
                            </span>
                          )}
                          {/* Starea faţă de Anexa 1 se spune numai când nu e cea aşteptată: un „Da"
                              pe fiecare rând ar fi o afirmaţie repetată degeaba. */}
                          {isGeneration && m.packagingOnMarket === false && (
                            <Tooltip content={pk.inAnexa1NoHint}>
                              <Badge variant="muted">{pk.inAnexa1No}</Badge>
                            </Tooltip>
                          )}
                          {isGeneration && m.packagingOnMarket == null && (
                            <Tooltip content={pk.inAnexa1LegacyHint}>
                              <Badge variant="warning">{pk.inAnexa1Legacy}</Badge>
                            </Tooltip>
                          )}
                        </span>
                      )}
                    </TD>
                    <TD className="whitespace-nowrap text-right font-mono font-medium">
                      {m.quantity != null ? (
                        <>
                          {m.quantity} <span className="text-xs font-normal text-content-muted">{e.unit[m.unit]}</span>
                        </>
                      ) : (
                        <Tooltip content={t.awaitingWeighingHint}>
                          <Badge variant="warning">{t.awaitingWeighing}</Badge>
                        </Tooltip>
                      )}
                    </TD>
                    {screen !== "IN" && (
                      <TD>
                        {/* A legacy exit is the one row on this screen that is wrong as it stands, so
                            it is red, not grey: the quantity left the site but reaches neither
                            official column of Anexa 1. Editing the row is how it gets completed. */}
                        {m.operation === "UNCLASSIFIED_OUT" ? (
                          <Tooltip content={t.missingCodeHint}>
                            <Badge variant="danger">{t.missingCode}</Badge>
                          </Tooltip>
                        ) : screen === "OUT" ? (
                          <>
                            <span className="font-mono font-medium">{m.operationCode ?? "—"}</span>
                            <span className="ml-1.5 text-xs text-content-muted">{e.wasteOperation[m.operation]}</span>
                          </>
                        ) : (
                          <>
                            {e.wasteOperation[m.operation]}
                            {m.operationCode && (
                              <span className="ml-1 font-mono text-xs text-content-muted">{m.operationCode}</span>
                            )}
                          </>
                        )}
                      </TD>
                    )}
                    <TD>
                      {m.partnerName || "—"}
                      {/* Galben, nu roșu: predarea chiar a avut loc, iar rândul nu e greșit — spre
                          deosebire de UNCLASSIFIED_OUT de mai sus, care nu intră în nicio coloană
                          oficială. Aici lipsește o condiție de legalitate a predării (OUG 92/2021
                          art. 23 alin. (1)), pe care clientul o poate lămuri cu partenerul; e
                          aceeași familie cu „De cântărit". */}
                      {m.recipientAuthorizationExpired &&
                        (() => {
                          const expiry = m.recipientAuthorizationExpiry
                            ? formatDate(m.recipientAuthorizationExpiry)
                            : null;
                          const why = t.authExpiredAtHandoverHint(expiry);
                          /* Badge-ul spunea că lipsește o condiție de legalitate a predării și
                             trimitea, în chiar textul lui, la fișa partenerului — dar nu ducea
                             nicăieri. Al doilea fund de sac din aplicație, după cel roșu reparat
                             pe 07.09; același drum, alt badge.

                             ⚠️ Nu mai e învelit în `Tooltip`: bula își randează propriul
                             `<button>`, deci n-ar putea înveli un link (defectul din 07.09,
                             seara). Data urcă în badge — pe ecran, nu în spatele unui hover, ceea
                             ce e oricum mai bine pe telefon —, iar motivul rămâne întreg în
                             `aria-label`, pentru cine citește cu tastatura. */
                          return m.partnerId ? (
                            <Link
                              to={`/parteneri?partener=${m.partnerId}`}
                              aria-label={why}
                              className="mt-0.5 block w-fit rounded hover:opacity-80"
                            >
                              <Badge variant="warning" className="whitespace-normal">
                                {expiry
                                  ? `${t.authExpiredAtHandover} · ${expiry}`
                                  : t.authExpiredAtHandover}
                              </Badge>
                            </Link>
                          ) : (
                            <Tooltip content={why}>
                              <Badge variant="warning" className="mt-0.5 block w-fit">
                                {t.authExpiredAtHandover}
                              </Badge>
                            </Tooltip>
                          );
                        })()}
                    </TD>
                    {isGeneration && <TD>{m.internalGeneratorName || "—"}</TD>}
                    <TD className="text-content-muted">{m.workPointName}</TD>
                    <TD className="text-center">
                      {m.attachments.length > 0 ? (
                        // Buton, nu text: cifra spunea că există un document și nu ducea la el.
                        // Nu `Tooltip` — bula își randează propriul `<button>`, deci n-ar putea
                        // înveli linkuri; și oricum un fișier se deschide, nu se citește la hover.
                        <button
                          type="button"
                          onClick={() => setAttachmentsOf(m.id)}
                          aria-label={t.attachmentsView}
                          className="inline-flex items-center gap-1 rounded px-1 py-0.5 text-content-muted underline-offset-2 hover:text-brand hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
                        >
                          <Paperclip className="h-3.5 w-3.5" />
                          {m.attachments.length}
                        </button>
                      ) : (
                        "—"
                      )}
                    </TD>
                    {canWrite && (
                      <TD sticky="right" className="text-right">
                        {/* Una afară, restul în meniu. Patru butoane cu text pe fiecare rând
                            înseamnă vreo 380px de comenzi repetate, într-un tabel care are deja
                            nouă coloane. Afară rămâne cea care e chiar de făcut acum: cântarul,
                            când lipsește cifra; altfel, editarea. */}
                        <div className="flex items-center justify-end gap-1">
                          {/* O linie de cântar nu se editează de aici: se schimbă numai prin
                              operațiunea ei (BUG-018), deci rândul duce acolo. */}
                          {m.weighingOperationId ? (
                            <LinkButton
                              to={`/cantar?op=${m.weighingOperationId}`}
                              variant="outline"
                              size="sm"
                            >
                              <Scale className="mr-1.5 h-3.5 w-3.5" aria-hidden />
                              {t.openWeighing}
                            </LinkButton>
                          ) : m.quantity == null ? (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => setWeighing(m)}
                              aria-label={t.recordWeight}
                            >
                              <Scale className="mr-1.5 h-3.5 w-3.5" aria-hidden />
                              {t.recordWeightShort}
                            </Button>
                          ) : (
                            // Doar creionul: cu IBM Plex, „Editează" scris pe fiecare rând scotea
                            // Generare din 1440px (măsurat 15.09.2026: 1301px în 1114). Numele
                            // rămâne pentru cititorul de ecran și în meniul „⋯".
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => openEdit(m)}
                              aria-label={strings.common.edit}
                            >
                              <Pencil className="h-4 w-4" aria-hidden />
                            </Button>
                          )}
                          <RowActions>
                            {!m.weighingOperationId && (
                              <>
                                <RowAction icon={Pencil} onClick={() => openEdit(m)}>
                                  {strings.common.edit}
                                </RowAction>
                                <RowAction icon={Copy} onClick={() => openDuplicate(m)}>
                                  {t.duplicate}
                                </RowAction>
                              </>
                            )}
                            {canPrintAnexa3(m) && (
                              <RowAction
                                icon={FileText}
                                disabled={downloadingId === m.id}
                                onClick={() => downloadAnexa3(m)}
                              >
                                {downloadingId === m.id ? t.anexa3Downloading : t.anexa3Download}
                              </RowAction>
                            )}
                            {canPrintAviz(m) && (
                              <RowAction
                                icon={FileText}
                                disabled={downloadingAvizId === m.id}
                                onClick={() => downloadAviz(m)}
                              >
                                {downloadingAvizId === m.id ? t.avizDownloading : t.avizDownload}
                              </RowAction>
                            )}
                            {/* Perechea: același transport, celălalt fel de deșeu. Butonul de
                                Anexa 2 apare exact unde nu apare cel de Anexa 3. */}
                            {canPrintAnexa2(m, company?.type) && (
                              <RowAction
                                icon={FileText}
                                disabled={downloadingAnexa2Id === m.id}
                                onClick={() => downloadAnexa2(m)}
                              >
                                {downloadingAnexa2Id === m.id
                                  ? t.anexa2Downloading
                                  : t.anexa2Download}
                              </RowAction>
                            )}
                            {canSeeHistory && (
                              <RowAction
                                icon={History}
                                onClick={() => navigate(`/setari/jurnal-audit?istoric=${m.id}`)}
                              >
                                {t.history}
                              </RowAction>
                            )}
                            {!m.weighingOperationId && (
                              <RowAction icon={Trash2} tone="danger" onClick={() => handleDelete(m)}>
                                {strings.common.delete}
                              </RowAction>
                            )}
                          </RowActions>
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

      {weighing && (
        <RecordWeightDialog movement={weighing} onClose={() => setWeighing(null)} />
      )}

      {viewingAttachments && (
        <AttachmentsDialog
          movement={viewingAttachments}
          onClose={() => setAttachmentsOf(null)}
        />
      )}

      {dialogOpen && (
        <MovementFormDialog
          editing={editing}
          duplicateOf={duplicating}
          sameAs={sameAs}
          onCreated={(m) => {
            setDialogOpen(false);
            setSavedMovement(m);
          }}
          workPoints={activeWorkPoints.map((w) => ({ id: w.id, name: w.name }))}
          defaultWorkPointId={workPointFilter || activeWorkPoints[0]?.id}
          screen={register}
          direction={direction}
          onClose={() => setDialogOpen(false)}
        />
      )}

      {savedMovement && (
        <MovementSavedDialog
          movement={savedMovement}
          screen={register}
          direction={direction}
          onClose={() => setSavedMovement(null)}
          onAnother={() => {
            const from = savedMovement;
            setSavedMovement(null);
            setEditing(null);
            setDuplicating(null);
            setSameAs(from);
            setDialogOpen(true);
          }}
        />
      )}

      {confirmDialog}
    </div>
  );
}
