import { LOGO_ARROWS, LOGO_ARROW_WIDTH, LOGO_LETTERS, LOGO_VIEWBOX } from "@/lib/brandLogo";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

/**
 * Logoul eco360 (`lib/brandLogo.ts`). Numele nu se mai scrie lângă el: e chiar în logo, „ECO” peste „360”.
 * Cititorul de ecran îl aude din `aria-label`.
 *
 * <p>Culorile vin din variabilele temei (`--logo-*` în `index.css`), deci logoul își schimbă singur literele
 * când omul alege altă temă. Culorile din `brandLogo.ts` rămân ale telefonului. `onPanel` ia
 * variantele panoului (`--panel-logo-*`): pe un panou închis literele sunt albe și prima săgeată mai deschisă.
 * Mărimea vine din `className` (implicit 40px, cât să se citească literele).
 */
export function BrandName({ className, onPanel = false }: { className?: string; onPanel?: boolean }) {
  const scope = onPanel ? "panel-logo" : "logo";
  // Doar prima săgeată se schimbă pe panou; a doua și a treia sunt aceleași pe toate fondurile.
  const arrowColor = (i: number) => `rgb(var(--${i === 0 ? scope : "logo"}-a${i + 1}))`;
  return (
    <svg
      viewBox={LOGO_VIEWBOX}
      role="img"
      aria-label={strings.appName}
      className={cn("block h-10 w-10 shrink-0", className)}
    >
      {LOGO_ARROWS.map((a, i) => (
        <g key={a.color}>
          <path d={a.arc} fill="none" style={{ stroke: arrowColor(i) }} strokeWidth={LOGO_ARROW_WIDTH} />
          <path d={a.head} style={{ fill: arrowColor(i) }} />
        </g>
      ))}
      {LOGO_LETTERS.map((d) => (
        <path key={d.slice(0, 12)} d={d} style={{ fill: `rgb(var(--${scope}-letters))` }} />
      ))}
    </svg>
  );
}
