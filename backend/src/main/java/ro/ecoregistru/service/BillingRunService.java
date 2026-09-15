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
import ro.ecoregistru.service.notification.BillingReminder;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
 *   <li><b>Mail.</b> Every issued invoice not yet mailed goes to the client from our address, with
 *       FGO's PDF link (§9.4). Marked only once sent, so a failed mail is sent again next run.</li>
 *   <li><b>Read payments.</b> Every ISSUED invoice is asked for its paid amount. FGO matches the
 *       transfer to the invoice from the bank statement; we only read the result.</li>
 *   <li><b>Cards (F3).</b> Saved cards are debited on their days, and card payments FGO does not have yet
 *       are recorded there ({@link CardPaymentService}).</li>
 *   <li><b>Status.</b> {@link SubscriptionStatusRules}: PAST_DUE after the due date, READ_ONLY 15 days
 *       later when switched on, ACTIVE once paid, CANCELLED after the last day of a stopped one.</li>
 *   <li><b>Reminders (F4).</b> Overdue, read-only in 7 days, read-only: each once per invoice.</li>
 * </ol>
 *
 * <p>Each step commits on its own, and neither FGO nor the mail server is called inside a transaction.
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

    /**
     * {@code failures} and {@code notStarted} are for whoever pressed the button: „1 căzute" was another
     * company's missing address, and a subscription starting next week was simply absent from the counts.
     */
    public record Result(boolean configured, int reserved, int issued, int failed, int paid,
                         List<Failure> failures, List<NotStarted> notStarted) {}

    public record Failure(String client, String reason) {}

    public record NotStarted(String client, LocalDate startsOn) {}

    SubscriptionRepository subscriptionRepository;
    SubscriptionInvoiceRepository invoiceRepository;
    SubscriptionService subscriptionService;
    NotificationService notificationService;
    FgoClient fgo;
    CardPaymentService cardPayments;
    TransactionTemplate tx;
    ObjectMapper objectMapper;

    public Result run(LocalDate today) {
        if (!fgo.isConfigured()) {
            log.warn("Facturarea abonamentelor e oprită: lipsesc FGO_COD_UNIC, FGO_PRIVATE_KEY sau FGO_SERIE.");
            return new Result(false, 0, 0, 0, 0, List.of(), List.of());
        }
        int reserved = tx.execute(status -> reserveDue(today));

        int issued = 0;
        List<Failure> failures = new ArrayList<>();
        for (UUID id : invoiceRepository.findIdsByStatus(InvoiceStatus.DRAFT)) {
            Failure failure = issue(id, today);
            if (failure == null) {
                issued++;
            } else {
                failures.add(failure);
            }
        }
        int failed = failures.size();

        for (UUID id : invoiceRepository.findIdsToEmail()) {
            mail(id);
        }

        cardPayments.debitSavedCards(today);
        cardPayments.recordCardPaymentsInFgo();

        int paid = 0;
        for (UUID id : invoiceRepository.findIdsByStatus(InvoiceStatus.ISSUED)) {
            if (readPayment(id)) {
                paid++;
            }
        }

        tx.executeWithoutResult(status -> updateStatuses(today));
        for (UUID id : invoiceRepository.findIdsOverdue(InvoiceStatus.ISSUED, today)) {
            remind(id, today);
        }
        List<NotStarted> notStarted = tx.execute(status -> subscriptionRepository
                .findAllByStatusNotAndStartedAtAfter(SubscriptionStatus.CANCELLED, today).stream()
                .map(s -> new NotStarted(clientName(s), s.getStartedAt()))
                .toList());
        Result result = new Result(true, reserved, issued, failed, paid, failures, notStarted);
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
            LocalDate periodStart = BillingCalculator.periodStart(s, period);
            // §9.3: a stopped subscription is billed up to its last day, never a period that starts after it.
            if ((s.getEndsOn() != null && periodStart.isAfter(s.getEndsOn()))
                    || invoiceRepository.existsBySubscription_IdAndPeriodStart(s.getId(), periodStart)) {
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

    /** Null once issued; otherwise who could not be invoiced and why, the same reason kept on the row. */
    private Failure issue(UUID invoiceId, LocalDate today) {
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
            return null;
        } catch (RuntimeException e) {
            log.error("Factura {} n-a putut fi emisă în FGO: {}", invoiceId, e.getMessage());
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            String reason = message.length() > 1000 ? message.substring(0, 1000) : message;
            String client = tx.execute(status -> invoiceRepository.findById(invoiceId)
                    .map(invoice -> {
                        invoice.setLastError(reason);
                        return clientName(invoice.getSubscription());
                    })
                    .orElse("?"));
            return new Failure(client, reason);
        }
    }

    private record Notice(SubscriptionInvoice invoice, String clientName, String to) {}

    /**
     * Without an address the invoice is left unmarked too: the email added later still gets it. The
     * detached invoice is only read for its own columns, never for the subscription.
     */
    private void mail(UUID invoiceId) {
        try {
            Notice notice = tx.execute(status -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                Subscription s = invoice.getSubscription();
                return new Notice(invoice, clientName(s), recipient(s));
            });
            if (notice.to() == null) {
                log.warn("Factura {} nu poate fi trimisă: abonamentul n-are email de facturare.", invoiceId);
                return;
            }
            notificationService.sendSubscriptionInvoice(notice.invoice(), notice.clientName(), notice.to());
            tx.executeWithoutResult(status -> invoiceRepository.findById(invoiceId)
                    .ifPresent(invoice -> invoice.setEmailedAt(Instant.now())));
        } catch (RuntimeException e) {
            log.error("Factura {} n-a putut fi trimisă pe email: {}", invoiceId, e.getMessage());
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
        for (Subscription s : subscriptionRepository.findAllByStatusIn(List.of(SubscriptionStatus.PENDING,
                SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.READ_ONLY))) {
            subscriptionService.refreshStatus(s, today);
        }
    }

    private record Reminder(SubscriptionInvoice invoice, String client, String to, BillingReminder kind) {}

    /**
     * §2.3, one mail per step and per invoice, marked once sent. Only the step that is due today goes out:
     * a first run on day 12 sends the warning, not the overdue notice as well. With read-only switched
     * off (§9.6) only the overdue notice exists, since the other two announce a restriction.
     */
    private void remind(UUID invoiceId, LocalDate today) {
        try {
            Reminder reminder = tx.execute(status -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                Subscription s = invoice.getSubscription();
                if (s.getStatus() == SubscriptionStatus.CANCELLED) {
                    return null;
                }
                long days = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                boolean readOnly = subscriptionService.readOnlyEnabled();
                BillingReminder kind = null;
                if (readOnly && days >= SubscriptionStatusRules.READ_ONLY_AFTER_DAYS) {
                    if (invoice.getReadOnlyMailedAt() == null && s.getStatus() == SubscriptionStatus.READ_ONLY) {
                        kind = BillingReminder.READ_ONLY;
                    }
                } else if (readOnly && days >= SubscriptionStatusRules.WARNING_AFTER_DAYS) {
                    if (invoice.getWarningMailedAt() == null) {
                        kind = BillingReminder.READ_ONLY_WARNING;
                    }
                } else if (days >= 1 && invoice.getOverdueMailedAt() == null) {
                    kind = BillingReminder.OVERDUE;
                }
                String to = recipient(s);
                return kind == null || to == null ? null : new Reminder(invoice, clientName(s), to, kind);
            });
            if (reminder == null) {
                return;
            }
            notificationService.sendBillingReminder(reminder.invoice(), reminder.client(), reminder.to(),
                    reminder.kind(), null);
            tx.executeWithoutResult(status -> invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                Instant now = Instant.now();
                switch (reminder.kind()) {
                    case OVERDUE -> invoice.setOverdueMailedAt(now);
                    case READ_ONLY_WARNING -> invoice.setWarningMailedAt(now);
                    case READ_ONLY -> invoice.setReadOnlyMailedAt(now);
                    case CARD_FAILED -> { }
                }
            }));
        } catch (RuntimeException e) {
            log.error("Mementoul pentru factura {} n-a plecat: {}", invoiceId, e.getMessage());
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
        String cui = company != null ? company.getCui() : s.getConsultancy().getCui();
        return new FgoClient.Buyer(clientName(s), cui, recipient(s), s.getBillingCounty(), s.getBillingCity(),
                s.getBillingAddress());
    }

    /** The direct company or the cabinet. Needs an open transaction: the owner is lazy. */
    static String clientName(Subscription s) {
        return s.getCompany() != null ? s.getCompany().getName() : s.getConsultancy().getName();
    }

    /** Where invoices go: the billing email, else the company's contact email. A cabinet has no other. */
    static String recipient(Subscription s) {
        if (!isBlank(s.getBillingEmail())) {
            return s.getBillingEmail();
        }
        Company company = s.getCompany();
        return company != null && !isBlank(company.getContactEmail()) ? company.getContactEmail() : null;
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
