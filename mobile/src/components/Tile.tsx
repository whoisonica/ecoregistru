import { StyleSheet, Text, View } from "react-native";

import { colors, fonts } from "../theme";
import { Icon, type IconName } from "./Icon";

export type TileTone = "ok" | "warn" | "bad" | "quiet" | "bin";

/** Pătratul de 40 din capul unui rând: iconița pe fondul tonului, sau pubela pe culoarea codului. */
export function Tile({ icon, tone, color, text }: {
  icon?: IconName;
  tone: TileTone;
  /** Culoarea pubelei, când `tone` e `bin`. */
  color?: string;
  /** În locul iconiței: un „?”, o cifră. */
  text?: string;
}) {
  const bg = tone === "bin" ? color ?? colors.ink3 : TONES[tone].bg;
  const fg = tone === "bin" ? "#fff" : TONES[tone].fg;
  return (
    <View style={[styles.tile, { backgroundColor: bg }]}>
      {text != null ? <Text style={[styles.text, { color: fg }]}>{text}</Text> : icon ? <Icon name={icon} size={21} color={fg} strokeWidth={1.9} /> : null}
    </View>
  );
}

const TONES = {
  ok: { bg: colors.greenSoft, fg: colors.greenText },
  warn: { bg: colors.amberSoft, fg: colors.amberText },
  bad: { bg: colors.redSoft, fg: colors.redText },
  quiet: { bg: "#EDEFEE", fg: colors.ink2 },
} as const;

const styles = StyleSheet.create({
  tile: { width: 40, height: 40, borderRadius: 11, alignItems: "center", justifyContent: "center" },
  text: { fontFamily: fonts.monoMedium, fontSize: 17 },
});
