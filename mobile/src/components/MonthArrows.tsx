import { strings } from "@web/strings";
import { Pressable, StyleSheet, View } from "react-native";

import { colors } from "../theme";
import { Icon } from "./Icon";

type Cursor = { year: number; month: number };

/** Săgețile de lună de pe rândul de jos al afișajului. Luna următoare e stinsă: viitorul n-are cifre. */
export function MonthArrows({ cursor, onChange }: { cursor: Cursor; onChange: (c: Cursor) => void }) {
  const now = new Date();
  const isCurrent = cursor.year === now.getFullYear() && cursor.month === now.getMonth() + 1;
  const shift = (delta: number) => {
    const d = new Date(cursor.year, cursor.month - 1 + delta, 1);
    onChange({ year: d.getFullYear(), month: d.getMonth() + 1 });
  };
  return (
    <View style={styles.arrows}>
      <Arrow name="left" label={strings.mobile.previousMonth} onPress={() => shift(-1)} />
      <Arrow name="right" label={strings.mobile.nextMonth} onPress={() => shift(1)} disabled={isCurrent} />
    </View>
  );
}

/** Aceleași săgeți, pe an. Anul următor e stins, ca luna următoare. */
export function YearArrows({ year, onChange }: { year: number; onChange: (y: number) => void }) {
  return (
    <View style={styles.arrows}>
      <Arrow name="left" label={strings.mobile.previousYear} onPress={() => onChange(year - 1)} />
      <Arrow
        name="right"
        label={strings.mobile.nextYear}
        onPress={() => onChange(year + 1)}
        disabled={year >= new Date().getFullYear()}
      />
    </View>
  );
}

function Arrow({ name, label, onPress, disabled }: {
  name: "left" | "right";
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityLabel={label}
      hitSlop={8}
      style={[styles.arrow, disabled && { opacity: 0.25 }]}
    >
      <Icon name={name} size={20} color={colors.lcdUnit} strokeWidth={2.2} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  arrows: { flexDirection: "row", gap: 6 },
  arrow: {
    width: 34,
    height: 34,
    borderRadius: 10,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(124,242,169,0.06)",
  },
});
