package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.ReceivedFormRequest;
import ro.ecoregistru.controller.response.ReceivedFormResponse;
import ro.ecoregistru.service.ReceivedFormService;

import java.util.List;
import java.util.UUID;

/**
 * D2.6 — registrul formularelor de transport primite, pe depozit. Îl scrie oricine scrie (omul de la poartă primește
 * formularul); nu se modifică și nu se șterge, se corectează cu un rând nou.
 */
@RestController
@RequestMapping("/api/v1/received-forms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReceivedFormController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";

    ReceivedFormService service;

    @GetMapping
    public List<ReceivedFormResponse> list(@RequestParam UUID workPointId, @RequestParam int year) {
        return service.list(workPointId, year);
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ReceivedFormResponse record(@RequestBody @Valid ReceivedFormRequest request) {
        return service.record(request);
    }

    @PostMapping("/{id}/correct")
    @PreAuthorize(CAN_WRITE)
    public ReceivedFormResponse correct(@PathVariable UUID id, @RequestBody @Valid ReceivedFormRequest request) {
        return service.correct(id, request);
    }

    @GetMapping("/registru")
    public ResponseEntity<byte[]> register(@RequestParam UUID workPointId, @RequestParam int year) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("registru-formulare-primite-" + year + ".pdf").build().toString())
                .body(service.render(workPointId, year));
    }
}
