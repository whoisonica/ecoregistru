import { useState, type FormEvent } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { strings } from "@/lib/strings";
import { apiErrorMessage, LOGIN_EXPIRED_PARAM } from "@/lib/api";

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

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate("/");
    } catch (err) {
      setError(apiErrorMessage(err, strings.login.genericError));
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
        <h1 className="mb-4 text-lg font-semibold">{strings.login.title}</h1>
        {expired && (
          <div className="mb-4 rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
            {strings.login.sessionExpired}
          </div>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">{strings.login.email}</label>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">{strings.login.password}</label>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
          </div>
          {error && <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? strings.login.loading : strings.login.submit}
          </Button>
        </form>
        <p className="mt-4 text-center text-sm">
          <Link to="/parola-uitata" className="text-blue-600 hover:underline">
            {strings.login.forgotPassword}
          </Link>
        </p>
        {/* The register is closed: there is no sign-up, only a request support acts on. */}
        <p className="mt-2 text-center text-sm">
          <Link to="/cerere-cont" className="text-blue-600 hover:underline">
            {strings.accountRequest.linkFromLogin}
          </Link>
        </p>
      </div>
    </div>
  );
}
