import type { ReactNode } from "react";
import { StyleSheet, Text, View } from "react-native";

import { colors, fonts, radius } from "../theme";

/**
 * Afișajul cântarului din prototip: cifra verde peste segmentele stinse „88.888”. Segmentele urmează lungimea cifrei,
 * ca să nu rămână o cifră mare peste o fantomă mai scurtă.
 */
export function Lcd({ label, state, value, unit, foot, footTone = "alert", footRight, ghost: ghostOverride }: {
  label: string;
  state: string;
  /** `null` = n-a încărcat. Arată „?”, nu „0” (todo-mobil §6). */
  value: string | null;
  unit?: string;
  foot?: string;
  footTone?: "alert" | "ok";
  /** Tastele de pe rândul de jos. Nu stau lângă cifră: îi iau lățimea, iar fantoma nu mai încape sub ea. */
  footRight?: ReactNode;
  /** Segmentele stinse, când cifra nu e o cantitate: „8” sub un număr de rânduri, ca în prototip. */
  ghost?: string;
}) {
  const shown = value ?? "?";
  const ghost = ghostOverride ?? ghostFor(shown);
  const size = ghost.length > 6 ? styles.digitsSmall : null;
  return (
    <View style={styles.lcd}>
      <View style={styles.top}>
        <Text style={styles.topText}>{label}</Text>
        <View style={styles.state}>
          <View style={styles.led} />
          <Text style={styles.topText}>{state}</Text>
        </View>
      </View>
      <View style={styles.readRow}>
        <View>
          {/* Fantoma dă lățimea (e mereu cel puțin cât cifra); cifra aprinsă stă peste ea, aliniată la dreapta. */}
          <Text style={[styles.digits, size, styles.ghost]} numberOfLines={1} accessibilityElementsHidden importantForAccessibility="no">
            {ghost}
          </Text>
          <Text style={[styles.digits, size, styles.lit]} numberOfLines={1} testID="lcd-value">{shown}</Text>
        </View>
        {unit ? <Text style={styles.unit}>{unit}</Text> : null}
      </View>
      {foot || footRight ? (
        <View style={styles.foot}>
          <Text style={[styles.footText, footTone === "ok" && { color: colors.lcdUnit }]}>{foot}</Text>
          {footRight}
        </View>
      ) : null}
    </View>
  );
}

/** „0” → „88.888”, „790,5” → „88.888,8”: fiecare cifră aprinsă cade peste un 8 stins. */
function ghostFor(shown: string) {
  const [int, dec] = shown.split(",");
  const intGhost = "888.888.888".slice(-Math.max(6, int.length));
  return dec === undefined ? intGhost : `${intGhost},${dec.replace(/./g, "8")}`;
}

const styles = StyleSheet.create({
  lcd: {
    marginTop: 16,
    backgroundColor: colors.lcd,
    borderRadius: radius.lcd,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "rgba(255,255,255,0.08)",
  },
  top: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  topText: { fontFamily: fonts.mono, fontSize: 10.5, letterSpacing: 1.2, color: colors.lcdUnit },
  state: { flexDirection: "row", alignItems: "center", gap: 6 },
  led: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: colors.lcdDigit,
    shadowColor: colors.lcdDigit,
    shadowOpacity: 1,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 0 },
  },
  readRow: { flexDirection: "row", justifyContent: "flex-end", alignItems: "baseline", gap: 8, marginTop: 2 },
  digits: { fontFamily: fonts.monoMedium, fontSize: 56, lineHeight: 62, letterSpacing: -1, fontVariant: ["tabular-nums"] },
  digitsSmall: { fontSize: 44, lineHeight: 50 },
  ghost: { color: colors.lcdGhost },
  lit: {
    position: "absolute",
    right: 0,
    top: 0,
    color: colors.lcdDigit,
    textShadowColor: "rgba(124,242,169,0.4)",
    textShadowRadius: 14,
    textShadowOffset: { width: 0, height: 0 },
  },
  unit: { fontFamily: fonts.mono, fontSize: 18, color: colors.lcdUnit },
  foot: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 8,
    marginTop: 8,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: "rgba(124,242,169,0.1)",
  },
  footText: { flexShrink: 1, fontFamily: fonts.mono, fontSize: 11.5, color: colors.lcdAlert },
});
