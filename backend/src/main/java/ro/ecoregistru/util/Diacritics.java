package ro.ecoregistru.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Folds Romanian diacritics away for comparison: „deșeuri" and „deseuri" must find the same rows.
 *
 * <p>The database half of this lives in {@code V17__waste_code_search_without_diacritics.sql},
 * which stores the same folding of every nomenclator row in a generated column. The two must
 * agree, so both handle exactly the seven Romanian letters, in both Unicode spellings — comma
 * below (ș U+0219, ț U+021B, the correct ones) and cedilla (ş U+015F, ţ U+0163, the legacy ones
 * that official files are full of).
 */
public final class Diacritics {

    private Diacritics() {
    }

    /**
     * @return the text lowercased with its diacritical marks removed, or the input unchanged when
     *         it is {@code null}
     */
    public static String fold(String text) {
        if (text == null) {
            return null;
        }
        // NFD splits a letter into base + combining mark ('ș' -> 's' + U+0326); \p{M} then drops
        // every mark, whichever of the two spellings the text used.
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
