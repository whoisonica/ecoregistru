import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Driver, DriverInput } from "@/lib/types";

/**
 * Șoferii firmei. GET-ul întoarce *toți* șoferii tenantului — și ai noștri, și ai
 * transportatorilor — fiindcă formularul de mișcare are nevoie de amândouă felurile într-un singur
 * apel și filtrează pe client după `partnerId`.
 *
 * Mutațiile ating doar șoferii noștri (`partnerId === null`). Ai unui transportator se editează în
 * fișa partenerului, unde lista se înlocuiește la salvare — două drumuri de scriere către aceleași
 * rânduri ar însemna că un șofer adăugat de aici dispare data viitoare când cineva deschide și
 * salvează partenerul. Backendul refuză explicit, deci regula nu ține doar de disciplina UI-ului.
 */
export const driversKey = ["drivers"] as const;

export function useDrivers() {
  return useQuery({
    queryKey: driversKey,
    queryFn: async () => (await api.get<Driver[]>("/api/v1/drivers")).data,
  });
}

export function useCreateDriver() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: DriverInput) =>
      (await api.post<Driver>("/api/v1/drivers", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: driversKey }),
  });
}

export function useUpdateDriver() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: DriverInput }) =>
      (await api.put<Driver>(`/api/v1/drivers/${id}`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: driversKey }),
  });
}

export function useDeactivateDriver() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/drivers/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: driversKey }),
  });
}
