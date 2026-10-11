package ro.ecoregistru.service.export;

import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.EnvironmentalReportResponse;
import ro.ecoregistru.controller.response.EnvironmentalReportResponse.Permit;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The raport anual de mediu (RAM) as a Word file (Andreea, 09.10.2026), laid out on the specialist's own RAM: a cover
 * letter to the authority that issued the authorization, then the report in nine chapters, signed by the
 * administrator. Plain on purpose — black on white, thin tables, no brand header — because it goes out under the
 * company's name. The registration number and the date stay blank: the company writes them when it files.
 *
 * <p>An answer nobody gave prints a dotted line to fill by hand, never "nu a fost cazul": only a "Nu" says that.
 */
@Component
public class EnvironmentalReportDocxGenerator {

    private static final String FONT = "Trebuchet MS";
    private static final int A4_WIDTH = 11906;
    private static final int A4_HEIGHT = 16838;
    private static final int MARGIN = 1134;
    private static final int TEXT_WIDTH = A4_WIDTH - 2 * MARGIN;
    private static final String DOTS = "………………………………………………………………";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] render(EnvironmentalReportDocument d) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            page(doc);
            coverLetter(doc, d);
            pageBreak(doc);
            report(doc, d);
            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- Adresa de înaintare ------------------------------------------------------------------------------------

    private void coverLetter(XWPFDocument doc, EnvironmentalReportDocument d) {
        Company c = d.company();
        Permit permit = d.report().permit();
        identity(doc, c);
        line(doc, "Nr. înregistrare ........../...............", true, ParagraphAlignment.RIGHT);
        blank(doc);
        blank(doc);
        line(doc, "Către,", false, ParagraphAlignment.LEFT);
        line(doc, or(permit.issuer(), DOTS), true, ParagraphAlignment.LEFT);
        if (permit.copyTo() != null) {
            line(doc, "Spre știința " + permit.copyTo(), false, ParagraphAlignment.LEFT);
        }
        blank(doc);

        XWPFParagraph p = paragraph(doc, ParagraphAlignment.BOTH);
        run(p, "În conformitate cu cerințele autorizației de mediu nr. " + or(permit.number(), "..........")
                + " din " + date(permit.issuedOn()), false);
        if (permit.revisedOn() != null) {
            run(p, ", revizuită la " + date(permit.revisedOn()), false);
        }
        if (permit.visaOn() != null) {
            run(p, ", cu viza anuală din " + date(permit.visaOn()), false);
        }
        if (permit.issuer() != null) {
            run(p, ", eliberată de " + permit.issuer(), false);
        }
        run(p, ", care reglementează activitatea ", false);
        run(p, nz(c.getName()), true);
        run(p, ", vă transmitem alăturat raportul anual de mediu pentru anul " + d.year() + ".", false);

        blank(doc);
        blank(doc);
        signature(doc, permit, c);
    }

    // ---- Raportul -----------------------------------------------------------------------------------------------

    private void report(XWPFDocument doc, EnvironmentalReportDocument d) {
        Company c = d.company();
        EnvironmentalReportResponse r = d.report();
        int year = d.year();
        line(doc, "RAPORT ANUAL DE MEDIU", true, ParagraphAlignment.CENTER);
        line(doc, nz(c.getName()), true, ParagraphAlignment.CENTER);
        line(doc, "Anul " + year, true, ParagraphAlignment.CENTER);
        blank(doc);

        chapter(doc, "1. DATE DE IDENTIFICARE");
        identity(doc, c);
        if (c.getCaenCode() != null && !c.getCaenCode().isBlank()) {
            text(doc, "Cod CAEN: " + c.getCaenCode());
        }
        if (!d.workPoints().isEmpty()) {
            text(doc, "Puncte de lucru:");
            for (WorkPoint w : d.workPoints()) {
                text(doc, "– " + w.getName() + (w.getAddress() == null || w.getAddress().isBlank()
                        ? "" : ", " + w.getAddress()));
            }
        }
        text(doc, "Persoana de contact: " + or(c.getContactName(), DOTS));
        text(doc, "Telefon: " + or(c.getContactPhone(), DOTS) + ", e-mail: " + or(c.getContactEmail(), DOTS));

        chapter(doc, "2. INCIDENTE, ACCIDENTE, POLUĂRI ACCIDENTALE");
        answer(doc, r.incidents(), r.incidentsText(),
                "În anul " + year + " nu s-au înregistrat incidente, accidente sau poluări accidentale.");

        chapter(doc, "3. RECLAMAȚII DE MEDIU");
        answer(doc, r.complaints(), r.complaintsText(),
                "În anul " + year + " nu s-au înregistrat sesizări sau reclamații de mediu.");

        chapter(doc, "4. CONTROALE ALE AUTORITĂȚILOR ȘI MĂSURI DISPUSE");
        inspections(doc, r, year);

        chapter(doc, "5. ALTE CERINȚE PREVĂZUTE DE AUTORIZAȚIA DE MEDIU");
        text(doc, or(r.otherRequirements(), DOTS));

        chapter(doc, "6. GESTIUNEA DEȘEURILOR");
        waste(doc, d.waste(), year);
        packaging(doc, d.packaging(), year);

        chapter(doc, "7. MĂSURI ȘI ACȚIUNI PENTRU PREVENIREA ȘI REPARAREA PREJUDICIULUI ASUPRA MEDIULUI");
        answer(doc, r.damage(), r.damageText(), "Nu a fost cazul.");

        chapter(doc, "8. ASPECTE REFERITOARE LA AUTORIZAȚIA DE MEDIU");
        Permit permit = r.permit();
        text(doc, "Autorizația de mediu nr. " + or(permit.number(), "..........") + " din "
                + date(permit.issuedOn()) + (permit.issuer() == null ? "" : ", emisă de " + permit.issuer()) + ".");
        text(doc, permit.revisedOn() == null
                ? "Ultima revizuire: nu a fost revizuită."
                : "Ultima revizuire s-a făcut la data de " + date(permit.revisedOn()) + ".");
        text(doc, "Ultima viză anuală: " + date(permit.visaOn()) + ".");

        chapter(doc, "9. MONITORIZĂRI");
        monitorings(doc, r);

        blank(doc);
        line(doc, "Data: ...............", false, ParagraphAlignment.LEFT);
        blank(doc);
        signature(doc, permit, c);
    }

    private void inspections(XWPFDocument doc, EnvironmentalReportResponse r, int year) {
        if (Boolean.FALSE.equals(r.inspected())) {
            text(doc, "În anul " + year + " nu au avut loc controale ale Gărzii de Mediu sau ale altor instituții.");
            return;
        }
        if (r.inspections().isEmpty()) {
            text(doc, DOTS);
            return;
        }
        List<String[]> rows = new ArrayList<>();
        int n = 1;
        for (EnvironmentalReportResponse.Inspection i : r.inspections()) {
            rows.add(new String[]{String.valueOf(n++), nz(i.workPoint()), nz(i.institution()),
                    numberAndDate(i.reportNumber(), i.reportDate()), nz(i.measures())});
        }
        table(doc, new String[]{"Nr.", "Punct de lucru", "Instituția", "Raport de inspecție (nr./data)",
                "Măsuri dispuse și modul de rezolvare"}, new int[]{5, 17, 18, 18, 42}, rows);
    }

    private void monitorings(XWPFDocument doc, EnvironmentalReportResponse r) {
        if (r.monitorings().isEmpty()) {
            text(doc, "Nu a fost cazul.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        int n = 1;
        for (EnvironmentalReportResponse.Monitoring m : r.monitorings()) {
            rows.add(new String[]{String.valueOf(n++), nz(m.workPoint()), nz(m.factor()), date(m.performedOn()),
                    numberAndDate(m.testReportNumber(), m.testReportDate()), nz(m.result())});
        }
        table(doc, new String[]{"Nr.", "Punct de lucru", "Factorul monitorizat", "Data", "Raport de încercare (nr./data)",
                "Rezultat"}, new int[]{5, 17, 16, 12, 20, 30}, rows);
    }

    /** The "centralizator" of the annual waste declaration, one table per work point, in kilograms. */
    private void waste(XWPFDocument doc, List<AnnualDeclaration> declarations, int year) {
        text(doc, "Deșeuri generate — centralizatorul raportării anuale (HG 856/2002), în kilograme:");
        if (declarations.isEmpty() || declarations.stream().allMatch(a -> a.rows().isEmpty())) {
            text(doc, "În anul " + year + " nu s-au înregistrat deșeuri generate.");
            return;
        }
        for (AnnualDeclaration a : declarations) {
            if (a.rows().isEmpty()) {
                continue;
            }
            if (declarations.size() > 1) {
                text(doc, "Punct de lucru: " + a.workPointName(), true);
            }
            List<String[]> rows = new ArrayList<>();
            for (AnnualDeclaration.Row w : a.rows()) {
                rows.add(new String[]{w.wasteCode() + (w.hazardous() ? "*" : ""), nz(w.wasteCodeName()),
                        kg(w.openingStock()), kg(w.generated()), kg(w.recovered()), kg(w.disposed()),
                        kg(w.closingStock())});
            }
            table(doc, new String[]{"Cod deșeu", "Denumire", "Stoc inițial", "Generat", "Valorificat", "Eliminat",
                    "Stoc final"}, new int[]{11, 33, 11, 11, 12, 11, 11}, rows);
        }
    }

    /** Tabelul 1 and Tabelul 2 of Anexa 1 Ambalaje (Ordinul 794/2012), centralised, in kilograms. */
    private void packaging(XWPFDocument doc, PackagingDeclaration p, int year) {
        blank(doc);
        text(doc, "Ambalaje — centralizatorul raportării anuale (Ordinul 794/2012), în kilograme:");
        List<PackagingDeclaration.MarketRow> market = p.marketRows().stream().filter(m -> !m.isEmpty()).toList();
        if (market.isEmpty() && p.handoverRows().isEmpty()) {
            text(doc, "Nu a fost cazul.");
            return;
        }
        if (!market.isEmpty()) {
            text(doc, "Ambalaje introduse pe piața națională:", true);
            List<String[]> rows = new ArrayList<>();
            for (PackagingDeclaration.MarketRow m : market) {
                rows.add(new String[]{m.material().getOfficialLabel(), kg(m.salesPackaging()), kg(m.primaryTotal()),
                        kg(m.secondaryTotal()), kg(m.packagedGoodsTotal())});
            }
            table(doc, new String[]{"Materialul", "Ambalaje de desfacere", "Ambalaje primare",
                    "Ambalaje secundare și de transport", "Total ambalaje folosite"}, new int[]{24, 19, 19, 19, 19},
                    rows);
        }
        if (!p.handoverRows().isEmpty()) {
            text(doc, "Deșeuri de ambalaje încredințate unui operator economic autorizat:", true);
            List<String[]> rows = new ArrayList<>();
            for (PackagingDeclaration.HandoverRow h : p.handoverRows()) {
                rows.add(new String[]{h.material() == null ? "" : h.material().getOfficialLabel(), kg(h.quantity()),
                        nz(h.operatorName()), nz(h.operation())});
            }
            table(doc, new String[]{"Materialul", "Cantitatea", "Operatorul economic", "Operațiunea"},
                    new int[]{22, 16, 44, 18}, rows);
        }
    }

    // ---- Piese ------------------------------------------------------------------------------------------------

    private void identity(XWPFDocument doc, Company c) {
        line(doc, nz(c.getName()), true, ParagraphAlignment.LEFT);
        text(doc, "C.U.I.: " + nz(c.getCui()));
        if (c.getTradeRegisterNumber() != null && !c.getTradeRegisterNumber().isBlank()) {
            text(doc, c.getTradeRegisterNumber());
        }
        text(doc, "Sediul social: " + nz(c.getAddress()));
        if (c.getContactPhone() != null && !c.getContactPhone().isBlank()) {
            text(doc, "Telefon: " + c.getContactPhone());
        }
    }

    /** The administrator signs; the contact person, who prepared it, is named under "prin". */
    private void signature(XWPFDocument doc, Permit permit, Company c) {
        line(doc, "Administrator,", true, ParagraphAlignment.LEFT);
        line(doc, or(permit.administratorName(), DOTS), false, ParagraphAlignment.LEFT);
        line(doc, "Semnătura ...............", false, ParagraphAlignment.LEFT);
        if (c.getContactName() != null && !c.getContactName().isBlank()) {
            blank(doc);
            String role = c.getContactRole() == null || c.getContactRole().isBlank() ? "" : " – " + c.getContactRole();
            line(doc, "Prin " + c.getContactName() + role, false, ParagraphAlignment.LEFT);
        }
    }

    private void answer(XWPFDocument doc, Boolean yes, String text, String no) {
        if (Boolean.FALSE.equals(yes)) {
            text(doc, no);
        } else if (Boolean.TRUE.equals(yes) && text != null) {
            for (String part : text.split("\\R")) {
                text(doc, part);
            }
        } else {
            text(doc, DOTS);
        }
    }

    private void table(XWPFDocument doc, String[] head, int[] percents, List<String[]> rows) {
        XWPFTable t = doc.createTable(rows.size() + 1, head.length);
        t.setWidth(TEXT_WIDTH);
        for (int r = 0; r <= rows.size(); r++) {
            XWPFTableRow row = t.getRow(r);
            String[] values = r == 0 ? head : rows.get(r - 1);
            for (int col = 0; col < head.length; col++) {
                XWPFTableCell cell = row.getCell(col);
                cell.setWidth(String.valueOf(TEXT_WIDTH * percents[col] / 100));
                XWPFParagraph p = cell.getParagraphs().get(0);
                String[] lines = values[col].split("\\R", -1);
                for (int i = 0; i < lines.length; i++) {
                    XWPFRun run = p.createRun();
                    style(run, r == 0, 9);
                    if (i > 0) {
                        run.addBreak();
                    }
                    run.setText(lines[i]);
                }
            }
        }
        // Word lipește tabelul de paragraful următor: un rând gol îl desparte.
        blank(doc);
    }

    private static void chapter(XWPFDocument doc, String title) {
        blank(doc);
        line(doc, title, true, ParagraphAlignment.LEFT);
    }

    private static void text(XWPFDocument doc, String text) {
        text(doc, text, false);
    }

    private static void text(XWPFDocument doc, String text, boolean bold) {
        line(doc, text, bold, ParagraphAlignment.BOTH);
    }

    private static void line(XWPFDocument doc, String text, boolean bold, ParagraphAlignment alignment) {
        XWPFParagraph p = paragraph(doc, alignment);
        run(p, text, bold);
    }

    private static void blank(XWPFDocument doc) {
        paragraph(doc, ParagraphAlignment.LEFT);
    }

    private static void pageBreak(XWPFDocument doc) {
        paragraph(doc, ParagraphAlignment.LEFT).createRun().addBreak(BreakType.PAGE);
    }

    private static XWPFParagraph paragraph(XWPFDocument doc, ParagraphAlignment alignment) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alignment);
        p.setSpacingAfter(0);
        return p;
    }

    private static void run(XWPFParagraph p, String text, boolean bold) {
        XWPFRun r = p.createRun();
        style(r, bold, 11);
        r.setText(text);
    }

    private static void style(XWPFRun r, boolean bold, int size) {
        r.setFontFamily(FONT);
        r.setFontSize(size);
        r.setBold(bold);
    }

    private static void page(XWPFDocument doc) {
        CTSectPr sect = doc.getDocument().getBody().addNewSectPr();
        CTPageSz size = sect.addNewPgSz();
        size.setW(BigInteger.valueOf(A4_WIDTH));
        size.setH(BigInteger.valueOf(A4_HEIGHT));
        CTPageMar margins = sect.addNewPgMar();
        margins.setTop(BigInteger.valueOf(MARGIN));
        margins.setBottom(BigInteger.valueOf(MARGIN));
        margins.setLeft(BigInteger.valueOf(MARGIN));
        margins.setRight(BigInteger.valueOf(MARGIN));
    }

    private static String numberAndDate(String number, LocalDate date) {
        if (number == null && date == null) {
            return "";
        }
        return nz(number) + (date == null ? "" : " / " + date.format(DATE));
    }

    private static String date(LocalDate date) {
        return date == null ? ".........." : date.format(DATE);
    }

    /** Kilograms with Romanian separators; whole kilograms without ",000". */
    private static String kg(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormat f = new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(new Locale("ro", "RO")));
        return f.format(value);
    }

    private static String or(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
