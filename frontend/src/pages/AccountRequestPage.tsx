import { useMemo, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { useSubmitAccountRequest } from "@/hooks/useAccountRequests";
import { useFormDraft } from "@/hooks/useFormDraft";
import type { AccountRequestInput, CompanyType, MarketRole, WasteOperationCode } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { withCount } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { DateInput } from "@/components/ui/date-input";
import { FormSection } from "@/components/ui/form-section";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { MarketRolePicker } from "@/components/CompanyProfileFields";

const t = strings.accountRequest;
const typeLabels = strings.enums.companyType;
const codeLabels = strings.enums.wasteOperationCode;

const COMPANY_TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];
const ALL_CODES = Object.keys(codeLabels) as WasteOperationCode[];
const R_CODES = ALL_CODES.filter((c) => c.startsWith("R"));
const D_CODES = ALL_CODES.filter((c) => c.startsWith("D"));

/** Aceeași formă pe care o cere `CompanyService` la crearea firmei din cerere. */
const CUI_PATTERN = /^(RO)?\d{2,10}$/;
/** Deliberat larg: validarea de email a browserului respinge deja ce e evident stricat. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type FieldErrors = Partial<Record<"companyName" | "cui" | "contactEmail", string>>;

/**
 * The intake form — the only public page besides login, and the only way into a closed register.
 * Submitting it creates a request, not an account: support reads the answers and creates the
 * company from them.
 *
 * <p>The form asks what it will actually use. The transport block appears only for a business that
 * takes waste from third parties, and the waste codes are free text on purpose: the 842-entry
 * nomenclator is behind authentication, and "carton, folie" from a client beats a guessed
 * six-digit code that support then has to undo.
 */
