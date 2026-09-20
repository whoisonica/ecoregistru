import { Fragment, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { FileSpreadsheet, FileText, Package } from "lucide-react";
import { useCanWrite } from "@/hooks/useBillingAccess";
import {
  downloadPackagingAnexa3,
  downloadPackagingDeclaration,
  usePackagingAnexa3,
  usePackagingHandovers,
  usePackagingMarket,
  usePackagingMovements,
  usePackagingTable1,
  usePackagingUnclassified,
  useSavePackagingMarket,
} from "@/hooks/usePackaging";
import type {
  PackagingMarketRow,
  PackagingMaterial,
  PackagingTable1Row,
} from "@/lib/types";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useTableView } from "@/hooks/useTableView";
import { useUrlState } from "@/hooks/useUrlState";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { withCount } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Menu, MenuItem, MenuLabel } from "@/components/ui/menu";
import { PillGroup } from "@/components/ui/pill-group";
import { Input } from "@/components/ui/input";
import { LoadError } from "@/components/ui/load-error";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow, TableSkeletonRows } from "@/components/ui/table-fallback";
import { TablePagination } from "@/components/ui/table-toolbar";
import { useToast } from "@/components/ui/toast";
import { Anexa3Section } from "@/components/packaging/Anexa3Section";
import { countMovements, kg, materialLabels } from "@/components/packaging/packagingFormat";

const t = strings.packaging;

/** Ce tabel e pe ecran. „Pus pe piață" e cel implicit, deci nu se scrie în adresă. */
type TableKey = "" | "predat" | "preluat";

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

