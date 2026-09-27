package ro.ecoregistru.controller.request;

import ro.ecoregistru.enums.SiatdModule;

import java.time.LocalDate;
import java.util.Map;

/** F6a — modulele SIATD bifate, fiecare cu data înrolării. Ce lipsește se debifează. O schimbă doar adminul firmei. */
public record SiatdSettingsRequest(Map<SiatdModule, LocalDate> enrolledFrom) {
}
