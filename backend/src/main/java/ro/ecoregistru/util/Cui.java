package ro.ecoregistru.util;

/**
 * Codul de identificare fiscală al unei firme românești: 2–10 cifre, ultima fiind cifra de control.
 *
 * <p>Cifra de control se verifică fiindcă un CUI greșit la o cifră nu cădea nicăieri la salvare, ci
 * zile mai târziu, la emiterea facturii în FGO („Client[CodUnic] are format invalid”, 17.09.2026).
 * Cheia e {@code 753217532}: cifrele fără cea de control, aliniate la dreapta cheii, se înmulțesc cu
 * ea, suma ori 10 modulo 11 e cifra de control, iar restul 10 se scrie 0.
 */
public final class Cui {

    private static final String KEY = "753217532";

    private Cui() {
    }

    /** Fără „RO” și fără spații; nu validează. */
    public static String digits(String raw) {
        String cui = raw == null ? "" : raw.replaceAll("\\s", "").toUpperCase();
        return cui.startsWith("RO") ? cui.substring(2) : cui;
    }

    /** Doar cifrele, fără „RO”: 2–10, cu cifra de control corectă. */
    public static boolean isValid(String digits) {
        if (digits == null || !digits.matches("\\d{2,10}")) {
            return false;
        }
        return controlDigit(digits.substring(0, digits.length() - 1)) == digits.charAt(digits.length() - 1);
    }

    /** Cifra de control pentru un corp de 1–9 cifre; pentru testele care își fac CUI-uri. */
    public static char controlDigit(String body) {
        int offset = KEY.length() - body.length();
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            sum += (body.charAt(i) - '0') * (KEY.charAt(offset + i) - '0');
        }
        int control = sum * 10 % 11;
        return (char) ('0' + (control == 10 ? 0 : control));
    }
}
