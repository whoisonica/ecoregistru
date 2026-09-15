package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * P2.14 — the consultancy's header on an <b>unofficial</b> report: its logo, „Pregătit de {cabinet}"
 * and its own header line. Null means a direct client, or a consultancy that set nothing, and then
 * nothing is printed — the page stays exactly what it was.
 *
 * <p>Never called from a generator of an official form: those print the model of the act and nothing else.
 */
@Slf4j
public record ReportBranding(String consultancyName, String headerLine, byte[] logo) {

    /** Height the logo is fitted into on a PDF, in points (~14 mm). */
    private static final float PDF_LOGO_HEIGHT = 40f;
    private static final float PDF_LOGO_WIDTH = 140f;
    /** Height the logo is scaled to on a sheet, in pixels (about three rows). */
    private static final double XLSX_LOGO_HEIGHT_PX = 60d;

    /** Set but empty — a row with neither logo nor line — prints like no branding at all. */
    public boolean isEmpty() {
        return logo == null && (headerLine == null || headerLine.isBlank());
    }

    public String preparedBy() {
        return "Pregătit de " + consultancyName;
    }

    /** One line for text files: „Pregătit de X · 0722 000 000". */
    public String textLine() {
        return headerLine == null || headerLine.isBlank() ? preparedBy() : preparedBy() + " · " + headerLine.strip();
    }

    // --- PDF ---

    /** The header band above the page's own title: logo left, the cabinet right, a thin rule under. */
    public static void addPdfHeader(Document doc, ReportBranding branding) throws DocumentException {
        if (branding == null) {
            return;
        }
        PdfPTable band = new PdfPTable(2);
        band.setWidthPercentage(100);
        band.setWidths(new float[]{1, 2});
        band.setSpacingAfter(8f);

        Image image = branding.pdfImage();
        PdfPCell left = image == null ? new PdfPCell(new Phrase("")) : new PdfPCell(image, false);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);
        band.addCell(banded(left));

        PdfPCell right = new PdfPCell();
        Paragraph who = new Paragraph(cp1250(branding.preparedBy()), new Font(centralEuropean(true), 9));
        who.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(who);
        if (branding.headerLine() != null && !branding.headerLine().isBlank()) {
            Paragraph line = new Paragraph(cp1250(branding.headerLine().strip()),
                    new Font(centralEuropean(false), 8, Font.NORMAL, new Color(0x4B, 0x55, 0x63)));
            line.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(line);
        }
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        band.addCell(banded(right));

        doc.add(band);
    }

    private static PdfPCell banded(PdfPCell cell) {
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(0xD1, 0xD5, 0xDB));
        cell.setPaddingBottom(6f);
        return cell;
    }

    /** The logo, or null if it cannot be read — a report is never refused over its decoration. */
    private Image pdfImage() {
        if (logo == null) {
            return null;
        }
        try {
            Image image = Image.getInstance(logo);
            image.scaleToFit(PDF_LOGO_WIDTH, PDF_LOGO_HEIGHT);
            return image;
        } catch (IOException | RuntimeException ex) {
            log.warn("Logo de cabinet ilizibil pentru {}: {}", consultancyName, ex.getMessage());
            return null;
        }
    }

    // --- XLSX ---

    /**
     * „Pregătit de …" in the first row, the logo anchored to the right of the header block. Returns the
     * row the sheet's own header starts on.
     */
    public static int addXlsxHeader(Workbook wb, Sheet sheet, int anchorColumn, ReportBranding branding) {
        if (branding == null) {
            return 0;
        }
        sheet.createRow(0).createCell(0).setCellValue(branding.textLine());
        if (branding.logo() != null) {
            try {
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(branding.logo()));
                if (decoded != null) {
                    int type = decoded.getColorModel().hasAlpha() || isPng(branding.logo())
                            ? Workbook.PICTURE_TYPE_PNG : Workbook.PICTURE_TYPE_JPEG;
                    int index = wb.addPicture(branding.logo(), type);
                    CreationHelper helper = wb.getCreationHelper();
                    Drawing<?> drawing = sheet.createDrawingPatriarch();
                    ClientAnchor anchor = helper.createClientAnchor();
                    anchor.setCol1(anchorColumn);
                    anchor.setRow1(0);
                    Picture picture = drawing.createPicture(anchor, index);
                    picture.resize(Math.min(1d, XLSX_LOGO_HEIGHT_PX / decoded.getHeight()));
                }
            } catch (IOException | RuntimeException ex) {
                log.warn("Logo de cabinet ilizibil pentru {}: {}", branding.consultancyName(), ex.getMessage());
            }
        }
        return 2;
    }

    static boolean isPng(byte[] bytes) {
        return bytes.length > 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
    }

    // --- fonts, as the other generators ---

    private static String cp1250(String value) {
        return value.replace('ș', 'ş').replace('Ș', 'Ş').replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont centralEuropean(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    "Cp1250", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica for the report header", ex);
        }
    }
}
