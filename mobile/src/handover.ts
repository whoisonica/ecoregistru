import { useQuery } from "@tanstack/react-query";

import * as api from "./api";
import { useCompany } from "./company";
import { cached } from "./outbox";
import { useSession } from "./session";

/**
 * Listele formularului de predare, ținute și pe telefon (`outbox.cached`), ca formularul să meargă
 * la rampă fără semnal: profilul firmei (codurile ei de deșeu și R/D, CUI-ul), punctele de lucru,
 * partenerii și ultimele predări pentru „La fel ca data trecută”.
 *
 * <p>Cheile poartă firma — și în React Query, și în cache-ul de pe disc. Profilul firmei vine din
 * `useCompany`, același pe care îl citește bara de jos.
 */
export function useHandoverData() {
  const { auth, session } = useSession();
  const tenant = session?.tenantId ?? "none";
  const enabled = !!auth && !!session?.tenantId;
  const opts = { enabled, staleTime: 5 * 60 * 1000, retry: 0 } as const;

  const company = useCompany();
  const workPoints = useQuery({
    ...opts,
    queryKey: ["work-points", session?.tenantId],
    queryFn: () => cached(`${tenant}:work-points`, () => api.workPoints(auth!)),
  });
  const partners = useQuery({
    ...opts,
    queryKey: ["partners", session?.tenantId],
    queryFn: () => cached(`${tenant}:partners`, () => api.partners(auth!)),
  });
  const recent = useQuery({
    ...opts,
    queryKey: ["movements", "recent-handovers", session?.tenantId],
    queryFn: () => cached(`${tenant}:recent-handovers`, () => api.recentHandovers(auth!)),
  });
  return { company, workPoints, partners, recent };
}
