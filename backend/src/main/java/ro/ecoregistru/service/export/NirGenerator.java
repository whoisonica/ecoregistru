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
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.WeighingOperationStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ro.ecoregistru.service.export.DepotPdf.cp1250;

/**
 * D1.17a — nota de recepție și constatare de diferențe (NIR, 14-3-1A, OMFP 2634/2015 anexa 2) pentru deșeurile preluate
 * gratuit de la o persoană fizică: „bunuri materiale care sosesc neînsoțite de documente de livrare”
 * ({@code surse-oficiale.md} §15.3). Un borderou cu preț 0 ar descrie o cumpărare care n-a avut loc.
 *
 * <p>Prețul și valoarea rămân goale: bunul primit gratuit intră la valoarea justă (OMFP 1802/2014 pct. 75 alin. (1)
 * lit. d)), pe care o stabilește contabilul (decizia proprietarului, 27.09.2026). Deci NIR-ul n-are nicio sumă și îl
 * poate tipări și cine nu vede prețurile. CNP-ul apare numai la metal, ca pe borderou.
 */
@Component
public class NirGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String[] COLUMNS = {"Nr. crt.", "Denumirea bunurilor (sortiment, cod deșeu)", "U/M",
            "Cantitate conform documentelor", "Cantitate recepționată", "Preț unitar", "Valoare"};
    private static final String BLANK = "..................";

    private final AppVersion version;
    private final Font title;
    private final Font body;
    private final Font small;
    private final Font head;

    public NirGenerator(AppVersion version) {
        this.version = version;
        title = new Font(DepotPdf.font(true), 12f);
        body = new Font(DepotPdf.font(false), 9f);
        small = new Font(DepotPdf.font(false), 7.5f);
        head = new Font(DepotPdf.font(true), 7f);
    }

    /** @param freeLines doar liniile preluate gratuit ({@link BorderouGenerator#free}) */
    public byte[] render(WeighingOperation op, List<WasteMovement> freeLines, Company company) {
        boolean metal = freeLines.stream().anyMatch(l -> l.getArticle() != null && l.getArticle().isMetal());
        NaturalPerson person = op.getNaturalPerson();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 28, 28, 28, DepotPdf.BOTTOM_MARGIN);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph(cp1250("Unitatea: " + company.getName()
                    + (company.getCui() == null ? "" : " · CUI " + company.getCui())), body));
            doc.add(new Paragraph(cp1250("Gestiunea: " + op.getWorkPoint().getName()), body));

            Paragraph heading = new Paragraph(cp1250("NOTĂ DE RECEPȚIE ȘI CONSTATARE DE DIFERENȚE"), title);
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingBefore(12f);
            doc.add(heading);
            Paragraph number = new Paragraph(cp1250("Nr. " + op.getReceptionNoteNumber() + " din data "
                    + op.getDate().format(DATE)), body);
            number.setAlignment(Element.ALIGN_CENTER);
            number.setSpacingAfter(10f);
            doc.add(number);
            if (op.getStatus() == WeighingOperationStatus.CANCELLED) {
                Paragraph band = new Paragraph(cp1250("ANULAT — " + op.getCancelReason()), title);
                band.setAlignment(Element.ALIGN_CENTER);
                band.setSpacingAfter(10f);
                doc.add(band);
            }

            doc.add(new Paragraph(cp1250("Furnizor: " + person.getName()
                    + (metal ? ", CNP " + orBlank(person.getCnp()) : "")), body));
            doc.add(new Paragraph(cp1250("Document de livrare: fără document — preluare gratuită de la persoană fizică"),
                    body));

            doc.add(table(freeLines));
            Paragraph note = new Paragraph(cp1250("Valoarea justă se stabilește de contabil (OMFP 1802/2014 pct. 75 "
                    + "alin. (1) lit. d))."), small);
            note.setSpacingBefore(4f);
            doc.add(note);
            doc.close();
            return DepotPdf.stamp(out.toByteArray(),
                    List.of("Comisia de recepție:\n" + BLANK + "\n" + BLANK + "\n" + BLANK,
                            "Primit în gestiune:\nGestionar, data " + BLANK),
                    version.value());
        } catch (IOException | DocumentException ex) {
            throw new IllegalStateException("Cannot build the NIR", ex);
        }
    }

    private PdfPTable table(List<WasteMovement> lines) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{5, 36, 5, 13, 13, 12, 12});
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);
        for (String c : COLUMNS) {
            PdfPCell cell = new PdfPCell(new Phrase(cp1250(c), head));
            cell.setGrayFill(0.9f);
            t.addCell(cell);
        }
        int no = 1;
        for (WasteMovement l : lines) {
            t.addCell(new Phrase(String.valueOf(no++), small));
            String name = (l.getArticle() == null ? l.getWasteCode().getName() : l.getArticle().getName())
                    + " (" + l.getWasteCode().getCode() + ")";
            t.addCell(new Phrase(cp1250(name), small));
            t.addCell(new Phrase("kg", small));
            t.addCell(right("—"));
            t.addCell(right(kg(l.getQuantity())));
            t.addCell(right(""));
            t.addCell(right(""));
        }
        return t;
    }

    private PdfPCell right(String text) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text), small));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static String kg(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String orBlank(String value) {
        return value == null || value.isBlank() ? BLANK : value;
    }
}
