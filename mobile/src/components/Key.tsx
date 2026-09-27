import { Pressable, StyleSheet, Text, View } from "react-native";

import { haptic } from "../haptics";
import { colors, fonts, radius } from "../theme";
import { Icon, type IconName } from "./Icon";

/**
 * Tasta mare a ecranului (56 px): numai pentru acțiunea lui — „Pozează avizul”. Plată, pe accent, fără
 * gradient și fără a doua culoare mare pe ecran (paleta A, 27.09.2026). `quiet` e varianta albă cu
 * contur, pentru a doua acțiune a unui ecran (Control: „Trimite dosarul” stă pe accent acolo).
 */
export function Key({ icon, label, hint, tone = "green", onPress, disabled, testID }: {
  icon: IconName;
  label: string;
  hint?: string;
  tone?: "green" | "quiet";
  onPress: () => void;
  disabled?: boolean;
  testID?: string;
}) {
  const quiet = tone === "quiet";
  const fg = quiet ? colors.ink : colors.onAccent;
  return (
    <Pressable
      testID={testID}
      onPress={() => {
        haptic.tap();
        onPress();
      }}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      style={({ pressed }) => [styles.key, quiet && styles.quiet, disabled && { opacity: 0.45 }, pressed && styles.pressed]}
    >
      <Icon name={icon} size={22} color={fg} strokeWidth={2} />
      <View style={styles.text}>
        <Text style={[styles.label, { color: fg }]} numberOfLines={1}>{label}</Text>
        {hint ? <Text style={[styles.hint, quiet && { color: colors.ink2 }]} numberOfLines={1}>{hint}</Text> : null}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  key: {
    minHeight: 56,
    borderRadius: radius.button,
    backgroundColor: colors.green,
    paddingHorizontal: 18,
    paddingVertical: 12,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
  },
  quiet: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.separator },
  pressed: { transform: [{ scale: 0.985 }], opacity: 0.9 },
  text: { alignItems: "center", gap: 1 },
  label: { fontFamily: fonts.sansSemiBold, fontSize: 16.5 },
  hint: { fontFamily: fonts.sans, fontSize: 12, color: "rgba(255,255,255,0.8)" },
});
