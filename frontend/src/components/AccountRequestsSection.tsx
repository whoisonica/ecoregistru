import { useState } from "react";
import { Check, Inbox, X } from "lucide-react";
import {
  useAccountRequests,
  useApproveAccountRequest,
  useRejectAccountRequest,
} from "@/hooks/useAccountRequests";
import type { AccountRequest } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { Dialog } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { formatDate } from "@/lib/utils";

const t = strings.accountRequest;
const typeLabels = strings.enums.companyType;
const marketRoleLabels = strings.enums.marketRole;

function StatusBadge({ request }: { request: AccountRequest }) {
  if (request.status === "APPROVED") return <Badge variant="success">{t.status.APPROVED}</Badge>;
  if (request.status === "REJECTED") return <Badge variant="muted">{t.status.REJECTED}</Badge>;
  return <Badge variant="warning">{t.status.NEW}</Badge>;
}

/**
 * The inbox behind the closed register: the forms clients submitted, and the one action that turns
 * one into an account.
 *
 * <p>Approving creates the company with the profile the client answered — it does not invite
 * anyone. Creating an account and giving a person access stay two deliberate acts, so support
 * invites the user from the list above once the company exists.
 */
export function AccountRequestsSection({ enabled }: { enabled: boolean }) {
  const { data: requests, isLoading, isError } = useAccountRequests(enabled);
  const approveMut = useApproveAccountRequest();
  const rejectMut = useRejectAccountRequest();
  const { notify } = useToast();

  const busy = approveMut.isPending || rejectMut.isPending;
  const [confirm, confirmDialog] = useConfirm();
  // Cererea pe cale de a fi respinsă, cu motivul care se scrie. `null` = dialogul e închis.
  const [rejecting, setRejecting] = useState<AccountRequest | null>(null);
  const [reason, setReason] = useState("");

  /**
   * Aprobarea creează o firmă reală și nu se poate desface: nu există ștergere de firmă, iar
   * cererea rămâne pe veci ca urmă de hârtie. Ștergerea unei mișcări întreabă de mult, cu
   * identitatea rândului în întrebare; asta nu întreba nimic, deși e fapta cu urmări mai mari.
   */
  function handleApprove(r: AccountRequest) {
    confirm({
      title: t.confirmApproveTitle,
      message: (
        <>
          <strong className="text-content">{r.companyName}</strong>
          {r.cui ? ` — CUI ${r.cui}` : ""}. {t.confirmApprove}
        </>
      ),
      confirmLabel: t.approve,
      onConfirm: () =>
        approveMut.mutate(r.id, {
          onSuccess: () => notify(t.approved, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.actionError), "error"),
        }),
    });
  }

  function openReject(r: AccountRequest) {
    setRejecting(r);
    setReason("");
  }

  /**
   * Respingerea cerea motivul printr-un `window.prompt` — ultimul dialog nativ rămas, după ce cele
   * cinci `window.confirm` au fost înlocuite. Butoane în limba sistemului de operare, pe ecranul
   * administratorului de platformă, și fără nicio cale de a spune **care** cerere se respinge.
   * În plus, un motiv gol trecea: `null` era singura ieșire verificată.
   */
  function submitReject() {
    if (!rejecting || !reason.trim()) return;
    rejectMut.mutate(
      { id: rejecting.id, reason: reason.trim() },
      {
        onSuccess: () => {
          notify(t.rejected, "success");
          setRejecting(null);
        },
        onError: (err) => notify(apiErrorMessage(err, t.actionError), "error"),
      }
    );
  }

  const view = useTableView(requests ?? [], {
    searchText: (r) => [r.companyName, r.cui, r.contactEmail, r.contactName].filter(Boolean).join(" "),
    comparators: {
      companyName: (a, b) => a.companyName.localeCompare(b.companyName, "ro"),
      createdAt: (a, b) => a.createdAt.localeCompare(b.createdAt),
    },
    initialSort: { key: "createdAt", direction: "desc" },
  });

  return (
    <section className="mt-10">
      <h2 className="text-lg font-semibold text-content">{t.adminTitle}</h2>
      <p className="mt-1 text-sm text-content-muted">{t.adminSubtitle}</p>

      <div className="mt-3">
        {isError && <p className="text-sm text-red-600">{t.adminLoadError}</p>}

        {!isError && (
          <>
            <TableToolbar view={view} placeholder={t.adminSearchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="companyName" sort={view.sort} onSort={view.toggleSort}>
                    {t.colCompany}
                  </SortableTH>
                  <TH>{t.colType}</TH>
                  <TH>{t.colContact}</TH>
                  <TH>{t.colWaste}</TH>
                  <SortableTH sortKey="createdAt" sort={view.sort} onSort={view.toggleSort}>
                    {t.colDate}
                  </SortableTH>
                  <TH>{t.colStatus}</TH>
                  <TH sticky="right" className="text-right">{strings.common.actions}</TH>
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={7}
                    loading={isLoading}
                    icon={Inbox}
                    title={view.emptiedBySearch ? strings.common.noResults : t.adminEmpty}
                  />
                )}
                {view.visible.map((r) => (
                  <TR key={r.id}>
                    <TD>
                      <span className="font-medium text-content">{r.companyName}</span>
                      <span className="block text-xs text-content-subtle">{r.cui}</span>
                      {r.workPointAddress && (
                        <span className="block max-w-xs truncate text-xs text-content-subtle">
                          {r.workPointAddress}
                        </span>
                      )}
                    </TD>
                    <TD>
                      {typeLabels[r.companyType]}
                      {r.caenCode && (
                        <span className="block text-xs text-content-subtle">CAEN {r.caenCode}</span>
                      )}
                      {(r.marketRoles ?? []).length > 0 && (
                        <span className="block text-xs text-content-muted">
                          {(r.marketRoles ?? []).map((m) => marketRoleLabels[m]).join(", ")}
                        </span>
                      )}
                    </TD>
                    <TD>
                      <span className="block text-content-strong">{r.contactEmail}</span>
                      {r.contactName && (
                        <span className="block text-xs text-content-subtle">
                          {r.contactName}
                          {r.contactRole ? ` · ${r.contactRole}` : ""}
                        </span>
                      )}
                    </TD>
                    <TD className="max-w-xs">
                      <span className="block truncate text-content-strong">{r.wasteCodesText || "—"}</span>
                      {(r.operationCodes ?? []).length > 0 && (
                        <span className="block text-xs text-content-subtle">
                          {(r.operationCodes ?? []).join(", ")}
                        </span>
                      )}
                    </TD>
                    <TD className="whitespace-nowrap">{formatDate(r.createdAt)}</TD>
                    <TD>
                      <StatusBadge request={r} />
                    </TD>
                    <TD sticky="right" className="text-right">
                      {r.status === "NEW" && (
                        <div className="flex justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={busy}
                            onClick={() => handleApprove(r)}
                          >
                            <Check className="mr-1 h-3.5 w-3.5" />
                            {t.approve}
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            disabled={busy}
                            onClick={() => openReject(r)}
                          >
                            <X className="mr-1 h-3.5 w-3.5" />
                            {t.reject}
                          </Button>
                        </div>
                      )}
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
            <TablePagination view={view} />
          </>
        )}
      </div>

      <Dialog
        open={rejecting !== null}
        onClose={() => setRejecting(null)}
        title={t.rejectTitle}
        description={rejecting ? `${rejecting.companyName}${rejecting.cui ? ` — ${rejecting.cui}` : ""}` : undefined}
        busy={rejectMut.isPending}
        footer={
          <>
            <Button variant="outline" onClick={() => setRejecting(null)} disabled={rejectMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button
              variant="danger"
              onClick={submitReject}
              disabled={!reason.trim()}
              loading={rejectMut.isPending}
            >
              {t.reject}
            </Button>
          </>
        }
      >
        <div>
          <Label htmlFor="ar-reject-reason">{t.rejectReasonLabel}</Label>
          <Textarea
            id="ar-reject-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={t.rejectReasonPlaceholder}
            rows={3}
            maxLength={500}
            autoFocus
          />
          <p className="mt-1 text-xs text-content-muted">{t.rejectReasonHint}</p>
        </div>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