export function AccountRequestPage() {
  const submitMut = useSubmitAccountRequest();
  const formRef = useRef<HTMLFormElement>(null);

  const [companyName, setCompanyName] = useState("");
  const [cui, setCui] = useState("");
  const [companyType, setCompanyType] = useState<CompanyType>("GENERATOR");
  const [companyAddress, setCompanyAddress] = useState("");
  const [workPointName, setWorkPointName] = useState("");
  const [workPointAddress, setWorkPointAddress] = useState("");
  const [contactName, setContactName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  // Cele două rubrici de antet ale declarației anuale. Se cer aici o dată, nu se retastează.
  const [contactRole, setContactRole] = useState("");
  const [caenCode, setCaenCode] = useState("");
  const [authNumber, setAuthNumber] = useState("");
  const [authExpiry, setAuthExpiry] = useState("");
  const [transportMeans, setTransportMeans] = useState("");
  const [transportLicenseNumber, setTransportLicenseNumber] = useState("");
  const [transportLicenseExpiry, setTransportLicenseExpiry] = useState("");
  const [marketRoles, setMarketRoles] = useState<MarketRole[]>([]);
  const [operationCodes, setOperationCodes] = useState<WasteOperationCode[]>([]);
  /**
   * Dacă omul deschide cele 28 de bife sau spune „nu știu". Nu se trimite nicăieri: un set gol e
   * deja răspunsul „nu s-a răspuns", iar profilul gol nu restrânge nimic (decizia 6). Starea
   * există doar ca lista să nu stea deschisă în fața cuiva care n-a auzit de R13.
   */
  const [chooseCodes, setChooseCodes] = useState(false);
  const [wasteCodesText, setWasteCodesText] = useState("");
  const [notes, setNotes] = useState("");
  /**
   * Momeala: o rubrică pe care un om n-o vede și n-o poate focaliza cu Tab, dar pe care un robot
   * care completează tot ce găsește o umple. Backendul primește tot 202 și nu scrie nimic — un
   * refuz vizibil i-ar spune botului ce să evite data viitoare.
   */
  const [website, setWebsite] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);
  /** Emailul cu care s-a trimis, ca pagina de mulțumire să-l poată numi. */
  const [sentToEmail, setSentToEmail] = useState("");

  // Six sections is a lot to retype. The draft lives in the browser only — nothing is sent until
  // the form is submitted — and it is dropped the moment the request goes through.
  const draftValues = useMemo(
    () => ({
      companyName, cui, companyType, companyAddress,
      workPointName, workPointAddress,
      contactName, contactEmail, contactPhone, contactRole, caenCode,
      authNumber, authExpiry,
      transportMeans, transportLicenseNumber, transportLicenseExpiry,
      marketRoles, operationCodes, wasteCodesText, notes,
    }),
    [
      companyName, cui, companyType, companyAddress,
      workPointName, workPointAddress,
      contactName, contactEmail, contactPhone, contactRole, caenCode,
      authNumber, authExpiry,
      transportMeans, transportLicenseNumber, transportLicenseExpiry,
      marketRoles, operationCodes, wasteCodesText, notes,
    ]
  );

  const draft = useFormDraft(
    "eco_draft_account_request",
    draftValues,
    (v) => {
      setCompanyName(v.companyName);
      setCui(v.cui);
      setCompanyType(v.companyType);
      setCompanyAddress(v.companyAddress);
      setWorkPointName(v.workPointName);
      setWorkPointAddress(v.workPointAddress);
      setContactName(v.contactName);
      setContactEmail(v.contactEmail);
      setContactPhone(v.contactPhone);
      setContactRole(v.contactRole);
      setCaenCode(v.caenCode);
      setAuthNumber(v.authNumber);
      setAuthExpiry(v.authExpiry);
      setTransportMeans(v.transportMeans);
      setTransportLicenseNumber(v.transportLicenseNumber);
      setTransportLicenseExpiry(v.transportLicenseExpiry);
      setMarketRoles(v.marketRoles);
      setOperationCodes(v.operationCodes);
      // Ciorna nu ține alegerea, o deduce: dacă erau coduri bifate, lista se redeschide la ele.
      setChooseCodes(v.operationCodes.length > 0);
      setWasteCodesText(v.wasteCodesText);
      setNotes(v.notes);
    },
    // "Nothing worth keeping": every field back to how the form opens. GENERATOR is the
    // preselected type, so it does not count as an answer on its own.
    (v) =>
      v.companyType === "GENERATOR" &&
      v.marketRoles.length === 0 &&
      v.operationCodes.length === 0 &&
      [
        v.companyName, v.cui, v.companyAddress,
        v.workPointName, v.workPointAddress,
        v.contactName, v.contactEmail, v.contactPhone, v.contactRole, v.caenCode,
        v.authNumber, v.authExpiry,
        v.transportMeans, v.transportLicenseNumber, v.transportLicenseExpiry,
        v.wasteCodesText, v.notes,
      ].every((field) => field === "")
  );

  function discardDraft() {
    draft.discard();
    setCompanyName("");
    setCui("");
    setCompanyType("GENERATOR");
    setCompanyAddress("");
    setWorkPointName("");
    setWorkPointAddress("");
    setContactName("");
    setContactEmail("");
    setContactPhone("");
    setContactRole("");
    setCaenCode("");
    setAuthNumber("");
    setAuthExpiry("");
    setTransportMeans("");
    setTransportLicenseNumber("");
    setTransportLicenseExpiry("");
    setMarketRoles([]);
    setOperationCodes([]);
    setChooseCodes(false);
    setWasteCodesText("");
    setNotes("");
    setErrors({});
    setError(null);
  }

  // Only a business that takes waste from third parties has transport to declare.
  const asksTransport = companyType !== "GENERATOR";

  function toggleCode(code: WasteOperationCode) {
    setOperationCodes((prev) =>
      prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code]
    );
  }

  /** Închiderea listei nu doar o ascunde — ar lăsa în urmă bife pe care omul crede că le-a retras. */
  function chooseUnknownCodes() {
    setChooseCodes(false);
    setOperationCodes([]);
  }

  function validate(): FieldErrors {
    const errs: FieldErrors = {};
    if (!companyName.trim()) errs.companyName = t.errCompanyName;
    const normalizedCui = cui.replace(/\s/g, "").toUpperCase();
    if (!normalizedCui) errs.cui = t.errCui;
    else if (!CUI_PATTERN.test(normalizedCui)) errs.cui = t.errCuiFormat;
    if (!contactEmail.trim()) errs.contactEmail = t.errContactEmail;
    else if (!EMAIL_PATTERN.test(contactEmail.trim())) errs.contactEmail = t.errContactEmailFormat;
    return errs;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const found = validate();
    if (Object.keys(found).length > 0) {
      setErrors(found);
      setError(strings.common.fixErrors);
      // După ce randarea a pus semnele pe rubrici, du ochiul la prima. `data-invalid` e cârligul,
      // deci nu ținem nicio listă de referințe în paralel cu formularul. Butonul stă la capătul a
      // șase secțiuni: fără derulare, apăsatul lui pare că nu face nimic.
      requestAnimationFrame(() => {
        const first = formRef.current?.querySelector<HTMLElement>('[data-invalid="true"]');
        if (!first) return;
        first.scrollIntoView({ block: "center", behavior: "smooth" });
        first.focus({ preventScroll: true });
      });
      return;
    }
    setErrors({});
    setError(null);
    const input: AccountRequestInput = {
      companyName: companyName.trim(),
      cui: cui.trim(),
      companyType,
      companyAddress: companyAddress.trim() || null,
      workPointName: workPointName.trim() || null,
      workPointAddress: workPointAddress.trim() || null,
      contactName: contactName.trim() || null,
      contactEmail: contactEmail.trim(),
      contactPhone: contactPhone.trim() || null,
      contactRole: contactRole.trim() || null,
      caenCode: caenCode.trim() || null,
      environmentalAuthNumber: authNumber.trim() || null,
      environmentalAuthExpiry: authExpiry || null,
      transportMeans: asksTransport ? transportMeans.trim() || null : null,
      transportLicenseNumber: asksTransport ? transportLicenseNumber.trim() || null : null,
      transportLicenseExpiry: asksTransport ? transportLicenseExpiry || null : null,
      marketRoles,
      operationCodes,
      wasteCodesText: wasteCodesText.trim() || null,
      notes: notes.trim() || null,
      website: website || null,
    };
    try {
      await submitMut.mutateAsync(input);
      draft.discard();
      setSentToEmail(contactEmail.trim());
      setSent(true);
    } catch (err) {
      setError(apiErrorMessage(err, t.submitError));
    }
  }

  if (sent) {
    return (
      <div className="mx-auto flex min-h-screen max-w-xl flex-col justify-center px-4 py-10">
        <div className="text-center">
          <div className="text-xl font-bold text-brand">{strings.appName}</div>
          <CheckCircle2 className="mx-auto mt-6 h-12 w-12 text-emerald-600" />
          <h1 className="mt-4 text-2xl font-bold text-content">{t.successTitle}</h1>
          <p className="mt-2 text-sm text-content-strong">{t.successBody}</p>
        </div>

        {/* „Am primit cererea" spune ce s-a întâmplat; asta spune ce urmează, cu un termen. */}
        <Card className="mt-8 text-left">
          <h2 className="text-xs font-semibold uppercase tracking-wide text-content-muted">
            {t.successNextTitle}
          </h2>
          <ol className="mt-3 space-y-3">
            {[
              t.successNext1,
              t.successNext2,
              t.successNext3.replace("{email}", sentToEmail || t.successNoEmailFallback),
            ].map((step, i) => (
              <li key={i} className="flex gap-3 text-sm text-content-strong">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-100 text-xs font-semibold text-brand">
                  {i + 1}
                </span>
                <span>{step}</span>
              </li>
            ))}
          </ol>
          <p className="mt-4 border-t border-line pt-3 text-xs text-content-muted">{t.successSpam}</p>
        </Card>

        <Link to="/login" className="mt-6 text-center text-sm text-brand hover:underline">
          {t.backToLogin}
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      {/* Cele două rânduri de brand pe care le are cardul de login. Fără ele, un prospect care
          intră pe link vede un formular lung fără să știe al cui e. */}
      <header className="text-center">
        <div className="text-2xl font-bold text-brand">{strings.appName}</div>
        <div className="text-sm text-content-muted">{strings.tagline}</div>
      </header>

      <h1 className="mt-8 text-2xl font-bold text-content">{t.title}</h1>
      <p className="mt-1 text-sm text-content-muted">{t.subtitle}</p>

      <Card className="mt-6 bg-surface-sunken">
        <h2 className="text-xs font-semibold uppercase tracking-wide text-content-muted">
          {t.stepsTitle}
        </h2>
        <ol className="mt-3 grid gap-3 sm:grid-cols-3">
          {[t.step1, t.step2, t.step3].map((step, i) => (
            <li key={i} className="flex gap-2 text-sm text-content-strong">
              <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-brand-100 text-xs font-semibold text-brand">
                {i + 1}
              </span>
              <span>{step}</span>
            </li>
          ))}
        </ol>
      </Card>

      <form ref={formRef} onSubmit={handleSubmit} className="mt-8 space-y-7" noValidate>
        {draft.restored && (
          <div className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-800">
            <span>{t.draftRestored}</span>
            <button
              type="button"
              onClick={discardDraft}
              className="shrink-0 font-medium underline hover:no-underline"
            >
              {t.draftDiscard}
            </button>
          </div>
        )}
        {error && (
          <p
            role="alert"
            className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
          >
            {error}
          </p>
        )}

        <p className="text-xs text-content-muted">{t.requiredLegend}</p>

        <FormSection title={t.sectionCompany}>
          <div>
            <Label htmlFor="ar-name" required>
              {t.companyName}
            </Label>
            <Input
              id="ar-name"
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              autoComplete="organization"
              {...invalidProps("ar-name-err", errors.companyName)}
            />
            <FieldError id="ar-name-err" message={errors.companyName} />
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="ar-cui" required>
                {t.cui}
              </Label>
              <Input
                id="ar-cui"
                value={cui}
                onChange={(e) => setCui(e.target.value)}
                placeholder={t.cuiPlaceholder}
                {...invalidProps("ar-cui-err", errors.cui)}
              />
              <FieldError id="ar-cui-err" message={errors.cui} />
            </div>
            <div>
              <Label htmlFor="ar-type">{t.companyType}</Label>
              <Select
                id="ar-type"
                value={companyType}
                onChange={(e) => setCompanyType(e.target.value as CompanyType)}
              >
                {COMPANY_TYPES.map((ct) => (
                  <option key={ct} value={ct}>
                    {typeLabels[ct]}
                  </option>
                ))}
              </Select>
            </div>
          </div>
          <div>
            <Label htmlFor="ar-caen">{t.caenCode}</Label>
            <Input
              id="ar-caen"
              value={caenCode}
              onChange={(e) => setCaenCode(e.target.value)}
              placeholder={t.caenCodePlaceholder}
            />
            <p className="mt-1 text-xs text-content-muted">{t.caenCodeHint}</p>
          </div>
          <div>
            <Label htmlFor="ar-address">{t.companyAddress}</Label>
            <Textarea
              id="ar-address"
              rows={2}
              value={companyAddress}
              onChange={(e) => setCompanyAddress(e.target.value)}
            />
          </div>
        </FormSection>

        <FormSection title={t.sectionWorkPoint} description={t.workPointHint}>
          <div>
            <Label htmlFor="ar-wp-name">{t.workPointName}</Label>
            <Input
              id="ar-wp-name"
              value={workPointName}
              onChange={(e) => setWorkPointName(e.target.value)}
              placeholder={t.workPointNamePlaceholder}
            />
          </div>
          <div>
            <Label htmlFor="ar-wp-address">{t.workPointAddress}</Label>
            <Textarea
              id="ar-wp-address"
              rows={2}
              value={workPointAddress}
              onChange={(e) => setWorkPointAddress(e.target.value)}
            />
          </div>
        </FormSection>

        <FormSection title={t.sectionContact} description={t.contactHint}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="ar-contact-name">{t.contactName}</Label>
              <Input
                id="ar-contact-name"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
                autoComplete="name"
              />
            </div>
            <div>
              <Label htmlFor="ar-contact-phone">{t.contactPhone}</Label>
              <Input
                id="ar-contact-phone"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
                autoComplete="tel"
              />
            </div>
          </div>
          <div>
            <Label htmlFor="ar-contact-role">{t.contactRole}</Label>
            <Input
              id="ar-contact-role"
              value={contactRole}
              onChange={(e) => setContactRole(e.target.value)}
              placeholder={t.contactRolePlaceholder}
            />
            <p className="mt-1 text-xs text-content-muted">{t.contactRoleHint}</p>
          </div>
          <div>
            <Label htmlFor="ar-contact-email" required>
              {t.contactEmail}
            </Label>
            <Input
              id="ar-contact-email"
              type="email"
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
              autoComplete="email"
              {...invalidProps("ar-contact-email-err", errors.contactEmail)}
            />
            <FieldError id="ar-contact-email-err" message={errors.contactEmail} />
          </div>
        </FormSection>

        <FormSection title={t.sectionAuthorization}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="ar-auth-number">{t.environmentalAuthNumber}</Label>
              <Input
                id="ar-auth-number"
                value={authNumber}
                onChange={(e) => setAuthNumber(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="ar-auth-expiry">{t.environmentalAuthExpiry}</Label>
              <DateInput
                id="ar-auth-expiry"
                value={authExpiry}
                onChange={(e) => setAuthExpiry(e.target.value)}
              />
            </div>
          </div>
        </FormSection>

        {asksTransport && (
          <FormSection title={t.sectionTransport} description={t.transportHint}>
            <div>
              <Label htmlFor="ar-transport-means">{t.transportMeans}</Label>
              <Textarea
                id="ar-transport-means"
                rows={2}
                value={transportMeans}
                onChange={(e) => setTransportMeans(e.target.value)}
                placeholder={t.transportMeansPlaceholder}
              />
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="ar-transport-licence">{t.transportLicenseNumber}</Label>
                <Input
                  id="ar-transport-licence"
                  value={transportLicenseNumber}
                  onChange={(e) => setTransportLicenseNumber(e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="ar-transport-expiry">{t.transportLicenseExpiry}</Label>
                <DateInput
                  id="ar-transport-expiry"
                  value={transportLicenseExpiry}
                  onChange={(e) => setTransportLicenseExpiry(e.target.value)}
                />
              </div>
            </div>
          </FormSection>
        )}

        <FormSection title={t.sectionMarketRole}>
          <MarketRolePicker
            value={marketRoles}
            onChange={setMarketRoles}
            label={t.marketRoles}
            hint={t.marketRolesHint}
          />
        </FormSection>

        <FormSection title={t.sectionWaste}>
          <div>
            <Label htmlFor="ar-waste-text">{t.wasteCodesText}</Label>
            <Textarea
              id="ar-waste-text"
              rows={3}
              value={wasteCodesText}
              onChange={(e) => setWasteCodesText(e.target.value)}
              placeholder={t.wasteCodesTextPlaceholder}
            />
            <p className="mt-1 text-xs text-content-muted">{t.wasteCodesTextHint}</p>
          </div>

          <fieldset>
            <legend className="text-sm font-medium text-content-strong">{t.operationCodes}</legend>
            <p className="mt-0.5 text-xs text-content-muted">{t.operationCodesHint}</p>

            {/* Două ieșiri, iar prima e onorabilă: „nu știu" e un răspuns pe care aplicația îl
                înțelege deja — set gol înseamnă „nu s-a răspuns", iar profilul gol nu restrânge
                nimic (decizia 6). Ce s-a schimbat e că formularul o spune. */}
            <div className="mt-2 space-y-2">
              <label className="flex items-start gap-2 rounded-md border border-line p-3 text-sm has-[:checked]:border-brand has-[:checked]:bg-brand-50">
                <input
                  type="radio"
                  name="ar-codes-mode"
                  className="mt-0.5 h-4 w-4 border-line-strong"
                  checked={!chooseCodes}
                  onChange={chooseUnknownCodes}
                />
                <span>
                  <span className="font-medium text-content-strong">{t.operationCodesUnknown}</span>
                  <span className="mt-0.5 block text-xs text-content-muted">
                    {t.operationCodesUnknownHint}
                  </span>
                </span>
              </label>

              <label className="flex items-start gap-2 rounded-md border border-line p-3 text-sm has-[:checked]:border-brand has-[:checked]:bg-brand-50">
                <input
                  type="radio"
                  name="ar-codes-mode"
                  className="mt-0.5 h-4 w-4 border-line-strong"
                  checked={chooseCodes}
                  onChange={() => setChooseCodes(true)}
                />
                <span>
                  <span className="font-medium text-content-strong">{t.operationCodesChoose}</span>
                  <span className="mt-0.5 block text-xs text-content-muted">
                    {operationCodes.length > 0
                      ? withCount(
                          t.operationCodesSelected,
                          operationCodes.length,
                          "operațiune aleasă",
                          "operațiuni alese"
                        )
                      : t.operationCodesChooseHint}
                  </span>
                </span>
              </label>
            </div>

            {chooseCodes && (
              <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                {[
                  { title: t.recovery, codes: R_CODES },
                  { title: t.disposal, codes: D_CODES },
                ].map((group) => (
                  <div key={group.title}>
                    <span className="text-xs font-semibold uppercase tracking-wide text-content-muted">
                      {group.title}
                    </span>
                    <div className="mt-1 max-h-48 space-y-1 overflow-y-auto rounded-md border border-line p-2">
                      {group.codes.map((c) => (
                        <label key={c} className="flex items-start gap-2 text-sm">
                          <input
                            type="checkbox"
                            className="mt-0.5 h-4 w-4 rounded border-line-strong"
                            checked={operationCodes.includes(c)}
                            onChange={() => toggleCode(c)}
                          />
                          <span className="text-content-strong">{codeLabels[c]}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </fieldset>

          <div>
            <Label htmlFor="ar-notes">{t.notes}</Label>
            <Textarea
              id="ar-notes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
        </FormSection>

        {/* Momeala. `sr-only` o scoate din pagină fără s-o scoată din DOM, `tabIndex={-1}` o scoate
            din drumul tastaturii, iar `aria-hidden` din cel al cititorului de ecran — deci un om
            n-o poate completa nici din greșeală. Un robot care umple tot ce găsește, da. */}
        <div aria-hidden="true" className="sr-only">
          <label htmlFor="ar-website">Website</label>
          <input
            id="ar-website"
            name="website"
            type="text"
            tabIndex={-1}
            autoComplete="off"
            value={website}
            onChange={(e) => setWebsite(e.target.value)}
          />
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line pt-5">
          <Link to="/login" className="text-sm text-brand hover:underline">
            {t.backToLogin}
          </Link>
          <Button type="submit" loading={submitMut.isPending}>
            {submitMut.isPending ? t.submitting : t.submit}
          </Button>
        </div>
      </form>
    </div>
  );
}
