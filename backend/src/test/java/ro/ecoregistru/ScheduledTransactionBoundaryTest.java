package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 15.09.2026 — pe producție, trei joburi programate rulau fără tranzacție: metoda {@code @Scheduled}
 * chema o metodă {@code @Transactional} din aceeași clasă, iar apelul intern ocolește proxy-ul Spring.
 * Testele chemau direct metoda tranzacțională (prin proxy), deci treceau. Ștergerea datelor șoferilor
 * cădea în fiecare noapte, iar fanioanele „mementou trimis” nu se salvau.
 *
 * <p>Regula: o clasă care are metode {@code @Transactional} și o metodă {@code @Scheduled} pune
 * tranzacția și pe metoda programată (sau pe clasă). O clasă fără nicio tranzacție (ex. un scheduler
 * care deleagă altui bean) nu e atinsă.
 */
class ScheduledTransactionBoundaryTest {

    @Test
    void everyScheduledEntryPointOpensItsOwnTransaction() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        List<String> scheduled = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("ro.ecoregistru")) {
            Class<?> type = Class.forName(candidate.getBeanClassName());
            boolean classTx = type.isAnnotationPresent(Transactional.class);
            boolean anyMethodTx = Arrays.stream(type.getDeclaredMethods())
                    .anyMatch(m -> m.isAnnotationPresent(Transactional.class));
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Scheduled.class)) {
                    continue;
                }
                scheduled.add(type.getSimpleName() + "." + method.getName());
                if (anyMethodTx && !classTx && !method.isAnnotationPresent(Transactional.class)) {
                    offenders.add(type.getSimpleName() + "." + method.getName());
                }
            }
        }

        // Controlul pozitiv: scanarea chiar vede joburile, altfel lista goală de mai jos n-ar dovedi nimic.
        assertThat(scheduled).contains("DriverDataRetentionScheduler.runDaily", "BillingScheduler.runDaily");
        assertThat(offenders).isEmpty();
    }
}
