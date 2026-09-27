package ro.ecoregistru.service.export;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Inventory;
import ro.ecoregistru.entity.InventoryCommissionMember;
import ro.ecoregistru.entity.InventoryLine;
import ro.ecoregistru.entity.KeeperDeclaration;
import ro.ecoregistru.entity.StockOpening;
import ro.ecoregistru.entity.StockOpeningLine;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.enums.InventoryKind;
import ro.ecoregistru.enums.ShortageNature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ro.ecoregistru.service.export.DepotPdf.cp1250;

/**
 * D3.5 — documentele inventarului și nota de preluare, pe OpenPDF. Conținutul urmează Normele OMFP 2861/2009 (decizia
 * pct. 6, declarația pct. 8 lit. a), lista pct. 18–20 și 33, procesul-verbal pct. 42–43) și modelul 14-3-12 din OMFP
 * 2634/2015 (coloanele 0–14; valorile rămân contabilului). Procesul-verbal n-are model oficial: are elementele pct. 42.
 */
@Component
public class InventoryDocumentsGenerator {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    static final String[] LIST_COLUMNS = {"Nr. crt.", "Denumirea bunurilor inventariate", "Codul sau numărul de inventar",
            "U/M", "Stocuri scriptice", "Stocuri faptice", "Diferențe plus", "Diferențe minus", "Preț unitar",
            "Valoarea contabilă", "Diferențe valoare plus", "Diferențe valoare minus", "Valoarea de inventar",
            "Deprecierea: valoarea", "Deprecierea: motivul (cod)"};

    /** Un membru al comisiei pe o scară de semnături, cu rolul lui. */
    public record ScaleNote(String name, String serial, String state) {
    }

    /** O operațiune din perioada inventarului, pentru anexa PV-ului (pct. 9). */
    public record DuringNote(String label, String counterparty) {
    }

    private final AppVersion version;
    private final Font title;
    private final Font bold;
    private final Font body;
    private final Font small;
    private final Font head;

    public InventoryDocumentsGenerator(AppVersion version) {
        this.version = version;
        title = new Font(DepotPdf.font(true), 12f);
        bold = new Font(DepotPdf.font(true), 9f);
        body = new Font(DepotPdf.font(false), 9f);
        small = new Font(DepotPdf.font(false), 7.5f);
        head = new Font(DepotPdf.font(true), 7f);
    }

    // --- decizia (pct. 6) ---

    public byte[] decision(Company company, Inventory inv) {
        return portrait(doc -> {
            entity(doc, company);
            heading(doc, "DECIZIE nr. " + dash(inv.getDecisionNumber()) + " din " + date(inv.getDecisionDate()),
                    "privind inventarierea gestiunii „" + inv.getWorkPoint().getName() + "”");
            para(doc, "În temeiul art. 7 din Legea contabilității nr. 82/1991 și al pct. 6 din Normele privind "
                    + "organizarea și efectuarea inventarierii, aprobate prin OMFP nr. 2861/2009, se decide:");
            para(doc, "Art. 1. Se efectuează inventarierea " + kind(inv.getKind()) + " a gestiunii „"
                    + inv.getWorkPoint().getName() + "”, gestionar " + keepers(inv) + ".");
            para(doc, "Art. 2. Comisia de inventariere:");
            for (InventoryCommissionMember m : inv.getCommission()) {
                para(doc, "    – " + m.getName() + (m.getRole() == null ? "" : ", " + m.getRole())
                        + (m.isPresident() ? " — preşedinte" : " — membru"));
            }
            para(doc, "Art. 3. Modul de efectuare: " + dash(inv.getMode()) + ". Metoda: " + dash(inv.getMethod()) + ".");
            para(doc, "Art. 4. Inventarierea începe la " + date(inv.getStartsOn()) + " și se termină la "
                    + date(inv.getEndsOn()) + ". Stocul scriptic se ia la începutul zilei de " + date(inv.getStartsOn()) + ".");
            if (inv.isCountsAsAnnual()) {
                para(doc, "Art. 5. Inventarierea ține loc de inventarierea anuală (pct. 2 alin. (2) din Norme).");
            }
        }, List.of("Administrator"));
    }

    // --- declarația gestionarului (pct. 8 lit. a)) ---

