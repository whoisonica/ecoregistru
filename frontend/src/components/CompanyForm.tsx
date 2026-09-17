import { useState, type FormEvent } from "react";
import type {
  AfmContribution,
  Company,
  CompanyInput,
  CompanyType,
  PackagingOperatorRole,
  Unit,
} from "@/lib/types";
import { useUpdateCompany } from "@/hooks/useCompanies";
import { CompanyProfileFields, type CompanyProfileValue } from "@/components/CompanyProfileFields";
import { CuiField } from "@/components/AnafLookup";
import { apiErrorMessage } from "@/lib/api";
import { isValidCui } from "@/lib/cui";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { FormSection } from "@/components/ui/form-section";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SectionNav } from "@/components/ui/section-nav";
import { Select } from "@/components/ui/select";
import { useToast } from "@/components/ui/toast";

const t = strings.clients;
const typeLabels = strings.enums.companyType;

/**
 * Ordinea în care se citesc: lunar, anual — ca în art. 11. „Economia circulară" (trimestrial) a ieșit
 * din configurare (proprietarul, 16.09.2026): e a depozitelor de deșeuri, nu a clienților noștri.
 * `V60` a scos-o și de pe firmele care o aveau, iar serverul n-o mai salvează.
 */
const AFM_CONTRIBUTIONS: AfmContribution[] = ["WITHHOLDING_2_PERCENT", "PACKAGING"];
const COMPANY_TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];

/** `""` rămâne „nu s-a răspuns”, și e altceva decât „Nu” — vezi `handleSubmit`. */
function triState(value: boolean | null | undefined): "" | "yes" | "no" {
  return value == null ? "" : value ? "yes" : "no";
}

/**
 * F-D (todo-clienti-abonamente.md) — fișa firmei, pe tabul „Profil” al paginii `/clienti/:id`. Era dialogul `xl` din
 * Clienți, cu aceleași rubrici în aceeași ordine: s-a mutat pe pagină, cu cuprinsul sus, nu s-a rescris.
 *
 * <p>Rubricile pornesc din firmă o singură dată: pagina dă componentei cheia firmei, deci o altă firmă înseamnă
 * alt formular, iar reîncărcarea listei după salvare nu calcă ce scrie omul.
 */
