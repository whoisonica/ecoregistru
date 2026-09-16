import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { movementSummary, UnauthorizedError } from "../../src/api";
import { isMultiCompany } from "../../src/auth";
import { GraphiteHeader } from "../../src/components/GraphiteHeader";
import { Icon } from "../../src/components/Icon";
import { Lcd } from "../../src/components/Lcd";
import { useSession } from "../../src/session";
import { colors, fonts, radius } from "../../src/theme";

export default function HomeScreen() {
  const { session, signOut } = useSession();
  const now = new Date();
  const [cursor, setCursor] = useState({ year: now.getFullYear(), month: now.getMonth() + 1 });
  const isCurrentMonth = cursor.year === now.getFullYear() && cursor.month === now.getMonth() + 1;

  // Consultantul și platforma n-au firmă în token; fără `X-Tenant-Id` serverul n-are ce număra (M1a).
  const noCompany = isMultiCompany(session?.role) && !session?.tenantId;

  const summary = useQuery({
    queryKey: ["movements", "summary", session?.tenantId, cursor.year, cursor.month],
    queryFn: () => movementSummary(session!.token, cursor.year, cursor.month),
    enabled: !!session && !noCompany,
  });

  useEffect(() => {
    if (summary.error instanceof UnauthorizedError) signOut();
  }, [summary.error, signOut]);

  const shift = (delta: number) =>
    setCursor(({ year, month }) => {
      const d = new Date(year, month - 1 + delta, 1);
      return { year: d.getFullYear(), month: d.getMonth() + 1 };
    });

  const monthName = strings.months[cursor.month - 1];
  const label = cursor.year === now.getFullYear() ? monthName : `${monthName} ${cursor.year}`;

  let foot: string | undefined;
  let footTone: "ok" | "alert" = "ok";
  if (noCompany) {
    foot = strings.mobile.pickCompanyOnWeb;
    footTone = "alert";
  } else if (summary.isError) {
    foot = strings.mobile.lcdError;
    footTone = "alert";
  } else if (summary.data) {
    foot = summary.data.movements === 0 ? strings.mobile.lcdEmpty : strings.mobile.lcdMovements(summary.data.movements);
  }

  const role = session ? strings.enums.role[session.role] : "";

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <GraphiteHeader
        title={session?.tenantName ?? session?.consultancyName ?? strings.appName}
        meta={role.toUpperCase()}
      >
        <Lcd
          label={strings.mobile.lcdLabel(label)}
          state={strings.mobile.lcdState}
          value={summary.data ? formatKg(summary.data.quantityKg) : summary.isPending && !noCompany ? "" : null}
          unit="kg"
          foot={foot}
          footTone={footTone}
          footRight={
            <View style={styles.arrows}>
              <Arrow name="left" label={strings.mobile.previousMonth} onPress={() => shift(-1)} />
              <Arrow name="right" label={strings.mobile.nextMonth} onPress={() => shift(1)} disabled={isCurrentMonth} />
            </View>
          }
        />
      </GraphiteHeader>

      <View style={styles.sheet}>
        <Text style={styles.secHead}>{strings.mobile.account}</Text>
        <View style={styles.group}>
          <View style={styles.row}>
            <Text style={styles.rowTitle} numberOfLines={1}>{session?.email}</Text>
            <Text style={styles.rowSub}>{role}</Text>
          </View>
          <Pressable
            testID="logout"
            style={({ pressed }) => [styles.row, styles.rowSep, pressed && styles.rowPressed]}
            onPress={signOut}
          >
            <Text style={[styles.rowTitle, { color: colors.redText }]}>{strings.nav.logout}</Text>
          </Pressable>
        </View>
      </View>
    </ScrollView>
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

/** Kilogramele în forma românească: 1.240 sau 12,5. Zecimala rămâne doar când există. */
function formatKg(kg: number) {
  const [int, dec] = (Math.round(kg * 10) / 10).toFixed(1).split(".");
  const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return dec === "0" ? grouped : `${grouped},${dec}`;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  arrows: { flexDirection: "row", gap: 6 },
  arrow: {
    width: 34,
    height: 34,
    borderRadius: 10,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(124,242,169,0.06)",
  },
  sheet: {
    backgroundColor: colors.ground,
    borderTopLeftRadius: radius.sheet,
    borderTopRightRadius: radius.sheet,
    marginTop: -20,
    paddingHorizontal: 16,
    paddingTop: 20,
    gap: 8,
  },
  secHead: {
    fontFamily: fonts.sansSemiBold,
    fontSize: 13,
    letterSpacing: 0.3,
    color: colors.ink2,
    textTransform: "uppercase",
    paddingHorizontal: 4,
  },
  group: { backgroundColor: colors.card, borderRadius: radius.group, overflow: "hidden" },
  row: { minHeight: 62, paddingHorizontal: 16, justifyContent: "center" },
  rowSep: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  rowPressed: { backgroundColor: "#F3F5F3" },
  rowTitle: { fontFamily: fonts.sansMedium, fontSize: 16, color: colors.ink },
  rowSub: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.ink2, marginTop: 1 },
});
