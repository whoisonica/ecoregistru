import { strings } from "@web/strings";
import type { ReactNode } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { haptic } from "../haptics";
import { colors, fonts } from "../theme";
import { Icon } from "./Icon";

/**
 * Rubrica pliată cu rezumatul pe rând (pasul 3 al predării): „Starea fizică · Solid ›”. Apăsată, se
 * deschide și arată pastilele; aleasă o valoare, se pliază la loc. Rubricile pe care omul nu le schimbă
 * de la o predare la alta nu mai umplu ecranul — dar niciuna nu e scoasă (serverul le cere, BUG-023).
 *
 * <p>Fără valoare, rezumatul e „Alege” pe accent; eroarea stă sub rând, în roșu, când e cerută.
 */
export function Fold({ label, summary, error, open, onToggle, children, testID, first }: {
  label: string;
  /** Ce s-a ales, într-o propoziție; `null` = nimic încă. */
  summary: string | null;
  error?: string;
  open: boolean;
  onToggle: () => void;
  children: ReactNode;
  testID: string;
  /** Primul rând din card n-are linia de deasupra. */
  first?: boolean;
}) {
  return (
    <View style={!first && styles.sep}>
      <Pressable
        testID={testID}
        onPress={() => {
          haptic.tap();
          onToggle();
        }}
        accessibilityRole="button"
        accessibilityState={{ expanded: open }}
        style={({ pressed }) => [styles.row, pressed && { backgroundColor: colors.pressed }]}
      >
        <Text style={styles.label}>{label}</Text>
        <View style={styles.trail}>
          <Text style={[styles.summary, summary == null && styles.choose]} numberOfLines={1}>
            {summary ?? strings.mobile.foldChoose}
          </Text>
          <Icon name={open ? "left" : "right"} size={18} color={colors.ink3} />
        </View>
      </Pressable>
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {open ? <View style={styles.body}>{children}</View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  sep: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  row: { minHeight: 52, paddingHorizontal: 16, paddingVertical: 12, flexDirection: "row", alignItems: "center", gap: 12 },
  label: { flexShrink: 0, fontFamily: fonts.sansMedium, fontSize: 15.5, color: colors.ink },
  trail: { flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "flex-end", gap: 4 },
  summary: { flexShrink: 1, fontFamily: fonts.sans, fontSize: 15, color: colors.ink2, textAlign: "right" },
  choose: { color: colors.greenText, fontFamily: fonts.sansMedium },
  error: { fontFamily: fonts.sans, fontSize: 13, color: colors.redText, paddingHorizontal: 16, paddingBottom: 8 },
  body: { paddingHorizontal: 16, paddingBottom: 14, gap: 8 },
});
