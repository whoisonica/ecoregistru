import { strings } from "@web/strings";
import type { MovementScreen } from "@/lib/movementScreens";
import { useState } from "react";

import { SCREEN_LABEL, useMovementScreens } from "../../src/company";
import { MovementList } from "../../src/components/MovementList";

/**
 * Mișcările firmei — un singur ecran pe telefon, cu comutator, acolo unde webul are trei rute.
 *
 * <p>Bara de jos are cinci locuri și toate sunt luate (Acasă · Mișcări · „+” · Termene · Control).
 * O firmă „generator și colector” are trei ecrane de mișcări; băgate în bară ar fi scos afară
 * „Control” — adică tocmai ecranul pentru care se ia telefonul în mână când vine Garda. Deci
 * ecranele stau unul lângă altul aici, și bara rămâne cea din prototipul aprobat.
 *
 * <p>Generatorul pur are un singur ecran, deci nu vede niciun comutator.
 */
export default function MiscariScreen() {
  const screens = useMovementScreens();
  const [picked, setPicked] = useState<MovementScreen | null>(null);
  const current = picked && screens.includes(picked) ? picked : screens[0];

  return (
    <MovementList
      title={current ? SCREEN_LABEL[current] : strings.nav.movements}
      screen={current}
      tabs={screens.length > 1 ? { screens, current, onPick: setPicked } : undefined}
    />
  );
}
