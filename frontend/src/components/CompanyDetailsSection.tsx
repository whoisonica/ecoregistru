import { Link } from "react-router-dom";
import { Building2, ExternalLink } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { strings } from "@/lib/strings";
import { formatDate, withCount } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Card, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

const t = strings.settings.company;
// Etichetele rubricilor sunt scrise o dată, în ecranul unde se **editează** (Clienți). Aici se
// citesc aceleași rubrici, deci se citesc și aceleași etichete: două nume pentru „Nr. Registrul
// Comerțului" ar fi două nume pentru același lucru.
const c = strings.clients;
const p = strings.companyProfile;
const e = strings.enums;

/** O rubrică și răspunsul ei. Necompletat se spune, nu se ascunde: golul e el însuși un răspuns. */
function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs text-content-subtle">{label}</dt>
      <dd className="mt-0.5 break-words text-sm text-content">
        {value === null || value === undefined || value === "" ? (
          <span className="text-content-subtle">{t.unset}</span>
        ) : (
          value
        )}
      </dd>
    </div>
  );
}

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="text-xs font-semibold uppercase tracking-wide text-content-muted">{title}</h3>
      <dl className="mt-2 grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2 lg:grid-cols-3">
        {children}
      </dl>
    </div>
  );
}

/**
 * Datele propriei firme, în citire.
 *
 * <p>Erau nicăieri. CAEN-ul, autorizația de mediu, persoana desemnată, seria Anexei 3 — toate se
 * tipăresc pe documentele oficiale ale firmei, și toate se editau **exclusiv** din „Clienți", care
 * e ecran de `PLATFORM_ADMIN`. Un ADMIN de firmă intra în „Setări" — singurul loc unde s-ar fi
 * uitat — și găsea puncte de lucru, secții și șoferi. Întrebarea „ce autorizație are firma mea în
 * aplicație?" n-avea unde primi răspuns, deci se punea nouă, pe mail.
 *
 * <p>Numai citire, dinadins: cine poate schimba rubricile astea rămâne cine era. Ce se schimbă e
 * că se **văd** — inclusiv golurile, fiindcă un CAEN necompletat se tipărește gol pe declarația
 * anuală, iar asta se află mai bine aici decât din documentul depus.
 */
