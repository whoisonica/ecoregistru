import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { api, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
    <div className="flex h-full items-center justify-center p-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-8 shadow-sm">
        <div className="mb-6 text-center">
          <div className="text-2xl font-bold text-brand">{strings.appName}</div>
          <div className="text-sm text-gray-500">{strings.tagline}</div>
        </div>

        {done ? (
          <div className="space-y-4 text-center">
            <CheckCircle2 className="mx-auto h-10 w-10 text-emerald-600" />
            <p className="text-sm text-gray-600">{t.done}</p>
            <Button className="w-full" onClick={() => navigate("/login")}>
              {t.toLogin}
            </Button>
          </div>
        ) : !code ? (
          // A bare /reseteaza-parola with nothing after it: say what is missing rather than
          // showing a form that cannot succeed.
          <div className="space-y-4">
            <h1 className="text-lg font-semibold">{t.title}</h1>
            <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{t.missingCode}</p>
            <Link to="/parola-uitata" className="block text-center text-sm text-blue-600 hover:underline">
              {t.requestNew}
            </Link>
          </div>
        ) : (
          <>
            <h1 className="mb-1 text-lg font-semibold">{t.title}</h1>
            <p className="mb-4 text-sm text-gray-500">{t.subtitle}</p>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <Label htmlFor="rp-pass">{t.password}</Label>
                <Input
                  id="rp-pass"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                />
                <p className="mt-1 text-xs text-gray-500">{t.rules}</p>
              </div>
              <div>
                <Label htmlFor="rp-confirm">{t.confirmPassword}</Label>
                <Input
                  id="rp-confirm"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  autoComplete="new-password"
                />
              </div>
              {error && (
                <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
              )}
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? t.saving : t.submit}
              </Button>
            </form>
            <p className="mt-4 text-center text-sm">
              <Link to="/parola-uitata" className="text-blue-600 hover:underline">
                {t.requestNew}
              </Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
}
