package ro.ecoregistru.service.export;

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
import ro.ecoregistru.entity.Company;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;

/**
 * The Declarație that goes with Anexa 1, as a Word file laid out like the specialist's copy: A4, 2.54 cm margins,
 * Trebuchet MS 11, a bold header, the sentence, and a borderless three-column signature table. A missing company
 * field leaves its line without a value; the transmission date stays blank for the signer.
 */
@Component
public class EnergyDeclarationDocxGenerator {

    private static final String FONT = "Trebuchet MS";
    private static final int A4_WIDTH = 11906;
    private static final int A4_HEIGHT = 16838;
    private static final int MARGIN = 1440;
    private static final int COLUMN = (A4_WIDTH - 2 * MARGIN) / 3;

    public byte[] render(Company company, int year) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            page(doc);
            String name = nz(company.getName());
            header(doc, name);
            header(doc, "Sediul social: " + nz(company.getAddress()));
            header(doc, "C.U.I.: " + nz(company.getCui()));
            header(doc, nz(company.getTradeRegisterNumber()));
            String fax = nz(company.getFax());
            header(doc, "Telefon " + nz(company.getContactPhone()) + (fax.isBlank() ? "" : ", fax " + fax));
            header(doc, "Mail: " + nz(company.getContactEmail()));
            header(doc, "Nr. ........../...............");

            blank(doc);
            XWPFRun title = paragraph(doc, ParagraphAlignment.CENTER).createRun();
            style(title, true);
            title.setText("Declarație");
            blank(doc);

            XWPFParagraph sentence = paragraph(doc, ParagraphAlignment.BOTH);
            run(sentence, "Subscrisa ", false);
            run(sentence, name, true);
            run(sentence, " prin prezenta declarăm faptul că informațiile prezentate în Anexa de raportare "
                    + "sunt corecte și conforme cu realitatea.", false);

            blank(doc);
            blank(doc);
            signatures(doc, nz(company.getEnergyContactName()));
            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private static void signatures(XWPFDocument doc, String contactName) {
        String[][] rows = {
                {"NUMELE ÎN CLAR ŞI SEMNĂTURA CONDUCĂTORULUI UNITĂŢII",
                        "NUMELE ÎN CLAR ŞI SEMNĂTURA PERSOANEI DE CONTACT", "DATA TRANSMITERII"},
                {"ŞTAMPILA UNITĂŢII", "MANAGER ENERGETIC SAU A CONDUCĂTORULUI COMPARTIMENTULUI TEHNIC", ""},
                {"………………………….", contactName, ""}};
        XWPFTable table = doc.createTable(rows.length, 3);
        table.setWidth(COLUMN * 3);
        table.setTopBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setBottomBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setLeftBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setRightBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setInsideHBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        for (int r = 0; r < rows.length; r++) {
            XWPFTableRow row = table.getRow(r);
            for (int c = 0; c < 3; c++) {
                XWPFTableCell cell = row.getCell(c);
                cell.setWidth(String.valueOf(COLUMN));
                XWPFParagraph p = cell.getParagraphs().get(0);
                p.setAlignment(ParagraphAlignment.LEFT);
                run(p, rows[r][c], r == 0);
            }
        }
    }

    private static void header(XWPFDocument doc, String text) {
        run(paragraph(doc, ParagraphAlignment.LEFT), text, true);
    }

    private static void blank(XWPFDocument doc) {
        paragraph(doc, ParagraphAlignment.LEFT);
    }

    private static XWPFParagraph paragraph(XWPFDocument doc, ParagraphAlignment alignment) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alignment);
        return p;
    }

    private static void run(XWPFParagraph p, String text, boolean bold) {
        XWPFRun r = p.createRun();
        style(r, bold);
        r.setText(text);
    }

    private static void style(XWPFRun r, boolean bold) {
        r.setFontFamily(FONT);
        r.setFontSize(11);
        r.setBold(bold);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
