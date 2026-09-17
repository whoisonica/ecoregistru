import { useEffect, useMemo, useRef } from "react";
import { Ban, MapPin, Pencil, Plus, RotateCcw, Truck, Users } from "lucide-react";
import { AUTH_STEP, PLACES_STEP, countLabel } from "@/components/partners/partnerForm";
import { useCanWrite } from "@/hooks/useBillingAccess";
import {
  usePartners,
  useDeactivatePartner,
  useReactivatePartner,
} from "@/hooks/usePartners";
import type {
  Partner,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useHotkey } from "@/hooks/useHotkey";
import { useUrlState } from "@/hooks/useUrlState";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import {
  PartnerFormDialog,
  type PartnerFormDialogHandle,
} from "@/components/partners/PartnerFormDialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { missingLast, useTableView } from "@/hooks/useTableView";
import { useActiveFilter } from "@/components/ui/active-filter";
import { formatDate } from "@/lib/utils";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { Menu, MenuItem } from "@/components/ui/menu";
import { PartnerRoleBadge } from "@/components/PartnerRoleBadge";
import { NaturalPersonsSection } from "@/components/NaturalPersonsSection";
import { registersFor } from "@/lib/movementScreens";

const t = strings.partners;
const typeLabels = strings.enums.partnerType;
const roleLabels = strings.enums.partnerRole;



/**
 * Filter values. The first four are the commercial role — "none" surfaces the partners still to be
 * classified — and "carrier" is a different axis riding in the same select, because it is one value
 * and a second dropdown for it would cost more than it explains.
 */
type RoleFilter = "" | "client" | "supplier" | "none" | "carrier";

/** Formats an authorization expiry as a status badge, mirroring backend `expiringSoon`. */
function ExpiryBadge({ partner }: { partner: Partner }) {
  // V41: data care vine prima dintre expirare și sfârșitul vizei anuale.
  if (!partner.authorizationValidUntil) {
    // Colectorul sau valorificatorul cu autorizație, dar fără viză: galben, nu roșu. Legea nu-i
    // cere generatorului să noteze viza; semnul spune doar ce e de cerut (17.09.2026). Dacă lipsa
    // ei ar trebui să oprească predarea e întrebarea BB pentru specialistă (intrebari-specialist.md).
    if (partner.authorizationNumber && (partner.type === "COLLECTOR" || partner.type === "RECOVERER")) {
      return <Badge variant="warning">{t.visaMissing}</Badge>;
    }
    return <span className="text-content-subtle">{t.noAuthorization}</span>;
  }
  const date = partner.authorizationValidUntil;
  const isExpired = new Date(date) < new Date(new Date().toDateString());
  if (isExpired) {
    return <Badge variant="danger">{t.expired}</Badge>;
  }
  // Data se scrie cum se scrie în România. Aici rămăsese ISO brut — `2026-11-15` lângă
  // `15.11.2026` pe Mișcări și pe Termene, în același produs.
  if (partner.expiringSoon) {
    return <Badge variant="warning">{`${t.expiringSoon} · ${formatDate(date)}`}</Badge>;
  }
  return <Badge variant="success">{formatDate(date)}</Badge>;
}

/**
 * Rândul de sub numele partenerului: punctele de lucru și șoferii lui, la vedere. Până pe 17.09.2026
 * stăteau doar în fișă, la coada pasului 1 și sub un card de transport, deci cine voia să adauge
 * unul mai târziu nu știa unde. Numărul deschide fișa pe pasul lor; „+” deschide și un rând gol.
 * Șoferii se arată numai la cine transportă: doar acolo îi alege formularul de mișcare.
 */
