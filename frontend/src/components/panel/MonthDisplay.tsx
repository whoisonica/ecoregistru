import { Link } from "react-router-dom";
import type { NextAction } from "@/hooks/useDashboardData";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

const t = strings.panel;

/** Kilogramele, cu separatorul românesc de mii și fără zecimale — cifra de pe afișaj. */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 });

/**
 * Afișajul lunii — LCD-ul cântarului, în panou.
 *
 * <p>Rândul de sus e luna și ce măsoară cifra („SEPTEMBRIE · INTRAT"), cifra e în kg, iar rândul
 * de jos e **cel mai urgent lucru** — același verdict ca banda „Următoarea acțiune" de pe Acasă,
 * din aceeași socoteală (`useDashboardData`), deci nu pot spune lucruri diferite. Clic pe el duce
 * acolo.
 *
 * <p>Când o sursă n-a răspuns, cifra e „?", nu „0": un zero verde peste o cerere căzută ar fi
 * aceeași minciună ca „Ești la zi" (decizia 68).
 */
export function MonthDisplay({
  monthName,
  label,
  kg,
  loading,
  failed,
  action,
  actionLoading,
  collapsed = false,
}: {
  monthName: string;
  label: string;
  kg: number | null;
  loading: boolean;
  failed: boolean;
  action: NextAction | null;
  actionLoading: boolean;
  collapsed?: boolean;
}) {
  const digits = failed ? t.unknown : loading || kg == null ? "—" : kgFormat.format(kg);
  const tone =
    action?.tone === "danger"
      ? "text-lcd-bad"
      : action?.tone === "warning"
        ? "text-lcd-warn"
        : action?.tone === "ok"
          ? "text-lcd-digit"
          : "text-panel-mid";

  if (collapsed) {
    return (
      <div
        className="rounded-md border border-panel-line bg-lcd px-1 py-1.5 text-center font-mono"
        aria-label={`${monthName} · ${label}: ${digits} ${t.kg}`}
      >
        <div className="truncate text-[0.6875rem] font-medium text-lcd-digit">{digits}</div>
        <div className="text-[0.5625rem] uppercase text-lcd-unit">{t.kg}</div>
      </div>
    );
  }

  return (
    <div className="rounded-md border border-panel-line bg-lcd px-3 pb-2 pt-2 font-mono" data-testid="month-display">
      <div className="flex items-center justify-between text-[0.6875rem] uppercase tracking-[0.07em] text-panel-dim">
        <span className="truncate">
          {monthName} · {label}
        </span>
      </div>
      <div className="mt-0.5 text-2xl font-medium leading-tight text-lcd-digit">
        {digits}
        <span className="ml-1 text-xs text-lcd-unit">{t.kg}</span>
      </div>
      <div className="mt-1.5 border-t border-dashed border-panel-line pt-1.5 text-[0.71875rem] leading-snug">
        {actionLoading || !action ? (
          <span className="text-panel-faint">…</span>
        ) : action.tone === "unknown" ? (
          <span className={cn("block", tone)}>{action.title}</span>
        ) : (
          <Link
            to={action.to}
            className={cn(
              "block truncate rounded-sm hover:underline focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
              tone
            )}
          >
            {action.tone === "danger" ? "▲ " : action.tone === "warning" ? "▲ " : "● "}
            {action.title}
          </Link>
        )}
      </div>
    </div>
  );
}
