package ro.ecoregistru.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.ServiceUnavailableException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks a company up by CUI in ANAF's public registry of taxpayers, web service v9
 * (<a href="https://static.anaf.ro/static/10/Anaf/Informatii_R/Servicii_web/doc_WS_V9.txt">documentation</a>,
 * read 15.09.2026), so the partner form fills in the name, the registered address and the trade
 * register number instead of having them typed — the three rubrics that print on Anexa 3.
 *
 * <p>The service is public and needs no key, but the documentation allows "maxim 1 request pe
 * secundă" and warns that overloading the server "va fi pedepsită conform reglementărilor în
 * vigoare". Calls are therefore serialised and spaced by {@code app.anaf.min-interval-ms}, and a
 * company found is kept for a day: its registered data does not change between two openings of the
 * same form, and a user pressing the button twice must not become two requests.
 *
 * <p>A CUI ANAF does not know is not cached — a firm registered this morning should be found this
 * afternoon.
 */
@Slf4j
@Component
public class AnafClient {

    public record Company(String cui, String name, String address, String tradeRegisterNumber,
                          String caenCode, String county, String city, String registrationStatus,
                          boolean inactive) {}

    private record Cached(Instant at, Company company) {}

    /** The rule `CompanyService` applies to a CUI: 2–10 digits, with or without "RO". */
    private static final Pattern CUI = Pattern.compile("(?i)^(?:RO)?(\\d{2,10})$");
    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final int CACHE_MAX = 1_000;

    private final RestClient http;
    private final long minIntervalMs;
    private final Clock clock;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Duration lockWait;
    private long lastCallAt;

    @Autowired
    public AnafClient(@Value("${app.anaf.base-url}") String baseUrl,
                      @Value("${app.anaf.min-interval-ms:1000}") long minIntervalMs) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(timeouts()).build(), minIntervalMs,
                Clock.system(BUCHAREST), Duration.ofSeconds(5));
    }

    AnafClient(RestClient http, long minIntervalMs, Clock clock) {
        this(http, minIntervalMs, clock, Duration.ofSeconds(5));
    }

    AnafClient(RestClient http, long minIntervalMs, Clock clock, Duration lockWait) {
        this.http = http;
        this.minIntervalMs = minIntervalMs;
        this.clock = clock;
        this.lockWait = lockWait;
    }

    /** The digits of a CUI, or empty when it cannot be one. */
    static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        Matcher m = CUI.matcher(raw.replaceAll("\\s", ""));
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    /**
     * @return the company, or empty when ANAF has no taxpayer with this CUI
     * @throws BusinessException when the text cannot be a CUI — refused before asking ANAF
     * @throws ServiceUnavailableException when ANAF does not answer, or answers something unreadable
     */
    public Optional<Company> lookup(String rawCui) {
        String cui = normalize(rawCui).orElseThrow(() -> new BusinessException(ErrorMessageEnum.INVALID_CUI));
        Cached hit = cache.get(cui);
        if (hit != null && hit.at().plus(CACHE_TTL).isAfter(clock.instant())) {
            return Optional.of(hit.company());
        }
        Optional<Company> company = fetch(cui);
        company.ifPresent(c -> {
            if (cache.size() >= CACHE_MAX) {
                cache.clear();
            }
            cache.put(cui, new Cached(clock.instant(), c));
        });
        return company;
    }

    /**
     * One call at a time, but a request does not queue behind the others for longer than
     * {@code lockWait}: ANAF may take the whole read timeout to answer, and a synchronized method would
     * hold every waiting request thread — enough lookups from one tab to stall the API for everyone.
     */
    private Optional<Company> fetch(String cui) {
        boolean locked;
        try {
            locked = lock.tryLock(lockWait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            locked = false;
        }
        if (!locked) {
            throw new ServiceUnavailableException(ErrorMessageEnum.ANAF_UNAVAILABLE);
        }
        try {
            return fetchLocked(cui);
        } finally {
            lock.unlock();
        }
    }

    private Optional<Company> fetchLocked(String cui) {
        throttle();
        JsonNode body;
        try {
            body = http.post()
                    .uri("/PlatitorTvaRest/v9/tva")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(Map.of("cui", Long.parseLong(cui), "data", LocalDate.now(clock).toString())))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // Includes the HTML "Request Rejected" page ANAF's firewall answers with, which is not JSON.
            log.warn("ANAF lookup failed: {}", e.getMessage());
            throw new ServiceUnavailableException(ErrorMessageEnum.ANAF_UNAVAILABLE);
        }
        if (body == null || !body.path("found").isArray()) {
            log.warn("ANAF answered without a 'found' list");
            throw new ServiceUnavailableException(ErrorMessageEnum.ANAF_UNAVAILABLE);
        }
        JsonNode found = body.path("found");
        if (found.isEmpty()) {
            return Optional.empty();
        }
        JsonNode first = found.get(0);
        JsonNode general = first.path("date_generale");
        JsonNode office = first.path("adresa_sediu_social");
        return Optional.of(new Company(
                Optional.ofNullable(text(general, "cui")).orElse(cui),
                text(general, "denumire"),
                text(general, "adresa"),
                text(general, "nrRegCom"),
                text(general, "cod_CAEN"),
                text(office, "sdenumire_Judet"),
                text(office, "sdenumire_Localitate"),
                text(general, "stare_inregistrare"),
                first.path("stare_inactiv").path("statusInactivi").asBoolean(false)));
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? null : value.trim();
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

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return factory;
    }
}
