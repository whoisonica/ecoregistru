import { LOGO_ARROWS, LOGO_ARROW_WIDTH, LOGO_LETTERS, LOGO_LETTERS_COLOR, LOGO_VIEWBOX } from "@/lib/brandLogo";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

/**
 * Logoul eco360 (`lib/brandLogo.ts`). Numele nu se mai scrie lângă el: e chiar în logo, „ECO” peste „360”.
 * Cititorul de ecran îl aude din `aria-label`.
 *
 * <p>`onDark` pune literele albe, pentru panoul grafit; altfel sunt verdele închis al logoului, pentru hârtie.
 * Mărimea vine din `className` (implicit 40px, cât să se citească literele).
 */
export function BrandName({ className, onDark = false }: { className?: string; onDark?: boolean }) {
  const ink = onDark ? LOGO_LETTERS_COLOR.dark : LOGO_LETTERS_COLOR.light;
  return (
    <svg
      viewBox={LOGO_VIEWBOX}
      role="img"
      aria-label={strings.appName}
      className={cn("block h-10 w-10 shrink-0", className)}
    >
      {LOGO_ARROWS.map((a) => (
        <g key={a.color}>
          <path d={a.arc} fill="none" stroke={a.color} strokeWidth={LOGO_ARROW_WIDTH} />
          <path d={a.head} fill={a.color} />
        </g>
      ))}
      {LOGO_LETTERS.map((d) => (
        <path key={d.slice(0, 12)} d={d} fill={ink} />
      ))}
    </svg>
  );
}
