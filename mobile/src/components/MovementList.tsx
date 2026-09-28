import { useInfiniteQuery, useQueries, useQuery } from "@tanstack/react-query";
import { binFor } from "@/lib/binColor";
import { directionOf, registerOf, type MovementScreen } from "@/lib/movementScreens";
import type { WasteMovement } from "@/lib/types";
import { strings } from "@web/strings";
import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { router } from "expo-router";
import { Pressable, RefreshControl, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";

import { movements, movementTotals, UnauthorizedError, type MovementFilters } from "../api";
import { formatKg, formatQuantity } from "../format";
import { haptic } from "../haptics";
import { byDay, composition, gapMonths, missingCode, monthsBack } from "../register";
import { useSession } from "../session";
import { binColors, colors, fonts } from "../theme";
import { BigNumber } from "./BigNumber";
import { Bin } from "./Bin";
import { PrimaryButton } from "./Form";
import { Icon } from "./Icon";
import { LightHead } from "./LightHead";
import { OfflineBand } from "./OfflineBand";
import { Chip, Note, rowStyles } from "./Rows";
import { SkeletonRows } from "./Skeleton";

const m = strings.mobile;

type Filter = "all" | "noCode" | "incomplete";

/**
 * Mișcările unei luni, pe o direcție — tabul „Generare” al generatorului și „Intrări”/„Ieșiri” ale
 * colectorului. Din 27.09.2026 (F6, valul B, macheta v6) e un **registru**: lunile de răsfoit cu kg-ul
 * fiecăreia (cea goală cu contur galben), cifra lunii cu compoziția pe coduri ca bară, cipurile de
 * filtru, căutarea și lista pe zile, cu totalul zilei și al lunii.
 *
 * <p>Cifrele vin de la `/movements/totals`, nu din rândurile aduse: pagina nu e luna, iar un total adunat
 * din 50 de rânduri ar fi mai mic decât adevărul fără să spună că e mai mic. Tot de aceea cipurile și
 * căutarea se aplică pe server. Singura socoteală din rânduri e bara compoziției, și numai când toată
 * luna e adusă și nefiltrată — altfel bara nu se desenează.
 */
export function MovementList({ title, screen, tabRow }: {
  title: string;
  /** Ecranul al cărui filtru se aplică. Lipsește cât timp tipul firmei nu se știe. */
  screen?: MovementScreen;
  /** Rândul de taburi al ecranului (`TabRow`), deasupra listei. */
  tabRow?: ReactNode;
}) {
  const { session, auth, signOut } = useSession();
  const months = useMemo(() => monthsBack(new Date()), []);
  const [picked, setPicked] = useState(0);
  const cursor = months[picked];
  const [filter, setFilter] = useState<Filter>("all");
  const [searchOpen, setSearchOpen] = useState(false);
  const [typed, setTyped] = useState("");
  const [search, setSearch] = useState("");
  useEffect(() => {
    const t = setTimeout(() => setSearch(typed.trim()), 350);
    return () => clearTimeout(t);
  }, [typed]);

  // Registrul și direcția vin din același loc ca pe web, ca ecranul să numere ce arată.
  const base = {
    register: screen ? registerOf(screen) : undefined,
    direction: screen ? directionOf(screen) : undefined,
  };
  const enabled = !!auth && !!session?.tenantId && !!screen;
  const totalsKey = (c: { year: number; month: number }) => ["movements", "totals", session?.tenantId, c.year, c.month, screen ?? "?"];

  // Kg-ul fiecărei luni din șir. Aceeași cheie ca cifra mare, deci luna aleasă nu se cere de două ori.
  const scrub = useQueries({
    queries: months.map((c) => ({
      queryKey: totalsKey(c),
      queryFn: () => movementTotals(auth!, { ...c, ...base }),
      enabled,
    })),
  });
  const totals = scrub[picked];
  const gaps = gapMonths(scrub.map((q) => q.data?.rows));

  const params: MovementFilters = {
    ...cursor,
    ...base,
    missingOperationCode: filter === "noCode" ? true : undefined,
    incomplete: filter === "incomplete" ? true : undefined,
    search: search || undefined,
  };
  const narrowed = filter !== "all" || !!search;

  // Câte 50; înainte lista se oprea la primele 50 fără să spună că mai sunt.
  const list = useInfiniteQuery({
    queryKey: ["movements", "list", session?.tenantId, cursor.year, cursor.month, screen ?? "?", filter, search],
    queryFn: ({ pageParam }) => movements(auth!, params, pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    enabled,
  });
  const rows = list.data?.pages.flatMap((p) => p.content);
  const totalRows = list.data?.pages[0]?.totalElements ?? 0;

  useEffect(() => {
    if (list.error instanceof UnauthorizedError || totals.error instanceof UnauthorizedError) signOut();
  }, [list.error, totals.error, signOut]);

  const [refreshing, setRefreshing] = useState(false);
  const refresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([list.refetch(), totals.refetch()]).catch(() => {});
    setRefreshing(false);
  }, [list.refetch, totals.refetch]); // eslint-disable-line react-hooks/exhaustive-deps

  const now = new Date();
  const monthName = strings.months[cursor.month - 1];
  const label = cursor.year === now.getFullYear() ? monthName : `${monthName} ${cursor.year}`;

  // Rândul de sub cifră spune ce mai lipsește din lună, nu doar câte rânduri are.
  let foot: string | undefined;
  let footTone: "ok" | "alert" = "ok";
  if (!session?.tenantId) {
    foot = m.noCompanyYet;
    footTone = "alert";
  } else if (totals.isError) {
    foot = m.lcdError;
    footTone = "alert";
  } else if (totals.data) {
    const gapsText: string[] = [];
    if (totals.data.missingOperationCode > 0) gapsText.push(`${totals.data.missingOperationCode} ${m.missingOperationCode}`);
    if (totals.data.awaitingWeighing > 0) gapsText.push(`${totals.data.awaitingWeighing} ${m.awaitingWeighing}`);
    foot = gapsText.length ? gapsText.join(" · ") : m.lcdMovements(totals.data.rows);
    footTone = gapsText.length ? "alert" : "ok";
  }

  // Bara compoziției: numai peste toată luna, nefiltrată — din rânduri, deci numai când toate sunt aduse.
  const shares = !narrowed && rows && !list.hasNextPage ? composition(rows) : [];

  const chips: { id: Filter; label: string; count?: number }[] = [{ id: "all", label: m.filterAll, count: totals.data?.rows }];
  if ((totals.data?.missingOperationCode ?? 0) > 0 || filter === "noCode")
    chips.push({ id: "noCode", label: m.filterNoCode, count: totals.data?.missingOperationCode });
  if ((totals.data?.incomplete ?? 0) > 0 || filter === "incomplete")
    chips.push({ id: "incomplete", label: m.filterIncomplete, count: totals.data?.incomplete });

  const pickMonth = (i: number) => {
    haptic.tap();
    setPicked(i);
    setFilter("all");
  };

  const searchButton = (
    <Pressable
      testID="register-search"
      onPress={() => {
        haptic.tap();
        if (searchOpen) setTyped("");
        setSearchOpen(!searchOpen);
      }}
      style={({ pressed }) => [styles.iconBtn, searchOpen && styles.iconBtnOn, pressed && { opacity: 0.7 }]}
      accessibilityRole="button"
      accessibilityLabel={m.searchOpen}
    >
      <Icon name={searchOpen ? "close" : "search"} size={18} color={colors.ink} strokeWidth={2} />
    </Pressable>
  );

  return (
    <ScrollView
      style={styles.fill}
      contentContainerStyle={styles.scroll}
      keyboardShouldPersistTaps="handled"
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.ink2} />}
    >
      <LightHead title={title} subtitle={subtitleFor(base.direction)} right={searchButton} />
      <OfflineBand />
      <View style={styles.body}>
        {tabRow}
        {searchOpen ? (
          <TextInput
            testID="register-search-input"
            value={typed}
            onChangeText={setTyped}
            placeholder={m.searchPlaceholder}
            placeholderTextColor={colors.ink3}
            autoFocus
            autoCorrect={false}
            returnKeyType="search"
            clearButtonMode="while-editing"
            style={styles.search}
          />
        ) : null}

        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.scrubWrap} contentContainerStyle={styles.scrub}>
          {months.map((c, i) => {
            const q = scrub[i];
            const on = i === picked;
            const gap = gaps.has(i);
            // „?” când n-a putut încărca, niciodată un zero inventat; „·” cât se încarcă.
            const kg = q.isError ? "?" : q.data ? (q.data.rows === 0 ? "—" : formatKg(q.data.quantityKg)) : "·";
            const name = strings.months[c.month - 1];
            return (
              <Pressable
                key={`${c.year}-${c.month}`}
                testID={`month-${i}`}
                onPress={() => pickMonth(i)}
                style={[styles.mon, gap && styles.monGap, on && styles.monOn]}
                accessibilityRole="button"
                accessibilityState={{ selected: on }}
                accessibilityLabel={`${name} ${c.year} · ${q.data ? `${formatKg(q.data.quantityKg)} kg` : ""}${gap ? ` · ${m.monthEmptyHint}` : ""}`}
              >
                <Text style={[styles.monName, on && styles.monNameOn]}>
                  {name.slice(0, 3)}
                  {c.year !== now.getFullYear() ? ` ${String(c.year).slice(2)}` : ""}
                </Text>
                <Text style={[styles.monKg, gap && styles.monKgGap, on && styles.monKgOn]}>{kg}</Text>
              </Pressable>
            );
          })}
        </ScrollView>

        <BigNumber
          label={lcdLabelFor(base.direction)(label)}
          value={totals.data ? formatKg(totals.data.quantityKg) : null}
          pending={totals.isPending && enabled}
          sub={foot}
          subTone={footTone === "alert" ? "alert" : "ok"}
          updatedAt={totals.dataUpdatedAt}
          testID="month-kg"
        />
        {shares.length > 0 ? <Composition shares={shares} /> : null}

        {chips.length > 1 ? (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.scrubWrap} contentContainerStyle={styles.chips}>
            {chips.map((c) => {
              const on = c.id === filter;
              return (
                <Pressable
                  key={c.id}
                  testID={`filter-${c.id}`}
                  onPress={() => {
                    haptic.tap();
                    setFilter(c.id);
                  }}
                  style={[styles.chip, on && styles.chipOn]}
                  accessibilityRole="button"
                  accessibilityState={{ selected: on }}
                >
                  {c.id === "noCode" ? <View style={styles.chipDot} /> : null}
                  <Text style={[styles.chipText, on && styles.chipTextOn]}>
                    {c.label}
                    {c.count != null ? <Text style={styles.chipCount}> {c.count}</Text> : null}
                  </Text>
                </Pressable>
              );
            })}
          </ScrollView>
        ) : null}

        <View testID="register" style={styles.reg}>
          {list.isError ? (
            <Note tone="alert">{m.movementsError}</Note>
          ) : !rows ? (
            enabled ? <SkeletonRows /> : <Note>{" "}</Note>
          ) : rows.length === 0 ? (
            <Note>{search ? m.searchNone : filter !== "all" ? m.filterNone : m.movementsEmpty}</Note>
          ) : (
            byDay(rows).map((day) => (
              <View key={day.date}>
                <View style={styles.dayHead}>
                  <Text style={styles.dayName}>{dayLabel(day.date)}</Text>
                  {/* O zi numai cu rânduri care așteaptă cântarul n-are „0 kg”: nu se știe încă. */}
                  {day.kg > 0 ? <Text style={styles.dayKg}>{formatKg(day.kg)} kg</Text> : null}
                </View>
                {day.rows.map((row) => (
                  <RegisterRow key={row.id} row={row} />
                ))}
              </View>
            ))
          )}
          {rows && rows.length > 0 && !narrowed && !list.hasNextPage && totals.data ? (
            <View style={styles.total} testID="register-total">
              <Text style={styles.totalText}>{m.registerTotal(label, totals.data.rows)}</Text>
              <Text style={[styles.totalText, styles.mono]}>{formatKg(totals.data.quantityKg)} kg</Text>
            </View>
          ) : null}
        </View>
        {list.hasNextPage && rows ? (
          <PrimaryButton
            tone="quiet"
            label={m.movementsMore(rows.length, totalRows)}
            onPress={() => list.fetchNextPage()}
            disabled={list.isFetchingNextPage}
            testID="movements-more"
          />
        ) : null}
        {rows && rows.length > 0 ? <Text style={styles.hint}>{m.registerHint}</Text> : null}
      </View>
    </ScrollView>
  );
}

