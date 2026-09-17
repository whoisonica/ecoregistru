package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.request.BillingDetailsRequest;
import ro.ecoregistru.controller.request.PaymentMethodRequest;
import ro.ecoregistru.controller.response.BillingAccessResponse;
import ro.ecoregistru.controller.response.BillingResponse;
import ro.ecoregistru.controller.response.CardCheckoutResponse;
import ro.ecoregistru.controller.response.CardPaymentResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.BillingRunService;
import ro.ecoregistru.service.CardPaymentService;
import ro.ecoregistru.service.SubscriptionService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * F2 of plata-abonamente.md — the client's own subscription and invoices, on {@code /abonament}. The
 * platform sets the package and the billing data on the Clients screen.
 *
 * <p>F3 — the client chooses card or transfer and pays an issued invoice on Netopia's page. F4 — every
 * role reads whether the account is read-only, for the banner.
 *
 * <p>An account with nothing to pay answers 204: a company of a cabinet (the cabinet pays), or a
 * client not billed at all. {@code /api/v1/billing/**} stays open to a read-only account: paying is how
 * it opens again.
 */
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingController {

    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN')";

    SubscriptionService subscriptionService;
    CardPaymentService cardPaymentService;
    BillingRunService billingRunService;

    @GetMapping
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<BillingResponse> current(@AuthenticationPrincipal AppUser user) {
        return subscriptionService.forAccount(user, TenantContext.get(), today())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** F4 — every role: an operator must know why „Adaugă" is gone. */
    @GetMapping("/access")
    public BillingAccessResponse access(@AuthenticationPrincipal AppUser user) {
        return subscriptionService.access(user, TenantContext.get(), today());
    }

    @PutMapping("/payment-method")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> paymentMethod(@AuthenticationPrincipal AppUser user,
                                              @RequestBody @Valid PaymentMethodRequest request) {
        subscriptionService.choosePaymentMethod(user, TenantContext.get(), request.paymentMethod());
        return ResponseEntity.noContent().build();
    }

    /** F-E — the client keeps its billing data up to date (contract art. 7.5); not the name, nor the CUI. */
    @PutMapping("/details")
    @PreAuthorize(CAN_MANAGE)
    public BillingResponse details(@AuthenticationPrincipal AppUser user,
                                   @RequestBody @Valid BillingDetailsRequest request) {
        return subscriptionService.updateBillingDetails(user, TenantContext.get(), request, today());
    }

    /** F-E — „Am plătit — verifică acum”: FGO asked about one of the account's own issued invoices. */
    @PostMapping("/invoices/{id}/check-payment")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> checkPayment(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        billingRunService.checkPaymentForAccount(user, TenantContext.get(), id, today());
        return ResponseEntity.noContent().build();
    }

    /** F3 — Netopia's payment page for one of the account's issued invoices. */
    @PostMapping("/invoices/{id}/card")
    @PreAuthorize(CAN_MANAGE)
    public CardCheckoutResponse payByCard(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return new CardCheckoutResponse(cardPaymentService.checkout(user, TenantContext.get(), id));
    }

    /** F3 — where the client lands back from Netopia: paid, refused, or still waiting for the notification. */
    @GetMapping("/card-payments/{id}")
    @PreAuthorize(CAN_MANAGE)
    public CardPaymentResponse cardPayment(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return subscriptionService.cardPayment(user, TenantContext.get(), id);
    }

    private static LocalDate today() {
        return LocalDate.now(BillingRunService.ZONE);
    }
}
