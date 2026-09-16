import { useMemo } from "react";
import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { api } from "@/lib/api";
import { declarationOf } from "@/lib/deadlines";
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
 * Declarația anului `year` (termenul de 15 martie al anului următor, bifat), sau `undefined`.
 * Citită la nevoie, pe gestul de salvare sau ștergere. Dacă termenele nu se pot citi, nu oprim
 * salvarea: avertismentul e un ajutor, nu o poartă.
 */
export async function fetchDeclaration(
  queryClient: QueryClient,
  year: number,
): Promise<Deadline | undefined> {
  try {
    const list = await queryClient.fetchQuery({
      queryKey: deadlinesKey(year + 1),
      queryFn: async () =>
        (await api.get<Deadline[]>("/api/v1/deadlines", { params: { year: year + 1 } })).data,
    });
    return declarationOf(list, year);
  } catch {
    return undefined;
  }
}

/** Aceeași întrebare, ca hook, pentru un dialog care o arată pe ecran. */
export function useDeclaration(year: number) {
  const q = useDeadlines(year + 1);
  return q.data ? declarationOf(q.data, year) : undefined;
}

/**
 * Termenele anului în curs **și** ale celui următor, într-o listă. De pe 16.09.2026 calendarul ține
 * doar următorul termen al fiecărui fel, iar acela e des în anul următor (15 martie pentru anul de
 * acum): Panoul care citea doar anul curent ar fi spus „niciun termen” unui cont nou din septembrie.
 * Și anul trecut: un termen ratat rămâne depășit până se bifează, deci unul din 25 decembrie trebuie
 * să se vadă și în ianuarie. Serverul întoarce din anul trecut doar ce e bifat sau ratat cu adevărat.
 */
export function useUpcomingDeadlines(enabled = true) {
  const year = new Date().getFullYear();
  const [previous, current, next] = useQueries({
    queries: [year - 1, year, year + 1].map((y) => ({
      enabled,
      queryKey: deadlinesKey(y),
      queryFn: async () =>
        (await api.get<Deadline[]>("/api/v1/deadlines", { params: { year: y } })).data,
    })),
  });
  const data = useMemo(
    () =>
      previous.data && current.data && next.data
        ? [...previous.data, ...current.data, ...next.data]
        : undefined,
    [previous.data, current.data, next.data]
  );
  return {
    data,
    isLoading: previous.isLoading || current.isLoading || next.isLoading,
    isError: previous.isError || current.isError || next.isError,
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
