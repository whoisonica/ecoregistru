import { useQuery } from "@tanstack/react-query";
import { byCode } from "@/lib/annualTotals";
import { binFor } from "@/lib/binColor";
import { daysLabel } from "@/lib/deadlines";
import { readiness } from "@/lib/readiness";
import { strings } from "@web/strings";
import { useRouter } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, View } from "react-native";

import { evidences, movementSummary, movementTotals, partners, recentHandovers, upcomingDeadlines, UnauthorizedError } from "../../src/api";
import { agoText, dayLine, greeting } from "../../src/agoText";
import { isMultiCompany } from "../../src/auth";
import { useMovementScreens } from "../../src/company";
import { Icon } from "../../src/components/Icon";
import { Key } from "../../src/components/Key";
import { LightHead } from "../../src/components/LightHead";
import { OfflineBand } from "../../src/components/OfflineBand";
import { Chip, Group, Note, rowStyles } from "../../src/components/Rows";
import { Skeleton } from "../../src/components/Skeleton";
import { StateCard, verdictOf } from "../../src/components/StateCard";
import { Tile } from "../../src/components/Tile";
import { controlChecks } from "../../src/control";
import { formatDate, formatKg, formatQuantity } from "../../src/format";
import { haptic } from "../../src/haptics";
import { useOutbox } from "../../src/outbox";
import { pickAvizPhoto } from "../../src/photo";
import { useSession } from "../../src/session";
import { binColors, colors, fonts, radius } from "../../src/theme";

const m = strings.mobile;

/**
 * Acasă, în patru blocuri (proprietarul, 27.09.2026 seara: „Acasă parcă e prea plin”): starea firmei
 * (`StateCard`, cu cele patru verificări ca puncte → Control), tasta „Pozează avizul”, luna (cifra,
 * compoziția pe coduri, o propoziție → Generare) și două rânduri (ultima predare, următorul termen).
 * „Trimite dosarul” stă pe Control, unde e dosarul; anul stă pe Generare › Totalul anului. Rândul cozii
 * apare numai când e ceva de trimis sau refuzat. **Nimic țintuit sus**: capul e pe hârtie și se
 * derulează cu pagina.
 *
 * <p>Verdictul e aceeași socoteală ca Panoul web și ca „A venit controlul” (`src/control.ts`, prin
 * `lib/readiness.ts`): telefonul nu poate spune „în regulă” despre ceva ce webul numește „de rezolvat”.
 * Cât timp o listă n-a venit, cuvântul e „Nu știu încă”, nu „În regulă”.
 */
