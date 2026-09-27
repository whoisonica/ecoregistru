import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import type { PackagingTable1Row } from "@web/types";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { RefreshControl, ScrollView, StyleSheet, Text, View } from "react-native";

import { packagingHandovers, packagingTable1, UnauthorizedError } from "../api";
import { formatKg } from "../format";
import { useSession } from "../session";
import { colors } from "../theme";
import { BigNumber } from "./BigNumber";
import { LightHead } from "./LightHead";
import { YearArrows } from "./MonthArrows";
import { OfflineBand } from "./OfflineBand";
import { Group, Note, rowStyles, SectionHead } from "./Rows";
import { SkeletonRows } from "./Skeleton";

const t = strings.packaging;
const m = strings.mobile;
const e = strings.enums;

/** Coloanele tabelului 1 care se arată pe telefon; reutilizabilele și conținutul periculos rămân pe web. */
const MARKET_COLUMNS = [
  ["salesPackaging", t.colSales],
  ["primaryTotal", t.colPrimary],
  ["secondaryTotal", t.colSecondary],
] as const;

/**
 * Tabul „Ambalaje” al ecranului „Generare” (webul, din 18.09.2026), de citit: ce s-a pus pe piață
 * (tabelul 1 al Anexei 1 Ambalaje) și ce s-a predat (tabelul 2), din aceleași cereri ca pe web.
 *
 * <p>Pe web tabelele stau pe o cheie proprie („Pus pe piață” · „Predat” · „Preluat de la alții”). Aici
 * stau unul sub altul: un al doilea rând de taburi sub cel al ecranului ar fi contrazis „toate pe
 * același rând”. „Preluat de la alții” (Anexa 3, doar la colectori) și suprascrierea cifrelor rămân pe web.
 *
 * <p>O celulă goală nu e zero (`table1Foot`): se arată „—”, nu „0”.
 */
export function PackagingSummary({ tabRow }: { tabRow: ReactNode }) {
  const { session, auth, signOut } = useSession();
  const [year, setYear] = useState(new Date().getFullYear());
  const enabled = !!auth && !!session?.tenantId;
  const market = useQuery({
    queryKey: ["packaging", "table1", session?.tenantId, year],
    queryFn: () => packagingTable1(auth!, year),
    enabled,
  });
  const handovers = useQuery({
    queryKey: ["packaging", "handovers", session?.tenantId, year],
    queryFn: () => packagingHandovers(auth!, year),
    enabled,
  });

  useEffect(() => {
    if (market.error instanceof UnauthorizedError || handovers.error instanceof UnauthorizedError) signOut();
  }, [market.error, handovers.error, signOut]);

  const handedOver = handovers.data?.reduce((sum, h) => sum + (h.quantity ?? 0), 0);
  const marketRows = market.data?.filter(hasFigures) ?? null;

  const [refreshing, setRefreshing] = useState(false);
  const refresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([market.refetch(), handovers.refetch()]).catch(() => {});
    setRefreshing(false);
  }, [market.refetch, handovers.refetch]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <ScrollView
      style={styles.fill}
      contentContainerStyle={styles.scroll}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.ink2} />}
    >
      <LightHead title={strings.movements.tabPackaging} subtitle={m.packagingSub} />
      <OfflineBand />
      <View style={styles.body}>
        {tabRow}
        <BigNumber
          label={m.yearPackaging(year)}
          value={handedOver != null ? formatKg(handedOver) : null}
          pending={handovers.isPending && enabled}
          sub={
            !session?.tenantId
              ? m.noCompanyYet
              : handovers.isError
                ? m.lcdError
                : handovers.data
                  ? m.packagingHandoverRows(handovers.data.length)
                  : undefined
          }
          subTone={!session?.tenantId || handovers.isError ? "alert" : "ok"}
          updatedAt={handovers.dataUpdatedAt}
          right={<YearArrows year={year} onChange={setYear} />}
          testID="packaging-kg"
        />

        <SectionHead>{t.keyHandedOver}</SectionHead>
        <Group>
          {handovers.isError ? (
            <Note tone="alert">{t.loadError}</Note>
          ) : !handovers.data ? (
            enabled ? <SkeletonRows rows={2} /> : <Note>{" "}</Note>
          ) : handovers.data.length === 0 ? (
            <Note>{t.noHandovers}</Note>
          ) : (
            handovers.data.map((h, i) => (
              <View
                key={`${h.material}-${h.operatorCui}-${h.operation}-${i}`}
                testID="packaging-handover"
                style={[rowStyles.row, i > 0 && rowStyles.sep, styles.line]}
              >
                <View style={styles.rowBody}>
                  <Text style={rowStyles.title}>{e.packagingMaterial[h.material]}</Text>
                  <Text style={rowStyles.sub} numberOfLines={2}>
                    {h.operatorName}
                    {h.operation ? ` · ${h.operation}` : ""}
                  </Text>
                </View>
                <Text style={rowStyles.mono}>{h.quantity != null ? `${formatKg(h.quantity)} kg` : "—"}</Text>
              </View>
            ))
          )}
        </Group>

        <SectionHead>{t.keyMarket}</SectionHead>
        <Group>
          {market.isError ? (
            <Note tone="alert">{t.loadError}</Note>
          ) : !marketRows ? (
            enabled ? <SkeletonRows rows={2} /> : <Note>{" "}</Note>
          ) : marketRows.length === 0 ? (
            <Note>{t.table1Empty.replace("{year}", String(year))}</Note>
          ) : (
            marketRows.map((row, i) => (
              <View key={row.material} testID="packaging-market" style={[rowStyles.row, i > 0 && rowStyles.sep]}>
                <Text style={rowStyles.title}>{e.packagingMaterial[row.material]}</Text>
                {MARKET_COLUMNS.map(([key, label]) => (
                  <View key={key} style={styles.line}>
                    <Text style={[rowStyles.sub, styles.rowBody]}>{label}</Text>
                    <Text style={rowStyles.mono}>{row[key] != null ? `${formatKg(row[key]!)} kg` : "—"}</Text>
                  </View>
                ))}
              </View>
            ))
          )}
        </Group>
        {marketRows && marketRows.length > 0 ? <Text style={styles.foot}>{t.table1Foot}</Text> : null}
      </View>
    </ScrollView>
  );
}

function hasFigures(row: PackagingTable1Row) {
  return MARKET_COLUMNS.some(([key]) => row[key] != null);
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 8 },
  line: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },
  foot: { fontSize: 12.5, color: colors.ink3, paddingHorizontal: 4 },
});
