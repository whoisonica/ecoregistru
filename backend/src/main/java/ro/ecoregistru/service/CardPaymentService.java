package ro.ecoregistru.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ro.ecoregistru.controller.NetopiaIpnController;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.CardPayment;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.PaymentNotification;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.CardPaymentKind;
import ro.ecoregistru.enums.CardPaymentStatus;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.CardPaymentRepository;
import ro.ecoregistru.repository.PaymentNotificationRepository;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.service.notification.BillingReminder;
import ro.ecoregistru.service.notification.NotificationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.CARD_PAYMENT_UNAVAILABLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.INVOICE_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.INVOICE_NOT_PAYABLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.SUBSCRIPTION_NOT_FOUND;

/**
 * F3 of plata-abonamente.md — paying an issued invoice by card, through Netopia.
 *
 * <p><b>The invoice comes first</b> (decision of 15.09.2026): the daily run issues it in FGO whatever the
 * payment method, and a card only ever pays an invoice that exists. No money comes in without an invoice,
 * and a refused card leaves an unpaid invoice, the same as a late transfer.
 *
 * <ul>
 *   <li><b>Checkout.</b> The client presses „Plătește cu cardul" on {@code /abonament}; Netopia's page takes
 *       the card and 3-D Secure. Works on the sandbox (15.09.2026).</li>
 *   <li><b>Notification.</b> Trusted only once {@link NetopiaIpnVerifier} passed. Stored unique on
 *       (ntpID, status), in the same transaction that settles the invoice, so a resent one settles nothing
 *       and a failed one is settled by the resend.</li>
 *   <li><b>Saved card.</b> When a notification carries a token, the next invoices are debited by the daily
 *       run on days {@link #DEBIT_DAYS} after issue, with a mail at every refusal. <b>Not proven</b>: the
 *       sandbox POS returned no token on 15.09.2026.</li>
 *   <li><b>FGO.</b> A card-paid invoice is PAID here at once; {@code factura/incasare} follows, and the run
 *       retries it while FGO is down.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CardPaymentService {

    /** §2.2: the debit on the day of issue, then again on days 3, 6 and 10. */
    static final int[] DEBIT_DAYS = {0, 3, 6, 10};

    SubscriptionService subscriptionService;
    SubscriptionInvoiceRepository invoiceRepository;
    CardPaymentRepository cardPaymentRepository;
    PaymentNotificationRepository notificationRepository;
    NetopiaClient netopia;
    FgoClient fgo;
    NotificationService notificationService;
    TransactionTemplate tx;

    @NonFinal
    @Value("${app.frontend-base-url}")
    String frontendBaseUrl;

    private record Order(UUID paymentId, String orderId, BigDecimal amount, String description,
                         NetopiaClient.Billing billing, String token) {}

    /** The saved card, as Netopia describes it. */
    record Card(String token, String panMasked, String expiry) {

        static Card of(JsonNode payment) {
            JsonNode binding = payment.path("binding");
            String token = text(binding, "token");
            if (token == null) {
                token = text(payment, "token");
            }
            int month = binding.path("expireMonth").asInt(0);
            int year = binding.path("expireYear").asInt(0);
            return new Card(token, text(payment.path("instrument"), "panMasked"),
                    month > 0 && year > 0 ? "%02d/%d".formatted(month, year) : null);
        }

        static Card of(NetopiaClient.Started started) {
            return new Card(started.token(), started.panMasked(),
                    started.expireMonth() != null && started.expireYear() != null
                            ? "%02d/%d".formatted(started.expireMonth(), started.expireYear()) : null);
        }
    }

    /** What a notification left to do once its transaction committed. */
    private record Outcome(UUID invoiceToRecord, UUID failedDebitToMail, String reason) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Checkout: the client, on Netopia's page
    // ─────────────────────────────────────────────────────────────────────────

    /** Netopia's payment page for one of the account's own issued invoices. */
    public String checkout(AppUser user, UUID tenantId, UUID invoiceId) {
        if (!netopia.isConfigured()) {
            throw new UnprocessableEntityException(CARD_PAYMENT_UNAVAILABLE);
        }
        Order order = tx.execute(status -> {
            Subscription s = subscriptionService.payerFor(user, tenantId)
                    .orElseThrow(() -> new NotFoundException(SUBSCRIPTION_NOT_FOUND));
            SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId)
                    .filter(i -> i.getSubscription().getId().equals(s.getId()))
                    .orElseThrow(() -> new NotFoundException(INVOICE_NOT_FOUND));
            if (invoice.getStatus() != InvoiceStatus.ISSUED) {
                throw new UnprocessableEntityException(INVOICE_NOT_PAYABLE);
            }
            return reserve(invoice, CardPaymentKind.CHECKOUT, null);
        });

        NetopiaClient.Started started;
        try {
            started = netopia.start(order.orderId(), order.amount(), order.description(), order.billing(),
                    frontendBaseUrl + "/abonament?plata=" + order.paymentId(), null);
        } catch (RuntimeException e) {
            log.error("Plata cu cardul {} n-a putut porni la Netopia: {}", order.orderId(), e.getMessage());
            fail(order.paymentId(), "Netopia indisponibil: " + e.getMessage());
            throw new UnprocessableEntityException(CARD_PAYMENT_UNAVAILABLE);
        }
        if (started.paymentUrl() == null) {
            log.error("Netopia n-a dat pagina de plată pentru {}: {} {}", order.orderId(), started.code(), started.message());
            fail(order.paymentId(), reason(started));
            throw new UnprocessableEntityException(CARD_PAYMENT_UNAVAILABLE);
        }
        tx.executeWithoutResult(status -> cardPaymentRepository.findById(order.paymentId()).ifPresent(p -> {
            p.setNtpId(started.ntpId());
            p.setPaymentUrl(started.paymentUrl());
            p.setUpdatedAt(Instant.now());
        }));
        return started.paymentUrl();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // The notification
    // ─────────────────────────────────────────────────────────────────────────

    /** Called with a notification whose signature already checked out. */
    public void handleNotification(JsonNode notification) {
        JsonNode payment = notification.path("payment");
        String ntpId = payment.path("ntpID").asText("");
        int status = payment.path("status").asInt(0);
        String orderId = text(notification.path("order"), "orderID");

        Outcome outcome;
        try {
            outcome = tx.execute(t -> {
                if (notificationRepository.existsByNtpIdAndStatus(ntpId, status)) {
                    log.info("Notificare Netopia repetată, ignorată: ntpID {}, stare {}", ntpId, status);
                    return null;
                }
                notificationRepository.saveAndFlush(PaymentNotification.builder()
                        .ntpId(ntpId).orderId(orderId).status(status)
                        .body(masked(notification)).receivedAt(Instant.now()).build());
                CardPayment p = orderId == null ? null : cardPaymentRepository.findByOrderId(orderId).orElse(null);
                if (p == null) {
                    log.warn("Notificare Netopia pentru o comandă necunoscută: {} (ntpID {})", orderId, ntpId);
                    return null;
                }
                if (status == NetopiaClient.STATUS_PAID || status == NetopiaClient.STATUS_CONFIRMED) {
                    return new Outcome(settleInTx(p, Card.of(payment)), null, null);
                }
                if (status == NetopiaClient.STATUS_DECLINED) {
                    String reason = declineReason(payment);
                    failInTx(p, reason);
                    return new Outcome(null, p.getKind() == CardPaymentKind.TOKEN ? p.getId() : null, reason);
                }
                log.info("Notificare Netopia intermediară: comanda {}, stare {}", orderId, status);
                return null;
            });
        } catch (DataIntegrityViolationException e) {
            log.info("Notificare Netopia prelucrată deja în paralel: ntpID {}, stare {}", ntpId, status);
            return;
        }
        if (outcome == null) {
            return;
        }
        if (outcome.invoiceToRecord() != null) {
            recordInFgo(outcome.invoiceToRecord());
        }
        if (outcome.failedDebitToMail() != null) {
            mailRefusal(outcome.failedDebitToMail(), outcome.reason());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // The daily run: saved cards, and FGO
    // ─────────────────────────────────────────────────────────────────────────

    /** Debits the saved card of every issued invoice whose next attempt falls today. */
    public int debitSavedCards(LocalDate today) {
        if (!netopia.isConfigured()) {
            return 0;
        }
        int attempted = 0;
        for (UUID invoiceId : invoiceRepository.findIdsToDebit(InvoiceStatus.ISSUED, SubscriptionPaymentMethod.CARD)) {
            try {
                if (debit(invoiceId, today)) {
                    attempted++;
                }
            } catch (RuntimeException e) {
                log.error("Debitarea cardului pentru factura {} a căzut: {}", invoiceId, e.getMessage());
            }
        }
        return attempted;
    }

    /** Retries {@code factura/incasare} for every card payment FGO does not have yet. */
    public int recordCardPaymentsInFgo() {
        if (!fgo.isConfigured()) {
            return 0;
        }
        int recorded = 0;
        for (UUID invoiceId : invoiceRepository.findIdsToRecordInFgo(InvoiceStatus.PAID, SubscriptionPaymentMethod.CARD)) {
            if (recordInFgo(invoiceId)) {
                recorded++;
            }
        }
        return recorded;
    }

    private boolean debit(UUID invoiceId, LocalDate today) {
        Order order = tx.execute(status -> {
            SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
            List<CardPayment> attempts = cardPaymentRepository.findAllByInvoice_IdOrderByCreatedAtDesc(invoiceId)
                    .stream().filter(p -> p.getKind() == CardPaymentKind.TOKEN).toList();
            for (CardPayment stale : attempts) {
                // A debit Netopia never answered by the next day would block every attempt after it.
                if (stale.getStatus() == CardPaymentStatus.STARTED
                        && LocalDate.ofInstant(stale.getCreatedAt(), BillingRunService.ZONE).isBefore(today)) {
                    failInTx(stale, "fără răspuns de la Netopia");
                }
            }
            if (attempts.size() >= DEBIT_DAYS.length
                    || attempts.stream().anyMatch(p -> p.getStatus() == CardPaymentStatus.STARTED)
                    || attempts.stream().anyMatch(p ->
                            LocalDate.ofInstant(p.getCreatedAt(), BillingRunService.ZONE).equals(today))) {
                return null;
            }
            LocalDate issued = LocalDate.ofInstant(invoice.getIssuedAt(), BillingRunService.ZONE);
            if (today.isBefore(issued.plusDays(DEBIT_DAYS[attempts.size()]))) {
                return null;
            }
            return reserve(invoice, CardPaymentKind.TOKEN, invoice.getSubscription().getCardToken());
        });
        if (order == null) {
            return false;
        }

        NetopiaClient.Started started;
        try {
            started = netopia.start(order.orderId(), order.amount(), order.description(), order.billing(),
                    frontendBaseUrl + "/abonament", order.token());
        } catch (RuntimeException e) {
            // Our outage, or Netopia's: no mail, the client did nothing wrong. It counts as an attempt.
            log.error("Debitarea {} n-a putut porni la Netopia: {}", order.orderId(), e.getMessage());
            fail(order.paymentId(), "Netopia indisponibil: " + e.getMessage());
            return true;
        }
        tx.executeWithoutResult(status -> cardPaymentRepository.findById(order.paymentId())
                .ifPresent(p -> p.setNtpId(started.ntpId())));

        if (started.isPaid()) {
            UUID paid = tx.execute(status -> settleInTx(cardPaymentRepository.findById(order.paymentId()).orElseThrow(),
                    Card.of(started)));
            recordInFgo(paid);
        } else if (started.status() == NetopiaClient.STATUS_NEEDS_3DS || started.paymentUrl() != null) {
            String reason = "banca cere confirmarea titularului cardului";
            fail(order.paymentId(), reason);
            mailRefusal(order.paymentId(), reason);
        } else if (started.status() == NetopiaClient.STATUS_DECLINED || !"00".equals(started.code())) {
            String reason = reason(started);
            fail(order.paymentId(), reason);
            mailRefusal(order.paymentId(), reason);
        }
        // Otherwise still STARTED: the notification decides.
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Order reserve(SubscriptionInvoice invoice, CardPaymentKind kind, String token) {
        Instant now = Instant.now();
        CardPayment p = cardPaymentRepository.save(CardPayment.builder()
                .invoice(invoice)
                .orderId("WH" + invoice.getFgoNumar() + "-" + UUID.randomUUID().toString().substring(0, 8))
                .kind(kind)
                .amount(invoice.getTotal())
                .status(CardPaymentStatus.STARTED)
                .createdAt(now)
                .updatedAt(now)
                .build());
        String number = invoice.getFgoSerie() + " " + invoice.getFgoNumar();
        return new Order(p.getId(), p.getOrderId(), invoice.getTotal(), "Factura " + number + " - abonament WasteHouse",
                billing(invoice.getSubscription()), token);
    }

    /** The invoice PAID by card, the card saved when Netopia gave one, the status read again. Returns the invoice. */
    private UUID settleInTx(CardPayment p, Card card) {
        Instant now = Instant.now();
        p.setStatus(CardPaymentStatus.PAID);
        p.setError(null);
        p.setUpdatedAt(now);
        SubscriptionInvoice invoice = p.getInvoice();
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            if (invoice.getPaidBy() != SubscriptionPaymentMethod.CARD) {
                log.warn("Factura {} era deja plătită ({}), iar cardul a plătit-o încă o dată: comanda {} de returnat.",
                        invoice.getId(), invoice.getPaidBy(), p.getOrderId());
            }
        } else {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidBy(SubscriptionPaymentMethod.CARD);
            invoice.setPaidAt(now);
            invoice.setAmountPaid(p.getAmount());
        }
        Subscription s = invoice.getSubscription();
        if (card.token() != null) {
            s.setCardToken(card.token());
            s.setCardPanMasked(card.panMasked());
            s.setCardExpiry(card.expiry());
            s.setPaymentMethod(SubscriptionPaymentMethod.CARD);
        }
        subscriptionService.refreshStatus(s, LocalDate.now(BillingRunService.ZONE));
        log.info("Factura {} plătită cu cardul: comanda {}, card salvat {}", invoice.getId(), p.getOrderId(),
                NetopiaIpnController.mask(card.token()));
        return invoice.getId();
    }

    private void failInTx(CardPayment p, String reason) {
        p.setStatus(CardPaymentStatus.FAILED);
        p.setError(truncate(reason));
        p.setUpdatedAt(Instant.now());
    }

    private void fail(UUID paymentId, String reason) {
        tx.executeWithoutResult(status -> cardPaymentRepository.findById(paymentId).ifPresent(p -> failInTx(p, reason)));
    }

    private record Refusal(SubscriptionInvoice invoice, String client, String to) {}

    private void mailRefusal(UUID paymentId, String reason) {
        try {
            Refusal refusal = tx.execute(status -> {
                SubscriptionInvoice invoice = cardPaymentRepository.findById(paymentId).orElseThrow().getInvoice();
                Subscription s = invoice.getSubscription();
                return new Refusal(invoice, BillingRunService.clientName(s), BillingRunService.recipient(s));
            });
            if (refusal.to() != null) {
                notificationService.sendBillingReminder(refusal.invoice(), refusal.client(), refusal.to(),
                        BillingReminder.CARD_FAILED, reason);
            }
        } catch (RuntimeException e) {
            log.error("Mailul despre cardul refuzat ({}) n-a plecat: {}", paymentId, e.getMessage());
        }
    }

    private record Collection(String serie, String numar, BigDecimal amount, Instant paidAt) {}

    private boolean recordInFgo(UUID invoiceId) {
        if (!fgo.isConfigured()) {
            return false;
        }
        try {
            Collection c = tx.execute(status -> invoiceRepository.findById(invoiceId)
                    .filter(i -> i.getFgoCollectedAt() == null && i.getPaidBy() == SubscriptionPaymentMethod.CARD)
                    .map(i -> new Collection(i.getFgoSerie(), i.getFgoNumar(), i.getAmountPaid(), i.getPaidAt()))
                    .orElse(null));
            if (c == null) {
                return false;
            }
            fgo.collect(c.serie(), c.numar(), c.amount(), LocalDateTime.ofInstant(c.paidAt(), BillingRunService.ZONE));
            tx.executeWithoutResult(status -> invoiceRepository.findById(invoiceId)
                    .ifPresent(i -> i.setFgoCollectedAt(Instant.now())));
            return true;
        } catch (RuntimeException e) {
            log.warn("Încasarea cu cardul a facturii {} n-a putut fi trecută în FGO: {}", invoiceId, e.getMessage());
            return false;
        }
    }

    /** Netopia's billing block: who pays, as on the invoice. */
    private static NetopiaClient.Billing billing(Subscription s) {
        String client = BillingRunService.clientName(s);
        Company company = s.getCompany();
        String phone = company != null && !isBlank(company.getContactPhone()) ? company.getContactPhone() : "0000000000";
        String person = company != null && !isBlank(company.getContactName()) ? company.getContactName() : client;
        String email = BillingRunService.recipient(s);
        return new NetopiaClient.Billing(email == null ? "contact@wastehouse.ro" : email, phone, person, client,
                nullToEmpty(s.getBillingCity()), nullToEmpty(s.getBillingCounty()), nullToEmpty(s.getBillingAddress()));
    }

    /** The body as stored: the token debits the card, so it is kept only on the subscription. */
    private static String masked(JsonNode notification) {
        JsonNode copy = notification.deepCopy();
        JsonNode payment = copy.path("payment");
        if (payment instanceof ObjectNode p && p.hasNonNull("token")) {
            p.put("token", NetopiaIpnController.mask(p.path("token").asText()));
        }
        if (payment.path("binding") instanceof ObjectNode b && b.hasNonNull("token")) {
            b.put("token", NetopiaIpnController.mask(b.path("token").asText()));
        }
        return copy.toString();
    }

    private static String declineReason(JsonNode payment) {
        String message = text(payment, "message");
        String code = text(payment, "code");
        return message == null ? "card refuzat" : (code == null ? message : code + " " + message);
    }

    private static String reason(NetopiaClient.Started started) {
        return started.message().isBlank() ? "card refuzat (" + started.status() + ")"
                : started.code() + " " + started.message();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static String truncate(String s) {
        return s == null || s.length() <= 1000 ? s : s.substring(0, 1000);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
