import { useQuery } from "@tanstack/react-query";
import { byCode } from "@/lib/annualTotals";
import { countOf } from "@/lib/count";
import { strings } from "@web/strings";
import { useEffect, useState, type ReactNode } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";

import { evidences, UnauthorizedError } from "../api";
import { formatKg } from "../format";
import { useSession } from "../session";
import { colors, radius } from "../theme";
import { Bin } from "./Bin";
import { GraphiteHeader } from "./GraphiteHeader";
import { Lcd } from "./Lcd";
import { YearArrows } from "./MonthArrows";
import { Chip, Group, Note, rowStyles, SectionHead } from "./Rows";

const t = strings.evidences;
const m = strings.mobile;

/**
 * Tabul „Totalul anului” al ecranului „Generare” (webul, din 18.09.2026): cifrele pe cod de deșeu care
 * se tastează în SIM pe 15 martie. Aceleași linii (`GET /evidences`) și aceeași socoteală
 * (`lib/annualTotals.ts`, importată) ca pe web — deci fără stoc, dinadins.
 *
 * <p>Starea fiecărui cod e cea de pe web: roșu = fără cod R/D (nu se poate depune așa), galben = așteaptă
 * cântarul, verde = gata. Documentele (fișa, declarația) rămân pe web: pe 15 martie se lucrează la birou.
 */
export function AnnualTotals({ tabRow }: { tabRow: ReactNode }) {
  const { session, auth, signOut } = useSession();
  const [year, setYear] = useState(new Date().getFullYear());
  const rows = useQuery({
    queryKey: ["evidences", session?.tenantId, year],
    queryFn: () => evidences(auth!, year),
    enabled: !!auth && !!session?.tenantId,
  });

  useEffect(() => {
    if (rows.error instanceof UnauthorizedError) signOut();
  }, [rows.error, signOut]);

  const codes = rows.data ? byCode(rows.data) : null;
  const generated = codes?.reduce((sum, c) => sum + c.generated, 0);
  const blocked = codes?.filter((c) => c.unclassified > 0).length ?? 0;

  let foot: string | undefined;
  if (!session?.tenantId) foot = m.noCompanyYet;
  else if (rows.isError) foot = m.lcdError;
  else if (codes) foot = blocked > 0 ? m.annualBlocked(blocked) : m.annualCodes(codes.length);

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <GraphiteHeader title={strings.movements.tabAnnual} meta={(session?.tenantName ?? strings.appName).toUpperCase()}>
        <Lcd
          label={m.lcdLabelYear(year)}
          state={m.lcdState}
          value={generated != null ? formatKg(generated) : rows.isPending && session?.tenantId ? "" : null}
          unit="kg"
          foot={foot}
          footTone={blocked > 0 || rows.isError || !session?.tenantId ? "alert" : "ok"}
          footRight={<YearArrows year={year} onChange={setYear} />}
        />
      </GraphiteHeader>

      <View style={styles.sheet}>
        {tabRow}
        <SectionHead>{m.annualByCode}</SectionHead>
        <Group>
          {rows.isError ? (
            <Note tone="alert">{t.loadError}</Note>
          ) : !codes ? (
            <Note>{" "}</Note>
          ) : codes.length === 0 ? (
            <Note>{t.empty.replace("{year}", String(year))}</Note>
          ) : (
            codes.map((c, i) => (
              <View key={c.wasteCode} testID="annual-row" style={[rowStyles.row, i > 0 && rowStyles.sep]}>
                <View style={styles.top}>
                  <Bin code={c.wasteCode} hazardous={c.hazardous} />
                  <View style={styles.body}>
                    <Text style={rowStyles.mono}>
                      {c.wasteCode}
                      {c.hazardous ? "*" : ""}
                    </Text>
                    <Text style={rowStyles.sub} numberOfLines={2}>
                      {c.wasteCodeName}
                    </Text>
                  </View>
                  {c.unclassified > 0 ? (
                    <Chip label={t.stateMissingCode.replace("{kg}", formatKg(c.unclassified))} tone="bad" />
                  ) : c.awaiting > 0 ? (
                    <Chip label={t.stateAwaiting.replace("{count}", countOf(c.awaiting, "linie", "linii"))} tone="warn" />
                  ) : (
                    <Chip label={t.stateReady} tone="ok" />
                  )}
                </View>
                <View style={styles.figures}>
                  <Figure label={t.colGenerated} value={c.generated} />
                  <Figure label={t.colRecovered} value={c.recovered} />
                  <Figure label={t.colDisposed} value={c.disposed} />
                </View>
              </View>
            ))
          )}
        </Group>
      </View>
    </ScrollView>
  );
}

function Figure({ label, value }: { label: string; value: number }) {
  return (
    <View>
      <Text style={styles.figLabel}>{label}</Text>
      <Text style={rowStyles.mono}>{formatKg(value)}</Text>
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
  top: { flexDirection: "row", alignItems: "center", gap: 12 },
  body: { flex: 1 },
  figures: { flexDirection: "row", gap: 20, marginTop: 8, marginLeft: 22 },
  figLabel: { fontSize: 11, color: colors.ink3, letterSpacing: 0.4, textTransform: "uppercase" },
});
