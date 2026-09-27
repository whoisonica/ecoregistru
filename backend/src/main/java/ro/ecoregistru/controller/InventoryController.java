package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.request.InventoryDeclarationRequest;
import ro.ecoregistru.controller.request.InventoryHeaderRequest;
import ro.ecoregistru.controller.request.InventoryLinesRequest;
import ro.ecoregistru.controller.request.InventoryPvRequest;
import ro.ecoregistru.controller.response.InventoryResponse;
import ro.ecoregistru.service.InventoryService;

import java.util.List;
import java.util.UUID;

/** D3.5 — inventarul depozitului. Îl citește oricine vede depozitul; îl face și îl aprobă cine administrează firma. */
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryController {

    InventoryService service;
    ro.ecoregistru.service.InventoryDocumentService documents;

    public record CancelRequest(String reason) {
    }

    @GetMapping
    public List<InventoryResponse> list(@RequestParam(required = false) UUID workPointId) {
        return service.list(workPointId);
    }

    @GetMapping("/{id}")
    public InventoryResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/operations-during")
    public List<InventoryResponse.DuringOperation> operationsDuring(@PathVariable UUID id) {
        return service.operationsDuring(id);
    }

    @PostMapping
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse open(@RequestBody InventoryHeaderRequest request) {
        return service.open(request);
    }

    @PutMapping("/{id}/header")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse header(@PathVariable UUID id, @RequestBody InventoryHeaderRequest request) {
        return service.updateHeader(id, request);
    }

    @PutMapping("/{id}/declaration")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse declaration(@PathVariable UUID id, @RequestBody InventoryDeclarationRequest request) {
        return service.saveDeclaration(id, request);
    }

    @PutMapping("/{id}/lines")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse lines(@PathVariable UUID id, @RequestBody InventoryLinesRequest request) {
        return service.saveLines(id, request);
    }

    @PutMapping("/{id}/pv")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse pv(@PathVariable UUID id, @RequestBody InventoryPvRequest request) {
        return service.savePv(id, request);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse close(@PathVariable UUID id) {
        return service.close(id);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    @PostMapping("/{id}/recalculate")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse recalculate(@PathVariable UUID id) {
        return service.recalculate(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(StockController.CAN_MANAGE)
    public InventoryResponse cancel(@PathVariable UUID id, @RequestBody CancelRequest request) {
        return service.cancel(id, request.reason());
    }

    @GetMapping("/{id}/pdf/{document}")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id, @PathVariable String document) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("inventar-" + document + ".pdf").build().toString())
                .body(documents.inventory(id, document));
    }
}
