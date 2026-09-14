package ro.ecoregistru.service.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The chronological monthly record of waste taken over from third parties — OUG 92/2021 art. 48
 * alin. (1), for a company that keeps the {@code ART_48} register.
 *
 * <p><b>No form is being reproduced, because none exists.</b> Alin. (1) prescribes the content
 * (lit. a–c) and one property of the shape, "cronologică lunară, în format tabelar"; alin. (3)
 * leaves the reporting procedure to an order we have not found, and alin. (7) puts the data in
 * ANMAP's electronic register, where it is typed by hand (question AG). So the document has two
 * halves: the chronological table the act asks for, and the year's totals laid out like the SIM
 * "Colectare/Tratare" questionnaire, to copy from. Sources: docs/surse-oficiale.md §2.1-bis.
 *
 * <p>All quantities are held in kilograms; the generator prints the tonnes the act and the
 * questionnaire ask for next to them.
 */
public record Art48Register(
        String companyName,
        String companyCui,
        /** Null when the document covers every work point. */
        String workPointName,
        int year,
        List<Entry> entries,
        /** Cap. 1, tabel 1 of the questionnaire: one line per waste code. */
        List<CodeTotal> collection,
        /** Cap. 2, tabel A: one line per recipient, code and R code. */
        List<Handover> recovery,
        /** Cap. 2, tabel B: one line per recipient, code and D code. */
        List<Handover> disposal,
        /** Movements of the year still waiting for their weight: listed, not summed. */
        int unweighed) {

    /** One movement, in date order. {@code kg} is null while the weight has not come back. */
    public record Entry(LocalDate date,
                        String workPoint,
                        String operation,
                        String wasteCode,
                        String wasteName,
                        BigDecimal kg,
                        String partner,
                        String partnerCui,
                        String operationCode,
                        String transport,
                        String treatment,
                        String document) {
    }

    public record CodeTotal(String wasteCode,
                            String wasteName,
                            BigDecimal openingKg,
                            BigDecimal collectedKg,
                            BigDecimal recoveredKg,
                            BigDecimal disposedKg,
                            /** Legacy exits with no R/D code: they leave the stock but fit neither column. */
                            BigDecimal unclassifiedKg,
                            BigDecimal closingKg,
                            List<String> recoveryCodes,
                            List<String> disposalCodes) {
    }

    public record Handover(String recipient,
                           String cui,
                           String address,
                           String wasteCode,
                           String wasteName,
                           BigDecimal kg,
                           String operationCode) {
    }
}
