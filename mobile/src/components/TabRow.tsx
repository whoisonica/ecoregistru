import { useRef } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { colors, fonts } from "../theme";

export interface TabItem {
  id: string;
  label: string;
}

/**
 * Taburile unui ecran, **toate pe același rând** (proprietarul, 26.09.2026) — numele și ordinea vin
 * de pe web (`lib/screenTabs.ts`), nu se scriu aici. Până la trei stau cât lățimea; de la patru
 * rândul se derulează, în loc să se rupă pe două rânduri sau să taie etichetele.
 *
 * <p>Fiecare tab e alt ecran, deci rândul se montează din nou la fiecare apăsare și ar porni de la
 * stânga — „Ieșiri”, ales, ar fi ieșit din vedere. De aceea se derulează singur până la tabul ales.
 */
export function TabRow({ tabs, selected, onSelect }: {
  tabs: TabItem[];
  selected: string;
  onSelect: (id: string) => void;
}) {
  const fits = tabs.length <= 3;
  const scroller = useRef<ScrollView>(null);
  const row = tabs.map((tab) => {
    const on = tab.id === selected;
    return (
      <Pressable
        key={tab.id || "default"}
        testID={`st-${tab.id || "default"}`}
        onPress={() => onSelect(tab.id)}
        style={[styles.tab, fits && styles.tabFit, on && styles.tabOn]}
        accessibilityRole="tab"
        accessibilityState={{ selected: on }}
        onLayout={
          on && !fits
            ? (ev) => scroller.current?.scrollTo({ x: Math.max(0, ev.nativeEvent.layout.x - 24), animated: false })
            : undefined
        }
      >
        <Text style={[styles.text, on && styles.textOn]} numberOfLines={1}>
          {tab.label}
        </Text>
      </Pressable>
    );
  });
  return fits ? (
    <View style={styles.track} accessibilityRole="tablist">
      {row}
    </View>
  ) : (
    <ScrollView
      ref={scroller}
      horizontal
      showsHorizontalScrollIndicator={false}
      style={styles.scroll}
      contentContainerStyle={styles.track}
      accessibilityRole="tablist"
    >
      {row}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: { flexGrow: 0 },
  track: { flexDirection: "row", backgroundColor: "#E2E6E3", borderRadius: 12, padding: 3, gap: 3 },
  tab: { alignItems: "center", justifyContent: "center", paddingVertical: 9, paddingHorizontal: 14, borderRadius: 9 },
  tabFit: { flex: 1, paddingHorizontal: 6 },
  tabOn: { backgroundColor: colors.card },
  text: { fontFamily: fonts.sansMedium, fontSize: 14, color: colors.ink2 },
  textOn: { color: colors.ink },
});
