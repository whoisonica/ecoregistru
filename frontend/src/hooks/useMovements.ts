import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Attachment, MovementFilters, WasteMovement, WasteMovementInput } from "@/lib/types";

/**
 * TanStack Query hooks for waste movements — same per-resource pattern as
 * useWorkPoints. The list key embeds the active filters so each filter combo
 * caches independently; every mutation invalidates the whole family.
 */
const movementsRoot = ["movements"] as const;
export const movementsKey = (filters: MovementFilters) => [...movementsRoot, filters] as const;

export function useMovements(filters: MovementFilters) {
  return useQuery({
    queryKey: movementsKey(filters),
    queryFn: async () => {
      const params: Record<string, string | number> = {};
      if (filters.year != null) params.year = filters.year;
      if (filters.month != null) params.month = filters.month;
      if (filters.workPointId) params.workPointId = filters.workPointId;
      if (filters.wasteCodeId) params.wasteCodeId = filters.wasteCodeId;
      return (await api.get<WasteMovement[]>("/api/v1/movements", { params })).data;
    },
  });
}

function invalidateAll(qc: ReturnType<typeof useQueryClient>) {
  return qc.invalidateQueries({ queryKey: movementsRoot });
}

export function useCreateMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: WasteMovementInput) =>
      (await api.post<WasteMovement>("/api/v1/movements", input)).data,
    onSuccess: () => invalidateAll(qc),
  });
}

export function useUpdateMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: WasteMovementInput }) =>
      (await api.put<WasteMovement>(`/api/v1/movements/${id}`, input)).data,
    onSuccess: () => invalidateAll(qc),
  });
}

export function useDeleteMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/movements/${id}`);
    },
    onSuccess: () => invalidateAll(qc),
  });
}

export function useAddAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ movementId, file }: { movementId: string; file: File }) => {
      const form = new FormData();
      form.append("file", file);
      return (
        await api.post<Attachment>(`/api/v1/movements/${movementId}/attachments`, form)
      ).data;
    },
    onSuccess: () => invalidateAll(qc),
  });
}

export function useDeleteAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ movementId, attachmentId }: { movementId: string; attachmentId: string }) => {
      await api.delete(`/api/v1/movements/${movementId}/attachments/${attachmentId}`);
    },
    onSuccess: () => invalidateAll(qc),
  });
}
