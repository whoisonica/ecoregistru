import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { movementSummary, movementTotals, UnauthorizedError } from "../../src/api";
import { isMultiCompany } from "../../src/auth";
import { useMovementScreens } from "../../src/company";
import { GraphiteHeader } from "../../src/components/GraphiteHeader";
import { Icon } from "../../src/components/Icon";
import { Lcd } from "../../src/components/Lcd";
import { MonthArrows } from "../../src/components/MonthArrows";
import { Group, rowStyles, SectionHead } from "../../src/components/Rows";
import { formatKg } from "../../src/format";
import { useSession } from "../../src/session";
import { colors, radius } from "../../src/theme";

export default function HomeScreen() {
  const { session, auth, signOut } = useSession();
  const router = useRouter();
  const now = new Date();
  const [cursor, setCursor] = useState({ year: now.getFullYear(), month: now.getMonth() + 1 });

  // Consultantul și platforma aleg firma din comutator; până atunci serverul n-are ce număra.
  const noCompany = isMultiCompany(session?.role) && !session?.tenantId;
  const screens = useMovementScreens();

  /**
   * Generatorul pur are un singur ecran, „Generare”, și pentru el cifra lunii e ce a **predat** —
   * `/movements/totals` pe direcția de ieșire. Colectorul are și intrări, deci rămâne suma lunii
   * din `/movements/summary`: o cifră pe o singură direcție ar ascunde jumătate din depozit.
   */
  const onlyGenerator = screens.length === 1 && screens[0] === "GENERATED";

  const summary = useQuery({
    queryKey: ["movements", "summary", session?.tenantId, cursor.year, cursor.month],
    queryFn: () => movementSummary(auth!, cursor.year, cursor.month),
    enabled: !!auth && !noCompany && !onlyGenerator,
  });
  const handed = useQuery({
    queryKey: ["movements", "totals", "OUT", session?.tenantId, cursor.year, cursor.month],
    queryFn: () => movementTotals(auth!, { ...cursor, direction: "OUT" }),
    enabled: !!auth && !noCompany && onlyGenerator,
  });

  const kg = onlyGenerator ? handed.data?.quantityKg : summary.data?.quantityKg;
  const rows = onlyGenerator ? handed.data?.rows : summary.data?.movements;
  const query = onlyGenerator ? handed : summary;

  useEffect(() => {
    if (query.error instanceof UnauthorizedError) signOut();
  }, [query.error, signOut]);

  const monthName = strings.months[cursor.month - 1];
  const label = cursor.year === now.getFullYear() ? monthName : `${monthName} ${cursor.year}`;

  let foot: string | undefined;
  let footTone: "ok" | "alert" = "ok";
  if (noCompany) {
    foot = strings.mobile.noCompanyYet;
    footTone = "alert";
  } else if (query.isError) {
    foot = strings.mobile.lcdError;
    footTone = "alert";
  } else if (rows != null) {
    foot = rows === 0 ? strings.mobile.lcdEmpty : strings.mobile.lcdMovements(rows);
  }

  const role = session ? strings.enums.role[session.role] : "";

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <GraphiteHeader
        title={session?.tenantName ?? session?.consultancyName ?? strings.appName}
        meta={role.toUpperCase()}
      >
        <Lcd
          label={(onlyGenerator ? strings.mobile.lcdLabelOut : strings.mobile.lcdLabel)(label)}
          state={strings.mobile.lcdState}
          value={kg != null ? formatKg(kg) : query.isPending && !noCompany ? "" : null}
          unit="kg"
          foot={foot}
          footTone={footTone}
          footRight={<MonthArrows cursor={cursor} onChange={setCursor} />}
        />
      </GraphiteHeader>

      <View style={styles.sheet}>
        <SectionHead>{strings.mobile.account}</SectionHead>
        <Group>
          <View style={rowStyles.row}>
            <Text style={rowStyles.title} numberOfLines={1}>{session?.email}</Text>
            <Text style={rowStyles.sub}>{role}</Text>
          </View>

          {/* Comutatorul de firmă: numai cine are mai multe (consultant, platformă). */}
          {isMultiCompany(session?.role) ? (
            <Link
              testID="pick-company"
              icon="building"
              title={strings.mobile.company}
              value={session?.tenantName ?? strings.mobile.pickCompany}
              onPress={() => router.push("/firme")}
            />
          ) : null}

          <Link
            testID="devices"
            icon="phone"
            title={strings.mobile.devices}
            onPress={() => router.push("/dispozitive")}
          />

          <Pressable
            testID="logout"
            style={({ pressed }) => [rowStyles.row, rowStyles.sep, pressed && rowStyles.pressed]}
            onPress={signOut}
          >
            <Text style={[rowStyles.title, { color: colors.redText }]}>{strings.nav.logout}</Text>
          </Pressable>
        </Group>
      </View>
    </ScrollView>
  );
}

function Link({ testID, icon, title, value, onPress }: {
  testID: string;
  icon: "building" | "phone";
  title: string;
  value?: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      style={({ pressed }) => [rowStyles.row, rowStyles.sep, styles.link, pressed && rowStyles.pressed]}
    >
      <Icon name={icon} size={21} color={colors.ink2} />
      <View style={styles.linkText}>
        <Text style={rowStyles.title}>{title}</Text>
        {value ? <Text style={rowStyles.sub} numberOfLines={1}>{value}</Text> : null}
      </View>
      <Icon name="right" size={18} color={colors.ink3} />
    </Pressable>
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
  link: { flexDirection: "row", alignItems: "center", gap: 12 },
  linkText: { flex: 1 },
});