function PartnerPlaces({
  partner,
  canManage,
  onOpen,
}: {
  partner: Partner;
  canManage: boolean;
  onOpen: (addRow?: "workPoint" | "driver") => void;
}) {
  const wpCount = partner.workPoints?.length ?? 0;
  const driverCount = partner.carrier ? partner.drivers?.length ?? 0 : 0;
  const items: { key: string; icon: typeof MapPin; label: string; addRow?: "workPoint" | "driver" }[] = [];
  if (wpCount > 0) {
    items.push({ key: "wp", icon: MapPin, label: countLabel(wpCount, t.workPointOne, t.workPointMany) });
  } else if (canManage) {
    items.push({ key: "wp", icon: Plus, label: t.addWorkPointShort, addRow: "workPoint" });
  }
  if (driverCount > 0) {
    items.push({ key: "dr", icon: Truck, label: countLabel(driverCount, t.driverOne, t.driverMany) });
  } else if (canManage && partner.carrier) {
    items.push({ key: "dr", icon: Plus, label: t.addDriverShort, addRow: "driver" });
  }
  if (items.length === 0) return null;
  return (
    <div className="mt-0.5 flex flex-wrap gap-x-3 gap-y-0.5 text-xs">
      {items.map(({ key, icon: Icon, label, addRow }) =>
        canManage ? (
          <button
            key={key}
            type="button"
            onClick={() => onOpen(addRow)}
            className={
              "inline-flex items-center gap-1 whitespace-nowrap underline-offset-2 hover:underline " +
              (addRow ? "text-brand-700" : "text-content-muted hover:text-content")
            }
          >
            <Icon aria-hidden className="h-3 w-3" />
            {label}
          </button>
        ) : (
          <span key={key} className="inline-flex items-center gap-1 whitespace-nowrap text-content-muted">
            <Icon aria-hidden className="h-3 w-3" />
            {label}
          </span>
        )
      )}
    </div>
  );
}


