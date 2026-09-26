import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Scale, ScaleEventInput, ScaleInput } from "@/lib/types";

/** Cântarele depozitelor (D2.3), cu starea de azi și istoricul. */
export const scalesKey = ["scales"] as const;

export function useScales(enabled = true) {
  return useQuery({
    queryKey: scalesKey,
    queryFn: async () => (await api.get<Scale[]>("/api/v1/scales")).data,
    enabled,
  });
}

function useScaleMutation<V>(fn: (vars: V) => Promise<unknown>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    // Starea cântarului intră și în operațiunile în lucru.
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: scalesKey });
      qc.invalidateQueries({ queryKey: ["weighing-operations"] });
    },
  });
}

export function useSaveScale() {
  return useScaleMutation(async ({ id, input }: { id: string | null; input: ScaleInput }) =>
    id ? (await api.put<Scale>(`/api/v1/scales/${id}`, input)).data : (await api.post<Scale>("/api/v1/scales", input)).data,
  );
}

export function useDeleteScale() {
  return useScaleMutation(async (id: string) => {
    await api.delete(`/api/v1/scales/${id}`);
  });
}

export function useAddScaleEvent() {
  return useScaleMutation(async ({ scaleId, input }: { scaleId: string; input: ScaleEventInput }) =>
    (await api.post<Scale>(`/api/v1/scales/${scaleId}/events`, input)).data,
  );
}

export function useDeleteScaleEvent() {
  return useScaleMutation(async ({ scaleId, eventId }: { scaleId: string; eventId: string }) =>
    (await api.delete<Scale>(`/api/v1/scales/${scaleId}/events/${eventId}`)).data,
  );
}
