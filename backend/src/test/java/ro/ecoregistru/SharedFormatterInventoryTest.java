package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Niciun {@link Format} static în generatoarele de documente.
 *
 * <p>{@code DecimalFormat}, {@code SimpleDateFormat} şi fraţii lor <b>nu sunt sincronizaţi</b> —
 * javadocul lor o spune —, iar generatoarele sunt componente singleton: un câmp
 * {@code static final DecimalFormat KG} e împărţit de toate cererile. Două descărcări în aceeaşi
 * clipă pot ieşi cu o cifră greşită de kilograme pe un document depus la agenţie, fără nimic în
 * log şi fără nimic de reprodus a doua zi. Cinci generatoare aveau câte unul (20.09.2026).
 *
 * <p>Gardă în tiparul lui {@link IsolationInventoryTest}: <b>citeşte lista din cod</b>, deci un
 * generator nou intră singur sub regulă. Reflexie curată, fără context Spring.
 *
 * <p>Regula e „nu-l ţine", nu „construieşte-l la fiecare apel": un formator local, chiar şi unul pe
 * document, e la fel de bun. Construcţia costă nimic pe lângă un PDF.
 */
class SharedFormatterInventoryTest {

    private static final List<String> PACKAGES = List.of(
            "ro.ecoregistru.service", "ro.ecoregistru.util", "ro.ecoregistru.controller");

    @Test
    void noGeneratorKeepsAFormatterInAStaticField() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        List<String> shared = new ArrayList<>();
        for (String pkg : PACKAGES) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                Class<?> type = Class.forName(bd.getBeanClassName());
                for (Field f : type.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) && Format.class.isAssignableFrom(f.getType())) {
                        shared.add(type.getSimpleName() + "." + f.getName()
                                + " (" + f.getType().getSimpleName() + ")");
                    }
                }
            }
        }

        assertThat(shared)
                .describedAs("java.text.Format nu e sincronizat; ţinut static pe un singleton, "
                        + "două cereri simultane îl folosesc pe acelaşi. Construieşte-l în metodă.")
                .isEmpty();
    }
}
