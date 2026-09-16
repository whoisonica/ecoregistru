import { useQuery } from "@tanstack/react-query";
import { binFor } from "@/lib/binColor";
import { directionOf, registerOf, type MovementScreen } from "@/lib/movementScreens";
import { strings } from "@web/strings";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { movements, movementTotals, UnauthorizedError } from "../api";
import { formatDate, formatKg } from "../format";
import { SCREEN_LABEL } from "../company";
import { useSession } from "../session";
import { binColors, colors, fonts, radius } from "../theme";
import { GraphiteHeader } from "./GraphiteHeader";
import { Chip, Group, Note, rowStyles, SectionHead } from "./Rows";
import { Lcd } from "./Lcd";
import { MonthArrows } from "./MonthArrows";

/**
 * Mișcările unei luni, pe o direcție — ecranul „Generare” al generatorului și „Intrări”/„Ieșiri”
 * ale colectorului. Același ecran, alt filtru: pe web sunt trei rute, aici trei intrări în bara de jos.
 *
 * <p>Cifra de sus vine de la `/movements/totals`, nu din rândurile aduse: pagina nu e luna, iar un
 * total adunat din 50 de rânduri ar fi mai mic decât adevărul fără să spună că e mai mic.
 */
export function MovementList({ title, screen, tabs }: {
  title: string;
  /** Ecranul al cărui filtru se aplică. Lipsește cât timp tipul firmei nu se știe. */
  screen?: MovementScreen;
  /** Comutatorul dintre ecranele firmei. Lipsește când firma are unul singur. */
  tabs?: { screens: MovementScreen[]; current: MovementScreen; onPick: (s: MovementScreen) => void };
}) {
  const { session, auth, signOut } = useSession();
  const now = new Date();
  const [cursor, setCursor] = useState({ year: now.getFullYear(), month: now.getMonth() + 1 });

  // Registrul și direcția vin din același loc ca pe web, ca ecranul să numere ce arată.
  const params = {
    ...cursor,
    register: screen ? registerOf(screen) : undefined,
    direction: screen ? directionOf(screen) : undefined,
  };
  const enabled = !!auth && !!session?.tenantId && !!screen;
  const key = [session?.tenantId, cursor.year, cursor.month, screen ?? "?"];

  const list = useQuery({
    queryKey: ["movements", "list", ...key],
    queryFn: () => movements(auth!, params),
    enabled,
  });
  const totals = useQuery({
    queryKey: ["movements", "totals", ...key],
    queryFn: () => movementTotals(auth!, params),
    enabled,
  });

  useEffect(() => {
    if (list.error instanceof UnauthorizedError || totals.error instanceof UnauthorizedError) signOut();
  }, [list.error, totals.error, signOut]);

  const monthName = strings.months[cursor.month - 1];
  const label = cursor.year === now.getFullYear() ? monthName : `${monthName} ${cursor.year}`;

  // Rândul de sub cifră spune ce mai lipsește din lună, nu doar câte rânduri are.
  let foot: string | undefined;
  let footTone: "ok" | "alert" = "ok";
  if (!session?.tenantId) {
    foot = strings.mobile.noCompanyYet;
    footTone = "alert";
  } else if (totals.isError) {
    foot = strings.mobile.lcdError;
    footTone = "alert";
  } else if (totals.data) {
    const gaps: string[] = [];
    if (totals.data.missingOperationCode > 0)
      gaps.push(`${totals.data.missingOperationCode} ${strings.mobile.missingOperationCode}`);
    if (totals.data.awaitingWeighing > 0)
      gaps.push(`${totals.data.awaitingWeighing} ${strings.mobile.awaitingWeighing}`);
    foot = gaps.length ? gaps.join(" · ") : strings.mobile.lcdMovements(totals.data.rows);
    footTone = gaps.length ? "alert" : "ok";
  }

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <GraphiteHeader title={title} meta={(session?.tenantName ?? strings.appName).toUpperCase()}>
        <Lcd
          label={lcdLabelFor(params.direction)(label)}
          state={strings.mobile.lcdState}
          value={totals.data ? formatKg(totals.data.quantityKg) : totals.isPending && enabled ? "" : null}
          unit="kg"
          foot={foot}
          footTone={footTone}
          footRight={<MonthArrows cursor={cursor} onChange={setCursor} />}
        />
      </GraphiteHeader>

      <View style={styles.sheet}>
        {tabs ? (
          <View style={styles.tabs}>
            {tabs.screens.map((s) => {
              const on = s === tabs.current;
              return (
                <Pressable
                  key={s}
                  testID={`screen-${s}`}
                  onPress={() => tabs.onPick(s)}
                  style={[styles.tab, on && styles.tabOn]}
                  accessibilityRole="tab"
                  accessibilityState={{ selected: on }}
                >
                  <Text style={[styles.tabText, on && styles.tabTextOn]} numberOfLines={1}>
                    {SCREEN_LABEL[s]}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        ) : null}
        <SectionHead>{strings.mobile.monthMovements}</SectionHead>
        <Group>
          {list.isError ? (
            <Note tone="alert">{strings.mobile.movementsError}</Note>
          ) : !list.data ? (
            <Note>{" "}</Note>
          ) : list.data.content.length === 0 ? (
            <Note>{strings.mobile.movementsEmpty}</Note>
          ) : (
            list.data.content.map((m, i) => (
              <View key={m.id} testID="movement-row" style={[rowStyles.row, i > 0 && rowStyles.sep, styles.row]}>
                {/* Pubela pe codul de deșeu, ca pe web (`lib/binColor.ts`) — aceeași listă explicită.
                    Un cod care nu e în ea nu primește un pătrat gol: locul rămâne liber. */}
                <Bin code={m.wasteCode} hazardous={m.hazardous} />
                <View style={styles.rowBody}>
                  <Text style={rowStyles.title} numberOfLines={1}>{m.wasteCodeName}</Text>
                  <Text style={rowStyles.sub} numberOfLines={1}>
                    {m.wasteCode} · {formatDate(m.date)}
                    {m.partnerName ? ` · ${m.partnerName}` : ""}
                  </Text>
                </View>
                <View style={styles.rowEnd}>
                  {m.quantity == null ? (
                    <Chip label={strings.mobile.awaitingWeighing} tone="warn" />
                  ) : (
                    <Text style={rowStyles.mono}>
                      {formatKg(m.quantity)} {strings.enums.unit[m.unit]}
                    </Text>
                  )}
                </View>
              </View>
            ))
          )}
        </Group>
      </View>
    </ScrollView>
  );
}

/** Pătrățelul pubelei. Nimic pentru un cod din afara listei — un pătrat gol ar fi o afirmație. */
function Bin({ code, hazardous }: { code: string; hazardous: boolean }) {
  const bin = binFor(code, hazardous);
  return bin ? <View style={[styles.bin, { backgroundColor: binColors[bin] }]} /> : <View style={styles.binGap} />;
}

/**
 * Eticheta afișajului urmează filtrul ecranului, nu ecranul.
 *
 * <p>Altfel aceeași cifră primea două nume: Acasă spunea „PREDAT" (cere `direction=OUT`), iar
 * „Generare" spunea „ÎNREGISTRAT" peste exact același total, fiindcă la un generator pur tot ce e în
 * Anexa 1 a și plecat. Pe „Generare" nu e o direcție, deci „înregistrat" e cuvântul corect acolo.
 */
function lcdLabelFor(direction?: "IN" | "OUT") {
  if (direction === "OUT") return strings.mobile.lcdLabelOut;
  if (direction === "IN") return strings.mobile.lcdLabelIn;
  return strings.mobile.lcdLabel;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  sheet: {
    backgroundColor: colors.ground,
    borderTopLeftRadius: radius.sheet,
    borderTopRightRadius: radius.sheet,
    marginTop: -20,
    paddingHorizontal: 16,
    paddingTop: 20,
    gap: 8,
  },
  tabs: { flexDirection: "row", backgroundColor: "#E2E6E3", borderRadius: 12, padding: 3, gap: 3 },
  tab: { flex: 1, alignItems: "center", justifyContent: "center", paddingVertical: 9, borderRadius: 9 },
  tabOn: { backgroundColor: colors.card },
  tabText: { fontFamily: fonts.sansMedium, fontSize: 14, color: colors.ink2 },
  tabTextOn: { color: colors.ink },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  bin: { width: 10, height: 13, borderRadius: 2 },
  binGap: { width: 10 },
  rowBody: { flex: 1 },
  rowEnd: { alignItems: "flex-end" },
});
