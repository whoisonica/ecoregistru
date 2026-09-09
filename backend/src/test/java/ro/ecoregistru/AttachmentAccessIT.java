package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.CloudinaryStorageService;

import static ro.ecoregistru.service.WasteMovementService.MAX_ATTACHMENT_BYTES;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 11-bis — an attachment is a document of a tenant, not a public link.
 *
 * <p>Until 09.09.2026 {@code AttachmentResponse} carried Cloudinary's {@code secure_url} and the
 * browser fetched the file straight from the CDN. That was not a theoretical hole: on the night it
 * was closed, a bare {@code curl} — no session, no header — downloaded a production document.
 * Anyone holding the URL held the file, for good, whichever company it belonged to, on an
 * application through which handover notes, contracts and drivers' identity papers pass, and
 * through which CNPs will pass at Etapa 9.
 *
 * <p>What the four tests below pin is the whole of the fix: the URL is gone from the API, and the
 * bytes cost a session and membership of the right tenant.
 *
 * <p>{@link CloudinaryStorageService} is mocked because the real one talks to Cloudinary over the
 * network — the seam exists so this suite proves the access rules without an account and without a
 * connection. What the storage service itself promises is pinned separately, in
 * {@code CloudinaryStorageServiceTest}.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AttachmentAccessIT {

    private static final byte[] FILE_BYTES = "conținutul avizului".getBytes();

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired AttachmentRepository attachmentRepository;

    @MockBean CloudinaryStorageService storageService;

    private String tokenA;
    private String tokenB;
    private UUID movementA;
    private UUID attachmentA;

    @BeforeEach
    void setUp() {
        when(storageService.signedUrl(anyString(), any(), any(), any()))
                .thenReturn("https://res.cloudinary.com/x/image/authenticated/s--sig--/v1/y.pdf");
        try {
            when(storageService.fetch(anyString())).thenReturn(FILE_BYTES);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        AppUser userA = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        tokenA = jwtService.generateToken(userA);

        WasteCode code = wasteCodeRepository.findAll().get(0);
        WorkPoint wpA = workPointRepository
                .findAllByCompany_IdAndActiveTrue(userA.getCompany().getId()).get(0);
        movementA = movementRepository.save(WasteMovement.builder()
                .company(userA.getCompany()).workPoint(wpA).date(LocalDate.now()).wasteCode(code)
                .quantity(new BigDecimal("5.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(userA.getId()).build()).getId();
        attachmentA = attachmentRepository.save(Attachment.builder()
                .movement(movementRepository.findById(movementA).orElseThrow())
                .url("https://res.cloudinary.com/x/image/authenticated/s--sig--/v1/aviz.pdf")
                .publicId("ecoregistru/movements/aviz")
                .resourceType("image").deliveryType("authenticated").format("pdf")
                .fileName("aviz.pdf").contentType("application/pdf")
                .createdAt(Instant.now()).build()).getId();

        // A second company, with a user of its own, to ask for someone else's file.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company companyB = companyRepository.save(Company.builder()
                .name("Tenant B SRL").cui("ROB" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser userB = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix + "@tenantb.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(companyB).enabled(true).createdAt(Instant.now()).build());
        tokenB = jwtService.generateToken(userB);
    }

    private String contentUrl() {
        return "/api/v1/movements/" + movementA + "/attachments/" + attachmentA + "/continut";
    }

    /** The exact command that downloaded a production document on 09.09.2026. */
    @Test
    void withoutASessionThereIsNoFile() throws Exception {
        mockMvc.perform(get(contentUrl()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * The half the old design could not do at all: even a valid session is not enough, because a
     * session belongs to a company. Another tenant's attachment is a 404 — not a 403, which would
     * confirm the id exists.
     */
    @Test
    void anotherTenantGetsNothing() throws Exception {
        mockMvc.perform(get(contentUrl()).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void theOwnerGetsTheFile() throws Exception {
        mockMvc.perform(get(contentUrl()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(header().string("Content-Disposition", containsString("aviz.pdf")))
                .andExpect(content().bytes(FILE_BYTES));
    }

    /**
     * Limita de mărime, pe server — nu în dropzone.
     *
     * <p>Până acum singura verificare era în browser, în {@code file-dropzone.tsx}, și era la
     * 15 MB: peste zidul adevărat, care e limita de 10 MB per asset a contului Cloudinary. Deci
     * un fișier de 12 MB trecea de interfață, trecea și de multipart (25 MB) și cădea abia la
     * furnizor, cu o eroare care nu ajungea la om ca mesaj. Iar un {@code curl} direct pe API nu
     * vedea niciodată verificarea din browser.
     *
     * <p>Ce se verifică aici nu e doar codul de răspuns, ci și că storage-ul <b>nu e chemat</b>:
     * un 400 dat după upload ar fi lăsat fișierul urcat și rândul nescris.
     */
    @Test
    void aFileOverTheLimitIsRefusedBeforeItIsUploaded() throws Exception {
        var tooBig = new MockMultipartFile("file", "scan.pdf", "application/pdf",
                new byte[(int) MAX_ATTACHMENT_BYTES + 1]);

        mockMvc.perform(multipart("/api/v1/movements/" + movementA + "/attachments").file(tooBig)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("attachment.too.large")));

        verify(storageService, never()).upload(any(), anyString());
    }

    /** Iar marginea de sub prag trece — altfel testul de deasupra ar fi trecut și cu totul închis. */
    @Test
    void aFileAtTheLimitStillGoesThrough() throws Exception {
        when(storageService.upload(any(), anyString())).thenReturn(
                // `url` e NOT NULL din `V1`; Cloudinary întoarce oricum `secure_url` la fiecare
                // upload. Se scrie în tabel, dar nu iese niciodată prin API — vezi testul de jos.
                new CloudinaryStorageService.StoredFile("https://res.cloudinary.com/x/y.pdf",
                        "ecoregistru/movements/x/y", "image", "authenticated", "pdf"));
        var justUnder = new MockMultipartFile("file", "scan.pdf", "application/pdf",
                new byte[(int) MAX_ATTACHMENT_BYTES]);

        mockMvc.perform(multipart("/api/v1/movements/" + movementA + "/attachments").file(justUnder)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    /**
     * The regression that matters most, and the cheapest to lose: if a URL ever creeps back into
     * the movement payload, every other test here still passes and the hole is open again.
     */
    @Test
    void theApiNeverHandsOutACloudinaryUrl() throws Exception {
        mockMvc.perform(get("/api/v1/movements/" + movementA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aviz.pdf")))
                .andExpect(content().string(not(containsString("cloudinary"))))
                .andExpect(content().string(not(containsString("http"))));
    }
}
