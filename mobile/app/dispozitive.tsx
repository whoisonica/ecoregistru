import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { Stack } from "expo-router";
import { Alert, ScrollView, StyleSheet, Text, View } from "react-native";

import { devices, removeDevice } from "../src/api";
import { PrimaryButton } from "../src/components/Form";
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
 * <p>„Scoate” (`DELETE /auth/devices/{id}`, 26.09.2026): un telefon pierdut sau al unui om plecat iese
 * din cont fără să se schimbe parola tuturor. Cu confirmare, fiindcă nu se desface. Nu și pe „telefonul
 * ăsta”: pentru el e „Ieși din cont”, care stinge și tokenul de push.
 */
export default function DispozitiveScreen() {
  const { auth, session } = useSession();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ["devices"],
    queryFn: () => devices(auth!),
    enabled: !!auth,
  });
  const remove = useMutation({
    mutationFn: (id: string) => removeDevice(auth!, id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["devices"] }),
    onError: () => Alert.alert(strings.mobile.deviceRemoveFailed),
  });
  const confirmRemove = (id: string, name: string) =>
    Alert.alert(strings.mobile.deviceRemoveTitle(name), strings.mobile.deviceRemoveBody, [
      { text: strings.mobile.deviceRemoveCancel, style: "cancel" },
      { text: strings.mobile.deviceRemoveConfirm, style: "destructive", onPress: () => remove.mutate(id) },
    ]);

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
                {d.id === session?.deviceSessionId ? (
                  <Chip label={strings.mobile.deviceThis} tone="ok" />
                ) : (
                  <PrimaryButton
                    tone="quiet"
                    label={strings.mobile.deviceRemove}
                    testID="device-remove"
                    disabled={remove.isPending}
                    onPress={() => confirmRemove(d.id, d.deviceName)}
                  />
                )}
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
