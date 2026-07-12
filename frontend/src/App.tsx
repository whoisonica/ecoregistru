import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/auth/AuthContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { Layout } from "@/components/Layout";
import { LoginPage } from "@/pages/LoginPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { MovementsPage } from "@/pages/MovementsPage";
import { SettingsPage } from "@/pages/SettingsPage";

/** Placeholder for sections built in later slices (B1+). */
function ComingSoon({ title }: { title: string }) {
  return (
    <div>
      <h1 className="text-2xl font-bold">{title}</h1>
      <div className="mt-6 rounded-xl border border-dashed border-gray-300 bg-white p-8 text-center text-gray-400">
        În curând.
      </div>
    </div>
  );
}

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
          <Route path="/evidente" element={<AppShell><ComingSoon title="Evidențe" /></AppShell>} />
          <Route path="/parteneri" element={<AppShell><ComingSoon title="Parteneri" /></AppShell>} />
          <Route path="/termene" element={<AppShell><ComingSoon title="Termene" /></AppShell>} />
          <Route path="/dosar-control" element={<AppShell><ComingSoon title="Dosar de control" /></AppShell>} />
          <Route path="/setari" element={<AppShell><SettingsPage /></AppShell>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
