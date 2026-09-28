import { LinearGradient } from "expo-linear-gradient";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { colors, fonts } from "../theme";
import { PrimaryButton } from "./Form";

/**
 * Butonul lipit jos al pasului („Continuă” / „Salvează predarea”) cu propoziția lui dedesubt: câte
 * rubrici din poză mai sunt de confirmat (galben) sau ce se întâmplă la apăsare. Hârtia de sub el se
 * stinge spre transparent în sus, ca lista să se vadă trecând pe dedesubt.
 */
export function StepFoot({ label, note, warn, onPress, disabled, testID, secondary }: {
  label: string;
  note?: string | null;
  warn?: boolean;
  onPress: () => void;
  disabled?: boolean;
  testID: string;
  /** O a doua cale, ca link sub buton — „Vezi restul pașilor” când „Salvează” vine înainte de pasul 3. */
  secondary?: { label: string; onPress: () => void; testID: string };
}) {
  const insets = useSafeAreaInsets();
  return (
    <LinearGradient
      colors={["rgba(244,244,239,0)", colors.ground, colors.ground]}
      locations={[0, 0.3, 1]}
      style={[styles.foot, { paddingBottom: Math.max(insets.bottom, 12) + 6 }]}
      pointerEvents="box-none"
    >
      <PrimaryButton label={label} onPress={onPress} disabled={disabled} testID={testID} />
      {secondary ? (
        <Pressable onPress={secondary.onPress} hitSlop={8} testID={secondary.testID} accessibilityRole="button">
          <Text style={styles.secondary}>{secondary.label}</Text>
        </Pressable>
      ) : null}
      {note ? (
        <Text style={[styles.note, warn && styles.warn]} testID="step-note" numberOfLines={2}>
          {note}
        </Text>
      ) : (
        <View style={styles.noteGap} />
      )}
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  foot: { position: "absolute", left: 0, right: 0, bottom: 0, paddingHorizontal: 16, paddingTop: 22, gap: 8 },
  note: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink2, textAlign: "center" },
  warn: { color: colors.amberText, fontFamily: fonts.sansMedium },
  noteGap: { height: 16 },
  secondary: { fontFamily: fonts.sansMedium, fontSize: 14, color: colors.green, textAlign: "center", paddingVertical: 2 },
});
