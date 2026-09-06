package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.util.List;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The 60-day authorization warning as it actually leaves the building: real
 * {@code EmailNotificationService}, real {@code EmailService}, real Thymeleaf, only the SMTP
 * transport mocked.
 *
 * <p>Why this exists next to {@link PartnerAuthorizationAlertIT}, which mocks the notification
 * service entirely: that one proves <em>when</em> we write, this one proves <em>what</em> comes
 * out. A template that reads {@code ${partnerNume}} while the service sets {@code partnerName}
 * renders an empty cell and passes every other test in the project — the rendered artifact is the
 * only place it shows, which is regula de lucru 5 applied to an e-mail instead of a PDF.
 */
// Mocking JavaMailSender leaves the actuator's mail health contributor without a
// JavaMailSenderImpl to inspect, and it fails the context with "Beans must not be empty". The
// check has nothing to do with what is under test here.
@SpringBootTest(properties = "management.health.mail.enabled=false")
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PartnerAuthorizationMailIT {

    @Autowired NotificationService notificationService;

    @MockBean JavaMailSender mailSender;

    @BeforeEach
    void stubMessageFactory() {
        // A real MimeMessage so the helper can fill it; only the sending is a no-op.
        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(i -> new JavaMailSenderImpl().createMimeMessage());
    }

    private Partner partner() {
        return Partner.builder()
                .name("Hamburger Recycling Romania SRL")
                .cui("RO12345678")
                .authorizationNumber("AM 214 din 12.03.2024")
                .authorizationExpiry(LocalDate.of(2026, 11, 3))
                .type(PartnerType.COLLECTOR).client(true).active(true)
                .build();
    }

    private String sendAndCaptureHtml(Partner p, long daysUntil) {
        notificationService.sendPartnerAuthorizationWarning(p, List.of("client@exemplu.ro"), daysUntil);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        try {
            // Without this the Content-Type headers are not written yet and every part still
            // claims text/plain — the transport is what normally triggers it.
            message.saveChanges();
        } catch (Exception e) {
            throw new IllegalStateException("could not finalize the message", e);
        }
        return html(message);
    }

    /** Walks the MIME tree for the text/html body the helper wrote. */
    private String html(Part part) {
        try {
            if (part.isMimeType("text/html")) {
                return (String) part.getContent();
            }
            if (part.getContent() instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    String found = html(multipart.getBodyPart(i));
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("could not read the message body", e);
        }
    }

    @Test
    void theWarningCarriesThePartnerTheDateAndTheReasonItMatters() {
        String body = sendAndCaptureHtml(partner(), 60);

        assertThat(body)
                .contains("Hamburger Recycling Romania SRL")
                .contains("RO12345678")
                .contains("AM 214 din 12.03.2024")
                .contains("03.11.2026")
                .contains("expiră în 60 de zile")
                // The legal reason, so the reader does not file this as the partner's problem.
                .contains("art. 23 alin. (1)");

        // Nothing left unrendered: a leaked th: attribute means the template never ran.
        assertThat(body).doesNotContain("th:text").doesNotContain("th:if");
    }

    /** The subject names the partner — a subject without a name is a subject nobody opens twice. */
    @Test
    void theSubjectNamesThePartner() throws Exception {
        Partner p = partner();
        notificationService.sendPartnerAuthorizationWarning(p, List.of("client@exemplu.ro"), 30);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        Mockito.verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getSubject())
                .contains("Hamburger Recycling Romania SRL")
                .contains("expiră în 30 de zile");
    }

    /** Under 20 the noun loses its "de": "în 5 zile", not "în 5 de zile". */
    @Test
    void theWordingAgreesWithSmallNumbers() {
        String body = sendAndCaptureHtml(partner(), 5);

        assertThat(body).contains("expiră în 5 zile").doesNotContain("5 de zile");
    }

    /**
     * A partner with only a name on file still produces a readable mail: the optional rows drop
     * out instead of printing empty labels. Regula de lucru 1 — a gap shows as a gap.
     */
    @Test
    void theOptionalRowsDisappearWhenTheyAreNotOnFile() {
        Partner bare = Partner.builder()
                .name("Transport Rapid SRL")
                .authorizationExpiry(LocalDate.of(2026, 10, 1))
                .carrier(true).active(true)
                .build();

        String body = sendAndCaptureHtml(bare, 27);

        assertThat(body).contains("Transport Rapid SRL").contains("01.10.2026");
        assertThat(body).doesNotContain(">CUI<").doesNotContain(">Autorizație<");
    }
}
