package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.ecoregistru.enums.SubscriptionStatus.*;

/**
 * F4 — drumul neplății din plata-abonamente.md §2.3, pe zile: scadența +1 restant, +15 doar-citire (numai cu
 * flagul pornit, §9.6), plata întoarce contul, oprirea (§9.3) îl închide după ultima zi.
 */
class SubscriptionStatusRulesTest {

    static final LocalDate DUE = LocalDate.of(2026, 10, 27);

    @Test
    void theDueDateItselfIsNotLate() {
        assertThat(status(DUE, true, issued(DUE))).isEqualTo(PENDING);
        assertThat(status(DUE.plusDays(1), true, issued(DUE))).isEqualTo(PAST_DUE);
    }

    @Test
    void readOnlyFifteenDaysAfterTheDueDateOnlyWhenSwitchedOn() {
        assertThat(status(DUE.plusDays(14), true, issued(DUE))).isEqualTo(PAST_DUE);
        assertThat(status(DUE.plusDays(15), true, issued(DUE))).isEqualTo(READ_ONLY);
        assertThat(status(DUE.plusDays(60), false, issued(DUE))).isEqualTo(PAST_DUE);
    }

    /** Plata celei vechi, cu una nouă încă în termen: contul revine singur, fără să-și amintească nimic. */
    @Test
    void paymentBringsTheAccountBack() {
        LocalDate today = DUE.plusDays(20);
        assertThat(SubscriptionStatusRules.statusOn(today, List.of(paid(DUE), issued(today.plusDays(5))), null, true,
                READ_ONLY)).isEqualTo(ACTIVE);
    }

    /** Cea mai veche factură neplătită decide, nu ultima. */
    @Test
    void theOldestUnpaidInvoiceDecides() {
        LocalDate today = DUE.plusDays(16);
        assertThat(status(today, true, issued(DUE), issued(today.plusDays(3)))).isEqualTo(READ_ONLY);
    }

    @Test
    void aStoppedSubscriptionEndsAfterItsLastDay() {
        LocalDate endsOn = DUE.plusDays(3);
        assertThat(SubscriptionStatusRules.statusOn(endsOn, List.of(paid(DUE)), endsOn, false, ACTIVE)).isEqualTo(ACTIVE);
        assertThat(SubscriptionStatusRules.statusOn(endsOn.plusDays(1), List.of(paid(DUE)), endsOn, false, ACTIVE))
                .isEqualTo(CANCELLED);
        assertThat(SubscriptionStatusRules.statusOn(DUE, List.of(), null, false, CANCELLED)).isEqualTo(CANCELLED);
    }

    @Test
    void withoutInvoicesTheStatusStays() {
        assertThat(SubscriptionStatusRules.statusOn(DUE, List.of(), null, true, PENDING)).isEqualTo(PENDING);
    }

    @Test
    void onlyReadOnlyAndCancelledRestrict() {
        assertThat(List.of(SubscriptionStatus.values())).filteredOn(SubscriptionStatusRules::restricts)
                .containsExactlyInAnyOrder(READ_ONLY, CANCELLED);
    }

    private static SubscriptionStatus status(LocalDate today, boolean readOnly, SubscriptionInvoice... invoices) {
        return SubscriptionStatusRules.statusOn(today, List.of(invoices), null, readOnly, PENDING);
    }

    private static SubscriptionInvoice issued(LocalDate due) {
        return SubscriptionInvoice.builder().status(InvoiceStatus.ISSUED).dueDate(due).build();
    }

    private static SubscriptionInvoice paid(LocalDate due) {
        return SubscriptionInvoice.builder().status(InvoiceStatus.PAID).dueDate(due).build();
    }
}
