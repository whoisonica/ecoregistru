package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.NaturalPersonRequest;
import ro.ecoregistru.controller.response.NaturalPersonResponse;
import ro.ecoregistru.controller.response.NaturalPersonSummary;
import ro.ecoregistru.service.NaturalPersonService;

import java.util.List;
import java.util.UUID;

/**
 * Persoanele fizice ale depozitului (D1.7b). Le scrie oricine scrie în firmă, și operatorul de la
 * cântar, fiindcă el primește omul. Lista o citește toată firma, cu CNP-ul mascat; fișa întreagă, cu
 * CNP-ul și actul, doar cine poate scrie (Legea 190/2018 art. 4, minimizare).
 */
@RestController
@RequestMapping("/api/v1/natural-persons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NaturalPersonController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";

    NaturalPersonService service;

    @GetMapping
    public List<NaturalPersonSummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public NaturalPersonResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public NaturalPersonResponse create(@RequestBody @Valid NaturalPersonRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public NaturalPersonResponse update(@PathVariable UUID id, @RequestBody @Valid NaturalPersonRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** Ștergerea definitivă: doar o fișă dezactivată și fără nicio operațiune. */
    @DeleteMapping("/{id}/definitiv")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        service.reactivate(id);
        return ResponseEntity.noContent().build();
    }
}
