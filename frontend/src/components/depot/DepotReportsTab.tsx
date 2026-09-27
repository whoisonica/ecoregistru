import { useMemo, useState } from "react";
import { FileSpreadsheet, FileText } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { useWasteArticles } from "@/hooks/useWasteArticles";
import { usePartners } from "@/hooks/usePartners";
import { downloadDepotReport, type DepotReportSlug } from "@/hooks/useDepotReports";
import { canManage } from "@/lib/roles";
import { strings } from "@/lib/strings";
import { apiBlobErrorMessage } from "@/lib/api";
import { periodOf, periodValid, type Shortcut } from "@/lib/depotReportPeriod";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";

const t = strings.depotReports;

/** Cine vede cardul — aceleași praguri ca pe server (`DepotReportKind.Access`). */
type Access = "ANY" | "APPROVER" | "MONEY";

interface ReportCard {
  slug: DepotReportSlug;
  pdf: boolean;
  access: Access;
  /** Banii sunt ai firmei: depozitul ales sus nu-i taie. */
  wholeCompany?: boolean;
}

const GROUPS: { title: string; cards: ReportCard[] }[] = [
  {
    title: t.groupRegisters,
    cards: [
      { slug: "registru", pdf: false, access: "ANY" },
      { slug: "jurnal-cantar", pdf: false, access: "ANY" },
      { slug: "documente", pdf: false, access: "ANY" },
      { slug: "anulate", pdf: false, access: "ANY" },
    ],
  },
  {
    title: t.groupStock,
    cards: [
      { slug: "fisa-stoc", pdf: true, access: "ANY" },
      { slug: "transferuri", pdf: false, access: "ANY" },
    ],
  },
  {
    title: t.groupMoney,
    cards: [
      { slug: "afm", pdf: true, access: "MONEY", wholeCompany: true },
      { slug: "impozit", pdf: true, access: "MONEY", wholeCompany: true },
      { slug: "numerar", pdf: false, access: "MONEY", wholeCompany: true },
    ],
  },
  {
    title: t.groupPartners,
    cards: [
      { slug: "persoane-fizice", pdf: true, access: "APPROVER" },
      { slug: "partener", pdf: false, access: "ANY" },
    ],
  },
];

/**
 * D4.7 — Cântar → Rapoarte: perioada și depozitul o dată, sus, apoi unsprezece rapoarte în patru grupe, fiecare cu
 * tastele lui de fișier. Un raport pe care rolul nu-l poate scoate nu apare (decizia proprietarului, 27.09.2026).
 */
