import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  DeclarationAnswer,
  Inventory,
  InventoryDuringOperation,
  InventoryHeaderInput,
  InventoryLine,
  StockOpening,
  StockOpeningInput,
} from "@/lib/types";

/** D3.5 — notele de preluare ale unui depozit (cel mult una confirmată). */
export function useStockOpenings(workPointId: string) {
  return useQuery({
    enabled: Boolean(workPointId),
    queryKey: ["stock-openings", workPointId],
    queryFn: async () =>
      (await api.get<StockOpening[]>("/api/v1/stock-openings", { params: { workPointId } })).data,
  });
}

/** D3.5 — inventarele unui depozit, cele mai noi întâi. */
export function useInventories(workPointId: string) {
  return useQuery({
    enabled: Boolean(workPointId),
    queryKey: ["inventories", workPointId],
    queryFn: async () => (await api.get<Inventory[]>("/api/v1/inventories", { params: { workPointId } })).data,
  });
}

export function useInventory(id: string | null) {
  return useQuery({
    enabled: Boolean(id),
    queryKey: ["inventories", "one", id],
    queryFn: async () => (await api.get<Inventory>(`/api/v1/inventories/${id}`)).data,
  });
}

export function useInventoryOperationsDuring(id: string | null) {
  return useQuery({
    enabled: Boolean(id),
    queryKey: ["inventories", "during", id],
    queryFn: async () =>
      (await api.get<InventoryDuringOperation[]>(`/api/v1/inventories/${id}/operations-during`)).data,
  });
}

/** Nota și inventarul schimbă stocul: după orice scriere se recitesc și ele, și „Stoc”. */
function useDepotMutation<V, R>(fn: (vars: V) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["inventories"] });
      qc.invalidateQueries({ queryKey: ["stock-openings"] });
      qc.invalidateQueries({ queryKey: ["stock"] });
    },
  });
}

export function useSaveStockOpening() {
  return useDepotMutation(async ({ id, input }: { id: string | null; input: StockOpeningInput }) =>
    id
      ? (await api.put<StockOpening>(`/api/v1/stock-openings/${id}`, input)).data
      : (await api.post<StockOpening>("/api/v1/stock-openings", input)).data
  );
}

export function useConfirmStockOpening() {
  return useDepotMutation(async (id: string) => (await api.post<StockOpening>(`/api/v1/stock-openings/${id}/confirm`)).data);
}

export function useDeleteStockOpening() {
  return useDepotMutation(async (id: string) => {
    await api.delete(`/api/v1/stock-openings/${id}`);
  });
}

export function useOpenInventory() {
  return useDepotMutation(async (input: InventoryHeaderInput) => (await api.post<Inventory>("/api/v1/inventories", input)).data);
}

export function useInventoryAction() {
  return useDepotMutation(
    async ({ id, action, body }: { id: string; action: string; body?: unknown }) => {
      const url = `/api/v1/inventories/${id}/${action}`;
      const put = ["header", "declaration", "lines", "pv"].includes(action);
      return (put ? await api.put<Inventory>(url, body) : await api.post<Inventory>(url, body)).data;
    }
  );
}

export type DeclarationInput = {
  answers: DeclarationAnswer[];
  lastEntryDoc: string | null;
  lastExitDoc: string | null;
  declarationDate: string | null;
};

export type LinesInput = {
  lines: Pick<
    InventoryLine,
    | "id"
    | "articleId"
    | "wasteCodeId"
    | "countedKg"
    | "countMethod"
    | "technicalData"
    | "explanation"
    | "shortageNature"
    | "responsiblePerson"
    | "slowMoving"
  >[];
};

export async function fetchInventoryPdf(id: string, document: string): Promise<Blob> {
  return (await api.get<Blob>(`/api/v1/inventories/${id}/pdf/${document}`, { responseType: "blob" })).data;
}

export async function fetchStockOpeningPdf(id: string): Promise<Blob> {
  return (await api.get<Blob>(`/api/v1/stock-openings/${id}/pdf`, { responseType: "blob" })).data;
}
