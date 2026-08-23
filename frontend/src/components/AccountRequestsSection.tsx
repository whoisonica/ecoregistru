import { Check, X } from "lucide-react";
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
import { useToast } from "@/components/ui/toast";

const t = strings.accountRequest;
const typeLabels = strings.enums.companyType;

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

  return (
    <section className="mt-10">
      <h2 className="text-lg font-semibold text-gray-900">{t.adminTitle}</h2>
      <p className="mt-1 text-sm text-gray-500">{t.adminSubtitle}</p>

      <div className="mt-3">
        {isLoading && <p className="text-sm text-gray-500">{strings.common.loading}</p>}
        {isError && <p className="text-sm text-red-600">{t.adminLoadError}</p>}

        {!isLoading && !isError && (
          <Table>
            <THead>
              <TR>
                <TH>{t.colCompany}</TH>
                <TH>{t.colType}</TH>
                <TH>{t.colContact}</TH>
                <TH>{t.colWaste}</TH>
                <TH>{t.colDate}</TH>
                <TH>{t.colStatus}</TH>
                <TH className="text-right">{strings.common.actions}</TH>
              </TR>
            </THead>
            <TBody>
              {(requests ?? []).length === 0 && (
                <TR>
                  <TD colSpan={7} className="text-center text-gray-400">
                    {t.adminEmpty}
                  </TD>
                </TR>
              )}
              {(requests ?? []).map((r) => (
                <TR key={r.id}>
                  <TD>
                    <span className="font-medium text-gray-900">{r.companyName}</span>
                    <span className="block text-xs text-gray-400">{r.cui}</span>
                    {r.workPointAddress && (
                      <span className="block max-w-xs truncate text-xs text-gray-400">
                        {r.workPointAddress}
                      </span>
                    )}
                  </TD>
                  <TD>{typeLabels[r.companyType]}</TD>
                  <TD>
                    <span className="block text-gray-700">{r.contactEmail}</span>
                    {r.contactName && (
                      <span className="block text-xs text-gray-400">{r.contactName}</span>
                    )}
                  </TD>
                  <TD className="max-w-xs">
                    <span className="block truncate text-gray-700">{r.wasteCodesText || "—"}</span>
                    {(r.operationCodes ?? []).length > 0 && (
                      <span className="block text-xs text-gray-400">
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
        )}
      </div>
    </section>
  );
}
