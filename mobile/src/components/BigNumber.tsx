import type { ReactNode } from "react";
import { StyleSheet, Text, View } from "react-native";

import { agoText } from "../agoText";
import { colors, fonts } from "../theme";
import { Skeleton } from "./Skeleton";

/**
 * Cifra ecranului ca tipografie mare, pe hârtie — locul afișajului LCD de dinainte de 27.09.2026
 * (proprietarul: „nu-mi place deloc partea de sus cu cantitatea”). „?” când n-a venit, niciodată „0”;
 * scheletul cât se încarcă prima dată; sub ea propoziția (ce lipsește din lună, câte rânduri) și
 * vârsta cifrei („actualizat acum 3 min”).
 */
export function BigNumber({ label, value, pending, unit = "kg", sub, subTone = "ok", updatedAt, right, testID = "big-value" }: {
  label: string;
  /** Formatată deja; `null` = n-a încărcat. */
  value: string | null;
  /** Se încarcă prima dată (fără cifră veche de arătat). */
  pending?: boolean;
  unit?: string;
  sub?: string;
  subTone?: "ok" | "alert";
  /** `query.dataUpdatedAt`, pentru „actualizat acum …”. */
  updatedAt?: number;
  /** Săgețile de lună sau de an, pe rândul etichetei. */
  right?: ReactNode;
  testID?: string;
}) {
  const updated = updatedAt ? agoText(updatedAt) : null;
  const line = [sub, updated].filter(Boolean).join(" · ");
  return (
    <View style={styles.wrap}>
      <View style={styles.head}>
        <Text style={styles.label} numberOfLines={1}>{label.toUpperCase()}</Text>
        {right}
      </View>
      {pending && value == null ? (
        <Skeleton width={170} height={40} radius={8} />
      ) : (
        <Text style={styles.value} testID={testID}>
          {value ?? "?"}
          {unit ? <Text style={styles.unit}> {unit}</Text> : null}
        </Text>
      )}
      {line ? <Text style={[styles.sub, subTone === "alert" && styles.subAlert]}>{line}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { paddingHorizontal: 4, gap: 8, paddingBottom: 4 },
  head: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10, minHeight: 34 },
  label: { flexShrink: 1, fontFamily: fonts.sansSemiBold, fontSize: 13, letterSpacing: 0.3, color: colors.ink2 },
  value: { fontFamily: fonts.monoMedium, fontSize: 44, lineHeight: 48, letterSpacing: -1.3, color: colors.ink, fontVariant: ["tabular-nums"] },
  unit: { fontFamily: fonts.mono, fontSize: 18, color: colors.ink2, letterSpacing: 0 },
  sub: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2 },
  subAlert: { color: colors.amberText },
});
