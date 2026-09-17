import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Building2, Factory, Recycle } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { isMultiCompany } from "@/lib/roles";
import { useAccountRequests } from "@/hooks/useAccountRequests";
import { useOnboardClient } from "@/hooks/useCompanies";
import { useFounderCount, useSubscriptionPreview } from "@/hooks/useSubscriptions";
import type { AccountRequest, CompanyType, InvoicePreview, OnboardClientResult } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { isValidCui } from "@/lib/cui";
import { COUNTIES, fgoCounty } from "@/lib/counties";
import { fold, formatDate } from "@/lib/utils";
import { CompanyProfileFields, emptyCompanyProfile, type CompanyProfileValue } from "@/components/CompanyProfileFields";
import { CuiField } from "@/components/AnafLookup";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ChoiceCards } from "@/components/ui/choice-cards";
import { DateInput } from "@/components/ui/date-input";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { FormStepRail } from "@/components/ui/form-steps";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import { Select } from "@/components/ui/select";
import { Stepper } from "@/components/ui/stepper";

const t = strings.newClient;
const PLANS = ["GENERATOR", "GENERATOR_PACKAGING", "FULL_SERVICE"] as const;
const TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];
const TYPE_ICONS = { GENERATOR: Factory, COLLECTOR: Recycle, BOTH: Building2 } as const;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { maximumFractionDigits: 2 })} lei`;
}

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/**
 * F-C — „Client nou” pe o pagină, în patru pași (macheta C din todo-clienti-abonamente.md): firma, ce face, abonamentul,
 * administratorul. Înainte, aprobarea unei cereri făcea doar firma; abonamentul și invitația erau în alte două locuri,
 * și nimic nu arăta un client rămas pe jumătate. Acum totul pleacă într-o singură cerere și se salvează împreună.
 *
 * <p>Pornește gol din „Client nou” (N) sau cu răspunsurile unei cereri (`?cerere=`). Consultantul n-are pasul de
 * abonament: firmele lui le plătește cabinetul. Firma se editează după aceea tot din lista de clienți.
 */
export function NewClientPage() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const requestId = params.get("cerere");
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";
  const requests = useAccountRequests(isPlatformAdmin && !!requestId);

  if (!isMultiCompany(user?.role)) {
    return <PageHeader title={t.title} description={strings.clients.onlyPlatformAdmin} />;
  }
  if (requestId && isPlatformAdmin && requests.isLoading) {
    return <PageHeader title={t.title} description={strings.common.loading} />;
  }
  const request = requestId ? (requests.data ?? []).find((r) => r.id === requestId && r.status === "NEW") : undefined;

  // Cheia repornește formularul când se trece de la o cerere la „Alt client nou”, fără un efect care să-l golească. E
  // adresa, nu cererea găsită: după salvare cererea nu mai e nouă, iar o cheie pe ea ar șterge rezumatul de final.
  return (
    <NewClientForm
      key={requestId ?? "nou"}
      request={request ?? null}
      requestMissing={!!requestId && !request}
      withSubscription={isPlatformAdmin}
    />
  );
}

function NewClientForm({
  request,
  requestMissing,
  withSubscription,
}: {
  request: AccountRequest | null;
  requestMissing: boolean;
  withSubscription: boolean;
}) {
  const navigate = useNavigate();
  const onboard = useOnboardClient();
  const { data: founderCount } = useFounderCount(withSubscription);

  const steps = withSubscription ? [t.step1, t.step2, t.step3, t.step4] : [t.step1, t.step2, t.step4];
  const subscriptionStep = withSubscription ? 2 : -1;
  const adminStep = steps.length - 1;
  const [step, setStep] = useState(0);
  const [tried, setTried] = useState<Set<number>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<OnboardClientResult | null>(null);

  // Pasul 1 — firma. ANAF completează doar ce e gol.
  const [cui, setCui] = useState(request?.cui ?? "");
  const [name, setName] = useState(request?.companyName ?? "");
  const [tradeRegisterNumber, setTradeRegisterNumber] = useState("");
  const [caenCode, setCaenCode] = useState(request?.caenCode ?? "");
  const [address, setAddress] = useState(request?.companyAddress ?? "");
  const [county, setCounty] = useState("");
  const [anafCounty, setAnafCounty] = useState<string | null>(null);
  const [city, setCity] = useState("");

  // Pasul 2 — ce face.
  const [type, setType] = useState<CompanyType>(request?.companyType ?? "GENERATOR");
  const [profile, setProfile] = useState<CompanyProfileValue>(
    request
      ? {
          ...emptyCompanyProfile,
          authorizedOperationCodes: request.operationCodes ?? [],
          marketRoles: request.marketRoles ?? [],
          transportMeans: request.transportMeans ?? "",
          transportLicenseNumber: request.transportLicenseNumber ?? "",
          transportLicenseExpiry: request.transportLicenseExpiry ?? "",
        }
      : emptyCompanyProfile
  );

  // Pasul 3 — abonamentul.
  const [subscriptionOn, setSubscriptionOn] = useState(withSubscription);
  const [plan, setPlan] = useState<(typeof PLANS)[number]>(
    (request?.marketRoles ?? []).length > 0 ? "GENERATOR_PACKAGING" : "GENERATOR"
  );
  const [startedAt, setStartedAt] = useState(todayIso());
  const [founder, setFounder] = useState<"no" | "yes">("no");
  const [billingEmail, setBillingEmail] = useState(request?.contactEmail ?? "");
  const [otherAddress, setOtherAddress] = useState<"same" | "other">("same");
  const [billingCounty, setBillingCounty] = useState("");
  const [billingCity, setBillingCity] = useState("");
  const [billingAddress, setBillingAddress] = useState("");

  // Pasul 4 — administratorul.
  const [adminNow, setAdminNow] = useState<"now" | "later">("now");
  const [adminEmail, setAdminEmail] = useState(request?.contactEmail ?? "");
  const [adminFirstName, setAdminFirstName] = useState("");
  const [adminLastName, setAdminLastName] = useState("");

  const prices = {
    GENERATOR: useSubscriptionPreview("GENERATOR", false, startedAt, withSubscription),
    GENERATOR_PACKAGING: useSubscriptionPreview("GENERATOR_PACKAGING", false, startedAt, withSubscription),
    FULL_SERVICE: useSubscriptionPreview("FULL_SERVICE", false, startedAt, withSubscription),
  };
  const preview = useSubscriptionPreview(plan, founder === "yes", startedAt, withSubscription && subscriptionOn);

  const invoiceCounty = otherAddress === "same" ? county : billingCounty;
  const invoiceCity = otherAddress === "same" ? city : billingCity;
  const invoiceAddress = otherAddress === "same" ? address : billingAddress;
  const checks = {
    cui: isValidCui(cui),
    address: !!invoiceCounty && !!invoiceCity.trim() && !!invoiceAddress.trim(),
    email: EMAIL.test(billingEmail.trim()),
  };

  const show = (i: number) => tried.has(i);
  const errors = {
    name: !name.trim() ? strings.common.requiredField : undefined,
    cui: !cui.trim() ? strings.common.requiredField : !isValidCui(cui) ? strings.common.cuiInvalid : undefined,
    adminEmail: adminNow === "now" && !EMAIL.test(adminEmail.trim()) ? t.emailInvalid : undefined,
  };
  const stepInvalid = [
    !!(errors.name || errors.cui),
    false,
    ...(withSubscription ? [subscriptionOn && !(checks.cui && checks.address && checks.email)] : []),
    !!errors.adminEmail,
  ];

  function goTo(next: number) {
    setError(null);
    setStep(next);
  }

  function advance(e?: FormEvent) {
    e?.preventDefault();
    setTried((prev) => new Set(prev).add(step));
    if (stepInvalid[step]) return;
    if (step < adminStep) {
      goTo(step + 1);
      return;
    }
    const firstBad = stepInvalid.findIndex(Boolean);
    if (firstBad >= 0) {
      setTried(new Set(steps.map((_, i) => i)));
      goTo(firstBad);
      return;
    }
    submit();
  }

  function skipSubscription() {
    setSubscriptionOn(false);
    goTo(adminStep);
  }

  async function submit() {
    setError(null);
    try {
      const result = await onboard.mutateAsync({
        accountRequestId: request?.id ?? null,
        company: {
          name: name.trim(),
          cui: cui.trim(),
          type,
          afmObligation: false,
          afmContributions: [],
          environmentalAuthNumber: request?.environmentalAuthNumber ?? null,
          environmentalAuthExpiry: request?.environmentalAuthExpiry ?? null,
          address: address.trim() || null,
          contactName: request?.contactName ?? null,
          contactEmail: request?.contactEmail ?? null,
          contactPhone: request?.contactPhone ?? null,
          contactRole: request?.contactRole ?? null,
          tradeRegisterNumber: tradeRegisterNumber.trim() || null,
          caenCode: caenCode.trim() || null,
          authorizedOperationCodes: profile.authorizedOperationCodes,
          marketRoles: profile.marketRoles,
          authorizedWasteCodeIds: profile.authorizedWasteCodes.map((w) => w.id),
          transportMeans: profile.transportMeans.trim() || null,
          transportLicenseNumber: profile.transportLicenseNumber.trim() || null,
          transportLicenseExpiry: profile.transportLicenseExpiry || null,
        },
        subscription:
          withSubscription && subscriptionOn
            ? {
                plan,
                founder: founder === "yes",
                startedAt,
                billingEmail: billingEmail.trim() || null,
                billingCounty: invoiceCounty || null,
                billingCity: invoiceCity.trim() || null,
                billingAddress: invoiceAddress.trim() || null,
              }
            : null,
        admin:
          adminNow === "now"
            ? {
                email: adminEmail.trim(),
                firstName: adminFirstName.trim() || null,
                lastName: adminLastName.trim() || null,
              }
            : null,
      });
      setDone(result);
    } catch (err) {
      setError(apiErrorMessage(err, t.createError));
    }
  }

  if (done) {
    return (
      <div>
        <PageHeader title={t.doneTitle.replace("{name}", done.company.name)} />
        <Card className="mt-6 max-w-2xl space-y-2 p-5 text-sm" data-testid="new-client-done">
          <p className="flex items-center gap-2 text-content">
            <span aria-hidden className="h-2 w-2 rounded-sm bg-state-ok" />
            <span className="font-mono">{done.company.cui}</span>
          </p>
          <p className="text-content">
            {done.plan && done.firstInvoice
              ? t.doneSubscription
                  .replace("{plan}", strings.subscriptions.plans[done.plan])
                  .replace("{total}", lei(done.firstInvoice.total))
                  .replace("{date}", formatDate(done.firstInvoice.from))
              : t.doneNoSubscription}
          </p>
          <p className="text-content">
            {done.invitedEmail ? t.doneInvited.replace("{email}", done.invitedEmail) : t.doneNotInvited}
          </p>
          <div className="flex flex-wrap gap-2 pt-3">
            <Button onClick={() => navigate("/clienti")}>{t.doneToClients}</Button>
            <Button variant="outline" onClick={() => navigate("/clienti/nou", { replace: true })}>
              {t.doneAnother}
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  const summaries = [
    [name.trim(), cui.trim()].filter(Boolean).join(" · "),
    strings.enums.companyType[type],
    ...(withSubscription ? [subscriptionOn ? strings.subscriptions.plans[plan] : t.stepSummaryNoSubscription] : []),
    adminNow === "now" ? adminEmail.trim() : t.stepSummaryLater,
  ];

  return (
    <div>
      <Link to="/clienti" className="text-xs font-semibold text-content-muted hover:text-content">
        {t.backToClients}
      </Link>
      <PageHeader
        className="mt-2"
        title={t.title}
        description={withSubscription ? t.subtitle : t.subtitleConsultant}
      />
      {requestMissing && <p className="mt-3 text-sm text-state-bad-text">{t.requestMissing}</p>}

      <div className="mt-6 md:grid md:grid-cols-[230px_minmax(0,1fr)] md:gap-8">
        <div className="hidden md:block">
          {request && (
            <p className="mb-3 text-xs text-content-muted">{t.fromRequest.replace("{name}", request.companyName)}</p>
          )}
          <FormStepRail
            className="md:sticky md:top-4"
            label={t.stepsLabel}
            current={step}
            onSelect={goTo}
            steps={steps.map((stepName, i) => ({
              name: stepName,
              summary: i < step || tried.has(i) ? summaries[i] : undefined,
              invalid: show(i) && stepInvalid[i],
            }))}
          />
        </div>

        <form onSubmit={advance} className="min-w-0 max-w-3xl space-y-5" noValidate>
          <Stepper className="md:hidden" steps={steps} current={step} />
          {request && step === 0 && <p className="text-sm text-content-muted">{t.fromRequestHint}</p>}

          {step === 0 && (
            <div className="space-y-4">
              <CuiField
                id="nc-cui"
                label={strings.clients.cui}
                required
                autoFocus
                value={cui}
                onChange={setCui}
                placeholder={strings.clients.cuiPlaceholder}
                invalid={invalidProps("nc-cui-error", show(0) ? errors.cui : undefined)}
                error={show(0) && <FieldError id="nc-cui-error" message={errors.cui} />}
                targets={[
                  { label: strings.partners.anafFieldName, current: name, pick: (f) => f.name, set: setName },
                  { label: strings.partners.anafFieldAddress, current: address, pick: (f) => f.address, set: setAddress },
                  {
                    label: strings.partners.anafFieldRegistry,
                    current: tradeRegisterNumber,
                    pick: (f) => f.tradeRegisterNumber,
                    set: setTradeRegisterNumber,
                  },
                  { label: strings.partners.anafFieldCaen, current: caenCode, pick: (f) => f.caenCode, set: setCaenCode },
                  {
                    label: t.county.toLowerCase(),
                    current: county,
                    pick: (f) => {
                      setAnafCounty(f.county);
                      return fgoCounty(f.county);
                    },
                    set: setCounty,
                  },
                  { label: t.city.toLowerCase(), current: city, pick: (f) => f.city, set: setCity },
                ]}
              />
              <div>
                <Label htmlFor="nc-name" required>
                  {strings.clients.name}
                </Label>
                <Input
                  id="nc-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder={strings.clients.namePlaceholder}
                  {...invalidProps("nc-name-error", show(0) ? errors.name : undefined)}
                />
                {show(0) && <FieldError id="nc-name-error" message={errors.name} />}
              </div>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <Label htmlFor="nc-reg">{strings.settings.company.tradeRegisterNumber}</Label>
                  <Input
                    id="nc-reg"
                    value={tradeRegisterNumber}
                    onChange={(e) => setTradeRegisterNumber(e.target.value)}
                    placeholder={strings.partners.tradeRegisterNumberPlaceholder}
                  />
                </div>
                <div>
                  <Label htmlFor="nc-caen">{strings.clients.caenCode}</Label>
                  <Input
                    id="nc-caen"
                    value={caenCode}
                    onChange={(e) => setCaenCode(e.target.value)}
                    placeholder={strings.clients.caenCodePlaceholder}
                  />
                </div>
              </div>
              <div>
                <Label htmlFor="nc-address">{strings.clients.address}</Label>
                <Input id="nc-address" value={address} onChange={(e) => setAddress(e.target.value)} />
              </div>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <Label htmlFor="nc-county">{t.county}</Label>
                  <Select id="nc-county" value={county} onChange={(e) => setCounty(e.target.value)}>
                    <option value="">{t.countyPlaceholder}</option>
                    {COUNTIES.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </Select>
                  {anafCounty && !county && !fgoCounty(anafCounty) ? (
                    <p className="mt-1 text-xs text-state-bad-text">
                      {t.countyNotMatched.replace("{county}", anafCounty)}
                    </p>
                  ) : (
                    <p className="mt-1 text-xs text-content-muted">{t.countyHint}</p>
                  )}
                </div>
                <div>
                  <Label htmlFor="nc-city">{t.city}</Label>
                  <Input id="nc-city" value={city} onChange={(e) => setCity(e.target.value)} />
                </div>
              </div>
            </div>
          )}

          {step === 1 && (
            <div className="space-y-5">
              <div>
                <span id="nc-type-label" className="mb-2 block text-xs font-medium text-content-muted">
                  {t.typeLabel}
                </span>
                <ChoiceCards
                  name="nc-type"
                  aria-labelledby="nc-type-label"
                  columns={3}
                  value={type}
                  onChange={setType}
                  options={TYPES.map((value) => {
                    const Icon = TYPE_ICONS[value];
                    return {
                      value,
                      label: strings.enums.companyType[value],
                      description: t.typeHint[value],
                      icon: <Icon className="h-5 w-5" />,
                    };
                  })}
                />
              </div>
              <CompanyProfileFields value={profile} onChange={setProfile} companyType={type} />
            </div>
          )}

          {step === subscriptionStep && (
            <div className="space-y-5">
              {!subscriptionOn ? (
                <div className="space-y-3 rounded-md border border-line bg-surface-muted p-4 text-sm">
                  <p className="text-content">{t.withoutSubscriptionOn}</p>
                  <Button type="button" variant="outline" size="sm" onClick={() => setSubscriptionOn(true)}>
                    {t.withSubscriptionAgain}
                  </Button>
                </div>
              ) : (
                <>
                  <div>
                    <span id="nc-plan-label" className="mb-2 block text-xs font-medium text-content-muted">
                      {t.planLabel}
                    </span>
                    <ChoiceCards
                      name="nc-plan"
                      aria-labelledby="nc-plan-label"
                      columns={3}
                      value={plan}
                      onChange={setPlan}
                      options={PLANS.map((value) => {
                        const p = prices[value].data;
                        const monthly = p?.monthlyInvoice.total;
                        const fee = p ? p.firstInvoice.total - p.monthlyInvoice.total : null;
                        return {
                          value,
                          label: (
                            <span className="block">
                              <span className="block">{strings.subscriptions.plans[value]}</span>
                              <span className="mt-1 block font-mono text-lg text-content">
                                {monthly != null ? monthly.toLocaleString("ro-RO") : "?"}{" "}
                                <span className="text-xs font-normal text-content-muted">{t.perMonth}</span>
                              </span>
                            </span>
                          ),
                          description: (
                            <>
                              {t.planDescription[value]}
                              <span className="mt-1 block text-content-subtle">
                                {fee == null ? "" : fee > 0 ? t.plusImplementation.replace("{fee}", lei(fee)) : t.noImplementation}
                              </span>
                            </>
                          ),
                        };
                      })}
                    />
                  </div>

                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <Label htmlFor="nc-start">{t.startedAt}</Label>
                      <DateInput id="nc-start" value={startedAt} onChange={(e) => setStartedAt(e.target.value)} />
                      <p className="mt-1 text-xs text-content-muted">{t.startedAtHint}</p>
                    </div>
                    <div>
                      <span id="nc-founder-label" className="mb-1 block text-xs font-medium text-content-muted">
                        {t.founderLabel}
                      </span>
                      <ChoiceCards
                        name="nc-founder"
                        aria-labelledby="nc-founder-label"
                        columns={2}
                        value={founder}
                        onChange={setFounder}
                        options={[
                          { value: "no", label: t.founderNo },
                          { value: "yes", label: t.founderYes },
                        ]}
                      />
                      {founderCount != null && (
                        <p className="mt-1 text-xs text-content-muted">
                          {t.founderCount.replace("{n}", String(founderCount))}
                        </p>
                      )}
                    </div>
                  </div>

                  <fieldset className="space-y-3 rounded-md border border-line p-4">
                    <legend className="px-1 text-sm font-semibold text-content">{t.billingTo}</legend>
                    <div>
                      <Label htmlFor="nc-billing-email" required>
                        {t.billingEmail}
                      </Label>
                      <Input
                        id="nc-billing-email"
                        type="email"
                        maxLength={100}
                        value={billingEmail}
                        onChange={(e) => setBillingEmail(e.target.value)}
                      />
                      <p className="mt-1 text-xs text-content-muted">{t.billingEmailHint}</p>
                    </div>
                    <ChoiceCards
                      name="nc-billing-address"
                      columns={2}
                      value={otherAddress}
                      onChange={setOtherAddress}
                      options={[
                        {
                          value: "same",
                          label: t.sameAddress,
                          // Adresa de la ANAF are deja județul și localitatea: nu se lipesc încă o dată.
                          description:
                            [address, city, county]
                              .map((x) => x.trim())
                              .filter((x, i) => x && (i === 0 || !fold(address).includes(fold(x))))
                              .join(", ") || "—",
                        },
                        { value: "other", label: t.otherAddress },
                      ]}
                    />
                    {otherAddress === "other" && (
                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <div>
                          <Label htmlFor="nc-billing-county">{t.county}</Label>
                          <Select
                            id="nc-billing-county"
                            value={billingCounty}
                            onChange={(e) => setBillingCounty(e.target.value)}
                          >
                            <option value="">{t.countyPlaceholder}</option>
                            {COUNTIES.map((c) => (
                              <option key={c} value={c}>
                                {c}
                              </option>
                            ))}
                          </Select>
                        </div>
                        <div>
                          <Label htmlFor="nc-billing-city">{t.city}</Label>
                          <Input
                            id="nc-billing-city"
                            maxLength={100}
                            value={billingCity}
                            onChange={(e) => setBillingCity(e.target.value)}
                          />
                        </div>
                        <div className="sm:col-span-2">
                          <Label htmlFor="nc-billing-address-line">{t.address}</Label>
                          <Input
                            id="nc-billing-address-line"
                            maxLength={500}
                            value={billingAddress}
                            onChange={(e) => setBillingAddress(e.target.value)}
                          />
                        </div>
                      </div>
                    )}
                  </fieldset>

                  <FirstInvoice invoice={preview.data?.firstInvoice} failed={preview.isError} />

                  <ul className="space-y-1 text-sm" data-testid="new-client-checks">
                    {(
                      [
                        [checks.cui, t.checkCui],
                        [checks.address, t.checkAddress],
                        [checks.email, t.checkEmail],
                      ] as const
                    ).map(([ok, label]) => (
                      <li key={label} data-ok={ok} className="flex items-center gap-2">
                        <span aria-hidden className={`h-2 w-2 rounded-sm ${ok ? "bg-state-ok" : "bg-state-bad"}`} />
                        <span className={ok ? "text-content" : "text-state-bad-text"}>{label}</span>
                      </li>
                    ))}
                  </ul>
                  {show(step) && stepInvalid[step] && <p className="text-sm text-state-bad-text">{t.checksFailed}</p>}
                </>
              )}
            </div>
          )}

          {step === adminStep && (
            <div className="space-y-4">
              <ChoiceCards
                name="nc-admin-when"
                columns={2}
                value={adminNow}
                onChange={setAdminNow}
                options={[
                  { value: "now", label: t.adminNow, description: t.adminHint },
                  { value: "later", label: t.adminLater, description: t.adminLaterHint },
                ]}
              />
              {adminNow === "now" && (
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div className="sm:col-span-2">
                    <Label htmlFor="nc-admin-email" required>
                      {t.adminEmail}
                    </Label>
                    <Input
                      id="nc-admin-email"
                      type="email"
                      value={adminEmail}
                      onChange={(e) => setAdminEmail(e.target.value)}
                      {...invalidProps("nc-admin-email-error", show(adminStep) ? errors.adminEmail : undefined)}
                    />
                    {show(adminStep) && <FieldError id="nc-admin-email-error" message={errors.adminEmail} />}
                  </div>
                  <div>
                    <Label htmlFor="nc-admin-first">{t.adminFirstName}</Label>
                    <Input id="nc-admin-first" value={adminFirstName} onChange={(e) => setAdminFirstName(e.target.value)} />
                  </div>
                  <div>
                    <Label htmlFor="nc-admin-last">{t.adminLastName}</Label>
                    <Input id="nc-admin-last" value={adminLastName} onChange={(e) => setAdminLastName(e.target.value)} />
                  </div>
                </div>
              )}
            </div>
          )}

          {error && (
            <p role="alert" className="rounded-md border border-line bg-surface-muted px-3 py-2 text-sm text-state-bad-text">
              {error}
            </p>
          )}

          <div className="flex flex-wrap items-center gap-2 border-t border-line pt-4">
            {step > 0 && (
              <Button type="button" variant="outline" onClick={() => goTo(step - 1)} disabled={onboard.isPending}>
                {t.back}
              </Button>
            )}
            <span className="flex-1" />
            {step === subscriptionStep && subscriptionOn && (
              <Button type="button" variant="outline" onClick={skipSubscription}>
                {t.withoutSubscription}
              </Button>
            )}
            <Button type="submit" loading={onboard.isPending}>
              {step === adminStep
                ? onboard.isPending
                  ? t.creating
                  : t.create
                : t.nextTo.replace("{step}", steps[step + 1].toLowerCase())}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

function FirstInvoice({ invoice, failed }: { invoice: InvoicePreview | undefined; failed: boolean }) {
  if (failed) return <p className="text-xs text-content-muted">{t.previewError}</p>;
  if (!invoice) return null;
  return (
    <div className="rounded-md border border-line p-4" data-testid="new-client-first-invoice">
      <span className="eyebrow">
        {t.firstInvoice.replace("{period}", `${formatDate(invoice.from).slice(0, 5)} – ${formatDate(invoice.to).slice(0, 5)}`)}
      </span>
      <ul className="mt-2 space-y-1 text-sm">
        {invoice.lines.map((line, i) => (
          <li key={i} className="flex justify-between gap-3">
            <span>
              {line.label}
              {line.quantity > 1 && ` × ${line.quantity}`}
            </span>
            <span className="font-mono tabular-nums">{lei(line.amount)}</span>
          </li>
        ))}
        <li className="flex justify-between gap-3 border-t border-line pt-1 font-semibold text-content">
          <span>{t.total}</span>
          <span className="font-mono tabular-nums">{lei(invoice.total)}</span>
        </li>
      </ul>
      <p className="mt-2 text-xs text-content-muted">{t.invoiceNote}</p>
    </div>
  );
}
