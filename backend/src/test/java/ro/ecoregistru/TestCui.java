package ro.ecoregistru;

import ro.ecoregistru.util.Cui;

import java.util.concurrent.ThreadLocalRandom;

/** CUI-uri întâmplătoare cu cifra de control corectă (F-A, 17.09.2026): aplicația le refuză pe celelalte. */
public final class TestCui {

    private TestCui() {
    }

    /** 8 cifre, fără „RO”. */
    public static String random() {
        String body = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000L, 9_999_999L));
        return body + Cui.controlDigit(body);
    }
}
