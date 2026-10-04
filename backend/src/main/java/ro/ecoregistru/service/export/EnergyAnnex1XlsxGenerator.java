package ro.ecoregistru.service.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.EnergySheetResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.EnergyCarrier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ro.ecoregistru.enums.EnergyCarrier.*;

/**
 * Anexa 1 — "consum anual mai mic de 1000 tep" (art. 9 alin. (7) din Legea 121/2014) as {@code .xlsx}, built cell by
 * cell after the Ministry of Energy 2023 template (three sheets, its texts, fonts, merges, widths, heights, yellow
 * input cells and medium borders), with the changes the specialist made on her filed copy: one attestation row (ANRE),
 * the chapter II title without "ANTERIOR", diesel as {@code =E16*1.015} (the template's {@code =0*1.015} is a bug),
 * the total with three decimals and the two POIM 6.4 questions. Where the two differ in looks only (the red font of the
 * values, the bold name, a few alignments, borders and number formats), the filed copy wins. Formulas stay live so the
 * file can be edited in Excel.
 * The cell map of the template: {@code ecoregistru-docs/docs/specs/2026-10-04-energie-anexa1-celule.txt}.
 *
 * <p>Nothing is prefilled: an unused carrier prints {@code 0}, a used one with months missing prints a blank cell,
 * an unanswered question stays blank. A dash is written only where the specialist writes one.
 */