/**
 * Semnalele de pe ecranul de ambalaje numără toate acelaşi lucru — mişcări —, deci substantivul
 * stă aici o dată, nu la fiecare apel. Restul numărătorilor din pagină trec prin `withCount`.
 */

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
 * **Ambalajele unui an** — Anexa 1 Ambalaje (Ordinul 794/2012), Anexa 3 şi tot ce se însumează din
 * mişcările pe coduri {@code 15 01 xx}.
 *
 * <p>Totul pleacă dintr-o singură sursă: mişcările. Până pe 18.09.2026, ecranul şi le arăta şi pe
 * ele, într-un registru propriu — al treilea tabel de mişcări din aplicaţie, cu aceleaşi rânduri
 * ca „Generare" şi fără căutare sau sortare la server. Registrul a rămas unul singur, iar
 * întrebările lui („numai ambalajele", „numai ce-am pus noi pe piaţă", „ce e de completat") au
 * devenit tastele de deasupra listei de mişcări. Ce e aici nu se mai găseşte nicăieri altundeva:
 * tabelele, suprascrierea şi cele două documente.
 *
 * <p>**Totul încape într-un ecran** (proprietarul, 18.09.2026: „să nu meargă pagina în jos, să tot
 * dai scroll"). Pe ecran stă un singur tabel, ales din taste — „Pus pe piață" (tabelul 1) sau
 * „Predat" (tabelul 2, paginat la zece) —, cele două documente sunt două butoane lipite, fără text
 * dedesubt, iar semnalele sunt un rând cu linkuri. Tabelul Anexei 3 a plecat de la generator: acolo
 * erau aceleaşi predări ca în „Predat", iar documentul era ascuns la fundul paginii. La firma care
 * şi colectează rămâne a treia tastă, „Preluat de la alții", fiindcă preluările pe provenienţă şi
 * avertismentul de rol nu se văd nicăieri altundeva.
 *
 * <p>Suprascrierea celor şaizeci şi şase de celule ale tabelului 1 nu mai e o a doua grilă sub
 * tabel: „Scrie cifre proprii" face câmpuri chiar din tabel. Rămâne excepţia, nu regula — tabelul
 * e legal despre marfa pusă pe piaţă şi o firmă poate şti că cifra ei diferă de ce arată deşeul.
 *
 * @param year anul raportat; vine din filtrul de deasupra, nu din starea componentei
 * @param movementsPath ecranul pe care se repară mişcările — „Generare" la generator, „Ieşiri" la
 *        colectorul pur. Semnalele de sus trimit acolo, cu tasta de ambalaje deja apăsată.
 */
export function PackagingReport({
  year,
  movementsPath,
}: {
  year: number;
  movementsPath: string;
}) {
  const canWrite = useCanWrite();
  // Anexa 3 se descarcă la toți: la colector cu preluări și ieșiri, la generator numai cu ieșirile
  // (proprietarul, 16.09.2026). Tabelul ei pe ecran rămâne doar unde spune ceva ce „Predat" nu
  // spune — la firma care și colectează. Cât timp firma nu s-a încărcat, tasta nu apare.
  const { data: company } = useCurrentCompany();
  const collects = company != null && company.type !== "GENERATOR";
  const { data: workPoints } = useWorkPoints();
  const activeWorkPoints = useMemo(() => (workPoints ?? []).filter((w) => w.active), [workPoints]);
  // Fără punct de lucru: doar ca să aflăm dacă profilul a spus care tabel se aplică (`printable`).
  const anexa3Q = usePackagingAnexa3(year);
  const anexa3 = anexa3Q.data;
  const [tableParam, setTable] = useUrlState("tabel");
  const table: TableKey =
    tableParam === "predat" || (tableParam === "preluat" && collects) ? tableParam : "";
  const movementsQ = usePackagingMovements(year);
  const movements = movementsQ.data;
  const table1Q = usePackagingTable1(year);
  const { data: table1, isLoading: loadingTable1 } = table1Q;
  const handoversQ = usePackagingHandovers(year);
  const handovers = handoversQ.data;
  const unclassifiedQ = usePackagingUnclassified(year);
  const unclassified = unclassifiedQ.data;
  const overridesQ = usePackagingMarket(year);
  const overrides = overridesQ.data;
  const saveMut = useSavePackagingMarket();
  const { notify } = useToast();
  const [downloading, setDownloading] = useState(false);
  const [editing, setEditing] = useState(false);

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

  // Zece predări pe pagină, ca pe „Totalul anului": ce trece de un ecran se paginează.
  const handoverView = useTableView(handovers ?? [], { pageSize: 10 });

  const rows = table1 ?? [];
  /** Câte rânduri au cifre tastate şi neplecate. Zero e starea normală, deci nu se scrie nimic. */
  const dirtyRows = Object.keys(draft).length;

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

  async function download(run: () => Promise<void>, errorText: string) {
    setDownloading(true);
    try {
      await run();
    } catch (err) {
      notify(await apiBlobErrorMessage(err, errorText), "error");
    } finally {
      setDownloading(false);
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

  /** Un material are rând pe ecran dacă are măcar o cifră; la scris se văd toate, ca să ai unde tasta. */
  const hasFigures = (material: PackagingMaterial) => {
    const row = rowFor(material);
    return row != null && COLUMNS.some((column) => row[column] != null);
  };
  const shown = MATERIAL_ORDER.filter((material) => editing || hasFigures(material));
  const hidden = MATERIAL_ORDER.filter((material) => !shown.includes(material));
  const overrideFor = (material: PackagingMaterial) =>
    (overrides ?? []).find((r) => r.material === material);

  const blocked: { text: string; to: string }[] = [
    {
      text: countMovements(t.blockedMissingMaterial, signals.missingMaterial),
      to: `${movementsPath}?luna=${year}&ambalaje=de-completat`,
      n: signals.missingMaterial,
    },
    {
      text: countMovements(t.blockedMissingCategory, signals.missingCategory),
      to: `${movementsPath}?luna=${year}&ambalaje=de-completat`,
      n: signals.missingCategory,
    },
    {
      text: countMovements(t.missingOperation, signals.missingOperation),
      to: `${movementsPath}?luna=${year}&problema=cod-rd`,
      n: signals.missingOperation,
    },
    {
      // „Toate ambalajele", nu „pus de noi pe piață": semnalul e despre kilograme care lipsesc,
      // iar tasta „pe piață" nici nu există pe ecranul colectorului.
      text: countMovements(t.awaitingWeighing, signals.awaitingWeighing),
      to: `${movementsPath}?luna=${year}&ambalaje=toate`,
      n: signals.awaitingWeighing,
    },
  ].filter((signal) => signal.n > 0);

  /**
   * Un ecran gol nu e un răspuns.
   *
   * <p>Cele şase cereri ale tabului n-aveau nicio ramură de eroare: la un 500 sau la rețea căzută,
   * Tabelul 1 apărea gol — adică „n-ai pus nimic pe piaţă în 2026" —, „Predat" la fel, iar banda de
   * semnale se socoteşte din liste goale, deci ieşea zero: ecranul spunea tăcut că **nimic nu
   * opreşte depunerea**, chiar înainte de termen. Pe un tab de conformitate, asta e mai rău decât o
   * eroare. Una singură pentru tot tabul, fiindcă toate şase descriu acelaşi an: şase casete roşii
   * n-ar spune mai mult decât una.
   */
  const failed = [anexa3Q, movementsQ, table1Q, handoversQ, unclassifiedQ, overridesQ].filter(
    (q) => q.isError
  );
  if (failed.length > 0) {
    return (
      <LoadError
        className="mt-3"
        message={t.loadError}
        onRetry={() => failed.forEach((q) => void q.refetch())}
      />
    );
  }

  return (
    <div>
      {/* ---- Documentele și tastele tabelului, pe un rând ----
           Butoanele poartă numele documentelor și n-au text dedesubt; termenul și formatul stau în
           meniu. Anexa 3 se descarcă pe punct de lucru (art. 4 alin. (4), decizia 49), deci
           punctul se alege chiar în meniul ei — nu într-un filtru, la mijlocul paginii. */}
      {/* `relative`: eticheta `sr-only` de mai jos e poziţionată absolut, iar fără un strămoş
          `relative` lungeşte documentul — capcana prinsă pe Acasă în aceeaşi zi. */}
      <div className="relative mt-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          <Menu label={t.docAnexa1} align="left" disabled={downloading}>
            <MenuLabel>{t.docAnexa1Deadline}</MenuLabel>
            <MenuItem
              icon={FileSpreadsheet}
              hint={t.downloadXlsHint}
              onClick={() => download(() => downloadPackagingDeclaration(year, "xls"), t.downloadError)}
            >
              {t.downloadXls}
            </MenuItem>
            <MenuItem
              icon={FileText}
              hint={t.downloadPdfHint}
              onClick={() => download(() => downloadPackagingDeclaration(year, "pdf"), t.downloadError)}
            >
              {t.downloadPdf}
            </MenuItem>
          </Menu>
          {/* Profilul n-a spus care tabel se aplică → nimic de tipărit; de ce, scrie pe tasta
              „Preluat de la alții". La generator `printable` e mereu adevărat. */}
          <Menu label={t.docAnexa3} align="left" disabled={downloading || !(anexa3?.printable ?? false)}>
            {activeWorkPoints.length === 0 && <MenuLabel>{t.anexa3NoWorkPoint}</MenuLabel>}
            {activeWorkPoints.map((wp) => (
              <Fragment key={wp.id}>
                <MenuLabel>{wp.name}</MenuLabel>
                <MenuItem
                  icon={FileSpreadsheet}
                  onClick={() => download(() => downloadPackagingAnexa3(year, wp.id, "xls"), t.anexa3DownloadError)}
                >
                  {t.downloadXls}
                </MenuItem>
                <MenuItem
                  icon={FileText}
                  onClick={() => download(() => downloadPackagingAnexa3(year, wp.id, "pdf"), t.anexa3DownloadError)}
                >
                  {t.downloadPdf}
                </MenuItem>
              </Fragment>
            ))}
          </Menu>
        </div>
        <span id="pk-table-picker" className="sr-only">
          {t.tablePicker}
        </span>
        <PillGroup<TableKey>
          name="tabel-ambalaje"
          aria-labelledby="pk-table-picker"
          options={[
            { value: "", label: t.keyMarket },
            { value: "predat", label: t.keyHandedOver },
            ...(collects ? [{ value: "preluat" as const, label: t.keyIntake }] : []),
          ]}
          selected={[table]}
          onToggle={(value) => {
            setEditing(false);
            setTable(value);
          }}
        />
      </div>

      {/* ---- Ce blochează declaraţia: un rând, fiecare semnal e linkul spre rândurile lui ----
           Până pe 18.09.2026 era o cutie cu titlu şi listă. Linkul duce în „Mişcări" cu tasta
           potrivită deja apăsată — altfel semnalul ar numi vinovatul şi s-ar opri acolo. */}
      {(blocked.length > 0 || (table === "" && canWrite)) && table !== "preluat" && (
        <div className="mt-4 flex min-h-8 flex-wrap items-center justify-between gap-x-4 gap-y-2">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
            {blocked.length > 0 && <Badge variant="warning">{t.blockedLead}</Badge>}
            {blocked.map((signal) => (
              <Link
                key={signal.text}
                to={signal.to}
                className="font-medium text-state-warn-text underline hover:no-underline"
              >
                {signal.text}
              </Link>
            ))}
          </div>
          {table === "" && canWrite && (
            <Button variant="muted" size="sm" onClick={() => setEditing((on) => !on)}>
              {editing ? t.overrideDone : t.overrideOpen}
            </Button>
          )}
        </div>
      )}

      {/* ---- Tabelul 1, însumat din mişcări. La scris, celulele devin câmpuri pe loc. ---- */}
      {table === "" && (
        <section id="tabelul-1" className="mt-3">
          <Table>
            <THead>
              <TR>
                <TH rowSpan={2}>{t.material}</TH>
                <TH rowSpan={2} className="text-right">{t.headSales}</TH>
                <TH rowSpan={2} className="text-right">{t.headTotal}</TH>
                <TH colSpan={2} className="border-b border-line-strong text-center">{t.headPrimary}</TH>
                <TH colSpan={2} className="border-b border-line-strong text-center">{t.headSecondary}</TH>
                <TH rowSpan={2} className="text-right">{t.headHazardous}</TH>
              </TR>
              <TR>
                <TH className="text-right">{t.headGroupTotal}</TH>
                <TH className="text-right">{t.headGroupReusable}</TH>
                <TH className="text-right">{t.headGroupTotal}</TH>
                <TH className="text-right">{t.headGroupReusable}</TH>
              </TR>
            </THead>
            <TBody>
              {loadingTable1 && <TableSkeletonRows columns={8} rows={5} />}
              {!loadingTable1 && shown.length === 0 && (
                <TableFallbackRow
                  columns={8}
                  loading={false}
                  icon={Package}
                  title={t.table1Empty.replace("{year}", String(year))}
                />
              )}
              {!loadingTable1 &&
                shown.map((material) => {
                  const row = rowFor(material);
                  const override = editing ? overrideFor(material) : undefined;
                  const cell = (column: Column) =>
                    override ? (
                      <Input
                        type="number"
                        step="0.001"
                        min="0"
                        aria-label={`${materialLabels[material]} — ${COLUMN_LABELS[column]}`}
                        // Cifra din mişcări rămâne la vedere cât scrii, dar nu e o valoare: un
                        // câmp gol nu suprascrie nimic.
                        placeholder={row?.overridden || row?.[column] == null ? "" : kg(row[column])}
                        className="ml-auto h-8 w-24 text-right"
                        value={cellValue(override, column)}
                        onChange={(ev) => edit(material, column, ev.target.value)}
                        onBlur={() => saveOverride(override)}
                      />
                    ) : (
                      kg(row?.[column])
                    );
                  // La scris, rândul cu câmpuri se strânge: opt rânduri de 53px împingeau pagina cu
                  // 58px peste un ecran de 900 (măsurat, 18.09.2026).
                  const num = editing ? "py-1.5 text-right" : "text-right";
                  return (
                    <Fragment key={material}>
                      <TR>
                        <TD className="whitespace-nowrap font-medium">
                          {materialLabels[material]}
                          {row?.overridden && !editing && (
                            <Badge className="ml-2" variant="default">
                              {t.overriddenBadge}
                            </Badge>
                          )}
                          {/* `aria-live` fiindcă starea se schimbă singură, fără ca nimeni să apese
                              ceva: altfel un cititor de ecran n-ar afla niciodată că cifra a plecat. */}
                          {editing && (
                            <span className="ml-2 text-xs" aria-live="polite">
                              <RowStatus state={rowState[material]} dirty={Boolean(draft[material])} />
                            </span>
                          )}
                        </TD>
                        <TD className={num}>{cell("salesPackaging")}</TD>
                        <TD className="text-right text-content-muted">{kg(packagedGoodsTotal(row))}</TD>
                        <TD className={num}>{cell("primaryTotal")}</TD>
                        <TD className={num}>{cell("primaryReusable")}</TD>
                        <TD className={num}>{cell("secondaryTotal")}</TD>
                        <TD className={num}>{cell("secondaryReusable")}</TD>
                        <TD className={num}>{cell("hazardousContent")}</TD>
                      </TR>
                      {/* Suma apare după ultimul ei material care se vede, nu după unul ascuns. */}
                      {material === lastShown(shown, PLASTIC_PARTS) && sumRow(rows, t.totalPlastic, PLASTIC_PARTS)}
                      {material === lastShown(shown, METAL_PARTS) && sumRow(rows, t.totalMetal, METAL_PARTS)}
                    </Fragment>
                  );
                })}
              {!loadingTable1 && shown.length > 0 && sumRow(rows, t.total, MATERIAL_ORDER)}
            </TBody>
          </Table>
          <p className="mt-2 text-xs text-content-muted">
            {editing ? t.overrideClear : t.table1Foot}
            {!editing && !loadingTable1 && shown.length > 0 && hidden.length > 0 && (
              <>
                {" "}
                {t.table1NoQuantities
                  .replace("{year}", String(year))
                  .replace("{materials}", hidden.map((material) => materialLabels[material]).join(", "))}
              </>
            )}
          </p>
          {/* Un rând nesalvat poate fi al unui material pe care nu te uiți — aici se vede oricum. */}
          {editing && dirtyRows > 0 && (
            <p className="mt-1 text-xs text-state-warn-text">
              {withCount(t.overrideUnsavedRows, dirtyRows, "rând", "rânduri")}
            </p>
          )}
        </section>
      )}

      {/* ---- Tabelul 2, calculat din predări; zece pe pagină ---- */}
      {table === "predat" && (
        <section id="tabelul-2" className="mt-3">
          <Table>
            <THead>
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
              {handoverView.visible.map((row, i) => (
                <TR key={`${row.material}-${row.operatorCui}-${row.operation}-${i}`}>
                  <TD className="whitespace-nowrap">{materialLabels[row.material]}</TD>
                  <TD className="text-right">{kg(row.quantity)}</TD>
                  <TD>{row.operatorName}</TD>
                  <TD>{row.operatorCui ?? "—"}</TD>
                  <TD>{row.operation || "—"}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
          <p className="mt-2 text-xs text-content-muted">{t.table2Foot}</p>
          <TablePagination view={handoverView} />
        </section>
      )}

      {table === "preluat" && <Anexa3Section year={year} />}
    </div>
  );
}

/** Ultimul material dintr-un grup care chiar are rând pe ecran; `undefined` când n-are niciunul. */
function lastShown(shown: PackagingMaterial[], parts: PackagingMaterial[]) {
  return shown.filter((material) => parts.includes(material)).pop();
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
