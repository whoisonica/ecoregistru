import { Fragment, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, FileSpreadsheet, FileText, Package, Pencil, Plus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  downloadPackagingAnexa3,
  downloadPackagingDeclaration,
  usePackagingHandovers,
  usePackagingMarket,
  usePackagingMovements,
  usePackagingAnexa3,
  usePackagingTable1,
  usePackagingUnclassified,
  useSavePackagingMarket,
} from "@/hooks/usePackaging";
import type {
  PackagingAnexa3,
  PackagingMarketRow,
  PackagingMaterial,
  PackagingTable1Row,
  PackagingUnclassifiedRow,
  WasteMovement,
} from "@/lib/types";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { useUrlNumber, useUrlState } from "@/hooks/useUrlState";
import { Badge } from "@/components/ui/badge";
import { Tooltip } from "@/components/ui/tooltip";
import { Button, LinkButton } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { SectionNav } from "@/components/ui/section-nav";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow, TableSkeletonRows } from "@/components/ui/table-fallback";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";

const t = strings.packaging;
const materialLabels = strings.enums.packagingMaterial;
const categoryLabels = strings.enums.packagingCategory;

/** Year options: current year down to five years back, same as the other documents. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
function kg(value: number | null | undefined) {
  return value == null ? "—" : kgFormat.format(value);
}

/** The six figures of a material row; "Total (col. 3+5)" is a sum and is never typed. */
const COLUMNS = [
  "salesPackaging",
  "primaryTotal",
  "primaryReusable",
  "secondaryTotal",
  "secondaryReusable",
  "hazardousContent",
] as const;
type Column = (typeof COLUMNS)[number];

/**
 * Numele coloanei, pentru numele accesibil al celulei.
 *
 * <p>Grila de suprascriere are șaizeci și șase de câmpuri numerice fără nicio etichetă: un
 * cititor de ecran anunța „câmp de editare" de șaizeci și șase de ori, fără să spună nici
 * materialul, nici coloana. Capul de tabel se **vede**, dar nu se aude.
 */
const COLUMN_LABELS: Record<Column, string> = {
  salesPackaging: t.colSales,
  primaryTotal: t.colPrimary,
  primaryReusable: t.colPrimaryReusable,
  secondaryTotal: t.colSecondary,
  secondaryReusable: t.colSecondaryReusable,
  hazardousContent: t.colHazardous,
};

/**
 * Rândurile de material, în ordinea actului, cu cele trei sume intercalate — exact aşa cum le
 * desenează formularul: PET + Alte plastice = Total plastic, Aluminiu + Oţel = Total metal, iar
 * Sticla, Hârtia carton, Lemnul şi Altele stau singure.
 */
const MATERIAL_ORDER: PackagingMaterial[] = [
  "STICLA",
  "PET",
  "ALTE_PLASTICE",
  "HARTIE_CARTON",
  "ALUMINIU",
  "OTEL",
  "LEMN",
  "ALTELE",
];
const PLASTIC_PARTS: PackagingMaterial[] = ["PET", "ALTE_PLASTICE"];
const METAL_PARTS: PackagingMaterial[] = ["ALUMINIU", "OTEL"];

/** Col. 2 of the form: a sum of columns 3 and 5, empty when both are. */
function packagedGoodsTotal(row: PackagingTable1Row | undefined) {
  if (!row || (row.primaryTotal == null && row.secondaryTotal == null)) return null;
  return (row.primaryTotal ?? 0) + (row.secondaryTotal ?? 0);
}

/**
 * A sum over several material rows that stays empty when every one of them is: adding nothing to
 * nothing is not zero on this form, it is still "nobody answered".
 */
function sumOver(
  rows: PackagingTable1Row[],
  parts: PackagingMaterial[],
  pick: (row: PackagingTable1Row) => number | null
) {
  let total: number | null = null;
  for (const row of rows) {
    if (!parts.includes(row.material)) continue;
    const value = pick(row);
    if (value != null) total = (total ?? 0) + value;
  }
  return total;
}

function fill(template: string, n: number) {
  return template.replace("{n}", String(n));
}

/**
 * Ce s-a întâmplat cu rândul de suprascriere: e ceva tastat şi neplecat, pleacă chiar acum, sau a
 * ajuns. Grila se salvează singură la ieşirea din celulă, iar până pe 08.09.2026 se vedeau numai
 * erorile — deci cine completa şaizeci şi şase de cifre n-avea de unde şti câte au ajuns.
 *
 * <p>Ordinea contează: cât timp salvarea zboară se spune asta, chiar dacă s-a mai tastat ceva între
 * timp; ce s-a tastat rămâne „nesalvat" după ce răspunsul vine, fiindcă aia e situaţia. Un rând
 * neatins nu spune nimic — un „—" pe opt rânduri ar fi opt afirmaţii despre nimic.
 */
function RowStatus({ state, dirty }: { state?: "saving" | "saved"; dirty: boolean }) {
  if (state === "saving") return <span className="text-content-muted">{t.overrideSaving}</span>;
  if (dirty) return <Badge variant="warning">{t.overrideDirty}</Badge>;
  if (state === "saved") return <Badge variant="success">{t.overrideSaved}</Badge>;
  return null;
}

