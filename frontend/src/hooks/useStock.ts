import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { StockReport } from "@/lib/types";

/** F3 — stocul unui depozit (sau al firmei, fără depozit) la o dată; calculat de server din linii. */
export function useStock(workPointId: string | null, date: string) {
  return useQuery({
    queryKey: ["stock", workPointId ?? "all", date],
    queryFn: async () =>
      (await api.get<StockReport>("/api/v1/stock", { params: { workPointId: workPointId ?? undefined, date } })).data,
  });
}
