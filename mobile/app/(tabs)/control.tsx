import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import * as Sharing from "expo-sharing";
import { useCallback, useEffect, useState } from "react";
import { RefreshControl, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError, downloadAuditFile, evidences, partners, upcomingDeadlines, UnauthorizedError } from "../../src/api";
import { agoText } from "../../src/agoText";
import { Pills, PrimaryButton } from "../../src/components/Form";
import { Icon } from "../../src/components/Icon";
import { LightHead } from "../../src/components/LightHead";
import { OfflineBand } from "../../src/components/OfflineBand";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { Skeleton } from "../../src/components/Skeleton";
import { controlChecks, type Check, type CheckTone } from "../../src/control";
import { useSession } from "../../src/session";
import { colors, fonts } from "../../src/theme";

const m = strings.mobile;

/**
 * „A venit controlul” (M1c): ce găsește inspectorul acum și dosarul trimis din telefon.
 *
 * <p>Rândurile sunt socoteala Panoului web (G5, `src/control.ts`), deci telefonul nu poate spune „în
 * regulă” despre ceva ce webul numește „de rezolvat”. Fără cache pe SQLite, dinadins: o listă de ieri
 * arătată ca „în regulă” în fața inspectorului e mai rea decât un „?”.
 */
export default function ControlScreen() {
  const { session, auth, signOut } = useSession();
  const tenant = session?.tenantId;
  const year = new Date().getFullYear();
  const enabled = !!auth && !!tenant;

  const deadlinesQ = useQuery({
    queryKey: ["deadlines", tenant, "upcoming"],
    queryFn: () => upcomingDeadlines(auth!),
    enabled,
  });
  const evidencesQ = useQuery({
    queryKey: ["evidences", tenant, year],
    queryFn: () => evidences(auth!, year),
    enabled,
  });
  const partnersQ = useQuery({
    queryKey: ["control", "partners", tenant],
    queryFn: () => partners(auth!),
    enabled,
  });

  const queries = [deadlinesQ, evidencesQ, partnersQ];
  useEffect(() => {
    if (queries.some((q) => q.error instanceof UnauthorizedError)) signOut();
  }, [deadlinesQ.error, evidencesQ.error, partnersQ.error, signOut]); // eslint-disable-line react-hooks/exhaustive-deps

  const checks = controlChecks(
    year,
    { data: deadlinesQ.data, failed: deadlinesQ.isError },
    { data: evidencesQ.data, failed: evidencesQ.isError },
    { data: partnersQ.data, failed: partnersQ.isError },
  );
  const loaded = checks.filter((c): c is Check => c !== null);
  const ready = loaded.length === checks.length;
  const anyUnknown = loaded.some((c) => c.tone === "unknown");
  // Primul lucru de văzut, în ordinea gravității — același rol ca banda „▲” de sub afișajul web.
  const worst = (["bad", "warn", "unknown"] as CheckTone[])
    .map((tone) => loaded.find((c) => c.tone === tone))
    .find(Boolean);

  // Verdictul, cu un cuvânt (același ca afișul de pe Acasă): cel mai grav rând dă tonul; un rând nevenit
  // sau necunoscut ține „Nu știu încă” — „3 din 4” ar fi numărat rândul care n-a venit ca fiind în neregulă.
  const verdictTone: CheckTone | "loading" = !ready ? "loading" : anyUnknown ? "unknown" : (worst?.tone ?? "ok");
  const verdictWord =
    verdictTone === "bad" ? m.verdictBad : verdictTone === "warn" ? m.verdictWarn : verdictTone === "ok" ? m.verdictOk : m.verdictUnknown;
  const verdictWhy =
    verdictTone === "loading" ? " " : verdictTone === "unknown" ? m.controlUnknown : worst ? worst.detail : m.controlAllOk;
  const updatedAt = Math.min(...queries.map((q) => q.dataUpdatedAt || Infinity));
  const updated = Number.isFinite(updatedAt) ? agoText(updatedAt) : null;

  const [refreshing, setRefreshing] = useState(false);
  const refresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all(queries.map((q) => q.refetch())).catch(() => {});
    setRefreshing(false);
  }, [deadlinesQ.refetch, evidencesQ.refetch, partnersQ.refetch]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <ScrollView
      style={styles.fill}
      contentContainerStyle={styles.scroll}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.ink2} />}
    >
      <LightHead title={m.controlTitle} subtitle={[m.controlSub, updated].filter(Boolean).join(" · ")} />
      <OfflineBand />
      <View style={styles.body}>
        {tenant ? (
          <>
            <View style={styles.verdict} testID="control-verdict">
              <View style={[styles.verdictLed, { backgroundColor: LED[verdictTone] }]} />
              <View style={styles.verdictText}>
                {verdictTone === "loading" ? (
                  <Skeleton width={160} height={26} radius={6} />
                ) : (
                  <Text style={[styles.verdictWord, { color: INK[verdictTone] }]} testID="lcd-value">{verdictWord}</Text>
                )}
                <Text style={styles.verdictWhy} numberOfLines={2}>{verdictWhy}</Text>
              </View>
            </View>
            <SectionHead>{m.controlChecks}</SectionHead>
            <Group>
              {checks.map((c, i) =>
                c ? (
                  <CheckRow key={c.key} check={c} first={i === 0} />
                ) : (
                  <View key={i} style={[rowStyles.row, i > 0 && rowStyles.sep]} />
                ),
              )}
            </Group>
            <Dossier year={year} />
          </>
        ) : (
          <Group>
            <Note tone="alert">{m.noCompanyYet}</Note>
          </Group>
        )}
      </View>
    </ScrollView>
  );
}