export function DepotReportsTab() {
  const { user } = useAuth();
  const { data: company } = useCurrentCompany();
  const approver = canManage(user?.role);
  const money = approver && Boolean(company?.pricesVisible);

  const workPoints = useWorkPoints();
  const depots = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  const articles = useWasteArticles();
  const partners = usePartners();
  const activeArticles = useMemo(() => (articles.data ?? []).filter((a) => a.active), [articles.data]);
  const firms = useMemo(
    () => [...(partners.data ?? [])].sort((a, b) => a.name.localeCompare(b.name, "ro")),
    [partners.data]
  );

  const [period, setPeriod] = useState(() => periodOf("THIS_MONTH"));
  const [depot, setDepot] = useState("");
  const [article, setArticle] = useState("");
  const [partner, setPartner] = useState("");
  const [busy, setBusy] = useState<string | null>(null);
  const { notify } = useToast();

  const shortcut = (["LAST_MONTH", "THIS_MONTH", "QUARTER", "YEAR"] as Shortcut[]).find((s) => {
    const p = periodOf(s);
    return p.from === period.from && p.to === period.to;
  });
  const valid = periodValid(period.from, period.to);

  function visible(card: ReportCard) {
    return card.access === "ANY" || (card.access === "APPROVER" ? approver : money);
  }

  async function download(card: ReportCard, format: "xlsx" | "pdf") {
    const key = `${card.slug}.${format}`;
    setBusy(key);
    try {
      await downloadDepotReport(
        card.slug,
        {
          ...period,
          workPointId: card.wholeCompany ? undefined : depot || undefined,
          articleId: card.slug === "fisa-stoc" ? article || undefined : undefined,
          partnerId: card.slug === "partener" ? partner : undefined,
        },
        format
      );
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.downloadError), "error");
    } finally {
      setBusy(null);
    }
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-end gap-3 border border-line bg-surface-sunken px-4 py-3">
        <div>
          <Label htmlFor="dr-from">{t.from}</Label>
          <DateInput
            id="dr-from"
            value={period.from}
            onChange={(e) => setPeriod((p) => ({ ...p, from: e.target.value }))}
          />
        </div>
        <div>
          <Label htmlFor="dr-to">{t.to}</Label>
          <DateInput id="dr-to" value={period.to} onChange={(e) => setPeriod((p) => ({ ...p, to: e.target.value }))} />
        </div>
        <div>
          <Label id="dr-shortcuts">{t.shortcuts}</Label>
          <PillGroup
            name="dr-shortcut"
            aria-labelledby="dr-shortcuts"
            options={[
              { value: "LAST_MONTH", label: t.lastMonth },
              { value: "THIS_MONTH", label: t.thisMonth },
              { value: "QUARTER", label: t.quarter },
              { value: "YEAR", label: t.year },
            ]}
            selected={shortcut ? [shortcut] : []}
            onToggle={(value) => setPeriod(periodOf(value))}
          />
        </div>
        <div>
          <Label htmlFor="dr-depot">{t.depot}</Label>
          <Select id="dr-depot" value={depot} onChange={(e) => setDepot(e.target.value)}>
            <option value="">{t.allDepots}</option>
            {depots.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </Select>
        </div>
        {!valid && (
          <p role="alert" className="mb-2 text-sm text-state-bad-text">
            {t.periodTooLong}
          </p>
        )}
      </div>

      <div className="grid gap-x-8 gap-y-4 lg:grid-cols-2">
        {[GROUPS.slice(0, 2), GROUPS.slice(2)].map((column, c) => (
          <div key={c} className="space-y-4">
            {column.map((group) => {
              const cards = group.cards.filter(visible);
              if (cards.length === 0) return null;
              return (
                <section key={group.title} aria-label={group.title}>
                  <h2 className="mb-1 text-xs font-semibold uppercase tracking-wide text-content-muted">
                    {group.title}
                  </h2>
                  <ul className="divide-y divide-line border border-line">
                    {cards.map((card) => {
                      const text = t.reports[card.slug];
                      return (
                        <li
                          key={card.slug}
                          data-report={card.slug}
                          className="flex flex-wrap items-center gap-x-3 gap-y-2 px-3 py-2"
                        >
                          <div className="min-w-0 flex-1">
                            <p className="flex items-center gap-1 text-sm font-semibold text-content-strong">
                              {text.title}
                              <Tooltip content={text.basis}>
                                <span className="cursor-help font-mono text-content-subtle">?</span>
                              </Tooltip>
                            </p>
                            <p className="text-xs text-content-muted">
                              {card.wholeCompany && depot ? t.wholeCompany : text.hint}
                            </p>
                          </div>
                          {card.slug === "fisa-stoc" && (
                            <Select
                              aria-label={t.article}
                              className="w-44"
                              value={article}
                              onChange={(e) => setArticle(e.target.value)}
                            >
                              <option value="">{t.allArticles}</option>
                              {activeArticles.map((a) => (
                                <option key={a.id} value={a.id}>
                                  {a.name}
                                </option>
                              ))}
                            </Select>
                          )}
                          {card.slug === "partener" && (
                            <Select
                              aria-label={t.partner}
                              className="w-44"
                              value={partner}
                              onChange={(e) => setPartner(e.target.value)}
                            >
                              <option value="">{t.choosePartner}</option>
                              {firms.map((p) => (
                                <option key={p.id} value={p.id}>
                                  {p.name}
                                </option>
                              ))}
                            </Select>
                          )}
                          <div className="flex gap-2">
                            <Button
                              size="sm"
                              variant="outline"
                              aria-label={`${text.title} — Excel`}
                              disabled={!valid || (card.slug === "partener" && !partner)}
                              loading={busy === `${card.slug}.xlsx`}
                              onClick={() => download(card, "xlsx")}
                            >
                              <FileSpreadsheet className="mr-1.5 h-4 w-4" />
                              xlsx
                            </Button>
                            {card.pdf && (
                              <Button
                                size="sm"
                                variant="outline"
                                aria-label={`${text.title} — PDF`}
                                disabled={!valid}
                                loading={busy === `${card.slug}.pdf`}
                                onClick={() => download(card, "pdf")}
                              >
                                <FileText className="mr-1.5 h-4 w-4" />
                                PDF
                              </Button>
                            )}
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                </section>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}
