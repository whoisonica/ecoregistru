package ro.ecoregistru.service;

import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Where a subscription is on the road of plata-abonamente.md §2.3, from its invoices alone. Pure, so the
 * daily run, a card notification and the tests all read the same rule.
 *
 * <table>
 *   <tr><td>no unpaid invoice past its due date</td><td>ACTIVE once anything was paid, else PENDING</td></tr>
 *   <tr><td>due date + 1</td><td>PAST_DUE</td></tr>
 *   <tr><td>due date + 15</td><td>READ_ONLY — only with {@code app.billing.read-only-enabled} (§9.6)</td></tr>
 *   <tr><td>after {@code endsOn} (§9.3)</td><td>CANCELLED, whatever is still owed</td></tr>
 * </table>
 *
 * <p>Payment brings it back on the next reading: nothing here remembers having been read-only.
 */
public final class SubscriptionStatusRules {

    /** Decision 6: read-only 15 days after the due date. */
    public static final int READ_ONLY_AFTER_DAYS = 15;
    /** §2.3: the warning goes out 7 days before read-only. */
    public static final int WARNING_AFTER_DAYS = 8;

    private SubscriptionStatusRules() {
    }

    public static SubscriptionStatus statusOn(LocalDate today, List<SubscriptionInvoice> invoices, LocalDate endsOn,
                                              boolean readOnlyEnabled, SubscriptionStatus current) {
        if (current == SubscriptionStatus.CANCELLED || (endsOn != null && today.isAfter(endsOn))) {
            return SubscriptionStatus.CANCELLED;
        }
        if (invoices.isEmpty()) {
            return current;
        }
        Optional<LocalDate> oldestUnpaidDue = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.ISSUED && i.getDueDate() != null)
                .map(SubscriptionInvoice::getDueDate)
                .min(Comparator.naturalOrder());
        if (oldestUnpaidDue.isPresent() && today.isAfter(oldestUnpaidDue.get())) {
            boolean readOnly = readOnlyEnabled && !today.isBefore(oldestUnpaidDue.get().plusDays(READ_ONLY_AFTER_DAYS));
            return readOnly ? SubscriptionStatus.READ_ONLY : SubscriptionStatus.PAST_DUE;
        }
        boolean anyPaid = invoices.stream().anyMatch(i -> i.getStatus() == InvoiceStatus.PAID);
        return anyPaid ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING;
    }

    /** What the read-only filter refuses writes for. */
    public static boolean restricts(SubscriptionStatus status) {
        return status == SubscriptionStatus.READ_ONLY || status == SubscriptionStatus.CANCELLED;
    }
}
