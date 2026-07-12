import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Company } from "@/lib/types";

/**
 * Lists all tenants for the PLATFORM_ADMIN tenant switcher. The endpoint is 403 for any
 * other role, so callers MUST pass `enabled` (their role check) to avoid firing it.
 */
export function useCompanies(enabled: boolean) {
  return useQuery({
    queryKey: ["companies"],
    queryFn: async () => (await api.get<Company[]>("/api/v1/companies")).data,
    enabled,
    staleTime: 5 * 60 * 1000, // the tenant list barely changes within a session
  });
}
