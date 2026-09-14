import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  CompanyUser,
  Consultancy,
  ConsultancyInput,
  InviteConsultantInput,
} from "@/lib/types";

/**
 * P2.13 — cabinetele, cum le vede platforma. Endpointul e 403 pentru oricine altcineva, deci lista
 * primește `enabled`, ca `useCompanies`.
 */
export const consultanciesKey = ["consultancies"] as const;

export function useConsultancies(enabled: boolean) {
  return useQuery({
    queryKey: consultanciesKey,
    queryFn: async () => (await api.get<Consultancy[]>("/api/v1/consultancies")).data,
    enabled,
  });
}

export function useCreateConsultancy() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: ConsultancyInput) =>
      (await api.post<Consultancy>("/api/v1/consultancies", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: consultanciesKey }),
  });
}

/** Schimbă numărul de consultanți al rândului, deci reîmprospătează lista. */
export function useInviteConsultant() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: InviteConsultantInput }) =>
      (await api.post<CompanyUser>(`/api/v1/consultancies/${id}/users`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: consultanciesKey }),
  });
}

/**
 * Echipa cabinetului, cum o vede un consultant. Cabinetul nu se numește nicăieri — e al sesiunii —,
 * la fel cum `useUsers` nu numește firma.
 */
export const teamKey = ["consultancy", "team"] as const;

export function useConsultancyTeam(enabled: boolean) {
  return useQuery({
    queryKey: teamKey,
    queryFn: async () => (await api.get<CompanyUser[]>("/api/v1/consultancy/users")).data,
    enabled,
  });
}

export function useInviteColleague() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: InviteConsultantInput) =>
      (await api.post<CompanyUser>("/api/v1/consultancy/users", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: teamKey }),
  });
}

export function useDeactivateColleague() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/consultancy/users/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: teamKey }),
  });
}

export function useReactivateColleague() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/consultancy/users/${id}/reactivate`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: teamKey }),
  });
}
