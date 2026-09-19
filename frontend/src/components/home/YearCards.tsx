import { Link } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import type { DashboardData } from "@/hooks/useDashboardData";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { Card, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { emptyMonths, monthlyKg, topCodes } from "@/lib/home";
import { formatKg } from "@/lib/units";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

const t = strings.dashboard;

function HeaderLink({ to, children }: { to: string; children: string }) {
  return (
    <Link to={to} className="flex items-center gap-1 text-sm font-medium text-brand-700 hover:underline">
      {children}
      <ChevronRight className="h-4 w-4" aria-hidden />
    </Link>
  );
}

/**
 * Anul pe luni: douăsprezece coloane de kilograme generate. Luna curentă e mai deschisă (încă în
 * lucru), cele viitoare sunt o linie, iar o lună trecută fără nimic între prima cu date și azi iese
 * galben — o întrebare, nu o greșeală (`emptyMonths`).
 */
export function YearMonthsCard({ d }: { d: DashboardData }) {
  const kg = monthlyKg(d.evidences);
  const gaps = emptyMonths(kg, d.month);
  const total = kg.reduce((a, b) => a + b, 0);
  const max = Math.max(...kg, 1);

  return (
    <Card data-testid="home-year" className="flex flex-col">
      <CardHeader
        title={t.yearTitle.replace("{year}", String(d.year))}
        action={
          <span className="font-mono text-sm text-content">
            {d.loadingEvidences ? "" : d.failedEvidences ? "?" : `${formatKg(total)} kg`}
          </span>
        }
        className="border-b-2 border-content pb-2.5"
      />
      {d.loadingEvidences ? (
        <Skeleton className="mt-4 h-40 w-full" />
      ) : d.failedEvidences ? (
        <p className="mt-4 text-sm text-content-subtle">{t.statLoadError}</p>
      ) : (
        <>
          <div className="mt-4 flex h-40 items-end gap-1.5 border-b border-line-strong sm:gap-2.5">
            {kg.map((v, i) => {
              const month = i + 1;
              const future = month > d.month;
              const gap = gaps.includes(month);
              const current = month === d.month;
              return (
                <div key={month} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1">
                  <span
                    className={cn(
                      "hidden font-mono text-[10px] leading-none sm:block",
                      gap ? "font-semibold text-state-warn-text" : "text-content-muted"
                    )}
                  >
                    {future ? "" : formatKg(Math.round(v))}
                  </span>
                  <div
                    data-month={month}
                    data-gap={gap || undefined}
                    className={cn(
                      "w-full",
                      future ? "bg-line" : gap ? "bg-state-warn" : current ? "bg-brand-300" : "bg-brand"
                    )}
                    style={{ height: future ? 2 : gap ? 6 : `${Math.max((v / max) * 88, v > 0 ? 3 : 1)}%` }}
                  />
                </div>
              );
            })}
          </div>
          <div className="mt-1.5 flex gap-1.5 font-mono text-[10.5px] text-content-muted sm:gap-2.5">
            {strings.months.map((m, i) => (
              <span
                key={m}
                className={cn(
                  "min-w-0 flex-1 text-center",
                  gaps.includes(i + 1) && "font-semibold text-state-warn-text",
                  i + 1 === d.month && "font-semibold text-content"
                )}
              >
                {m[0]}
              </span>
            ))}
          </div>
          <p className="mt-3 text-xs text-content-muted">
            {total === 0
              ? t.yearEmpty.replace("{year}", String(d.year))
              : gaps.length > 0
                ? t.yearGap.replace("{month}", strings.months[gaps[0] - 1])
                : t.yearHint}
          </p>
        </>
      )}
    </Card>
  );
}

/** Deșeurile anului, după cantitate: pubela, codul, numele și o bară față de cel mai mare. */
export function WasteCodesCard({ d }: { d: DashboardData }) {
  /**
   * Cinci rânduri, dar cardul spune și ce a rămas pe dinafară: fără „și încă N", lista aduna
   * 18.449 kg sub un grafic al anului care arăta 18.699 și părea că una din cele două greșește.
   * Rândul stă în afara listei, ca numărătoarea de coduri să rămână cea cerută (proba 39).
   */
  const all = topCodes(d.evidences, Number.POSITIVE_INFINITY);
  const codes = all.slice(0, 5);
  const rest = all.slice(5);
  const restKg = rest.reduce((sum, c) => sum + c.kg, 0);
  const max = codes[0]?.kg ?? 1;
  return (
    <Card data-testid="home-codes">
      <CardHeader
        title={t.codesTitle}
        action={<HeaderLink to={`/generare?tab=total&luna=${d.year}`}>{t.codesAll}</HeaderLink>}
        className="border-b-2 border-content pb-2.5"
      />
      {d.loadingEvidences ? (
        <Skeleton className="mt-4 h-32 w-full" />
      ) : d.failedEvidences ? (
        <p className="mt-4 text-sm text-content-subtle">{t.statLoadError}</p>
      ) : codes.length === 0 ? (
        <p className="mt-4 text-sm text-content-subtle">{t.codesEmpty.replace("{year}", String(d.year))}</p>
      ) : (
        <>
          <ul className="mt-1">
            {codes.map((c) => (
              <li key={c.code} className="flex items-center gap-3 border-t border-line py-2.5 text-sm first:border-t-0">
                <BinSwatch code={c.code} hazardous={c.hazardous} />
                <span className="w-[4.75rem] shrink-0 font-mono text-content">{c.code}</span>
                <span className="min-w-0 flex-1 truncate text-content">{c.name}</span>
                <span className="hidden h-1.5 w-24 shrink-0 bg-surface-muted sm:block" aria-hidden>
                  <span className="block h-1.5 bg-content" style={{ width: `${Math.max((c.kg / max) * 100, 2)}%` }} />
                </span>
                <span className="w-20 shrink-0 text-right font-mono text-content">{formatKg(Math.round(c.kg))} kg</span>
              </li>
            ))}
          </ul>
          {rest.length > 0 && (
            <p className="border-t border-line pt-2.5 text-sm text-content-subtle">
              {t.codesMore
                .replace("{n}", String(rest.length))
                .replace("{kg}", formatKg(Math.round(restKg)))}
            </p>
          )}
        </>
      )}
    </Card>
  );
}
