package ro.ecoregistru.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CuiTest {

    /** Coduri reale, publice (ANAF): lungimi diferite, și unul cu restul 10 scris 0 dacă apare. */
    @Test
    void realCodesPass() {
        assertThat(Cui.isValid("14399840")).isTrue();
        assertThat(Cui.isValid("1590082")).isTrue();
        assertThat(Cui.isValid("361757")).isTrue();
        assertThat(Cui.isValid("13548146")).isTrue();
    }

    @Test
    void oneDigitOffFails() {
        assertThat(Cui.isValid("14399841")).isFalse();
        assertThat(Cui.isValid("12345678")).isFalse();
    }

    @Test
    void theShapeIsChecked() {
        assertThat(Cui.isValid("1")).isFalse();
        assertThat(Cui.isValid("12345678901")).isFalse();
        assertThat(Cui.isValid("RO14399840")).isFalse();
        assertThat(Cui.isValid(null)).isFalse();
        assertThat(Cui.digits(" ro 1439 9840 ")).isEqualTo("14399840");
    }

    /** Suma ori 10 modulo 11 dă 10: cifra de control e 0. */
    @Test
    void aRemainderOfTenIsWrittenZero() {
        String body = findBodyWithRemainderTen();
        assertThat(Cui.controlDigit(body)).isEqualTo('0');
        assertThat(Cui.isValid(body + "0")).isTrue();
    }

    private static String findBodyWithRemainderTen() {
        String key = "753217532";
        for (int n = 1_000_000; ; n++) {
            String body = String.valueOf(n);
            int sum = 0;
            for (int i = 0; i < body.length(); i++) {
                sum += (body.charAt(i) - '0') * (key.charAt(2 + i) - '0');
            }
            if (sum * 10 % 11 == 10) {
                return body;
            }
        }
    }
}
