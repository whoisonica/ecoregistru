package ro.ecoregistru.service.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * „Registrul Anexa 3 (transport)” — numele ales de proprietar pe 29.09.2026 — centralizatorul formularelor de încărcare-descărcare deșeuri
 * nepericuloase (anexa 3 la HG 1061/2008) emise de firmă într-un an.
 *
 * <p>Cerut de specialistă pe 29.09.2026, cu rubricile în cuvintele ei: „număr, data, seria nr doc,
 * cantitate, cod deșeu, denumire deșeu, către cine s-a predat și codul de valorificare — un document
 * care centralizează anexele 3 transport”. Actul nu prevede un model pentru expeditor (art. 20
 * alin. (5) cere un registru numai destinatarului), deci forma urmează practica de control, ca la
 * {@link Art48Register}.
 *
 * <p>Un rând = un formular <b>emis</b>, adică o predare căreia i s-a alocat numărul la prima
 * tipărire ({@code WasteMovement.anexa3Number}). O predare netipărită nu e un formular şi nu intră.
 *
 * @param workPointName punctul de lucru ales, sau null pentru toată firma
 * @param rows          în ordinea numerelor alocate
 */
public record Anexa3Register(String companyName,
                             String companyCui,
                             String workPointName,
                             int year,
                             List<Row> rows) {

    /**
     * @param position      nr. crt. în registru, de la 1
     * @param date          data de pe antetul formularului (data mişcării, ca în
     *                      {@code Anexa3FormGenerator.seriesLine})
     * @param series        seria formularelor firmei la data tipăririi; poate fi null la firmele fără serie
     * @param kg            cantitatea în kg; null la „se cântăreşte la descărcare”
     * @param wasteCode     codul din Lista europeană, cu asterisc dacă e periculos
     * @param recipient     partenerul căruia i s-a predat
     * @param operationCode codul R/D al operaţiunii destinatarului; null la ieşirile vechi fără cod
     */
    public record Row(int position,
                      LocalDate date,
                      String series,
                      int number,
                      BigDecimal kg,
                      String wasteCode,
                      String wasteName,
                      String recipient,
                      String recipientCui,
                      String operationCode) {
    }
}
