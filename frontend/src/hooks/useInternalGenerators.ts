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

export function useCreateInternalGenerator() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: InternalGeneratorInput) =>
      (await api.post<InternalGenerator>("/api/v1/internal-generators", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: internalGeneratorsKey }),
  });
}

export function useUpdateInternalGenerator() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: InternalGeneratorInput }) =>
      (await api.put<InternalGenerator>(`/api/v1/internal-generators/${id}`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: internalGeneratorsKey }),
  });
}

export function useDeactivateInternalGenerator() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/internal-generators/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: internalGeneratorsKey }),
  });
}
