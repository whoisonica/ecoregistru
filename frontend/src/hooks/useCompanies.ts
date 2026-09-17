import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  ClientOverview,
  Company,
  CompanyInput,
  CompanyUser,
  InviteUserInput,
  OnboardClientInput,
  OnboardClientResult,
  PriceVisibility,
} from "@/lib/types";

/**
 * Companies (tenants) — platform admin and consultant. The list drives the tenant switcher AND the
 * client-management screen; for a consultant the server narrows it to their consultancy. The
 * endpoint is 403 for any other role, so the list query MUST receive `enabled` (the caller's role
 * check) to avoid firing it. Mutations invalidate the list.
 */
export const companiesKey = ["companies"] as const;
/** Sub „subscriptions”: orice schimbare de abonament sau factură îl reîmprospătează singură. */
export const clientOverviewKey = ["subscriptions", "client-overview"] as const;

/** F-B — abonamentul, ultima factură și utilizatorii fiecărei firme, pentru tabelul Clienți. */
export function useClientOverview(enabled: boolean) {
  return useQuery({
    queryKey: clientOverviewKey,
    queryFn: async () => (await api.get<ClientOverview[]>("/api/v1/companies/overview")).data,
    enabled,
  });
}

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
 * The tenant switcher drops the cached data of the company being left, so no tenant id belongs
 * in the key.
 */
export const currentCompanyKey = ["company", "current"] as const;

export function useCurrentCompany(enabled = true) {
  return useQuery({
    queryKey: currentCompanyKey,
    queryFn: async () => (await api.get<Company>("/api/v1/companies/current")).data,
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateCompany() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: CompanyInput) =>
      (await api.post<Company>("/api/v1/companies", input)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: companiesKey });
      qc.invalidateQueries({ queryKey: clientOverviewKey });
    },
  });
}

/**
 * F-C — „Client nou”: firma, cererea aprobată, abonamentul și invitația într-o singură cerere. Schimbă lista de firme,
 * cererile și tot ce e sub „subscriptions” (tabelul Clienți, fondatorii).
 */
export function useOnboardClient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: OnboardClientInput) =>
      (await api.post<OnboardClientResult>("/api/v1/companies/onboard", input)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: companiesKey });
      qc.invalidateQueries({ queryKey: ["subscriptions"] });
      qc.invalidateQueries({ queryKey: ["account-requests"] });
    },
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

/**
 * D1.8 — cine vede prețurile depozitului, pe firma curentă. Doar adminul firmei. Răspunsul e chiar
 * firma curentă, deci se pune în cache direct; operațiunile se recitesc, fiindcă prețul din ele
 * atârnă de setare.
 */
export function useUpdatePriceVisibility() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (priceVisibility: PriceVisibility) =>
      (await api.put<Company>("/api/v1/companies/current/price-visibility", { priceVisibility })).data,
    onSuccess: (company) => {
      qc.setQueryData(currentCompanyKey, company);
      qc.invalidateQueries({ queryKey: ["weighing-operations"] });
    },
  });
}

/**
 * P2.13 — mută firma într-un cabinet, sau (`null`) o face client direct. Numai platforma. Schimbă
 * și numărul de firme al cabinetelor, deci se reîmprospătează și lista lor.
 */
export function useAssignConsultancy() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, consultancyId }: { id: string; consultancyId: string | null }) =>
      (await api.put<Company>(`/api/v1/companies/${id}/consultancy`, { consultancyId })).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: companiesKey });
      qc.invalidateQueries({ queryKey: ["consultancies"] });
    },
  });
}

export function useInviteUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: InviteUserInput }) =>
      (await api.post<CompanyUser>(`/api/v1/companies/${id}/users`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: clientOverviewKey }),
  });
}
