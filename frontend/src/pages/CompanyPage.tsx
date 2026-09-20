import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ArrowRight, Briefcase, UserPlus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { isMultiCompany } from "@/lib/roles";
import { companiesKey, useClientOverview, useCompanies } from "@/hooks/useCompanies";
import { useUrlState } from "@/hooks/useUrlState";
import { useHotkey } from "@/hooks/useHotkey";
import { clientRow } from "@/lib/clients";
import { countOf } from "@/lib/count";
import { strings } from "@/lib/strings";
import { AssignConsultancyDialog } from "@/components/ConsultanciesSection";
import { AuditLogSection } from "@/components/AuditLogSection";
import { CompanyForm } from "@/components/CompanyForm";
import { CompanyUsersSection } from "@/components/CompanyUsersSection";
import { InviteUserDialog } from "@/components/InviteUserDialog";
import { lei, STATUS_BADGE, SubscriptionPanel } from "@/components/SubscriptionDialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Menu, MenuItem } from "@/components/ui/menu";
import { PageHeader } from "@/components/ui/page-header";
import { PageTabs, type PageTab } from "@/components/ui/page-tabs";
import { todayIso } from "@/lib/utils";
import { LoadError } from "@/components/ui/load-error";

const t = strings.clients;
const p = strings.companyPage;

/**
 * F-D (todo-clienti-abonamente.md, macheta B) — pagina unei firme, `/clienti/:id`. Înlocuiește dialogul `xl` al firmei
 * și dialogul de abonament de pe Clienți: **Profil** (fișa, cu ce lipsește pentru dosar) · **Utilizatori** · **Abonament
 * și facturi** (doar platforma, doar la clientul direct) · **Istoric**. Sus „Intră în cont” și „Invită utilizator”.
 *
 * <p>Utilizatorii și istoricul sunt ecranele din Setări, cerute pe firma din adresă (antetul `X-Tenant-Id` al cererii),
 * nu pe cea din comutator: pagina se citește fără să schimbe firma pe care lucrezi.
 */
