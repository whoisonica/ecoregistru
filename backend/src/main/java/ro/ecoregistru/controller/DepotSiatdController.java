package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.request.SiatdConfirmRequest;
import ro.ecoregistru.controller.response.SiatdReceptionRow;
import ro.ecoregistru.controller.response.SiatdSummary;
import ro.ecoregistru.service.SiatdDeadlines.SiatdDeadline.State;
import ro.ecoregistru.service.SiatdService;

import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.controller.WeighingOperationController.CAN_APPROVE;

/**
 * F6a — termenele SIATD ale recepțiilor. Citirea, ca lista Cântarului, e a oricui din firmă, tăiată de
 * {@code DepotAccess}; confirmarea și anularea ei sunt ale celor care aprobă (serviciul verifică același lucru).
 */
@RestController
@RequestMapping("/api/v1/depot-siatd")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepotSiatdController {

    SiatdService service;

    @GetMapping("/receptions")
    public List<SiatdReceptionRow> receptions(@RequestParam(defaultValue = "PENDING") State state) {
        return service.receptions(state);
    }

    @GetMapping("/summary")
    public SiatdSummary summary() {
        return service.summary();
    }

    @PostMapping("/confirmations")
    @PreAuthorize(CAN_APPROVE)
    public List<SiatdReceptionRow> confirm(@RequestBody SiatdConfirmRequest request) {
        return service.confirm(request.operationIds(), request.code());
    }

    @DeleteMapping("/confirmations/{operationId}")
    @PreAuthorize(CAN_APPROVE)
    public ResponseEntity<Void> unconfirm(@PathVariable UUID operationId) {
        service.unconfirm(operationId);
        return ResponseEntity.noContent().build();
    }
}
