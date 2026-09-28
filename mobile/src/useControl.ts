import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { useCallback, useEffect } from "react";

import { evidences, partners, upcomingDeadlines, UnauthorizedError } from "./api";
import { agoText } from "./agoText";
import { verdictOf } from "./components/StateCard";
import { controlChecks, type Check, type CheckTone } from "./control";
import { useSession } from "./session";

const m = strings.mobile;

/**
 * Verificările controlului și verdictul lor — pentru „A venit controlul” și pentru modul inspector (F10),
 * din aceleași cereri (aceleași chei), deci cele două ecrane nu pot spune lucruri diferite.
 *
 * <p>Fără cache pe SQLite, dinadins: o listă de ieri arătată ca „în regulă” în fața inspectorului e mai
 * rea decât un „?”.
 */
export function useControl() {
  const { session, auth, signOut } = useSession();
  const tenant = session?.tenantId;
  const year = new Date().getFullYear();
  const enabled = !!auth && !!tenant;

  const deadlinesQ = useQuery({ queryKey: ["deadlines", tenant, "upcoming"], queryFn: () => upcomingDeadlines(auth!), enabled });
  const evidencesQ = useQuery({ queryKey: ["evidences", tenant, year], queryFn: () => evidences(auth!, year), enabled });
  const partnersQ = useQuery({ queryKey: ["control", "partners", tenant], queryFn: () => partners(auth!), enabled });
  const queries = [deadlinesQ, evidencesQ, partnersQ];

  useEffect(() => {
    if (queries.some((q) => q.error instanceof UnauthorizedError)) signOut();
  }, [deadlinesQ.error, evidencesQ.error, partnersQ.error, signOut]); // eslint-disable-line react-hooks/exhaustive-deps

  const checks = controlChecks(
    year,
    { data: deadlinesQ.data, failed: deadlinesQ.isError },
    { data: evidencesQ.data, failed: evidencesQ.isError },
    { data: partnersQ.data, failed: partnersQ.isError },
  );
  // Același cuvânt ca pe Acasă (`verdictOf`): cel mai grav rând dă tonul; un rând nevenit sau necunoscut
  // ține „Nu știu încă” — „3 din 4” ar fi numărat rândul care n-a venit ca fiind în neregulă.
  const verdict = verdictOf(checks);
  const worst = (["bad", "warn", "unknown"] as CheckTone[])
    .map((tone) => checks.find((c): c is Check => c !== null && c.tone === tone))
    .find(Boolean);
  const why = verdict.tone === "loading" ? " " : verdict.tone === "unknown" ? m.controlUnknown : worst ? worst.detail : m.controlAllOk;
  const updatedAt = Math.min(...queries.map((q) => q.dataUpdatedAt || Infinity));
  const updated = Number.isFinite(updatedAt) ? agoText(updatedAt) : null;

  const refresh = useCallback(async () => {
    await Promise.all(queries.map((q) => q.refetch())).catch(() => {});
  }, [deadlinesQ.refetch, evidencesQ.refetch, partnersQ.refetch]); // eslint-disable-line react-hooks/exhaustive-deps

  return { tenant, year, checks, verdict: { ...verdict, why }, updated, refresh };
}
