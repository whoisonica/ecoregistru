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
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.TransportDestination;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Renders Anexa 3 la HG 1061/2008 — <em>formularul de încărcare-descărcare deşeuri
 * nepericuloase</em> — from a movement that is already recorded.
 *
 * <p>The layout follows the filled model received from the specialist (series HMB 180) rubric by
 * rubric: a six-column table under the title, with the carrier and its delegate on the left, the
 * two dates next to them, the waste and the "Destinat:" ticks in the middle, the quantity, then
 * the loading and unloading parties on the right, and the observations column last.
 *
 * <p>Two things the model taught us and this generator keeps:
 *
 * <ul>
 *   <li><b>The quantity cell can legitimately be empty.</b> On the model it is filled in by hand —
 *       "1,02" — because the sender had no weighbridge. When the movement says the recipient
 *       weighs at unloading, the cell prints blank with a line saying so, and the driver or the
 *       depot writes the figure in. Printing a zero would put a made-up number on a legal
 *       transport document.</li>
 *   <li><b>More than one "Destinat:" box may be ticked.</b> The model has an X on both
 *       <em>Colectării</em> and <em>Valorificării</em>.</li>
 * </ul>
 *
 * <p>Diacritics: the built-in Helvetica is rendered through <b>Cp1250</b>, the Central European
 * code page, because Cp1252 has no ă/ş/ţ and would drop them from an official form. Cp1250 carries
 * the cedilla forms (ş, ţ), which is also what the legal text itself uses, so
 * {@link #cp1250(String)} folds the comma-below variants onto them.
 */
@Component
public class Anexa3FormGenerator {

    private static final String TITLE =
            "FORMULAR DE ÎNCĂRCARE-DESCĂRCARE DEŞEURI NEPERICULOASE";
    private static final String LEGAL_BASIS = "Anexa 3 la HG 1061/2008";
    private static final String FOOTNOTE =
            "*) Se va completa numai în cazul în care încărcarea/descărcarea are loc la un punct "
                    + "de lucru care nu reprezintă sediul social.";
    private static final String WEIGHED_AT_UNLOADING =
            "Se cântăreşte la descărcare, de destinatar.";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Font title;
    private final Font label;
    private final Font body;
    private final Font small;

    public Anexa3FormGenerator() {
        BaseFont base = centralEuropeanHelvetica(false);
        BaseFont bold = centralEuropeanHelvetica(true);
        this.title = new Font(bold, 11);
        this.label = new Font(bold, 7.5f);
        this.body = new Font(base, 8);
        this.small = new Font(base, 6.5f);
    }

    public byte[] render(WasteMovement movement, Company sender) {
        Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph head = new Paragraph(cp1250(TITLE), title);
            doc.add(head);
            Paragraph basis = new Paragraph(cp1250(LEGAL_BASIS), body);
            basis.setSpacingAfter(6f);
            doc.add(basis);

            PdfPTable series = new PdfPTable(1);
            series.setWidthPercentage(100);
            Paragraph seriesLine = new Paragraph();
            seriesLine.add(text("Serie şi număr: " + seriesAndNumber(movement), label));
            PdfPCell seriesCell = new PdfPCell();
            seriesCell.addElement(seriesLine);
            seriesCell.setPadding(4f);
            series.addCell(seriesCell);
            doc.add(series);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{16, 11, 21, 13, 30, 9});
            table.addCell(cell(carrierColumn(movement, sender)));
            table.addCell(cell(dateColumn(movement)));
            table.addCell(cell(wasteColumn(movement)));
            table.addCell(cell(quantityColumn(movement)));
            table.addCell(cell(partiesColumn(movement, sender)));
            table.addCell(cell(observationsColumn(movement)));
            doc.add(table);

            Paragraph foot = new Paragraph(cp1250(FOOTNOTE), small);
            foot.setSpacingBefore(4f);
            doc.add(foot);

            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the Anexa 3 transport form", ex);
        }
    }

    // --- the six columns, in the order the form prints them ---

    /**
     * "Date de identificare transportator" plus the delegate and the goods-transport licence. With
     * no carrier named we haul it ourselves, and the form prints our own details there.
     */
    private Paragraph carrierColumn(WasteMovement m, Company sender) {
        Paragraph p = new Paragraph();
        p.add(text("Date de identificare transportator", label));
        Partner carrier = m.getTransportPartner();
        if (carrier != null) {
            addLines(p, carrier.getName(), carrier.getAddress(),
                    prefixed("CUI: ", carrier.getCui()),
                    prefixed("Reg. Com. ", carrier.getTradeRegisterNumber()));
        } else {
            addLines(p, sender.getName(), sender.getAddress(),
                    prefixed("CUI: ", sender.getCui()),
                    prefixed("Reg. Com. ", sender.getTradeRegisterNumber()));
        }

        p.add(gap());
        p.add(text("Date de identificare delegat şi nr. de înmatriculare mijloc de transport:", label));
        addLines(p, m.getDriverName(), m.getDriverIdentification(), m.getVehicleRegistration());

        p.add(gap());
        p.add(text("Licenţa de transport mărfuri nepericuloase nr.", label));
        addLines(p, carrier != null ? carrier.getTransportLicenseNumber()
                : sender.getTransportLicenseNumber());
        p.add(text("Data la care expiră licenţa", label));
        addLines(p, date(carrier != null ? carrier.getTransportLicenseExpiry()
                : sender.getTransportLicenseExpiry()));

        p.add(gap());
        p.add(text("Semnătura", label));
        return p;
    }

    private Paragraph dateColumn(WasteMovement m) {
        Paragraph p = new Paragraph();
        p.add(text("Data", label));
        p.add(gap());
        p.add(text("Încărcare", label));
        addLines(p, date(m.getDate()));
        p.add(gap());
        p.add(text("Descărcare", label));
        addLines(p, date(m.getUnloadDate()));
        return p;
    }

    /** "Caracteristici deşeuri: Categorii deşeuri/cod", the free description, and "Destinat:". */
    private Paragraph wasteColumn(WasteMovement m) {
        Paragraph p = new Paragraph();
        p.add(text("Caracteristici deşeuri: Categorii deşeuri/cod", label));
        addLines(p, m.getWasteCode().getName(), "cod " + m.getWasteCode().getCode());

        if (m.getNotes() != null && !m.getNotes().isBlank()) {
            p.add(gap());
            p.add(text("Descriere", label));
            addLines(p, m.getNotes());
        }

        p.add(gap());
        p.add(text("Destinat:", label));
        for (TransportDestination d : TransportDestination.values()) {
            boolean ticked = m.getTransportDestinations().contains(d);
            p.add(text(d.getOfficialLabel() + "   [" + (ticked ? "X" : " ") + "]", body));
        }
        return p;
    }

    /**
     * "Cantitate". Empty on purpose when the recipient does the weighing — that is how the filled
     * model reached us, with the figure written in afterwards by hand.
     */
    private Paragraph quantityColumn(WasteMovement m) {
        Paragraph p = new Paragraph();
        p.add(text("Cantitate", label));
        p.add(gap());
        BigDecimal quantity = m.getQuantity();
        if (quantity != null) {
            p.add(text(plain(quantity) + " " + unitLabel(m), body));
        } else {
            // The line stays blank so it can be filled in on the spot; the note says why.
            p.add(text("_______ " + unitLabel(m), body));
            p.add(text(WEIGHED_AT_UNLOADING, small));
        }
        if (m.getVolumeM3() != null) {
            p.add(gap());
            p.add(text(plain(m.getVolumeM3()) + " mc", body));
        }
        return p;
    }

    /** "Date privind punctul de lucru *) unde se efectuează": ÎNCĂRCAREA, then DESCĂRCAREA. */
    private Paragraph partiesColumn(WasteMovement m, Company sender) {
        Paragraph p = new Paragraph();
        p.add(text("Date privind punctul de lucru *) unde se efectuează", label));

        p.add(gap());
        p.add(text("ÎNCĂRCAREA — date de identificare expeditor:", label));
        addLines(p, sender.getName(),
                joinNonBlank(sender.getCui(), sender.getTradeRegisterNumber()),
                workPointAddress(m, sender));
        p.add(text("Autorizaţie de mediu nr.", label));
        addLines(p, sender.getEnvironmentalAuthNumber());
        p.add(text("Data la care expiră autorizaţia", label));
        addLines(p, date(sender.getEnvironmentalAuthExpiry()));
        p.add(text("Semnătura şi ştampila", label));

        p.add(gap());
        p.add(text("DESCĂRCAREA — date de identificare destinatar:", label));
        Partner recipient = m.getPartner();
        if (recipient != null) {
            addLines(p, recipient.getName(),
                    joinNonBlank(recipient.getCui(), recipient.getTradeRegisterNumber()),
                    recipient.getAddress());
            p.add(text("Autorizaţie de mediu nr.", label));
            addLines(p, recipient.getAuthorizationNumber());
            p.add(text("Data la care expiră autorizaţia", label));
            addLines(p, date(recipient.getAuthorizationExpiry()));
        }
        p.add(text("Semnătura şi ştampila", label));
        return p;
    }

    private Paragraph observationsColumn(WasteMovement m) {
        Paragraph p = new Paragraph();
        p.add(text("Obs", label));
        p.add(gap());
        // The reference is printed as the client wrote it; the model's own cell just says
        // "aviz 1406/11.01", so a separate "aviz" label above it would only repeat the word.
        addLines(p, m.getDocumentReference());
        return p;
    }

    // --- helpers ---

    /**
     * The work point address, which is what the starred rubric is for: it is filled in only when
     * loading happens somewhere other than the registered office. Falls back to the company
     * address when the work point has none recorded.
     */
    private String workPointAddress(WasteMovement m, Company sender) {
        String address = m.getWorkPoint().getAddress();
        return address != null && !address.isBlank() ? address : sender.getAddress();
    }

    private String seriesAndNumber(WasteMovement m) {
        String series = m.getAnexa3Series() == null ? "" : m.getAnexa3Series() + " ";
        return series + (m.getAnexa3Number() == null ? "" : m.getAnexa3Number());
    }

    private String unitLabel(WasteMovement m) {
        return switch (m.getUnit()) {
            case KG -> "kg";
            case TONS -> "tone";
        };
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String date(LocalDate value) {
        return value == null ? null : value.format(DATE);
    }

    private String prefixed(String prefix, String value) {
        return value == null || value.isBlank() ? null : prefix + value;
    }

    /** Arrays.stream, not List.of: the parts are routinely null and List.of would throw on them. */
    private String joinNonBlank(String... parts) {
        return String.join("   ", Arrays.stream(parts)
                .filter(v -> v != null && !v.isBlank())
                .toList());
    }

    private void addLines(Paragraph p, String... lines) {
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                p.add(text(line, body));
            }
        }
    }

    private Phrase text(String value, Font font) {
        return new Phrase(cp1250(value) + "\n", font);
    }

    private Phrase gap() {
        return new Phrase("\n", small);
    }

    private PdfPCell cell(Paragraph content) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(content);
        cell.setPadding(4f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setMinimumHeight(320f);
        return cell;
    }

    /**
     * Folds the comma-below Romanian letters onto the cedilla ones Cp1250 actually encodes. The
     * legal text uses the cedilla forms too ("deşeuri", "activităţi"), so nothing is lost; without
     * this the letters would simply vanish from the printed form.
     */
    private static String cp1250(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('ș', 'ş').replace('Ș', 'Ş')
                .replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont centralEuropeanHelvetica(boolean bold) {
        try {
            return BaseFont.createFont(
                    bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    "Cp1250", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica for Anexa 3", ex);
        }
    }
}
