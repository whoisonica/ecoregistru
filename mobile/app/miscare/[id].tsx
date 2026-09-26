import { useQuery } from "@tanstack/react-query";
import { canPrintAnexa3, canPrintAviz, movementPdfName } from "@/lib/movementPrint";
import { strings } from "@web/strings";
import type { WasteMovement } from "@web/types";
import { Stack, useLocalSearchParams, useRouter } from "expo-router";
import * as Sharing from "expo-sharing";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { ApiError, downloadAttachment, downloadMovementPdf, movement, UnauthorizedError } from "../../src/api";
import { canWrite } from "../../src/auth";
import { Bin } from "../../src/components/Bin";
import { PrimaryButton } from "../../src/components/Form";
import { Icon } from "../../src/components/Icon";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { formatDate, formatKg } from "../../src/format";
import { canEditOnPhone } from "../../src/movementEdit";
import { useSession } from "../../src/session";
import { colors, fonts } from "../../src/theme";

const t = strings.movements;
const m = strings.mobile;
const e = strings.enums;

/**
 * M1e — predarea deschisă: rândul din Mișcări duce aici, cu tot ce e în ea, citit din
 * `GET /movements/{id}` (proprietarul, 26.09.2026: „nu pot face Anexa 3 sau avizul, nici să văd ce e
 * în linii”).
 *
 * <p>Anexa 3 și avizul apar după aceeași regulă ca pe web (`lib/movementPrint.ts`); PDF-ul se scrie în
 * cache și se dă foii de partajare — vizualizatorul iOS, „Deschide cu” pe Android, de acolo tipărire,
 * mail, WhatsApp. Documentele sunt cele de pe server, neschimbate (G06, validarea Andreei).
 *
 * <p>„Corectează” (M1f) deschide formularul de predare cu rubricile ei — numai pe predările proprii de pe
 * Anexa 1, fără cântar (`canEditOnPhone`). Restul se corectează pe web, unde sunt toate rubricile.
 */
export default function MiscareScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { auth, session, signOut } = useSession();
  const query = useQuery({
    queryKey: ["movements", "one", session?.tenantId, id],
    queryFn: () => movement(auth!, id),
    enabled: !!auth && !!id,
  });

  useEffect(() => {
    if (query.error instanceof UnauthorizedError) signOut();
  }, [query.error, signOut]);

  const mv = query.data;
  return (
    <>
      <Stack.Screen
        options={{ headerShown: true, title: mv?.wasteCode ?? "", headerBackTitle: strings.nav.movements }}
      />
      <ScrollView style={styles.fill} contentContainerStyle={styles.scroll}>
        {query.isError ? (
          <Group>
            <Note tone="alert">{m.movementError}</Note>
          </Group>
        ) : !mv ? null : (
          <Details mv={mv} writer={canWrite(session?.role)} />
        )}
      </ScrollView>
    </>
  );
}

