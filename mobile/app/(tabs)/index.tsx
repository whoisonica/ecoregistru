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
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { Skeleton } from "../../src/components/Skeleton";
import { Tile } from "../../src/components/Tile";
import { controlChecks, type Check, type CheckTone } from "../../src/control";
import { shareDossier } from "../../src/dossier";
import { formatDate, formatKg, formatQuantity } from "../../src/format";
import { haptic } from "../../src/haptics";
import { useOutbox } from "../../src/outbox";
import { pickAvizPhoto } from "../../src/photo";
import { useSession } from "../../src/session";
import { binColors, colors, fonts, radius } from "../../src/theme";

const m = strings.mobile;

/**
 * Acasă = afișul „Semaforul” (proprietarul, 27.09.2026, pe machete): verdictul ca un cuvânt mare pe
 * fond colorat, banda cu patru LED-uri → Control, tastele ecranului, cifra lunii ca tipografie mare cu
 * bara pe coduri și trei propoziții (ultima predare, următorul termen, anul). **Nimic țintuit sus**:
 * capul e pe hârtie și se derulează cu pagina.
 *
 * <p>Verdictul e aceeași socoteală ca Panoul web și ca „A venit controlul” (`src/control.ts`, prin
 * `lib/readiness.ts`): telefonul nu poate spune „în regulă” despre ceva ce webul numește „de rezolvat”.
 * Cât timp o listă n-a venit, cuvântul e „Nu știu încă”, nu „În regulă”.
 *
 * <p>Contul (email, firma, dispozitive, ieșire) a plecat în Profil (avatarul din colț).
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

  // ── verdictul ──
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

  // ── anul ──
  const yearCodes = evidencesQ.data ? byCode(evidencesQ.data) : null;
  const yearKg = yearCodes?.reduce((s, c) => s + c.generated, 0);
  const yearBlocked = yearCodes?.filter((c) => c.unclassified > 0).length ?? 0;

  const r = readiness(deadlinesQ.data, undefined, undefined);
  const nextDeadline = r.overdue[0] ?? r.nextDeadline;
  const last = recent.data?.content[0];
  const pending = outbox.filter((i) => i.state === "PENDING").length;
  const rejected = outbox.filter((i) => i.state === "REJECTED").length;

  const monthName = strings.months[month - 1];
  const updated = agoText(monthQ.dataUpdatedAt);

  // ── tastele ──
  const [cameraDenied, setCameraDenied] = useState(false);
  const snap = async () => {
    const photo = await pickAvizPhoto("camera");
    if (photo === "denied") return setCameraDenied(true);
    if (!photo) return;
    setCameraDenied(false);
    router.push({ pathname: "/predare", params: { photo } });
  };
  const [dossierBusy, setDossierBusy] = useState(false);
  const [dossierError, setDossierError] = useState<string | null>(null);
  const sendDossier = async () => {
    if (!auth || dossierBusy) return;
    setDossierBusy(true);
    setDossierError(null);
    try {
      setDossierError(await shareDossier(auth, year, 1));
    } catch (e) {
      if (e instanceof UnauthorizedError) signOut();
    } finally {
      setDossierBusy(false);
    }
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
            <Poster label={m.posterLabel(monthName)} verdict={verdict} checks={checks} onPress={() => router.navigate("/control")} />

            <View style={styles.keys}>
              <Key icon="cam" label={m.snapAviz} hint={m.snapAvizHint} onPress={snap} testID="key-snap" />
              <Key icon="shield" tone="dark" label={dossierBusy ? m.sendDossierBusy : m.sendDossierKey} hint={m.sendDossierKeyHint(year)} onPress={sendDossier} disabled={dossierBusy} testID="key-dossier" />
            </View>
            {cameraDenied ? <Note tone="alert">{m.cameraDenied}</Note> : null}
            {dossierError ? <Text style={styles.error}>{dossierError}</Text> : null}

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

            {/* Cifra lunii: tipografie mare, nu afișaj. „?” când n-a venit, niciodată „0”. */}
            <View style={styles.bignum}>
              <View style={styles.bignumHead}>
                <Text style={styles.bignumLabel}>{(onlyGenerator ? m.monthHanded : m.monthRegistered)(monthName)}</Text>
                <Pressable onPress={() => router.navigate("/miscari")} hitSlop={8} testID="see-month">
                  <Text style={styles.linkText}>{m.seeMonth}</Text>
                </Pressable>
              </View>
              {kg == null && monthQ.isPending && enabled ? (
                <Skeleton width={160} height={40} radius={8} />
              ) : (
                <Text style={styles.bignumValue} testID="month-kg">
                  {kg == null ? "?" : formatKg(kg)}
                  <Text style={styles.bignumUnit}> kg</Text>
                </Text>
              )}
              <Text style={styles.bignumSub}>
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
                </>
              ) : rows === 0 && yearKg === 0 ? (
                <Text style={styles.hint}>{m.firstAviz}</Text>
              ) : null}
            </View>

            {/* Trei propoziții: nimic nu arată ca Termene. */}
            <View style={styles.lines}>
              <Line
                label={m.lastHandover}
                testID="line-last"
                onPress={last ? () => router.push({ pathname: "/miscare/[id]", params: { id: last.id } }) : undefined}
              >
                {recent.isPending && enabled ? (
                  <Skeleton width="80%" height={15} />
                ) : last ? (
                  <Text style={styles.lineText} numberOfLines={2}>
                    <Text style={styles.lineStrong}>{last.wasteCodeName}</Text>
                    {last.partnerName ? ` → ${last.partnerName}` : ""}
                    {last.quantity == null ? `, ${strings.mobile.awaitingWeighing}` : `, ${formatQuantity(last.quantity, last.unit)} ${strings.enums.unit[last.unit]}`}
                  </Text>
                ) : (
                  <Text style={styles.lineText}>{recent.isError ? m.movementsError : m.yearEmpty}</Text>
                )}
              </Line>
              <Line label={m.nextDeadline} testID="line-deadline" onPress={() => router.navigate("/termene")}>
                {deadlinesQ.isPending && enabled ? (
                  <Skeleton width="60%" height={15} />
                ) : nextDeadline ? (
                  <Text style={styles.lineText} numberOfLines={2}>
                    <Text style={styles.lineStrong}>{strings.enums.reportType[nextDeadline.reportType]}</Text>
                    {daysLabel(nextDeadline) ? `, ${daysLabel(nextDeadline)}` : ""}
                  </Text>
                ) : (
                  <Text style={styles.lineText}>{deadlinesQ.isError ? m.deadlinesError : m.noOpenDeadline}</Text>
                )}
              </Line>
              <Line label={m.yearLine(year)} testID="line-year" onPress={() => router.navigate({ pathname: "/miscari", params: { tab: "total" } })}>
                {evidencesQ.isPending && enabled ? (
                  <Skeleton width="70%" height={15} />
                ) : yearCodes ? (
                  <Text style={styles.lineText} numberOfLines={2}>
                    {yearCodes.length === 0 ? (
                      m.yearEmpty
                    ) : (
                      <>
                        <Text style={styles.lineStrong}>{m.yearSummary(formatKg(yearKg ?? 0), yearCodes.length)}</Text>
                        {`, ${yearBlocked > 0 ? m.yearBlocked(yearBlocked) : m.yearReady}`}
                      </>
                    )}
                  </Text>
                ) : (
                  <Text style={styles.lineText}>{evidencesQ.isError ? m.lcdError : "?"}</Text>
                )}
              </Line>
            </View>
          </>
        )}
      </View>
    </ScrollView>
  );
}

