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

/**
 * Downloads the evidence export (generic, unofficial table) as an xlsx/pdf file. Not a query —
 * it streams a blob and triggers a browser download, so it lives outside TanStack's cache.
 * Uses the same filters as the list so the file matches what's on screen.
 */
export async function downloadEvidenceExport(
  filters: EvidenceFilters,
  format: "xlsx" | "pdf"
): Promise<void> {
  const params: Record<string, string | number> = { year: filters.year, format };
  if (filters.month != null) params.month = filters.month;
  if (filters.workPointId) params.workPointId = filters.workPointId;

  const res = await api.get("/api/v1/evidences/export", { params, responseType: "blob" });
  const url = URL.createObjectURL(res.data as Blob);
  try {
    const a = document.createElement("a");
    a.href = url;
    a.download = `evidenta-${filters.year}.${format}`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}
