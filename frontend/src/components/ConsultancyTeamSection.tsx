import { useState, type FormEvent } from "react";
import { notifyInvited } from "@/lib/inviteNotice";
import { Ban, Mail, Plus, RotateCcw, Users, X } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  useCancelColleagueInvite,
  useConsultancyTeam,
  useDeactivateColleague,
  useInviteColleague,
  useReactivateColleague,
  useResendColleagueInvite,
} from "@/hooks/useConsultancies";
import type { CompanyUser, InviteConsultantInput } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { LoadError } from "@/components/ui/load-error";

const t = strings.consultancyTeam;
// Etichetele comune — email, stare, dezactivare — sunt ale ecranului de utilizatori al firmei:
// același fel de rând, aceleași cuvinte.
const u = strings.settings.users;

/**
 * P2.13 — colegii din cabinet, pe ecranul Clienți al consultantului.
 *
 * <p>Sora mai mică a lui `CompanyUsersSection`: fără rol de ales (într-un cabinet toți sunt
 * consultanți, cu aceleași drepturi). Din felia 2 are și retrimiterea și anularea invitației, cu
 * aceleași cuvinte ca la firmă. Rândul propriu n-are niciun buton — serverul refuză oricum.
 */
export function ConsultancyTeamSection() {
  const { user: me } = useAuth();
  const { data: team, isLoading, isError, refetch } = useConsultancyTeam(true);
  const inviteMut = useInviteColleague();
  const deactivateMut = useDeactivateColleague();
  const reactivateMut = useReactivateColleague();
  const resendMut = useResendColleagueInvite();
  const cancelMut = useCancelColleagueInvite();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [inviteOpen, setInviteOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [emailError, setEmailError] = useState(false);

  const view = useTableView(team ?? [], {
    searchText: (m) => [m.email, m.firstName, m.lastName].filter(Boolean).join(" "),
    comparators: { email: (a, b) => a.email.localeCompare(b.email, "ro") },
  });

  function openInvite() {
    setEmail("");
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
    const input: InviteConsultantInput = {
      email: email.trim(),
      firstName: firstName.trim() || null,
      lastName: lastName.trim() || null,
    };
    try {
      const invited = await inviteMut.mutateAsync(input);
      notifyInvited(notify, invited, t.invited);
      setInviteOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, u.inviteError), "error");
    }
  }

  function handleDeactivate(m: CompanyUser) {
    confirm({
      title: u.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{m.email}</strong>. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: u.deactivate,
      onConfirm: async () => {
        try {
          await deactivateMut.mutateAsync(m.id);
          notify(u.deactivated, "success");
        } catch (err) {
          notify(apiErrorMessage(err, u.saveError), "error");
        }
      },
    });
  }

  async function handleReactivate(m: CompanyUser) {
    try {
      await reactivateMut.mutateAsync(m.id);
      notify(u.reactivated, "success");
    } catch (err) {
      notify(apiErrorMessage(err, u.saveError), "error");
    }
  }

  async function handleResend(m: CompanyUser) {
    try {
      await resendMut.mutateAsync(m.id);
      notify(u.resent, "success");
    } catch (err) {
      notify(apiErrorMessage(err, u.resendError), "error");
    }
  }

  function handleCancelInvite(m: CompanyUser) {
    confirm({
      title: u.confirmCancelTitle,
      message: (
        <>
          <strong className="text-content">{m.email}</strong>. {u.confirmCancel}
        </>
      ),
      confirmLabel: u.cancelInvite,
      tone: "danger",
      onConfirm: async () => {
        try {
          await cancelMut.mutateAsync(m.id);
          notify(u.cancelled, "success");
        } catch (err) {
          notify(apiErrorMessage(err, u.saveError), "error");
        }
      },
    });
  }

  function statusBadge(m: CompanyUser) {
    if (m.status === "ACTIVE") return <Badge variant="success">{u.statusActive}</Badge>;
    if (m.status === "PENDING_INVITE") return <Badge variant="warning">{u.statusPending}</Badge>;
    return <Badge variant="muted">{u.statusDeactivated}</Badge>;
  }

  return (
    <section id="echipa-cabinetului" className="mt-10 scroll-mt-20">
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

      {isError && <LoadError message={t.loadError} onRetry={refetch} />}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={u.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="email" sort={view.sort} onSort={view.toggleSort}>
                  {u.email}
                </SortableTH>
                <TH>{u.name}</TH>
                <TH>{u.status}</TH>
                <TH sticky="right" className="text-right">
                  {strings.common.actions}
                </TH>
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={4}
                  loading={isLoading}
                  icon={Users}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((m) => {
                const isMe = me?.email === m.email;
                return (
                  <TR key={m.id}>
                    <TD className="font-medium text-content">
                      {m.email}
                      {isMe && (
                        <span className="ml-2 text-xs font-normal text-content-subtle">({u.you})</span>
                      )}
                    </TD>
                    <TD>{[m.firstName, m.lastName].filter(Boolean).join(" ") || "—"}</TD>
                    <TD>{statusBadge(m)}</TD>
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        {m.status === "DEACTIVATED" && (
                          <Button variant="ghost" size="sm" onClick={() => handleReactivate(m)}>
                            <RotateCcw className="mr-1 h-3.5 w-3.5" />
                            {strings.common.reactivate}
                          </Button>
                        )}
                        {m.status === "ACTIVE" && !isMe && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            onClick={() => handleDeactivate(m)}
                          >
                            <Ban className="mr-1 h-3.5 w-3.5" />
                            {u.deactivate}
                          </Button>
                        )}
                        {m.status === "PENDING_INVITE" && !isMe && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              aria-label={u.resend}
                              disabled={resendMut.isPending}
                              onClick={() => handleResend(m)}
                            >
                              <Mail className="mr-1 h-3.5 w-3.5" />
                              {u.resendShort}
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              aria-label={u.cancelInvite}
                              className="text-red-600 hover:bg-red-50"
                              onClick={() => handleCancelInvite(m)}
                            >
                              <X className="mr-1 h-3.5 w-3.5" />
                              {u.cancelInviteShort}
                            </Button>
                          </>
                        )}
                        {isMe && (
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
            <Button type="submit" form="team-invite-form" disabled={inviteMut.isPending}>
              {inviteMut.isPending ? strings.common.saving : t.invite}
            </Button>
          </>
        }
      >
        <form id="team-invite-form" onSubmit={handleInvite} className="space-y-4">
          <p className="text-xs text-content-muted">{u.inviteHint}</p>
          <div>
            <Label htmlFor="team-email">{u.email}</Label>
            <Input
              id="team-email" maxLength={255}
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (emailError) setEmailError(false);
              }}
              placeholder={u.emailPlaceholder}
              autoFocus
            />
            {emailError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="team-first">{u.firstName}</Label>
              <Input id="team-first" maxLength={128} value={firstName} onChange={(e) => setFirstName(e.target.value)} />
            </div>
            <div>
              <Label htmlFor="team-last">{u.lastName}</Label>
              <Input id="team-last" maxLength={128} value={lastName} onChange={(e) => setLastName(e.target.value)} />
            </div>
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
