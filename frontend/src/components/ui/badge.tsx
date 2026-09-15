import { cva, type VariantProps } from "class-variance-authority";
import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

/**
 * Starea unui rând, ca LED: un pătrățel colorat de 8px și cuvântul lângă el.
 *
 * <p>Direcția „Cântar” (docs/stil-interfata.md): nu mai e o pastilă cu fundal colorat. Culoarea e
 * a unui punct, nu a unei suprafețe — deci pe un rând de tabel se citește dintr-o privire care
 * coloană e „stare" și nu se bate cu culoarea pubelei de lângă codul de deșeu. Sensul rămâne cel
 * dintotdeauna: verde = gata, galben = o așteptare legitimă, roșu = nu se poate depune așa.
 */
const badgeVariants = cva(
  "inline-flex items-center gap-1.5 whitespace-nowrap text-xs font-medium before:h-2 before:w-2 before:shrink-0 before:rounded-sm before:content-['']",
  {
    variants: {
      variant: {
        default: "text-content-strong before:bg-content-subtle",
        success: "text-state-ok-text before:bg-state-ok",
        warning: "text-state-warn-text before:bg-state-warn",
        danger: "text-state-bad-text before:bg-state-bad",
        muted: "text-content-muted before:bg-state-off",
      },
    },
    defaultVariants: { variant: "default" },
  }
);

export interface BadgeProps
  extends HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant, className }))} {...props} />;
}
