package ro.ecoregistru.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.DeviceSession;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.repository.DeviceSessionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * G2 — notificările pe telefon, prin Expo Push (decizia proprietarului, 16.09.2026).
 *
 * <p><b>Un plus, nu un canal.</b> Mailul rămâne ce hotărăște dacă o alertă „a plecat” (fanioanele din
 * schedulere). Push-ul pleacă numai după un mail reușit, deci o singură dată pe alertă, și nu aruncă
 * niciodată: Expo căzut nu oprește nicio alertă și nu face nicio alertă să se repete.
 *
 * <p><b>Fără date personale în notificare</b> (todo-mobil G2): numele firmei și al partenerului da —
 * sunt firme —, nimic despre șoferi, CNP sau cantități. Expo e sub-procesor în SUA; conținutul trece pe
 * ecranul blocat al telefonului.
 *
 * <p><b>Cui.</b> Sesiunile vii de dispozitiv ale oamenilor care primesc mailul, cu token declarat.
 * Un token pe care Expo îl numește {@code DeviceNotRegistered} se șterge.
 *
 * <p>Oprit implicit ({@code app.push.enabled}); nicio cheie nu e obligatorie pentru Expo, dar
 * {@code app.push.access-token} se trimite când e setat („Enhanced Push Security” din contul Expo).
 */
@Slf4j
@Component
public class PushNotifier {

    /** O notificare, cu ecranul pe care îl deschide în aplicație (`mobile/src/push.ts`, lista închisă). */
    public record Message(String title, String body, String screen) {}

    /** Expo primește cel mult 100 de notificări într-o cerere. */
    static final int BATCH = 100;

    private final RestClient http;
    private final DeviceSessionRepository deviceSessions;
    private final boolean enabled;
    private final String accessToken;

    @Autowired
    public PushNotifier(DeviceSessionRepository deviceSessions,
                        @Value("${app.push.enabled:false}") boolean enabled,
                        @Value("${app.push.base-url:https://exp.host}") String baseUrl,
                        @Value("${app.push.access-token:}") String accessToken) {
        this(RestClient.builder().baseUrl(baseUrl).requestFactory(timeouts()).build(), deviceSessions, enabled, accessToken);
    }

    PushNotifier(RestClient http, DeviceSessionRepository deviceSessions, boolean enabled, String accessToken) {
        this.http = http;
        this.deviceSessions = deviceSessions;
        this.enabled = enabled;
        this.accessToken = accessToken;
    }

    /** Trimite notificarea pe telefoanele acestor oameni. Nu aruncă; întoarce câte au fost primite de Expo. */
    public int send(Collection<AppUser> users, Message message) {
        if (!enabled || users.isEmpty()) {
            return 0;
        }
        try {
            List<String> tokens = deviceSessions.findLiveWithPushToken(users, Instant.now()).stream()
                    .map(DeviceSession::getPushToken).distinct().toList();
            int accepted = 0;
            for (int from = 0; from < tokens.size(); from += BATCH) {
                accepted += sendBatch(tokens.subList(from, Math.min(from + BATCH, tokens.size())), message);
            }
            return accepted;
        } catch (Exception e) {
            log.warn("Push notification '{}' not sent: {}", message.title(), e.getMessage());
            return 0;
        }
    }

    private int sendBatch(List<String> tokens, Message message) {
        List<Map<String, Object>> payload = tokens.stream().map(to -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("to", to);
            m.put("title", message.title());
            m.put("body", message.body());
            m.put("data", Map.of("screen", message.screen()));
            m.put("channelId", "default");
            return m;
        }).toList();

        RestClient.RequestBodySpec request = http.post().uri("/--/api/v2/push/send")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isBlank()) {
            request = request.header("Authorization", "Bearer " + accessToken);
        }
        JsonNode response = request.body(payload).retrieve().body(JsonNode.class);

        // Biletele vin în ordinea notificărilor trimise.
        JsonNode tickets = response == null ? null : response.get("data");
        if (tickets == null || !tickets.isArray()) {
            return 0;
        }
        int accepted = 0;
        List<String> gone = new ArrayList<>();
        for (int i = 0; i < tickets.size() && i < tokens.size(); i++) {
            JsonNode ticket = tickets.get(i);
            if ("ok".equals(ticket.path("status").asText())) {
                accepted++;
            } else if ("DeviceNotRegistered".equals(ticket.path("details").path("error").asText())) {
                gone.add(tokens.get(i));
            }
        }
        if (!gone.isEmpty()) {
            deviceSessions.clearPushTokens(gone);
        }
        return accepted;
    }

    // ── textele ─────────────────────────────────────────────────────────────

    /** Termenul unei firme, pentru oamenii ei. Eticheta e scurtă: pe ecranul blocat încap două rânduri. */
    public static Message deadline(ReportingDeadline deadline, long daysUntil) {
        String when = daysUntil <= 0 ? "scadent azi" : daysUntil == 1 ? "scadent mâine" : "scadent în " + daysUntil + " zile";
        return new Message(deadline.getCompany().getName() + ": termen " + when,
                shortLabel(deadline.getReportType()), "termene");
    }

    /** Autorizația unui partener; ecranul „A venit controlul” o arată pe rândul ei. */
    public static Message partnerAuthorization(Partner partner, long daysUntil) {
        String when = daysUntil <= 0 ? "expiră azi" : daysUntil == 1 ? "expiră mâine"
                : "expiră în " + daysUntil + (daysUntil >= 20 ? " de zile" : " zile");
        return new Message("Autorizația partenerului " + partner.getName() + " " + when,
                "Verifică autorizația înainte de următoarea predare.", "control");
    }

    /** Rezumatul cabinetului: câte termene, nu care — detaliile sunt în mail și pe web. */
    public static Message consultantDigest(String consultancyName, int deadlines) {
        String count = deadlines == 1 ? "1 termen" : deadlines + (deadlines >= 20 ? " de termene" : " termene");
        return new Message(consultancyName + ": " + count + " în următoarele 7 zile",
                "Lista e în mailul de azi.", "acasa");
    }

    /** Numele documentului, fără temeiul legal din mail. */
    static String shortLabel(ReportType type) {
        return switch (type) {
            case SIM_ANNUAL -> "Evidența gestiunii deșeurilor (anuală)";
            case AFM_MONTHLY -> "Declarația AFM lunară";
            case AFM_QUARTERLY -> "Declarația AFM trimestrială";
            case AFM_ANNUAL -> "Declarația AFM anuală";
            case PACKAGING_ANNUAL -> "Anexa 1 Ambalaje";
            case PACKAGING_ANNEX3 -> "Anexa 3 Ambalaje";
            case APM_ANNUAL_APRIL -> "Raportarea anuală la APM (30 aprilie)";
            case APM_ANNUAL_MAY -> "Programul de prevenire a deșeurilor";
            case OTHER -> "Raportare";
        };
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return factory;
    }
}