export default function HomeScreen() {
  const { session, auth, signOut } = useSession();
  const router = useRouter();
  const tenant = session?.tenantId;
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;

  // Consultantul și platforma aleg firma din comutator; până atunci serverul n-are ce număra.
  const noCompany = isMultiCompany(session?.role) && !tenant;
  const enabled = !!auth && !!tenant;
  const screens = useMovementScreens();
  const onlyGenerator = screens.length === 1 && screens[0] === "GENERATED";

  // Aceleași chei ca „A venit controlul”: o listă adusă aici e deja acolo.
  const deadlinesQ = useQuery({ queryKey: ["deadlines", tenant, "upcoming"], queryFn: () => upcomingDeadlines(auth!), enabled });
  const evidencesQ = useQuery({ queryKey: ["evidences", tenant, year], queryFn: () => evidences(auth!, year), enabled });
  const partnersQ = useQuery({ queryKey: ["control", "partners", tenant], queryFn: () => partners(auth!), enabled });
  // Cifra lunii: generatorul pur numără ce a **predat** (`/totals` pe ieșiri); colectorul are și intrări.
  const summary = useQuery({
    queryKey: ["movements", "summary", tenant, year, month],
    queryFn: () => movementSummary(auth!, year, month),
    enabled: enabled && !onlyGenerator,
  });
  const handed = useQuery({
    queryKey: ["movements", "totals", "OUT", tenant, year, month],
    queryFn: () => movementTotals(auth!, { year, month, direction: "OUT" }),
    enabled: enabled && onlyGenerator,
  });
  const monthQ = onlyGenerator ? handed : summary;
  const kg = onlyGenerator ? handed.data?.quantityKg : summary.data?.quantityKg;
  const rows = onlyGenerator ? handed.data?.rows : summary.data?.movements;
  // Ultima predare: aceeași listă ca „La fel ca data trecută”, ținută și pe telefon.
  const recent = useQuery({ queryKey: ["movements", "recent-handovers", tenant], queryFn: () => recentHandovers(auth!), enabled, staleTime: 5 * 60 * 1000 });
  const outbox = useOutbox(session?.email);

  const queries = [deadlinesQ, evidencesQ, partnersQ, monthQ, recent];
  useEffect(() => {
    if (queries.some((q) => q.error instanceof UnauthorizedError)) signOut();
  }, [deadlinesQ.error, evidencesQ.error, partnersQ.error, monthQ.error, recent.error, signOut]); // eslint-disable-line react-hooks/exhaustive-deps

  const [refreshing, setRefreshing] = useState(false);
  const refresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all(queries.map((q) => q.refetch())).catch(() => {});
    setRefreshing(false);
  }, [deadlinesQ.refetch, evidencesQ.refetch, partnersQ.refetch, monthQ.refetch, recent.refetch]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── starea ──
  const checks = controlChecks(
    year,
    { data: deadlinesQ.data, failed: deadlinesQ.isError },
    { data: evidencesQ.data, failed: evidencesQ.isError },
    { data: partnersQ.data, failed: partnersQ.isError },
  );
  const verdict = verdictOf(checks);

  // ── luna: compoziția pe coduri, din liniile de evidență ale lunii (aceeași sursă ca „Totalul anului”) ──
  const monthCodes = evidencesQ.data
    ? byCode(evidencesQ.data.filter((r) => r.month === month)).filter((c) => c.generated > 0)
    : null;
  const monthTotalFromCodes = monthCodes?.reduce((s, c) => s + c.generated, 0) ?? 0;
  const yearEmpty = evidencesQ.data ? byCode(evidencesQ.data).length === 0 : false;

  const r = readiness(deadlinesQ.data, undefined, undefined);
  const nextDeadline = r.overdue[0] ?? r.nextDeadline;
  const last = recent.data?.content[0];
  const pending = outbox.filter((i) => i.state === "PENDING").length;
  const rejected = outbox.filter((i) => i.state === "REJECTED").length;

  const monthName = strings.months[month - 1];
  const updated = agoText(monthQ.dataUpdatedAt);

  // ── tasta ──
  const [cameraDenied, setCameraDenied] = useState(false);
  const snap = async () => {
    const photo = await pickAvizPhoto("camera");
    if (photo === "denied") return setCameraDenied(true);
    if (!photo) return;
    setCameraDenied(false);
    router.push({ pathname: "/predare", params: { photo } });
  };

  return (
    <ScrollView
      style={styles.fill}
      contentContainerStyle={styles.scroll}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.ink2} />}
    >
      <LightHead title={greeting(now)} subtitle={dayLine(now)} />
      <OfflineBand />
      <View style={styles.body}>
        {noCompany ? (
          <Group>
            <Note tone="alert">{m.noCompanyYet}</Note>
            <Pressable testID="pick-company" onPress={() => router.push("/firme")} style={[rowStyles.row, rowStyles.sep, styles.link]}>
              <Icon name="building" size={21} color={colors.ink2} />
              <Text style={[rowStyles.title, styles.linkText]}>{m.pickCompany}</Text>
              <Icon name="right" size={18} color={colors.ink3} />
            </Pressable>
          </Group>
        ) : (
          <>
            <StateCard verdict={verdict} checks={checks} onPress={() => router.navigate("/control")} />

            <Key icon="cam" label={m.snapAviz} onPress={snap} testID="key-snap" />
            {cameraDenied ? <Note tone="alert">{m.cameraDenied}</Note> : null}

            {pending + rejected > 0 ? (
              <Group>
                <Pressable testID="queue-row" onPress={() => router.navigate("/adauga")} style={({ pressed }) => [rowStyles.row, styles.row, pressed && rowStyles.pressed]}>
                  <Tile icon={rejected ? "alert" : "clock"} tone={rejected ? "bad" : "warn"} />
                  <View style={styles.rowBody}>
                    <Text style={rowStyles.title}>{rejected ? m.queueRejected(rejected) : m.queueTitle(pending)}</Text>
                    <Text style={rowStyles.sub}>{rejected ? m.queueRejectedSub : m.outboxHint}</Text>
                  </View>
                  <Chip label={rejected ? m.outboxRejected : m.outboxPending} tone={rejected ? "bad" : "warn"} />
                </Pressable>
              </Group>
            ) : null}

            {/* Luna: cifra ca tipografie mare, nu afișaj. „?” când n-a venit, niciodată „0”. */}
            <Pressable
              testID="see-month"
              onPress={() => {
                haptic.tap();
                router.navigate("/miscari");
              }}
              accessibilityRole="button"
              style={({ pressed }) => [styles.month, pressed && { backgroundColor: colors.pressed }]}
            >
              <View style={styles.monthHead}>
                <Text style={styles.monthLabel}>{(onlyGenerator ? m.monthHanded : m.monthRegistered)(monthName)}</Text>
                <Text style={styles.linkText}>{m.seeMonth}</Text>
              </View>
              {kg == null && monthQ.isPending && enabled ? (
                <Skeleton width={150} height={34} radius={8} />
              ) : (
                <Text style={styles.monthValue} testID="month-kg">
                  {kg == null ? "?" : formatKg(kg)}
                  <Text style={styles.monthUnit}> kg</Text>
                </Text>
              )}
              <Text style={styles.monthSub} numberOfLines={2}>
                {monthQ.isError
                  ? m.lcdError
                  : rows == null
                    ? " "
                    : [m.monthCount(rows), last && rows > 0 ? m.monthLast(formatDate(last.date)) : null, pending ? m.monthStillOnPhone(pending) : null, updated]
                        .filter(Boolean)
                        .join(" · ")}
              </Text>
              {monthCodes && monthCodes.length > 0 && monthTotalFromCodes > 0 ? (
                <>
                  <View style={styles.stack}>
                    {monthCodes.map((c) => (
                      <View key={c.wasteCode} style={[styles.stackPart, { flex: c.generated, backgroundColor: binColorOf(c.wasteCode, c.hazardous) }]} />
                    ))}
                  </View>
                  {monthCodes.length > 1 ? (
                    <View style={styles.legend}>
                      {monthCodes.map((c) => (
                        <View key={c.wasteCode} style={styles.legendItem}>
                          <View style={[styles.legendDot, { backgroundColor: binColorOf(c.wasteCode, c.hazardous) }]} />
                          <Text style={styles.legendText}>
                            {c.wasteCode} <Text style={styles.legendKg}>{formatKg(c.generated)}</Text>
                          </Text>
                        </View>
                      ))}
                    </View>
                  ) : null}
                </>
              ) : rows === 0 && yearEmpty ? (
                <Text style={styles.hint}>{m.firstAviz}</Text>
              ) : null}
            </Pressable>

            {/* Două rânduri: ultima predare, următorul termen. Anul stă pe Generare › Totalul anului. */}
            <Group>
              <Line
                label={m.lastHandover}
                testID="line-last"
                tile={last ? <Tile tone="bin" color={binColorOf(last.wasteCode, last.hazardous)} /> : <Tile tone="quiet" icon="list" />}
                onPress={last ? () => router.push({ pathname: "/miscare/[id]", params: { id: last.id } }) : undefined}
              >
                {recent.isPending && enabled ? (
                  <Skeleton width="80%" height={15} />
                ) : last ? (
                  <>
                    <Text style={rowStyles.title} numberOfLines={1}>
                      {last.wasteCodeName}
                      {last.partnerName ? ` → ${last.partnerName}` : ""}
                    </Text>
                    <Text style={rowStyles.sub} numberOfLines={1}>
                      {formatDate(last.date)} · {last.quantity == null ? m.awaitingWeighing : `${formatQuantity(last.quantity, last.unit)} ${strings.enums.unit[last.unit]}`}
                    </Text>
                  </>
                ) : (
                  <Text style={rowStyles.title} numberOfLines={2}>{recent.isError ? m.movementsError : m.yearEmpty}</Text>
                )}
              </Line>
              <Line label={m.nextDeadline} testID="line-deadline" tile={<Tile tone="quiet" icon="clock" />} onPress={() => router.navigate("/termene")} sep>
                {deadlinesQ.isPending && enabled ? (
                  <Skeleton width="60%" height={15} />
                ) : nextDeadline ? (
                  <>
                    <Text style={rowStyles.title} numberOfLines={1}>{strings.enums.reportType[nextDeadline.reportType]}</Text>
                    <Text style={rowStyles.sub} numberOfLines={1}>
                      {[nextDeadline.dueDate ? formatDate(nextDeadline.dueDate) : null, daysLabel(nextDeadline)].filter(Boolean).join(" · ")}
                    </Text>
                  </>
                ) : (
                  <Text style={rowStyles.title} numberOfLines={2}>{deadlinesQ.isError ? m.deadlinesError : m.noOpenDeadline}</Text>
                )}
              </Line>
            </Group>
          </>
        )}
      </View>
    </ScrollView>
  );
}