    public byte[] declaration(Company company, Inventory inv) {
        return portrait(doc -> {
            entity(doc, company);
            heading(doc, "DECLARAŢIA GESTIONARULUI", "luată înainte de inventarierea gestiunii „"
                    + inv.getWorkPoint().getName() + "” (pct. 8 lit. a) din Normele aprobate prin OMFP nr. 2861/2009)");
            para(doc, "Subsemnatul " + inv.getKeeperName() + ", gestionar al gestiunii „" + inv.getWorkPoint().getName()
                    + "”, declar că:");
            List<KeeperDeclaration.Answer> answers = inv.getDeclaration() == null ? null : inv.getDeclaration().answers();
            for (int i = 0; i < KeeperDeclaration.QUESTIONS.size(); i++) {
                KeeperDeclaration.Answer a = answers == null || i >= answers.size() ? null : answers.get(i);
                String answer = a == null ? "____" : a.yes() ? "DA" + (a.detail() == null ? "" : " — " + a.detail()) : "NU";
                para(doc, "    " + (i + 1) + ". " + KeeperDeclaration.QUESTIONS.get(i) + ": " + answer);
            }
            para(doc, "Ultimul document de intrare: " + dash(inv.getLastEntryDoc()));
            para(doc, "Ultimul document de ieșire: " + dash(inv.getLastExitDoc()));
            para(doc, "Data: " + date(inv.getDeclarationDate()));
            para(doc, "Semnarea declarației de către gestionar se face în fața comisiei de inventariere.");
        }, signatures(inv, false));
    }

    // --- lista de inventariere 14-3-12 ---

    public byte[] list(Company company, Inventory inv) {
        List<InventoryLine> normal = inv.getLines().stream().filter(l -> !l.isSlowMoving()).toList();
        List<InventoryLine> slow = inv.getLines().stream().filter(InventoryLine::isSlowMoving).toList();
        return render(PageSize.A4.rotate(), doc -> {
            listHeader(doc, company, inv, null);
            listTable(doc, normal);
            if (!slow.isEmpty()) {
                doc.newPage();
                listHeader(doc, company, inv, "Stocuri depreciate, fără mișcare sau greu vandabile (pct. 20 din Norme)");
                listTable(doc, slow);
            }
            Paragraph gap = new Paragraph(" ", body);
            doc.add(gap);
            para(doc, "Gestionarul menționează că toate bunurile din gestiune au fost inventariate și consemnate în "
                    + "listele de inventariere în prezența sa.");
            para(doc, "Obiecțiile gestionarului cu privire la modul de efectuare a inventarierii: "
                    + (inv.getKeeperObjections() == null ? "fără obiecții" : inv.getKeeperObjections()));
            if (inv.getKeeperObjections() != null) {
                para(doc, "Concluziile comisiei asupra obiecțiilor: " + dash(inv.getCommissionConclusions()));
            }
        }, signatures(inv, true));
    }