export function CompanyDetailsSection() {
  const { user } = useAuth();
  const { data: company, isLoading, isError } = useCurrentCompany();
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";

  if (isError) {
    return (
      <section id="datele-firmei" className="mt-8 scroll-mt-20">
        <p className="text-sm text-red-600">{t.loadError}</p>
      </section>
    );
  }

  return (
    <section id="datele-firmei" className="mt-8 scroll-mt-20">
      <Card>
        <CardHeader
          title={
            <span className="flex items-center gap-2">
              <Building2 className="h-4 w-4 text-content-subtle" aria-hidden />
              {t.title}
            </span>
          }
          description={t.subtitle}
          action={
            isPlatformAdmin && (
              <Link
                to="/clienti"
                className="inline-flex items-center gap-1 text-sm font-medium text-brand hover:underline"
              >
                {t.editInClients}
                <ExternalLink className="h-3.5 w-3.5" aria-hidden />
              </Link>
            )
          }
        />

        {isLoading || !company ? (
          <div className="mt-4 space-y-2">
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        ) : (
          <div className="mt-5 space-y-6">
            <Group title={c.groupIdentity}>
              <Field label={c.name} value={company.name} />
              <Field label={c.cui} value={company.cui} />
              <Field label={t.tradeRegisterNumber} value={company.tradeRegisterNumber} />
              <Field label={c.type} value={e.companyType[company.type]} />
              <Field label={c.caenCode} value={company.caenCode} />
              <Field label={c.address} value={company.address} />
            </Group>

            <Group title={t.groupAuthorization}>
              <Field label={c.environmentalAuthNumber} value={company.environmentalAuthNumber} />
              <Field
                label={c.environmentalAuthExpiry}
                value={
                  company.environmentalAuthExpiry ? (
                    <span className="flex flex-wrap items-center gap-2">
                      {formatDate(company.environmentalAuthExpiry)}
                      {expired(company.environmentalAuthExpiry) && (
                        <Badge variant="danger">{t.expired}</Badge>
                      )}
                    </span>
                  ) : null
                }
              />
              <Field
                label={p.operationCodes}
                value={
                  company.authorizedOperationCodes?.length
                    ? company.authorizedOperationCodes.join(", ")
                    : p.empty
                }
              />
              <Field
                label={p.wasteCodes}
                value={
                  company.authorizedWasteCodes?.length
                    ? withCount(
                        t.wasteCodesCount,
                        company.authorizedWasteCodes.length,
                        "cod",
                        "coduri"
                      )
                    : p.empty
                }
              />
              <Field label={p.transportMeans} value={company.transportMeans} />
              <Field
                label={p.transportLicenseNumber}
                value={
                  company.transportLicenseNumber
                    ? company.transportLicenseExpiry
                      ? `${company.transportLicenseNumber} · ${t.until} ${formatDate(company.transportLicenseExpiry)}`
                      : company.transportLicenseNumber
                    : null
                }
              />
            </Group>

            <Group title={c.groupWasteManager}>
              <Field label={c.wasteManagerName} value={company.wasteManagerName} />
              <Field label={c.wasteManagerRole} value={company.wasteManagerRole} />
              <Field
                label={t.wasteManagerExternal}
                value={
                  company.wasteManagerExternal == null
                    ? null
                    : company.wasteManagerExternal
                      ? c.wasteManagerExternalYes
                      : c.wasteManagerExternalNo
                }
              />
              <Field label={c.wasteManagerTraining} value={company.wasteManagerTraining} />
            </Group>

            {/* O singură rubrică, dar propriul ei grup: nu e o preferință de formular, e o
                obligație a firmei, și din ea pleacă (sau nu) alerta de 30 aprilie. */}
            <Group title={c.groupObligations}>
              <Field
                label={c.constructionPermitHolder}
                value={
                  company.constructionPermitHolder == null
                    ? null
                    : company.constructionPermitHolder
                      ? c.constructionPermitHolderYes
                      : c.constructionPermitHolderNo
                }
              />
            </Group>

            <Group title={c.groupReporting}>
              <Field label={c.anexa3Series} value={company.anexa3Series} />
              <Field
                label={c.anexa3Unit}
                value={company.anexa3Unit ? e.unit[company.anexa3Unit] : c.anexa3UnitAsRecorded}
              />
              <Field label={t.afm} value={company.afmObligation ? c.afmYes : c.afmNo} />
              <Field
                label={c.afmContributions}
                value={
                  company.afmContributions?.length
                    ? company.afmContributions.map((a) => e.afmContribution[a]).join(" · ")
                    : p.empty
                }
              />
              <Field
                label={p.marketRoles}
                value={
                  company.marketRoles?.length
                    ? company.marketRoles.map((r) => e.marketRole[r]).join(" · ")
                    : p.empty
                }
              />
              <Field
                label={strings.packagingOperatorRole.label}
                value={
                  company.packagingOperatorRole
                    ? strings.packagingOperatorRole[company.packagingOperatorRole]
                    : p.empty
                }
              />
            </Group>

            <Group title={c.groupContact}>
              <Field label={c.contactName} value={company.contactName} />
              <Field label={c.contactRole} value={company.contactRole} />
              <Field label={c.contactEmail} value={company.contactEmail} />
              <Field label={c.contactPhone} value={company.contactPhone} />
            </Group>

            {!isPlatformAdmin && (
              <p className="rounded-md border border-line bg-surface-muted px-3 py-2 text-xs text-content-muted">
                {t.readOnlyNote}
              </p>
            )}
          </div>
        )}
      </Card>
    </section>
  );
}

/** Ziua de azi trece drept validă: autorizația expiră la sfârșitul zilei înscrise pe ea. */
function expired(iso: string): boolean {
  return iso < new Date().toISOString().slice(0, 10);
}
