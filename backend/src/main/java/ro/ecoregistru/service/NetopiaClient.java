package ro.ecoregistru.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NETOPIA Payments API v2, the one call F3 needs: {@code POST /payment/card/start}.
 *
 * <ul>
 *   <li><b>Without a token</b> the answer carries {@code paymentURL}, Netopia's page where the client
 *       types the card (error code 101, „Redirect user to payment page"). Checked on the sandbox,
 *       15.09.2026.</li>
 *   <li><b>With a token</b> {@code instrument.token} „overrides all other data" (the OpenAPI spec). Whether
 *       it debits without the client or asks for 3-D Secure (status 15) is <b>not proven</b>: the sandbox
 *       POS gave no token on 15.09.2026, neither in the answer nor in the notification.</li>
 * </ul>
 *
 * <p>The result of a payment is trusted only from the signed notification ({@link NetopiaIpnVerifier}); the
 * answer here only says where to send the client, or that a token debit was refused right away.
 */
@Component
public class NetopiaClient {

    /** Netopia's {@code payment.status}. */
    public static final int STATUS_PAID = 3;
    public static final int STATUS_CONFIRMED = 5;
    public static final int STATUS_DECLINED = 12;
    public static final int STATUS_NEEDS_3DS = 15;

    public record Billing(String email, String phone, String firstName, String lastName, String city,
                          String state, String details) {}

    /**
     * @param code    Netopia's {@code error.code}: "00" approved, "101" go to the payment page, anything
     *                else a refusal
     * @param token   the saved card, when Netopia returns one
     */
    public record Started(String ntpId, int status, String paymentUrl, String code, String message,
                          String token, String panMasked, Integer expireMonth, Integer expireYear) {

        public boolean isPaid() {
            return status == STATUS_PAID || status == STATUS_CONFIRMED;
        }
    }

    public static class NetopiaException extends RuntimeException {
        public NetopiaException(String message) {
            super(message);
        }
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final RestClient http;
    private final String apiKey;
    private final String posSignature;
    private final String notifyUrl;

    @Autowired
    public NetopiaClient(@Value("${app.netopia.base-url:https://secure-sandbox.netopia-payments.com}") String baseUrl,
                         @Value("${app.netopia.api-key:}") String apiKey,
                         @Value("${app.netopia.pos-signature:}") String posSignature,
                         @Value("${app.netopia.notify-url:}") String notifyUrl) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(timeouts()).build(), apiKey, posSignature, notifyUrl);
    }

    NetopiaClient(RestClient http, String apiKey, String posSignature, String notifyUrl) {
        this.http = http;
        this.apiKey = apiKey;
        this.posSignature = posSignature;
        this.notifyUrl = notifyUrl;
    }

    /** Without the notify URL a payment could succeed and never reach us, so nothing starts. */
    public boolean isConfigured() {
        return !apiKey.isBlank() && !posSignature.isBlank() && !notifyUrl.isBlank();
    }

    /**
     * @param token null for Netopia's payment page, the saved card for a debit without the client
     */
    public Started start(String orderId, BigDecimal amount, String description, Billing billing,
                         String redirectUrl, String token) {
        Map<String, Object> instrument = new LinkedHashMap<>();
        if (token == null) {
            instrument.put("type", "card");
        } else {
            instrument.put("token", token);
        }

        Map<String, Object> billingBody = new LinkedHashMap<>();
        billingBody.put("email", billing.email());
        billingBody.put("phone", billing.phone());
        billingBody.put("firstName", billing.firstName());
        billingBody.put("lastName", billing.lastName());
        billingBody.put("city", billing.city());
        billingBody.put("country", 642);
        billingBody.put("state", billing.state());
        billingBody.put("postalCode", "");
        billingBody.put("details", billing.details());

        BigDecimal lei = amount.setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("ntpID", "");
        order.put("posSignature", posSignature);
        order.put("dateTime", DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC)));
        order.put("description", description);
        order.put("orderID", orderId);
        order.put("amount", lei);
        order.put("currency", "RON");
        order.put("billing", billingBody);
        order.put("products", List.of(Map.of("name", description, "code", "WH-ABONAMENT",
                "category", "abonament", "price", lei, "vat", 0)));
        order.put("installments", Map.of("selected", 0, "available", List.of(0)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("config", Map.of("emailTemplate", "", "emailSubject", "", "notifyUrl", notifyUrl,
                "redirectUrl", redirectUrl, "language", "ro"));
        body.put("payment", Map.of("options", Map.of("installments", 0, "bonus", 0), "instrument", instrument));
        body.put("order", order);

        JsonNode response = http.post().uri("/payment/card/start")
                .header("Authorization", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, res) -> {
                    JsonNode json = res.bodyTo(JsonNode.class);
                    if (res.getStatusCode().is5xxServerError() || json == null) {
                        throw new NetopiaException("Netopia /payment/card/start: HTTP " + res.getStatusCode().value());
                    }
                    return json;
                });
        return parse(response);
    }

    /** Package-private for the tests: the answer's shape, from the sandbox probes of 15.09.2026. */
    static Started parse(JsonNode response) {
        JsonNode payment = response.path("payment");
        JsonNode error = response.path("error");
        if (payment.isMissingNode() && !error.isMissingNode()) {
            // A 4xx with no payment at all: a bad key, a bad POS, a malformed order.
            throw new NetopiaException("Netopia: " + error.path("code").asText() + " " + error.path("message").asText());
        }
        JsonNode binding = payment.path("binding");
        String token = textOrNull(binding, "token");
        if (token == null) {
            token = textOrNull(payment, "token");
        }
        int expireMonth = binding.path("expireMonth").asInt(0);
        int expireYear = binding.path("expireYear").asInt(0);
        return new Started(textOrNull(payment, "ntpID"), payment.path("status").asInt(0),
                textOrNull(payment, "paymentURL"), error.path("code").asText(""), error.path("message").asText(""),
                token, textOrNull(payment.path("instrument"), "panMasked"),
                expireMonth > 0 ? expireMonth : null, expireYear > 0 ? expireYear : null);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        return factory;
    }
}
