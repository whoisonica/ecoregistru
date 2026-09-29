package ro.ecoregistru.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.security.TooManyRequestsException;
import ro.ecoregistru.service.AuditFileService;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Control dossier (FAZA DOSAR). Downloads a ZIP bundling the year's evidence, partner
 * authorizations and movement attachments. Read-only: any authenticated tenant member,
 * including CLIENT_VIEWER — presenting the dossier is a read action.
 */
@RestController
@RequestMapping("/api/v1/audit-file")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditFileController {

    AuditFileService auditFileService;

    /**
     * Câte dosare se împachetează deodată, pe toată aplicaţia, şi care firme au deja unul pe drum.
     *
     * <p><b>De ce.</b> Dosarul se scrie în flux, dintr-o singură tranzacţie, deci ţine o conexiune la
     * bază cât curge arhiva — la {@code ?years=5} cu ataşamente, minute întregi. Pool-ul are zece;
     * câteva descărcări paralele (un dublu-clic, un tab repornit, un consultant care deschide pe rând
     * dosarele clienţilor) îl goleau pentru toate firmele (29.09.2026). O firmă primeşte un dosar
     * odată, iar aplicaţia cel mult {@value #MAX_PARALLEL}: restul primesc 429 pe loc, fără să ocupe
     * nimic. Limita stă aici, nu în serviciu, ca dosarul însuşi să rămână neatins.
     */
    static final int MAX_PARALLEL = 3;
    /** Cât să aştepte clientul înainte să reîncerce: un dosar de un an iese de obicei sub asta. */
    static final long RETRY_AFTER_SECONDS = 30;

    java.util.Set<java.util.UUID> packing = java.util.concurrent.ConcurrentHashMap.newKeySet();
    java.util.concurrent.Semaphore slots = new java.util.concurrent.Semaphore(MAX_PARALLEL);

    /** Cât cântărește dosarul, ca ecranul s-o spună înainte de descărcare. */
    @GetMapping("/size")
    public AuditFileService.AuditFileSize size(@RequestParam int year,
                                               @RequestParam(defaultValue = "1") int years) {
        return auditFileService.size(year, years);
    }

    /** Ce documente intră în dosar și de ce, ca ecranul să le spună pe cele reale. */
    @GetMapping("/contents")
    public AuditFileService.AuditFileContents contents(@RequestParam int year,
                                                       @RequestParam(defaultValue = "1") int years) {
        return auditFileService.contents(year, years);
    }

    /**
     * @param year  the last reporting year in the dossier
     * @param years how many consecutive years back to include, 1..5. Three is the retention
     *              period an inspection may ask for (OUG 92/2021, art. 48 alin. (5)); five is the
     *              margin the specialist asked for on 24.08.2026. The default stays 1, because
     *              most downloads are for the year being filed.
     */
    @GetMapping
    public void download(@RequestParam int year,
                         @RequestParam(defaultValue = "1") int years,
                         HttpServletResponse response) {
        java.util.UUID tenant = TenantContext.require();
        if (!packing.add(tenant)) {
            throw new TooManyRequestsException(RETRY_AFTER_SECONDS, ErrorMessageEnum.AUDIT_FILE_BUSY);
        }
        if (!slots.tryAcquire()) {
            packing.remove(tenant);
            throw new TooManyRequestsException(RETRY_AFTER_SECONDS, ErrorMessageEnum.AUDIT_FILE_BUSY);
        }
        try {
            write(year, years, response);
        } finally {
            slots.release();
            packing.remove(tenant);
        }
    }

    private void write(int year, int years, HttpServletResponse response) {
        String name = years == 1
                ? "dosar-control-" + year + ".zip"
                : "dosar-control-" + (year - years + 1) + "-" + year + ".zip";
        auditFileService.write(year, years, new OutputStream() {
            // The zip headers go on with the first byte, not before: a refusal thrown ahead of it
            // (years out of range, no tenant) must still reach AdviceController as JSON.
            OutputStream body;

            private OutputStream body() throws IOException {
                if (body == null) {
                    response.setContentType("application/zip");
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(name).build().toString());
                    body = response.getOutputStream();
                }
                return body;
            }

            @Override
            public void write(int b) throws IOException {
                body().write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                body().write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                if (body != null) body.flush();
            }
        });
    }
}
