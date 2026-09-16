import { strings } from "@web/strings";
import { LinearGradient } from "expo-linear-gradient";
import { useState } from "react";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { ApiError } from "../src/api";
import { Logo } from "../src/components/Icon";
import { useSession } from "../src/session";
import { colors, fonts, radius } from "../src/theme";

export default function LoginScreen() {
  const { signIn } = useSession();
  const insets = useSafeAreaInsets();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (busy || !email || !password) return;
    setBusy(true);
    setError(null);
    try {
      await signIn(email, password);
    } catch (e) {
      // Un răspuns de la server = date greșite sau blocat; fără răspuns = n-am ajuns la server.
      setError(e instanceof ApiError ? strings.login.genericError : strings.mobile.serverUnreachable);
      setBusy(false);
    }
  }

  return (
    <LinearGradient colors={[colors.graphiteTop, colors.graphiteBottom]} style={styles.fill}>
      <KeyboardAvoidingView style={styles.fill} behavior={Platform.OS === "ios" ? "padding" : undefined}>
        <ScrollView
          contentContainerStyle={[styles.content, { paddingTop: insets.top + 56, paddingBottom: insets.bottom + 24 }]}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.brand}>
            <LinearGradient colors={["#2F3833", "#1A201C"]} style={styles.logo}>
              <Logo size={30} />
            </LinearGradient>
            <Text style={styles.appName}>{strings.appName}</Text>
            <Text style={styles.tagline}>{strings.tagline}</Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>{strings.login.title}</Text>
            <Text style={styles.label}>{strings.login.email}</Text>
            <TextInput
              testID="login-email"
              style={styles.input}
              value={email}
              onChangeText={setEmail}
              autoCapitalize="none"
              autoComplete="email"
              keyboardType="email-address"
              textContentType="username"
              returnKeyType="next"
            />
            <Text style={styles.label}>{strings.login.password}</Text>
            <TextInput
              testID="login-password"
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              secureTextEntry
              autoComplete="password"
              textContentType="password"
              returnKeyType="go"
              onSubmitEditing={submit}
            />
            {error ? <Text style={styles.error} testID="login-error">{error}</Text> : null}
            <Pressable testID="login-submit" onPress={submit} disabled={busy} style={({ pressed }) => [pressed && styles.pressed]}>
              <LinearGradient colors={[colors.greenHi, colors.green]} style={styles.button}>
                {busy ? <ActivityIndicator color="#fff" /> : null}
                <Text style={styles.buttonText}>{busy ? strings.login.loading : strings.login.submit}</Text>
              </LinearGradient>
            </Pressable>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  content: { paddingHorizontal: 18, gap: 28 },
  brand: { alignItems: "center", gap: 6 },
  logo: {
    width: 64,
    height: 64,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "rgba(255,255,255,0.14)",
  },
  appName: { fontFamily: fonts.sansSemiBold, fontSize: 30, color: "#fff", letterSpacing: -0.5 },
  tagline: { fontFamily: fonts.sans, fontSize: 14.5, color: colors.onDark2, textAlign: "center" },
  card: { backgroundColor: colors.ground, borderRadius: radius.sheet, padding: 18, gap: 6 },
  cardTitle: { fontFamily: fonts.sansSemiBold, fontSize: 20, color: colors.ink, marginBottom: 6 },
  label: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.ink2, marginTop: 8 },
  input: {
    backgroundColor: colors.card,
    borderRadius: 12,
    minHeight: 50,
    paddingHorizontal: 14,
    fontFamily: fonts.sans,
    fontSize: 16,
    color: colors.ink,
    borderWidth: 1,
    borderColor: colors.separator,
  },
  error: { fontFamily: fonts.sans, fontSize: 14, color: colors.redText, marginTop: 10 },
  button: {
    marginTop: 18,
    minHeight: 54,
    borderRadius: radius.button,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
  },
  buttonText: { fontFamily: fonts.sansSemiBold, fontSize: 16.5, color: "#fff" },
  pressed: { transform: [{ scale: 0.97 }], opacity: 0.95 },
});
