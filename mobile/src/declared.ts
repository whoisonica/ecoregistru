import { declaredText } from "@/lib/deadlines";
import { strings } from "@web/strings";
import type { Deadline } from "@web/types";
import { Alert } from "react-native";

const t = strings.movements;

/**
 * Întrebarea webului înainte de a schimba o cifră dintr-un an deja declarat (bifa termenului SIM):
 * „Anul e declarat pe … Salvezi oricum?”. Folosită de corectura predării și de „Adaugă cantitatea”.
 */
export function confirmDeclared(year: number, declaration: Deadline): Promise<boolean> {
  return new Promise((resolve) =>
    Alert.alert(
      declaredText(t.declaredTitle, year, declaration),
      declaredText(t.declaredSave, year, declaration),
      [
        { text: strings.common.cancel, style: "cancel", onPress: () => resolve(false) },
        { text: t.declaredConfirm, onPress: () => resolve(true) },
      ],
      { cancelable: true, onDismiss: () => resolve(false) },
    ),
  );
}

