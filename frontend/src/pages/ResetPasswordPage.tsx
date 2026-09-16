import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { api, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";
import { CornerLink, PosterFacts, PublicError, PublicShell, publicButtonClass } from "@/components/PublicShell";
import { Button } from "@/components/ui/button";
import { LegalNotice } from "@/components/LegalFooter";
import { PasswordInput } from "@/components/ui/password-input";
import { Label } from "@/components/ui/label";

const t = strings.resetPassword;

/**
 * Where the link in the email lands — both the "forgot password" mail and the invite, because
 * an invite IS a reset: a platform admin creates the user disabled with an unusable password,
 * and setting one through this page is what enables the account (AuthenticationService.
 * resetPassword sets enabled = true). Without this page the invite ended on a 404 and an
 * invited client could never get in, however well the mail was delivered.
 *
 * <p>Public on purpose: the code in the query string is the credential, and it lives 30 minutes.
 */
export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const code = params.get("code") ?? "";

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    // Checked here as well as on the server: it costs a round trip to be told what the two
    // boxes in front of you already say.
    if (password !== confirmPassword) {
      setError(t.mismatch);
      return;
    }
    setLoading(true);
    try {
      await api.post("/api/v1/auth/reset-password", { code, password, confirmPassword });
      setDone(true);
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
      corner={<CornerLink prompt={t.haveAccount} label={t.goToLogin} to="/login" />}
    >
      <div className="mx-auto flex w-full max-w-[400px] flex-col gap-8">
        {done ? (
          <>
            <div className="flex flex-col gap-3">
              <CheckCircle2 className="h-12 w-12 text-mark" aria-hidden />
              <h1 className="text-[32px] font-semibold leading-10 tracking-[-0.015em] text-content">{t.done}</h1>
            </div>
            <Button size="lg" className={cn("w-full", publicButtonClass)} onClick={() => navigate("/login")}>
              {t.toLogin}
            </Button>
          </>
        ) : !code ? (
          // A bare /reseteaza-parola with nothing after it: say what is missing rather than
          // showing a form that cannot succeed.
          <>
            <h1 className="text-[32px] font-semibold leading-10 tracking-[-0.015em] text-content">{t.title}</h1>
            <PublicError>{t.missingCode}</PublicError>
            <Link to="/parola-uitata" className="text-sm font-medium text-mark hover:underline">
              {t.requestNew}
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
                <Label htmlFor="rp-pass" className="text-[0.8125rem] text-content-strong">
                  {t.password}
                </Label>
                <PasswordInput
                  id="rp-pass"
                  showStrength
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                  autoFocus
                  className="h-12 px-4 text-base"
                />
                <p className="mt-1.5 text-xs text-content-muted">{t.rules}</p>
              </div>
              <div>
                <Label htmlFor="rp-confirm" className="text-[0.8125rem] text-content-strong">
                  {t.confirmPassword}
                </Label>
                <PasswordInput
                  id="rp-confirm"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                  className="h-12 px-4 text-base"
                />
              </div>
              {error && <PublicError>{error}</PublicError>}
              <Button type="submit" size="lg" className={cn("w-full", publicButtonClass)} loading={loading}>
                {loading ? t.saving : t.submit}
              </Button>
              {/* O invitație e tot o resetare (vezi comentariul clasei), deci rândul apare și la o
                  parolă uitată. Nu strică: termenii se acceptă „prin utilizare” oricum (cap. 2). */}
              <LegalNotice text={strings.legal.setPasswordNotice} />
            </form>
            <Link to="/parola-uitata" className="text-sm font-medium text-mark hover:underline">
              {t.requestNew}
            </Link>
          </>
        )}
      </div>
    </PublicShell>
  );
}
