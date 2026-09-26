package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.ReceivedForm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * D2.6 — registrul formularelor primite al unui depozit, ca PDF de tipărit și șnuruit: seria registrului în antet,
 * numărul de ordine pe fiecare rând și „Pagina X din Y” pe fiecare pagină (HG 1061/2008 art. 20 alin. (1): „înseriat
 * și numerotat”). Corecturile sunt rânduri proprii, care numesc rândul îndreptat; nimic nu se șterge din registru.
 */
@Component
public class ReceivedFormsRegisterGenerator {

    static final String TITLE = "Registrul formularelor de transport primite";
    static final String[] COLUMNS = {"Nr.", "Data primirii", "Formular (seria, nr., data)", "Expeditor", "Deșeu",
            "Cantitate (kg)", "Observații"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Font title;
    private final Font head;
    private final Font body;
    private final BaseFont plain;

    public ReceivedFormsRegisterGenerator() {
        plain = font(false);
        BaseFont bold = font(true);
        title = new Font(bold, 12);
        head = new Font(bold, 7.5f);
        body = new Font(plain, 7.5f);
    }

    public byte[] pdf(String company, String depot, String series, String period, List<ReceivedForm> rows) {
        Map<UUID, Integer> numberOf = new java.util.HashMap<>();
        rows.forEach(r -> numberOf.put(r.getId(), r.getEntryNo()));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Paragraph heading = new Paragraph(cp1250(TITLE), title);
            heading.setAlignment(Element.ALIGN_CENTER);
            doc.add(heading);
            Paragraph sub = new Paragraph(cp1250(company + " · " + depot + " · seria "
                    + (series == null || series.isBlank() ? "—" : series) + " · " + period), body);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(8f);
            doc.add(sub);

            PdfPTable t = new PdfPTable(new float[]{4, 7, 14, 18, 22, 8, 27});
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            for (String c : COLUMNS) {
                PdfPCell cell = new PdfPCell(new Phrase(cp1250(c), head));
                cell.setGrayFill(0.9f);
                t.addCell(cell);
            }
            if (rows.isEmpty()) {
                PdfPCell none = new PdfPCell(new Phrase(cp1250("Niciun formular primit în perioada aleasă."), body));
                none.setColspan(COLUMNS.length);
                t.addCell(none);
            }
            for (ReceivedForm r : rows) {
                t.addCell(new Phrase(String.valueOf(r.getEntryNo()), body));
                t.addCell(new Phrase(r.getReceivedOn().format(DATE), body));
                t.addCell(new Phrase(cp1250(form(r)), body));
                t.addCell(new Phrase(cp1250(r.getSenderName() + (r.getSenderCui() == null ? "" : ", CUI " + r.getSenderCui())), body));
                t.addCell(new Phrase(cp1250(r.getWasteDescription()), body));
                PdfPCell kg = new PdfPCell(new Phrase(kg(r.getQuantityKg()), body));
                kg.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(kg);
                t.addCell(new Phrase(cp1250(r.getCorrectsId() == null ? ""
                        : "Corectează rândul " + numberOf.getOrDefault(r.getCorrectsId(), 0) + ": " + r.getCorrectionReason()), body));
            }
            doc.add(t);
            doc.close();
            return numbered(out.toByteArray());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the received forms register", ex);
        }
    }

    /** „Pagina X din Y”, după ce se știe câte pagini are registrul. */
    private byte[] numbered(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfStamper stamper = new PdfStamper(reader, out);
            int pages = reader.getNumberOfPages();
            for (int p = 1; p <= pages; p++) {
                PdfContentByte over = stamper.getOverContent(p);
                var size = reader.getPageSize(p);
                ColumnText.showTextAligned(over, Element.ALIGN_CENTER,
                        new Phrase(cp1250("Pagina " + p + " din " + pages), body),
                        size.getWidth() / 2, 14, 0);
            }
            stamper.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Cannot number the received forms register", ex);
        } finally {
            reader.close();
        }
    }

    private static String form(ReceivedForm r) {
        String kind = "ANEXA_2".equals(r.getFormKind()) ? "Anexa 2" : "Anexa 3";
        String number = (r.getFormSeries() == null ? "" : r.getFormSeries() + " ") + r.getFormNumber();
        LocalDate date = r.getFormDate();
        return kind + " " + number + (date == null ? "" : " / " + date.format(DATE));
    }

    private static String kg(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş').replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont font(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA, "Cp1250",
                    BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica", ex);
        }
    }
}
