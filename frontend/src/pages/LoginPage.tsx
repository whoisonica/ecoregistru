import { useState, type FormEvent } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/ui/password-input";
import { Label } from "@/components/ui/label";
import { strings } from "@/lib/strings";
import { CornerLink, PosterTile, PublicError, PublicShell, publicButtonClass } from "@/components/PublicShell";
import { apiErrorMessage, LOGIN_EXPIRED_PARAM, REDIRECT_PARAM } from "@/lib/api";
import { cn } from "@/lib/utils";

const t = strings.login;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  // Set by the 401 interceptor when it had to end a session. Navigating away drops it, so the
  // message never outlives the eviction that caused it.
  const [searchParams] = useSearchParams();
  const expired = searchParams.get(LOGIN_EXPIRED_PARAM) === "1";
  /**
   * Pagina cerută înainte de autentificare, dusă până aici de `ProtectedRoute`.
   *
   * <p>Se acceptă **numai** o cale internă care începe cu un singur `/`. `//alt-domeniu.ro` e o
   * adresă absolută cu schema moștenită, deci ar fi o redirectare deschisă — genul de lucru pe
   * care o pagină de login nu-l oferă nimănui.
   */
  const requested = searchParams.get(REDIRECT_PARAM);
  const redirectTo =
    requested && requested.startsWith("/") && !requested.startsWith("//") ? requested : "/";

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err, t.genericError));
    } finally {
      setLoading(false);
    }
  }

  return (
    // Direcția „Poster” (16.09.2026): verdele cu promisiunea produsului în stânga, formularul
    // aerisit în dreapta. Tile-urile spun ce găsești înăuntru — ecrane care chiar există.
    <PublicShell
      headline={t.posterHeadline}
      accent={t.posterAccent}
      lede={t.posterLede}
      aside={
        <div className="hidden flex-wrap gap-3.5 lg:flex">
          <PosterTile kicker={t.tile1Kicker} title={t.tile1Title} note={t.tile1Note} led />
          <PosterTile kicker={t.tile2Kicker} title={t.tile2Title} note={t.tile2Note} />
          <PosterTile kicker={t.tile3Kicker} title={t.tile3Title} note={t.tile3Note} />
        </div>
      }
      corner={<CornerLink prompt={t.noAccount} label={t.requestAccount} to="/cerere-cont" />}
    >
      <div className="mx-auto flex w-full max-w-[400px] flex-col gap-8">
        <div className="flex flex-col gap-2">
          <h1 className="text-[32px] font-semibold leading-10 tracking-[-0.015em] text-content">{t.title}</h1>
          <p className="text-content-muted">{t.subtitle}</p>
        </div>
        {expired && (
          <div className="flex items-start gap-2 rounded-md border border-state-warn px-3 py-2 text-sm text-state-warn-text">
            <span aria-hidden className="mt-2 inline-block h-2 w-2 shrink-0 rounded-sm bg-state-warn" />
            {t.sessionExpired}
          </div>
        )}
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <div>
            {/* `Label` cu `htmlFor`, ca peste tot în aplicație: eticheta scrisă de mână nu era
                legată de câmp, deci nici clicul pe ea nu focaliza, nici cititorul de ecran nu
                știa ce se cere. */}
            <Label htmlFor="login-email" className="text-[0.8125rem] text-content-strong">
              {t.email}
            </Label>
            <Input
              id="login-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              autoFocus
              className="h-12 px-4 text-base"
            />
          </div>
          <div>
            <div className="flex items-baseline justify-between">
              <Label htmlFor="login-password" className="text-[0.8125rem] text-content-strong">
                {t.password}
              </Label>
              <Link to="/parola-uitata" className="mb-1 text-[0.8125rem] font-medium text-mark hover:underline">
                {t.forgotPassword}
              </Link>
            </div>
            <PasswordInput
              id="login-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              className="h-12 px-4 text-base"
            />
          </div>
          {error && <PublicError>{error}</PublicError>}
          <Button type="submit" size="lg" className={cn("w-full", publicButtonClass)} loading={loading}>
            {loading ? t.loading : t.submit}
          </Button>
        </form>
        {/* The register is closed: there is no sign-up, only a request support acts on. */}
        <p className="text-sm text-content-muted lg:hidden">
          <Link to="/cerere-cont" className="font-medium text-mark hover:underline">
            {strings.accountRequest.linkFromLogin}
          </Link>
        </p>
      </div>
    </PublicShell>
  );
}
