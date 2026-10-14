package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.repository.AccountRequestRepository;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A3 (todo-reparatii-2809) — cine află de o cerere de cont nouă.
 *
 * <p>Formularul promite „în 1–2 zile lucrătoare”, dar cererea doar se salva: fără mail, iar banda
 * din „Clienți” se vede numai dacă cineva deschide pagina. Acum pleacă un mail pe adresa platformei
 * ({@code app.mail.platform-inbox}, implicit adresa de trimitere — nu una personală, repo-ul e
 * public). Un mail căzut nu strică cererea, iar capcana de roboți nu trimite nimic.
 */
// Mocking JavaMailSender leaves the actuator's mail health contributor without a
// JavaMailSenderImpl to inspect (see PartnerAuthorizationMailIT).
@SpringBootTest(properties = "management.health.mail.enabled=false")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AccountRequestNoticeIT {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRequestRepository accountRequestRepository;
    @Value("${app.frontend-base-url}") String frontendBaseUrl;

    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void stubMessageFactory() {
        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(i -> new JavaMailSenderImpl().createMimeMessage());
    }

    @Test
    void aNewRequestIsMailedToThePlatformInbox() throws Exception {
        String cui = "RO" + TestCui.random();
        mockMvc.perform(submission("Tamplaria Noua SRL", cui, null)).andExpect(status().isAccepted());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        assertThat(message.getRecipients(Message.RecipientType.TO)).extracting(Object::toString)
                .containsExactly("contact@wastehouse.ro");
        assertThat(message.getSubject()).contains("Tamplaria Noua SRL");
    }

    /** 14.10.2026 — aplicația se cheamă eco360: expeditorul și logoul din cap, luat de lângă aplicația web. */
    @Test
    void theMailIsSignedEco360WithItsLogo() throws Exception {
        mockMvc.perform(submission("Sigla Noua SRL", "RO" + TestCui.random(), null)).andExpect(status().isAccepted());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        assertThat(((InternetAddress) message.getFrom()[0]).getPersonal()).isEqualTo("eco360");
        assertThat(html(message.getContent()))
                .contains("<img src=\"" + frontendBaseUrl + "/brand/eco360-mail.png\"")
                .contains("alt=\"eco360\"")
                .doesNotContain("WasteHouse").doesNotContain("wastehouse-mark");
    }

    /** Textul HTML din mailul construit de {@code MimeMessageHelper} (multipart în multipart). */
    private static String html(Object content) throws Exception {
        if (content instanceof String text) return text;
        Multipart multipart = (Multipart) content;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) out.append(html(multipart.getBodyPart(i).getContent()));
        return out.toString();
    }

    @Test
    void theHoneypotSendsNothing() throws Exception {
        mockMvc.perform(submission("Robotel SRL", "RO" + TestCui.random(), "https://spam.example"))
                .andExpect(status().isAccepted());

        Mockito.verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void aMailThatFailsDoesNotLoseTheRequest() throws Exception {
        Mockito.doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));
        String cui = "RO" + TestCui.random();

        mockMvc.perform(submission("Cerere Salvata SRL", cui, null)).andExpect(status().isAccepted());

        assertThat(accountRequestRepository.findAllByOrderByCreatedAtDesc())
                .anyMatch(r -> cui.equals(r.getCui()));
    }

    private MockHttpServletRequestBuilder submission(String name, String cui, String website) {
        String body = """
                {"companyName": "%s", "cui": "%s", "companyType": "GENERATOR", "sizeTier": 2,
                 "contactName": "Ion Popescu", "contactEmail": "ion.popescu@example.ro",
                 "contactPhone": "0740111222"%s}
                """.formatted(name, cui, website == null ? "" : ", \"website\": \"" + website + "\"");
        return post("/api/v1/account-requests").contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
