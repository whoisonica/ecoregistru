package ro.ecoregistru.service.export;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * D3.5 — ce au în comun documentele de depozit tipărite: Helvetica în Cp1250 (ș/ț cu sedilă, singurele pe care le are
 * fontul standard) și ștampila de după: pe fiecare pagină rândul de semnături (Normele OMFP 2861/2009 pct. 33: lista se
 * semnează pe fiecare filă), „Pagina X din Y” și numele și versiunea programului (OMFP 2634/2015 anexa 1 pct. 58 lit.
 * k)). Documentul se scrie cu marginea de jos de cel puțin {@link #BOTTOM_MARGIN}, ca ștampila să nu calce pe text.
 */
public final class DepotPdf {

    /** Loc pentru semnături + subsol. */
    public static final float BOTTOM_MARGIN = 78f;

    private DepotPdf() {
    }

    public static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş').replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    public static BaseFont font(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA, "Cp1250",
                    BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica", ex);
        }
    }

    /**
     * Pune pe fiecare pagină rândul de semnături (fiecare rol cu numele lui și o linie), „Pagina X din Y” și
     * „Generat cu WasteHouse, versiunea …”.
     */
    public static byte[] stamp(byte[] pdf, List<String> signatures, String version) {
        Font small = new Font(font(false), 7f);
        Font sign = new Font(font(false), 7.5f);
        try {
            PdfReader reader = new PdfReader(pdf);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PdfStamper stamper = new PdfStamper(reader, out);
                int pages = reader.getNumberOfPages();
                for (int p = 1; p <= pages; p++) {
                    PdfContentByte over = stamper.getOverContent(p);
                    var size = reader.getPageSize(p);
                    float left = 28f;
                    float width = size.getWidth() - 56f;
                    if (!signatures.isEmpty()) {
                        float cell = width / signatures.size();
                        for (int i = 0; i < signatures.size(); i++) {
                            ColumnText ct = new ColumnText(over);
                            ct.setSimpleColumn(left + i * cell, 26f, left + (i + 1) * cell - 8f, 70f);
                            ct.addText(new Phrase(cp1250(signatures.get(i) + "\nSemnătura: ____________________"), sign));
                            ct.go();
                        }
                    }
                    ColumnText.showTextAligned(over, Element.ALIGN_LEFT,
                            new Phrase(cp1250("Generat cu WasteHouse (app.wastehouse.ro), versiunea " + version), small),
                            left, 12f, 0);
                    ColumnText.showTextAligned(over, Element.ALIGN_RIGHT,
                            new Phrase(cp1250("Pagina " + p + " din " + pages), small), left + width, 12f, 0);
                }
                stamper.close();
                return out.toByteArray();
            } finally {
                reader.close();
            }
        } catch (IOException | DocumentException ex) {
            throw new IllegalStateException("Cannot stamp the depot document", ex);
        }
    }
}
