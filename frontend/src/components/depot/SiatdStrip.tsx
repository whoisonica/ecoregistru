import { useSiatdSummary } from "@/hooks/useSiatd";
import { strings } from "@/lib/strings";
import type { SiatdSummary } from "@/lib/types";

const t = strings.siatd;

/** Părțile benzii, doar cele diferite de zero; gol când nu e nimic de spus. */
export function siatdStripParts(s: SiatdSummary | undefined): { text: string; tone: "muted" | "warn" | "bad" }[] {
  if (!s?.anyModule) return [];
  const parts: { text: string; tone: "muted" | "warn" | "bad" }[] = [];
  if (s.pending > 0) parts.push({ text: t.stripPending.replace("{count}", String(s.pending)), tone: "muted" });
  if (s.dueToday > 0) parts.push({ text: t.stripToday.replace("{count}", String(s.dueToday)), tone: "warn" });
  if (s.dueTomorrow > 0) parts.push({ text: t.stripTomorrow.replace("{count}", String(s.dueTomorrow)), tone: "warn" });
  if (s.missed > 0) parts.push({ text: t.stripMissed.replace("{count}", String(s.missed)), tone: "bad" });
  return parts;
}

/**
 * F6a — rândul SIATD de pe Operațiuni: „SIATD: 5 de confirmat · 1 expiră azi · 2 ratate”, clic → tabul SIATD. Stă pe
 * același rând cu reținerile, când sunt, ca ecranul să nu crească (1440×900 fără derulare).
 */
export function SiatdStripContent({ onOpen }: { onOpen: () => void }) {
  const { data } = useSiatdSummary();
  const parts = siatdStripParts(data);
  if (parts.length === 0) return null;
  return (
    <button
      type="button"
      onClick={onOpen}
      aria-label={t.stripOpen}
      data-testid="siatd-strip"
      className="flex flex-wrap items-baseline gap-x-2 text-left text-sm hover:underline"
    >
      <span className="text-xs font-medium uppercase tracking-wide text-content-muted">{t.stripTitle}</span>
      {parts.map((p, i) => (
        <span
          key={p.text}
          className={
            p.tone === "bad" ? "text-state-bad-text" : p.tone === "warn" ? "text-state-warn-text" : "text-content"
          }
        >
          {i > 0 && <span className="mr-2 text-content-subtle">·</span>}
          {p.text}
        </span>
      ))}
    </button>
  );
}

/** Banda singură, pentru cine nu vede reținerile (operatorul): aceeași cutie, un singur rând. */
export function SiatdStrip({ onOpen }: { onOpen: () => void }) {
  const { data } = useSiatdSummary();
  if (siatdStripParts(data).length === 0) return null;
  return (
    <section className="mb-3 border border-line bg-surface-sunken px-4 py-2">
      <SiatdStripContent onOpen={onOpen} />
    </section>
  );
}
