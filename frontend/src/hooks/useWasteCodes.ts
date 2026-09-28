import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { WasteCode } from "@/lib/types";

/**
 * Searchable waste-code lookup for the movement form's combobox. The query is
 * expected to be already debounced by the caller (the Combobox debounces).
 * `on` is the movement date (ISO): the list has editions — from 9.11.2026 the
 * battery codes of Decision (EU) 2025/934 — so it follows the list in force that
 * day. Without it the server uses today.
 */
export function useWasteCodeSearch(query: string, on?: string) {
  return useQuery({
    queryKey: ["waste-codes", query, on ?? null] as const,
    queryFn: async () => {
      const params: Record<string, string> = {};
      if (query) params.q = query;
      if (on) params.on = on;
      return (await api.get<WasteCode[]>("/api/v1/waste-codes", { params })).data;
    },
    staleTime: 60_000,
  });
}
