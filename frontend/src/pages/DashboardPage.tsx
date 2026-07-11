import { useAuth } from "@/auth/AuthContext";
import { strings } from "@/lib/strings";

export function DashboardPage() {
  const { user } = useAuth();
  return (
    <div>
      <h1 className="text-2xl font-bold">{strings.dashboard.title}</h1>
      <p className="mt-1 text-gray-500">
        {strings.dashboard.welcome}, <span className="font-medium">{user?.email}</span>.
      </p>
      <div className="mt-6 rounded-xl border border-dashed border-gray-300 bg-white p-8 text-center text-gray-400">
        {strings.dashboard.placeholder}
      </div>
    </div>
  );
}
