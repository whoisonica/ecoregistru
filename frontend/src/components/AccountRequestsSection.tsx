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

const t = strings.accountRequest;
const typeLabels = strings.enums.companyType;
const marketRoleLabels = strings.enums.marketRole;

function StatusBadge({ request }: { request: AccountRequest }) {
  if (request.status === "APPROVED") return <Badge variant="success">{t.status.APPROVED}</Badge>;
  if (request.status === "REJECTED") return <Badge variant="muted">{t.status.REJECTED}</Badge>;
  return <Badge variant="warning">{t.status.NEW}</Badge>;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("ro-RO");
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

  function handleApprove(r: AccountRequest) {
    approveMut.mutate(r.id, {
      onSuccess: () => notify(t.approved, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.actionError), "error"),
    });
  }

  function handleReject(r: AccountRequest) {
    const reason = window.prompt(t.rejectPrompt);
    if (reason === null) return;
    rejectMut.mutate(
      { id: r.id, reason },
      {
        onSuccess: () => notify(t.rejected, "success"),
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
                  <TH className="text-right">{strings.common.actions}</TH>
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
                    <TD className="text-right">
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
                            onClick={() => handleReject(r)}
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
    </section>
  );
}
