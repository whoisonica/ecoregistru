import { strings } from "@web/strings";
import { useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { dayLine } from "../src/agoText";
import { useCompany } from "../src/company";
import type { CheckTone } from "../src/control";
import { colors, fonts } from "../src/theme";
import { useControl } from "../src/useControl";

const m = strings.mobile;

const DOT: Record<CheckTone | "loading", string> = {
  ok: colors.green,
  warn: colors.amber,
  bad: colors.red,
  unknown: colors.unknown,
  loading: colors.unknown,
};

/**
 * F10 (valul B) — modul inspector: telefonul întins peste masă, în fața inspectorului. Ecran întreg, alb,
 * litere mari: firma, CUI-ul, adresa, ziua și ora (vii — nu o captură de ieri), verdictul și cele patru
 * verificări. Aceleași date ca „A venit controlul” (`useControl`), niciun rând în plus.
 */
export default function InspectorScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const company = useCompany();
  const { checks, verdict, updated } = useControl();

  // Ora merge: inspectorul vede că ecranul e de acum.
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 15_000);
    return () => clearInterval(t);
  }, []);
  const time = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

  const firm = company.data;
  return (
    <ScrollView style={styles.fill} contentContainerStyle={[styles.page, { paddingTop: insets.top + 12, paddingBottom: insets.bottom + 24 }]}>
      <View style={styles.top}>
        <Text style={styles.kicker}>{m.inspectorKicker.toUpperCase()}</Text>
        <Pressable onPress={() => router.back()} hitSlop={12} accessibilityRole="button" testID="inspector-close">
          <Text style={styles.close}>{m.inspectorClose}</Text>
        </Pressable>
      </View>

      <View style={styles.firm} testID="inspector">
        <Text style={styles.name}>{firm?.name ?? "?"}</Text>
        {firm?.cui ? <Text style={styles.line}>{m.inspectorCui(firm.cui)}</Text> : null}
        {firm?.address ? <Text style={styles.line}>{firm.address}</Text> : null}
        <Text style={styles.line} testID="inspector-time">
          {dayLine(now)} {now.getFullYear()} · {time}
        </Text>
      </View>

      <View style={styles.verdict}>
        <View style={[styles.led, { backgroundColor: DOT[verdict.tone] }]} />
        <View style={styles.verdictBody}>
          <Text style={styles.word} testID="inspector-verdict">{verdict.word}</Text>
          <Text style={styles.why}>{verdict.why}</Text>
        </View>
      </View>

      <View style={styles.list}>
        {checks.map((c, i) => (
          <View key={c?.key ?? i} style={[styles.item, i > 0 && styles.itemSep]}>
            <View style={[styles.dot, { backgroundColor: DOT[c?.tone ?? "loading"] }]} />
            <View style={styles.itemBody}>
              <Text style={styles.itemTitle}>{c?.title ?? " "}</Text>
              <Text style={styles.itemDetail}>{c?.detail ?? "…"}</Text>
            </View>
          </View>
        ))}
      </View>

      {updated ? <Text style={styles.stamp}>{updated}</Text> : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.card },
  page: { paddingHorizontal: 24, gap: 28 },
  top: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  kicker: { fontFamily: fonts.mono, fontSize: 13, color: colors.ink2, letterSpacing: 0.5 },
  close: { fontFamily: fonts.sansSemiBold, fontSize: 17, color: colors.green },
  firm: { gap: 6 },
  name: { fontFamily: fonts.sansSemiBold, fontSize: 30, lineHeight: 36, color: colors.ink },
  line: { fontFamily: fonts.sans, fontSize: 18, lineHeight: 25, color: colors.ink2 },
  verdict: { flexDirection: "row", gap: 16, alignItems: "flex-start" },
  led: { width: 22, height: 22, borderRadius: 11, marginTop: 8 },
  verdictBody: { flex: 1, gap: 4 },
  word: { fontFamily: fonts.sansSemiBold, fontSize: 34, lineHeight: 40, color: colors.ink },
  why: { fontFamily: fonts.sans, fontSize: 18, lineHeight: 25, color: colors.ink2 },
  list: { borderTopWidth: 2, borderTopColor: colors.ink },
  item: { flexDirection: "row", gap: 14, paddingVertical: 16, alignItems: "flex-start" },
  itemSep: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  dot: { width: 14, height: 14, borderRadius: 7, marginTop: 7 },
  itemBody: { flex: 1, gap: 2 },
  itemTitle: { fontFamily: fonts.sansSemiBold, fontSize: 20, lineHeight: 27, color: colors.ink },
  itemDetail: { fontFamily: fonts.sans, fontSize: 17, lineHeight: 24, color: colors.ink2 },
  stamp: { fontFamily: fonts.mono, fontSize: 13, color: colors.ink3 },
});
