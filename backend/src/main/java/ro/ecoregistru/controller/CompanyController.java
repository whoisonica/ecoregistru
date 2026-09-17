package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.AssignConsultancyRequest;
import ro.ecoregistru.controller.request.CompanyRequest;
import ro.ecoregistru.controller.request.OnboardClientRequest;
import ro.ecoregistru.controller.response.OnboardClientResponse;
import ro.ecoregistru.service.ClientOnboardingService;
import ro.ecoregistru.controller.request.InviteUserRequest;
import ro.ecoregistru.controller.request.PriceVisibilityRequest;
import ro.ecoregistru.controller.response.ClientOverviewResponse;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.service.CompanyService;

import java.util.List;
import java.util.UUID;

/**
 * Company (tenant) directory + management. The only global (NOT tenant-scoped) domain endpoints:
 * they drive the tenant switcher (X-Tenant-Id) and create/edit companies and invite users onto
 * them. A deliberate, documented exception to the "everything is tenant-scoped" rule — hence the
 * strict role gate on every method.
 *
 * <p>P2.13: a {@code CONSULTANT} reaches the same endpoints, and {@code CompanyService} narrows
 * every one of them to their consultancy's companies. Moving a company between consultancies stays
 * with the platform admin.
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyController {

    static final String PLATFORM_ONLY = "hasAuthority('PLATFORM_ADMIN')";
    static final String MULTI_COMPANY = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT')";

    CompanyService companyService;
    ClientOnboardingService clientOnboardingService;

    @GetMapping
    @PreAuthorize(MULTI_COMPANY)
    public List<CompanyResponse> list() {
        return companyService.listAll();
    }

    /** F-B — subscription, latest invoice and user count per company, for the Clients table. */
    @GetMapping("/overview")
    @PreAuthorize(MULTI_COMPANY)
    public List<ClientOverviewResponse> overview() {
        return companyService.overview();
    }

    /**
     * The current tenant's own profile - the one endpoint here that is not platform-only, because
     * every screen needs it: the movement form offers the operations this kind of company may
     * record (CompanyType.allowedOperations()), and only the company itself knows its type.
     */
    @GetMapping("/current")
    public CompanyResponse current() {
        return companyService.current();
    }

    /**
     * D1.8 — cine vede prețurile depozitului. Doar adminul firmei (decizia proprietarului, 15.09.2026):
     * consultantul și platforma nu, altfel și-ar deschide singuri prețurile ascunse de el. Serviciul
     * verifică același lucru.
     */
    @PutMapping("/current/price-visibility")
    @PreAuthorize("hasAuthority('ADMIN')")
    public CompanyResponse updatePriceVisibility(@RequestBody PriceVisibilityRequest request) {
        return companyService.updatePriceVisibility(request.priceVisibility());
    }

    @PostMapping
    @PreAuthorize(MULTI_COMPANY)
    public ResponseEntity<CompanyResponse> create(@RequestBody @Valid CompanyRequest request) {
        return ResponseEntity.ok(companyService.create(request));
    }

    /** F-C — „Client nou”: the company, its request, subscription and administrator together, or nothing. */
    @PostMapping("/onboard")
    @PreAuthorize(MULTI_COMPANY)
    public OnboardClientResponse onboard(@RequestBody @Valid OnboardClientRequest request) {
        return clientOnboardingService.onboard(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(MULTI_COMPANY)
    public CompanyResponse update(@PathVariable UUID id, @RequestBody @Valid CompanyRequest request) {
        return companyService.update(id, request);
    }

    @PostMapping("/{id}/users")
    @PreAuthorize(MULTI_COMPANY)
    public ResponseEntity<CompanyUserResponse> inviteUser(
            @PathVariable UUID id, @RequestBody @Valid InviteUserRequest request) {
        return ResponseEntity.ok(companyService.inviteUser(id, request));
    }

    /** P2.13 — which consultancy manages the company; {@code null} makes it a direct client. */
    @PutMapping("/{id}/consultancy")
    @PreAuthorize(PLATFORM_ONLY)
    public CompanyResponse assignConsultancy(
            @PathVariable UUID id, @RequestBody AssignConsultancyRequest request) {
        return companyService.assignConsultancy(id, request.consultancyId());
    }
}
