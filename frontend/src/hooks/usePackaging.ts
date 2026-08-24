import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  PackagingHandoverRow,
  PackagingMarketRow,
  PackagingMarketInput,
} from "@/lib/types";

/**
 * The packaging module — Anexa 1 Ambalaje (Ordinul 794/2012).
 *
 * <p>Two queries because the declaration has two halves with different owners: tabelul 1 is
 * answered by the client and stored, tabelul 2 is computed from the movements. Saving a market row
 * invalidates both, since the printed form draws on the two together.
 */
const packagingRoot = ["packaging"] as const;

export function usePackagingMarket(year: number) {
  return useQuery({
    queryKey: [...packagingRoot, "market", year] as const,
    queryFn: async () =>
      (await api.get<PackagingMarketRow[]>("/api/v1/packaging/market", { params: { year } })).data,
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

export function useSavePackagingMarket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: PackagingMarketInput) =>
      (await api.put<PackagingMarketRow>("/api/v1/packaging/market", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: packagingRoot }),
  });
}

/** Streams the PDF and hands it to the browser, like the other official documents. */
export async function downloadPackagingDeclaration(year: number) {
  const res = await api.get("/api/v1/packaging/anexa1", {
    params: { year },
    responseType: "blob",
  });
  const url = URL.createObjectURL(res.data as Blob);
  try {
    const a = document.createElement("a");
    a.href = url;
    a.download = `anexa1-ambalaje-${year}.pdf`;
    a.click();
  } finally {
    URL.revokeObjectURL(url);
  }
}
