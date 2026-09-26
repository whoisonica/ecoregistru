import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { AuthorizedLimitInput, StockReport, StockThreshold } from "@/lib/types";

/** F3 — stocul unui depozit (sau al firmei, fără depozit) la o dată; calculat de server din linii. */
export function useStock(workPointId: string | null, date: string) {
  return useQuery({
    queryKey: ["stock", workPointId ?? "all", date],
    queryFn: async () =>
      (await api.get<StockReport>("/api/v1/stock", { params: { workPointId: workPointId ?? undefined, date } })).data,
  });
}

/** D3.3 — pragurile unui depozit. */
export function useStockThresholds(workPointId: string, enabled = true) {
  return useQuery({
    enabled: enabled && Boolean(workPointId),
    queryKey: ["stock", "thresholds", workPointId],
    queryFn: async () =>
      (await api.get<StockThreshold[]>("/api/v1/stock/thresholds", { params: { workPointId } })).data,
  });
}

/** Pragurile și limitele schimbă stările de pe „Stoc”: se recitește tot ce ține de stoc. */
function useStockMutation<V>(fn: (vars: V) => Promise<unknown>) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: fn, onSuccess: () => qc.invalidateQueries({ queryKey: ["stock"] }) });
}

export function useSaveStockThreshold() {
  return useStockMutation(async (input: { workPointId: string; articleId: string; minKg: number | null; maxKg: number | null }) =>
    (await api.put<StockThreshold>("/api/v1/stock/thresholds", input)).data
  );
}

export function useDeleteStockThreshold() {
  return useStockMutation(async (id: string) => {
    await api.delete(`/api/v1/stock/thresholds/${id}`);
  });
}

export function useAddAuthorizedLimit() {
  return useStockMutation(async (input: AuthorizedLimitInput) => (await api.post("/api/v1/stock/limits", input)).data);
}

export function useDeleteAuthorizedLimit() {
  return useStockMutation(async (id: string) => {
    await api.delete(`/api/v1/stock/limits/${id}`);
  });
}
