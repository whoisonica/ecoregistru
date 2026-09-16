import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { useEffect } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";

import { upcomingDeadlines, UnauthorizedError } from "../../src/api";
import { GraphiteHeader } from "../../src/components/GraphiteHeader";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { formatDate } from "../../src/format";
import { useSession } from "../../src/session";
import { colors, radius } from "../../src/theme";

/**
 * Termenele firmei: ce trebuie depus și până când. Aceleași date ca panoul web
 * (`GET /api/v1/deadlines`), sortate cum vine omul la ele — restanțele întâi.
 *
 * <p>Bifarea unui termen (`POST /{id}/complete`) nu e aici: pe teren nu se închide o declarație,
 * se află că e deschisă. Rămâne pe web până cere cineva altceva.
 */
export default function TermeneScreen() {
  const { session, auth, signOut } = useSession();
  // Termenele anului în curs și ale celui următor: acolo stă des următorul termen al fiecărui fel.
  const query = useQuery({
    queryKey: ["deadlines", session?.tenantId, "upcoming"],
    queryFn: () => upcomingDeadlines(auth!),
    enabled: !!auth && !!session?.tenantId,
  });

  useEffect(() => {
    if (query.error instanceof UnauthorizedError) signOut();
  }, [query.error, signOut]);

  const rows = query.data
    ? [...query.data].sort((a, b) => RANK[a.status] - RANK[b.status] || a.dueDate.localeCompare(b.dueDate))
    : null;

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <GraphiteHeader
        title={strings.nav.deadlines}
        meta={(session?.tenantName ?? strings.appName).toUpperCase()}
      />
      <View style={styles.sheet}>
        <SectionHead>{strings.nav.deadlines}</SectionHead>
        <Group>
          {!session?.tenantId ? (
            <Note tone="alert">{strings.mobile.noCompanyYet}</Note>
          ) : query.isError ? (
            <Note tone="alert">{strings.mobile.deadlinesError}</Note>
          ) : !rows ? (
            <Note>{" "}</Note>
          ) : rows.length === 0 ? (
            <Note>{strings.mobile.deadlinesEmpty}</Note>
          ) : (
            rows.map((d, i) => (
              <View key={d.id} testID="deadline-row" style={[rowStyles.row, i > 0 && rowStyles.sep, styles.row]}>
                <View style={styles.body}>
                  <Text style={rowStyles.title} numberOfLines={2}>{strings.enums.reportType[d.reportType]}</Text>
                  <Text style={rowStyles.sub}>
                    {d.completedAt
                      ? strings.mobile.deadlineDone(formatDate(d.completedAt))
                      : strings.mobile.deadlineDue(formatDate(d.dueDate))}
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
  sheet: {
    backgroundColor: colors.ground,
    borderTopLeftRadius: radius.sheet,
    borderTopRightRadius: radius.sheet,
    marginTop: -20,
    paddingHorizontal: 16,
    paddingTop: 20,
    gap: 8,
  },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  body: { flex: 1 },
});
