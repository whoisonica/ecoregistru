package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Declarația care însoțește Anexa 1 energie, ca PDF — geamănul de citit și de tipărit al lui
 * {@link EnergyDeclarationDocxGenerator}, din 05.10.2026 (Chrome nu arată un .docx). Același conținut în aceeași
 * ordine: antetul firmei cu aldine, „Nr. ........../...............”, titlul „Declarație”, fraza cu numele firmei
 * îngroșat și tabelul de semnături pe trei coloane, fără chenare. A4, margini de 2,54 cm, ca în Word; Trebuchet MS nu
 * e în PDF-ul standard, așa că literele sunt Helvetica în Cp1250 ({@link DepotPdf}), ca la celelalte formulare. Un
 * câmp lipsă al firmei își lasă rândul fără valoare, iar data transmiterii rămâne goală pentru cel care semnează.
 */
@Component
public class EnergyDeclarationPdfGenerator {

    private static final float MARGIN = 72f;

    private final Font regular;
    private final Font bold;

    public EnergyDeclarationPdfGenerator() {
        BaseFont plain = DepotPdf.font(false);
        BaseFont strong = DepotPdf.font(true);
        this.regular = new Font(plain, 11f);
        this.bold = new Font(strong, 11f);
    }

    /** {@code year} nu se tipărește: Declarația nu poartă an, dar semnătura e aceeași ca la geamănul .docx. */
    public byte[] render(Company company, int year) {
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            String name = nz(company.getName());
            header(doc, name);
            header(doc, "Sediul social: " + nz(company.getAddress()));
            header(doc, "C.U.I.: " + nz(company.getCui()));
            header(doc, nz(company.getTradeRegisterNumber()));
            String fax = nz(company.getFax());
            String phone = nz(company.getContactPhone());
            header(doc, "Telefon" + (phone.isBlank() ? "" : " " + phone) + (fax.isBlank() ? "" : ", fax " + fax));
            header(doc, "Mail: " + nz(company.getContactEmail()));
            header(doc, "Nr. ........../...............");

            Paragraph title = new Paragraph(DepotPdf.cp1250("Declarație"), bold);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(26f);
            title.setSpacingAfter(26f);
            doc.add(title);

            Paragraph sentence = new Paragraph();
            sentence.setAlignment(Element.ALIGN_JUSTIFIED);
            sentence.setLeading(15f);
            sentence.add(new Phrase(DepotPdf.cp1250("Subscrisa "), regular));
            sentence.add(new Phrase(DepotPdf.cp1250(name), bold));
            sentence.add(new Phrase(DepotPdf.cp1250(" prin prezenta declarăm faptul că informațiile prezentate în "
                    + "Anexa de raportare sunt corecte și conforme cu realitatea."), regular));
            sentence.setSpacingAfter(40f);
            doc.add(sentence);

            doc.add(signatures(nz(company.getEnergyContactName())));
            doc.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PdfPTable signatures(String contactName) {
        String[][] rows = {
                {"NUMELE ÎN CLAR ŞI SEMNĂTURA CONDUCĂTORULUI UNITĂŢII",
                        "NUMELE ÎN CLAR ŞI SEMNĂTURA PERSOANEI DE CONTACT", "DATA TRANSMITERII"},
                {"ŞTAMPILA UNITĂŢII", "MANAGER ENERGETIC SAU A CONDUCĂTORULUI COMPARTIMENTULUI TEHNIC", ""},
                {"………………………….", contactName, ""}};
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        for (int r = 0; r < rows.length; r++) {
            for (String text : rows[r]) {
                PdfPCell c = new PdfPCell(new Phrase(DepotPdf.cp1250(text), r == 0 ? bold : regular));
                c.setBorder(Rectangle.NO_BORDER);
                c.setPaddingLeft(0f);
                c.setPaddingRight(8f);
                c.setPaddingBottom(r == 1 ? 18f : 4f);
                c.setHorizontalAlignment(Element.ALIGN_LEFT);
                t.addCell(c);
            }
        }
        return t;
    }

    private void header(Document doc, String text) {
        doc.add(new Paragraph(DepotPdf.cp1250(text), bold));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
