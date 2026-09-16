import { useQuery, useQueryClient } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { Stack, useRouter } from "expo-router";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { companies } from "../src/api";
import { Group, Note, rowStyles, SectionHead } from "../src/components/Rows";
import { Icon } from "../src/components/Icon";
import { useSession } from "../src/session";
import { colors } from "../src/theme";

/**
 * Comutatorul de firmă al consultantului și al platformei — `X-Tenant-Id` pe fiecare cerere.
 *
 * <p>Lista vine de la `/companies`, pe care serverul o îngustează singur la firmele cabinetului
 * (P2.13). Alegerea golește cache-ul de interogări: rândurile firmei părăsite n-au ce căuta sub
 * numele celei alese, iar cheile de interogare poartă tenantul tocmai ca asta să se vadă.
 */
export default function FirmeScreen() {
  const { auth, session, switchTenant } = useSession();
  const router = useRouter();
  const qc = useQueryClient();

  const query = useQuery({
    queryKey: ["companies"],
    queryFn: () => companies(auth!),
    enabled: !!auth,
    staleTime: 5 * 60 * 1000,
  });

  const pick = async (id: string, name: string) => {
    await switchTenant(id, name);
    await qc.invalidateQueries();
    router.back();
  };

  return (
    <>
      <Stack.Screen
        options={{
          headerShown: true,
          title: strings.mobile.pickCompany,
          // Fără asta iOS scrie numele rutei părinte pe butonul de întoarcere — „(tabs)".
          headerBackTitle: strings.mobile.tabHome,
        }}
      />
      <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
        <SectionHead>{strings.mobile.company}</SectionHead>
        <Group>
          {query.isError ? (
            <Note tone="alert">{strings.mobile.lcdError}</Note>
          ) : !query.data ? (
            <Note>{" "}</Note>
          ) : query.data.length === 0 ? (
            <Note>{strings.mobile.pickCompanyEmpty}</Note>
          ) : (
            query.data.map((c, i) => (
              <Pressable
                key={c.id}
                testID="company-row"
                onPress={() => pick(c.id, c.name)}
                style={({ pressed }) => [rowStyles.row, i > 0 && rowStyles.sep, styles.row, pressed && rowStyles.pressed]}
              >
                <View style={styles.body}>
                  <Text style={rowStyles.title} numberOfLines={1}>{c.name}</Text>
                  <Text style={rowStyles.sub}>
                    {c.cui} · {strings.enums.companyType[c.type]}
                  </Text>
                </View>
                {c.id === session?.tenantId ? <Icon name="check" size={20} color={colors.greenText} strokeWidth={2.4} /> : null}
              </Pressable>
            ))
          )}
        </Group>
      </ScrollView>
    </>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { padding: 16, gap: 8, paddingBottom: 40 },
  row: { flexDirection: "row", alignItems: "center", gap: 12 },
  body: { flex: 1 },
});
