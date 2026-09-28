import { Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { colors, fonts } from "../theme";
import { Icon } from "./Icon";

/**
 * Capul unui pas al predării (F4): pe hârtie, se derulează cu pagina — nimic țintuit sus (regula din
 * 27.09). Rândul de sus are săgeata înapoi („Renunță” la primul pas, „Înapoi” apoi) și eticheta mono
 * mică „PREDARE NOUĂ · 1 DIN 3”; dedesubt titlul pasului și trei bare de progres.
 */
export function StepHead({ step, total, kicker, title, back }: {
  step: number;
  total: number;
  kicker: string;
  title: string;
  back: { label: string; onPress: () => void };
}) {
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.head, { paddingTop: insets.top + 10 }]}>
      <View style={styles.topRow}>
        <Pressable onPress={back.onPress} style={styles.back} hitSlop={8} accessibilityRole="button" testID="step-back">
          <Icon name="back" size={20} color={colors.green} strokeWidth={2.2} />
          <Text style={styles.backText}>{back.label}</Text>
        </Pressable>
        <Text style={styles.kicker} numberOfLines={1}>{kicker.toUpperCase()}</Text>
      </View>
      <Text style={styles.title} testID="step-title">{title}</Text>
      <View style={styles.bars} accessibilityLabel={kicker}>
        {Array.from({ length: total }, (_, i) => (
          <View key={i} style={[styles.bar, i < step && styles.barOn]} />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  head: { paddingHorizontal: 16, paddingBottom: 8, gap: 12 },
  topRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10, minHeight: 34 },
  back: { flexDirection: "row", alignItems: "center", gap: 2 },
  backText: { fontFamily: fonts.sansMedium, fontSize: 16, color: colors.green },
  kicker: { flexShrink: 1, fontFamily: fonts.mono, fontSize: 11, letterSpacing: 1.1, color: colors.ink3 },
  title: { fontFamily: fonts.sansSemiBold, fontSize: 30, lineHeight: 33, letterSpacing: -0.75, color: colors.ink },
  bars: { flexDirection: "row", gap: 6 },
  bar: { flex: 1, height: 4, borderRadius: 2, backgroundColor: colors.track },
  barOn: { backgroundColor: colors.green },
});
