package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.util.LogSafe;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Semnul cu care o adresă de mail apare în jurnal.
 *
 * <p>Jurnalul pleacă la furnizorul de loguri şi rămâne acolo: adresa scrisă pe faţă e date
 * personale într-un al treilea loc, pe care nimeni nu-l numără la ştergerea unui cont. Dar
 * mesajele aveau dreptate să numească pe cineva, aşa că semnul trebuie să fie bun la depanare.
 */
class LogSafeTest {

    /** Nimic din adresă nu se mai vede — nici numele, nici domeniul, nici măcar o literă. */
    @Test
    void theAddressItselfNeverAppears() {
        String marked = LogSafe.email("ionela.popescu@exemplu.ro");

        assertThat(marked).doesNotContain("ionela", "popescu", "exemplu", "@");
        assertThat(marked).startsWith("mail#");
    }

    /**
     * Acelaşi semn pentru aceeaşi adresă — asta e tot rostul. Două rânduri din jurnal se leagă
     * între ele, deci cineva care insistă pe o adresă se vede, fără să ştim care e adresa.
     */
    @Test
    void theSameAddressAlwaysGetsTheSameMark() {
        assertThat(LogSafe.email("cineva@exemplu.ro"))
                .isEqualTo(LogSafe.email("cineva@exemplu.ro"))
                // Scrisul cu majuscule şi spaţiile din jur sunt aceeaşi cutie poştală.
                .isEqualTo(LogSafe.email("  Cineva@Exemplu.RO  "));
    }

    /** Şi semne diferite pentru adrese diferite, altfel n-ar deosebi pe nimeni. */
    @Test
    void differentAddressesGetDifferentMarks() {
        assertThat(LogSafe.email("unu@exemplu.ro")).isNotEqualTo(LogSafe.email("doi@exemplu.ro"));
    }

    /** O adresă lipsă nu strică rândul din jurnal şi nu se preface că e cineva. */
    @Test
    void aMissingAddressIsSaidToBeMissing() {
        assertThat(LogSafe.email(null)).isEqualTo("mail#?");
        assertThat(LogSafe.email("   ")).isEqualTo("mail#?");
    }
}
