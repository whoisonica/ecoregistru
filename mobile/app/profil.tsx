import { strings } from "@web/strings";
import Constants from "expo-constants";
import * as Linking from "expo-linking";
import { Stack, useRouter } from "expo-router";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { isMultiCompany } from "../src/auth";
import { useCompany } from "../src/company";
import { Icon, type IconName } from "../src/components/Icon";
import { LightHead } from "../src/components/LightHead";
import { Group, rowStyles, SectionHead } from "../src/components/Rows";
import { Tile } from "../src/components/Tile";
import { useHandoverData } from "../src/handover";
import { useSession } from "../src/session";
import { colors, fonts } from "../src/theme";

const m = strings.mobile;
const POLICY_URL = "https://app.wastehouse.ro/confidentialitate";

/**
 * Profil (27.09.2026): tot ce era pe Acasă și nu era despre azi — contul, firma (comutatorul, la
 * consultant), dispozitivele conectate, despre, ieșirea. Acasă nu mai e pagină de setări.
 *
 * <p>Notificările pe feluri și deblocarea cu Face ID (D8) nu sunt aici: n-au încă nici server, nici
 * decizie, iar un comutator care nu face nimic e o minciună pe ecran.
 */
export default function ProfilScreen() {
  const { session, signOut } = useSession();
  const router = useRouter();
  const company = useCompany();
  const { workPoints } = useHandoverData();
  const role = session ? strings.enums.role[session.role] : "";
  const version = Constants.expoConfig?.version ?? "";

  const firmSub = company.data
    ? [
        `CUI ${company.data.cui}`,
        strings.enums.companyType[company.data.type],
        workPoints.data ? m.workPointsCount(workPoints.data.filter((w) => w.active).length) : null,
      ]
        .filter(Boolean)
        .join(" · ")
    : session?.tenantId
      ? " "
      : m.pickCompany;

  return (
    <>
      <Stack.Screen options={{ headerShown: false }} />
      <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
        <LightHead
          title={m.profile}
          subtitle={[session?.email, role].filter(Boolean).join(" · ")}
          back={{ label: m.tabHome, onPress: () => router.back() }}
          right={null}
        />
        <View style={styles.body}>
          <SectionHead>{m.profileFirm}</SectionHead>
          <Group>
            <Row
              icon="building"
              title={session?.tenantName ?? session?.consultancyName ?? m.pickCompany}
              sub={firmSub}
              onPress={isMultiCompany(session?.role) ? () => router.push("/firme") : undefined}
              testID={isMultiCompany(session?.role) ? "pick-company" : "firm-row"}
            />
          </Group>

          <SectionHead>{m.profilePhone}</SectionHead>
          <Group>
            <Row icon="phone" title={m.devices} onPress={() => router.push("/dispozitive")} testID="devices" />
          </Group>

          <SectionHead>{m.profileHelp}</SectionHead>
          <Group>
            <Row icon="info" title={m.about} sub={[version ? m.aboutVersion(version) : null, m.privacyPolicy].filter(Boolean).join(" · ")} onPress={() => Linking.openURL(POLICY_URL)} testID="about" />
          </Group>

          <Pressable testID="logout" onPress={signOut} style={({ pressed }) => [styles.logout, pressed && { opacity: 0.8 }]} accessibilityRole="button">
            <Icon name="logout" size={20} color={colors.redText} strokeWidth={2} />
            <Text style={styles.logoutText}>{strings.nav.logout}</Text>
          </Pressable>
        </View>
      </ScrollView>
    </>
  );
}

function Row({ icon, title, sub, onPress, testID }: { icon: IconName; title: string; sub?: string; onPress?: () => void; testID: string }) {
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      disabled={!onPress}
      style={({ pressed }) => [rowStyles.row, styles.row, pressed && rowStyles.pressed]}
    >
      <Tile icon={icon} tone="quiet" />
      <View style={styles.rowBody}>
        <Text style={rowStyles.title} numberOfLines={1}>{title}</Text>
        {sub ? <Text style={rowStyles.sub} numberOfLines={2}>{sub}</Text> : null}
      </View>
      {onPress ? <Icon name="right" size={18} color={colors.ink3} /> : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 60 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 8 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  rowBody: { flex: 1 },
  logout: {
    marginTop: 16,
    minHeight: 54,
    borderRadius: 15,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.separator,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  },
  logoutText: { fontFamily: fonts.sansSemiBold, fontSize: 16.5, color: colors.redText },
});
