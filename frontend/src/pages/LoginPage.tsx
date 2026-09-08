import { useState, type FormEvent } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { strings } from "@/lib/strings";
import { apiErrorMessage, LOGIN_EXPIRED_PARAM, REDIRECT_PARAM } from "@/lib/api";

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
      setError(apiErrorMessage(err, strings.login.genericError));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex h-full items-center justify-center p-4">
      <Card className="w-full max-w-sm p-8">
        <div className="mb-6 text-center">
          <div className="text-2xl font-bold text-brand">{strings.appName}</div>
          <div className="text-sm text-content-muted">{strings.tagline}</div>
        </div>
        <h1 className="mb-4 text-lg font-semibold">{strings.login.title}</h1>
        {expired && (
          <div className="mb-4 rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
            {strings.login.sessionExpired}
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            {/* `Label` cu `htmlFor`, ca peste tot în aplicație: eticheta scrisă de mână nu era
                legată de câmp, deci nici clicul pe ea nu focaliza, nici cititorul de ecran nu
                știa ce se cere. */}
            <Label htmlFor="login-email">{strings.login.email}</Label>
            <Input
              id="login-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              autoFocus
            />
          </div>
          <div>
            <Label htmlFor="login-password">{strings.login.password}</Label>
            <Input
              id="login-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
          </div>
          {error && (
            <div role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}
          <Button type="submit" className="w-full" loading={loading}>
            {loading ? strings.login.loading : strings.login.submit}
          </Button>
        </form>
        <p className="mt-4 text-center text-sm">
          <Link to="/parola-uitata" className="text-brand hover:underline">
            {strings.login.forgotPassword}
          </Link>
        </p>
        {/* The register is closed: there is no sign-up, only a request support acts on. */}
        <p className="mt-2 text-center text-sm">
          <Link to="/cerere-cont" className="text-brand hover:underline">
            {strings.accountRequest.linkFromLogin}
          </Link>
        </p>
      </Card>
    </div>
  );
}
