import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { InternalGenerator, InternalGeneratorInput } from "@/lib/types";

/**
 * Internal generators (generatori interni) — the section inside a work point that produced the
 * waste, printed as "Secţia" in Anexa 1 cap. 2. Same per-resource pattern as the other lists;
 * deactivate is one-way, like work points and partners.
 */
export const internalGeneratorsKey = ["internal-generators"] as const;

export function useInternalGenerators(workPointId?: string) {
  return useQuery({
    queryKey: [...internalGeneratorsKey, workPointId ?? "all"],
    queryFn: async () =>
      (
        await api.get<InternalGenerator[]>("/api/v1/internal-generators", {
          params: workPointId ? { workPointId } : undefined,
        })
      ).data,
  });
}

/**
 * Every list of sections, wherever it is mounted, after one of them changes.
 *
 * `refetchType: "all"` is the point: the default only refetches queries that are mounted right
 * now, and marks the rest stale. The section list in the movement dialog is exactly one of "the
 * rest" — it lives on another route, so it used to come back with the cached list and show the
 * new section only on the second open (proba de acceptanță, 24.08.2026: prima mișcare s-a salvat
 * fără secție). Awaiting it also keeps the Settings dialog on screen until the data is real.
 */
function refreshInternalGenerators(qc: ReturnType<typeof useQueryClient>) {
  return qc.invalidateQueries({ queryKey: internalGeneratorsKey, refetchType: "all" });
}

export function useCreateInternalGenerator() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: InternalGeneratorInput) =>
      (await api.post<InternalGenerator>("/api/v1/internal-generators", input)).data,
    onSuccess: () => refreshInternalGenerators(qc),
  });
}

export function useUpdateInternalGenerator() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: InternalGeneratorInput }) =>
      (await api.put<InternalGenerator>(`/api/v1/internal-generators/${id}`, input)).data,
    onSuccess: () => refreshInternalGenerators(qc),
  });
}

export function useDeactivateInternalGenerator() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/internal-generators/${id}`);
    },
    onSuccess: () => refreshInternalGenerators(qc),
  });
}
