package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Runs {@link BillingRunService} every morning, on Romanian time: a period starts on a calendar day
 * in Romania, and Heroku's clock is UTC. Daily rather than on the 1st, because every subscription's
 * period starts on its own start date (decision of 15.09.2026).
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingScheduler {

    BillingRunService billingRunService;

    @Scheduled(cron = "${app.billing.cron:0 30 6 * * *}", zone = "Europe/Bucharest")
    public void runDaily() {
        billingRunService.run(LocalDate.now(BillingRunService.ZONE));
    }
}
