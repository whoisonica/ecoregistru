package ro.ecoregistru;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA de lansare, generator — G17. Heroku rulează pe UTC și aplicația nu fixează fusul JVM-ului
 * (niciun {@code TimeZone.setDefault}), deci {@code LocalDate.now()} fără argument dă ziua din UTC:
 * între 00:00 și 03:00, ora României, e ziua de ieri. {@code DeadlineService.today()} și
 * programările ({@code SchedulerZoneTest}) iau deja ora României; restul codului trebuie să facă la fel.
 *
 * <p>Inventar pe sursă, ca {@code SchedulerZoneTest}: fiecare zi de calendar citită în codul
 * aplicației trece printr-un fus explicit. {@code LocalDateTime.now()} rămâne voie: se folosește doar
 * la termene relative (codul de resetare expiră peste N minute), unde fusul se anulează.
 * {@code bootstrap/} e datele de demo.
 */
class CalendarDayZoneTest {

    private static final Path MAIN = Path.of("src/main/java/ro/ecoregistru");

    @Test
    void noCalendarDayIsReadInTheJvmZone() throws IOException {
        assertThat(sitesUsingTheJvmZone()).isEmpty();
    }

    /** Controlul: inventarul găsește ceva de citit, deci o listă goală de mai sus ar însemna ceva. */
    @Test
    void theInventoryReadsTheSources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            assertThat(files.filter(p -> p.toString().endsWith(".java")).count()).isGreaterThan(100);
        }
    }

    static List<String> sitesUsingTheJvmZone() throws IOException {
        List<String> sites = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/bootstrap/")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains("LocalDate.now()")) {
                        sites.add(MAIN.relativize(file) + ":" + (i + 1));
                    }
                }
            }
        }
        return sites;
    }
}
