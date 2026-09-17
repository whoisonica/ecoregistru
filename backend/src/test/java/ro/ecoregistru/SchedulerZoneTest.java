package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import ro.ecoregistru.service.BillingScheduler;
import ro.ecoregistru.service.ConsultantDigestScheduler;
import ro.ecoregistru.service.DeadlineAlertScheduler;
import ro.ecoregistru.service.DeadlineCalendarScheduler;
import ro.ecoregistru.service.DriverAttestationAlertScheduler;
import ro.ecoregistru.service.DriverDataRetentionScheduler;
import ro.ecoregistru.service.NaturalPersonRetentionScheduler;
import ro.ecoregistru.service.PartnerAuthorizationAlertScheduler;
import ro.ecoregistru.service.VehicleExpiryAlertScheduler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scanarea din 17.09.2026: șase programări n-aveau {@code zone}, iar Heroku rulează pe UTC — mementourile plecau la
 * 10:00 vara, iar ziua lor era cea din UTC. Fiecare {@code @Scheduled} din aplicație merge pe ora României.
 */
class SchedulerZoneTest {

    private static final List<Class<?>> SCHEDULERS = List.of(
            BillingScheduler.class, ConsultantDigestScheduler.class, DeadlineAlertScheduler.class,
            DeadlineCalendarScheduler.class, DriverAttestationAlertScheduler.class, DriverDataRetentionScheduler.class,
            NaturalPersonRetentionScheduler.class, PartnerAuthorizationAlertScheduler.class,
            VehicleExpiryAlertScheduler.class);

    @Test
    void everyScheduledJobRunsOnRomanianTime() {
        List<Method> scheduled = SCHEDULERS.stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(Scheduled.class))
                .toList();
        assertThat(scheduled).hasSize(SCHEDULERS.size());
        assertThat(scheduled).allSatisfy(m ->
                assertThat(m.getAnnotation(Scheduled.class).zone()).as(m.toString()).isEqualTo("Europe/Bucharest"));
    }
}
