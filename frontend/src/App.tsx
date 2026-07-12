import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/auth/AuthContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { Layout } from "@/components/Layout";
import { LoginPage } from "@/pages/LoginPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { MovementsPage } from "@/pages/MovementsPage";
import { EvidencesPage } from "@/pages/EvidencesPage";
import { PartnersPage } from "@/pages/PartnersPage";
import { DeadlinesPage } from "@/pages/DeadlinesPage";
import { AuditFilePage } from "@/pages/AuditFilePage";
import { SettingsPage } from "@/pages/SettingsPage";

function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <Layout>{children}</Layout>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<AppShell><DashboardPage /></AppShell>} />
          <Route path="/miscari" element={<AppShell><MovementsPage /></AppShell>} />
          <Route path="/evidente" element={<AppShell><EvidencesPage /></AppShell>} />
          <Route path="/parteneri" element={<AppShell><PartnersPage /></AppShell>} />
          <Route path="/termene" element={<AppShell><DeadlinesPage /></AppShell>} />
          <Route path="/dosar-control" element={<AppShell><AuditFilePage /></AppShell>} />
          <Route path="/setari" element={<AppShell><SettingsPage /></AppShell>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