type Verdict = { tone: CheckTone | "loading"; word: string; why: string };

/** Cuvântul de pe afiș, din rândurile Controlului: cel mai grav rând dă tonul; un rând nevenit ține „Nu știu încă”. */
function verdictOf(checks: (Check | null)[]): Verdict {
  const loaded = checks.filter((c): c is Check => c !== null);
  if (loaded.length < checks.length) return { tone: "loading", word: m.verdictUnknown, why: " " };
  const worst = (["bad", "warn", "unknown"] as CheckTone[]).map((t) => loaded.find((c) => c.tone === t)).find(Boolean);
  if (!worst) return { tone: "ok", word: m.verdictOk, why: m.verdictOkWhy };
  if (worst.tone === "unknown") return { tone: "unknown", word: m.verdictUnknown, why: m.verdictUnknownWhy };
  return { tone: worst.tone, word: worst.tone === "bad" ? m.verdictBad : m.verdictWarn, why: `${worst.title}: ${worst.detail}` };
}

const LED_LABEL: Record<Check["key"], string> = {
  deadlines: m.ledDeadlines,
  missingCode: m.ledCode,
  weighing: m.ledWeighing,
  partners: m.ledPartners,
};
const LED_KEYS: Check["key"][] = ["deadlines", "missingCode", "weighing", "partners"];

