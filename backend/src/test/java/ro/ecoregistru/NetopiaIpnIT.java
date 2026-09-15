package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F3 — adresa la care Netopia trimite rezultatul plății: deschisă fără login, dar crede numai ce e semnat.
 * Semnarea și verificările de câmpuri sunt în {@code NetopiaIpnVerifierTest}; aici e drumul HTTP.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class NetopiaIpnIT {

    static final String POS = "TEST-POS1-SIGN-ATUR-EXXX";
    static final KeyPair NETOPIA = rsa();
    static final String BODY = "{\"payment\":{\"ntpID\":\"1\",\"status\":3,\"binding\":{\"token\":\"eE10aklrbGxPRE05\"}},\"order\":{\"orderID\":\"wh-1\"}}";

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void netopia(DynamicPropertyRegistry registry) {
        registry.add("app.netopia.pos-signature", () -> POS);
        registry.add("app.netopia.public-key", () -> "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(NETOPIA.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----");
    }

    @Test
    void aSignedNotificationIsAcceptedWithoutASession() throws Exception {
        mockMvc.perform(post("/api/v1/billing/netopia/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Verification-token", token(BODY))
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorType").value(0));
    }

    @Test
    void anUnsignedOrAlteredNotificationIsRefused() throws Exception {
        mockMvc.perform(post("/api/v1/billing/netopia/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/billing/netopia/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Verification-token", token(BODY))
                        .content(BODY.replace("\"status\":3", "\"status\":5")))
                .andExpect(status().isBadRequest());
    }

    private static String token(String body) {
        try {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", "NETOPIA Payments");
            claims.put("aud", POS);
            claims.put("exp", Instant.now().getEpochSecond() + 60);
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
}