/** O linie din registru: pubela, deșeul (cu pătrățelul pozei), destinatarul și codul R/D, kg-ul. */
function RegisterRow({ row }: { row: WasteMovement }) {
  const bad = missingCode(row);
  const sub = [row.partnerName, row.operationCode, row.documentReference].filter(Boolean).join(" · ");
  return (
    // M1e: rândul deschide predarea, cu tot ce e în ea și cu Anexa 3 / avizul.
    <Pressable
      testID="movement-row"
      onPress={() => router.push({ pathname: "/miscare/[id]", params: { id: row.id } })}
      style={({ pressed }) => [styles.row, pressed && rowStyles.pressed]}
    >
      {/* Pubela pe codul de deșeu, ca pe web (`lib/binColor.ts`) — aceeași listă explicită. */}
      <Bin code={row.wasteCode} hazardous={row.hazardous} />
      <View style={styles.rowBody}>
        <Text style={rowStyles.title} numberOfLines={1}>
          {row.wasteCodeName}
          {row.attachments?.length ? <Text accessibilityLabel={m.withPhoto}>{"  "}<Text style={styles.photo}>▪</Text></Text> : null}
        </Text>
        <Text style={rowStyles.sub} numberOfLines={1}>
          {row.wasteCode}
          {sub ? ` · ${sub}` : ""}
        </Text>
      </View>
      {row.quantity == null ? (
        <Chip label={m.awaitingWeighing} tone="warn" />
      ) : (
        <Text style={[styles.kg, bad && styles.kgBad]}>
          {bad ? "▲ " : ""}
          {formatQuantity(row.quantity, row.unit)} {strings.enums.unit[row.unit]}
        </Text>
      )}
    </Pressable>
  );
}

