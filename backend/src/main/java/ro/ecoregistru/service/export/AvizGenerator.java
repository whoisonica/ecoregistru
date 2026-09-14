package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
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
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.PartnerWorkPoint;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.Unit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Avizul de însoţire a mărfii, tipărit dintr-o predare — cerut de specialistă pe 15.09.2026, după
 * modelul pe care l-a trimis (ALPIN Digital, „AVZ-ALPCJ-A / 14"): şase secţiuni, antet, expeditor,
 * destinatar, date de transport, poziţiile şi semnăturile.
 *
 * <p>Nu e un formular din legislaţia deşeurilor, deci nu are rubrici impuse; urmează modelul primit.
 * Seria şi numărul avizului sunt <b>referinţa documentului</b> de pe mişcare, adică numărul pe care
 * clientul îl scrie deja la „Observaţii" pe Anexa 3. Aplicaţia nu alocă numere de aviz: seria unui
 * aviz e a firmei, iar un număr inventat aici s-ar ciocni cu carnetul sau cu programul de facturare.
 *
 * <p>La şofer se tipăreşte ca pe model: „Nume | CNP … | seria actului", cu ce ţine mişcarea. CNP-ul
 * are rubrica lui din V42, cerută de specialistă pe 15.09.2026.
 */
@Component
public class AvizGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String DOCUMENT_TYPE = "Aviz de însoţire a mărfii";

    private final Font title;
    private final Font section;
    private final Font label;
    private final Font body;
    private final Font bold;

    public AvizGenerator() {
        BaseFont regular = helvetica(false);
        BaseFont heavy = helvetica(true);
        this.title = new Font(heavy, 15);
        this.section = new Font(heavy, 9);
        this.label = new Font(regular, 7, Font.NORMAL, new java.awt.Color(0x55, 0x55, 0x55));
        this.body = new Font(regular, 9);
        this.bold = new Font(heavy, 9);
    }

    public byte[] render(WasteMovement m, Company sender) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph heading = new Paragraph(cp1250("AVIZ DE ÎNSOŢIRE A MĂRFII"), title);
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingAfter(8f);
            doc.add(heading);

            String number = m.getDocumentReference();
            doc.add(sectionTitle("1. Antet document"));
            doc.add(row(new float[]{40, 30, 30},
                    field("Serie / număr aviz", number),
                    field("Data emitere", date(m.getDate())),
                    field("Data transport", date(m.loadingDate()))));

            doc.add(sectionTitle("2. Expeditor"));
            doc.add(row(new float[]{40, 30, 30},
                    field("Denumire", sender.getName()),
                    field("CUI", sender.getCui()),
                    field("Nr. reg. comerţ", sender.getTradeRegisterNumber())));
            doc.add(row(new float[]{100},
                    field("Adresă / localitate / judeţ", senderAddress(m, sender))));

            Partner recipient = m.getPartner();
            doc.add(sectionTitle("3. Destinatar"));
            doc.add(row(new float[]{40, 30, 30},
                    field("Denumire", recipient.getName()),
                    field("CUI", recipient.getCui()),
                    field("Nr. reg. comerţ", recipient.getTradeRegisterNumber())));
            doc.add(row(new float[]{100},
                    field("Adresă / localitate / judeţ", recipientAddress(m, recipient))));

            doc.add(sectionTitle("4. Date transport"));
            doc.add(row(new float[]{70, 30},
                    field("Tip document transport", DOCUMENT_TYPE),
                    field("Număr document transport", number)));
            doc.add(row(new float[]{70, 30},
                    field("Nume şofer", joined(" | ", m.getDriverName(),
                            m.getDriverCnp() == null ? null : "CNP " + m.getDriverCnp(),
                            m.getDriverIdentification())),
                    field("Număr auto / remorcă", m.getVehicleRegistration())));

            doc.add(sectionTitle("5. Poziţii marfă / deşeuri"));
            doc.add(positions(m));

            doc.add(sectionTitle("6. Validare şi semnare"));
            doc.add(signatures());

            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the aviz", ex);
        }
    }

    private PdfPTable positions(WasteMovement m) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        setWidths(table, new float[]{50, 18, 22, 10});
        table.addCell(line(new Phrase(cp1250("Denumire"), bold), Element.ALIGN_LEFT));
        table.addCell(line(new Phrase(cp1250("Cod"), bold), Element.ALIGN_LEFT));
        table.addCell(line(new Phrase(cp1250("Cantitate"), bold), Element.ALIGN_RIGHT));
        table.addCell(line(new Phrase(cp1250("UM"), bold), Element.ALIGN_LEFT));

        table.addCell(line(new Phrase(cp1250(m.getWasteCode().getName()), body), Element.ALIGN_LEFT));
        table.addCell(line(new Phrase(cp1250(m.getWasteCode().getCode()), body), Element.ALIGN_LEFT));
        // Fără cantitate când cântăreşte destinatarul — acelaşi motiv ca pe Anexa 3: o cifră
        // inventată pe un document care pleacă cu camionul e mai rea decât un loc gol.
        String quantity = m.getQuantity() == null ? "" : quantity(m.getQuantity());
        table.addCell(line(new Phrase(quantity, body), Element.ALIGN_RIGHT));
        table.addCell(line(new Phrase(unit(m.getUnit()), body), Element.ALIGN_LEFT));
        table.setSpacingAfter(10f);
        return table;
    }

    private PdfPTable signatures() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        for (String who : List.of("Semnătură emitent", "Semnătură primitor")) {
            PdfPCell cell = new PdfPCell(new Phrase(cp1250(who), bold));
            cell.setBorder(Rectangle.BOTTOM);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPaddingBottom(40f);
            table.addCell(cell);
        }
        return table;
    }

    /** Sediul, plus punctul de lucru când încărcarea are loc în altă parte — ca pe model. */
    private String senderAddress(WasteMovement m, Company sender) {
        String office = sender.getAddress();
        String workPoint = m.getWorkPoint() == null ? null : m.getWorkPoint().getAddress();
        if (workPoint == null || workPoint.isBlank() || workPoint.equals(office)) {
            return office;
        }
        return office == null || office.isBlank()
                ? "Punct de lucru: " + workPoint
                : "Sediu: " + office + ". Punct de lucru: " + workPoint;
    }

    /** Unde a ajuns marfa: punctul de lucru ales, singurul pe care îl are, altfel sediul. */
    private String recipientAddress(WasteMovement m, Partner recipient) {
        if (m.getPartnerWorkPoint() != null) {
            return m.getPartnerWorkPoint().label();
        }
        List<PartnerWorkPoint> points = recipient.getWorkPoints();
        if (points != null && points.size() == 1) {
            return points.get(0).label();
        }
        return recipient.getAddress();
    }

    // --- layout helpers ---

    private Paragraph sectionTitle(String text) {
        Paragraph p = new Paragraph(cp1250(text), section);
        p.setSpacingBefore(6f);
        p.setSpacingAfter(3f);
        return p;
    }

    private PdfPTable row(float[] widths, PdfPCell... cells) {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidthPercentage(100);
        setWidths(table, widths);
        for (PdfPCell cell : cells) {
            table.addCell(cell);
        }
        return table;
    }

    private PdfPCell field(String name, String value) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(cp1250(name) + "\n", label));
        phrase.add(new Phrase(cp1250(value == null ? "" : value), body));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(new java.awt.Color(0xBB, 0xBB, 0xBB));
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell line(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new java.awt.Color(0xBB, 0xBB, 0xBB));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        return cell;
    }

    private static void setWidths(PdfPTable table, float[] widths) {
        try {
            table.setWidths(widths);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Bad column widths for the aviz", ex);
        }
    }

    private static String joined(String separator, String... parts) {
        return String.join(separator, java.util.Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .toList());
    }

    /** 7.800,000 — românește, cu trei zecimale, cum scrie modelul. */
    static String quantity(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.000", DecimalFormatSymbols.getInstance(
                Locale.forLanguageTag("ro-RO")));
        return format.format(value);
    }

    private static String unit(Unit unit) {
        return unit == Unit.TONS ? "t" : "kg";
    }

    private static String date(LocalDate value) {
        return value == null ? "" : value.format(DATE);
    }

    /** Cp1250 are ş/ţ cu sedilă, nu cu virgulă; fără pliere literele ar dispărea din PDF. */
    private static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş')
                .replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont helvetica(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    "Cp1250", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica for the aviz", ex);
        }
    }
}
