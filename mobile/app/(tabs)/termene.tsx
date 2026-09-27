import { useQuery } from "@tanstack/react-query";
import { DEADLINE_TABS } from "@/lib/screenTabs";
import { strings } from "@web/strings";
import type { Deadline } from "@web/types";
import { daysLabel } from "@/lib/deadlines";
import { useCallback, useEffect, useState } from "react";
import { RefreshControl, ScrollView, StyleSheet, Text, View } from "react-native";

import { deadlines, pastDeadlines, upcomingDeadlines, UnauthorizedError } from "../../src/api";
import { LightHead } from "../../src/components/LightHead";
import { YearArrows } from "../../src/components/MonthArrows";
import { OfflineBand } from "../../src/components/OfflineBand";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { SkeletonRows } from "../../src/components/Skeleton";
import { TabRow } from "../../src/components/TabRow";
import { formatDate } from "../../src/format";
import { useSession } from "../../src/session";
import { colors } from "../../src/theme";

const t = strings.deadlines;

/**
 * Termenele firmei, pe taburile de pe web (`DEADLINE_TABS`, 17.09.2026): **De făcut** (următorul termen
 * al fiecărui fel, restanțele întâi) · **Bifate** (pe an) · **Trecute** (anul în curs până ieri, bifate
 * sau nu). Aceleași cereri ca `hooks/useDeadlines.ts`.
 *
 * <p>Bifarea unui termen (`POST /{id}/complete`) nu e aici: pe teren nu se închide o declarație, se află
 * că e deschisă. Rămâne pe web până cere cineva altceva.
 */
export default function TermeneScreen() {
  const { session, auth, signOut } = useSession();
  const [tab, setTab] = useState("");
  const enabled = !!auth && !!session?.tenantId;
  const currentYear = new Date().getFullYear();

  // Anul în curs, cel următor și cel trecut: acolo stă des următorul termen al fiecărui fel.
  const upcoming = useQuery({
    queryKey: ["deadlines", session?.tenantId, "upcoming"],
    queryFn: () => upcomingDeadlines(auth!),
    enabled,
  });
  // BUG-059, ca pe web: „Bifate” pornește pe anul celui mai recent termen bifat — un 15 martie bifat
  // acum stă în anul următor, iar pe anul curent tabul ar fi părut gol.
  const latestDoneYear = (upcoming.data ?? [])
    .filter((d) => d.status === "DONE" && d.dueDate)
    .reduce((y, d) => Math.max(y, Number(d.dueDate!.slice(0, 4))), currentYear);
  const [doneYear, setDoneYear] = useState<number | null>(null);
  const year = doneYear ?? latestDoneYear;
  const done = useQuery({
    queryKey: ["deadlines", session?.tenantId, year],
    queryFn: () => deadlines(auth!, year),
    enabled: enabled && tab === "bifate",
  });
  const past = useQuery({
    queryKey: ["deadlines", session?.tenantId, "past"],
    queryFn: () => pastDeadlines(auth!),
    enabled: enabled && tab === "trecute",
  });

  const active = tab === "bifate" ? done : tab === "trecute" ? past : upcoming;
  useEffect(() => {
    if (active.error instanceof UnauthorizedError) signOut();
  }, [active.error, signOut]);

  const rows: Deadline[] | null = !active.data
    ? null
    : tab === "bifate"
      ? active.data.filter((d) => d.status === "DONE")
      : tab === "trecute"
        ? active.data
        : [...active.data]
            .filter((d) => d.status !== "DONE")
            .sort((a, b) => RANK[a.status] - RANK[b.status] || (a.dueDate ?? "").localeCompare(b.dueDate ?? ""));
  // Propoziția de sub titlu: câte sunt de făcut și în câte zile e primul, din lista „De făcut”.
  const todo = (upcoming.data ?? [])
    .filter((d) => d.status !== "DONE")
    .sort((a, b) => RANK[a.status] - RANK[b.status] || (a.dueDate ?? "").localeCompare(b.dueDate ?? ""));
  const subtitle = upcoming.data ? strings.mobile.deadlinesSub(todo.length, todo[0] ? daysLabel(todo[0]) : null) : null;

  const [refreshing, setRefreshing] = useState(false);
  const refresh = useCallback(async () => {
    setRefreshing(true);
    await active.refetch().catch(() => {});
    setRefreshing(false);
  }, [active.refetch]); // eslint-disable-line react-hooks/exhaustive-deps

  const empty =
    tab === "bifate"
      ? t.doneEmpty.replace("{year}", String(year))
      : tab === "trecute"
        ? t.pastEmpty.replace("{year}", String(currentYear))
        : t.todoEmpty;

  return (
    <ScrollView
      style={styles.fill}
      contentContainerStyle={styles.scroll}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.ink2} />}
    >
      <LightHead title={strings.nav.deadlines} subtitle={subtitle} />
      <OfflineBand />
      <View style={styles.body}>
        <TabRow tabs={DEADLINE_TABS} selected={tab} onSelect={setTab} />
        {tab === "bifate" ? (
          <View style={styles.yearRow}>
            <SectionHead>{strings.mobile.doneInYear(year)}</SectionHead>
            <View style={styles.arrows}>
              <YearArrows year={year} onChange={setDoneYear} />
            </View>
          </View>
        ) : tab === "trecute" ? (
          <Text style={styles.hint}>{t.pastHint.replace("{year}", String(currentYear))}</Text>
        ) : null}
        <Group>
          {!session?.tenantId ? (
            <Note tone="alert">{strings.mobile.noCompanyYet}</Note>
          ) : active.isError ? (
            <Note tone="alert">{strings.mobile.deadlinesError}</Note>
          ) : !rows ? (
            <SkeletonRows />
          ) : rows.length === 0 ? (
            <Note>{empty}</Note>
          ) : (
            rows.map((d, i) => (
              <View key={d.id} testID="deadline-row" style={[rowStyles.row, i > 0 && rowStyles.sep, styles.row]}>
                <View style={styles.rowBody}>
                  <Text style={rowStyles.title} numberOfLines={2}>{strings.enums.reportType[d.reportType]}</Text>
                  <Text style={rowStyles.sub}>
                    {d.completedAt
                      ? strings.mobile.deadlineDone(formatDate(d.completedAt))
                      : d.dueDate
                        ? strings.mobile.deadlineDue(formatDate(d.dueDate))
                        : ""}
                  </Text>
                </View>
                <Chip label={strings.enums.deadlineStatus[d.status]} tone={TONE[d.status]} />
              </View>
            ))
          )}
        </Group>
      </View>
    </ScrollView>
  );
}

/** Restanțele întâi: pe telefon se deschide ecranul ca să se afle ce arde, nu ce e bifat. */
const RANK = { OVERDUE: 0, UPCOMING: 1, DONE: 2 } as const;
const TONE = { OVERDUE: "bad", UPCOMING: "warn", DONE: "ok" } as const;

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 8 },
  yearRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  arrows: {},
  hint: { fontSize: 13, color: colors.ink2, paddingHorizontal: 4 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },
});