const LED: Record<CheckTone | "loading", string> = {
  ok: colors.green,
  warn: "#F59E0B",
  bad: colors.red,
  unknown: "#C9D0CB",
  loading: "#C9D0CB",
};
const INK: Record<CheckTone | "loading", string> = {
  ok: colors.greenText,
  warn: colors.amberText,
  bad: colors.redText,
  unknown: colors.ink2,
  loading: colors.ink2,
};

const CHIP = {
  ok: { tone: "ok", label: m.controlOk },
  warn: { tone: "warn", label: m.controlWarn },
  bad: { tone: "bad", label: m.controlBad },
  unknown: { tone: "quiet", label: m.controlUnknownChip },
} as const;

function CheckRow({ check, first }: { check: Check; first: boolean }) {
  const chip = CHIP[check.tone];
  const tile = check.tone === "ok" ? styles.tileOk : check.tone === "unknown" ? styles.tileQuiet : check.tone === "bad" ? styles.tileBad : styles.tileWarn;
  const ink = check.tone === "ok" ? colors.greenText : check.tone === "unknown" ? colors.ink2 : check.tone === "bad" ? colors.redText : colors.amberText;
  return (
    <View testID={`check-${check.key}`} style={[rowStyles.row, !first && rowStyles.sep, styles.row]}>
      <View style={[styles.tile, tile]}>
        {check.tone === "unknown" ? (
          <Text style={[styles.tileText, { color: ink }]}>?</Text>
        ) : (
          <Icon name={check.tone === "ok" ? "check" : "alert"} size={18} color={ink} strokeWidth={2.2} />
        )}
      </View>
      <View style={styles.rowBody}>
        <Text style={rowStyles.title} numberOfLines={2}>{check.title}</Text>
        <Text style={rowStyles.sub} testID={`check-${check.key}-detail`}>{check.detail}</Text>
      </View>
      <Chip label={chip.label} tone={chip.tone} />
    </View>
  );
}

/**
 * D5 — dosarul prin foaia de partajare a telefonului: zero backend nou, iar omul alege singur Mail,
 * WhatsApp sau Drive. Implicit anul curent, ca pe web; trei ani e cât poate cere un control.
 */
function Dossier({ year }: { year: number }) {
  const { auth, signOut } = useSession();
  const [years, setYears] = useState<"1" | "3">("1");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = async () => {
    if (!auth) return;
    setBusy(true);
    setError(null);
    try {
      if (!(await Sharing.isAvailableAsync())) return setError(m.shareUnavailable);
      const uri = await downloadAuditFile(auth, year, Number(years));
      await Sharing.shareAsync(uri, { mimeType: "application/zip", UTI: "public.zip-archive", dialogTitle: m.sendDossier });
    } catch (e) {
      if (e instanceof UnauthorizedError) return signOut();
      setError(e instanceof ApiError ? (e.serverMessage ?? strings.auditFile.downloadError) : m.sendDossierOffline);
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.dossier}>
      <SectionHead>{strings.nav.auditFile}</SectionHead>
      <Pills
        testID="dossier-years"
        value={years}
        onChange={setYears}
        options={[
          { value: "1", label: m.dossierYear(year) },
          { value: "3", label: m.dossierThreeYears },
        ]}
      />
      <PrimaryButton testID="send-dossier" label={busy ? m.sendDossierBusy : m.sendDossier} onPress={send} disabled={busy} />
      {error ? <Text style={styles.error}>{error}</Text> : <Text style={styles.hint}>{m.sendDossierHint}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 8 },
  verdict: {
    backgroundColor: colors.card,
    borderRadius: 18,
    padding: 16,
    flexDirection: "row",
    alignItems: "center",
    gap: 14,
    marginBottom: 8,
  },
  verdictLed: { width: 22, height: 22, borderRadius: 6 },
  verdictText: { flex: 1, gap: 2 },
  verdictWord: { fontFamily: fonts.sansSemiBold, fontSize: 24, letterSpacing: -0.5 },
  verdictWhy: { fontFamily: fonts.sans, fontSize: 13.5, color: colors.ink2 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },
  tile: { width: 34, height: 34, borderRadius: 9, alignItems: "center", justifyContent: "center" },
  tileOk: { backgroundColor: colors.greenSoft },
  tileWarn: { backgroundColor: colors.amberSoft },
  tileBad: { backgroundColor: colors.redSoft },
  tileQuiet: { backgroundColor: "#EDEFEE" },
  tileText: { fontFamily: fonts.monoMedium, fontSize: 17 },
  dossier: { marginTop: 16, gap: 10 },
  hint: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2, paddingHorizontal: 4 },
  error: { fontFamily: fonts.sans, fontSize: 14, color: colors.redText, paddingHorizontal: 4 },
});
