import { strings } from "@web/strings";
import * as Sharing from "expo-sharing";
import { useRouter } from "expo-router";
import { useCallback, useState } from "react";
import { RefreshControl, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError, downloadAuditFile, UnauthorizedError } from "../../src/api";
import { Pills, PrimaryButton } from "../../src/components/Form";
import { LightHead } from "../../src/components/LightHead";
import { OfflineBand } from "../../src/components/OfflineBand";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { StateCard } from "../../src/components/StateCard";
import { SkeletonRows } from "../../src/components/Skeleton";
import { Tile } from "../../src/components/Tile";
import type { Check } from "../../src/control";
import { useControl } from "../../src/useControl";
import { useSession } from "../../src/session";
import { colors, fonts } from "../../src/theme";

const m = strings.mobile;

/**
 * „A venit controlul” (M1c): ce găsește inspectorul acum și dosarul trimis din telefon.
 *
 * <p>Rândurile sunt socoteala Panoului web (G5, `src/control.ts`), deci telefonul nu poate spune „în
 * regulă” despre ceva ce webul numește „de rezolvat”. Fără cache pe SQLite, dinadins: o listă de ieri
 * arătată ca „în regulă” în fața inspectorului e mai rea decât un „?”. Socoteala stă în `useControl`,
 * aceeași pentru modul inspector (F10); fiecare verificare are dedesubt „ce înseamnă”.
 */
export default function ControlScreen() {
  const router = useRouter();
  const { tenant, year, checks, verdict, updated, refresh: reload } = useControl();

  const [refreshing, setRefreshing] = useState(false);
  const refresh = useCallback(async () => {
    setRefreshing(true);
    await reload();
    setRefreshing(false);
  }, [reload]);

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
            <StateCard verdict={verdict} testID="control-verdict" wordTestID="lcd-value" />
            <SectionHead>{m.controlChecks}</SectionHead>
            <Group>
              {checks.map((c, i) =>
                c ? (
                  <CheckRow key={c.key} check={c} first={i === 0} />
                ) : (
                  <View key={i} style={i > 0 && rowStyles.sep}>
                    <SkeletonRows rows={1} />
                  </View>
                ),
              )}
            </Group>
            {/* F10: telefonul întins peste masă — ecran întreg, alb, litere mari. */}
            <PrimaryButton testID="inspector-open" label={m.inspectorOpen} onPress={() => router.push("/inspector")} />
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

const CHIP = {
  ok: { tone: "ok", label: m.controlOk },
  warn: { tone: "warn", label: m.controlWarn },
  bad: { tone: "bad", label: m.controlBad },
  unknown: { tone: "quiet", label: m.controlUnknownChip },
} as const;

function CheckRow({ check, first }: { check: Check; first: boolean }) {
  const chip = CHIP[check.tone];
  return (
    <View testID={`check-${check.key}`} style={[rowStyles.row, !first && rowStyles.sep, styles.row]}>
      {check.tone === "unknown" ? (
        <Tile tone="quiet" text="?" />
      ) : (
        <Tile tone={check.tone} icon={check.tone === "ok" ? "check" : "alert"} />
      )}
      <View style={styles.rowBody}>
        <Text style={rowStyles.title} numberOfLines={2}>{check.title}</Text>
        <Text style={rowStyles.sub} testID={`check-${check.key}-detail`}>{check.detail}</Text>
        <Text style={styles.meaning}>{m.checkMeaning[check.key]}</Text>
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
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 10 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },
  meaning: { fontFamily: fonts.sans, fontSize: 12.5, lineHeight: 17, color: colors.ink3, marginTop: 4 },
  dossier: { marginTop: 16, gap: 10 },
  hint: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2, paddingHorizontal: 4 },
  error: { fontFamily: fonts.sans, fontSize: 14, color: colors.redText, paddingHorizontal: 4 },
});
