import { cva, type VariantProps } from "class-variance-authority";
import { Loader2 } from "lucide-react";
import { forwardRef, type ButtonHTMLAttributes } from "react";
import { Link, type LinkProps } from "react-router-dom";
import { cn } from "@/lib/utils";

/**
 * `whitespace-nowrap` stă în bază, nu la apelanți.
 *
 * <p>Toate măsurile de mai jos sunt **înălțimi fixe** (`h-8`, `h-10`, `h-12`): o etichetă de două
 * cuvinte care se rupe nu mărește butonul, îi iese din cutie. „Adaugă cantitatea" de pe Mișcări
 * măsura `scrollHeight 36` într-un `clientHeight 32`, cu pictograma rămasă lângă primul rând — și
 * era al treilea caz al aceluiași defect, după coloana de acțiuni din Cereri (07.09) și cea din
 * Termene (08.09), reparate amândouă local. Al treilea l-a găsit tot o captură, nu o verificare de
 * DOM: regula 5.
 *
 * <p>Al patrulea nu mai are de unde veni. Butoanele cu text lung care chiar trebuie să curgă pe
 * mai multe rânduri — zona de fișiere, rândurile de meniu — nu trec prin primitiva asta.
 */
const buttonVariants = cva(
  // Direcția „Cântar”: colț de 5px, chenar de 1px, text de 600. Fără pastilă, fără umbră — butonul
  // principal e verdele Punctului Verde, plin; celelalte sunt albe cu chenar.
  "inline-flex items-center justify-center whitespace-nowrap rounded-md border text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "border-brand bg-brand text-brand-fg hover:border-brand-700 hover:bg-brand-700",
        outline: "border-line-strong bg-surface text-content hover:bg-surface-muted",
        ghost: "border-transparent hover:bg-surface-sunken",
        // Intrarea în depozit: albastru, cealaltă direcție față de verde.
        inbound: "border-inbound bg-inbound text-white hover:border-inbound-key hover:bg-inbound-key",
        // Pentru fapta care nu se ia înapoi. Până acum se scria pe loc, ca
        // `className="text-red-600 hover:bg-red-50"` pe un buton ghost — de patru ori, cu patru
        // nuanțe ușor diferite.
        danger: "border-state-bad bg-state-bad text-white hover:border-state-bad-text hover:bg-state-bad-text",
        "danger-ghost": "border-transparent text-state-bad-text hover:bg-red-50",
      },
      size: {
        default: "h-10 px-4",
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
  /**
   * Tasta care face același lucru, scrisă mic în buton („N”, „I”, „E”). Doar ca reper: scurtătura
   * o leagă cine o are (`useHotkey`), butonul doar o arată. Nu se randează sub `sm`, unde nu e
   * tastatură.
   */
  hotkey?: string;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, loading = false, disabled, hotkey, children, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    >
      {loading && <Loader2 className="mr-2 h-4 w-4 shrink-0 animate-spin" aria-hidden />}
      {children}
      {hotkey && (
        <kbd
          aria-hidden
          className={cn(
            "key ml-2 hidden sm:inline-grid",
            variant === "outline" || variant === "ghost" || variant === "danger-ghost"
              ? "key-light"
              : "border-white/40 bg-transparent text-white"
          )}
        >
          {hotkey}
        </kbd>
      )}
    </button>
  )
);
Button.displayName = "Button";

/**
 * Un buton care duce undeva. Arată identic, dar **este** o legătură.
 *
 * <p>Diferența nu e cosmetică: o navigare scrisă ca `<button onClick={navigate(...)}>` nu se poate
 * deschide într-o filă nouă, nu se poate copia cu clic dreapta și nu spune cititorului de ecran că
 * duce în altă parte. Rapoartele care trimit spre mișcarea vinovată sunt exact cazul în care omul
 * vrea des a doua filă: repară acolo, se întoarce la listă aici.
 */
export const LinkButton = forwardRef<
  HTMLAnchorElement,
  LinkProps & VariantProps<typeof buttonVariants>
>(({ className, variant, size, ...props }, ref) => (
  <Link ref={ref} className={cn(buttonVariants({ variant, size, className }))} {...props} />
));
LinkButton.displayName = "LinkButton";
