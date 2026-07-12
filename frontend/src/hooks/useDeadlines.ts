import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Deadline, DeadlineGenerationResponse } from "@/lib/types";

/**
 * TanStack Query hooks for reporting deadlines (FAZA TERMENE). The list is scoped by year;
 * generation and completion invalidate the whole family so the table reflects the new state.
 */
const deadlinesRoot = ["deadlines"] as const;
export const deadlinesKey = (year: number) => [...deadlinesRoot, year] as const;

export function useDeadlines(year: number) {
  return useQuery({
    queryKey: deadlinesKey(year),
    queryFn: async () =>
      (await api.get<Deadline[]>("/api/v1/deadlines", { params: { year } })).data,
  });
}

export function useRegenerateDeadlines() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (year: number) =>
      (
        await api.post<DeadlineGenerationResponse>("/api/v1/deadlines/regenerate", null, {
          params: { year },
        })
      ).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: deadlinesRoot }),
  });
}

export function useCompleteDeadline() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, note }: { id: string; note?: string }) =>
      (await api.post<Deadline>(`/api/v1/deadlines/${id}/complete`, { note })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: deadlinesRoot }),
  });
}

export function useReopenDeadline() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) =>
      (await api.post<Deadline>(`/api/v1/deadlines/${id}/reopen`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: deadlinesRoot }),
  });
}
