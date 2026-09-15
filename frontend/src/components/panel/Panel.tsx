import { NavLink, Link } from "react-router-dom";
import { ArrowDownToLine, ArrowUpFromLine, ChevronsLeft, ChevronsRight, Plus, Search, X } from "lucide-react";
import { BrandName } from "@/components/BrandName";
import { CompanyLabel } from "@/components/panel/CompanyLabel";
import { MonthDisplay } from "@/components/panel/MonthDisplay";
import { AccountMenu } from "@/components/panel/AccountMenu";
import type { Indicator } from "@/hooks/usePanelData";
import type { NavEntry, NavModel } from "@/lib/navItems";
import { SCREEN_PATH, type MovementScreen } from "@/lib/movementScreens";
import { strings } from "@/lib/strings";
import type { BillingAccess, Company } from "@/lib/types";
import { cn } from "@/lib/utils";

const t = strings.panel;

export interface PanelProps {
  nav: NavModel;
  company: Company | undefined;
  screens: MovementScreen[];
  canWrite: boolean;
  canManage: boolean;
  monthName: string;
  monthLabel: string;
  monthKg: number | null;
  monthLoading: boolean;
  monthFailed: boolean;
  nextAction: Parameters<typeof MonthDisplay>[0]["action"];
  nextActionLoading: boolean;
  indicatorFor: (screen: MovementScreen | undefined, to: string) => Indicator | null;
  billing: BillingAccess | undefined;
  collapsed: boolean;
  onToggleCollapsed: () => void;
  onOpenSearch: () => void;
  onCloseDrawer: () => void;
  email?: string;
  role?: string;
  onLogout: () => void;
}

/**
 * Panoul — bara din stânga, direcția „Cântar” (`todo-ui-cantar.md` §3).
 *
 * <p>De sus în jos: logo + tasta `[`; firma ca etichetă; afișajul lunii; tastele de adăugare
 * (ascunse fără drept de scriere); „Caută oriunde · Ctrl K"; meniul cu tastă și indicator pe fiecare
 * intrare; grupul Cabinet; jos, abonamentul (doar cine administrează) și contul.
 *
 * <p>Același component randează sertarul de pe telefon: acolo `collapsed` e mereu fals și butonul
 * `[` e înlocuit de „×".
 */
