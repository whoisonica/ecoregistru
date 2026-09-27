import { StyleSheet, Text, View } from "react-native";

import { colors, fonts } from "../theme";
import { Icon, type IconName } from "./Icon";

export type TileTone = "ok" | "warn" | "bad" | "quiet" | "bin";

/**
 * Pătratul de 36 din capul unui rând: iconița pe fondul deschis al tonului, sau — la `bin` — pătrățelul
 * pubelei pe fondul accentului deschis (nu un pătrat plin de culoare: paleta A, 27.09.2026).
 */
export function Tile({ icon, tone, color, text }: {
  icon?: IconName;
  tone: TileTone;
  /** Culoarea pubelei, când `tone` e `bin`. */
  color?: string;
  /** În locul iconiței: un „?”, o cifră. */
  text?: string;
}) {
  if (tone === "bin") {
    return (
      <View style={[styles.tile, { backgroundColor: colors.greenSoft }]}>
        <View style={[styles.bin, { backgroundColor: color ?? colors.ink3 }]} />
      </View>
    );
  }
  const { bg, fg } = TONES[tone];
  return (
    <View style={[styles.tile, { backgroundColor: bg }]}>
      {text != null ? <Text style={[styles.text, { color: fg }]}>{text}</Text> : icon ? <Icon name={icon} size={19} color={fg} strokeWidth={1.9} /> : null}
    </View>
  );
}

const TONES = {
  ok: { bg: colors.greenSoft, fg: colors.greenText },
  warn: { bg: colors.amberSoft, fg: colors.amberText },
  bad: { bg: colors.redSoft, fg: colors.redText },
  quiet: { bg: colors.quiet, fg: colors.ink2 },
} as const;

const styles = StyleSheet.create({
  tile: { width: 36, height: 36, borderRadius: 10, alignItems: "center", justifyContent: "center" },
  bin: { width: 14, height: 14, borderRadius: 4 },
  text: { fontFamily: fonts.monoMedium, fontSize: 16 },
});
