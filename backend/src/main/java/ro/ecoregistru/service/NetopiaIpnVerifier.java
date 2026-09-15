package ro.ecoregistru.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;

/**
 * F3 of plata-abonamente.md — whether a payment notification (IPN) really comes from NETOPIA, the same
 * checks as NETOPIA's own SDKs ({@code netopiapayments/go-sdk}, {@code ipn.go}):
 *
 * <ul>
 *   <li>the {@code Verification-token} header is a JWT signed with NETOPIA's RSA key;</li>
 *   <li>{@code iss} is „NETOPIA Payments" and {@code aud} is our POS signature;</li>
 *   <li>{@code sub} is base64(SHA-512) of the raw body, so the body is read before any parsing.</li>
 * </ul>
 *
 * <p>Checked by hand, not with jjwt: jjwt 0.12 refuses RSA keys under 2048 bits for RS512, and the
 * sandbox certificate NETOPIA issued on 15.09.2026 is 1024 bits.
 */
@Component
public class NetopiaIpnVerifier {

    static final String ISSUER = "NETOPIA Payments";

    public static class InvalidNotification extends RuntimeException {
        public InvalidNotification(String message) {
            super(message);
        }
    }

    private final PublicKey publicKey;
    private final String posSignature;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public NetopiaIpnVerifier(@Value("${app.netopia.public-key:}") String publicKeyPem,
                              @Value("${app.netopia.pos-signature:}") String posSignature,
                              ObjectMapper objectMapper) {
        this(publicKeyPem.isBlank() ? null : publicKey(publicKeyPem), posSignature, objectMapper, Clock.systemUTC());
    }

    NetopiaIpnVerifier(PublicKey publicKey, String posSignature, ObjectMapper objectMapper, Clock clock) {
        this.publicKey = publicKey;
        this.posSignature = posSignature;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public boolean isConfigured() {
        return publicKey != null && posSignature != null && !posSignature.isBlank();
    }

    /** The parsed notification, once every check passed; otherwise {@link InvalidNotification} with the reason. */
    public JsonNode verify(String verificationToken, byte[] body) {
        if (!isConfigured()) {
            throw new InvalidNotification("lipsesc NETOPIA_PUBLIC_KEY sau NETOPIA_POS_SIGNATURE");
        }
        if (verificationToken == null || verificationToken.isBlank()) {
            throw new InvalidNotification("lipsește antetul Verification-token");
        }
        String[] parts = verificationToken.trim().split("\\.");
        if (parts.length != 3) {
            throw new InvalidNotification("Verification-token nu e un JWT");
        }

        String algorithm = switch (json(decode(parts[0])).path("alg").asText()) {
            case "RS256" -> "SHA256withRSA";
            case "RS384" -> "SHA384withRSA";
            case "RS512" -> "SHA512withRSA";
            default -> throw new InvalidNotification("algoritm de semnare neașteptat");
        };
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(decode(parts[2]))) {
                throw new InvalidNotification("semnătura nu e a NETOPIA");
            }
        } catch (GeneralSecurityException e) {
            throw new InvalidNotification("semnătura nu poate fi verificată: " + e.getMessage());
        }

        JsonNode claims = json(decode(parts[1]));
        if (!ISSUER.equals(claims.path("iss").asText())) {
            throw new InvalidNotification("emitent greșit");
        }
        JsonNode aud = claims.path("aud");
        String audience = aud.isArray() ? aud.path(0).asText() : aud.asText();
        if (!posSignature.equals(audience)) {
            throw new InvalidNotification("notificarea e pentru alt punct de vânzare");
        }
        long now = clock.instant().getEpochSecond();
        if (claims.has("exp") && now >= claims.path("exp").asLong()) {
            throw new InvalidNotification("Verification-token expirat");
        }
        if (claims.has("nbf") && now < claims.path("nbf").asLong()) {
            throw new InvalidNotification("Verification-token încă nevalabil");
        }
        if (!sha512Base64(body).equals(claims.path("sub").asText())) {
            throw new InvalidNotification("corpul notificării nu corespunde semnăturii");
        }
        return json(body);
    }

    /** NETOPIA's certificate (as downloaded) or a bare public key; literal „\n" from a config var too. */
    static PublicKey publicKey(String pem) {
        String text = pem.replace("\\n", "\n").trim();
        try {
            if (text.contains("BEGIN CERTIFICATE")) {
                return CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII)))
                        .getPublicKey();
            }
            String base64 = text.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("NETOPIA_PUBLIC_KEY nu e un certificat sau o cheie publică RSA", e);
        }
    }

    static String sha512Base64(byte[] body) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decode(String base64Url) {
        try {
            return Base64.getUrlDecoder().decode(base64Url);
        } catch (IllegalArgumentException e) {
            throw new InvalidNotification("Verification-token nu e un JWT");
        }
    }

    private JsonNode json(byte[] bytes) {
        try {
            return objectMapper.readTree(bytes);
        } catch (IOException e) {
            throw new InvalidNotification("JSON invalid");
        }
    }
}
