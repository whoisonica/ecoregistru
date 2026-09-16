import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import { openPdfInTab } from "@/lib/openFileInTab";
import type {
  DepotRetentionReport,
  WeighingLinesInput,
  WeighingOperation,
  WeighingOperationInput,
  WeighingOperationType,
} from "@/lib/types";

/**
 * Operațiunile de cântar ale depozitului (D1.4–D1.10). Cheia poartă filtrele, ca luna schimbată să
 * nu citească din cache-ul altei luni; orice scriere le invalidează pe toate.
 */
export const weighingKey = ["weighing-operations"] as const;

export interface WeighingFilters {
  type?: WeighingOperationType;
  year?: number;
  month?: number;
}

export function useWeighingOperations(filters: WeighingFilters, enabled = true) {
  return useQuery({
    enabled,
    queryKey: [...weighingKey, filters.type ?? null, filters.year ?? null, filters.month ?? null],
    queryFn: async () =>
      (
        await api.get<WeighingOperation[]>("/api/v1/weighing-operations", {
          params: { type: filters.type, year: filters.year, month: filters.month },
        })
      ).data,
  });
}

/**
 * O operațiune anume, pentru linkul venit de pe „Intrări”/„Ieșiri” (`/cantar?op=…`): rândul de acolo
 * poate fi din altă lună decât cea aleasă pe ecran, deci nu se găsește în listă.
 */
export function useWeighingOperation(id: string | null) {
  return useQuery({
    enabled: Boolean(id),
    queryKey: [...weighingKey, "one", id],
    queryFn: async () => (await api.get<WeighingOperation>(`/api/v1/weighing-operations/${id}`)).data,
  });
}

export const retentionsKey = ["depot-retentions"] as const;

function useInvalidate() {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: weighingKey });
    // Reținerile se nasc la finalizare și mor la anulare: banda de sus se recitește odată cu lista.
    qc.invalidateQueries({ queryKey: retentionsKey });
  };
}

export function useCreateWeighingOperation() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async (input: WeighingOperationInput) =>
      (await api.post<WeighingOperation>("/api/v1/weighing-operations", input)).data,
    onSuccess: invalidate,
  });
}

export function useUpdateWeighingOperation() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: WeighingOperationInput }) =>
      (await api.put<WeighingOperation>(`/api/v1/weighing-operations/${id}`, input)).data,
    onSuccess: invalidate,
  });
}

/** Tot cântarul odată: serverul înlocuiește liniile salvate cu cele trimise. */
export function useSaveWeighingLines() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: WeighingLinesInput }) =>
      (await api.put<WeighingOperation>(`/api/v1/weighing-operations/${id}/lines`, input)).data,
    onSuccess: invalidate,
  });
}

export function useFinalizeWeighingOperation() {
  const invalidate = useInvalidate();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) =>
      (await api.post<WeighingOperation>(`/api/v1/weighing-operations/${id}/finalize`)).data,
    onSuccess: () => {
      invalidate();
      // Din clipa asta liniile contează în stoc, în registre și în totalurile de pe Intrări/Ieșiri.
      qc.invalidateQueries({ queryKey: ["movements"] });
      qc.invalidateQueries({ queryKey: ["evidences"] });
    },
  });
}

export function useCancelWeighingOperation() {
  const invalidate = useInvalidate();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, reason }: { id: string; reason: string }) =>
      (await api.post<WeighingOperation>(`/api/v1/weighing-operations/${id}/cancel`, { reason })).data,
    onSuccess: () => {
      invalidate();
      qc.invalidateQueries({ queryKey: ["movements"] });
      qc.invalidateQueries({ queryKey: ["evidences"] });
    },
  });
}

/**
 * Reținerile la sursă dintr-o lună, sau dintr-un an fără lună (D1.9, D1.10). Serverul le dă doar
 * celui care administrează firma și vede prețurile; pentru ceilalți cererea cade cu 403, iar ecranul
 * nu arată banda.
 */
export function useDepotRetentions(year: number, month: number | null, enabled = true) {
  return useQuery({
    enabled,
    retry: false,
    queryKey: [...retentionsKey, year, month],
    queryFn: async () =>
      (
        await api.get<DepotRetentionReport>("/api/v1/weighing-operations/retentions", {
          params: { year, month: month ?? undefined },
        })
      ).data,
  });
}

/**
 * D1.14 — registrul intrărilor și ieșirilor pe o lună: amândouă direcțiile, toate stările, o linie pe
 * sortiment. `.xlsx`, fiindcă e documentul de lucru al depozitului, nu un formular de depus.
 */
export async function downloadDepotRegister(year: number, month: number): Promise<void> {
  const data = (
    await api.get("/api/v1/weighing-operations/registru", {
      params: { year, month },
      responseType: "blob",
    })
  ).data as Blob;
  saveBlob(data, `registru-intrari-iesiri-${year}-${String(month).padStart(2, "0")}.xlsx`);
}

/**
 * D1.13 — Anexa 3 sau avizul pe tot transportul unei ieșiri, într-un tab. Prima Anexa 3 alocă numărul
 * formularului, deci o cere doar cine scrie; retipărirea dă același număr.
 */
export async function openWeighingDocument(
  operation: WeighingOperation,
  document: "anexa3" | "aviz"
): Promise<void> {
  await openPdfInTab(
    async () =>
      (
        await api.get(`/api/v1/weighing-operations/${operation.id}/${document}`, {
          responseType: "blob",
        })
      ).data as Blob,
    `${document}-iesire-${operation.number}-${operation.date}.pdf`
  );
}
