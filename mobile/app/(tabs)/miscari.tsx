import { strings } from "@web/strings";
import type { MovementScreen } from "@/lib/movementScreens";
import { GENERATION_TABS } from "@/lib/screenTabs";
import { useState } from "react";

import { SCREEN_LABEL, useMovementScreens } from "../../src/company";
import { AnnualTotals } from "../../src/components/AnnualTotals";
import { MovementList } from "../../src/components/MovementList";
import { PackagingSummary } from "../../src/components/PackagingSummary";
import { TabRow, type TabItem } from "../../src/components/TabRow";

/**
 * Mișcările firmei — un singur ecran pe telefon, acolo unde webul are trei rute și, pe „Generare”,
 * trei taburi (Mișcări · Totalul anului · Ambalaje, `lib/screenTabs.ts`).
 *
 * <p>**Toate pe același rând** (proprietarul, 26.09.2026): ecranele firmei și taburile lui „Generare”,
 * în ordinea de pe web — la generatorul pur „Mișcări · Totalul anului · Ambalaje”, la „generator și
 * colector” „Generare · Totalul anului · Ambalaje · Intrări · Ieșiri”, la colector „Intrări · Ieșiri”.
 *
 * <p>Bara de jos are cinci locuri și toate sunt luate (Acasă · Mișcări · „+” · Termene · Control), deci
 * rândul ăsta ține tot ce webul pune în meniu sub „Generare”, „Intrări” și „Ieșiri”.
 */
export default function MiscariScreen() {
  const screens = useMovementScreens();
  const tabs = tabsFor(screens);
  const [picked, setPicked] = useState<string | null>(null);
  const current = picked && tabs.some((tab) => tab.id === picked) ? picked : tabs[0]?.id;

  const row = tabs.length > 1 ? <TabRow tabs={tabs} selected={current ?? ""} onSelect={setPicked} /> : null;

  if (current === "total") return <AnnualTotals tabRow={row} />;
  if (current === "ambalaje") return <PackagingSummary tabRow={row} />;
  const screen = current as MovementScreen | undefined;
  return (
    <MovementList title={screen ? SCREEN_LABEL[screen] : strings.nav.movements} screen={screen} tabRow={row} />
  );
}

/** Rândul de taburi al firmei. Id-ul unui ecran e ecranul însuși; ale taburilor, cele din adresa webului. */
function tabsFor(screens: MovementScreen[]): TabItem[] {
  const tabs: TabItem[] = [];
  for (const screen of screens) {
    if (screen !== "GENERATED") {
      tabs.push({ id: screen, label: SCREEN_LABEL[screen] });
      continue;
    }
    const [list, ...rest] = GENERATION_TABS;
    // Lângă „Intrări” și „Ieșiri”, „Mișcări” n-ar spune ale cui: acolo tabul poartă numele ecranului.
    tabs.push({ id: screen, label: screens.length > 1 ? SCREEN_LABEL.GENERATED : list.label });
    tabs.push(...rest);
  }
  return tabs;
}
