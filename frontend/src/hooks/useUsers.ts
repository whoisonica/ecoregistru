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

/**
 * F-D — pe pagina unei firme (`/clienti/:id`) lista e a firmei din adresă, nu a celei din comutator: cererea poartă
 * antetul ei. Fără `companyId` rămâne firma sesiunii, ca în Setări.
 */
function tenant(companyId?: string) {
  return companyId ? { headers: { "X-Tenant-Id": companyId } } : undefined;
}

export function useUsers(enabled: boolean, companyId?: string) {
  return useQuery({
    queryKey: [...usersKey, companyId ?? "current"],
    queryFn: async () => (await api.get<CompanyUser[]>("/api/v1/users", tenant(companyId))).data,
    enabled,
  });
}

export function useInviteCompanyUser(companyId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: InviteUserInput) =>
      (await api.post<CompanyUser>("/api/v1/users", input, tenant(companyId))).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: usersKey });
      // Numărul de utilizatori din tabelul Clienți.
      qc.invalidateQueries({ queryKey: ["subscriptions", "client-overview"] });
    },
  });
}

/**
 * Retrimite invitația. Nu invalidează lista: nu schimbă niciun rând, doar pleacă un mail — iar
 * linkul vechi din mailul vechi se stinge pe server, ca să nu circule două deodată.
 */
export function useResendInvite(companyId?: string) {
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/users/${id}/resend-invite`, undefined, tenant(companyId));
    },
  });
}

export function useChangeUserRole(companyId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, role }: { id: string; role: InviteRole }) =>
      (await api.put<CompanyUser>(`/api/v1/users/${id}/role`, { role }, tenant(companyId))).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}

/**
 * Dezactivează contul. `DELETE`, dar nu șterge nimic — rândul rămâne, fiindcă e cel din
 * `createdBy` al fiecărei mișcări pe care omul a înregistrat-o. Sesiunile deschise se închid la
 * prima cerere de după (P0.4).
 */
export function useDeactivateUser(companyId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/users/${id}`, tenant(companyId));
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}

/**
 * Anulează o invitație nefolosită — singurul loc din aplicație care chiar șterge un rând.
 * Se poate doar aici: într-un cont în care nu s-a intrat niciodată nu e nimic scris, deci nu taie
 * niciun fir de evidență. Adresa se eliberează, ceea ce e chiar rostul, după o greșeală de tastare.
 */
export function useCancelInvite(companyId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/users/${id}/invitation`, tenant(companyId));
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}

export function useReactivateUser(companyId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/users/${id}/reactivate`, undefined, tenant(companyId));
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: usersKey }),
  });
}
