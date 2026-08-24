package ro.ecoregistru.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.Attachment;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.Anexa1FormGenerator;
import ro.ecoregistru.service.export.AnnualDeclarationGenerator;
import ro.ecoregistru.service.export.ExportFormat;
import ro.ecoregistru.service.export.GenericEvidenceExporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.joining;
import static ro.ecoregistru.exception.ErrorMessageEnum.AUDIT_FILE_YEARS_UNSUPPORTED;
import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;

/**
 * Builds the "dosar de control" (audit file) for a tenant and year as a single ZIP:
 *   - README.txt describing the contents and generation date,
 *   - "Evidenta gestiunii deseurilor generate" (HG 856/2002, anexa 1): four chapters per
 *     waste code, one page each,
 *   - the annual declaration: the same year folded to one line per waste code, per work point,
 *   - the generic evidence summary in both xlsx and pdf,
 *   - a PDF summary of partner authorizations (with expiry status),
 *   - atasamente/index.txt listing every movement attachment, and the attachment files
 *     themselves (downloaded best-effort from Cloudinary; a failed download stays referenced
 *     in the index so the dossier is still complete).
 *
 * Tenant-scoped throughout via {@link TenantContext#require()}. The dossier is not itself a
 * regulated format, but since 24.08.2026 it carries one document that is: the specialist asked
 * that printing it respect the four-table-per-waste-code structure of her own sheets, which is
 * exactly what {@link Anexa1FormGenerator} draws. The rest of the bundle stays a practical,
 * human-readable working pack around that sheet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditFileService {

    private static final java.time.format.DateTimeFormatter DATE =
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int ATTACHMENT_TIMEOUT_SECONDS = 15;
    /**
     * How far back a dossier may reach. OUG 92/2021 art. 48 alin. (5): the operator keeps the
     * waste-management evidence "cel putin 3 ani" (12 months for transporters), and that is the
     * period an inspection can ask for.
     *
     * <p><b>Five, not three, since 24.08.2026</b>, at the specialist's request: "sa fie pastrate
     * 5 ani documentele din dosar, sunt 3 in lege dar de safety". The law sets a floor, not a
     * ceiling, and the margin costs nothing here - a year the application never kept simply comes
     * out empty, and README.txt names it as such instead of shipping a blank official sheet.
     */
    private static final int MAX_YEARS = 5;
    /** Width of the file-name column in README.txt, wide enough for "2026/declaratie-anuala-2026.pdf". */
    private static final int NAME_COLUMN = 31;

    EvidenceCalculator evidenceCalculator;
    GenericEvidenceExporter evidenceExporter;
    Anexa1FormGenerator anexa1FormGenerator;
    AnnualDeclarationGenerator annualDeclarationGenerator;
    PartnerRepository partnerRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;

    /** One year, at the root of the archive - the shape the dossier had before Etapa 6. */
    public byte[] build(int year) {
        return build(year, 1);
    }

    /**
     * The dossier for {@code years} consecutive years ending in {@code year} - so
     * {@code build(2026, 3)} covers 2024, 2025 and 2026, the retention window of OUG 92/2021
     * art. 48 alin. (5).
     *
     * <p>One year keeps the flat layout; more than one puts each year in its own folder, because
     * the file names inside repeat. The partner authorizations stay at the root either way: their
     * status ("expira in 30 de zile") is read against today, not against a reporting year, so a
     * copy per year would be the same page three times, carrying a date that fits none of them.
     */
    @Transactional
    public byte[] build(int year, int years) {
        if (years < 1 || years > MAX_YEARS) {
            throw new BadRequestException(AUDIT_FILE_YEARS_UNSUPPORTED);
        }
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));

        List<Partner> partners = partnerRepository.findAllByCompany_Id(tenantId);
        int firstYear = year - years + 1;

        // Read once, used twice: the README says how many evidence lines each year actually has,
        // and the exports print them.
        //
        // Rebuilt first, and that is the point. The monthly lines are a cache of the movements,
        // and a client who has never pressed "Regenerează" used to get a dossier of blank official
        // sheets — which is what the specialist saw on 25.08.2026 ("a generat doar documente
        // aiurea, fără date"). The dossier is the one download nobody should have to prepare for,
        // so it brings the cache up to date itself. Idempotent: with nothing new to fold in, the
        // lines come out identical.
        Map<Integer, List<MonthlyEvidenceResponse>> evidenceByYear = new LinkedHashMap<>();
        for (int y = firstYear; y <= year; y++) {
            evidenceCalculator.regenerateYear(y);
            evidenceByYear.put(y, evidenceCalculator.list(y, null, null));
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {

            writeEntry(zip, "README.txt",
                    readme(company, firstYear, year, evidenceByYear).getBytes(StandardCharsets.UTF_8));
            for (int y = firstYear; y <= year; y++) {
                // A single year stays where it always was; several would collide on the file
                // names, so each gets a folder named after it.
                writeYear(zip, years == 1 ? "" : y + "/", company, tenantId, y, evidenceByYear.get(y));
            }
            writeEntry(zip, "autorizatii-parteneri.pdf",
                    partnerAuthorizationsPdf(company.getName(), partners));

            zip.finish();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build audit-file ZIP", ex);
        }
    }

    /** Everything that belongs to one reporting year, written under {@code prefix}. */
    private void writeYear(ZipOutputStream zip, String prefix, Company company, UUID tenantId,
                           int year, List<MonthlyEvidenceResponse> evidence) throws IOException {
        List<WasteMovement> movements = movementRepository
                .findAllByCompany_IdAndDeletedFalseAndDateBetween(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        // The regulated document of the bundle, and the reason the dossier gets printed at
        // all: one page per waste code, carrying the four chapters of the form.
        writeEntry(zip, prefix + "evidenta-gestiunii-deseurilor-" + year + ".pdf",
                anexa1FormGenerator.render(evidenceCalculator.anexa1(year, null)));
        // The summary page that goes in front of those sheets: same figures, folded to the
        // year, which is what the authority reads before it opens the twelve-row detail.
        writeEntry(zip, prefix + "declaratie-anuala-" + year + ".pdf",
                annualDeclarationGenerator.render(
                        evidenceCalculator.annualDeclaration(year, null)));
        writeEntry(zip, prefix + "evidenta-" + year + ".xlsx",
                evidenceExporter.export(ExportFormat.XLSX, company.getName(), year, null, evidence));
        writeEntry(zip, prefix + "evidenta-" + year + ".pdf",
                evidenceExporter.export(ExportFormat.PDF, company.getName(), year, null, evidence));

        writeAttachments(zip, prefix, movements);
    }

    // --- attachments ---

    private void writeAttachments(ZipOutputStream zip, String prefix, List<WasteMovement> movements)
            throws IOException {
        StringBuilder index = new StringBuilder();
        index.append("Atașamente ale mișcărilor de deșeuri\n");
        index.append("=====================================\n\n");

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(ATTACHMENT_TIMEOUT_SECONDS)).build();

        int n = 0;
        int downloaded = 0;
        for (WasteMovement m : movements.stream()
                .sorted(Comparator.comparing(WasteMovement::getDate)).toList()) {
            for (Attachment a : m.getAttachments()) {
                n++;
                String label = m.getDate().format(DATE) + " · " + m.getWasteCode().getCode()
                        + " · " + safe(m.getDocumentReference());
                String entryName = prefix + "atasamente/" + n + "-" + fileName(a);
                index.append(n).append(". ").append(label).append("\n")
                        .append("   fișier: ").append(fileName(a)).append("\n")
                        .append("   URL:    ").append(a.getUrl()).append("\n");

                byte[] bytes = tryDownload(http, a.getUrl());
                if (bytes != null) {
                    writeEntry(zip, entryName, bytes);
                    downloaded++;
                    index.append("   inclus în arhivă: da\n");
                } else {
                    index.append("   inclus în arhivă: NU (descărcare eșuată — vezi URL)\n");
                }
                index.append("\n");
            }
        }

        if (n == 0) {
            index.append("Nu există atașamente pentru mișcările din această perioadă.\n");
        } else {
            index.append("Total: ").append(n).append(" atașamente, ")
                    .append(downloaded).append(" incluse în arhivă.\n");
        }
        writeEntry(zip, prefix + "atasamente/index.txt", index.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Best-effort binary download; returns null on any failure so the build never breaks. */
    private byte[] tryDownload(HttpClient http, String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(ATTACHMENT_TIMEOUT_SECONDS)).GET().build();
            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() == 200) {
                return res.body();
            }
            log.warn("Attachment download returned {} for {}", res.statusCode(), url);
        } catch (Exception e) {
            log.warn("Attachment download failed for {}: {}", url, e.getMessage());
        }
        return null;
    }

    // --- partner authorizations PDF ---

    private byte[] partnerAuthorizationsPdf(String companyName, List<Partner> partners) {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font companyFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font titleFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            doc.add(new Paragraph(companyName == null ? "" : companyName, companyFont));
            Paragraph title = new Paragraph("Autorizații parteneri (colectori / transportatori)", titleFont);
            title.setSpacingAfter(10f);
            doc.add(title);

            String[] cols = {"Denumire", "CUI", "Tip", "Nr. autorizație", "Expirare", "Status"};
            PdfPTable table = new PdfPTable(cols.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{26, 14, 16, 16, 14, 14});

            Font headFont = new Font(Font.HELVETICA, 8, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
            for (String col : cols) {
                PdfPCell hc = new PdfPCell(new Phrase(col, headFont));
                hc.setBackgroundColor(new java.awt.Color(0xEC, 0xFD, 0xF5));
                hc.setPadding(4f);
                table.addCell(hc);
            }

            LocalDate today = LocalDate.now();
            for (Partner p : partners.stream()
                    .sorted(Comparator.comparing(Partner::getName, String.CASE_INSENSITIVE_ORDER)).toList()) {
                cell(table, p.getName(), bodyFont);
                cell(table, safe(p.getCui()), bodyFont);
                cell(table, partnerType(p.getType()), bodyFont);
                cell(table, safe(p.getAuthorizationNumber()), bodyFont);
                cell(table, p.getAuthorizationExpiry() != null
                        ? p.getAuthorizationExpiry().format(DATE) : "—", bodyFont);
                cell(table, statusText(p, today), bodyFont);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build partner authorizations PDF", ex);
        }
    }

    private String statusText(Partner p, LocalDate today) {
        if (!p.isActive()) {
            return "Inactiv";
        }
        LocalDate expiry = p.getAuthorizationExpiry();
        if (expiry == null) {
            return "Activ";
        }
        long days = ChronoUnit.DAYS.between(today, expiry);
        if (days < 0) {
            return "Autorizație expirată";
        }
        if (days <= 60) {
            return "Expiră în " + days + " zile";
        }
        return "Activ";
    }

    private static void cell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setPadding(3f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    // --- helpers ---

    /**
     * The cover note. It names the Anexa 1 sheet first because that is the regulated document of
     * the bundle, and it prints its filing deadline: 15 March of the following year, a legal term
     * (OUG 92/2021 art. 48 alin. (1)), not an ANPM custom.
     *
     * <p>A dossier covering several years repeats the content block once per year, under the
     * folder that holds it, and says up front why three is the number that matters.
     */
    private String readme(Company company, int firstYear, int lastYear,
                          Map<Integer, List<MonthlyEvidenceResponse>> evidenceByYear) {
        boolean single = firstYear == lastYear;
        StringBuilder sb = new StringBuilder();
        sb.append("DOSAR DE CONTROL — ").append(company.getName()).append("\n");
        if (single) {
            sb.append("Anul de raportare: ").append(lastYear).append("\n");
        } else {
            sb.append("Anii de raportare: ").append(firstYear).append("–").append(lastYear)
                    .append(" (").append(lastYear - firstYear + 1).append(" ani)\n")
                    .append("Termenul de păstrare a evidenței: cel puțin 3 ani — OUG 92/2021, art. 48\n")
                    .append("alin. (5). Pentru transportatori, cel puțin 12 luni.\n");
        }
        sb.append("Generat: ").append(LocalDate.now().format(DATE)).append("\n\n");

        for (int year = firstYear; year <= lastYear; year++) {
            String prefix = single ? "" : year + "/";
            if (!single) {
                sb.append("== ").append(year).append(" ").append("=".repeat(60)).append("\n");
            }
            sb.append("Conținut:\n")
                    .append(entry(prefix + "evidenta-gestiunii-deseurilor-" + year + ".pdf",
                            "Evidența gestiunii deșeurilor generate " + year,
                            "(HG 856/2002, anexa 1) — fișa oficială, cu cele patru",
                            "capitole, o pagină per cod de deșeu.",
                            "Termen de depunere: 15 martie " + (year + 1) + "."))
                    .append(entry(prefix + "declaratie-anuala-" + year + ".pdf",
                            "centralizatorul anual — un rând per cod de deșeu,",
                            "cu stoc inițial, generat, valorificat, eliminat,",
                            "stoc final și prin cine. O pagină per punct de lucru."))
                    .append(entry(prefix + "evidenta-" + year + ".xlsx / .pdf",
                            "același an ca tabel de lucru (rezumat neoficial)"));
            if (single) {
                sb.append(entry("autorizatii-parteneri.pdf",
                        "autorizațiile partenerilor și statusul lor"));
            }
            sb.append(entry(prefix + "atasamente/",
                    "documentele justificative atașate mișcărilor (+ index.txt)"));
            sb.append(evidenceNote(year, evidenceByYear.get(year)));
            sb.append("\n");
        }

        if (!single) {
            sb.append("La rădăcina arhivei, o singură dată:\n")
                    .append(entry("autorizatii-parteneri.pdf",
                            "autorizațiile partenerilor și statusul lor",
                            "(status citit la data generării, nu pe an)"))
                    .append("\n");
        }

        sb.append(marketRoleNote(company))
                .append("Notă: în afară de evidența gestiunii deșeurilor de mai sus, dosarul NU înlocuiește\n")
                .append("formularele oficiale de\n")
                .append("raportare (SIM / AFM); este un pachet de lucru pentru pregătirea și prezentarea la control.\n");
        return sb.toString();
    }

    /**
     * One "  - name : description" line, plus its continuation lines under the description.
     * The name column is padded to a fixed width so the colons line up whether the file sits at
     * the root or inside a year folder - the README is read at an inspection, on paper.
     */
    private static String entry(String name, String... lines) {
        String padded = name.length() < NAME_COLUMN
                ? name + " ".repeat(NAME_COLUMN - name.length())
                : name;
        StringBuilder sb = new StringBuilder("  - ").append(padded).append(" : ").append(lines[0]).append("\n");
        String indent = " ".repeat(4 + NAME_COLUMN + 3);
        for (int i = 1; i < lines.length; i++) {
            sb.append(indent).append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    /**
     * A year with no evidence lines is not an empty year - it is a year nobody regenerated, and
     * the sheets for it come out blank. Saying so beats handing over a blank official form.
     */
    private String evidenceNote(int year, List<MonthlyEvidenceResponse> evidence) {
        if (evidence != null && !evidence.isEmpty()) {
            return "";
        }
        return "\n  ATENȚIE: pentru " + year + " nu există nicio linie de evidență calculată, deci fișele de\n"
                + "  mai sus ies goale. Dacă anul are mișcări înregistrate, deschide Evidențe, alege anul\n"
                + "  " + year + " și apasă „Regenerează”, apoi descarcă dosarul din nou.\n";
    }

    /**
     * What the account answered to "ce tip de generator" — producător, importator or comerciant —
     * and the one thing that follows from it. Only the packaging declaration does: the sheet above
     * is kept by anyone who generates waste (HG 856/2002 art. 1 alin. (1)), whatever it sells.
     *
     * <p>An unanswered profile prints nothing at all rather than a guess in either direction.
     */
    private String marketRoleNote(Company company) {
        Set<MarketRole> roles = company.getMarketRoles();
        if (!MarketRole.answered(roles)) {
            return "";
        }
        String named = roles.stream().map(AuditFileService::marketRole).collect(joining(", "));
        return "Tipul de generator declarat: " + named + ".\n"
                + (MarketRole.putsPackagingOnMarket(roles)
                ? "  -> pune produse ambalate pe piață, deci depune și declarația de ambalaje\n"
                + "     — Anexa 1 Ambalaje (Ordinul 794/2012), termen 25 februarie.\n"
                : "  -> comerciant: vinde marfă ambalată de altcineva, deci NU depune declarația de\n"
                + "     ambalaje — Anexa 1 Ambalaje (Ordinul 794/2012). Evidența gestiunii\n"
                + "     deșeurilor generate de mai sus rămâne obligatorie.\n")
                + "\n";
    }

    private static String marketRole(MarketRole role) {
        return switch (role) {
            case PRODUCER -> "producător";
            case IMPORTER -> "importator";
            case TRADER -> "comerciant";
        };
    }

    private String partnerType(PartnerType type) {
        return switch (type) {
            case COLLECTOR -> "Colector";
            case RECOVERER -> "Valorificator";
            case GENERATOR -> "Generator";
        };
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    /** A filesystem-safe attachment file name for the zip entry. */
    private static String fileName(Attachment a) {
        String name = a.getFileName();
        if (name == null || name.isBlank()) {
            name = "atasament";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
