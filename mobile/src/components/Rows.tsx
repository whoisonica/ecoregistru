import { StyleSheet, Text, View, type ViewStyle } from "react-native";

import { colors, fonts, radius } from "../theme";

/** Titlul unei grupe de rânduri: litere mici, 13 px, gri — nu majuscule (paleta A, 27.09.2026). */
export function SectionHead({ children }: { children: string }) {
  return <Text style={styles.head}>{children}</Text>;
}

/** Cardul alb cu contur subțire și colțuri de 18 în care stau rândurile. */
export function Group({ children, style }: { children: React.ReactNode; style?: ViewStyle }) {
  return <View style={[styles.group, style]}>{children}</View>;
}

/** Ce se arată când o listă e goală sau n-a putut încărca — niciodată un zero inventat. */
export function Note({
  children,
  tone = "quiet",
  testID,
}: {
  children: string;
  tone?: "quiet" | "alert";
  testID?: string;
}) {
  return (
    <View style={styles.note} testID={testID}>
      <Text style={[styles.noteText, tone === "alert" && { color: colors.redText }]}>{children}</Text>
    </View>
  );
}

/** Pastila de stare: punct colorat + cuvânt, pe fond deschis. Singurul loc unde roșul/galbenul au fond. */
export function Chip({ label, tone }: { label: string; tone: "ok" | "warn" | "bad" | "quiet" }) {
  const t = TONES[tone];
  return (
    <View style={[styles.chip, { backgroundColor: t.bg }]}>
      <View style={[styles.dot, { backgroundColor: t.dot }]} />
      <Text style={[styles.chipText, { color: t.fg }]}>{label}</Text>
    </View>
  );
}

const TONES = {
  ok: { bg: colors.greenSoft, fg: colors.greenText, dot: colors.green },
  warn: { bg: colors.amberSoft, fg: colors.amberText, dot: colors.amberText },
  bad: { bg: colors.redSoft, fg: colors.redText, dot: colors.red },
  quiet: { bg: colors.quiet, fg: colors.ink2, dot: colors.ink3 },
} as const;

export const rowStyles = StyleSheet.create({
  row: { minHeight: 60, paddingHorizontal: 16, paddingVertical: 10, justifyContent: "center" },
  sep: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  pressed: { backgroundColor: colors.pressed },
  title: { fontFamily: fonts.sansMedium, fontSize: 15.5, color: colors.ink },
  sub: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink2, marginTop: 2 },
  mono: { fontFamily: fonts.monoMedium, fontSize: 15, color: colors.ink, fontVariant: ["tabular-nums"] },
});

const styles = StyleSheet.create({
  head: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.ink2, paddingHorizontal: 4 },
  group: {
    backgroundColor: colors.card,
    borderRadius: radius.group,
    borderWidth: 1,
    borderColor: colors.separator,
    overflow: "hidden",
  },
  note: { paddingHorizontal: 16, paddingVertical: 18 },
  noteText: { fontFamily: fonts.sans, fontSize: 15, color: colors.ink2 },
  chip: { flexDirection: "row", alignItems: "center", gap: 6, borderRadius: 999, paddingHorizontal: 9, paddingVertical: 4 },
  dot: { width: 6, height: 6, borderRadius: 3 },
  chipText: { fontFamily: fonts.sansMedium, fontSize: 12 },
});
