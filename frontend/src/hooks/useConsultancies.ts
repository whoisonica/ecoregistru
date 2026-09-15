import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  CompanyUser,
  Consultancy,
  ConsultancyInput,
  ConsultancyOverviewRow,
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

/** Nu schimbă nimic din listă — invitația rămâne în așteptare —, deci nu reîmprospătează. */
export function useResendColleagueInvite() {
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/consultancy/users/${id}/resend-invite`);
    },
  });
}

export function useCancelColleagueInvite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/consultancy/users/${id}/invitation`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: teamKey }),
  });
}

/**
 * P2.13, felia 2 — „Toate firmele mele". Nu ține de firma aleasă; comutatorul golește oricum cache-ul,
 * deci la întoarcere cifrele se cer din nou, după ce s-a lucrat pe firmă.
 */
export const overviewKey = ["consultancy", "overview"] as const;

export function useConsultancyOverview(enabled: boolean) {
  return useQuery({
    queryKey: overviewKey,
    queryFn: async () =>
      (await api.get<ConsultancyOverviewRow[]>("/api/v1/consultancy/overview")).data,
    enabled,
  });
}
