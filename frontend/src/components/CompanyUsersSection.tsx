import { useState, type FormEvent } from "react";
import { Ban, Mail, Plus, RotateCcw, ShieldCheck, Users, X } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  useUsers,
  useInviteCompanyUser,
  useResendInvite,
  useCancelInvite,
  useChangeUserRole,
  useDeactivateUser,
  useReactivateUser,
} from "@/hooks/useUsers";
import type { CompanyUser, InviteRole, InviteUserInput } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";

const t = strings.settings.users;
const roleLabels = strings.enums.inviteRole;
const INVITE_ROLES: InviteRole[] = ["ADMIN", "OPERATOR", "CLIENT_VIEWER"];

/**
 * P1.12 — utilizatorii firmei.
 *
 * <p>Stă în Setări, lângă punctele de lucru și șoferi, fiindcă e același fel de lucru: date ale
 * firmei pe care și le ține singură. **Nu** e ecranul de Clienți — acolo administratorul de
 * platformă alege o firmă după id; aici firma e cea din sesiune și nu se numește nicăieri.
 *
 * <p>Cele trei stări sunt tot ce trebuie citit ca să se înțeleagă ecranul, şi sunt motivul pentru
 * care `V34` există: **Activ** are parolă și intră; **În aşteptare** a fost invitat şi n-a apăsat
 * încă linkul, deci singurul buton care i se potriveşte e „Retrimite invitaţia"; **Dezactivat** a
 * avut parolă şi i s-a luat accesul, deci al lui e „Reactivează". Un singur `enabled` nu putea
 * spune care dintre ultimele două e.
 *
 * <p>Rândul propriu n-are butoane, iar serverul refuză oricum: cine se dezactivează singur nu se
 * mai poate repara din aplicaţie.
 */
