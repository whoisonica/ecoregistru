package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.VerificationRecord;
import ro.ecoregistru.enums.VerificationRecordType;
import ro.ecoregistru.exception.EmailException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.VerificationRecordRepository;
import ro.ecoregistru.service.EmailService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA de lansare, generator — G33: ce se întâmplă cu invitația când mailul nu pleacă sau când linkul
 * a expirat. E primul pas al oricărui client nou (cerere de cont → aprobare → invitație → parolă).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class InvitationFailureIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository appUserRepository;
    @Autowired VerificationRecordRepository verificationRecordRepository;
    @MockitoBean EmailService emailService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateToken(appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
    }

    /**
     * SMTP cade exact la invitație. Contul se creează (bine: invitația se poate retrimite), dar
     * răspunsul e același ca la o invitație trimisă, iar eroarea rămâne doar în log:
     * {@code AuthenticationService.java:249-251} o prinde cu {@code log.error}, iar Sentry raportează
     * numai 500-urile ({@code sentry.exception-resolver-order}, fără {@code sentry-logback}). Adminul
     * — sau noi, la aprobarea unei cereri de cont — credem că omul are linkul; omul nu primește nimic.
     *
     * <p>Așteptat: răspunsul spune că mailul n-a plecat. Numele câmpului ({@code inviteEmailSent}) e
     * propunerea reparației, nu un contract existent.
     */
    @Test
    void anInvitationWhoseMailFailedSaysSo() throws Exception {
        doThrow(new EmailException(ErrorMessageEnum.EMAIL_SEND_FAILED))
                .when(emailService).sendInviteEmail(any(), anyString(), anyInt());

        invite(uniqueEmail("fara-mail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteEmailSent", is(false)));
    }

    /** BUG-038, retrimiterea: un 204 citea „a plecat”; acum mailul eșuat e o eroare pe ecran. */
    @Test
    void aResendWhoseMailFailedIsAnError() throws Exception {
        String id = objectMapper.readTree(invite(uniqueEmail("retrimis"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        doThrow(new EmailException(ErrorMessageEnum.EMAIL_SEND_FAILED))
                .when(emailService).sendInviteEmail(any(), anyString(), anyInt());

        mockMvc.perform(post("/api/v1/users/" + id + "/resend-invite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("email.send.failed")));
    }

    /** Controlul de mai sus: contul există, e în așteptare, deci „Retrimite invitația” îl poate salva. */
    @Test
    void anInvitationWhoseMailFailedStillCreatesThePendingAccount() throws Exception {
        doThrow(new EmailException(ErrorMessageEnum.EMAIL_SEND_FAILED))
                .when(emailService).sendInviteEmail(any(), anyString(), anyInt());
        String email = uniqueEmail("fara-mail");

        invite(email).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("PENDING_INVITE")));

        assertThat(appUserRepository.findByEmail(email)).get().extracting(AppUser::isEnabled).isEqualTo(false);
    }

    /** Linkul de invitație trăiește 7 zile. După aceea parola nu se mai poate alege cu el, iar contul rămâne închis. */
    @Test
    void anExpiredInvitationLinkIsRefusedAndTheAccountStaysClosed() throws Exception {
        String email = uniqueEmail("expirat");
        invite(email).andExpect(status().isOk());
        String code = lastInviteCode();
        var record = verificationRecordRepository
                .findByCodeAndVerificationRecordType(
                        ro.ecoregistru.service.AuthenticationService.fingerprint(code),
                        VerificationRecordType.RESET_PASSWORD).orElseThrow();
        record.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        verificationRecordRepository.save(record);

        choosePassword(code).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("verification.code.expired")));

        assertThat(appUserRepository.findByEmail(email)).get().extracting(AppUser::isEnabled).isEqualTo(false);
    }

    /** Invitația expirată se retrimite: linkul nou merge, cel vechi nu mai merge deloc. */
    @Test
    void aResentInvitationWorksAndTheOldLinkIsDead() throws Exception {
        String email = uniqueEmail("retrimis");
        String id = objectMapper.readTree(invite(email).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        String oldCode = lastInviteCode();

        mockMvc.perform(post("/api/v1/users/" + id + "/resend-invite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        String newCode = lastInviteCode();
        assertThat(newCode).isNotEqualTo(oldCode);

        choosePassword(oldCode).andExpect(status().isNotFound());
        choosePassword(newCode).andExpect(status().isOk());
        assertThat(appUserRepository.findByEmail(email)).get().extracting(AppUser::isEnabled).isEqualTo(true);
    }

    /**
     * Codul din mail nu se regăsește nicăieri în bază — stă numai amprenta lui.
     *
     * <p>Cât timp coloana ținea valoarea din link, un dump al bazei sau un backup scurs era o listă
     * de preluări de conturi gata făcute: cine îl citea alegea parola pe orice cont cu o invitație
     * sau o resetare nefolosită. Proba caută codul în <b>toate</b> rândurile, nu doar în al ei, ca
     * un al doilea loc de scriere (o invitație nouă, alt tip de cod) să cadă și el aici.
     *
     * <p>Perechea ei — că linkul tot funcționează — e
     * {@link #aResentInvitationWorksAndTheOldLinkIsDead()}: acolo {@code choosePassword(newCode)}
     * cere 200. Fără ea, „nu se mai potrivește nimic” ar trece drept reparație.
     */
    @Test
    void theMailedCodeIsNowhereInTheDatabase() throws Exception {
        String email = uniqueEmail("amprenta");
        invite(email).andExpect(status().isOk());
        String code = lastInviteCode();

        assertThat(verificationRecordRepository.findAll())
                .as("codul din mail nu are voie să stea în clar în nicio coloană `code`")
                .noneMatch(r -> code.equals(r.getCode()));

        assertThat(verificationRecordRepository
                .findByCodeAndVerificationRecordType(
                        ro.ecoregistru.service.AuthenticationService.fingerprint(code),
                        VerificationRecordType.RESET_PASSWORD))
                .as("dar amprenta lui da, altfel linkul n-ar mai deschide nimic")
                .isPresent();

        // Control pozitiv al măsurătorii: scriem noi un rând cu codul în clar — exact forma pe care
        // o avea baza până la V64 — și cerem ca prima verificare să-l vadă. Fără pasul ăsta,
        // „nu s-a găsit nimic în clar" ar fi putut însemna doar că proba se uită unde nu trebuie.
        VerificationRecord inTheClear = verificationRecordRepository.save(VerificationRecord.builder()
                .user(appUserRepository.findByEmail(email).orElseThrow())
                .code(code)
                .verificationRecordType(VerificationRecordType.RESET_PASSWORD)
                .confirmed(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build());
        assertThat(verificationRecordRepository.findAll())
                .as("măsurătoarea chiar vede un cod în clar când există unul")
                .anyMatch(r -> code.equals(r.getCode()));
        verificationRecordRepository.delete(inTheClear);
    }

    // --- helpers ---

    private ResultActions invite(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email, "role", "OPERATOR", "firstName", "Test", "lastName", "Invitat"))));
    }

    private ResultActions choosePassword(String code) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "code", code, "password", "Parola2026sigura", "confirmPassword", "Parola2026sigura"))));
    }

    private String lastInviteCode() {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendInviteEmail(any(), code.capture(), anyInt());
        return code.getValue();
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro";
    }
}
