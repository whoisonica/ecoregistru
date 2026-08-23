import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Company, CompanyInput, CompanyUser, InviteUserInput } from "@/lib/types";

/**
 * Companies (tenants) — platform-admin only. The list drives the tenant switcher AND the
 * client-management screen. The endpoint is 403 for any other role, so the list query MUST
 * receive `enabled` (the caller's role check) to avoid firing it. Mutations invalidate the list.
 */
export const companiesKey = ["companies"] as const;

export function useCompanies(enabled: boolean) {
  return useQuery({
    queryKey: companiesKey,
    queryFn: async () => (await api.get<Company[]>("/api/v1/companies")).data,
    enabled,
    staleTime: 5 * 60 * 1000, // the tenant list barely changes within a session
  });
}

/**
 * The tenant the session is scoped to, readable by any member. Every screen that has to know what
 * kind of company this is reads it here — the movement form offers the operations the type allows.
 * The tenant switcher clears the whole query cache, so no tenant id belongs in the key.
 */
export const currentCompanyKey = ["company", "current"] as const;

export function useCurrentCompany() {
  return useQuery({
    queryKey: currentCompanyKey,
    queryFn: async () => (await api.get<Company>("/api/v1/companies/current")).data,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateCompany() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: CompanyInput) =>
      (await api.post<Company>("/api/v1/companies", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: companiesKey }),
  });
}

export function useUpdateCompany() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: CompanyInput }) =>
      (await api.put<Company>(`/api/v1/companies/${id}`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: companiesKey }),
  });
}

export function useInviteUser() {
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: InviteUserInput }) =>
      (await api.post<CompanyUser>(`/api/v1/companies/${id}/users`, input)).data,
  });
}
