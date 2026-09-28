package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Prints {@link Anexa3Register} as a PDF to hand to an inspector: one table, the columns the
 * specialist named, in her order (29.09.2026). Same drawing conventions as
 * {@link Art48RegisterGenerator}: A4 landscape, Cp1250 Helvetica, a header that says what the
 * document is and what it is not.
 */
@Component
public class Anexa3RegisterGenerator {

    static final String TITLE = "Registrul formularelor de transport — Anexa 3 la HG 1061/2008";
    static final String BASIS = "Centralizator al formularelor de încărcare-descărcare deșeuri nepericuloase emise "
            + "(HG 1061/2008 art. 20), în ordinea numerelor alocate. Actul nu prevede un model pentru expeditor; "
            + "forma urmează practica de control.";
    static final String EMPTY = "Niciun formular emis în ";

    static final String[] COLUMNS = {
            "Nr. crt.", "Data", "Seria și nr. formular", "Cantitate (kg)", "Cod deșeu", "Denumire deșeu",
            "Predat către", "CUI destinatar", "Cod R/D"
    };
    private static final float[] WIDTHS = {5, 8, 12, 9, 8, 26, 18, 9, 5};

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] pdf(Anexa3Register r) {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            BaseFont plain = centralEuropean(false);
            BaseFont boldBase = centralEuropean(true);
            Font title = new Font(boldBase, 11f);
            Font heading = new Font(boldBase, 9f);
            Font head = new Font(boldBase, 7f);
            Font body = new Font(plain, 7f);
            Font small = new Font(plain, 7f);

            doc.add(new Paragraph(cp1250(company(r)), heading));
            doc.add(new Paragraph(cp1250(TITLE), title));
            doc.add(new Paragraph(cp1250(period(r)), small));
            Paragraph basis = new Paragraph(cp1250(BASIS), small);
            basis.setSpacingAfter(8f);
            doc.add(basis);

            if (r.rows().isEmpty()) {
                doc.add(new Paragraph(cp1250(EMPTY + r.year() + "."), small));
            } else {
                PdfPTable t = table(head);
                for (Anexa3Register.Row row : r.rows()) {
                    number(t, body, String.valueOf(row.position()));
                    cell(t, body, row.date() == null ? "" : DATE.format(row.date()));
                    cell(t, body, series(row));
                    number(t, body, kgText(row.kg()));
                    cell(t, body, row.wasteCode());
                    cell(t, body, row.wasteName());
                    cell(t, body, row.recipient());
                    cell(t, body, row.recipientCui());
                    cell(t, body, row.operationCode());
                }
                doc.add(t);
                Paragraph total = new Paragraph(cp1250(r.rows().size() + " formulare emise."), small);
                total.setSpacingBefore(6f);
                doc.add(total);
            }
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot render the Anexa 3 register", ex);
        }
    }

    /** „RTS 17” — seria firmei şi numărul alocat; fără serie, numărul singur. */
    static String series(Anexa3Register.Row row) {
        return row.series() == null || row.series().isBlank()
                ? String.valueOf(row.number())
                : row.series() + " " + row.number();
    }

    private static PdfPTable table(Font font) {
        PdfPTable t = new PdfPTable(COLUMNS.length);
        t.setWidthPercentage(100);
        t.setWidths(WIDTHS);
        t.setHeaderRows(1);
        for (String c : COLUMNS) {
            PdfPCell cell = new PdfPCell(new Phrase(cp1250(c), font));
            cell.setPadding(3f);
            cell.setBackgroundColor(new java.awt.Color(0xEC, 0xFD, 0xF5));
            t.addCell(cell);
        }
        return t;
    }

    private static void cell(PdfPTable t, Font font, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(cp1250(value), font));
        cell.setPadding(2.5f);
        t.addCell(cell);
    }

    private static void number(PdfPTable t, Font font, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(2.5f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(cell);
    }

    private static String company(Anexa3Register r) {
        return r.companyCui() == null ? r.companyName() : r.companyName() + " · CUI " + r.companyCui();
    }

    private static String period(Anexa3Register r) {
        return "Anul " + r.year() + " · "
                + (r.workPointName() == null ? "toate punctele de lucru" : "Punct de lucru: " + r.workPointName());
    }

    private static String kgText(BigDecimal kg) {
        return kg == null ? "" : new DecimalFormat("#,##0.###",
                DecimalFormatSymbols.getInstance(Locale.of("ro", "RO"))).format(kg);
    }

    /** Cp1250 has the cedilla forms of ş/ţ only; the comma-below ones would drop out of the PDF. */
    private static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş').replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont centralEuropean(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    "Cp1250", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 base font", ex);
        }
    }
}
