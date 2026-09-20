package ro.ecoregistru.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Date personale care nu trebuie să ajungă în jurnal, dar despre care tot trebuie să poţi vorbi.
 *
 * <p>Jurnalul aplicaţiei pleacă la furnizorul de loguri şi se păstrează acolo: o adresă de mail
 * scrisă în el e date personale ţinute într-un al treilea loc, pe care nimeni nu-l numără la
 * ştergerea unui cont. Dar mesajele aveau dreptate să numească pe cineva — „a picat mailul" fără
 * „al cui" nu se poate depana.
 *
 * <p>Soluţia e o referinţă stabilă, nu o ascundere: aceeaşi adresă dă mereu acelaşi semn, deci două
 * rânduri din jurnal se leagă între ele şi un atacator care insistă se vede, fără ca din semn să se
 * poată afla adresa. Unde există deja un cont, se scrie mai bine <b>id-ul</b> lui: e la fel de
 * netrivial de inversat şi se caută direct în bază.
 */
public final class LogSafe {

    private LogSafe() {
    }

    /** {@code mail#3f9a2c} — acelaşi pentru aceeaşi adresă, nimic de citit din el. */
    public static String email(String address) {
        if (address == null || address.isBlank()) {
            return "mail#?";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(address.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return "mail#" + HexFormat.of().formatHex(digest).substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e obligatoriu în orice JVM. Dacă totuşi lipseşte, jurnalul nu e locul în care
            // să cadă cererea — şi cu atât mai puţin locul în care să apară adresa pe faţă.
            return "mail#?";
        }
    }
}
