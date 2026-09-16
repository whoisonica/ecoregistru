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

    /** Cât cântărește dosarul, ca ecranul s-o spună înainte de descărcare. */
    @GetMapping("/size")
    public AuditFileService.AuditFileSize size(@RequestParam int year,
                                               @RequestParam(defaultValue = "1") int years) {
        return auditFileService.size(year, years);
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