/** Compoziția lunii: bara pe coduri, cu primele trei în legendă și restul la „altele”. */
function Composition({ shares }: { shares: ReturnType<typeof composition> }) {
  const colorOf = (s: (typeof shares)[number]) => {
    const bin = binFor(s.code, s.hazardous);
    return bin ? binColors[bin] : colors.unknown;
  };
  const top = shares.slice(0, 3);
  const rest = shares.slice(3).reduce((sum, s) => sum + s.kg, 0);
  return (
    <View style={styles.comp} testID="composition">
      <View style={styles.bar}>
        {shares.map((s) => (
          <View key={s.code} style={{ flex: s.kg, backgroundColor: colorOf(s) }} />
        ))}
      </View>
      <View style={styles.legend}>
        {top.map((s) => (
          <View key={s.code} style={styles.legendItem}>
            <View style={[styles.legendDot, { backgroundColor: colorOf(s) }]} />
            <Text style={styles.legendText}>
              {s.code} <Text style={styles.legendKg}>{formatKg(s.kg)}</Text>
            </Text>
          </View>
        ))}
        {rest > 0 ? (
          <Text style={styles.legendText}>
            {m.otherCodes} <Text style={styles.legendKg}>{formatKg(rest)}</Text>
          </Text>
        ) : null}
      </View>
    </View>
  );
}

