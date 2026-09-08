import { useState, type ReactNode } from "react";
import { ArrowUpRight, Check, Eye, Inbox, X } from "lucide-react";
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

const operationLabels = strings.enums.wasteOperationCode;

function StatusBadge({ request }: { request: AccountRequest }) {
  if (request.status === "APPROVED") return <Badge variant="success">{t.status.APPROVED}</Badge>;
  if (request.status === "REJECTED") return <Badge variant="muted">{t.status.REJECTED}</Badge>;
  return <Badge variant="warning">{t.status.NEW}</Badge>;
}

/**
 * O secțiune din cererea citită, cu rubricile ei.
 *
 * <p>Rubricile goale **se arată**, nu se sar: cine creează firma trebuie să vadă că adresa lipsește,
 * nu să caute printre cele completate ca să deducă asta. Secțiunea întreagă dispare doar dacă n-are
 * niciun răspuns — atunci absența e informația, și o spune un rând, nu opt liniuțe.
 */
function AnswerSection({
  title,
  rows,
}: {
  title: string;
  rows: { label: string; value: ReactNode }[];
}) {
  const answered = rows.filter((r) => r.value != null && r.value !== "");
  return (
    <section>
      <h3 className="border-b border-line pb-1.5 text-xs font-semibold uppercase tracking-wide text-content-muted">
        {title}
      </h3>
      {answered.length === 0 ? (
        <p className="mt-2 text-sm text-content-subtle">{t.viewNoAnswers}</p>
      ) : (
        <dl className="mt-2 grid grid-cols-1 gap-x-6 gap-y-2 sm:grid-cols-2">
          {rows.map((r) => (
            <div key={r.label}>
              <dt className="text-xs text-content-muted">{r.label}</dt>
              <dd className="whitespace-pre-wrap break-words text-sm text-content-strong">
                {r.value == null || r.value === "" ? (
                  <span className="text-content-subtle">{t.viewEmptyValue}</span>
                ) : (
                  r.value
                )}
              </dd>
            </div>
          ))}
        </dl>
      )}
    </section>
  );
}

/**
 * The inbox behind the closed register: the forms clients submitted, and the one action that turns
 * one into an account.
 *
 * <p>Approving creates the company with the profile the client answered — it does not invite
 * anyone. Creating an account and giving a person access stay two deliberate acts, so support
 * invites the user from the list above once the company exists.
 */
