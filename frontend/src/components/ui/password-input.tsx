import { forwardRef, useState, type InputHTMLAttributes } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Input } from "@/components/ui/input";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

const t = strings.common.password;

/**
 * Cât de bună e o parolă nouă, 0–3. Regula serverului e pragul 1 (8 caractere, literă mare, literă mică,
 * cifră — `AuthenticationService.validatePasswordStrength`); peste ea, lungimea și un semn în plus o fac mai grea.
 * Fără bibliotecă: nu judecă dicționare, doar spune omului unde e față de regulă.
 */
export function passwordStrength(value: string): 0 | 1 | 2 | 3 {
  // Unicode, ca `Character.isUpperCase` din `AuthenticationService.validatePasswordStrength`: „Ă” e literă mare.
  const meetsRule = value.length >= 8 && /\p{Lu}/u.test(value) && /\p{Ll}/u.test(value) && /\p{Nd}/u.test(value);
  if (!meetsRule) return 0;
  const symbol = /[^\p{L}\p{Nd}]/u.test(value);
  if (value.length >= 14 || (value.length >= 12 && symbol)) return 3;
  if (value.length >= 12 || symbol) return 2;
  return 1;
}

const LEVELS = [
  { label: t.strength0, bar: "bg-red-500" },
  { label: t.strength1, bar: "bg-amber-500" },
  { label: t.strength2, bar: "bg-brand" },
  { label: t.strength3, bar: "bg-brand" },
] as const;

/**
 * Rubrica de parolă cu „Arată”. Cu `showStrength`, sub ea stă bara de putere — numai la o parolă nouă,
 * nu la login, unde ar da indicii despre o parolă existentă cuiva care se uită peste umăr.
 */
export const PasswordInput = forwardRef<
  HTMLInputElement,
  Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & { showStrength?: boolean }
>(({ className, showStrength = false, value, ...props }, ref) => {
  const [visible, setVisible] = useState(false);
  const text = typeof value === "string" ? value : "";
  const level = passwordStrength(text);
  return (
    <div>
      <div className="relative">
        <Input
          ref={ref}
          type={visible ? "text" : "password"}
          value={value}
          className={cn("pr-10", className)}
          {...props}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? t.hide : t.show}
          aria-pressed={visible}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center text-content-subtle hover:text-content"
        >
          {visible ? <EyeOff className="h-4 w-4" aria-hidden /> : <Eye className="h-4 w-4" aria-hidden />}
        </button>
      </div>
      {showStrength && text.length > 0 && (
        <div className="mt-1.5" data-testid="password-strength" data-level={level}>
          <div className="flex gap-1" aria-hidden>
            {[1, 2, 3].map((step) => (
              <span
                key={step}
                className={cn(
                  "h-1 flex-1 rounded-full",
                  level === 0 && step === 1 ? LEVELS[0].bar : step <= level ? LEVELS[level].bar : "bg-line-strong"
                )}
              />
            ))}
          </div>
          <p className="mt-1 text-xs text-content-muted" aria-live="polite">
            {LEVELS[level].label}
          </p>
        </div>
      )}
    </div>
  );
});
PasswordInput.displayName = "PasswordInput";
