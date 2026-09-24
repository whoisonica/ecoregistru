package ro.ecoregistru.service;

import ro.ecoregistru.entity.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The amount of one billing period of a subscription, as the lines printed on the invoice.
 *
 * <p><b>A period runs from the day the subscription starts, to the day before the same date of the
 * next month</b>, at the full price (decision of 15.09.2026): started on 17 October, the periods are
 * 17.10–16.11, 17.11–16.12, and so on. Nothing is billed by the day. Every period is counted from
 * the start date itself, never from the previous period, so a subscription started on 31 January
 * runs 31.01–27.02, then 28.02–30.03, then 31.03 again, instead of drifting to the 28th for good.
 *
 * <p>A pure function on purpose: no clock, no repository. The caller counts what the period bills
 * on — active work points for a company, active companies for a consultancy — when the period
 * starts, so a company added to a consultancy mid-period is paid from the next period (decision of
 * 15.09.2026). The first period also carries the implementation fee.
 *
 * <p>With the 12-month commitment (contract art. 4.3, 24.09.2026) the fee moves: the first period
 * shows it as free, and it is billed only on the last period of a subscription stopped before the
 * 12th. Only the remaining months are forgiven, never the fee itself. A founder pays it in no case.
 */
public final class BillingCalculator {

    static final int TIER1_UP_TO = 10;
    static final int TIER2_UP_TO = 30;
    public static final int COMMITMENT_PERIODS = 12;

    public record Line(String label, int quantity, BigDecimal unitPrice, BigDecimal amount) {}

    /** {@code from} and {@code to} are both billed days. */
    public record Invoice(LocalDate from, LocalDate to, List<Line> lines, BigDecimal total) {}

    private BillingCalculator() {}

    /** The first day of the given period; period 0 starts on the subscription's start date. */
    public static LocalDate periodStart(Subscription s, int period) {
        return s.getStartedAt().plusMonths(period);
    }

    /**
     * The period {@code day} falls in, or 0 before the start. A month is not a fixed length, so the
     * estimate from whole months is corrected both ways: started on 31.01, 28.02 is already period 1.
     */
    public static int periodOn(LocalDate startedAt, LocalDate day) {
        if (day.isBefore(startedAt)) {
            return 0;
        }
        int period = (int) ChronoUnit.MONTHS.between(startedAt, day);
        while (!startedAt.plusMonths(period + 1).isAfter(day)) {
            period++;
        }
        while (period > 0 && startedAt.plusMonths(period).isAfter(day)) {
            period--;
        }
        return period;
    }

    /**
     * @param period             0 for the first period, 1 for the next, and so on
     * @param activeWorkPoints   a company's active work points; ignored for a consultancy
     * @param managedCompanies   a consultancy's active companies; ignored for a company
     * @param packagingCompanies how many of those put packaging on the market
     */
    public static Invoice invoice(Subscription s, int activeWorkPoints, int managedCompanies,
                                  int packagingCompanies, int period) {
        if (period < 0) {
            throw new IllegalArgumentException("There is no period before the subscription starts");
        }
        List<Line> lines = new ArrayList<>();
        recurring(lines, s.getPlan().label(), 1, s.getMonthlyPrice());
        if (s.getPlan().forConsultancy()) {
            int n = managedCompanies;
            recurring(lines, "Firmă gestionată, 1–10", Math.min(n, TIER1_UP_TO), s.getCompanyPriceTier1());
            recurring(lines, "Firmă gestionată, 11–30",
                    Math.clamp(n - TIER1_UP_TO, 0, TIER2_UP_TO - TIER1_UP_TO), s.getCompanyPriceTier2());
            recurring(lines, "Firmă gestionată, 31 și peste", Math.max(n - TIER2_UP_TO, 0), s.getCompanyPriceTier3());
            recurring(lines, "Ambalaje, pe firmă", packagingCompanies, s.getPackagingCompanyPrice());
        } else {
            recurring(lines, "Punct de lucru în plus", Math.max(activeWorkPoints - 1, 0), s.getExtraWorkPointPrice());
        }

        String label = s.getPlan().forConsultancy() ? "Pornirea cabinetului" : "Implementare";
        boolean fee = s.getImplementationFee().signum() > 0;
        if (s.isFounder()) {
            if (period == 0) {
                lines.add(new Line(label + " (client fondator, gratuită)", 1, BigDecimal.ZERO, BigDecimal.ZERO));
            }
        } else if (!s.isTwelveMonthCommitment()) {
            if (period == 0 && fee) {
                lines.add(new Line(label, 1, s.getImplementationFee(), s.getImplementationFee()));
            }
        } else if (fee && stoppedEarlyIn(s, period)) {
            lines.add(new Line(label + " (oprire înainte de 12 luni)", 1, s.getImplementationFee(),
                    s.getImplementationFee()));
        } else if (period == 0 && fee) {
            lines.add(new Line(label + " (angajament 12 luni, gratuită)", 1, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        BigDecimal total = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Invoice(periodStart(s, period), periodStart(s, period + 1).minusDays(1),
                List.copyOf(lines), total);
    }

    /**
     * The last billed period of a stopped subscription, before the 12th: periods count from 0, so a
     * last period of 11 is the twelfth month and the commitment is kept.
     */
    static boolean stoppedEarlyIn(Subscription s, int period) {
        return s.getEndsOn() != null && period < COMMITMENT_PERIODS - 1
                && !periodStart(s, period + 1).minusDays(1).isBefore(s.getEndsOn());
    }

    private static void recurring(List<Line> lines, String label, int quantity, BigDecimal unitPrice) {
        if (quantity > 0) {
            lines.add(new Line(label, quantity, unitPrice, unitPrice.multiply(BigDecimal.valueOf(quantity))));
        }
    }
}
