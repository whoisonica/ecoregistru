import { strings } from "@web/strings";
import type { Unit } from "@web/types";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import { haptic } from "../haptics";
import { colors, fonts } from "../theme";

/**
 * Cantitatea ca pe afișaj (F3, 27.09.2026): cifra mare, în mono, cu unitatea lângă ea și tastatura
 * numerică a telefonului. Punctul și virgula fac același lucru (`parseQuantity`). Ce e nativ (tastatura)
 * nu se redesenează; ce e al nostru (cifra mare, KG / t) arată ca restul aplicației.
 */
export function QuantityField({ value, onChange, unit, onUnit, editable = true, testID }: {
  value: string;
  onChange: (v: string) => void;
  unit: Unit;
  onUnit: (u: Unit) => void;
  editable?: boolean;
  testID?: string;
}) {
  return (
    <View style={styles.wrap}>
      <View style={styles.readRow}>
        <TextInput
          testID={testID}
          value={value}
          editable={editable}
          onChangeText={onChange}
          keyboardType="decimal-pad"
          placeholder="0"
          placeholderTextColor={colors.ink3}
          style={[styles.digits, !editable && { color: colors.ink3 }]}
          maxLength={12}
          selectTextOnFocus
          accessibilityLabel={strings.movements.quantity}
        />
        {/* „kg” / „t” lângă cifră, ca pe afișaj; numele întreg al unității stă pe comutator. */}
        <Text style={styles.unit}>{unit === "KG" ? "kg" : "t"}</Text>
      </View>
      <View style={styles.seg} accessibilityRole="radiogroup">
        {(["KG", "TONS"] as Unit[]).map((u) => {
          const on = u === unit;
          return (
            <Pressable
              key={u}
              testID={testID ? `${testID}-${u}` : undefined}
              onPress={() => {
                if (!on) haptic.tap();
                onUnit(u);
              }}
              accessibilityRole="radio"
              accessibilityState={{ selected: on }}
              style={[styles.segItem, on && styles.segOn]}
            >
              <Text style={[styles.segText, on && styles.segTextOn]}>{strings.enums.unit[u]}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flexDirection: "row", alignItems: "center", gap: 12 },
  readRow: { flex: 1, flexDirection: "row", alignItems: "baseline", gap: 8, backgroundColor: colors.card, borderWidth: 1, borderColor: colors.separator, borderRadius: 12, paddingHorizontal: 12, minHeight: 60 },
  digits: { flex: 1, fontFamily: fonts.monoMedium, fontSize: 32, color: colors.ink, letterSpacing: -0.5, paddingVertical: 8, textAlign: "right", fontVariant: ["tabular-nums"] },
  unit: { fontFamily: fonts.mono, fontSize: 16, color: colors.ink2, paddingBottom: 12 },
  seg: { backgroundColor: "#E1E5E2", borderRadius: 11, padding: 3, gap: 3 },
  segItem: { paddingVertical: 8, paddingHorizontal: 12, borderRadius: 9, alignItems: "center", minWidth: 48 },
  segOn: { backgroundColor: colors.card },
  segText: { fontFamily: fonts.sansMedium, fontSize: 13.5, color: colors.ink2 },
  segTextOn: { fontFamily: fonts.sansSemiBold, color: colors.ink },
});
