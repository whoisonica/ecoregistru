import { Link } from "react-router-dom";
import { AlertTriangle } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useBillingAccess } from "@/hooks/useBillingAccess";
import { canManage } from "@/lib/roles";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { cn } from "@/lib/utils";

const t = strings.billingBanner;

/**
 * F4 — deasupra oricărui ecran, pentru orice rol: factura restantă, doar-citirea, abonamentul oprit.
 *
 * <p>Operatorul vede bannerul fiindcă lui îi dispar butoanele de scriere, dar nu și linkul: plata o face
 * administratorul. Platforma nu-l vede: ea lucrează pe firmele tuturor și n-are ce plăti.
 */
export function BillingBanner() {
  const { user } = useAuth();
  const { data: access } = useBillingAccess();
  if (!access?.status || user?.role === "PLATFORM_ADMIN") return null;

  let text: string | null = null;
  let danger = false;
  switch (access.status) {
    case "PAST_DUE":
      text = access.readOnlyOn ? t.pastDueReadOnlyOn.replace("{date}", formatDate(access.readOnlyOn)) : t.pastDue;
      break;
    case "READ_ONLY":
      text = t.readOnly;
      danger = true;
      break;
    case "CANCELLED":
      text = t.cancelled;
      danger = true;
      break;
  }
  if (!text) return null;

  return (
    <div
      role="status"
      className={cn(
        "mb-4 flex flex-wrap items-center gap-x-3 gap-y-1 rounded-md border px-4 py-3 text-sm",
        danger ? "border-red-300 bg-red-50 text-red-900" : "border-amber-300 bg-amber-50 text-amber-900"
      )}
    >
      <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden />
      <span className="min-w-0 flex-1">{text}</span>
      {canManage(user?.role) ? (
        <Link to="/abonament" className="font-medium underline">
          {t.action}
        </Link>
      ) : (
        <span className="text-xs opacity-80">{t.askAdmin}</span>
      )}
    </div>
  );
}