export function CompanyPage() {
  const { id = "" } = useParams();
  const { user, switchTenant } = useAuth();
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";
  const multiCompany = isMultiCompany(user?.role);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const companies = useCompanies(multiCompany);
  const overview = useClientOverview(multiCompany);
  const company = companies.data?.find((c) => c.id === id);
  const row = company ? clientRow(company, overview.data?.find((o) => o.companyId === id), todayIso()) : null;

  const [tabParam, setTab] = useUrlState("tab");
  const [inviting, setInviting] = useState(false);
  const [assigning, setAssigning] = useState(false);
  // O firmă dintr-un cabinet n-are abonament propriu: o plătește cabinetul.
  const hasOwnSubscription = isPlatformAdmin && !company?.consultancyId;

  const users = row?.overview?.userCount;
  const tabs: PageTab[] = [
    { id: "", label: p.tabProfile, count: row && row.gaps.length + (row.cuiInvalid ? 1 : 0) > 0 ? row.gaps.length + (row.cuiInvalid ? 1 : 0) : undefined, alert: Boolean(row && (row.gaps.length > 0 || row.cuiInvalid)) },
    { id: "utilizatori", label: p.tabUsers, count: users, alert: row?.noUsers },
    ...(hasOwnSubscription
      ? [{ id: "abonament", label: p.tabSubscription, alert: Boolean(row && (row.failed || row.overdue)) }]
      : []),
    { id: "istoric", label: p.tabHistory },
  ];
  const tab = tabs.some((x) => x.id === tabParam) ? tabParam : "";

  useHotkey("n", () => setInviting(true), { enabled: Boolean(company) && !inviting && !assigning });

  /** Ca pe panoul cabinetului: datele firmei de dinainte se aruncă, lista de firme rămâne. */
  function enterAccount() {
    queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== companiesKey[0] });
    switchTenant(id);
    navigate("/");
  }

  const back = (
    <Link
      to="/clienti"
      className="inline-flex items-center gap-1.5 text-sm font-medium text-content-muted hover:text-content"
    >
      <ArrowLeft className="h-4 w-4" aria-hidden />
      {t.title}
    </Link>
  );

  if (!multiCompany) return <PageHeader title={t.title} description={t.onlyPlatformAdmin} />;
  if (companies.isLoading) return <div>{back}<p className="mt-6 text-sm text-content-muted">{strings.common.loading}</p></div>;
  if (companies.isError) return <div>{back}<LoadError className="mt-6" message={t.loadError} onRetry={companies.refetch} /></div>;
  if (!company || !row) {
    return (
      <div>
        {back}
        <PageHeader className="mt-4" title={p.notFound} description={p.notFoundHint} />
      </div>
    );
  }

  const o = row.overview;
  const meta = [company.cui, company.tradeRegisterNumber, strings.enums.companyType[company.type]].filter(Boolean).join(" · ");

  return (
    <div>
      {back}
      <PageHeader
        className="mt-4"
        title={company.name}
        description={
          <>
            <span className="block font-mono text-xs">{meta}</span>
            {company.address && <span className="block">{company.address}</span>}
          </>
        }
        actions={
          <>
            <Button variant="outline" onClick={() => setInviting(true)} hotkey="N">
              <UserPlus className="mr-2 h-4 w-4" />
              {t.invite}
            </Button>
            <Button onClick={enterAccount}>
              {p.enterAccount}
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
            {isPlatformAdmin && (
              <Menu>
                <MenuItem icon={Briefcase} onClick={() => setAssigning(true)}>
                  {t.assignConsultancy}
                </MenuItem>
              </Menu>
            )}
          </>
        }
      />

      {/* Starea pe un rând, ca eticheta de pe cântar: ce-i trebuie firmei se vede înainte de taburi. */}
      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm" data-testid="company-status">
        {!company.active && <Badge variant="muted">{t.inactive}</Badge>}
        {company.consultancyId ? (
          <Badge variant="muted">{`${t.paidByCabinet} · ${company.consultancyName ?? ""}`}</Badge>
        ) : isPlatformAdmin && o ? (
          o.subscriptionStatus && o.plan ? (
            <Badge variant={STATUS_BADGE[o.subscriptionStatus]}>
              {`${strings.subscriptions.status[o.subscriptionStatus]} · ${strings.subscriptions.plans[o.plan]}${o.monthlyPrice != null ? ` · ${lei(o.monthlyPrice)}` : ""}`}
            </Badge>
          ) : (
            <Badge variant="muted">{t.noSubscription}</Badge>
          )
        ) : null}
        {o && (
          <Badge variant={row.noUsers ? "danger" : "success"}>
            {row.noUsers ? p.noUsers : countOf(o.userCount, p.userOne, p.userMany)}
          </Badge>
        )}
        {row.failed && <Badge variant="danger">{t.invoiceFailed}</Badge>}
        {row.overdue && <Badge variant="danger">{p.overdue}</Badge>}
      </div>

      <PageTabs tabs={tabs} selected={tab} onSelect={setTab} label={p.tabsLabel} />

      {tab === "" && (
        <div className="mt-4">
          {(row.gaps.length > 0 || row.cuiInvalid) && (
            <div
              data-testid="profile-gaps"
              className="mb-2 rounded-md border border-line bg-surface-muted px-4 py-3 text-sm"
            >
              <span className="font-semibold text-content">{p.gapsTitle}</span>
              <ul className="mt-1 flex flex-wrap gap-x-4 gap-y-1">
                {row.cuiInvalid && (
                  <li>
                    <a href="#firma-identificare" className="text-state-bad-text underline">
                      {t.profileCui}
                    </a>
                  </li>
                )}
                {row.gaps.map((g) => (
                  <li key={g}>
                    <a href={`#${GAP_SECTION[g]}`} className="text-content underline">
                      {t.gapFields[g]}
                    </a>
                  </li>
                ))}
              </ul>
              <p className="mt-1 text-xs text-content-muted">{p.gapsHint}</p>
            </div>
          )}
          <CompanyForm key={company.id} company={company} />
        </div>
      )}
      {tab === "utilizatori" && (
        <div className="mt-6">
          <CompanyUsersSection canManage companyId={company.id} />
        </div>
      )}
      {tab === "abonament" && hasOwnSubscription && (
        <SubscriptionPanel owner={{ kind: "company", id: company.id, name: company.name }} />
      )}
      {tab === "istoric" && (
        <div className="mt-6">
          <AuditLogSection canManage companyId={company.id} />
        </div>
      )}

      {inviting && <InviteUserDialog company={company} onClose={() => setInviting(false)} />}
      {assigning && <AssignConsultancyDialog company={company} onClose={() => setAssigning(false)} />}
    </div>
  );
}

/** Unde stă, în fișă, fiecare rubrică de dosar care poate lipsi. */
const GAP_SECTION = {
  address: "firma-identificare",
  caenCode: "firma-identificare",
  wasteManagerName: "firma-persoana-desemnata",
} as const;
