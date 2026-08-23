import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { MailCheck } from "lucide-react";
import { api, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
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
    <div className="flex h-full items-center justify-center p-4">
      <div className="w-full max-w-sm rounded-xl border border-gray-200 bg-white p-8 shadow-sm">
        <div className="mb-6 text-center">
          <div className="text-2xl font-bold text-brand">{strings.appName}</div>
          <div className="text-sm text-gray-500">{strings.tagline}</div>
        </div>

        {sent ? (
          <div className="space-y-4 text-center">
            <MailCheck className="mx-auto h-10 w-10 text-emerald-600" />
            <p className="text-sm text-gray-600">{t.sent}</p>
            <p className="text-xs text-gray-500">{t.sentHint}</p>
            <Link to="/login" className="block text-sm text-blue-600 hover:underline">
              {t.backToLogin}
            </Link>
          </div>
        ) : (
          <>
            <h1 className="mb-1 text-lg font-semibold">{t.title}</h1>
            <p className="mb-4 text-sm text-gray-500">{t.subtitle}</p>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <Label htmlFor="fp-email">{t.email}</Label>
                <Input
                  id="fp-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </div>
              {error && (
                <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
              )}
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? t.sending : t.submit}
              </Button>
            </form>
            <p className="mt-4 text-center text-sm">
              <Link to="/login" className="text-blue-600 hover:underline">
                {t.backToLogin}
              </Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
}