function Details({ mv, writer }: { mv: WasteMovement; writer: boolean }) {
  const router = useRouter();
  const operation = mv.operationCode ? e.wasteOperationCode[mv.operationCode] : null;
  const anexa3 = canPrintAnexa3(mv, writer);
  const aviz = canPrintAviz(mv, writer);

  return (
    <>
      <SectionHead>{m.movementWaste}</SectionHead>
      <Group>
        <View style={[rowStyles.row, styles.head]}>
          <Bin code={mv.wasteCode} hazardous={mv.hazardous} />
          <View style={styles.body}>
            <Text style={rowStyles.title}>{mv.wasteCodeName}</Text>
            <Text style={rowStyles.sub}>
              {mv.wasteCode} · {formatDate(mv.date)}
            </Text>
          </View>
          {mv.quantity == null ? (
            <Chip label={m.awaitingWeighing} tone="warn" />
          ) : (
            <Text style={rowStyles.mono} testID="movement-quantity">
              {formatKg(mv.quantity)} {e.unit[mv.unit]}
            </Text>
          )}
        </View>
        <Lines
          afterHead
          rows={[
            [m.movementWorkPoint, mv.workPointName],
            [t.operation, e.wasteOperation[mv.operation]],
            [t.operationCode, operation],
          ]}
        />
      </Group>

      <LinesGroup
        head={m.movementSheet}
        rows={[
          [t.physicalState, mv.physicalState && e.physicalState[mv.physicalState]],
          [t.storageType, mv.storageType && e.storageType[mv.storageType]],
          [t.treatmentMethod, mv.treatmentMethod && e.treatmentMethod[mv.treatmentMethod]],
          [t.transportMeans, mv.transportMeans && e.transportMeans[mv.transportMeans]],
          [t.wasteDestination, mv.wasteDestination && e.wasteDestination[mv.wasteDestination]],
        ]}
      />

      {mv.partnerName ? (
        <>
          <SectionHead>{m.movementPartner}</SectionHead>
          <Group>
            <View style={[rowStyles.row, styles.head]}>
              <Text style={[rowStyles.title, styles.body]}>{mv.partnerName}</Text>
              {mv.recipientAuthorizationExpired ? <Chip label={m.movementAuthorizationExpired} tone="bad" /> : null}
            </View>
            <Lines afterHead rows={[[t.partnerWorkPoint, mv.partnerWorkPointLabel]]} />
          </Group>
        </>
      ) : null}

      <LinesGroup
        head={m.movementTransport}
        rows={[
          [t.documentReference, mv.documentReference],
          [t.transportPartner, mv.transportPartnerName],
          [t.vehicleRegistration, mv.vehicleRegistration],
          [t.driverName, mv.driverName],
          [t.loadDate, mv.loadDate && formatDate(mv.loadDate)],
          [t.unloadDate, mv.unloadDate && formatDate(mv.unloadDate)],
        ]}
      />

      {mv.notes ? (
        <>
          <SectionHead>{t.notes}</SectionHead>
          <Group>
            <Note>{mv.notes}</Note>
          </Group>
        </>
      ) : null}

      {mv.attachments.length > 0 ? <Attachments mv={mv} /> : null}

      {anexa3 || aviz ? <Documents mv={mv} anexa3={anexa3} aviz={aviz} /> : null}

      {canEditOnPhone(mv, writer) ? (
        <View style={styles.edit}>
          <PrimaryButton
            tone="quiet"
            label={m.movementEdit}
            onPress={() => router.push({ pathname: "/predare", params: { edit: mv.id } })}
            testID="movement-edit"
          />
        </View>
      ) : null}
    </>
  );
}

type Row = [label: string, value: string | null | undefined];

/**
 * Rânduri „etichetă — valoare”. O rubrică fără valoare nu apare deloc: goală ar arăta ca un răspuns
 * („—”), iar pe telefon locul e scump. Ce lipsește de pe fișă spune webul, cu regulile lui.
 */
function Lines({ rows, afterHead }: { rows: Row[]; afterHead?: boolean }) {
  return rows
    .filter(([, value]) => !!value)
    .map(([label, value], i) => (
      <View key={label} style={[styles.line, (afterHead || i > 0) && rowStyles.sep]}>
        <Text style={styles.label}>{label}</Text>
        <Text style={styles.value}>{value}</Text>
      </View>
    ));
}

/** O grupă întreagă de rânduri, cu titlul ei; fără niciun rând completat nu apare nici titlul. */
function LinesGroup({ head, rows }: { head: string; rows: Row[] }) {
  if (!rows.some(([, value]) => !!value)) return null;
  return (
    <>
      <SectionHead>{head}</SectionHead>
      <Group>
        <Lines rows={rows} />
      </Group>
    </>
  );
}

