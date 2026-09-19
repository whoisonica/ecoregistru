import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Ban,
  Building2,
  ChevronRight,
  Eye,
  FileUp,
  History,
  Layers,
  MapPin,
  Pencil,
  Plus,
  RotateCcw,
  Tags,
  Truck,
  UserRound,
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { canImport, canManage as roleCanManage, canWrite as roleCanWrite } from "@/lib/roles";
import {
  useWorkPoints,
  useCreateWorkPoint,
  useUpdateWorkPoint,
  useDeactivateWorkPoint,
  useReactivateWorkPoint,
} from "@/hooks/useWorkPoints";
import type { WorkPoint } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useHotkey } from "@/hooks/useHotkey";
import { Button, LinkButton } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { useActiveFilter } from "@/components/ui/active-filter";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { InternalGeneratorsSection } from "@/components/InternalGeneratorsSection";
import { OwnDriversSection } from "@/components/OwnDriversSection";
import { WasteArticlesSection } from "@/components/WasteArticlesSection";
import { VehiclesSection } from "@/components/VehiclesSection";
import { PriceVisibilitySection } from "@/components/PriceVisibilitySection";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { registersFor } from "@/lib/movementScreens";
import { CompanyDetailsSection } from "@/components/CompanyDetailsSection";
import { CompanyUsersSection } from "@/components/CompanyUsersSection";
import { AuditLogSection } from "@/components/AuditLogSection";
import { SETTINGS_CARD } from "@/components/ui/card";
import { useInternalGenerators } from "@/hooks/useInternalGenerators";
import { useUsers } from "@/hooks/useUsers";
import { useDrivers } from "@/hooks/useDrivers";
import { formatDate, todayIso } from "@/lib/utils";

const t = strings.settings.workPoints;
const h = strings.settings.hub;

type SectionId =
  | "datele-firmei"
  | "puncte-de-lucru"
  | "generatori-interni"
  | "utilizatori"
  | "jurnal-audit"
  | "soferi"
  | "flota"
  | "preturi"
  | "sortimente";

interface HubCard {
  id: SectionId;
  icon: LucideIcon;
  title: string;
  description: string;
  /** Starea pe scurt, sub descriere. `bad` o scrie în roșu: e ceva de reparat, nu o informație. */
  summary?: { text: string; bad?: boolean };
}

/** „{count} active” — numărul, fără forme de plural: cardul e o etichetă, nu o frază. */
function count(template: string, n: number) {
  return template.replace("{count}", String(n));
}

