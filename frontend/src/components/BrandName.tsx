import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

/**
 * Numele aplicației, cu semnul mărcii în față — bucla-casă de pe wastehouse.ro, aceeași ca în favicon.
 *
 * <p>Culorile semnului sunt ale mărcii, nu ale temei: nu urmează `brand` din Tailwind, care rămâne
 * paleta ecranelor. Semnul se scalează cu textul (`em`), deci merge la fel în bara laterală și pe
 * cardul de login.
 */
export function BrandName({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <svg viewBox="0 0 32 32" aria-hidden className="h-[1.25em] w-[1.25em] shrink-0">
        <rect width="32" height="32" rx="8" fill="#047857" />
        <path
          d="M11.5 23.5H7V13L16 5l9 8v10.5h-7.5M21 20l-3.5 3.5L21 27"
          stroke="#ffffff"
          strokeWidth="2.8"
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      {strings.appName}
    </span>
  );
}
