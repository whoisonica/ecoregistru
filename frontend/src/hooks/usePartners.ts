import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { driversKey } from "@/hooks/useDrivers";
import type { Partner, PartnerInput } from "@/lib/types";

/**
 * Partners — the per-resource TanStack Query pattern (one key, list query,
 * mutations that invalidate the key on success). Deactivate is one-way:
 * the backend has no reactivate endpoint, so the UI hides the action on
 * inactive rows rather than exposing a toggle.
 *
 * Salvarea unui partener îi rescrie și șoferii, deci invalidează și `driversKey`: altfel
 * formularul de mișcare ar oferi lista de dinaintea salvării.
 */
export const partnersKey = ["partners"] as const;

export function usePartners() {
  return useQuery({
    queryKey: partnersKey,
    queryFn: async () => (await api.get<Partner[]>("/api/v1/partners")).data,
  });
}

export function useCreatePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: PartnerInput) =>
      (await api.post<Partner>("/api/v1/partners", input)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: partnersKey });
      qc.invalidateQueries({ queryKey: driversKey });
    },
  });
}

export function useUpdatePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: PartnerInput }) =>
      (await api.put<Partner>(`/api/v1/partners/${id}`, input)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: partnersKey });
      qc.invalidateQueries({ queryKey: driversKey });
    },
  });
}

export function useDeactivatePartner() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/partners/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: partnersKey }),
  });
}