/** „Sâmbătă 27” — ziua săptămânii și a lunii; luna o spune deja cifra de sus. */
function dayLabel(iso: string) {
  const [y, mo, d] = iso.split("-").map(Number);
  return `${strings.weekdays[new Date(y, mo - 1, d).getDay()]} ${d}`;
}

/**
 * Eticheta afișajului urmează filtrul ecranului, nu ecranul.
 *
 * <p>Altfel aceeași cifră primea două nume: Acasă spunea „PREDAT" (cere `direction=OUT`), iar
 * „Generare" spunea „ÎNREGISTRAT" peste exact același total, fiindcă la un generator pur tot ce e în
 * Anexa 1 a și plecat. Pe „Generare" nu e o direcție, deci „înregistrat" e cuvântul corect acolo.
 */
function lcdLabelFor(direction?: "IN" | "OUT") {
  if (direction === "OUT") return m.monthHanded;
  if (direction === "IN") return m.monthReceived;
  return m.monthRegistered;
}

/** Propoziția de sub titlu: ce e ecranul, pe înțelesul cuiva care nu e specialist. */
function subtitleFor(direction?: "IN" | "OUT") {
  if (direction === "OUT") return m.outSub;
  if (direction === "IN") return m.inSub;
  return m.generationSub;
}

