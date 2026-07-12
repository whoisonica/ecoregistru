import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { WorkPoint, WorkPointInput } from "@/lib/types";

/**
 * TanStack Query hooks for work points — the per-resource pattern the other
 * resources (partners, movements) will follow: one query key, a list query,
 * and mutations that invalidate that key on success.
 */
export const workPointsKey = ["work-points"] as const;

export function useWorkPoints() {
  return useQuery({
    queryKey: workPointsKey,
    queryFn: async () => (await api.get<WorkPoint[]>("/api/v1/work-points")).data,
  });
}

export function useCreateWorkPoint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: WorkPointInput) =>
      (await api.post<WorkPoint>("/api/v1/work-points", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: workPointsKey }),
  });
}

export function useUpdateWorkPoint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: WorkPointInput }) =>
      (await api.put<WorkPoint>(`/api/v1/work-points/${id}`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: workPointsKey }),
  });
}

export function useDeactivateWorkPoint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/work-points/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: workPointsKey }),
  });
}
