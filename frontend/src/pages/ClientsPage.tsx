import { useState, type FormEvent } from "react";
import { Building2, Pencil, Plus, UserPlus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  useCompanies,
  useCreateCompany,
  useUpdateCompany,
  useInviteUser,
} from "@/hooks/useCompanies";
import type {
  AfmContribution,
  Company,
  CompanyInput,
  CompanyType,
  InviteRole,
  InviteUserInput,
  PackagingOperatorRole,
  Unit,
} from "@/lib/types";
import {
  CompanyProfileFields,
  emptyCompanyProfile,
  type CompanyProfileValue,
} from "@/components/CompanyProfileFields";
import { AccountRequestsSection } from "@/components/AccountRequestsSection";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";

const t = strings.clients;
const typeLabels = strings.enums.companyType;
const roleLabels = strings.enums.inviteRole;

/** Ordinea în care se citesc: lunar, trimestrial, anual — ca în art. 11. */
const AFM_CONTRIBUTIONS: AfmContribution[] = [
  "WITHHOLDING_2_PERCENT",
  "CIRCULAR_ECONOMY",
  "PACKAGING",
];
const COMPANY_TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];
const INVITE_ROLES: InviteRole[] = ["ADMIN", "OPERATOR", "CLIENT_VIEWER"];

