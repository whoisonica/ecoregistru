import { useMemo, useState, type FormEvent } from "react";
import { Briefcase, Building2, Plus, Receipt, UserPlus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { isMultiCompany } from "@/lib/roles";
import { AssignConsultancyDialog, ConsultanciesSection } from "@/components/ConsultanciesSection";
import { SubscriptionDialog } from "@/components/SubscriptionDialog";
import { ConsultancyTeamSection } from "@/components/ConsultancyTeamSection";
import { ConsultancyBrandingSection } from "@/components/ConsultancyBrandingSection";
import {
  useClientOverview,
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
import { useAccountRequests } from "@/hooks/useAccountRequests";
import { useInvoiceMoney } from "@/hooks/useSubscriptions";
import { useConsultancies } from "@/hooks/useConsultancies";
import { useUrlState } from "@/hooks/useUrlState";
import { useHotkey } from "@/hooks/useHotkey";
import {
  byAttention,
  clientRow,
  CLIENT_FILTERS,
  CONSULTANT_FILTERS,
  MATCHES,
  type ClientFilter,
  type ClientRow,
} from "@/lib/clients";
import { countOf } from "@/lib/count";
import { formatDate } from "@/lib/utils";
import { Card } from "@/components/ui/card";
import { Menu, MenuItem } from "@/components/ui/menu";
import { PillGroup } from "@/components/ui/pill-group";
import { Tooltip } from "@/components/ui/tooltip";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { isValidCui } from "@/lib/cui";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { CuiField } from "@/components/AnafLookup";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { FormSection } from "@/components/ui/form-section";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";

const t = strings.clients;
const typeLabels = strings.enums.companyType;
const roleLabels = strings.enums.inviteRole;

/**
 * Ordinea în care se citesc: lunar, anual — ca în art. 11. „Economia circulară" (trimestrial) a ieșit
 * din configurare (proprietarul, 16.09.2026): e a depozitelor de deșeuri, nu a clienților noștri.
 * `V60` a scos-o și de pe firmele care o aveau, iar serverul n-o mai salvează.
 */
const AFM_CONTRIBUTIONS: AfmContribution[] = ["WITHHOLDING_2_PERCENT", "PACKAGING"];
const COMPANY_TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];
const INVITE_ROLES: InviteRole[] = ["ADMIN", "OPERATOR", "CLIENT_VIEWER"];

export function ClientsPage() {
  const { user } = useAuth();
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";
  // P2.13 — consultantul ajunge și el aici: aceleași firme, restrânse de server la cabinetul lui.
  const isConsultant = user?.role === "CONSULTANT";
  const multiCompany = isMultiCompany(user?.role);

  const { data: companies, isLoading, isError } = useCompanies(multiCompany);
  const [assigning, setAssigning] = useState<Company | null>(null);
  const [billing, setBilling] = useState<Company | null>(null);
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
  // Trei stări, ca `wasteManagerExternal`: "" nu e „Nu", e „nimeni n-a întrebat" — iar din asta
  // atârnă dacă pleacă sau nu alerta de 30 aprilie.
  const [constructionPermitHolder, setConstructionPermitHolder] = useState<"" | "yes" | "no">("");
  // The answers from the client's intake form. Empty is a valid answer: nothing is narrowed.
  const [profile, setProfile] = useState<CompanyProfileValue>(emptyCompanyProfile);
  const [formError, setFormError] = useState<false | "name" | "cui" | "cuiInvalid">(false);

  // --- invite-user dialog ---
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteCompany, setInviteCompany] = useState<Company | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<InviteRole>("OPERATOR");
  const [inviteFirstName, setInviteFirstName] = useState("");
  const [inviteLastName, setInviteLastName] = useState("");
  const [inviteEmailError, setInviteEmailError] = useState(false);

  // F-B — abonamentul, ultima factură și oamenii fiecărei firme; banii și cererile doar la platformă.
  const overview = useClientOverview(multiCompany);
  const money = useInvoiceMoney(isPlatformAdmin);
  const requests = useAccountRequests(isPlatformAdmin);
  const consultancies = useConsultancies(isPlatformAdmin);
  const [tabParam, setTab] = useUrlState("tab");
  const newRequests = (requests.data ?? []).filter((r) => r.status === "NEW").length;
  const [filter, setFilter] = useState<ClientFilter>("ALL");
  const today = todayIso();

  const rows = useMemo(() => {
    const byId = new Map((overview.data ?? []).map((o) => [o.companyId, o]));
    return (companies ?? []).map((c) => clientRow(c, byId.get(c.id), today)).sort(byAttention);
  }, [companies, overview.data, today]);
  const filters = isPlatformAdmin ? CLIENT_FILTERS : CONSULTANT_FILTERS;
  const counts = Object.fromEntries(filters.map((f) => [f, rows.filter(MATCHES[f]).length])) as Record<
    ClientFilter,
    number
  >;

  const view = useTableView(
    rows.filter(MATCHES[filter]),
    { searchText: (r) => [r.company.name, r.company.cui].filter(Boolean).join(" ") }
  );

  // Taburile (F-B2): o singură secțiune pe ecran, ca pagina să se termine sub tabel. Tabul stă în adresă.
  const tabs: { id: string; label: string; count?: number; alert?: boolean }[] = isPlatformAdmin
    ? [
        { id: "", label: t.tabClients, count: companies?.length },
        { id: "cereri", label: t.tabRequests, count: requests.data ? newRequests : undefined, alert: newRequests > 0 },
        { id: "cabinete", label: t.tabCabinets, count: consultancies.data?.length },
      ]
    : [
        { id: "", label: t.tabClients, count: companies?.length },
        { id: "echipa", label: t.tabTeam },
        { id: "antet", label: t.tabBranding },
      ];
  const tab = tabs.some((x) => x.id === tabParam) ? tabParam : "";

  useHotkey("n", () => openCreate(), { enabled: multiCompany && tab === "" && !dialogOpen && !inviteOpen });

  const isSubmitting = createMut.isPending || updateMut.isPending;

  if (!multiCompany) {
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
    setAfmContributions((c?.afmContributions ?? []).filter((a) => a !== "CIRCULAR_ECONOMY"));
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
    setConstructionPermitHolder(
      c?.constructionPermitHolder == null ? "" : c.constructionPermitHolder ? "yes" : "no"
    );
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
    setTab("");
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
    if (!isValidCui(cui)) {
      setFormError("cuiInvalid");
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
        description={isConsultant ? t.subtitleConsultant : t.subtitle}
        actions={
          <Button onClick={openCreate} hotkey="N">
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        }
      />

      <div role="tablist" aria-label={t.tabsLabel} className="mt-6 flex gap-1 overflow-x-auto border-b border-line">
        {tabs.map((item) => {
          const selected = item.id === tab;
          return (
            <button
              key={item.id || "clienti"}
              type="button"
              role="tab"
              aria-selected={selected}
              onClick={() => setTab(item.id)}
              className={
                "-mb-px inline-flex shrink-0 items-center gap-2 whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium " +
                (selected
                  ? "border-brand-600 text-content-strong"
                  : "border-transparent text-content-muted hover:text-content")
              }
            >
              {item.alert && <span className="h-2 w-2 rounded-sm bg-state-warn" aria-hidden />}
              {item.label}
              {item.count !== undefined && <span className="font-mono text-xs opacity-70">{item.count}</span>}
            </button>
          );
        })}
      </div>

      {tab === "" && (
      <>
      <ClientFigures
        rows={rows}
        overviewFailed={overview.isError}
        money={isPlatformAdmin ? money : null}
        today={today}
      />

      {isPlatformAdmin && <RequestsBand count={newRequests} names={(requests.data ?? []).filter((r) => r.status === "NEW").map((r) => r.companyName)} onOpen={() => setTab("cereri")} />}

      <section className="mt-6">
        {isError && <p className="text-sm text-state-bad-text">{t.loadError}</p>}

        {!isError && (
          <>
            <PillGroup
              name="client-filter"
              options={filters.map((f) => ({
                value: f,
                label: (
                  <>
                    {t.filters[f]}{" "}
                    <span className="font-mono text-xs opacity-70">
                      {companies && (f === "ALL" || f === "CABINETS" || overview.data) ? counts[f] : t.unknown}
                    </span>
                  </>
                ),
              }))}
              selected={[filter]}
              onToggle={setFilter}
              className="mb-3"
            />
            <TableToolbar view={view} placeholder={t.searchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <TH>{t.colClient}</TH>
                  {isPlatformAdmin && <TH>{t.colSubscription}</TH>}
                  {isPlatformAdmin && <TH>{t.colLastInvoice}</TH>}
                  <TH className="text-right">{t.colUsers}</TH>
                  <TH>{t.colProfile}</TH>
                  <TH sticky="right" className="text-right">{strings.common.actions}</TH>
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={isPlatformAdmin ? 6 : 4}
                    loading={isLoading}
                    icon={Building2}
                    title={view.emptiedBySearch || filter !== "ALL" ? strings.common.noResults : t.empty}
                    description={
                      view.emptiedBySearch || filter !== "ALL"
                        ? strings.common.noResultsHint
                        : isConsultant
                          ? t.emptyHintConsultant
                          : t.emptyHint
                    }
                    action={
                      <Button onClick={openCreate}>
                        <Plus className="mr-2 h-4 w-4" />
                        {t.add}
                      </Button>
                    }
                  />
                )}
                {view.visible.map((r) => (
                  <TR key={r.company.id}>
                    <TD className="min-w-[14rem]">
                      <span className="font-medium text-content">{r.company.name}</span>
                      <span className="block font-mono text-xs text-content-muted">
                        {r.company.cui} · {typeLabels[r.company.type]}
                      </span>
                      {!r.company.active && <Badge variant="muted">{t.inactive}</Badge>}
                    </TD>
                    {isPlatformAdmin && (
                      <TD className="whitespace-nowrap">
                        <SubscriptionCell row={r} />
                      </TD>
                    )}
                    {isPlatformAdmin && (
                      <TD className="whitespace-nowrap">
                        <InvoiceCell row={r} />
                      </TD>
                    )}
                    <TD className="text-right font-mono tabular-nums">
                      {r.overview ? (
                        r.noUsers ? <Badge variant="danger">0</Badge> : r.overview.userCount
                      ) : (
                        t.unknown
                      )}
                    </TD>
                    <TD className="whitespace-nowrap">
                      <ProfileCell row={r} />
                    </TD>
                    <TD sticky="right" className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(r.company)}>
                          {t.open}
                        </Button>
                        <Menu>
                          {/* O firmă dintr-un cabinet n-are abonament propriu: o plătește cabinetul. */}
                          {isPlatformAdmin && !r.company.consultancyId && (
                            <MenuItem icon={Receipt} onClick={() => setBilling(r.company)}>
                              {t.subscriptionAction}
                            </MenuItem>
                          )}
                          {isPlatformAdmin && (
                            <MenuItem icon={Briefcase} onClick={() => setAssigning(r.company)}>
                              {t.assignConsultancy}
                            </MenuItem>
                          )}
                          <MenuItem icon={UserPlus} onClick={() => openInvite(r.company)}>
                            {t.invite}
                          </MenuItem>
                        </Menu>
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
      </>
      )}

      {/* Create / edit company */}
      {/* `xl`, ca formularul de mișcare: al doilea ca mărime din aplicație, ~25 de rubrici, și
          singurul rămas la 512px după modernizare. */}
      <Dialog
        open={dialogOpen}
        size="xl"
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
        <form id="company-form" onSubmit={handleSubmit} className="space-y-6">
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
                autoFocus
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

          {/* Persoana desemnată cu gestiunea deșeurilor — secțiune separată, fiindcă e altceva
              decât persoana de contact de mai jos și se confundă ușor cu ea. */}
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

          {/* Jumătatea de profil a termenului de 30 aprilie (art. 49 alin. (9)). Stă aici, lângă
              raportare, fiindcă asta e: o întrebare al cărei singur efect e o alertă. Cealaltă
              jumătate — uleiurile uzate — se citește din mișcări şi n-are rubrică. */}
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

      {/* Inboxul cererilor publice și cabinetele sunt ale platformei; echipa, a consultantului. */}
      {isPlatformAdmin && tab === "cereri" && <AccountRequestsSection enabled onOpenCompany={openCompanyById} />}
      {isPlatformAdmin && tab === "cabinete" && <ConsultanciesSection />}
      {isConsultant && tab === "echipa" && <ConsultancyTeamSection />}
      {isConsultant && tab === "antet" && <ConsultancyBrandingSection />}
      {assigning && (
        <AssignConsultancyDialog company={assigning} onClose={() => setAssigning(null)} />
      )}
      {billing && (
        <SubscriptionDialog
          owner={{ kind: "company", id: billing.id, name: billing.name }}
          onClose={() => setBilling(null)}
        />
      )}
    </div>
  );
}

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { maximumFractionDigits: 2 })} lei`;
}

/** O cifră în mono, ca pe afișaj, cu eticheta deasupra și explicația lângă. Sursa căzută arată „?”, nu „0”. */
function Figure({ label, value, sub, tone }: { label: string; value: string; sub: string; tone?: "bad" }) {
  return (
    <Card data-figure={label} className="px-4 py-3">
      <div className="eyebrow">{label}</div>
      <div className="mt-1 flex flex-wrap items-baseline gap-x-2">
        <span
          data-figure-value
          className={`font-mono text-xl tabular-nums ${tone === "bad" ? "text-state-bad-text" : "text-content"}`}
        >
          {value}
        </span>
        <span className="text-xs text-content-muted">{sub}</span>
      </div>
    </Card>
  );
}

function ClientFigures({
  rows,
  overviewFailed,
  money,
  today,
}: {
  rows: ClientRow[];
  overviewFailed: boolean;
  money: ReturnType<typeof useInvoiceMoney> | null;
  today: string;
}) {
  const active = rows.filter((r) => r.company.active).length;
  const attention = rows.filter((r) => r.reasons.length > 0);
  const failed = attention.filter((r) => r.reasons.includes("FAILED")).length;
  const overdue = attention.filter((r) => r.reasons.includes("OVERDUE")).length;
  const noUsers = attention.filter((r) => r.reasons.includes("NO_USERS")).length;
  const m = money?.data;
  const month = new Date(`${today}T12:00:00`).toLocaleString("ro-RO", { month: "long" });
  const reasons = [
    failed > 0 && countOf(failed, t.reasonFailedOne, t.reasonFailedMany),
    overdue > 0 && countOf(overdue, t.reasonOverdueOne, t.reasonOverdueMany),
    noUsers > 0 && `${noUsers} ${t.reasonNoUsers}`,
  ].filter(Boolean);

  return (
    <div className={`mt-4 grid grid-cols-2 gap-3 ${money ? "lg:grid-cols-4" : ""}`} data-testid="client-figures">
      <Figure label={t.statActive} value={String(active)} sub={t.statActiveSub.replace("{n}", String(rows.length))} />
      {money && (
        <Figure
          label={t.statPaid.replace("{month}", month)}
          value={m ? lei(m.paidThisMonth) : t.unknown}
          sub={m && m.paidThisMonthCount > 0 ? countOf(m.paidThisMonthCount, t.statPaidOne, t.statPaidMany) : "—"}
        />
      )}
      {money && (
        <Figure
          label={t.statDue}
          value={m ? lei(m.unpaid) : t.unknown}
          sub={m && m.unpaidCount > 0 ? countOf(m.unpaidCount, t.statDueOne, t.statDueMany) : "—"}
        />
      )}
      <Figure
        label={t.statAttention}
        value={overviewFailed ? t.unknown : String(attention.length)}
        sub={overviewFailed ? "—" : reasons.length > 0 ? reasons.join(" · ") : t.statAttentionNone}
        tone={attention.length > 0 && !overviewFailed ? "bad" : undefined}
      />
    </div>
  );
}

function RequestsBand({ count, names, onOpen }: { count: number; names: string[]; onOpen: () => void }) {
  if (count === 0) return null;
  return (
    <div className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-line bg-surface-muted px-4 py-2 text-sm">
      <span className="text-content">
        <span className="font-semibold">{countOf(count, t.requestOne, t.requestMany)}</span>
        {names.length > 0 && <span className="text-content-muted"> · {names.join(", ")}</span>}
      </span>
      <Button size="sm" variant="outline" onClick={onOpen}>
        {t.requestsSee}
      </Button>
    </div>
  );
}

const SUBSCRIPTION_BADGE = {
  PENDING: "warning",
  ACTIVE: "success",
  PAST_DUE: "danger",
  READ_ONLY: "danger",
  CANCELLED: "muted",
} as const;

function SubscriptionCell({ row }: { row: ClientRow }) {
  const o = row.overview;
  if (row.company.consultancyId) {
    return (
      <>
        <Badge variant="muted">{t.paidByCabinet}</Badge>
        <span className="block text-xs text-content-muted">{row.company.consultancyName}</span>
      </>
    );
  }
  if (!o) return <span className="text-content-subtle">{t.unknown}</span>;
  if (!o.subscriptionStatus || !o.plan) return <Badge variant="muted">{t.noSubscription}</Badge>;
  return (
    <>
      <Badge variant={SUBSCRIPTION_BADGE[o.subscriptionStatus]}>
        {strings.subscriptions.status[o.subscriptionStatus]}
      </Badge>
      <span className="block text-xs text-content-muted">
        {strings.subscriptions.plans[o.plan]}
        {o.monthlyPrice != null && ` · ${lei(o.monthlyPrice)}`}
      </span>
    </>
  );
}

function InvoiceCell({ row }: { row: ClientRow }) {
  const i = row.overview?.lastInvoice;
  if (!row.overview) return <span className="text-content-subtle">{t.unknown}</span>;
  if (!i) return <span className="text-content-subtle">—</span>;
  const [variant, label] =
    i.status === "PAID"
      ? (["success", t.invoicePaid] as const)
      : row.failed
        ? (["danger", t.invoiceFailed] as const)
        : i.status === "DRAFT"
          ? (["muted", t.invoiceDraft] as const)
          : row.overdueDays > 0
            ? (["danger", t.invoiceOverdue.replace("{days}", countOf(row.overdueDays, "zi", "zile"))] as const)
            : (["warning", t.invoiceDue.replace("{date}", formatDate(i.dueDate).slice(0, 5))] as const);
  return (
    <>
      <span className="font-mono text-sm text-content">
        {i.number && `${i.number} · `}
        {lei(i.total)}
      </span>
      <span className="block">
        <Badge variant={variant}>{label}</Badge>
      </span>
      {/* Motivul întreg la hover: pe trei rânduri, un client căzut umfla rândul cât trei (F-B2). */}
      {row.failed && i.lastError && (
        <Tooltip content={i.lastError}>
          <span className="block max-w-[28ch] truncate text-xs text-state-bad-text">{i.lastError}</span>
        </Tooltip>
      )}
    </>
  );
}

function ProfileCell({ row }: { row: ClientRow }) {
  if (row.cuiInvalid) return <Badge variant="danger">{t.profileCui}</Badge>;
  if (row.gaps.length === 0) return <Badge variant="success">{t.profileComplete}</Badge>;
  return (
    <Tooltip content={t.profileGapsHint.replace("{fields}", row.gaps.map((g) => t.gapFields[g]).join(", "))}>
      <Badge variant="warning">{countOf(row.gaps.length, t.profileGapOne, t.profileGapMany)}</Badge>
    </Tooltip>
  );
}
