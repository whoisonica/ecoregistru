import { strings } from "@/lib/strings";

/**
 * Taburile ecranelor care au taburi — o singură listă, citită și de pagină (`PageTabs`), și de
 * panou, care le arată sub intrarea deschisă (18.09.2026, varianta „E3" a meniului). Două liste
 * care trebuie să spună același lucru ajung mereu să nu-l mai spună: un tab adăugat numai în
 * pagină ar lipsi din meniu fără ca nimic să cadă.
 *
 * <p>`id` e valoarea din adresă (`?tab=`); `""` e tabul implicit, care nu se scrie.
 */
export interface ScreenTab {
  id: string;
  label: string;
}

export const GENERATION_TABS: ScreenTab[] = [
  { id: "", label: strings.movements.tabMovements },
  { id: "total", label: strings.movements.tabAnnual },
  { id: "ambalaje", label: strings.movements.tabPackaging },
];

export const DEADLINE_TABS: ScreenTab[] = [
  { id: "", label: strings.deadlines.todoTitle },
  { id: "bifate", label: strings.deadlines.doneTitle },
  { id: "trecute", label: strings.deadlines.pastTitle },
];

/** Numai la firma cu depozit (registrul art. 48): restul n-au persoane fizice, deci nici taburi. */
export const PARTNER_TABS: ScreenTab[] = [
  { id: "", label: strings.naturalPersons.tabFirms },
  { id: "persoane-fizice", label: strings.naturalPersons.tab },
];
