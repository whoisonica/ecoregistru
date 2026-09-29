package ro.ecoregistru.service.notification;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.exception.EmailException;
import ro.ecoregistru.service.EmailService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static ro.ecoregistru.exception.ErrorMessageEnum.EMAIL_SEND_FAILED;

/**
 * O căsuță refuzată nu-i mai trimite pe ceilalți înapoi la coadă (29.09.2026).
 *
 * <p>Mementoul scrie steagul de „trimis" doar dacă apelul de aici reușește. Când prima adresă
 * arunca, bucla se oprea, steagul rămânea jos și a doua zi mailul pleca din nou la toți cei care îl
 * primiseră — în fiecare dimineață, cât timp o adresă era greșită.
 */
class EmailNotificationServiceTest {

    private final EmailService emailService = Mockito.mock(EmailService.class);
    private final EmailNotificationService service = new EmailNotificationService(emailService);

    private final ReportingDeadline deadline = ReportingDeadline.builder()
            .reportType(ReportType.SIM_ANNUAL).dueDate(LocalDate.of(2027, 3, 15))
            .status(DeadlineStatus.UPCOMING).build();

    @Test
    void oneBadAddressDoesNotFailTheOthers() {
        Mockito.doThrow(new EmailException(EMAIL_SEND_FAILED))
                .when(emailService).send(eq("gresit@exemplu.ro"), anyString(), anyString(), any());

        assertThatCode(() -> service.sendDeadlineReminder(deadline,
                List.of("gresit@exemplu.ro", "bun@exemplu.ro"), 5)).doesNotThrowAnyException();

        Mockito.verify(emailService).send(eq("bun@exemplu.ro"), anyString(), eq("mail/deadline_reminder"), any());
    }

    /** Dacă nu pleacă niciunul, apelul pică: steagul rămâne jos și mâine se reîncearcă. */
    @Test
    void nobodyReachedStillFails() {
        Mockito.doThrow(new EmailException(EMAIL_SEND_FAILED))
                .when(emailService).send(anyString(), anyString(), anyString(), any());

        assertThatThrownBy(() -> service.sendDeadlineReminder(deadline,
                List.of("a@exemplu.ro", "b@exemplu.ro"), 5)).isInstanceOf(EmailException.class);
        Mockito.verify(emailService, Mockito.times(2)).send(anyString(), anyString(), anyString(), any());
    }
}
