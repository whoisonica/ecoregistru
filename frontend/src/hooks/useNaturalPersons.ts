import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { NaturalPerson, NaturalPersonInput, NaturalPersonSummary } from "@/lib/types";

/**
 * Persoanele fizice ale depozitului (D1.7b). Lista are CNP-ul mascat; fișa întreagă se cere doar când
 * se deschide formularul, și numai de cine poate scrie (serverul dă 403 vizualizatorului).
 */
export const naturalPersonsKey = ["natural-persons"] as const;

export function useNaturalPersons() {
  return useQuery({
    queryKey: naturalPersonsKey,
    queryFn: async () => (await api.get<NaturalPersonSummary[]>("/api/v1/natural-persons")).data,
  });
}

export function useNaturalPerson(id: string | null) {
  return useQuery({
    queryKey: [...naturalPersonsKey, id],
    queryFn: async () => (await api.get<NaturalPerson>(`/api/v1/natural-persons/${id}`)).data,
    enabled: id !== null,
    // CNP-ul nu stă în cache după ce formularul s-a închis.
    gcTime: 0,
  });
}

function useInvalidatingMutation<T>(fn: (arg: T) => Promise<unknown>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: naturalPersonsKey }),
  });
}

export function useCreateNaturalPerson() {
  return useInvalidatingMutation(async (input: NaturalPersonInput) =>
    (await api.post<NaturalPerson>("/api/v1/natural-persons", input)).data
  );
}

export function useUpdateNaturalPerson() {
  return useInvalidatingMutation(async ({ id, input }: { id: string; input: NaturalPersonInput }) =>
    (await api.put<NaturalPerson>(`/api/v1/natural-persons/${id}`, input)).data
  );
}

export function useDeactivateNaturalPerson() {
  return useInvalidatingMutation((id: string) => api.delete(`/api/v1/natural-persons/${id}`));
}

export function useReactivateNaturalPerson() {
  return useInvalidatingMutation((id: string) => api.post(`/api/v1/natural-persons/${id}/reactivate`));
}

export function useDeleteNaturalPerson() {
  return useInvalidatingMutation((id: string) => api.delete(`/api/v1/natural-persons/${id}/definitiv`));
}