/** Dă un fișier din cache foii de partajare; întoarce mesajul de arătat, sau null când a mers. */
async function share(get: () => Promise<string>, mimeType: string, offline: string): Promise<string | null> {
  if (!(await Sharing.isAvailableAsync())) return m.shareUnavailable;
  try {
    const uri = await get();
    await Sharing.shareAsync(uri, { mimeType, UTI: mimeType === "application/pdf" ? "com.adobe.pdf" : undefined });
    return null;
  } catch (err) {
    if (err instanceof UnauthorizedError) throw err;
    // Mesajul serverului spune ce e de completat (ex. Anexa 3 fără destinatar); altfel e semnalul.
    return err instanceof ApiError ? (err.serverMessage ?? offline) : offline;
  }
}

function useShare() {
  const { signOut } = useSession();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  async function run(key: string, get: () => Promise<string>, mimeType: string, offline: string) {
    setBusy(key);
    setError(null);
    try {
      setError(await share(get, mimeType, offline));
    } catch {
      signOut();
    } finally {
      setBusy(null);
    }
  }
  return { busy, error, run };
}

function Documents({ mv, anexa3, aviz }: { mv: WasteMovement; anexa3: boolean; aviz: boolean }) {
  const { auth } = useSession();
  const { busy, error, run } = useShare();
  const pdf = (document: "anexa3" | "aviz") =>
    run(
      document,
      () => downloadMovementPdf(auth!, mv.id, document, movementPdfName(document, mv)),
      "application/pdf",
      m.movementDocumentOffline,
    );

  return (
    <>
      <SectionHead>{m.movementDocuments}</SectionHead>
      <View style={styles.docs}>
        {anexa3 ? (
          <PrimaryButton
            testID="movement-anexa3"
            label={busy === "anexa3" ? t.anexa3Downloading : t.anexa3Download}
            disabled={busy != null}
            onPress={() => pdf("anexa3")}
          />
        ) : null}
        {aviz ? (
          <PrimaryButton
            testID="movement-aviz"
            tone="quiet"
            label={busy === "aviz" ? t.avizDownloading : t.avizDownload}
            disabled={busy != null}
            onPress={() => pdf("aviz")}
          />
        ) : null}
        <Text style={error ? styles.error : styles.hint}>{error ?? m.movementDocumentsHint}</Text>
      </View>
    </>
  );
}

function Attachments({ mv }: { mv: WasteMovement }) {
  const { auth } = useSession();
  const { busy, error, run } = useShare();

  return (
    <>
      <SectionHead>{m.movementAttachments}</SectionHead>
      <Group>
        {mv.attachments.map((a, i) => (
          <Pressable
            key={a.id}
            testID="movement-attachment"
            disabled={busy != null}
            onPress={() =>
              run(
                a.id,
                // Id-ul în față: două poze pot avea același nume, iar cache-ul e unul singur.
                () => downloadAttachment(auth!, mv.id, a.id, `${a.id}-${a.fileName.replace(/[\\/]/g, "_")}`),
                a.contentType,
                m.movementAttachmentOffline,
              )
            }
            style={({ pressed }) => [rowStyles.row, styles.head, i > 0 && rowStyles.sep, pressed && rowStyles.pressed]}
          >
            <Text style={[rowStyles.title, styles.body]} numberOfLines={1}>
              {busy === a.id ? t.anexa3Downloading : a.fileName}
            </Text>
            <Icon name="right" size={18} color={colors.ink3} />
          </Pressable>
        ))}
        {error ? <Note tone="alert">{error}</Note> : null}
      </Group>
    </>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { padding: 16, gap: 8, paddingBottom: 40 },
  head: { flexDirection: "row", alignItems: "center", gap: 12 },
  body: { flex: 1 },
  edit: { marginTop: 8 },
  line: { paddingHorizontal: 16, paddingVertical: 10, gap: 2 },
  label: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink2 },
  value: { fontFamily: fonts.sansMedium, fontSize: 15.5, color: colors.ink },
  docs: { gap: 10 },
  hint: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink2, paddingHorizontal: 4 },
  error: { fontFamily: fonts.sans, fontSize: 13, color: colors.redText, paddingHorizontal: 4 },
});
