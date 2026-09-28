import { useQueryClient } from "@tanstack/react-query";
import { strings } from "@web/strings";
import { useRouter } from "expo-router";
import { useState } from "react";
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { useCompany } from "../../src/company";
import { PrimaryButton } from "../../src/components/Form";
import { LightHead } from "../../src/components/LightHead";
import { OfflineBand } from "../../src/components/OfflineBand";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { formatDate } from "../../src/format";
import { useHandoverData } from "../../src/handover";
import { drain, remove, retryNow, useOutbox, type OutboxItem } from "../../src/outbox";
import { pickAvizPhoto } from "../../src/photo";
import { useSession } from "../../src/session";
import { colors, fonts } from "../../src/theme";
import { useDraft } from "../../src/useDraft";
import { Tile } from "../../src/components/Tile";

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
  const [cameraDenied, setCameraDenied] = useState<"denied" | "failed" | false>(false);
  // F4: un formular închis pe la mijloc se reia de aici („Continui predarea de la 14:20?”).
  const { draft, discard } = useDraft(session?.email, session?.tenantId ?? undefined);

  // Colectorul pur n-are ecranul „Generare”: intrările lui trec prin cântar (M2).
  const collectorOnly = company.data?.type === "COLLECTOR";

  const open = async (source: "camera" | "gallery" | "none") => {
    if (source === "none") return router.push("/predare");
    const photo = await pickAvizPhoto(source);
    if (photo === "denied" || photo === "failed") return setCameraDenied(photo);
    if (!photo) return;
    setCameraDenied(false);
    router.push({ pathname: "/predare", params: { photo } });
  };

  return (
    <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
      <LightHead title={m.tabAdd} subtitle={m.addSub} />
      <OfflineBand />
      <View style={styles.body}>
        {collectorOnly ? (
          <Group>
            <Note>{m.collectorLater}</Note>
          </Group>
        ) : (
          <>
            {draft ? (
              <Group>
                <View style={[rowStyles.row, styles.draftRow]} testID="draft-card">
                  <Tile icon="doc" tone="warn" />
                  <View style={styles.outText}>
                    <Text style={rowStyles.title}>{`${m.draftTitle} · ${m.draftSince(timeOf(draft.savedAt))}`}</Text>
                    <Text style={rowStyles.sub} numberOfLines={1}>
                      {[draft.summary.wasteCode ?? m.draftNoCode, draft.summary.quantity].filter(Boolean).join(" · ")}
                    </Text>
                  </View>
                </View>
                <View style={[rowStyles.sep, styles.draftActions]}>
                  <Pressable onPress={() => discard()} testID="draft-discard" style={styles.draftAction}>
                    <Text style={styles.linkText}>{m.draftDiscard}</Text>
                  </Pressable>
                  <Pressable onPress={() => router.push("/predare?draft=1")} testID="draft-resume" style={[styles.draftAction, styles.draftResume]}>
                    <Text style={styles.draftResumeText}>{m.draftResume}</Text>
                  </Pressable>
                </View>
              </Group>
            ) : null}
            <Text style={styles.hint}>{m.addHint}</Text>
            <PrimaryButton label={m.snapAviz} onPress={() => open("camera")} testID="snap-aviz" />
            {cameraDenied ? <Note tone="alert">{cameraDenied === "failed" ? m.photoPickFailed : m.cameraDenied}</Note> : null}
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
  const router = useRouter();
  const queryClient = useQueryClient();
  const hasPending = items.some((i) => i.state === "PENDING");
  // B6: butonul arată că lucrează; o trimitere deja în aer poate ține până la 30 s pe semnal slab.
  const [sending, setSending] = useState(false);
  const sendNow = async () => {
    if (!auth || !session) return;
    setSending(true);
    try {
      await retryNow(session.email);
      const sent = await drain(auth, session.email).catch(() => 0);
      if (sent) queryClient.invalidateQueries({ queryKey: ["movements"] });
    } finally {
      setSending(false);
    }
  };
  // B4: scoaterea nu se mai face dintr-o atingere; o predare care n-a ajuns pe server s-ar pierde.
  const confirmRemove = (item: OutboxItem) =>
    Alert.alert(m.outboxRemoveTitle, item.movementId ? m.outboxRemoveKeepsMovement : m.outboxRemoveLoses, [
      { text: m.outboxRemoveCancel, style: "cancel" },
      { text: m.outboxRemove, style: "destructive", onPress: () => remove(item.id) },
    ]);
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
              ) : item.error ? (
                <Text style={rowStyles.sub}>{m.outboxServerError(item.error)}</Text>
              ) : null}
            </View>
            {item.state === "REJECTED" ? (
              <View style={styles.outSide}>
                <Chip label={m.outboxRejected} tone="bad" />
                {/* Numai cât predarea nu e pe server: una cu poza căzută se corectează din ecranul ei. */}
                {!item.movementId ? (
                  <Pressable onPress={() => router.push(`/predare?outbox=${item.id}`)} testID="outbox-fix">
                    <Text style={styles.linkText}>{m.outboxFix}</Text>
                  </Pressable>
                ) : null}
                <Pressable onPress={() => confirmRemove(item)} testID="outbox-remove">
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
          <PrimaryButton tone="quiet" label={sending ? m.outboxSending : m.outboxSendNow} onPress={sendNow} disabled={sending} testID="outbox-send" />
        </>
      ) : null}
    </>
  );
}

/** „14:20” din momentul ciornei. */
function timeOf(ms: number) {
  const d = new Date(ms);
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  draftRow: { flexDirection: "row", alignItems: "center", gap: 12 },
  draftActions: { flexDirection: "row" },
  draftAction: { flex: 1, alignItems: "center", paddingVertical: 12 },
  draftResume: { borderLeftWidth: StyleSheet.hairlineWidth, borderLeftColor: colors.separator },
  draftResumeText: { fontFamily: fonts.sansSemiBold, fontSize: 15, color: colors.greenText },
  scroll: { paddingBottom: 120 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 10 },
  hint: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2, paddingHorizontal: 4 },
  link: { alignItems: "center", paddingVertical: 10 },
  linkText: { fontFamily: fonts.sansMedium, fontSize: 15, color: colors.greenText },
  outRow: { flexDirection: "row", alignItems: "center", gap: 10 },
  outText: { flex: 1 },
  outSide: { alignItems: "flex-end", gap: 6 },
  error: { fontFamily: fonts.sans, fontSize: 13, color: colors.redText, marginTop: 4 },
});
