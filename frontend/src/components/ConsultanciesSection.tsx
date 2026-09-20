import { useState, type FormEvent } from "react";
import { notifyInvited } from "@/lib/inviteNotice";
import { Briefcase, Plus, Receipt, UserPlus } from "lucide-react";
import { SubscriptionDialog } from "@/components/SubscriptionDialog";
import {
  useConsultancies,
  useCreateConsultancy,
  useInviteConsultant,
} from "@/hooks/useConsultancies";
import { useAssignConsultancy } from "@/hooks/useCompanies";
import type { Company, Consultancy } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { isValidCui } from "@/lib/cui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CuiField } from "@/components/AnafLookup";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { LoadError } from "@/components/ui/load-error";

const t = strings.consultancies;
const u = strings.settings.users;

/**
 * P2.13 — cabinetele de consultanță, pe ecranul Clienți al platformei.
 *
 * <p>Ce face platforma aici e puțin, dinadins: creează cabinetul (după contract) și îi invită primul
 * consultant. De acolo cabinetul își adaugă singur firmele și colegii. Mutarea unei firme existente
 * într-un cabinet stă pe rândul firmei, mai sus — {@link AssignConsultancyDialog} —, fiindcă e o
 * decizie despre firmă.
 */
export function ConsultanciesSection() {
  const { data: consultancies, isLoading, isError, refetch } = useConsultancies(true);
  const createMut = useCreateConsultancy();
  const inviteMut = useInviteConsultant();
  const { notify } = useToast();

  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState("");
  const [cui, setCui] = useState("");
  const [formError, setFormError] = useState<false | "name" | "cui" | "cuiInvalid">(false);

  const [inviting, setInviting] = useState<Consultancy | null>(null);
  const [billing, setBilling] = useState<Consultancy | null>(null);
  const [email, setEmail] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [emailError, setEmailError] = useState(false);

  const view = useTableView(consultancies ?? [], {
    searchText: (c) => [c.name, c.cui].join(" "),
    comparators: {
      name: (a, b) => a.name.localeCompare(b.name, "ro"),
      companies: (a, b) => a.companyCount - b.companyCount,
    },
    initialSort: { key: "name", direction: "asc" },
  });

  function openCreate() {
    setName("");
    setCui("");
    setFormError(false);
    setCreateOpen(true);
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return setFormError("name");
    if (!cui.trim()) return setFormError("cui");
    if (!isValidCui(cui)) return setFormError("cuiInvalid");
    try {
      await createMut.mutateAsync({ name: name.trim(), cui: cui.trim() });
      notify(t.created, "success");
      setCreateOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function openInvite(c: Consultancy) {
    setEmail("");
    setFirstName("");
    setLastName("");
    setEmailError(false);
    setInviting(c);
  }

  async function handleInvite(e: FormEvent) {
    e.preventDefault();
    if (!inviting) return;
    if (!email.trim()) return setEmailError(true);
    try {
      const invited = await inviteMut.mutateAsync({
        id: inviting.id,
        input: {
          email: email.trim(),
          firstName: firstName.trim() || null,
          lastName: lastName.trim() || null,
        },
      });
      notifyInvited(notify, invited, t.invited, strings.common.inviteMailFailedNoList);
      setInviting(null);
    } catch (err) {
      notify(apiErrorMessage(err, t.inviteError), "error");
    }
  }

  return (
    <section id="cabinete" className="mt-10 scroll-mt-20">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-2xl text-sm text-content-muted">{t.subtitle}</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" />
          {t.add}
        </Button>
      </div>

      {isError && <LoadError message={t.loadError} onRetry={refetch} />}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                  {t.name}
                </SortableTH>
                <TH>{t.cui}</TH>
                <SortableTH sortKey="companies" sort={view.sort} onSort={view.toggleSort}>
                  {t.companies}
                </SortableTH>
                <TH>{t.consultants}</TH>
                <TH sticky="right" className="text-right">
                  {strings.common.actions}
                </TH>
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={5}
                  loading={isLoading}
                  icon={Briefcase}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((c) => (
                <TR key={c.id}>
                  <TD className="font-medium text-content">{c.name}</TD>
                  <TD>{c.cui}</TD>
                  <TD>{c.companyCount}</TD>
                  <TD>{c.consultantCount}</TD>
                  <TD sticky="right" className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="sm" onClick={() => setBilling(c)}>
                        <Receipt className="mr-1 h-3.5 w-3.5" />
                        {strings.subscriptions.action}
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => openInvite(c)}>
                        <UserPlus className="mr-1 h-3.5 w-3.5" />
                        {t.invite}
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

      <Dialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title={t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setCreateOpen(false)} disabled={createMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="consultancy-form" disabled={createMut.isPending}>
              {createMut.isPending ? strings.common.saving : t.add}
            </Button>
          </>
        }
      >
        <form id="consultancy-form" onSubmit={handleCreate} className="space-y-4">
          <div>
            <Label htmlFor="cons-name">{t.name}</Label>
            <Input
              id="cons-name" maxLength={255}
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (formError === "name") setFormError(false);
              }}
              placeholder={t.namePlaceholder}
              aria-invalid={formError === "name"}
              autoFocus
            />
            {formError === "name" && (
              <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
            )}
          </div>
          <CuiField
            id="cons-cui"
            label={t.cui}
            value={cui}
            onChange={(v) => {
              setCui(v);
              if (formError === "cui" || formError === "cuiInvalid") setFormError(false);
            }}
            placeholder={t.cuiPlaceholder}
            invalid={{ "aria-invalid": formError === "cui" || formError === "cuiInvalid" }}
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
            ]}
          />
        </form>
      </Dialog>

      <Dialog
        open={inviting !== null}
        onClose={() => setInviting(null)}
        title={t.inviteTitle.replace("{consultancy}", inviting?.name ?? "")}
        footer={
          <>
            <Button variant="outline" onClick={() => setInviting(null)} disabled={inviteMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="consultant-invite-form" disabled={inviteMut.isPending}>
              {inviteMut.isPending ? strings.common.saving : t.invite}
            </Button>
          </>
        }
      >
        <form id="consultant-invite-form" onSubmit={handleInvite} className="space-y-4">
          <p className="text-xs text-content-muted">{u.inviteHint}</p>
          <div>
            <Label htmlFor="cons-email">{u.email}</Label>
            <Input
              id="cons-email" maxLength={255}
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (emailError) setEmailError(false);
              }}
              placeholder={u.emailPlaceholder}
              aria-invalid={emailError}
              autoFocus
            />
            {emailError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="cons-first">{u.firstName}</Label>
              <Input id="cons-first" maxLength={128} value={firstName} onChange={(e) => setFirstName(e.target.value)} />
            </div>
            <div>
              <Label htmlFor="cons-last">{u.lastName}</Label>
              <Input id="cons-last" maxLength={128} value={lastName} onChange={(e) => setLastName(e.target.value)} />
            </div>
          </div>
        </form>
      </Dialog>

      {billing && (
        <SubscriptionDialog
          owner={{ kind: "consultancy", id: billing.id, name: billing.name }}
          onClose={() => setBilling(null)}
        />
      )}
    </section>
  );
}

