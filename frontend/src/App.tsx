import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import { AuthProvider } from "@/auth/AuthContext";
import { ProtectedRoute, RequireTenant } from "@/components/ProtectedRoute";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { Layout } from "@/components/Layout";
import { LoginPage } from "@/pages/LoginPage";
import { AccountRequestPage } from "@/pages/AccountRequestPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { ResetPasswordPage } from "@/pages/ResetPasswordPage";
import { TermsPage } from "@/pages/TermsPage";
import { PrivacyPage } from "@/pages/PrivacyPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { MovementsPage } from "@/pages/MovementsPage";
import { EvidencesPage } from "@/pages/EvidencesPage";
import { PartnersPage } from "@/pages/PartnersPage";
import { DeadlinesPage } from "@/pages/DeadlinesPage";
import { AuditFilePage } from "@/pages/AuditFilePage";
import { PackagingPage } from "@/pages/PackagingPage";
import { ClientsPage } from "@/pages/ClientsPage";
import { SettingsPage } from "@/pages/SettingsPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

function AppShell({
  children,
  /**
   * Ecranul e al unei firme anume, deci n-are ce randa până când administratorul de platformă
   * alege una. Implicit `true`: aproape toate sunt. Vezi `RequireTenant`.
   */
  needsTenant = true,
}: {
  children: React.ReactNode;
  needsTenant?: boolean;
}) {
  const location = useLocation();
  return (
    <ProtectedRoute>
      <Layout>
        {/* Plasa e **înăuntrul** lui `Layout`: o excepție într-o pagină lasă în picioare antetul
            și meniul, deci se poate merge în altă parte fără reîncărcare. Cheia e adresa, ca
            plecarea de pe ecranul căzut să șteargă mesajul. */}
        <ErrorBoundary resetKey={location.pathname}>
          {needsTenant ? <RequireTenant>{children}</RequireTenant> : children}
        </ErrorBoundary>
      </Layout>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          {/* Public, and deliberately so: the register is closed, so the intake form is the
              only way in. It creates a request, not an account. */}
          <Route path="/cerere-cont" element={<AccountRequestPage />} />
          {/* Where the mail lands. EmailService builds these two URLs from FRONTEND_BASE_URL,
              so the paths are a contract with the backend, not a choice of the router. */}
          <Route path="/parola-uitata" element={<ForgotPasswordPage />} />
          <Route path="/reseteaza-parola" element={<ResetPasswordPage />} />
          {/* Publice și **fără sesiune**, dinadins: un consultant cere politica înainte de demo,
              adică înainte să aibă cont. Un document pe care trebuie să te autentifici ca să-l
              citești nu e publicat. Adresele sunt cele scrise în documentele din `juridic/`. */}
          <Route path="/termeni" element={<TermsPage />} />
          <Route path="/confidentialitate" element={<PrivacyPage />} />
          <Route path="/" element={<AppShell><DashboardPage /></AppShell>} />
          <Route path="/miscari" element={<AppShell><MovementsPage /></AppShell>} />
          <Route path="/evidente" element={<AppShell><EvidencesPage /></AppShell>} />
          <Route path="/parteneri" element={<AppShell><PartnersPage /></AppShell>} />
          <Route path="/termene" element={<AppShell><DeadlinesPage /></AppShell>} />
          <Route path="/ambalaje" element={<AppShell><PackagingPage /></AppShell>} />
          <Route path="/dosar-control" element={<AppShell><AuditFilePage /></AppShell>} />
          {/* Singurul ecran de sub `AppShell` care **nu** e al unei firme: e chiar cel din care
              se aleg și se administrează. */}
          <Route
            path="/clienti"
            element={
              <AppShell needsTenant={false}>
                <ClientsPage />
              </AppShell>
            }
          />
          <Route path="/setari" element={<AppShell><SettingsPage /></AppShell>} />
          {/* Fără ruta asta, o adresă greșită nu randa nimic: pagină albă, fără meniu și fără
              mesaj, adică o aplicație care pare căzută pentru o literă în plus. */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
