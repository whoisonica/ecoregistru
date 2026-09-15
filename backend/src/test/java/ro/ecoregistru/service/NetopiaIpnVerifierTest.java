package ro.ecoregistru.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F3 — verificarea notificării Netopia, cu un JWT semnat aici, la fel ca în testul SDK-ului lor Python
 * ({@code tests/test_ipn.py}): RS512, {@code iss}, {@code aud} = POS, {@code sub} = base64(SHA-512(corp)).
 */
class NetopiaIpnVerifierTest {

    static final String POS = "TEST-POS1-SIGN-ATUR-EXXX";
    static final Instant NOW = Instant.parse("2026-09-15T09:00:00Z");
    static final byte[] BODY = """
            {"payment":{"ntpID":"3019383","status":3,"amount":1,"currency":"RON","binding":{"token":"eE10aklrbGxPRE05"}},"order":{"orderID":"wh-1"}}"""
            .getBytes(StandardCharsets.UTF_8);

    static final KeyPair NETOPIA = rsa();
    final ObjectMapper objectMapper = new ObjectMapper();
    final NetopiaIpnVerifier verifier = new NetopiaIpnVerifier(NETOPIA.getPublic(), POS, objectMapper,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void aNotificationSignedByNetopiaIsReadBack() {
        assertThat(verifier.verify(token(NETOPIA.getPrivate(), claims()), BODY).at("/payment/binding/token").asText())
                .isEqualTo("eE10aklrbGxPRE05");
    }

    @Test
    void aChangedBodyIsRefused() {
        byte[] other = new String(BODY, StandardCharsets.UTF_8).replace("\"status\":3", "\"status\":5")
                .getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> verifier.verify(token(NETOPIA.getPrivate(), claims()), other))
                .hasMessageContaining("corpul");
    }

    @Test
    void anotherKeyIsRefused() {
        assertThatThrownBy(() -> verifier.verify(token(rsa().getPrivate(), claims()), BODY))
                .hasMessageContaining("semnătura");
    }

    @Test
    void anotherPointOfSaleIsRefusedAlsoAsAnArray() {
        Map<String, Object> claims = claims();
        claims.put("aud", List.of("ALT-POS"));
        assertThatThrownBy(() -> verifier.verify(token(NETOPIA.getPrivate(), claims), BODY))
                .hasMessageContaining("alt punct de vânzare");

        claims.put("aud", List.of(POS));
        assertThat(verifier.verify(token(NETOPIA.getPrivate(), claims), BODY)).isNotNull();
    }

    @Test
    void anotherIssuerIsRefused() {
        Map<String, Object> claims = claims();
        claims.put("iss", "Altcineva");
        assertThatThrownBy(() -> verifier.verify(token(NETOPIA.getPrivate(), claims), BODY))
                .hasMessageContaining("emitent");
    }

    @Test
    void anExpiredTokenIsRefused() {
        Map<String, Object> claims = claims();
        claims.put("exp", NOW.getEpochSecond());
        assertThatThrownBy(() -> verifier.verify(token(NETOPIA.getPrivate(), claims), BODY))
                .hasMessageContaining("expirat");
    }

    @Test
    void withoutHeaderOrConfigurationNothingIsTrusted() {
        assertThatThrownBy(() -> verifier.verify(null, BODY)).hasMessageContaining("Verification-token");
        NetopiaIpnVerifier unconfigured = new NetopiaIpnVerifier(null, "", objectMapper, Clock.systemUTC());
        assertThatThrownBy(() -> unconfigured.verify(token(NETOPIA.getPrivate(), claims()), BODY))
                .hasMessageContaining("NETOPIA_PUBLIC_KEY");
    }

    /** O variabilă de config cu „\n" scris literal, cum iese uneori din Heroku. */
    @Test
    void aPublicKeyPemWithEscapedNewlinesIsRead() {
        String base64 = Base64.getMimeEncoder().encodeToString(NETOPIA.getPublic().getEncoded()).replace("\r\n", "\\n");
        String pem = "-----BEGIN PUBLIC KEY-----\\n" + base64 + "\\n-----END PUBLIC KEY-----";
        assertThat(pem).contains("\\n").doesNotContain("\n");
        assertThat(NetopiaIpnVerifier.publicKey(pem)).isEqualTo(NETOPIA.getPublic());
    }

    // ─────────────────────────────────────────────────────────────────────────

    static Map<String, Object> claims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", NetopiaIpnVerifier.ISSUER);
        claims.put("aud", POS);
        claims.put("iat", NOW.getEpochSecond() - 5);
        claims.put("exp", NOW.getEpochSecond() + 60);
        claims.put("sub", NetopiaIpnVerifier.sha512Base64(BODY));
        return claims;
    }

    static String token(PrivateKey key, Map<String, Object> claims) {
        try {
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            ObjectMapper om = new ObjectMapper();
            String signingInput = b64.encodeToString(om.writeValueAsBytes(Map.of("alg", "RS512", "typ", "JWT")))
                    + "." + b64.encodeToString(om.writeValueAsBytes(claims));
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initSign(key);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + b64.encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static KeyPair rsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
