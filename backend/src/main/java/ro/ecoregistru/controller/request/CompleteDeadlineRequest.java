package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

/** Marks a deadline as done, with an optional free-text note (e.g. filing reference). */
public record CompleteDeadlineRequest(
        @Size(max = 500) String note
) {}
