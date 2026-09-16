import { strings } from "@web/strings";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Slot, usePathname, useRouter, type Href } from "expo-router";
import { Platform, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { canWrite } from "../../src/auth";
import { Icon, type IconName } from "../../src/components/Icon";
import { useSession } from "../../src/session";
import { colors, fonts } from "../../src/theme";

type Tab = { href: Href; path: string; label: string; icon: IconName };

// Bara generatorului din prototip. Varianta colectorului (Intrare albastru) vine cu tipul firmei, în M1a.
const LEFT: Tab[] = [
  { href: "/", path: "/", label: strings.mobile.tabHome, icon: "home" },
  { href: "/generare", path: "/generare", label: strings.nav.movementsGenerator, icon: "list" },
];
const RIGHT: Tab[] = [
  { href: "/termene", path: "/termene", label: strings.nav.deadlines, icon: "clock" },
  { href: "/control", path: "/control", label: strings.mobile.tabControl, icon: "shield" },
];

export default function TabsLayout() {
  return (
    <View style={styles.fill}>
      <Slot />
      <TabBar />
    </View>
  );
}

function TabBar() {
  const { session } = useSession();
  const pathname = usePathname();
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const item = (tab: Tab) => {
    const active = pathname === tab.path;
    const tint = active ? colors.greenText : colors.ink3;
    return (
      <Pressable
        key={tab.path}
        style={styles.item}
        onPress={() => router.replace(tab.href)}
        accessibilityRole="tab"
        accessibilityState={{ selected: active }}
      >
        <Icon name={tab.icon} size={25} color={tint} />
        <Text style={[styles.label, { color: tint }]}>{tab.label}</Text>
      </Pressable>
    );
  };

  const content = (
    <View style={[styles.row, { paddingBottom: Math.max(insets.bottom, 10) }]}>
      {LEFT.map(item)}
      {/* VIEWER nu vede „+” (todo-mobil §6): locul rămâne gol, ca celelalte să nu sară. */}
      {canWrite(session?.role) ? (
        <Pressable style={styles.item} onPress={() => router.replace("/adauga")} accessibilityRole="button">
          <LinearGradient colors={[colors.greenHi, colors.green]} style={styles.plus}>
            <Icon name="plus" size={24} color="#fff" strokeWidth={2.6} />
          </LinearGradient>
          <Text style={[styles.label, { color: colors.ink2 }]}>{strings.mobile.tabAdd}</Text>
        </Pressable>
      ) : (
        <View style={styles.item} />
      )}
      {RIGHT.map(item)}
    </View>
  );

  // Pe Android blurul nativ e scump și inegal; fundalul aproape opac arată la fel pe ecranele de azi.
  return Platform.OS === "ios" ? (
    <BlurView intensity={60} tint="light" style={styles.bar}>{content}</BlurView>
  ) : (
    <View style={[styles.bar, styles.barAndroid]}>{content}</View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  bar: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.separator,
    backgroundColor: "rgba(250,251,250,0.72)",
  },
  barAndroid: { backgroundColor: "rgba(250,251,250,0.97)" },
  row: { flexDirection: "row", paddingTop: 8, paddingHorizontal: 8 },
  item: { flex: 1, alignItems: "center", justifyContent: "flex-end", gap: 3 },
  label: { fontFamily: fonts.sansMedium, fontSize: 10.5 },
  plus: {
    width: 54,
    height: 44,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
    shadowColor: colors.green,
    shadowOpacity: 0.45,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 6 },
    elevation: 4,
  },
});