export function Panel({
  nav,
  company,
  screens,
  canWrite,
  canManage,
  monthName,
  monthLabel,
  monthKg,
  monthLoading,
  monthFailed,
  nextAction,
  nextActionLoading,
  indicatorFor,
  billing,
  collapsed,
  onToggleCollapsed,
  onOpenSearch,
  onCloseDrawer,
  email,
  role,
  onLogout,
}: PanelProps) {
  const hasGenerated = screens.includes("GENERATED");
  const hasCollector = screens.includes("IN");

  return (
    <div className={cn("flex h-full flex-col gap-2.5 bg-panel text-panel-text", collapsed ? "px-2 pb-2.5 pt-3" : "px-3 pb-3 pt-3.5")}>
      {/* Logo + tasta de strângere. Pe telefon (sertar) e „×". */}
      <div className={cn("flex items-center gap-2 text-white", collapsed ? "justify-center" : "px-1")}>
        {!collapsed && (
          <Link to="/" className="min-w-0 truncate text-base font-semibold text-white">
            <BrandName />
          </Link>
        )}
        <button
          type="button"
          onClick={onToggleCollapsed}
          aria-label={collapsed ? t.expand : t.collapse}
          aria-pressed={collapsed}
          className={cn(
            "hidden items-center gap-1 rounded text-panel-mid transition-colors hover:text-white focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0 lg:inline-flex",
            !collapsed && "ml-auto"
          )}
        >
          {collapsed ? <ChevronsRight className="h-4 w-4" aria-hidden /> : <ChevronsLeft className="h-4 w-4" aria-hidden />}
          {!collapsed && <kbd className="key key-dark">{t.collapseKey}</kbd>}
        </button>
        <button
          type="button"
          onClick={onCloseDrawer}
          aria-label={strings.common.closeNav}
          className="ml-auto rounded p-1 text-panel-mid hover:text-white lg:hidden"
        >
          <X className="h-5 w-5" aria-hidden />
        </button>
      </div>

      <CompanyLabel company={company} collapsed={collapsed} />

      {company && (
        <MonthDisplay
          monthName={monthName}
          label={monthLabel}
          kg={monthKg}
          loading={monthLoading}
          failed={monthFailed}
          action={nextAction}
          actionLoading={nextActionLoading}
          collapsed={collapsed}
        />
      )}

      {/* Tastele de adăugare, după tipul firmei (§5). Ascunse la cine nu poate scrie: o tastă care
          nu face nimic e mai rea decât una lipsă. */}
      {company && canWrite && (
        <div className="flex flex-col gap-1.5" data-testid="panel-actions">
          {hasGenerated && (
            <GoButton
              to={`${SCREEN_PATH.GENERATED}?nou=1`}
              icon={Plus}
              hotkey="N"
              tone="green"
              collapsed={collapsed}
              label={hasCollector ? t.addOwnWaste : t.addWaste}
            />
          )}
          {hasCollector && (
            <div className={cn("flex gap-1.5", collapsed && "flex-col")}>
              <GoButton to={`${SCREEN_PATH.IN}?nou=1`} icon={ArrowDownToLine} hotkey="I" tone="blue" collapsed={collapsed} label={t.addInbound} />
              <GoButton to={`${SCREEN_PATH.OUT}?nou=1`} icon={ArrowUpFromLine} hotkey="E" tone="dark" collapsed={collapsed} label={t.addOutbound} />
            </div>
          )}
        </div>
      )}

      <button
        type="button"
        onClick={onOpenSearch}
        aria-label={`${t.searchEverywhere} (${t.searchKey})`}
        className={cn(
          "flex items-center gap-2 rounded-md border border-panel-line text-[0.8125rem] text-panel-dim transition-colors hover:border-panel-key hover:text-panel-text focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
          collapsed ? "justify-center px-0 py-2" : "px-2.5 py-1.5"
        )}
      >
        <Search className="h-4 w-4 shrink-0" aria-hidden />
        {!collapsed && (
          <>
            <span className="flex-1 text-left">{t.searchEverywhere}</span>
            <kbd className="key key-dark hidden lg:inline-grid">{t.searchKey}</kbd>
          </>
        )}
      </button>

      <nav aria-label={strings.common.mainNav} className="-mx-1 flex-1 overflow-y-auto px-1">
        <ul className="flex flex-col gap-px">
          {nav.main.map((item) => (
            <NavRow key={item.to} item={item} indicator={indicatorFor(item.screen, item.to)} collapsed={collapsed} />
          ))}
        </ul>
        {nav.cabinet.length > 0 && (
          <>
            {!collapsed && (
              <div className="px-2 pb-0.5 pt-2.5 font-mono text-[0.625rem] uppercase tracking-[0.08em] text-panel-faint">
                {strings.nav.groupCabinet}
              </div>
            )}
            {collapsed && <div className="my-1.5 border-t border-panel-line" />}
            <ul className="flex flex-col gap-px">
              {nav.cabinet.map((item) => (
                <NavRow key={item.to} item={item} indicator={null} collapsed={collapsed} />
              ))}
            </ul>
          </>
        )}
      </nav>

      <div className="mt-auto flex flex-col gap-1.5">
        {canManage && <BillingRow billing={billing} collapsed={collapsed} />}
        <div className="border-t border-panel-line pt-1.5">
          <AccountMenu email={email} role={role} onLogout={onLogout} collapsed={collapsed} hasCollector={hasCollector} />
        </div>
      </div>
    </div>
  );
}

