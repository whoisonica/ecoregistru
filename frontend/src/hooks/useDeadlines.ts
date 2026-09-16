import { useMemo } from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Deadline, DeadlineGenerationResponse } from "@/lib/types";

/**
 * TanStack Query hooks for reporting deadlines (FAZA TERMENE). The list is scoped by year;
 * generation and completion invalidate the whole family so the table reflects the new state.
 */
const deadlinesRoot = ["deadlines"] as const;
export const deadlinesKey = (year: number) => [...deadlinesRoot, year] as const;

export function useDeadlines(year: number, enabled = true) {
  return useQuery({
    enabled,
    queryKey: deadlinesKey(year),
    queryFn: async () =>
      (await api.get<Deadline[]>("/api/v1/deadlines", { params: { year } })).data,
  });
}

/**
 * Termenele anului în curs **și** ale celui următor, într-o listă. De pe 16.09.2026 calendarul ține
 * doar următorul termen al fiecărui fel, iar acela e des în anul următor (15 martie pentru anul de
 * acum): Panoul care citea doar anul curent ar fi spus „niciun termen” unui cont nou din septembrie.
 */
export function useUpcomingDeadlines(enabled = true) {
  const year = new Date().getFullYear();
  const [current, next] = useQueries({
    queries: [year, year + 1].map((y) => ({
      enabled,
      queryKey: deadlinesKey(y),
      queryFn: async () =>
        (await api.get<Deadline[]>("/api/v1/deadlines", { params: { year: y } })).data,
    })),
  });
  const data = useMemo(
    () => (current.data && next.data ? [...current.data, ...next.data] : undefined),
    [current.data, next.data]
  );
  return {
    data,
    isLoading: current.isLoading || next.isLoading,
    isError: current.isError || next.isError,
  };
}

/** Completează calendarul cu următorul termen al fiecărui fel; pe server, nu pe un an ales. */
export function useRegenerateDeadlines() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      (await api.post<DeadlineGenerationResponse>("/api/v1/deadlines/regenerate")).data,
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
