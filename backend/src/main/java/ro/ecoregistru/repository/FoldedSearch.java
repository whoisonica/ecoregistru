package ro.ecoregistru.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import ro.ecoregistru.util.Diacritics;

import java.util.Arrays;
import java.util.List;

/**
 * The table toolbar's search, expressed in SQL.
 *
 * <p>Until server-side paging, every table fetched all its rows and searched them in the browser
 * ({@code useTableView.ts}). Paging on the server without moving the search with it would have
 * been a regression dressed as an improvement: the box would still be there, but it would only
 * look inside the twenty-five rows that happened to be on screen. So the three rules the hook
 * follows are reproduced here, and they are rules, not approximations:
 *
 * <ol>
 *   <li><b>Diacritics do not matter.</b> Same folding as {@link Diacritics#fold} and as the
 *       generated column of {@code V17}: the seven Romanian letters, both Unicode spellings.
 *       Whoever types „deseuri" on a keyboard without a Romanian layout finds „deșeuri".</li>
 *   <li><b>The phrase wins.</b> If anything matches the typed text as one string, only those rows
 *       are shown; the word search is the fallback, not an addition. „15 01 02" is one code, not
 *       three numbers.</li>
 *   <li><b>A word must start a word.</b> The fallback requires every typed word to begin a word of
 *       the row — otherwise „02" matches the „2026" of a date, which is how a waste-code search
 *       used to return a different waste code.</li>
 * </ol>
 *
 * <p>Same order of evaluation, too: the caller counts the phrase matches first and only falls back
 * when there are none, because the hook returns the phrase rows whenever there is at least one.
 */
public final class FoldedSearch {

    /**
     * The two halves of Postgres' {@code translate}, character for character as in {@code V17}: ă â
     * î ș ş ț ţ and their capitals, comma-below and cedilla alike, because official files mix the
     * two spellings.
     */
    private static final String DIACRITICS = "ăâîșşțţĂÂÎȘŞȚŢ";
    private static final String PLAIN = "aaissttAAISSTT";

    /** LIKE's own wildcards, neutralised with this escape so a search for „50%" means 50 percent. */
    private static final char LIKE_ESCAPE = '\\';

    private FoldedSearch() {
    }

    /** The same folding the browser applies, so both sides of a comparison mean the same thing. */
    public static String fold(String text) {
        return Diacritics.fold(text);
    }

    /**
     * Splits folded text into words on everything that is not a letter or a digit — the server's
     * copy of {@code tokenize} in {@code useTableView.ts}.
     *
     * <p>That the pieces come out alphanumeric is what keeps {@link #words} safe: they are dropped
     * into a regular expression, and a token that cannot hold a metacharacter cannot carry one in.
     */
    public static List<String> tokenize(String folded) {
        return Arrays.stream(folded.split("[^\\p{L}\\p{N}]+")).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * The searchable text of a row: the columns someone would actually type, folded and glued with
     * spaces.
     *
     * <p>Every part is coalesced to the empty string first. In SQL a concatenation with one NULL in
     * it <em>is</em> NULL, so a single movement without a partner would have made the whole row
     * unsearchable — including by its waste code.
     */
    @SafeVarargs
    public static Expression<String> haystack(CriteriaBuilder cb, Expression<String>... parts) {
        Expression<String> joined = null;
        for (Expression<String> part : parts) {
            Expression<String> safe = cb.coalesce(part, cb.literal(""));
            joined = joined == null ? safe : cb.concat(cb.concat(joined, cb.literal(" ")), safe);
        }
        Expression<String> text = joined == null ? cb.literal("") : joined;
        return cb.lower(cb.function("translate", String.class, text,
                cb.literal(DIACRITICS), cb.literal(PLAIN)));
    }

    /** Rule 2: the typed text, found anywhere in the row as one string. */
    public static Predicate phrase(CriteriaBuilder cb, Expression<String> haystack, String foldedQuery) {
        return cb.like(haystack, "%" + escapeLike(foldedQuery) + "%", LIKE_ESCAPE);
    }

    /**
     * Rule 3: every typed word begins a word of the row.
     *
     * <p>„Begins a word" is the start of the text or a non-alphanumeric character in front of it —
     * which LIKE cannot say (its {@code _} matches one character of <em>any</em> kind), so this is
     * the one place that needs a regular expression. {@code regexp_like} exists in Postgres 15 and
     * up; production runs 18 and the tests run 15.
     */
    public static Predicate words(CriteriaBuilder cb, Expression<String> haystack, List<String> tokens) {
        Predicate all = cb.conjunction();
        for (String token : tokens) {
            Expression<Boolean> matches = cb.function("regexp_like", Boolean.class, haystack,
                    cb.literal("(^|[^[:alnum:]])" + token));
            all = cb.and(all, cb.isTrue(matches));
        }
        return all;
    }

    /** Turns LIKE's wildcards into literal characters. The escape itself goes first, or it doubles. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
