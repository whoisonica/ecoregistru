import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { WasteCode } from "@/lib/types";

/**
 * Searchable waste-code lookup for the movement form's combobox. The query is
 * expected to be already debounced by the caller (the Combobox debounces).
 * Results are kept fresh briefly and reused across identical queries.
 */
export function useWasteCodeSearch(query: string) {
  return useQuery({
    queryKey: ["waste-codes", query] as const,
    queryFn: async () =>
      (await api.get<WasteCode[]>("/api/v1/waste-codes", { params: query ? { q: query } : {} })).data,
    staleTime: 60_000,
  });
}
