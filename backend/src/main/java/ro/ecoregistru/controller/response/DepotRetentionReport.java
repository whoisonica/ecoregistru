package ro.ecoregistru.controller.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * D1.9 și D1.10 — ce a reținut depozitul la sursă într-o lună sau într-un an, din operațiunile de
 * intrare finalizate. E baza a două declarații care se depun oricum de contabil; aplicația dă cifrele
 * și data până la care se depun, nu transmite nimic nicăieri.
 *
 * <p><b>Lunar</b> (cu {@code month}): cei 2% AFM (OUG 196/2005 art. 11 alin. (1)) și impozitul reținut
 * (Codul fiscal art. 115 alin. (3), D100) se declară și se plătesc până pe <b>25 a lunii următoare</b>.
 * <b>Anual</b> (fără {@code month}): declarația pe fiecare beneficiar de venit (art. 132 alin. (2),
 * D205) se depune până în <b>ultima zi a lui februarie</b> a anului următor.
 *
 * <p>Luna unei operațiuni e cea a <b>datei ei</b>, nu a introducerii în aplicație.
 *
 * @param dueDate data până la care se depune: 25 a lunii următoare la raportul lunar, ultima zi a lui
 *                februarie la cel anual
 * @param afmRate cota aplicată acum; sumele rămân cele reținute atunci, deci pe o lună veche pot fi
 *                calculate cu altă cotă
 */
public record DepotRetentionReport(
        int year,
        Integer month,
        LocalDate from,
        LocalDate to,
        LocalDate dueDate,
        BigDecimal afmRate,
        BigDecimal afmBase,
        BigDecimal afmContribution,
        BigDecimal incomeTaxRate,
        BigDecimal incomeTaxBase,
        BigDecimal incomeTax,
        long operations,
        List<Beneficiary> beneficiaries) {

    /**
     * Un beneficiar de venit pentru D205: persoana fizică de la care s-a reținut impozit. CNP-ul e
     * întreg, fiindcă declarația îl cere pe fiecare beneficiar; raportul îl vede doar cine
     * administrează firma <b>și</b> vede prețurile.
     */
    public record Beneficiary(UUID personId, String name, String cnp, BigDecimal base, BigDecimal tax) {
    }
}
