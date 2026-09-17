package ro.ecoregistru.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The buyer's address as FGO prints it: county, locality and the rest on three separate rubrics.
 *
 * <p>ANAF gives the whole address in one string („JUD. BIHOR, SAT SÂNTANDREI COM. SÂNTANDREI, STR. FĂCLIEI, NR.79”)
 * and the locality as „Sat Sântandrei Com. Sântandrei”. Sent as they are, the first invoice of ONSIA S.R.L.
 * (WH 1, 17.09.2026) printed „Bihor” twice and „Sântandrei” four times. Cleaned here, when the invoice is built, so
 * the billing data already saved is fixed too.
 */
public final class BillingAddress {

    private static final Pattern VILLAGE = Pattern.compile("(?iu)^sat\\s+(.+?)\\s+com\\.?\\s+(.+)$");
    private static final Pattern TOWN_PREFIX = Pattern.compile("(?iu)^(?:mun\\.|municipiul|ora[sșş]|or[sșş]\\.)\\s+");

    private BillingAddress() {
    }

    /** „Sat Sântandrei Com. Sântandrei” → „Sântandrei”; „Sat Palota Com. Sântandrei” → „Palota, com. Sântandrei”; „Mun. Oradea” → „Oradea”. */
    public static String city(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String city = raw.trim();
        Matcher village = VILLAGE.matcher(city);
        if (village.matches()) {
            String sat = village.group(1).trim();
            String commune = village.group(2).trim();
            return same(sat, commune) ? sat : sat + ", com. " + commune;
        }
        return TOWN_PREFIX.matcher(city).replaceFirst("");
    }

    /** The address without the parts FGO already prints as county and locality; unchanged if nothing else is left. */
    public static String street(String address, String county, String city) {
        if (address == null || address.isBlank()) {
            return address;
        }
        List<String> kept = Arrays.stream(address.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .filter(part -> !isCounty(part, county) && !isCity(part, city))
                .toList();
        return kept.isEmpty() ? address.trim() : String.join(", ", kept);
    }

    private static boolean isCounty(String part, String county) {
        String folded = Diacritics.fold(part);
        return folded.startsWith("jud.") || folded.startsWith("judetul ") || folded.equals("municipiul bucuresti")
                || same(part, county);
    }

    private static boolean isCity(String part, String city) {
        return city != null && (same(part, city) || same(part, city(city)));
    }

    private static boolean same(String a, String b) {
        return a != null && b != null && Objects.equals(Diacritics.fold(a.trim()), Diacritics.fold(b.trim()));
    }
}
