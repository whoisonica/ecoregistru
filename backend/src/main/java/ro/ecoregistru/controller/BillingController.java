package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.BillingResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.BillingRunService;
import ro.ecoregistru.service.SubscriptionService;

import java.time.LocalDate;

/**
 * F2 of plata-abonamente.md — the client's own subscription and invoices, on {@code /abonament}.
 * Read-only: the platform sets the package and the billing data on the Clients screen.
 *
 * <p>An account with nothing to pay answers 204: a company of a cabinet (the cabinet pays), or a
 * client not billed at all.
 */
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingController {

    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN')";

    SubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<BillingResponse> current(@AuthenticationPrincipal AppUser user) {
        return subscriptionService.forAccount(user, TenantContext.get(), LocalDate.now(BillingRunService.ZONE))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
