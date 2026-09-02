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
import ro.ecoregistru.enums.Unit;
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
 * <p>The layout follows, rubric by rubric and line by line, the stamped model from Hamburger
 * Recycling Romania (series HRR-BH, two forms). Redrawn against it on 02.09.2026 at the user's
 * request — "cumva sa fie 1 la 1 cu ce vezi tu aici" — and the differences that closed then are
 * worth naming, because each was us adding something the paper does not have:
 *
 * <ul>
 *   <li>the header is <b>one line</b>: "ANEXA 3" on the left, "Serie şi număr: … Nr: … / … . … .
 *       ####" on the right. No document title, no legal-basis subtitle, and no bordered box;</li>
 *   <li><b>no copy is labelled.</b> The three printed pages are the three copies art. 20 alin. (2)
 *       asks for, and on paper they are a carbon booklet: identical sheets, nothing written on them
 *       to say who keeps which. We used to print "Exemplarul 2 din 3 — destinatar (colector)" in
 *       the header, which no model has;</li>
 *   <li>the first box of <b>every</b> column is that column's heading, and only that — the way the
 *       paper draws its top row;</li>
 *   <li>the footer carries the publication line as well as the starred note.</li>
 * </ul>
 *
 * <p>The grid itself is six columns: the carrier and its delegate on the left, the two dates next
 * to them, the waste and the "Destinat:" ticks in the middle, the quantity, then the loading and
 * unloading parties on the right, and the observations column last.
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

    /** What the paper prints in the top left corner, and the whole of its title. */
    private static final String TITLE = "ANEXA 3";
    private static final String FOOTNOTE =
            "*) Se va completa numai în cazul în care încărcarea/descărcarea are loc la un punct "
                    + "de lucru care nu reprezintă sediul social.";
    /** The second footer line of the model — how the paper names the act it comes from. */
    private static final String PUBLICATION =
            "Publicat în Monitorul Oficial cu numărul 672 din data de 30 septembrie 2008";
    /** The rubric heading the model repeats, once above ÎNCĂRCAREA and once above DESCĂRCAREA. */
    private static final String WORK_POINT_RUBRIC =
            "Date privind punctul de lucru unde se efectuează *)";
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

    /**
     * How many copies the PDF carries. HG 1061/2008 art. 20 alin. (2) asks for three — the
     * specialist named the parties on 24.08.2026 (answer A3.3): "în 3 exemplare pentru generator,
     * colector şi transportator" — and printing all three in one PDF spares the client three trips
     * to the printer.
     *
     * <p>They are <b>identical and unlabelled</b>, which is the change of 02.09.2026. We used to
     * write "Exemplarul 2 din 3 — destinatar (colector)" into the header; no model has such a line,
     * and on paper the three copies are a carbon booklet — the same sheet three times, sorted after
     * signing, not before. Who keeps which is in the screen's hint, where it costs nothing, rather
     * than printed onto an official form.
     */
    private static final int COPIES = 3;

    public byte[] render(WasteMovement movement, Company sender) {
        Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            for (int copy = 0; copy < COPIES; copy++) {
                if (copy > 0) {
                    doc.newPage();
                }
                addForm(doc, movement, sender);
            }
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the Anexa 3 transport form", ex);
        }
    }

    private void addForm(Document doc, WasteMovement movement, Company sender)
            throws DocumentException {
        doc.add(header(movement));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{16, 11, 21, 13, 30, 9});
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
        doc.add(new Paragraph(cp1250(PUBLICATION), small));
    }

    /**
     * The one header line of the model: "ANEXA 3" hard left, the series and number filling the rest.
     * A borderless two-column table rather than one paragraph, because the two halves have to sit on
     * the same baseline with the number free to run long.
     */
    private PdfPTable header(WasteMovement m) {
        PdfPTable head = new PdfPTable(2);
        head.setWidthPercentage(100);
        head.setWidths(new float[]{18, 82});
        head.setSpacingAfter(4f);
        head.addCell(borderless(new Phrase(cp1250(TITLE), title), Element.ALIGN_LEFT));
        head.addCell(borderless(new Phrase(cp1250(seriesLine(m)), label), Element.ALIGN_CENTER));
        return head;
    }

    private PdfPCell borderless(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        cell.setPadding(0f);
        return cell;
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

        // The model's first box in every column is the column heading with, here, the carrier
        // underneath it. Address, CUI and trade register follow: the rubric asks for identification
        // data, and the paper omits them only because it is that carrier's own pre-printed booklet.
        Paragraph who = block("Date de identificare transportator");
        addLines(who, name, address, prefixed("CUI: ", cui), prefixed("Reg. Com. ", reg));

        Paragraph delegate =
                block("Date de identificare delegat şi nr. înmatriculare mijloc de transport");
        delegate.add(text("Nume si prenume:", label));
        addLines(delegate, m.getDriverName(), m.getDriverIdentification());
        delegate.add(text("Nr.inmatr.mij.trans:", label));
        addLines(delegate, m.getVehicleRegistration());

        Paragraph licenceBox = block("Licenţa de transport mărfuri nepericuloase nr.");
        addLines(licenceBox, licence);

        Paragraph expiry = block("Data la care expiră licenţa de transport mărfuri nepericuloase");
        addLines(expiry, date(licenceExpiry));
        expiry.add(gap());
        expiry.add(text("Semnătura", label));

        return List.of(who, delegate, licenceBox, expiry);
    }

    private List<Paragraph> dateColumn(WasteMovement m) {
        Paragraph heading = block("Data");

        Paragraph loading = block("Încărcare");
        addLines(loading, date(m.getDate()));

        Paragraph unloading = block("Descărcare");
        addLines(unloading, date(m.getUnloadDate()));

        return List.of(heading, loading, unloading);
    }

    /** "Caracteristici deşeuri: Categorii deşeuri/cod", the free description, and "Destinat:". */
    private List<Paragraph> wasteColumn(WasteMovement m) {
        Paragraph heading = block("Caracteristici deşeuri:");

        Paragraph waste = block("Categorii deşeuri");
        addLines(waste, m.getWasteCode().getName(), "Cod: " + m.getWasteCode().getCode());

        // "Descriere" and "Destinat:" share one box on the paper; the ticks sit in the next one.
        Paragraph description = block("Descriere");
        addLines(description, m.getNotes());
        description.add(text("Destinat:", label));

        Paragraph destination = new Paragraph();
        for (TransportDestination d : TransportDestination.values()) {
            boolean ticked = m.getTransportDestinations().contains(d);
            // |X| and |_| is the model's notation, and lower case is how it writes the five words.
            destination.add(text(decapitalize(d.getOfficialLabel())
                    + "   |" + (ticked ? "X" : "_") + "|", body));
        }

        return List.of(heading, waste, description, destination);
    }

    /** "Colectării" is how the enum names it for the screen; the form writes "colectării". */
    private static String decapitalize(String value) {
        return value.isEmpty() ? value : Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * "Cantitate". Empty on purpose when the recipient does the weighing — that is how the filled
     * model reached us, with the figure written in afterwards by hand.
     */
    private List<Paragraph> quantityColumn(WasteMovement m) {
        Unit printed = printedUnit(m);
        Paragraph heading = block("Cantitate");
        Paragraph weight = block(unitLabel(printed));
        BigDecimal quantity = converted(m.getQuantity(), m.getUnit(), printed);
        if (quantity != null) {
            weight.add(text(plain(quantity), body));
        } else {
            // The line stays blank so it can be filled in on the spot; the note says why.
            weight.add(text("_______", body));
            weight.add(text(WEIGHED_AT_UNLOADING, small));
        }

        Paragraph volume = block("mc");
        addLines(volume, m.getVolumeM3() == null ? null : plain(m.getVolumeM3()));

        return List.of(heading, weight, volume);
    }

    /**
     * Where the load was actually unloaded — the recipient's work point, not their registered
     * office. The filled model writes "P.L. ILFOV, Şos. de Centura nr. 2-8, Bragadiru" here.
     *
     * <p>From the most specific downwards: the work point picked on this movement, then their only
     * one if they have exactly one, then the head office. A partner with several depots and no
     * choice made prints the office rather than picking a depot for them — on a form that travels
     * with the truck, the wrong depot is worse than none.
     */
    private String recipientPlace(WasteMovement m, Partner recipient) {
        if (m.getPartnerWorkPoint() != null) {
            return m.getPartnerWorkPoint().label();
        }
        List<ro.ecoregistru.entity.PartnerWorkPoint> points = recipient.getWorkPoints();
        if (points != null && points.size() == 1) {
            return points.get(0).label();
        }
        return recipient.getAddress();
    }

    /** "Date privind punctul de lucru *) unde se efectuează": ÎNCĂRCAREA, then DESCĂRCAREA. */
    private List<Paragraph> partiesColumn(WasteMovement m, Company sender) {
        Paragraph heading = block(WORK_POINT_RUBRIC);

        Paragraph loading = block("ÎNCĂRCAREA");
        loading.add(text("Date de identificare expeditor", label));
        addLines(loading, sender.getName(),
                joinNonBlank(sender.getCui(), sender.getTradeRegisterNumber()),
                workPointAddress(m, sender));

        Paragraph senderAuth = block("Autorizaţie de mediu nr.");
        addLines(senderAuth, sender.getEnvironmentalAuthNumber());
        senderAuth.add(text("Data la care expiră autorizaţia de mediu", label));
        addLines(senderAuth, date(sender.getEnvironmentalAuthExpiry()));
        senderAuth.add(text("Semnătura şi ştampila", label));

        // The model repeats the rubric here, between the two signature blocks.
        Paragraph unloadingRubric = block(WORK_POINT_RUBRIC);

        // On the paper the unloading place comes first — "Str. Borşului, Nr.19D Oradea" — and the
        // recipient's own identification underneath it. Two different addresses: where the truck
        // went, then which company owns it.
        Paragraph unloading = block("DESCĂRCAREA");
        Partner recipient = m.getPartner();
        if (recipient != null) {
            addLines(unloading, recipientPlace(m, recipient));
            unloading.add(text("Date de identificare destinatar", label));
            addLines(unloading, recipient.getName(),
                    joinNonBlank(recipient.getCui(), recipient.getTradeRegisterNumber()),
                    recipient.getAddress());
        }

        Paragraph recipientAuth = block("Autorizaţie de mediu nr.");
        if (recipient != null) {
            addLines(recipientAuth, recipient.getAuthorizationNumber());
            recipientAuth.add(text("Data la care expiră autorizaţia de mediu", label));
            addLines(recipientAuth, date(recipient.getAuthorizationExpiry()));
        }
        recipientAuth.add(text("Semnătura şi ştampila", label));

        return List.of(heading, loading, senderAuth, unloadingRubric, unloading, recipientAuth);
    }

    private List<Paragraph> observationsColumn(WasteMovement m) {
        // The reference is printed as the client wrote it; the model's own cell just says
        // "aviz 1406/11.01", so a separate "aviz" label above it would only repeat the word.
        Paragraph heading = block("Observaţii");
        Paragraph p = new Paragraph();
        addLines(p, m.getDocumentReference());
        return List.of(heading, p);
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

    /**
     * The header line of the model, verbatim in shape: "Serie şi număr: HRR-BH 20 Nr: 169924 / 12 .
     * 08 . 2026" — the series, the allocated number, then the date of the transport spelled out with
     * the same separators the printed booklet leaves blanks for.
     */
    private String seriesLine(WasteMovement m) {
        String series = m.getAnexa3Series() == null ? "" : m.getAnexa3Series() + " ";
        String number = m.getAnexa3Number() == null ? "" : String.valueOf(m.getAnexa3Number());
        LocalDate on = m.getDate();
        String when = on == null ? ""
                : " / %02d . %02d . %d".formatted(on.getDayOfMonth(), on.getMonthValue(),
                        on.getYear());
        return "Serie şi număr: " + series + "Nr: " + number + when;
    }

    /**
     * The unit this form prints its quantity in, from the most specific choice to the least: this
     * transport's own, then the company's standing one, then the unit the movement was recorded in
     * — which is what every account did before either setting existed.
     *
     * <p>The choice exists because the sources disagree. HG 1061/2008 anexa 3 carries "tone" and
     * "mc"; two of the three filled models agree with it, including the stamped one from a
     * professional collector where 76 kilograms are written 0,076. The third prints KG. Asked which
     * matters at an inspection (question A3.4), the specialist answered that the client should be
     * able to pick "la introducerea mişcării" — so we do not pick for them at either level.
     */
    public static Unit printedUnit(WasteMovement m) {
        if (m.getAnexa3Unit() != null) {
            return m.getAnexa3Unit();
        }
        Unit chosen = m.getCompany() == null ? null : m.getCompany().getAnexa3Unit();
        return chosen != null ? chosen : m.getUnit();
    }

    /**
     * The quantity expressed in {@code target}. Exact: moving the decimal point three places, never
     * rounding — a form that leaves the site must not carry a figure that disagrees with the unit
     * printed beside it, and 1000x is the worst kind of disagreement.
     *
     * @return {@code null} when there is no quantity yet, which is legitimate: the recipient weighs
     *         at unloading and the rubric is printed blank
     */
    public static BigDecimal converted(BigDecimal quantity, Unit recorded, Unit target) {
        if (quantity == null || recorded == target) {
            return quantity;
        }
        return recorded == Unit.KG ? quantity.movePointLeft(3) : quantity.movePointRight(3);
    }

    private String unitLabel(Unit unit) {
        return switch (unit) {
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
        // The table fills the page, as it does on paper: the six column lines run to the bottom and
        // the last rubric of each column ends in a tall empty box, which is where the signatures and
        // the handwritten quantity go. 700pt is A4 minus the margins, the header line and the two
        // footer lines, with room for the border widths.
        cell.setMinimumHeight(700f);
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
