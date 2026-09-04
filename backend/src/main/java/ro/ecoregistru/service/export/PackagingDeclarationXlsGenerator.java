package ro.ecoregistru.service.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Font;
import org.springframework.stereotype.Component;
import ro.ecoregistru.enums.PackagingMaterial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * Anexa 1 Ambalaje (Ordinul 794/2012) as the spreadsheet the authority receives.
 *
 * <p>The layout is copied from the two files the specialist sent —
 * {@code documente oficiale/RAPORTARE AMBALAJE _anexa 1.xlsx} (blank) and
 * {@code RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx} (filled): two sheets named
 * <b>{@code Tabelul nr. 1}</b> and <b>{@code Tabelul nr. 2}</b>, the seven-line identification
 * header on the first, the numbered column band, the eleven material rows with their three computed
 * sums, the four footnotes, and the signature block at the foot of the second sheet. Both in
 * <b>[kilograme]</b>, which the act prints at the head of every one of its five annexes.
 *
 * <p>Cell addresses follow the model exactly — title in {@code B2}, header in {@code B4:B10},
 * material rows from {@code B20} — so a client who has filed this form before recognises the sheet
 * they already know rather than a rebuilt one.
 *
 * <p><b>De ce BIFF8 şi nu OOXML.</b> Art. 6 numeşte formatul, nu doar „un fişier": «.xls». Până la
 * auditul de conformitate din 02.09.2026 clasa asta folosea {@code XSSFWorkbook}, care produce
 * <b>.xlsx</b> — formatul introdus în 2007 — deşi documentaţia noastră promitea deja lucrul corect.
 * {@code HSSFWorkbook} scrie BIFF8, adică exact ce cere articolul, servit cu
 * {@code application/vnd.ms-excel}. Art. 7 spune că ANPM publică formatul pe pagina proprie, deci
 * depunerea se face pe şablonul lor: un portal care validează extensia sau semnătura fişierului ar
 * fi respins un .xlsx chiar în ziua depunerii. Cât de strict e în practică e întrebarea <b>AG</b> —
 * dar reparaţia nu depinde de răspuns, fiindcă actul e explicit.
 *
 * <p><b>De ce e foaia protejată.</b> Art. 6 din ordin, verbatim: „Datele de raportare se transmit
 * în format electronic «.xls» <b>protejat împotriva modificării datelor</b> şi <b>pe suport
 * hârtie</b>, până cel târziu la data de 25 februarie a fiecărui an". Deci protecţia e cerută, nu
 * o alegere — şi tot de acolo vine faptul că PDF-ul nu e un moft: e exemplarul pe hârtie. Parola e
 * goală dinadins: protecţia opreşte modificarea din greşeală, dar clientul o poate ridica dacă are
 * de corectat ceva înainte de depunere. O parolă pe care n-o ştie ar transforma cerinţa actului
 * într-un obstacol.
 *
 * <p>Empty stays empty. A material nobody has movements for prints blank, not 0: on a filed form
 * "none" and "not answered" are different statements, and only the client may make either.
 */
@Component
public class PackagingDeclarationXlsGenerator {

    private static final String TITLE =
            "ANEXA Nr. 1: Producători şi importatori de ambalaje de desfacere, de produse "
                    + "ambalate, supraambalatori de produse ambalate";

