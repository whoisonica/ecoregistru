package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import static ro.ecoregistru.service.export.DepotPdf.cp1250;

/**
 * D4.7 — un {@link DepotReport} ca PDF semnabil: A4 culcat (rapoartele au multe coloane), titlul, firma · perioada ·
 * depozitul, secțiunile ca tabele cu antetul repetat pe fiecare pagină, notele, apoi ștampila comună a documentelor de
 * depozit ({@link DepotPdf#stamp}: semnăturile pe fiecare filă, „Pagina X din Y”, programul și versiunea).
 */
@Component
public class DepotReportPdf {

    private final AppVersion version;
    private final Font title = new Font(DepotPdf.font(true), 12f);
    private final Font body = new Font(DepotPdf.font(false), 8.5f);
    private final Font bold = new Font(DepotPdf.font(true), 8.5f);
    private final Font head = new Font(DepotPdf.font(true), 7f);
    private final Font cell = new Font(DepotPdf.font(false), 7f);
    private final Font cellBold = new Font(DepotPdf.font(true), 7f);

    public DepotReportPdf(AppVersion version) {
        this.version = version;
    }

    public byte[] render(DepotReport report) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, DepotPdf.BOTTOM_MARGIN);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Paragraph heading = new Paragraph(cp1250(report.title()), title);
            heading.setSpacingAfter(4f);
            doc.add(heading);
            for (String line : report.heading()) {
                doc.add(new Paragraph(cp1250(line), body));
            }
            for (DepotReport.Section section : report.sections()) {
                doc.add(table(section));
            }
            for (String note : report.notes()) {
                Paragraph p = new Paragraph(cp1250(note), body);
                p.setSpacingBefore(4f);
                doc.add(p);
            }
            doc.close();
            return DepotPdf.stamp(out.toByteArray(), report.signatures(), version.value());
        } catch (IOException | DocumentException ex) {
            throw new IllegalStateException("Cannot build the depot report (pdf)", ex);
        }
    }

    /**
     * Titlul secțiunii e primul rând de antet al tabelului, nu un paragraf deasupra: altfel poate rămâne singur jos pe o
     * pagină, cu tabelul lui pe următoarea. Ca antet, se repetă și pe paginile pe care continuă tabelul.
     */
    private PdfPTable table(DepotReport.Section section) {
        List<DepotReport.Column> columns = section.columns();
        PdfPTable t = new PdfPTable(widths(columns));
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);
        if (section.title() != null) {
            PdfPCell caption = new PdfPCell(new Phrase(cp1250(section.title()), bold));
            caption.setColspan(columns.size());
            caption.setBorder(PdfPCell.NO_BORDER);
            caption.setPaddingBottom(4f);
            t.addCell(caption);
        }
        t.setHeaderRows(section.title() == null ? 1 : 2);
        for (DepotReport.Column c : columns) {
            PdfPCell h = new PdfPCell(new Phrase(cp1250(c.name()), head));
            h.setGrayFill(0.9f);
            t.addCell(h);
        }
        if (section.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase(cp1250(section.empty()), cell));
            empty.setColspan(columns.size());
            t.addCell(empty);
        }
        for (List<Object> row : section.rows()) {
            addRow(t, columns, row, cell);
        }
        if (section.total() != null) {
            addRow(t, columns, section.total(), cellBold);
        }
        return t;
    }

    /** Textul ia loc, numerele mai puțin, un număr de ordine cel mai puțin. */
    private static float[] widths(List<DepotReport.Column> columns) {
        float[] w = new float[columns.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = switch (columns.get(i).kind()) {
                case TEXT -> 3f;
                case KG, LEI -> 2f;
                case INT -> 1.2f;
            };
        }
        return w;
    }

    private static void addRow(PdfPTable t, List<DepotReport.Column> columns, List<Object> values, Font font) {
        for (int c = 0; c < columns.size(); c++) {
            Object value = c < values.size() ? values.get(c) : null;
            PdfPCell pc = new PdfPCell(new Phrase(cp1250(text(value, columns.get(c).kind())), font));
            if (!(value instanceof String)) {
                pc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            t.addCell(pc);
        }
    }

    /** Numerele cu virgulă zecimală și punct la mii, cum se citesc în România. */
    static String text(Object value, DepotReport.Kind kind) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal number) {
            DecimalFormat f = new DecimalFormat(kind == DepotReport.Kind.LEI ? "#,##0.00" : "#,##0.###",
                    DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ro-RO")));
            f.setRoundingMode(RoundingMode.HALF_UP);
            return f.format(number);
        }
        return value.toString();
    }
}
