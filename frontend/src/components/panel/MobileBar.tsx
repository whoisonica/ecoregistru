import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { ArrowDownToLine, ArrowUpFromLine, CalendarClock, Home, Menu, Plus } from "lucide-react";
import { Dialog } from "@/components/ui/dialog";
import type { NavEntry } from "@/lib/navItems";
import { SCREEN_PATH, type MovementScreen } from "@/lib/movementScreens";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

const t = strings.panel;

/**
 * Bara de jos, pe telefon: Acasă · lista principală a firmei · + · Termene · Mai mult.
 *
 * <p>Pe un ecran de 375px meniul de zece intrări nu încape la vedere; ce încape sunt cele patru
 * locuri în care se ajunge de zece ori pe zi, plus butonul de adăugare — care oferă **doar** ce e
 * permis tipului de firmă (un generator n-are „Intrare"). „Mai mult" deschide sertarul cu restul.
 */
export function MobileBar({
  mainList,
  screens,
  canWrite,
  overdue,
  onMore,
}: {
  /** Prima intrare de mișcări a firmei — sau, fără firmă, nimic. */
  mainList: NavEntry | null;
  screens: MovementScreen[];
  canWrite: boolean;
  /** Punct roșu pe Termene când e ceva depășit. */
  overdue: boolean;
  onMore: () => void;
}) {
  const navigate = useNavigate();
  const [pick, setPick] = useState(false);
  const hasGenerated = screens.includes("GENERATED");
  const hasCollector = screens.includes("IN");
  const choices: { to: string; label: string; icon: typeof Plus; tone: string }[] = [];
  if (hasGenerated) choices.push({ to: `${SCREEN_PATH.GENERATED}?nou=1`, label: hasCollector ? t.addOwnWaste : t.addWaste, icon: Plus, tone: "bg-brand text-white" });
  if (hasCollector) {
    choices.push({ to: `${SCREEN_PATH.IN}?nou=1`, label: t.addInbound, icon: ArrowDownToLine, tone: "bg-inbound text-white" });
    choices.push({ to: `${SCREEN_PATH.OUT}?nou=1`, label: t.addOutbound, icon: ArrowUpFromLine, tone: "bg-panel-active text-white ring-1 ring-inset ring-lcd-unit" });
  }

  function add() {
    if (choices.length === 1) navigate(choices[0].to);
    else setPick(true);
  }

  const tab = "flex flex-col items-center gap-0.5 rounded px-1 pb-1 pt-1.5 text-[0.6875rem] font-medium text-content-muted";
  const active = "text-brand-700 font-semibold";

  return (
    <>
      <nav
        aria-label={strings.common.mainNav}
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 items-end border-t border-line-strong bg-surface px-1 pb-[max(0.5rem,env(safe-area-inset-bottom))] lg:hidden"
      >
        <NavLink to="/" end className={({ isActive }) => cn(tab, isActive && active)}>
          <Home className="h-5 w-5" aria-hidden />
          {t.barHome}
        </NavLink>
        {mainList ? (
          <NavLink to={mainList.to} className={({ isActive }) => cn(tab, isActive && active)}>
            <mainList.icon className="h-5 w-5" aria-hidden />
            {mainList.label}
          </NavLink>
        ) : (
          <span />
        )}
        {canWrite && choices.length > 0 ? (
          <button type="button" onClick={add} aria-label={t.barAdd} className={tab}>
            <span className="-mt-5 grid h-11 w-11 place-items-center rounded-md bg-brand text-white shadow-[0_3px_0_theme(colors.brand.800)]">
              <Plus className="h-6 w-6" aria-hidden />
            </span>
            {t.barAdd}
          </button>
        ) : (
          <span />
        )}
        <NavLink to="/termene" className={({ isActive }) => cn(tab, "relative", isActive && active)}>
          <CalendarClock className="h-5 w-5" aria-hidden />
          {t.barDeadlines}
          {overdue && <span aria-hidden className="absolute right-3 top-1 h-1.5 w-1.5 rounded-[1px] bg-state-bad" />}
        </NavLink>
        <button type="button" onClick={onMore} aria-label={strings.common.openNav} aria-controls="navigatie-principala" className={tab}>
          <Menu className="h-5 w-5" aria-hidden />
          {t.barMore}
        </button>
      </nav>
      {pick && (
        <Dialog open onClose={() => setPick(false)} title={t.barAddTitle} size="sm">
          <div className="flex flex-col gap-2">
            {choices.map((c) => (
              <button
                key={c.to}
                type="button"
                onClick={() => {
                  setPick(false);
                  navigate(c.to);
                }}
                className={cn("flex items-center gap-3 rounded-md px-4 py-3 text-left text-sm font-semibold", c.tone)}
              >
                <c.icon className="h-5 w-5" aria-hidden />
                {c.label}
              </button>
            ))}
          </div>
        </Dialog>
      )}
    </>
  );
}
