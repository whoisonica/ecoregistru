package ro.ecoregistru.service.export;

import com.lowagie.text.Document;
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
import ro.ecoregistru.controller.response.EnergySheetResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.EnergyCarrier;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static ro.ecoregistru.enums.EnergyCarrier.*;

/**
 * Anexa 1 energie (consum sub 1000 tep) ca PDF — geamănul de citit și de tipărit al lui
 * {@link EnergyAnnex1XlsxGenerator}, din 05.10.2026: Chrome nu arată un .xlsx, iar omul vrea să-și vadă anexa în
 * browser ca pe celelalte documente. Aceleași date, aceleași texte și aceeași ordine — Cap. I (date de contact),
 * Cap. II (consumurile cu tep-urile lor) și Cap. III (IMM, audit, măsuri, POIM) —, pe o pagină A4 în picioare, nu o
 * copie la pixel a foii Excel. Fișierul care se depune rămâne .xlsx-ul; ăsta e să-l citești.
 *
 * <p>Ce în Excel e formulă vie aici e calculat o dată, cu coeficienții formularului (cei din {@link EnergyCarrier}),
 * pe trei zecimale. Aceleași reguli de „nimic precompletat”: un purtător nebifat tipărește 0, unul bifat cu luni
 * lipsă tipărește gol — și, spre deosebire de Excel, unde golul intră în sumă ca zero, tep-ul lui și totalul care îl
 * cuprinde rămân și ele goale: un total din care lipsesc luni nu e totalul anului. Cărbunele și „alți combustibili”
 * au tep-ul scris de mână, ca pe formular.
 *
 * <p>Diacriticele trec prin Cp1250 ({@link DepotPdf#cp1250}), ca la celelalte formulare tipărite: Helvetica nu are
 * ș/ț cu virgulă, are doar varianta cu sedilă.
 */