const styles = StyleSheet.create({
  fill: { flex: 1, backgroundColor: colors.ground },
  scroll: { paddingBottom: 120 },
  body: { paddingHorizontal: 16, paddingTop: 12, gap: 10 },
  mono: { fontFamily: fonts.monoMedium },

  iconBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.card,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.separator,
  },
  iconBtnOn: { backgroundColor: colors.quiet },
  search: {
    fontFamily: fonts.sans,
    fontSize: 16,
    color: colors.ink,
    backgroundColor: colors.card,
    borderRadius: 12,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.separator,
    paddingHorizontal: 14,
    paddingVertical: 11,
  },

  // Lunile de răsfoit ies din marginile paginii, ca un rând care se derulează.
  scrubWrap: { flexGrow: 0, marginHorizontal: -16 },
  scrub: { paddingHorizontal: 16, gap: 6 },
  mon: {
    minWidth: 64,
    alignItems: "center",
    gap: 2,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 12,
    backgroundColor: colors.card,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.separator,
  },
  monGap: { borderWidth: 1, borderColor: colors.amber },
  monOn: { backgroundColor: colors.ink, borderColor: colors.ink },
  monName: { fontFamily: fonts.sansSemiBold, fontSize: 13.5, color: colors.ink },
  monNameOn: { color: colors.onAccent },
  monKg: { fontFamily: fonts.mono, fontSize: 11, color: colors.ink2, fontVariant: ["tabular-nums"] },
  monKgGap: { color: colors.amberText },
  monKgOn: { color: "rgba(255,255,255,0.7)" },

  comp: { gap: 8, paddingHorizontal: 4, marginTop: -2 },
  bar: { flexDirection: "row", height: 8, borderRadius: 4, overflow: "hidden", gap: 2, backgroundColor: colors.quiet },
  legend: { flexDirection: "row", flexWrap: "wrap", columnGap: 14, rowGap: 4 },
  legendItem: { flexDirection: "row", alignItems: "center", gap: 6 },
  legendDot: { width: 8, height: 8, borderRadius: 2 },
  legendText: { fontFamily: fonts.mono, fontSize: 12, color: colors.ink2 },
  legendKg: { fontFamily: fonts.monoMedium, color: colors.ink },

  chips: { paddingHorizontal: 16, gap: 6 },
  chip: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    paddingVertical: 7,
    paddingHorizontal: 12,
    borderRadius: 999,
    backgroundColor: colors.card,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.separator,
  },
  chipOn: { backgroundColor: colors.ink, borderColor: colors.ink },
  chipDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.red },
  chipText: { fontFamily: fonts.sansMedium, fontSize: 13.5, color: colors.ink },
  chipTextOn: { color: colors.onAccent },
  chipCount: { fontFamily: fonts.monoMedium },

  reg: { marginTop: 2 },
  dayHead: { flexDirection: "row", justifyContent: "space-between", gap: 10, paddingTop: 14, paddingBottom: 4, paddingHorizontal: 4 },
  dayName: { fontFamily: fonts.sansSemiBold, fontSize: 12, color: colors.ink3, textTransform: "uppercase", letterSpacing: 0.5 },
  dayKg: { fontFamily: fonts.mono, fontSize: 12, color: colors.ink3 },
  row: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    paddingVertical: 11,
    paddingHorizontal: 4,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.separator,
  },
  rowBody: { flex: 1 },
  photo: { color: colors.green, fontSize: 15 },
  kg: { fontFamily: fonts.monoMedium, fontSize: 15, color: colors.ink, fontVariant: ["tabular-nums"] },
  kgBad: { color: colors.redText },
  total: {
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 10,
    paddingVertical: 12,
    paddingHorizontal: 4,
    borderTopWidth: 2,
    borderTopColor: colors.ink,
  },
  totalText: { fontFamily: fonts.sansSemiBold, fontSize: 15, color: colors.ink },
  hint: { fontFamily: fonts.sans, fontSize: 12.5, color: colors.ink3, paddingHorizontal: 4 },
});
