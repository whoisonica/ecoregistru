import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { AccountRequest, AccountRequestInput, Company } from "@/lib/types";
import { companiesKey } from "@/hooks/useCompanies";

/**
 * The intake form. Submitting is public — it is the only unauthenticated write in the app, and it
 * creates a request, not an account — so `useSubmitAccountRequest` deliberately has no query key
 * to invalidate: the submitter can never read back what they sent.
 *
 * Reading and handling the requests is PLATFORM_ADMIN, so the list query must receive `enabled`
 * (the caller's role check) the same way `useCompanies` does.
 */
export const accountRequestsKey = ["account-requests"] as const;

export function useSubmitAccountRequest() {
  return useMutation({
    mutationFn: async (input: AccountRequestInput) => {
      await api.post("/api/v1/account-requests", input);
    },
  });
}

export function useAccountRequests(enabled: boolean) {
  return useQuery({
    queryKey: accountRequestsKey,
    queryFn: async () => (await api.get<AccountRequest[]>("/api/v1/account-requests")).data,
    enabled,
  });
}

export function useApproveAccountRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) =>
      (await api.post<Company>(`/api/v1/account-requests/${id}/approve`)).data,
    onSuccess: () => {
      // Approving creates a company, so both lists are stale.
      qc.invalidateQueries({ queryKey: accountRequestsKey });
      qc.invalidateQueries({ queryKey: companiesKey });
    },
  });
}

export function useRejectAccountRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, reason }: { id: string; reason: string }) => {
      await api.post(`/api/v1/account-requests/${id}/reject`, { reason });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: accountRequestsKey }),
  });
}
