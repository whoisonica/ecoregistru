import { strings } from "@web/strings";
import { BlurView } from "expo-blur";
import { LinearGradient } from "expo-linear-gradient";
import { Slot, usePathname, useRouter, type Href } from "expo-router";
import { type MovementScreen } from "@/lib/movementScreens";
import { Platform, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { canWrite } from "../../src/auth";
import { SCREEN_LABEL, useCompany, useMovementScreens } from "../../src/company";
import { Icon, type IconName } from "../../src/components/Icon";
import { useSession } from "../../src/session";
import { colors, fonts } from "../../src/theme";

type Tab = { href: Href; path: string; label: string; icon: IconName };

/**
 * Bara de jos, cinci locuri, ca în prototipul aprobat: Acasă · Mișcări · „+” · Termene · Control.
 *
 * <p>Locul „Mișcări” poartă eticheta primului ecran al firmei — „Generare” la generator, „Intrări”
 * la colector —, citită din `@/lib/movementScreens`, aceeași regulă ca pe web. Când firma are mai
 * multe (un „generator și colector” are trei), restul stau pe comutatorul din capul ecranului, nu în
 * bară: șapte locuri n-ar încăpea, iar ce ar fi ieșit afară e „Control”, adică ecranul pentru care
 * se scoate telefonul când vine Garda.
 *
 * <p>Cât timp tipul firmei nu se știe (consultant fără firmă aleasă, cerere în drum), locul poartă
 * eticheta neutră „Mișcări”: o filă ghicită ar duce omul pe ecranul altei firme.
 */
const HOME: Tab = { href: "/", path: "/", label: strings.mobile.tabHome, icon: "home" };
const RIGHT: Tab[] = [
  { href: "/termene", path: "/termene", label: strings.nav.deadlines, icon: "clock" },
  { href: "/control", path: "/control", label: strings.mobile.tabControl, icon: "shield" },
];
const MOVEMENT_ICON: Record<MovementScreen, IconName> = { GENERATED: "list", IN: "in", OUT: "out" };

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
  const company = useCompany();
  const screens = useMovementScreens();
  const pathname = usePathname();
  const router = useRouter();
  const insets = useSafeAreaInsets();

  // Eticheta numește ecranul numai când firma are unul singur. Cu mai multe ar fi mințit: bara ar
  // fi scris „Generare" în timp ce omul stă pe „Intrări", fiindcă acolo se schimbă, nu aici.
  const only = screens.length === 1 ? screens[0] : undefined;
  const left: Tab[] = [
    HOME,
    {
      href: "/miscari",
      path: "/miscari",
      label: only ? SCREEN_LABEL[only] : strings.nav.movements,
      icon: only ? MOVEMENT_ICON[only] : "list",
    },
  ];

  // Colectorul lucrează pe intrări, deci „+” îi e albastru, ca pe ecranul de intrare din prototip.
  const collector = company.data?.type === "COLLECTOR";
  const plusColors: [string, string] = collector ? [colors.blueHi, colors.blue] : [colors.greenHi, colors.green];

  const item = (tab: Tab) => {
    const active = pathname === tab.path;
    const tint = active ? (collector ? colors.blue : colors.greenText) : colors.ink3;
    return (
      <Pressable
        key={tab.path}
        testID={`tab-${tab.path.replace("/", "") || "acasa"}`}
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
      {left.map(item)}
      {/* VIEWER nu vede „+” (todo-mobil §6): locul rămâne gol, ca celelalte să nu sară. */}
      {canWrite(session?.role) ? (
        <Pressable style={styles.item} onPress={() => router.replace("/adauga")} accessibilityRole="button">
          <LinearGradient colors={plusColors} style={[styles.plus, collector && styles.plusBlue]}>
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
  plusBlue: { shadowColor: colors.blue },
});