    private void listHeader(Document doc, Company company, Inventory inv, String subtitle) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{40, 30, 30});
        t.setWidthPercentage(100);
        t.addCell(plain("Entitatea: " + company.getName() + (company.getCui() == null ? "" : ", CUI " + company.getCui()), body));
        PdfPCell name = plain("LISTĂ DE INVENTARIERE", title);
        name.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(name);
        PdfPCell when = plain("Data: " + date(inv.getStartsOn()) + " · Nr. " + inv.getNumber(), body);
        when.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(when);
        String depot = inv.getWorkPoint().getName();
        PdfPCell where = plain("Gestiunea: " + depot + " · Magazia: " + depot + " · Loc de depozitare: " + depot, body);
        where.setColspan(3);
        t.addCell(where);
        doc.add(t);
        if (subtitle != null) {
            Paragraph p = new Paragraph(cp1250(subtitle), bold);
            p.setSpacingBefore(4f);
            doc.add(p);
        }
        doc.add(new Paragraph(" ", small));
    }

    private void listTable(Document doc, List<InventoryLine> lines) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{3, 16, 7, 3, 6, 6, 6, 6, 5, 6, 6, 6, 6, 6, 6});
        t.setWidthPercentage(100);
        t.setHeaderRows(2);
        for (String c : LIST_COLUMNS) {
            PdfPCell cell = new PdfPCell(new Phrase(cp1250(c), head));
            cell.setGrayFill(0.9f);
            t.addCell(cell);
        }
        for (int i = 0; i < LIST_COLUMNS.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(i), head));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(cell);
        }
        int no = 1;
        for (InventoryLine l : lines) {
            BigDecimal d = l.difference();
            t.addCell(new Phrase(String.valueOf(no++), small));
            String name = l.getArticle() != null ? l.getArticle().getName() : l.getWasteCode().getName();
            if (l.getTechnicalData() != null) {
                name += "\nCalcul tehnic: " + l.getTechnicalData();
            }
            t.addCell(new Phrase(cp1250(name), small));
            t.addCell(new Phrase(code(l.getWasteCode()), small));
            t.addCell(new Phrase("kg", small));
            t.addCell(number(l.getBookKg()));
            t.addCell(number(l.getCountedKg()));
            t.addCell(number(d != null && d.signum() > 0 ? d : null));
            t.addCell(number(d != null && d.signum() < 0 ? d.negate() : null));
            for (int i = 8; i < LIST_COLUMNS.length; i++) {
                t.addCell(new Phrase("", small));
            }
        }
        doc.add(t);
    }

    // --- procesul-verbal (pct. 42–43) ---

    public byte[] pv(Company company, Inventory inv, List<DuringNote> during, List<ScaleNote> scales) {
        return portrait(doc -> {
            entity(doc, company);
            heading(doc, "PROCES-VERBAL DE INVENTARIERE nr. " + inv.getNumber(), "gestiunea „" + inv.getWorkPoint().getName() + "”");
            para(doc, "1. Data întocmirii: " + date(inv.getPvDate() != null ? inv.getPvDate() : inv.getClosedOn()));
            StringBuilder members = new StringBuilder();
            for (InventoryCommissionMember m : inv.getCommission()) {
                if (!members.isEmpty()) members.append("; ");
                members.append(m.getName()).append(m.getRole() == null ? "" : ", " + m.getRole())
                        .append(m.isPresident() ? " (preşedinte)" : "");
            }
            para(doc, "2. Comisia de inventariere: " + members);
            para(doc, "3. Decizia de inventariere: nr. " + dash(inv.getDecisionNumber()) + " din " + date(inv.getDecisionDate())
                    + " (inventariere " + kind(inv.getKind()) + ")");
            para(doc, "4. Gestiunea inventariată: " + inv.getWorkPoint().getName() + ", gestionar " + keepers(inv));
            para(doc, "5. Data începerii şi data terminării: " + date(inv.getStartsOn()) + " – " + date(inv.getEndsOn()));
            para(doc, "6. Rezultatele inventarierii:");
            results(doc, inv);
            para(doc, "7. Cauzele plusurilor şi lipsurilor constatate şi persoanele vinovate: " + dash(inv.getPvCauses()));
            for (InventoryLine l : inv.getLines()) {
                BigDecimal d = l.difference();
                if (d == null || d.signum() == 0) continue;
                para(doc, "    – " + describe(l) + ": " + (d.signum() > 0 ? "plus " : "minus ") + kg(d.abs()) + " kg — "
                        + dash(l.getExplanation()) + (d.signum() < 0 ? " (" + nature(l.getShortageNature())
                        + (l.getResponsiblePerson() == null ? "" : ", " + l.getResponsiblePerson()) + ")" : ""));
            }
            para(doc, "8. Propunerile de măsuri privind regularizarea diferențelor: " + dash(inv.getPvMeasures()));
            para(doc, "9. Stocurile depreciate, fără mișcare sau greu vandabile şi propunerile de valorificare: "
                    + dash(inv.getPvSlowStock()));
            inv.getLines().stream().filter(InventoryLine::isSlowMoving)
                    .forEach(l -> para(doc, "    – " + describe(l) + ": " + kg(l.getCountedKg()) + " kg"));
            para(doc, "10. Propunerile de scoatere din funcțiune: nu e cazul (mijloace fixe).");
            para(doc, "11. Propunerile de scoatere din uz, declasare sau casare: nu e cazul (mijloace fixe).");
            para(doc, "12. Constatări privind păstrarea, depozitarea şi conservarea bunurilor: " + dash(inv.getPvStorageFindings()));
            if (!scales.isEmpty()) {
                para(doc, "    Cântarele depozitului la " + date(inv.getStartsOn()) + ":");
                scales.forEach(s -> para(doc, "    – " + s.name() + (s.serial() == null ? "" : ", seria " + s.serial())
                        + ": " + s.state()));
            }
            para(doc, "13. Alte aspecte: " + dash(inv.getPvOther()));
            para(doc, "Obiecțiile gestionarului: " + (inv.getKeeperObjections() == null ? "fără obiecții" : inv.getKeeperObjections())
                    + (inv.getKeeperObjections() == null ? "" : ". Concluziile comisiei: " + dash(inv.getCommissionConclusions())));
            para(doc, "Anexă — operațiuni datate în perioada inventarului (primit/eliberat în timpul inventarierii, pct. 9):");
            if (during.isEmpty()) {
                para(doc, "    – niciuna");
            }
            during.forEach(o -> para(doc, "    – " + o.label() + (o.counterparty() == null ? "" : " — " + o.counterparty())));
            doc.add(new Paragraph(" ", body));
            para(doc, "Aviz financiar-contabil: ______________________________");
            para(doc, "Aviz juridic: ______________________________");
            para(doc, "Aprobat, administrator: ______________________________   Data: ____________");
        }, signatures(inv, true));
    }

    private void results(Document doc, Inventory inv) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{24, 9, 10, 10, 9, 9});
        t.setWidthPercentage(100);
        for (String c : new String[]{"Denumire", "Cod", "Scriptic (kg)", "Faptic (kg)", "Plus (kg)", "Minus (kg)"}) {
            PdfPCell cell = new PdfPCell(new Phrase(cp1250(c), head));
            cell.setGrayFill(0.9f);
            t.addCell(cell);
        }
        BigDecimal plus = BigDecimal.ZERO;
        BigDecimal minus = BigDecimal.ZERO;
        for (InventoryLine l : inv.getLines()) {
            BigDecimal d = l.difference();
            t.addCell(new Phrase(cp1250(describe(l)), small));
            t.addCell(new Phrase(code(l.getWasteCode()), small));
            t.addCell(number(l.getBookKg()));
            t.addCell(number(l.getCountedKg()));
            t.addCell(number(d != null && d.signum() > 0 ? d : null));
            t.addCell(number(d != null && d.signum() < 0 ? d.negate() : null));
            if (d != null && d.signum() > 0) plus = plus.add(d);
            if (d != null && d.signum() < 0) minus = minus.add(d.negate());
        }
        PdfPCell total = new PdfPCell(new Phrase(cp1250("Total"), head));
        total.setColspan(4);
        t.addCell(total);
        t.addCell(number(plus));
        t.addCell(number(minus));
        doc.add(t);
    }

    // --- nota de preluare ---

    public byte[] openingNote(Company company, StockOpening o) {
        return portrait(doc -> {
            entity(doc, company);
            heading(doc, "NOTĂ DE PRELUARE A SOLDURILOR" + (o.getNumber() == null ? " (ciornă)" : " nr. " + o.getNumber()),
                    "gestiunea „" + o.getWorkPoint().getName() + "” · data de tăiere " + date(o.getCutOffDate()));
            para(doc, "Soldurile de mai jos sunt stocul existent la data de tăiere, preluat în evidența ținută în "
                    + "aplicație din " + source(o) + ". Nu sunt plusuri de inventar. Reconcilierea cu evidența de până "
                    + "acum (OMFP nr. 2634/2015, anexa 1 pct. 61) o confirmă gestionarul și contabilul prin semnătură.");
            PdfPTable t = new PdfPTable(new float[]{4, 30, 10, 4, 10});
            t.setWidthPercentage(100);
            for (String c : new String[]{"Nr. crt.", "Denumire", "Cod deșeu", "U/M", "Cantitate"}) {
                PdfPCell cell = new PdfPCell(new Phrase(cp1250(c), head));
                cell.setGrayFill(0.9f);
                t.addCell(cell);
            }
            int no = 1;
            for (StockOpeningLine l : o.getLines()) {
                t.addCell(new Phrase(String.valueOf(no++), small));
                t.addCell(new Phrase(cp1250(l.getArticle() != null ? l.getArticle().getName() : l.getWasteCode().getName()), small));
                t.addCell(new Phrase(code(l.getWasteCode()), small));
                t.addCell(new Phrase("kg", small));
                t.addCell(number(l.getKg()));
            }
            doc.add(t);
            if (o.getNotes() != null) {
                para(doc, "Observații: " + o.getNotes());
            }
            if (o.getConfirmedOn() != null) {
                para(doc, "Confirmată la " + date(o.getConfirmedOn()) + ".");
            }
        }, List.of("Gestionar: " + dash(o.getKeeperName()), "Contabil: " + dash(o.getAccountantName())));
    }

    // --- helpers ---

    private interface Body {
        void write(Document doc) throws DocumentException;
    }

    private byte[] portrait(Body body, List<String> signatures) {
        return render(PageSize.A4, body, signatures);
    }

    private byte[] render(Rectangle size, Body content, List<String> signatures) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(size, 28, 28, 28, DepotPdf.BOTTOM_MARGIN);
            PdfWriter.getInstance(doc, out);
            doc.open();
            content.write(doc);
            doc.close();
            return DepotPdf.stamp(out.toByteArray(), signatures, version.value());
        } catch (IOException | DocumentException ex) {
            throw new IllegalStateException("Cannot build the inventory document", ex);
        }
    }

    private List<String> signatures(Inventory inv, boolean withKeeperOnEveryPage) {
        List<String> s = new ArrayList<>();
        StringBuilder commission = new StringBuilder("Comisia de inventariere:");
        for (InventoryCommissionMember m : inv.getCommission()) {
            commission.append("\n").append(m.getName()).append(m.isPresident() ? " (preşedinte)" : "");
        }
        s.add(commission.toString());
        s.add("Gestionar: " + inv.getKeeperName()
                + (inv.getKeeperRepresentative() == null ? "" : "\nReprezentant: " + inv.getKeeperRepresentative()));
        if (inv.getReceivingKeeperName() != null) {
            s.add("Gestionar primitor: " + inv.getReceivingKeeperName());
        }
        if (withKeeperOnEveryPage) {
            s.add("Contabilitate");
        }
        return s;
    }

    private void entity(Document doc, Company company) throws DocumentException {
        doc.add(new Paragraph(cp1250(company.getName() + (company.getCui() == null ? "" : " · CUI " + company.getCui())), small));
    }

    private void heading(Document doc, String text, String sub) throws DocumentException {
        Paragraph p = new Paragraph(cp1250(text), title);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(8f);
        doc.add(p);
        Paragraph s = new Paragraph(cp1250(sub), body);
        s.setAlignment(Element.ALIGN_CENTER);
        s.setSpacingAfter(10f);
        doc.add(s);
    }

    private void para(Document doc, String text) {
        try {
            Paragraph p = new Paragraph(cp1250(text), body);
            p.setSpacingAfter(3f);
            doc.add(p);
        } catch (DocumentException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(new Chunk(cp1250(text), font)));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell number(BigDecimal value) {
        PdfPCell c = new PdfPCell(new Phrase(value == null ? "" : kg(value), small));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static String kg(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String code(WasteCode c) {
        return c.getCode() + (c.isHazardous() ? "*" : "");
    }

    private static String describe(InventoryLine l) {
        return (l.getArticle() != null ? l.getArticle().getName() : l.getWasteCode().getName()) + " (" + code(l.getWasteCode()) + ")";
    }

    private static String keepers(Inventory inv) {
        return inv.getKeeperName() + (inv.getReceivingKeeperName() == null ? "" : " (predător), "
                + inv.getReceivingKeeperName() + " (primitor)");
    }

    private static String date(LocalDate d) {
        return d == null ? "____________" : d.format(DATE);
    }

    private static String dash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String nature(ShortageNature n) {
        return n == null ? "natură nestabilită" : n == ShortageNature.IMPUTABLE ? "imputabilă" : "neimputabilă";
    }

    private static String source(StockOpening o) {
        return switch (o.getSource()) {
            case STOCK_CARDS -> "fișele de magazie";
            case ACCOUNTING -> "balanța analitică a conturilor de stocuri";
            case PHYSICAL_COUNT -> "o numărătoare faptică (firma nu ținea evidență cantitativă)";
        };
    }

    static String kind(InventoryKind k) {
        return switch (k) {
            case START_OF_ACTIVITY -> "la începutul activității";
            case ANNUAL -> "anuală";
            case MERGER_OR_LIQUIDATION -> "la fuziune, divizare sau încetarea activității";
            case CONTROL -> "la cererea organelor de control";
            case SUSPECTED_DIFFERENCES -> "ca urmare a unor indicii de lipsuri sau plusuri";
            case HANDOVER -> "la predarea-primirea gestiunii";
            case REORGANIZATION -> "la reorganizarea gestiunilor";
            case FORCE_MAJEURE -> "după calamități sau forță majoră";
            case OTHER -> "potrivit procedurii proprii";
        };
    }
}
