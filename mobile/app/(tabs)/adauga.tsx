import { useQueryClient } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { ImageManipulator, SaveFormat } from "expo-image-manipulator";
import * as ImagePicker from "expo-image-picker";
import { useRouter } from "expo-router";
import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { useCompany } from "../../src/company";
import { PrimaryButton } from "../../src/components/Form";
import { GraphiteHeader } from "../../src/components/GraphiteHeader";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { formatDate } from "../../src/format";
import { useHandoverData } from "../../src/handover";
import { drain, remove, retryNow, useOutbox, type OutboxItem } from "../../src/outbox";
import { useSession } from "../../src/session";
import { colors, fonts } from "../../src/theme";

const m = strings.mobile;

/**
 * „+” din bara de jos. La M1b: predarea deșeului propriu, cu poza avizului, și coada a ce n-a plecat.
 *
 * <p>Ecranul încarcă listele formularului (`useHandoverData`) chiar dacă omul nu apasă nimic: așa
 * ajung pe telefon cât e semnal, iar formularul merge și la rampa fără.
 */
export default function AddScreen() {
  const { session, auth } = useSession();
  const router = useRouter();
  const company = useCompany();
  useHandoverData();
  const outbox = useOutbox(session?.email);
  const [cameraDenied, setCameraDenied] = useState(false);

  // Colectorul pur n-are ecranul „Generare”: intrările lui trec prin cântar (M2).
  const collectorOnly = company.data?.type === "COLLECTOR";

  const open = async (source: "camera" | "gallery" | "none") => {
    if (source === "none") return router.push("/predare");
    let result: ImagePicker.ImagePickerResult;
    if (source === "camera") {
      const permission = await ImagePicker.requestCameraPermissionsAsync();
      if (!permission.granted) return setCameraDenied(true);
      result = await ImagePicker.launchCameraAsync({ mediaTypes: ["images"], quality: 1 });
    } else {
      result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ["images"], quality: 1 });
    }
    if (result.canceled || !result.assets[0]) return;
    setCameraDenied(false);
    const photo = await shrink(result.assets[0]);
    router.push({ pathname: "/predare", params: { photo } });
  };

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <GraphiteHeader title={m.tabAdd} meta={(session?.tenantName ?? strings.appName).toUpperCase()} />
      <View style={styles.body}>
        {collectorOnly ? (
          <Group>
            <Note>{m.collectorLater}</Note>
          </Group>
        ) : (
          <>
            <Text style={styles.hint}>{m.addHint}</Text>
            <PrimaryButton label={m.snapAviz} onPress={() => open("camera")} testID="snap-aviz" />
            {cameraDenied ? <Note tone="alert">{m.cameraDenied}</Note> : null}
            <PrimaryButton tone="quiet" label={m.pickFromGallery} onPress={() => open("gallery")} testID="pick-aviz" />
            <Pressable onPress={() => open("none")} testID="without-photo" style={styles.link}>
              <Text style={styles.linkText}>{m.withoutPhoto}</Text>
            </Pressable>
          </>
        )}

        {outbox.length > 0 ? <Outbox items={outbox} /> : null}
      </View>
    </ScrollView>
  );
}

/**
 * Ce n-a ajuns încă pe server. „De trimis” pleacă singur (`useOutboxSync`); butonul doar sare pauza.
 * „Refuzată” rămâne până o scoate omul, cu propoziția serverului — aceeași cerere ar fi refuzată iar.
 */
function Outbox({ items }: { items: OutboxItem[] }) {
  const { session, auth } = useSession();
  const queryClient = useQueryClient();
  const hasPending = items.some((i) => i.state === "PENDING");
  const sendNow = async () => {
    if (!auth || !session) return;
    await retryNow(session.email);
    const sent = await drain(auth, session.email).catch(() => 0);
    if (sent) queryClient.invalidateQueries({ queryKey: ["movements"] });
  };
  return (
    <>
      <SectionHead>{`${m.outboxTitle} · ${items.length}`}</SectionHead>
      <Group>
        {items.map((item, i) => (
          <View key={item.id} testID="outbox-row" style={[rowStyles.row, i > 0 && rowStyles.sep, styles.outRow]}>
            <View style={styles.outText}>
              <Text style={rowStyles.mono}>
                {item.summary.wasteCode} · {item.summary.quantity}
              </Text>
              <Text style={rowStyles.sub} numberOfLines={1}>
                {[formatDate(item.summary.date), item.summary.partnerName].filter(Boolean).join(" · ")}
              </Text>
              {item.state === "REJECTED" ? (
                <Text style={styles.error}>
                  {item.movementId ? `${m.outboxPhotoFailed} ` : ""}
                  {item.error}
                </Text>
              ) : null}
            </View>
            {item.state === "REJECTED" ? (
              <View style={styles.outSide}>
                <Chip label={m.outboxRejected} tone="bad" />
                <Pressable onPress={() => remove(item.id)} testID="outbox-remove">
                  <Text style={styles.linkText}>{m.outboxRemove}</Text>
                </Pressable>
              </View>
            ) : (
              <Chip label={m.outboxPending} tone="warn" />
            )}
          </View>
        ))}
      </Group>
      {hasPending ? (
        <>
          <Text style={styles.hint}>{m.outboxHint}</Text>
          <PrimaryButton tone="quiet" label={m.outboxSendNow} onPress={sendNow} testID="outbox-send" />
        </>
      ) : null}
    </>
  );
}

/**
 * Poza se micșorează pe telefon înainte de orice (todo-mobil §6): latura lungă la 2000 px, JPEG 0,7
 * — în jur de un megaoctet, destul ca textul să se citească și departe de plafonul de 10 MB.
 */
async function shrink(asset: ImagePicker.ImagePickerAsset): Promise<string> {
  const wide = asset.width >= asset.height;
  const context = ImageManipulator.manipulate(asset.uri);
  if (Math.max(asset.width, asset.height) > 2000) context.resize(wide ? { width: 2000 } : { height: 2000 });
  const image = await context.renderAsync();
  const saved = await image.saveAsync({ compress: 0.7, format: SaveFormat.JPEG });
  return saved.uri;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  body: { padding: 16, gap: 10 },
  hint: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2, paddingHorizontal: 4 },
  link: { alignItems: "center", paddingVertical: 10 },
  linkText: { fontFamily: fonts.sansMedium, fontSize: 15, color: colors.greenText },
  outRow: { flexDirection: "row", alignItems: "center", gap: 10 },
  outText: { flex: 1 },
  outSide: { alignItems: "flex-end", gap: 6 },
  error: { fontFamily: fonts.sans, fontSize: 13, color: colors.redText, marginTop: 4 },
});
