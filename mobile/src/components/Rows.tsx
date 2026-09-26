import { StyleSheet, Text, View, type ViewStyle } from "react-native";

import { colors, fonts, radius } from "../theme";

/** Titlul unei grupe de rânduri, ca în prototip: majuscule mici, deasupra cardului alb. */
export function SectionHead({ children }: { children: string }) {
  return <Text style={styles.head}>{children}</Text>;
}

/** Cardul alb cu colțuri de 16 în care stau rândurile. */
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

/** Eticheta de stare din §6: pătrățel colorat + cuvânt, ca LED-urile de pe web. */
export function Chip({ label, tone }: { label: string; tone: "ok" | "warn" | "bad" | "quiet" }) {
  const t = TONES[tone];
  return (
    <View style={[styles.chip, { backgroundColor: t.bg }]}>
      <View style={[styles.dot, { backgroundColor: t.fg }]} />
      <Text style={[styles.chipText, { color: t.fg }]}>{label}</Text>
    </View>
  );
}

const TONES = {
  ok: { bg: colors.greenSoft, fg: colors.greenText },
  warn: { bg: colors.amberSoft, fg: colors.amberText },
  bad: { bg: colors.redSoft, fg: colors.redText },
  quiet: { bg: "#EDEFEE", fg: colors.ink2 },
} as const;

export const rowStyles = StyleSheet.create({
  row: { minHeight: 62, paddingHorizontal: 16, paddingVertical: 10, justifyContent: "center" },
  sep: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  pressed: { backgroundColor: "#F3F5F3" },
  title: { fontFamily: fonts.sansMedium, fontSize: 16, color: colors.ink },
  sub: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.ink2, marginTop: 2 },
  mono: { fontFamily: fonts.monoMedium, fontSize: 15, color: colors.ink },
});

const styles = StyleSheet.create({
  head: {
    fontFamily: fonts.sansSemiBold,
    fontSize: 13,
    letterSpacing: 0.3,
    color: colors.ink2,
    textTransform: "uppercase",
    paddingHorizontal: 4,
  },
  group: { backgroundColor: colors.card, borderRadius: radius.group, overflow: "hidden" },
  note: { paddingHorizontal: 16, paddingVertical: 18 },
  noteText: { fontFamily: fonts.sans, fontSize: 15, color: colors.ink2 },
  chip: { flexDirection: "row", alignItems: "center", gap: 6, borderRadius: 8, paddingHorizontal: 8, paddingVertical: 4 },
  dot: { width: 7, height: 7, borderRadius: 2 },
  chipText: { fontFamily: fonts.sansMedium, fontSize: 12.5 },
});
