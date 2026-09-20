package ro.ecoregistru.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
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
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AttachmentRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.Anexa1FormGenerator;
import ro.ecoregistru.service.export.AnnualDeclarationGenerator;
import ro.ecoregistru.service.export.ExportFormat;
import ro.ecoregistru.service.export.PackagingAnexa3;
import ro.ecoregistru.service.export.ReportBranding;
import ro.ecoregistru.util.Diacritics;
import ro.ecoregistru.util.UsedOilCodes;
import ro.ecoregistru.util.WasteCodeLabel;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static ro.ecoregistru.exception.ErrorMessageEnum.AUDIT_FILE_YEARS_UNSUPPORTED;
import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;

/**
 * Builds the "dosar de control" (audit file) for a tenant and year as a single ZIP:
 *   - 00-cuprins.txt describing the contents and generation date (README.txt until 17.09.2026),
 *   - autorizatii-parteneri.pdf, a summary of partner authorizations (with expiry status and the
 *     codes each carried),
 *   - rapoarte/: the official reports —
 *   - "Evidenta gestiunii deseurilor generate" (HG 856/2002, anexa 1): four chapters per
 *     waste code, one page each,
 *   - "Evidenta gestiunii deseurilor centralizata" (the former annual declaration): the same year
 *     folded to one line per waste code, per work point,
 *   - Anexa 1 Ambalaje (.xls + .pdf), when the company puts packaging on the market,
 *   - Anexa 3 Ambalaje (.xls + .pdf), one pair per work point that moved packaging that year,
 *   - atasamente/index.txt, outside rapoarte/, listing every movement attachment, and the attachment files
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

    /*
     * Structura arhivei (proprietarul, 17.09.2026): cuprinsul primul, lista autorizațiilor, rapoartele
     * oficiale împreună în rapoarte/ și atașamentele în atasamente/. Rezumatul neoficial al evidenței
     * a ieșit din dosar; rămâne de descărcat din Evidențe.
     */
    private static final String CONTENTS = "00-cuprins.txt";
    private static final String PARTNERS = "autorizatii-parteneri.pdf";
    private static final String REPORTS_DIR = "rapoarte/";
    private static final String ATTACHMENTS_DIR = "atasamente/";
    /** R2 — ultima intrare din arhivă; prezenţa ei cu „DOSAR COMPLET" e dovada că nimic n-a căzut. */
    private static final String CHECK = "99-verificare.txt";

    private static final Color LINE = new Color(0xD1, 0xD5, 0xDB);
    private static final Color MUTED = new Color(0x4B, 0x55, 0x63);
    private static final Color HEAD = new Color(0xEC, 0xFD, 0xF5);

    EvidenceCalculator evidenceCalculator;
    Anexa1FormGenerator anexa1FormGenerator;
    AnnualDeclarationGenerator annualDeclarationGenerator;
    PartnerRepository partnerRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    AttachmentRepository attachmentRepository;
    CloudinaryStorageService storageService;
    PackagingService packagingService;
    WorkPointRepository workPointRepository;
    ReportBrandingService brandingService;

    /**
     * The dossier for {@code years} consecutive years ending in {@code year} - so
     * {@code write(2026, 3, out)} covers 2024, 2025 and 2026, the retention window of OUG 92/2021
     * art. 48 alin. (5).
     *
     * <p>One year keeps the flat layout; more than one puts each year in its own folder, because
     * the file names inside repeat. The partner authorizations stay at the root either way: their
     * status ("expira in 30 de zile") is read against today, not against a reporting year, so a
     * copy per year would be the same page three times, carrying a date that fits none of them.
     *
     * <p>R3 (QA-FINAL-REPORT §5): the archive goes straight into {@code target}, not into a byte
     * array first. Five years of attachments held whole in a 300 MB heap was the one download that
     * could end in {@code OutOfMemoryError}; now only one entry at a time is in memory. Everything
     * that can refuse the request (the year range, the tenant, the company) runs before the first
     * byte, so a refusal still leaves the response uncommitted for the error envelope.
     */
    @Transactional
    public void write(int year, int years, OutputStream target) {
        if (years < 1 || years > MAX_YEARS) {
            throw new BadRequestException(AUDIT_FILE_YEARS_UNSUPPORTED);
        }
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));

        List<Partner> partners = partnerRepository.findAllByCompany_Id(tenantId);
        int firstYear = year - years + 1;
        // P2.14: the consultancy's header, on the working pack only — the contents and the partner
        // list. The official sheets in the same archive print their model and nothing else.
        ReportBranding branding = brandingService.forCompany(tenantId);

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
        Map<Integer, YearFiles> filesByYear = new LinkedHashMap<>();
        List<WorkPoint> workPoints = workPointRepository.findAllByCompany_Id(tenantId).stream()
                .sorted(Comparator.comparing(WorkPoint::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        boolean single = years == 1;
        boolean packaging = MarketRole.putsPackagingOnMarket(company.getMarketRoles());
        // One regeneration of the first year rebuilds the whole chain, earlier years included (BUG-045).
        //
        // R1 — într-o tranzacție a ei, care comite aici. Reconstrucția ia `pg_advisory_xact_lock`
        // pe firmă, iar un lacăt de tranzacție ține până la commit: chemată în tranzacția asta,
        // ținea firma blocată pe toată durata descărcării, iar orice salvare de mișcare aștepta.
        // Vezi `EvidenceCalculator.regenerateYearBeforeStreaming`.
        evidenceCalculator.regenerateYearBeforeStreaming(firstYear);
        for (int y = firstYear; y <= year; y++) {
            evidenceByYear.put(y, evidenceCalculator.list(y, null, null));
            filesByYear.put(y, yearFiles(y, single, packaging, anexa3Plan(y, workPoints)));
        }

        List<String> written = new java.util.ArrayList<>();

        try (ZipOutputStream zip = new ZipOutputStream(target)) {
            try {
                writeEntry(zip, written, CONTENTS,
                        readme(company, firstYear, year, evidenceByYear, filesByYear, branding)
                                .getBytes(StandardCharsets.UTF_8));
                // Codurile de deșeu pe care le-a purtat fiecare partener în perioada dosarului, strânse din
                // aceleași mișcări pe care le citesc anii — coloana din lista autorizațiilor.
                Map<UUID, Set<String>> codesByPartner = new HashMap<>();
                for (int y = firstYear; y <= year; y++) {
                    // A single year stays where it always was; several would collide on the file
                    // names, so each gets a folder named after it.
                    writeYear(zip, written, tenantId, y, filesByYear.get(y), codesByPartner);
                }
                writeEntry(zip, written, PARTNERS,
                        partnerAuthorizationsPdf(company, partners, codesByPartner, firstYear, year, branding));

                // R2 — ultima intrare, şi numai dacă tot ce e mai sus a reuşit. Vezi `CHECK`.
                writeEntry(zip, written, CHECK, checkList(written, true));
            } catch (IOException | RuntimeException ex) {
                // Răspunsul e deja comis cu 200 de la primul octet — statusul nu se mai poate
                // schimba. Singurul lucru cinstit care a mai rămas e ca arhiva să **spună** că e
                // incompletă, în locul în care cineva se uită: lipseşte `99-verificare.txt` cu
                // „DOSAR COMPLET", iar în loc apare unul care numeşte ce lipseşte.
                log.error("Dosarul de control s-a întrerupt după {} intrări", written.size(), ex);
                io.sentry.Sentry.captureException(ex);
                try {
                    writeEntry(zip, written, CHECK, checkList(written, false));
                } catch (IOException ignored) {
                    // Conexiunea a căzut de tot: n-avem unde scrie avertismentul. Jurnalul rămâne.
                }
                throw (ex instanceof IOException io) ? new UncheckedIOException(io) : (RuntimeException) ex;
            }
            zip.finish();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build audit-file ZIP", ex);
        }
    }

    /**
     * R2 — foaia care spune dacă arhiva e întreagă.
     *
     * <p>Odată scris primul octet, răspunsul e comis cu 200: o excepţie la jumătatea dosarului
     * lăsa clientul cu un ZIP care se deschidea şi părea în regulă, dar căruia îi lipseau ani
     * întregi — iar dosarul ăsta se duce la control. „Incomplet, dar aparent valid" e cel mai prost
     * mod de a pierde date, fiindcă nimeni nu caută ce nu ştie că lipseşte.
     *
     * <p>Foaia se scrie <b>ultima</b>, deci simpla ei prezenţă cu „DOSAR COMPLET" e dovada că tot
     * ce e înaintea ei a trecut. Lista de fişiere şi mărimi e pentru omul care compară.
     */
    private static byte[] checkList(List<String> written, boolean complete) {
        StringBuilder sb = new StringBuilder();
        sb.append(complete ? "DOSAR COMPLET\n" : "⚠️ DOSAR INCOMPLET — generarea s-a întrerupt\n");
        sb.append("Generat: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        sb.append("Fișiere scrise: ").append(written.size()).append("\n\n");
        if (!complete) {
            sb.append("Arhiva se deschide, dar NU conține tot ce trebuia. Descarcă dosarul din nou;\n")
                    .append("dacă se întrerupe iar, anunță-ne — mai jos e exact ce a apucat să intre.\n\n");
        }
        sb.append("fișier\tocteți\n");
        written.forEach(line -> sb.append(line).append("\n"));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Cât cântărește dosarul, înainte de descărcare. Numai atașamentele: foile generate au câteva sute de KB,
     * iar pozele și PDF-urile scanate sunt restul. Aceleași reguli de interval și de firmă ca {@link #write}.
     */
    @Transactional(readOnly = true)
    public AuditFileSize size(int year, int years) {
        if (years < 1 || years > MAX_YEARS) {
            throw new BadRequestException(AUDIT_FILE_YEARS_UNSUPPORTED);
        }
        UUID tenantId = TenantContext.require();
        Object[] row = attachmentRepository.sizeOfLiveMovementsBetween(
                tenantId, LocalDate.of(year - years + 1, 1, 1), LocalDate.of(year, 12, 31)).get(0);
        long all = ((Number) row[0]).longValue();
        long sized = ((Number) row[1]).longValue();
        return new AuditFileSize(all, ((Number) row[2]).longValue(), all - sized);
    }

    /** {@code unknownSize}: atașamente de dinainte de V57, fără mărime ținută. */
    public record AuditFileSize(long attachments, long attachmentBytes, long unknownSize) {}

    /**
     * Ce intră în dosar și de ce, înainte de descărcare (proprietarul, 17.09.2026: „să fie informații
     * reale”). Citește exact regulile după care scrie {@link #write} — același {@link #anexa3Plan},
     * aceeași condiție pe rolul de piață, aceleași mișcări care contează —, deci ecranul nu poate
     * promite un document pe care arhiva nu-l are. Nu regenerează nimic: numără mișcările.
     */
    @Transactional(readOnly = true)
    public AuditFileContents contents(int year, int years) {
        if (years < 1 || years > MAX_YEARS) {
            throw new BadRequestException(AUDIT_FILE_YEARS_UNSUPPORTED);
        }
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        List<WorkPoint> workPoints = workPointRepository.findAllByCompany_Id(tenantId).stream()
                .sorted(Comparator.comparing(WorkPoint::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<YearContents> perYear = new java.util.ArrayList<>();
        for (int y = year - years + 1; y <= year; y++) {
            Anexa3Plan plan = anexa3Plan(y, workPoints);
            perYear.add(new YearContents(y,
                    movementRepository.countCountedBetween(tenantId, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31)),
                    plan.files().stream().map(Anexa3File::workPointName).toList(),
                    plan.roleMissing()));
        }

        Set<MarketRole> roles = company.getMarketRoles();
        PackagingDeclaration declaration = MarketRole.putsPackagingOnMarket(roles) ? PackagingDeclaration.INCLUDED
                : MarketRole.answered(roles) ? PackagingDeclaration.TRADER_ONLY
                : PackagingDeclaration.NOT_ANSWERED;

        List<Partner> partners = partnerRepository.findAllByCompany_Id(tenantId);
        LocalDate today = DeadlineService.today();
        long expired = partners.stream().filter(p -> status(p, today) == AuthStatus.EXPIRED).count();
        long soon = partners.stream().filter(p -> status(p, today) == AuthStatus.SOON).count();

        return new AuditFileContents(perYear, declaration, !company.getType().keepsArt48Register(),
                partners.size(), expired, soon);
    }

    /**
     * {@code anexa3ExitsOnly}: firma nu ține registrul de colector, deci Anexa 3 Ambalaje are numai
     * ieșirile și nu are termen de depunere (Ordinul 794/2012 art. 4 alin. (1)).
     */
    public record AuditFileContents(List<YearContents> years, PackagingDeclaration packagingDeclaration,
                                    boolean anexa3ExitsOnly, long partners, long partnersExpired,
                                    long partnersExpiringSoon) {}

    /** {@code anexa3WorkPoints}: punctele de lucru care primesc o Anexă 3 Ambalaje în anul acesta. */
    public record YearContents(int year, long movements, List<String> anexa3WorkPoints,
                               boolean anexa3RoleMissing) {}

    /** De ce intră sau nu Anexa 1 Ambalaje: după rolul de piață din profilul firmei. */
    public enum PackagingDeclaration { INCLUDED, TRADER_ONLY, NOT_ANSWERED }

    /** Everything that belongs to one reporting year, written under {@code files.prefix()}. */
    private void writeYear(ZipOutputStream zip, List<String> written, UUID tenantId, int year, YearFiles files,
                           Map<UUID, Set<String>> codesByPartner) throws IOException {
        String prefix = files.prefix() + REPORTS_DIR;
        List<WasteMovement> movements = movementRepository
                .findCountedBetween(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        for (WasteMovement m : movements) {
            for (Partner p : new Partner[]{m.getPartner(), m.getTransportPartner()}) {
                if (p != null && m.getWasteCode() != null) {
                    codesByPartner.computeIfAbsent(p.getId(), id -> new TreeSet<>())
                            .add(WasteCodeLabel.official(m.getWasteCode().getCode(), m.getWasteCode().isHazardous()));
                }
            }
        }

        // The regulated document of the bundle, and the reason the dossier gets printed at
        // all: one page per waste code, carrying the four chapters of the form.
        writeEntry(zip, written, prefix + files.sheet(),
                anexa1FormGenerator.render(evidenceCalculator.anexa1(year, null)));
        // The summary page that goes in front of those sheets: same figures, folded to the
        // year, which is what the authority reads before it opens the twelve-row detail.
        // „Evidenţa gestiunii deşeurilor centralizată" — numele cerut de specialistă pe 15.09.2026
        // pentru ce se numea până atunci „declaraţia anuală".
        writeEntry(zip, written, prefix + files.centralized(),
                annualDeclarationGenerator.render(
                        evidenceCalculator.annualDeclaration(year, null)));
        // Anexa 1 Ambalaje lipsea din dosar (specialista, 15.09.2026). Numai la firma care pune
        // ambalaje pe piaţă: un comerciant n-o depune, iar la un profil nerăspuns nu ghicim.
        if (files.packaging() != null) {
            writeEntry(zip, written, prefix + files.packaging() + ".xls",
                    packagingService.render(year, ExportFormat.XLS));
            writeEntry(zip, written, prefix + files.packaging() + ".pdf",
                    packagingService.render(year, ExportFormat.PDF));
        }
        // Anexa 3 Ambalaje (proprietarul, 16.09.2026): una per punct de lucru, fiindcă aşa se depune
        // (Ordinul 794/2012 art. 4 alin. (4)), şi numai unde anul are ambalaje — o foaie oficială
        // goală n-are ce căuta la control.
        for (Anexa3File file : files.anexa3()) {
            writeEntry(zip, written, prefix + file.baseName() + ".xls",
                    packagingService.renderAnexa3(year, file.workPointId(), ExportFormat.XLS));
            writeEntry(zip, written, prefix + file.baseName() + ".pdf",
                    packagingService.renderAnexa3(year, file.workPointId(), ExportFormat.PDF));
        }
        writeAttachments(zip, written, files.prefix() + ATTACHMENTS_DIR, movements, attachmentRepository.findAllOfLiveMovementsBetween(
                tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    /**
     * Numele fișierelor unui an, calculate o dată și citite de două ori — de arhivă și de cuprins —, ca
     * cele două să nu poată spune lucruri diferite. {@code prefix} e folderul anului (gol la un singur an);
     * rapoartele stau sub el, în rapoarte/. {@code packaging}: numele Anexei 1 Ambalaje fără extensie,
     * null când firma n-o depune.
     */
    private record YearFiles(String prefix, String sheet, String centralized, String packaging,
                             List<Anexa3File> anexa3, boolean anexa3RoleMissing, int anexa3Unplaced) {}

    private static YearFiles yearFiles(int year, boolean single, boolean packaging, Anexa3Plan plan) {
        return new YearFiles(single ? "" : year + "/",
                "evidenta-gestiunii-deseurilor-" + year + ".pdf",
                "evidenta-centralizata-" + year + ".pdf",
                packaging ? "anexa1-ambalaje-" + year : null,
                plan.files(), plan.roleMissing(), plan.unplaced());
    }

    // --- attachments ---

    private void writeAttachments(ZipOutputStream zip, List<String> written, String folder, List<WasteMovement> movements,
                                  List<Attachment> attachments) throws IOException {
        // Read once for the year, not through `m.getAttachments()`: the regeneration just before
        // flushes a hundred-odd writes, and after it Hibernate stopped batching that lazy
        // collection — one select per movement (BUG-016, measured in `PerformanceIT`).
        Map<UUID, List<Attachment>> attachmentsByMovement = attachments.stream()
                .collect(groupingBy(a -> a.getMovement().getId()));
        StringBuilder index = new StringBuilder();
        index.append("Atașamente ale mișcărilor de deșeuri\n");
        index.append("=====================================\n\n");

        int n = 0;
        int downloaded = 0;
        for (WasteMovement m : movements.stream()
                .sorted(Comparator.comparing(WasteMovement::getDate)).toList()) {
            for (Attachment a : attachmentsByMovement.getOrDefault(m.getId(), List.of())) {
                n++;
                String label = m.getDate().format(DATE) + " · " + m.getWasteCode().getCode()
                        + " · " + safe(m.getDocumentReference());
                String entryName = folder + n + "-" + fileName(a);
                index.append(n).append(". ").append(label).append("\n")
                        .append("   fișier: ").append(fileName(a)).append("\n");

                byte[] bytes = tryDownload(a);
                if (bytes != null) {
                    writeEntry(zip, written, entryName, bytes);
                    downloaded++;
                    index.append("   inclus în arhivă: da\n");
                } else {
                    index.append("   inclus în arhivă: NU (descărcarea a eșuat)\n");
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
        writeEntry(zip, written, folder + "index.txt", index.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Best-effort binary download; returns null on any failure so the build never breaks.
     *
     * <p>Since 11-bis the URL is signed here and thrown away — it is not written into index.txt
     * any more. It used to be, as a fallback for the reader when a download failed, and that line
     * was a public link to a client's document sitting inside a file the client mails to an
     * inspector. The archive carries the file itself or says it is missing; it no longer carries
     * a way in.
     */
    private byte[] tryDownload(Attachment a) {
        try {
            String url = a.getDeliveryType() == null
                    ? a.getUrl()
                    : storageService.signedUrl(a.getPublicId(), a.getResourceType(),
                            a.getDeliveryType(), a.getFormat());
            return storageService.fetch(url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Attachment download interrupted for id={}", a.getId());
        } catch (Exception e) {
            log.warn("Attachment download failed for id={}: {}", a.getId(), e.getMessage());
        }
        return null;
    }

    // --- partner authorizations PDF ---

    /**
     * The partner list: a working page, so it carries the consultancy's header, but it sits next to
     * the official sheets and has to look like it belongs there.
     *
     * <p>17.09.2026: until now it was set in the standard Helvetica, whose encoding has no ă, ș or ț —
     * the page printed „Autorizaii” and „Expir în”. It now uses the Cp1250 Helvetica of the other
     * generators, lies in landscape for the extra column, repeats its header on every page, colours the
     * status and names the waste codes each partner carried in the dossier's period, so an inspector can
     * lay the authorization next to what went through it.
     */
    private byte[] partnerAuthorizationsPdf(Company company, List<Partner> partners,
                                            Map<UUID, Set<String>> codesByPartner, int firstYear, int lastYear,
                                            ReportBranding branding) {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 48);
        LocalDate today = DeadlineService.today();
        String period = firstYear == lastYear ? String.valueOf(lastYear) : firstYear + "–" + lastYear;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageFooter(company.getName() + " · Autorizațiile partenerilor"));
            doc.open();

            ReportBranding.addPdfHeader(doc, branding);

            BaseFont plain = centralEuropean(false);
            BaseFont bold = centralEuropean(true);
            doc.add(new Paragraph(cp1250(safe(company.getName())), new Font(bold, 14)));
            if (company.getCui() != null && !company.getCui().isBlank()) {
                doc.add(new Paragraph(cp1250("CUI " + company.getCui()), new Font(plain, 9, Font.NORMAL, MUTED)));
            }
            Paragraph title = new Paragraph(cp1250("Autorizațiile partenerilor"), new Font(bold, 12));
            title.setSpacingBefore(10f);
            doc.add(title);
            doc.add(new Paragraph(cp1250("Status citit la " + today.format(DATE)
                    + " · codurile de deșeu din " + period), new Font(plain, 9, Font.NORMAL, MUTED)));
            Paragraph summary = new Paragraph(cp1250(summaryLine(partners, today)), new Font(plain, 9));
            summary.setSpacingBefore(4f);
            summary.setSpacingAfter(10f);
            doc.add(summary);

            String[] cols = {"Denumire", "CUI", "Tip", "Nr. autorizație", "Viză anuală",
                    "Valabilă până la", "Coduri de deșeu " + period, "Status"};
            PdfPTable table = new PdfPTable(cols.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{19, 9, 10, 11, 12, 10, 16, 13});
            table.setHeaderRows(1);

            Font headFont = new Font(bold, 8);
            Font bodyFont = new Font(plain, 8);
            for (String col : cols) {
                PdfPCell hc = new PdfPCell(new Phrase(cp1250(col), headFont));
                hc.setBackgroundColor(HEAD);
                hc.setBorderColor(LINE);
                hc.setPadding(5f);
                table.addCell(hc);
            }

            List<Partner> sorted = partners.stream()
                    .sorted(Comparator.comparing(Partner::getName, String.CASE_INSENSITIVE_ORDER)).toList();
            for (Partner p : sorted) {
                cell(table, p.getName(), bodyFont);
                cell(table, safe(p.getCui()), bodyFont);
                cell(table, partnerType(p), bodyFont);
                cell(table, safe(p.getAuthorizationNumber()), bodyFont);
                cell(table, visaText(p), bodyFont);
                cell(table, p.authorizationValidUntil() != null
                        ? p.authorizationValidUntil().format(DATE) : "—", bodyFont);
                Set<String> codes = codesByPartner.get(p.getId());
                cell(table, codes == null || codes.isEmpty() ? "—" : String.join(", ", codes), bodyFont);
                statusCell(table, p, today, bold);
            }
            if (sorted.isEmpty()) {
                PdfPCell none = new PdfPCell(new Phrase(cp1250("Nu există parteneri înregistrați."), bodyFont));
                none.setColspan(cols.length);
                none.setBorderColor(LINE);
                none.setPadding(6f);
                table.addCell(none);
            }
            doc.add(table);

            Paragraph note = new Paragraph(cp1250("„Valabilă până la” este data care vine prima dintre expirarea "
                    + "autorizației și sfârșitul vizei anuale. Statusul trece pe „Expiră în …” cu 60 de zile înainte."),
                    new Font(plain, 7.5f, Font.NORMAL, MUTED));
            note.setSpacingBefore(8f);
            doc.add(note);
            doc.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build partner authorizations PDF", ex);
        }
    }

    /** „8 parteneri · 1 cu autorizația expirată · 2 expiră în 60 de zile · 1 inactiv”, numai ce nu e zero. */
    private static String summaryLine(List<Partner> partners, LocalDate today) {
        long expired = 0;
        long soon = 0;
        long inactive = 0;
        for (Partner p : partners) {
            switch (status(p, today)) {
                case EXPIRED -> expired++;
                case SOON -> soon++;
                case INACTIVE -> inactive++;
                default -> { }
            }
        }
        StringBuilder sb = new StringBuilder(count(partners.size(), "partener", "parteneri"));
        if (expired > 0) {
            sb.append(" · ").append(expired).append(" cu autorizația expirată");
        }
        if (soon > 0) {
            sb.append(" · ").append(soon).append(" expiră în următoarele 60 de zile");
        }
        if (inactive > 0) {
            sb.append(" · ").append(count(inactive, "inactiv", "inactivi"));
        }
        return sb.toString();
    }

    /** „1 partener”, „5 parteneri”, „20 de parteneri”: de la 20 în sus româna cere „de”. */
    private static String count(long n, String one, String many) {
        if (n == 1) {
            return "1 " + one;
        }
        long lastTwo = n % 100;
        return n + (n >= 20 && (lastTwo == 0 || lastTwo >= 20) ? " de " : " ") + many;
    }

    private enum AuthStatus { INACTIVE, NO_DATE, EXPIRED, SOON, VALID }

    private static AuthStatus status(Partner p, LocalDate today) {
        if (!p.isActive()) {
            return AuthStatus.INACTIVE;
        }
        LocalDate expiry = p.authorizationValidUntil();
        if (expiry == null) {
            return AuthStatus.NO_DATE;
        }
        long days = ChronoUnit.DAYS.between(today, expiry);
        if (days < 0) {
            return AuthStatus.EXPIRED;
        }
        return days <= 60 ? AuthStatus.SOON : AuthStatus.VALID;
    }

    private static String statusText(Partner p, LocalDate today) {
        return switch (status(p, today)) {
            case INACTIVE -> "Inactiv";
            case NO_DATE, VALID -> "Activ";
            case EXPIRED -> "Autorizație expirată";
            case SOON -> {
                long days = ChronoUnit.DAYS.between(today, p.authorizationValidUntil());
                yield days == 0 ? "Expiră azi"
                        : days == 1 ? "Expiră mâine"
                        : "Expiră în " + count(days, "zi", "zile");
            }
        };
    }

    /** Statusul: verde valabilă, galben aproape de expirare, roșu expirată, gri inactiv, alb fără dată. */
    private static void statusCell(PdfPTable table, Partner p, LocalDate today, BaseFont bold) {
        Color[] colors = switch (status(p, today)) {
            case EXPIRED -> new Color[]{new Color(0xFE, 0xE2, 0xE2), new Color(0x99, 0x1B, 0x1B)};
            case SOON -> new Color[]{new Color(0xFE, 0xF3, 0xC7), new Color(0x92, 0x40, 0x0E)};
            case INACTIVE -> new Color[]{new Color(0xF3, 0xF4, 0xF6), MUTED};
            case VALID -> new Color[]{new Color(0xD1, 0xFA, 0xE5), new Color(0x06, 0x5F, 0x46)};
            // Fără dată de valabilitate nu știm dacă e valabilă: activ, dar fără verdele care ar spune-o.
            case NO_DATE -> new Color[]{Color.WHITE, Color.BLACK};
        };
        PdfPCell cell = new PdfPCell(new Phrase(cp1250(statusText(p, today)), new Font(bold, 8, Font.NORMAL, colors[1])));
        cell.setBackgroundColor(colors[0]);
        cell.setBorderColor(LINE);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    /** Decizia de viză cum o scrie agenţia: numărul şi data, sau o liniuţă când nu e tastată (V41). */
    private static String visaText(Partner p) {
        if (p.getVisaDecisionNumber() == null && p.getVisaDecisionDate() == null) {
            return "—";
        }
        return (p.getVisaDecisionNumber() == null ? "" : "nr. " + p.getVisaDecisionNumber())
                + (p.getVisaDecisionDate() == null ? "" : " din " + p.getVisaDecisionDate().format(DATE));
    }

    private static void cell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(cp1250(value == null ? "" : value), font));
        cell.setPadding(4f);
        cell.setBorderColor(LINE);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    /** „Firma · Autorizațiile partenerilor” la stânga, „pagina 1 din 3” la dreapta, pe fiecare pagină. */
    private static final class PageFooter extends PdfPageEventHelper {
        private final String label;
        private PdfTemplate total;
        private BaseFont font;

        PageFooter(String label) {
            this.label = label;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(30, 14);
            font = centralEuropean(false);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float y = document.bottom() - 24;
            String page = "pagina " + writer.getPageNumber() + " din ";
            float pageWidth = font.getWidthPoint(page, 7.5f);
            cb.saveState();
            cb.setColorFill(MUTED);
            cb.beginText();
            cb.setFontAndSize(font, 7.5f);
            cb.setTextMatrix(document.left(), y);
            cb.showText(cp1250(label));
            cb.setTextMatrix(document.right() - pageWidth - 14, y);
            cb.showText(page);
            cb.endText();
            cb.addTemplate(total, document.right() - 14, y - 3);
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(font, 7.5f);
            total.setColorFill(MUTED);
            total.setTextMatrix(0, 3);
            // La închidere writer-ul stă deja pe pagina următoarei, necreate: ultima e cu una mai jos.
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }

    /** ș și ț cu virgulă nu sunt în Cp1250; varianta cu sedilă e, și așa scriu și celelalte generatoare. */
    private static String cp1250(String value) {
        return value.replace('ș', 'ş').replace('Ș', 'Ş').replace('ț', 'ţ').replace('Ț', 'Ţ');
    }

    private static BaseFont centralEuropean(boolean bold) {
        try {
            return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                    "Cp1250", BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Cannot load the Cp1250 Helvetica for the partner list", ex);
        }
    }

    // --- helpers ---

    /**
     * The cover note, {@code 00-cuprins.txt}. It names the Anexa 1 sheet first because that is the
     * regulated document of the bundle, and it prints its filing deadline: 15 March of the following
     * year, a legal term (OUG 92/2021 art. 48 alin. (1)), not an ANMAP custom.
     *
     * <p>A dossier covering several years repeats the content block once per year, under the
     * folder that holds it, and says up front why three is the number that matters. The file names
     * come from {@link YearFiles}, the same record the archive is written from.
     */
    private String readme(Company company, int firstYear, int lastYear,
                          Map<Integer, List<MonthlyEvidenceResponse>> evidenceByYear,
                          Map<Integer, YearFiles> filesByYear,
                          ReportBranding branding) {
        boolean single = firstYear == lastYear;
        StringBuilder sb = new StringBuilder();
        sb.append("DOSAR DE CONTROL — ").append(company.getName()).append("\n");
        if (branding != null) {
            sb.append(branding.textLine()).append("\n");
        }
        if (single) {
            sb.append("Anul de raportare: ").append(lastYear).append("\n");
        } else {
            sb.append("Anii de raportare: ").append(firstYear).append("–").append(lastYear)
                    .append(" (").append(lastYear - firstYear + 1).append(" ani)\n")
                    .append("Termenul de păstrare a evidenței: cel puțin 3 ani — OUG 92/2021, art. 48\n")
                    .append("alin. (5). Pentru transportatori, cel puțin 12 luni.\n");
        }
        sb.append("Generat: ").append(DeadlineService.today().format(DATE)).append("\n\n");
        sb.append("Rapoartele oficiale sunt în folderul ").append(REPORTS_DIR)
                .append(", iar documentele atașate mișcărilor\nîn ").append(ATTACHMENTS_DIR).append(".\n\n");

        if (!single) {
            sb.append("La rădăcina arhivei, o singură dată:\n")
                    .append(entry(PARTNERS,
                            "Autorizațiile partenerilor și statusul lor, citit la data generării,",
                            "nu pe an, cu codurile de deșeu din toată perioada dosarului."))
                    .append("\n");
        }

        for (int year = firstYear; year <= lastYear; year++) {
            YearFiles files = filesByYear.get(year);
            String prefix = files.prefix() + REPORTS_DIR;
            if (!single) {
                sb.append("== ").append(year).append(" ").append("=".repeat(60)).append("\n");
            }
            sb.append("Conținut:\n")
                    .append(entry(prefix + files.sheet(),
                            "Evidența gestiunii deșeurilor generate " + year,
                            "(HG 856/2002, anexa 1) — fișa oficială, cu cele patru",
                            "capitole, o pagină per cod de deșeu.",
                            "Termen de depunere: 15 martie " + (year + 1) + "."))
                    .append(entry(prefix + files.centralized(),
                            "Evidența gestiunii deșeurilor centralizată — un rând",
                            "per cod de deșeu, cu stoc inițial, generat, valorificat,",
                            "eliminat, stoc final și prin cine. O pagină per punct de lucru."));
            if (files.packaging() != null) {
                sb.append(entry(prefix + files.packaging() + ".xls / .pdf",
                        "Anexa 1 Ambalaje (Ordinul 794/2012) — declarația de",
                        "ambalaje: .xls pentru depunere, PDF pe hârtie.",
                        "Termen: 25 februarie " + (year + 1) + "."));
            }
            // Termenul de 25 februarie e al celor din art. 4 alin. (1) — colectori, comercianţi,
            // reciclatori, valorificatori. Un generator nu e numit acolo (docs/surse-oficiale.md
            // §2.11): la el foaia e tipărită la cerere, cu ieşirile, şi cuprinsul nu-i pune un
            // termen pe care nu-l are (proprietarul, 17.09.2026 — scanarea de conformitate, pct. 4).
            boolean owesAnexa3 = company.getType().keepsArt48Register();
            for (Anexa3File file : files.anexa3()) {
                sb.append(owesAnexa3
                        ? entry(prefix + file.baseName() + ".xls / .pdf",
                                "Anexa 3 Ambalaje (Ordinul 794/2012) — punctul de lucru",
                                "„" + file.workPointName() + "”: .xls pentru depunere, PDF pe hârtie.",
                                "Termen: 25 februarie " + (year + 1) + ".")
                        : entry(prefix + file.baseName() + ".xls / .pdf",
                                "Anexa 3 Ambalaje (Ordinul 794/2012) — punctul de lucru",
                                "„" + file.workPointName() + "”, numai cu ieşirile: tipărită la cerere.",
                                "Nu e o obligaţie de depunere a generatorului (art. 4 alin. (1) numeşte",
                                "colectorii, comercianţii, reciclatorii şi valorificatorii); fără termen."));
            }
            if (files.anexa3RoleMissing()) {
                sb.append(entry(prefix + "anexa3-ambalaje-" + year,
                        "LIPSEȘTE: anul are ambalaje, dar profilul firmei nu spune",
                        "dacă e colector, comerciant, reciclator sau valorificator,",
                        "deci nu se știe care tabel se completează. Răspunde în Setări."));
            }
            if (files.anexa3Unplaced() > 0) {
                sb.append(entry(prefix + "anexa3-ambalaje-" + year,
                        "LIPSEȘTE: " + (files.anexa3Unplaced() == 1 ? "o ieșire de ambalaj fără material nu apare"
                                : files.anexa3Unplaced() + " ieșiri de ambalaj fără material nu apar") + " pe Anexa 3.",
                        "Alege materialul pe „Generare”, la „De completat”, apoi descarcă din nou."));
            }
            if (single) {
                sb.append(entry(PARTNERS,
                        "Autorizațiile partenerilor și statusul lor, cu codurile de deșeu din an."));
            }
            sb.append(entry(files.prefix() + ATTACHMENTS_DIR,
                            "Documentele justificative atașate mișcărilor, cu index.txt."));
            sb.append(evidenceNote(year, evidenceByYear.get(year)));
            sb.append("\n");
        }

        sb.append(marketRoleNote(company))
                .append(wasteManagerNote(company))
                .append(otherObligationsNote(company, evidenceByYear))
                .append("Notă: în afară de evidența gestiunii deșeurilor de mai sus, dosarul NU înlocuiește\n")
                .append("formularele oficiale de\n")
                .append("raportare (SIM / AFM); este un pachet de lucru pentru pregătirea și prezentarea la control.\n");
        return sb.toString();
    }

    /**
     * One file of the contents: its name on its own line, the description indented under it. The
     * folder paths are too long for a column beside them, and the contents is read on paper.
     */
    private static String entry(String name, String... lines) {
        StringBuilder sb = new StringBuilder("  ").append(name).append("\n");
        for (String line : lines) {
            sb.append("      ").append(line).append("\n");
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

    /**
     * The person designated for waste management — OUG 92/2021 art. 23 alin. (4) and (5). Audit
     * point 8, added 04.09.2026: it is among the first things an inspector asks for, and the
     * dossier neither carried it nor named it.
     *
     * <p>When it has not been filled in, the block says so <b>out loud</b> instead of staying
     * silent, and that is the one place this differs from {@link #marketRoleNote}. The market role
     * is a property of the business: not knowing it means we cannot conclude anything, and printing
     * a guess would be worse than printing nothing. The designated person is an <em>obligation</em>
     * of every waste-generating business (art. 23 alin. (4) as amended by Legea 17/2023, in force
     * since 12.01.2023 — the earlier text said "titularul unei autorizații de mediu"), so its absence
     * is itself the finding — and
     * regula de lucru 1 says a gap must be visible as a gap. Better the client reads it here than
     * hears it from the inspector.
     */
    private String wasteManagerNote(Company company) {
        String name = company.getWasteManagerName();
        if (name == null || name.isBlank()) {
            return "Persoana desemnată cu gestiunea deșeurilor: NECOMPLETATĂ.\n"
                    + "  -> OUG 92/2021, art. 23 alin. (4): orice firmă a cărei activitate generează\n"
                    + "     deșeuri desemnează o persoană dintre angajați SAU deleagă obligația unei\n"
                    + "     terțe persoane (de exemplu consultantul de mediu). Alin. (5): dacă activitatea\n"
                    + "     are autorizație de mediu, persoana trebuie să fie și instruită prin programe\n"
                    + "     recunoscute la nivel național.\n"
                    // Profilul firmei se editează din ecranul Clienți, care e PLATFORM_ONLY — deci
                    // un ADMIN de client nu-l poate completa singur. Mesajul spune pe cine să
                    // întrebe, nu îl trimite într-un ecran pe care nu-l are.
                    + "     Se completează în profilul firmei (ecranul Clienți, cont de platformă):\n"
                    + "     cere-i-o consultantului care îți administrează contul, apoi regenerează\n"
                    + "     dosarul.\n\n";
        }
        StringBuilder sb = new StringBuilder("Persoana desemnată cu gestiunea deșeurilor ")
                .append("(OUG 92/2021, art. 23 alin. (4)):\n")
                .append("  - Nume    : ").append(name).append("\n");
        if (company.getWasteManagerRole() != null && !company.getWasteManagerRole().isBlank()) {
            sb.append("  - Calitate: ").append(company.getWasteManagerRole()).append("\n");
        }
        if (Boolean.TRUE.equals(company.getWasteManagerExternal())) {
            sb.append("  - Delegată unei terțe persoane (art. 23 alin. (4), a doua variantă).\n");
        } else if (Boolean.FALSE.equals(company.getWasteManagerExternal())) {
            sb.append("  - Angajat propriu.\n");
        }
        String training = company.getWasteManagerTraining();
        boolean hasEnvironmentalAuth = company.getEnvironmentalAuthNumber() != null
                && !company.getEnvironmentalAuthNumber().isBlank();
        if (training != null && !training.isBlank()) {
            sb.append("  - Instruire: ").append(training).append("\n");
        } else if (hasEnvironmentalAuth) {
            // Alin. (5) cere instruirea numai „pentru activitățile care necesită autorizație de mediu”;
            // fără autorizație în profil, un „NECOMPLETATĂ” ar fi o lipsă pe care legea nu o cere.
            sb.append("  - Instruire: NECOMPLETATĂ — art. 23 alin. (5) cere, la activitățile cu\n")
                    .append("               autorizație de mediu, absolvirea unui program de perfecționare\n")
                    .append("               recunoscut la nivel național.\n");
        }
        return sb.append("\n").toString();
    }

    /**
     * Five obligations of OUG 92/2021 that an inspection checks, that the dossier used to pass
     * over in silence, and that sit in the same sanctioned list as the evidence itself:
     * art. 62 alin. (1) lit. a), 40.000–60.000 lei for a legal person. Four read on 10.09.2026
     * from {@code docs/surse-oficiale.md} §2.8; the fifth — art. 44 — on 11.09.2026, from the
     * consolidated act.
     *
     * <p>Same principle as {@link #wasteManagerNote}: the absence of a legal obligation is itself
     * the finding, so the block speaks instead of staying quiet. The difference is what it can
     * claim. Three of the five the application can <em>check</em> against what it holds, and those
     * print as findings, naming the codes or the permit they found; the other two it can only
     * <em>name</em>, because nothing in the data decides them.
     *
     * <p><b>The fifth is half a finding, deliberately.</b> Art. 44 alin. (3) requires two separate
     * things: the programme is <em>published on the company's own website</em> and <em>transmitted
     * annually to the county agency by 31 May</em>. The second half is watched — it is
     * {@link ro.ecoregistru.enums.ReportType#APM_ANNUAL_MAY} in the calendar. The first half is a
     * website, which this application cannot observe, so it is named and handed back rather than
     * guessed at. Splitting them keeps the block honest about which half we actually cover.
     *
     * <p><b>Why art. 17 alin. (3) is not derived, though it looks derivable.</b> The temptation is
     * to list which of paper, metal, plastic, glass and textiles already appear in the evidence.
     * But a fraction missing from the evidence does not mean it is not collected separately — most
     * often it means the client generates none of it — and a fraction present does not prove the
     * separate collection the article is about, which happens on site and not in a register.
     * Neither direction carries information, and the rule from {@code ReportType} holds here too:
     * an alert is a statement. So the obligation is named in full, with its own date for textiles,
     * and nothing is concluded.
     */
    private String otherObligationsNote(Company company,
            Map<Integer, List<MonthlyEvidenceResponse>> evidenceByYear) {
        List<MonthlyEvidenceResponse> lines = evidenceByYear.values().stream()
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .toList();
        List<String> oils = UsedOilCodes
                .among(lines.stream().map(MonthlyEvidenceResponse::wasteCode).toList())
                .stream()
                .map(code -> WasteCodeLabel.official(code, true))
                .sorted()
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("ALTE OBLIGAȚII PE CARE LE VERIFICĂ INSPECTORUL\n")
                .append("Toate patru sunt în aceeași listă sancționată ca evidența de mai sus — OUG 92/2021,\n")
                .append("art. 62 alin. (1) lit. a): 40.000–60.000 lei pentru persoane juridice. Dosarul le\n")
                .append("numește ca să nu fie aflate la control; dovada lor nu stă în evidență.\n\n");

        sb.append("  1. Colectarea separată — art. 17 alin. (3)\n")
                .append("     Orice producător și deținător de deșeuri introduce colectarea separată cel\n")
                .append("     puțin pentru hârtie, metal, plastic și sticlă, iar de la 1 ianuarie 2025 și\n")
                .append("     pentru textile. Se verifică pe teren, la fracții, nu în registru — de aceea\n")
                .append("     aplicația o numește, dar nu o constată.\n\n");

        sb.append("  2. Înscrierea în registrul ANMAP — art. 36 alin. (1)–(2)\n")
                .append("     Trei feluri de operatori NU se autorizează, dar sunt obligați să se înscrie\n")
                .append("     în registrul ținut de agenție: cei care transportă deșeuri nepericuloase în\n")
                .append("     sistem profesional, comercianții care nu intră fizic în posesia deșeurilor și\n")
                .append("     brokerii. Dacă firma face una din cele trei, înscrierea se dovedește separat.\n")
                .append("     Alin. (3) al aceluiași articol mai cere o înscriere, în alt registru și fără\n")
                .append("     să fie în lista de amenzi de mai sus: operatorii care REPARĂ produse.\n\n");

        sb.append("  3. Predarea uleiurilor uzate — art. 31 alin. (3)\n");
        if (oils.isEmpty()) {
            sb.append("     Producătorii și deținătorii de uleiuri uzate, cu excepția persoanelor fizice,\n")
                    .append("     predau ÎNTREAGA cantitate numai operatorilor autorizați pentru colectarea,\n")
                    .append("     valorificarea sau eliminarea lor. În anii din dosar nu apare niciun cod de\n")
                    .append("     ulei uzat, deci obligația nu se activează pe aceste date.\n\n");
        } else {
            sb.append("     Te privește: în anii din dosar apar mișcări pe coduri de ulei uzat.\n")
                    .append(wrapped("     ", "Codurile: " + String.join(", ", oils) + "."))
                    .append("     Art. 31 alin. (3) cere ca ÎNTREAGA cantitate să fie predată numai operatorilor\n")
                    .append("     autorizați pentru colectarea, valorificarea sau eliminarea uleiurilor uzate —\n")
                    .append("     nu o parte din ea. Autorizațiile partenerilor prin care au plecat sunt în\n")
                    .append("     ").append(PARTNERS).append(", din acest dosar.\n\n");
        }

        sb.append("  4. Programul de prevenire și reducere a deșeurilor — art. 44 alin. (1) și (3)\n");
        String permit = company.getEnvironmentalAuthNumber();
        if (permit == null || permit.isBlank()) {
            sb.append("     Persoana juridică cu activitate comercială sau industrială PENTRU CARE s-a emis\n")
                    .append("     o autorizație de mediu întocmește un program de prevenire și reducere a\n")
                    .append("     cantităților de deșeuri, îl publică pe propriul site și îl transmite anual\n")
                    .append("     agenției județene, cu progresul, până la 31 mai. În profilul firmei nu e\n")
                    .append("     trecut niciun număr de autorizație de mediu, deci obligația nu se activează\n")
                    .append("     pe aceste date.\n\n");
        } else {
            sb.append("     Te privește: firma are autorizație de mediu (")
                    .append(permit).append(").\n")
                    .append("     Art. 44 alin. (1) cere un program de prevenire și reducere a cantităților de\n")
                    .append("     deșeuri generate, întocmit pe rezultatele unui audit de deșeuri, și măsuri de\n")
                    .append("     reducere a periculozității. Poate fi elaborat și de un terț (alin. (2)).\n")
                    .append("     Alin. (3) cere DOUĂ lucruri, nu unul: programul se PUBLICĂ pe pagina proprie\n")
                    .append("     de internet a firmei ȘI se TRANSMITE anual agenției județene, cu progresul\n")
                    .append("     înregistrat, până la 31 mai anul următor. Termenul din 31 mai e în calendarul\n")
                    .append("     aplicației; publicarea pe site nu se poate verifica de aici și rămâne a ta.\n\n");
        }
        return sb.toString();
    }

    /**
     * A line that may be long — a list of codes — folded under a fixed indent so README.txt stays
     * readable on paper, which is where it is read.
     */
    private static String wrapped(String indent, String text) {
        int width = 78 - indent.length();
        StringBuilder sb = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                sb.append(indent).append(line).append("\n");
                line.setLength(0);
            }
            line.append(line.length() > 0 ? " " : "").append(word);
        }
        if (line.length() > 0) {
            sb.append(indent).append(line).append("\n");
        }
        return sb.toString();
    }

    private static String marketRole(MarketRole role) {
        return switch (role) {
            case PRODUCER -> "producător";
            case IMPORTER -> "importator";
            case TRADER -> "comerciant";
        };
    }

    /**
     * Null type since V28: a pure haulage firm does nothing with the waste. The carrier is a tick, not
     * a type, so it is added to whatever the type says — the same firm is often collector and carrier.
     */
    private static String partnerType(Partner p) {
        String type = p.getType() == null ? null : switch (p.getType()) {
            case COLLECTOR -> "Colector";
            case RECOVERER -> "Valorificator";
            case GENERATOR -> "Generator";
        };
        if (p.isCarrier()) {
            return type == null ? "Transportator" : type + ", transportator";
        }
        return type == null ? "—" : type;
    }

    /** Un fişier Anexa 3 din dosar: numele fără extensie şi punctul de lucru pe care îl raportează. */
    private record Anexa3File(String baseName, UUID workPointId, String workPointName) {}

    /** Ce Anexe 3 intră într-un an, şi dacă a rămas vreuna pe dinafară fiindcă lipseşte rolul din profil. */
    /** {@code unplaced}: ieșiri vechi fără material, pe care formularul nu le poate așeza (BUG-049). */
    private record Anexa3Plan(List<Anexa3File> files, boolean roleMissing, int unplaced) {}

    /**
     * Anexele 3 Ambalaje ale unui an: câte una pe fiecare punct de lucru care are rânduri (preluări,
     * predări sau tratări pe coduri 15 01 xx). Un punct fără ambalaje nu primește foaie, iar unul cu
     * ambalaje dar fără rolul din profil e numit în README, nu tipărit ghicind tabelul.
     */
    private Anexa3Plan anexa3Plan(int year, List<WorkPoint> workPoints) {
        List<Anexa3File> files = new java.util.ArrayList<>();
        Set<String> used = new java.util.HashSet<>();
        boolean roleMissing = false;
        int unplaced = 0;
        for (WorkPoint wp : workPoints) {
            PackagingAnexa3 doc = packagingService.anexa3(year, wp.getId());
            unplaced += (int) doc.unclassified().stream().filter(PackagingAnexa3.UnclassifiedRow::missingMaterial).count();
            if (doc.intake().isEmpty() && doc.handovers().isEmpty() && doc.treatments().isEmpty()
                    && doc.unclassified().isEmpty()) {
                continue;
            }
            if (!doc.printable()) {
                roleMissing = true;
                continue;
            }
            String base = "anexa3-ambalaje-" + year + "-" + slug(wp.getName());
            String name = base;
            for (int i = 2; !used.add(name); i++) {
                name = base + "-" + i;
            }
            files.add(new Anexa3File(name, wp.getId(), wp.getName()));
        }
        return new Anexa3Plan(files, roleMissing, unplaced);
    }

    /** „Punct de lucru Cluj" → „punct-de-lucru-cluj": un nume de fişier care nu se strică pe nicio arhivă. */
    private static String slug(String name) {
        String folded = Diacritics.fold(name == null ? "" : name).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return folded.isEmpty() ? "punct" : folded;
    }

    private static void writeEntry(ZipOutputStream zip, List<String> written, String name, byte[] bytes)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
        // R2 — ce s-a scris chiar, nu ce s-a plănuit: din lista asta se naşte `99-verificare.txt`.
        written.add(name + "\t" + bytes.length);
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