@Component
public class EnergyAnnex1XlsxGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String NARROW = "Arial Narrow";
    private static final String CALIBRI = "Calibri";
    private static final String QUANTITY = "0.000";
    private static final String DASH = "-";

    public byte[] render(EnergyAnnex1 annex) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Writer w = new Writer(wb);
            generalData(w, sheet(wb, "Date generale"), annex);
            statistics(w, sheet(wb, "Date statistice"), annex);
            classification(w, sheet(wb, "Incadrare OE"), annex.declaration());
            // Cached results for viewers that do not calculate; Excel recalculates on open anyway.
            XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
            wb.setForceFormulaRecalculation(true);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build Anexa 1 energie (xlsx)", ex);
        }
    }

    private static Sheet sheet(XSSFWorkbook wb, String name) {
        Sheet sh = wb.createSheet(name);
        sh.setDefaultRowHeightInPoints(14.4f);
        return sh;
    }

    // ---- CAP. I ---------------------------------------------------------------------------------

    private void generalData(Writer w, Sheet sh, EnergyAnnex1 annex) {
        Company c = annex.company();
        widths(sh, 'A', 14.44140625, 'B', 12, 'G', 18.44140625, 'H', 18.44140625, 'I', 9.21875);
        heights(sh, 1, 76.5, 2, 30.75, 3, 15, 4, 15, 5, 28.2, 6, 15, 7, 30, 8, 48, 9, 56.25, 10, 28.2, 11, 15);
        Look label = Look.narrow(11, true);
        Look input = Look.narrow(11, false).yellow().h(HorizontalAlignment.CENTER);
        Look inputLeft = input.h(HorizontalAlignment.LEFT);
        Look inputText = input.format("@");

        w.put(sh, "A1:H1", "CAP. I – DATE DE CONTACT ALE CONSUMATORULUI DE ENERGIE. CHESTIONAR DE ANALIZĂ "
                + "ENERGETICĂ pentru anul " + annex.year() + "\npentru operatori economici cu consum anual mai mic "
                + "de 1000 tep", Look.calibri(14, true).h(HorizontalAlignment.CENTER));
        w.put(sh, "A2:B2", "Denumirea operatorului economic", label);
        w.put(sh, "C2:H2", c.getName(), input.inBold());
        w.put(sh, "A3:B3", "Adresa poștală", label);
        w.put(sh, "C3:H3", c.getAddress(), inputLeft);
        w.put(sh, "A4:B4", "CUI", label);
        w.put(sh, "C4:H4", c.getCui(), inputLeft);
        w.put(sh, "A5", "Telefon", label.edges("lr-b"));
        w.put(sh, "B5:C5", c.getContactPhone(), inputText);
        w.put(sh, "D5", "Pag. Internet", label.edges("-r-b"));
        w.put(sh, "E5:H5", c.getWebsite(), input);
        w.put(sh, "A6", "Fax", label.edges("lr-b"));
        w.put(sh, "B6:C6", c.getFax(), inputText);
        w.put(sh, "D6", "E-mail", label.edges("-r-b"));
        w.put(sh, "E6:H6", c.getContactEmail(), input);
        w.put(sh, "A7:A8", "Profil de activitate", label);
        w.put(sh, "B7", "Cod CAEN             ", Look.narrow(11, false));
        w.put(sh, "C7:H7", c.getCaenCode(), inputLeft.edges("-rtb"));
        w.put(sh, "B8:C8", "Sector de activitate", Look.narrow(11, false).h(HorizontalAlignment.CENTER));
        w.put(sh, "D8:H8", c.getActivitySector(), input);

        // The specialist's copy: one attestation row (ANRE), the Ministry of Energy row is gone.
        w.put(sh, "A9:F9", "Manager energetic sau Persoana de contact ", label);
        String attestation = "deține Atestat * eliberat de ANRE la data de:";
        XSSFRichTextString g9 = new XSSFRichTextString(attestation);
        int at = attestation.indexOf("Atestat");
        g9.applyFont(at, at + "Atestat".length(), w.font(NARROW, 11, true, true));
        w.put(sh, "G9", g9, label.edges("-r-b"));
        w.put(sh, "H9", c.getEnergyContactAttestedOn() == null ? DASH : DATE.format(c.getEnergyContactAttestedOn()),
                inputText.edges("-r-b"));
        w.put(sh, "A10", "NUME, PRENUME", label.edges("lr-b"));
        w.put(sh, "B10:E10", c.getEnergyContactName(), inputLeft);
        w.put(sh, "F10", "E-mail", label.edges("-r-b"));
        w.put(sh, "G10:H10", c.getEnergyContactEmail(), inputLeft);
        w.put(sh, "A11", "Telefon fix", label.edges("lr-b"));
        w.put(sh, "B11:E11", c.getEnergyContactPhone(), inputLeft.format("@"));
        w.put(sh, "F11", "Tel. mobil", label.edges("-r-b"));
        w.put(sh, "G11:H11", c.getEnergyContactMobile(), inputLeft);
    }

    // ---- CAP. II --------------------------------------------------------------------------------

    private void statistics(Writer w, Sheet sh, EnergyAnnex1 annex) {
        widths(sh, 'E', 13.5546875, 'F', 10.77734375, 'G', 16.21875, 'I', 9.21875);
        heights(sh, 1, 46.5, 2, 16.5, 3, 49.5, 4, 15, 5, 15, 6, 15, 7, 15, 8, 15, 9, 15, 10, 15, 11, 33, 12, 17.25,
                13, 27.6, 14, 15, 15, 15, 16, 15, 17, 15, 18, 15, 19, 15, 20, 27, 21, 27, 22, 15, 23, 23.25, 24, 30,
                26, 15, 28, 15);
        Look text = Look.narrow(11, false);
        Look bold = Look.narrow(11, true);
        Look unit = text.h(HorizontalAlignment.CENTER);
        Look share = Look.calibri(11, false).h(HorizontalAlignment.CENTER).noWrap().format("0.0%");
        Look input = unit.yellow().format(QUANTITY);
        Look tep = unit.format(QUANTITY);
        Look separator = unit.edges("lrt-");

        w.put(sh, "A1:F1", "CAP. II – DATE STATISTICE DE CONSUM DE ENERGIE LA NIVELUL ANULUI DE RAPORTARE",
                Look.calibri(14, true).h(HorizontalAlignment.CENTER));
        w.put(sh, "G1:H1", annex.year(), Look.calibri(16, true).inRed().h(HorizontalAlignment.CENTER).noWrap());

        w.put(sh, "A2:E2", "CONSUM DE ENERGIE TOTAL ANUAL", bold.edges("lrt-"));
        w.put(sh, "F2", null, text.edges("lrt-"));
        w.put(sh, "G2", null, Look.narrow(14, true).edges("lrt-"));
        w.put(sh, "H2", null, Look.calibri(11, false).format("0%").edges("lrt-"));
        w.put(sh, "A3:E3", "[Se calculează prin însumarea consumurilor totale de energie electrică, energie termică, "
                + "combustibili şi carburanții exprimate în tep/an]", text.thin());
        w.put(sh, "F3", "[tep / an]", unit.thin());
        w.put(sh, "G3", new Formula("G5+G8+G11+G21+G24"),
                Look.narrow(14, true).h(HorizontalAlignment.CENTER).format("0.000").thin());
        w.put(sh, "H3", new Formula("H5+H8+H11+H20+H23"),
                Look.calibri(11, false).h(HorizontalAlignment.CENTER).format("0%").thin());
        w.put(sh, "A4:H4", null, unit.edges("lr--"));

        w.put(sh, "A5:E5", "ENERGIE ELECTRICĂ** – Consumul total anual din SEN", bold.edges("lrt-"));
        w.put(sh, "F5", "[ tep / an ]", unit);
        w.put(sh, "G5", new Formula("G6*0.086"), tep);
        w.put(sh, "H5:H6", new Formula("G5/G3"), share);
        w.put(sh, "A6:E6", " (Coef. de transformare: 1 MWh = 0,086 tep)", text.edges("lr-b"));
        w.put(sh, "F6", "[ MWh / an ]", unit.edges("-r-b"));
        w.put(sh, "G6", quantity(annex, ELECTRICITY), input.edges("-r-b"));
        w.put(sh, "A7:H7", null, separator);

        w.put(sh, "A8:E8", "ENERGIE TERMICĂ***  – Consumul total anual", bold.edges("lrt-"));
        w.put(sh, "F8", "[ tep / an ]", unit);
        w.put(sh, "G8", new Formula("G9*0.1"), unit);
        w.put(sh, "H8:H9", new Formula("G8/G3"), share);
        w.put(sh, "A9:E9", "(Coef. de transformare: 1 Gcal = 0,1 tep)", text.edges("lr-b"));
        w.put(sh, "F9", "[ Gcal / an ]", unit.edges("-r-b"));
        w.put(sh, "G9", quantity(annex, HEAT), input.edges("-r-b"));
        w.put(sh, "A10:H10", null, separator);

        w.put(sh, "A11:E11", "COMBUSTIBILI ŞI CARBURANŢI – Consumuri totale anuale", bold);
        w.put(sh, "F11", "[ tep / an ]", unit);
        w.put(sh, "G11", new Formula("A18+B18+C18+D18+E18+F18+G18"), tep);
        w.put(sh, "H11:H12", new Formula("G11/G3"), share);
        w.put(sh, "A12:G12", "(Coeficient de transformare: precizat în paranteze)",
                text.h(HorizontalAlignment.LEFT).edges("lr-b"));
        w.put(sh, "H13:H18", null, Look.calibri(11, false).h(HorizontalAlignment.CENTER).noWrap().bottom());

        List<String> fuels = List.of("Gaze naturale", "Păcură", "CLU", "Benzină", "Motorină", "Cărbune", "Alți comb.");
        List<String> coefficients = List.of("(0,086)", "(0,95)", "(0,97)", "(1,05)", "(1,015)", "(fcţ. de tip)",
                "(fcţ. de tip)");
        List<String> units = List.of("[ MWh/an ]", "[ t / an ]", "[ t / an ]", "[ t / an ]", "[ t / an ]",
                "[ t / an ]", "[ u.m. / an ]");
        List<EnergyCarrier> carriers = List.of(NATURAL_GAS, FUEL_OIL, LIGHT_FUEL_OIL, PETROL, DIESEL, COAL, OTHER_FUEL);
        List<String> teps = List.of("A16*0.086", "B16*0.95", "C16*0.97", "D16*1.05", "E16*1.015");
        for (int i = 0; i < 7; i++) {
            char col = (char) ('A' + i);
            String side = i == 0 ? "lr-" : "-r-";
            w.put(sh, col + "13", fuels.get(i), bold.h(HorizontalAlignment.CENTER).edges(side + "-"));
            w.put(sh, col + "14", coefficients.get(i), unit.edges(side + "b"));
            w.put(sh, col + "15", units.get(i), (i == 6 ? unit.yellow() : unit).edges(side + "b"));
            w.put(sh, col + "16", quantity(annex, carriers.get(i)), input.edges(side + "b"));
            w.put(sh, col + "17", "[ tep / an ]", unit.edges(side + "b"));
        }
        for (int i = 0; i < 5; i++) {
            w.put(sh, (char) ('A' + i) + "18", new Formula(teps.get(i)), tep.edges(i == 0 ? "lr-b" : "-r-b"));
        }
        w.put(sh, "F18", tep(annex, COAL, annex.coalTep()), input.edges("-r-b"));
        w.put(sh, "G18", tep(annex, OTHER_FUEL, annex.otherTep()), input.edges("-r-b"));
        w.put(sh, "A19:H19", null, separator);

        w.put(sh, "A20:E21", "ENERGIE ELECTRICĂ PRODUSĂ DIN SURSE RECUPERABILE ŞI/SAU REGENERABILE DE ENERGIE – "
                + "Consumuri totale anuale", text);
        w.put(sh, "F20", "[ MWh / an ]", unit);
        w.put(sh, "G20", quantity(annex, RENEWABLE_ELECTRICITY), input);
        w.put(sh, "H20:H21", new Formula("G21/G3"), share);
        w.put(sh, "F21", "[ tep / an ]", unit.edges("-r-b"));
        w.put(sh, "G21", new Formula("G20*0.086"), unit.edges("-r-b"));
        w.put(sh, "A22:H22", null, separator);

        w.put(sh, "A23:E24", "ENERGIE TERMICĂ PRODUSĂ DIN SURSE RECUPERABILE ŞI/SAU REGENERABILE DE ENERGIE – "
                + "Consumuri totale anuale", text);
        w.put(sh, "F23", "[ Gcal / an ]", unit);
        w.put(sh, "G23", quantity(annex, RENEWABLE_HEAT), input.edges("-rtb"));
        w.put(sh, "H23:H24", new Formula("G24/G3"), share);
        w.put(sh, "F24", "[ tep / an ]", unit.edges("-r-b"));
        w.put(sh, "G24", new Formula("G23*0.1"), unit.edges("-r-b"));

        Look white = Look.calibri(11, false).white().noWrap().bottom().edges("----");
        for (int r = 25; r <= 26; r++) {
            for (char col = 'A'; col <= 'H'; col++) {
                w.put(sh, col + String.valueOf(r), null, white);
            }
        }
        w.put(sh, "A27:H28", note(w, "Notă: \t** Nu se consideră energia electrică produsă intern din surse "
                        + "regenerabile de energie;\n\t*** Se consideră doar energia termică cumpărată de la terți."),
                Look.calibri(11, false).h(HorizontalAlignment.LEFT));
    }

    /** Unused carrier: 0. Used with months missing: blank. Otherwise the annual quantity. */
    private static BigDecimal quantity(EnergyAnnex1 annex, EnergyCarrier carrier) {
        return annex.quantities().containsKey(carrier) ? annex.quantities().get(carrier) : BigDecimal.ZERO;
    }

    private static BigDecimal tep(EnergyAnnex1 annex, EnergyCarrier carrier, BigDecimal tep) {
        return annex.quantities().containsKey(carrier) ? tep : BigDecimal.ZERO;
    }

    // ---- CAP. III -------------------------------------------------------------------------------

    private void classification(Writer w, Sheet sh, EnergySheetResponse.Declaration d) {
        widths(sh, 'B', 57.77734375, 'C', 12.77734375, 'D', 13.77734375, 'E', 15, 'F', 9.77734375, 'G', 6.21875,
                'H', 16, 'I', 17.21875, 'J', 9.21875);
        heights(sh, 1, 48, 2, 33, 3, 33, 4, 33, 5, 23.25, 6, 65.25, 7, 21.75, 8, 33, 9, 18, 31, 31.5, 32, 31.5,
                33, 37.5, 34, 37.5, 35, 37.5, 36, 15, 44, 15);
        for (int r = 10; r <= 30; r++) {
            heights(sh, r, 15);
        }
        Look text = Look.narrow(11, false);
        Look bold = Look.narrow(11, true);
        Look input = text.yellow().h(HorizontalAlignment.CENTER);
        Look answer = Look.narrow(9, false).yellow().h(HorizontalAlignment.CENTER);
        Look head = bold.h(HorizontalAlignment.CENTER);
        Look sub = Look.narrow(10, true).h(HorizontalAlignment.CENTER);
        Look total = Look.calibri(14, true).h(HorizontalAlignment.CENTER).noWrap();
        Look figure = Look.calibri(14, false).h(HorizontalAlignment.CENTER).noWrap();

        Boolean sme = d.sme();
        w.put(sh, "A1:I1", "CAP III. Încadrarea operatorului economic în categoria întreprinderilor mici și mijlocii "
                + "(IMM)*", total);
        w.put(sh, "A2:F2", leadInBold(w, "DA:", " se va anexa Documentul privind încadrarea întreprinderii în "
                + "categoria IMM-urilor – Certificatul de atestare IMM"), bold);
        w.put(sh, "G2:I2", sme == null ? null : sme ? "DA" : DASH, answer);
        w.put(sh, "A3:F3", leadInBold(w, "NU: ", "în acest caz, se vor completa datele solicitate mai jos privind "
                + "Auditul energetic efectuat"), bold);
        w.put(sh, "G3:I3", sme == null ? null : sme ? DASH : "NU", answer);

        boolean dashes = Boolean.TRUE.equals(sme);
        w.put(sh, "A4:B4", "Data ultimului Audit energetic efectuat", text);
        w.put(sh, "C4:I4", dashes ? DASH : d.auditDate() == null ? null : DATE.format(d.auditDate()), input);
        w.put(sh, "A5:B5", "Nume auditor  (persoana fizică /persoana juridică)  care a efectuat auditul energetic",
                text);
        w.put(sh, "C5:I5", dashes ? DASH : d.auditor(), input);
        w.put(sh, "A6:B6", "Contur bilant energetic - descriere ce a intrat in evaluarea de audit energetic",
                text.edges("lr--"));
        w.put(sh, "C6:I6", dashes ? DASH : d.auditScope(), input.edges("lr--"));
        w.put(sh, "A7:B7", "Procentul din totalul consumului de energie reprezentat de conturul respectiv (%)", text);
        w.put(sh, "C7:I7", dashes ? DASH : d.auditSharePct(), input.format("0.00\"%\""));
        w.put(sh, "A8:I8", "Principalele măsuri rezultate din ultimul Auditul energetic, respectiv, implementate**:",
                bold);

        w.put(sh, "A9:A10", "Nr. crt.", text.h(HorizontalAlignment.CENTER));
        w.put(sh, "B9:B10", "Măsura:", text.h(HorizontalAlignment.CENTER));
        w.put(sh, "C9:D9", "Costuri (mii lei)", head);
        w.put(sh, "E9:G9", "Economii (tep/an)", head);
        w.put(sh, "H9:I9", "Economii de cost (mii lei / an)", head);
        w.put(sh, "C10", "estimate", sub.edges("-r-b"));
        w.put(sh, "D10", "realizate", sub.edges("-r-b"));
        w.put(sh, "E10", "estimate", sub.edges("-r-b"));
        w.put(sh, "F10:G10", "realizate", sub);
        w.put(sh, "H10", "estimate", sub.edges("-r-b"));
        w.put(sh, "I10", "realizate", sub);

        Map<Integer, EnergySheetResponse.Measure> measures = new HashMap<>();
        d.measures().forEach(m -> measures.put(m.position(), m));
        for (int n = 1; n <= 20; n++) {
            int r = 10 + n;
            EnergySheetResponse.Measure m = measures.get(n);
            w.put(sh, "A" + r, n, text.h(HorizontalAlignment.CENTER).edges("lr-b"));
            w.put(sh, "B" + r, m == null ? null : m.name(), text.yellow().h(HorizontalAlignment.LEFT).edges("-r-b"));
            w.put(sh, "C" + r, m == null ? null : m.costEstimated(), input.edges("-r-b"));
            w.put(sh, "D" + r, m == null ? null : m.costActual(), input.edges("-r-b"));
            w.put(sh, "E" + r, m == null ? null : m.savingsTepEstimated(), input.edges("-r-b"));
            w.put(sh, "F" + r + ":G" + r, m == null ? null : m.savingsTepActual(), input);
            w.put(sh, "H" + r, m == null ? null : m.savingsCostEstimated(), input);
            w.put(sh, "I" + r, m == null ? null : m.savingsCostActual(), input.edges("-rtb"));
        }

        w.put(sh, "A31:B31", "TOTAL", total);
        w.put(sh, "C31", new Formula("SUM(C11:C30)"), total);
        w.put(sh, "D31", new Formula("SUM(D11:D30)"), total);
        w.put(sh, "E31", new Formula("SUM(E11:E30)"), total);
        w.put(sh, "F31:G31", new Formula("SUM(F11:F30)"), total.edges("l-tb"));
        w.put(sh, "H31", new Formula("SUM(H11:H30)"), total);
        w.put(sh, "I31", new Formula("SUM(I11:I30)"), total);
        w.put(sh, "A32:B32", "Cost specific economii de energie [lei/MWh]", total);
        w.put(sh, "C32", new Formula("1000*C31/E31/11.63"), figure.format("0.0"));
        w.put(sh, "D32", new Formula("1000*D31/F31/11.63"), figure.format("0.0"));
        w.put(sh, "E32:I32", null, figure);
        w.put(sh, "A33:B33", "Ponderea economiei de energie rezultata din audit in total consum raportat",
                Look.calibri(12, true).h(HorizontalAlignment.CENTER));
        w.put(sh, "C33:D33", null, figure.bottom());
        w.put(sh, "E33", new Formula("E31/'Date statistice'!G3"), figure.format("0.0%"));
        w.put(sh, "F33:G33", new Formula("F31/'Date statistice'!G3"), figure.format("0.0%"));
        w.put(sh, "H33:I33", null, figure.format("0%"));

        // The specialist's copy: the two POIM 6.4 questions, answered by the client (blank until then).
        Look question = Look.calibri(11, true).h(HorizontalAlignment.LEFT);
        Look yesNo = Look.narrow(10, true).yellow().h(HorizontalAlignment.CENTER);
        w.put(sh, "A34:H34", "Exista interes pentru programul de finantare nerambursabila pentru sisteme de "
                + "cogenerare de inalta eficienta prin POIM 6.4?", question.noWrap());
        w.put(sh, "I34", yesNo(d.poimInterest()), yesNo.edges("-r-b"));
        w.put(sh, "A35:H35", "Daca da, s-a depus sau se intentioneaza depunerea unui proiect de accesare finantare "
                + "nerambursabila pana la 80%?", question);
        w.put(sh, "I35", yesNo(d.poimProject()), yesNo.edges("-r-b"));

        w.put(sh, "A37:I44", note(w, "Notă:\t*  Se evidențiază situația operatorului economic la data completării "
                        + "acestei Declarații.\nÎntreprinderi mici şi mijlocii, IMM-uri - întreprinderi în sensul "
                        + "celor definite în titlul I din anexa la Recomandarea 2003/361/CE a Comisiei din 6 mai 2003 "
                        + "privind definirea microîntreprinderilor şi a întreprinderilor mici şi mijlocii; categoria "
                        + "microîntreprinderilor şi întreprinderilor mici şi mijlocii este formată din întreprinderi "
                        + "care au sub 250 de angajați şi a căror cifră de afaceri anuală nu depășește 50 milioane "
                        + "euro şi/sau al căror bilanţ anual nu depăşeşte 43 milioane euro;\n\t** Se completează "
                        + "costurile și economiile estimate pentru toate măsurile menționate în Auditul energetic. "
                        + "Pentru măsurile deja implementate, se completează și coloanele reprezentând costurile și "
                        + "economiile realizate.   "),
                Look.calibri(11, false).h(HorizontalAlignment.LEFT));
    }

    private static String yesNo(Boolean answer) {
        return answer == null ? null : answer ? "Da" : "Nu";
    }

    /** A cell whose font is bold, with the text after {@code lead} set back to regular, as on the template. */
    private static XSSFRichTextString leadInBold(Writer w, String lead, String rest) {
        XSSFRichTextString text = new XSSFRichTextString(lead + rest);
        text.applyFont(lead.length(), lead.length() + rest.length(), w.font(NARROW, 11, false, false));
        return text;
    }

    /** A note: "Notă:" in bold, the rest regular Calibri 11. */
    private static XSSFRichTextString note(Writer w, String text) {
        XSSFRichTextString rich = new XSSFRichTextString(text);
        rich.applyFont(0, "Notă:".length(), w.font(CALIBRI, 11, true, false));
        return rich;
    }

    // ---- layout helpers -------------------------------------------------------------------------

    private static void widths(Sheet sh, Object... columnWidthPairs) {
        for (int i = 0; i < columnWidthPairs.length; i += 2) {
            int col = (Character) columnWidthPairs[i] - 'A';
            double width = ((Number) columnWidthPairs[i + 1]).doubleValue();
            sh.setColumnWidth(col, (int) Math.round(width * 256));
        }
    }

    private static void heights(Sheet sh, double... rowHeightPairs) {
        for (int i = 0; i < rowHeightPairs.length; i += 2) {
            row(sh, (int) rowHeightPairs[i] - 1).setHeightInPoints((float) rowHeightPairs[i + 1]);
        }
    }

    private static Row row(Sheet sh, int index) {
        Row row = sh.getRow(index);
        return row != null ? row : sh.createRow(index);
    }

    /** A live formula, written without the leading "=". */
    private record Formula(String expression) {
    }

    private enum Fill { NONE, YELLOW, WHITE }

    /**
     * How a cell (or a merged region) looks. {@code edges} reads left, right, top, bottom — a letter draws that edge
     * of the whole region, "-" leaves it open, as in the cell map. {@code red}: the font colour of the values, as on
     * the template and on the filed copy.
     */
    private record Look(String font, double size, boolean bold, boolean red, Fill fill, HorizontalAlignment h,
                        boolean middle, boolean wrap, String format, String edges, BorderStyle line) {

        static Look narrow(double size, boolean bold) {
            return new Look(NARROW, size, bold, false, Fill.NONE, HorizontalAlignment.GENERAL, true, true, null,
                    "lrtb", BorderStyle.MEDIUM);
        }

        static Look calibri(double size, boolean bold) {
            return new Look(CALIBRI, size, bold, false, Fill.NONE, HorizontalAlignment.GENERAL, true, true, null,
                    "lrtb", BorderStyle.MEDIUM);
        }

        Look h(HorizontalAlignment align) {
            return new Look(font, size, bold, red, fill, align, middle, wrap, format, edges, line);
        }

        Look inBold() {
            return new Look(font, size, true, red, fill, h, middle, wrap, format, edges, line);
        }

        Look inRed() {
            return new Look(font, size, bold, true, fill, h, middle, wrap, format, edges, line);
        }

        /** A cell to fill in: yellow, its value in red. */
        Look yellow() {
            return new Look(font, size, bold, true, Fill.YELLOW, h, middle, wrap, format, edges, line);
        }

        Look white() {
            return new Look(font, size, bold, red, Fill.WHITE, h, middle, wrap, format, edges, line);
        }

        Look noWrap() {
            return new Look(font, size, bold, red, fill, h, middle, false, format, edges, line);
        }

        /** Vertical alignment left at Excel's default (bottom), as on the few template cells without one. */
        Look bottom() {
            return new Look(font, size, bold, red, fill, h, false, wrap, format, edges, line);
        }

        Look format(String f) {
            return new Look(font, size, bold, red, fill, h, middle, wrap, f, edges, line);
        }

        Look edges(String e) {
            return new Look(font, size, bold, red, fill, h, middle, wrap, format, e, line);
        }

        Look thin() {
            return new Look(font, size, bold, red, fill, h, middle, wrap, format, edges, BorderStyle.THIN);
        }
    }

    /** Writes values into regions and keeps one cell style per distinct look, so the file stays small. */
    private static final class Writer {

        // ARGB, as the template stores them: FFFFFF00 is the yellow of the cells to fill in, FFFF0000 their text.
        private static final XSSFColor YELLOW = argb(0xFFFFFF00);
        private static final XSSFColor WHITE = argb(0xFFFFFFFF);
        private static final XSSFColor RED = argb(0xFFFF0000);

        private static XSSFColor argb(int argb) {
            return new XSSFColor(new byte[]{(byte) (argb >>> 24), (byte) (argb >>> 16), (byte) (argb >>> 8),
                    (byte) argb}, null);
        }

        private record Key(Look look, boolean left, boolean right, boolean top, boolean bottom) {
        }

        private final XSSFWorkbook wb;
        private final Map<Key, XSSFCellStyle> styles = new HashMap<>();
        private final Map<String, XSSFFont> fonts = new HashMap<>();

        Writer(XSSFWorkbook wb) {
            this.wb = wb;
        }

        XSSFFont font(String name, double size, boolean bold, boolean italic) {
            return font(name, size, bold, italic, false);
        }

        private XSSFFont font(String name, double size, boolean bold, boolean italic, boolean red) {
            return fonts.computeIfAbsent(name + size + bold + italic + red, k -> {
                XSSFFont f = wb.createFont();
                f.setFontName(name);
                f.setFontHeight(size);
                f.setBold(bold);
                f.setItalic(italic);
                if (red) {
                    f.setColor(RED);
                }
                return f;
            });
        }

        /**
         * Puts {@code value} in the top-left cell of {@code range} (merging it when it spans more than one cell)
         * and styles every cell of the range, so the borders close around the whole region. A null value leaves
         * a blank cell, never an empty string.
         */
        void put(Sheet sh, String range, Object value, Look look) {
            CellRangeAddress region = CellRangeAddress.valueOf(range);
            if (region.getNumberOfCells() > 1) {
                sh.addMergedRegion(region);
            }
            for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                Row row = row(sh, r);
                for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                    Cell cell = row.getCell(c) != null ? row.getCell(c) : row.createCell(c);
                    cell.setCellStyle(style(new Key(look,
                            c == region.getFirstColumn() && look.edges().charAt(0) != '-',
                            c == region.getLastColumn() && look.edges().charAt(1) != '-',
                            r == region.getFirstRow() && look.edges().charAt(2) != '-',
                            r == region.getLastRow() && look.edges().charAt(3) != '-')));
                }
            }
            Cell cell = row(sh, region.getFirstRow()).getCell(region.getFirstColumn());
            switch (value) {
                case null -> cell.setBlank();
                case Formula f -> cell.setCellFormula(f.expression());
                case XSSFRichTextString rich -> cell.setCellValue(rich);
                case BigDecimal number -> cell.setCellValue(number.doubleValue());
                case Number number -> cell.setCellValue(number.doubleValue());
                default -> {
                    String text = value.toString();
                    if (text.isBlank()) {
                        cell.setBlank();
                    } else {
                        cell.setCellValue(text);
                    }
                }
            }
        }

        private XSSFCellStyle style(Key key) {
            return styles.computeIfAbsent(key, k -> {
                Look look = k.look();
                XSSFCellStyle s = wb.createCellStyle();
                s.setFont(font(look.font(), look.size(), look.bold(), false, look.red()));
                s.setAlignment(look.h());
                if (look.middle()) {
                    s.setVerticalAlignment(VerticalAlignment.CENTER);
                }
                s.setWrapText(look.wrap());
                if (look.format() != null) {
                    s.setDataFormat(wb.createDataFormat().getFormat(look.format()));
                }
                XSSFColor color = switch (look.fill()) {
                    case YELLOW -> YELLOW;
                    case WHITE -> WHITE;
                    case NONE -> null;
                };
                if (color != null) {
                    s.setFillForegroundColor(color);
                    s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
                if (k.left()) {
                    s.setBorderLeft(look.line());
                }
                if (k.right()) {
                    s.setBorderRight(look.line());
                }
                if (k.top()) {
                    s.setBorderTop(look.line());
                }
                if (k.bottom()) {
                    s.setBorderBottom(look.line());
                }
                return s;
            });
        }
    }
}
