import { cva, type VariantProps } from "class-variance-authority";
import { Loader2 } from "lucide-react";
import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-1 disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-brand text-brand-fg hover:bg-brand-800",
        outline: "border border-line-strong bg-surface hover:bg-surface-muted",
        ghost: "hover:bg-surface-sunken",
        // Pentru fapta care nu se ia înapoi. Până acum se scria pe loc, ca
        // `className="text-red-600 hover:bg-red-50"` pe un buton ghost — de patru ori, cu patru
        // nuanțe ușor diferite.
        danger: "bg-red-600 text-white hover:bg-red-700",
        "danger-ghost": "text-red-600 hover:bg-red-50",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-8 px-3",
        lg: "h-12 px-6 text-base",
        // Pătrat, pentru butoanele care poartă doar o pictogramă.
        icon: "h-10 w-10 shrink-0",
        "icon-sm": "h-8 w-8 shrink-0",
      },
    },
    defaultVariants: { variant: "default", size: "default" },
  }
);

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  /**
   * Arată o rotiță în locul conținutului și dezactivează butonul.
   *
   * <p>Până acum fiecare apelant își scria singur „se salvează…" și `disabled`, deci butonul
   * își schimba lățimea sub deget, iar unele locuri uitau `disabled` cu totul și lăsau trimiterea
   * de două ori.
   */
  loading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, loading = false, disabled, children, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    >
      {loading && <Loader2 className="mr-2 h-4 w-4 shrink-0 animate-spin" aria-hidden />}
      {children}
    </button>
  )
);
Button.displayName = "Button";
