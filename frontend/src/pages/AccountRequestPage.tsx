import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import { useSubmitAccountRequest } from "@/hooks/useAccountRequests";
import type { AccountRequestInput, CompanyType, MarketRole, WasteOperationCode } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { DateInput } from "@/components/ui/date-input";
import { MarketRolePicker } from "@/components/CompanyProfileFields";

const t = strings.accountRequest;
const typeLabels = strings.enums.companyType;
const codeLabels = strings.enums.wasteOperationCode;

const COMPANY_TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];
const ALL_CODES = Object.keys(codeLabels) as WasteOperationCode[];
const R_CODES = ALL_CODES.filter((c) => c.startsWith("R"));
const D_CODES = ALL_CODES.filter((c) => c.startsWith("D"));

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="space-y-3 border-t border-gray-200 pt-5 first:border-0 first:pt-0">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500">{title}</h2>
      {children}
    </section>
  );
}

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

  const [companyName, setCompanyName] = useState("");
  const [cui, setCui] = useState("");
  const [companyType, setCompanyType] = useState<CompanyType>("GENERATOR");
  const [companyAddress, setCompanyAddress] = useState("");
  const [workPointName, setWorkPointName] = useState("");
  const [workPointAddress, setWorkPointAddress] = useState("");
  const [contactName, setContactName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [authNumber, setAuthNumber] = useState("");
  const [authExpiry, setAuthExpiry] = useState("");
  const [transportMeans, setTransportMeans] = useState("");
  const [transportLicenseNumber, setTransportLicenseNumber] = useState("");
  const [transportLicenseExpiry, setTransportLicenseExpiry] = useState("");
  const [marketRoles, setMarketRoles] = useState<MarketRole[]>([]);
  const [operationCodes, setOperationCodes] = useState<WasteOperationCode[]>([]);
  const [wasteCodesText, setWasteCodesText] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  // Only a business that takes waste from third parties has transport to declare.
  const asksTransport = companyType !== "GENERATOR";

  function toggleCode(code: WasteOperationCode) {
    setOperationCodes((prev) =>
      prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code]
    );
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!companyName.trim() || !cui.trim() || !contactEmail.trim()) {
      setError(strings.common.requiredField);
      return;
    }
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
      environmentalAuthNumber: authNumber.trim() || null,
      environmentalAuthExpiry: authExpiry || null,
      transportMeans: asksTransport ? transportMeans.trim() || null : null,
      transportLicenseNumber: asksTransport ? transportLicenseNumber.trim() || null : null,
      transportLicenseExpiry: asksTransport ? transportLicenseExpiry || null : null,
      marketRoles,
      operationCodes,
      wasteCodesText: wasteCodesText.trim() || null,
      notes: notes.trim() || null,
    };
    try {
      await submitMut.mutateAsync(input);
      setSent(true);
    } catch (err) {
      setError(apiErrorMessage(err, t.submitError));
    }
  }

  if (sent) {
    return (
      <div className="mx-auto flex min-h-screen max-w-xl flex-col items-center justify-center px-4 text-center">
        <CheckCircle2 className="h-12 w-12 text-emerald-600" />
        <h1 className="mt-4 text-2xl font-bold text-gray-900">{t.successTitle}</h1>
        <p className="mt-2 text-sm text-gray-600">{t.successBody}</p>
        <Link to="/login" className="mt-6 text-sm text-blue-600 hover:underline">
          {t.backToLogin}
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900">{t.title}</h1>
      <p className="mt-2 text-sm text-gray-600">{t.subtitle}</p>

      <form onSubmit={handleSubmit} className="mt-8 space-y-6">
        {error && (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}

        <Section title={t.sectionCompany}>
          <div>
            <Label htmlFor="ar-name">{t.companyName}</Label>
            <Input id="ar-name" value={companyName} onChange={(e) => setCompanyName(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="ar-cui">{t.cui}</Label>
              <Input
                id="ar-cui"
                value={cui}
                onChange={(e) => setCui(e.target.value)}
                placeholder={t.cuiPlaceholder}
              />
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
            <Label htmlFor="ar-address">{t.companyAddress}</Label>
            <Textarea
              id="ar-address"
              rows={2}
              value={companyAddress}
              onChange={(e) => setCompanyAddress(e.target.value)}
            />
          </div>
        </Section>

        <Section title={t.sectionWorkPoint}>
          <p className="text-xs text-gray-500">{t.workPointHint}</p>
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
        </Section>

        <Section title={t.sectionContact}>
          <p className="text-xs text-gray-500">{t.contactHint}</p>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="ar-contact-name">{t.contactName}</Label>
              <Input
                id="ar-contact-name"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="ar-contact-phone">{t.contactPhone}</Label>
              <Input
                id="ar-contact-phone"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
              />
            </div>
          </div>
          <div>
            <Label htmlFor="ar-contact-email">{t.contactEmail}</Label>
            <Input
              id="ar-contact-email"
              type="email"
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
            />
          </div>
        </Section>

        <Section title={t.sectionAuthorization}>
          <div className="grid grid-cols-2 gap-3">
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
        </Section>

        {asksTransport && (
          <Section title={t.sectionTransport}>
            <p className="text-xs text-gray-500">{t.transportHint}</p>
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
            <div className="grid grid-cols-2 gap-3">
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
          </Section>
        )}

        <Section title={t.sectionMarketRole}>
          <MarketRolePicker
            value={marketRoles}
            onChange={setMarketRoles}
            label={t.marketRoles}
            hint={t.marketRolesHint}
          />
        </Section>

        <Section title={t.sectionWaste}>
          <div>
            <Label htmlFor="ar-waste-text">{t.wasteCodesText}</Label>
            <Textarea
              id="ar-waste-text"
              rows={3}
              value={wasteCodesText}
              onChange={(e) => setWasteCodesText(e.target.value)}
              placeholder={t.wasteCodesTextPlaceholder}
            />
            <p className="mt-1 text-xs text-gray-500">{t.wasteCodesTextHint}</p>
          </div>

          <div>
            <span className="block text-sm font-medium text-gray-700">{t.operationCodes}</span>
            <p className="text-xs text-gray-500">{t.operationCodesHint}</p>
            <div className="mt-2 grid gap-3 sm:grid-cols-2">
              {[
                { title: t.recovery, codes: R_CODES },
                { title: t.disposal, codes: D_CODES },
              ].map((group) => (
                <div key={group.title}>
                  <span className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                    {group.title}
                  </span>
                  <div className="mt-1 max-h-48 space-y-1 overflow-y-auto rounded-md border border-gray-200 p-2">
                    {group.codes.map((c) => (
                      <label key={c} className="flex items-start gap-2 text-sm">
                        <input
                          type="checkbox"
                          className="mt-0.5 h-4 w-4 rounded border-gray-300"
                          checked={operationCodes.includes(c)}
                          onChange={() => toggleCode(c)}
                        />
                        <span className="text-gray-700">{codeLabels[c]}</span>
                      </label>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <Label htmlFor="ar-notes">{t.notes}</Label>
            <Textarea
              id="ar-notes"
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
        </Section>

        <div className="flex items-center justify-between border-t border-gray-200 pt-5">
          <Link to="/login" className="text-sm text-blue-600 hover:underline">
            {t.backToLogin}
          </Link>
          <Button type="submit" disabled={submitMut.isPending}>
            {submitMut.isPending ? t.submitting : t.submit}
          </Button>
        </div>
      </form>
    </div>
  );
}
