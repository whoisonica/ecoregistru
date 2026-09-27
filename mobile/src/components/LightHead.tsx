import { strings } from "@web/strings";
import { useRouter } from "expo-router";
import type { ReactNode } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { useSession } from "../session";
import { colors, fonts } from "../theme";
import { Icon, Logo } from "./Icon";

/**
 * Capul pe hârtie (proprietarul, 27.09.2026: „nu-mi place deloc ... chestia aia blocată pe ecran”):
 * nimic țintuit sus. Firma e o linie mono mică cu semnul, avatarul din colț duce la Profil, titlul e
 * mare, iar dedesubt o propoziție. Se derulează cu pagina, ca o listă nativă.
 *
 * <p>`right` înlocuiește avatarul (lupa pe „Generare”); `back` pune în locul liniei firmei o
 * săgeată cu numele ecranului de unde s-a venit (Profil).
 */
export function LightHead({ title, subtitle, right, back, children }: {
  title: string;
  subtitle?: string | null;
  right?: ReactNode;
  back?: { label: string; onPress: () => void };
  children?: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  const { session } = useSession();
  const router = useRouter();
  const role = session ? strings.enums.role[session.role] : "";
  const firm = [session?.tenantName ?? session?.consultancyName ?? strings.appName, role].filter(Boolean).join(" · ");
  const initials = (session?.email ?? "?").slice(0, 2).toUpperCase();

  return (
    <View style={[styles.head, { paddingTop: insets.top + 10 }]}>
      <View style={styles.topRow}>
        {back ? (
          <Pressable onPress={back.onPress} style={styles.back} hitSlop={8} accessibilityRole="button" testID="head-back">
            <Icon name="back" size={20} color={colors.green} strokeWidth={2.2} />
            <Text style={styles.backText}>{back.label}</Text>
          </Pressable>
        ) : (
          // Și linia firmei duce la Profil: acolo e firma. (Pe clientul de dezvoltare rotița lui acoperă avatarul.)
          <Pressable
            testID="firm-line"
            onPress={() => router.push("/profil")}
            style={({ pressed }) => [styles.firm, pressed && { opacity: 0.6 }]}
            accessibilityRole="button"
            accessibilityLabel={strings.mobile.profile}
          >
            <Logo size={24} />
            <Text style={styles.firmText} numberOfLines={1}>{firm.toUpperCase()}</Text>
          </Pressable>
        )}
        {right !== undefined ? (
          right
        ) : (
          <Pressable
            testID="avatar"
            onPress={() => router.push("/profil")}
            style={({ pressed }) => [styles.avatar, pressed && { opacity: 0.7 }]}
            accessibilityRole="button"
            accessibilityLabel={strings.mobile.profile}
            hitSlop={6}
          >
            <Text style={styles.avatarText}>{initials}</Text>
          </Pressable>
        )}
      </View>
      <View>
        <Text style={styles.title} testID="head-title">{title}</Text>
        {subtitle ? <Text style={styles.subtitle}>{subtitle}</Text> : null}
      </View>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  head: { paddingHorizontal: 16, paddingBottom: 6, gap: 14 },
  topRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10, minHeight: 34 },
  firm: { flexDirection: "row", alignItems: "center", gap: 8, flexShrink: 1 },
  firmText: { flexShrink: 1, fontFamily: fonts.mono, fontSize: 11, letterSpacing: 0.7, color: colors.ink2 },
  back: { flexDirection: "row", alignItems: "center", gap: 2 },
  backText: { fontFamily: fonts.sansMedium, fontSize: 16, color: colors.green },
  avatar: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: colors.card,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.separator,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { fontFamily: fonts.monoMedium, fontSize: 12, color: colors.ink2 },
  title: { fontFamily: fonts.sansSemiBold, fontSize: 30, lineHeight: 33, letterSpacing: -0.75, color: colors.ink },
  subtitle: { fontFamily: fonts.sans, fontSize: 15, color: colors.ink2, marginTop: 4 },
});
