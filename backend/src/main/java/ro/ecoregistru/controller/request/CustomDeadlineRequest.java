package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.DeadlineRecurrence;

import java.time.LocalDate;

/** Un termen propriu, la adăugare și la modificare (V81). */
public record CustomDeadlineRequest(
        @NotBlank @Size(max = 120) String title,
        @NotNull LocalDate dueDate,
        @NotNull DeadlineRecurrence recurrence,
        @Size(max = 500) String details
) {}
