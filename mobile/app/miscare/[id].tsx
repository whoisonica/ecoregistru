import { useQuery, useQueryClient } from "@tanstack/react-query";
import { declarationOf } from "@/lib/deadlines";
import { canPrintAnexa3, canPrintAviz, movementPdfName } from "@/lib/movementPrint";
import { strings } from "@web/strings";
import type { Unit, WasteMovement } from "@web/types";
import { Stack, useLocalSearchParams, useRouter } from "expo-router";
import * as Crypto from "expo-crypto";
import * as Sharing from "expo-sharing";
import { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import {
  ApiError,
  deadlines,
  downloadAttachment,
  downloadMovementPdf,
  movement,
  recordWeight,
  UnauthorizedError,
  uploadAttachment,
} from "../../src/api";
import { canWrite } from "../../src/auth";
import { Bin } from "../../src/components/Bin";
import { Field, Input, Pills, PrimaryButton } from "../../src/components/Form";
import { confirmDeclared } from "../../src/declared";
import { parseQuantity } from "../../src/handoverForm";
import { Icon } from "../../src/components/Icon";
import { Chip, Group, Note, rowStyles, SectionHead } from "../../src/components/Rows";
import { formatDate, formatQuantity } from "../../src/format";
import { canAddPhotoOnPhone, canEditOnPhone, canRecordWeightOnPhone, canRepeatOnPhone } from "../../src/movementEdit";
import { pickAvizPhoto } from "../../src/photo";
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
 *
 * <p>F7 (valul B): „Repetă predarea” deschide formularul cu ea, ca predare nouă de azi
 * (`/predare?repeat=<id>`), iar „Pozează” / „Din galerie” adaugă bonul de cântar sau avizul după salvare.
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
      {/* „Adaugă cantitatea” are câmp de text: fără `handled`, prima atingere pe „tone” doar închidea tastatura;
          tastatura numerică de pe iOS n-are „Gata”, deci se închide la derulare, ca în formularul de predare. */}
      <ScrollView
        style={styles.fill}
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
      >
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
              {formatQuantity(mv.quantity, mv.unit)} {e.unit[mv.unit]}
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

      {/* Lângă cifra care lipsește: aici o caută omul care a primit tichetul de la cântar. */}
      {canRecordWeightOnPhone(mv, writer) ? <RecordWeight mv={mv} /> : null}

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

      {mv.attachments.length > 0 || canAddPhotoOnPhone(mv, writer) ? (
        <Attachments mv={mv} canAdd={canAddPhotoOnPhone(mv, writer)} />
      ) : null}

      {anexa3 || aviz ? <Documents mv={mv} anexa3={anexa3} aviz={aviz} /> : null}

      {canRepeatOnPhone(mv, writer) ? (
        <View style={styles.edit}>
          <PrimaryButton
            label={m.movementRepeat}
            onPress={() => router.push({ pathname: "/predare", params: { repeat: mv.id } })}
            testID="movement-repeat"
          />
        </View>
      ) : null}
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

/**
 * „Adaugă cantitatea”: cifra de pe tichetul colectorului, după cântărirea la descărcare. Până acum se
 * putea pune numai pe web. Aceleași reguli ca acolo (`RecordWeightDialog`): numai cu semnal, întrebarea
 * anului declarat, propoziția serverului la refuz.
 */
function RecordWeight({ mv }: { mv: WasteMovement }) {
  const { auth } = useSession();
  const queryClient = useQueryClient();
  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState<Unit>(mv.unit);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const amount = parseQuantity(quantity);

  const save = async () => {
    if (!auth || busy) return;
    if (amount == null) return setError(m.quantityFormat);
    setBusy(true);
    setError(null);
    try {
      const year = Number(mv.date.slice(0, 4));
      const declaration = await deadlines(auth, year + 1)
        .then((list) => declarationOf(list, year))
        .catch(() => undefined);
      if (declaration && !(await confirmDeclared(year, declaration))) return;
      await recordWeight(auth, mv.id, amount, unit);
      await queryClient.invalidateQueries({ queryKey: ["movements"] });
    } catch (e) {
      setError(
        e instanceof ApiError && e.serverMessage
          ? e.serverMessage
          : e instanceof TypeError ? m.movementEditOffline : t.recordWeightError,
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <SectionHead>{t.recordWeightTitle}</SectionHead>
      <Group>
        <Field label={t.quantity} testID="weight-field">
          <View style={styles.weightRow}>
            <Input
              style={styles.weightInput}
              value={quantity}
              onChangeText={setQuantity}
              keyboardType="decimal-pad"
              testID="weight-qty"
            />
            <Pills
              options={(["KG", "TONS"] as Unit[]).map((u) => ({ value: u, label: e.unit[u] }))}
              value={unit}
              onChange={setUnit}
            />
          </View>
        </Field>
        {error ? (
          <Note tone="alert" testID="weight-error">
            {error}
          </Note>
        ) : null}
      </Group>
      <PrimaryButton label={t.recordWeight} onPress={save} disabled={busy || !quantity.trim()} testID="weight-save" />
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

function Attachments({ mv, canAdd }: { mv: WasteMovement; canAdd: boolean }) {
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
        {canAdd ? <AddPhoto mv={mv} first={mv.attachments.length === 0} /> : null}
      </Group>
    </>
  );
}

/**
 * „Pozează” / „Din galerie” (F7): o poză nouă pe predare, cu semnal. Poza care n-a urcat rămâne aici cu
 * aceeași cheie (`clientUploadId`, V67) — „Încearcă din nou” n-o poate dubla nici dacă prima încercare a
 * ajuns, dar răspunsul s-a pierdut pe drum.
 */
function AddPhoto({ mv, first }: { mv: WasteMovement; first: boolean }) {
  const { auth } = useSession();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<{ uri: string; key: string } | null>(null);
  const [state, setState] = useState<"idle" | "uploading" | "failed" | "denied">("idle");

  const send = async (item: { uri: string; key: string }) => {
    if (!auth) return;
    setState("uploading");
    try {
      await uploadAttachment(auth, mv.id, item.uri, item.key);
      setPending(null);
      setState("idle");
      await queryClient.invalidateQueries({ queryKey: ["movements"] });
    } catch {
      setState("failed");
    }
  };

  const pick = async (source: "camera" | "gallery") => {
    const uri = await pickAvizPhoto(source);
    if (uri === "denied") return setState("denied");
    if (!uri) return;
    const item = { uri, key: Crypto.randomUUID() };
    setPending(item);
    await send(item);
  };

  const busy = state === "uploading";
  return (
    <View style={[rowStyles.row, !first && rowStyles.sep, styles.photoBox]}>
      <Text style={styles.label}>{m.photoAddHint}</Text>
      {pending && state === "failed" ? (
        <>
          <Note tone="alert" testID="photo-failed">{m.photoFailed}</Note>
          <PrimaryButton label={m.photoRetry} onPress={() => send(pending)} testID="photo-retry" />
        </>
      ) : (
        <View style={styles.photoActions}>
          <View style={styles.body}>
            <PrimaryButton tone="quiet" label={busy ? m.photoUploading : m.photoCamera} onPress={() => pick("camera")} disabled={busy} testID="photo-camera" />
          </View>
          <View style={styles.body}>
            <PrimaryButton tone="quiet" label={m.photoGallery} onPress={() => pick("gallery")} disabled={busy} testID="photo-gallery" />
          </View>
        </View>
      )}
      {state === "denied" ? <Note tone="alert">{m.photoCameraDenied}</Note> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  weightRow: { flexDirection: "row", alignItems: "center", gap: 8 },
  weightInput: { flex: 1 },
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { padding: 16, gap: 8, paddingBottom: 40 },
  head: { flexDirection: "row", alignItems: "center", gap: 12 },
  body: { flex: 1 },
  edit: { marginTop: 8 },
  photoBox: { flexDirection: "column", alignItems: "stretch", gap: 8 },
  photoActions: { flexDirection: "row", gap: 8 },
  line: { paddingHorizontal: 16, paddingVertical: 10, gap: 2 },
  label: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink2 },
  value: { fontFamily: fonts.sansMedium, fontSize: 15.5, color: colors.ink },
  docs: { gap: 10 },
  hint: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink2, paddingHorizontal: 4 },
  error: { fontFamily: fonts.sans, fontSize: 13, color: colors.redText, paddingHorizontal: 4 },
});
