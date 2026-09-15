package ro.ecoregistru.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.service.NetopiaIpnVerifier;

import java.util.Map;

/**
 * F3 of plata-abonamente.md, first piece — where NETOPIA sends the payment result. Public (NETOPIA has
 * no session), trusted only after {@link NetopiaIpnVerifier}.
 *
 * <p>For now it only logs what arrived, the card token masked. The sandbox probe of 15.09.2026 showed
 * no token in {@code operation/status}; this is how we see whether the notification carries one.
 * Nothing is stored yet.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing/netopia")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NetopiaIpnController {

    NetopiaIpnVerifier verifier;

    /** The raw bytes, not a DTO: the signature covers the body exactly as sent. */
    @PostMapping("/ipn")
    public ResponseEntity<Map<String, Object>> ipn(
            @RequestHeader(value = "Verification-token", required = false) String verificationToken,
            @RequestBody byte[] body) {
        JsonNode notification;
        try {
            notification = verifier.verify(verificationToken, body);
        } catch (NetopiaIpnVerifier.InvalidNotification e) {
            log.warn("Notificare Netopia respinsă: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("errorType", 1, "errorCode", "", "errorMessage", e.getMessage()));
        }
        JsonNode payment = notification.path("payment");
        String token = payment.path("binding").path("token").asText(payment.path("token").asText());
        log.info("Notificare Netopia: comanda {}, ntpID {}, stare {}, {} {}, card {}, token {}",
                notification.path("order").path("orderID").asText(), payment.path("ntpID").asText(),
                payment.path("status").asInt(), payment.path("amount").asText(), payment.path("currency").asText(),
                payment.path("instrument").path("panMasked").asText(), mask(token));
        return ResponseEntity.ok(Map.of("errorType", 0, "errorCode", "", "errorMessage", ""));
    }

    /** The token debits the card: never whole in a log. */
    static String mask(String token) {
        if (token == null || token.isBlank()) {
            return "lipsă";
        }
        return token.length() <= 8 ? "***" : token.substring(0, 4) + "…" + token.substring(token.length() - 4)
                + " (" + token.length() + ")";
    }
}