export function ClientsPage() {
  const { user } = useAuth();
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";

  const { data: companies, isLoading, isError } = useCompanies(!!isPlatformAdmin);
  const createMut = useCreateCompany();
  const updateMut = useUpdateCompany();
  const inviteMut = useInviteUser();
  const { notify } = useToast();

  // --- company create/edit dialog ---
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Company | null>(null);
  const [name, setName] = useState("");
  const [cui, setCui] = useState("");
  const [type, setType] = useState<CompanyType>("GENERATOR");
  const [afmObligation, setAfmObligation] = useState(false);
  const [afmContributions, setAfmContributions] = useState<AfmContribution[]>([]);
  const [environmentalAuthNumber, setEnvironmentalAuthNumber] = useState("");
  const [environmentalAuthExpiry, setEnvironmentalAuthExpiry] = useState("");
  const [address, setAddress] = useState("");
  const [contactName, setContactName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [tradeRegisterNumber, setTradeRegisterNumber] = useState("");
  const [anexa3Series, setAnexa3Series] = useState("");
  // Header rubrics of the annual declaration. Blank prints blank on the form — the sheet never
  // guesses a CAEN code or a job title.
  const [caenCode, setCaenCode] = useState("");
  const [anexa3Unit, setAnexa3Unit] = useState<"" | Unit>("");
  const [contactRole, setContactRole] = useState("");
  // Persoana desemnată cu gestiunea deșeurilor (OUG 92/2021 art. 23 alin. (4)-(5)). Altceva decât
  // contactRole de mai sus, care e blocul de semnătură al declarației anuale.
  // Calitatea din Ordinul 794/2012 art. 4 alin. (1). Nu se cere la cererea de cont: priveşte doar
  // operatorii care preiau deşeuri de ambalaje de la terţi, iar pe formularul pe care îl completează
  // orice generator ar fi o întrebare care nu i se aplică.
  const [packagingOperatorRole, setPackagingOperatorRole] =
    useState<"" | PackagingOperatorRole>("");
  const [wasteManagerName, setWasteManagerName] = useState("");
  const [wasteManagerRole, setWasteManagerRole] = useState("");
  const [wasteManagerExternal, setWasteManagerExternal] = useState<"" | "yes" | "no">("");
  const [wasteManagerTraining, setWasteManagerTraining] = useState("");
  // The answers from the client's intake form. Empty is a valid answer: nothing is narrowed.
  const [profile, setProfile] = useState<CompanyProfileValue>(emptyCompanyProfile);
  const [formError, setFormError] = useState<false | "name" | "cui">(false);

  // --- invite-user dialog ---
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteCompany, setInviteCompany] = useState<Company | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<InviteRole>("OPERATOR");
  const [inviteFirstName, setInviteFirstName] = useState("");
  const [inviteLastName, setInviteLastName] = useState("");
  const [inviteEmailError, setInviteEmailError] = useState(false);

  const view = useTableView(companies ?? [], {
    searchText: (c) => [c.name, c.cui].filter(Boolean).join(" "),
    comparators: {
      name: (a, b) => a.name.localeCompare(b.name, "ro"),
      cui: (a, b) => (a.cui ?? "").localeCompare(b.cui ?? "", "ro"),
    },
    initialSort: { key: "name", direction: "asc" },
  });

  const isSubmitting = createMut.isPending || updateMut.isPending;

  if (!isPlatformAdmin) {
    return (
      <div>
        <PageHeader title={t.title} description={t.onlyPlatformAdmin} />
      </div>
    );
  }

  /**
   * Umple formularul din firma dată, sau îl golește de tot când nu e niciuna.
   *
   * <p>Erau două funcții — una pentru „adaugă", una pentru „editează" — ținute sincronizate cu
   * mâna, iar cea de adăugare rămăsese în urmă cu **șapte rubrici**: contribuțiile AFM, unitatea de
   * pe Anexa 3, calitatea de la Anexa 3 Ambalaje și toate cele patru ale persoanei desemnate. Deci
   * editai firma A, închideai, apăsai „Adaugă firmă" — și firma B se năștea cu persoana desemnată a
   * firmei A, care se tipărește în dosarul ei de control, și cu calitatea care decide **care tabel**
   * din Anexa 3 Ambalaje i se tipărește.
   *
   * <p>Ce era greșit nu erau cele șapte rânduri lipsă, ci că se puteau lipsi: două liste care
   * trebuie să acopere aceleași rubrici ajung mereu să nu le mai acopere. Aici e una singură, iar
   * `null` e chiar cazul „firmă nouă", cu implicitele scrise o dată.
   */
  function fillForm(c: Company | null) {
    setEditing(c);
    setName(c?.name ?? "");
    setCui(c?.cui ?? "");
    setType(c?.type ?? "GENERATOR");
    setAfmObligation(!!c?.afmObligation);
    setAfmContributions(c?.afmContributions ?? []);
    setEnvironmentalAuthNumber(c?.environmentalAuthNumber ?? "");
    setEnvironmentalAuthExpiry(c?.environmentalAuthExpiry ?? "");
    setAddress(c?.address ?? "");
    setContactName(c?.contactName ?? "");
    setContactEmail(c?.contactEmail ?? "");
    setContactPhone(c?.contactPhone ?? "");
    setTradeRegisterNumber(c?.tradeRegisterNumber ?? "");
    setAnexa3Series(c?.anexa3Series ?? "");
    setCaenCode(c?.caenCode ?? "");
    setAnexa3Unit(c?.anexa3Unit ?? "");
    setContactRole(c?.contactRole ?? "");
    setPackagingOperatorRole(c?.packagingOperatorRole ?? "");
    setWasteManagerName(c?.wasteManagerName ?? "");
    setWasteManagerRole(c?.wasteManagerRole ?? "");
    // "" rămâne "nu s-a răspuns", și e altceva decât "angajat propriu" — vezi handleSubmit.
    setWasteManagerExternal(
      c?.wasteManagerExternal == null ? "" : c.wasteManagerExternal ? "yes" : "no",
    );
    setWasteManagerTraining(c?.wasteManagerTraining ?? "");
    setProfile(
      c
        ? {
            authorizedOperationCodes: c.authorizedOperationCodes ?? [],
            marketRoles: c.marketRoles ?? [],
            authorizedWasteCodes: c.authorizedWasteCodes ?? [],
            transportMeans: c.transportMeans ?? "",
            transportLicenseNumber: c.transportLicenseNumber ?? "",
            transportLicenseExpiry: c.transportLicenseExpiry ?? "",
          }
        : emptyCompanyProfile
    );
    setFormError(false);
    setDialogOpen(true);
  }

  function openCreate() {
    fillForm(null);
  }

  function openEdit(c: Company) {
    fillForm(c);
  }

  /**
   * Capătul celălalt al fluxului de cereri: din rândul „Cont creat" se ajunge la firma pe care a
   * născut-o, care stă în tabelul de deasupra dar putea fi la al treizecilea rând sau pe altă
   * pagină a listei.
   *
   * <p>Când firma nu e în listă — s-a creat de altcineva de la ultima încărcare — se spune, nu se
   * deschide un formular gol: un dialog cu rubricile goale ar arăta ca o firmă fără date.
   */
  function openCompanyById(companyId: string) {
    const found = (companies ?? []).find((c) => c.id === companyId);
    if (!found) {
      notify(strings.accountRequest.openCompanyMissing, "error");
      return;
    }
    openEdit(found);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setFormError("name");
      return;
    }
    if (!cui.trim()) {
      setFormError("cui");
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
      authorizedOperationCodes: profile.authorizedOperationCodes,
      marketRoles: profile.marketRoles,
      authorizedWasteCodeIds: profile.authorizedWasteCodes.map((w) => w.id),
      transportMeans: profile.transportMeans.trim() || null,
      transportLicenseNumber: profile.transportLicenseNumber.trim() || null,
      transportLicenseExpiry: profile.transportLicenseExpiry || null,
    };
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, input });
        notify(t.updated, "success");
      } else {
        await createMut.mutateAsync(input);
        notify(t.created, "success");
      }
      setDialogOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function openInvite(c: Company) {
    setInviteCompany(c);
    setInviteEmail("");
    setInviteRole("OPERATOR");
    setInviteFirstName("");
    setInviteLastName("");
    setInviteEmailError(false);
    setInviteOpen(true);
  }

  async function handleInvite(e: FormEvent) {
    e.preventDefault();
    if (!inviteEmail.trim()) {
      setInviteEmailError(true);
      return;
    }
    if (!inviteCompany) return;
    const input: InviteUserInput = {
      email: inviteEmail.trim(),
      role: inviteRole,
      firstName: inviteFirstName.trim() || null,
      lastName: inviteLastName.trim() || null,
    };
    try {
      await inviteMut.mutateAsync({ id: inviteCompany.id, input });
      notify(t.invited, "success");
      setInviteOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.inviteError), "error");
    }
  }

  return (
    <div>
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        }
      />

      <section className="mt-6">
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isError && (
          <>
            <TableToolbar view={view} placeholder={t.searchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                    {t.name}
                  </SortableTH>
                  <SortableTH sortKey="cui" sort={view.sort} onSort={view.toggleSort}>
                    {t.cui}
                  </SortableTH>
                  <TH>{t.type}</TH>
                  <TH>{t.afm}</TH>
                  <TH>{strings.common.status}</TH>
                  <TH sticky="right" className="text-right">{strings.common.actions}</TH>
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={6}
                    loading={isLoading}
                    icon={Building2}
                    title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                    description={
                      view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                    }
                    action={
                      <Button onClick={openCreate}>
                        <Plus className="mr-2 h-4 w-4" />
                        {t.add}
                      </Button>
                    }
                  />
                )}
                {view.visible.map((c) => (
                  <TR key={c.id}>
                    <TD className="font-medium text-content">{c.name}</TD>
                    <TD>{c.cui}</TD>
                    <TD>{typeLabels[c.type]}</TD>
                    <TD>
                      {c.afmObligation ? (
                        <Badge variant="warning">{t.afmYes}</Badge>
                      ) : (
                        <span className="text-content-subtle">{t.afmNo}</span>
                      )}
                    </TD>
                    <TD>
                      {c.active ? (
                        <Badge variant="success">{t.active}</Badge>
                      ) : (
                        <Badge variant="muted">{t.inactive}</Badge>
                      )}
                    </TD>
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openInvite(c)}>
                          <UserPlus className="mr-1 h-3.5 w-3.5" />
                          {t.invite}
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => openEdit(c)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                      </div>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
            <TablePagination view={view} />
          </>
        )}
      </section>

      {/* Create / edit company */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.editTitle : t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="company-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="company-form" onSubmit={handleSubmit} className="space-y-4">
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
              autoFocus
            />
            {formError === "name" && (
              <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
            )}
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="c-cui">{t.cui}</Label>
              <Input
                id="c-cui"
                value={cui}
                onChange={(e) => {
                  setCui(e.target.value);
                  if (formError === "cui") setFormError(false);
                }}
                placeholder={t.cuiPlaceholder}
              />
              {formError === "cui" && (
                <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
              )}
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
          </div>
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
          <div>
            <Label htmlFor="c-address">{t.address}</Label>
            <Input id="c-address" value={address} onChange={(e) => setAddress(e.target.value)} />
          </div>
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
              <Label htmlFor="c-contact-phone">{t.contactPhone}</Label>
              <Input
                id="c-contact-phone"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
              />
            </div>
          </div>
          {/* The two rubrics the annual declaration's header and signature block need. */}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
            <div className="col-span-2">
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
          </div>

          {/* Persoana desemnată cu gestiunea deșeurilor — bloc separat, fiindcă e altceva decât
              persoana de contact de mai sus și se confundă ușor cu ea. */}
          <div className="rounded-lg border border-line p-3">
            <p className="text-sm font-semibold text-content-strong">{t.wasteManagerTitle}</p>
            <p className="mt-1 text-xs text-content-muted">{t.wasteManagerHint}</p>
            <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
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
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="c-reg">{strings.partners.tradeRegisterNumber}</Label>
              <Input
                id="c-reg"
                value={tradeRegisterNumber}
                onChange={(e) => setTradeRegisterNumber(e.target.value)}
                placeholder={strings.partners.tradeRegisterNumberPlaceholder}
              />
            </div>
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

          <CompanyProfileFields value={profile} onChange={setProfile} companyType={type} />
        </form>
      </Dialog>

      {/* Invite user */}
      <Dialog
        open={inviteOpen}
        onClose={() => setInviteOpen(false)}
        title={t.inviteTitle.replace("{company}", inviteCompany?.name ?? "")}
        footer={
          <>
            <Button variant="outline" onClick={() => setInviteOpen(false)} disabled={inviteMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="invite-form" disabled={inviteMut.isPending}>
              {inviteMut.isPending ? strings.common.saving : t.inviteSubmit}
            </Button>
          </>
        }
      >
        <form id="invite-form" onSubmit={handleInvite} className="space-y-4">
          <p className="text-xs text-content-muted">{t.inviteHint}</p>
          <div>
            <Label htmlFor="i-email">{t.inviteEmail}</Label>
            <Input
              id="i-email"
              type="email"
              value={inviteEmail}
              onChange={(e) => {
                setInviteEmail(e.target.value);
                if (inviteEmailError) setInviteEmailError(false);
              }}
              placeholder={t.inviteEmailPlaceholder}
              autoFocus
            />
            {inviteEmailError && (
              <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
            )}
          </div>
          <div>
            <Label htmlFor="i-role">{t.inviteRole}</Label>
            <Select
              id="i-role"
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value as InviteRole)}
            >
              {INVITE_ROLES.map((r) => (
                <option key={r} value={r}>
                  {roleLabels[r]}
                </option>
              ))}
            </Select>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="i-first">{t.inviteFirstName}</Label>
              <Input
                id="i-first"
                value={inviteFirstName}
                onChange={(e) => setInviteFirstName(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="i-last">{t.inviteLastName}</Label>
              <Input
                id="i-last"
                value={inviteLastName}
                onChange={(e) => setInviteLastName(e.target.value)}
              />
            </div>
          </div>
        </form>
      </Dialog>

      <AccountRequestsSection enabled={isPlatformAdmin} onOpenCompany={openCompanyById} />
    </div>
  );
}