export function SettingsPage() {
  const { user } = useAuth();
  const canManage = roleCanManage(user?.role);
  // Sortimentele sunt ale depozitului: le vede doar firma care are „Intrări și ieșiri” (art. 48).
  const { data: company } = useCurrentCompany();
  const hasDepot = Boolean(company) && registersFor(company?.type).includes("ART_48");

  const { data: workPoints, isLoading, isError } = useWorkPoints();
  const createMut = useCreateWorkPoint();
  const updateMut = useUpdateWorkPoint();
  const deactivateMut = useDeactivateWorkPoint();
  const reactivateMut = useReactivateWorkPoint();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WorkPoint | null>(null);
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [nameError, setNameError] = useState(false);

  const { rows: visibleWorkPoints, control: activeFilter } = useActiveFilter(workPoints ?? []);
  const view = useTableView(visibleWorkPoints, {
    searchText: (wp) => [wp.name, wp.address].filter(Boolean).join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });

  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openCreate() {
    setEditing(null);
    setName("");
    setAddress("");
    setNameError(false);
    setDialogOpen(true);
  }

  function openEdit(wp: WorkPoint) {
    setEditing(wp);
    setName(wp.name);
    setAddress(wp.address ?? "");
    setNameError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setNameError(true);
      return;
    }
    const input = { name: name.trim(), address: address.trim() || null };
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

  function handleDeactivate(wp: WorkPoint) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{wp.name}</strong>
          {wp.address ? ` — ${wp.address}` : ""}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () => deactivate(wp),
    });
  }

  function deactivate(wp: WorkPoint) {
    deactivateMut.mutate(wp.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  // Reactivarea nu întreabă nimic: nu strică nimic și se desface la loc cu butonul de alături.
  function reactivate(wp: WorkPoint) {
    reactivateMut.mutate(wp.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  const workPointsSection = (
    <section id="puncte-de-lucru" className={SETTINGS_CARD}>
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={openCreate} hotkey="N">
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder}>
            {activeFilter}
          </TableToolbar>
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                  {t.name}
                </SortableTH>
                <TH>{t.address}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={canManage ? 4 : 3}
                  loading={isLoading}
                  icon={MapPin}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={
                    view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                  }
                  action={
                    canManage && (
                      <Button onClick={openCreate}>
                        <Plus className="mr-2 h-4 w-4" />
                        {t.add}
                      </Button>
                    )
                  }
                />
              )}
              {view.visible.map((wp) => (
                <TR key={wp.id}>
                  <TD className="font-medium text-content">{wp.name}</TD>
                  <TD>{wp.address || "—"}</TD>
                  <TD>
                    {wp.active ? (
                      <Badge variant="success">{t.active}</Badge>
                    ) : (
                      <Badge variant="muted">{t.inactive}</Badge>
                    )}
                  </TD>
                  {canManage && (
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(wp)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        {wp.active ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            onClick={() => handleDeactivate(wp)}
                          >
                            <Ban className="mr-1 h-3.5 w-3.5" />
                            {t.deactivate}
                          </Button>
                        ) : (
                          <Button variant="ghost" size="sm" onClick={() => reactivate(wp)}>
                            <RotateCcw className="mr-1 h-3.5 w-3.5" />
                            {strings.common.reactivate}
                          </Button>
                        )}
                      </div>
                    </TD>
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
          <TablePagination view={view} />
        </>
      )}
    </section>
  );

  // Setările (17.09.2026, varianta C): `/setari` e o pagină de carduri pe grupuri, fiecare card duce la
  // `/setari/<secțiune>`. Înainte era o pagină lungă cu până la zece tabele și un cuprins lipicios.
  const { section } = useParams();
  const location = useLocation();
  const allowed = new Set<SectionId>([
    "datele-firmei",
    "puncte-de-lucru",
    "generatori-interni",
    "soferi",
    ...(canManage ? (["utilizatori", "jurnal-audit"] as const) : []),
    ...(hasDepot ? (["flota", "preturi", "sortimente"] as const) : []),
  ]);

  // `n` deschide formularul, unde contul are voie. Scurtătura tace pe un cont care
  // n-ar putea salva oricum: o comandă care nu face nimic e mai rea decât una lipsă.
  useHotkey("n", openCreate, { enabled: Boolean(canManage) && section === "puncte-de-lucru" });

  // Legăturile vechi purtau secțiunea în ancoră (`/setari#soferi`, „Primii pași”) sau, pentru
  // „Istoric” de pe o mișcare, `?istoric=` cu `#jurnal-audit`. Duc tot acolo, pe pagina ei.
  if (!section) {
    const anchor = location.hash.slice(1);
    const legacy = new URLSearchParams(location.search).has("istoric") ? "jurnal-audit" : anchor;
    if (legacy) return <Navigate to={`/setari/${legacy}${location.search}`} replace />;
    return <SettingsHub canManage={canManage} canImportExcel={canManage && canImport(user?.role)} hasDepot={hasDepot} workPoints={workPoints} />;
  }
  // Până se încarcă firma nu se știe dacă are depozit: o secțiune de depozit nu trimite înapoi până atunci.
  if (!allowed.has(section as SectionId) && (company || !["flota", "preturi", "sortimente"].includes(section))) {
    return <Navigate to="/setari" replace />;
  }

  // Titlul paginii e al secțiunii; vizibil îl spune deja capul cardului, aici e pentru cititorul de ecran.
  const sectionTitle: Record<SectionId, string> = {
    "datele-firmei": h.company,
    "puncte-de-lucru": h.workPoints,
    "generatori-interni": h.internalGenerators,
    utilizatori: h.users,
    "jurnal-audit": h.audit,
    soferi: h.drivers,
    flota: h.vehicles,
    preturi: h.prices,
    sortimente: h.articles,
  };

  return (
    <div>
      <h1 className="sr-only">{`${strings.settings.title} · ${sectionTitle[section as SectionId]}`}</h1>
      <Link
        to="/setari"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-content-muted hover:text-content"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden />
        {strings.settings.title}
      </Link>

      <div className="mt-4 space-y-6">
        {section === "datele-firmei" && <CompanyDetailsSection />}
        {section === "puncte-de-lucru" && workPointsSection}
        {section === "generatori-interni" && (
          <InternalGeneratorsSection workPoints={workPoints ?? []} canManage={canManage} />
        )}
        {section === "utilizatori" && <CompanyUsersSection canManage={canManage} />}
        {section === "jurnal-audit" && <AuditLogSection canManage={canManage} />}
        {section === "soferi" && (
          <OwnDriversSection canManage={canManage} workPoints={workPoints ?? []} hasDepot={hasDepot} />
        )}
        {/* D2.1 — flota; o scrie oricine scrie, ca sortimentele și ca serverul. */}
        {section === "flota" && hasDepot && (
          <VehiclesSection workPoints={workPoints ?? []} canManage={roleCanWrite(user?.role)} />
        )}
        {section === "preturi" && hasDepot && <PriceVisibilitySection />}
        {/* Sortimentele le personalizează oricine scrie, și operatorul (proprietarul, 15.09.2026). */}
        {section === "sortimente" && hasDepot && <WasteArticlesSection canManage={roleCanWrite(user?.role)} />}
      </div>

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.edit : t.add}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="work-point-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="work-point-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="wp-name">{t.name}</Label>
            <Input
              id="wp-name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (nameError) setNameError(false);
              }}
              autoFocus
            />
            {nameError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
          </div>
          <div>
            <Label htmlFor="wp-address">{t.address}</Label>
            <Textarea
              id="wp-address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              rows={2}
            />
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </div>
  );
}

