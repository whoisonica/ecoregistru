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
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.util.WasteCodeLabel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Renders <b>Anexa 2 la HG 1061/2008</b> — <em>formularul de expediţie/transport deşeuri
 * periculoase</em> — from a movement that is already recorded.
 *
 * <p>Unlike Anexa 3, this layout is drawn from the <b>act itself</b>, not from a filled model. The
 * annex is reproduced in facsimile on the Portalul Legislativ (consolidated 23.01.2026) and the
 * verbatim model is kept in {@code docs/surse-oficiale.md} §4.1. That is not a detail of
 * housekeeping: the one filled copy we were given is <b>not conformant</b> — it is missing the
 * three rubrics at the foot (means of transport, packaging, observations) and shortens the
 * recipient's signature legend — so building from it would have reproduced somebody's private
 * abbreviation of an official form.
 *
 * <p><b>Who fills this in: the expeditor.</b> Art. 8, verbatim: <em>"Expeditorul completează,
 * semnează şi ştampilează formularul de expediţie/transport deşeuri periculoase."</em> Our client
 * is the expeditor, which is why this generator exists at all — read from the filled copy alone,
 * the form looked like something the collector hands out.
 *
 * <h2>The four rubrics that print empty, and why</h2>
 *
 * <p>{@code Cantitatea primită}, {@code Cantitatea recepţionată}, {@code Cantitatea respinsă} and
 * {@code Data primirii} are left blank on purpose. The expeditor writes the form, but those four
 * belong to the carrier and the recipient, who sign <em>at the moment they take the waste over</em>
 * (art. 9 alin. (1), art. 10 alin. (1)). The filled copy we hold has all of them completed, because
 * it was written up after the transport had ended; copying that would mean printing somebody
 * else's declaration for them. The precedent is already in the codebase: on Anexa 3 the quantity
 * weighed at unloading prints as an empty line.
 *
 * <h2>The number is not ours to allocate</h2>
 *
 * <p>{@code anexa2Number} is typed by the client and printed as typed. The note under the model:
 * <em>"*1) Număr înscris de către agenţia judeţeană pentru protecţia mediului."</em> This is the
 * exact opposite of {@code anexa3Number}, which we allocate {@code max+1} per company — and the
 * difference is the whole reason the field exists rather than a counter.
 *
 * <h2>How many copies</h2>
 *
 * <p>Below the threshold, three (art. 15 alin. (2): expeditor, destinatar, transportator). Above
 * it, six (art. 12 with art. 4 alin. (10): the three parties, the agency that approved it, the ISU
 * of the expeditor's county, and the agency of the expeditor's county). They are identical and
 * unlabelled, as on Anexa 3 and for the same reason: the act prints one model, and who keeps which
 * copy is not written on the paper. The screen says who gets which.
 *
 * <p>Diacritics go through <b>Cp1250</b>, as everywhere else here — Cp1252 has no ă/ş/ţ and would
 * drop them off an official form.
 */
@Component
public class Anexa2FormGenerator {

    private static final String TITLE = "Formular de expediţie/transport deşeuri periculoase";

    /**
     * Two quirks of the official text are reproduced as they stand — "Numar" without its breve and
     * "cuprinzand" without diacritics. They are the act's own spelling; correcting them here would
     * be us editing a legal form.
     */
    private static final String PACKAGING_RUBRIC =
            "Numar şi tip de ambalaje utilizate pentru transportul deşeurilor periculoase:";
    private static final String FOOTNOTE_1 =
            "*1) Număr înscris de către agenţia judeţeană pentru protecţia mediului.";
    private static final String FOOTNOTE_2 =
            "*2) Conform Hotărârii Guvernului nr. 856/2002 privind evidenţa gestiunii deşeurilor "
                    + "şi pentru aprobarea listei cuprinzand deşeurile, inclusiv deşeurile "
                    + "periculoase, cu completările ulterioare.";
    private static final String THRESHOLD_NOTE =
            "*) Nu este necesară aprobare pentru cantităţi < 1t/an.";
    private static final String PUBLICATION =
            "Publicat în Monitorul Oficial cu numărul 672 din data de 30 septembrie 2008";
    private static final String WEIGHED_AT_UNLOADING =
            "Se cântăreşte la descărcare, de destinatar.";

