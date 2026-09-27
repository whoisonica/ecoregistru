import { LinearGradient } from "expo-linear-gradient";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { haptic } from "../haptics";
import { colors, fonts, radius } from "../theme";
import { Icon, type IconName } from "./Icon";

/**
 * Tasta mare a ecranului (54–64 px): numai pentru acțiunea lui — „Pozează avizul”, „Trimite dosarul”.
 * Verde pentru ce adaugă, grafit pentru ce trimite. Restul ecranului sunt rânduri, nu taste.
 */
export function Key({ icon, label, hint, tone = "green", onPress, disabled, testID }: {
  icon: IconName;
  label: string;
  hint?: string;
  tone?: "green" | "dark";
  onPress: () => void;
  disabled?: boolean;
  testID?: string;
}) {
  const gradient: [string, string] = tone === "green" ? [colors.greenHi, colors.green] : ["#333C37", "#232925"];
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
      style={({ pressed }) => [styles.wrap, disabled && { opacity: 0.45 }, pressed && styles.pressed]}
    >
      <LinearGradient colors={gradient} style={[styles.key, tone === "dark" && styles.dark]}>
        <Icon name={icon} size={26} color="#fff" strokeWidth={2} />
        <View style={styles.text}>
          <Text style={styles.label} numberOfLines={1}>{label}</Text>
          {hint ? <Text style={styles.hint} numberOfLines={1}>{hint}</Text> : null}
        </View>
      </LinearGradient>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1 },
  pressed: { transform: [{ scale: 0.97 }], opacity: 0.95 },
  key: {
    minHeight: 92,
    borderRadius: radius.hero,
    paddingHorizontal: 12,
    paddingVertical: 14,
    justifyContent: "flex-end",
    gap: 8,
  },
  dark: { borderWidth: StyleSheet.hairlineWidth, borderColor: "rgba(124,242,169,0.35)" },
  text: { gap: 2 },
  label: { fontFamily: fonts.sansSemiBold, fontSize: 15.5, color: "#fff" },
  hint: { fontFamily: fonts.sans, fontSize: 12, color: "rgba(255,255,255,0.8)" },
});
