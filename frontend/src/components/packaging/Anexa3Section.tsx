/*
 * Scos din `pages/PackagingPage.tsx` pe 18.09.2026, neschimbat: pagina trecuse de 1.000 de linii și
 * ținea două documente diferite în același fișier. Secțiunea nu împrumuta nimic din starea paginii —
 * primește doar anul —, deci mutarea e o tăiere, nu o refacere.
 */
import { useEffect, useMemo } from "react";
import { AlertTriangle } from "lucide-react";
import { usePackagingAnexa3 } from "@/hooks/usePackaging";
import type { PackagingAnexa3 } from "@/lib/types";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { strings } from "@/lib/strings";
import { withCount } from "@/lib/utils";
import { useUrlState } from "@/hooks/useUrlState";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { countMovements, kg, materialLabels } from "./packagingFormat";

const t = strings.packaging;

/**
 * Anexa 3 la Ordinul 794/2012 — celălalt capăt al lanţului faţă de Anexa 1: ce a preluat firma de
 * la terţi şi ce a făcut cu marfa.
 *
 * <p>Se arată **un singur tabel**, cel care i se aplică firmei (art. 4 alin. (1): „tabelul 1 sau,
 * după caz, tabelul 2"), iar când profilul n-a răspuns nu se arată niciunul şi ecranul spune ce e
 * de completat. Un ecran e o ofertă, un document e o afirmaţie — vezi decizia 37.
 *
 * <p>Din 18.09.2026 e tabelul de sub tasta „Preluat de la alții", numai la firma care şi
 * colectează; descărcarea a urcat în butonul „Anexa 3 Ambalaje" de deasupra, cu punctul de lucru
 * în meniul lui. Selectorul de aici alege doar ce se vede pe ecran.
 */
export function Anexa3Section({ year }: { year: number }) {
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

  // Cu un singur punct de lucru, alegerea nu e o alegere: se selectează singur, ca ecranul să arate
  // din prima ce se depune. Cu mai multe, rămâne pe „Toate" până alege omul.
  useEffect(() => {
    if (!workPointId && activeWorkPoints.length === 1) {
      setWorkPointId(activeWorkPoints[0].id);
    }
  }, [activeWorkPoints, workPointId, setWorkPointId]);
  const { data, isLoading } = usePackagingAnexa3(year, workPointId || undefined);
  const table2 = data?.usesTable2 ?? false;
  const exitsOnly = data?.exitsOnly ?? false;
  const missingOrigin = (data?.unclassified ?? []).filter((r) => r.missingOrigin).length;
  const missingMaterial = (data?.unclassified ?? []).filter((r) => r.missingMaterial).length;
  const missingQuantity = (data?.unclassified ?? []).filter((r) => r.missingQuantity).length;

  return (
    <section id="anexa-3" className="mt-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="max-w-3xl">
          <h2 className="text-lg font-semibold text-content">
            {exitsOnly ? t.anexa3ExitsTitle : t.anexa3Title}
          </h2>
          <p className="mt-1 text-sm text-content-muted">{exitsOnly ? t.anexa3ExitsHint : t.anexa3Hint}</p>
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
        </div>
      </div>
      <p className="mt-1 text-xs text-content-muted">{t.anexa3WorkPointHint}</p>

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
            {!exitsOnly && (
              <>
                <span className="font-medium">
                  {table2 ? t.anexa3Table2Title : t.anexa3Table1Title}
                </span>
                {" · "}
              </>
            )}
            {t.anexa3Addressee}: {addresseeOf(data)}
          </p>

          {(missingOrigin > 0 || missingMaterial > 0 || missingQuantity > 0) && (
            <div className="mt-3 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
              <p className="font-medium">{t.anexa3UnclassifiedTitle}</p>
              {missingOrigin > 0 && (
                <p className="mt-1">
                  {withCount(t.anexa3MissingOrigin, missingOrigin, "preluare", "preluări")}
                </p>
              )}
              {missingMaterial > 0 && (
                <p className="mt-1">
                  {withCount(t.anexa3MissingMaterialCount, missingMaterial, "preluare", "preluări")}
                </p>
              )}
              {missingQuantity > 0 && (
                <p className="mt-1">
                  {countMovements(t.anexa3MissingQuantity, missingQuantity)}
                </p>
              )}
            </div>
          )}

          {!exitsOnly && (
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
          )}

          <div className={exitsOnly ? "mt-3" : "mt-6"}>
            <h3 className="text-sm font-semibold text-content-strong">
              {exitsOnly ? t.anexa3ExitsTableTitle : t.anexa3OutTitle}
            </h3>
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
        </>
      )}
    </section>
  );
}

/**
 * Art. 4 alin. (3): toţi depun la agenţia din raza punctului de lucru, comerciantul la agenţia
 * naţională — care din 11.07.2026 se numeşte ANMAP, nu ANPM. Motivul pentru care scriem numele nou
 * deşi Ordinul 794/2012 îl scrie pe cel vechi e în `strings.ts`, lângă şir.
 */
function addresseeOf(d: PackagingAnexa3): string {
  return d.role === "COMERCIANT" ? t.anexa3AddresseeAnmap : t.anexa3AddresseeLocal;
}