@Component
public class EnergyAnnex1PdfGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String DASH = "-";
    /** Galbenul pal al celulelor de completat din formular, mai blând la tipar decât galbenul plin din Excel. */
    private static final Color INPUT = new Color(0xFF, 0xF7, 0xC2);

    private final Font chapter;
    private final Font label;
    private final Font text;
    private final Font value;
    private final Font valueBold;
    private final Font small;
    private final Font note;

    public EnergyAnnex1PdfGenerator() {
        BaseFont plain = DepotPdf.font(false);
        BaseFont bold = DepotPdf.font(true);
        this.chapter = new Font(bold, 10f);
        this.label = new Font(bold, 8f);
        this.text = new Font(plain, 8f);
        this.value = new Font(plain, 8f, Font.NORMAL, new Color(0xB9, 0x1C, 0x1C));
        this.valueBold = new Font(bold, 8f, Font.NORMAL, new Color(0xB9, 0x1C, 0x1C));
        this.small = new Font(plain, 7f);
        this.note = new Font(plain, 6.5f);
    }

    public byte[] render(EnergyAnnex1 annex) {
        Document doc = new Document(PageSize.A4, 36, 36, 32, 32);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Teps teps = Teps.of(annex);
            generalData(doc, annex);
            statistics(doc, annex, teps);
            classification(doc, annex.declaration(), teps.total());
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build Anexa 1 energie (pdf)", ex);
        }
    }

    // ---- CAP. I ---------------------------------------------------------------------------------

    private void generalData(Document doc, EnergyAnnex1 annex) {
        Company c = annex.company();
        doc.add(title("CAP. I – DATE DE CONTACT ALE CONSUMATORULUI DE ENERGIE. CHESTIONAR DE ANALIZĂ ENERGETICĂ "
                + "pentru anul " + annex.year() + "\npentru operatori economici cu consum anual mai mic de 1000 tep"));

        PdfPTable t = table(new float[]{22, 30, 18, 30});
        labelCell(t, "Denumirea operatorului economic", 1);
        valueCell(t, c.getName(), 3, true);
        labelCell(t, "Adresa poștală", 1);
        valueCell(t, c.getAddress(), 3, false);
        labelCell(t, "CUI", 1);
        valueCell(t, c.getCui(), 3, false);
        labelCell(t, "Telefon", 1);
        valueCell(t, c.getContactPhone(), 1, false);
        labelCell(t, "Pag. Internet", 1);
        valueCell(t, c.getWebsite(), 1, false);
        labelCell(t, "Fax", 1);
        valueCell(t, c.getFax(), 1, false);
        labelCell(t, "E-mail", 1);
        valueCell(t, c.getContactEmail(), 1, false);

        PdfPCell profile = cell("Profil de activitate", label, Element.ALIGN_LEFT);
        profile.setRowspan(2);
        t.addCell(profile);
        t.addCell(cell("Cod CAEN", text, Element.ALIGN_LEFT));
        valueCell(t, c.getCaenCode(), 2, false);
        t.addCell(cell("Sector de activitate", text, Element.ALIGN_LEFT));
        valueCell(t, c.getActivitySector(), 2, false);

        // Copia specialistei: un singur rând de atestat (ANRE); rândul Ministerului Energiei nu mai e.
        labelCell(t, "Manager energetic sau Persoana de contact", 2);
        labelCell(t, "deține Atestat * eliberat de ANRE la data de:", 1);
        valueCell(t, c.getEnergyContactAttestedOn() == null ? DASH : DATE.format(c.getEnergyContactAttestedOn()),
                1, false);
        labelCell(t, "NUME, PRENUME", 1);
        valueCell(t, c.getEnergyContactName(), 1, false);
        labelCell(t, "E-mail", 1);
        valueCell(t, c.getEnergyContactEmail(), 1, false);
        labelCell(t, "Telefon fix", 1);
        valueCell(t, c.getEnergyContactPhone(), 1, false);
        labelCell(t, "Tel. mobil", 1);
        valueCell(t, c.getEnergyContactMobile(), 1, false);
        doc.add(t);
    }

    // ---- CAP. II --------------------------------------------------------------------------------

    private void statistics(Document doc, EnergyAnnex1 annex, Teps teps) {
        doc.add(title("CAP. II – DATE STATISTICE DE CONSUM DE ENERGIE LA NIVELUL ANULUI DE RAPORTARE: "
                + annex.year()));

        float[] widths = {58, 14, 16, 12};
        PdfPTable t = table(widths);
        head(t, "Consum", 1);
        head(t, "U.M.", 1);
        head(t, "Valoare", 1);
        head(t, "Pondere", 1);

        describe(t, "CONSUM DE ENERGIE TOTAL ANUAL", "[Se calculează prin însumarea consumurilor totale de energie "
                + "electrică, energie termică, combustibili şi carburanții exprimate în tep/an]", 1);
        t.addCell(cell("[tep / an]", small, Element.ALIGN_CENTER));
        t.addCell(cell(number(teps.total()), label, Element.ALIGN_RIGHT));
        t.addCell(cell(teps.total() == null || teps.total().signum() == 0 ? "" : "100%", small,
                Element.ALIGN_CENTER));

        pair(t, "ENERGIE ELECTRICĂ** – Consumul total anual din SEN", "(Coef. de transformare: 1 MWh = 0,086 tep)",
                teps, ELECTRICITY, "[ MWh / an ]", annex);
        pair(t, "ENERGIE TERMICĂ*** – Consumul total anual", "(Coef. de transformare: 1 Gcal = 0,1 tep)",
                teps, HEAT, "[ Gcal / an ]", annex);

        describe(t, "COMBUSTIBILI ŞI CARBURANŢI – Consumuri totale anuale",
                "(Coeficient de transformare: precizat în paranteze)", 1);
        t.addCell(cell("[ tep / an ]", small, Element.ALIGN_CENTER));
        t.addCell(cell(number(teps.fuels()), text, Element.ALIGN_RIGHT));
        t.addCell(cell(share(teps.fuels(), teps.total()), small, Element.ALIGN_CENTER));
        doc.add(t);

        PdfPTable f = table(new float[]{1, 1, 1, 1, 1, 1, 1, 0.9f});
        List<String> fuels = List.of("Gaze naturale", "Păcură", "CLU", "Benzină", "Motorină", "Cărbune", "Alți comb.");
        List<String> coefficients = List.of("(0,086)", "(0,95)", "(0,97)", "(1,05)", "(1,015)", "(fcţ. de tip)",
                "(fcţ. de tip)");
        List<String> units = List.of("[ MWh/an ]", "[ t / an ]", "[ t / an ]", "[ t / an ]", "[ t / an ]",
                "[ t / an ]", "[ u.m. / an ]");
        List<EnergyCarrier> carriers = List.of(NATURAL_GAS, FUEL_OIL, LIGHT_FUEL_OIL, PETROL, DIESEL, COAL, OTHER_FUEL);
        fuels.forEach(name -> f.addCell(cell(name, label, Element.ALIGN_CENTER)));
        f.addCell(cell("", small, Element.ALIGN_CENTER));
        coefficients.forEach(k -> f.addCell(cell(k, small, Element.ALIGN_CENTER)));
        f.addCell(cell("", small, Element.ALIGN_CENTER));
        units.forEach(u -> f.addCell(cell(u, small, Element.ALIGN_CENTER)));
        f.addCell(cell("", small, Element.ALIGN_CENTER));
        carriers.forEach(k -> f.addCell(input(number(quantity(annex, k)), Element.ALIGN_RIGHT)));
        f.addCell(cell("cantitate", small, Element.ALIGN_CENTER));
        carriers.forEach(k -> f.addCell(k.tepWrittenByHand()
                ? input(number(teps.of(k)), Element.ALIGN_RIGHT)
                : cell(number(teps.of(k)), text, Element.ALIGN_RIGHT)));
        f.addCell(cell("[ tep / an ]", small, Element.ALIGN_CENTER));
        doc.add(f);

        PdfPTable r = table(widths);
        renewable(r, "ENERGIE ELECTRICĂ PRODUSĂ DIN SURSE RECUPERABILE ŞI/SAU REGENERABILE DE ENERGIE – "
                + "Consumuri totale anuale", teps, RENEWABLE_ELECTRICITY, "[ MWh / an ]", annex);
        renewable(r, "ENERGIE TERMICĂ PRODUSĂ DIN SURSE RECUPERABILE ŞI/SAU REGENERABILE DE ENERGIE – "
                + "Consumuri totale anuale", teps, RENEWABLE_HEAT, "[ Gcal / an ]", annex);
        doc.add(r);

        doc.add(notes("Notă: ** Nu se consideră energia electrică produsă intern din surse regenerabile de energie;\n"
                + "*** Se consideră doar energia termică cumpărată de la terți."));
    }

    /** Electricitate și căldură: întâi rândul în tep, apoi cantitatea, ca în formular (G5/G6, G8/G9). */
    private void pair(PdfPTable t, String title, String coefficient, Teps teps, EnergyCarrier carrier,
                      String unit, EnergyAnnex1 annex) {
        describe(t, title, coefficient, 2);
        t.addCell(cell("[ tep / an ]", small, Element.ALIGN_CENTER));
        t.addCell(cell(number(teps.of(carrier)), text, Element.ALIGN_RIGHT));
        PdfPCell share = cell(share(teps.of(carrier), teps.total()), small, Element.ALIGN_CENTER);
        share.setRowspan(2);
        t.addCell(share);
        t.addCell(cell(unit, small, Element.ALIGN_CENTER));
        t.addCell(input(number(quantity(annex, carrier)), Element.ALIGN_RIGHT));
    }

    /** Sursele regenerabile: întâi cantitatea, apoi tep-ul, ca în formular (G20/G21, G23/G24). */
    private void renewable(PdfPTable t, String title, Teps teps, EnergyCarrier carrier, String unit,
                           EnergyAnnex1 annex) {
        PdfPCell d = cell(title, text, Element.ALIGN_LEFT);
        d.setRowspan(2);
        t.addCell(d);
        t.addCell(cell(unit, small, Element.ALIGN_CENTER));
        t.addCell(input(number(quantity(annex, carrier)), Element.ALIGN_RIGHT));
        PdfPCell share = cell(share(teps.of(carrier), teps.total()), small, Element.ALIGN_CENTER);
        share.setRowspan(2);
        t.addCell(share);
        t.addCell(cell("[ tep / an ]", small, Element.ALIGN_CENTER));
        t.addCell(cell(number(teps.of(carrier)), text, Element.ALIGN_RIGHT));
    }

    private void describe(PdfPTable t, String title, String detail, int rows) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(DepotPdf.cp1250(title), label));
        p.add(new Phrase(DepotPdf.cp1250("\n" + detail), text));
        PdfPCell c = new PdfPCell(p);
        c.setRowspan(rows);
        c.setPadding(2.5f);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(c);
    }

    /** Purtător nebifat: 0. Bifat cu luni lipsă: gol. Altfel cantitatea anuală. */
    private static BigDecimal quantity(EnergyAnnex1 annex, EnergyCarrier carrier) {
        return annex.quantities().containsKey(carrier) ? annex.quantities().get(carrier) : BigDecimal.ZERO;
    }

    /**
     * Tep-urile Cap. II, calculate ca formulele din .xlsx: cantitate × coeficient, cărbunele și „alți combustibili”
     * scrise de mână. Un gol rămâne gol și golește suma care îl cuprinde.
     */
    private record Teps(Map<EnergyCarrier, BigDecimal> byCarrier, BigDecimal fuels, BigDecimal total) {

        static Teps of(EnergyAnnex1 annex) {
            Map<EnergyCarrier, BigDecimal> map = new HashMap<>();
            for (EnergyCarrier k : EnergyCarrier.values()) {
                BigDecimal tep;
                if (!annex.quantities().containsKey(k)) {
                    tep = BigDecimal.ZERO;
                } else if (k == COAL) {
                    tep = annex.coalTep();
                } else if (k == OTHER_FUEL) {
                    tep = annex.otherTep();
                } else {
                    BigDecimal q = annex.quantities().get(k);
                    tep = q == null ? null : q.multiply(k.coefficient().orElseThrow());
                }
                map.put(k, tep);
            }
            BigDecimal fuels = sum(map, NATURAL_GAS, FUEL_OIL, LIGHT_FUEL_OIL, PETROL, DIESEL, COAL, OTHER_FUEL);
            BigDecimal total = fuels == null ? null
                    : sum(map, ELECTRICITY, HEAT, RENEWABLE_ELECTRICITY, RENEWABLE_HEAT);
            return new Teps(map, fuels, total == null ? null : total.add(fuels));
        }

        BigDecimal of(EnergyCarrier carrier) {
            return byCarrier.get(carrier);
        }

        private static BigDecimal sum(Map<EnergyCarrier, BigDecimal> map, EnergyCarrier... carriers) {
            BigDecimal sum = BigDecimal.ZERO;
            for (EnergyCarrier k : carriers) {
                if (map.get(k) == null) {
                    return null;
                }
                sum = sum.add(map.get(k));
            }
            return sum;
        }
    }

    // ---- CAP. III -------------------------------------------------------------------------------

    private void classification(Document doc, EnergySheetResponse.Declaration d, BigDecimal totalTep) {
        doc.add(title("CAP III. Încadrarea operatorului economic în categoria întreprinderilor mici și mijlocii "
                + "(IMM)*"));

        Boolean sme = d.sme();
        boolean dashes = Boolean.TRUE.equals(sme);
        PdfPTable t = table(new float[]{70, 30});
        leadInBold(t, "DA:", " se va anexa Documentul privind încadrarea întreprinderii în categoria IMM-urilor – "
                + "Certificatul de atestare IMM");
        t.addCell(input(sme == null ? null : sme ? "DA" : DASH, Element.ALIGN_CENTER));
        leadInBold(t, "NU: ", "în acest caz, se vor completa datele solicitate mai jos privind Auditul energetic "
                + "efectuat");
        t.addCell(input(sme == null ? null : sme ? DASH : "NU", Element.ALIGN_CENTER));
        t.addCell(cell("Data ultimului Audit energetic efectuat", text, Element.ALIGN_LEFT));
        t.addCell(input(dashes ? DASH : d.auditDate() == null ? null : DATE.format(d.auditDate()),
                Element.ALIGN_CENTER));
        t.addCell(cell("Nume auditor  (persoana fizică /persoana juridică)  care a efectuat auditul energetic", text,
                Element.ALIGN_LEFT));
        t.addCell(input(dashes ? DASH : d.auditor(), Element.ALIGN_CENTER));
        t.addCell(cell("Contur bilant energetic - descriere ce a intrat in evaluarea de audit energetic", text,
                Element.ALIGN_LEFT));
        t.addCell(input(dashes ? DASH : d.auditScope(), Element.ALIGN_CENTER));
        t.addCell(cell("Procentul din totalul consumului de energie reprezentat de conturul respectiv (%)", text,
                Element.ALIGN_LEFT));
        t.addCell(input(dashes ? DASH : d.auditSharePct() == null ? null
                : decimals(2).format(d.auditSharePct()) + "%", Element.ALIGN_CENTER));
        doc.add(t);

        Paragraph lead = new Paragraph(DepotPdf.cp1250(
                "Principalele măsuri rezultate din ultimul Auditul energetic, respectiv, implementate**:"), label);
        lead.setSpacingBefore(4f);
        lead.setSpacingAfter(2f);
        doc.add(lead);

        PdfPTable m = table(new float[]{5, 35, 10, 10, 10, 10, 10, 10});
        // Cele 20 de rânduri ale formularului trec de obicei pe pagina a doua: capul tabelului se repetă acolo.
        m.setHeaderRows(2);
        PdfPCell nr = headCell("Nr. crt.");
        nr.setRowspan(2);
        m.addCell(nr);
        PdfPCell measure = headCell("Măsura:");
        measure.setRowspan(2);
        m.addCell(measure);
        for (String group : List.of("Costuri (mii lei)", "Economii (tep/an)", "Economii de cost (mii lei / an)")) {
            PdfPCell g = headCell(group);
            g.setColspan(2);
            m.addCell(g);
        }
        for (int i = 0; i < 3; i++) {
            m.addCell(headCell("estimate"));
            m.addCell(headCell("realizate"));
        }

        Map<Integer, EnergySheetResponse.Measure> byPosition = new HashMap<>();
        d.measures().forEach(x -> byPosition.put(x.position(), x));
        BigDecimal[] sums = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO};
        for (int n = 1; n <= 20; n++) {
            EnergySheetResponse.Measure x = byPosition.get(n);
            m.addCell(cell(String.valueOf(n), small, Element.ALIGN_CENTER));
            m.addCell(input(x == null ? null : x.name(), Element.ALIGN_LEFT));
            List<BigDecimal> figures = x == null ? java.util.Arrays.asList(null, null, null, null, null, null)
                    : java.util.Arrays.asList(x.costEstimated(), x.costActual(), x.savingsTepEstimated(),
                    x.savingsTepActual(), x.savingsCostEstimated(), x.savingsCostActual());
            for (int i = 0; i < 6; i++) {
                BigDecimal v = figures.get(i);
                m.addCell(input(v == null ? null : plain(v), Element.ALIGN_RIGHT));
                if (v != null) {
                    sums[i] = sums[i].add(v);
                }
            }
        }
        // Ca SUM() din Excel: o coloană goală dă 0.
        PdfPCell total = cell("TOTAL", label, Element.ALIGN_CENTER);
        total.setColspan(2);
        m.addCell(total);
        for (BigDecimal s : sums) {
            m.addCell(cell(plain(s), label, Element.ALIGN_RIGHT));
        }
        // 1000 × cost / economie (tep) / 11,63 — gol unde formula Excel ar da #DIV/0!.
        PdfPCell specific = cell("Cost specific economii de energie [lei/MWh]", label, Element.ALIGN_CENTER);
        specific.setColspan(2);
        m.addCell(specific);
        m.addCell(cell(specificCost(sums[0], sums[2]), text, Element.ALIGN_RIGHT));
        m.addCell(cell(specificCost(sums[1], sums[3]), text, Element.ALIGN_RIGHT));
        PdfPCell rest = cell("", text, Element.ALIGN_RIGHT);
        rest.setColspan(4);
        m.addCell(rest);
        PdfPCell weight = cell("Ponderea economiei de energie rezultata din audit in total consum raportat", label,
                Element.ALIGN_CENTER);
        weight.setColspan(2);
        m.addCell(weight);
        PdfPCell empty = cell("", text, Element.ALIGN_RIGHT);
        empty.setColspan(2);
        m.addCell(empty);
        m.addCell(cell(share(sums[2], totalTep), text, Element.ALIGN_RIGHT));
        m.addCell(cell(share(sums[3], totalTep), text, Element.ALIGN_RIGHT));
        PdfPCell tail = cell("", text, Element.ALIGN_RIGHT);
        tail.setColspan(2);
        m.addCell(tail);
        doc.add(m);

        // Copia specialistei: cele două întrebări POIM 6.4, la care răspunde clientul (goale până atunci).
        PdfPTable q = table(new float[]{85, 15});
        q.addCell(cell("Exista interes pentru programul de finantare nerambursabila pentru sisteme de cogenerare de "
                + "inalta eficienta prin POIM 6.4?", label, Element.ALIGN_LEFT));
        q.addCell(input(yesNo(d.poimInterest()), Element.ALIGN_CENTER));
        q.addCell(cell("Daca da, s-a depus sau se intentioneaza depunerea unui proiect de accesare finantare "
                + "nerambursabila pana la 80%?", label, Element.ALIGN_LEFT));
        q.addCell(input(yesNo(d.poimProject()), Element.ALIGN_CENTER));
        doc.add(q);

        doc.add(notes("Notă: * Se evidențiază situația operatorului economic la data completării acestei "
                + "Declarații.\nÎntreprinderi mici şi mijlocii, IMM-uri - întreprinderi în sensul celor definite în "
                + "titlul I din anexa la Recomandarea 2003/361/CE a Comisiei din 6 mai 2003 privind definirea "
                + "microîntreprinderilor şi a întreprinderilor mici şi mijlocii; categoria microîntreprinderilor şi "
                + "întreprinderilor mici şi mijlocii este formată din întreprinderi care au sub 250 de angajați şi a "
                + "căror cifră de afaceri anuală nu depășește 50 milioane euro şi/sau al căror bilanţ anual nu "
                + "depăşeşte 43 milioane euro;\n** Se completează costurile și economiile estimate pentru toate "
                + "măsurile menționate în Auditul energetic. Pentru măsurile deja implementate, se completează și "
                + "coloanele reprezentând costurile și economiile realizate."));
    }

    private static String yesNo(Boolean answer) {
        return answer == null ? null : answer ? "Da" : "Nu";
    }

    private static String specificCost(BigDecimal cost, BigDecimal tep) {
        if (tep.signum() == 0) {
            return "";
        }
        BigDecimal lei = cost.multiply(BigDecimal.valueOf(1000))
                .divide(tep, 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("11.63"), 10, RoundingMode.HALF_UP);
        return decimals(1).format(lei);
    }

    private void leadInBold(PdfPTable t, String lead, String rest) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(DepotPdf.cp1250(lead), label));
        p.add(new Phrase(DepotPdf.cp1250(rest), text));
        PdfPCell c = new PdfPCell(p);
        c.setPadding(2.5f);
        t.addCell(c);
    }

    // ---- layout helpers -------------------------------------------------------------------------

    private Paragraph title(String s) {
        Paragraph p = new Paragraph(DepotPdf.cp1250(s), chapter);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(10f);
        p.setSpacingAfter(6f);
        return p;
    }

    private Paragraph notes(String s) {
        Paragraph p = new Paragraph(DepotPdf.cp1250(s), note);
        p.setLeading(8f);
        p.setSpacingBefore(3f);
        return p;
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        return t;
    }

    private void labelCell(PdfPTable t, String s, int colspan) {
        PdfPCell c = cell(s, label, Element.ALIGN_LEFT);
        c.setColspan(colspan);
        t.addCell(c);
    }

    private void valueCell(PdfPTable t, String s, int colspan, boolean bold) {
        PdfPCell c = new PdfPCell(new Phrase(DepotPdf.cp1250(s), bold ? valueBold : value));
        c.setColspan(colspan);
        c.setPadding(2.5f);
        c.setBackgroundColor(INPUT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(c);
    }

    private void head(PdfPTable t, String s, int colspan) {
        PdfPCell c = headCell(s);
        c.setColspan(colspan);
        t.addCell(c);
    }

    private PdfPCell headCell(String s) {
        PdfPCell c = cell(s, label, Element.ALIGN_CENTER);
        c.setBackgroundColor(new Color(0xF1, 0xF5, 0xF9));
        return c;
    }

    /** O celulă de completat: fond galben pal, valoarea în roșu, ca pe formular. */
    private PdfPCell input(String s, int align) {
        PdfPCell c = cell(s, value, align);
        c.setBackgroundColor(INPUT);
        return c;
    }

    private static PdfPCell cell(String s, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(DepotPdf.cp1250(s), font));
        c.setPadding(2.5f);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private static String share(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.signum() == 0) {
            return "";
        }
        return decimals(1).format(part.multiply(BigDecimal.valueOf(100)).divide(total, 10, RoundingMode.HALF_UP))
                + "%";
    }

    /** Trei zecimale, ca formatul {@code 0.000} din Excel; null rămâne gol. */
    private static String number(BigDecimal v) {
        return v == null ? "" : decimals(3).format(v);
    }

    /** Cifrele măsurilor, ca formatul „General” din Excel: fără zerouri de umplutură. */
    private static String plain(BigDecimal v) {
        return new DecimalFormat("#0.###", new DecimalFormatSymbols(Locale.ROOT)).format(v);
    }

    // Unul nou la fiecare cifră: DecimalFormat nu e sincronizat, iar generatorul e singleton.
    private static DecimalFormat decimals(int n) {
        DecimalFormat f = new DecimalFormat(n == 0 ? "#0" : "#0." + "0".repeat(n),
                new DecimalFormatSymbols(Locale.ROOT));
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f;
    }
}
