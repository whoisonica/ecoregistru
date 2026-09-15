import { useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";

/** Ce știe ANAF despre un CUI (`/api/v1/company-lookup/{cui}`). Nu se salvează nimic din el singur. */
export interface CompanyLookup {
  cui: string;
  name: string | null;
  address: string | null;
  tradeRegisterNumber: string | null;
  caenCode: string | null;
  county: string | null;
  city: string | null;
  registrationStatus: string | null;
  inactive: boolean;
}

/**
 * Căutarea după CUI, la cerere. E o mutație, nu o interogare: pleacă numai când omul apasă butonul,
 * nu la fiecare cifră tastată — serviciul ANAF primește o cerere pe secundă și sancționează
 * suprasolicitarea.
 */
export function useCompanyLookup() {
  return useMutation({
    mutationFn: async (cui: string) =>
      (await api.get<CompanyLookup>(`/api/v1/company-lookup/${encodeURIComponent(cui)}`)).data,
  });
}
