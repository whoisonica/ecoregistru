package ro.ecoregistru.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.repository.SubscriptionRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * F2 of plata-abonamente.md — the daily invoicing run, by bank transfer.
 *
 * <ol>
 *   <li><b>Reserve.</b> Every subscription whose current period has no invoice gets a DRAFT row,
 *       with the lines counted now. Run twice, the second run finds the row and reserves nothing.</li>
 *   <li><b>Issue.</b> Every DRAFT goes to FGO with its own id as {@code IdExtern}; a failure leaves the
 *       row DRAFT with the reason, and the next run retries the same id.</li>
 *   <li><b>Read payments.</b> Every ISSUED invoice is asked for its paid amount. FGO matches the
 *       transfer to the invoice from the bank statement; we only read the result.</li>
 *   <li><b>Status.</b> An unpaid invoice past its due date makes the subscription PAST_DUE; a paid one
 *       makes it ACTIVE. Nothing is restricted here: read-only is F4.</li>
 * </ol>
 *
 * <p>Each step commits on its own, and FGO is never called inside a transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingRunService {

    public static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");
    /** Decision 5 of plata-abonamente.md. */
    static final int PAYMENT_TERM_DAYS = 10;
    private static final DateTimeFormatter RO_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final TypeReference<List<BillingCalculator.Line>> LINES = new TypeReference<>() {};

    public record Result(boolean configured, int reserved, int issued, int failed, int paid) {}

    SubscriptionRepository subscriptionRepository;
    SubscriptionInvoiceRepository invoiceRepository;
    SubscriptionService subscriptionService;
    FgoClient fgo;
    TransactionTemplate tx;
    ObjectMapper objectMapper;

    public Result run(LocalDate today) {
        if (!fgo.isConfigured()) {
            log.warn("Facturarea abonamentelor e oprită: lipsesc FGO_COD_UNIC, FGO_PRIVATE_KEY sau FGO_SERIE.");
            return new Result(false, 0, 0, 0, 0);
        }
        int reserved = tx.execute(status -> reserveDue(today));

        int issued = 0;
        int failed = 0;
        for (UUID id : invoiceRepository.findIdsByStatus(InvoiceStatus.DRAFT)) {
            if (issue(id, today)) {
                issued++;
            } else {
                failed++;
            }
        }

        int paid = 0;
        for (UUID id : invoiceRepository.findIdsByStatus(InvoiceStatus.ISSUED)) {
            if (readPayment(id)) {
                paid++;
            }
        }

        tx.executeWithoutResult(status -> updateStatuses(today));
        Result result = new Result(true, reserved, issued, failed, paid);
        if (reserved + issued + failed + paid > 0) {
            log.info("Facturarea abonamentelor, {}: {}", today, result);
        }
        return result;
    }

    private int reserveDue(LocalDate today) {
        int reserved = 0;
        for (Subscription s : subscriptionRepository
                .findAllByStatusNotAndStartedAtLessThanEqual(SubscriptionStatus.CANCELLED, today)) {
            int period = BillingCalculator.periodOn(s.getStartedAt(), today);
            if (invoiceRepository.existsBySubscription_IdAndPeriodStart(s.getId(), BillingCalculator.periodStart(s, period))) {
                continue;
            }
            BillingCalculator.Invoice invoice = subscriptionService.invoiceFor(s, period);
            invoiceRepository.save(SubscriptionInvoice.builder()
                    .subscription(s)
                    .periodStart(invoice.from())
                    .periodEnd(invoice.to())
                    .total(invoice.total())
                    .linesJson(toJson(invoice.lines()))
                    .status(InvoiceStatus.DRAFT)
                    .createdAt(Instant.now())
                    .build());
            reserved++;
        }
        return reserved;
    }

    private record Draft(FgoClient.Buyer buyer, List<BillingCalculator.Line> lines, String explanation) {}

    private boolean issue(UUID invoiceId, LocalDate today) {
        try {
            Draft draft = tx.execute(status -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                return new Draft(buyer(invoice.getSubscription()), fromJson(invoice.getLinesJson()),
                        "Abonament WasteHouse, perioada " + invoice.getPeriodStart().format(RO_DATE)
                                + " – " + invoice.getPeriodEnd().format(RO_DATE) + ".");
            });
            LocalDate due = today.plusDays(PAYMENT_TERM_DAYS);
            FgoClient.Issued issued = fgo.emit(invoiceId.toString(), draft.buyer(), draft.lines(), today, due,
                    draft.explanation());
            tx.executeWithoutResult(status -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                invoice.setStatus(InvoiceStatus.ISSUED);
                invoice.setFgoSerie(issued.serie());
                invoice.setFgoNumar(issued.numar());
                invoice.setFgoLink(issued.link());
                invoice.setFgoLinkPlata(issued.linkPlata());
                invoice.setDueDate(due);
                invoice.setIssuedAt(Instant.now());
                invoice.setLastError(null);
            });
            return true;
        } catch (RuntimeException e) {
            log.error("Factura {} n-a putut fi emisă în FGO: {}", invoiceId, e.getMessage());
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            tx.executeWithoutResult(status -> invoiceRepository.findById(invoiceId)
                    .ifPresent(invoice -> invoice.setLastError(reason.length() > 1000 ? reason.substring(0, 1000) : reason)));
            return false;
        }
    }

    private boolean readPayment(UUID invoiceId) {
        try {
            SubscriptionInvoice snapshot = invoiceRepository.findById(invoiceId).orElseThrow();
            FgoClient.Status status = fgo.status(snapshot.getFgoSerie(), snapshot.getFgoNumar());
            return Boolean.TRUE.equals(tx.execute(t -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                invoice.setAmountPaid(status.paid());
                if (!status.isPaid()) {
                    return false;
                }
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidAt(Instant.now());
                return true;
            }));
        } catch (RuntimeException e) {
            log.warn("Plata facturii {} n-a putut fi citită din FGO: {}", invoiceId, e.getMessage());
            return false;
        }
    }

    private void updateStatuses(LocalDate today) {
        for (Subscription s : subscriptionRepository.findAllByStatusIn(
                List.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE))) {
            List<SubscriptionInvoice> invoices = invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId());
            if (invoices.isEmpty()) {
                continue;
            }
            boolean overdue = invoices.stream().anyMatch(i ->
                    i.getStatus() == InvoiceStatus.ISSUED && i.getDueDate().isBefore(today));
            boolean anyPaid = invoices.stream().anyMatch(i -> i.getStatus() == InvoiceStatus.PAID);
            s.setStatus(overdue ? SubscriptionStatus.PAST_DUE
                    : anyPaid ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING);
        }
    }

    /** Who pays: the direct company or the consultancy, with the address FGO needs for a Romanian buyer. */
    private static FgoClient.Buyer buyer(Subscription s) {
        List<String> missing = new ArrayList<>();
        if (isBlank(s.getBillingCounty())) {
            missing.add("județul");
        }
        if (isBlank(s.getBillingCity())) {
            missing.add("localitatea");
        }
        if (isBlank(s.getBillingAddress())) {
            missing.add("adresa");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Lipsesc datele de facturare: " + String.join(", ", missing)
                    + ". Completează-le în dialogul Abonament.");
        }
        Company company = s.getCompany();
        String name = company != null ? company.getName() : s.getConsultancy().getName();
        String cui = company != null ? company.getCui() : s.getConsultancy().getCui();
        String email = !isBlank(s.getBillingEmail()) ? s.getBillingEmail()
                : company != null ? company.getContactEmail() : null;
        return new FgoClient.Buyer(name, cui, email, s.getBillingCounty(), s.getBillingCity(), s.getBillingAddress());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toJson(List<BillingCalculator.Line> lines) {
        try {
            return objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<BillingCalculator.Line> fromJson(String json) {
        try {
            return objectMapper.readValue(json, LINES);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
