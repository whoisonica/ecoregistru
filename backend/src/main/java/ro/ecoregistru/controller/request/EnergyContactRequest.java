package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** The company's contact details as Anexa 1 prints them; written on the company. */
public record EnergyContactRequest(
        @Size(max = 50) String fax,
        @Size(max = 255) String website,
        @Size(max = 255) String activitySector,
        @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 50) String mobile,
        LocalDate attestedOn
) {}
