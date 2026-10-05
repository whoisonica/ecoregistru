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
 *
 * <p>Annual billing (05.10.2026): a period lasts {@code billingMonths} months, 1 or 12, and an annual
 * invoice has a single line, the price per year, with nothing added per work point or company. The
 * period index stays absolute: after a switch, period {@code anchorPeriod} starts on
 * {@code periodAnchor} and the next ones are counted from there, so the rules keyed on the index
 * (the fee in period 0, the 12-month commitment) read the same as before.
 */
public final class BillingCalculator {

    static final int TIER1_UP_TO = 10;
    static final int TIER2_UP_TO = 30;
    public static final int COMMITMENT_PERIODS = 12;
    /** {@code billingMonths} of an annual subscription: one invoice a year, at the price per year. */
    public static final int ANNUAL = 12;

    public record Line(String label, int quantity, BigDecimal unitPrice, BigDecimal amount) {}

    /** {@code from} and {@code to} are both billed days. */
    public record Invoice(LocalDate from, LocalDate to, List<Line> lines, BigDecimal total) {}

    private BillingCalculator() {}

    /**
     * The first day of the given period. From the anchor on, periods are counted from the anchor
     * itself; period 0 starts on the start date when there is no anchor. A period before the anchor
     * was billed under the other length, counted from the start date.
     */
    public static LocalDate periodStart(Subscription s, int period) {
        if (period >= s.getAnchorPeriod()) {
            return anchor(s).plusMonths((long) (period - s.getAnchorPeriod()) * s.getBillingMonths());
        }
        // Exact only for the most recent switch: earlier segments are not stored. Callers never
        // reserve or bill a period before periodAnchor (Task 2 enforces this).
        int before = s.getBillingMonths() == ANNUAL ? 1 : ANNUAL;
        return s.getStartedAt().plusMonths((long) period * before);
    }

    /** The last billed day of the given period. */
    public static LocalDate periodEnd(Subscription s, int period) {
        return periodStart(s, period + 1).minusDays(1);
    }

    /**
     * The period {@code day} falls in, or 0 before the start. A day before the anchor belongs to the
     * period just before it, already billed. A month is not a fixed length, so the estimate from whole
     * months is corrected both ways: started on 31.01, 28.02 is already period 1.
     */
    public static int periodOn(Subscription s, LocalDate day) {
        LocalDate anchor = anchor(s);
        if (day.isBefore(anchor)) {
            return day.isBefore(s.getStartedAt()) ? 0 : Math.max(s.getAnchorPeriod() - 1, 0);
        }
        long months = s.getBillingMonths();
        int n = (int) (ChronoUnit.MONTHS.between(anchor, day) / months);
        while (!anchor.plusMonths((n + 1) * months).isAfter(day)) {
            n++;
        }
        while (n > 0 && anchor.plusMonths(n * months).isAfter(day)) {
            n--;
        }
        return s.getAnchorPeriod() + n;
    }

    private static LocalDate anchor(Subscription s) {
        return s.getPeriodAnchor() != null ? s.getPeriodAnchor() : s.getStartedAt();
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
        if (s.getBillingMonths() == ANNUAL) {
            // One line, the price per year: nothing is added per work point or managed company.
            recurring(lines, s.getPlan().label() + ", abonament anual", 1, s.getMonthlyPrice());
        } else {
            recurring(lines, baseLabel(s), 1, s.getMonthlyPrice());
            perUnit(lines, s, activeWorkPoints, managedCompanies, packagingCompanies);
        }

        String label = s.getPlan().forConsultancy() ? "Pornirea contului de consultant" : "Implementare";
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
        return new Invoice(periodStart(s, period), periodEnd(s, period), List.copyOf(lines), total);
    }

    /**
     * The last billed period of a stopped subscription, ending before the 12th month does. The
     * commitment counts months, not periods, so an annual period is a year of it. For a monthly
     * subscription without an anchor this is exactly {@code period < 11}: a last period of 11 is the
     * twelfth month and the commitment is kept.
     */
    static boolean stoppedEarlyIn(Subscription s, int period) {
        LocalDate commitmentEnds = s.getStartedAt().plusMonths(COMMITMENT_PERIODS).minusDays(1);
        return s.getEndsOn() != null && periodEnd(s, period).isBefore(commitmentEnds)
                && !periodEnd(s, period).isBefore(s.getEndsOn());
    }

    /** The plan label, with the employee tier when the subscription is on the grid and not custom-priced. */
    static String baseLabel(Subscription s) {
        if (s.getSizeTier() == null || s.isCustomPrice()) {
            return s.getPlan().label();
        }
        return s.getPlan().label() + ", treapta " + s.getSizeTier().number()
                + " (" + s.getSizeTier().range() + " angajați)";
    }

    private static void perUnit(List<Line> lines, Subscription s, int activeWorkPoints, int managedCompanies,
                                int packagingCompanies) {
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
    }

    private static void recurring(List<Line> lines, String label, int quantity, BigDecimal unitPrice) {
        if (quantity > 0 && unitPrice.signum() != 0) {
            lines.add(new Line(label, quantity, unitPrice, unitPrice.multiply(BigDecimal.valueOf(quantity))));
        }
    }
}