    /**
     * Footnotes 1 to 4 of tabelul 1, from the model — with the two dead references replaced by the
     * acts in force. Audit point 10, decided 04.09.2026.
     *
     * <p><b>The policy, now applied in all five places instead of three.</b> Ordinul 794/2012 has
     * not been touched since 2015, so its notes still name acts that no longer exist: nota 1 cites
     * HG 621/2005, abrogated on 1 November 2015 by Legea 249/2015, and nota 3 cites HG 937/2010,
     * abrogated in 2016. We print the act in force — the same licence answer A granted for cap. 3/4
     * of the fişă and nota 2 of tabelul 2, which had already been updated from Legea 211/2011 to
     * OUG 92/2021. Reproducing a reference to a repealed act because the model does is regula de
     * lucru 2 inverted: the corpus says how a rubric is filled in, never which law is in force.
     *
     * <p><b>Why nota 3 does not cite HG 539/2016.</b> That is the act which <em>abrogated</em>
     * HG 937/2010 — verified on the Portal Legislativ, 04.09.2026: its full title is "HOTĂRÂRE
     * nr. 539 din 27 iulie 2016 pentru abrogarea Hotărârii Guvernului nr. 1.408/2008 [...] şi a
     * Hotărârii Guvernului nr. 937/2010 [...]". It is a pure repealing act with no substantive
     * content, so a note pointing a reader there would send them to a page that says nothing about
     * how a package is labelled. The rule in force is <b>Regulamentul (CE) nr. 1272/2008</b> (CLP),
     * directly applicable, and named in that decision's own preamble as the reason the national
     * acts became redundant. The audit had suggested "Regulamentul CLP / HG 539/2016"; only the
     * first half survives reading the act.
     */
    private static final List<String> TABLE1_NOTES = List.of(
            "1) Se raportează numai ambalajele de desfacere destinate pieţei naţionale, definite "
                    + "prin Legea nr. 249/2015 privind modalitatea de gestionare a ambalajelor şi a "
                    + "deşeurilor de ambalaje, cu modificările şi completările ulterioare.",
            "2) Se raportează o singură dată, atunci când sunt introduse în circuitul de umplere şi "
                    + "livrate pentru prima dată.",
            "3) Se raportează numai ambalajele care au conţinut substanţe periculoase inscripţionate "
                    + "ca atare potrivit Regulamentului (CE) nr. 1272/2008 al Parlamentului European "
                    + "şi al Consiliului. Cantităţile de ambalaje "
                    + "cu conţinut periculos sunt tot ambalaje primare şi se regăsesc şi în coloana 3.",
            "4) Se raportează numai ambalajele folosite la ambalarea produselor destinate pieţei "
                    + "naţionale şi se includ şi ambalajele utilizate pentru ambalarea ambalajelor "
                    + "de desfacere.");

    private static final List<String> TABLE2_NOTES = List.of(
            "1) Se completează câte o rubrică distinctă pentru fiecare dintre operatorii care au "
                    + "preluat deşeurile de ambalaje din materialul respectiv.",
            // Nota 2 a modelului trimite la Legea 211/2011, abrogată de OUG 92/2021. Tipărim actul
            // în vigoare — aceeaşi îngăduinţă pe care răspunsul A a dat-o la cap. 3/4 ale fişei.
            "2) Se menţionează operaţiunea la care au fost supuse deşeurile potrivit anexei nr. 3 la "
                    + "Ordonanţa de urgenţă a Guvernului nr. 92/2021 privind regimul deşeurilor.",
            "În cazul în care operaţiunea de reciclare/valorificare se face prin export sau transfer "
                    + "intracomunitar, se va specifica alături de denumirea operatorului economic, "
                    + "adresa punctului de lucru şi ţara de destinaţie.");

