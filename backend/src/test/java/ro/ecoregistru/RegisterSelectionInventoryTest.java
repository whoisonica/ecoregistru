package ro.ecoregistru;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R1 din QA-FINAL-REPORT §5, „gaura structurală 11": un al cincilea cititor de mişcări scris fără
 * filtrul de registru n-ar fi căzut la niciun test. Cele patru existente erau probate negativ, dar
 * proprietatea era a codului, nu a testelor.
 *
 * <p>Clasa asta citeşte sursa, ca {@code EndpointGuardInventoryIT} citeşte rutele: orice fişier din
 * motor sau din generatoarele de documente care primeşte o listă de mişcări într-o metodă publică
 * trebuie să treacă prin {@code WasteRegister.X.select(...)}. Granulaţia e pe fişier, nu pe metodă —
 * destul ca un builder nou să nu poată uita registrul de tot.
 */
class RegisterSelectionInventoryTest {

    private static final Path MAIN = Path.of("src/main/java/ro/ecoregistru/service");
    private static final Pattern PUBLIC_TAKES_MOVEMENTS =
            Pattern.compile("public [^;{=]*\\([^)]*List<WasteMovement> \\w+[^)]*\\)", Pattern.DOTALL);
    /**
     * D1.13 (16.09.2026): formularele de transport primesc liniile <b>unui singur camion</b> — o operațiune
     * de depozit, deci un singur registru prin construcție (V46: liniile sunt în {@code ART_48}). Nu adună
     * și nu raportează nimic; o listă a lor e un transport, nu o evidență. Excepție numită, nu tipul
     * parametrului schimbat ca să scape de tipar: un generator nou tot aici trebuie trecut, cu motivul lui.
     */
    private static final Set<String> TRANSPORT_DOCUMENTS = Set.of("Anexa3FormGenerator.java", "AvizGenerator.java");
    private static final Pattern SELECTS_A_REGISTER =
            Pattern.compile("WasteRegister\\.(ANEXA_1|ART_48)\\.select\\(");

    @Test
    void everyReaderOfAMovementListCutsItToARegister() throws IOException {
        List<Path> readers;
        try (Stream<Path> files = Stream.concat(
                Stream.of(MAIN.resolve("EvidenceCalculator.java")),
                Files.list(MAIN.resolve("export")))) {
            readers = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> PUBLIC_TAKES_MOVEMENTS.matcher(read(p)).find()
                            || p.endsWith("EvidenceCalculator.java"))
                    .toList();
        }
        // Motorul + Anexa 1, centralizata, Anexa 1 Ambalaje, art. 48, Anexa 3 Ambalaje.
        assertThat(readers).as("cititorii de mişcări au fost găsiţi").hasSizeGreaterThanOrEqualTo(6);

        List<String> unfiltered = readers.stream()
                .filter(p -> !TRANSPORT_DOCUMENTS.contains(p.getFileName().toString()))
                .filter(p -> !SELECTS_A_REGISTER.matcher(read(p)).find())
                .map(p -> p.getFileName().toString())
                .toList();
        assertThat(unfiltered).as("primesc mişcări şi nu aleg registrul").isEmpty();
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
