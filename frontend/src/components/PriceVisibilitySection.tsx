import { Building2, Eye, Lock, Users } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany, useUpdatePriceVisibility } from "@/hooks/useCompanies";
import type { PriceVisibility } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Card, CardHeader } from "@/components/ui/card";
import { ChoiceCards, type ChoiceOption } from "@/components/ui/choice-cards";
import { useToast } from "@/components/ui/toast";

const t = strings.settings.prices;

/**
 * Cine vede prețurile depozitului (D1.8). Alege doar adminul firmei (proprietarul, 15.09.2026):
 * consultantul și platforma n-au alegerea, fiindcă și-ar putea deschide singuri prețurile ascunse.
 * Serverul refuză la fel.
 *
 * <p>Trei variante, deci trei carduri, nu o listă derulantă (stilul „Prietenos”): textele lor sunt
 * lungi fiindcă numesc și suportul WasteHouse, iar într-un select se tăiau pe telefon.
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

  const options: ChoiceOption<PriceVisibility>[] = [
    { value: "COMPANY", label: t.choice.COMPANY.label, description: t.choice.COMPANY.description, icon: <Users className="h-5 w-5" /> },
    { value: "NO_CONSULTANT", label: t.choice.NO_CONSULTANT.label, description: t.choice.NO_CONSULTANT.description, icon: <Building2 className="h-5 w-5" /> },
    { value: "ADMIN_ONLY", label: t.choice.ADMIN_ONLY.label, description: t.choice.ADMIN_ONLY.description, icon: <Lock className="h-5 w-5" /> },
  ];

  async function change(value: PriceVisibility) {
    if (value === company?.priceVisibility) return;
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
        <div className="mt-4">
          <p id="price-visibility-label" className="mb-2 text-sm font-bold text-content-strong">
            {t.label}
          </p>
          {isAdmin ? (
            <ChoiceCards
              name="price-visibility"
              aria-labelledby="price-visibility-label"
              value={company.priceVisibility}
              onChange={change}
              options={options}
              columns={3}
              disabled={updateMut.isPending}
            />
          ) : (
            <p id="price-visibility" className="text-sm text-content">
              {t.options[company.priceVisibility]}
            </p>
          )}
          <p className="mt-3 text-sm text-content-muted">
            {company.pricesVisible ? t.youSee : t.youDontSee}
            {!isAdmin && ` ${t.onlyAdmin}`}
          </p>
        </div>
      </Card>
    </section>
  );
}
