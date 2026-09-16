import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { MailCheck } from "lucide-react";
import { api, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";
import { CornerLink, PosterFacts, PublicError, PublicShell, publicButtonClass } from "@/components/PublicShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const t = strings.forgotPassword;

/**
 * Asks for a fresh reset link. Also the way back into an invite whose link has expired — the
 * code lives 30 minutes, and an invited client who opens the mail the next morning would
 * otherwise have to be re-invited by hand.
 *
 * <p>The confirmation is the same whether or not the address has an account: the backend is a
 * deliberate silent no-op for unknown addresses so the form cannot be used to find out who is
 * registered, and the screen must not give away what the API refuses to.
 */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await api.post("/api/v1/auth/request-reset-password", { email });
      setSent(true);
    } catch (err) {
      setError(apiErrorMessage(err, t.genericError));
    } finally {
      setLoading(false);
    }
  }

  return (
    // Direcția „Poster”, ca loginul (docs/stil-interfata.md, „Paginile de dinaintea contului”).
    <PublicShell
      headline={t.posterHeadline}
      accent={t.posterAccent}
      lede={t.posterLede}
      aside={<PosterFacts lines={t.posterFacts} />}
      corner={<CornerLink prompt={t.remembered} label={t.goToLogin} to="/login" />}
    >
      <div className="mx-auto flex w-full max-w-[400px] flex-col gap-8">
        {sent ? (
          <>
            <div className="flex flex-col gap-3">
              <MailCheck className="h-12 w-12 text-mark" aria-hidden />
              <h1 className="text-[32px] font-semibold leading-10 tracking-[-0.015em] text-content">{t.sent}</h1>
              <p className="text-content-muted">{t.sentHint}</p>
            </div>
            <Link to="/login" className="text-sm font-medium text-mark hover:underline">
              {t.backToLogin}
            </Link>
          </>
        ) : (
          <>
            <div className="flex flex-col gap-2">
              <h1 className="text-[32px] font-semibold leading-10 tracking-[-0.015em] text-content">{t.title}</h1>
              <p className="text-content-muted">{t.subtitle}</p>
            </div>
            <form onSubmit={handleSubmit} className="flex flex-col gap-5">
              <div>
                <Label htmlFor="fp-email" className="text-[0.8125rem] text-content-strong">
                  {t.email}
                </Label>
                <Input
                  id="fp-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                  autoFocus
                  className="h-12 px-4 text-base"
                />
              </div>
              {error && <PublicError>{error}</PublicError>}
              <Button type="submit" size="lg" className={cn("w-full", publicButtonClass)} loading={loading}>
                {loading ? t.sending : t.submit}
              </Button>
            </form>
            <Link to="/login" className="text-sm font-medium text-mark hover:underline">
              {t.backToLogin}
            </Link>
          </>
        )}
      </div>
    </PublicShell>
  );
}