    public byte[] render(PackagingDeclaration d) {
        try (Workbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles s = new Styles(wb);
            sheet1(wb, s, d);
            sheet2(wb, s, d);
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                wb.getSheetAt(i).protectSheet("");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // ------------------------------------------------------------------ foaia 1

    private void sheet1(Workbook wb, Styles s, PackagingDeclaration d) {
        Sheet sh = wb.createSheet("Tabelul nr. 1");
        sh.setColumnWidth(0, 900);
        sh.setColumnWidth(1, 7000);
        for (int c = 2; c <= 8; c++) {
            sh.setColumnWidth(c, 4600);
        }

        put(sh, 1, 1, TITLE, s.title);
        sh.addMergedRegion(new CellRangeAddress(1, 1, 1, 8));
        sh.getRow(1).setHeightInPoints(32);

        String[] header = {
                "Denumirea operatorului economic: " + nvl(d.companyName()),
                "Judeţ şi localitate: " + nvl(d.county()),
                "Adresa: " + nvl(d.address()),
                "Tel./Fax/e-mail: " + nvl(d.contact()),
                "Cod CAEN pentru activitatea aferentă raportării: " + nvl(d.caenCode()),
                "CUI: " + nvl(d.cui()),
                "Anul pentru care se realizează raportarea: " + d.year(),
        };
        for (int i = 0; i < header.length; i++) {
            put(sh, 3 + i, 1, header[i], s.plain);
            sh.addMergedRegion(new CellRangeAddress(3 + i, 3 + i, 1, 8));
        }

        put(sh, 12, 1, "Tabel 1. Ambalaje introduse pe piaţa naţională", s.tableTitle);
        put(sh, 14, 8, "[kilograme]", s.unit);

        // Banda de antet: rândurile 16-19 din model, cu aceleaşi îmbinări.
        put(sh, 15, 1, "Material", s.head);
        put(sh, 15, 2, "Ambalaje de desfacere fabricate/importate *1)", s.head);
        put(sh, 15, 3, "Ambalaje folosite la ambalarea produselor introduse pe piaţa natională *4)",
                s.head);
        merge(sh, 15, 17, 1, 1, s.head);
        merge(sh, 15, 17, 2, 2, s.head);
        sh.addMergedRegion(new CellRangeAddress(15, 15, 3, 8));

        put(sh, 16, 3, "Total (col. 3+5)", s.head);
        put(sh, 16, 4, "Ambalaje primare", s.head);
        put(sh, 16, 6, "Ambalaje secundare şi de transport", s.head);
        put(sh, 16, 8, "Ambalaje cu conţinut periculos *3) din coloana 3", s.head);
        merge(sh, 16, 17, 3, 3, s.head);
        sh.addMergedRegion(new CellRangeAddress(16, 16, 4, 5));
        sh.addMergedRegion(new CellRangeAddress(16, 16, 6, 7));
        merge(sh, 16, 17, 8, 8, s.head);

        put(sh, 17, 4, "Total", s.head);
        put(sh, 17, 5, "din care: ambalaj reutilizabil *2)", s.head);
        put(sh, 17, 6, "Total", s.head);
        put(sh, 17, 7, "din care: ambalaj reutilizabil *2)", s.head);
        sh.getRow(15).setHeightInPoints(34);
        sh.getRow(17).setHeightInPoints(30);

        for (int c = 1; c <= 8; c++) {
            put(sh, 18, c, String.valueOf(c - 1), s.head);
        }

        // Rândurile de material, în ordinea actului, cu cele trei sume intercalate.
        List<PackagingDeclaration.MarketRow> rows = d.marketRows();
        int r = 19;
        r = materialRow(sh, s, r, rows, PackagingMaterial.STICLA);
        r = materialRow(sh, s, r, rows, PackagingMaterial.PET);
        r = materialRow(sh, s, r, rows, PackagingMaterial.ALTE_PLASTICE);
        r = sumRow(sh, s, r, rows, "Total plastic", PackagingMaterial.plasticParts());
        r = materialRow(sh, s, r, rows, PackagingMaterial.HARTIE_CARTON);
        r = materialRow(sh, s, r, rows, PackagingMaterial.ALUMINIU);
        r = materialRow(sh, s, r, rows, PackagingMaterial.OTEL);
        r = sumRow(sh, s, r, rows, "Total metal", PackagingMaterial.metalParts());
        r = materialRow(sh, s, r, rows, PackagingMaterial.LEMN);
        r = materialRow(sh, s, r, rows, PackagingMaterial.ALTELE);
        r = sumRow(sh, s, r, rows, "TOTAL:", List.of(PackagingMaterial.values()));

        r++;
        for (String note : TABLE1_NOTES) {
            put(sh, r, 1, note, s.note);
            sh.addMergedRegion(new CellRangeAddress(r, r, 1, 8));
            sh.getRow(r).setHeightInPoints(26);
            r++;
        }

        // Ce n-a intrat în tabel se spune pe hârtie, nu se pierde tăcut.
        if (!d.unclassified().isEmpty()) {
            r++;
            put(sh, r, 1, unclassifiedNote(d), s.warning);
            sh.addMergedRegion(new CellRangeAddress(r, r, 1, 8));
            sh.getRow(r).setHeightInPoints(30);
        }
    }

    private int materialRow(Sheet sh, Styles s, int r,
                            List<PackagingDeclaration.MarketRow> rows, PackagingMaterial material) {
        PackagingDeclaration.MarketRow row = rows.stream()
                .filter(x -> x.material() == material)
                .findFirst()
                .orElse(null);

        put(sh, r, 1, material.getOfficialLabel(), s.label);
        number(sh, s, r, 2, row == null ? null : row.salesPackaging());
        number(sh, s, r, 3, row == null ? null : row.packagedGoodsTotal());
        number(sh, s, r, 4, row == null ? null : row.primaryTotal());
        number(sh, s, r, 5, row == null ? null : row.primaryReusable());
        number(sh, s, r, 6, row == null ? null : row.secondaryTotal());
        number(sh, s, r, 7, row == null ? null : row.secondaryReusable());
        number(sh, s, r, 8, row == null ? null : row.hazardousContent());
        return r + 1;
    }

    private int sumRow(Sheet sh, Styles s, int r, List<PackagingDeclaration.MarketRow> rows,
                       String label, List<PackagingMaterial> parts) {
        put(sh, r, 1, label, s.labelBold);
        number(sh, s, r, 2, sum(rows, parts, PackagingDeclaration.MarketRow::salesPackaging), true);
        number(sh, s, r, 3, sum(rows, parts, PackagingDeclaration.MarketRow::packagedGoodsTotal), true);
        number(sh, s, r, 4, sum(rows, parts, PackagingDeclaration.MarketRow::primaryTotal), true);
        number(sh, s, r, 5, sum(rows, parts, PackagingDeclaration.MarketRow::primaryReusable), true);
        number(sh, s, r, 6, sum(rows, parts, PackagingDeclaration.MarketRow::secondaryTotal), true);
        number(sh, s, r, 7, sum(rows, parts, PackagingDeclaration.MarketRow::secondaryReusable), true);
        number(sh, s, r, 8, sum(rows, parts, PackagingDeclaration.MarketRow::hazardousContent), true);
        return r + 1;
    }

    /**
     * A sum of cells that may be empty stays empty when every one of them is: adding nothing to
     * nothing is not zero on this form, it is still "not answered".
     */
    private BigDecimal sum(List<PackagingDeclaration.MarketRow> rows,
                           List<PackagingMaterial> parts,
                           Function<PackagingDeclaration.MarketRow, BigDecimal> column) {
        BigDecimal total = null;
        for (PackagingDeclaration.MarketRow row : rows) {
            if (!parts.contains(row.material())) {
                continue;
            }
            BigDecimal value = column.apply(row);
            if (value != null) {
                total = total == null ? value : total.add(value);
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ foaia 2

    private void sheet2(Workbook wb, Styles s, PackagingDeclaration d) {
        Sheet sh = wb.createSheet("Tabelul nr. 2");
        sh.setColumnWidth(0, 900);
        sh.setColumnWidth(1, 5200);
        sh.setColumnWidth(2, 4200);
        sh.setColumnWidth(3, 11000);
        sh.setColumnWidth(4, 4200);
        sh.setColumnWidth(5, 6000);

        put(sh, 1, 1, "Tabelul 2. Deşeuri de ambalaje gestionate", s.tableTitle);
        put(sh, 3, 5, "[kilograme]", s.unit);

        put(sh, 4, 1, "Materialul", s.head);
        put(sh, 4, 2, "Deşeuri de ambalaje încredinţate unui operator economic autorizat", s.head);
        put(sh, 4, 5, "Operaţiunea 2) la care a supus deşeul operatorul menţionat în coloana 2",
                s.head);
        merge(sh, 4, 6, 1, 1, s.head);
        sh.addMergedRegion(new CellRangeAddress(4, 4, 2, 4));
        merge(sh, 4, 6, 5, 5, s.head);

        put(sh, 5, 2, "Cantitatea", s.head);
        put(sh, 5, 3, "Operatorul economic 1) pentru colectarea, reciclarea şi valorificarea "
                + "deşeurilor de ambalaje", s.head);
        merge(sh, 5, 6, 2, 2, s.head);
        sh.addMergedRegion(new CellRangeAddress(5, 5, 3, 4));

        put(sh, 6, 3, "Denumirea, adresă punct de lucru", s.head);
        put(sh, 6, 4, "CUI", s.head);
        sh.getRow(4).setHeightInPoints(34);
        sh.getRow(5).setHeightInPoints(30);

        int r = 7;
        BigDecimal total = null;
        for (PackagingMaterial material : PackagingMaterial.values()) {
            List<PackagingDeclaration.HandoverRow> lines = d.handoverRows().stream()
                    .filter(x -> x.material() == material)
                    .toList();
            if (lines.isEmpty()) {
                // Rândul de material rămâne pe foaie chiar gol, ca în model: cine citeşte
                // formularul vede că materialul a fost luat în considerare, nu că a fost uitat.
                put(sh, r, 1, material.getOfficialLabel(), s.label);
                blank(sh, s, r, 2, 5);
                r++;
                continue;
            }
            for (PackagingDeclaration.HandoverRow line : lines) {
                put(sh, r, 1, material.getOfficialLabel(), s.label);
                number(sh, s, r, 2, line.quantity());
                put(sh, r, 3, nvl(line.operatorName())
                        + (isBlank(line.operatorAddress()) ? "" : ", " + line.operatorAddress()),
                        s.cell);
                put(sh, r, 4, nvl(line.operatorCui()), s.cell);
                put(sh, r, 5, nvl(line.operation()), s.cellCenter);
                if (line.quantity() != null) {
                    total = total == null ? line.quantity() : total.add(line.quantity());
                }
                r++;
            }
        }

        put(sh, r, 1, "TOTAL:", s.labelBold);
        number(sh, s, r, 2, total, true);
        blank(sh, s, r, 3, 5);
        r += 2;

        for (String note : TABLE2_NOTES) {
            put(sh, r, 1, note, s.note);
            sh.addMergedRegion(new CellRangeAddress(r, r, 1, 5));
            sh.getRow(r).setHeightInPoints(26);
            r++;
        }
        put(sh, r++, 1, "NOTĂ: Se completează în tabel distinct în cazul deşeurilor de ambalaje "
                + "periculoase.", s.note);
        r++;

        put(sh, r++, 1, "Semnătura autorizată şi ştampila", s.plain);
        put(sh, r, 1, "Numele şi prenumele:", s.plain);
        put(sh, r, 3, nvl(d.preparedBy()), s.plain);
        put(sh, r, 5, "Data:", s.plain);
        r++;
        put(sh, r, 1, "Funcţia:", s.plain);
        put(sh, r, 3, nvl(d.preparedByRole()), s.plain);
    }

    // ------------------------------------------------------------------ ajutoare

    private String unclassifiedNote(PackagingDeclaration d) {
        long noMaterial = d.unclassified().stream()
                .filter(PackagingDeclaration.UnclassifiedRow::missingMaterial).count();
        long noCategory = d.unclassified().stream()
                .filter(x -> !x.missingMaterial() && x.missingCategory()).count();

        StringBuilder sb = new StringBuilder("Atenţie: ")
                .append(d.unclassified().size())
                .append(d.unclassified().size() == 1 ? " mişcare de ambalaje nu a intrat în tabel"
                        : " mişcări de ambalaje nu au intrat în tabel");
        if (noMaterial > 0) {
            sb.append(" — ").append(noMaterial).append(" fără materialul ambalajului (codul de "
                    + "deşeu nu îl decide singur)");
        }
        if (noCategory > 0) {
            sb.append(noMaterial > 0 ? ", " : " — ").append(noCategory)
                    .append(" fără felul ambalajului (desfacere / primar / secundar)");
        }
        return sb.append(". Completează-le în tabul Ambalaje şi regenerează.").toString();
    }

    private void number(Sheet sh, Styles s, int r, int c, BigDecimal value) {
        number(sh, s, r, c, value, false);
    }

    /** An unanswered figure prints as an empty bordered cell, never as 0. */
    private void number(Sheet sh, Styles s, int r, int c, BigDecimal value, boolean bold) {
        Cell cell = cellAt(sh, r, c);
        cell.setCellStyle(bold ? s.numberBold : s.number);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }

    private void blank(Sheet sh, Styles s, int r, int from, int to) {
        for (int c = from; c <= to; c++) {
            cellAt(sh, r, c).setCellStyle(s.cell);
        }
    }

    private void put(Sheet sh, int r, int c, String text, CellStyle style) {
        Cell cell = cellAt(sh, r, c);
        cell.setCellStyle(style);
        cell.setCellValue(text == null ? "" : text);
    }

    /** Merges a block and gives every cell in it the border style, so the frame stays closed. */
    private void merge(Sheet sh, int r1, int r2, int c1, int c2, CellStyle style) {
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                cellAt(sh, r, c).setCellStyle(style);
            }
        }
        if (r1 != r2 || c1 != c2) {
            sh.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
        }
    }

    private Cell cellAt(Sheet sh, int r, int c) {
        Row row = sh.getRow(r);
        if (row == null) {
            row = sh.createRow(r);
        }
        Cell cell = row.getCell(c);
        return cell == null ? row.createCell(c) : cell;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** One place for the handful of looks the sheet needs, built once per workbook. */
    private static final class Styles {
        final CellStyle title;
        final CellStyle plain;
        final CellStyle tableTitle;
        final CellStyle unit;
        final CellStyle head;
        final CellStyle label;
        final CellStyle labelBold;
        final CellStyle cell;
        final CellStyle cellCenter;
        final CellStyle number;
        final CellStyle numberBold;
        final CellStyle note;
        final CellStyle warning;

        Styles(Workbook wb) {
            Font bold = wb.createFont();
            bold.setBold(true);
            Font small = wb.createFont();
            small.setFontHeightInPoints((short) 8);
            Font warn = wb.createFont();
            warn.setFontHeightInPoints((short) 8);
            warn.setBold(true);

            title = wb.createCellStyle();
            title.setFont(bold);
            title.setWrapText(true);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            plain = wb.createCellStyle();
            plain.setVerticalAlignment(VerticalAlignment.CENTER);

            tableTitle = wb.createCellStyle();
            tableTitle.setFont(bold);

            unit = wb.createCellStyle();
            unit.setFont(small);
            unit.setAlignment(HorizontalAlignment.RIGHT);

            head = bordered(wb);
            head.setFont(bold);
            head.setWrapText(true);
            head.setAlignment(HorizontalAlignment.CENTER);
            head.setVerticalAlignment(VerticalAlignment.CENTER);

            label = bordered(wb);
            label.setVerticalAlignment(VerticalAlignment.CENTER);

            labelBold = bordered(wb);
            labelBold.setFont(bold);
            labelBold.setVerticalAlignment(VerticalAlignment.CENTER);

            cell = bordered(wb);
            cell.setWrapText(true);
            cell.setVerticalAlignment(VerticalAlignment.CENTER);

            cellCenter = bordered(wb);
            cellCenter.setAlignment(HorizontalAlignment.CENTER);
            cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);

            short kg = wb.createDataFormat().getFormat("#,##0.###");
            number = bordered(wb);
            number.setDataFormat(kg);
            number.setAlignment(HorizontalAlignment.RIGHT);

            numberBold = bordered(wb);
            numberBold.setDataFormat(kg);
            numberBold.setAlignment(HorizontalAlignment.RIGHT);
            numberBold.setFont(bold);

            note = wb.createCellStyle();
            note.setFont(small);
            note.setWrapText(true);
            note.setVerticalAlignment(VerticalAlignment.TOP);

            warning = wb.createCellStyle();
            warning.setFont(warn);
            warning.setWrapText(true);
            warning.setVerticalAlignment(VerticalAlignment.TOP);
        }

        private static CellStyle bordered(Workbook wb) {
            CellStyle style = wb.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
