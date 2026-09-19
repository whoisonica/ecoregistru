package ro.ecoregistru.audit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cum se scrie o cantitate în jurnalul de modificări.
 *
 * <p>{@code onFlushDirty} compară valorile <b>ca șiruri</b>, iar {@code BigDecimal.toString()}
 * păstrează scara: baza întoarce {@code NUMERIC(…,3)} — adică {@code 400.000} — și formularul
 * trimite {@code 400}. Același număr, două șiruri, deci orice salvare a unei mișcări lăsa în
 * jurnal „Cantitate: 400.000 → 400", o schimbare care nu s-a întâmplat. Un jurnal care raportează
 * modificări inventate e mai rău decât unul care tace: la un control nu mai poți spune care rând e
 * real.
 */
class AuditInterceptorFormatTest {

    @Test
    void sameNumberDifferentScaleFormatsTheSame() {
        assertThat(AuditInterceptor.format(new BigDecimal("400.000")))
                .isEqualTo(AuditInterceptor.format(new BigDecimal("400")));
    }

    @Test
    void trailingZeroesGoButTheValueStays() {
        assertThat(AuditInterceptor.format(new BigDecimal("400.000"))).isEqualTo("400");
        assertThat(AuditInterceptor.format(new BigDecimal("1.100"))).isEqualTo("1.1");
        assertThat(AuditInterceptor.format(new BigDecimal("0.000"))).isEqualTo("0");
        // Fără notație științifică: `1E+3` în jurnal n-ar fi citit de nimeni ca o mie de kilograme.
        assertThat(AuditInterceptor.format(new BigDecimal("1000.000"))).isEqualTo("1000");
    }

    @Test
    void arealChangeIsStillAChange() {
        assertThat(AuditInterceptor.format(new BigDecimal("400.500")))
                .isNotEqualTo(AuditInterceptor.format(new BigDecimal("400")));
    }

    @Test
    void nullStaysNull() {
        assertThat(AuditInterceptor.format(null)).isNull();
    }
}
