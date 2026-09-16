import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import * as Sharing from "expo-sharing";
import { useEffect, useState } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError, downloadAuditFile, evidences, partners, upcomingDeadlines, UnauthorizedError } from "../../src/api";
import { Pills, PrimaryButton } from "../../src/components/Form";
import { GraphiteHeader } from "../../src/components/GraphiteHeader";
import { Icon } from "../../src/components/Icon";
import { Lcd } from "../../src/components/Lcd";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { controlChecks, type Check, type CheckTone } from "../../src/control";
import { useSession } from "../../src/session";
import { colors, fonts, radius } from "../../src/theme";

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

  let value: string | null = "";
  let foot: string | undefined;
  let footTone: "ok" | "alert" = "alert";
  if (!tenant) {
    value = null;
    foot = m.noCompanyYet;
  } else if (ready) {
    // Un singur rând necunoscut face cifra „?”: „3 din 4” ar număra rândul care n-a venit ca fiind în neregulă.
    value = anyUnknown ? null : String(loaded.filter((c) => c.tone === "ok").length);
    if (worst?.tone === "unknown") foot = m.controlUnknown;
    else if (worst) foot = `▲ ${worst.detail}`;
    else {
      foot = m.controlAllOk;
      footTone = "ok";
    }
  }

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <GraphiteHeader title={m.controlTitle} meta={(session?.tenantName ?? strings.appName).toUpperCase()}>
        <Lcd
          label={m.controlLcdLabel}
          state={m.controlLcdState}
          value={value}
          ghost="8"
          unit={ready && tenant ? m.controlOf(checks.length) : undefined}
          foot={foot}
          footTone={footTone}
        />
      </GraphiteHeader>

      <View style={styles.sheet}>
        {tenant ? (
          <>
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
      <View style={styles.body}>
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