export function CompanyForm({ company }: { company: Company }) {
  const updateMut = useUpdateCompany();
  const { notify } = useToast();
  const c = company;

  const [name, setName] = useState(c.name ?? "");
  const [cui, setCui] = useState(c.cui ?? "");
  const [type, setType] = useState<CompanyType>(c.type ?? "GENERATOR");
  const [afmObligation, setAfmObligation] = useState(!!c.afmObligation);
  const [afmContributions, setAfmContributions] = useState<AfmContribution[]>(
    (c.afmContributions ?? []).filter((a) => a !== "CIRCULAR_ECONOMY")
  );
  const [environmentalAuthNumber, setEnvironmentalAuthNumber] = useState(c.environmentalAuthNumber ?? "");
  const [environmentalAuthExpiry, setEnvironmentalAuthExpiry] = useState(c.environmentalAuthExpiry ?? "");
  const [address, setAddress] = useState(c.address ?? "");
  const [contactName, setContactName] = useState(c.contactName ?? "");
  const [contactEmail, setContactEmail] = useState(c.contactEmail ?? "");
  const [contactPhone, setContactPhone] = useState(c.contactPhone ?? "");
  const [tradeRegisterNumber, setTradeRegisterNumber] = useState(c.tradeRegisterNumber ?? "");
  const [anexa3Series, setAnexa3Series] = useState(c.anexa3Series ?? "");
  // Header rubrics of the annual declaration. Blank prints blank on the form — the sheet never
  // guesses a CAEN code or a job title.
  const [caenCode, setCaenCode] = useState(c.caenCode ?? "");
  const [anexa3Unit, setAnexa3Unit] = useState<"" | Unit>(c.anexa3Unit ?? "");
  const [contactRole, setContactRole] = useState(c.contactRole ?? "");
  // Calitatea din Ordinul 794/2012 art. 4 alin. (1). Priveşte doar operatorii care preiau deşeuri de
  // ambalaje de la terţi.
  const [packagingOperatorRole, setPackagingOperatorRole] = useState<"" | PackagingOperatorRole>(
    c.packagingOperatorRole ?? ""
  );
  // Persoana desemnată cu gestiunea deșeurilor (OUG 92/2021 art. 23 alin. (4)-(5)). Altceva decât
  // contactRole de mai sus, care e blocul de semnătură al declarației anuale.
  const [wasteManagerName, setWasteManagerName] = useState(c.wasteManagerName ?? "");
  const [wasteManagerRole, setWasteManagerRole] = useState(c.wasteManagerRole ?? "");
  const [wasteManagerExternal, setWasteManagerExternal] = useState(triState(c.wasteManagerExternal));
  const [wasteManagerTraining, setWasteManagerTraining] = useState(c.wasteManagerTraining ?? "");
  // Trei stări: "" nu e „Nu", e „nimeni n-a întrebat" — iar din asta atârnă dacă pleacă alerta de 30 aprilie.
  const [constructionPermitHolder, setConstructionPermitHolder] = useState(triState(c.constructionPermitHolder));
  // The answers from the client's intake form. Empty is a valid answer: nothing is narrowed.
  const [profile, setProfile] = useState<CompanyProfileValue>({
    authorizedOperationCodes: c.authorizedOperationCodes ?? [],
    marketRoles: c.marketRoles ?? [],
    authorizedWasteCodes: c.authorizedWasteCodes ?? [],
    transportMeans: c.transportMeans ?? "",
    transportLicenseNumber: c.transportLicenseNumber ?? "",
    transportLicenseExpiry: c.transportLicenseExpiry ?? "",
  });
  const [formError, setFormError] = useState<false | "name" | "cui" | "cuiInvalid">(false);
  const isSubmitting = updateMut.isPending;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setFormError("name");
      document.getElementById("firma-identificare")?.scrollIntoView();
      return;
    }
    if (!cui.trim()) {
      setFormError("cui");
      document.getElementById("firma-identificare")?.scrollIntoView();
      return;
    }
    if (!isValidCui(cui)) {
      setFormError("cuiInvalid");
      document.getElementById("firma-identificare")?.scrollIntoView();
      return;
    }
    const input: CompanyInput = {
      name: name.trim(),
      cui: cui.trim(),
      type,
      afmObligation,
      afmContributions,
      environmentalAuthNumber: environmentalAuthNumber.trim() || null,
      environmentalAuthExpiry: environmentalAuthExpiry || null,
      address: address.trim() || null,
      contactName: contactName.trim() || null,
      contactEmail: contactEmail.trim() || null,
      contactPhone: contactPhone.trim() || null,
      tradeRegisterNumber: tradeRegisterNumber.trim() || null,
      anexa3Series: anexa3Series.trim() || null,
      caenCode: caenCode.trim() || null,
      anexa3Unit: anexa3Unit || null,
      contactRole: contactRole.trim() || null,
      packagingOperatorRole: packagingOperatorRole || null,
      wasteManagerName: wasteManagerName.trim() || null,
      wasteManagerRole: wasteManagerRole.trim() || null,
      // "" rămâne null: „nu s-a răspuns" nu e același lucru cu „angajat propriu".
      wasteManagerExternal: wasteManagerExternal === "" ? null : wasteManagerExternal === "yes",
      wasteManagerTraining: wasteManagerTraining.trim() || null,
      constructionPermitHolder:
        constructionPermitHolder === "" ? null : constructionPermitHolder === "yes",
      authorizedOperationCodes: profile.authorizedOperationCodes,
      marketRoles: profile.marketRoles,
      authorizedWasteCodeIds: profile.authorizedWasteCodes.map((w) => w.id),
      transportMeans: profile.transportMeans.trim() || null,
      transportLicenseNumber: profile.transportLicenseNumber.trim() || null,
      transportLicenseExpiry: profile.transportLicenseExpiry || null,
    };
    try {
      await updateMut.mutateAsync({ id: company.id, input });
      notify(t.updated, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  return (
    <form id="company-form" onSubmit={handleSubmit} noValidate>
      <SectionNav
        label={t.profileNav}
        items={[
          { id: "firma-identificare", label: t.groupIdentity },
          { id: "firma-autorizatie", label: t.navAuthorization },
          { id: "firma-persoana-desemnata", label: t.navWasteManager },
          { id: "firma-obligatii", label: t.navObligations },
          { id: "firma-raportare", label: t.groupReporting },
          { id: "firma-contact", label: t.navContact },
          { id: "firma-activitate", label: t.navActivity },
        ]}
      />
      <div className="mt-4 max-w-3xl space-y-8">
        <div id="firma-identificare" className="scroll-mt-24">
<FormSection title={t.groupIdentity}>
          <div>
            <Label htmlFor="c-name">{t.name}</Label>
            <Input
              id="c-name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (formError === "name") setFormError(false);
              }}
              placeholder={t.namePlaceholder}
            />
            {formError === "name" && (
              <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
            )}
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <CuiField
                id="c-cui"
                label={t.cui}
                value={cui}
                onChange={(v) => {
                  setCui(v);
                  if (formError === "cui" || formError === "cuiInvalid") setFormError(false);
                }}
                placeholder={t.cuiPlaceholder}
                error={
                  (formError === "cui" || formError === "cuiInvalid") && (
                    <p className="mt-1 text-xs text-red-600">
                      {formError === "cui" ? strings.common.requiredField : strings.common.cuiInvalid}
                    </p>
                  )
                }
                targets={[
                  {
                    label: strings.partners.anafFieldName,
                    current: name,
                    pick: (f) => f.name,
                    set: (v) => {
                      setName(v);
                      if (formError === "name") setFormError(false);
                    },
                  },
                  { label: strings.partners.anafFieldAddress, current: address, pick: (f) => f.address, set: setAddress },
                  {
                    label: strings.partners.anafFieldRegistry,
                    current: tradeRegisterNumber,
                    pick: (f) => f.tradeRegisterNumber,
                    set: setTradeRegisterNumber,
                  },
                  { label: strings.partners.anafFieldCaen, current: caenCode, pick: (f) => f.caenCode, set: setCaenCode },
                ]}
              />
            </div>
            <div>
              <Label htmlFor="c-reg">{strings.settings.company.tradeRegisterNumber}</Label>
              <Input
                id="c-reg"
                value={tradeRegisterNumber}
                onChange={(e) => setTradeRegisterNumber(e.target.value)}
                placeholder={strings.partners.tradeRegisterNumberPlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="c-type">{t.type}</Label>
              <Select
                id="c-type"
                value={type}
                onChange={(e) => setType(e.target.value as CompanyType)}
              >
                {COMPANY_TYPES.map((ct) => (
                  <option key={ct} value={ct}>
                    {typeLabels[ct]}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="c-caen">{t.caenCode}</Label>
              <Input
                id="c-caen"
                value={caenCode}
                onChange={(e) => setCaenCode(e.target.value)}
                placeholder={t.caenCodePlaceholder}
              />
              <p className="mt-1 text-xs text-content-muted">{t.caenCodeHint}</p>
            </div>
          </div>
          <div>
            <Label htmlFor="c-address">{t.address}</Label>
            <Input id="c-address" value={address} onChange={(e) => setAddress(e.target.value)} />
          </div>
        </FormSection>
</div>

        <div id="firma-autorizatie" className="scroll-mt-24">
<FormSection title={t.groupAuthorization}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="c-auth-number">{t.environmentalAuthNumber}</Label>
              <Input
                id="c-auth-number"
                value={environmentalAuthNumber}
                onChange={(e) => setEnvironmentalAuthNumber(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="c-auth-expiry">{t.environmentalAuthExpiry}</Label>
              <DateInput
                id="c-auth-expiry"
                value={environmentalAuthExpiry}
                onChange={(e) => setEnvironmentalAuthExpiry(e.target.value)}
              />
            </div>
          </div>
        </FormSection>
</div>

        {/* Persoana desemnată cu gestiunea deșeurilor — secțiune separată, fiindcă e altceva
            decât persoana de contact de mai jos și se confundă ușor cu ea. */}
        <div id="firma-persoana-desemnata" className="scroll-mt-24">
<FormSection title={t.groupWasteManager} description={t.wasteManagerHint}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="c-wm-name">{t.wasteManagerName}</Label>
              <Input
                id="c-wm-name"
                value={wasteManagerName}
                onChange={(e) => setWasteManagerName(e.target.value)}
                placeholder={t.wasteManagerNamePlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="c-wm-role">{t.wasteManagerRole}</Label>
              <Input
                id="c-wm-role"
                value={wasteManagerRole}
                onChange={(e) => setWasteManagerRole(e.target.value)}
                placeholder={t.wasteManagerRolePlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="c-wm-external">{t.wasteManagerExternal}</Label>
              <Select
                id="c-wm-external"
                value={wasteManagerExternal}
                onChange={(e) =>
                  setWasteManagerExternal(e.target.value as "" | "yes" | "no")
                }
              >
                <option value="">{t.wasteManagerExternalUnset}</option>
                <option value="no">{t.wasteManagerExternalNo}</option>
                <option value="yes">{t.wasteManagerExternalYes}</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="c-wm-training">{t.wasteManagerTraining}</Label>
              <Input
                id="c-wm-training"
                value={wasteManagerTraining}
                onChange={(e) => setWasteManagerTraining(e.target.value)}
                placeholder={t.wasteManagerTrainingPlaceholder}
              />
              <p className="mt-1 text-xs text-content-muted">{t.wasteManagerTrainingHint}</p>
            </div>
          </div>
        </FormSection>
</div>

        {/* Jumătatea de profil a termenului de 30 aprilie (art. 49 alin. (9)). Stă aici, lângă
            raportare, fiindcă asta e: o întrebare al cărei singur efect e o alertă. Cealaltă
            jumătate — uleiurile uzate — se citește din mișcări şi n-are rubrică. */}
        <div id="firma-obligatii" className="scroll-mt-24">
<FormSection title={t.groupObligations}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="c-construction-permit">{t.constructionPermitHolder}</Label>
              <Select
                id="c-construction-permit"
                value={constructionPermitHolder}
                onChange={(e) =>
                  setConstructionPermitHolder(e.target.value as "" | "yes" | "no")
                }
              >
                <option value="">{t.constructionPermitHolderUnset}</option>
                <option value="no">{t.constructionPermitHolderNo}</option>
                <option value="yes">{t.constructionPermitHolderYes}</option>
              </Select>
              <p className="mt-1 text-xs text-content-muted">{t.constructionPermitHolderHint}</p>
            </div>
          </div>
        </FormSection>
</div>

        <div id="firma-raportare" className="scroll-mt-24">
<FormSection title={t.groupReporting}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="c-anexa3-series">{t.anexa3Series}</Label>
              <Input
                id="c-anexa3-series"
                value={anexa3Series}
                onChange={(e) => setAnexa3Series(e.target.value)}
                placeholder={t.anexa3SeriesPlaceholder}
              />
              <p className="mt-1 text-xs text-content-muted">{t.anexa3SeriesHint}</p>
            </div>
            <div>
              <Label htmlFor="c-a3unit">{t.anexa3Unit}</Label>
              <Select
                id="c-a3unit"
                value={anexa3Unit}
                onChange={(e) => setAnexa3Unit(e.target.value as "" | Unit)}
              >
                <option value="">{t.anexa3UnitAsRecorded}</option>
                <option value="KG">{t.anexa3UnitKg}</option>
                <option value="TONS">{t.anexa3UnitTons}</option>
              </Select>
              <p className="mt-1 text-xs text-content-muted">{t.anexa3UnitHint}</p>
            </div>
          </div>
          {/* Calitatea decide tabelul Anexei 3 Ambalaje — ambalaje preluate de la terți, pe care un
              generator nu le are (specialista, 14.09.2026). */}
          {type !== "GENERATOR" && (
          <div>
            <Label htmlFor="c-pkg-role">{strings.packagingOperatorRole.label}</Label>
            <Select
              id="c-pkg-role"
              value={packagingOperatorRole}
              onChange={(e) =>
                setPackagingOperatorRole(e.target.value as "" | PackagingOperatorRole)
              }
            >
              <option value="">{strings.packagingOperatorRole.none}</option>
              <option value="COLECTOR">{strings.packagingOperatorRole.COLECTOR}</option>
              <option value="COMERCIANT">{strings.packagingOperatorRole.COMERCIANT}</option>
              <option value="RECICLATOR">{strings.packagingOperatorRole.RECICLATOR}</option>
              <option value="VALORIFICATOR">
                {strings.packagingOperatorRole.VALORIFICATOR}
              </option>
            </Select>
            <p className="mt-1 text-xs text-content-muted">{strings.packagingOperatorRole.hint}</p>
          </div>
          )}
          <div className="rounded-md border border-line bg-surface-muted p-3">
            <span className="block text-sm font-medium text-content-strong">{t.afmContributions}</span>
            <p className="mt-0.5 text-xs text-content-muted">{t.afmContributionsHint}</p>
            <div className="mt-2 space-y-2">
              {AFM_CONTRIBUTIONS.map((contribution) => (
                <label key={contribution} className="flex items-start gap-2 text-sm">
                  <input
                    type="checkbox"
                    className="mt-0.5 h-4 w-4 rounded border-line-strong text-brand focus:ring-brand"
                    checked={afmContributions.includes(contribution)}
                    onChange={() =>
                      setAfmContributions((prev) =>
                        prev.includes(contribution)
                          ? prev.filter((x) => x !== contribution)
                          : [...prev, contribution]
                      )
                    }
                  />
                  <span>
                    <span className="font-medium text-content-strong">
                      {strings.enums.afmContribution[contribution]}
                    </span>
                    <span className="block text-xs text-content-muted">
                      {strings.enums.afmContribution[`${contribution}_HINT`]}
                    </span>
                  </span>
                </label>
              ))}
            </div>
            {afmContributions.length === 0 && (
              <label className="mt-3 flex items-center gap-2 border-t border-line pt-2 text-sm text-content-strong">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-line-strong text-brand focus:ring-brand"
                  checked={afmObligation}
                  onChange={(e) => setAfmObligation(e.target.checked)}
                />
                {t.afmLabel}
              </label>
            )}
          </div>
        </FormSection>
</div>

        <div id="firma-contact" className="scroll-mt-24">
<FormSection title={t.groupContact}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="c-contact-name">{t.contactName}</Label>
              <Input
                id="c-contact-name"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="c-contact-role">{t.contactRole}</Label>
              <Input
                id="c-contact-role"
                value={contactRole}
                onChange={(e) => setContactRole(e.target.value)}
                placeholder={t.contactRolePlaceholder}
              />
              <p className="mt-1 text-xs text-content-muted">{t.contactRoleHint}</p>
            </div>
            <div>
              <Label htmlFor="c-contact-email">{t.contactEmail}</Label>
              <Input
                id="c-contact-email"
                type="email"
                value={contactEmail}
                onChange={(e) => setContactEmail(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="c-contact-phone">{t.contactPhone}</Label>
              <Input
                id="c-contact-phone"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
              />
            </div>
          </div>
        </FormSection>
</div>

        <div id="firma-activitate" className="scroll-mt-24">
<CompanyProfileFields value={profile} onChange={setProfile} companyType={type} />
</div>
      </div>
      {/* Salvarea stă lipită jos: fișa e lungă, iar butonul de la capăt nu se vedea din prima secțiune. */}
      <div className="sticky bottom-0 z-20 -mx-4 mt-8 flex max-w-3xl items-center justify-end gap-2 border-t border-line bg-surface px-4 py-3 sm:mx-0 sm:px-0">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? strings.common.saving : strings.common.save}
        </Button>
      </div>
    </form>
  );
}