export function AccountRequestsSection({
  enabled,
  onOpenCompany,
}: {
  enabled: boolean;
  /**
   * Deschide firma creată dintr-o cerere aprobată. Lipsa ei ascunde acțiunea, deci secțiunea
   * rămâne folosibilă oriunde ar fi pusă — legătura e a paginii care ține și lista de firme.
   */
  onOpenCompany?: (companyId: string) => void;
}) {
  const { data: requests, isLoading, isError } = useAccountRequests(enabled);
  const approveMut = useApproveAccountRequest();
  const rejectMut = useRejectAccountRequest();
  const { notify } = useToast();

  const busy = approveMut.isPending || rejectMut.isPending;
  const [confirm, confirmDialog] = useConfirm();
  // Cererea pe cale de a fi respinsă, cu motivul care se scrie. `null` = dialogul e închis.
  const [rejecting, setRejecting] = useState<AccountRequest | null>(null);
  const [reason, setReason] = useState("");
  /** Cererea citită întreagă. `null` = dialogul e închis. */
  const [viewing, setViewing] = useState<AccountRequest | null>(null);

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
                      <div className="flex items-center justify-end gap-1">
                        {/* `whitespace-nowrap` pe toate trei, şi nu e cosmetic: al treilea buton
                            din coloană le rupea pe fiecare pe câte două rânduri („Vezi / cererea",
                            „Creează / contul"), iar rândurile creşteau în înălţime. Coloana e
                            `sticky="right"`, deci poate fi mai lată fără să iasă din îndemână.
                            (`Tooltip` nu e o variantă aici: îşi randează propriul `<button>`, deci
                            ar fi buton în buton.) */}
                        <Button
                          variant="ghost"
                          size="sm"
                          className="whitespace-nowrap"
                          onClick={() => setViewing(r)}
                        >
                          <Eye className="mr-1 h-3.5 w-3.5 shrink-0" />
                          {t.view}
                        </Button>
                        {r.status === "NEW" && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="whitespace-nowrap"
                              disabled={busy}
                              onClick={() => handleApprove(r)}
                            >
                              <Check className="mr-1 h-3.5 w-3.5 shrink-0" />
                              {t.approve}
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="whitespace-nowrap text-red-600 hover:bg-red-50"
                              disabled={busy}
                              onClick={() => openReject(r)}
                            >
                              <X className="mr-1 h-3.5 w-3.5 shrink-0" />
                              {t.reject}
                            </Button>
                          </>
                        )}
                        {r.status === "APPROVED" && r.createdCompanyId && onOpenCompany && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="whitespace-nowrap"
                            onClick={() => onOpenCompany(r.createdCompanyId!)}
                          >
                            <ArrowUpRight className="mr-1 h-3.5 w-3.5 shrink-0" />
                            {t.openCompany}
                          </Button>
                        )}
                      </div>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
            <TablePagination view={view} />
          </>
        )}
      </div>

      {/* Cererea, întreagă. Ordinea e cea din formularul pe care l-a completat clientul: cine
          citește aici și cine a scris acolo trec prin aceleași secțiuni, în aceeași ordine. */}
      <Dialog
        open={viewing !== null}
        onClose={() => setViewing(null)}
        title={t.viewTitle}
        description={
          viewing
            ? `${viewing.companyName}${viewing.cui ? ` — ${viewing.cui}` : ""}`
            : undefined
        }
        size="xl"
        footer={
          <>
            {viewing?.status === "APPROVED" && viewing.createdCompanyId && onOpenCompany && (
              <Button
                variant="outline"
                onClick={() => {
                  const companyId = viewing.createdCompanyId!;
                  setViewing(null);
                  onOpenCompany(companyId);
                }}
              >
                <ArrowUpRight className="mr-1 h-4 w-4" />
                {t.openCompany}
              </Button>
            )}
            <Button onClick={() => setViewing(null)}>{strings.common.close}</Button>
          </>
        }
      >
        {viewing && (
          <div className="space-y-5">
            <AnswerSection
              title={t.sectionCompany}
              rows={[
                { label: t.companyName, value: viewing.companyName },
                { label: t.cui, value: viewing.cui },
                { label: t.companyType, value: typeLabels[viewing.companyType] },
                { label: t.caenCode, value: viewing.caenCode },
                { label: t.companyAddress, value: viewing.companyAddress },
              ]}
            />
            <AnswerSection
              title={t.sectionWorkPoint}
              rows={[
                { label: t.workPointName, value: viewing.workPointName },
                { label: t.workPointAddress, value: viewing.workPointAddress },
              ]}
            />
            <AnswerSection
              title={t.sectionContact}
              rows={[
                { label: t.contactName, value: viewing.contactName },
                { label: t.contactRole, value: viewing.contactRole },
                { label: t.contactEmail, value: viewing.contactEmail },
                { label: t.contactPhone, value: viewing.contactPhone },
              ]}
            />
            <AnswerSection
              title={t.sectionAuthorization}
              rows={[
                { label: t.environmentalAuthNumber, value: viewing.environmentalAuthNumber },
                {
                  label: t.environmentalAuthExpiry,
                  value: formatDate(viewing.environmentalAuthExpiry),
                },
              ]}
            />
            {/* Transportul se cere doar unui cont care poate prelua de la terți — dar dacă s-a
                completat, se citește oricare ar fi tipul de azi: răspunsul e al clientului. */}
            <AnswerSection
              title={t.sectionTransport}
              rows={[
                { label: t.transportMeans, value: viewing.transportMeans },
                { label: t.transportLicenseNumber, value: viewing.transportLicenseNumber },
                {
                  label: t.transportLicenseExpiry,
                  value: formatDate(viewing.transportLicenseExpiry),
                },
              ]}
            />
            <AnswerSection
              title={t.sectionMarketRole}
              rows={[
                {
                  label: t.marketRoles,
                  value: (viewing.marketRoles ?? [])
                    .map((m) => marketRoleLabels[m])
                    .join(", "),
                },
              ]}
            />
            <AnswerSection
              title={t.sectionWaste}
              rows={[
                { label: t.wasteCodesText, value: viewing.wasteCodesText },
                {
                  label: t.operationCodes,
                  value: (viewing.operationCodes ?? [])
                    .map((c) => operationLabels[c])
                    .join(" · "),
                },
                { label: t.notes, value: viewing.notes },
              ]}
            />
            <p className="border-t border-line pt-3 text-xs text-content-muted">
              {t.viewSubmittedAt} {formatDate(viewing.createdAt)}
              {viewing.handledAt && ` · ${t.viewHandledAt} ${formatDate(viewing.handledAt)}`}
            </p>
          </div>
        )}
      </Dialog>

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
