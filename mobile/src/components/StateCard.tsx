import { strings } from "@web/strings";
import { Pressable, StyleSheet, Text, View } from "react-native";

import type { Check, CheckTone } from "../control";
import { haptic } from "../haptics";
import { colors, fonts, radius } from "../theme";
import { Icon } from "./Icon";
import { Skeleton } from "./Skeleton";

const m = strings.mobile;

export type Verdict = { tone: CheckTone | "loading"; word: string; why: string };

/**
 * Starea firmei, ca pe macheta din 27.09.2026 seara: un punct colorat, cuvântul la 20 px și motivul —
 * nu un afiș pe fond roz cu litere roșii. Dedesubt, opțional, cele patru verificări din Control ca
 * puncte mici (Termene · Cod R/D · Cântar · Autorizații). Pe Acasă duce la Control; pe Control stă
 * deasupra rândurilor și n-are punctele (sunt rândurile).
 *
 * <p>Cuvântul e aceeași socoteală ca Panoul web (`src/control.ts`): cel mai grav rând dă tonul; un
 * rând nevenit ține „Nu știu încă”, nu „În regulă”.
 */
export function StateCard({ verdict, checks, onPress, testID = "poster", wordTestID = "verdict" }: {
  verdict: Verdict;
  /** Cele patru rânduri ale Controlului, în ordinea `LED_KEYS`; `null` = n-a venit încă. Fără ele, fără rândul de puncte. */
  checks?: (Check | null)[];
  onPress?: () => void;
  testID?: string;
  wordTestID?: string;
}) {
  const dot = DOT[verdict.tone];
  return (
    <Pressable
      testID={testID}
      onPress={
        onPress
          ? () => {
              haptic.tap();
              onPress();
            }
          : undefined
      }
      disabled={!onPress}
      accessibilityRole={onPress ? "button" : undefined}
      style={({ pressed }) => [styles.card, pressed && { backgroundColor: colors.pressed }]}
    >
      <View style={styles.top}>
        <View style={[styles.dot, { backgroundColor: dot.fill, shadowColor: dot.fill }, verdict.tone === "bad" && styles.dotBad]} />
        <View style={styles.text}>
          {verdict.tone === "loading" ? (
            <Skeleton width={150} height={22} radius={6} />
          ) : (
            <Text style={styles.word} testID={wordTestID}>{verdict.word}</Text>
          )}
          <Text style={styles.why} numberOfLines={3}>{verdict.why}</Text>
        </View>
        {onPress ? <Icon name="right" size={18} color={colors.ink3} /> : null}
      </View>
      {checks ? (
        <View style={styles.checks}>
          {LED_KEYS.map((key, i) => {
            const c = checks[i];
            return (
              <View key={key} style={styles.check} testID={`led-${key}`}>
                <View style={[styles.checkDot, { backgroundColor: c ? DOT[c.tone].fill : colors.unknown }]} />
                <Text style={styles.checkText} numberOfLines={1}>{LED_LABEL[key]}</Text>
              </View>
            );
          })}
        </View>
      ) : null}
    </Pressable>
  );
}

/** Cuvântul de pe card, din rândurile Controlului. Aceeași funcție pentru Acasă și pentru Control. */
export function verdictOf(checks: (Check | null)[]): Verdict {
  const loaded = checks.filter((c): c is Check => c !== null);
  if (loaded.length < checks.length) return { tone: "loading", word: m.verdictUnknown, why: " " };
  const worst = (["bad", "warn", "unknown"] as CheckTone[]).map((t) => loaded.find((c) => c.tone === t)).find(Boolean);
  if (!worst) return { tone: "ok", word: m.verdictOk, why: m.verdictOkWhy };
  if (worst.tone === "unknown") return { tone: "unknown", word: m.verdictUnknown, why: m.verdictUnknownWhy };
  return { tone: worst.tone, word: worst.tone === "bad" ? m.verdictBad : m.verdictWarn, why: `${worst.title}: ${worst.detail}` };
}

export const LED_KEYS: Check["key"][] = ["deadlines", "missingCode", "weighing", "partners"];
const LED_LABEL: Record<Check["key"], string> = {
  deadlines: m.ledDeadlines,
  missingCode: m.ledCode,
  weighing: m.ledWeighing,
  partners: m.ledPartners,
};

const DOT: Record<CheckTone | "loading", { fill: string }> = {
  ok: { fill: colors.green },
  warn: { fill: colors.amberText },
  bad: { fill: colors.red },
  unknown: { fill: colors.unknown },
  loading: { fill: colors.unknown },
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.card,
    borderRadius: radius.group,
    borderWidth: 1,
    borderColor: colors.separator,
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 14,
    gap: 14,
  },
  top: { flexDirection: "row", alignItems: "center", gap: 12 },
  dot: { width: 12, height: 12, borderRadius: 6 },
  dotBad: { shadowOpacity: 0.35, shadowRadius: 6, shadowOffset: { width: 0, height: 0 } },
  text: { flex: 1, gap: 2 },
  word: { fontFamily: fonts.sansSemiBold, fontSize: 20, lineHeight: 24, letterSpacing: -0.2, color: colors.ink },
  why: { fontFamily: fonts.sans, fontSize: 14, color: colors.ink2 },
  checks: {
    flexDirection: "row",
    gap: 6,
    paddingTop: 12,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.separator,
  },
  check: { flex: 1, flexDirection: "row", alignItems: "center", gap: 6 },
  checkDot: { width: 7, height: 7, borderRadius: 4 },
  checkText: { flexShrink: 1, fontFamily: fonts.sans, fontSize: 11.5, color: colors.ink2 },
});
