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
import java.util.List;

/**
 * Renders Anexa 3 la HG 1061/2008 — <em>formularul de încărcare-descărcare deşeuri
 * nepericuloase</em> — from a movement that is already recorded.
 *
 * <p>The layout follows the filled model received from the specialist (series HMB 180) rubric by
 * rubric: a six-column table under the title, with the carrier and its delegate on the left, the
 * two dates next to them, the waste and the "Destinat:" ticks in the middle, the quantity, then
 * the loading and unloading parties on the right, and the observations column last.
 *
 * <p>Printed in <b>four copies</b>, one per page, each labelled with its addressee. The act asks
 * for three (art. 20 alin. (2)); the fourth is the sender's own file copy and says so on the page.
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

    /**
     * Who each printed copy is for. HG 1061/2008 art. 20 alin. (2) requires three — one stays with
     * the sender, one with the carrier, one reaches the recipient through the carrier. The fourth
     * is the specialist's request, not the act's: the sender's own file copy, because the copy
     * labelled "expeditor" leaves signed with the load and the dossier is left with nothing. The
     * act requires that three exist and reach the right hands; it does not forbid a fourth.
     */
    private static final String[] COPIES = {
            "expeditor", "transportator", "destinatar", "copie de arhivă (expeditor)"
    };

    private static final String EXTRA_COPY_NOTE =
            "Exemplarul 4 este copia de arhivă a expeditorului. HG 1061/2008 art. 20 alin. (2) cere "
                    + "3 exemplare — expeditor, transportator, destinatar.";

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

            for (int copy = 0; copy < COPIES.length; copy++) {
                if (copy > 0) {
                    doc.newPage();
                }
                addCopy(doc, movement, sender, copy);
            }

            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the Anexa 3 transport form", ex);
        }
    }

    /** One printed copy of the form: identical content, different addressee on the label. */
    private void addCopy(Document doc, WasteMovement movement, Company sender, int copy) {
        Paragraph head = new Paragraph(cp1250(TITLE), title);
        doc.add(head);
        Paragraph basis = new Paragraph(cp1250(LEGAL_BASIS), body);
        doc.add(basis);

        Paragraph which = new Paragraph(
                cp1250("Exemplarul " + (copy + 1) + " din " + COPIES.length + " — " + COPIES[copy]),
                label);
        which.setSpacingAfter(6f);
        doc.add(which);

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
        try {
            table.setWidths(new float[]{16, 11, 21, 13, 30, 9});
        } catch (DocumentException ex) {
            throw new IllegalStateException("Bad column widths for the Anexa 3 table", ex);
        }
        table.setWidthPercentage(100);
        table.addCell(column(carrierColumn(movement, sender)));
        table.addCell(column(dateColumn(movement)));
        table.addCell(column(wasteColumn(movement)));
        table.addCell(column(quantityColumn(movement)));
        table.addCell(column(partiesColumn(movement, sender)));
        table.addCell(column(observationsColumn(movement)));
        doc.add(table);

        Paragraph foot = new Paragraph(cp1250(FOOTNOTE), small);
        foot.setSpacingBefore(4f);
        doc.add(foot);

        // The fourth copy is ours, not the law's: say so on the page rather than leaving whoever
        // files it to wonder which of the three it is.
        if (copy == COPIES.length - 1) {
            doc.add(new Paragraph(cp1250(EXTRA_COPY_NOTE), small));
        }
    }

    // --- the six columns, in the order the form prints them ---

    /**
     * "Date de identificare transportator" plus the delegate and the goods-transport licence. With
     * no carrier named we haul it ourselves, and the form prints our own details there.
     */
    private List<Paragraph> carrierColumn(WasteMovement m, Company sender) {
        Partner carrier = m.getTransportPartner();
        String name = carrier != null ? carrier.getName() : sender.getName();
        String address = carrier != null ? carrier.getAddress() : sender.getAddress();
        String cui = carrier != null ? carrier.getCui() : sender.getCui();
        String reg = carrier != null
                ? carrier.getTradeRegisterNumber() : sender.getTradeRegisterNumber();
        String licence = carrier != null
                ? carrier.getTransportLicenseNumber() : sender.getTransportLicenseNumber();
        LocalDate licenceExpiry = carrier != null
                ? carrier.getTransportLicenseExpiry() : sender.getTransportLicenseExpiry();

        Paragraph who = block("Date de identificare transportator");
        addLines(who, name, address, prefixed("CUI: ", cui), prefixed("Reg. Com. ", reg));

        Paragraph delegate =
                block("Date de identificare delegat şi nr. de înmatriculare mijloc de transport:");
        addLines(delegate, m.getDriverName(), m.getDriverIdentification(),
                m.getVehicleRegistration());

        Paragraph licenceBox = block("Licenţa de transport mărfuri nepericuloase nr.");
        addLines(licenceBox, licence);

        Paragraph expiry = block("Data la care expiră licenţa de transport mărfuri nepericuloase");
        addLines(expiry, date(licenceExpiry));
        expiry.add(gap());
        expiry.add(text("Semnătura", label));

        return List.of(who, delegate, licenceBox, expiry);
    }

    private List<Paragraph> dateColumn(WasteMovement m) {
        Paragraph loading = block("Data\nÎncărcare");
        addLines(loading, date(m.getDate()));

        Paragraph unloading = block("Data\nDescărcare");
        addLines(unloading, date(m.getUnloadDate()));

        return List.of(loading, unloading);
    }

    /** "Caracteristici deşeuri: Categorii deşeuri/cod", the free description, and "Destinat:". */
    private List<Paragraph> wasteColumn(WasteMovement m) {
        Paragraph waste = block("Caracteristici deşeuri: Categorii deşeuri/cod");
        addLines(waste, m.getWasteCode().getName(), "cod " + m.getWasteCode().getCode());

        Paragraph description = block("Descriere");
        addLines(description, m.getNotes());

        Paragraph destination = block("Destinat:");
        for (TransportDestination d : TransportDestination.values()) {
            boolean ticked = m.getTransportDestinations().contains(d);
            destination.add(text(d.getOfficialLabel() + "   [" + (ticked ? "X" : " ") + "]", body));
        }

        return List.of(waste, description, destination);
    }

    /**
     * "Cantitate". Empty on purpose when the recipient does the weighing — that is how the filled
     * model reached us, with the figure written in afterwards by hand.
     */
    private List<Paragraph> quantityColumn(WasteMovement m) {
        Paragraph weight = block("Cantitate\n" + unitLabel(m));
        BigDecimal quantity = m.getQuantity();
        if (quantity != null) {
            weight.add(text(plain(quantity), body));
        } else {
            // The line stays blank so it can be filled in on the spot; the note says why.
            weight.add(text("_______", body));
            weight.add(text(WEIGHED_AT_UNLOADING, small));
        }

        Paragraph volume = block("mc");
        addLines(volume, m.getVolumeM3() == null ? null : plain(m.getVolumeM3()));

        return List.of(weight, volume);
    }

    /** "Date privind punctul de lucru *) unde se efectuează": ÎNCĂRCAREA, then DESCĂRCAREA. */
    private List<Paragraph> partiesColumn(WasteMovement m, Company sender) {
        Paragraph header = block("Date privind punctul de lucru *) unde se efectuează");

        Paragraph loading = block("ÎNCĂRCAREA\nDate de identificare expeditor:");
        addLines(loading, sender.getName(),
                joinNonBlank(sender.getCui(), sender.getTradeRegisterNumber()),
                workPointAddress(m, sender));

        Paragraph senderAuth = block("Autorizaţie de mediu nr.");
        addLines(senderAuth, sender.getEnvironmentalAuthNumber());
        senderAuth.add(text("Data la care expiră autorizaţia", label));
        addLines(senderAuth, date(sender.getEnvironmentalAuthExpiry()));
        senderAuth.add(text("Semnătura şi ştampila", label));

        Paragraph unloading = block("DESCĂRCAREA\nDate de identificare destinatar:");
        Partner recipient = m.getPartner();
        if (recipient != null) {
            addLines(unloading, recipient.getName(),
                    joinNonBlank(recipient.getCui(), recipient.getTradeRegisterNumber()),
                    // The unloading actually happens at their work point when they have one;
                    // the model writes "P.L. ILFOV, Sos. de Centura nr. 2-8" there, not the office.
                    firstNonBlank(recipient.getWorkPointAddress(), recipient.getAddress()));
        }

        Paragraph recipientAuth = block("Autorizaţie de mediu nr.");
        if (recipient != null) {
            addLines(recipientAuth, recipient.getAuthorizationNumber());
            recipientAuth.add(text("Data la care expiră autorizaţia", label));
            addLines(recipientAuth, date(recipient.getAuthorizationExpiry()));
        }
        recipientAuth.add(text("Semnătura şi ştampila", label));

        return List.of(header, loading, senderAuth, unloading, recipientAuth);
    }

    private List<Paragraph> observationsColumn(WasteMovement m) {
        // The reference is printed as the client wrote it; the model's own cell just says
        // "aviz 1406/11.01", so a separate "aviz" label above it would only repeat the word.
        Paragraph p = block("Obs");
        addLines(p, m.getDocumentReference());
        return List.of(p);
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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

    /** A titled block: the rubric name in bold, then whatever is written under it. */
    private Paragraph block(String title) {
        Paragraph p = new Paragraph();
        p.add(text(title, label));
        return p;
    }

    /**
     * One column of the form, as the models draw it: a stack of separately bordered rubrics rather
     * than one tall cell with everything in it. The nested table is what gives each rubric its own
     * box — four of them down the carrier column, five down the parties column — so the printed
     * page can be read and signed rubric by rubric like the paper original.
     */
    private PdfPCell column(List<Paragraph> blocks) {
        PdfPTable nested = new PdfPTable(1);
        nested.setWidthPercentage(100);
        for (Paragraph b : blocks) {
            PdfPCell box = new PdfPCell();
            box.addElement(b);
            box.setPadding(4f);
            box.setVerticalAlignment(Element.ALIGN_TOP);
            box.setMinimumHeight(56f);
            nested.addCell(box);
        }
        PdfPCell cell = new PdfPCell(nested);
        cell.setPadding(0f);
        cell.setBorderWidth(0.8f);
        cell.setMinimumHeight(340f);
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
