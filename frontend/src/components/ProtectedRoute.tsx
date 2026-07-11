import { Navigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { strings } from "@/lib/strings";
import type { ReactNode } from "react";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="flex h-full items-center justify-center text-gray-500">{strings.common.loading}</div>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