/**
 * P2.13 — în ce cabinet stă o firmă. Numai platforma: schimbă cine îi citește evidența, deci nu e
 * ceva ce un cabinet își face singur.
 */
export function AssignConsultancyDialog({ company, onClose }: { company: Company; onClose: () => void }) {
  const c = strings.clients;
  const { data: consultancies } = useConsultancies(true);
  const assignMut = useAssignConsultancy();
  const { notify } = useToast();
  const [selected, setSelected] = useState(company.consultancyId ?? "");

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await assignMut.mutateAsync({ id: company.id, consultancyId: selected || null });
      notify(c.assigned, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, c.assignError), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={c.assignTitle.replace("{company}", company.name)}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={assignMut.isPending}>
            {strings.common.cancel}
          </Button>
          <Button type="submit" form="assign-consultancy-form" disabled={assignMut.isPending}>
            {assignMut.isPending ? strings.common.saving : strings.common.save}
          </Button>
        </>
      }
    >
      <form id="assign-consultancy-form" onSubmit={handleSubmit} className="space-y-4">
        <p className="text-xs text-content-muted">{c.assignHint}</p>
        <div>
          <Label htmlFor="assign-consultancy">{c.consultancy}</Label>
          <Select
            id="assign-consultancy"
            value={selected}
            onChange={(e) => setSelected(e.target.value)}
          >
            <option value="">{c.assignNone}</option>
            {consultancies?.map((k) => (
              <option key={k.id} value={k.id}>
                {k.name}
              </option>
            ))}
          </Select>
        </div>
      </form>
    </Dialog>
  );
}
