package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import ro.ecoregistru.audit.AuditChangeCodec;
import ro.ecoregistru.audit.PendingAudit;
import ro.ecoregistru.controller.request.BillingDetailsRequest;
import ro.ecoregistru.controller.request.SubscriptionRequest;
import ro.ecoregistru.entity.AuditLog;
import ro.ecoregistru.enums.AuditAction;
import ro.ecoregistru.repository.AuditLogRepository;
import ro.ecoregistru.service.notification.NotificationService;
import java.util.ArrayList;
import java.util.Objects;
import ro.ecoregistru.controller.response.BillingAccessResponse;
import ro.ecoregistru.controller.response.BillingResponse;
import ro.ecoregistru.controller.response.CardPaymentResponse;
import ro.ecoregistru.controller.response.BillingInvoiceRow;
import ro.ecoregistru.controller.response.InvoiceMoneyResponse;
import ro.ecoregistru.controller.response.InvoicePageResponse;
import ro.ecoregistru.enums.InvoiceFilter;
import ro.ecoregistru.repository.InvoiceSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;
import ro.ecoregistru.controller.response.SubscriptionInvoiceResponse;
import ro.ecoregistru.controller.response.SubscriptionResponse;
import ro.ecoregistru.controller.response.SubscriptionPreviewResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.CardPayment;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.CardPaymentStatus;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.CardPaymentRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.CONSULTANCY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.INVOICE_NOT_DISCARDABLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.INVOICE_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.SUBSCRIPTION_ALREADY_CANCELLED;
import static ro.ecoregistru.exception.ErrorMessageEnum.SUBSCRIPTION_NOT_FOUND;
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
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionService {

    SubscriptionRepository subscriptionRepository;
    SubscriptionInvoiceRepository invoiceRepository;
    CompanyRepository companyRepository;
    ConsultancyRepository consultancyRepository;
    WorkPointRepository workPointRepository;
    CardPaymentRepository cardPaymentRepository;
    NetopiaClient netopia;
    AuditLogRepository auditLogRepository;
    NotificationService notificationService;

    /** §9.6: off until the contract says it. */
    @NonFinal
    @Value("${app.billing.read-only-enabled:false}")
    boolean readOnlyEnabled;

    /** F-E — the account a transfer goes to: the one in the contract and on the FGO invoice. */
    @NonFinal
    @Value("${app.billing.payee.name:ONSIA S.R.L.}")
    String payeeName;

    @NonFinal
    @Value("${app.billing.payee.cui:51779887}")
    String payeeCui;

    @NonFinal
    @Value("${app.billing.payee.iban:RO32RZBR0000060027995375}")
    String payeeIban;

    @NonFinal
    @Value("${app.billing.payee.bank:Raiffeisen Bank România}")
    String payeeBank;

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

    /**
     * The subscription an account pays, for {@code /abonament}: a consultant sees the cabinet's, an
     * admin their own company's, the platform the company it has chosen. A company of a cabinet has
     * none of its own, since the cabinet pays for it.
     */
    @Transactional(readOnly = true)
    public Optional<BillingResponse> forAccount(AppUser user, UUID tenantId, LocalDate today) {
        return payerFor(user, tenantId).map(s -> toBillingResponse(s, today));
    }

    /** The subscription the account pays itself; empty for a company of a cabinet. Needs a transaction. */
    public Optional<Subscription> payerFor(AppUser user, UUID tenantId) {
        return switch (user.getRole()) {
            case CONSULTANT -> Optional.ofNullable(user.getConsultancy())
                    .flatMap(c -> subscriptionRepository.findByConsultancy_Id(c.getId()));
            case PLATFORM_ADMIN -> Optional.ofNullable(tenantId).flatMap(subscriptionRepository::findByCompany_Id);
            default -> Optional.ofNullable(user.getCompany())
                    .flatMap(c -> subscriptionRepository.findByCompany_Id(c.getId()));
        };
    }

    /**
     * F4 — for every role, on every screen: the status of whoever pays for the account (its own or its
     * cabinet's subscription) and whether writes are refused now. The platform is never restricted.
     */
    @Transactional(readOnly = true)
    public BillingAccessResponse access(AppUser user, UUID tenantId, LocalDate today) {
        Optional<Subscription> payer = switch (user.getRole()) {
            case CONSULTANT -> Optional.ofNullable(user.getConsultancy())
                    .flatMap(c -> subscriptionRepository.findByConsultancy_Id(c.getId()));
            default -> {
                UUID companyId = tenantId != null ? tenantId : user.getCompany() != null ? user.getCompany().getId() : null;
                yield companyId == null ? Optional.<Subscription>empty() : companyRepository.findById(companyId)
                        .flatMap(c -> c.getConsultancy() != null
                                ? subscriptionRepository.findByConsultancy_Id(c.getConsultancy().getId())
                                : subscriptionRepository.findByCompany_Id(c.getId()));
            }
        };
        return payer.map(s -> new BillingAccessResponse(s.getStatus(),
                        readOnlyEnabled && user.getRole() != Role.PLATFORM_ADMIN
                                && SubscriptionStatusRules.restricts(s.getStatus()),
                        readOnlyOn(s)))
                .orElse(new BillingAccessResponse(null, false, null));
    }

    /** F3 — the client's choice. The invoice comes first either way; the choice decides the saved card and the text. */
    @Transactional
    public void choosePaymentMethod(AppUser user, UUID tenantId, SubscriptionPaymentMethod method) {
        Subscription s = payerFor(user, tenantId).orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND));
        s.setPaymentMethod(method);
        if (method == SubscriptionPaymentMethod.TRANSFER) {
            // Choosing transfer is saying „do not debit my card": the token goes, not only the flag.
            s.setCardToken(null);
            s.setCardPanMasked(null);
            s.setCardExpiry(null);
        }
    }

    /**
     * F-E — the client keeps its billing data up to date (contract art. 7.5). Invoices already issued keep
     * what they were issued with; the next one takes the new data.
     *
     * <p>A company's change goes in its journal, with the fields before and after. A cabinet has no journal
     * (it is a company's, {@code company_id NOT NULL}), so there only the mail says it. When the email
     * changes, the old address is told: the terms take a stop request from the billing address, so a
     * change nobody asked for must not go unnoticed. A mail that fails does not undo the change.
     */
    @Transactional
    public BillingResponse updateBillingDetails(AppUser user, UUID tenantId, BillingDetailsRequest request,
                                                LocalDate today) {
        Subscription s = payerFor(user, tenantId).orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND));
        requireFgoCounty(request.billingCounty());
        String oldRecipient = BillingRunService.recipient(s);
        List<PendingAudit.FieldChange> changes = new ArrayList<>();
        s.setBillingEmail(changed(changes, "billingEmail", s.getBillingEmail(), request.billingEmail()));
        s.setBillingCounty(changed(changes, "billingCounty", s.getBillingCounty(), request.billingCounty()));
        s.setBillingCity(changed(changes, "billingCity", s.getBillingCity(), request.billingCity()));
        s.setBillingAddress(changed(changes, "billingAddress", s.getBillingAddress(), request.billingAddress()));
        if (changes.isEmpty()) {
            return toBillingResponse(s, today);
        }
        if (s.getCompany() != null) {
            auditLogRepository.save(AuditLog.builder()
                    .company(s.getCompany())
                    .entityType("Subscription")
                    .entityId(s.getId())
                    .action(AuditAction.UPDATE)
                    .label(s.getCompany().getName())
                    .changes(AuditChangeCodec.write(changes))
                    .actorId(user.getId())
                    .actorEmail(user.getEmail())
                    .actorRole(user.getRole())
                    .occurredAt(Instant.now())
                    .build());
        }
        String newRecipient = BillingRunService.recipient(s);
        if (oldRecipient != null && !oldRecipient.equalsIgnoreCase(newRecipient)) {
            try {
                notificationService.sendBillingEmailChanged(BillingRunService.clientName(s), oldRecipient,
                        newRecipient, user.getEmail());
            } catch (RuntimeException e) {
                log.warn("Mailul despre adresa de facturare schimbată n-a plecat către {}: {}", oldRecipient,
                        e.getMessage());
            }
        }
        return toBillingResponse(s, today);
    }

    private static String changed(List<PendingAudit.FieldChange> changes, String field, String from, String to) {
        String value = trimToNull(to);
        if (!Objects.equals(from, value)) {
            changes.add(new PendingAudit.FieldChange(field, from, value));
        }
        return value;
    }

    /** F3 — one of the account's own card payments, for the page Netopia sends the client back to. */
    @Transactional(readOnly = true)
    public CardPaymentResponse cardPayment(AppUser user, UUID tenantId, UUID paymentId) {
        Subscription s = payerFor(user, tenantId).orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND));
        CardPayment p = cardPaymentRepository.findById(paymentId)
                .filter(c -> c.getInvoice().getSubscription().getId().equals(s.getId()))
                .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
        return new CardPaymentResponse(p.getId(), p.getStatus(), p.getError(), p.getInvoice().getId(),
                p.getInvoice().getStatus());
    }

    /**
     * §9.3 — stopped with a month's notice: the period that holds the day a month from now is the last one
     * billed, then CANCELLED and read-only. Stopping twice keeps the first date.
     */
    @Transactional
    public SubscriptionResponse cancelForCompany(UUID companyId, LocalDate today) {
        requireCompany(companyId);
        return cancel(subscriptionRepository.findByCompany_Id(companyId)
                .orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND)), today);
    }

    @Transactional
    public SubscriptionResponse cancelForConsultancy(UUID consultancyId, LocalDate today) {
        requireConsultancy(consultancyId);
        return cancel(subscriptionRepository.findByConsultancy_Id(consultancyId)
                .orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND)), today);
    }

    /** Takes a stop back, while the subscription has not ended yet. */
    @Transactional
    public SubscriptionResponse resumeForCompany(UUID companyId) {
        requireCompany(companyId);
        return resume(subscriptionRepository.findByCompany_Id(companyId)
                .orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND)));
    }

    @Transactional
    public SubscriptionResponse resumeForConsultancy(UUID consultancyId) {
        requireConsultancy(consultancyId);
        return resume(subscriptionRepository.findByConsultancy_Id(consultancyId)
                .orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND)));
    }

    private SubscriptionResponse cancel(Subscription s, LocalDate today) {
        if (s.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new UnprocessableEntityException(SUBSCRIPTION_ALREADY_CANCELLED);
        }
        if (s.getEndsOn() == null) {
            s.setEndsOn(endsOnAfterNotice(s, today));
        }
        return toResponse(s);
    }

    private SubscriptionResponse resume(Subscription s) {
        if (s.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new UnprocessableEntityException(SUBSCRIPTION_ALREADY_CANCELLED);
        }
        s.setEndsOn(null);
        return toResponse(s);
    }

    /** The last day of the period holding the day a month from now (or the start, if that is later). */
    static LocalDate endsOnAfterNotice(Subscription s, LocalDate today) {
        LocalDate noticeEnds = today.plusMonths(1);
        LocalDate day = noticeEnds.isBefore(s.getStartedAt()) ? s.getStartedAt() : noticeEnds;
        int period = BillingCalculator.periodOn(s.getStartedAt(), day);
        return BillingCalculator.periodStart(s, period + 1).minusDays(1);
    }

    /** Reads the status again from the invoices ({@link SubscriptionStatusRules}). Needs a transaction. */
    public void refreshStatus(Subscription s, LocalDate today) {
        s.setStatus(SubscriptionStatusRules.statusOn(today,
                invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()),
                s.getEndsOn(), readOnlyEnabled, s.getStatus()));
    }

    public boolean readOnlyEnabled() {
        return readOnlyEnabled;
    }

    /** Oldest unpaid due date + 15, when read-only is on and something is unpaid. */
    private LocalDate readOnlyOn(Subscription s) {
        if (!readOnlyEnabled || s.getStatus() == SubscriptionStatus.CANCELLED) {
            return null;
        }
        return invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.ISSUED && i.getDueDate() != null)
                .map(SubscriptionInvoice::getDueDate)
                .min(Comparator.naturalOrder())
                .map(due -> due.plusDays(SubscriptionStatusRules.READ_ONLY_AFTER_DAYS))
                .orElse(null);
    }

    /** The latest card attempt's refusal, while it is the latest: an attempt that went through after it hides it. */
    private String lastCardError(UUID invoiceId) {
        return cardPaymentRepository.findAllByInvoice_IdOrderByCreatedAtDesc(invoiceId).stream().findFirst()
                .filter(p -> p.getStatus() == CardPaymentStatus.FAILED)
                .map(CardPayment::getError)
                .orElse(null);
    }

    /** The next invoice is the first one until the start date, then the one after the current period. */
    private BillingResponse toBillingResponse(Subscription s, LocalDate today) {
        int next = today.isBefore(s.getStartedAt()) ? 0 : BillingCalculator.periodOn(s.getStartedAt(), today) + 1;
        List<BillingResponse.IssuedInvoice> invoices = invoiceRepository
                .findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()).stream()
                .filter(i -> i.getStatus() != InvoiceStatus.DRAFT)
                .map(i -> new BillingResponse.IssuedInvoice(i.getId(), i.getPeriodStart(), i.getPeriodEnd(),
                        i.getTotal(), i.getStatus(), i.getDueDate(), i.getFgoSerie(), i.getFgoNumar(),
                        i.getFgoLink(), i.getFgoLinkPlata(), i.getPaidAt(), i.getPaidBy(), lastCardError(i.getId()),
                        i.getPaymentCheckedAt()))
                .toList();
        return new BillingResponse(BillingRunService.clientName(s), s.getPlan(), s.getStatus(), s.getStartedAt(),
                s.isFounder(), invoiceFor(s, next), BillingRunService.recipient(s),
                s.getBillingCounty(), s.getBillingCity(), s.getBillingAddress(), invoices,
                s.getPaymentMethod(), s.getCardPanMasked(), s.getCardExpiry(), netopia.isConfigured(),
                s.getEndsOn(), readOnlyOn(s),
                s.getCompany() != null ? s.getCompany().getCui() : s.getConsultancy().getCui(),
                new BillingResponse.Payee(payeeName, payeeCui, payeeIban, payeeBank));
    }

    /** F-A — the Facturare screen: every invoice, newest first. */
    /** F-B2 — one page of the Facturare table, newest first, and the count of every filter key for the same month and search. */
    @Transactional(readOnly = true)
    public InvoicePageResponse invoicePage(InvoiceFilter filter, YearMonth month, String query, int page, int size) {
        LocalDate today = LocalDate.now(BillingRunService.ZONE);
        Page<SubscriptionInvoice> result = invoiceRepository.findAll(
                InvoiceSpecifications.of(filter, month, query, today),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))));
        Map<InvoiceFilter, Long> counts = new EnumMap<>(InvoiceFilter.class);
        for (InvoiceFilter f : InvoiceFilter.values()) {
            counts.put(f, invoiceRepository.count(InvoiceSpecifications.of(f, month, query, today)));
        }
        List<BillingInvoiceRow> rows = result.getContent().stream().map(i -> {
            Subscription s = i.getSubscription();
            BillingRunService.Owner owner = BillingRunService.owner(s);
            return new BillingInvoiceRow(i.getId(), BillingRunService.clientName(s), owner.kind(), owner.id(),
                    s.getStatus(), i.getPeriodStart(), i.getPeriodEnd(), i.getTotal(), i.getStatus(), i.getDueDate(),
                    i.getFgoSerie(), i.getFgoNumar(), i.getFgoLink(), i.getAmountPaid(), i.getLastError(),
                    i.getIssuedAt(), i.getPaidAt(), i.getPaidBy(), i.getEmailedAt(), i.getOverdueMailedAt(),
                    i.getPaymentCheckedAt());
        }).toList();
        return new InvoicePageResponse(rows, result.getTotalElements(), result.getNumber(), result.getSize(), counts);
    }

    /** F-B — paid since the 1st of this month (Bucharest), and issued but not paid, whatever the month. */
    @Transactional(readOnly = true)
    public InvoiceMoneyResponse invoiceMoney() {
        Instant monthStart = LocalDate.now(BillingRunService.ZONE).withDayOfMonth(1)
                .atStartOfDay(BillingRunService.ZONE).toInstant();
        Object[] paid = invoiceRepository.sumAndCount(InvoiceStatus.PAID, monthStart).get(0);
        Object[] unpaid = invoiceRepository.sumAndCount(InvoiceStatus.ISSUED, monthStart).get(0);
        return new InvoiceMoneyResponse((BigDecimal) paid[0], (Long) paid[1], (BigDecimal) unpaid[0], (Long) unpaid[1]);
    }

    /**
     * F-A, „Oprește” on an invoice FGO refused: the reservation goes, and so does the subscription — a test client
     * failed every morning at 06:30 (Transilvania ABC, 17.09.2026). Only a refused one: FGO answered, so nothing
     * of it exists there. Without other invoices the subscription is deleted, the client back to not billed;
     * with some, it is stopped the day before this period, so the run reserves nothing after it.
     */
    @Transactional
    public void discardFailedInvoice(UUID invoiceId) {
        SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
        if (invoice.getStatus() != InvoiceStatus.DRAFT || invoice.getLastError() == null) {
            throw new UnprocessableEntityException(INVOICE_NOT_DISCARDABLE);
        }
        Subscription s = invoice.getSubscription();
        invoiceRepository.delete(invoice);
        invoiceRepository.flush();
        if (!invoiceRepository.existsBySubscription_Id(s.getId())) {
            subscriptionRepository.delete(s);
            return;
        }
        s.setEndsOn(invoice.getPeriodStart().minusDays(1));
        s.setStatus(SubscriptionStatus.CANCELLED);
    }

    /**
     * F-C — what a subscription would invoice, not saved: a new client has at most the one work point of its request,
     * which the price already covers. A company plan only; a cabinet is not made in the „Client nou” steps.
     */
    public SubscriptionPreviewResponse preview(SubscriptionRequest request) {
        if (request.plan().forConsultancy()) {
            throw new UnprocessableEntityException(SUBSCRIPTION_PLAN_MISMATCH);
        }
        Subscription s = pending().founder(request.founder()).startedAt(request.startedAt()).build();
        applyGrid(s, request.plan());
        return new SubscriptionPreviewResponse(BillingCalculator.invoice(s, 1, 0, 0, 0),
                BillingCalculator.invoice(s, 1, 0, 0, 1));
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
        if (trimToNull(request.billingCounty()) != null) {
            requireFgoCounty(request.billingCounty());
        }
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

    private static void requireFgoCounty(String county) {
        if (!ro.ecoregistru.util.FgoCounties.isValid(county)) {
            throw new UnprocessableEntityException(ro.ecoregistru.exception.ErrorMessageEnum.BILLING_COUNTY_INVALID);
        }
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
                        i.getFgoLink(), i.getFgoLinkPlata(), i.getAmountPaid(), i.getLastError(), i.getPaidAt(),
                        i.getPaidBy(), i.getFgoCollectedAt(), lastCardError(i.getId()), i.getPaymentCheckedAt()))
                .toList();
        return new SubscriptionResponse(s.getId(), s.getPlan(), s.getStatus(),
                s.getMonthlyPrice(), s.getImplementationFee(), s.getExtraWorkPointPrice(),
                s.getCompanyPriceTier1(), s.getCompanyPriceTier2(), s.getCompanyPriceTier3(),
                s.getPackagingCompanyPrice(), s.isFounder(), s.getStartedAt(),
                invoiceFor(s, 0), invoiceFor(s, 1),
                s.getBillingEmail(), s.getBillingCounty(), s.getBillingCity(), s.getBillingAddress(),
                invoices, s.getPaymentMethod(), s.getCardPanMasked(), s.getCardExpiry(), s.getEndsOn());
    }

    private Company requireCompany(UUID id) {
        return companyRepository.findById(id).orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    private Consultancy requireConsultancy(UUID id) {
        return consultancyRepository.findById(id).orElseThrow(() -> new NotFoundException(CONSULTANCY_NOT_FOUND));
    }
}
