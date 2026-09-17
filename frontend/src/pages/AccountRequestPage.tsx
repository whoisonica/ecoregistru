import { useMemo, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { useSubmitAccountRequest } from "@/hooks/useAccountRequests";
import { useFormDraft } from "@/hooks/useFormDraft";
import type { AccountRequestInput, CompanyType, MarketRole, WasteOperationCode } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { isValidCui } from "@/lib/cui";
import { cn, withCount } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { DateInput } from "@/components/ui/date-input";
import { FormSection } from "@/components/ui/form-section";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { ChoiceCards } from "@/components/ui/choice-cards";
import { MarketRolePicker } from "@/components/CompanyProfileFields";
import { PillGroup } from "@/components/ui/pill-group";
import { LegalNotice } from "@/components/LegalFooter";
import { CornerLink, PosterFact, PosterStep, PublicShell, publicButtonClass } from "@/components/PublicShell";

const t = strings.accountRequest;
const codeLabels = strings.enums.wasteOperationCode;

const COMPANY_TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];
const ALL_CODES = Object.keys(codeLabels) as WasteOperationCode[];

/**
 * Deșeurile pe care le au aproape toți clienții, pe nume de zi cu zi, cu codul din Lista europeană
 * lângă (proprietarul, 16.09.2026: „să fie și lista, dar să lase gol dacă vrea”). Bifele sunt
 * opționale, iar ce nu e aici se scrie dedesubt. Se trimit în același text ca până acum, cu codul
 * între paranteze, deci cine aprobă cererea vede exact ce a ales omul.
 */
const COMMON_WASTES: { name: string; code: string }[] = [
  { name: "Carton și ambalaje de hârtie", code: "15 01 01" },
  { name: "Folie și ambalaje de plastic", code: "15 01 02" },
  { name: "Paleți și ambalaje de lemn", code: "15 01 03" },
  { name: "Doze și ambalaje metalice", code: "15 01 04" },
  { name: "Ambalaje de sticlă", code: "15 01 07" },
  { name: "Hârtie de birou", code: "20 01 01" },
  { name: "Deșeu menajer amestecat", code: "20 03 01" },
  { name: "Resturi alimentare", code: "20 01 08" },
  { name: "Metale, fier vechi", code: "20 01 40" },
  { name: "Lemn", code: "20 01 38" },
  { name: "Moloz", code: "17 01 07" },
  { name: "Echipamente electrice casate", code: "20 01 36" },
  { name: "Anvelope uzate", code: "16 01 03" },
  { name: "Ulei uzat", code: "13 02 08*" },
  { name: "Becuri și tuburi fluorescente", code: "20 01 21*" },
  { name: "Ambalaje contaminate (vopsele, chimicale)", code: "15 01 10*" },
];
const R_CODES = ALL_CODES.filter((c) => c.startsWith("R"));
const D_CODES = ALL_CODES.filter((c) => c.startsWith("D"));

