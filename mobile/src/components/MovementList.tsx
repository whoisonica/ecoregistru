import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { directionOf, registerOf, type MovementScreen } from "@/lib/movementScreens";
import { strings } from "@web/strings";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { router } from "expo-router";
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from "react-native";

import { movements, movementTotals, UnauthorizedError } from "../api";
import { formatDate, formatKg, formatQuantity } from "../format";
import { useSession } from "../session";
import { colors } from "../theme";
import { BigNumber } from "./BigNumber";
import { Bin } from "./Bin";
import { PrimaryButton } from "./Form";
import { LightHead } from "./LightHead";
import { MonthArrows } from "./MonthArrows";
import { OfflineBand } from "./OfflineBand";
import { Chip, Group, Note, rowStyles, SectionHead } from "./Rows";
import { SkeletonRows } from "./Skeleton";

/**
 * Mișcările unei luni, pe o direcție — ecranul „Generare” al generatorului și „Intrări”/„Ieșiri”
 * ale colectorului. Același ecran, alt filtru: pe web sunt trei rute, aici trei intrări în bara de jos.
 *
 * <p>Cifra de sus vine de la `/movements/totals`, nu din rândurile aduse: pagina nu e luna, iar un
 * total adunat din 50 de rânduri ar fi mai mic decât adevărul fără să spună că e mai mic.
 */
export function MovementList({ title, screen, tabRow }: {
  title: string;
  /** Ecranul al cărui filtru se aplică. Lipsește cât timp tipul firmei nu se știe. */
  screen?: MovementScreen;
  /** Rândul de taburi al ecranului (`TabRow`), deasupra listei. */
  tabRow?: ReactNode;
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

  // Câte 50; înainte lista se oprea la primele 50 fără să spună că mai sunt.
  const list = useInfiniteQuery({
    queryKey: ["movements", "list", ...key],
    queryFn: ({ pageParam }) => movements(auth!, params, pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    enabled,
  });
  const rows = list.data?.pages.flatMap((p) => p.content);
  const totalRows = list.data?.pages[0]?.totalElements ?? 0;
  const totals = useQuery({
    queryKey: ["movements", "totals", ...key],
    queryFn: () => movementTotals(auth!, params),
    enabled,
  });

  useEffect(() => {
    if (list.error instanceof UnauthorizedError || totals.error instanceof UnauthorizedError) signOut();
  }, [list.error, totals.error, signOut]);

  const [refreshing, setRefreshing] = useState(false);
  const refresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([list.refetch(), totals.refetch()]).catch(() => {});
    setRefreshing(false);
  }, [list.refetch, totals.refetch]); // eslint-disable-line react-hooks/exhaustive-deps

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
    <ScrollView
      style={styles.fill}
      contentContainerStyle={styles.scroll}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.ink2} />}
    >
      <LightHead title={title} subtitle={subtitleFor(params.direction)} />
      <OfflineBand />
      <View style={styles.body}>
        {tabRow}
        <BigNumber
          label={lcdLabelFor(params.direction)(label)}
          value={totals.data ? formatKg(totals.data.quantityKg) : null}
          pending={totals.isPending && enabled}
          sub={foot}
          subTone={footTone === "alert" ? "alert" : "ok"}
          updatedAt={totals.dataUpdatedAt}
          right={<MonthArrows cursor={cursor} onChange={setCursor} />}
          testID="month-kg"
        />
        <SectionHead>{strings.mobile.monthMovements}</SectionHead>
        <Group>
          {list.isError ? (
            <Note tone="alert">{strings.mobile.movementsError}</Note>
          ) : !rows ? (
            enabled ? <SkeletonRows /> : <Note>{" "}</Note>
          ) : rows.length === 0 ? (
            <Note>{strings.mobile.movementsEmpty}</Note>
          ) : (
            rows.map((m, i) => (
              // M1e: rândul deschide predarea, cu tot ce e în ea și cu Anexa 3 / avizul.
              <Pressable
                key={m.id}
                testID="movement-row"
                onPress={() => router.push({ pathname: "/miscare/[id]", params: { id: m.id } })}
                style={({ pressed }) => [rowStyles.row, i > 0 && rowStyles.sep, styles.row, pressed && rowStyles.pressed]}
              >
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
                      {formatQuantity(m.quantity, m.unit)} {strings.enums.unit[m.unit]}
                    </Text>
                  )}
                </View>
              </Pressable>
            ))
          )}
        </Group>
        {list.hasNextPage && rows ? (
          <PrimaryButton
            tone="quiet"
            label={strings.mobile.movementsMore(rows.length, totalRows)}
            onPress={() => list.fetchNextPage()}
            disabled={list.isFetchingNextPage}
            testID="movements-more"
          />
        ) : null}
      </View>
    </ScrollView>
  );
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

/** Propoziția de sub titlu: ce e ecranul, pe înțelesul cuiva care nu e specialist. */
function subtitleFor(direction?: "IN" | "OUT") {
  if (direction === "OUT") return strings.mobile.outSub;
  if (direction === "IN") return strings.mobile.inSub;
  return strings.mobile.generationSub;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 8 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },
  rowEnd: { alignItems: "flex-end" },
});