    /** Art. 15 alin. (2): expeditor, destinatar, transportator. */
    private static final int COPIES_BELOW_THRESHOLD = 3;
    /** Art. 12 + art. 4 alin. (10): the three parties, the approving APM, ISU, and the local APM. */
    private static final int COPIES_ABOVE_THRESHOLD = 6;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd . MM . yyyy");
    /** The blank a rubric leaves when the party who owns it has not signed yet. */
    private static final String BLANK = "________________";

    private final Font title;
    private final Font label;
    private final Font body;
    private final Font small;

    public Anexa2FormGenerator() {
        BaseFont base = centralEuropeanHelvetica(false);
        BaseFont bold = centralEuropeanHelvetica(true);
        this.title = new Font(bold, 11);
        this.label = new Font(bold, 7.5f);
        this.body = new Font(base, 8);
        this.small = new Font(base, 6.5f);
    }

    /**
     * @param belowOneTon what the "&lt; 1t/an" tick says — the client's answer where they gave one,
     *                    the yearly total's proposal where they did not. It decides both the tick
     *                    and how many copies come out of the printer.
     */
    public byte[] render(WasteMovement movement, Company sender, boolean belowOneTon) {
        Document doc = new Document(PageSize.A4, 24, 24, 20, 18);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            int copies = belowOneTon ? COPIES_BELOW_THRESHOLD : COPIES_ABOVE_THRESHOLD;
            for (int copy = 0; copy < copies; copy++) {
                if (copy > 0) {
                    doc.newPage();
                }
                addForm(doc, movement, sender, belowOneTon);
            }
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build the Anexa 2 transport form", ex);
        }
    }

    private void addForm(Document doc, WasteMovement m, Company sender, boolean belowOneTon)
            throws DocumentException {
        doc.add(header(m));

        PdfPTable grid = new PdfPTable(3);
        grid.setWidthPercentage(100);
        grid.setWidths(new float[]{34, 33, 33});

        // Row 1 — the waste, across the width, as the model draws it.
        Paragraph name = block("Denumirea deşeurilor periculoase*2)");
        addLines(name, m.getWasteCode().getName());
        grid.addCell(wide(name));

        // Row 2 — code, threshold, approval number.
        Paragraph code = block("Cod deşeuri periculoase");
        addLines(code, WasteCodeLabel.official(m.getWasteCode().getCode(),
                m.getWasteCode().isHazardous()));
        grid.addCell(box(code));

        Paragraph threshold = new Paragraph();
        threshold.add(text("Deşeuri periculoase < 1t/an   " + tick(belowOneTon), body));
        threshold.add(text("Deşeuri periculoase > 1t/an   " + tick(!belowOneTon), body));
        grid.addCell(box(threshold));

        Paragraph approval = block("Nr. formularului de aprobare al transportului*)");
        addLines(approval, m.getAnexa2ApprovalNumber());
        grid.addCell(box(approval));

        // Row 3 — the three registration numbers. The form does not define "nr. de înregistrare",
        // but the identification block below it says "cod unic de identificare" three times, for
        // the same three parties, so it is the CUI.
        Partner carrier = m.getTransportPartner();
        Partner recipient = m.getPartner();
        grid.addCell(box(rubric("Nr. de înregistrare al expeditorului", sender.getCui())));
        grid.addCell(box(rubric("Nr. de înregistrare al transportatorului",
                carrier != null ? carrier.getCui() : sender.getCui())));
        grid.addCell(box(rubric("Nr. de înregistrare al destinatarului",
                recipient != null ? recipient.getCui() : null)));

        // Row 4 — "În calitate de:" for each of the three.
        grid.addCell(box(senderCapacity(m)));
        grid.addCell(box(carrierCapacity(m)));
        grid.addCell(box(recipientCapacity(m)));

        // Row 5 — the quantities. Only the first is ours to write.
        grid.addCell(box(handedOverQuantity(m)));
        grid.addCell(box(rubric("Cantitatea primită în tone", BLANK)));
        Paragraph received = rubric("Cantitatea recepţionată în tone", BLANK);
        received.add(text("Cantitatea respinsă în tone", label));
        received.add(text(BLANK, body));
        grid.addCell(box(received));

        // Row 6 — the dates. The handover date is one event, so it is the same on both sides of
        // it; the receipt date is the recipient's to write, and prints blank.
        String handover = date(m.getDate());
        grid.addCell(box(rubric("Data predării (zi, lună, an)", handover)));
        grid.addCell(box(rubric("Data predării (zi, lună, an)", handover)));
        grid.addCell(box(rubric("Data primirii (zi, lună, an)", BLANK)));

        // Row 7 — who the three parties are.
        grid.addCell(box(identification(sender.getName(), sender.getAddress(), sender.getCui())));
        grid.addCell(box(carrier != null
                ? identification(carrier.getName(), carrier.getAddress(), carrier.getCui())
                : identification(sender.getName(), sender.getAddress(), sender.getCui())));
        grid.addCell(box(recipient != null
                ? identification(recipient.getName(), recipientPlace(m, recipient),
                        recipient.getCui())
                : identification(null, null, null)));

        // Row 8 — the three signatures, with the legends the model prints under them.
        grid.addCell(signature("(asigurare pentru o declaraţie corectă)"));
        grid.addCell(signature("(asigurare pentru transport regulamentar)"));
        grid.addCell(signature("(asigurare pentru preluare în vederea unei colectări sau stocări "
                + "temporare, tratare/valorificare/eliminare conform prevederilor legale)"));

        // The three rubrics at the foot — the ones the filled copy we were given does not have.
        Paragraph means = block("Tipul mijloacelor de transport:");
        addLines(means, m.getTransportMeans() == null
                ? null : m.getTransportMeans().getOfficialLabel());
        grid.addCell(wide(means));

        Paragraph packaging = block(PACKAGING_RUBRIC);
        addLines(packaging, m.getAnexa2Packaging());
        grid.addCell(wide(packaging));

        Paragraph observations = block("Observaţii:");
        addLines(observations, m.getDocumentReference());
        grid.addCell(wide(observations));

        doc.add(grid);

        Paragraph notes = new Paragraph();
        notes.setLeading(7.5f);
        notes.add(text("___________", small));
        notes.add(text(FOOTNOTE_1, small));
        notes.add(text(FOOTNOTE_2, small));
        notes.add(text(THRESHOLD_NOTE, small));
        notes.add(text(PUBLICATION, small));
        notes.setSpacingBefore(2f);
        doc.add(notes);
    }

    /** The title, then the form's own number on its own line, as the model prints them. */
    private Paragraph header(WasteMovement m) {
        Paragraph head = new Paragraph();
        head.setAlignment(Element.ALIGN_CENTER);
        head.add(text(TITLE, title));
        String number = m.getAnexa2Number() == null || m.getAnexa2Number().isBlank()
                ? BLANK : m.getAnexa2Number();
        head.add(text("nr.*1)  " + number, label));
        head.setSpacingAfter(4f);
        return head;
    }

    /**
     * "În calitate de:" on the expeditor's side. Read from the register the movement belongs to:
     * Anexa 1 is waste this company generated in its own activity (HG 856/2002 art. 1 alin. (1)),
     * the art. 48 register is goods taken over from third parties — which is exactly the
     * difference between "Generator" and "Operator economic care realizează operaţia de:
     * Colectare" on this form.
     *
     * <p>"Stocare temporară" is never ticked automatically. It is a third thing the register does
     * not tell us apart from collection, and a wrong tick on the party's own capacity is worse
     * than an empty box the client can fill in by hand.
     */
    private Paragraph senderCapacity(WasteMovement m) {
        boolean collector = m.getRegister() == WasteRegister.ART_48;
        Paragraph p = block("În calitate de:");
        p.add(text("Generator   " + tick(!collector), body));
        p.add(text("Operator economic care realizează operaţia de:", label));
        p.add(text("Colectare   " + tick(collector), body));
        p.add(text("Stocare temporară   " + tick(false), body));
        return p;
    }

    private Paragraph carrierCapacity(WasteMovement m) {
        Paragraph p = block("În calitate de:");
        p.add(text("Nume delegat:", label));
        addLines(p, m.getDriverName(), m.getDriverIdentification());
        p.add(text("Nr. de înmatriculare mijloc transport:", label));
        addLines(p, m.getVehicleRegistration());
        return p;
    }

    /**
     * The recipient's five boxes. They are the five values of {@link TransportDestination}, which
     * the model of Anexa 3 already made a set — so nothing new had to be asked of the client for
     * this form.
     */
    private Paragraph recipientCapacity(WasteMovement m) {
        Paragraph p = block("În calitate de operator economic care realizează operaţia de:");
        for (TransportDestination d : TransportDestination.values()) {
            p.add(text(operationLabel(d) + "   " + tick(m.getTransportDestinations().contains(d)),
                    body));
        }
        return p;
    }

    /**
     * Anexa 3 asks what the transport is <em>for</em> ("Destinat: colectării"), Anexa 2 what the
     * recipient <em>does</em> ("operaţia de: Colectare"). Same five things, two grammatical cases,
     * and each form is printed with its own wording.
     */
    private String operationLabel(TransportDestination destination) {
        return switch (destination) {
            case COLECTARE -> "Colectare";
            case STOCARE_TEMPORARA -> "Stocare temporară";
            case TRATARE -> "Tratare";
            case VALORIFICARE -> "Valorificare";
            case ELIMINARE -> "Eliminare";
        };
    }

    /**
     * "Cantitatea predată în tone" — the one quantity on this form that is ours to declare, and the
     * only unit the annex offers: it prints "în tone" in the rubric itself, so unlike Anexa 3 there
     * is no unit to choose. The figure is converted exactly, by moving the decimal point.
     *
     * <p>Blank when the recipient does the weighing, with the note that says why. A hazardous
     * transport is the last place to print a number nobody has measured.
     */
    private Paragraph handedOverQuantity(WasteMovement m) {
        Paragraph p = block("Cantitatea predată în tone");
        BigDecimal tons = Anexa3FormGenerator.converted(m.getQuantity(), m.getUnit(), Unit.TONS);
        if (tons != null) {
            p.add(text(decimalComma(tons), body));
        } else {
            p.add(text(BLANK, body));
            p.add(text(WEIGHED_AT_UNLOADING, small));
        }
        return p;
    }

    /**
     * Where the load actually went — the recipient's work point when this movement names one, else
     * their registered office. Same rule as Anexa 3: on a paper that travels with the truck, the
     * wrong depot is worse than no depot.
     */
    private String recipientPlace(WasteMovement m, Partner recipient) {
        if (m.getPartnerWorkPoint() != null) {
            return m.getPartnerWorkPoint().label();
        }
        var points = recipient.getWorkPoints();
        if (points != null && points.size() == 1) {
            return points.get(0).label();
        }
        return recipient.getAddress();
    }

    private Paragraph identification(String name, String address, String cui) {
        Paragraph p = block("Denumirea societăţii, sediul, cod unic de identificare");
        addLines(p, name, address, cui);
        return p;
    }

    private PdfPCell signature(String legend) {
        Paragraph p = block("Semnatura şi ştampila");
        p.add(text(legend, small));
        p.add(text("\n.............................", body));
        PdfPCell cell = box(p);
        cell.setMinimumHeight(54f);
        return cell;
    }

    private Paragraph rubric(String heading, String value) {
        Paragraph p = block(heading);
        addLines(p, value);
        return p;
    }

    // --- helpers ---

    /** The model's notation for a box: |X| ticked, |_| empty. */
    private String tick(boolean ticked) {
        return ticked ? "|X|" : "|_|";
    }

    private PdfPCell box(Paragraph content) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(content);
        cell.setPadding(3f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setMinimumHeight(34f);
        cell.setBorderWidth(0.8f);
        return cell;
    }

    /** A rubric that runs the width of the form, as the model's own full-width rows do. */
    private PdfPCell wide(Paragraph content) {
        PdfPCell cell = box(content);
        cell.setColspan(3);
        cell.setMinimumHeight(20f);
        return cell;
    }

    private Paragraph block(String heading) {
        Paragraph p = new Paragraph();
        p.add(text(heading, label));
        return p;
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

    /**
     * The Romanian decimal separator, which on this form is not a nicety: everything here is in
     * tonnes, so almost every quantity a generator writes has a fractional part — the filled copy
     * we hold reads "0,002" — and a dot would be the wrong mark on an official form in Romanian.
     *
     * <p>(Anexa 3 still prints a dot. It is in production and its quantities are usually whole
     * kilograms, so it is a cosmetic difference rather than a defect to fix in this slice; it is
     * noted in {@code status.md}.)
     */
    private String decimalComma(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private String date(LocalDate value) {
        return value == null ? BLANK : value.format(DATE);
    }

    /** See {@code Anexa3FormGenerator}: Cp1250 carries the cedilla forms, which the act uses too. */
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
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica for Anexa 2", ex);
        }
    }
}
