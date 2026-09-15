import { Eye } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany, useUpdatePriceVisibility } from "@/hooks/useCompanies";
import type { PriceVisibility } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Card, CardHeader } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { useToast } from "@/components/ui/toast";

const t = strings.settings.prices;
const OPTIONS: PriceVisibility[] = ["COMPANY", "NO_CONSULTANT", "ADMIN_ONLY"];

/**
 * Cine vede prețurile depozitului (D1.8). Alege doar adminul firmei (proprietarul, 15.09.2026):
 * consultantul și platforma n-au select, fiindcă și-ar putea deschide singuri prețurile ascunse.
 * Serverul refuză la fel.
 *
 * <p>Ceilalți văd setarea în citire și dacă ei înșiși văd prețurile: `pricesVisible` vine de pe
 * server, ca regula să nu fie scrisă a doua oară aici.
 */
export function PriceVisibilitySection() {
  const { user } = useAuth();
  const { data: company } = useCurrentCompany();
  const updateMut = useUpdatePriceVisibility();
  const { notify } = useToast();

  if (!company?.priceVisibility) return null;
  const isAdmin = user?.role === "ADMIN";

  async function change(value: PriceVisibility) {
    try {
      await updateMut.mutateAsync(value);
      notify(t.saved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  return (
    <section id="preturi" className="mt-8 scroll-mt-20">
      <Card>
        <CardHeader
          title={
            <span className="flex items-center gap-2">
              <Eye className="h-4 w-4 text-content-subtle" aria-hidden />
              {t.title}
            </span>
          }
          description={t.subtitle}
        />
        <div className="mt-4 max-w-xl">
          <Label htmlFor="price-visibility">{t.label}</Label>
          {isAdmin ? (
            <div className="mt-1">
              <Select
                id="price-visibility"
                value={company.priceVisibility}
                disabled={updateMut.isPending}
                onChange={(e) => change(e.target.value as PriceVisibility)}
              >
                {OPTIONS.map((option) => (
                  <option key={option} value={option}>
                    {t.options[option]}
                  </option>
                ))}
              </Select>
            </div>
          ) : (
            <p id="price-visibility" className="mt-1 text-sm text-content">
              {t.options[company.priceVisibility]}
            </p>
          )}
          <p className="mt-2 text-sm text-content-muted">
            {company.pricesVisible ? t.youSee : t.youDontSee}
            {!isAdmin && ` ${t.onlyAdmin}`}
          </p>
        </div>
      </Card>
    </section>
  );
}
