package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.CardPaymentKind;
import ro.ecoregistru.enums.CardPaymentStatus;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CardPaymentRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PaymentNotificationRepository;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.service.BillingRunService;
import ro.ecoregistru.service.EmailService;
import ro.ecoregistru.service.FgoClient;
import ro.ecoregistru.service.NetopiaClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F3 din plata-abonamente.md — cardul, cu Netopia și FGO înlocuite de mock-uri, iar notificarea semnată
 * de-adevăratelea cu o cheie a testului. Ce acceptă Netopia se probează pe sandbox; aici e ce ține de noi:
 *
 * <ul>
 *   <li>factura întâi, cardul o plătește (decizia din 15.09.2026);</li>
 *   <li>o notificare trimisă de două ori plătește o singură dată și trece încasarea în FGO o singură dată;</li>
 *   <li>tokenul se salvează, nu pleacă la client, iar facturile următoare se debitează fără client;</li>
 *   <li>debitarea refuzată se reia în zilele 3, 6 și 10, cu mail la fiecare refuz, și nu mai mult;</li>
 *   <li>FGO căzut la încasare nu pierde încasarea; factura altui cont nu se poate plăti.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CardPaymentIT {

    static final LocalDate START = LocalDate.of(2026, 10, 17);
    static final String POS = "TEST-POS1-SIGN-ATUR-EXXX";
    static final KeyPair NETOPIA = rsa();
    static final String REMINDER_MAIL = "mail/billing_reminder";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionInvoiceRepository invoiceRepository;
    @Autowired CardPaymentRepository cardPaymentRepository;
    @Autowired PaymentNotificationRepository notificationRepository;
    @Autowired BillingRunService billing;

    @MockBean EmailService emailService;
    @MockBean FgoClient fgo;
    @MockBean NetopiaClient netopia;

    @DynamicPropertySource
    static void netopiaKeys(DynamicPropertyRegistry registry) {
        registry.add("app.netopia.pos-signature", () -> POS);
        registry.add("app.netopia.public-key", () -> "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(NETOPIA.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----");
    }

    @BeforeEach
    void setUp() {
        when(fgo.isConfigured()).thenReturn(true);
        when(fgo.emit(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return new FgoClient.Issued("WH", id.substring(0, 8), "https://fgo.test/" + id + ".pdf", null);
        });
        when(fgo.status(any(), any())).thenAnswer(inv -> new FgoClient.Status(new BigDecimal("389"), BigDecimal.ZERO));
        when(netopia.isConfigured()).thenReturn(true);
        when(netopia.start(any(), any(), any(), any(), any(), isNull())).thenAnswer(inv -> new NetopiaClient.Started(
                "ntp-" + suffix(), 1, "https://sandbox.test/ui/card?p=" + inv.getArgument(0), "101",
                "Redirect user to payment page", null, null, null, null));
    }

    @Test
    void theSignedNotificationPaysTheInvoiceOnceAndRecordsItInFgoOnce() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        billing.run(START);
        SubscriptionInvoice invoice = invoice(s);

        String url = checkout(admin(company), invoice);
        assertThat(url).startsWith("https://sandbox.test/ui/card");
        String orderId = lastOrderId();
        assertThat(orderId).startsWith("WH" + invoice.getFgoNumar() + "-");

        String body = notification(orderId, "ntp-" + suffix(), 3, null);
        ipn(body);
        ipn(body);

        SubscriptionInvoice paid = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getPaidBy()).isEqualTo(SubscriptionPaymentMethod.CARD);
        assertThat(paid.getFgoCollectedAt()).isNotNull();
        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(fgo, times(1)).collect(eq("WH"), eq(invoice.getFgoNumar()), argEq("389"), any());
        assertThat(notificationRepository.findAll()).filteredOn(n -> orderId.equals(n.getOrderId())).hasSize(1);
        assertThat(cardPaymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(CardPaymentStatus.PAID);
    }

    /** Tokenul se salvează pe abonament, nu apare nici în răspunsul clientului, nici în notificarea păstrată. */
    @Test
    void aNotificationWithATokenSavesTheCardAndTheNextInvoiceIsDebitedWithoutTheClient() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        billing.run(START);
        SubscriptionInvoice first = invoice(s);
        checkout(admin(company), first);
        String orderId = lastOrderId();
        String token = "tok" + suffix() + suffix();

        ipn(notification(orderId, "ntp-" + suffix(), 3, token));

        Subscription saved = subscriptionRepository.findById(s.getId()).orElseThrow();
        assertThat(saved.getCardToken()).isEqualTo(token);
        assertThat(saved.getPaymentMethod()).isEqualTo(SubscriptionPaymentMethod.CARD);
        assertThat(saved.getCardPanMasked()).isEqualTo("9900****5098");
        assertThat(saved.getCardExpiry()).isEqualTo("12/2030");
        assertThat(notificationRepository.findAll()).filteredOn(n -> orderId.equals(n.getOrderId()))
                .singleElement().satisfies(n -> assertThat(n.getBody()).doesNotContain(token));
        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(admin(company))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod", is("CARD")))
                .andExpect(jsonPath("$.cardPanMasked", is("9900****5098")))
                .andExpect(content().string(not(containsString(token))));

        when(netopia.start(any(), any(), any(), any(), any(), eq(token))).thenAnswer(inv -> new NetopiaClient.Started(
                "ntp-" + suffix(), 3, null, "00", "Approved", token, "9900****5098", 12, 2030));
        billing.run(LocalDate.of(2026, 11, 17));

        List<SubscriptionInvoice> invoices = invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId());
        assertThat(invoices).hasSize(2).first().satisfies(i -> {
            assertThat(i.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(i.getPaidBy()).isEqualTo(SubscriptionPaymentMethod.CARD);
            assertThat(i.getTotal()).isEqualByComparingTo("99");
        });
        verify(netopia, times(1)).start(any(), argEq("99"), any(), any(), any(), eq(token));
        verify(fgo, times(1)).collect(eq("WH"), eq(invoices.get(0).getFgoNumar()), argEq("99"), any());
    }

    /** §2.2: zilele 0, 3, 6 și 10 după emitere, un mail la fiecare refuz, apoi nimic — rămâne transferul. */
    @Test
    void aRefusedDebitIsRetriedOnDaysThreeSixAndTenThenStops() {
        Company company = company();
        Subscription s = subscription(company);
        String token = "tok" + suffix() + suffix();
        s.setPaymentMethod(SubscriptionPaymentMethod.CARD);
        s.setCardToken(token);
        subscriptionRepository.save(s);
        when(netopia.start(any(), any(), any(), any(), any(), eq(token))).thenAnswer(inv -> new NetopiaClient.Started(
                "ntp-" + suffix(), 12, null, "12", "Card refuzat", null, null, null, null));

        for (int day = 0; day <= 14; day++) {
            billing.run(START.plusDays(day));
        }

        verify(netopia, times(4)).start(any(), any(), any(), any(), any(), eq(token));
        SubscriptionInvoice invoice = invoice(s);
        assertThat(cardPaymentRepository.findAllByInvoice_IdOrderByCreatedAtDesc(invoice.getId()))
                .hasSize(4)
                .allSatisfy(p -> {
                    assertThat(p.getKind()).isEqualTo(CardPaymentKind.TOKEN);
                    assertThat(p.getStatus()).isEqualTo(CardPaymentStatus.FAILED);
                    assertThat(p.getError()).contains("Card refuzat");
                });
        verify(emailService, times(4)).send(eq(billingEmail(company)), contains("nu a trecut"), eq(REMINDER_MAIL), any());
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    }

    /** Netopia a plătit, FGO e căzut: factura e plătită la noi, iar încasarea în FGO se reia la rularea următoare. */
    @Test
    void aCollectionFgoMissedIsRecordedOnTheNextRun() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        billing.run(START);
        SubscriptionInvoice invoice = invoice(s);
        doThrow(new FgoClient.FgoException("FGO /factura/incasare: indisponibil")).doNothing()
                .when(fgo).collect(eq("WH"), eq(invoice.getFgoNumar()), any(), any());

        checkout(admin(company), invoice);
        ipn(notification(lastOrderId(), "ntp-" + suffix(), 3, null));
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow()).satisfies(i -> {
            assertThat(i.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(i.getFgoCollectedAt()).isNull();
        });

        billing.run(START.plusDays(1));

        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getFgoCollectedAt()).isNotNull();
        verify(fgo, times(2)).collect(eq("WH"), eq(invoice.getFgoNumar()), any(), any());
    }

    @Test
    void aRefusedCheckoutLeavesTheInvoiceUnpaidAndSaysWhy() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        billing.run(START);
        SubscriptionInvoice invoice = invoice(s);
        AppUser admin = admin(company);
        checkout(admin, invoice);
        String orderId = lastOrderId();

        ipn(notification(orderId, "ntp-" + suffix(), 12, null));

        UUID paymentId = cardPaymentRepository.findByOrderId(orderId).orElseThrow().getId();
        mockMvc.perform(get("/api/v1/billing/card-payments/" + paymentId).header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.invoiceStatus", is("ISSUED")));
        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(admin)))
                .andExpect(jsonPath("$.invoices[0].lastCardError", containsString("Card refuzat")));
        verify(fgo, never()).collect(any(), eq(invoice.getFgoNumar()), any(), any());
    }

    @Test
    void anotherAccountsInvoiceCannotBePaidNorSeen() throws Exception {
        Company owner = company();
        Subscription s = subscription(owner);
        billing.run(START);
        SubscriptionInvoice invoice = invoice(s);
        Company stranger = company();
        subscription(stranger);

        mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/card")
                        .header("Authorization", "Bearer " + token(admin(stranger))))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/card")
                        .header("Authorization", "Bearer " + token(user(Role.OPERATOR, owner))))
                .andExpect(status().isForbidden());

        checkout(admin(owner), invoice);
        UUID paymentId = cardPaymentRepository.findByOrderId(lastOrderId()).orElseThrow().getId();
        mockMvc.perform(get("/api/v1/billing/card-payments/" + paymentId)
                        .header("Authorization", "Bearer " + token(admin(stranger))))
                .andExpect(status().isNotFound());
    }

    /** O factură plătită nu se mai plătește cu cardul. */
    @Test
    void aPaidInvoiceIsNotPayableAgain() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        billing.run(START);
        SubscriptionInvoice invoice = invoice(s);
        AppUser admin = admin(company);
        checkout(admin, invoice);
        ipn(notification(lastOrderId(), "ntp-" + suffix(), 3, null));

        mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/card")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error-code", is("invoice.not.payable")));
    }

    /** „Transfer" înseamnă „nu-mi mai debita cardul": tokenul pleacă, nu doar bifa. */
    @Test
    void choosingTransferForgetsTheSavedCard() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        s.setPaymentMethod(SubscriptionPaymentMethod.CARD);
        s.setCardToken("tok" + suffix());
        s.setCardPanMasked("9900****5098");
        subscriptionRepository.save(s);

        mockMvc.perform(put("/api/v1/billing/payment-method")
                        .header("Authorization", "Bearer " + token(admin(company)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"TRANSFER\"}"))
                .andExpect(status().isNoContent());

        Subscription saved = subscriptionRepository.findById(s.getId()).orElseThrow();
        assertThat(saved.getPaymentMethod()).isEqualTo(SubscriptionPaymentMethod.TRANSFER);
        assertThat(saved.getCardToken()).isNull();
        assertThat(saved.getCardPanMasked()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String checkout(AppUser admin, SubscriptionInvoice invoice) throws Exception {
        String json = mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/card")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(json).path("paymentUrl").asText();
    }

    private String lastOrderId() {
        ArgumentCaptor<String> orderIds = ArgumentCaptor.forClass(String.class);
        verify(netopia, atLeastOnce()).start(orderIds.capture(), any(), any(), any(), any(), isNull());
        return orderIds.getValue();
    }

    private void ipn(String body) throws Exception {
        mockMvc.perform(post("/api/v1/billing/netopia/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Verification-token", signed(body))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorType").value(0));
    }

    /** Forma notificării prinse pe sandbox pe 15.09.2026, cu tokenul acolo unde spune spec-ul (`binding`). */
    private static String notification(String orderId, String ntpId, int status, String token) {
        String binding = token == null ? "{\"expireMonth\":0,\"expireYear\":0}"
                : "{\"expireMonth\":12,\"expireYear\":2030,\"token\":\"" + token + "\"}";
        String instrument = token == null ? "{\"country\":0}" : "{\"country\":642,\"panMasked\":\"9900****5098\"}";
        String code = status == 3 ? "00" : String.valueOf(status);
        String message = status == 3 ? "Approved" : "Card refuzat";
        return "{\"order\":{\"orderID\":\"" + orderId + "\"},\"payment\":{\"amount\":389,\"binding\":" + binding
                + ",\"code\":\"" + code + "\",\"currency\":\"RON\",\"instrument\":" + instrument + ",\"message\":\""
                + message + "\",\"ntpID\":\"" + ntpId + "\",\"status\":" + status + "}}";
    }

    private SubscriptionInvoice invoice(Subscription s) {
        return invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()).get(0);
    }

    private Company company() {
        return companyRepository.save(Company.builder()
                .name("Firma " + suffix()).cui(cui())
                .type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
    }

    private static String billingEmail(Company company) {
        return "facturi+" + company.getCui() + "@firma.ro";
    }

    private Subscription subscription(Company company) {
        return subscriptionRepository.save(Subscription.builder()
                .company(company)
                .plan(SubscriptionPlan.GENERATOR)
                .status(SubscriptionStatus.PENDING)
                .monthlyPrice(SubscriptionPlan.GENERATOR.monthlyPrice())
                .implementationFee(SubscriptionPlan.GENERATOR.implementationFee())
                .extraWorkPointPrice(SubscriptionPlan.EXTRA_WORK_POINT_PRICE)
                .startedAt(START)
                .createdAt(Instant.now())
                .billingEmail(billingEmail(company))
                .billingCounty("Cluj").billingCity("Cluj-Napoca").billingAddress("Str. Memorandumului nr. 1")
                .build());
    }

    private AppUser admin(Company company) {
        return user(Role.ADMIN, company);
    }

    private AppUser user(Role role, Company company) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@firma.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(company)
                .enabled(true).createdAt(Instant.now()).build());
    }

    private String token(AppUser user) {
        return jwtService.generateToken(user);
    }

    private static BigDecimal argEq(String amount) {
        return argThat(a -> a != null && a.compareTo(new BigDecimal(amount)) == 0);
    }

    private static String signed(String body) {
        try {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("aud", List.of(POS));
            claims.put("iat", Instant.now().getEpochSecond());
            claims.put("iss", "NETOPIA Payments");
            claims.put("sub", Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-512").digest(body.getBytes(StandardCharsets.UTF_8))));
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            ObjectMapper om = new ObjectMapper();
            String signingInput = b64.encodeToString(om.writeValueAsBytes(Map.of("alg", "RS512", "typ", "JWT")))
                    + "." + b64.encodeToString(om.writeValueAsBytes(claims));
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initSign(NETOPIA.getPrivate());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + b64.encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair rsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cui() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