export function CompanyUsersSection({ canManage }: { canManage: boolean }) {
  const { user: me } = useAuth();
  const { data: users, isLoading, isError } = useUsers(canManage);
  const inviteMut = useInviteCompanyUser();
  const resendMut = useResendInvite();
  const cancelMut = useCancelInvite();
  const roleMut = useChangeUserRole();
  const deactivateMut = useDeactivateUser();
  const reactivateMut = useReactivateUser();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [inviteOpen, setInviteOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<InviteRole>("OPERATOR");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [emailError, setEmailError] = useState(false);

  const view = useTableView(users ?? [], {
    searchText: (u) =>
      [u.email, u.firstName, u.lastName, roleLabels[u.role]].filter(Boolean).join(" "),
    comparators: {
      email: (a, b) => a.email.localeCompare(b.email, "ro"),
      role: (a, b) => roleLabels[a.role].localeCompare(roleLabels[b.role], "ro"),
    },
  });

  if (!canManage) return null;

  function openInvite() {
    setEmail("");
    setRole("OPERATOR");
    setFirstName("");
    setLastName("");
    setEmailError(false);
    setInviteOpen(true);
  }

  async function handleInvite(e: FormEvent) {
    e.preventDefault();
    if (!email.trim()) {
      setEmailError(true);
      return;
    }
    const input: InviteUserInput = {
      email: email.trim(),
      role,
      firstName: firstName.trim() || null,
      lastName: lastName.trim() || null,
    };
    try {
      await inviteMut.mutateAsync(input);
      notify(t.invited, "success");
      setInviteOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.inviteError), "error");
    }
  }

  async function handleResend(u: CompanyUser) {
    try {
      await resendMut.mutateAsync(u.id);
      notify(t.resent, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.resendError), "error");
    }
  }

  async function handleRoleChange(u: CompanyUser, next: InviteRole) {
    if (next === u.role) return;
    try {
      await roleMut.mutateAsync({ id: u.id, role: next });
      notify(t.roleChanged, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function handleDeactivate(u: CompanyUser) {
    confirm({
      title: t.confirmDeactivateTitle,
      // Mesajul poartă identitatea contului, ca la celelalte confirmări: „ești sigur?" nu
      // opreşte nicio greşeală, adresa scrisă cu litere îngroşate o opreşte.
      message: (
        <>
          <strong className="text-content">{u.email}</strong>
          {fullName(u) === "—" ? "" : ` — ${fullName(u)}`}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      // Fără `danger`: dezactivarea se desface dintr-un buton, chiar de pe rândul ăsta.
      onConfirm: async () => {
        try {
          await deactivateMut.mutateAsync(u.id);
          notify(t.deactivated, "success");
        } catch (err) {
          notify(apiErrorMessage(err, t.saveError), "error");
        }
      },
    });
  }

  function handleCancelInvite(u: CompanyUser) {
    confirm({
      title: t.confirmCancelTitle,
      message: (
        <>
          <strong className="text-content">{u.email}</strong>. {t.confirmCancel}
        </>
      ),
      confirmLabel: t.cancelInvite,
      // `danger`, spre deosebire de dezactivare: rândul chiar dispare, și e singurul buton
      // din aplicație despre care asta e adevărat.
      tone: "danger",
      onConfirm: async () => {
        try {
          await cancelMut.mutateAsync(u.id);
          notify(t.cancelled, "success");
        } catch (err) {
          notify(apiErrorMessage(err, t.saveError), "error");
        }
      },
    });
  }

  async function handleReactivate(u: CompanyUser) {
    try {
      await reactivateMut.mutateAsync(u.id);
      notify(t.reactivated, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function statusBadge(u: CompanyUser) {
    if (u.status === "ACTIVE") return <Badge variant="success">{t.statusActive}</Badge>;
    if (u.status === "PENDING_INVITE") return <Badge variant="warning">{t.statusPending}</Badge>;
    return <Badge variant="muted">{t.statusDeactivated}</Badge>;
  }

  function fullName(u: CompanyUser) {
    return [u.firstName, u.lastName].filter(Boolean).join(" ") || "—";
  }

  return (
    <section id="utilizatori" className="mt-8 scroll-mt-20">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-2xl text-sm text-content-muted">{t.subtitle}</p>
        </div>
        <Button onClick={openInvite}>
          <Plus className="mr-2 h-4 w-4" />
          {t.invite}
        </Button>
      </div>

      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="email" sort={view.sort} onSort={view.toggleSort}>
                  {t.email}
                </SortableTH>
                <TH>{t.name}</TH>
                <SortableTH sortKey="role" sort={view.sort} onSort={view.toggleSort}>
                  {t.role}
                </SortableTH>
                <TH>{t.status}</TH>
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
                  icon={Users}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                  action={
                    <Button onClick={openInvite}>
                      <Plus className="mr-2 h-4 w-4" />
                      {t.invite}
                    </Button>
                  }
                />
              )}
              {view.visible.map((u) => {
                const isMe = me?.email === u.email;
                return (
                  <TR key={u.id}>
                    <TD className="font-medium text-content">
                      {u.email}
                      {isMe && (
                        <span className="ml-2 text-xs font-normal text-content-subtle">
                          ({t.you})
                        </span>
                      )}
                    </TD>
                    <TD>{fullName(u)}</TD>
                    <TD>
                      {/* Rândul propriu arată rolul, nu-l oferă: serverul refuză oricum, iar un
                          control care nu poate reuși e mai rău decât o etichetă. */}
                      {isMe ? (
                        <span className="inline-flex items-center gap-1.5 text-content">
                          <ShieldCheck className="h-3.5 w-3.5 text-content-subtle" />
                          {roleLabels[u.role]}
                        </span>
                      ) : (
                        <Select
                          aria-label={t.changeRole}
                          value={u.role}
                          disabled={roleMut.isPending}
                          onChange={(e) => handleRoleChange(u, e.target.value as InviteRole)}
                        >
                          {INVITE_ROLES.map((r) => (
                            <option key={r} value={r}>
                              {roleLabels[r]}
                            </option>
                          ))}
                        </Select>
                      )}
                    </TD>
                    <TD>{statusBadge(u)}</TD>
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        {/* Fiecare stare are exact butoanele care i se potrivesc. Un invitat nu
                            se dezactivează — n-are ce lua, iar drumul înapoi ar fi chiar starea
                            stricată pe care serverul o refuză — se anulează. */}
                        {u.status === "PENDING_INVITE" && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              aria-label={t.resend}
                              disabled={resendMut.isPending}
                              onClick={() => handleResend(u)}
                            >
                              <Mail className="mr-1 h-3.5 w-3.5" />
                              {t.resendShort}
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              aria-label={t.cancelInvite}
                              className="text-red-600 hover:bg-red-50"
                              onClick={() => handleCancelInvite(u)}
                            >
                              <X className="mr-1 h-3.5 w-3.5" />
                              {t.cancelInviteShort}
                            </Button>
                          </>
                        )}
                        {u.status === "DEACTIVATED" && (
                          <Button variant="ghost" size="sm" onClick={() => handleReactivate(u)}>
                            <RotateCcw className="mr-1 h-3.5 w-3.5" />
                            {strings.common.reactivate}
                          </Button>
                        )}
                        {u.status === "ACTIVE" && !isMe && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            onClick={() => handleDeactivate(u)}
                          >
                            <Ban className="mr-1 h-3.5 w-3.5" />
                            {t.deactivate}
                          </Button>
                        )}
                        {u.status === "ACTIVE" && isMe && (
                          <span className="text-xs text-content-subtle">—</span>
                        )}
                      </div>
                    </TD>
                  </TR>
                );
              })}
            </TBody>
          </Table>
          <TablePagination view={view} />
          <p className="mt-2 text-xs text-content-muted">{t.roleLegend}</p>
        </>
      )}

      <Dialog
        open={inviteOpen}
        onClose={() => setInviteOpen(false)}
        title={t.inviteTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setInviteOpen(false)} disabled={inviteMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="company-invite-form" disabled={inviteMut.isPending}>
              {inviteMut.isPending ? strings.common.saving : t.invite}
            </Button>
          </>
        }
      >
        <form id="company-invite-form" onSubmit={handleInvite} className="space-y-4">
          <p className="text-xs text-content-muted">{t.inviteHint}</p>
          <div>
            <Label htmlFor="cu-email">{t.email}</Label>
            <Input
              id="cu-email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (emailError) setEmailError(false);
              }}
              placeholder={t.emailPlaceholder}
              autoFocus
            />
            {emailError && (
              <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
            )}
          </div>
          <div>
            <Label htmlFor="cu-role">{t.role}</Label>
            <Select
              id="cu-role"
              value={role}
              onChange={(e) => setRole(e.target.value as InviteRole)}
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
              <Label htmlFor="cu-first">{t.firstName}</Label>
              <Input id="cu-first" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
            </div>
            <div>
              <Label htmlFor="cu-last">{t.lastName}</Label>
              <Input id="cu-last" value={lastName} onChange={(e) => setLastName(e.target.value)} />
            </div>
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
