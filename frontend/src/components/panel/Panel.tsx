import { NavLink, Link, useLocation } from "react-router-dom";
import { ChevronsLeft, ChevronsRight, Search, X } from "lucide-react";
import { BrandName } from "@/components/BrandName";
import { CompanyLabel } from "@/components/panel/CompanyLabel";
import { AccountMenu } from "@/components/panel/AccountMenu";
import type { Indicator } from "@/hooks/usePanelData";
import type { NavEntry, NavModel } from "@/lib/navItems";
import type { MovementScreen } from "@/lib/movementScreens";
import { strings } from "@/lib/strings";
import type { BillingAccess, Company } from "@/lib/types";
import { cn } from "@/lib/utils";

const t = strings.panel;

export interface PanelProps {
  nav: NavModel;
  company: Company | undefined;
  screens: MovementScreen[];
  canManage: boolean;
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
 * Panoul — bara din stânga, direcția „Cântar” (`todo-ui-cantar.md` §3), în forma „E3" aleasă de
 * proprietar pe 18.09.2026 din machetele „Meniul după fuziuni" — fără „+"-urile de pe rânduri, pe care
 * le-a văzut pe localhost și le-a scos în aceeași seară („arată urâțel"), deci e de fapt „E2".
 *
 * <p>De sus în jos: logo + căutarea ca iconiță + tasta `[`; firma, pe plăcuța ei; meniul — tastă,
 * nume, indicator; sub intrarea deschisă, taburile ecranului; grupul Cabinet; jos, abonamentul (doar cine administrează) și contul.
 *
 * <p>**Ce a plecat, ca să nu se pună la loc:** afișajul lunii, tastele mari de adăugare, rândul
 * „Caută oriunde" (proprietarul: „nu îmi plac") și orice „+" pe rândurile meniului. Din panou nu se
 * adaugă nimic: adăugarea e butonul din antetul ecranului și tastele N / I / E. Toate trei repetau ceva: cifra lunii e pe Acasă,
 * „Adaugă deșeuri" e sus pe Acasă și pe Generare (iar N / I / E merg de oriunde), căutarea e Ctrl K.
 *
 * <p>Același component randează sertarul de pe telefon: acolo `collapsed` e mereu fals și butonul
 * `[` e înlocuit de „×".
 */
export function Panel({
  nav,
  company,
  screens,
  canManage,
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
  const hasCollector = screens.includes("IN");
  const searchButton = (
    <button
      type="button"
      onClick={onOpenSearch}
      aria-label={`${t.searchEverywhere} (${t.searchKey})`}
      data-testid="panel-search"
      className={cn(
        "grid place-items-center rounded-md border border-panel-line text-panel-mid transition-colors hover:border-panel-key hover:text-white focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
        collapsed ? "h-9 w-full" : "h-7 w-7"
      )}
    >
      <Search className="h-4 w-4" aria-hidden />
    </button>
  );

  return (
    <div className={cn("flex h-full flex-col gap-2.5 bg-panel text-panel-text", collapsed ? "px-2 pb-2.5 pt-3" : "px-3 pb-3 pt-3.5")}>
      {/* Logo + tasta de strângere. Pe telefon (sertar) e „×". */}
      <div className={cn("flex items-center gap-2 text-white", collapsed ? "justify-center" : "px-1")}>
        {!collapsed && (
          <Link to="/" className="min-w-0 truncate text-base font-semibold text-white">
            <BrandName />
          </Link>
        )}
        {!collapsed && <span className="ml-auto">{searchButton}</span>}
        <button
          type="button"
          onClick={onToggleCollapsed}
          aria-label={collapsed ? t.expand : t.collapse}
          aria-pressed={collapsed}
          className="hidden items-center gap-1 rounded text-panel-mid transition-colors hover:text-white focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0 lg:inline-flex"
        >
          {collapsed ? <ChevronsRight className="h-4 w-4" aria-hidden /> : <ChevronsLeft className="h-4 w-4" aria-hidden />}
          {!collapsed && <kbd className="key key-dark">{t.collapseKey}</kbd>}
        </button>
        <button
          type="button"
          onClick={onCloseDrawer}
          aria-label={strings.common.closeNav}
          className="rounded p-1 text-panel-mid hover:text-white lg:hidden"
        >
          <X className="h-5 w-5" aria-hidden />
        </button>
      </div>

      <CompanyLabel company={company} collapsed={collapsed} />

      {/* Pe șina strânsă iconița de căutare n-are loc lângă logo: stă sub firmă. */}
      {collapsed && searchButton}

      <nav aria-label={strings.common.mainNav} className="-mx-1 flex-1 overflow-y-auto px-1">
        <ul className="flex flex-col gap-px">
          {nav.main.map((item) => (
            <NavRow
              key={item.to}
              item={item}
              indicator={indicatorFor(item.screen, item.to)}
              collapsed={collapsed}
            />
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

function IndicatorDot({ indicator }: { indicator: Indicator }) {
  return (
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
}

/**
 * Un rând de meniu. Deschis (panoul lat): tasta, numele și indicatorul; iconița rămâne numai pe șina strânsă, unde ține locul numelui. Două semne înaintea
 * fiecărui cuvânt (tastă + iconiță) erau două lucruri de citit până la el.
 *
 * <p>Sub intrarea deschisă stau taburile ecranului (`item.tabs`, aceeași listă ca în pagină). Cât
 * sunt desfăcute, indicatorul coboară pe primul tab: cifrele lui („1 de cântărit", „2 expiră",
 * „1 depășit") sunt toate despre lista implicită, deci acolo e locul lor.
 */
function NavRow({
  item,
  indicator,
  collapsed,
}: {
  item: NavEntry;
  indicator: Indicator | null;
  collapsed: boolean;
}) {
  const location = useLocation();
  const active = item.end ? location.pathname === item.to : location.pathname === item.to || location.pathname.startsWith(`${item.to}/`);
  const tabs = !collapsed && active && item.tabs ? item.tabs : null;
  const tabParam = new URLSearchParams(location.search).get("tab") ?? "";
  // O valoare stricată în adresă cade pe tabul implicit, ca în pagină.
  const currentTab = tabs?.some((tab) => tab.id === tabParam) ? tabParam : "";

  /** Ca `setTab` din pagină: schimbă doar `tab`, restul adresei (luna, punctul, filtrele) rămâne. */
  const tabTo = (id: string) => {
    const params = new URLSearchParams(location.search);
    if (id) params.set("tab", id);
    else params.delete("tab");
    const search = params.toString();
    return { pathname: item.to, search: search ? `?${search}` : "" };
  };

  return (
    <li>
      <div
        className={cn(
          "relative flex items-center rounded-md transition-colors",
          active ? "bg-panel-active shadow-[inset_3px_0_0_theme(colors.lcd.digit)]" : "hover:bg-panel-hover"
        )}
      >
        <NavLink
          to={item.to}
          end={item.end}
          data-nav="row"
          aria-label={collapsed ? `${item.label}${indicator ? ` · ${indicator.text}` : ""}` : undefined}
          className={cn(
            "flex min-w-0 flex-1 items-center gap-2 rounded-md text-sm font-medium transition-colors focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
            collapsed ? "justify-center px-0 py-2" : "px-2 py-1.5",
            active ? "text-white" : "text-panel-text hover:text-white"
          )}
        >
          {!collapsed && item.hotkey && <kbd className="key key-dark hidden lg:inline-grid">{item.hotkey}</kbd>}
          {collapsed && <item.icon className="h-4 w-4 shrink-0" aria-hidden />}
          {!collapsed && <span className="min-w-0 flex-1 truncate">{item.label}</span>}
          {!collapsed && !tabs && indicator && <IndicatorDot indicator={indicator} />}
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
      </div>
      {tabs && (
        <ul className="mb-1 ml-[1.125rem] mt-px flex flex-col gap-px border-l border-panel-key pl-2.5" aria-label={item.label}>
          {tabs.map((tab, i) => {
            const on = tab.id === currentTab;
            return (
              <li key={tab.id || "implicit"}>
                <Link
                  to={tabTo(tab.id)}
                  replace
                  data-nav="tab"
                  aria-current={on ? "page" : undefined}
                  className={cn(
                    "flex items-center gap-2 rounded px-2 py-1 text-[0.84375rem] transition-colors focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
                    on ? "bg-panel-hover text-white" : "text-panel-mid hover:bg-panel-hover hover:text-white"
                  )}
                >
                  <span className="min-w-0 flex-1 truncate">{tab.label}</span>
                  {i === 0 && indicator && <IndicatorDot indicator={indicator} />}
                </Link>
              </li>
            );
          })}
        </ul>
      )}
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
