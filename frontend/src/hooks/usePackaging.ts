import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import type {
  PackagingAnexa3,
  PackagingHandoverRow,
  PackagingMarketRow,
  PackagingMarketInput,
  PackagingTable1Row,
  PackagingUnclassifiedRow,
  WasteMovement,
} from "@/lib/types";

/**
 * Modulul Ambalaje — Anexa 1 Ambalaje (Ordinul 794/2012).
 *
 * <p>Totul pleacă din același loc: mișcările pe coduri `15 01 xx`. Registrul le arată așa cum sunt,
 * iar cele două tabele ale declarației se însumează din ele. Cifra scrisă de mână există în
 * continuare, dar numai ca **suprascriere** pe un material — de aceea salvarea ei invalidează tot
 * grupul, nu doar grila.
 */
const packagingRoot = ["packaging"] as const;

/** Registrul: mișcările de ambalaje ale anului, cele mai noi întâi. */
export function usePackagingMovements(year: number) {
  return useQuery({
    queryKey: [...packagingRoot, "movements", year] as const,
    queryFn: async () =>
      (await api.get<WasteMovement[]>("/api/v1/packaging/movements", { params: { year } })).data,
  });
}

/** Tabelul 1 așa cum îl va tipări documentul. */
export function usePackagingTable1(year: number) {
  return useQuery({
    queryKey: [...packagingRoot, "table1", year] as const,
    queryFn: async () =>
      (await api.get<PackagingTable1Row[]>("/api/v1/packaging/table1", { params: { year } })).data,
  });
}

export function usePackagingHandovers(year: number) {
  return useQuery({
    queryKey: [...packagingRoot, "handovers", year] as const,
    queryFn: async () =>
      (await api.get<PackagingHandoverRow[]>("/api/v1/packaging/handovers", { params: { year } }))
        .data,
  });
}

/** Ce n-a intrat în tabele și de ce — semnalul care blochează depunerea. */
export function usePackagingUnclassified(year: number) {
  return useQuery({
    queryKey: [...packagingRoot, "unclassified", year] as const,
    queryFn: async () =>
      (
        await api.get<PackagingUnclassifiedRow[]>("/api/v1/packaging/unclassified", {
          params: { year },
        })
      ).data,
  });
}

/** Doar suprascrierile stocate: rânduri goale acolo unde firma n-a scris nimic. */
export function usePackagingMarket(year: number) {
  return useQuery({
    queryKey: [...packagingRoot, "market", year] as const,
    queryFn: async () =>
      (await api.get<PackagingMarketRow[]>("/api/v1/packaging/market", { params: { year } })).data,
  });
}

export function useSavePackagingMarket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: PackagingMarketInput) =>
      (await api.put<PackagingMarketRow>("/api/v1/packaging/market", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: packagingRoot }),
  });
}

/**
 * Descarcă documentul. Implicit `.xls` — art. 6 din ordin numește formatul pe litere, „în format
 * electronic «.xls»", iar până pe 02.09.2026 trimiteam de fapt un `.xlsx`. PDF-ul rămâne
 * exemplarul pe hârtie cerut de același articol, și dosarul de control.
 */
export async function downloadPackagingDeclaration(year: number, format: "xls" | "pdf" = "xls") {
  const res = await api.get("/api/v1/packaging/anexa1", {
    params: { year, format },
    responseType: "blob",
  });
  saveBlob(res.data as Blob, `anexa1-ambalaje-${year}.${format}`);
}

/**
 * Anexa 3 la Ordinul 794/2012 — raportul anual al colectorilor, comercianților, reciclatorilor și
 * valorificatorilor de deșeuri de ambalaje.
 *
 * <p>Per punct de lucru, fiindcă art. 4 alin. (4) o cere „pentru fiecare punct de lucru în parte"
 * și alin. (3) o trimite la agenția din raza lui. `workPointId` gol construiește toată firma, ceea
 * ce e același lucru pentru un cont cu un singur punct de lucru și o privire de ansamblu utilă
 * pentru unul cu mai multe.
 */
export function usePackagingAnexa3(year: number, workPointId?: string) {
  return useQuery({
    queryKey: [...packagingRoot, "anexa3", year, workPointId ?? null] as const,
    queryFn: async () =>
      (
        await api.get<PackagingAnexa3>("/api/v1/packaging/anexa3", {
          params: workPointId ? { year, workPointId } : { year },
        })
      ).data,
  });
}

/**
 * Descarcă Anexa 3. Aceleași două formate ca la Anexa 1 și din același art. 6: `.xls` protejat
 * pentru depunere, PDF pentru exemplarul pe hârtie. Se tipărește **un singur tabel**, cel care i se
 * aplică firmei — art. 4 alin. (1) cere „tabelul 1 sau, după caz, tabelul 2", nu amândouă.
 */
export async function downloadPackagingAnexa3(
  year: number,
  workPointId?: string,
  format: "xls" | "pdf" = "xls"
) {
  const res = await api.get("/api/v1/packaging/anexa3/download", {
    params: workPointId ? { year, workPointId, format } : { year, format },
    responseType: "blob",
  });
  saveBlob(res.data as Blob, `anexa3-ambalaje-${year}.${format}`);
}