/**
 * Tabul **Ambalaje** — Anexa 1 Ambalaje (Ordinul 794/2012) şi tot ce ţine de ea.
 *
 * <p>Ecranul are o singură sursă: **mişcările pe coduri 15 01 xx**. Registrul le arată aşa cum
 * sunt, cu ce le lipseşte scris pe fiecare rând; cele două tabele ale declaraţiei se însumează din
 * ele; iar butonul de descărcare scoate exact ce va citi agenţia — un `.xls` cu două foi, formatul
 * pe care art. 6 din ordin îl cere pe nume.
 *
 * <p>Grila în care se completau cele şaizeci şi şase de celule ale tabelului 1 nu mai e ecranul
 * principal: rămâne, pliată, ca **suprascriere** pe material, fiindcă tabelul e legal despre marfa
 * pusă pe piaţă şi o firmă poate şti că cifra ei diferă de ce arată deşeul.
 */
export function PackagingPage() {
  const { user } = useAuth();
  const canWrite =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const [year, setYear] = useUrlNumber("an", new Date().getFullYear());
  const { data: movements, isLoading: loadingMovements } = usePackagingMovements(year);
  const { data: table1, isLoading: loadingTable1 } = usePackagingTable1(year);
  const { data: handovers } = usePackagingHandovers(year);
  const { data: unclassified } = usePackagingUnclassified(year);
  const { data: overrides } = usePackagingMarket(year);
  const saveMut = useSavePackagingMarket();
  const { notify } = useToast();
  const [downloading, setDownloading] = useState<"xls" | "pdf" | null>(null);
  const [overridesOpen, setOverridesOpen] = useState(false);

  // Ce s-a tastat şi nu s-a salvat încă, per material. Salvarea e pe rând, la ieşirea din câmp:
  // grila are şaizeci şi şase de celule, iar un buton „salvează tot" ar face o greşeală invizibilă.
  const [draft, setDraft] = useState<Record<string, Record<string, string>>>({});
  /**
   * Ce face rândul chiar acum: pleacă spre server, sau tocmai a ajuns. Starea e **per material**,
   * nu pe mutaţie: `saveMut.isPending` e unul singur pentru toată grila, deci ar aprinde toate cele
   * opt rânduri la fiecare salvare. „Nesalvat" nu stă aici — ăla se citeşte din `draft`, care e
   * chiar definiţia lui.
   */
  const [rowState, setRowState] = useState<Record<string, "saving" | "saved">>({});
  // „Salvat" se stinge singur; fără curăţarea asta, un cronometru ar scrie într-o componentă
  // demontată la schimbarea anului sau la plecarea de pe ecran.
  const savedTimers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  useEffect(() => {
    const timers = savedTimers.current;
    return () => {
      for (const id of Object.values(timers)) clearTimeout(id);
    };
  }, []);
  useEffect(() => {
    setDraft({});
    setRowState({});
  }, [year]);

  const rows = table1 ?? [];
  /** Câte rânduri au cifre tastate şi neplecate. Zero e starea normală, deci nu se scrie nimic. */
  const dirtyRows = Object.keys(draft).length;

  /**
   * Registrul de mișcări pe coduri 15 01 xx. Fără sortare implicită: vine de la server în ordinea
   * în care se citește un registru, iar aia rămâne.
   */
  const registerView = useTableView(movements ?? [], {
    searchText: (m: WasteMovement) =>
      [m.wasteCode, m.wasteCodeName, m.partnerName, m.workPointName].filter(Boolean).join(" "),
    comparators: {
      date: (a: WasteMovement, b: WasteMovement) => a.date.localeCompare(b.date),
      wasteCode: (a: WasteMovement, b: WasteMovement) =>
        a.wasteCode.localeCompare(b.wasteCode, "ro"),
    },
  });
  const rowFor = (material: PackagingMaterial) => rows.find((r) => r.material === material);

  const signals = useMemo(() => {
    const list = unclassified ?? [];
    // Semnalele privesc doar ce hrăneşte declaraţia. O mişcare pe marfă preluată nu intră în Anexa 1
    // oricât de completă ar fi, deci a o număra la „de cântărit" ar cere o reparaţie fără efect.
    // Semnalele privesc doar ce hrăneşte declaraţia: marfa preluată şi ambalajul pe care nu l-am
    // pus noi pe piaţă n-au ce repara acolo, oricât de incomplete ar fi.
    const all = (movements ?? []).filter(
      (m) => m.register !== "ART_48" && m.countsForAnexa1Packaging
    );
    return {
      missingMaterial: list.filter((r) => r.missingMaterial).length,
      missingCategory: list.filter((r) => !r.missingMaterial && r.missingCategory).length,
      awaitingWeighing: all.filter((m) => m.quantity == null).length,
      missingOperation: all.filter(
        (m) => (m.operation === "RECOVERED" || m.operation === "DISPOSED") && !m.operationCode
      ).length,
    };
  }, [unclassified, movements]);

  const unclassifiedIds = useMemo(
    () => new Set((unclassified ?? []).map((r: PackagingUnclassifiedRow) => r.movementId)),
    [unclassified]
  );

  /**
   * Rândurile care ies din calcul dinadins: marfa preluată de la terţi (Anexa 1 e despre deşeul
   * propriu) şi ambalajul pe care l-a pus pe piaţă furnizorul. Nu sunt incomplete, sunt în afara
   * documentului — de aceea nici nu se colorează, nici nu primesc acţiune.
   */
  const isExcluded = (m: WasteMovement) =>
    m.register === "ART_48" || m.packagingOnMarket === false;

  /**
   * Rândul are ceva de completat **pe mişcare**, deci acţiunea are unde duce.
   *
   * <p>Condiţia e ţinută dinadins identică cu cea care colorează rândul: dacă tabelul semnalează
   * ceva, se poate apăsa; dacă nu semnalează nimic, nu apare niciun buton care să sugereze că ar
   * fi. Două condiţii apropiate dar diferite ar fi arătat exact ca un defect.
   */
  const needsFixing = (m: WasteMovement) =>
    !isExcluded(m) &&
    (unclassifiedIds.has(m.id) ||
      ((m.operation === "RECOVERED" || m.operation === "DISPOSED") && !m.operationCode));

  async function handleDownload(format: "xls" | "pdf") {
    setDownloading(format);
    try {
      await downloadPackagingDeclaration(year, format);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.downloadError), "error");
    } finally {
      setDownloading(null);
    }
  }

  function cellValue(row: PackagingMarketRow, column: Column) {
    const pending = draft[row.material]?.[column];
    if (pending !== undefined) return pending;
    return row[column] == null ? "" : String(row[column]);
  }

  function edit(material: PackagingMaterial, column: Column, value: string) {
    setDraft((prev) => ({ ...prev, [material]: { ...prev[material], [column]: value } }));
  }

  function markSaved(material: string) {
    setRowState((prev) => ({ ...prev, [material]: "saved" }));
    clearTimeout(savedTimers.current[material]);
    savedTimers.current[material] = setTimeout(() => {
      setRowState((prev) => {
        // Numai dacă între timp n-a început altă salvare pe rândul ăsta: altfel „se salvează…"
        // s-ar stinge de la cronometrul salvării dinainte.
        if (prev[material] !== "saved") return prev;
        const next = { ...prev };
        delete next[material];
        return next;
      });
    }, 4000);
  }

  function saveOverride(row: PackagingMarketRow) {
    const pending = draft[row.material];
    if (!pending) return;
    // O celulă goală înseamnă „nu suprascriu", şi se trimite null, nu zero. Un rând golit de tot
    // şterge suprascrierea şi lasă cifra din mişcări să revină.
    const numeric = (column: Column) => {
      const raw = pending[column] ?? (row[column] == null ? "" : String(row[column]));
      if (raw.trim() === "") return null;
      const parsed = Number(raw.replace(",", "."));
      return Number.isFinite(parsed) ? parsed : null;
    };
    // Ce pleacă acum, exact aşa cum e în clipa asta. Se ţine minte fiindcă răspunsul vine mai
    // târziu, iar între timp se poate tasta în altă celulă a aceluiaşi rând.
    const sent = { ...pending };
    setRowState((prev) => ({ ...prev, [row.material]: "saving" }));
    clearTimeout(savedTimers.current[row.material]);
    saveMut.mutate(
      {
        material: row.material,
        year,
        salesPackaging: numeric("salesPackaging"),
        primaryTotal: numeric("primaryTotal"),
        primaryReusable: numeric("primaryReusable"),
        secondaryTotal: numeric("secondaryTotal"),
        secondaryReusable: numeric("secondaryReusable"),
        hazardousContent: numeric("hazardousContent"),
      },
      {
        onSuccess: () => {
          setDraft((prev) => {
            const current = prev[row.material];
            if (!current) return prev;
            // Se şterge din ciornă **numai ce s-a trimis şi n-a fost tastat între timp**. Înainte
            // se ştergea rândul întreg: cine tasta în altă celulă cât zbura salvarea îşi pierdea
            // cifra tăcut, la reîmprospătarea de după răspuns. Ce rămâne aici e chiar ce n-a plecat,
            // deci rândul rămâne pe „nesalvat" şi pleacă la ieşirea din celula lui.
            const rest = Object.fromEntries(
              Object.entries(current).filter(([column, value]) => sent[column] !== value)
            );
            const next = { ...prev };
            if (Object.keys(rest).length === 0) delete next[row.material];
            else next[row.material] = rest;
            return next;
          });
          markSaved(row.material);
        },
        onError: (err) => {
          // Ciorna rămâne pe loc: cifra tastată nu se pierde fiindcă serverul a refuzat-o, iar
          // rândul se întoarce la „nesalvat", care e adevărul.
          setRowState((prev) => {
            const next = { ...prev };
            delete next[row.material];
            return next;
          });
          notify(apiErrorMessage(err, t.saveError), "error");
        },
      }
    );
  }

  return (
    <div>
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          <div className="flex flex-col gap-2 sm:items-end">
            <div className="flex flex-wrap gap-2">
              {/* Eticheta rămâne aceeași cât se lucrează: schimbată în „Se încarcă...",
                  butonul își schimba lățimea sub deget. */}
              <Button
                onClick={() => handleDownload("xls")}
                disabled={downloading != null}
                loading={downloading === "xls"}
              >
                {downloading !== "xls" && <FileSpreadsheet className="mr-2 h-4 w-4" />}
                {t.downloadXls}
              </Button>
              <Button
                variant="outline"
                onClick={() => handleDownload("pdf")}
                disabled={downloading != null}
                loading={downloading === "pdf"}
              >
                {downloading !== "pdf" && <FileText className="mr-2 h-4 w-4" />}
                {t.downloadPdf}
              </Button>
            </div>
            <p className="max-w-sm text-xs text-content-muted sm:text-right">{t.downloadHint}</p>
          </div>
        }
      />

      <div className="mt-6 w-40">
        <Label htmlFor="pk-year">{t.year}</Label>
        <Select id="pk-year" value={String(year)} onChange={(ev) => setYear(Number(ev.target.value))}>
          {yearOptions().map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </Select>
      </div>

      {/* Cuprinsul stă deasupra semnalelor, nu sub ele: bara e a paginii, iar caseta chihlimbarie
          apare şi dispare după cum e completată luna — un cuprins care sare cu ea ar fi altă bară
          la fiecare deschidere. */}
      <SectionNav
        label={t.sections}
        items={[
          { id: "registru", label: t.navRegister },
          { id: "tabelul-1", label: t.navTable1 },
          { id: "tabelul-2", label: t.navTable2 },
          { id: "anexa-3", label: t.navAnexa3 },
        ]}
      />

      {/* ---- Ce blochează declaraţia, spus înainte de tabele ---- */}
      {(signals.missingMaterial > 0 ||
        signals.missingCategory > 0 ||
        signals.awaitingWeighing > 0 ||
        signals.missingOperation > 0) && (
        <section className="mt-6 rounded-lg border border-amber-200 bg-amber-50 p-4">
          <h2 className="flex items-center gap-2 text-sm font-semibold text-amber-900">
            <AlertTriangle className="h-4 w-4" />
            {t.blockedTitle}
          </h2>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-amber-900">
            {signals.missingMaterial > 0 && (
              <li>{fill(t.blockedMissingMaterial, signals.missingMaterial)}</li>
            )}
            {signals.missingCategory > 0 && (
              <li>{fill(t.blockedMissingCategory, signals.missingCategory)}</li>
            )}
            {signals.missingOperation > 0 && (
              <li>{fill(t.missingOperation, signals.missingOperation)}</li>
            )}
            {signals.awaitingWeighing > 0 && (
              <li>{fill(t.awaitingWeighing, signals.awaitingWeighing)}</li>
            )}
          </ul>
        </section>
      )}

      {/* ---- Registrul: mişcările din care iese totul ---- */}
      <section id="registru" className="mt-8 scroll-mt-20">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">{t.registerTitle}</h2>
            <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.registerHint}</p>
          </div>
          {canWrite && (
            <Link
              to="/miscari"
              className="inline-flex h-10 items-center rounded-md border border-line-strong px-4 text-sm font-medium text-content-strong hover:bg-surface-muted"
            >
              <Plus className="mr-2 h-4 w-4" />
              {t.addMovement}
            </Link>
          )}
        </div>
        <TableToolbar
          view={registerView}
          placeholder={t.searchPlaceholder}
          className="mt-3"
        />
        <div>
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH
                  sortKey="date"
                  sort={registerView.sort}
                  onSort={registerView.toggleSort}
                >
                  {t.date}
                </SortableTH>
                <TH>{t.code}</TH>
                <TH>{t.material}</TH>
                <TH>{t.kind}</TH>
                <TH className="text-right">{t.quantity}</TH>
                <TH>{t.partner}</TH>
                <TH>{t.operation}</TH>
                <TH>{t.inAnexa1}</TH>
                <TH>{t.origin}</TH>
                <TH>{t.workPoint}</TH>
                <TH sticky="right" className="text-right">
                  {strings.common.actions}
                </TH>
              </TR>
            </THead>
            <TBody>
              {(loadingMovements || registerView.visible.length === 0) && (
                <TableFallbackRow
                  columns={11}
                  loading={loadingMovements}
                  icon={Package}
                  title={
                    registerView.emptiedBySearch ? strings.common.noResults : t.registerEmpty
                  }
                />
              )}
              {registerView.visible.map((m: WasteMovement) => (
                <TR
                  key={m.id}
                  className={
                    isExcluded(m)
                      ? "text-content-subtle"
                      : unclassifiedIds.has(m.id)
                        ? "bg-amber-50/60"
                        : undefined
                  }
                >
                  <TD className="whitespace-nowrap">{formatDate(m.date)}</TD>
                  <TD className="whitespace-nowrap font-mono text-xs">{m.wasteCode}</TD>
                  <TD className="whitespace-nowrap">
                    {m.effectivePackagingMaterial ? (
                      <>
                        {materialLabels[m.effectivePackagingMaterial]}
                        {m.packagingMaterial == null && (
                          <span className="ml-1 text-xs text-content-subtle">({t.fromCode})</span>
                        )}
                      </>
                    ) : (
                      <Badge variant="warning">{t.fix}</Badge>
                    )}
                  </TD>
                  <TD className="whitespace-nowrap">
                    {m.packagingCategory ? (
                      <>
                        {categoryLabels[m.packagingCategory]}
                        {m.packagingReusable && (
                          <span className="ml-1 text-xs text-content-muted">· reutilizabil</span>
                        )}
                        {m.packagingHazardousContent && (
                          <span className="ml-1 text-xs text-content-muted">· periculos</span>
                        )}
                      </>
                    ) : (
                      <Badge variant="warning">{t.fix}</Badge>
                    )}
                  </TD>
                  <TD className="text-right">
                    {m.quantity == null ? (
                      <Badge variant="warning">{strings.movements.awaitingWeighing}</Badge>
                    ) : (
                      `${kg(m.quantity)} ${m.unit === "TONS" ? "t" : "kg"}`
                    )}
                  </TD>
                  <TD>{m.partnerName ?? "—"}</TD>
                  <TD>
                    {m.operationCode ?? (
                      (m.operation === "RECOVERED" || m.operation === "DISPOSED") ? (
                        <Badge variant="danger">{t.fix}</Badge>
                      ) : (
                        "—"
                      )
                    )}
                  </TD>
                  {/* Bifa de pe mişcare decide dacă rândul hrăneşte declaraţia. Se arată aici,
                      fiindcă tabul e locul unde omul verifică ce va fi depus. */}
                  <TD className="whitespace-nowrap">
                    {m.packagingOnMarket === false ? (
                      <Tooltip content={t.inAnexa1NoHint}>
                        <Badge variant="muted">{t.inAnexa1No}</Badge>
                      </Tooltip>
                    ) : m.packagingOnMarket == null ? (
                      <Tooltip content={t.inAnexa1LegacyHint}>
                        <Badge variant="warning">{t.inAnexa1Legacy}</Badge>
                      </Tooltip>
                    ) : (
                      <span className="text-xs text-content-muted">{t.inAnexa1Yes}</span>
                    )}
                  </TD>
                  {/* Mişcările pe marfă preluată apar în registru fiindcă sunt ambalaj, dar nu
                      hrănesc niciun tabel: Anexa 1 e despre deşeul propriu. Se spune pe rând, ca
                      să nu pară că lipsesc din calcul dintr-o eroare. */}
                  <TD className="whitespace-nowrap">
                    {m.register === "ART_48" ? (
                      <Tooltip content={t.originTakeoverInTab}>
                        <Badge variant="muted">{t.originTakeoverShort}</Badge>
                      </Tooltip>
                    ) : (
                      <span className="text-xs text-content-muted">{t.originOwnShort}</span>
                    )}
                  </TD>
                  <TD>{m.workPointName}</TD>
                  {/* Rândul amber spunea ce lipsește și se oprea acolo. Acțiunea apare doar unde
                      chiar e ceva de completat pe mișcare — un rând care nu hrănește declaraţia
                      (marfă preluată, ambalaj pus pe piaţă de furnizor) n-are ce repara. */}
                  <TD sticky="right" className="text-right">
                    {needsFixing(m) && (
                      <LinkButton
                        variant="ghost"
                        size="sm"
                        to={`/miscari?luna=${m.date.slice(0, 7)}&miscare=${m.id}`}
                      >
                        <Pencil className="mr-1 h-3.5 w-3.5" />
                        {t.fixOnMovement}
                      </LinkButton>
                    )}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
          <TablePagination view={registerView} />
        </div>
      </section>

      {/* ---- Tabelul 1, însumat din registrul de mai sus ---- */}
      <section id="tabelul-1" className="mt-10 scroll-mt-20">
        <h2 className="text-lg font-semibold">{t.table1Title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.table1Hint}</p>
        <div className="mt-3">
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <TH>{t.material}</TH>
                <TH className="text-right">{t.colSales}</TH>
                <TH className="text-right">{t.colTotal}</TH>
                <TH className="text-right">{t.colPrimary}</TH>
                <TH className="text-right">{t.colPrimaryReusable}</TH>
                <TH className="text-right">{t.colSecondary}</TH>
                <TH className="text-right">{t.colSecondaryReusable}</TH>
                <TH className="text-right">{t.colHazardous}</TH>
              </TR>
            </THead>
            <TBody>
              {loadingTable1 && <TableSkeletonRows columns={8} rows={8} />}
              {!loadingTable1 &&
                MATERIAL_ORDER.map((material) => {
                  const row = rowFor(material);
                  return (
                    <Fragment key={material}>
                      <TR>
                        <TD className="whitespace-nowrap font-medium">
                          {materialLabels[material]}
                          {row?.overridden && (
                            <Badge className="ml-2" variant="default">
                              {t.overriddenBadge}
                            </Badge>
                          )}
                        </TD>
                        <TD className="text-right">{kg(row?.salesPackaging)}</TD>
                        <TD className="text-right text-content-muted">{kg(packagedGoodsTotal(row))}</TD>
                        <TD className="text-right">{kg(row?.primaryTotal)}</TD>
                        <TD className="text-right">{kg(row?.primaryReusable)}</TD>
                        <TD className="text-right">{kg(row?.secondaryTotal)}</TD>
                        <TD className="text-right">{kg(row?.secondaryReusable)}</TD>
                        <TD className="text-right">{kg(row?.hazardousContent)}</TD>
                      </TR>
                      {material === "ALTE_PLASTICE" && sumRow(rows, t.totalPlastic, PLASTIC_PARTS)}
                      {material === "OTEL" && sumRow(rows, t.totalMetal, METAL_PARTS)}
                    </Fragment>
                  );
                })}
              {!loadingTable1 && sumRow(rows, t.total, MATERIAL_ORDER)}
            </TBody>
          </Table>
        </div>

        {/* Suprascrierea: pliată, fiindcă e excepţia, nu regula. */}
        {canWrite && (
          <div className="mt-4">
            <button
              type="button"
              className="text-sm font-medium text-emerald-700 hover:underline"
              onClick={() => setOverridesOpen((open) => !open)}
            >
              {overridesOpen ? t.overrideClose : t.overrideOpen}
            </button>
            {overridesOpen && (
              <div className="mt-3 rounded-lg border border-line p-4">
                <p className="max-w-3xl text-sm text-content-muted">{t.table1Override}</p>
                <p className="mt-1 text-xs text-content-subtle">{t.overrideClear}</p>
                <p className="mt-1 text-xs text-content-subtle">{t.overrideAutosaveHint}</p>
                <div className="mt-3">
                  <Table stickyHeader>
                    <THead sticky>
                      <TR>
                        <TH>{t.material}</TH>
                        <TH className="text-right">{t.colSales}</TH>
                        <TH className="text-right">{t.colPrimary}</TH>
                        <TH className="text-right">{t.colPrimaryReusable}</TH>
                        <TH className="text-right">{t.colSecondary}</TH>
                        <TH className="text-right">{t.colSecondaryReusable}</TH>
                        <TH className="text-right">{t.colHazardous}</TH>
                        <TH>{t.overrideStatus}</TH>
                      </TR>
                    </THead>
                    <TBody>
                      {(overrides ?? []).map((row) => (
                        <TR key={row.material}>
                          <TD className="whitespace-nowrap font-medium">
                            {materialLabels[row.material]}
                          </TD>
                          {COLUMNS.map((column) => (
                            <TD key={column} className="text-right">
                              <Input
                                type="number"
                                step="0.001"
                                min="0"
                                aria-label={`${materialLabels[row.material]} — ${COLUMN_LABELS[column]}`}
                                className="w-28 text-right"
                                value={cellValue(row, column)}
                                onChange={(ev) => edit(row.material, column, ev.target.value)}
                                onBlur={() => saveOverride(row)}
                              />
                            </TD>
                          ))}
                          {/* Ce s-a întâmplat cu rândul, în cuvinte. `aria-live` fiindcă starea se
                              schimbă singură, fără ca nimeni să apese ceva: altfel un cititor de
                              ecran n-ar afla niciodată că cifra a plecat. */}
                          <TD className="whitespace-nowrap text-xs" aria-live="polite">
                            <RowStatus state={rowState[row.material]} dirty={Boolean(draft[row.material])} />
                          </TD>
                        </TR>
                      ))}
                    </TBody>
                  </Table>
                </div>
                {/* Cifra de care întreba felia: „câte au ajuns". Coloana o spune pe rând, dar un
                    rând nesalvat poate fi derulat afară din ochi — aici se vede oricum. */}
                {dirtyRows > 0 && (
                  <p className="mt-2 text-xs text-amber-700">
                    {dirtyRows === 1 ? t.overrideUnsavedRow : fill(t.overrideUnsavedRows, dirtyRows)}
                  </p>
                )}
              </div>
            )}
          </div>
        )}
      </section>

      {/* ---- Tabelul 2, calculat din predări ---- */}
      <section id="tabelul-2" className="mt-10 scroll-mt-20">
        <h2 className="text-lg font-semibold">{t.table2Title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.table2Hint}</p>
        <div className="mt-3">
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <TH>{t.material}</TH>
                <TH className="text-right">{t.quantity}</TH>
                <TH>{t.operator}</TH>
                <TH>{t.operatorCui}</TH>
                <TH>{t.operation}</TH>
              </TR>
            </THead>
            <TBody>
              {(handovers ?? []).length === 0 && (
                <TableFallbackRow columns={5} loading={false} icon={Package} title={t.noHandovers} />
              )}
              {(handovers ?? []).map((row, i) => (
                <TR key={`${row.material}-${row.operatorCui}-${row.operation}-${i}`}>
                  <TD className="whitespace-nowrap">{materialLabels[row.material]}</TD>
                  <TD className="text-right">{kg(row.quantity)}</TD>
                  <TD>
                    {row.operatorName}
                    {row.operatorAddress ? (
                      <span className="block text-xs text-content-muted">{row.operatorAddress}</span>
                    ) : null}
                  </TD>
                  <TD>{row.operatorCui ?? "—"}</TD>
                  <TD>{row.operation || "—"}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </div>
      </section>

      <Anexa3Section year={year} />
    </div>
  );
}

/**
 * Anexa 3 la Ordinul 794/2012 — celălalt capăt al lanţului faţă de Anexa 1: ce a preluat firma de
 * la terţi şi ce a făcut cu marfa.
 *
 * <p>Se arată **un singur tabel**, cel care i se aplică firmei (art. 4 alin. (1): „tabelul 1 sau,
 * după caz, tabelul 2"), iar când profilul n-a răspuns nu se arată niciunul şi ecranul spune ce e
 * de completat. Un ecran e o ofertă, un document e o afirmaţie — vezi decizia 37.
 */
function Anexa3Section({ year }: { year: number }) {
  const { data: workPoints } = useWorkPoints();
  /**
   * Numai punctele **active**. Restul ecranelor filtrează așa de mult (Mișcări, Evidențe); aici
   * lista le arăta pe toate, deci se putea alege un punct de lucru scos din uz — și, cu
   * auto-selecția de mai jos, se putea chiar nimeri singură pe el.
   */
  const activeWorkPoints = useMemo(
    () => (workPoints ?? []).filter((w) => w.active),
    [workPoints]
  );
  // În adresă, ca filtrul de an de deasupra: altfel un link către raportul unui punct de lucru
  // anume nu putea exista, iar alegerea se pierdea la fiecare navigare.
  const [workPointId, setWorkPointId] = useUrlState("punctA3");

  // Cu un singur punct de lucru, alegerea nu e o alegere: se selectează singur, ca butonul de
  // descărcare să fie activ din prima. Cu mai multe, rămâne pe „Toate" până alege omul.
  useEffect(() => {
    if (!workPointId && activeWorkPoints.length === 1) {
      setWorkPointId(activeWorkPoints[0].id);
    }
  }, [activeWorkPoints, workPointId, setWorkPointId]);
  const { data, isLoading } = usePackagingAnexa3(year, workPointId || undefined);
  const { notify } = useToast();
  const [downloading, setDownloading] = useState<"xls" | "pdf" | null>(null);

  async function download(format: "xls" | "pdf") {
    setDownloading(format);
    try {
      await downloadPackagingAnexa3(year, workPointId || undefined, format);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.anexa3DownloadError), "error");
    } finally {
      setDownloading(null);
    }
  }

  // „Toate punctele de lucru" e util pe ecran şi nedepunibil pe hârtie: art. 4 alin. (4) cere
  // raportarea per punct de lucru, iar alin. (3) o trimite la agenţia din raza lui. Un fişier cu
  // rubrica „Punct de lucru" goală ar fi un formular pe care clientul nu-l poate folosi.
  const canDownload = (data?.printable ?? false) && workPointId !== "";
  const table2 = data?.usesTable2 ?? false;
  const missingOrigin = (data?.unclassified ?? []).filter((r) => r.missingOrigin).length;
  const missingMaterial = (data?.unclassified ?? []).filter((r) => r.missingMaterial).length;
  const missingQuantity = (data?.unclassified ?? []).filter((r) => r.missingQuantity).length;

  return (
    <section id="anexa-3" className="mt-10 scroll-mt-20">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="max-w-3xl">
          <h2 className="text-lg font-semibold text-content">{t.anexa3Title}</h2>
          <p className="mt-1 text-sm text-content-muted">{t.anexa3Hint}</p>
        </div>
        <div className="flex items-end gap-2">
          <div>
            <Label htmlFor="a3-wp">{t.anexa3WorkPoint}</Label>
            <Select id="a3-wp" value={workPointId} onChange={(e) => setWorkPointId(e.target.value)}>
              <option value="">{t.anexa3AllWorkPoints}</option>
              {activeWorkPoints.map((wp) => (
                <option key={wp.id} value={wp.id}>
                  {wp.name}
                </option>
              ))}
            </Select>
          </div>
          {/* `loading`, ca butoanele de sus: starea `downloading` exista deja, dar nu o citea
              nimeni, deci un `.xls` care se construiește câteva secunde arăta ca un buton mort. */}
          <Button
            variant="outline"
            disabled={!canDownload || downloading !== null}
            loading={downloading === "xls"}
            onClick={() => download("xls")}
          >
            {downloading !== "xls" && <FileSpreadsheet className="mr-2 h-4 w-4" />}
            {t.anexa3Download}
          </Button>
          <Button
            variant="outline"
            disabled={!canDownload || downloading !== null}
            loading={downloading === "pdf"}
            onClick={() => download("pdf")}
          >
            {downloading !== "pdf" && <FileText className="mr-2 h-4 w-4" />}
            PDF
          </Button>
        </div>
      </div>
      <p className="mt-1 text-xs text-content-muted">{t.anexa3WorkPointHint}</p>
      {data?.printable && workPointId === "" && (
        <p className="mt-1 text-xs text-amber-700">{t.anexa3PickWorkPoint}</p>
      )}

      {/* Scheletul ține forma a ce urmează — o casetă de avertisment sau un tabel mic — ca
          secțiunea să nu sară când vin datele. */}
      {isLoading && (
        <div className="mt-4 space-y-2">
          <Skeleton className="h-4 w-48" />
          <Skeleton className="h-24 w-full rounded-lg" />
        </div>
      )}

      {/* Profilul n-a spus care tabel se aplică: nu tipărim nimic şi spunem de ce. */}
      {data && !data.printable && (
        <div className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-4">
          <div className="flex items-start gap-2">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
            <div>
              <p className="text-sm font-medium text-amber-900">{t.anexa3RoleMissing}</p>
              <p className="mt-1 text-sm text-amber-800">{t.anexa3RoleMissingHint}</p>
              <p className="mt-2 text-xs text-amber-700">{t.anexa3RoleMissingAction}</p>
            </div>
          </div>
        </div>
      )}

      {data?.printable && (
        <>
          <p className="mt-3 text-sm text-content-strong">
            <span className="font-medium">
              {table2 ? t.anexa3Table2Title : t.anexa3Table1Title}
            </span>
            {" · "}
            {t.anexa3Addressee}: {addresseeOf(data)}
          </p>

          {(missingOrigin > 0 || missingMaterial > 0 || missingQuantity > 0) && (
            <div className="mt-3 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
              <p className="font-medium">{t.anexa3UnclassifiedTitle}</p>
              {missingOrigin > 0 && (
                <p className="mt-1">{t.anexa3MissingOrigin.replace("{n}", String(missingOrigin))}</p>
              )}
              {missingMaterial > 0 && (
                <p className="mt-1">
                  {t.anexa3MissingMaterialCount.replace("{n}", String(missingMaterial))}
                </p>
              )}
              {missingQuantity > 0 && (
                <p className="mt-1">
                  {t.anexa3MissingQuantity.replace("{n}", String(missingQuantity))}
                </p>
              )}
            </div>
          )}

          <div className="mt-3">
            <h3 className="text-sm font-semibold text-content-strong">{t.anexa3IntakeTitle}</h3>
            <p className="mb-2 text-xs text-content-muted">{t.anexa3IntakeHint}</p>
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <TH>{t.material}</TH>
                  <TH className="text-right">{t.anexa3ColTotal}</TH>
                  <TH className="text-right">{t.anexa3ColHazardous}</TH>
                  <TH>{t.anexa3ColOrigin}</TH>
                </TR>
              </THead>
              <TBody>
                {data.intake.length === 0 && (
                  <TR>
                    <TD colSpan={4} className="text-content-muted">
                      {t.anexa3Empty}
                    </TD>
                  </TR>
                )}
                {data.intake.map((row, i) => (
                  <TR key={`${row.material}-${row.origin}-${i}`}>
                    <TD className="whitespace-nowrap">{materialLabels[row.material]}</TD>
                    <TD className="text-right">{kg(row.total)}</TD>
                    <TD className="text-right">{row.hazardous ? kg(row.hazardous) : "—"}</TD>
                    <TD>{strings.packagingOrigin[row.origin]}</TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </div>

          <div className="mt-6">
            <h3 className="text-sm font-semibold text-content-strong">{t.anexa3OutTitle}</h3>
            {table2 && <p className="mb-2 text-xs text-content-muted">{t.anexa3RecyclingHint}</p>}
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <TH>{t.material}</TH>
                  {table2 ? (
                    <>
                      <TH className="text-right">{t.anexa3ColRecycled}</TH>
                      <TH className="text-right">{t.anexa3ColOtherRecovery}</TH>
                      <TH>{t.anexa3ColMethods}</TH>
                    </>
                  ) : (
                    <>
                      <TH className="text-right">{t.anexa3ColOut}</TH>
                      <TH>{t.anexa3ColOperator}</TH>
                    </>
                  )}
                </TR>
              </THead>
              <TBody>
                {table2
                  ? data.treatments.map((row, i) => (
                      <TR key={`${row.material}-${i}`}>
                        <TD className="whitespace-nowrap">{materialLabels[row.material]}</TD>
                        <TD className="text-right">{kg(row.recycled)}</TD>
                        <TD className="text-right">{kg(row.otherRecovery)}</TD>
                        <TD>{row.methods.join(", ") || "—"}</TD>
                      </TR>
                    ))
                  : data.handovers.map((row, i) => (
                      <TR key={`${row.material}-${row.operatorCui}-${i}`}>
                        <TD className="whitespace-nowrap">{materialLabels[row.material]}</TD>
                        <TD className="text-right">{kg(row.quantity)}</TD>
                        <TD>
                          {row.operatorName ?? "—"}
                          {row.operatorCui ? (
                            <span className="block text-xs text-content-muted">{row.operatorCui}</span>
                          ) : null}
                        </TD>
                      </TR>
                    ))}
                {(table2 ? data.treatments : data.handovers).length === 0 && (
                  <TR>
                    <TD colSpan={table2 ? 4 : 3} className="text-content-muted">
                      {t.noHandovers}
                    </TD>
                  </TR>
                )}
              </TBody>
            </Table>
          </div>

          <p className="mt-3 text-xs text-content-muted">{t.anexa3DownloadHint}</p>
        </>
      )}
    </section>
  );
}

/** Art. 4 alin. (3): toţi depun la agenţia din raza punctului de lucru, comerciantul la ANPM. */
function addresseeOf(d: PackagingAnexa3): string {
  return d.role === "COMERCIANT"
    ? "ANPM"
    : "agenţia judeţeană pentru protecţia mediului din raza punctului de lucru";
}

/** Un rând de sumă din formular — Total plastic, Total metal, TOTAL. */
function sumRow(rows: PackagingTable1Row[], label: string, parts: PackagingMaterial[]) {
  const columns: ((row: PackagingTable1Row) => number | null)[] = [
    (r) => r.salesPackaging,
    (r) => packagedGoodsTotal(r),
    (r) => r.primaryTotal,
    (r) => r.primaryReusable,
    (r) => r.secondaryTotal,
    (r) => r.secondaryReusable,
    (r) => r.hazardousContent,
  ];
  return (
    <TR className="bg-surface-muted">
      <TD className="font-semibold">{label}</TD>
      {columns.map((pick, i) => (
        <TD key={i} className="text-right font-semibold">
          {kg(sumOver(rows, parts, pick))}
        </TD>
      ))}
    </TR>
  );
}
