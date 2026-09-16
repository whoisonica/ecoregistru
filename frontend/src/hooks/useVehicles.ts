import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Vehicle, VehicleInput } from "@/lib/types";

/** Flota (D2.1): toate vehiculele firmei, active și inactive; ecranele filtrează. */
export const vehiclesKey = ["vehicles"] as const;

export function useVehicles(enabled = true) {
  return useQuery({
    queryKey: vehiclesKey,
    queryFn: async () => (await api.get<Vehicle[]>("/api/v1/vehicles")).data,
    enabled,
  });
}

export function useCreateVehicle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: VehicleInput) => (await api.post<Vehicle>("/api/v1/vehicles", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: vehiclesKey }),
  });
}

export function useUpdateVehicle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: VehicleInput }) =>
      (await api.put<Vehicle>(`/api/v1/vehicles/${id}`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: vehiclesKey }),
  });
}

export function useDeactivateVehicle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/vehicles/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: vehiclesKey }),
  });
}

export function useReactivateVehicle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/vehicles/${id}/reactivate`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: vehiclesKey }),
  });
}

export function useDeleteVehicle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/vehicles/${id}/definitiv`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: vehiclesKey }),
  });
}
