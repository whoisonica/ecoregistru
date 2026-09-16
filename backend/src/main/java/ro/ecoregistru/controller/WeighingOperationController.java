package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.WeighingCancelRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.DepotRetentionReport;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.service.WeighingOperationService;

import java.util.List;
import java.util.UUID;

/**
 * Operațiunile de depozit (V46). Oricine scrie creează și cântărește ce e în lucru; finalizarea și
 * anularea sunt ale celor care aprobă (decizia proprietarului, 15.09.2026: admin și consultant).
 * Serviciul verifică același lucru. Vezi {@link WeighingOperationService}.
 */
@RestController
@RequestMapping("/api/v1/weighing-operations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WeighingOperationController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";
    static final String CAN_APPROVE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN')";

    WeighingOperationService service;
    ro.ecoregistru.service.WeighingDocumentService documentService;

    /** Lista ecranului: o direcție și o lună (sau un an); fără ele, tot ce are firma. */
    @GetMapping
    public List<WeighingOperationResponse> list(@RequestParam(required = false) WeighingOperationType type,
                                                @RequestParam(required = false) Integer year,
                                                @RequestParam(required = false) Integer month) {
        return service.list(type, year, month);
    }

    /**
     * D1.9 și D1.10 — reținerile la sursă dintr-o lună; fără {@code month}, tot anul, cu beneficiarii
     * pentru D205. Serviciul cere pe deasupra și dreptul de a vedea prețurile.
     */
    @GetMapping("/retentions")
    @PreAuthorize(CAN_APPROVE)
    public DepotRetentionReport retentions(@RequestParam int year, @RequestParam(required = false) Integer month) {
        return service.retentions(year, month);
    }

    /** D1.14 — registrul intrărilor și ieșirilor pe o lună (sau pe an), ca {@code .xlsx}. */
    @GetMapping("/registru")
    public org.springframework.http.ResponseEntity<byte[]> register(@RequestParam int year,
                                                                   @RequestParam(required = false) Integer month) {
        String file = "registru-intrari-iesiri-" + year + (month == null ? "" : String.format("-%02d", month)) + ".xlsx";
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment().filename(file).build().toString())
                .body(service.renderRegister(year, month));
    }

    /**
     * D1.13 — Anexa 3 pe tot transportul. Doar cine scrie, fiindcă prima tipărire alocă numărul
     * formularului; retipărirea dă același document.
     */
    @GetMapping("/{id}/anexa3")
    @PreAuthorize(CAN_WRITE)
    public org.springframework.http.ResponseEntity<byte[]> anexa3(@PathVariable UUID id) {
        return pdf(documentService.renderAnexa3(id), "anexa3-operatiune-" + id + ".pdf");
    }

    /** D1.13 — avizul de însoțire pe tot transportul. O citire: nu alocă nimic. */
    @GetMapping("/{id}/aviz")
    public org.springframework.http.ResponseEntity<byte[]> aviz(@PathVariable UUID id) {
        return pdf(documentService.renderAviz(id), "aviz-operatiune-" + id + ".pdf");
    }

    private static org.springframework.http.ResponseEntity<byte[]> pdf(byte[] body, String file) {
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.inline().filename(file).build().toString())
                .body(body);
    }

    @GetMapping("/{id}")
    public WeighingOperationResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public WeighingOperationResponse create(@RequestBody WeighingOperationRequest request) {
        return service.create(request);
    }

    /** Capul unei operațiuni în lucru. Tipul nu se schimbă; restul, da. */
    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public WeighingOperationResponse update(@PathVariable UUID id, @RequestBody WeighingOperationRequest request) {
        return service.update(id, request);
    }

    /** Tot formularul odată: liniile trimise le înlocuiesc pe cele salvate. */
    @PutMapping("/{id}/lines")
    @PreAuthorize(CAN_WRITE)
    public WeighingOperationResponse replaceLines(@PathVariable UUID id, @RequestBody WeighingLinesRequest request) {
        return service.replaceLines(id, request);
    }

    /** POST, ca `/{id}/reactivate` de la șoferi: e o faptă, nu o resursă. */
    @PostMapping("/{id}/finalize")
    @PreAuthorize(CAN_APPROVE)
    public WeighingOperationResponse finalizeOperation(@PathVariable UUID id) {
        return service.finalizeOperation(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(CAN_APPROVE)
    public WeighingOperationResponse cancel(@PathVariable UUID id, @RequestBody WeighingCancelRequest request) {
        return service.cancel(id, request.reason());
    }
}
