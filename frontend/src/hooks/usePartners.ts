import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Partner } from "@/lib/types";

/** Partners list — read-only here; full CRUD arrives with the Partners screen (U2). */
export const partnersKey = ["partners"] as const;

export function usePartners() {
  return useQuery({
    queryKey: partnersKey,
    queryFn: async () => (await api.get<Partner[]>("/api/v1/partners")).data,
  });
}
