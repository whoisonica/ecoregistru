package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.SubscriptionRequest;
import ro.ecoregistru.controller.response.BillingInvoiceRow;
import ro.ecoregistru.controller.response.SubscriptionResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.BillingRun;
import ro.ecoregistru.service.BillingRunService;
import ro.ecoregistru.service.SubscriptionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F1 of plata-abonamente.md — the subscriptions, as the platform sets them on the Clients screen.
 * A client without one answers 204: not billed, not restricted.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController {

    static final String PLATFORM_ONLY = "hasAuthority('PLATFORM_ADMIN')";

    SubscriptionService subscriptionService;
    BillingRunService billingRunService;

    @GetMapping("/company/{id}")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<SubscriptionResponse> company(@PathVariable UUID id) {
        return orNoContent(subscriptionService.forCompany(id));
    }

    @PutMapping("/company/{id}")
    @PreAuthorize(PLATFORM_ONLY)
    public SubscriptionResponse saveCompany(@PathVariable UUID id, @RequestBody @Valid SubscriptionRequest request) {
        return subscriptionService.saveForCompany(id, request);
    }

    @DeleteMapping("/company/{id}")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID id) {
        subscriptionService.deleteForCompany(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consultancy/{id}")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<SubscriptionResponse> consultancy(@PathVariable UUID id) {
        return orNoContent(subscriptionService.forConsultancy(id));
    }

    @PutMapping("/consultancy/{id}")
    @PreAuthorize(PLATFORM_ONLY)
    public SubscriptionResponse saveConsultancy(@PathVariable UUID id, @RequestBody @Valid SubscriptionRequest request) {
        return subscriptionService.saveForConsultancy(id, request);
    }

    @DeleteMapping("/consultancy/{id}")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<Void> deleteConsultancy(@PathVariable UUID id) {
        subscriptionService.deleteForConsultancy(id);
        return ResponseEntity.noContent().build();
    }

    /** F4, §9.3 — stopped with a month's notice: billed to the end of that period, then read-only. */
    @PostMapping("/company/{id}/cancel")
    @PreAuthorize(PLATFORM_ONLY)
    public SubscriptionResponse cancelCompany(@PathVariable UUID id) {
        return subscriptionService.cancelForCompany(id, LocalDate.now(BillingRunService.ZONE));
    }

    @PostMapping("/company/{id}/resume")
    @PreAuthorize(PLATFORM_ONLY)
    public SubscriptionResponse resumeCompany(@PathVariable UUID id) {
        return subscriptionService.resumeForCompany(id);
    }

    @PostMapping("/consultancy/{id}/cancel")
    @PreAuthorize(PLATFORM_ONLY)
    public SubscriptionResponse cancelConsultancy(@PathVariable UUID id) {
        return subscriptionService.cancelForConsultancy(id, LocalDate.now(BillingRunService.ZONE));
    }

    @PostMapping("/consultancy/{id}/resume")
    @PreAuthorize(PLATFORM_ONLY)
    public SubscriptionResponse resumeConsultancy(@PathVariable UUID id) {
        return subscriptionService.resumeForConsultancy(id);
    }

    /** „X din 30" next to the founder box. */
    @GetMapping("/founders")
    @PreAuthorize(PLATFORM_ONLY)
    public long founders() {
        return subscriptionService.founderCount();
    }

    /**
     * F2 — the daily invoicing run, now, instead of waiting for 06:30. The same run the scheduler
     * does, so pressing it twice issues nothing twice.
     */
    @PostMapping("/billing/run")
    @PreAuthorize(PLATFORM_ONLY)
    public BillingRunService.Result runBilling(@AuthenticationPrincipal AppUser user) {
        return billingRunService.run(LocalDate.now(BillingRunService.ZONE), BillingRun.Kind.MANUAL, user.getId());
    }

    /** F-A — the last run, the 06:30 one or a button press, row by row. 204 before the first. */
    @GetMapping("/billing/runs/last")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<BillingRunService.LastRun> lastRun() {
        return billingRunService.lastRun().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** F-A — every client's invoices, for the Facturare screen. */
    @GetMapping("/invoices")
    @PreAuthorize(PLATFORM_ONLY)
    public List<BillingInvoiceRow> invoices() {
        return subscriptionService.allInvoices();
    }

    /** F-A — FGO asked about one invoice now, not at the next run. */
    @PostMapping("/invoices/{id}/check-payment")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<Void> checkPayment(@PathVariable UUID id) {
        billingRunService.checkPayment(id, LocalDate.now(BillingRunService.ZONE));
        return ResponseEntity.noContent().build();
    }

    /** F-A — „Oprește” on an invoice FGO refused: the reservation and its subscription go. */
    @PostMapping("/invoices/{id}/discard")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<Void> discard(@PathVariable UUID id) {
        subscriptionService.discardFailedInvoice(id);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<SubscriptionResponse> orNoContent(Optional<SubscriptionResponse> s) {
        return s.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
