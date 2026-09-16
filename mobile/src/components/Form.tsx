import { strings } from "@web/strings";
import type { ReactNode } from "react";
import { Pressable, StyleSheet, Text, TextInput, View, type TextInputProps } from "react-native";

import { colors, fonts, radius } from "../theme";

/**
 * O rubrică a formularului: eticheta, câmpul și — când valoarea vine din poză — rândul din care a
 * fost citită, cu butonul „Corect”. Cât timp nu e confirmată, rubrica are contur galben și
 * „Salvează” nu pleacă (todo-mobil §5: nimic nu se salvează fără bifă).
 */
export function Field({
  label,
  read,
  onConfirm,
  error,
  children,
  testID,
}: {
  label: string;
  /** Rândul de pe aviz, dacă valoarea din câmp e citită din poză și încă neconfirmată. */
  read?: string;
  onConfirm?: () => void;
  error?: string;
  children: ReactNode;
  testID?: string;
}) {
  return (
    <View style={[styles.field, read != null && styles.fieldRead]} testID={testID}>
      <Text style={styles.label}>{label}</Text>
      {children}
      {read != null ? (
        <View style={styles.readRow}>
          <Text style={styles.readText} numberOfLines={2}>
            {strings.mobile.readFrom(read)}
          </Text>
          <Pressable
            onPress={onConfirm}
            style={({ pressed }) => [styles.confirm, pressed && { opacity: 0.7 }]}
            accessibilityRole="button"
            testID={testID ? `${testID}-confirm` : undefined}
          >
            <Text style={styles.confirmText}>{strings.mobile.confirmRead}</Text>
          </Pressable>
        </View>
      ) : null}
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}

export function Input(props: TextInputProps) {
  return <TextInput placeholderTextColor={colors.ink3} {...props} style={[styles.input, props.style]} />;
}

/** Sub șapte opțiuni: butoane, nu listă derulantă (aceeași regulă ca `PillGroup` pe web). */
export function Pills<T extends string>({
  options,
  value,
  onChange,
  testID,
}: {
  options: { value: T; label: string }[];
  value: T | "" | null;
  onChange: (v: T) => void;
  testID?: string;
}) {
  return (
    <View style={styles.pills}>
      {options.map((o) => {
        const on = o.value === value;
        return (
          <Pressable
            key={o.value}
            testID={testID ? `${testID}-${o.value}` : undefined}
            onPress={() => onChange(o.value)}
            style={[styles.pill, on && styles.pillOn]}
            accessibilityRole="radio"
            accessibilityState={{ selected: on }}
          >
            <Text style={[styles.pillText, on && styles.pillTextOn]}>{o.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export function PrimaryButton({
  label,
  onPress,
  disabled,
  testID,
  tone = "green",
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  testID?: string;
  tone?: "green" | "quiet";
}) {
  return (
    <Pressable
      testID={testID}
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      style={({ pressed }) => [
        styles.button,
        tone === "quiet" && styles.buttonQuiet,
        disabled && styles.buttonDisabled,
        pressed && { opacity: 0.85 },
      ]}
    >
      <Text style={[styles.buttonText, tone === "quiet" && styles.buttonTextQuiet]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  field: { paddingHorizontal: 16, paddingVertical: 12, gap: 8 },
  fieldRead: { backgroundColor: colors.amberSoft },
  label: { fontFamily: fonts.sansMedium, fontSize: 13, color: colors.ink2 },
  readRow: { flexDirection: "row", alignItems: "center", gap: 10 },
  readText: { flex: 1, fontFamily: fonts.mono, fontSize: 11.5, color: colors.amberText },
  confirm: {
    backgroundColor: colors.card,
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderWidth: 1,
    borderColor: colors.amberText,
  },
  confirmText: { fontFamily: fonts.sansSemiBold, fontSize: 14, color: colors.amberText },
  error: { fontFamily: fonts.sans, fontSize: 13, color: colors.redText },
  input: {
    fontFamily: fonts.sans,
    fontSize: 17,
    color: colors.ink,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.separator,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  pills: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  pill: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.separator,
    backgroundColor: colors.card,
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  pillOn: { borderColor: colors.green, backgroundColor: colors.greenSoft },
  pillText: { fontFamily: fonts.sansMedium, fontSize: 15, color: colors.ink },
  pillTextOn: { color: colors.greenText },
  button: {
    backgroundColor: colors.green,
    borderRadius: radius.button,
    minHeight: 54,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 18,
  },
  buttonQuiet: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.separator },
  buttonDisabled: { opacity: 0.45 },
  buttonText: { fontFamily: fonts.sansSemiBold, fontSize: 17, color: "#fff" },
  buttonTextQuiet: { color: colors.ink },
});
