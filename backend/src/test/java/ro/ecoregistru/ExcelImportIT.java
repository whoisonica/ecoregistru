package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.importer.ImportTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2.15 — importul de istoric din Excel. Ce contează, în ordine: că „Verifică" nu scrie nimic, că un
 * singur rând greşit ţine tot fişierul afară, că regulile sunt ale formularului (nu ale importului),
 * că acelaşi fişier de două ori nu dublează nimic şi că un CUI din altă firmă nu se găseşte.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ExcelImportIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired ImportTemplate template;
    @Autowired ConsultancyRepository consultancyRepository;

    private String platformToken;
    private String viewerToken;
    private UUID companyId;
    private String workPoint;
    private String cui;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Import " + suffix + " SRL").cui("RO" + suffix).type(CompanyType.BOTH)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        companyId = company.getId();
        workPoint = "Sediu " + suffix;
        workPointRepository.save(WorkPoint.builder()
                .company(company).name(workPoint).active(true).createdAt(Instant.now()).build());
        // Importul e numai al platformei (16.09.2026), care n-are firmă: o alege cu X-Tenant-Id.
        platformToken = jwtService.generateToken(user(null, Role.PLATFORM_ADMIN));
        viewerToken = jwtService.generateToken(user(company, Role.CLIENT_VIEWER));
        cui = "RO9" + suffix.replaceAll("\\D", "7");
    }

    private AppUser user(Company company, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + UUID.randomUUID().toString().substring(0, 8) + "@import.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private Object[] partnerRow() {
        return new Object[]{"Reciclare Import SRL", cui, "Colector", "Da", "Nu", "Nu",
                "AUT-12", LocalDate.of(2027, 1, 1), "Str. Fabricii 1", "J05/1/2020"};
    }

    private Object[] disposal() {
        return new Object[]{LocalDate.of(2026, 3, 10), workPoint, "15 01 01", "Eliminare", 1200, "kg",
                "D5", "Deșeu propriu", null, "Fișa 3", "Solid", "CT", null, null, null, null};
    }

    /** Text, nu celule tipate: aşa arată un Excel lipit din altă parte. */
    private Object[] recovery() {
        return new Object[]{"20.03.2026", workPoint, "150101*", "valorificare", "1,2", "tone",
                "R3", "Deseu propriu", cui, "Aviz 7", null, null, null, null, null, null};
    }

    @Test
    void theTemplateHasTheTwoDataSheetsAndTheInstructions() throws Exception {
        byte[] xlsx = mockMvc.perform(get("/api/v1/import/sablon").header("Authorization", "Bearer " + platformToken)
                        .header("X-Tenant-Id", companyId.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getSheetName(0)).isEqualTo("Puncte de lucru");
            assertThat(wb.getSheetName(1)).isEqualTo("Parteneri");
            assertThat(wb.getSheetName(2)).isEqualTo("Mișcări");
            assertThat(wb.getSheetName(3)).isEqualTo("Instrucțiuni");
            assertThat(wb.getSheet("Mișcări").getRow(0).getCell(16).getStringCellValue()).isEqualTo("Ambalaj pus pe piață");
            assertThat(wb.getSheet("Mișcări").getRow(0).getCell(2).getStringCellValue()).isEqualTo("Cod deșeu *");
        }
    }

    @Test
    void verifyingRunsTheWholeImportAndKeepsNothing() throws Exception {
        send("/api/v1/import/verificare", file(List.<Object[]>of(partnerRow()), List.<Object[]>of(disposal(), recovery())), platformToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved", is(false)))
                .andExpect(jsonPath("$.partnersNew", is(1)))
                .andExpect(jsonPath("$.movementsNew", is(2)))
                .andExpect(jsonPath("$.errors", empty()));

        assertThat(partnerRepository.findAllByCompany_Id(companyId)).isEmpty();
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).isEmpty();
    }

    @Test
    void importingSavesThePartnersAndTheMovementsThatNameThem() throws Exception {
        send("/api/v1/import", file(List.<Object[]>of(partnerRow()), List.<Object[]>of(disposal(), recovery())), platformToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved", is(true)));

        List<Partner> partners = partnerRepository.findAllByCompany_Id(companyId);
        assertThat(partners).singleElement().satisfies(p -> {
            assertThat(p.getCui()).isEqualTo(cui);
            assertThat(p.getType()).isEqualTo(PartnerType.COLLECTOR);
            assertThat(p.isClient()).isTrue();
            assertThat(p.getAuthorizationExpiry()).isEqualTo(LocalDate.of(2027, 1, 1));
        });

        List<WasteMovement> movements = movementRepository.findAllByCompany_IdAndDeletedFalse(companyId);
        assertThat(movements).hasSize(2);
        WasteMovement recovered = movements.stream()
                .filter(m -> m.getOperation() == WasteOperation.RECOVERED).findFirst().orElseThrow();
        assertThat(recovered.getDate()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(recovered.getWasteCode().getId()).isEqualTo(wasteCodeRepository.findByCode("15 01 01").orElseThrow().getId());
        assertThat(recovered.getQuantity()).isEqualByComparingTo(new BigDecimal("1.2"));
        assertThat(recovered.getUnit()).isEqualTo(Unit.TONS);
        assertThat(recovered.getOperationCode()).isEqualTo(WasteOperationCode.R3);
        assertThat(recovered.getRegister()).isEqualTo(WasteRegister.ANEXA_1);
        assertThat(recovered.getPartner().getId()).isEqualTo(partners.get(0).getId());
        WasteMovement disposed = movements.stream()
                .filter(m -> m.getOperation() == WasteOperation.DISPOSED).findFirst().orElseThrow();
        assertThat(disposed.getStorageType()).isEqualTo(StorageType.CT);
        assertThat(disposed.getPhysicalState()).isEqualTo(PhysicalState.SOLID);
    }

    @Test
    void theSameFileTwiceDoublesNothing() throws Exception {
        byte[] xlsx = file(List.<Object[]>of(partnerRow()), List.<Object[]>of(disposal(), recovery()));
        send("/api/v1/import", xlsx, platformToken).andExpect(jsonPath("$.saved", is(true)));

        send("/api/v1/import", xlsx, platformToken)
                .andExpect(jsonPath("$.saved", is(true)))
                .andExpect(jsonPath("$.partnersNew", is(0)))
                .andExpect(jsonPath("$.partnersExisting", is(1)))
                .andExpect(jsonPath("$.movementsNew", is(0)))
                .andExpect(jsonPath("$.movementsExisting", is(2)));

        assertThat(partnerRepository.findAllByCompany_Id(companyId)).hasSize(1);
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).hasSize(2);
    }

    /**
     * Rândurile 3–6 au câte o greşeală, fiecare de alt fel; rândul 2 e bun şi tot nu intră. Rândul 6
     * trece de citire şi e oprit de regula formularului, cu mesajul formularului.
     */
    @Test
    void oneBadRowKeepsTheWholeFileOutAndEveryBadRowIsNamed() throws Exception {
        Object[] unknownCode = disposal();
        unknownCode[2] = "15 01 99";
        Object[] unknownWorkPoint = disposal();
        unknownWorkPoint[1] = "Hala care nu există";
        Object[] noQuantity = disposal();
        noQuantity[4] = null;
        Object[] recoveryWithoutCode = recovery();
        recoveryWithoutCode[6] = null;

        send("/api/v1/import", file(List.<Object[]>of(partnerRow()),
                        List.<Object[]>of(disposal(), unknownCode, unknownWorkPoint, noQuantity, recoveryWithoutCode)), platformToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved", is(false)))
                .andExpect(jsonPath("$.errors[*].row", containsInAnyOrder(3, 4, 5, 6)))
                .andExpect(jsonPath("$.errors[?(@.row == 3)].message", hasItem(containsString("15 01 99"))))
                .andExpect(jsonPath("$.errors[?(@.row == 4)].message", hasItem(containsString("Punct de lucru"))))
                .andExpect(jsonPath("$.errors[?(@.row == 5)].message", hasItem(containsString("obligatoriu"))))
                .andExpect(jsonPath("$.errors[?(@.row == 6)].message",
                        hasItem(ErrorMessageEnum.OPERATION_CODE_REQUIRED_RECOVERY.getMessage())));

        assertThat(partnerRepository.findAllByCompany_Id(companyId)).isEmpty();
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).isEmpty();
    }

    /**
     * „Generare” a ieșit din listă (16.09.2026): deșeul propriu se importă ca predare, la fel ca pe
     * ecran. Un fișier pe șablonul vechi, cu rânduri de generare, se oprește pe rândul lor.
     */
    @Test
    void aGenerationRowIsNamedAndKeepsTheFileOut() throws Exception {
        Object[] generation = disposal();
        generation[3] = "Generare";
        generation[6] = null;
        generation[8] = null;

        send("/api/v1/import", file(List.<Object[]>of(partnerRow()), List.<Object[]>of(generation)), platformToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved", is(false)))
                .andExpect(jsonPath("$.errors[*].row", containsInAnyOrder(2)))
                .andExpect(jsonPath("$.errors[0].message", containsString("Generare")));

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).isEmpty();
    }

    @Test
    void aPartnerOfAnotherCompanyIsNotFoundByItsCui() throws Exception {
        Company other = companyRepository.save(Company.builder()
                .name("Altă firmă SRL").cui("RO" + UUID.randomUUID().toString().substring(0, 8))
                .type(CompanyType.GENERATOR).active(true).afmObligation(false).createdAt(Instant.now()).build());
        partnerRepository.save(Partner.builder().company(other).name("Partenerul altcuiva SRL").cui(cui)
                .type(PartnerType.COLLECTOR).client(true).active(true).createdAt(Instant.now()).build());

        send("/api/v1/import/verificare", file(List.<Object[]>of(), List.<Object[]>of(recovery())), platformToken)
                .andExpect(jsonPath("$.errors[0].row", is(2)))
                .andExpect(jsonPath("$.errors[0].message", containsString("nu e nici în foaia Parteneri")));
    }

    @Test
    void aFileThatIsNotTheTemplateIsRefusedWhole() throws Exception {
        byte[] renamed;
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(template.render()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.getSheet("Mișcări").getRow(0).getCell(4).setCellValue("Cantitate (kg)");
            wb.write(out);
            renamed = out.toByteArray();
        }
        send("/api/v1/import/verificare", renamed, platformToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("import.template.mismatch")));

        send("/api/v1/import/verificare", "%PDF-1.4 nu e Excel".getBytes(), platformToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("import.file.unreadable")));
    }

    /**
     * BUG-017 — un fişier mic care se umflă mult e oprit înainte ca POI să-l citească în model.
     * Aici e o singură celulă de 9 MB: comprimată intră în câţiva KB (trece de plasa de 12 MB a
     * cererii), dar despachetată sufocă heap-ul de 300 MB al dyno-ului. {@code MAX_ROWS} nu o
     * prinde — e un rând, nu două mii. Fără garda din {@code open()}, cererea ar da 500 (OOM) şi ar
     * doborî procesul pentru toţi clienţii; cu ea, un 400 curat şi serverul sănătos (cererea de mai
     * jos tot răspunde).
     */
    @Test
    void aTinyFileThatInflatesHugeIsRefusedBeforeItIsParsed() throws Exception {
        // Se umflă la nivelul arhivei, ca atacul real: fiecare foaie a şablonului e recopiată, iar
        // în sheet1.xml se strecoară un comentariu XML de 9 MB înainte de </worksheet>. Comprimat
        // (acelaşi octet repetat) intră în câţiva KB.
        byte[] bomb;
        try (java.util.zip.ZipInputStream in =
                     new java.util.zip.ZipInputStream(new ByteArrayInputStream(template.render()));
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            String padding = "<!--" + "A".repeat(9 * 1024 * 1024) + "-->";
            java.util.zip.ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                byte[] data = in.readAllBytes();
                if (e.getName().endsWith("sheet1.xml")) {
                    data = new String(data, java.nio.charset.StandardCharsets.UTF_8)
                            .replace("</worksheet>", padding + "</worksheet>")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
                zip.putNextEntry(new java.util.zip.ZipEntry(e.getName()));
                zip.write(data);
                zip.closeEntry();
            }
            zip.finish();
            bomb = out.toByteArray();
        }
        assertThat(bomb.length).as("comprimat trece de plasa de 12 MB").isLessThan(1024 * 1024);

        send("/api/v1/import/verificare", bomb, platformToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("import.too.many.rows")));

        // Serverul e viu după: garda a oprit fişierul, nu l-a lăsat să consume heap-ul.
        send("/api/v1/import/verificare", file(List.<Object[]>of(), List.<Object[]>of(recovery())), platformToken)
                .andExpect(status().isOk());
    }

    @Test
    void aViewerCannotImport() throws Exception {
        send("/api/v1/import", file(List.<Object[]>of(partnerRow()), List.<Object[]>of()), viewerToken)
                .andExpect(status().isForbidden());
        assertThat(partnerRepository.findAllByCompany_Id(companyId)).isEmpty();
    }

    // --- a doua felie: punctele de lucru, ambalajele, transportul ---

    /** Punctul de lucru din foaia lui e găsit de mișcarea din același fișier; cel existent nu se dublează. */
    @Test
    void aWorkPointFromItsSheetIsCreatedAndUsedByTheMovementsOfTheSameFile() throws Exception {
        Object[] atTheNewSite = disposal();
        atTheNewSite[1] = "Depozit Nord";
        send("/api/v1/import", file(List.<Object[]>of(new Object[]{"Depozit Nord", "Str. Nordului 3"},
                        new Object[]{workPoint.toUpperCase(), null}),
                List.<Object[]>of(), List.<Object[]>of(atTheNewSite)), platformToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved", is(true)))
                .andExpect(jsonPath("$.workPointsNew", is(1)))
                .andExpect(jsonPath("$.workPointsExisting", is(1)))
                .andExpect(jsonPath("$.movementsNew", is(1)));

        List<WorkPoint> points = workPointRepository.findAllByCompany_Id(companyId);
        assertThat(points).extracting(WorkPoint::getName).containsExactlyInAnyOrder(workPoint, "Depozit Nord");
        UUID north = points.stream().filter(p -> p.getName().equals("Depozit Nord")).findFirst().orElseThrow().getId();
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId))
                .singleElement().satisfies(m -> assertThat(m.getWorkPoint().getId()).isEqualTo(north));
    }

    /**
     * Importul îl facem noi, la implementare (proprietarul, 16.09.2026): nici administratorul firmei, nici
     * consultantul, nici operatorul nu ajung la el — nici la șablon. Fișierul e valid, deci refuzul vine din
     * rol, nu din conținut.
     */
    @Test
    void onlyThePlatformImports() throws Exception {
        Company company = companyRepository.findById(companyId).orElseThrow();
        byte[] valid = file(List.<Object[]>of(partnerRow()), List.<Object[]>of(disposal()));
        // Consultantul e chiar al cabinetului care are firma: vede firma, dar tot nu importă.
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + companyId).cui("RO" + companyId.toString().replaceAll("\\D", "").substring(0, 8))
                .createdAt(Instant.now()).build());
        company.setConsultancy(cabinet);
        companyRepository.save(company);
        AppUser consultant = appUserRepository.save(AppUser.builder()
                .email("consultant+" + UUID.randomUUID().toString().substring(0, 8) + "@import.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CONSULTANT).consultancy(cabinet).enabled(true).createdAt(Instant.now()).build());
        for (Role role : List.of(Role.ADMIN, Role.CONSULTANT, Role.OPERATOR)) {
            String token = jwtService.generateToken(role == Role.CONSULTANT ? consultant : user(company, role));
            mockMvc.perform(get("/api/v1/import/sablon").header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Id", companyId.toString()))
                    .andExpect(status().isForbidden());
            send("/api/v1/import/verificare", valid, token).andExpect(status().isForbidden());
            send("/api/v1/import", valid, token).andExpect(status().isForbidden());
        }
        assertThat(partnerRepository.findAllByCompany_Id(companyId)).isEmpty();
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).isEmpty();
    }

    /** Coloanele de după „Observații” ajung în rubricile de ambalaje și de Anexa 3 ale mișcării. */
    @Test
    void packagingAndTransportColumnsReachTheMovement() throws Exception {
        String carrierCui = "RO8" + UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 6);
        Object[] carrier = {"Transport Rapid SRL", carrierCui, "Colector", "Nu", "Da", "Da",
                "AM 7/2025", null, null, null};
        Object[] row = Arrays.copyOf(disposal(), 28);
        row[16] = "Da";
        row[17] = "Hârtie carton";
        row[18] = "Ambalaje secundare şi de transport";
        row[19] = "Nu";
        row[20] = "nu";
        row[21] = "GENERATOR_PJ";
        row[22] = LocalDate.of(2026, 3, 10);
        row[23] = "11.03.2026";
        row[24] = carrierCui;
        row[25] = "Ion Popescu";
        row[26] = "CI AB 123456";
        row[27] = "B 12 ABC";

        send("/api/v1/import", file(List.<Object[]>of(), List.<Object[]>of(carrier), List.<Object[]>of(row)), platformToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors", empty()))
                .andExpect(jsonPath("$.saved", is(true)));

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).singleElement().satisfies(m -> {
            assertThat(m.getPackagingOnMarket()).isTrue();
            assertThat(m.getPackagingMaterial()).isEqualTo(PackagingMaterial.HARTIE_CARTON);
            assertThat(m.getPackagingCategory()).isEqualTo(PackagingCategory.SECONDARY);
            assertThat(m.getPackagingReusable()).isFalse();
            assertThat(m.getPackagingHazardousContent()).isFalse();
            assertThat(m.getPackagingOrigin()).isEqualTo(PackagingOrigin.GENERATOR_PJ);
            assertThat(m.getLoadDate()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(m.getUnloadDate()).isEqualTo(LocalDate.of(2026, 3, 11));
            assertThat(m.getTransportPartner().getId()).isEqualTo(partnerRepository.findAllByCompany_Id(companyId)
                    .stream().filter(p -> carrierCui.equals(p.getCui())).findFirst().orElseThrow().getId());
            assertThat(m.getDriverName()).isEqualTo("Ion Popescu");
            assertThat(m.getDriverIdentification()).isEqualTo("CI AB 123456");
            assertThat(m.getVehicleRegistration()).isEqualTo("B 12 ABC");
        });
    }

    /** O valoare greșită în coloanele noi e o eroare cu rândul și coloana ei, ca în rest. */
    @Test
    void aWrongPackagingMaterialIsNamedOnItsRow() throws Exception {
        Object[] row = Arrays.copyOf(disposal(), 28);
        row[16] = "Da";
        row[17] = "Carton ondulat";
        send("/api/v1/import/verificare", file(List.<Object[]>of(), List.<Object[]>of(row)), platformToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].message", containsString("Material ambalaj")));
    }

    /** Un fișier pe șablonul de dinainte (fără foaia de puncte și fără coloanele noi) se citește ca până acum. */
    @Test
    void aFileOnTheFirstTemplateStillImports() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));
            Sheet movements = wb.createSheet("Mișcări");
            Row header = movements.createRow(0);
            List<String> firstTemplate = List.of("Data *", "Punct de lucru *", "Cod deșeu *", "Operațiune *",
                    "Cantitate *", "UM *", "Cod R/D", "Registru", "Partener (CUI sau denumire)", "Nr. document",
                    "Stare fizică", "Tip stocare", "Mod tratare", "Mijloc de transport", "Destinație", "Observații");
            for (int i = 0; i < firstTemplate.size(); i++) header.createCell(i).setCellValue(firstTemplate.get(i));
            List<Object[]> rows = new ArrayList<>();
            rows.add(disposal());
            for (int i = 0; i < rows.size(); i++) {
                Row r = movements.createRow(i + 1);
                Object[] v = rows.get(i);
                for (int j = 0; j < v.length; j++) {
                    if (v[j] == null) continue;
                    if (v[j] instanceof LocalDate d) { r.createCell(j).setCellValue(d); r.getCell(j).setCellStyle(date); }
                    else if (v[j] instanceof Number n) r.createCell(j).setCellValue(n.doubleValue());
                    else r.createCell(j).setCellValue(v[j].toString());
                }
            }
            wb.write(out);
            send("/api/v1/import", out.toByteArray(), platformToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.saved", is(true)))
                    .andExpect(jsonPath("$.movementsNew", is(1)));
        }
    }

    /** Coloanele noi sunt un bloc: un antet care le începe și apoi le schimbă e refuzat întreg. */
    @Test
    void aHalfWrittenExtraHeaderIsRefused() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template.render()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.getSheet("Mișcări").getRow(0).getCell(18).setCellValue("Altceva");
            wb.write(out);
            send("/api/v1/import/verificare", out.toByteArray(), platformToken)
                    .andExpect(status().isBadRequest());
        }
    }

    // --- helpers ---

    private ResultActions send(String path, byte[] xlsx, String token) throws Exception {
        return mockMvc.perform(multipart(path)
                .file(new MockMultipartFile("file", "import.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-Id", companyId.toString()));
    }

    private byte[] file(List<Object[]> partners, List<Object[]> movements) throws Exception {
        return file(List.of(), partners, movements);
    }

    private byte[] file(List<Object[]> workPoints, List<Object[]> partners, List<Object[]> movements) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template.render()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));
            fill(wb.getSheet("Puncte de lucru"), workPoints, date);
            fill(wb.getSheet("Parteneri"), partners, date);
            fill(wb.getSheet("Mișcări"), movements, date);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void fill(Sheet sheet, List<Object[]> rows, CellStyle date) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Object[] values = rows.get(i);
            for (int j = 0; j < values.length; j++) {
                if (values[j] == null) continue;
                Cell cell = row.createCell(j);
                if (values[j] instanceof LocalDate d) {
                    cell.setCellValue(d);
                    cell.setCellStyle(date);
                } else if (values[j] instanceof Number n) {
                    cell.setCellValue(n.doubleValue());
                } else {
                    cell.setCellValue(values[j].toString());
                }
            }
        }
    }
}
