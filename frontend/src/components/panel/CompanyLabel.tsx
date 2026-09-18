import { useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Building2, ChevronDown, Search } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { companiesKey, useCompanies } from "@/hooks/useCompanies";
import { useConsultancyOverview } from "@/hooks/useConsultancies";
import { isMultiCompany } from "@/lib/roles";
import { strings } from "@/lib/strings";
import type { Company, ConsultancyOverviewRow } from "@/lib/types";
import { cn, countOf, fold, formatDate } from "@/lib/utils";
import { daysUntil } from "@/lib/deadlines";

const t = strings.panel;

/**
 * Firma, ca eticheta de pe cântar: numele, apoi `CUI · tip` în mono.
 *
 * <p>Pentru un cont de firmă e o etichetă. Pentru consultant și platformă e un buton care deschide
 * selectorul: căutare, apoi firmele grupate în „Cer atenție" (cu motivul: termen depășit, linii
 * fără cod R/D, autorizație pe terminate) și „În regulă · N". Motivele vin din panoul cabinetului
 * (`useConsultancyOverview`), deci numai consultantul le are; platforma vede lista simplă.
 *
 * <p>Înlocuiește `CompanyBlock` (un `<select>` nativ) din 15.09.2026.
 */
export function CompanyLabel({
  company,
  collapsed = false,
}: {
  company: Company | undefined;
  collapsed?: boolean;
}) {
  const { user, tenantId, switchTenant } = useAuth();
  const multiCompany = isMultiCompany(user?.role);
  const queryClient = useQueryClient();
  const { data: companies, isError } = useCompanies(multiCompany);
  const { data: overview } = useConsultancyOverview(user?.role === "CONSULTANT");
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const ref = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  /**
   * P2.13 — firma aleasă care nu mai e în listă se uită. Firma stă în browser între sesiuni;
   * pentru un consultant lipsește exact când firma a fost mutată în alt cabinet.
   */
  useEffect(() => {
    if (!multiCompany || !companies || !tenantId) return;
    if (!companies.some((c) => c.id === tenantId)) {
      queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== companiesKey[0] });
      switchTenant(null);
    }
  }, [multiCompany, companies, tenantId, switchTenant, queryClient]);

  useEffect(() => {
    if (!open) return;
    setQuery("");
    requestAnimationFrame(() => inputRef.current?.focus());
    function onDown(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") {
        e.preventDefault();
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const attention = useMemo(() => {
    const byId = new Map<string, ConsultancyOverviewRow>();
    for (const row of overview ?? []) byId.set(row.companyId, row);
    return byId;
  }, [overview]);

  const filtered = useMemo(() => {
    const q = fold(query.trim());
    const list = (companies ?? []).filter((c) => !q || fold(c.name).includes(q) || fold(c.cui ?? "").includes(q));
    const needs: { company: Company; reason: string; tone: "bad" | "warn" }[] = [];
    const fine: Company[] = [];
    for (const c of list) {
      const r = attention.get(c.id);
      const reason = r ? reasonFor(r) : null;
      if (reason) needs.push({ company: c, ...reason });
      else fine.push(c);
    }
    return { needs, fine };
  }, [companies, query, attention]);

  function choose(id: string) {
    // Schimbarea firmei nu amestecă datele între firme: se aruncă tot ce e în cache, în afară de
    // lista de firme, care e aceeași pentru toate și se citește de aici.
    queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== companiesKey[0] });
    switchTenant(id || null);
    setOpen(false);
  }

  const name = multiCompany ? company?.name ?? (tenantId ? "…" : t.companyNone) : user?.tenantName ?? t.companyNone;
  const meta = company ? `${company.cui} · ${strings.enums.companyType[company.type]}` : multiCompany ? t.companyPick : "";

  if (collapsed) {
    return (
      <div
        className="grid h-9 w-full place-items-center rounded-md border border-panel-line bg-panel-hover font-mono text-sm font-medium text-white"
        aria-label={name}
      >
        {name.charAt(0).toUpperCase()}
      </div>
    );
  }

  const body = (
    <>
      <div className="min-w-0 flex-1">
        <span className="block truncate text-sm font-semibold text-white">{name}</span>
        <span className="block truncate font-mono text-[0.6875rem] text-panel-mid">{meta}</span>
      </div>
      {multiCompany && <ChevronDown className="h-4 w-4 shrink-0 text-panel-mid" aria-hidden />}
    </>
  );

  if (!multiCompany) {
    return (
      <div className="flex w-full items-center gap-2 rounded-md border border-panel-line bg-lcd px-2.5 py-2" data-testid="company-label">
        {body}
      </div>
    );
  }

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={t.companyPick}
        data-testid="company-label"
        className="flex w-full items-center gap-2 rounded-md border border-panel-line bg-lcd px-2.5 py-2 text-left transition-colors hover:border-panel-key focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0"
      >
        {body}
      </button>
      {isError && <p className="mt-1 text-xs text-lcd-bad">{strings.header.loadCompaniesError}</p>}
      {open && (
        <div
          role="dialog"
          aria-label={t.companyPick}
          className="absolute left-0 top-full z-40 mt-1 w-[300px] animate-slide-up rounded-lg border border-line-strong bg-surface p-2 text-sm text-content shadow-popover"
        >
          <div className="relative">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-content-subtle" aria-hidden />
            <input
              ref={inputRef}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t.companySearch}
              aria-label={t.companySearch}
              className="h-9 w-full rounded-md border border-line-strong bg-surface pl-8 pr-2 text-sm placeholder:text-content-subtle focus-visible:border-brand focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/25"
            />
          </div>
          <div role="listbox" className="mt-1 max-h-80 overflow-y-auto">
            {filtered.needs.length > 0 && (
              <>
                <div className="eyebrow px-1.5 pb-0.5 pt-2">{t.companyNeedsAttention}</div>
                {filtered.needs.map(({ company: c, reason, tone }) => (
                  <CompanyRow key={c.id} company={c} active={c.id === tenantId} onPick={choose}>
                    <span className={cn("font-mono text-[0.6875rem]", tone === "bad" ? "text-state-bad-text" : "text-state-warn-text")}>
                      {reason}
                    </span>
                  </CompanyRow>
                ))}
              </>
            )}
            <div className="eyebrow px-1.5 pb-0.5 pt-2">
              {t.companyFine} · {filtered.fine.length}
            </div>
            {filtered.fine.map((c) => (
              <CompanyRow key={c.id} company={c} active={c.id === tenantId} onPick={choose} />
            ))}
            {filtered.needs.length === 0 && filtered.fine.length === 0 && (
              <p className="px-1.5 py-3 text-xs text-content-subtle">{strings.common.noResults}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function CompanyRow({
  company,
  active,
  onPick,
  children,
}: {
  company: Company;
  active: boolean;
  onPick: (id: string) => void;
  children?: React.ReactNode;
}) {
  return (
    <button
      type="button"
      role="option"
      aria-selected={active}
      onClick={() => onPick(company.id)}
      className={cn(
        "flex w-full items-center gap-2 rounded px-1.5 py-1.5 text-left transition-colors hover:bg-surface-muted",
        active && "bg-brand-50"
      )}
    >
      <Building2 className="h-3.5 w-3.5 shrink-0 text-content-subtle" aria-hidden />
      <span className="min-w-0 flex-1 truncate font-medium">{company.name}</span>
      {children}
    </button>
  );
}

/** Motivul pentru care o firmă „cere atenție", cel mai scump întâi — aceeași ordine ca pe Acasă. */
function reasonFor(r: ConsultancyOverviewRow): { reason: string; tone: "bad" | "warn" } | null {
  const c = strings.consultancyOverview;
  if (r.overdueDeadlines > 0) {
    return {
      reason:
        r.overdueDeadlines === 1
          ? c.overdueOne
          : c.overdue.replace("{count}", countOf(r.overdueDeadlines, "termen", "termene")),
      tone: "bad",
    };
  }
  if (r.linesWithoutOperationCode > 0) {
    return {
      reason: c.withoutCode.replace("{count}", countOf(r.linesWithoutOperationCode, "linie", "linii")),
      tone: "bad",
    };
  }
  if (r.nextDeadline && daysUntil(r.nextDeadline.dueDate) <= 30) {
    return { reason: c.next.replace("{date}", formatDate(r.nextDeadline.dueDate)), tone: "warn" };
  }
  if (r.partnersExpiring > 0) {
    return {
      reason: c.partnersExpiring.replace("{count}", countOf(r.partnersExpiring, "partener", "parteneri")),
      tone: "warn",
    };
  }
  return null;
}