function GoButton({
  to,
  icon: Icon,
  hotkey,
  tone,
  label,
  collapsed,
}: {
  to: string;
  icon: typeof Plus;
  hotkey: string;
  tone: "green" | "blue" | "dark";
  label: string;
  collapsed: boolean;
}) {
  const toneClass = {
    green: "bg-brand text-white hover:bg-brand-700 [&_kbd]:border-brand-800 [&_kbd]:bg-brand-700 [&_kbd]:text-white",
    blue: "bg-inbound text-white hover:bg-inbound-key [&_kbd]:border-inbound-border [&_kbd]:bg-inbound-key [&_kbd]:text-white",
    dark: "bg-panel-active text-white ring-1 ring-inset ring-lcd-unit hover:bg-panel-key [&_kbd]:border-panel-key [&_kbd]:bg-panel [&_kbd]:text-panel-text",
  }[tone];
  return (
    <Link
      to={to}
      aria-label={collapsed ? `${label} (${hotkey})` : undefined}
      className={cn(
        "flex flex-1 items-center gap-2 rounded-md text-sm font-semibold transition-colors focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
        collapsed ? "justify-center px-0 py-2" : "px-2.5 py-2",
        toneClass
      )}
    >
      <Icon className="h-4 w-4 shrink-0" aria-hidden />
      {!collapsed && (
        <>
          <span className="min-w-0 flex-1 truncate">{label}</span>
          <kbd className="key hidden lg:inline-grid">{hotkey}</kbd>
        </>
      )}
    </Link>
  );
}

function NavRow({ item, indicator, collapsed }: { item: NavEntry; indicator: Indicator | null; collapsed: boolean }) {
  const dot = indicator && (
    <span
      className={cn(
        "ml-auto flex shrink-0 items-center gap-1.5 whitespace-nowrap font-mono text-[0.6875rem]",
        indicator.tone === "bad" ? "text-lcd-bad" : indicator.tone === "warn" ? "text-lcd-warn" : "text-panel-dim"
      )}
    >
      {indicator.tone !== "unknown" && <span aria-hidden className="h-1.5 w-1.5 rounded-[1px] bg-current" />}
      {indicator.text}
    </span>
  );
  return (
    <li>
      <NavLink
        to={item.to}
        end={item.end}
        aria-label={collapsed ? `${item.label}${indicator ? ` · ${indicator.text}` : ""}` : undefined}
        className={({ isActive }) =>
          cn(
            "relative flex items-center gap-2 rounded-md text-sm font-medium transition-colors focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
            collapsed ? "justify-center px-0 py-2" : "px-2 py-1.5",
            isActive
              ? "bg-panel-active text-white shadow-[inset_3px_0_0_theme(colors.lcd.digit)]"
              : "text-panel-text hover:bg-panel-hover hover:text-white"
          )
        }
      >
        {!collapsed && item.hotkey && <kbd className="key key-dark hidden lg:inline-grid">{item.hotkey}</kbd>}
        <item.icon className="h-4 w-4 shrink-0" aria-hidden />
        {!collapsed && <span className="min-w-0 flex-1 truncate">{item.label}</span>}
        {!collapsed && dot}
        {collapsed && indicator && (
          <span
            aria-hidden
            className={cn(
              "absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-[1px]",
              indicator.tone === "bad" ? "bg-lcd-bad" : indicator.tone === "warn" ? "bg-lcd-warn" : "bg-panel-dim"
            )}
          />
        )}
      </NavLink>
    </li>
  );
}

/**
 * Abonamentul, ca un rând mic cu starea lui. Numai pentru cine îl plătește (`canManage`). Cifra
 * de zile vine din `readOnlyOn` (F4): câte zile mai sunt până se închide scrisul, când e cazul.
 */
function BillingRow({ billing, collapsed }: { billing: BillingAccess | undefined; collapsed: boolean }) {
  const status = billing?.status ?? null;
  const tone = status === "READ_ONLY" || status === "CANCELLED" ? "text-lcd-bad" : status === "PAST_DUE" ? "text-lcd-warn" : "text-panel-mid";
  const label =
    status === "READ_ONLY" ? t.billingReadOnly : status === "PAST_DUE" ? t.billingPastDue : status === "CANCELLED" ? t.billingCancelled : "";
  return (
    <NavLink
      to="/abonament"
      aria-label={collapsed ? t.billing : undefined}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-2 rounded px-1.5 py-1 font-mono text-[0.6875rem] transition-colors hover:bg-panel-hover",
          collapsed && "justify-center px-0",
          isActive ? "text-white" : "text-panel-mid"
        )
      }
    >
      {collapsed ? (
        <span className={cn("h-1.5 w-1.5 rounded-[1px]", status === "ACTIVE" || status == null ? "bg-lcd-unit" : tone.replace("text-", "bg-"))} aria-hidden />
      ) : (
        <>
          <span className="flex-1">{t.billing}</span>
          <span className={tone}>{label}</span>
        </>
      )}
    </NavLink>
  );
}
