package ro.ecoregistru.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Termenul de confirmare SIATD numărat după Codul civil (art. 2.553–2.554): fără prima și ultima zi, iar un termen care
 * se împlinește într-o zi nelucrătoare trece la prima zi lucrătoare. Zilele nelucrătoare = sâmbăta, duminica și
 * sărbătorile legale din Codul muncii art. 139 alin. (1), citite pe 28.09.2026 ({@code surse-oficiale.md} §6.1).
 * Alt termen, altă regulă decât {@code InventoryService.addWorkingDays}.
 */
public final class SiatdCalendar {

    private static final Set<MonthDay> FIXED = Set.of(
            MonthDay.of(1, 1), MonthDay.of(1, 2), MonthDay.of(1, 6), MonthDay.of(1, 7), MonthDay.of(1, 24),
            MonthDay.of(5, 1), MonthDay.of(6, 1), MonthDay.of(8, 15), MonthDay.of(11, 30), MonthDay.of(12, 1),
            MonthDay.of(12, 25), MonthDay.of(12, 26));

    /** Vinerea Mare, Paștele (2 zile), Rusaliile (2 zile) — față de duminica Paștelui ortodox. */
    private static final int[] EASTER_OFFSETS = {-2, 0, 1, 49, 50};

    private SiatdCalendar() {
    }

    /** Ultima zi în care se mai poate confirma (până la 24:00). */
    public static LocalDate due(LocalDate start, int days) {
        LocalDate day = start.plusDays(days + 1L);
        while (isNonWorking(day)) {
            day = day.plusDays(1);
        }
        return day;
    }

    /** Mementoul: o zi mai devreme decât termenul fără zilele nelucrătoare (prudența după RIL 8/2024). */
    public static LocalDate reminder(LocalDate start, int days) {
        return start.plusDays(days);
    }

    public static boolean isNonWorking(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY || FIXED.contains(MonthDay.from(day))) {
            return true;
        }
        LocalDate easter = orthodoxEaster(day.getYear());
        for (int offset : EASTER_OFFSETS) {
            if (day.equals(easter.plusDays(offset))) {
                return true;
            }
        }
        return false;
    }

    /** Algoritmul lui Meeus pe calendarul iulian, mutat în gregorian (+13 zile, valabil 1900–2099). */
    public static LocalDate orthodoxEaster(int year) {
        int a = year % 4;
        int b = year % 7;
        int c = year % 19;
        int d = (19 * c + 15) % 30;
        int e = (2 * a + 4 * b - d + 34) % 7;
        int month = (d + e + 114) / 31;
        int day = (d + e + 114) % 31 + 1;
        return LocalDate.of(year, month, day).plusDays(13);
    }
}
