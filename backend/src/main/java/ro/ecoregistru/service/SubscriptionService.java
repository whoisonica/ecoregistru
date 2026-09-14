package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.SubscriptionRequest;
import ro.ecoregistru.controller.response.SubscriptionInvoiceResponse;
import ro.ecoregistru.controller.response.SubscriptionResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.CONSULTANCY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.SUBSCRIPTION_COMPANY_IN_CONSULTANCY;
import static ro.ecoregistru.exception.ErrorMessageEnum.SUBSCRIPTION_HAS_INVOICES;
import static ro.ecoregistru.exception.ErrorMessageEnum.SUBSCRIPTION_PLAN_MISMATCH;

/**
 * F1 of plata-abonamente.md — the platform sets the package of a direct company or of a
 * consultancy, and sees what it will invoice. F2 adds the billing data and the invoices issued by
 * {@link BillingRunService}.
 *
 * <p><b>The grid is copied, not referenced.</b> Creating a subscription, or moving it to another
 * plan, writes today's prices on it; saving it again on the same plan keeps the prices it had.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionService {

    SubscriptionRepository subscriptionRepository;
    SubscriptionInvoiceRepository invoiceRepository;
    CompanyRepository companyRepository;
    ConsultancyRepository consultancyRepository;
    WorkPointRepository workPointRepository;

    @Transactional(readOnly = true)
    public Optional<SubscriptionResponse> forCompany(UUID companyId) {
        requireCompany(companyId);
        return subscriptionRepository.findByCompany_Id(companyId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionResponse> forConsultancy(UUID consultancyId) {
        requireConsultancy(consultancyId);
        return subscriptionRepository.findByConsultancy_Id(consultancyId).map(this::toResponse);
    }

    @Transactional
    public SubscriptionResponse saveForCompany(UUID companyId, SubscriptionRequest request) {
        Company company = requireCompany(companyId);
        if (company.getConsultancy() != null) {
            throw new UnprocessableEntityException(SUBSCRIPTION_COMPANY_IN_CONSULTANCY);
        }
        if (request.plan().forConsultancy()) {
            throw new UnprocessableEntityException(SUBSCRIPTION_PLAN_MISMATCH);
        }
        Subscription s = subscriptionRepository.findByCompany_Id(companyId)
                .orElseGet(() -> pending().company(company).build());
        return toResponse(apply(s, request));
    }

    @Transactional
    public SubscriptionResponse saveForConsultancy(UUID consultancyId, SubscriptionRequest request) {
        Consultancy consultancy = requireConsultancy(consultancyId);
        if (!request.plan().forConsultancy()) {
            throw new UnprocessableEntityException(SUBSCRIPTION_PLAN_MISMATCH);
        }
        Subscription s = subscriptionRepository.findByConsultancy_Id(consultancyId)
                .orElseGet(() -> pending().consultancy(consultancy).build());
        return toResponse(apply(s, request));
    }

    /**
     * A mistaken subscription goes away; the client is back to not billed. Not once it has invoices:
     * the row is what ties them to FGO.
     */
    @Transactional
    public void deleteForCompany(UUID companyId) {
        requireCompany(companyId);
        subscriptionRepository.findByCompany_Id(companyId).ifPresent(this::delete);
    }

    @Transactional
    public void deleteForConsultancy(UUID consultancyId) {
        requireConsultancy(consultancyId);
        subscriptionRepository.findByConsultancy_Id(consultancyId).ifPresent(this::delete);
    }

    @Transactional(readOnly = true)
    public long founderCount() {
        return subscriptionRepository.countByFounderTrue();
    }

    /**
     * The invoice of one period, counted on the database as it is now. {@link BillingRunService} calls
     * it when the period starts, which is what makes a company added mid-period payable from the next.
     * Needs an open transaction: it reads the owner.
     */
    public BillingCalculator.Invoice invoiceFor(Subscription s, int period) {
        int workPoints = 0;
        int companies = 0;
        int packaging = 0;
        if (s.getConsultancy() != null) {
            List<Company> managed = companyRepository.findAllByConsultancy_Id(s.getConsultancy().getId())
                    .stream().filter(Company::isActive).toList();
            companies = managed.size();
            packaging = (int) managed.stream()
                    .filter(c -> MarketRole.putsPackagingOnMarket(c.getMarketRoles())).count();
        } else {
            workPoints = (int) workPointRepository.countByCompany_IdAndActiveTrue(s.getCompany().getId());
        }
        return BillingCalculator.invoice(s, workPoints, companies, packaging, period);
    }

    private void delete(Subscription s) {
        if (invoiceRepository.existsBySubscription_Id(s.getId())) {
            throw new UnprocessableEntityException(SUBSCRIPTION_HAS_INVOICES);
        }
        subscriptionRepository.delete(s);
    }

    private static Subscription.SubscriptionBuilder pending() {
        return Subscription.builder().status(SubscriptionStatus.PENDING).createdAt(Instant.now());
    }

    private Subscription apply(Subscription s, SubscriptionRequest request) {
        if (s.getPlan() != request.plan()) {
            applyGrid(s, request.plan());
        }
        s.setFounder(request.founder());
        s.setStartedAt(request.startedAt());
        s.setBillingEmail(trimToNull(request.billingEmail()));
        s.setBillingCounty(trimToNull(request.billingCounty()));
        s.setBillingCity(trimToNull(request.billingCity()));
        s.setBillingAddress(trimToNull(request.billingAddress()));
        return subscriptionRepository.save(s);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void applyGrid(Subscription s, SubscriptionPlan plan) {
        s.setPlan(plan);
        s.setMonthlyPrice(plan.monthlyPrice());
        s.setImplementationFee(plan.implementationFee());
        boolean consultancy = plan.forConsultancy();
        s.setExtraWorkPointPrice(consultancy ? null : SubscriptionPlan.EXTRA_WORK_POINT_PRICE);
        s.setCompanyPriceTier1(consultancy ? SubscriptionPlan.COMPANY_PRICE_TIER1 : null);
        s.setCompanyPriceTier2(consultancy ? SubscriptionPlan.COMPANY_PRICE_TIER2 : null);
        s.setCompanyPriceTier3(consultancy ? SubscriptionPlan.COMPANY_PRICE_TIER3 : null);
        s.setPackagingCompanyPrice(consultancy ? SubscriptionPlan.PACKAGING_COMPANY_PRICE : null);
    }

    private SubscriptionResponse toResponse(Subscription s) {
        List<SubscriptionInvoiceResponse> invoices = s.getId() == null ? List.of()
                : invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()).stream()
                .map(i -> new SubscriptionInvoiceResponse(i.getId(), i.getPeriodStart(), i.getPeriodEnd(),
                        i.getTotal(), i.getStatus(), i.getDueDate(), i.getFgoSerie(), i.getFgoNumar(),
                        i.getFgoLink(), i.getFgoLinkPlata(), i.getAmountPaid(), i.getLastError(), i.getPaidAt()))
                .toList();
        return new SubscriptionResponse(s.getId(), s.getPlan(), s.getStatus(),
                s.getMonthlyPrice(), s.getImplementationFee(), s.getExtraWorkPointPrice(),
                s.getCompanyPriceTier1(), s.getCompanyPriceTier2(), s.getCompanyPriceTier3(),
                s.getPackagingCompanyPrice(), s.isFounder(), s.getStartedAt(),
                invoiceFor(s, 0), invoiceFor(s, 1),
                s.getBillingEmail(), s.getBillingCounty(), s.getBillingCity(), s.getBillingAddress(),
                invoices);
    }

    private Company requireCompany(UUID id) {
        return companyRepository.findById(id).orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    private Consultancy requireConsultancy(UUID id) {
        return consultancyRepository.findById(id).orElseThrow(() -> new NotFoundException(CONSULTANCY_NOT_FOUND));
    }
}
