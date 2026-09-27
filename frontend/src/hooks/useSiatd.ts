import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { weighingKey } from "@/hooks/useWeighingOperations";
import type { SiatdReceptionRow, SiatdState, SiatdSummary } from "@/lib/types";

/**
 * F6a — termenele SIATD ale recepțiilor. Lista și banda le citește oricine din firmă (tăiate pe depozitele lui);
 * confirmarea e a celor care aprobă. Orice confirmare reîmprospătează tabul, banda și operațiunile (rândul din dialog).
 */
export const siatdKey = ["siatd"] as const;

export function useSiatdReceptions(state: SiatdState, enabled = true) {
  return useQuery({
    enabled,
    queryKey: [...siatdKey, "receptions", state],
    queryFn: async () =>
      (await api.get<SiatdReceptionRow[]>("/api/v1/depot-siatd/receptions", { params: { state } })).data,
  });
}

export function useSiatdSummary(enabled = true) {
  return useQuery({
    enabled,
    queryKey: [...siatdKey, "summary"],
    queryFn: async () => (await api.get<SiatdSummary>("/api/v1/depot-siatd/summary")).data,
  });
}

function useInvalidateSiatd() {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: siatdKey });
    qc.invalidateQueries({ queryKey: weighingKey });
  };
}

export function useConfirmSiatd() {
  const invalidate = useInvalidateSiatd();
  return useMutation({
    mutationFn: async (input: { operationIds: string[]; code?: string }) =>
      (await api.post<SiatdReceptionRow[]>("/api/v1/depot-siatd/confirmations", input)).data,
    onSuccess: invalidate,
  });
}

export function useUnconfirmSiatd() {
  const invalidate = useInvalidateSiatd();
  return useMutation({
    mutationFn: async (operationId: string) => {
      await api.delete(`/api/v1/depot-siatd/confirmations/${operationId}`);
    },
    onSuccess: invalidate,
  });
}
