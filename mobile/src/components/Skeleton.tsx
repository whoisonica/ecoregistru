import { useEffect, useRef } from "react";
import { Animated, StyleSheet, View, type ViewStyle } from "react-native";

/**
 * Scheletul unei cifre sau al unui rând cât timp se încarcă: gri, pulsează. Ecranul își ține forma în
 * loc să „sară” când vin datele — și nu arată niciodată un „0” în locul cifrei care n-a venit.
 */
export function Skeleton({ width, height = 16, radius = 6, style }: {
  width: number | `${number}%`;
  height?: number;
  radius?: number;
  style?: ViewStyle;
}) {
  const pulse = useRef(new Animated.Value(0.55)).current;
  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 700, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0.55, duration: 700, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [pulse]);
  return (
    <Animated.View
      accessibilityElementsHidden
      importantForAccessibility="no"
      style={[styles.bone, { width, height, borderRadius: radius, opacity: pulse }, style]}
    />
  );
}

/** Trei rânduri-schelet într-un card, pentru o listă care n-a venit încă. */
export function SkeletonRows({ rows = 3 }: { rows?: number }) {
  return (
    <View style={styles.rows}>
      {Array.from({ length: rows }, (_, i) => (
        <View key={i} style={[styles.row, i > 0 && styles.sep]}>
          <Skeleton width={40} height={40} radius={11} />
          <View style={styles.text}>
            <Skeleton width="70%" height={14} />
            <Skeleton width="45%" height={11} />
          </View>
          <Skeleton width={54} height={14} />
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  bone: { backgroundColor: "#DDE2DE" },
  rows: { paddingVertical: 2 },
  row: { flexDirection: "row", alignItems: "center", gap: 12, paddingHorizontal: 14, minHeight: 62 },
  sep: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: "rgba(17,21,19,0.09)" },
  text: { flex: 1, gap: 7 },
});
