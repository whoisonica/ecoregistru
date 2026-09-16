import { LinearGradient } from "expo-linear-gradient";
import type { ReactNode } from "react";
import { StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { colors, fonts } from "../theme";
import { Logo } from "./Icon";

/** Antetul grafit: gradientul de sus în jos și lumina verde slabă din colțul din dreapta. */
export function GraphiteHeader({ title, meta, right, children }: {
  title: string;
  meta: string;
  right?: ReactNode;
  children?: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  return (
    <LinearGradient colors={[colors.graphiteTop, colors.graphiteBottom]} style={[styles.head, { paddingTop: insets.top + 8 }]}>
      <LinearGradient
        colors={["rgba(124,242,169,0.13)", "rgba(124,242,169,0)"]}
        start={{ x: 1, y: 0 }}
        end={{ x: 0.35, y: 0.6 }}
        style={StyleSheet.absoluteFill}
        pointerEvents="none"
      />
      <View style={styles.nav}>
        <View style={styles.firm}>
          <LinearGradient colors={["#2F3833", "#1A201C"]} style={styles.logo}>
            <Logo />
          </LinearGradient>
          <View style={styles.firmText}>
            <Text style={styles.title} numberOfLines={1}>{title}</Text>
            <Text style={styles.meta} numberOfLines={1}>{meta}</Text>
          </View>
        </View>
        {right}
      </View>
      {children}
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  head: { paddingHorizontal: 18, paddingBottom: 38 },
  nav: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", minHeight: 40, gap: 10 },
  firm: { flexDirection: "row", alignItems: "center", gap: 10, flexShrink: 1 },
  logo: {
    width: 36,
    height: 36,
    borderRadius: 10,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "rgba(255,255,255,0.14)",
  },
  firmText: { flexShrink: 1 },
  title: { fontFamily: fonts.sansSemiBold, fontSize: 16, color: "#fff", lineHeight: 20 },
  meta: { fontFamily: fonts.mono, fontSize: 10.5, letterSpacing: 0.6, color: colors.onDark3 },
});
