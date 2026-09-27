import { strings } from "@web/strings";
import * as Sharing from "expo-sharing";
import { Stack, useLocalSearchParams, useRouter } from "expo-router";
import { useEffect, useRef, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { ApiError, downloadMovementPdf, UnauthorizedError } from "../src/api";
import { Icon, type IconName } from "../src/components/Icon";
import { Group, rowStyles, SectionHead } from "../src/components/Rows";
import { Tile } from "../src/components/Tile";
import { formatDate } from "../src/format";
import { haptic } from "../src/haptics";
import { lastSaved, onSent, sentMovementId } from "../src/lastSaved";
import { useOnline } from "../src/online";
import { useOutboxState } from "../src/outbox";
import { ticketStatus, type TicketStatus } from "../src/outboxRules";
import { useSession } from "../src/session";
import { colors, fonts, radius } from "../src/theme";

const m = strings.mobile;

type Status = TicketStatus;

/**
 * Ecranul de după „Salvează predarea” (F1, din prototipul aprobat pe 15.09, construit pe 27.09.2026):
 * bonul termic, bifa verde cu o vibrație scurtă când predarea a ajuns în registru, galben cât stă în
 * telefon, roșu dacă serverul a refuzat-o. Dedesubt „Ce urmează”. Până acum aplicația făcea doar
 * `router.back()`, și omul nu știa dacă a reușit.
 *
 * <p>Starea vine din coadă, nu din ce credem: rândul e încă „de trimis” → în telefon; a dispărut și
 * `noteSent` i-a dat id-ul → în registru; „refuzată” → cuvântul serverului. Bonul e din `lastSaved`,
 * ce a apăsat omul, cu numărul de pe server completat când vine.
 */
export default function GataScreen() {
  const { outbox: id } = useLocalSearchParams<{ outbox: string }>();
  const { session, auth, signOut } = useSession();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const online = useOnline();
  const { items: rows, loaded } = useOutboxState(session?.email);
  const row = rows.find((r) => r.id === id);
  const saved = lastSaved();
  const ticket = saved?.outboxId === id ? saved : null;

  const [movementId, setMovementId] = useState(() => (id ? sentMovementId(id) : null));
  useEffect(() => onSent(() => setMovementId(id ? sentMovementId(id) : null)), [id]);

  const status = ticketStatus({ loaded, online, row });

  // Vibrația o singură dată, când intră în registru; refuzul are a lui.
  const felt = useRef<Status | null>(null);
  useEffect(() => {
    if (felt.current === status) return;
    if (status === "sent" || status === "photoFailed") haptic.success();
    else if (status === "rejected") haptic.error();
    felt.current = status;
  }, [status]);

  const tone = status === "sent" || status === "photoFailed" ? "ok" : status === "rejected" ? "bad" : "warn";
  const title = status === "sent" || status === "photoFailed" ? m.doneSent : status === "rejected" ? m.doneRejected : status === "sending" ? m.doneSending : m.doneQueued;
  const sub =
    status === "sent"
      ? ticket?.hasPhoto
        ? m.doneSentSub
        : m.doneSentSubNoPhoto
      : status === "photoFailed"
        ? m.donePhotoFailed
        : status === "rejected"
          ? `${row?.error ?? ""}\n${m.doneRejectedSub}`.trim()
          : m.doneQueuedSub;

  // Anexa 3 pentru șofer: PDF-ul de pe server, deci abia după ce predarea a ajuns acolo.
  const serverId = movementId ?? row?.movementId ?? null;
  const [shareBusy, setShareBusy] = useState(false);
  const [shareError, setShareError] = useState<string | null>(null);
  const shareAnexa3 = async () => {
    if (!auth || !serverId || !ticket || shareBusy) return;
    setShareBusy(true);
    setShareError(null);
    try {
      if (!(await Sharing.isAvailableAsync())) return setShareError(m.shareUnavailable);
      const uri = await downloadMovementPdf(auth, serverId, "anexa3", `anexa3-${ticket.wasteCode.replace(/\s/g, "")}-${ticket.date}.pdf`);
      await Sharing.shareAsync(uri, { mimeType: "application/pdf", UTI: "com.adobe.pdf" });
    } catch (e) {
      if (e instanceof UnauthorizedError) return signOut();
      setShareError(e instanceof ApiError ? (e.serverMessage ?? m.movementDocumentOffline) : m.movementDocumentOffline);
    } finally {
      setShareBusy(false);
    }
  };

  const bg = tone === "ok" ? colors.greenSoft : tone === "bad" ? colors.redSoft : colors.amberSoft;
  const icon: IconName = tone === "ok" ? "check" : tone === "bad" ? "alert" : "clock";
  const circle = tone === "ok" ? colors.green : tone === "bad" ? colors.red : colors.amber;

  return (
    <>
      <Stack.Screen options={{ headerShown: false, gestureEnabled: false }} />
      <ScrollView style={[styles.fill, { backgroundColor: bg }]} contentContainerStyle={[styles.scroll, { paddingTop: insets.top + 56, paddingBottom: insets.bottom + 24 }]}>
        <View style={styles.done} testID={`gata-${status}`}>
          <View style={[styles.circle, { backgroundColor: circle }]}>
            <Icon name={icon} size={40} color={colors.onAccent} strokeWidth={3} />
          </View>
          <Text style={styles.title} testID="gata-title">{title}</Text>
          <Text style={styles.sub}>{sub}</Text>
        </View>

        {ticket ? (
          <View style={styles.ticketWrap}>
            <View style={styles.ticket} testID="ticket">
              <Text style={styles.ticketHead}>{m.ticketTitle}</Text>
              <Row k={m.ticketNo} v={serverId ? `WH-${serverId.slice(0, 8).toUpperCase()}` : m.ticketNoPending} />
              <Row k={m.ticketDate} v={formatDate(ticket.date)} />
              <Dash />
              <Row k={m.ticketWaste} v={`${ticket.wasteCode} ${ticket.wasteCodeName}`.trim()} />
              <Row k={m.ticketQty} v={ticket.quantity} strong />
              {ticket.partnerName ? <Row k={m.ticketTo} v={ticket.partnerName} /> : null}
              {ticket.operationCode ? <Row k={m.ticketOp} v={strings.enums.wasteOperationCode[ticket.operationCode as keyof typeof strings.enums.wasteOperationCode] ?? ticket.operationCode} /> : null}
              <Dash />
              {ticket.documentReference ? <Row k={m.ticketDoc} v={ticket.documentReference} /> : null}
              {ticket.vehicle ? <Row k={m.ticketVehicle} v={ticket.vehicle} /> : null}
              {ticket.driverName ? <Row k={m.ticketDriver} v={ticket.driverName} /> : null}
              <Row
                k={m.ticketProof}
                v={ticket.hasPhoto ? `${m.ticketProofPhoto} · ${status === "sent" ? m.ticketProofUploaded : m.ticketProofPending}` : m.ticketNoProof}
              />
              {ticket.workPointName ? <Row k={m.ticketFrom} v={ticket.workPointName} /> : null}
            </View>
            <View style={styles.zigzag}>
              {Array.from({ length: 24 }, (_, i) => (
                <View key={i} style={styles.tooth} />
              ))}
            </View>
          </View>
        ) : null}

        <SectionHead>{m.whatNext}</SectionHead>
        <Group>
          {status === "rejected" ? (
            <Next icon="again" tone="bad" title={m.outboxFix} sub={m.doneRejectedSub} onPress={() => router.replace({ pathname: "/predare", params: { outbox: id } })} testID="next-fix" />
          ) : null}
          {ticket?.partnerName ? (
            <Next
              icon="doc"
              tone={serverId ? "ok" : "quiet"}
              title={shareBusy ? m.sendDossierBusy : m.anexa3ForDriver}
              sub={shareError ?? (serverId ? m.anexa3ForDriverSub : m.anexa3ForDriverLater)}
              onPress={serverId ? shareAnexa3 : undefined}
              sep={status === "rejected"}
              testID="next-anexa3"
            />
          ) : null}
          <Next
            icon="again"
            tone="quiet"
            title={m.anotherSame}
            sub={m.anotherSameSub}
            onPress={() => router.replace({ pathname: "/predare", params: { again: "1" } })}
            sep={status === "rejected" || !!ticket?.partnerName}
            testID="next-again"
          />
        </Group>

        <Pressable testID="gata-done" onPress={() => router.back()} style={({ pressed }) => [styles.doneBtn, pressed && { opacity: 0.8 }]} accessibilityRole="button">
          <Text style={styles.doneBtnText}>{m.done}</Text>
        </Pressable>
      </ScrollView>
    </>
  );
}

function Row({ k, v, strong }: { k: string; v: string; strong?: boolean }) {
  return (
    <View style={styles.row}>
      <Text style={[styles.k, strong && styles.strong]}>{k}</Text>
      <Text style={[styles.v, strong && styles.strong]} numberOfLines={2}>{v}</Text>
    </View>
  );
}

function Dash() {
  return <View style={styles.dash} />;
}

function Next({ icon, tone, title, sub, onPress, sep, testID }: {
  icon: IconName;
  tone: "ok" | "bad" | "quiet";
  title: string;
  sub: string;
  onPress?: () => void;
  sep?: boolean;
  testID: string;
}) {
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      disabled={!onPress}
      style={({ pressed }) => [rowStyles.row, sep && rowStyles.sep, styles.next, pressed && rowStyles.pressed, !onPress && { opacity: 0.6 }]}
    >
      <Tile icon={icon} tone={tone} />
      <View style={styles.nextBody}>
        <Text style={rowStyles.title}>{title}</Text>
        <Text style={rowStyles.sub} numberOfLines={3}>{sub}</Text>
      </View>
      {onPress ? <Icon name="right" size={18} color={colors.ink3} /> : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  scroll: { paddingHorizontal: 16, gap: 14 },
  done: { alignItems: "center", gap: 8, paddingHorizontal: 10, paddingBottom: 6 },
  circle: { width: 76, height: 76, borderRadius: 38, alignItems: "center", justifyContent: "center", marginBottom: 6 },
  title: { fontFamily: fonts.sansSemiBold, fontSize: 24, letterSpacing: -0.4, color: colors.ink, textAlign: "center" },
  sub: { fontFamily: fonts.sans, fontSize: 15, color: colors.ink2, textAlign: "center", maxWidth: 300 },
  ticketWrap: { marginBottom: 6 },
  ticket: { backgroundColor: colors.card, paddingHorizontal: 18, paddingTop: 16, paddingBottom: 12, borderTopLeftRadius: 4, borderTopRightRadius: 4, gap: 6 },
  ticketHead: { fontFamily: fonts.monoMedium, fontSize: 12, letterSpacing: 1, color: colors.ink, textAlign: "center", paddingBottom: 4 },
  row: { flexDirection: "row", justifyContent: "space-between", gap: 10 },
  k: { fontFamily: fonts.mono, fontSize: 13, color: colors.ink2 },
  v: { flex: 1, fontFamily: fonts.mono, fontSize: 13, color: colors.ink, textAlign: "right" },
  strong: { fontFamily: fonts.monoMedium, fontSize: 15, color: colors.ink },
  dash: { borderTopWidth: 1, borderTopColor: colors.ink3, borderStyle: "dashed", marginVertical: 4 },
  zigzag: { flexDirection: "row", overflow: "hidden", height: 8, justifyContent: "space-between", paddingHorizontal: 2 },
  tooth: { width: 10, height: 10, backgroundColor: colors.card, transform: [{ rotate: "45deg" }], marginTop: -6 },
  next: { flexDirection: "row", alignItems: "center", gap: 12 },
  nextBody: { flex: 1 },
  doneBtn: { minHeight: 54, borderRadius: radius.button, backgroundColor: colors.card, borderWidth: 1, borderColor: colors.separator, alignItems: "center", justifyContent: "center", marginTop: 6 },
  doneBtnText: { fontFamily: fonts.sansSemiBold, fontSize: 17, color: colors.ink },
});
