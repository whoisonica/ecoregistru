import { useQueryClient } from "@tanstack/react-query";
import { strings } from "@web/strings";
import type { ReportType } from "@web/types";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";

import { ApiError, completeDeadline } from "../src/api";
import { Input, PrimaryButton } from "../src/components/Form";
import { Note } from "../src/components/Rows";
import { formatDate } from "../src/format";
import { haptic } from "../src/haptics";
import { useSession } from "../src/session";
import { colors, fonts } from "../src/theme";

const t = strings.deadlines;
const m = strings.mobile;

/**
 * F5 (valul B, D9) — foaia de jos a lui „Bifează” de pe Termene: termenul, nota (numărul de înregistrare,
 * ca pe web) și „Marchează finalizat”. Aceeași cerere ca `useCompleteDeadline`; se bifează numai cu
 * semnal, iar nota scrisă rămâne în foaie dacă n-a plecat. Cine vede butonul hotărăște `canCompleteOnPhone`.
 */
export default function BifeazaSheet() {
  const { id, type, due } = useLocalSearchParams<{ id: string; type: ReportType; due: string }>();
  const { auth } = useSession();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const save = async () => {
    if (!auth || busy) return;
    setBusy(true);
    setError(null);
    try {
      await completeDeadline(auth, id, note.trim() || undefined);
      haptic.success();
      await queryClient.invalidateQueries({ queryKey: ["deadlines"] });
      router.back();
    } catch (e) {
      setError(e instanceof ApiError && e.serverMessage ? e.serverMessage : e instanceof TypeError ? m.deadlineCompleteOffline : t.actionError);
    } finally {
      setBusy(false);
    }
  };

  return (
    <View style={styles.sheet} testID="complete-sheet">
      <Text style={styles.kicker}>{t.completeTitle}</Text>
      <Text style={styles.title}>{type ? strings.enums.reportType[type] : ""}</Text>
      {due ? <Text style={styles.due}>{m.deadlineDue(formatDate(due))}</Text> : null}
      <Text style={styles.label}>{t.noteLabel}</Text>
      <Input
        testID="complete-note"
        value={note}
        onChangeText={setNote}
        placeholder={t.notePlaceholder}
        maxLength={500}
        multiline
        style={styles.note}
      />
      {error ? <Note tone="alert" testID="complete-error">{error}</Note> : null}
      <View style={styles.foot}>
        <PrimaryButton label={busy ? strings.common.saving : t.markDone} onPress={save} disabled={busy} testID="complete-save" />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  sheet: { flex: 1, backgroundColor: colors.ground, padding: 20, paddingTop: 28, gap: 8 },
  kicker: { fontFamily: fonts.mono, fontSize: 12, color: colors.ink2, textTransform: "uppercase", letterSpacing: 0.5 },
  title: { fontFamily: fonts.sansSemiBold, fontSize: 20, color: colors.ink },
  due: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2 },
  label: { fontFamily: fonts.sans, fontSize: 13, color: colors.ink2, marginTop: 10 },
  note: { minHeight: 84, textAlignVertical: "top" },
  foot: { marginTop: 8 },
});
