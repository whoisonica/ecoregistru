import DateTimePicker, { DateTimePickerAndroid, type DateTimePickerEvent } from "@react-native-community/datetimepicker";
import { strings } from "@web/strings";
import { useState } from "react";
import { Platform, Pressable, StyleSheet, Text, View } from "react-native";

import { haptic } from "../haptics";
import { colors, fonts } from "../theme";
import { Icon } from "./Icon";

const m = strings.mobile;

/**
 * Data cu selectorul telefonului (F3, 27.09.2026), nu „zz.ll.aaaa” tastat: jumătate din greșelile de
 * tastare dispar. Pe iOS calendarul se deschide sub rând; pe Android e dialogul sistemului.
 *
 * <p>Valoarea e `yyyy-MM-dd`, cum o vrea serverul; `null` = nescrisă (o dată citită din poză care nu
 * s-a putut citi rămâne așa până alege omul). Azi se spune „Azi, 27 septembrie 2026”.
 */
export function DateField({ value, onChange, testID, min, max }: {
  value: string | null;
  onChange: (iso: string) => void;
  testID?: string;
  min?: Date;
  max?: Date;
}) {
  const [open, setOpen] = useState(false);
  const picked = value ? fromIso(value) : new Date();
  const change = (_: DateTimePickerEvent, d?: Date) => {
    if (Platform.OS === "android") setOpen(false);
    if (!d) return;
    haptic.tap();
    onChange(toIso(d));
  };
  const press = () => {
    if (Platform.OS === "android") {
      DateTimePickerAndroid.open({ value: picked, mode: "date", onChange: change, minimumDate: min, maximumDate: max });
      return;
    }
    setOpen((o) => !o);
  };
  return (
    <View>
      <Pressable
        testID={testID}
        onPress={press}
        accessibilityRole="button"
        accessibilityLabel={m.pickDate}
        style={({ pressed }) => [styles.row, pressed && { opacity: 0.7 }]}
      >
        <Icon name="cal" size={20} color={colors.ink2} />
        <Text style={[styles.text, !value && { color: colors.ink3 }]} testID={testID ? `${testID}-text` : undefined}>
          {value ? dateLabel(value) : m.pickDate}
        </Text>
        <Icon name={open ? "left" : "right"} size={18} color={colors.ink3} />
      </Pressable>
      {open && Platform.OS === "ios" ? (
        <View style={styles.picker}>
          <DateTimePicker
            value={picked}
            mode="date"
            display="inline"
            locale="ro-RO"
            onChange={change}
            minimumDate={min}
            maximumDate={max}
            accentColor={colors.green}
            themeVariant="light"
          />
          <Pressable onPress={() => setOpen(false)} style={styles.done} accessibilityRole="button" testID={testID ? `${testID}-done` : undefined}>
            <Text style={styles.doneText}>{m.dateDone}</Text>
          </Pressable>
        </View>
      ) : null}
    </View>
  );
}

/** „Azi, 27 septembrie 2026” sau „14 septembrie 2026”. */
export function dateLabel(iso: string) {
  const d = fromIso(iso);
  const text = `${d.getDate()} ${strings.months[d.getMonth()].toLowerCase()} ${d.getFullYear()}`;
  return iso === toIso(new Date()) ? m.dateToday(text) : text;
}

function fromIso(iso: string) {
  const [y, mo, d] = iso.split("-").map(Number);
  return new Date(y, mo - 1, d);
}

export function toIso(d: Date) {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.separator,
    borderRadius: 12,
    paddingHorizontal: 12,
    minHeight: 48,
  },
  text: { flex: 1, fontFamily: fonts.sansMedium, fontSize: 17, color: colors.ink },
  picker: { marginTop: 8, backgroundColor: colors.card, borderRadius: 12, borderWidth: 1, borderColor: colors.separator, paddingBottom: 4 },
  done: { alignSelf: "flex-end", paddingHorizontal: 16, paddingVertical: 8 },
  doneText: { fontFamily: fonts.sansSemiBold, fontSize: 15, color: colors.greenText },
});
