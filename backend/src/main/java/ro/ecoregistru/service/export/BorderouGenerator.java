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
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.PaymentMethod;
import ro.ecoregistru.service.DepotRetentions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * D1.11 — borderoul de achiziție de la o persoană fizică, după modelul din anexa la OUG 31/2011, rubrică
 * cu rubrică ({@code surse-oficiale.md} §9.1): operatorul, deținătorul, tabelul (0)–(4) cu TOTAL, plata,
 * reținerile, gestionarul primitor și declarația de gospodărie proprie.
 *
 * <p><b>Două abateri de la model, amândouă cerute de lege:</b>
 * <ul>
 *   <li><b>cotele în vigoare</b>, cu sumele calculate la finalizare, nu „16% și 3%” din 2011 (C2, §18.2):
 *       fraza nu e în conținutul obligatoriu, iar cifrele vechi ar declara o reținere care nu s-a făcut;
 *       impozitul apare numai când există linii de metal;</li>
 *   <li><b>fără CNP și act când nu e metal</b> (C3, §18.3): la hârtie sau plastic borderoul e la cerere, iar
 *       nicio lege nu cere acolo datele de identitate (Legea 190/2018 art. 4). Declarația de gospodărie
 *       proprie e tot a metalului (art. 1 alin. (1^1)).</li>
 * </ul>
 *
 * <p>Ce aplicația nu ține se tipărește gol, de completat de mână: data emiterii autorizației de mediu și
 * emitentul actului de identitate.
 */
