package ro.ecoregistru.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The FGO invoicing API v7 (<a href="https://api-testuat.fgo.ro/v1/testing.html">documentation</a>),
 * only the calls billing needs: issue an invoice, read whether it was paid, and record a card payment.
 *
 * <p>Every request is a raw JSON body, authenticated by an uppercase SHA-1 of the company's CUI, the
 * private key and one more value: the buyer's name when issuing, the invoice number otherwise. FGO
 * allows one request per second, so calls are spaced by {@code app.fgo.min-interval-ms}.
 *
 * <p>Unconfigured (no CUI, key or series) the application still boots; {@link BillingRunService}
 * reads {@link #isConfigured()} and invoices nothing.
 */
@Component
public class FgoClient {

    public record Buyer(String name, String cui, String email, String county, String city, String address) {}

    public record Issued(String serie, String numar, String link, String linkPlata) {}

    public record Status(BigDecimal value, BigDecimal paid) {
        public boolean isPaid() {
            return value.signum() > 0 && paid.compareTo(value) >= 0;
        }
    }

    /** Lacătul FGO n-a fost liber la timp pentru o cerere a clientului; nu s-a întrebat nimic. */
    public static class FgoBusyException extends FgoException {
        public FgoBusyException() {
            super("FGO e ocupat cu altă cerere");
        }
    }

    public static class FgoException extends RuntimeException {
        public FgoException(String message) {
            super(message);
        }
    }

    private static final DateTimeFormatter FGO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient http;
    private final String codUnic;
    private final String privateKey;
    private final String serie;
    private final String tipFactura;
    private final BigDecimal cotaTva;
    private final String platformaUrl;
    private final long minIntervalMs;
    private final String tipIncasareCard;
    private static final int CONFLICT_ATTEMPTS = 3;
    /**
     * Cât așteaptă „Am plătit — verifică acum” după lacătul FGO. Rularea de la 06:30 și butoanele platformei
     * așteaptă cât e nevoie; clientul nu: un apel ocupat poate ține lacătul peste un minut (30 s de citire, de trei
     * ori, cu pauzele la 409), iar cererile lui s-ar strânge în spatele lui peste limita de 30 s a Heroku.
     */
    static final long CLIENT_LOCK_WAIT_MS = 5_000;
    private final ReentrantLock lock = new ReentrantLock();
    private long lastCallAt;

    @Autowired
    public FgoClient(@Value("${app.fgo.base-url}") String baseUrl,
                     @Value("${app.fgo.cod-unic:}") String codUnic,
                     @Value("${app.fgo.private-key:}") String privateKey,
                     @Value("${app.fgo.serie:}") String serie,
                     @Value("${app.fgo.tip-factura:Factura}") String tipFactura,
                     @Value("${app.fgo.cota-tva:0}") BigDecimal cotaTva,
                     @Value("${app.fgo.platforma-url}") String platformaUrl,
                     @Value("${app.fgo.min-interval-ms:1000}") long minIntervalMs,
                     @Value("${app.fgo.tip-incasare-card:Banca}") String tipIncasareCard) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(timeouts()).build(),
                codUnic, privateKey, serie, tipFactura, cotaTva, platformaUrl, minIntervalMs, tipIncasareCard);
    }

    FgoClient(RestClient http, String codUnic, String privateKey, String serie, String tipFactura,
              BigDecimal cotaTva, String platformaUrl, long minIntervalMs) {
        this(http, codUnic, privateKey, serie, tipFactura, cotaTva, platformaUrl, minIntervalMs, "Banca");
    }

    FgoClient(RestClient http, String codUnic, String privateKey, String serie, String tipFactura,
              BigDecimal cotaTva, String platformaUrl, long minIntervalMs, String tipIncasareCard) {
        this.http = http;
        this.codUnic = codUnic;
        this.privateKey = privateKey;
        this.serie = serie;
        this.tipFactura = tipFactura;
        this.cotaTva = cotaTva;
        this.platformaUrl = platformaUrl;
        this.minIntervalMs = minIntervalMs;
        this.tipIncasareCard = tipIncasareCard;
    }

    public boolean isConfigured() {
        return !codUnic.isBlank() && !privateKey.isBlank() && !serie.isBlank();
    }

    /**
     * Issues an invoice in RON. Lines worth nothing (the founder's free implementation) are left out:
     * FGO needs a non-zero quantity and the line says nothing a zero total would not.
     *
     * @param externalId our invoice row id; with {@code VerificareDuplicat} FGO refuses a second one
     */
    public Issued emit(String externalId, Buyer buyer, List<BillingCalculator.Line> lines,
                       LocalDate issuedOn, LocalDate dueOn, String explanation) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("Denumire", buyer.name());
        putIfPresent(client, "CodUnic", buyer.cui());
        putIfPresent(client, "Email", buyer.email());
        client.put("Tara", "RO");
        putIfPresent(client, "Judet", buyer.county());
        putIfPresent(client, "Localitate", buyer.city());
        putIfPresent(client, "Adresa", buyer.address());
        client.put("Tip", "PJ");

        List<Map<String, Object>> content = lines.stream()
                .filter(l -> l.amount().signum() > 0)
                .map(l -> Map.<String, Object>of(
                        "Denumire", l.label(),
                        "NrProduse", l.quantity(),
                        "UM", "BUC",
                        "CotaTVA", cotaTva,
                        "PretUnitar", l.unitPrice()))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("CodUnic", codUnic);
        body.put("Hash", hash(codUnic, privateKey, buyer.name()));
        body.put("PlatformaUrl", platformaUrl);
        body.put("Serie", serie);
        body.put("Valuta", "RON");
        body.put("TipFactura", tipFactura);
        body.put("DataEmitere", issuedOn.toString());
        body.put("DataScadenta", dueOn.toString());
        putIfPresent(body, "Explicatii", explanation);
        body.put("IdExtern", externalId);
        body.put("VerificareDuplicat", true);
        body.put("Client", client);
        body.put("Continut", content);

        // With VerificareDuplicat, a repeated IdExtern is refused with Success false, but the answer
        // carries the invoice already issued for it (checked on api-testuat, 15.09.2026: "Factura WH 1
        // exista deja salvata"). That is our invoice, not an error; otherwise a lost first answer would
        // leave the row DRAFT and retried forever.
        JsonNode response = send("/factura/emitere", body);
        JsonNode invoice = response.path("Factura");
        if (!response.path("Success").asBoolean(false) && invoice.path("Numar").asText("").isBlank()) {
            throw failure("/factura/emitere", response);
        }
        return new Issued(invoice.path("Serie").asText(), invoice.path("Numar").asText(),
                textOrNull(invoice, "Link"), textOrNull(invoice, "LinkPlata"));
    }

    public Status status(String invoiceSerie, String invoiceNumar) {
        return status(invoiceSerie, invoiceNumar, false);
    }

    /**
     * Aceeași citire, cerută de client de pe {@code /abonament}: o singură încercare, și numai dacă lacătul se
     * eliberează în {@link #CLIENT_LOCK_WAIT_MS}. Altfel {@link FgoBusyException}, iar clientul reîncearcă.
     */
    public Status statusForClient(String invoiceSerie, String invoiceNumar) {
        return status(invoiceSerie, invoiceNumar, true);
    }

    private Status status(String invoiceSerie, String invoiceNumar, boolean client) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("CodUnic", codUnic);
        body.put("Hash", hash(codUnic, privateKey, invoiceNumar));
        body.put("PlatformaUrl", platformaUrl);
        body.put("Serie", invoiceSerie);
        body.put("Numar", invoiceNumar);
        JsonNode invoice;
        try {
            invoice = post("/factura/getstatus", body, client).path("Factura");
        } catch (FgoBusyException e) {
            throw e;
        } catch (FgoException e) {
            throw new FgoException(invoiceSerie + " " + invoiceNumar + ": " + e.getMessage());
        }
        return new Status(new BigDecimal(invoice.path("Valoare").asText("0")),
                new BigDecimal(invoice.path("ValoareAchitata").asText("0")));
    }

    /**
     * F3 — records a card payment on an issued invoice ({@code factura/incasare}, Premium and Enterprise
     * only). FGO has no „card" payment type; the money reaches the bank from Netopia, so the type comes
     * from {@code app.fgo.tip-incasare-card}, "Banca" by default.
     */
    public void collect(String invoiceSerie, String invoiceNumar, BigDecimal amount, LocalDateTime paidAt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("CodUnic", codUnic);
        body.put("Hash", hash(codUnic, privateKey, invoiceNumar));
        body.put("PlatformaUrl", platformaUrl);
        body.put("SerieFactura", invoiceSerie);
        body.put("NumarFactura", invoiceNumar);
        body.put("TipIncasare", tipIncasareCard);
        body.put("SumaIncasata", amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        body.put("DataIncasare", paidAt.format(FGO_DATE_TIME));
        post("/factura/incasare", body);
    }

    /** Uppercase hex SHA-1 of the concatenated parts, as FGO computes it. */
    static String hash(String... parts) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().withUpperCase()
                    .formatHex(sha1.digest(String.join("", parts).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** FGO answers errors with a JSON body and {@code Success: false}, sometimes on a 4xx: read both. */
    private JsonNode post(String path, Map<String, Object> body) {
        return post(path, body, false);
    }

    private JsonNode post(String path, Map<String, Object> body, boolean client) {
        JsonNode response = client ? sendForClient(path, body) : send(path, body);
        if (!response.path("Success").asBoolean(false)) {
            throw failure(path, response);
        }
        return response;
    }

    /**
     * The answer as FGO gave it, successful or not; only an empty body is an error here.
     *
     * <p>A 409 („The page was not displayed because there was a conflict") is FGO's rate limit, and from
     * Heroku it came on a lone getstatus minutes after any other call of ours (17.09.2026) — the egress
     * IP is shared. The page is refused before anything is done, so the call is simply asked again.
     */
    private JsonNode send(String path, Map<String, Object> body) {
        lock.lock();
        try {
            return sendLocked(path, body, CONFLICT_ATTEMPTS);
        } finally {
            lock.unlock();
        }
    }

    /** Scanarea din 17.09.2026: clientul nu stă la coadă după lacăt și nu reîncearcă un 409 — reîncearcă el. */
    private JsonNode sendForClient(String path, Map<String, Object> body) {
        boolean locked;
        try {
            locked = lock.tryLock(CLIENT_LOCK_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            locked = false;
        }
        if (!locked) {
            throw new FgoBusyException();
        }
        try {
            return sendLocked(path, body, 1);
        } finally {
            lock.unlock();
        }
    }

    private JsonNode sendLocked(String path, Map<String, Object> body, int attempts) {
        for (int attempt = 1; ; attempt++) {
            try {
                return sendOnce(path, body);
            } catch (Conflict e) {
                if (attempt >= attempts) {
                    throw new FgoException(e.getMessage());
                }
                // Three intervals, then six: well past the one-a-second FGO asks for.
                lastCallAt = System.currentTimeMillis() + 3 * minIntervalMs * attempt - minIntervalMs;
            }
        }
    }

    /** FGO's 409 page, kept apart so {@link #send} can ask again. */
    private static class Conflict extends RuntimeException {
        Conflict(String message) {
            super(message);
        }
    }

    private JsonNode sendOnce(String path, Map<String, Object> body) {
        throttle();
        return http.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, res) -> {
                    String text = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (text.isBlank()) {
                        throw new FgoException("FGO " + path + ": răspuns gol (HTTP " + res.getStatusCode().value() + ")");
                    }
                    try {
                        return JSON.readTree(text);
                    } catch (JsonProcessingException e) {
                        // 17.09.2026: getstatus answered HTML on every run and the log said only
                        // "no suitable HttpMessageConverter" — the page itself is what tells why.
                        String page = text.replaceAll("(?s)<(script|style).*?</\\1>", " ")
                                .replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                        String message = "FGO " + path + ": HTTP " + res.getStatusCode().value()
                                + ", nu e JSON: " + page.substring(0, Math.min(300, page.length()));
                        if (res.getStatusCode().value() == 409) {
                            throw new Conflict(message);
                        }
                        throw new FgoException(message);
                    }
                });
    }

    private static FgoException failure(String path, JsonNode response) {
        return new FgoException("FGO " + path + ": " + response.path("Message").asText("fără mesaj"));
    }

    private void throttle() {
        long wait = lastCallAt + minIntervalMs - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallAt = System.currentTimeMillis();
    }

    /**
     * Buffered, so the body goes with a Content-Length: unbuffered, Java streams it chunked, which the FGO
     * test server does not decode (17.09.2026, every getstatus of WH 6 answered HTML; curl chunked → 500).
     */
    private static ClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new BufferingClientHttpRequestFactory(factory);
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