/**
 * Pagina de start a Setărilor: carduri pe grupuri, fiecare cu starea pe scurt. Cardurile citesc
 * listele pe care le-ar citi oricum secțiunile — mici și deja în cache după prima vizită.
 */
function SettingsHub({
  canManage,
  canImportExcel,
  hasDepot,
  workPoints,
}: {
  canManage: boolean;
  canImportExcel: boolean;
  hasDepot: boolean;
  workPoints: WorkPoint[] | undefined;
}) {
  const { data: company } = useCurrentCompany();
  const { data: generators } = useInternalGenerators();
  const { data: users } = useUsers(canManage);
  const { data: drivers } = useDrivers();

  const today = todayIso();
  const authExpiry = company?.environmentalAuthExpiry;
  const companySummary = !company
    ? undefined
    : !authExpiry
      ? { text: h.authMissing, bad: true }
      : authExpiry < today
        ? { text: h.authExpired, bad: true }
        : { text: `${h.authValidUntil} ${formatDate(authExpiry)}` };

  const activeUsers = users?.filter((u) => u.status === "ACTIVE").length ?? 0;
  const pending = users?.filter((u) => u.status === "PENDING_INVITE").length ?? 0;
  const ownDrivers = drivers?.filter((d) => d.partnerId === null && d.active);

  const groups: { title: string; cards: HubCard[] }[] = [
    {
      title: h.groupCompany,
      cards: [
        { id: "datele-firmei", icon: Building2, title: h.company, description: h.companyHint, summary: companySummary },
        {
          id: "puncte-de-lucru",
          icon: MapPin,
          title: h.workPoints,
          description: h.workPointsHint,
          summary: workPoints && { text: count(h.active, workPoints.filter((w) => w.active).length) },
        },
        {
          id: "generatori-interni",
          icon: Layers,
          title: h.internalGenerators,
          description: h.internalGeneratorsHint,
          summary: generators && { text: count(h.activeMasc, generators.filter((g) => g.active).length) },
        },
      ],
    },
    ...(canManage
      ? [
          {
            title: h.groupTeam,
            cards: [
              {
                id: "utilizatori" as const,
                icon: UserRound,
                title: h.users,
                description: h.usersHint,
                summary: users && {
                  text: count(h.activeMasc, activeUsers) + (pending ? ` · ${count(h.pending, pending)}` : ""),
                },
              },
              { id: "jurnal-audit" as const, icon: History, title: h.audit, description: h.auditHint },
            ],
          },
        ]
      : []),
    {
      title: h.groupTransport,
      cards: [
        {
          id: "soferi",
          icon: Truck,
          title: h.drivers,
          description: h.driversHint,
          summary: ownDrivers && { text: count(h.activeMasc, ownDrivers.length) },
        },
        ...(hasDepot ? [{ id: "flota" as const, icon: Truck, title: h.vehicles, description: h.vehiclesHint }] : []),
      ],
    },
    ...(hasDepot
      ? [
          {
            title: h.groupDepot,
            cards: [
              { id: "preturi" as const, icon: Eye, title: h.prices, description: h.pricesHint },
              { id: "sortimente" as const, icon: Tags, title: h.articles, description: h.articlesHint },
            ],
          },
        ]
      : []),
  ];

  return (
    <div>
      <PageHeader
        title={strings.settings.title}
        description={strings.settings.subtitle}
        actions={
          // Importul din Excel nu stă în meniu: îl facem noi, la implementare. Din 16.09.2026 butonul
          // e numai al platformei; clientul ne trimite fișierul.
          canImportExcel && (
            <LinkButton to="/import" variant="outline">
              <FileUp className="mr-2 h-4 w-4" />
              {strings.nav.importExcel}
            </LinkButton>
          )
        }
      />

      <div className="mt-8 space-y-8">
        {groups.map((group) => (
          <section key={group.title} aria-labelledby={`grup-${group.title}`}>
            <h2 id={`grup-${group.title}`} className="eyebrow mb-3">
              {group.title}
            </h2>
            <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {group.cards.map((card) => (
                <li key={card.id}>
                  <Link
                    to={`/setari/${card.id}`}
                    className="group flex h-full items-start gap-4 rounded-lg border border-line-strong/70 bg-surface p-4 transition-colors hover:border-content-subtle sm:p-5"
                  >
                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-md border border-line bg-surface-muted text-content-strong">
                      <card.icon className="h-5 w-5" aria-hidden />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block text-[0.9375rem] font-semibold text-content">{card.title}</span>
                      <span className="mt-0.5 block text-sm text-content-muted">{card.description}</span>
                      {card.summary && (
                        <span
                          className={
                            "mt-2.5 block font-mono text-xs " +
                            (card.summary.bad ? "text-red-700" : "text-content-strong")
                          }
                        >
                          {card.summary.text}
                        </span>
                      )}
                    </span>
                    <ChevronRight
                      className="mt-0.5 h-4 w-4 shrink-0 text-content-subtle transition-transform group-hover:translate-x-0.5"
                      aria-hidden
                    />
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  );
}
