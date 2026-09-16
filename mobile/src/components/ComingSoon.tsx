import { strings } from "@web/strings";
import { StyleSheet, Text, View } from "react-native";

import { useSession } from "../session";
import { colors, fonts } from "../theme";
import { GraphiteHeader } from "./GraphiteHeader";

/** Locul unui ecran din bara de jos care vine într-o felie următoare (todo-mobil §7). */
export function ComingSoon({ title }: { title: string }) {
  const { session } = useSession();
  return (
    <View style={styles.fill}>
      <GraphiteHeader title={title} meta={(session?.tenantName ?? strings.appName).toUpperCase()} />
      <View style={styles.body}>
        <Text style={styles.text}>{strings.mobile.comingSoon}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  body: { padding: 20 },
  text: { fontFamily: fonts.sans, fontSize: 15, color: colors.ink2 },
});
