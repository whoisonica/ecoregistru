import { useQuery } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { Stack } from "expo-router";
import { ScrollView, StyleSheet, Text, View } from "react-native";

import { devices } from "../src/api";
import { Chip, Group, Note, rowStyles, SectionHead } from "../src/components/Rows";
import { Icon } from "../src/components/Icon";
import { formatDate } from "../src/format";
import { useSession } from "../src/session";
import { colors } from "../src/theme";

/**
 * G1 — „Dispozitive conectate”: ce telefoane mai pot intra în cont fără parolă.
 *
 * <p>Care e cel din mână se vede de aici, nu de la server: aplicația își știe id-ul sesiunii de la
 * login sau de la ultima reîmprospătare.
 *
 * <p>Scoaterea unui telefon (`DELETE /auth/devices/{id}`) e scrisă pe server și probată
 * (`DeviceSessionIT`), dar butonul nu e aici: pe ecranul ăsta nu e nimic de <em>reparat</em> în M1a,
 * iar o ștergere cu confirmare vine cu felia în care aplicația chiar scrie (M1b).
 */
export default function DispozitiveScreen() {
  const { auth, session } = useSession();
  const query = useQuery({
    queryKey: ["devices"],
    queryFn: () => devices(auth!),
    enabled: !!auth,
  });

  return (
    <>
      <Stack.Screen
        options={{
          headerShown: true,
          title: strings.mobile.devices,
          // Fără asta iOS scrie numele rutei părinte pe butonul de întoarcere — „(tabs)".
          headerBackTitle: strings.mobile.tabHome,
        }}
      />
      <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
        <SectionHead>{strings.mobile.devices}</SectionHead>
        <Group>
          {query.isError ? (
            <Note tone="alert">{strings.mobile.devicesError}</Note>
          ) : !query.data ? (
            <Note>{" "}</Note>
          ) : query.data.length === 0 ? (
            <Note>{strings.mobile.devicesEmpty}</Note>
          ) : (
            query.data.map((d, i) => (
              <View key={d.id} testID="device-row" style={[rowStyles.row, i > 0 && rowStyles.sep, styles.row]}>
                <Icon name="phone" size={21} color={colors.ink2} />
                <View style={styles.body}>
                  <Text style={rowStyles.title} numberOfLines={1}>{d.deviceName}</Text>
                  <Text style={rowStyles.sub}>{strings.mobile.deviceLastUsed(formatDate(d.lastUsedAt))}</Text>
                </View>
                {d.id === session?.deviceSessionId ? <Chip label={strings.mobile.deviceThis} tone="ok" /> : null}
              </View>
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
