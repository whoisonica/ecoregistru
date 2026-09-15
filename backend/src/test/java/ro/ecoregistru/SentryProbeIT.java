package ro.ecoregistru;

import io.sentry.Sentry;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.PlatformDiagnosticsController.SentryProbeException;
import ro.ecoregistru.repository.AppUserRepository;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0.6 — proba de 500 pentru Sentry. Pe producţie se vede evenimentul în Sentry; aici se probează
 * că endpointul chiar ajunge pe ramura care raportează, şi că nu-l poate apăsa nimeni altcineva.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SentryProbeIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;

    private String tokenOf(String email) {
        return jwtService.generateToken(appUserRepository.findByEmail(email).orElseThrow());
    }

    @Test
    void thePlatformAdminGetsA500ThatIsReportedToSentry() throws Exception {
        try (MockedStatic<Sentry> sentry = Mockito.mockStatic(Sentry.class, Mockito.CALLS_REAL_METHODS)) {
            mockMvc.perform(post("/api/v1/platform/sentry-probe")
                            .header("Authorization", "Bearer " + tokenOf("platform@ecoregistru.ro")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$['error-code']").value("internal.error"))
                    // Same contract as every other 500: nothing of the exception reaches the client.
                    .andExpect(content().string(not(containsString("Sentry probe"))));

            sentry.verify(() -> Sentry.captureException(any(SentryProbeException.class)));
        }
    }

    @Test
    void aCompanyAdminIsRefusedAndNothingIsReported() throws Exception {
        try (MockedStatic<Sentry> sentry = Mockito.mockStatic(Sentry.class, Mockito.CALLS_REAL_METHODS)) {
            mockMvc.perform(post("/api/v1/platform/sentry-probe")
                            .header("Authorization", "Bearer " + tokenOf("admin@demo.ro")))
                    .andExpect(status().isForbidden());

            sentry.verify(() -> Sentry.captureException(any()), Mockito.never());
        }
    }
}
