import type { ScaleState } from "@/lib/types";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { WARNING_DAYS, daysFromToday } from "@/components/VehiclesSection";

/**
 * Starea unui cântar (D2.3), ca LED + cuvânt: verde cât e legal, galben în ultimele 30 de zile și la
 * nedeclarat, roșu când nu se mai poate cântări fără motiv. Data e „valabil până la”, dacă există.
 */
export function ScaleStateBadge({ state, validUntil }: { state: ScaleState; validUntil?: string | null }) {
  const label = strings.scaleState[state];
  const until = validUntil ? ` · ${strings.settings.scales.validUntil} ${formatDate(validUntil)}` : "";
  if (state === "VALID") {
    const soon = validUntil != null && daysFromToday(validUntil) <= WARNING_DAYS;
    return <Badge variant={soon ? "warning" : "success"}>{`${label}${until}`}</Badge>;
  }
  if (state === "NOT_DECLARED") return <Badge variant="warning">{label}</Badge>;
  if (state === "OUT_OF_USE") return <Badge variant="muted">{label}</Badge>;
  return <Badge variant="danger">{state === "EXPIRED" ? `${label}${until}` : label}</Badge>;
}
