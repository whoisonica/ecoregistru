package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkPointRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 512) String address,
        /** D2.5 — autorizația de mediu a depozitului; goală = a firmei. */
        @Size(max = 100) String environmentalAuthNumber,
        java.time.LocalDate environmentalAuthExpiry,
        /** D2.6 — seria registrului formularelor primite. */
        @Size(max = 20) String receivedFormsSeries
) {

    public WorkPointRequest(String name, String address) {
        this(name, address, null, null, null);
    }
}
