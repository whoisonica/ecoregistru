import { useState } from "react";
import { Radio } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany, useUpdateSiatdSettings } from "@/hooks/useCompanies";
import { useNaturalPersons } from "@/hooks/useNaturalPersons";
import type { Company, SiatdModule } from "@/lib/types";
import { SIATD_MODULES, siatdPayload, type SiatdDraftRow } from "@/lib/siatd";
import { apiErrorMessage } from "@/lib/api";
import { formatDate } from "@/lib/dates";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { DateInput } from "@/components/ui/date-input";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { useToast } from "@/components/ui/toast";

const t = strings.settings.siatd;

type Draft = Partial<Record<SiatdModule, SiatdDraftRow>>;

function draftOf(enrolled: Partial<Record<SiatdModule, string>> | undefined): Draft {
  const draft: Draft = {};
  for (const m of SIATD_MODULES) draft[m] = { on: Boolean(enrolled?.[m]), from: enrolled?.[m] ?? "" };
  return draft;
}

/**
 * F6a — modulele SIATD în care e înrolată firma, fiecare cu data înrolării. Le bifează doar adminul firmei (ca prețurile):
 * data o știe doar firma, iar de ea atârnă termenele pe care le vede toată lumea. Ceilalți le văd în citire.
 *
 * <p>Deasupra modulului municipal, la un depozit care cumpără de la persoane fizice, obligația din OUG 196/2005 — nu se
 * bifează singur, fiindcă data înrolării n-o știe decât firma.
 */
export function SiatdSettingsSection() {
  const { data: company } = useCurrentCompany();
  if (!company) return null;
  // Ciorna pornește din ce e salvat; o altă firmă sau o salvare din altă parte o reface de la zero.
  return <SiatdSettingsCard key={`${company.id}:${JSON.stringify(company.siatdEnrolledFrom ?? {})}`} company={company} />;
}

function SiatdSettingsCard({ company }: { company: Company }) {
  const { user } = useAuth();
  const { data: persons } = useNaturalPersons();
  const updateMut = useUpdateSiatdSettings();
  const { notify } = useToast();
  const [draft, setDraft] = useState<Draft>(() => draftOf(company.siatdEnrolledFrom));
  const [missing, setMissing] = useState<SiatdModule[]>([]);

  const isAdmin = user?.role === "ADMIN";
  const buysFromPeople = (persons?.length ?? 0) > 0;
  const enrolled = SIATD_MODULES.filter((m) => company.siatdEnrolledFrom?.[m]);

  function set(module: SiatdModule, row: Partial<SiatdDraftRow>) {
    setDraft((d) => ({ ...d, [module]: { ...(d[module] ?? { on: false, from: "" }), ...row } }));
    setMissing((m) => m.filter((x) => x !== module));
  }

  async function save() {
    const payload = siatdPayload(draft);
    if (!payload.ok) {
      setMissing(payload.missing);
      return;
    }
    try {
      await updateMut.mutateAsync(payload.enrolledFrom);
      notify(t.saved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  return (
    <section id="siatd" className="scroll-mt-20">
      <Card>
        <CardHeader
          title={
            <span className="flex items-center gap-2">
              <Radio className="h-4 w-4 text-content-subtle" aria-hidden />
              {t.title}
            </span>
          }
          description={t.subtitle}
        />
        {isAdmin ? (
          <div className="mt-4 space-y-3">
            {SIATD_MODULES.map((m) => {
              const row = draft[m] ?? { on: false, from: "" };
              const errorId = `siatd-${m}-error`;
              const error = missing.includes(m) ? t.dateRequired : undefined;
              return (
                <div key={m}>
                  {m === "MUNICIPAL" && buysFromPeople && (
                    <p className="mb-2 text-sm text-state-warn-text" data-testid="siatd-municipal-duty">
                      {t.municipalDuty}
                    </p>
                  )}
                  <div className="grid gap-3 sm:grid-cols-[1fr_12rem] sm:items-start">
                    <Switch
                      id={`siatd-${m}`}
                      checked={row.on}
                      onChange={(on) => set(m, { on })}
                      label={t.modules[m]}
                      disabled={updateMut.isPending}
                    />
                    {row.on && (
                      <div>
                        <Label htmlFor={`siatd-${m}-from`} required>
                          {t.enrolledFrom}
                        </Label>
                        <DateInput
                          id={`siatd-${m}-from`}
                          value={row.from}
                          onChange={(e) => set(m, { from: e.target.value })}
                          disabled={updateMut.isPending}
                          {...invalidProps(errorId, error)}
                        />
                        <FieldError id={errorId} message={error} />
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
            <p className="text-sm text-content-muted">{t.enrolledFromHint}</p>
            <Button onClick={save} disabled={updateMut.isPending}>
              {t.save}
            </Button>
          </div>
        ) : (
          <div className="mt-4 text-sm text-content">
            {enrolled.length === 0 ? (
              <p>{t.none}</p>
            ) : (
              <ul className="space-y-1">
                {enrolled.map((m) => (
                  <li key={m}>
                    {t.modules[m]} — {t.enrolledFrom.toLowerCase()} {formatDate(company.siatdEnrolledFrom?.[m] ?? "")}
                  </li>
                ))}
              </ul>
            )}
            <p className="mt-3 text-content-muted">{t.onlyAdmin}</p>
          </div>
        )}
      </Card>
    </section>
  );
}