function Poster({ label, verdict, checks, onPress }: { label: string; verdict: Verdict; checks: (Check | null)[]; onPress: () => void }) {
  const tone = verdict.tone;
  const bg = tone === "bad" ? colors.redSoft : tone === "warn" ? colors.amberSoft : tone === "ok" ? colors.greenSoft : "#E6E9E6";
  const fg = tone === "bad" ? colors.redText : tone === "warn" ? colors.amberText : tone === "ok" ? colors.greenText : colors.ink2;
  return (
    <Pressable
      testID="poster"
      onPress={() => {
        haptic.tap();
        onPress();
      }}
      accessibilityRole="button"
      style={({ pressed }) => [styles.poster, { backgroundColor: bg }, pressed && { opacity: 0.9 }]}
    >
      <Text style={[styles.posterLabel, { color: fg }]}>{label.toUpperCase()}</Text>
      {tone === "loading" ? <Skeleton width={200} height={36} radius={8} /> : <Text style={[styles.posterWord, { color: fg }]} testID="verdict">{verdict.word}</Text>}
      <Text style={styles.posterWhy} numberOfLines={3}>{verdict.why}</Text>
      <View style={styles.strip}>
        {LED_KEYS.map((key, i) => {
          const c = checks[i];
          const led = !c ? "#C9D0CB" : c.tone === "bad" ? colors.red : c.tone === "warn" ? "#F59E0B" : c.tone === "ok" ? colors.green : "#C9D0CB";
          return (
            <View key={key} style={styles.led} testID={`led-${key}`}>
              <View style={[styles.ledBar, { backgroundColor: led }, c?.tone === "bad" && styles.ledBad]} />
              <Text style={styles.ledText}>{LED_LABEL[key]}</Text>
            </View>
          );
        })}
      </View>
      <View style={styles.posterGo}>
        <Text style={[styles.posterGoText, { color: fg }]}>{m.posterGo}</Text>
        <Icon name="arrow" size={16} color={fg} strokeWidth={2.4} />
      </View>
    </Pressable>
  );
}

function Line({ label, children, onPress, testID }: { label: string; children: React.ReactNode; onPress?: () => void; testID: string }) {
  return (
    <Pressable testID={testID} onPress={onPress} disabled={!onPress} style={({ pressed }) => [styles.line, pressed && { opacity: 0.7 }]}>
      <View style={styles.lineBody}>
        <Text style={styles.lineLabel}>{label.toUpperCase()}</Text>
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
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 14 },
  link: { flexDirection: "row", alignItems: "center", gap: 12 },
  linkText: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.greenText },
  error: { fontFamily: fonts.sans, fontSize: 14, color: colors.redText, paddingHorizontal: 4 },
  hint: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },

  poster: { borderRadius: 22, paddingHorizontal: 18, paddingTop: 20, paddingBottom: 16, gap: 14 },
  posterLabel: { fontFamily: fonts.mono, fontSize: 10.5, letterSpacing: 1.4, opacity: 0.8 },
  posterWord: { fontFamily: fonts.sansSemiBold, fontSize: 40, lineHeight: 42, letterSpacing: -1.2 },
  posterWhy: { fontFamily: fonts.sans, fontSize: 15, color: colors.ink, maxWidth: 320 },
  strip: { flexDirection: "row", gap: 8 },
  led: { flex: 1, alignItems: "center", gap: 6 },
  ledBar: { width: "100%", height: 14, borderRadius: 5 },
  ledBad: { shadowColor: colors.red, shadowOpacity: 0.6, shadowRadius: 8, shadowOffset: { width: 0, height: 0 } },
  ledText: { fontFamily: fonts.sans, fontSize: 11, color: colors.ink2, textAlign: "center" },
  posterGo: { flexDirection: "row", alignItems: "center", gap: 6 },
  posterGoText: { fontFamily: fonts.sansSemiBold, fontSize: 14 },

  keys: { flexDirection: "row", gap: 10 },

  bignum: { paddingHorizontal: 4, gap: 10 },
  bignumHead: { flexDirection: "row", justifyContent: "space-between", alignItems: "baseline" },
  bignumLabel: { fontFamily: fonts.sansSemiBold, fontSize: 13, letterSpacing: 0.3, color: colors.ink2, textTransform: "uppercase" },
  bignumValue: { fontFamily: fonts.monoMedium, fontSize: 44, lineHeight: 48, letterSpacing: -1.3, color: colors.ink, fontVariant: ["tabular-nums"] },
  bignumUnit: { fontFamily: fonts.mono, fontSize: 18, color: colors.ink2, letterSpacing: 0 },
  bignumSub: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2 },
  stack: { flexDirection: "row", height: 12, borderRadius: 6, overflow: "hidden", gap: 2 },
  stackPart: { height: "100%" },
  legend: { flexDirection: "row", flexWrap: "wrap", gap: 6, columnGap: 14 },
  legendItem: { flexDirection: "row", alignItems: "center", gap: 6 },
  legendDot: { width: 10, height: 10, borderRadius: 3 },
  legendText: { fontFamily: fonts.sans, fontSize: 12.5, color: colors.ink2 },
  legendKg: { fontFamily: fonts.monoMedium, color: colors.ink },

  lines: { marginTop: 4 },
  line: { flexDirection: "row", alignItems: "center", gap: 12, paddingVertical: 14, paddingHorizontal: 4, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.separator },
  lineBody: { flex: 1, gap: 2 },
  lineLabel: { fontFamily: fonts.sans, fontSize: 12, letterSpacing: 0.5, color: colors.ink3 },
  lineText: { fontFamily: fonts.sans, fontSize: 15, color: colors.ink },
  lineStrong: { fontFamily: fonts.sansMedium },
});