function Line({ label, tile, children, onPress, testID, sep }: {
  label: string;
  tile: React.ReactNode;
  children: React.ReactNode;
  onPress?: () => void;
  testID: string;
  sep?: boolean;
}) {
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      disabled={!onPress}
      style={({ pressed }) => [rowStyles.row, sep && rowStyles.sep, styles.row, pressed && rowStyles.pressed]}
    >
      {tile}
      <View style={styles.rowBody}>
        <Text style={styles.lineLabel}>{label}</Text>
        {children}
      </View>
      {onPress ? <Icon name="right" size={18} color={colors.ink3} /> : null}
    </Pressable>
  );
}

function binColorOf(code: string, hazardous: boolean) {
  const bin = binFor(code, hazardous);
  return bin ? binColors[bin] : colors.ink3;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 12 },
  link: { flexDirection: "row", alignItems: "center", gap: 12 },
  linkText: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.green },
  hint: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },
  lineLabel: { fontFamily: fonts.sans, fontSize: 12, color: colors.ink3, marginBottom: 2 },

  month: {
    backgroundColor: colors.card,
    borderRadius: radius.group,
    borderWidth: 1,
    borderColor: colors.separator,
    padding: 16,
    gap: 10,
  },
  monthHead: { flexDirection: "row", justifyContent: "space-between", alignItems: "baseline", gap: 10 },
  monthLabel: { flexShrink: 1, fontFamily: fonts.sans, fontSize: 13, color: colors.ink2 },
  monthValue: { fontFamily: fonts.monoMedium, fontSize: 34, lineHeight: 38, letterSpacing: -1, color: colors.ink, fontVariant: ["tabular-nums"] },
  monthUnit: { fontFamily: fonts.mono, fontSize: 15, color: colors.ink2, letterSpacing: 0 },
  monthSub: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.ink2 },
  stack: { flexDirection: "row", height: 8, borderRadius: 4, overflow: "hidden", gap: 2 },
  stackPart: { height: "100%" },
  legend: { flexDirection: "row", flexWrap: "wrap", gap: 6, columnGap: 14 },
  legendItem: { flexDirection: "row", alignItems: "center", gap: 6 },
  legendDot: { width: 9, height: 9, borderRadius: 3 },
  legendText: { fontFamily: fonts.sans, fontSize: 12.5, color: colors.ink2 },
  legendKg: { fontFamily: fonts.monoMedium, color: colors.ink },
});
