import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { EvidenceFilters, EvidenceRegenerationResponse, MonthlyEvidence } from "@/lib/types";

/**
 * TanStack Query hooks for monthly evidence — same per-resource pattern as
 * useMovements. Evidence is a regenerable cache: the list reflects the last
 * regeneration, so useRegenerateEvidence invalidates the whole family on success.
 */
const evidencesRoot = ["evidences"] as const;
export const evidencesKey = (filters: EvidenceFilters) => [...evidencesRoot, filters] as const;

export function useEvidences(filters: EvidenceFilters) {
  return useQuery({
    queryKey: evidencesKey(filters),
    queryFn: async () => {
      const params: Record<string, string | number> = { year: filters.year };
      if (filters.month != null) params.month = filters.month;
      if (filters.workPointId) params.workPointId = filters.workPointId;
      return (await api.get<MonthlyEvidence[]>("/api/v1/evidences", { params })).data;
    },
  });
}

export function useRegenerateEvidence() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (year: number) =>
      (
        await api.post<EvidenceRegenerationResponse>("/api/v1/evidences/regenerate", null, {
          params: { year },
        })
      ).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: evidencesRoot }),
  });
}
