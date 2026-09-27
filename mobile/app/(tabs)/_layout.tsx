import { strings } from "@web/strings";
import { BlurView } from "expo-blur";
import { Tabs } from "expo-router";
import { type MovementScreen } from "@/lib/movementScreens";
import { Platform, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { canWrite } from "../../src/auth";
import { haptic } from "../../src/haptics";
import { SCREEN_LABEL, useCompany, useMovementScreens } from "../../src/company";
import { Icon, type IconName } from "../../src/components/Icon";
import { useSession } from "../../src/session";
import { useNotificationRoutes, usePushRegistration } from "../../src/push";
import { useOutboxSync } from "../../src/sync";
import { colors, fonts } from "../../src/theme";

type Tab = { name: string; label: string; icon: IconName };

/** Ce dă `Tabs` barei ei: care ecran e deschis și cum se sare la altul. Doar cât folosim. */
interface BarProps {
  state: { index: number; routes: { key: string; name: string }[] };
  navigation: { navigate: (name: string) => void };
}

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
 *
 * <p>Din 27.09.2026 bara e `Tabs` din Expo Router, nu `Slot` + `router.replace`: fiecare ecran își
 * ține starea (luna aleasă, tabul „Ambalaje”, poziția în listă) când omul trece pe altul și se
 * întoarce. Bara desenată rămâne a noastră, cu „+” ridicat, prin `tabBar`.
 */
const HOME: Tab = { name: "index", label: strings.mobile.tabHome, icon: "home" };
const RIGHT: Tab[] = [
  { name: "termene", label: strings.nav.deadlines, icon: "clock" },
  { name: "control", label: strings.mobile.tabControl, icon: "shield" },
];
const MOVEMENT_ICON: Record<MovementScreen, IconName> = { GENERATED: "list", IN: "in", OUT: "out" };

export default function TabsLayout() {
  // Coada de predări pleacă de oriunde ar fi omul în aplicație, nu doar de pe „Adaugă”.
  useOutboxSync();
  // G2 — tokenul de push pe sesiunea telefonului, și ecranul deschis de o notificare apăsată.
  usePushRegistration();
  useNotificationRoutes();
  return (
    <Tabs
      screenOptions={{ headerShown: false, sceneStyle: styles.fill, lazy: true }}
      tabBar={(props) => <TabBar {...(props as unknown as BarProps)} />}
    >
      <Tabs.Screen name="index" />
      <Tabs.Screen name="miscari" />
      <Tabs.Screen name="adauga" />
      <Tabs.Screen name="termene" />
      <Tabs.Screen name="control" />
    </Tabs>
  );
}

function TabBar({ state, navigation }: BarProps) {
  const { session } = useSession();
  const company = useCompany();
  const screens = useMovementScreens();
  const current = state.routes[state.index]?.name;
  const insets = useSafeAreaInsets();
  const go = (name: string) => {
    if (name !== current) haptic.tap();
    navigation.navigate(name);
  };

  // Eticheta numește ecranul numai când firma are unul singur. Cu mai multe ar fi mințit: bara ar
  // fi scris „Generare" în timp ce omul stă pe „Intrări", fiindcă acolo se schimbă, nu aici.
  const only = screens.length === 1 ? screens[0] : undefined;
  const left: Tab[] = [
    HOME,
    {
      name: "miscari",
      label: only ? SCREEN_LABEL[only] : strings.nav.movements,
      icon: only ? MOVEMENT_ICON[only] : "list",
    },
  ];

  // Colectorul lucrează pe intrări, deci „+” îi e albastru, ca pe ecranul de intrare din prototip.
  const collector = company.data?.type === "COLLECTOR";
  const plusColor = collector ? colors.blue : colors.green;

  const item = (tab: Tab) => {
    const active = current === tab.name;
    const tint = active ? (collector ? colors.blue : colors.green) : colors.ink3;
    return (
      <Pressable
        key={tab.name}
        testID={`tab-${tab.name === "index" ? "acasa" : tab.name}`}
        style={styles.item}
        onPress={() => go(tab.name)}
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
        <Pressable
          testID="tab-adauga"
          style={styles.item}
          onPress={() => go("adauga")}
          accessibilityRole="button"
        >
          <View style={[styles.plus, { backgroundColor: plusColor }]}>
            <Icon name="plus" size={22} color={colors.onAccent} strokeWidth={2.4} />
          </View>
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
    <BlurView intensity={40} tint="light" style={styles.bar}>
      {content}
    </BlurView>
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
    backgroundColor: "rgba(255,255,255,0.86)",
  },
  barAndroid: { backgroundColor: "rgba(255,255,255,0.97)" },
  row: { flexDirection: "row", paddingTop: 8, paddingHorizontal: 8 },
  item: { flex: 1, alignItems: "center", justifyContent: "flex-end", gap: 3 },
  label: { fontFamily: fonts.sansMedium, fontSize: 10.5 },
  // Plat, fără umbră colorată (paleta A, 27.09.2026).
  plus: { width: 50, height: 40, borderRadius: 13, alignItems: "center", justifyContent: "center" },
});
