import { strings } from "@web/strings";
import { StyleSheet, Text, View } from "react-native";

import { useOnline } from "../online";
import { useOutbox } from "../outbox";
import { useSession } from "../session";
import { colors, fonts } from "../theme";
import { Icon } from "./Icon";

/**
 * Banda „Fără semnal” de sub cap, cu câte predări stau în telefon: omul află de ce nu se încarcă
 * înainte să dea vina pe aplicație (propunerea de refresh, §3.8). Cu semnal nu e nimic aici.
 */
export function OfflineBand() {
  const online = useOnline();
  const { session } = useSession();
  const outbox = useOutbox(session?.email);
  if (online) return null;
  const pending = outbox.filter((i) => i.state === "PENDING").length;
  return (
    <View style={styles.band} testID="offline-band">
      <Icon name="nosig" size={16} color="#FFC24D" strokeWidth={2} />
      <Text style={styles.text}>{strings.mobile.offlineBand}</Text>
      {pending > 0 ? <Text style={styles.count}>{strings.mobile.offlinePending(pending).toUpperCase()}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  band: {
    marginHorizontal: 16,
    marginTop: 8,
    borderRadius: 12,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    paddingHorizontal: 16,
    paddingVertical: 9,
    backgroundColor: "#2B2F2C",
  },
  text: { flex: 1, fontFamily: fonts.sansMedium, fontSize: 13, color: "#FFC24D" },
  count: { fontFamily: fonts.monoMedium, fontSize: 12, color: "#FFC24D", letterSpacing: 0.4 },
});
