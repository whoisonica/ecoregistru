package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.InviteUserRequest;
import ro.ecoregistru.controller.request.OnboardClientRequest;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.controller.response.OnboardClientResponse;
import ro.ecoregistru.controller.response.SubscriptionResponse;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.security.SecurityUtils;

/**
 * F-C of todo-clienti-abonamente.md — a client made in one step instead of three or four: approving a request used to
 * create only the company, and the subscription and the invitation were separate acts nothing reminded anyone of.
 *
 * <p><b>One transaction.</b> A CUI already taken, a price plan that does not fit or an email that already has an
 * account undoes the company too. The invitation goes last, because its mail leaves at once: everything that can
 * still refuse has already answered by then.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientOnboardingService {

    CompanyService companyService;
    AccountRequestService accountRequestService;
    SubscriptionService subscriptionService;

    @Transactional
    public OnboardClientResponse onboard(OnboardClientRequest request) {
        boolean platform = SecurityUtils.currentUser().getRole() == Role.PLATFORM_ADMIN;
        // A consultant adds companies to their cabinet, which pays for them; requests and prices are the platform's.
        if (!platform && (request.accountRequestId() != null || request.subscription() != null)) {
            throw new AccessDeniedException("Only the platform approves requests and sets subscriptions");
        }

        CompanyResponse company = request.accountRequestId() != null
                ? accountRequestService.approve(request.accountRequestId(), request.company())
                : companyService.create(request.company());

        SubscriptionResponse subscription = request.subscription() == null ? null
                : subscriptionService.saveForCompany(company.id(), request.subscription());

        String invited = null;
        if (request.admin() != null) {
            OnboardClientRequest.Admin admin = request.admin();
            invited = companyService.inviteUser(company.id(),
                    new InviteUserRequest(admin.email().trim(), Role.ADMIN,
                            blankToNull(admin.firstName()), blankToNull(admin.lastName()))).email();
        }

        return new OnboardClientResponse(company,
                subscription == null ? null : subscription.plan(),
                subscription == null ? null : subscription.firstInvoice(),
                invited);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
