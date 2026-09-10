import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { CompanyUser, InviteRole, InviteUserInput } from "@/lib/types";

/**
 * P1.12 — utilizatorii firmei pe care e sesiunea.
 *
 * Nu e același lucru cu `useCompanies`: acolo administratorul de platformă alege o firmă după id
 * și lucrează pe ea. Aici firma nu se numește nicăieri — e cea din sesiune (`X-Tenant-Id`), deci
 * un `ADMIN` de client își vede exact colegii lui și pe nimeni altcineva. Administratorul de
 * platformă ajunge la același ecran prin comutatorul de firme, deci e o singură implementare.
 *
 * Endpointul e 403 pentru operator și vizualizare, deci interogarea primește `enabled`, la fel ca
 * lista de firme: cine n-are voie nici n-o pornește.
 */
export const usersKey = ["users"] as const;

export function useUsers(enabled: boolean) {
  return useQuery({
    queryKey: usersKey,
    queryFn: async () => (await api.get<CompanyUser[]>("/api/v1/users")).data,
    enabled,
  });
}

export function useInviteCompanyUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: InviteUserInput) =>
      (await api.post<CompanyUser>("/api/v1/users", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}

/**
 * Retrimite invitația. Nu invalidează lista: nu schimbă niciun rând, doar pleacă un mail — iar
 * linkul vechi din mailul vechi se stinge pe server, ca să nu circule două deodată.
 */
export function useResendInvite() {
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/users/${id}/resend-invite`);
    },
  });
}

export function useChangeUserRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, role }: { id: string; role: InviteRole }) =>
      (await api.put<CompanyUser>(`/api/v1/users/${id}/role`, { role })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}

/**
 * Dezactivează contul. `DELETE`, dar nu șterge nimic — rândul rămâne, fiindcă e cel din
 * `createdBy` al fiecărei mișcări pe care omul a înregistrat-o. Sesiunile deschise se închid la
 * prima cerere de după (P0.4).
 */
export function useDeactivateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/users/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}

/**
 * Anulează o invitație nefolosită — singurul loc din aplicație care chiar șterge un rând.
 * Se poate doar aici: într-un cont în care nu s-a intrat niciodată nu e nimic scris, deci nu taie
 * niciun fir de evidență. Adresa se eliberează, ceea ce e chiar rostul, după o greșeală de tastare.
 */
export function useCancelInvite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/users/${id}/invitation`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}

export function useReactivateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/users/${id}/reactivate`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}