/** Aceeași formă pe care o cere `CompanyService` la crearea firmei din cerere. */
/** Deliberat larg: validarea de email a browserului respinge deja ce e evident stricat. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type FieldErrors = Partial<
  Record<
    | "companyName"
    | "cui"
    | "companyAddress"
    | "caenCode"
    | "workPointName"
    | "workPointAddress"
    | "authNumber"
    | "authExpiry"
    | "transportMeans"
    | "transportLicenseNumber"
    | "transportLicenseExpiry"
    | "contactName"
    | "contactEmail"
    | "contactPhone"
    | "contactRole"
    | "marketRoles",
    string
  >
>;
type Step = 1 | 2 | 3 | 4;

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
  /** Generatorul care spune explicit că activitatea lui nu cere autorizație de mediu. */
  const [noEnvAuth, setNoEnvAuth] = useState(false);
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
  /** Deșeurile bifate din lista uzuală, după nume. Goale e un răspuns bun: nu se cere nimic. */
  const [wasteNames, setWasteNames] = useState<string[]>([]);
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
  /** Pasul curent, 1–4. Ciorna ține rubricile, nu pasul: la revenire se pornește de la început. */
  const [step, setStep] = useState<Step>(1);
  /** Emailul cu care s-a trimis, ca pagina de mulțumire să-l poată numi. */
  const [sentToEmail, setSentToEmail] = useState("");

  // Six sections is a lot to retype. The draft lives in the browser only — nothing is sent until
  // the form is submitted — and it is dropped the moment the request goes through.
  const draftValues = useMemo(
    () => ({
      companyName, cui, companyType, companyAddress,
      workPointName, workPointAddress,
      contactName, contactEmail, contactPhone, contactRole, caenCode,
      authNumber, authExpiry, noEnvAuth,
      transportMeans, transportLicenseNumber, transportLicenseExpiry,
      marketRoles, operationCodes, wasteCodesText, wasteNames, notes,
    }),
    [
      companyName, cui, companyType, companyAddress,
      workPointName, workPointAddress,
      contactName, contactEmail, contactPhone, contactRole, caenCode,
      authNumber, authExpiry, noEnvAuth,
      transportMeans, transportLicenseNumber, transportLicenseExpiry,
      marketRoles, operationCodes, wasteCodesText, wasteNames, notes,
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
      // O ciornă de dinaintea bifei n-o are.
      setNoEnvAuth(v.noEnvAuth ?? false);
      setTransportMeans(v.transportMeans);
      setTransportLicenseNumber(v.transportLicenseNumber);
      setTransportLicenseExpiry(v.transportLicenseExpiry);
      setMarketRoles(v.marketRoles);
      setOperationCodes(v.operationCodes);
      // Ciorna nu ține alegerea, o deduce: dacă erau coduri bifate, lista se redeschide la ele.
      setChooseCodes(v.operationCodes.length > 0);
      setWasteCodesText(v.wasteCodesText);
      // O ciornă de dinaintea listei n-are bifele.
      setWasteNames(v.wasteNames ?? []);
      setNotes(v.notes);
    },
    // "Nothing worth keeping": every field back to how the form opens. GENERATOR is the
    // preselected type, so it does not count as an answer on its own.
    (v) =>
      v.companyType === "GENERATOR" &&
      v.marketRoles.length === 0 &&
      !v.noEnvAuth &&
      v.operationCodes.length === 0 &&
      (v.wasteNames ?? []).length === 0 &&
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
    setNoEnvAuth(false);
    setTransportMeans("");
    setTransportLicenseNumber("");
    setTransportLicenseExpiry("");
    setMarketRoles([]);
    setOperationCodes([]);
    setChooseCodes(false);
    setWasteCodesText("");
    setWasteNames([]);
    setNotes("");
    setErrors({});
    setError(null);
  }

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

  // Only a business that takes waste from third parties has transport to declare.
  const asksTransport = companyType !== "GENERATOR";
  // Doar cine generează are „tipul de generator” (producător / importator / comerciant).
  const asksMarketRoles = companyType !== "COLLECTOR";
  /** Bifa „n-avem nevoie de autorizație” există doar la generatorul pur; colectorul are întotdeauna. */
  const mayLackEnvAuth = companyType === "GENERATOR";
  const skipsEnvAuth = mayLackEnvAuth && noEnvAuth;

  /**
   * Rubricile obligatorii ale unui pas; `undefined` = toate (la trimitere). Aproape totul e
   * obligatoriu (proprietarul, 16.09.2026): ce lipsea cerea un telefon la aprobare. Rămân libere
   * doar textele („alte deșeuri”, observațiile) și lista de deșeuri, unde golul e un răspuns.
   */
  function validate(only?: Step): FieldErrors {
    const errs: FieldErrors = {};
    const need = (value: string, key: keyof FieldErrors, message: string = t.errRequired) => {
      if (!value.trim()) errs[key] = message;
    };
    if (only === undefined || only === 1) {
      need(companyName, "companyName", t.errCompanyName);
      const normalizedCui = cui.replace(/\s/g, "").toUpperCase();
      if (!normalizedCui) errs.cui = t.errCui;
      else if (!isValidCui(normalizedCui)) errs.cui = t.errCuiFormat;
      need(companyAddress, "companyAddress");
      need(caenCode, "caenCode");
    }
    if (only === undefined || only === 2) {
      need(workPointName, "workPointName");
      need(workPointAddress, "workPointAddress");
      if (!skipsEnvAuth) {
        need(authNumber, "authNumber");
        need(authExpiry, "authExpiry", t.errRequiredDate);
      }
      if (asksTransport) {
        need(transportMeans, "transportMeans");
        need(transportLicenseNumber, "transportLicenseNumber");
        need(transportLicenseExpiry, "transportLicenseExpiry", t.errRequiredDate);
      }
    }
    if (only === undefined || only === 3) {
      need(contactName, "contactName");
      need(contactPhone, "contactPhone");
      need(contactRole, "contactRole");
      if (!contactEmail.trim()) errs.contactEmail = t.errContactEmail;
      else if (!EMAIL_PATTERN.test(contactEmail.trim())) errs.contactEmail = t.errContactEmailFormat;
    }
    if (only === undefined || only === 4) {
      if (asksMarketRoles && marketRoles.length === 0) errs.marketRoles = t.errMarketRoles;
    }
    return errs;
  }

  /** Pe ce pas stă o rubrică greșită — ca trimiterea să ducă omul înapoi la ea, nu doar s-o marcheze. */
  function stepOf(errs: FieldErrors): Step {
    if (errs.companyName || errs.cui || errs.companyAddress || errs.caenCode) return 1;
    if (
      errs.workPointName ||
      errs.workPointAddress ||
      errs.authNumber ||
      errs.authExpiry ||
      errs.transportMeans ||
      errs.transportLicenseNumber ||
      errs.transportLicenseExpiry
    )
      return 2;
    if (errs.contactName || errs.contactEmail || errs.contactPhone || errs.contactRole) return 3;
    return 4;
  }

  /**
   * După ce randarea a pus semnele pe rubrici, du ochiul la prima. `data-invalid` e cârligul,
   * deci nu ținem nicio listă de referințe în paralel cu formularul.
   */
  function focusFirstInvalid() {
    requestAnimationFrame(() => {
      const first = formRef.current?.querySelector<HTMLElement>('[data-invalid="true"]');
      if (!first) return;
      first.scrollIntoView({ block: "center", behavior: "smooth" });
      first.focus({ preventScroll: true });
    });
  }

  function goTo(next: Step) {
    setStep(next);
    // Pasul nou începe de sus, ca o pagină nouă — nu de unde rămăsese derularea celui vechi.
    requestAnimationFrame(() => window.scrollTo({ top: 0 }));
  }

  /** „Continuă”: pasul curent se verifică aici, nu abia la trimitere, trei ecrane mai încolo. */
  function nextStep() {
    const found = validate(step);
    if (Object.keys(found).length > 0) {
      setErrors(found);
      setError(strings.common.fixErrors);
      focusFirstInvalid();
      return;
    }
    setErrors({});
    setError(null);
    if (step < 4) goTo((step + 1) as Step);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const found = validate();
    if (Object.keys(found).length > 0) {
      setErrors(found);
      setError(strings.common.fixErrors);
      // Rubrica greșită poate fi pe un pas trecut (ciorna a pus la loc un CUI stricat): întoarce
      // omul la pasul ei și abia apoi du-i ochiul la rubrică.
      const target = stepOf(found);
      if (target !== step) goTo(target);
      focusFirstInvalid();
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
      environmentalAuthNumber: skipsEnvAuth ? null : authNumber.trim() || null,
      environmentalAuthExpiry: skipsEnvAuth ? null : authExpiry || null,
      transportMeans: asksTransport ? transportMeans.trim() || null : null,
      transportLicenseNumber: asksTransport ? transportLicenseNumber.trim() || null : null,
      transportLicenseExpiry: asksTransport ? transportLicenseExpiry || null : null,
      marketRoles,
      operationCodes,
      wasteCodesText:
        [
          ...COMMON_WASTES.filter((w) => wasteNames.includes(w.name)).map((w) => `${w.name} (${w.code})`),
          wasteCodesText.trim(),
        ]
          .filter(Boolean)
          .join(", ") || null,
      // Bifa pleacă drept propoziție în observații: cine aprobă o citește acolo, lângă restul, fără
      // o rubrică nouă în backend.
      notes: [skipsEnvAuth ? t.noEnvAuthNote : "", notes.trim()].filter(Boolean).join("\n") || null,
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

  // Direcția „Poster” (16.09.2026): verdele din stânga cu cuprinsul formularului — cei trei pași, cu
  // cel curent aprins și ce s-a completat scris sub cei trecuți; formularul aerisit în dreapta, un
  // pas pe ecran. Același cadru pe pagina de mulțumire, ca omul să nu sară în alt decor după ce a apăsat.
  const stepNames = [t.step1Name, t.step2Name, t.step3Name, t.step4Name] as const;
  const stepTitles = [t.step1Title, t.step2Title, t.step3Title, t.step4Title] as const;
  const stepSubtitles = [t.step1Subtitle, t.step2Subtitle, t.step3Subtitle, t.step4Subtitle] as const;
  const fieldClass = "h-12 px-4 text-base";
  const areaClass = "px-4 text-base";
  const labelClass = "text-[0.8125rem] text-content-strong";

  return (
    <PublicShell
      split="narrow"
      align="top"
      headline={t.posterHeadline}
      accent={t.posterAccent}
      lede={t.posterLede}
      aside={
        <div className="flex flex-col gap-7">
          <ol className="flex flex-col gap-5">
            <PosterStep
              index={1}
              title={t.step1Name}
              body={t.step1Lead}
              current={!sent && step === 1}
              done={sent || step > 1}
              summary={[companyName.trim(), cui.trim()].filter(Boolean).join(" · ")}
            />
            <PosterStep
              index={2}
              title={t.step2Name}
              body={t.step2Lead}
              current={!sent && step === 2}
              done={sent || step > 2}
              summary={workPointName.trim()}
            />
            <PosterStep
              index={3}
              title={t.step3Name}
              body={t.step3Lead}
              current={!sent && step === 3}
              done={sent || step > 3}
              summary={contactEmail.trim()}
            />
            <PosterStep index={4} title={t.step4Name} body={t.step4Lead} current={!sent && step === 4} done={sent} />
          </ol>
          <div className="flex flex-col gap-1.5 border-t border-line pt-5">
            {[t.noteRequired, t.noteAccess, t.posterNote].map((line) => (
              <PosterFact key={line}>{line}</PosterFact>
            ))}
          </div>
        </div>
      }
      corner={<CornerLink prompt={t.haveAccount} label={t.goToLogin} to="/login" />}
    >
      {sent ? (
        <div className="flex max-w-[560px] flex-col gap-8">
          <div className="flex flex-col gap-3">
            <CheckCircle2 className="h-12 w-12 text-mark" aria-hidden />
            <h1 className="text-[32px] font-semibold leading-10 tracking-[-0.015em] text-content">{t.successTitle}</h1>
            <p className="text-content-muted">{t.successBody}</p>
          </div>

          {/* „Am primit cererea" spune ce s-a întâmplat; asta spune ce urmează, cu un termen. */}
          <Card className="text-left">
            <h2 className="text-xs font-semibold uppercase tracking-wide text-content-muted">
              {t.successNextTitle}
            </h2>
            <ol className="mt-3 space-y-3">
              {[
                t.successNext1,
                t.successNext2,
                t.successNext3.replace("{email}", sentToEmail || t.successNoEmailFallback),
              ].map((line, i) => (
                <li key={i} className="flex gap-3 text-sm text-content-strong">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded bg-brand-100 font-mono text-xs font-medium text-brand-800">
                    {i + 1}
                  </span>
                  <span>{line}</span>
                </li>
              ))}
            </ol>
            <p className="mt-4 border-t border-line pt-3 text-xs text-content-muted">{t.successSpam}</p>
          </Card>

          <Link to="/login" className="text-sm font-medium text-mark hover:underline">
            {t.backToLogin}
          </Link>
        </div>
      ) : (
        <form ref={formRef} onSubmit={handleSubmit} className="flex w-full max-w-[680px] flex-col gap-9" noValidate>
          <div className="flex flex-col gap-2">
            <span className="font-mono text-xs font-medium uppercase tracking-[0.08em] text-mark">
              {t.stepOf.replace("{n}", String(step)).replace("{name}", stepNames[step - 1])}
            </span>
            <h1 id="ar-step-title" className="text-[32px] font-semibold leading-10 tracking-[-0.015em] text-content">
              {stepTitles[step - 1]}
            </h1>
            <p className="text-content-muted">{stepSubtitles[step - 1]}</p>
          </div>

          {draft.restored && (
            <div className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-inbound px-3 py-2 text-sm text-inbound-border">
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
              className="flex items-start gap-2 rounded-md border border-state-bad px-3 py-2 text-sm text-state-bad-text"
            >
              <span aria-hidden className="mt-2 inline-block h-2 w-2 shrink-0 rounded-sm bg-state-bad" />
              {error}
            </p>
          )}

          {step === 1 && (
            <div className="flex flex-col gap-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <Label className={labelClass} htmlFor="ar-cui" required>
                    {t.cui}
                  </Label>
                  <Input
                    className={fieldClass}
                    id="ar-cui"
                    value={cui}
                    onChange={(e) => setCui(e.target.value)}
                    placeholder={t.cuiPlaceholder}
                    autoFocus
                    {...invalidProps("ar-cui-err", errors.cui)}
                  />
                  <FieldError id="ar-cui-err" message={errors.cui} />
                </div>
                <div>
                  <Label className={labelClass} htmlFor="ar-name" required>
                    {t.companyName}
                  </Label>
                  <Input
                    className={fieldClass}
                    id="ar-name"
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    autoComplete="organization"
                    {...invalidProps("ar-name-err", errors.companyName)}
                  />
                  <FieldError id="ar-name-err" message={errors.companyName} />
                </div>
              </div>
              <div>
                <span id="ar-type-label" className={cn("mb-1 block text-xs font-medium", labelClass)}>
                  {t.companyType}
                </span>
                <ChoiceCards
                  name="ar-type"
                  aria-labelledby="ar-type-label"
                  columns={3}
                  value={companyType}
                  onChange={setCompanyType}
                  options={COMPANY_TYPES.map((ct) => ({
                    value: ct,
                    label: t.companyTypeChoice[ct].label,
                    description: t.companyTypeChoice[ct].hint,
                  }))}
                />
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-[1fr_180px]">
                <div>
                  <Label className={labelClass} htmlFor="ar-address" required>
                    {t.companyAddress}
                  </Label>
                  <Textarea
                    className={areaClass}
                    id="ar-address"
                    rows={2}
                    value={companyAddress}
                    onChange={(e) => setCompanyAddress(e.target.value)}
                    {...invalidProps("ar-address-err", errors.companyAddress)}
                  />
                  <FieldError id="ar-address-err" message={errors.companyAddress} />
                </div>
                <div>
                  <Label className={labelClass} htmlFor="ar-caen" required>
                    {t.caenCode}
                  </Label>
                  <Input
                    className={cn(fieldClass, "font-mono")}
                    id="ar-caen"
                    value={caenCode}
                    onChange={(e) => setCaenCode(e.target.value)}
                    placeholder={t.caenCodePlaceholder}
                    {...invalidProps("ar-caen-err", errors.caenCode)}
                  />
                  <FieldError id="ar-caen-err" message={errors.caenCode} />
                  <p className="mt-1 text-xs text-content-muted">{t.caenCodeHint}</p>
                </div>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="flex flex-col gap-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <Label className={labelClass} htmlFor="ar-wp-name" required>
                    {t.workPointName}
                  </Label>
                  <Input
                    className={fieldClass}
                    id="ar-wp-name"
                    value={workPointName}
                    onChange={(e) => setWorkPointName(e.target.value)}
                    placeholder={t.workPointNamePlaceholder}
                    autoFocus
                    {...invalidProps("ar-wp-name-err", errors.workPointName)}
                  />
                  <FieldError id="ar-wp-name-err" message={errors.workPointName} />
                </div>
                <div>
                  <Label className={labelClass} htmlFor="ar-wp-address" required>
                    {t.workPointAddress}
                  </Label>
                  <Textarea
                    className={areaClass}
                    id="ar-wp-address"
                    rows={2}
                    value={workPointAddress}
                    onChange={(e) => setWorkPointAddress(e.target.value)}
                    {...invalidProps("ar-wp-address-err", errors.workPointAddress)}
                  />
                  <FieldError id="ar-wp-address-err" message={errors.workPointAddress} />
                </div>
              </div>

              <FormSection size="lg" title={t.sectionAuthorization} className="pt-2">
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <Label className={labelClass} htmlFor="ar-auth-number" required={!skipsEnvAuth}>
                      {t.environmentalAuthNumber}
                    </Label>
                    <Input
                      className={fieldClass}
                      id="ar-auth-number"
                      value={authNumber}
                      onChange={(e) => setAuthNumber(e.target.value)}
                      disabled={skipsEnvAuth}
                      {...invalidProps("ar-auth-number-err", errors.authNumber)}
                    />
                    <FieldError id="ar-auth-number-err" message={errors.authNumber} />
                  </div>
                  <div>
                    <Label className={labelClass} htmlFor="ar-auth-expiry" required={!skipsEnvAuth}>
                      {t.environmentalAuthExpiry}
                    </Label>
                    <DateInput
                      className={fieldClass}
                      id="ar-auth-expiry"
                      value={authExpiry}
                      onChange={(e) => setAuthExpiry(e.target.value)}
                      disabled={skipsEnvAuth}
                      {...invalidProps("ar-auth-expiry-err", errors.authExpiry)}
                    />
                    <FieldError id="ar-auth-expiry-err" message={errors.authExpiry} />
                  </div>
                </div>
                {mayLackEnvAuth && (
                  <label className="flex items-start gap-3 rounded-md border border-line p-3.5 text-sm has-[:checked]:border-mark has-[:checked]:bg-mark-soft/40">
                    <input
                      type="checkbox"
                      id="ar-no-env-auth"
                      className="mt-0.5 h-4 w-4 rounded border-line-strong"
                      checked={noEnvAuth}
                      onChange={(e) => {
                        setNoEnvAuth(e.target.checked);
                        // Bifa scoate cele două rubrici din obligații — și semnele lor, dacă erau puse.
                        if (e.target.checked) setErrors((prev) => ({ ...prev, authNumber: undefined, authExpiry: undefined }));
                      }}
                    />
                    <span>
                      <span className="font-medium text-content-strong">{t.noEnvAuth}</span>
                      <span className="mt-0.5 block text-xs text-content-muted">{t.noEnvAuthHint}</span>
                    </span>
                  </label>
                )}
              </FormSection>

              {asksTransport && (
                <FormSection size="lg" title={t.sectionTransport} description={t.transportHint} className="pt-2">
                  <div>
                    <Label className={labelClass} htmlFor="ar-transport-means" required>
                      {t.transportMeans}
                    </Label>
                    <Textarea
                      className={areaClass}
                      id="ar-transport-means"
                      rows={2}
                      value={transportMeans}
                      onChange={(e) => setTransportMeans(e.target.value)}
                      placeholder={t.transportMeansPlaceholder}
                      {...invalidProps("ar-transport-means-err", errors.transportMeans)}
                    />
                    <FieldError id="ar-transport-means-err" message={errors.transportMeans} />
                  </div>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <Label className={labelClass} htmlFor="ar-transport-licence" required>
                        {t.transportLicenseNumber}
                      </Label>
                      <Input
                        className={fieldClass}
                        id="ar-transport-licence"
                        value={transportLicenseNumber}
                        onChange={(e) => setTransportLicenseNumber(e.target.value)}
                        {...invalidProps("ar-transport-licence-err", errors.transportLicenseNumber)}
                      />
                      <FieldError id="ar-transport-licence-err" message={errors.transportLicenseNumber} />
                    </div>
                    <div>
                      <Label className={labelClass} htmlFor="ar-transport-expiry" required>
                        {t.transportLicenseExpiry}
                      </Label>
                      <DateInput
                        className={fieldClass}
                        id="ar-transport-expiry"
                        value={transportLicenseExpiry}
                        onChange={(e) => setTransportLicenseExpiry(e.target.value)}
                        {...invalidProps("ar-transport-expiry-err", errors.transportLicenseExpiry)}
                      />
                      <FieldError id="ar-transport-expiry-err" message={errors.transportLicenseExpiry} />
                    </div>
                  </div>
                </FormSection>
              )}
            </div>
          )}

          {step === 3 && (
            <div className="flex flex-col gap-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <Label className={labelClass} htmlFor="ar-contact-name" required>
                    {t.contactName}
                  </Label>
                  <Input
                    className={fieldClass}
                    id="ar-contact-name"
                    value={contactName}
                    onChange={(e) => setContactName(e.target.value)}
                    autoComplete="name"
                    autoFocus
                    {...invalidProps("ar-contact-name-err", errors.contactName)}
                  />
                  <FieldError id="ar-contact-name-err" message={errors.contactName} />
                </div>
                <div>
                  <Label className={labelClass} htmlFor="ar-contact-phone" required>
                    {t.contactPhone}
                  </Label>
                  <Input
                    className={fieldClass}
                    id="ar-contact-phone"
                    value={contactPhone}
                    onChange={(e) => setContactPhone(e.target.value)}
                    autoComplete="tel"
                    {...invalidProps("ar-contact-phone-err", errors.contactPhone)}
                  />
                  <FieldError id="ar-contact-phone-err" message={errors.contactPhone} />
                </div>
              </div>
              <div>
                <Label className={labelClass} htmlFor="ar-contact-email" required>
                  {t.contactEmail}
                </Label>
                <Input
                  className={fieldClass}
                  id="ar-contact-email"
                  type="email"
                  value={contactEmail}
                  onChange={(e) => setContactEmail(e.target.value)}
                  autoComplete="email"
                  {...invalidProps("ar-contact-email-err", errors.contactEmail)}
                />
                <FieldError id="ar-contact-email-err" message={errors.contactEmail} />
              </div>
              <div>
                <Label className={labelClass} htmlFor="ar-contact-role" required>
                  {t.contactRole}
                </Label>
                <Input
                  className={fieldClass}
                  id="ar-contact-role"
                  value={contactRole}
                  onChange={(e) => setContactRole(e.target.value)}
                  placeholder={t.contactRolePlaceholder}
                  {...invalidProps("ar-contact-role-err", errors.contactRole)}
                />
                <FieldError id="ar-contact-role-err" message={errors.contactRole} />
                <p className="mt-1 text-xs text-content-muted">{t.contactRoleHint}</p>
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="flex flex-col gap-8">
              <div className="flex flex-col gap-4">
                <PillGroup
                  name="ar-waste-names"
                  multiple
                  aria-labelledby="ar-step-title"
                  options={COMMON_WASTES.map((w) => ({ value: w.name, label: w.name, code: w.code }))}
                  selected={wasteNames}
                  onToggle={(name) =>
                    setWasteNames((prev) => (prev.includes(name) ? prev.filter((x) => x !== name) : [...prev, name]))
                  }
                />
                <div>
                  <Label className={labelClass} htmlFor="ar-waste-text">
                    {t.wasteOtherText}
                  </Label>
                  <Textarea
                    className={areaClass}
                    id="ar-waste-text"
                    rows={2}
                    value={wasteCodesText}
                    onChange={(e) => setWasteCodesText(e.target.value)}
                    placeholder={t.wasteCodesTextPlaceholder}
                  />
                  <p className="mt-1 text-xs text-content-muted">{t.wasteOtherHint}</p>
                </div>
              </div>

              {asksMarketRoles && (
                <FormSection size="lg" title={t.sectionMarketRole}>
                  <div data-invalid={errors.marketRoles ? "true" : undefined} tabIndex={-1}>
                    <MarketRolePicker
                      value={marketRoles}
                      onChange={setMarketRoles}
                      label={`${t.marketRoles} *`}
                      hint={t.marketRolesHint}
                    />
                    <FieldError id="ar-market-roles-err" message={errors.marketRoles} />
                  </div>
                </FormSection>
              )}

              <fieldset>
                <legend className="text-xl font-semibold leading-7 text-content">{t.operationCodes}</legend>
                <p className="mt-0.5 text-content-muted">{t.operationCodesHint}</p>

                {/* Două ieșiri, iar prima e onorabilă: „nu știu" e un răspuns pe care aplicația îl
                    înțelege deja — set gol înseamnă „nu s-a răspuns", iar profilul gol nu restrânge
                    nimic (decizia 6). Ce s-a schimbat e că formularul o spune. */}
                <div className="mt-3 space-y-2">
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
                <Label className={labelClass} htmlFor="ar-notes">
                  {t.notes}
                </Label>
                <Textarea
                  className={areaClass}
                  id="ar-notes"
                  rows={3}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
              </div>
            </div>
          )}

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

          <div className="flex flex-wrap items-start justify-between gap-4 border-t border-line pt-6">
            {step === 1 ? (
              <span />
            ) : (
              <button
                type="button"
                onClick={() => goTo((step - 1) as Step)}
                className="text-sm font-medium text-mark hover:underline"
              >
                ← {t.backStep}
              </button>
            )}
            {step < 4 ? (
              <Button type="button" size="lg" className={publicButtonClass} onClick={nextStep}>
                {t.continueStep} →
              </Button>
            ) : (
              <div className="flex flex-col items-end gap-2">
                <Button type="submit" size="lg" className={publicButtonClass} loading={submitMut.isPending}>
                  {submitMut.isPending ? t.submitting : t.submit}
                </Button>
                {/* Sub buton, nu deasupra lui: se citește în drum spre apăsare. */}
                <LegalNotice className="text-right" />
              </div>
            )}
          </div>
        </form>
      )}
    </PublicShell>
  );
}