@Component
public class BorderouGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String BLANK = "..................";

    private final Font title;
    private final Font body;
    private final Font bold;
    private final Font small;

    public BorderouGenerator() {
        BaseFont regular = helvetica(false);
        BaseFont heavy = helvetica(true);
        this.title = new Font(heavy, 13);
        this.body = new Font(regular, 9);
        this.bold = new Font(heavy, 9);
        this.small = new Font(regular, 7.5f);
    }

    public byte[] render(WeighingOperation op, List<WasteMovement> lines, Company operator) {
        boolean metal = lines.stream().anyMatch(l -> l.getArticle() != null && l.getArticle().isMetal());
        NaturalPerson person = op.getNaturalPerson();
        Document doc = new Document(PageSize.A4, 40, 40, 36, 36);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Operatorul economic colector/valorificator.
            para(doc, operator.getName(), bold);
            para(doc, joined(", ", operator.getAddress(),
                    op.getWorkPoint().getAddress() == null || op.getWorkPoint().getAddress().isBlank()
                            ? null : "punct de lucru: " + op.getWorkPoint().getAddress()), body);
            para(doc, joined("   ", prefixed("Nr. Reg. Com.: ", operator.getTradeRegisterNumber()),
                    prefixed("CUI/CIF: ", operator.getCui())), body);
            para(doc, "Autorizaţia de mediu nr. " + orBlank(operator.getEnvironmentalAuthNumber())
                    + " din data " + BLANK, body);

            Paragraph heading = new Paragraph(cp1250(metal
                    ? "BORDEROU DE ACHIZIŢIE DE DEŞEURI METALICE" : "BORDEROU DE ACHIZIŢIE DE DEŞEURI"), title);
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingBefore(14f);
            doc.add(heading);
            Paragraph number = new Paragraph(cp1250("Nr. " + op.getBorderouNumber() + " din data "
                    + op.getDate().format(DATE)), body);
            number.setAlignment(Element.ALIGN_CENTER);
            number.setSpacingAfter(12f);
            doc.add(number);

            // Deținătorul.
            para(doc, "Subsemnatul/Subsemnata " + person.getName()
                    + (metal ? ", identificat/ă cu actul de identitate seria şi nr. " + orBlank(person.getIdentification())
                            + ", eliberat de " + BLANK + ", CNP " + orBlank(person.getCnp())
                            + ", domiciliat/ă în " + orBlank(person.getAddress()) : "")
                    + ", mijlocul de transport " + orBlank(op.getVehicleRegistration())
                    + ", am predat următoarele deşeuri:", body);

            doc.add(table(lines));

            BigDecimal total = total(lines);
            DepotRetentions.Amounts retained = retained(op, lines);
            BigDecimal paid = total.subtract(retained.afm()).subtract(retained.incomeTax());

            // Felul plății nu e ales: fraza modelului rămâne întreagă, cu ambele variante, de tăiat de mână.
            String payment = op.getPaymentMethod() == null
                    ? "Se achită suma de " + lei(paid) + " lei cu chitanţa nr. " + BLANK + " sau în termen de maximum "
                            + "3 zile lucrătoare de la data prezentei, prin virament bancar în contul deţinătorului."
                    : op.getPaymentMethod() == PaymentMethod.VIREMENT
                    ? "Se achită suma de " + lei(paid) + " lei în termen de maximum 3 zile lucrătoare de la data "
                            + "prezentei, prin virament bancar în contul deţinătorului."
                    : "Se achită suma de " + lei(paid) + " lei cu chitanţa nr. " + orBlank(op.getReceiptNumber()) + ".";
            para(doc, payment, body);
            String retention = retained.incomeTax().signum() > 0
                    ? "Impozitul pe venit de " + percent(DepotRetentions.INCOME_TAX_RATE) + " (" + lei(retained.incomeTax())
                            + " lei) şi contribuţia de " + percent(DepotRetentions.AFM_RATE) + " (" + lei(retained.afm())
                            + " lei) la Administraţia Fondului pentru Mediu au fost reţinute la sursă din valoarea brută."
                    : "Contribuţia de " + percent(DepotRetentions.AFM_RATE) + " (" + lei(retained.afm())
                            + " lei) la Administraţia Fondului pentru Mediu a fost reţinută la sursă din valoarea brută.";
            para(doc, retention, body);

            if (metal) {
                Paragraph declaration = new Paragraph(cp1250("Declar pe propria răspundere că deşeurile pe care "
                        + "le predau provin din gospodăria proprie."), bold);
                declaration.setSpacingBefore(10f);
                doc.add(declaration);
            }
            doc.add(signatures());
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the borderou", ex);
        }
    }

    private PdfPTable table(List<WasteMovement> lines) {
        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100);
        t.setSpacingBefore(8f);
        t.setSpacingAfter(10f);
        setWidths(t, new float[]{38, 16, 14, 14, 18});
        for (String h : List.of("Denumirea deşeului şi descrierea acestuia", "Codul conform HG nr. 856/2002",
                "Cantitatea (kg)", "Preţul unitar (lei/kg)", "Valoarea (lei)")) {
            t.addCell(cell(h, small, Element.ALIGN_CENTER));
        }
        for (String h : List.of("0", "1", "2", "3", "4 = 2 × 3")) {
            t.addCell(cell(h, small, Element.ALIGN_CENTER));
        }
        for (WasteMovement l : lines) {
            String name = l.getArticle() == null ? l.getWasteCode().getName()
                    : l.getArticle().getName() + " — " + l.getWasteCode().getName();
            t.addCell(cell(name, body, Element.ALIGN_LEFT));
            t.addCell(cell(l.getWasteCode().getCode(), body, Element.ALIGN_LEFT));
            t.addCell(cell(l.getQuantity() == null ? "" : kg(l.getQuantity()), body, Element.ALIGN_RIGHT));
            t.addCell(cell(l.getUnitPrice() == null ? "" : lei(l.getUnitPrice()), body, Element.ALIGN_RIGHT));
            t.addCell(cell(l.getTotalValue() == null ? "" : lei(l.getTotalValue()), body, Element.ALIGN_RIGHT));
        }
        PdfPCell label = cell("TOTAL", bold, Element.ALIGN_LEFT);
        label.setColspan(4);
        t.addCell(label);
        t.addCell(cell(lei(total(lines)), bold, Element.ALIGN_RIGHT));
        return t;
    }

    private PdfPTable signatures() {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingBefore(24f);
        for (String who : List.of("Gestionar primitor", "Deţinător (semnătura)")) {
            PdfPCell c = new PdfPCell(new Phrase(cp1250(who), bold));
            c.setBorder(Rectangle.NO_BORDER);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPaddingBottom(36f);
            t.addCell(c);
        }
        return t;
    }

    /** Suma liniilor cu valoare. */
    static BigDecimal total(List<WasteMovement> lines) {
        return lines.stream().map(WasteMovement::getTotalValue).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Reținerile păstrate la finalizare; un borderou se tipărește doar după ea, deci nu se recalculează. */
    private static DepotRetentions.Amounts retained(WeighingOperation op, List<WasteMovement> lines) {
        if (op.getAfmContribution() != null && op.getIncomeTax() != null) {
            return new DepotRetentions.Amounts(op.getAfmBase(), op.getAfmContribution(),
                    op.getIncomeTaxBase(), op.getIncomeTax());
        }
        return DepotRetentions.of(op, lines);
    }

    private void para(Document doc, String text, Font font) {
        doc.add(new Paragraph(cp1250(text), font));
    }

    private PdfPCell cell(String text, Font font, int alignment) {
        PdfPCell c = new PdfPCell(new Phrase(cp1250(text), font));
        c.setHorizontalAlignment(alignment);
        c.setPadding(4f);
        return c;
    }

    private static String percent(BigDecimal rate) {
        return rate.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }

    static String lei(BigDecimal value) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ro-RO")))
                .format(value);
    }

    private static String kg(BigDecimal value) {
        return new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ro-RO")))
                .format(value);
    }

    private static String orBlank(String value) {
        return value == null || value.isBlank() ? BLANK : value;
    }

    private static String prefixed(String prefix, String value) {
        return value == null || value.isBlank() ? null : prefix + value;
    }

    private static String joined(String separator, String... parts) {
        return String.join(separator, java.util.Arrays.stream(parts).filter(p -> p != null && !p.isBlank()).toList());
    }

    private static void setWidths(PdfPTable table, float[] widths) {
        try {
            table.setWidths(widths);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Bad column widths for the borderou", ex);
        }
    }

    /** Cp1250 are ş/ţ cu sedilă, nu cu virgulă; fără pliere literele ar dispărea din PDF. */
    private static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş').replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont helvetica(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    "Cp1250", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica for the borderou", ex);
        }
    }
}
