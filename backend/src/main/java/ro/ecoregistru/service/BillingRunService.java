package ro.ecoregistru.service;

import ro.ecoregistru.util.BillingAddress;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.BillingRun;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.ServiceUnavailableException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.BillingRunRepository;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.service.notification.BillingReminder;
import ro.ecoregistru.service.notification.NotificationService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Objects;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
     * The issued and paid invoices are named too, since the Facturare screen lists the run row by row.
     */
    public record Result(boolean configured, int reserved, int issued, int failed, int paid,
                         List<Failure> failures, List<NotStarted> notStarted,
                         List<Done> issuedInvoices, List<Done> paidInvoices) {}

    /** Who pays, as the Clients screen opens it: {@code company} or {@code consultancy}. */
    public record Owner(String kind, UUID id) {}

    /** What trying to issue one invoice came to. */
    sealed interface IssueOutcome permits Done, Failure {}

    public record Failure(UUID invoiceId, Owner owner, String client, String reason) implements IssueOutcome {}

    public record NotStarted(Owner owner, String client, LocalDate startsOn) {}

    public record Done(UUID invoiceId, String client, String number, BigDecimal total) implements IssueOutcome {}

    SubscriptionRepository subscriptionRepository;
    SubscriptionInvoiceRepository invoiceRepository;
    BillingRunRepository billingRunRepository;
    SubscriptionService subscriptionService;
    NotificationService notificationService;
    FgoClient fgo;
    CardPaymentService cardPayments;
    TransactionTemplate tx;
    ObjectMapper objectMapper;

    /** The 06:30 run. */
    public Result run(LocalDate today) {
        return run(today, BillingRun.Kind.SCHEDULED, null);
    }

    /** Saved once it ends (V63), except without FGO keys, when nothing was run. */
    public Result run(LocalDate today, BillingRun.Kind kind, UUID triggeredBy) {
        if (!fgo.isConfigured()) {
            log.warn("Facturarea abonamentelor e oprită: lipsesc FGO_COD_UNIC, FGO_PRIVATE_KEY sau FGO_SERIE.");
            return new Result(false, 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of());
        }
        Instant startedAt = Instant.now();
        int reserved = tx.execute(status -> reserveDue(today));

        List<Done> issuedInvoices = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        for (UUID id : invoiceRepository.findIdsByStatus(InvoiceStatus.DRAFT)) {
            switch (issue(id, today)) {
                case Done done -> issuedInvoices.add(done);
                case Failure failure -> failures.add(failure);
            }
        }
        int issued = issuedInvoices.size();
        int failed = failures.size();

        for (UUID id : invoiceRepository.findIdsToEmail()) {
            mail(id);
        }

        cardPayments.debitSavedCards(today);
        cardPayments.recordCardPaymentsInFgo();

        List<Done> paidInvoices = new ArrayList<>();
        for (UUID id : invoiceRepository.findIdsByStatus(InvoiceStatus.ISSUED)) {
            readPayment(id).ifPresent(paidInvoices::add);
        }
        int paid = paidInvoices.size();

        tx.executeWithoutResult(status -> updateStatuses(today));
        for (UUID id : invoiceRepository.findIdsOverdue(InvoiceStatus.ISSUED, today)) {
            remind(id, today);
        }
        List<NotStarted> notStarted = tx.execute(status -> subscriptionRepository
                .findAllByStatusNotAndStartedAtAfter(SubscriptionStatus.CANCELLED, today).stream()
                .map(s -> new NotStarted(owner(s), clientName(s), s.getStartedAt()))
                .toList());
        Result result = new Result(true, reserved, issued, failed, paid, failures, notStarted,
                issuedInvoices, paidInvoices);
        if (reserved + issued + failed + paid > 0) {
            log.info("Facturarea abonamentelor, {}: {}", today, result);
        }
        save(result, startedAt, kind, triggeredBy);
        return result;
    }

    /** The last run, as it was saved; empty before the first one. */
    public Optional<LastRun> lastRun() {
        return billingRunRepository.findFirstByOrderByStartedAtDesc().map(r -> {
            Result result;
            try {
                result = objectMapper.readValue(r.getResultJson(), Result.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
            // F-B2: the screen no longer holds every invoice, so the server says which refusals are still open.
            List<UUID> ids = result.failures().stream().map(Failure::invoiceId).filter(Objects::nonNull).toList();
            Set<UUID> stillFailed = invoiceRepository.findAllById(ids).stream()
                    .filter(i -> i.getStatus() == InvoiceStatus.DRAFT && i.getLastError() != null)
                    .map(SubscriptionInvoice::getId)
                    .collect(Collectors.toSet());
            return new LastRun(r.getStartedAt(), r.getFinishedAt(), r.getKind(), result, stillFailed);
        });
    }

    /** {@code stillFailed}: the refused invoices of this run not yet corrected or stopped. */
    public record LastRun(Instant startedAt, Instant finishedAt, BillingRun.Kind kind, Result result, Set<UUID> stillFailed) {}

    /** A run that could not be saved still ran: the invoices are in FGO and in their own rows. */
    private void save(Result result, Instant startedAt, BillingRun.Kind kind, UUID triggeredBy) {
        try {
            billingRunRepository.save(BillingRun.builder()
                    .startedAt(startedAt)
                    .finishedAt(Instant.now())
                    .kind(kind)
                    .triggeredBy(triggeredBy)
                    .resultJson(objectMapper.writeValueAsString(result))
                    .build());
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("Rezultatul facturării n-a putut fi salvat: {}", e.getMessage());
        }
    }

    /**
     * „Verifică plata acum” on one issued invoice, instead of the whole run. The subscription's status is read
     * again at once: a paid invoice lifts PAST_DUE without waiting for 06:30.
     */
    public void checkPayment(UUID invoiceId, LocalDate today) {
        checkPayment(invoiceId, today, false);
    }

    private void checkPayment(UUID invoiceId, LocalDate today, boolean client) {
        if (!fgo.isConfigured()) {
            throw new ServiceUnavailableException(ErrorMessageEnum.FGO_NOT_CONFIGURED);
        }
        SubscriptionInvoice snapshot = tx.execute(status -> invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(ErrorMessageEnum.INVOICE_NOT_FOUND)));
        if (snapshot.getStatus() != InvoiceStatus.ISSUED) {
            throw new UnprocessableEntityException(ErrorMessageEnum.INVOICE_NOT_ISSUED);
        }
        FgoClient.Status status;
        try {
            status = client
                    ? fgo.statusForClient(snapshot.getFgoSerie(), snapshot.getFgoNumar())
                    : fgo.status(snapshot.getFgoSerie(), snapshot.getFgoNumar());
        } catch (RuntimeException e) {
            log.warn("Plata facturii {} n-a putut fi citită din FGO: {}", invoiceId, e.getMessage());
            throw new ServiceUnavailableException(ErrorMessageEnum.FGO_UNAVAILABLE);
        }
        tx.executeWithoutResult(t -> {
            SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
            recordPayment(invoice, status);
            subscriptionService.refreshStatus(invoice.getSubscription(), today);
        });
    }

    /** F-E — how long a client's „Am plătit — verifică acum” rests, so a click held down does not flood FGO. */
    static final Duration CLIENT_CHECK_PAUSE = Duration.ofMinutes(2);

    /**
     * Când a întrebat clientul ultima oară FGO, pe factură — reușit sau nu. {@code paymentCheckedAt} se scrie doar
     * după un răspuns, deci cu FGO căzut sau pe 409 pauza nu ținea și fiecare clic mai punea o cerere la coadă
     * (scanarea din 17.09.2026). În memorie, dinadins: e o frână, nu o evidență, și un dyno repornit o poate uita.
     */
    final Map<UUID, Instant> clientAttempts = new ConcurrentHashMap<>();

    /**
     * F-E — „Am plătit — verifică acum” on {@code /abonament}: the same check, on an invoice of the account's
     * own subscription only. Asked again within {@link #CLIENT_CHECK_PAUSE} of the last reading (the run's
     * or a click's), FGO is not asked: the page shows when it was read. Within the same pause after an attempt
     * that got no answer, FGO is not asked either, and the client is told to come back.
     */
    public void checkPaymentForAccount(AppUser user, UUID tenantId, UUID invoiceId, LocalDate today) {
        Instant lastChecked = tx.execute(status -> {
            Subscription payer = subscriptionService.payerFor(user, tenantId)
                    .orElseThrow(() -> new NotFoundException(ErrorMessageEnum.SUBSCRIPTION_NOT_FOUND));
            SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId)
                    .filter(i -> i.getSubscription().getId().equals(payer.getId()))
                    // A DRAFT is ours: to the client it does not exist.
                    .filter(i -> i.getStatus() != InvoiceStatus.DRAFT)
                    .orElseThrow(() -> new NotFoundException(ErrorMessageEnum.INVOICE_NOT_FOUND));
            return invoice.getPaymentCheckedAt();
        });
        Instant now = Instant.now();
        Instant cutoff = now.minus(CLIENT_CHECK_PAUSE);
        if (lastChecked != null && lastChecked.isAfter(cutoff)) {
            return;
        }
        clientAttempts.values().removeIf(at -> !at.isAfter(cutoff));
        boolean[] mine = {false};
        clientAttempts.compute(invoiceId, (id, previous) -> {
            if (previous != null && previous.isAfter(cutoff)) {
                return previous;
            }
            mine[0] = true;
            return now;
        });
        if (!mine[0]) {
            throw new ServiceUnavailableException(ErrorMessageEnum.FGO_RECENTLY_ASKED);
        }
        checkPayment(invoiceId, today, true);
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

    /** A {@link Done} once issued; otherwise the {@link Failure}: who, and why, the same reason kept on the row. */
    private IssueOutcome issue(UUID invoiceId, LocalDate today) {
        try {
            Draft draft = tx.execute(status -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                return new Draft(buyer(invoice.getSubscription()), fromJson(invoice.getLinesJson()),
                        "Abonament WasteHouse, perioada " + invoice.getPeriodStart().format(RO_DATE)
                                // A plain hyphen: FGO printed the en dash as „&ndash;” on WH 1 (17.09.2026).
                                + " - " + invoice.getPeriodEnd().format(RO_DATE) + ".");
            });
            LocalDate due = today.plusDays(PAYMENT_TERM_DAYS);
            FgoClient.Issued issued = fgo.emit(invoiceId.toString(), draft.buyer(), draft.lines(), today, due,
                    draft.explanation());
            return tx.execute(status -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                invoice.setStatus(InvoiceStatus.ISSUED);
                invoice.setFgoSerie(issued.serie());
                invoice.setFgoNumar(issued.numar());
                invoice.setFgoLink(issued.link());
                invoice.setFgoLinkPlata(issued.linkPlata());
                invoice.setDueDate(due);
                invoice.setIssuedAt(Instant.now());
                invoice.setLastError(null);
                return new Done(invoiceId, draft.buyer().name(), issued.serie() + " " + issued.numar(),
                        invoice.getTotal());
            });
        } catch (RuntimeException e) {
            log.error("Factura {} n-a putut fi emisă în FGO: {}", invoiceId, e.getMessage());
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return tx.execute(status -> invoiceRepository.findById(invoiceId)
                    .map(invoice -> {
                        String reason = reasonFor(message, invoice.getSubscription());
                        invoice.setLastError(reason);
                        Subscription s = invoice.getSubscription();
                        return new Failure(invoiceId, owner(s), clientName(s), reason);
                    })
                    .orElse(new Failure(invoiceId, null, "?", message)));
        }
    }

    /**
     * FGO's refusals, in words the platform can act on. Only the ones seen on the test account are translated
     * (17.09.2026); anything else is kept as FGO said it, which is still better than a guess.
     */
    static String reasonFor(String message, Subscription s) {
        String reason = message;
        if (message.contains("CodUnic")) {
            String cui = s.getCompany() != null ? s.getCompany().getCui() : s.getConsultancy().getCui();
            reason = "CUI-ul „" + cui + "” nu e valid: FGO nu-l acceptă. Corectează CUI-ul în fișa firmei.";
        }
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
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

    /** The invoice, once FGO says it is paid. */
    private Optional<Done> readPayment(UUID invoiceId) {
        try {
            SubscriptionInvoice snapshot = invoiceRepository.findById(invoiceId).orElseThrow();
            FgoClient.Status status = fgo.status(snapshot.getFgoSerie(), snapshot.getFgoNumar());
            return Optional.ofNullable(tx.execute(t -> {
                SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
                return recordPayment(invoice, status)
                        ? new Done(invoiceId, clientName(invoice.getSubscription()),
                                invoice.getFgoSerie() + " " + invoice.getFgoNumar(), invoice.getTotal())
                        : null;
            }));
        } catch (RuntimeException e) {
            log.warn("Plata facturii {} n-a putut fi citită din FGO: {}", invoiceId, e.getMessage());
            return Optional.empty();
        }
    }

    /** True when this answer is what made it paid. Needs a transaction. */
    private static boolean recordPayment(SubscriptionInvoice invoice, FgoClient.Status status) {
        invoice.setAmountPaid(status.paid());
        invoice.setPaymentCheckedAt(Instant.now());
        if (!status.isPaid()) {
            return false;
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        return true;
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
        // ANAF's address already holds the county and the locality; FGO prints those on rubrics of their own.
        return new FgoClient.Buyer(clientName(s), cui, recipient(s), s.getBillingCounty(),
                BillingAddress.city(s.getBillingCity()),
                BillingAddress.street(s.getBillingAddress(), s.getBillingCounty(), s.getBillingCity()));
    }

    static Owner owner(Subscription s) {
        return s.getCompany() != null ? new Owner("company", s.getCompany().getId())
                : new Owner("consultancy", s.getConsultancy().getId());
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