export function PartnersPage() {
  const canManage = useCanWrite();

  const { data: partners, isLoading, isError } = usePartners();
  const { data: company } = useCurrentCompany();
  // D1.7b: persoanele fizice sunt ale depozitului, deci tabul apare doar la firma cu art. 48.
  const hasDepot = Boolean(company) && registersFor(company?.type).includes("ART_48");
  const [tab, setTab] = useUrlState("tab");
  const personsTab = hasDepot && tab === "persoane-fizice";
  const deactivateMut = useDeactivatePartner();
  const reactivateMut = useReactivatePartner();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();
  /** Fișa de partener e un copil care-și ține propria stare; pagina doar îi dă comenzi. */
  const dialogRef = useRef<PartnerFormDialogHandle>(null);

  const [roleFilterRaw, setRoleFilter] = useUrlState("rol");
  const roleFilter = roleFilterRaw as RoleFilter;


  const filteredByRole = useMemo(
    () =>
      (partners ?? []).filter((p) => {
        if (roleFilter === "client") return p.client;
        if (roleFilter === "supplier") return p.supplier;
        if (roleFilter === "none") return !p.client && !p.supplier;
        if (roleFilter === "carrier") return p.carrier;
        return true;
      }),
    [partners, roleFilter]
  );

  /**
   * Filtrul de rol restrânge, apoi vederea caută, sortează și paginează ce a rămas. Ordinea
   * contează: căutarea peste tot, urmată de filtru, ar arăta un număr de potriviri din care o
   * parte nici nu se vede.
   */
  // Rolul restrânge, apoi starea: „furnizori activi" e întrebarea obișnuită, iar cimitirul de
  // parteneri scoși din uz nu trebuie să stea în calea ei.
  const { rows: activePartners, control: activeFilter } = useActiveFilter(filteredByRole);
  const view = useTableView(activePartners, {
    searchText: (p) =>
      [p.name, p.cui, p.authorizationNumber, p.address].filter(Boolean).join(" "),
    comparators: {
      name: (a, b) => a.name.localeCompare(b.name, "ro"),
      cui: (a, b) => (a.cui ?? "").localeCompare(b.cui ?? "", "ro"),
      // Autorizația fără dată stă la coadă, în ambele sensuri: „nu se știe" nu e nici devreme,
      // nici târziu. `|| null` păstrează înțelesul de dinainte, în care și șirul gol e o lipsă.
      authorizationExpiry: missingLast(
        (p) => p.authorizationValidUntil || null,
        (x, y) => x.localeCompare(y)
      ),
    },
    initialSort: { key: "name", direction: "asc" },
  });
  const visiblePartners = view.visible;



  /**
   * `?nou=1` — formularul gol, cerut din paletă (Ctrl+K → „Adaugă partener").
   *
   * <p>Parametrul se consumă la deschidere, ca `?miscare=` pe Mișcări: lăsat în adresă, un refresh
   * ar redeschide dialogul peste ce lucrezi. Se consumă și pe un rol care nu poate scrie.
   *
   * <p>Dialogul se deschide prin `ref`, nu printr-un `open` ținut aici: „deschide formularul gol”
   * e o comandă, nu o stare, iar cârligul e stabil, deci efectul nu repornește la fiecare tastă —
   * motivul pentru care pagina ținea până acum funcțiile prin `useRef`.
   */

  /**
   * `?partener=<id>` — deschide fişa partenerului cerut, ca `?miscare=` pe Mişcări.
   *
   * <p>Vine din badge-ul „Autorizaţie expirată" de pe o predare: badge-ul spunea că lipseşte o
   * condiţie de legalitate a predării (decizia 36) şi nu ducea nicăieri — al doilea fund de sac
   * din aplicaţie, după cel roşu reparat pe 07.09. Numărul şi data autorizaţiei se editează în
   * fişa partenerului, deci acolo duce.
   *
   * <p>Parametrul se **consumă** la deschidere: lăsat în adresă, un refresh ar redeschide dialogul
   * peste ce lucrezi. Dacă partenerul nu mai e printre rândurile aduse — dezactivat, sau link
   * vechi — se **spune**, nu se deschide un formular gol.
   */
  const [focusPartner, setFocusPartner] = useUrlState("partener");
  useEffect(() => {
    if (!focusPartner) return;
    const found = (partners ?? []).find((p) => p.id === focusPartner);
    if (found) {
      // Linkul vine din „Autorizație expirată”, deci fișa se deschide direct pe autorizație.
      dialogRef.current?.openEdit(found, AUTH_STEP); // deschide și dialogul
      setFocusPartner("");
      return;
    }
    if (!isLoading && partners) {
      notify(t.partnerNotFound, "error");
      setFocusPartner("");
    }
  }, [focusPartner, partners, isLoading, setFocusPartner, notify]);
  const [newParam, setNewParam] = useUrlState("nou");
  useEffect(() => {
    if (!newParam) return;
    setNewParam("");
    if (canManage) dialogRef.current?.openCreate();
  }, [newParam, setNewParam, canManage]);






  function handleDeactivate(p: Partner) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{p.name}</strong>
          {p.cui ? ` — CUI ${p.cui}` : ""}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () => deactivate(p),
    });
  }

  function deactivate(p: Partner) {
    deactivateMut.mutate(p.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  function reactivate(p: Partner) {
    reactivateMut.mutate(p.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }


  // `n` deschide formularul, unde contul are voie. Scurtătura tace pe un cont care
  // n-ar putea salva oricum: o comandă care nu face nimic e mai rea decât una lipsă.
  useHotkey("n", () => dialogRef.current?.openCreate(), { enabled: Boolean(canManage) && !personsTab });

  return (
    <div>
      <PageHeader
        title={t.title}
        // Pe tabul „Persoane fizice” antetul spune ce e lista de dedesubt, nu ce sunt firmele.
        description={personsTab ? strings.naturalPersons.subtitle : t.subtitle}
        actions={
          canManage && !personsTab && (
            <Button onClick={() => dialogRef.current?.openCreate()}>
              <Plus className="mr-2 h-4 w-4" />
              {t.add}
            </Button>
          )
        }
      />

      {hasDepot && (
        <div role="tablist" className="mt-6 flex gap-1 border-b border-line">
          {[
            { id: "", label: strings.naturalPersons.tabFirms },
            { id: "persoane-fizice", label: strings.naturalPersons.tab },
          ].map((item) => {
            const selected = (item.id === "persoane-fizice") === personsTab;
            return (
              <button
                key={item.id || "firme"}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setTab(item.id)}
                className={
                  "-mb-px border-b-2 px-3 py-2 text-sm font-medium " +
                  (selected
                    ? "border-brand-600 text-content-strong"
                    : "border-transparent text-content-muted hover:text-content")
                }
              >
                {item.label}
              </button>
            );
          })}
        </div>
      )}

      {personsTab ? (
        <NaturalPersonsSection canManage={Boolean(canManage)} />
      ) : (
      <>
      <div className="mt-6 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <div>
          <Label htmlFor="filter-role">{t.filterRole}</Label>
          <Select
            id="filter-role"
            value={roleFilter}
            onChange={(ev) => setRoleFilter(ev.target.value as RoleFilter)}
            className="w-full sm:w-56"
          >
            <option value="">{t.filterRoleAll}</option>
            <option value="client">{roleLabels.client}</option>
            <option value="supplier">{roleLabels.supplier}</option>
            <option value="none">{roleLabels.none}</option>
            {/* Altă axă, același select: e o singură valoare, iar un al doilea dropdown ar
                costa mai mult decât explică. */}
            <optgroup label={t.carrierColumn}>
              <option value="carrier">{t.filterCarrier}</option>
            </optgroup>
          </Select>
        </div>
      </div>

      <section className="mt-4">
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
                  <SortableTH sortKey="cui" sort={view.sort} onSort={view.toggleSort}>
                    {t.cui}
                  </SortableTH>
                  <TH>{t.role}</TH>
                  <TH>{t.type}</TH>
                  <TH>{t.carrierColumn}</TH>
                  <TH>{t.authorizationNumber}</TH>
                  <SortableTH
                    sortKey="authorizationExpiry"
                    sort={view.sort}
                    onSort={view.toggleSort}
                  >
                    {t.authorizationValidUntil}
                  </SortableTH>
                  <TH>{strings.common.status}</TH>
                  {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
                </TR>
              </THead>
              <TBody>
                {(isLoading || visiblePartners.length === 0) && (
                  <TableFallbackRow
                    columns={canManage ? 9 : 8}
                    loading={isLoading}
                    icon={Users}
                    title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                    description={
                      view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                    }
                    action={
                      canManage && (
                        <Button onClick={() => dialogRef.current?.openCreate()}>
                          <Plus className="mr-2 h-4 w-4" />
                          {t.add}
                        </Button>
                      )
                    }
                  />
                )}
                {visiblePartners.map((p) => (
                  <TR key={p.id}>
                    <TD>
                      <div className="font-medium text-content">{p.name}</div>
                      <PartnerPlaces
                        partner={p}
                        canManage={Boolean(canManage)}
                        onOpen={(addRow) => dialogRef.current?.openEdit(p, PLACES_STEP, addRow)}
                      />
                    </TD>
                    <TD>{p.cui || "—"}</TD>
                    <TD>
                      <PartnerRoleBadge partner={p} />
                    </TD>
                    <TD>
                      {p.type ? (
                        typeLabels[p.type]
                      ) : (
                        <span className="text-content-muted">{t.typeNoneShort}</span>
                      )}
                    </TD>
                    <TD>
                      {p.carrier ? (
                        <Badge variant="muted">{t.carrierYes}</Badge>
                      ) : (
                        <span className="text-content-subtle">{t.carrierNo}</span>
                      )}
                    </TD>
                    <TD>{p.authorizationNumber || "—"}</TD>
                    <TD>
                      <ExpiryBadge partner={p} />
                    </TD>
                    <TD>
                      {p.active ? (
                        <Badge variant="success">{t.active}</Badge>
                      ) : (
                        <Badge variant="muted">{t.inactive}</Badge>
                      )}
                    </TD>
                    {canManage && (
                      <TD sticky="right" className="text-right">
                        {/* „Dezactivează” a intrat în „⋯” lângă „Puncte de lucru și șoferi” (17.09.2026):
                            rândul de sub nume cerea lățimea, iar tabelul nu mai încăpea la 1440px. */}
                        <div className="flex items-center justify-end gap-1">
                          <Button variant="ghost" size="sm" onClick={() => dialogRef.current?.openEdit(p)}>
                            <Pencil className="mr-1 h-3.5 w-3.5" />
                            {strings.common.edit}
                          </Button>
                          <Menu>
                            <MenuItem icon={MapPin} onClick={() => dialogRef.current?.openEdit(p, PLACES_STEP)}>
                              {t.step4Name}
                            </MenuItem>
                            {p.active ? (
                              <MenuItem icon={Ban} tone="danger" onClick={() => handleDeactivate(p)}>
                                {t.deactivate}
                              </MenuItem>
                            ) : (
                              <MenuItem icon={RotateCcw} onClick={() => reactivate(p)}>
                                {strings.common.reactivate}
                              </MenuItem>
                            )}
                          </Menu>
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
      </>
      )}


      <PartnerFormDialog ref={dialogRef} />

      {confirmDialog}
    </div>
  );
}
